# Handoff — Church Lite Backend

> Atualizado em 15/07/2026.

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
- `entity-generator` 1.0.2.

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

O contrato possui 22 entidades.

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
- `getBalanceBankAccount`, `getResumeTransaction`;
- `createRecurringAppointments`;
- `createChurchUser`;
- `getDashboardFinancial`;
- `getDashboardAgenda`.

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

`user_access.id` é formalizado como PK pela migration e o telefone existe nas duas tabelas. O fluxo multi-tenant atual está descrito na atualização de 15/07/2026 abaixo.

## Atualização — autenticação multi-tenant (15/07/2026)

O mesmo e-mail pode existir em mais de uma igreja. `AuthenticationService.login` busca os vínculos em `ADMIN.user_access` por e-mail, ordenados pelo ID, e executa BCrypt individualmente para cada registro. Um vínculo com senha diferente não tem seu token exposto.

Resposta atual de `POST /authenticate`:

- `accessToken`: JWT do primeiro vínculo válido;
- `token`: ID do usuário do primeiro vínculo válido;
- `churches`: lista com `userId`, `name`, `tenant` e `accessToken` de cada vínculo validado;
- `requiresTenantSelection`: indica se existe mais de uma opção.

A resposta contém os tokens finais; não existe endpoint adicional para trocar tenant. Ao alterar esse fluxo, preservar a validação individual da senha e o comentário de segurança em `AuthenticationService`.

## Atualização — dashboard executivo (15/07/2026)

O dashboard foi implementado em `com.smartverse.churchlitebackend.dashboard`, sem regras no handler e sem alterações manuais em `_gen`:

- `DashboardHandlerImpl`: implementa os contratos gerados e converte os snapshots tipados para a saída `Map` do contrato;
- `DashboardService`: filtros, comparações, agrupamentos, saldos, alertas e agenda;
- `DashboardRepository`: centraliza o acesso aos repositories gerados;
- `DashboardModels`: records específicos de entrada e saída.

Contratos declarados no `properties.json`:

- `GET /getDashboardFinancial` recebe período, banco, conta bancária, caixa, somente caixas abertos, centro de custo e plano de contas;
- `GET /getDashboardAgenda` retorna agenda de hoje, próximos eventos e contadores do dia, semana e mês.

Regras adotadas:

- receitas e despesas realizadas são calculadas sobre `transactions`; `financial` fornece classificação, origem, plano de contas e centro de custo;
- o período anterior possui a mesma quantidade de dias do período selecionado;
- evolução usa granularidade diária, semanal ou mensal conforme a duração;
- despesas sem centro de custo ou plano de contas permanecem nos agrupamentos;
- saldo bancário considera as movimentações da conta;
- saldo de caixa aberto usa saldo inicial da sessão mais movimentações vinculadas; caixa fechado usa o saldo final da última sessão;
- saldo disponível representa a posição atual e não é limitado pelo período financeiro;
- compromissos cancelados não entram nos resumos da agenda;
- o tenant continua vindo exclusivamente do JWT/interceptor.

Ponto de evolução: o repository atual monta o snapshot usando `findAll()` dentro de transação read-only. Para tenants com alto volume, substituir por consultas agregadas/paginadas preservando os mesmos DTOs e regras de fechamento dos agrupamentos.

Validação realizada com JDK 25:

```bash
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw clean compile -DskipTests
```


## Atualização — traduções customizadas (15/07/2026)

A entidade gerada `translation` armazena somente sobrescritas por tenant, identificadas por `language + translationKey`. O CRUD padrão gerado (`/translation`) é usado para consultar, criar, alterar e remover customizações. A migration `V20260715090000003__create_translation.sql` cria a tabela e a restrição única. Os arquivos JSON do frontend continuam sendo a fonte padrão; restaurar uma tradução remove a sobrescrita persistida.


## Atualização consolidada — traduções e usuários (15/07/2026)

### Traduções

O contrato possui 22 entidades após a inclusão de `translation`. A entidade e seu CRUD são gerados pelo `entity-generator` 1.0.2. O identificador usa `GenerationType.UUID`; portanto, inclusões em `POST /translation` não enviam `id`. O banco gera o UUID, enquanto atualizações usam `PUT /translation/{id}`.

A unicidade de `language + translation_key` é garantida pela migration incremental. Como a tabela vive no schema do tenant, customizações permanecem isoladas por igreja. O backend armazena somente sobrescritas; os valores padrão continuam nos JSONs do frontend.

Recursos gerados: `CREATE`, `VIEW`, `UPDATE` e `DELETE` para `translation`. Não adicionar regra manual ao handler `_gen`. Qualquer validação adicional de catálogo deve ser implementada fora do diretório gerado.

### Cadastro administrativo de usuários

O padrão atual permanece baseado em contrato gerado e implementação manual: `POST /createChurchUser` define entrada/saída no `properties.json`, e `UserConfigurationCustomHandlerImpl` delega a operação ao serviço. O tenant vem exclusivamente de `TenantContext`; criação, edição e exclusão sincronizam `ADMIN.user_access` com `user_configuration`. Senhas usam BCrypt, e a unicidade é `email + tenant`.

O fluxo de autenticação multi-tenant valida a senha individualmente em cada vínculo e retorna somente opções validadas. Alterações futuras devem preservar atomicidade, isolamento, ausência de senha/hash nas respostas e sincronismo entre os dois registros.

Especificações reutilizáveis na raiz do workspace:

- `spec/FEATURE_TRANSLATIONS_SPEC.md`;
- `spec/FEATURE_USER_REGISTRATION_SPEC.md`.


## Atualização — módulo de células, Fase 1 inicial (15/07/2026)

A base gerável do módulo foi adicionada ao `properties.json` com `cellOrganizationLevelType`, `cellOrganizationUnit`, `cell`, `cellLeadership`, `cellMember` e `cellModuleSettings`, além dos enums específicos. A migration `V20260715090000004__create_cells_module_base.sql` cria tabelas, FKs, índices, unicidade de código e restrições para vínculos ativos.

Regras manuais ficam em `com.smartverse.churchlitebackend.cells`: validação de célula ativa, código único, hierarquia sem ciclos, preservação de subunidades, liderança ativa e vínculos de membros sem duplicidade. Exclusão física de liderança e membro foi bloqueada; esses vínculos devem ser encerrados para preservar histórico.


### Continuação da Fase 1 — contratos em uso (15/07/2026)

Os CRUDs gerados de níveis, unidades, lideranças, membros e configurações passaram a ser consumidos pela área administrativa do frontend. Lideranças e membros são encerrados por atualização, nunca por exclusão física. A hierarquia continua protegida contra autorreferência e ciclos no handler manual, e índices parciais reforçam vínculos ativos e célula principal.


## Atualização — módulo de células, Fase 2 (15/07/2026)

Foram declaradas no `properties.json` e geradas as entidades `cellVisitor`, `cellMeeting`, `cellAttendance` e `cellPrayerRequest`, com seus enums e CRUDs abstratos. A migration `V20260715090000005__create_cells_meetings.sql` cria as tabelas, relacionamentos, índices e restrições de datas, contagens e presença única por reunião.

As regras manuais permanecem em `com.smartverse.churchlitebackend.cells`. Visitantes validam período e quantidade de visitas; reuniões nascem em `DRAFT` e somente rascunhos/rejeitadas podem ser editados; presenças exigem exatamente uma pessoa ou visitante e não admitem duplicidade; pedidos de oração aceitam pessoa ou visitante, nunca ambos.

O workflow usa os contratos gerados `POST /submitCellMeeting` e `POST /reviewCellMeeting`. A transição permitida é `DRAFT|REJECTED -> SUBMITTED -> APPROVED|REJECTED`; rejeição exige motivo. A implementação altera a entidade gerenciada carregada do repository, sem persistir DTO convertido com identificador existente. Validação realizada com `JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw compile -DskipTests`.
