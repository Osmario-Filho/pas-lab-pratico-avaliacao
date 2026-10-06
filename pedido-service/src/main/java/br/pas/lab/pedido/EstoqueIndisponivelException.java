package br.pas.lab.pedido;

public class EstoqueIndisponivelException extends RuntimeException {

    public EstoqueIndisponivelException(Throwable causa) {
        super("Estoque Service indisponível", causa);
    }
}
