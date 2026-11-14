# Infraestrutura (AWS SAM)

O template AWS SAM cria as três Lambdas, as APIs, o agendamento, os logs e os papéis de acesso. O PostgreSQL fica na Neon. As Lambdas usam a rede padrão da AWS para acessar a Neon, o SES e a API de notificações.

## Funções e gatilhos

Veja o fluxo entre os módulos no [README do projeto](../README.md#arquitetura).

| Função | Gatilho | Responsabilidade |
| --- | --- | --- |
| `feedback-service` | API pública `POST /avaliacao` | Valida, classifica a urgência, grava no banco e encaminha avaliações críticas. |
| `critical-notifications` | `POST /notifications` com autenticação IAM | Envia os avisos de urgência `CRITICAL` aos administradores via SES. |
| `weekly-report` | EventBridge Scheduler, segunda-feira 09:00 (`America/Sao_Paulo`) | Gera o relatório da semana anterior e o envia por e-mail via Amazon SES. |

Cada função tem pacote, papel IAM, grupo de logs e gatilho próprios.

## Recursos

| Grupo | Recursos | Observação |
| --- | --- | --- |
| Banco | PostgreSQL na Neon | Criado fora da stack, com acesso JDBC via TLS. |
| Credenciais | GitHub Actions Secrets | O deploy configura a conexão nas variáveis das funções. |
| E-mail | Identidade do Amazon SES (`ReportSenderEmail`) | O SES envia um link de verificação no primeiro deploy. |
| Funções | 3 `AWS::Serverless::Function` (Java 21) | `feedback-service` e `critical-notifications` com 1024 MB e SnapStart; `weekly-report` com 512 MB e 60 s. |
| APIs | 2 HTTP APIs do API Gateway | Pública com limite de 20 req/s (pico 50); interna com 5 req/s (pico 10). Ambas com log de acesso. |
| Agendamento | EventBridge Scheduler (`ScheduleV2`) | Até 2 novas tentativas em caso de falha, dentro de 1 hora. |
| Logs | 5 grupos no CloudWatch Logs | Um por função e um por API, com retenção configurável (padrão 14 dias). |
| CI/CD | Bucket S3 de artefatos e papel de execução do CloudFormation | O workflow atual usa um usuário IAM. O template também oferece suporte a OIDC. |

## Segurança e governança de acesso

- **Conexão ao banco:** o driver PostgreSQL usa `sslmode=verify-full` para verificar o certificado e o nome do host da Neon.
- **Credenciais do banco:** use GitHub Actions Secrets. O CloudFormation mascara a senha com `NoEcho`. Restrinja o acesso à configuração das Lambdas.
- **Menor privilégio nas funções:** cada função tem seu próprio papel IAM, com apenas o que precisa:

  | Função | Permissão além dos próprios logs |
  | --- | --- |
  | `feedback-service` | `execute-api:Invoke` somente na rota `POST /notifications` da API interna. |
  | `critical-notifications` | `ses:SendEmail` somente com a identidade do remetente configurado. |
  | `weekly-report` | `ses:SendEmail` somente quando o remetente é o endereço configurado. |

- **E-mail sem senha:** as duas funções que enviam e-mail usam a API do SES com o papel IAM da própria função; não há usuário nem senha SMTP.
- **API interna autenticada:** a rota `POST /notifications` exige assinatura AWS (autorização IAM). Só o `feedback-service`, que assina a chamada com as credenciais do seu papel, consegue chamá-la; requisições sem assinatura recebem 403.
- **Limites de requisições:** a API pública aceita até 20 req/s (pico 50) e a interna até 5 req/s (pico 10).
- **Dados pessoais nos logs:** os logs de acesso registram rota, status e latência, mas não o IP de quem chamou. Todos os logs expiram após o período de retenção.
- **Deploy pelo GitHub Actions:** as credenciais da AWS e da Neon ficam nos *secrets* do repositório, mascarados nos logs. O workflow repassa a criação dos recursos ao papel de execução do CloudFormation (`--role-arn`); as permissões recomendadas para o usuário da pipeline estão em [Com o usuário IAM do projeto AWS](#com-o-usuário-iam-do-projeto-aws). O template também oferece papéis com OIDC para um deploy sem chaves de acesso (veja [Com OIDC](#com-oidc)).

## Parâmetros

| Parâmetro | Padrão | Uso |
| --- | --- | --- |
| `Environment` | `dev` | Prefixo dos nomes dos recursos (`dev` ou `prod`). |
| `ReportSenderEmail` | obrigatório | Remetente do relatório semanal; precisa ser verificado no SES. |
| `AdminEmails` | obrigatório | Destinatários do relatório, separados por vírgula. |
| `LogRetentionInDays` | `14` | Retenção dos logs (1, 3, 7, 14 ou 30 dias). |
| `GitHubRepository` | `gabrielresan/fiap-tc-4` | Repositório autorizado a fazer deploy. |
| `GitHubDeployBranch` | `main` | Branch autorizada a fazer deploy. |
| `EnableGitHubOidc` | `true` | Controla a criação do provedor e dos papéis de OIDC. O workflow com chaves IAM usa `false`. |
| `NeonDbUrl` | obrigatório | URL JDBC da Neon, sem credenciais, com `sslmode=verify-full`. |
| `NeonDbUser` | obrigatório | Usuário PostgreSQL da Neon. |
| `NeonDbPassword` | obrigatório | Senha da Neon, com `NoEcho`. |
| `DeploymentRevision` | obrigatório | Valor único por deploy para atualizar a versão publicada do feedback-service. |
| `ServicesEnabled` | `true` | Controla as execuções das Lambdas e o agendamento do relatório. |

Os e-mails não ficam no repositório: são informados no comando de deploy ou como variáveis do GitHub Actions.

## Pré-requisitos

- JDK 21, Docker (para os testes dos módulos), [AWS CLI v2](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) e [AWS SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html).
- Credenciais da AWS configuradas (`aws configure`) com permissão para criar os recursos acima. Teste com `aws sts get-caller-identity`.
- Um alerta de orçamento no AWS Budgets, para acompanhar o consumo de créditos.

## Deploy manual pelo terminal

Execute a partir da raiz do repositório.

1. Gere os pacotes das três funções:

   ```sh
   mvn -f feedback-service/pom.xml clean package
   mvn -f critical-notifications/pom.xml clean package
   mvn -f weekly-report/pom.xml clean package
   ```

   O template usa os artefatos `feedback-service/target/feedback-service-lambda.zip`, `critical-notifications/target/critical-notifications-lambda.zip` e `weekly-report/target/weekly-report-lambda.jar`.

2. Valide o template:

   ```sh
   cd infra
   sam validate --lint
   ```

3. Carregue os três valores da Neon nas variáveis de ambiente `NEON_DB_URL`, `NEON_DB_USER` e `NEON_DB_PASSWORD` e execute o deploy.

   ```sh
   sam deploy --parameter-overrides Environment=dev EnableGitHubOidc=false \
     ReportSenderEmail=remetente@exemplo.com AdminEmails=admin@exemplo.com \
     "NeonDbUrl=$NEON_DB_URL" "NeonDbUser=$NEON_DB_USER" "NeonDbPassword=$NEON_DB_PASSWORD" \
     "DeploymentRevision=$(date -u +%Y%m%dT%H%M%S)"
   ```

   O exemplo usa `EnableGitHubOidc=false`, como o workflow atual. O `samconfig.toml` define a stack `fiap-tc4-dev`, a região `us-east-2`, a resolução do bucket de artefatos e a confirmação do *change set*. O SAM envia os pacotes ao S3 e mostra as alterações para revisão.

4. Abra o e-mail de verificação enviado pela AWS ao `ReportSenderEmail` e clique no link. Enquanto a conta estiver no *sandbox* do SES, cada destinatário diferente do remetente também precisa ser verificado:

   ```sh
   aws sesv2 create-email-identity --region us-east-2 --email-identity admin@exemplo.com
   ```

5. Consulte os outputs (URL da API, nomes das funções, ARNs dos papéis):

   ```sh
   aws cloudformation describe-stacks --region us-east-2 --stack-name fiap-tc4-dev --query "Stacks[0].Outputs" --output table
   ```

No primeiro deploy, o SnapStart inicializa o `feedback-service` para gerar a imagem da função; nesse momento o Hibernate cria a tabela `feedbacks` no banco.

## Teste após o deploy

### Postman

Importe a [coleção FIAP Tech Challenge 4 — AWS dev](../postman/fiap-tc-4-aws.postman_collection.json). Ela vem com os endereços do deploy de desenvolvimento em Ohio. Se a stack for recriada, abra **CloudFormation → fiap-tc4-dev → Saídas** e atualize `feedbackEndpoint`, `notificationsApiUrl` e `weeklyReportFunctionName` com os valores de `FeedbackEndpoint`, `NotificationsApiUrl` e `WeeklyReportFunctionName`.

- **Cadastrar avaliação crítica** grava uma avaliação de teste na Neon. Confira o aviso no CloudWatch para validar o encaminhamento.
- **Enviar aviso direto** chama a API de notificações com AWS Signature e dispara um e-mail real. A identidade do Postman precisa de `execute-api:Invoke` no ARN `arn:aws:execute-api:us-east-2:<ID_DA_CONTA>:<ID_DA_API>/$default/POST/notifications`. O ID da API é o primeiro trecho do host em `NotificationsApiUrl`.
- **Enviar relatório semanal** invoca a Lambda e pode enviar um e-mail real aos destinatários configurados. Execute essa requisição separadamente quando quiser testar o envio.

Para a requisição do relatório, crie o ambiente **FIAP AWS** no Postman e selecione-o no canto superior direito. Adicione `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` como variáveis sensíveis e preencha apenas os valores locais. Use uma identidade autorizada a executar `lambda:InvokeFunction` em `dev-weekly-report`. Se usar credenciais temporárias, adicione também `AWS_SESSION_TOKEN` ao ambiente e informe `{{AWS_SESSION_TOKEN}}` no campo **Session Token** da aba **Authorization**. [Ambientes do Postman](https://learning.postman.com/docs/use/send-requests/variables/managing-environments) · [AWS Signature no Postman](https://learning.postman.com/docs/use/send-requests/authorization/aws-signature/) · [Permissão para invocar a Lambda](https://docs.aws.amazon.com/lambda/latest/api/API_Invoke.html)

### Terminal

Enviar uma avaliação crítica (PowerShell), usando o output `FeedbackEndpoint`:

```powershell
$body = @{ descricao = 'Vídeo da aula não carrega'; nota = 2 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri '<FeedbackEndpoint>' -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

Acompanhar o aviso crítico e as requisições:

```sh
aws logs tail /aws/lambda/dev-critical-notifications --region us-east-2 --since 10m
aws logs tail /fiap-tc4/dev/api/feedback --region us-east-2 --since 10m
```

Executar o relatório semanal sem esperar a segunda-feira:

```sh
aws lambda invoke --region us-east-2 --function-name dev-weekly-report response.json
```

O relatório considera a semana anterior. Avaliações da semana corrente aparecem no relatório da próxima segunda-feira.

Para validar o aviso crítico, confirme o e-mail com descrição, urgência e data de envio. O grupo `/aws/lambda/dev-critical-notifications` registra `critical_notification_sent` com o ID retornado pelo SES, ou `critical_notification_delivery_failed` com a causa da falha. As duas funções usam o remetente e os destinatários configurados no deploy. Se o SES estiver no sandbox, verifique todos esses endereços em Ohio.

## Monitoramento

As funções e as APIs registram seus eventos no CloudWatch Logs, em grupos criados pelo próprio template, com retenção definida por `LogRetentionInDays` (padrão 14 dias).

| Grupo de logs | O que registra |
| --- | --- |
| `/aws/lambda/dev-feedback-service` | Execuções do `feedback-service` e o resultado do encaminhamento de avaliações críticas. |
| `/aws/lambda/dev-critical-notifications` | Execuções do `critical-notifications` e o resultado de cada envio pelo SES. |
| `/aws/lambda/dev-weekly-report` | Execuções do relatório, com período, quantidade de avaliações e média. |
| `/fiap-tc4/dev/api/feedback` | Uma linha JSON por requisição a `POST /avaliacao`: horário, rota, status HTTP, latência e erro de integração. |
| `/fiap-tc4/dev/api/notifications` | O mesmo para as chamadas a `POST /notifications`. |

Mensagens que indicam o andamento do fluxo:

| Mensagem | Grupo | Significado |
| --- | --- | --- |
| `critical_notification_sent id=...` | `dev-feedback-service` | A avaliação crítica foi encaminhada ao `critical-notifications`. |
| `critical_notification_failed id=...` | `dev-feedback-service` | O encaminhamento falhou; a avaliação continua gravada. |
| `critical_notification_sent messageId=...` | `dev-critical-notifications` | O SES aceitou o e-mail de aviso. |
| `critical_notification_delivery_failed` | `dev-critical-notifications` | O envio pelo SES falhou. |
| `Weekly report sent: ...` | `dev-weekly-report` | O relatório foi gerado e enviado. |

Para acompanhar em tempo real, use `aws logs tail` com `--follow`. No console, os mesmos dados ficam em **CloudWatch → Log groups** e, para cada função, em **Lambda → Monitor**, que mostra invocações, erros e duração.

```sh
aws logs tail /aws/lambda/dev-feedback-service --region us-east-2 --follow
aws logs tail /aws/lambda/dev-critical-notifications --region us-east-2 --follow
```

## Deploy pelo GitHub Actions

### Com o usuário IAM do projeto AWS

O [workflow](../.github/workflows/deploy-lambda.yml) testa e empacota os três módulos e usa o SAM para criar ou atualizar a stack `fiap-tc4-dev`. Cada push na `main`, inclusive o merge de um PR, dispara o workflow. A região padrão é Ohio (`us-east-2`).

1. Na conta e região do projeto, prepare um bucket S3 privado para os pacotes e um papel de serviço que o CloudFormation possa assumir. A política precisa cobrir os recursos do template. [Papéis de serviço do CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-iam-servicerole.html)
2. Autorize o usuário da pipeline a acessar o prefixo `fiap-tc4-dev/` do bucket, criar e executar *change sets* da stack e consultar seu andamento, e usar `iam:PassRole` somente para o papel de execução do CloudFormation. A política de atualização de uma Lambda é insuficiente para criar a stack.
3. Em **Settings → Secrets and variables → Actions** no GitHub, cadastre:

   | Tipo | Nome | Valor |
   | --- | --- | --- |
   | Secret | `AWS_ACCESS_KEY_ID` | ID da chave do usuário da pipeline. |
   | Secret | `AWS_SECRET_ACCESS_KEY` | Chave secreta do usuário da pipeline. |
   | Secret | `REPORT_SENDER_EMAIL` | Remetente do relatório e dos avisos críticos no SES. |
   | Secret | `ADMIN_EMAILS` | Destinatários do relatório e dos avisos críticos, separados por vírgula e sem espaços. |
   | Variável | `AWS_CFN_EXECUTION_ROLE_ARN` | ARN do papel de serviço do CloudFormation. |
   | Variável | `AWS_SAM_ARTIFACT_BUCKET` | Nome do bucket de artefatos. |
   | Variável opcional | `AWS_REGION` | Região da stack, caso seja diferente de `us-east-2`. |
   | Secret | `NEON_DB_URL` | `jdbc:postgresql://HOST_DA_NEON:5432/feedbacks?sslmode=verify-full`. Use o nome real do banco. |
   | Secret | `NEON_DB_USER` | Usuário selecionado em **Connect** na Neon. |
   | Secret | `NEON_DB_PASSWORD` | Senha desse usuário. |

4. Após um push ou merge na `main`, acompanhe a execução em **Actions → Deploy dos três serviços na AWS**. Para disparar manualmente, clique em **Run workflow** e selecione `main`.
5. Envie uma avaliação e confira se ela aparece na Neon. Teste o relatório e confira o envio pelo SES.

### Pausar e reativar o ambiente de desenvolvimento

O [workflow de pausa](../.github/workflows/schedule-environment.yml) liga as funções às 08:00 e as pausa às 18:00 em dias úteis, no horário de Brasília. Há uma verificação adicional às 18:25 todos os dias. O relatório roda às 09:00 de segunda.

1. Faça o deploy com a Neon antes de rodar o workflow. O papel do CloudFormation precisa permitir `lambda:PutFunctionConcurrency`, `lambda:DeleteFunctionConcurrency` e a atualização do agendamento.
2. Em **Actions → Ligar e pausar ambiente AWS → Run workflow**, teste `off` e depois `on`. Em `off`, as três funções ficam com concorrência igual a zero e o agendamento fica desativado. Em `on`, teste as APIs depois de a execução terminar.
3. Use `AWS_ENVIRONMENT_SCHEDULE_ENABLED=true` nas variáveis do repositório para habilitar o horário automático.

A Neon controla o próprio banco. As URLs continuam disponíveis e requisições podem gerar cobrança no API Gateway durante a pausa. [Agendamento no GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#schedule)

### Com OIDC

Para usar OIDC, faça o primeiro deploy local com `EnableGitHubOidc=true` e permissões para criar o provedor e os papéis. Depois, adapte o workflow para usar:

- `permissions: id-token: write` e `contents: read`;
- `aws-actions/configure-aws-credentials` com `role-to-assume` igual ao output `GitHubDeployRoleArn` e `aws-region: us-east-2`;
- build dos três módulos com Maven;
- `sam deploy --no-confirm-changeset --role-arn <CloudFormationExecutionRoleArn> --parameter-overrides ...`, executado em `infra/`.

O papel de deploy aceita tokens do repositório e da branch configurados no template. Por padrão, a branch é `main`.

## Remoção

```sh
cd infra
sam delete --region us-east-2
```

Remove a stack da AWS. A Neon e o bucket de artefatos são gerenciados fora dela.

## Custos

Os custos da AWS dependem do uso de Lambda, API Gateway, EventBridge, SES, CloudWatch e S3, todos cobrados por uso. A Neon foi escolhida para o banco por não gerar custo para o projeto. Confira os limites do plano no console da Neon.

## Observações de operação

- **Avisos críticos:** a avaliação é gravada antes do encaminhamento. Se o aviso falhar, o estudante recebe a confirmação normalmente e a falha fica registrada nos logs.
- **SES em sandbox:** remetente e destinatários precisam ser verificados; envio limitado a 200 e-mails por dia.
