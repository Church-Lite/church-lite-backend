ALTER TABLE plan_account ADD COLUMN financial_nature INTEGER;

-- Classificação inicial tolerante para dados históricos. A descrição da raiz
-- determina toda a árvore: menções a receita/revenue/income/entrada recebem
-- REVENUE (ordinal 0); todas as demais árvores recebem EXPENSE (ordinal 1).
WITH RECURSIVE account_tree AS (
    SELECT id, id AS root_id, lower(description) AS root_description
    FROM plan_account
    WHERE parent_code IS NULL

    UNION ALL

    SELECT child.id, tree.root_id, tree.root_description
    FROM plan_account child
    INNER JOIN account_tree tree ON child.parent_code = tree.id
)
UPDATE plan_account account
SET financial_nature = CASE
    WHEN tree.root_description LIKE '%receita%'
      OR tree.root_description LIKE '%revenue%'
      OR tree.root_description LIKE '%income%'
      OR tree.root_description LIKE '%entrada%'
        THEN 0
    ELSE 1
END
FROM account_tree tree
WHERE account.id = tree.id;

-- Protege instalações com registros órfãos ou hierarquias históricas inválidas.
-- Nesses casos usa a própria descrição e assume despesa quando não houver pista.
UPDATE plan_account
SET financial_nature = CASE
    WHEN lower(description) LIKE '%receita%'
      OR lower(description) LIKE '%revenue%'
      OR lower(description) LIKE '%income%'
      OR lower(description) LIKE '%entrada%'
        THEN 0
    ELSE 1
END
WHERE financial_nature IS NULL;

ALTER TABLE plan_account ALTER COLUMN financial_nature SET NOT NULL;
ALTER TABLE plan_account ADD CONSTRAINT ck_plan_account_financial_nature
    CHECK (financial_nature IN (0, 1));
CREATE INDEX idx_plan_account_financial_nature ON plan_account(financial_nature);
