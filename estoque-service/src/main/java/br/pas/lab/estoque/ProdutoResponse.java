package br.pas.lab.estoque;

public record ProdutoResponse(Long id, String nome, Integer quantidade) {

    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(produto.getId(), produto.getNome(), produto.getQuantidade());
    }
}
