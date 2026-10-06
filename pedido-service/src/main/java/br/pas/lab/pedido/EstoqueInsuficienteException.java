package br.pas.lab.pedido;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException() {
        super("Estoque insuficiente");
    }
}
