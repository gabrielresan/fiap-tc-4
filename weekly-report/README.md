# Weekly report

Função serverless (AWS Lambda, Java 21) que gera o **relatório semanal dos feedbacks** e o envia por e-mail aos administradores. É executada uma vez por semana por um agendador e não expõe endpoint HTTP.

## Fluxo

1. Calcula o período: a semana anterior, de segunda 00:00 a domingo 23:59, no fuso `America/Sao_Paulo`.
2. Lê no PostgreSQL os feedbacks enviados nesse período.
3. Calcula total, média das notas, quantidade por dia e quantidade por urgência.
4. Monta um e-mail em HTML e o envia pelo Amazon SES na AWS ou pelo Mailpit via SMTP no ambiente local.

Erros de banco ou de envio são propagados e a execução é marcada como falha na Lambda.

## Responsabilidades

| Classe | Responsabilidade |
| --- | --- |
| `WeeklyReportHandler` | Ponto de entrada da Lambda; apenas orquestra as etapas. |
| `ReportPeriod` | Define a semana anterior no fuso de São Paulo. |
| `JdbcFeedbackRepository` | Consulta os feedbacks do período no PostgreSQL. |
| `WeeklyReportGenerator` | Calcula média, contagem por dia e por urgência. |
| `HtmlReportFormatter` | Gera o assunto e o corpo HTML do e-mail. |
| `SesReportPublisher` | Envia o e-mail pela API do Amazon SES. |
| `SmtpReportPublisher` | Envia o e-mail por SMTP na execução local. |

Banco e envio ficam atrás de interfaces (`FeedbackRepository`, `ConnectionProvider`, `ReportPublisher`), o que permite testar a orquestração sem infraestrutura e trocar a autenticação do banco na AWS sem alterar o restante.

## Dados lidos

A função apenas **lê** a tabela `feedbacks`. O esquema pertence ao `feedback-service`, que grava as avaliações e cria a tabela a partir de `FeedbackEntity`. O arquivo `src/test/resources/schema.sql` reproduz esse esquema para os testes e a execução local; se a entidade mudar, ele deve ser atualizado.

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `id` | `UUID` | Chave primária; não é lida pelo relatório. |
| `description` | `TEXT` | Obrigatória. |
| `rating` | `SMALLINT` | De 0 a 10. |
| `urgency` | `VARCHAR(10)` | Classificada pelo `feedback-service`. Por padrão, `CRITICAL` de 0 a 3, `MEDIUM` de 4 a 6 e `LOW` de 7 a 10. |
| `submitted_at` | `TIMESTAMPTZ` | Data de envio; indexada. |

Como a tabela é criada pelo `feedback-service` na primeira inicialização, ele precisa ter sido implantado antes da primeira execução do relatório.

## Conteúdo do relatório

- Período, total de avaliações e média das notas (duas casas decimais).
- Quantidade de avaliações por dia, incluindo dias sem avaliações.
- Quantidade de avaliações por urgência.
- Lista com data de envio (horário de São Paulo), urgência, nota e descrição.

As descrições são escapadas antes de entrar no HTML, impedindo injeção de conteúdo no e-mail.

## Configuração

As variáveis de banco seguem os mesmos nomes do `feedback-service`. Na AWS, o SAM configura o envio pela API do SES com o papel IAM da Lambda.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DB_URL` | obrigatório | URL JDBC do PostgreSQL. |
| `DB_USER` | obrigatório | Usuário do banco (basta permissão de leitura). |
| `DB_PASSWORD` | obrigatório | Senha do banco. |
| `NOTIFICATION_FROM` | obrigatório | E-mail remetente. |
| `NOTIFICATION_RECIPIENTS` | obrigatório | E-mails dos administradores, separados por vírgula. |
| `EMAIL_PROVIDER` | `smtp` | Use `smtp` para Mailpit local e `ses` para a API do Amazon SES. |
| `SMTP_HOST` | `localhost` | Servidor SMTP. |
| `SMTP_PORT` | `1025` | Porta SMTP. |
| `SMTP_USERNAME` | vazio | Usuário SMTP. |
| `SMTP_PASSWORD` | vazio | Senha SMTP. |
| `SMTP_AUTH` | `false` | Habilita autenticação SMTP. |
| `SMTP_STARTTLS` | `false` | Habilita e exige STARTTLS. |

A função falha na inicialização se faltar alguma variável obrigatória ou se algum endereço de e-mail for inválido. Conexões SMTP têm timeout de cinco segundos.

## Execução local

### Ambiente integrado

Use o [Compose da raiz](../README.md#executar-localmente-com-docker). O emulador da Lambda fica em `http://localhost:8082` e o Mailpit em `http://localhost:8025`.

### Execução com Maven

Na raiz do repositório, suba o PostgreSQL compartilhado (porta `55433`) e o Mailpit (SMTP em `1025`, interface em `8025`). Na primeira inicialização, o PostgreSQL carrega avaliações de exemplo da semana anterior.

```sh
docker compose up -d --wait postgres mailpit
mvn -f weekly-report/pom.xml -q test-compile exec:java
```

O e-mail gerado pode ser visto em http://localhost:8025. Para parar as dependências e preservar os dados:

```sh
docker compose stop postgres mailpit
```

O relatório lê a mesma base local do `feedback-service`. Ele considera apenas a semana anterior. Avaliações enviadas na semana corrente aparecem no relatório da próxima segunda-feira.

## Testes

Execute `mvn -f weekly-report/pom.xml test` com o Docker ativo. Os testes de integração usam PostgreSQL e Mailpit com Testcontainers.

Na [coleção Postman local](../postman/fiap-tc-4-local.postman_collection.json), execute a pasta **Weekly report** com o Compose da raiz ativo. A requisição invoca a função pelo emulador e o e-mail fica no Mailpit.

## Execução na AWS Lambda

```sh
mvn -f weekly-report/pom.xml clean package
```

Gera `weekly-report/target/weekly-report-lambda.jar` com todas as dependências.

| Configuração da Lambda | Valor |
| --- | --- |
| Runtime | Java 21 |
| Handler | `br.com.fiap.tc4.reporting.WeeklyReportHandler::handleRequest` |
| Gatilho | EventBridge Scheduler, segunda-feira às 09:00 (`America/Sao_Paulo`) |

## Integração com o Tech Challenge

Lê os feedbacks gravados pelo `feedback-service` no mesmo PostgreSQL, sem depender do código de outros módulos. Na AWS, lê o banco na Neon e entrega o relatório por e-mail via Amazon SES. Veja a [configuração do deploy](../infra/README.md).
