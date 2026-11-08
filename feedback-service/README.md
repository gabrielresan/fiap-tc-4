# Feedback service

API REST independente em Java 21 e Spring Boot 4.1.2 (Spring MVC) que recebe as avaliações dos estudantes, classifica a urgência pela nota, grava no PostgreSQL e encaminha as avaliações críticas ao módulo `critical-notifications`.

É o único serviço que **escreve** na tabela `feedbacks`; o `weekly-report` apenas lê.

## Endpoint

`POST /avaliacao`, com `Content-Type: application/json`:

```json
{
  "descricao": "Vídeo da aula não carrega",
  "nota": 2
}
```

A rota usa `avaliacao` sem acento, para evitar codificação de URL. Os dois campos são obrigatórios:

- `descricao`: texto não vazio, com até 2000 caracteres.
- `nota`: inteiro de 0 a 10. Valores decimais, como `7.5`, são rejeitados.

| HTTP | Resposta | Significado |
| --- | --- | --- |
| 201 | JSON da avaliação gravada | Avaliação registrada. |
| 400 | `application/problem+json` | JSON inválido ou campos inválidos/ausentes. |

Exemplo de resposta 201:

```json
{
  "id": "6c1f1d0e-4b7a-4f4e-9a52-2f0d8e1b7c3a",
  "descricao": "Vídeo da aula não carrega",
  "nota": 2,
  "urgencia": "CRITICAL",
  "dataEnvio": "2026-11-08T14:03:12.123456Z"
}
```

A data de envio é definida pelo servidor, em UTC, no momento do recebimento. O cliente não a informa.

## Classificação de urgência

| Nota | Urgência |
| --- | --- |
| 0 a 3 | `CRITICAL` |
| 4 a 6 | `MEDIUM` |
| 7 a 10 | `LOW` |

Os limites são configuráveis por `URGENCY_CRITICAL_MAX` e `URGENCY_MEDIUM_MAX`. A aplicação não inicia se `0 <= critical-max < medium-max <= 10` não for respeitado.

## Fluxo

1. Valida o corpo da requisição.
2. `UrgencyClassifier` define a urgência pela nota.
3. `FeedbackService` gera o `id`, define `dataEnvio` e grava a avaliação na tabela `feedbacks`.
4. Se a urgência for `CRITICAL`, `HttpCriticalNotifier` chama `POST /notifications` do `critical-notifications` com `description`, `rating`, `urgency` e `submittedAt`.

A avaliação é gravada **antes** da notificação. Se o `critical-notifications` estiver indisponível ou responder com erro, a avaliação continua gravada e o estudante recebe 201. A falha é registrada no log em nível ERROR (`critical_notification_failed id=...`), para que o monitoramento gere alertas. Não há retentativa automática.

## Responsabilidades

| Classe | Responsabilidade |
| --- | --- |
| `FeedbackController` | Recebe `POST /avaliacao` e converte entrada e saída. |
| `ApiExceptionHandler` | Responde 400 em `problem+json` para entradas inválidas. |
| `FeedbackService` | Orquestra classificação, gravação e notificação. |
| `UrgencyClassifier` | Converte a nota em urgência, segundo `UrgencyThresholds`. |
| `FeedbackRepository` / `FeedbackEntity` | Grava na tabela `feedbacks` com Spring Data JPA. |
| `HttpCriticalNotifier` | Chama o `critical-notifications` via `RestClient`, com timeouts. |

A notificação fica atrás da interface `CriticalNotifier`. Assim, a chamada HTTP direta pode ser trocada por uma fila (por exemplo, SNS/SQS) sem alterar o serviço.

## Banco de dados

O esquema pertence a este serviço e é gerado pelo Hibernate (JPA) a partir de `FeedbackEntity`. As restrições `CHECK`, o índice e os valores padrão estão declarados na entidade, de modo que a tabela gerada é a mesma que o `weekly-report` espera:

| Coluna | Tipo | Observação |
| --- | --- | --- |
| `id` | `UUID` | Chave primária, gerada pela aplicação. |
| `description` | `TEXT` | Obrigatória. |
| `rating` | `SMALLINT` | De 0 a 10. |
| `urgency` | `VARCHAR(10)` | `LOW`, `MEDIUM` ou `CRITICAL`. |
| `submitted_at` | `TIMESTAMPTZ` | Data de envio; indexada. |

O Hibernate usa `ddl-auto=update`. Na primeira inicialização, cria a tabela e o índice. Nas seguintes, não executa DDL se a tabela já estiver correta. O modo `update` nunca remove nem altera colunas existentes: mudanças de tipo ou de restrição em uma tabela já criada precisam ser aplicadas manualmente no banco. Após a criação inicial, é possível usar `DB_DDL_AUTO=validate`, para que a aplicação apenas confira o esquema.

## Configuração

| Variável | Padrão | Uso |
| --- | --- | --- |
| `PORT` | `8080` | Porta HTTP. |
| `DB_URL` | `jdbc:postgresql://localhost:55433/feedbacks` | URL JDBC do PostgreSQL. |
| `DB_USER` | `feedback_service` | Usuário do banco (precisa de permissão de escrita e, com `update`, de criação de tabelas). |
| `DB_PASSWORD` | `feedback_service` | Senha do banco. |
| `DB_DDL_AUTO` | `update` | Modo de geração do esquema do Hibernate (`update` ou `validate`). |
| `NOTIFICATIONS_URL` | `http://localhost:8081` | URL base do `critical-notifications`. |
| `URGENCY_CRITICAL_MAX` | `3` | Maior nota considerada `CRITICAL`. |
| `URGENCY_MEDIUM_MAX` | `6` | Maior nota considerada `MEDIUM`. |

Os padrões de banco servem apenas para execução local. Em nuvem, as credenciais devem vir de um gerenciador de segredos. A chamada ao `critical-notifications` tem timeout de conexão de 2 s e de leitura de 5 s.

## Execução local

Com JDK 21, Maven e Docker, execute a partir da raiz do repositório.

Suba o PostgreSQL (porta `55433`):

```sh
docker compose -f feedback-service/docker-compose.yml up -d
```

Inicie o `critical-notifications` (porta `8081`), em outro terminal:

```sh
mvn -f critical-notifications/pom.xml spring-boot:run
```

Inicie este serviço (porta `8080`):

```sh
mvn -f feedback-service/pom.xml spring-boot:run
```

Envie uma avaliação pelo PowerShell:

```powershell
$body = @{ descricao = 'Vídeo da aula não carrega'; nota = 2 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/avaliacao' -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($body))
```

O aviso `critical_notification` aparece no console do `critical-notifications`.

### Coleção Postman

A coleção `postman/feedback-service.postman_collection.json` tem 20 requisições com testes automáticos, em três pastas:

1. **Avaliações válidas**: CRITICAL, MEDIUM, LOW e descrição com 2000 caracteres. Verifica status 201, `id`, eco dos campos, `dataEnvio` e urgência.
2. **Limites de urgência**: notas 0, 3, 4, 6, 7 e 10.
3. **Validação (400)**: descrição em branco, ausente ou com 2001 caracteres; nota ausente, nula, acima de 10, negativa ou decimal; JSON malformado; corpo vazio.

Importe o arquivo no Postman, ajuste a variável de coleção `baseUrl` (padrão `http://localhost:8080`) e execute pelo Collection Runner. Pela linha de comando, com Node.js:

```sh
npx newman run feedback-service/postman/feedback-service.postman_collection.json --env-var baseUrl=http://localhost:8080
```

Cada execução grava 10 avaliações novas no banco.

Para encerrar e apagar os dados:

```sh
docker compose -f feedback-service/docker-compose.yml down -v
```

Para gerar o JAR executável:

```sh
mvn -f feedback-service/pom.xml clean package
java -jar feedback-service/target/feedback-service-0.0.1-SNAPSHOT.jar
```

## Integração com o Tech Challenge

O PostgreSQL local deste módulo é separado do PostgreSQL do `weekly-report`, que cria o esquema por script próprio. Em nuvem, os dois serviços devem apontar para o mesmo banco. O `feedback-service` cria o esquema via JPA, e o `weekly-report` usa um usuário somente leitura.

Ainda pendentes: empacotamento serverless (por exemplo, AWS Lambda com AWS Serverless Java Container, ou contêiner), provisionamento na AWS, gerenciamento de segredos, deploy automatizado e monitoramento. O endpoint atual não implementa autenticação nem limite de requisições.
