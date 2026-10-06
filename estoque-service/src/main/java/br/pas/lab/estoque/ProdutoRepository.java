package br.pas.lab.estoque;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findAllByOrderByIdAsc();

    // Baixa atômica: a verificação "há quantidade suficiente" e a subtração acontecem no mesmo
    // UPDATE, então duas reservas simultâneas nunca deixam o estoque negativo.
    // Retorna 1 se reservou e 0 se o produto não existe ou não tem quantidade suficiente.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Produto p set p.quantidade = p.quantidade - :quantidade "
            + "where p.id = :id and p.quantidade >= :quantidade")
    int reservar(@Param("id") Long id, @Param("quantidade") Integer quantidade);

    // Compensação da reserva: devolve as unidades ao estoque. Retorna 0 se o produto não existe.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Produto p set p.quantidade = p.quantidade + :quantidade where p.id = :id")
    int liberar(@Param("id") Long id, @Param("quantidade") Integer quantidade);
}
