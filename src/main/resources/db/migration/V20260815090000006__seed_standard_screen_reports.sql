DO $$
BEGIN
    IF upper(current_schema()) NOT LIKE '%\_ADMIN' ESCAPE '\' THEN
        INSERT INTO screen_report (
            id,
            name,
            screen,
            smart_report_id,
            display_order,
            active
        )
        VALUES
            (
                'c53c567f-a97e-43a1-8718-f9782974609a',
                'Listagem de Membros',
                '/home/register/personMembers',
                'e3019b5e-5b3f-4309-9749-583401e48029',
                0,
                true
            ),
            (
                '47d444ec-13fb-44a0-9380-d52daea9e510',
                'Relatório de receitas',
                '/home/register/revenues',
                '47d444ec-13fb-44a0-9380-d52daea9e510',
                0,
                true
            ),
            (
                'f83d0a9e-cf8a-426a-bde6-fad4e6123a4f',
                'Relatório de despesas',
                '/home/register/expenses',
                'f83d0a9e-cf8a-426a-bde6-fad4e6123a4f',
                0,
                true
            ),
            (
                '6e02e0b8-cc7e-4408-b7a8-389cf8f1695a',
                'Extrato bancário',
                '/home/bank-statament',
                '6e02e0b8-cc7e-4408-b7a8-389cf8f1695a',
                0,
                true
            )
        ON CONFLICT (screen, smart_report_id) DO UPDATE SET
            name = EXCLUDED.name,
            display_order = EXCLUDED.display_order,
            active = EXCLUDED.active;
    END IF;
END $$;
