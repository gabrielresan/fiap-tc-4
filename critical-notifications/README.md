# Critical notifications

API REST em Java 21 e Spring Boot 4.1.2. Recebe avaliações do `feedback-service` e registra os avisos de urgência `CRITICAL` nos logs. Na AWS, os registros ficam no CloudWatch Logs.

## Endpoint

`POST /notifications`, com `Content-Type: application/json`:

```json
{
  "description": "Aula indisponível",
  "rating": 1,
  "urgency": "CRITICAL",
  "submittedAt": "2026-11-06T12:00:00Z"
}
```

Todos os campos são obrigatórios. A descrição não pode ser vazia. A nota deve ser um inteiro entre 0 e 10. A urgência deve ser `LOW`, `MEDIUM` ou `CRITICAL`. A data é um `Instant` ISO-8601. A classificação da urgência é responsabilidade do serviço de feedback.

| HTTP | Resposta | Significado |
| --- | --- | --- |
| 200 | `{"logged":true}` | Feedback crítico registrado no log. |
| 200 | `{"logged":false}` | Feedback LOW ou MEDIUM; nenhum aviso registrado. |
| 400 | `application/problem+json` | JSON inválido ou campos inválidos/ausentes. |

A resposta usa `logged`, substituindo o antigo `sent`, para informar o resultado do registro. Não representa entrega de e-mail. O registro inclui descrição, urgência, data original do feedback (`submittedAt`) e data do registro (`loggedAt`). Exemplo:

```text
critical_notification description=Aula indisponível urgency=CRITICAL submittedAt=2026-11-06T12:00:00Z loggedAt=2026-11-06T12:01:00Z
```

Quebras de linha e tabulações da descrição são escapadas para manter o aviso em uma linha. O adaptador `LoggingNotificationSender` usa SLF4J em nível INFO; os logs aparecem no console da aplicação. O serviço de domínio continua separado do transporte HTTP e do adaptador de saída pela interface `NotificationSender`.

Não há persistência, deduplicação ou retentativa interna: cada chamada crítica registra um novo aviso.

## Execução local

### Ambiente integrado

Use o [Compose da raiz](../README.md#executar-localmente-com-docker). A API fica em `http://localhost:8081`.

### Execução com Maven

O módulo tem aplicação, porta e dependências próprias, sem depender da aplicação raiz ou do weekly-report. A única variável opcional é `PORT` (padrão `8081`). Não exige servidor de e-mail ou variáveis `SMTP_*`/`NOTIFICATION_*`.

Com JDK 21 e Maven instalados, execute a partir da raiz:

```sh
mvn -f critical-notifications/pom.xml spring-boot:run
```

Em outro terminal PowerShell:

```powershell
$body = @{
    description = 'Aula indisponível'
    rating = 1
    urgency = 'CRITICAL'
    submittedAt = '2026-11-06T12:00:00Z'
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/notifications' -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

Para validar e gerar o JAR executável:

```sh
mvn -f critical-notifications/pom.xml clean verify
java -jar critical-notifications/target/critical-notifications-0.0.1-SNAPSHOT.jar
```

## Testes

Execute `mvn -f critical-notifications/pom.xml test` para verificar o filtro de urgência, a validação HTTP e o registro dos avisos. Os testes Maven deste módulo dispensam SMTP e Docker.

Na [coleção Postman local](../postman/fiap-tc-4-local.postman_collection.json), execute a pasta **Critical notifications** para testar a rota `POST /notifications`.

## Execução na AWS Lambda

O mesmo controller recebe eventos do API Gateway HTTP API (payload 2.0) na Lambda. O handler ativa o perfil `lambda` e inicializa o Spring uma vez por instância. A execução local continua usando o Tomcat embutido.

```sh
mvn -f critical-notifications/pom.xml clean verify
```

Use o pacote `critical-notifications/target/critical-notifications-lambda.zip` para configurar a função.

| Configuração | Valor |
| --- | --- |
| Runtime | Java 21 |
| Handler | `br.com.fiap.tc4.notifications.CriticalNotificationsHandler::handleRequest` |
| Integração | API Gateway HTTP API, payload 2.0 |
| Rota | `POST /notifications` |

O `feedback-service` deve receber a URL base da API em `NOTIFICATIONS_URL`. Uma avaliação crítica deve gerar o evento `critical_notification` nos logs da função. O corpo da requisição e as respostas HTTP são os mesmos da execução local.

Veja o [guia de deploy e operação](../infra/README.md) para testar o fluxo completo na AWS.
