# Handoff — Church Lite Backend

## Visão do produto

O Church Lite é o backend de um sistema de gestão de igrejas. Ele isola os dados de cada igreja por tenant e oferece cadastros, agenda, financeiro, armazenamento de imagens e autenticação.

Parte da API é gerada pelo plugin `entity-generator`. Este documento descreve o serviço Church Lite e a separação correta entre código manual e gerado.

## Stack

- Spring Boot 3.5 e Java configurado para 25;
- Spring Web, Data JPA e HATEOAS;
- PostgreSQL e Flyway;
- autenticação/JWT via `authorization-backend`;
- multitenancy por schema;
- AWS S3, RabbitMQ e OpenAPI;
- `entity-generator` 1.0.1.

Swagger local: `http://localhost:5050/church-lite/swagger-ui/index.html`.

## Código manual e gerado

O contrato fica em `properties.json`. O gerador produz:

```text
src/main/java/com/smartverse/churchlitebackend_gen/
src/main/resources/properties.json
src/main/resources/resources.json
src/main/resources/postgree.sql
```

Regras:

- nunca implementar regra de negócio em `_gen`;
- não revisar `_gen` como código autoral;
- alterações estruturais começam no `properties.json` da raiz;
- regenerar e revisar os impactos nos handlers manuais;
- customizações ficam em `com.smartverse.churchlitebackend`;
- `_gen` é saída descartável e pode ser recriado.

## Domínio

O contrato possui 21 entidades.

### Pessoas

`person`, `personAddress`, `personalDocs`, `personalTelphone`, `personalEmail`, `personMember`, `positions`, `city`, `state` e `country`.

Uma pessoa pode representar membro, visitante, novo convertido, fornecedor, criança ou outra igreja.

### Agenda

`appointments` e `eventsType`.

### Financeiro

`planAccount`, `costCenter`, `bank`, `cash`, `financial`, `transactions` e `cashTransactions`. Plano de contas e centro de custo são hierárquicos.

### Usuário e implantação

`userConfirmation`, `userConfiguration` e estruturas de autenticação mantidas pelas migrations/biblioteca de autorização.

## Responsabilidades

O gerador fornece entidades, DTOs, converters, repositories, handlers CRUD, interfaces de endpoints, enums e metadados.

O código manual fornece:

- autenticação, registro e confirmação da igreja;
- configuração e migração de tenants;
- abertura, fechamento e consolidação de caixa;
- transações originadas de receitas/despesas;
- hierarquias de plano de contas e centro de custo;
- configuração do usuário;
- URLs assinadas do S3;
- health check, metadados, e-mail e RabbitMQ.

## Multitenancy

Cada igreja opera em schema próprio. O interceptor valida o JWT, extrai o tenant, configura `TenantContext`, carrega migrations e direciona a conexão para `<DATABASE>_<TENANT>`.

Rotas anônimas fora da lista fixa precisam enviar `Xtenant`.

Cuidados:

- liberações usam `/church-lite` fixo;
- o tenant participa da seleção do schema;
- migrations no primeiro acesso podem afetar latência;
- mudanças de banco devem usar migrations incrementais.

## Regras financeiras

### Receitas e despesas

Ao salvar, alterar ou excluir `financial`, o serviço sincroniza a movimentação em `transactions`.

### Caixa

- não deve haver mais de uma abertura ativa por caixa;
- abertura e fechamento alteram o status;
- endpoints calculam receitas, despesas, saldo e resumo atual.

### Hierarquias

Plano de contas e centro de custo preservam filhos nas atualizações. Exclusões de filhos também atualizam o pai. Alterações exigem testes com múltiplos níveis.

## Endpoints customizados

Declarados no contrato:

- `verifyURL`;
- `requestUpload`, `requestUrl` e `deleteObject`;
- `getUser`;
- `getIDCashTransaction`, `getSumValuesCash`;
- `getBalanceBankAccount`, `getResumeTransaction`.

Também existem `/authenticate`, `/register`, `/metadata`, `/status` e os CRUDs gerados.

## Ambiente

Variáveis conhecidas: `DATABASE_SCHEMA_NAME`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `SECRET_JWT`, `SERVER_PORT`, `SERVICE_NAME`, `SERVER_HOST`, AWS e RabbitMQ.

O README repete `SERVER_PORT`; uma ocorrência provavelmente deveria ser a porta do banco.

## Estado e riscos

- Maven exige Java 25; com Java 17 falha em `release version 25 not supported`.
- Surefire usa `skipTests=true`.
- O teste existente cobre apenas carga básica do contexto.
- Há segredo fixo em `application.properties`; mover para variável/secret.
- `spring.jpa.show-sql=true` deve ficar restrito a desenvolvimento.
- `MetadataHandler` retorna JSON como texto e pode retornar `null`.
- `MultiTenantConnectionProviderImpl.unwrap()` retorna `null`.
- Há CORS `*` em handlers; consolidar por ambiente.
- Operações financeiras devem ser atômicas.
- O serviço já usa gerador 1.0.1; documentação antiga citava 1.0.0.

## Nova funcionalidade

### CRUD simples

1. Alterar `properties.json`.
2. Regenerar fontes e recursos.
3. Revisar impactos, SQL e contrato.
4. Criar migration incremental.
5. Compilar e testar.

### Regra de negócio

1. Manter contrato gerável quando aplicável.
2. Criar handler/service/repository fora de `_gen`.
3. Usar transação atômica.
4. Validar isolamento por tenant.
5. Testar sucesso, autorização e falhas parciais.

### Endpoint específico

1. Declarar no contrato quando compatível.
2. Implementar a interface gerada em handler manual.
3. Definir anonimato e permissões.
4. Tipar entrada e saída para o frontend.

## Checklist de entrega

1. Nenhum código manual em `_gen`.
2. Migration compatível com schemas existentes.
3. JDK alinhada ao `pom.xml`.
4. Compilação e testes executados.
5. Swagger e contrato do frontend validados.
6. Autenticação, autorização e tenant validados.
7. Atomicidade financeira verificada.
8. Nenhum segredo adicionado.

## Atualização — agenda recorrente (14/07/2026)

A agenda utiliza `eventsType` para nome, descrição e cor visual dos compromissos. `appointments` agora possui status, recorrência semanal, dias da semana, data final e identificador de série.

A criação usa o endpoint gerado `POST /createRecurringAppointments`, com `CreateRecurringAppointmentsInput` e `CreateRecurringAppointmentsOutput`. Para recorrência semanal, o backend gera todas as ocorrências de forma transacional, limitada a dois anos. Compromissos podem ser atualizados, reagendados, cancelados logicamente ou excluídos pelo CRUD padrão.

Os relacionamentos de `appointments` com `eventsType` e `userConfiguration` são `ManyToOne`. Os enums gerados usam persistência ordinal: `SCHEDULED/NONE = 0` e `CANCELLED/WEEKLY = 1`.

## Atualização — cadastro de usuários (14/07/2026)

O cadastro administrativo usa o endpoint gerado `POST /createChurchUser`. O tenant nunca vem da entrada: ele é obtido de `TenantContext`, preenchido pelo JWT.

A operação grava as credenciais com BCrypt em `ADMIN.user_access` e cria a configuração correspondente em `user_configuration` no schema da igreja. Nome, e-mail e telefone são sincronizados nas edições; a exclusão remove os dois registros. A duplicidade é verificada por `email + tenant`, permitindo o mesmo e-mail em igrejas diferentes.

`user_access.id` é formalizado como PK pela migration e o telefone existe nas duas tabelas. O fluxo futuro de escolha de igreja para e-mails com múltiplos tenants não faz parte desta entrega.
