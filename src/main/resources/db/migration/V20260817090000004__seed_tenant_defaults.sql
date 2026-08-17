-- Cadastros iniciais de cada igreja. Todos os inserts são idempotentes e
-- preservam registros equivalentes que o tenant já tenha criado.

-- Cargos ministeriais e administrativos.
DO $$
BEGIN
IF to_regclass('position_members') IS NOT NULL THEN
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000001', 'Pastor', 'Responsável pelo pastoreio e direção espiritual'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Pastor'));
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000002', 'Presbítero', 'Responsável pelo apoio pastoral e espiritual'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Presbítero'));
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000003', 'Diácono', 'Responsável pelo serviço e apoio às atividades da igreja'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Diácono'));
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000004', 'Líder', 'Responsável pela liderança de ministério ou grupo'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Líder'));
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000005', 'Secretário', 'Responsável por atividades administrativas e secretaria'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Secretário'));
INSERT INTO position_members (id, name, description)
SELECT 'a1000000-0000-4000-8000-000000000006', 'Tesoureiro', 'Responsável pelo acompanhamento financeiro da igreja'
WHERE NOT EXISTS (SELECT 1 FROM position_members WHERE lower(name) = lower('Tesoureiro'));
END IF;
END $$;

-- Funções que um membro pode exercer.
DO $$
BEGIN
IF to_regclass('member_function') IS NOT NULL THEN
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000001', 'Membro', 'Participante integrado à igreja'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Membro'));
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000002', 'Líder de célula', 'Responsável pela condução de uma célula'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Líder de célula'));
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000003', 'Auxiliar', 'Auxilia lideranças e atividades ministeriais'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Auxiliar'));
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000004', 'Professor', 'Responsável por ensino e formação bíblica'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Professor'));
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000005', 'Músico', 'Participante das atividades de música e louvor'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Músico'));
INSERT INTO member_function (id, name, description)
SELECT 'a2000000-0000-4000-8000-000000000006', 'Voluntário', 'Participante voluntário em atividades da igreja'
WHERE NOT EXISTS (SELECT 1 FROM member_function WHERE lower(name) = lower('Voluntário'));
END IF;
END $$;

-- Tipos de compromisso da agenda.
DO $$
BEGIN
IF to_regclass('events_type') IS NOT NULL THEN
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000001', 'Culto', '#6366F1', 'Cultos e celebrações da igreja'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Culto'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000002', 'Reunião', '#0EA5E9', 'Reuniões administrativas ou ministeriais'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Reunião'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000003', 'Ensaio', '#8B5CF6', 'Ensaios de música, teatro ou apresentações'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Ensaio'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000004', 'Célula', '#10B981', 'Encontros de células e pequenos grupos'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Célula'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000005', 'Escola Bíblica', '#F59E0B', 'Aulas, cursos e formação bíblica'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Escola Bíblica'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000006', 'Conferência', '#EC4899', 'Conferências, congressos e encontros especiais'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Conferência'));
INSERT INTO events_type (id, name, color, description)
SELECT 'a3000000-0000-4000-8000-000000000007', 'Atendimento', '#64748B', 'Atendimentos pastorais e aconselhamentos'
WHERE NOT EXISTS (SELECT 1 FROM events_type WHERE lower(name) = lower('Atendimento'));
END IF;
END $$;

-- Centros de custo iniciais, mantidos sem hierarquia para uso imediato.
DO $$
BEGIN
IF to_regclass('cost_center') IS NOT NULL THEN
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000001', 'Administração', '1', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Administração'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000002', 'Templo', '2', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Templo'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000003', 'Ministério Infantil', '3', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Ministério Infantil'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000004', 'Louvor', '4', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Louvor'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000005', 'Missões', '5', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Missões'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000006', 'Ação Social', '6', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Ação Social'));
INSERT INTO cost_center (id, description, code_tree, parent_code)
SELECT 'a4000000-0000-4000-8000-000000000007', 'Eventos', '7', NULL
WHERE NOT EXISTS (SELECT 1 FROM cost_center WHERE lower(description) = lower('Eventos'));
END IF;
END $$;

-- Plano de contas. TypePlanAccount: ANALYTICAL=0, SYNTHETIC=1.
-- PlanAccountFinancialNature: REVENUE=0, EXPENSE=1.
DO $$
BEGIN
IF to_regclass('plan_account') IS NOT NULL THEN
INSERT INTO plan_account (id, description, code_tree, type, parent_code, financial_nature)
SELECT 'a5000000-0000-4000-8000-000000000001', 'Receitas', '1', 1, NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM plan_account WHERE parent_code IS NULL AND lower(description) = lower('Receitas'));
INSERT INTO plan_account (id, description, code_tree, type, parent_code, financial_nature)
SELECT 'a5000000-0000-4000-8000-000000000002', 'Despesas', '2', 1, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM plan_account WHERE parent_code IS NULL AND lower(description) = lower('Despesas'));

INSERT INTO plan_account (id, description, code_tree, type, parent_code, financial_nature)
SELECT seed.id, seed.description, revenue.code_tree || seed.code_suffix, 0, revenue.id, 0
FROM (VALUES
    ('a5100000-0000-4000-8000-000000000001'::UUID, 'Dízimos', '.1'),
    ('a5100000-0000-4000-8000-000000000002'::UUID, 'Ofertas', '.2'),
    ('a5100000-0000-4000-8000-000000000003'::UUID, 'Doações', '.3'),
    ('a5100000-0000-4000-8000-000000000004'::UUID, 'Eventos', '.4'),
    ('a5100000-0000-4000-8000-000000000005'::UUID, 'Outras receitas', '.5')
) AS seed(id, description, code_suffix)
CROSS JOIN LATERAL (
    SELECT id, code_tree FROM plan_account
    WHERE parent_code IS NULL AND lower(description) = lower('Receitas')
    ORDER BY id LIMIT 1
) revenue
WHERE NOT EXISTS (
    SELECT 1 FROM plan_account existing
    WHERE existing.parent_code = revenue.id AND lower(existing.description) = lower(seed.description)
);

INSERT INTO plan_account (id, description, code_tree, type, parent_code, financial_nature)
SELECT seed.id, seed.description, expense.code_tree || seed.code_suffix, 0, expense.id, 1
FROM (VALUES
    ('a5200000-0000-4000-8000-000000000001'::UUID, 'Água', '.1'),
    ('a5200000-0000-4000-8000-000000000002'::UUID, 'Energia elétrica', '.2'),
    ('a5200000-0000-4000-8000-000000000003'::UUID, 'Internet e telefone', '.3'),
    ('a5200000-0000-4000-8000-000000000004'::UUID, 'Aluguel', '.4'),
    ('a5200000-0000-4000-8000-000000000005'::UUID, 'Manutenção', '.5'),
    ('a5200000-0000-4000-8000-000000000006'::UUID, 'Pessoal', '.6'),
    ('a5200000-0000-4000-8000-000000000007'::UUID, 'Ministério Infantil', '.7'),
    ('a5200000-0000-4000-8000-000000000008'::UUID, 'Louvor', '.8'),
    ('a5200000-0000-4000-8000-000000000009'::UUID, 'Missões', '.9'),
    ('a5200000-0000-4000-8000-000000000010'::UUID, 'Ação social', '.10'),
    ('a5200000-0000-4000-8000-000000000011'::UUID, 'Eventos', '.11'),
    ('a5200000-0000-4000-8000-000000000012'::UUID, 'Outras despesas', '.12')
) AS seed(id, description, code_suffix)
CROSS JOIN LATERAL (
    SELECT id, code_tree FROM plan_account
    WHERE parent_code IS NULL AND lower(description) = lower('Despesas')
    ORDER BY id LIMIT 1
) expense
WHERE NOT EXISTS (
    SELECT 1 FROM plan_account existing
    WHERE existing.parent_code = expense.id AND lower(existing.description) = lower(seed.description)
);
END IF;
END $$;

-- Somente a estrutura do módulo de células; nenhuma célula fictícia é criada.
DO $$
BEGIN
IF to_regclass('cell_organization_level_type') IS NOT NULL THEN
INSERT INTO cell_organization_level_type (id, name, display_order, active)
SELECT 'a6000000-0000-4000-8000-000000000001', 'Rede', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE lower(name) = lower('Rede'))
  AND NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE display_order = 1);
INSERT INTO cell_organization_level_type (id, name, display_order, active)
SELECT 'a6000000-0000-4000-8000-000000000002', 'Supervisão', 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE lower(name) = lower('Supervisão'))
  AND NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE display_order = 2);
INSERT INTO cell_organization_level_type (id, name, display_order, active)
SELECT 'a6000000-0000-4000-8000-000000000003', 'Área', 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE lower(name) = lower('Área'))
  AND NOT EXISTS (SELECT 1 FROM cell_organization_level_type WHERE display_order = 3);
END IF;
END $$;

DO $$
BEGIN
IF to_regclass('cell_module_settings') IS NOT NULL THEN
INSERT INTO cell_module_settings (
    id, cell_term, allow_multiple_cells_per_person, allow_multiple_leadership,
    require_report_approval, require_multiplication_approval,
    allow_visitors_without_person, absence_alert_count,
    visits_for_integration_suggestion, late_report_days, default_capacity
)
SELECT
    'a6000000-0000-4000-8000-000000000010', 'Célula', FALSE, FALSE,
    FALSE, TRUE, TRUE, 3, 3, 3, 15
WHERE NOT EXISTS (SELECT 1 FROM cell_module_settings);
END IF;
END $$;
