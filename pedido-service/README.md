# Pedido Service

Responsável inicial: **Aluno A** (Etapa 2 do roteiro); integração com Estoque e RabbitMQ feita
pela dupla (Etapas 3, 5, 11 e 12).

Recebe pedidos de compra e os mantém no seu próprio banco (`pedido-db`). Para criar um pedido,
reserva o estoque no Estoque Service por REST e, depois de gravar o pedido, publica o evento
`pedido.criado` no RabbitMQ. Todo pedido nasce com status `AGUARDANDO_PAGAMENTO`; o status passa a
`PAGO` ou `REJEITADO` quando chega o evento `pagamento.processado` (Etapa 12).

## Endpoints

| Método | Caminho          | Sucesso                                             | Erros |
|--------|------------------|-----------------------------------------------------|-------|
| POST   | `/pedidos`       | `201 Created` + `Location` + `X-Correlation-Id`     | `400` corpo inválido, `404` produto inexistente, `409` estoque insuficiente, `503` Estoque Service fora do ar, `500` falha simulada (Etapa 3) |
| GET    | `/pedidos`       | `200` lista de pedidos                              | —     |
| GET    | `/pedidos/{id}`  | `200` pedido                                        | `404` pedido inexistente |

Erros seguem o mesmo formato do Estoque Service: `{"mensagem": "..."}`. Em qualquer erro do
`POST`, o pedido não é criado e nenhum evento é publicado.

```bash
curl -i -X POST localhost:8080/pedidos \
     -H 'Content-Type: application/json' \
     -d '{"produtoId": 1, "quantidade": 2}'
# HTTP/1.1 201
# Location: /pedidos/1
# X-Correlation-Id: 44978062-a6be-4640-949b-cadc80322629
# {"id":1,"produtoId":1,"quantidade":2,"status":"AGUARDANDO_PAGAMENTO"}

curl localhost:8080/pedidos
curl localhost:8080/pedidos/1
```

## Criação do pedido

`PedidoService.criar` executa, nesta ordem:

1. **Reserva o estoque** com `PUT /produtos/{id}/reservar` (`EstoqueClient`). Se o Estoque
   responde `409` ou `404`, ou não responde, a criação para aqui.
2. **Grava o pedido** com status `AGUARDANDO_PAGAMENTO`. Se a gravação falha, a reserva é desfeita
   com `PUT /produtos/{id}/liberar` (compensação) e o erro é devolvido ao cliente.
3. **Publica `pedido.criado`** em `pedidos.exchange`, com routing key `pedido.criado`.

O método não é `@Transactional`: a reserva acontece em outro serviço e outro banco, então não há
transação única que cubra os dois passos. A consistência vem da compensação.

## Eventos

| Evento                 | Direção  | Exchange             | Fila / routing key     | Conteúdo |
|------------------------|----------|----------------------|------------------------|----------|
| `pedido.criado`        | publica  | `pedidos.exchange`   | `pedido.criado`        | `pedidoId`, `produtoId`, `quantidade`, `correlationId` |
| `pagamento.processado` | consome  | `pagamentos.exchange`| `pagamento.processado` | `pedidoId`, `status` (`APROVADO`/`REJEITADO`), `correlationId` |

O consumo de `pagamento.processado` é idempotente: só um pedido em `AGUARDANDO_PAGAMENTO` muda de
status, então reentregas do mesmo evento não alteram nada.

## Correlação (Etapa 11)

Cada `POST /pedidos` gera um `correlationId` (`UUID`), devolvido no cabeçalho `X-Correlation-Id`,
enviado ao Estoque no mesmo cabeçalho e ao Pagamento no campo `correlationId` do evento:

```
correlationId=<id> Pedido 1 criado: produtoId=1 quantidade=2 status=AGUARDANDO_PAGAMENTO
correlationId=<id> Evento publicado 1
correlationId=<id> Pedido 1 atualizado para PAGO
```

## Configuração

| Propriedade                          | Variável de ambiente                | Padrão                  |
|--------------------------------------|-------------------------------------|-------------------------|
| `estoque.service.url`                | `ESTOQUE_SERVICE_URL`               | `http://localhost:8081` |
| `spring.rabbitmq.host`               | `SPRING_RABBITMQ_HOST`              | `localhost`             |
| `pedido.simular-falha-apos-reserva`  | `PEDIDO_SIMULAR_FALHA_APOS_RESERVA` | `false`                 |
| `pedido.compensacao.habilitada`      | `PEDIDO_COMPENSACAO_HABILITADA`     | `true`                  |

As duas últimas servem ao experimento de consistência da Etapa 3: a primeira provoca uma falha
entre a reserva e a gravação do pedido; a segunda, quando `false`, deixa a reserva sem desfazer.

## Banco de dados

A tabela é criada pelo [`schema.sql`](src/main/resources/schema.sql), com o mesmo DDL do roteiro.
O Hibernate não gera DDL (`ddl-auto=none`). A conexão padrão é
`jdbc:postgresql://localhost:5432/pedido` (usuário e senha `pedido`); no Docker Compose ela é
sobrescrita por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

## Como executar isoladamente

O sistema completo sobe com o `docker-compose.yml` da raiz. Para rodar só o Pedido fora do
Compose, com as dependências em contêineres:

```bash
docker compose up -d rabbitmq estoque-service     # na raiz do repositório
docker run -d --name pedido-db -p 5432:5432 \
  -e POSTGRES_DB=pedido -e POSTGRES_USER=pedido -e POSTGRES_PASSWORD=pedido postgres:16

./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test
```

Os testes sobem a aplicação inteira com o perfil `test`, que troca o PostgreSQL por um H2 em memória
e usa o mesmo `schema.sql`. O `EstoqueClient` e o `RabbitTemplate` são substituídos por mocks e os
listeners do RabbitMQ não iniciam, então não é preciso ter Estoque nem RabbitMQ no ar.

## Limites

- **Falha ao publicar o evento.** Se o RabbitMQ estiver fora do ar no passo 3, o pedido já foi
  gravado e o estoque já foi baixado; o erro só é registrado no log (`Evento NÃO publicado`) e o
  pedido fica em `AGUARDANDO_PAGAMENTO` para sempre. Resolver isso exigiria um *outbox*: gravar o
  evento no `pedido-db` na mesma transação do pedido e publicá-lo depois.
- **Falha na compensação.** Se a chamada a `/liberar` também falhar, a reserva continua sem
  pedido; o log registra `Compensação FALHOU` e não há nova tentativa.
- **Queda do processo.** A compensação roda no mesmo processo; se o Pedido Service cair entre a
  reserva e a gravação, nada desfaz a reserva.

## Ajustes em relação ao roteiro

- `status` é o enum `StatusPedido` (gravado como texto na coluna `VARCHAR(50)`), e não uma `String`
  livre: impede status fora de `AGUARDANDO_PAGAMENTO`, `PAGO` e `REJEITADO`.
- O estado inicial fica no construtor de `Pedido`, então não há como criar pedido sem ele.
- A API devolve `PedidoResponse` em vez da entidade JPA, e `POST` responde `201 Created`.
- O cliente REST usa `RestClient` em vez do `RestTemplate` do exemplo, com timeouts de conexão (3 s)
  e de leitura (5 s).
