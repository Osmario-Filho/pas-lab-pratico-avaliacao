# Laboratório avaliativo — Arquitetura de Microsserviços

Laboratório em dupla da disciplina Padrões de Arquitetura de Software (2026/2): plataforma de
comércio eletrônico com três microsserviços, cada um com seu próprio banco, comunicando-se por
REST (Pedido → Estoque) e por RabbitMQ (Pedido → Pagamento → Pedido).

## Estrutura

```
.
├── pedido-service       # Aluno A — Etapa 2
├── estoque-service      # Aluno B — Etapa 1
├── pagamento-service    # dupla — Etapa 6
└── docker-compose.yml   # dupla — Etapa 4
```

Nenhum serviço acessa o banco de outro serviço.

## Andamento

| Etapa | Conteúdo                            | Responsável | Situação  |
|-------|-------------------------------------|-------------|-----------|
| 1     | Estoque Service                     | Aluno B     | concluída |
| 2     | Pedido Service                      | Aluno A     | concluída |
| 3     | Integração REST Pedido → Estoque    | dupla       | pendente  |
| 4     | Docker Compose                      | dupla       | pendente  |
| 5     | RabbitMQ (`pedido.criado`)          | dupla       | pendente  |
| 6     | Pagamento Service                   | dupla       | pendente  |
| 7–10  | Teste funcional, falha, recuperação, escala | dupla | pendente |
| 11    | Observabilidade (`correlationId`)   | dupla       | pendente  |
| 12    | Atualização assíncrona do pedido    | dupla       | pendente  |

## Convenções entre os serviços

- Spring Boot 4.1, Java 17, PostgreSQL 16; pacote `br.pas.lab.<servico>`.
- Cada serviço escuta na porta `8080` dentro do contêiner (o Compose publica o Estoque em `8081`).
- Erros HTTP no formato `{"mensagem": "..."}`.
- Cada serviço tem um `Dockerfile` próprio, usado pelo `build:` do Compose.
