# fiap-tc-4

## Executar o TC localmente com Docker

Com o Docker e o Compose em execução, rode na raiz do repositório:

```sh
docker compose up --build -d --wait
```

| Serviço | Endereço local | Uso |
| --- | --- | --- |
| `feedback-service` | `http://localhost:8080` | Receber avaliações com `POST /avaliacao`. |
| `critical-notifications` | `http://localhost:8081` | Registrar avisos críticos via `POST /notifications`. |
| `weekly-report` | `http://localhost:8082` | Invocar o handler pelo emulador local da Lambda. |
| Mailpit | `http://localhost:8025` | Conferir a captura local dos e-mails do relatório. |

Os serviços se comunicam pela rede do Compose. A ingestão e o relatório usam o mesmo PostgreSQL, com volume persistente. Na primeira inicialização, o banco cria a tabela e carrega avaliações de exemplo da semana anterior para a demonstração do relatório. As credenciais do Compose são para uso local.

Para testar a integração pelo PowerShell, envie uma avaliação crítica e confira o aviso:

```powershell
$body = @{ descricao = 'Video da aula indisponivel'; nota = 2 } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/avaliacao' -ContentType 'application/json' -Body $body
docker compose logs critical-notifications
```

Para executar o relatório da semana anterior, faça uma invocação manual. O e-mail fica disponível no Mailpit, que captura a mensagem localmente.

```powershell
Invoke-RestMethod -Method Post -Uri 'http://localhost:8082/2015-03-31/functions/function/invocations' -ContentType 'application/json' -Body '{}'
```

O endpoint do relatório é fornecido pelo [emulador da imagem Java da AWS Lambda](https://docs.aws.amazon.com/lambda/latest/dg/java-image.html). Os pacotes de deploy da Lambda são gerados separadamente pelos builds Maven dos módulos.

Para parar os serviços e preservar o banco:

```sh
docker compose down
```
