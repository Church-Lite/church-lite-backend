-- Massa de dados para teste de carga do Church Lite
-- PostgreSQL 14+; execute depois de todas as migrations do Flyway.
-- O script e deterministico e pode ser executado novamente: as PKs geradas sao estaveis.
-- Volume aproximado: 8.000 pessoas, 1.110 contas, 1.110 centros de custo,
-- 50.000 titulos financeiros, 35.000 transacoes, 1.680 fechamentos de caixa
-- e 20.000 agendamentos (alem dos cadastros auxiliares).

BEGIN;

-- ---------------------------------------------------------------------------
-- Usuarios, configuracoes e tipos de evento
-- ---------------------------------------------------------------------------
INSERT INTO user_access (id, name, email, password, tenant, active, user_confirm, phone)
SELECT md5('stress-access-' || g)::uuid,
       'Usuario de Carga ' || lpad(g::text, 3, '0'),
       'carga.usuario' || g || '@example.test',
       '$2a$10$7EqJtq98hPqEX7fNZaFWoO5YQhY5hGQYwTq9qQj7f5X1J5rM5vE2K',
       'stress-test', true, true,
       '+55 11 9' || lpad(g::text, 8, '0')
  FROM generate_series(1, 100) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_configuration (id, name, theme, lang, email, hash, phone)
SELECT md5('stress-user-' || g)::uuid,
       'Operador de Carga ' || lpad(g::text, 3, '0'),
       g % 2, g % 3,
       'carga.usuario' || g || '@example.test',
       md5('stress-access-' || g)::uuid,
       '+55 11 9' || lpad(g::text, 8, '0')
  FROM generate_series(1, 100) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO events_type (id, name, color, description)
SELECT md5('stress-event-type-' || g)::uuid,
       (ARRAY['Culto','Reuniao','Ensaio','Aconselhamento','Visita','Curso','Conferencia','Batismo','Casamento','Acao social'])[g],
       (ARRAY['#2563EB','#16A34A','#9333EA','#EA580C','#0891B2','#4F46E5','#DC2626','#0D9488','#DB2777','#65A30D'])[g],
       'Tipo de evento para teste de carga ' || g
  FROM generate_series(1, 10) g
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Pessoas e dados relacionados
-- ---------------------------------------------------------------------------
INSERT INTO person (id, name, status, type, gender, nationality, marital_status, birthplace, image)
SELECT md5('stress-person-' || g)::uuid,
       (ARRAY['Ana','Bruno','Carla','Daniel','Elisa','Felipe','Gabriela','Henrique','Isabela','Joao',
              'Karen','Lucas','Mariana','Nicolas','Olivia','Paulo','Renata','Samuel','Talita','Vinicius'])[(g - 1) % 20 + 1]
       || ' Teste ' || lpad(g::text, 5, '0'),
       CASE WHEN g % 17 = 0 THEN 1 ELSE 0 END,
       g % 6, g % 2,
       '901C6B60-CEF5-4266-8DB5-40132E1313A0'::uuid,
       g % 6,
       (ARRAY['Sao Paulo - SP','Campinas - SP','Curitiba - PR','Goiania - GO','Belo Horizonte - MG'])[(g - 1) % 5 + 1],
       NULL
  FROM generate_series(1, 8000) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO person_docs (id, person, cpf, rg, cnpj, birth_date, type_person, ie)
SELECT md5('stress-doc-' || g)::uuid,
       md5('stress-person-' || g)::uuid,
       lpad((10000000000::bigint + g)::text, 11, '0'),
       'RG' || lpad(g::text, 9, '0'),
       CASE WHEN g % 20 = 0 THEN lpad((10000000000000::bigint + g)::text, 14, '0') END,
       current_date - (6570 + (g * 37) % 20000),
       CASE WHEN g % 20 = 0 THEN 1 ELSE 0 END,
       CASE WHEN g % 20 = 0 THEN 'IE' || lpad(g::text, 10, '0') END
  FROM generate_series(1, 8000) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO person_email (id, person, email)
SELECT md5('stress-email-' || g)::uuid, md5('stress-person-' || g)::uuid,
       'pessoa.teste' || g || '@example.test'
  FROM generate_series(1, 8000) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO person_telphone (id, person, phone, cell_phone)
SELECT md5('stress-phone-' || g)::uuid, md5('stress-person-' || g)::uuid,
       '11' || lpad((30000000 + g)::text, 8, '0'),
       '11' || lpad((900000000 + g)::text, 9, '0')
  FROM generate_series(1, 8000) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO position_members (id, name, description)
SELECT md5('stress-position-' || g)::uuid,
       'Cargo ministerial ' || lpad(g::text, 2, '0'),
       'Cargo gerado para teste de carga'
  FROM generate_series(1, 30) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO person_member (id, person, entry_date, date_baptism, position)
SELECT md5('stress-member-' || g)::uuid, md5('stress-person-' || g)::uuid,
       current_date - (g % 3650),
       current_date - (g % 3000),
       md5('stress-position-' || ((g - 1) % 30 + 1))::uuid
  FROM generate_series(1, 5000) g
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Plano de contas: 10 raizes, 100 filhos e 1.000 folhas.
-- type: 0=ANALYTICAL (folha), 1=SYNTHETIC (agrupador).
-- ---------------------------------------------------------------------------
INSERT INTO plan_account (id, description, code_tree, type, parent_code)
SELECT md5('stress-plan-' || r)::uuid,
       CASE WHEN r <= 5 THEN 'Receitas - grupo ' ELSE 'Despesas - grupo ' END || r,
       r::text, 1, NULL
  FROM generate_series(1, 10) r
ON CONFLICT (id) DO NOTHING;

INSERT INTO plan_account (id, description, code_tree, type, parent_code)
SELECT md5('stress-plan-' || r || '.' || c)::uuid,
       'Subgrupo contabil ' || r || '.' || c,
       r || '.' || c, 1,
       md5('stress-plan-' || r)::uuid
  FROM generate_series(1, 10) r
 CROSS JOIN generate_series(1, 10) c
ON CONFLICT (id) DO NOTHING;

INSERT INTO plan_account (id, description, code_tree, type, parent_code)
SELECT md5('stress-plan-' || r || '.' || c || '.' || a)::uuid,
       CASE WHEN r <= 5 THEN 'Receita analitica ' ELSE 'Despesa analitica ' END || r || '.' || c || '.' || a,
       r || '.' || c || '.' || a, 0,
       md5('stress-plan-' || r || '.' || c)::uuid
  FROM generate_series(1, 10) r
 CROSS JOIN generate_series(1, 10) c
 CROSS JOIN generate_series(1, 10) a
ON CONFLICT (id) DO NOTHING;

-- Centros de custo com a mesma arvore 10 x 10 x 10.
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT md5('stress-cost-' || r)::uuid, 'Area organizacional ' || r, r::text, NULL
  FROM generate_series(1, 10) r
ON CONFLICT (id) DO NOTHING;

INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT md5('stress-cost-' || r || '.' || c)::uuid,
       'Ministerio/Departamento ' || r || '.' || c, r || '.' || c,
       md5('stress-cost-' || r)::uuid
  FROM generate_series(1, 10) r
 CROSS JOIN generate_series(1, 10) c
ON CONFLICT (id) DO NOTHING;

INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT md5('stress-cost-' || r || '.' || c || '.' || a)::uuid,
       'Projeto/Unidade ' || r || '.' || c || '.' || a, r || '.' || c || '.' || a,
       md5('stress-cost-' || r || '.' || c)::uuid
  FROM generate_series(1, 10) r
 CROSS JOIN generate_series(1, 10) c
 CROSS JOIN generate_series(1, 10) a
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- Caixas, contas bancarias e periodos mensais (7 anos, incluindo o atual).
-- ---------------------------------------------------------------------------
INSERT INTO cash (id, description, type_cash, bank, number_account, digit, status)
SELECT md5('stress-cash-' || g)::uuid,
       CASE WHEN g <= 4 THEN 'Caixa fisico ' ELSE 'Conta bancaria ' END || lpad(g::text, 2, '0'),
       CASE WHEN g <= 4 THEN 0 ELSE 1 END,
       CASE WHEN g <= 4 THEN '07B0887E-E8E2-434E-9DC7-C1108213C474'::uuid
            ELSE (ARRAY['5E0183F2-1086-4D9D-A4D4-EEB1F4AB0672'::uuid,
                        '02D55FCB-E5FE-4681-ABA5-CC889FB88F5C'::uuid,
                        '2E096F2C-5026-4DF7-883D-C83E59B11720'::uuid,
                        'A0569A5C-E8B8-4483-AD8B-11AD8CB520CD'::uuid])[(g - 5) % 4 + 1] END,
       lpad((100000 + g)::text, 8, '0'), (g % 10)::text,
       CASE WHEN g = 20 THEN 1 ELSE 0 END
  FROM generate_series(1, 20) g
ON CONFLICT (id) DO NOTHING;

INSERT INTO cash_transactions (id, start_date, balance, initial_balance, final_balance, cash, end_date)
SELECT md5('stress-cash-period-' || c || '-' || to_char(date_trunc('month', current_date) - (m || ' months')::interval, 'YYYY-MM'))::uuid,
       (date_trunc('month', current_date) - (m || ' months')::interval)::date,
       0, round((1000 + c * 250 + m * 3.75)::numeric, 2), 0,
       md5('stress-cash-' || c)::uuid,
       CASE WHEN m = 0 THEN NULL
            ELSE (date_trunc('month', current_date) - (m || ' months')::interval + interval '1 month - 1 day')::date END
  FROM generate_series(1, 20) c
 CROSS JOIN generate_series(0, 83) m
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 50 mil contas a pagar/receber ao longo de 6 anos.
-- 70% sao liquidadas; 30% ficam pendentes (incluindo vencidas e futuras).
-- ---------------------------------------------------------------------------
WITH source AS (
    SELECT g,
           current_date - ((g * 17) % 2190) AS issue_date,
           ((g - 1) % 20 + 1) AS cash_no,
           ((g - 1) % 10 + 1) AS root_no,
           (((g - 1) / 10) % 10 + 1) AS child_no,
           (((g - 1) / 100) % 10 + 1) AS leaf_no
      FROM generate_series(1, 50000) g
)
INSERT INTO financial (id, description, type_financial, cash, value, person,
                       plan_account, cost_center, issue_date, due_date, payment_receipt_date)
SELECT md5('stress-financial-' || g)::uuid,
       CASE WHEN root_no <= 5 THEN 'Recebimento de carga ' ELSE 'Pagamento de carga ' END || lpad(g::text, 6, '0'),
       CASE WHEN root_no <= 5 THEN 0 ELSE 1 END,
       md5('stress-cash-' || cash_no)::uuid,
       round((25 + ((g * 7919) % 250000) / 100.0)::numeric, 2),
       md5('stress-person-' || ((g - 1) % 8000 + 1))::uuid,
       md5('stress-plan-' || root_no || '.' || child_no || '.' || leaf_no)::uuid,
       md5('stress-cost-' || root_no || '.' || child_no || '.' || leaf_no)::uuid,
       issue_date, issue_date + (7 + g % 54),
       CASE WHEN ((g - 1) / 10) % 10 < 7
            THEN least(current_date, issue_date + (3 + g % 40)) ELSE NULL END
  FROM source
ON CONFLICT (id) DO NOTHING;

-- Uma transacao para cada titulo liquidado, vinculada ao periodo mensal correto.
-- A trigger original agrega a tabela a cada linha; nesta carga ela e substituida pela
-- consolidacao em lote logo abaixo e reativada antes do COMMIT.
ALTER TABLE transactions DISABLE TRIGGER USER;

WITH source AS (
    SELECT g,
           current_date - ((g * 17) % 2190) AS issue_date,
           ((g - 1) % 20 + 1) AS cash_no,
           ((g - 1) % 10 + 1) AS root_no
      FROM generate_series(1, 50000) g
     WHERE ((g - 1) / 10) % 10 < 7
), paid AS (
    SELECT *, least(current_date, issue_date + (3 + g % 40)) AS paid_date FROM source
)
INSERT INTO transactions (id, description, value, transaction_operation, person,
                          financial, date_transaction, cash_transaction)
SELECT md5('stress-transaction-' || g)::uuid,
       'Baixa automatica de carga ' || lpad(g::text, 6, '0'),
       round((25 + ((g * 7919) % 250000) / 100.0)::numeric, 2),
       CASE WHEN root_no <= 5 THEN 2 ELSE 3 END,
       md5('stress-person-' || ((g - 1) % 8000 + 1))::uuid,
       md5('stress-financial-' || g)::uuid,
       paid_date,
       md5('stress-cash-period-' || cash_no || '-' || to_char(date_trunc('month', paid_date), 'YYYY-MM'))::uuid
  FROM paid
ON CONFLICT (id) DO NOTHING;

ALTER TABLE transactions ENABLE TRIGGER USER;

-- Consolida saldos/finais, inclusive numa reexecucao em que os triggers nao disparam.
WITH totals AS (
    SELECT ct.id,
           COALESCE(sum(CASE WHEN f.type_financial = 0 THEN t.value ELSE -t.value END), 0) AS movement
      FROM cash_transactions ct
      LEFT JOIN transactions t ON t.cash_transaction = ct.id
      LEFT JOIN financial f ON f.id = t.financial
     WHERE ct.id IN (
         SELECT md5('stress-cash-period-' || c || '-' || to_char(date_trunc('month', current_date) - (m || ' months')::interval, 'YYYY-MM'))::uuid
           FROM generate_series(1, 20) c CROSS JOIN generate_series(0, 83) m
     )
     GROUP BY ct.id
)
UPDATE cash_transactions ct
   SET balance = totals.movement,
       final_balance = ct.initial_balance + totals.movement
  FROM totals
 WHERE ct.id = totals.id;

-- ---------------------------------------------------------------------------
-- 20 mil compromissos: historicos, atuais, futuros, cancelados e recorrentes.
-- ---------------------------------------------------------------------------
WITH source AS (
    SELECT g,
           date_trunc('day', current_timestamp)
             + (((g * 37) % 1460) - 730) * interval '1 day'
             + (8 + g % 12) * interval '1 hour' AS starts_at
      FROM generate_series(1, 20000) g
)
INSERT INTO appointments (id, events_type, user_configuration, initial_date, final_date,
                          local, description, status, recurrence_type, recurrence_days,
                          recurrence_end_date, recurrence_group_id)
SELECT md5('stress-appointment-' || g)::uuid,
       md5('stress-event-type-' || ((g - 1) % 10 + 1))::uuid,
       md5('stress-user-' || ((g - 1) % 100 + 1))::uuid,
       starts_at, starts_at + (30 + (g % 7) * 15) * interval '1 minute',
       (ARRAY['Templo principal','Sala 1','Sala 2','Auditorio','Online','Residencia','Quadra'])[(g - 1) % 7 + 1],
       'Agendamento para teste de carga numero ' || lpad(g::text, 6, '0'),
       CASE WHEN g % 13 = 0 THEN 1 ELSE 0 END,
       CASE WHEN g % 5 = 0 THEN 1 ELSE 0 END,
       CASE WHEN g % 5 = 0 THEN (ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'])[(g - 1) % 7 + 1] END,
       CASE WHEN g % 5 = 0 THEN starts_at::date + 180 ELSE NULL END,
       CASE WHEN g % 5 = 0 THEN md5('stress-recurrence-' || ((g - 1) / 10))::uuid ELSE NULL END
  FROM source
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Contagens esperadas da massa (na primeira execucao em banco sem outra massa):
-- person=8000 | plan_account=1110 | cost_center=1110 | financial=50000
-- transactions=35000 | cash_transactions=1680 | appointments=20000
