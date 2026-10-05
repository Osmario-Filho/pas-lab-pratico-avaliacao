-- Dados iniciais do roteiro. Cada INSERT só grava se o id ainda não existe: reiniciar o serviço
-- não "reabastece" o estoque já consumido, que fica no volume estoque-data.
-- (Forma portável: roda igual no PostgreSQL de produção e no H2 dos testes.)
INSERT INTO produto (id, nome, quantidade)
SELECT v.id, v.nome, v.quantidade
FROM (VALUES (1, 'Notebook', 10), (2, 'Mouse', 50), (3, 'Teclado', 20)) AS v(id, nome, quantidade)
WHERE NOT EXISTS (SELECT 1 FROM produto p WHERE p.id = v.id);
