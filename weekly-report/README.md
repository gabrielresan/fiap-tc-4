# Weekly report

Função serverless (AWS Lambda, Java 21) que gera o **relatório semanal dos feedbacks** e o envia por e-mail aos administradores. É executada uma vez por semana por um agendador e não expõe endpoint HTTP.

## Fluxo

1. Calcula o período: a semana anterior, de segunda 00:00 a domingo 23:59, no fuso `America/Sao_Paulo`.
2. Lê no PostgreSQL os feedbacks enviados nesse período.
3. Calcula total, média das notas, quantidade por dia e quantidade por urgência.
4. Monta um e-mail em HTML e o envia por SMTP.

Erros de banco ou de envio não são tratados silenciosamente: a exceção é propagada para que a execução seja marcada como falha na Lambda e dispare o alarme de monitoramento.

## Responsabilidades

| Classe | Responsabilidade |
| --- | --- |
| `WeeklyReportHandler` | Ponto de entrada da Lambda; apenas orquestra as etapas. |
| `ReportPeriod` | Define a semana anterior no fuso de São Paulo. |
| `JdbcFeedbackRepository` | Consulta os feedbacks do período no PostgreSQL. |
| `WeeklyReportGenerator` | Calcula média, contagem por dia e por urgência. |
| `HtmlReportFormatter` | Gera o assunto e o corpo HTML do e-mail. |
| `SmtpReportPublisher` | Envia o e-mail por SMTP. |

Banco e envio ficam atrás de interfaces (`FeedbackRepository`, `ConnectionProvider`, `ReportPublisher`), o que permite testar a orquestração sem infraestrutura e trocar a autenticação do banco na AWS sem alterar o restante.

## Dados lidos

A função apenas **lê** a tabela `feedbacks`. O esquema pertence ao `feedback-service`, que grava as avaliações e cria a tabela a partir de `FeedbackEntity`. O arquivo `src/test/resources/schema.sql` reproduz esse esquema para os testes e a execução local; se a entidade mudar, ele deve ser atualizado.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `id` | `UUID` | Chave primária; não é lida pelo relatório. |
| `description` | `TEXT` | Obrigatória. |
| `rating` | `SMALLINT` | De 0 a 10. |
| `urgency` | `VARCHAR(10)` | `LOW`, `MEDIUM` ou `CRITICAL`, classificada pelo `feedback-service` (0 a 3, 4 a 6 e 7 a 10). |
| `submitted_at` | `TIMESTAMPTZ` | Data de envio; indexada. |

Como a tabela é criada pelo `feedback-service` na primeira inicialização, ele precisa ter sido implantado antes da primeira execução do relatório.

## Conteúdo do relatório

- Período, total de avaliações e média das notas (duas casas decimais).
- Quantidade de avaliações por dia, incluindo dias sem avaliações.
- Quantidade de avaliações por urgência.
- Lista com data de envio (horário de São Paulo), urgência, nota e descrição.

As descrições são escapadas antes de entrar no HTML, impedindo injeção de conteúdo no e-mail.

## Configuração

As variáveis de banco seguem os mesmos nomes do `feedback-service`. O envio por SMTP é próprio deste módulo: localmente usa o Mailpit; na AWS, o SMTP do Amazon SES.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DB_URL` | obrigatório | URL JDBC do PostgreSQL. |
| `DB_USER` | obrigatório | Usuário do banco (basta permissão de leitura). |
| `DB_PASSWORD` | obrigatório | Senha do banco. |
| `NOTIFICATION_FROM` | obrigatório | E-mail remetente. |
| `NOTIFICATION_RECIPIENTS` | obrigatório | E-mails dos administradores, separados por vírgula. |
| `SMTP_HOST` | `localhost` | Servidor SMTP. |
| `SMTP_PORT` | `1025` | Porta SMTP. |
| `SMTP_USERNAME` | vazio | Usuário SMTP. |
| `SMTP_PASSWORD` | vazio | Senha SMTP. |
| `SMTP_AUTH` | `false` | Habilita autenticação SMTP. |
| `SMTP_STARTTLS` | `false` | Habilita e exige STARTTLS. |

A função falha na inicialização se faltar alguma variável obrigatória ou se algum endereço de e-mail for inválido. Conexões SMTP têm timeout de cinco segundos.

## Testes

Requer Docker em execução (os testes de banco e de e-mail sobem PostgreSQL e Mailpit com Testcontainers).

```sh
mvn -f weekly-report/pom.xml test
```

## Execução local

O `docker-compose.yml` sobe PostgreSQL (porta `55432`, com o esquema e feedbacks de exemplo da semana anterior) e Mailpit (SMTP em `1025`, interface em `8025`).

```sh
docker compose -f weekly-report/docker-compose.yml up -d
mvn -f weekly-report/pom.xml -q test-compile exec:java
```

O e-mail gerado pode ser visto em http://localhost:8025. Para encerrar e apagar os dados:

```sh
docker compose -f weekly-report/docker-compose.yml down -v
```

### Com os dados do `feedback-service`

Para gerar o relatório a partir das avaliações gravadas pelo `feedback-service` local (banco na porta `55433`), suba apenas o Mailpit deste módulo e aponte as variáveis de banco para aquele PostgreSQL. As variáveis de ambiente têm prioridade sobre os padrões locais:

```powershell
docker compose -f weekly-report/docker-compose.yml up -d mailpit
$env:DB_URL = 'jdbc:postgresql://localhost:55433/feedbacks'
$env:DB_USER = 'feedback_service'
$env:DB_PASSWORD = 'feedback_service'
mvn -f weekly-report/pom.xml -q test-compile exec:java
```

O relatório considera apenas a semana anterior; avaliações enviadas na semana corrente aparecem no relatório da próxima segunda-feira.

## Empacotamento para a Lambda

```sh
mvn -f weekly-report/pom.xml clean package
```

Gera `weekly-report/target/weekly-report-lambda.jar` com todas as dependências.

| Configuração da Lambda | Valor |
| --- | --- |
| Runtime | Java 21 |
| Handler | `com.example.weeklyreport.WeeklyReportHandler::handleRequest` |
| Gatilho | Agendamento semanal (ex.: segunda-feira 08:00, `America/Sao_Paulo`) |

## Integração com o Tech Challenge

Lê os feedbacks gravados pelo `feedback-service` no mesmo PostgreSQL, sem depender do código de outros módulos. Na AWS, lê o banco na Neon e entrega o relatório por e-mail via Amazon SES. Veja a [configuração do deploy](../infra/README.md).
