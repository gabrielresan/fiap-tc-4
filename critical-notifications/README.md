# Critical notifications

Módulo Maven independente em Java 21, seguindo a estrutura da branch `feature/weekly-report`, com modelos imutáveis, serviço Java e testes JUnit 5.

## Comportamento

`CriticalNotificationService.notify(Feedback)` envia uma notificação quando a urgência é `CRITICAL` e retorna `true` após o envio concluir. Para `LOW` e `MEDIUM`, não envia e retorna `false`. A classificação de urgência é recebida do serviço de feedback; este módulo não infere urgência pela nota.

O contrato `Feedback` mantém os campos e valores da weekly-report: `description`, `rating` (0 a 10), `urgency` (`LOW`, `MEDIUM`, `CRITICAL`) e `submittedAt` (`Instant`).

O aviso contém a descrição, a urgência, a data original do feedback (`submittedAt`) e o instante da tentativa de envio (`sentAt`), obtido pelo `Clock` injetado. As datas usam `Instant` para preservar o instante independentemente do fuso horário.

## Integração

Construa o serviço com uma implementação de `NotificationSender` e um `Clock`, por exemplo `new CriticalNotificationService(sender, Clock.systemUTC())`. O adaptador de envio deve entregar o aviso aos administradores e lançar uma exceção quando a entrega falhar. O serviço propaga essa exceção para que o chamador possa tratar a falha ou repetir a operação.

O módulo não implementa envio real de e-mail, endpoint HTTP, persistência nem configuração de cloud nesta etapa. A implementação do adaptador, os destinatários e o handler serverless serão definidos na integração com o provedor escolhido pelo grupo. Não há deduplicação ou retentativa interna: cada chamada para feedback crítico aciona o adaptador uma vez.

## Validação

Com JDK 21 e Maven instalados, execute a partir da raiz do repositório:

```sh
mvn -f critical-notifications/pom.xml clean verify
```

Os testes verificam o filtro de urgência, os dados do aviso, a data de envio determinística, a propagação de falhas e as validações dos modelos e dependências.
