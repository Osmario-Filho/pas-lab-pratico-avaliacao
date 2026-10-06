package br.pas.lab.pedido;

// status: "APROVADO" ou "REJEITADO" (vocabulário do Pagamento Service)
public record PagamentoProcessadoEvento(Long pedidoId, String status, String correlationId) {
}
