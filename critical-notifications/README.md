# Critical notifications

API REST independente em Java 21 e Spring Boot 4.1.2 para receber feedbacks de outros serviços e registrar nos logs os avisos de urgência `CRITICAL`. A entrega de e-mail é simulada por logs, sem servidor SMTP, credenciais ou destinatários configurados.

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

## Execução

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

Os testes verificam filtro de urgência, validação HTTP, conteúdo e datas do aviso, registro em logs e escape de quebras de linha. Não dependem de SMTP ou Docker.

## Tech Challenge

A entrega por e-mail foi substituída por simulação em logs por decisão do grupo. Esta etapa não entrega avisos à caixa de entrada dos administradores. A implantação cloud/serverless, o deploy automatizado, a coleta dos logs e o controle de acesso entre serviços continuam pendentes. O endpoint atual não implementa autenticação.
