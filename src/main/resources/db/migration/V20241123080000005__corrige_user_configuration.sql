do $$
DECLARE
_user RECORD;
    _tenant varchar;
BEGIN

FOR _user in SELECT * FROM user_access LOOP
    RAISE NOTICE 'Usuário: %', _user.name;

_tenant := 'CHURCH_LITE_' || _user.tenant;


EXECUTE FORMAT(
        'INSERT INTO %I.user_configuration (id, name,user_photo, theme,lang, email) VALUES ($1, $2, $3,$4,$5,$6)',
        _tenant
        )USING gen_random_uuid(), _user.name, '','0','0',_user.email;
END LOOP;

END $$;