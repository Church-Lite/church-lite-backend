CREATE OR REPLACE FUNCTION calculate_balance()
RETURNS TRIGGER AS $$
DECLARE
sumRevenues numeric(18,2);
    sumExpenses numeric(18,2);
    _transaction uuid;
BEGIN

    IF TG_OP = 'INSERT' THEN
        _transaction := new.cash_transaction;
    ELSIF TG_OP = 'UPDATE' THEN
        _transaction := new.cash_transaction;
    ELSIF TG_OP = 'DELETE' THEN
        _transaction := old.cash_transaction;
END IF;

    sumRevenues := (select sum(transactions.value) as valor
                        from transactions
                            inner join financial on financial.id = transactions.financial
                        WHERe transactions.cash_transaction = _transaction and type_financial = '0');

    RAISE NOTICE 'receitas %', sumRevenues;

    sumExpenses := (select sum(transactions.value) as valor
                    from transactions
                        inner join financial on financial.id = transactions.financial
                    WHERe transactions.cash_transaction = _transaction and type_financial = '1');


update cash_transactions set balance = (sumRevenues - sumExpenses) where id = _transaction;

RETURN NULL;
END;
$$ LANGUAGE plpgsql;



CREATE OR REPLACE TRIGGER trg_transaction_balance
AFTER INSERT OR UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION calculate_balance();