package br.pas.lab.estoque;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Produto {

    // O id vem do catálogo (dados iniciais); não é gerado pelo banco.
    @Id
    private Long id;

    private String nome;

    private Integer quantidade;

    protected Produto() {
        // exigido pelo JPA
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }
}
