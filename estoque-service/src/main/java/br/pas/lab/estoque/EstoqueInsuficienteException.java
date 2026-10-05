package br.pas.lab.estoque;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException() {
        super("Estoque insuficiente");
    }
}
