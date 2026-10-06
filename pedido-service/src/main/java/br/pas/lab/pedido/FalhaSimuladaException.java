package br.pas.lab.pedido;

// Falha injetada de propósito entre a reserva do estoque e a criação do pedido (experimento de
// consistência da Etapa 3). Só ocorre com pedido.simular-falha-apos-reserva=true.
public class FalhaSimuladaException extends RuntimeException {

    public FalhaSimuladaException() {
        super("Falha simulada após a reserva do estoque");
    }
}
