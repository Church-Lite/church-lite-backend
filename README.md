# Getting Started

#### [Swagger](http://localhost:8081/basebackend/swagger-ui/index.html)

Ao copiar este projeto deve-se fazer as seguintes configurações:

Definir as seguintes variaveis de ambiente

    DATABASE_SCHEMA_NAME=base_backend_java;
    DB_NAME=POSTGRES;
    DB_PASSWORD=229977@@##;
    DB_USERNAME=postgres;
    SECRET_JWT=;
    SERVER_PORT=8081;
    SERVICE_NAME=basebackend;

O projeto é configurado para ser multitenancy com locatarios usando schemas

sendo assim os schemas da base deven ser cirados usando o seguinte padrão:

"NOMEBANCO_NOMESCHEMA" ambos em maiusculo, para manter um padrão, mas o nome do schema não precisa ser necessariamente maiusculo
basta estar de acordo com o cadastrado na tabela user_supplier
