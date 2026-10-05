# Estoque Service

Responsável inicial: **Aluno B** (Etapa 1 do roteiro).

Mantém o catálogo e as quantidades em estoque no seu próprio banco (`estoque-db`). É o único
serviço que lê ou altera a tabela `produto`; o Pedido Service só a alcança por este REST.

## Endpoints

| Método | Caminho                      | Sucesso                              | Erros                                               |
|--------|------------------------------|--------------------------------------|-----------------------------------------------------|
| GET    | `/produtos`                  | `200` lista de produtos              | —                                                   |
| GET    | `/produtos/{id}`             | `200` produto                        | `404` produto inexistente                           |
| PUT    | `/produtos/{id}/reservar`    | `200` produto com a quantidade nova  | `404` inexistente, `409` estoque insuficiente, `400` corpo inválido |

Erros seguem o mesmo formato do Pedido Service: `{"mensagem": "..."}`.

Dados iniciais: `1 Notebook 10`, `2 Mouse 50`, `3 Teclado 20`.

```bash
curl localhost:8081/produtos
curl localhost:8081/produtos/1

curl -i -X PUT localhost:8081/produtos/1/reservar \
     -H 'Content-Type: application/json' \
     -d '{"quantidade": 2}'
# HTTP/1.1 200
# {"id":1,"nome":"Notebook","quantidade":8}

curl -i -X PUT localhost:8081/produtos/1/reservar \
     -H 'Content-Type: application/json' \
     -d '{"quantidade": 999}'
# HTTP/1.1 409
# {"mensagem":"Estoque insuficiente"}

curl -i -X PUT localhost:8081/produtos/42/reservar \
     -H 'Content-Type: application/json' \
     -d '{"quantidade": 1}'
# HTTP/1.1 404
# {"mensagem":"Produto inexistente"}
```

A porta `8081` é a publicada pelo Docker Compose; rodando com `spring-boot:run` o serviço escuta
em `8080`.

## Reserva

A reserva é um único `UPDATE ... SET quantidade = quantidade - :n WHERE id = :id AND quantidade >= :n`
(`ProdutoRepository.reservar`). Verificar e baixar no mesmo comando evita que duas reservas
simultâneas deixem o estoque negativo, sem precisar de lock explícito. Se nenhuma linha for afetada,
o serviço consulta se o produto existe para escolher entre `404` e `409`; em ambos os casos o
estoque não é alterado.

## Correlação (Etapa 11)

O cabeçalho opcional `X-Correlation-Id` da reserva é registrado nos logs:

```
correlationId=<id> Produto 1 reservado: quantidade=2 restante=8
```

O Pedido Service deve enviá-lo na chamada `PUT /produtos/{id}/reservar`. Sem o cabeçalho o log sai
com `correlationId=null`.

## Banco de dados

A tabela é criada pelo [`schema.sql`](src/main/resources/schema.sql) (mesmo DDL do roteiro) e
populada pelo [`data.sql`](src/main/resources/data.sql) com `ON CONFLICT DO NOTHING`, então reiniciar
o serviço não repõe o estoque já consumido. O Hibernate não gera DDL (`ddl-auto=none`). A conexão
padrão é `jdbc:postgresql://localhost:5432/estoque` (usuário e senha `estoque`); no Docker Compose
ela é sobrescrita por `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD`.

## Como executar isoladamente

Enquanto o `docker-compose.yml` da Etapa 4 não existe:

```bash
docker run -d --name estoque-db -p 5432:5432 \
  -e POSTGRES_DB=estoque -e POSTGRES_USER=estoque -e POSTGRES_PASSWORD=estoque postgres:16

./mvnw spring-boot:run
```

## Testes

```bash
./mvnw test
```

Os testes sobem a aplicação inteira com o perfil `test`, que troca o PostgreSQL por um H2 em memória
e usa o mesmo `schema.sql` e `data.sql`. Cada teste roda numa transação desfeita ao final, então
todos partem do estoque inicial.

## Ajustes em relação ao roteiro

- A resposta do `404` é `"Produto inexistente"`; o `?` do roteiro foi tratado como erro de digitação.
- `PUT .../reservar` devolve o produto com a quantidade atualizada (o roteiro só exige `200`).
- A API devolve `ProdutoResponse` em vez da entidade JPA.
- `quantidade` ausente, zero ou negativa na reserva responde `400`.
