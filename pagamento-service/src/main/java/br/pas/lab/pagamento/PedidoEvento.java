package br.pas.lab.pagamento;

// Evento consumido de pedido.criado (publicado pelo Pedido Service).
public record PedidoEvento(Long pedidoId, Long produtoId, Integer quantidade, String correlationId) {
}
