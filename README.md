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
├── docker-compose.yml   # dupla — Etapa 4
└── relatorio_entrega.md # dupla — documento de entrega
```

Nenhum serviço acessa o banco de outro serviço.

## Andamento

| Etapa | Conteúdo                            | Responsável | Situação  |
|-------|-------------------------------------|-------------|-----------|
| 1     | Estoque Service                     | Aluno B     | concluída |
| 2     | Pedido Service                      | Aluno A     | concluída |
| 3     | Integração REST Pedido → Estoque (com compensação) | dupla | concluída |
| 4     | Docker Compose                      | dupla       | concluída |
| 5     | RabbitMQ (`pedido.criado`)          | dupla       | concluída |
| 6     | Pagamento Service                   | dupla       | concluída |
| 7–10  | Teste funcional, falha, recuperação, escala | dupla | concluída |
| 11    | Observabilidade (`correlationId`)   | dupla       | concluída |
| 12    | Atualização assíncrona do pedido    | dupla       | concluída |
| —     | Entrega (`relatorio_entrega.md`)    | dupla       | em andamento: faltam os prints (Parte 4) e as respostas da investigação do Pedido #17 (Etapa 11) |

## Como executar

```bash
docker compose up --build
```

| Serviço   | Endereço                                          |
|-----------|---------------------------------------------------|
| Pedido    | `http://localhost:8080/pedidos`                   |
| Estoque   | `http://localhost:8081/produtos`                  |
| RabbitMQ  | `http://localhost:15672` (usuário e senha `guest`) |
| Pagamento | sem porta publicada: só consome e publica eventos |

Variáveis dos experimentos (todas opcionais):

| Variável                            | Padrão  | Etapa | Efeito                                                       |
|-------------------------------------|---------|-------|--------------------------------------------------------------|
| `PEDIDO_SIMULAR_FALHA_APOS_RESERVA` | `false` | 3     | Falha entre reservar o estoque e gravar o pedido (`500`)     |
| `PEDIDO_COMPENSACAO_HABILITADA`     | `true`  | 3     | Com `false`, a reserva não é desfeita após a falha           |
| `PAGAMENTO_ATRASO_MS`               | `0`     | 10    | Atraso por pagamento, para ver as instâncias se alternarem   |

```bash
# Etapa 3: falha após a reserva, sem compensação
PEDIDO_SIMULAR_FALHA_APOS_RESERVA=true PEDIDO_COMPENSACAO_HABILITADA=false docker compose up -d pedido-service

# Etapas 8 e 9: Pagamento fora do ar e de volta
docker compose stop pagamento-service
docker compose start pagamento-service

# Etapa 10: duas instâncias de Pagamento. Sem atraso, cada pagamento termina em milissegundos
# e a divisão entre as instâncias pode sair bem desigual; com 1 s elas se alternam.
PAGAMENTO_ATRASO_MS=1000 docker compose up -d --scale pagamento-service=2
```

## Convenções entre os serviços

- Spring Boot 4.1, Java 17, PostgreSQL 16; pacote `br.pas.lab.<servico>`.
- Cada serviço escuta na porta `8080` dentro do contêiner (o Compose publica o Pedido em `8080` e
  o Estoque em `8081`).
- Erros HTTP no formato `{"mensagem": "..."}`.
- O Pedido Service gera um `correlationId` por requisição e o propaga no cabeçalho
  `X-Correlation-Id` (REST) e no campo `correlationId` dos eventos; todos os logs o registram.
- Cada serviço tem um `Dockerfile` próprio, usado pelo `build:` do Compose.
