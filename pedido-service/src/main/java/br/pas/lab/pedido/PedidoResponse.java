package br.pas.lab.pedido;

public record PedidoResponse(Long id, Long produtoId, Integer quantidade, StatusPedido status) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(), pedido.getProdutoId(), pedido.getQuantidade(), pedido.getStatus());
    }
}
