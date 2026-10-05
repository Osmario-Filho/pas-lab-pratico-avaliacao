# Pedido Service

Responsável inicial: **Aluno A** (Etapa 2 do roteiro).

Recebe pedidos de compra e os mantém no seu próprio banco (`pedido-db`). Todo pedido nasce com
status `AGUARDANDO_PAGAMENTO`; os status `PAGO` e `REJEITADO` serão atribuídos a partir do evento
`pagamento.processado` (Etapa 12).

## Endpoints

| Método | Caminho          | Sucesso                         | Erros                         |
|--------|------------------|---------------------------------|-------------------------------|
| POST   | `/pedidos`       | `201 Created` + `Location`      | `400` corpo inválido          |
| GET    | `/pedidos`       | `200` lista de pedidos          | —                             |
| GET    | `/pedidos/{id}`  | `200` pedido                    | `404` pedido inexistente      |

Erros seguem o mesmo formato do Estoque Service: `{"mensagem": "..."}`.

```bash
curl -i -X POST localhost:8080/pedidos \
     -H 'Content-Type: application/json' \
     -d '{"produtoId": 1, "quantidade": 2}'
# HTTP/1.1 201
# Location: /pedidos/1
# {"id":1,"produtoId":1,"quantidade":2,"status":"AGUARDANDO_PAGAMENTO"}

curl localhost:8080/pedidos
curl localhost:8080/pedidos/1
```

## Banco de dados

A tabela é criada pelo [`schema.sql`](src/main/resources/schema.sql), com o mesmo DDL do roteiro.
O Hibernate não gera DDL (`ddl-auto=none`). A conexão padrão é
`jdbc:postgresql://localhost:5432/pedido` (usuário e senha `pedido`); no Docker Compose ela é
sobrescrita por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`.

## Como executar isoladamente

Enquanto o `docker-compose.yml` da Etapa 4 não existe:

```bash
docker run -d --name pedido-db -p 5432:5432 \
  -e POSTGRES_DB=pedido -e POSTGRES_USER=pedido -e POSTGRES_PASSWORD=pedido postgres:16

./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test
```

Os testes sobem a aplicação inteira com o perfil `test`, que troca o PostgreSQL por um H2 em memória
e usa o mesmo `schema.sql`.

## Ajustes em relação ao roteiro

- `status` é o enum `StatusPedido` (gravado como texto na coluna `VARCHAR(50)`), e não uma `String`
  livre: impede status fora de `AGUARDANDO_PAGAMENTO`, `PAGO` e `REJEITADO`.
- O estado inicial fica no construtor de `Pedido`, então não há como criar pedido sem ele.
- A API devolve `PedidoResponse` em vez da entidade JPA, e `POST` responde `201 Created`.
