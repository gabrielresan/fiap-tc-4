# Feedback service

API REST independente em Java 21 e Spring Boot 4.1.2 (Spring MVC) que recebe as avaliações dos estudantes, classifica a urgência pela nota, grava no PostgreSQL e encaminha as avaliações críticas ao módulo `critical-notifications`.

O mesmo código roda localmente, sobre o Tomcat embutido, e como função AWS Lambda, atrás do API Gateway (ver [Execução na AWS Lambda](#execução-na-aws-lambda)).

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
| `FeedbackServiceHandler` | Ponto de entrada da Lambda; repassa os eventos do API Gateway à aplicação Spring. |
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
| `NOTIFICATIONS_IAM_REGION` | vazio | Região usada para assinar a chamada à API na AWS. |
| `NOTIFICATIONS_READ_TIMEOUT` | `5s` | Tempo de espera pela resposta da notificação. O SAM configura `12s` para o envio por SES. |
| `URGENCY_CRITICAL_MAX` | `3` | Maior nota considerada `CRITICAL`. |
| `URGENCY_MEDIUM_MAX` | `6` | Maior nota considerada `MEDIUM`. |

Os padrões de banco servem apenas para execução local. Em nuvem, as credenciais devem vir de um gerenciador de segredos. A chamada ao `critical-notifications` tem timeout de conexão de 2 s e de leitura de 5 s.

## Execução local

### Ambiente integrado

Use o [Compose da raiz](../README.md#executar-localmente-com-docker). A API fica em `http://localhost:8080`.

### Execução com Maven

Com JDK 21, Maven e Docker, execute a partir da raiz do repositório.

Suba o PostgreSQL do Compose da raiz e aguarde a inicialização:

```sh
docker compose up -d --wait postgres
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

Para parar o PostgreSQL e preservar os dados:

```sh
docker compose stop postgres
```

Para gerar o JAR executável:

```sh
mvn -f feedback-service/pom.xml clean package
java -jar feedback-service/target/feedback-service-0.0.1-SNAPSHOT.jar
```

## Testes

Na [coleção Postman local](../postman/fiap-tc-4-local.postman_collection.json), execute a pasta **Feedback service** para validar o cadastro, os limites de urgência e as entradas inválidas. A pasta tem 20 requisições e cada execução grava 10 avaliações no banco local.

## Execução na AWS Lambda

A função usa a biblioteca [AWS Serverless Java Container](https://github.com/aws/serverless-java-container) (`aws-serverless-java-container-springboot4`). `FeedbackServiceHandler` inicia a aplicação Spring uma única vez, na fase de inicialização da instância, com o perfil `lambda`. Em seguida, converte cada evento do API Gateway em uma requisição para o mesmo `FeedbackController` usado localmente. Controller, serviço, validação e JPA não mudam.

### Empacotamento

```sh
mvn -f feedback-service/pom.xml clean package
```

Gera dois artefatos em `feedback-service/target/`:

| Artefato | Uso |
| --- | --- |
| `feedback-service-0.0.1-SNAPSHOT.jar` | Execução local com `java -jar` (Tomcat). |
| `feedback-service-lambda.zip` | Pacote da Lambda: classes na raiz e dependências em `lib/`, formato aceito pelo runtime `java21`. |

O zip não inclui o Tomcat, pois na Lambda as requisições chegam pelo container serverless. Com cerca de 45 MB, deve ser enviado via S3, como fazem SAM, CDK e Terraform, pois o limite de upload direto no console é de 50 MB. O módulo `weekly-report` usa JAR único (shade). Aqui foi usado o zip com `lib/` porque o shade sobrescreve os arquivos de registro do Spring em `META-INF` e pode descartar autoconfigurações.

### Configuração da função

| Configuração | Valor |
| --- | --- |
| Runtime | Java 21 |
| Handler | `br.com.fiap.tc4.feedback.FeedbackServiceHandler::handleRequest` |
| Gatilho | API Gateway HTTP API (payload 2.0), rota `POST /avaliacao` |
| Memória | 1024 MB ou mais (mais memória também dá mais CPU e acelera a inicialização) |
| SnapStart | Recomendado: a inicialização do Spring leva de 8 a 10 s sem ele |

Na Lambda, `DB_URL`, `DB_USER`, `DB_PASSWORD` e `NOTIFICATIONS_URL` são obrigatórias. Como no `weekly-report`, a função falha na inicialização se alguma estiver ausente. As demais variáveis de [Configuração](#configuração) continuam opcionais. O deploy injeta a conexão Neon a partir dos secrets do GitHub Actions.

O perfil `lambda` (`application-lambda.properties`):

- Ativa o container serverless no lugar do Tomcat. Fora da Lambda, ele fica desativado por `spring.autoconfigure.exclude` em `application.properties`.
- Limita o pool a uma conexão por instância da Lambda para reduzir o número de conexões ao PostgreSQL.
- Desativa o banner e reduz avisos de log. Os logs vão para o stdout, que a Lambda envia ao CloudWatch Logs.

A função acessa a Neon via TLS e chama a API do `critical-notifications` pela rede padrão do Lambda. Veja a [configuração do deploy](../infra/README.md).

Em produção, após a criação inicial da tabela, recomenda-se `DB_DDL_AUTO=validate`, para evitar a verificação de DDL em cada inicialização a frio.
