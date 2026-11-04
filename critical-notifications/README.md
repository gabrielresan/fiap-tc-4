# Critical notifications

API REST independente para receber feedbacks de outros serviços e enviar e-mails aos administradores quando a urgência for `CRITICAL`. Usa Java 21 e Spring Boot 4.1.2, mesma versão do projeto raiz. Mantém os modelos e a lógica de domínio separados do transporte HTTP e SMTP.

## Endpoint

`POST /notifications`, com `Content-Type: application/json`:

```json
{
  "description": "Aula indisponível",
  "rating": 1,
  "urgency": "CRITICAL",
  "submittedAt": "2026-11-04T12:00:00Z"
}
```

Todos os campos são obrigatórios. A descrição não pode ser vazia. A nota deve ser um inteiro entre 0 e 10. A urgência deve ser `LOW`, `MEDIUM` ou `CRITICAL`. A data é um `Instant` ISO-8601. A classificação da urgência é responsabilidade do serviço de feedback; esta API não a infere pela nota.

| HTTP | Resposta | Significado |
| --- | --- | --- |
| 200 | `{"sent":true}` | Feedback crítico; servidor SMTP aceitou o envio. |
| 200 | `{"sent":false}` | Feedback de urgência LOW ou MEDIUM; nenhum e-mail enviado. |
| 400 | `application/problem+json` | JSON inválido ou campos inválidos/ausentes. |
| 503 | `application/problem+json` | Falha ao enviar pelo servidor SMTP. |

O envio é síncrono; a API só retorna `sent: true` após o adaptador SMTP concluir. Isso não confirma a chegada à caixa de entrada. O aviso contém descrição, urgência, data original do feedback e instante da tentativa de envio, em UTC. Não há persistência, deduplicação ou retentativa interna. Repetir a requisição pode enviar outro e-mail, inclusive após falhas de comunicação com resultado incerto.

## Configuração e execução

O módulo tem aplicação, porta e dependências próprias, sem depender da aplicação raiz ou do módulo weekly-report.

| Variável | Padrão | Uso |
| --- | --- | --- |
| `PORT` | `8081` | Porta HTTP. |
| `NOTIFICATION_FROM` | obrigatório | E-mail remetente. |
| `NOTIFICATION_RECIPIENTS` | obrigatório | E-mails dos administradores, separados por vírgula. |
| `SMTP_HOST` | `localhost` | Servidor SMTP. |
| `SMTP_PORT` | `1025` | Porta SMTP. |
| `SMTP_USERNAME` | vazio | Usuário SMTP. |
| `SMTP_PASSWORD` | vazio | Senha SMTP. |
| `SMTP_AUTH` | `false` | Habilita autenticação SMTP. |
| `SMTP_STARTTLS` | `false` | Habilita e exige STARTTLS. |

A aplicação valida remetente e destinatários ao iniciar. Os padrões SMTP são para um servidor de captura de e-mails local. Para um provedor SMTP autenticado, configure host, porta, usuário, senha e habilite `SMTP_AUTH` e `SMTP_STARTTLS` conforme o provedor. Os timeouts de conexão, leitura e escrita são de cinco segundos.

Exemplo em PowerShell com servidor SMTP local já disponível em `localhost:1025`:

```powershell
$env:NOTIFICATION_FROM = 'notifications@example.com'
$env:NOTIFICATION_RECIPIENTS = 'admin@example.com'
mvn -f critical-notifications/pom.xml spring-boot:run
```

Em outro terminal:

```powershell
$body = @{
    description = 'Aula indisponível'
    rating = 1
    urgency = 'CRITICAL'
    submittedAt = '2026-11-04T12:00:00Z'
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/notifications' -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

Para gerar e executar o JAR independente:

```sh
mvn -f critical-notifications/pom.xml clean verify
java -jar critical-notifications/target/critical-notifications-0.0.1-SNAPSHOT.jar
```

As mesmas variáveis de ambiente devem estar presentes ao executar o JAR. Os testes exercitam o fluxo HTTP com Spring MVC e envio simulado; não enviam e-mails reais.

## Integração com o Tech Challenge

O serviço de feedback chama este endpoint após classificar o feedback. A API não acessa o banco de outro serviço. A implantação em cloud/serverless, o deploy automatizado, o monitoramento e o controle de acesso entre serviços continuam pendentes. O endpoint atual não implementa autenticação.

Referências: [Spring MVC](https://docs.spring.io/spring-boot/reference/web/servlet.html) e [configuração de e-mail do Spring Boot](https://docs.spring.io/spring-boot/reference/io/email.html).
