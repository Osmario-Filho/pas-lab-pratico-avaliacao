package br.pas.lab.pagamento;

// Evento publicado em pagamento.processado; status: "APROVADO" ou "REJEITADO".
public record PagamentoProcessadoEvento(Long pedidoId, String status, String correlationId) {
}
