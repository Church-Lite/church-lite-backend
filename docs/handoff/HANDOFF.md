# Handoff — Church Lite Backend

> Atualizado em 19/08/2026.

> Configuração da igreja e aprovação de fechamento documentadas em `spec/SESSION_2026-07-17_CASH_CLOSING.md` na raiz do workspace.

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
- `getDashboardAgenda`;
- `getPermissionResources`.

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

## Atualização — Portal do Membro, transparência e aprovação consultiva (18/08/2026)

O acesso diferencia `MEMBER` e `STAFF` no mesmo `user_access`; `XAccessProfile` restringe sessões MEMBER às rotas `/church-lite/member-api/`. O membro é ligado ao acesso por `person_member.access_user_hash`. Autocadastro e ativação individual usam links UUID, confirmação de e-mail e CPF. A estrutura inicial está em `V20260817090000006__create_member_portal_access.sql`.

`GET /member-api/dashboard` retorna apenas nome do próprio membro, eventos futuros e contribuições pessoais recebidas. Os repositories de membro, eventos e contribuições são separados por entidade; não voltar a colocar `@EntityGraph(eventsType)` em repository cuja raiz seja `PersonMemberEntity`.

A transparência é configurada pelos endpoints `/memberPortal/transparency` e persistida por `V20260817090000007__create_member_portal_transparency.sql`. `GET /member-api/transparency` usa movimentações realizadas e aplica `DISABLED`, `FULL` ou `PARTIAL`, além de `HIDDEN`, `TOTAL_ONLY` e `DETAILED` por plano de contas. O contrato não serializa pessoa ou doador.

A aprovação consultiva usa `V20260817090000008__create_member_financial_approval.sql`. Endpoints administrativos `/memberApproval/*` criam prestações com um ou mais `cash_transactions` fechados, publicam e encerram. Endpoints `/member-api/financial-approvals` listam e registram voto. O voto não possui FK para membro/usuário; uma impressão SHA-256 específica da prestação impede duplicidade e a gestão recebe somente agregados. Esse fluxo nunca participa das regras de fechamento ou aprovação administrativa do caixa.

Pendente para a próxima sessão: definir autenticação e projeção de identidade do `church-lite-social`, que possui banco separado. Não replicar credenciais. A alternativa preferida é validação do JWT do Church Lite, com UUID estável do acesso e tenant como claims confiáveis, e perfil social local provisionado sob demanda ou por evento.

### Integração inicial com o Church Lite Social (18/08/2026)

O cadastro/ativação do Portal do Membro publica após commit o evento `member.profile.synced` no exchange `smart.church.events`. O contrato contém uma chave idempotente, `user_access.id`, IDs de membro e pessoa, tenant e dados públicos mínimos do perfil. Novos acessos ainda pendentes são enviados inativos; a confirmação do e-mail publica a atualização ativa. CPF, senha e JWT nunca são publicados. A fila padrão do consumidor é `smart.church.member-profile.social`, configurável por `SOCIAL_MEMBER_PROFILE_QUEUE`.

O evento independente `tenant.synced` garante a projeção de todas as igrejas no Social. Na inicialização, o Church Lite publica os tenants distintos existentes em `ADMIN.user_access`; novas confirmações também publicam o evento após commit. O Social cria/migra o schema homônimo no banco próprio. A fila padrão é `smart.church.tenant.social`, configurável por `SOCIAL_TENANT_QUEUE`.

Também na inicialização, `MemberProfileReconciliationPublisher` busca todos os acessos históricos que possuem `AccessProfile.MEMBER`, abre transação no schema de cada tenant, resolve `person_member.access_user_hash` e republica `member.profile.synced`. Logs informam total encontrado, publicação individual sem dados sensíveis, vínculos ausentes e total concluído. Pessoas sem acesso MEMBER não geram `social_access` até a ativação do Portal do Membro.


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


## Revisão do módulo de células — ajustes pós-Fase 2 (15/07/2026)

A migration incremental `V20260715090000006__align_cells_generated_columns.sql` corrige os relacionamentos criados na Fase 1 com sufixo `_id`. As entidades geradas usam os nomes definidos pelo gerador (`level_type`, `parent_unit`, `responsible`, `organization_unit`, `city`, `cell` e `person`). A migration original V04 não foi alterada para preservar o checksum do Flyway; a V06 renomeia as colunas existentes e mantém dados, FKs e índices.

Ao criar migrations para entidades geradas, conferir sempre os valores de `@JoinColumn(name = ...)` produzidos a partir do `properties.json`. Não presumir sufixo `_id`. Backend validado após a correção com `JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw compile -DskipTests`.

Para testes manuais foi criado temporariamente na raiz do workspace o arquivo `MASSA_DADOS_CELULAS_TEMP.sql`. Ele usa UUIDs fixos, casts explícitos `::uuid`, `ON CONFLICT DO NOTHING` e aproveita pessoas existentes no schema do tenant. O arquivo é descartável e não faz parte das migrations do produto.


## Atualização — grupos e permissionamento (16/07/2026)

O permissionamento administrativo usa grupos ativos e persistência deny-only. Todo recurso/operação do catálogo começa permitido; somente switches desligados geram registros em `permission_group_denial`. Um usuário pode participar de vários grupos e qualquer bloqueio de qualquer grupo ativo prevalece. Usuário sem grupo ou sem negação explícita continua permitido.

O catálogo é sempre obtido de `src/main/resources/resources.json`, gerado a partir do `properties.json`; nunca criar uma tabela de recursos. `GET /getPermissionResources` relê o catálogo e retorna `resource`, `description` e `permissions`. Comentários de entidades/endpoints alimentam `description`; contratos sem comentário usam o identificador do recurso. Novos recursos aparecem automaticamente permitidos.

Contratos gerados: `permissionGroup`, `permissionGroupMember`, `permissionGroupDenial` e o DTO não persistente `permissionResource` com `onlyDTO: true`. O CRUD de grupos segue o handler gerado. O endpoint de catálogo é declarado em `endpoints` e implementado manualmente por `handlers/permissions/PermissionResourcesHandlerImpl`, com `@CrossOrigin`, sem duplicar contrato. Metadados ficam em `config/metadata`, regras em `services/permissions` e autorização no interceptor existente em `config/interceptor`.

O interceptor resolve recurso/operação pelo catálogo e responde `403` com a chave `permission_access_denied` quando encontra negação. `OPTIONS` deve ser liberado antes da autenticação. A migration aceita `CREATE`, `VIEW`, `UPDATE` e `DELETE`. A separação futura `VIEW`/`VIEW_ALL` foi adiada até atualização do gerador; por enquanto `VIEW` cobre GET por ID e listagem.

Especificação canônica: `spec/FEATURE_PERMISSION_GROUPS_SPEC.md`. Backend validado com `JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw compile -DskipTests`.


## Atualização — template de cabeçalho e rodapé de relatórios (16/07/2026)

O contrato gerado `reportTemplate` representa a configuração visual única de relatórios por tenant. A entidade possui `id`, `headerImage`, `headerText` e `footerText`. Os dois textos armazenam HTML produzido pelo editor rich text; não criar campos separados para fonte, tamanho ou alinhamento.

O CRUD padrão está disponível em `/reportTemplate`, com `generateDefaultHandlers: true` e `handlerAbstract: false`. Não existe controller manual nem endpoint singleton específico: o frontend consulta o GET paginado com `size=1`, cria pelo POST quando não há registro e atualiza pelo PUT quando existe.

A migration `V20260716090000002__create_report_template.sql` cria `report_template`. `header_image` guarda a referência da imagem selecionada pelo componente compartilhado; `header_text` e `footer_text` usam `text`. O recurso também passa a fazer parte do catálogo gerado de permissões. Validação realizada com Java 25 e `./mvnw compile -DskipTests`.


## Atualização — planos SaaS, Fase 1 (19/07/2026)

A fundação de assinaturas foi adicionada ao contrato Gonthera com as entidades `subscriptionPlan`, `subscriptionPlanLimit`, `subscriptionPlanFeature` e `tenantSubscription`. Todas usam `generateDefaultControllers: false`: entity, DTO, converter, repository e service são gerados, mas nenhum CRUD HTTP é exposto.

A migration incremental `V20260719090000001__create_subscription_plans.sql` cria as quatro tabelas em cada schema migrado, inclusive o tenant administrativo. Cada igreja mantém localmente catálogo, limites, funcionalidades e assinatura. O schema `ADMIN` usa as mesmas estruturas para a futura visão consolidada por tenant, sem endpoint administrativo nesta fase.

Os planos iniciais são `FREE` (R$ 0,00), `ESSENTIAL` (R$ 19,99) e `PREMIUM` (R$ 39,99). Limites são linhas configuráveis por `plan + resource`; `limit_value = null` representa uso ilimitado. A migration semeia limites para pessoas, usuários administrativos, células ativas, armazenamento e grupos de permissão, além das funcionalidades comerciais acordadas. Alterações como elevar pessoas do gratuito de 30 para 50 são feitas por atualização de dados, sem migration estrutural ou regeneração.

O contrato foi validado com `gonthera-cli:validate` e os artefatos foram regenerados. A primeira compilação revelou um defeito geral do Gonthera CLI 2.0.0: DTOs gerados tinham campos package-private em `dtos`, enquanto converters em outro pacote faziam acesso direto. O problema foi resolvido na atualização para 2.0.1 registrada na Fase 2, sem edição manual de `_gen`.


## Atualização — planos SaaS, Fase 2 (19/07/2026)

O backend passou a resolver a assinatura vigente por tenant. Na ausência de registro em `tenant_subscription`, cria automaticamente uma assinatura ativa do plano `FREE`. A mesma assinatura é espelhada em `tenant_subscription` do tenant `admin`, mantendo a visão administrativa consolidada sem expor CRUD de planos. O espelhamento não monta nome de schema nem usa SQL nativo: executa a migration do tenant, troca `TenantContext`, aplica `TenantSchemaInterceptor.switchSchema()`, usa os repositories gerados e restaura o tenant original em `finally`. O contexto JPA recebe `flush/clear` antes de cada troca para não reutilizar entidades entre schemas.

`SubscriptionCache` define a abstração de cache e `InMemorySubscriptionCache` é a implementação inicial, com TTL de cinco minutos por tenant. O cache armazena plano, limites e funcionalidades; o consumo continua sendo calculado sobre os dados atuais. A interface foi separada para permitir futura substituição por Redis sem alterar as regras.

O endpoint autenticado gerado `GET /getCurrentSubscription` retorna DTOs tipados com plano, status, preço, período, recursos, consumo, limites, percentuais e funcionalidades. O tamanho usado no armazenamento é calculado pela soma paginada dos objetos do bucket do tenant.

Bloqueios implementados no backend:

- criação de pessoas;
- criação de usuários administrativos;
- criação de células ativas e ativação de células existentes;
- criação de grupos ativos e ativação de grupos existentes;
- novas solicitações de upload quando o bucket já alcançou o limite;
- dashboard executivo, traduções customizadas e template de relatório conforme as features do plano.

Edição, leitura e exclusão dos dados existentes permanecem liberadas, exceto nos módulos inteiramente condicionados a feature. Não foram adicionados locks, contadores persistidos, Redis, rate limit ou endpoints administrativos. O armazenamento não reserva antecipadamente o tamanho do upload: no MVP, o bloqueio ocorre quando o consumo já atingiu o teto.

Os services de `person`, `permissionGroup`, `translation` e `reportTemplate` são abstratos no contrato e possuem implementações Spring fora de `_gen`. O Gonthera CLI 2.0.1 corrigiu a visibilidade dos DTOs observada na Fase 1. Contrato validado, fontes regeneradas e backend compilado com Java 25 por `./mvnw compile -DskipTests`.

### Complemento — limites financeiros (20/07/2026)

Caixas e contas bancárias são recursos separados por `CashEntity.typeCash`: `CASH_ACCOUNT` e `BANK_ACCOUNT`. O cadastro `bank` representa apenas o catálogo de instituições e não consome limite. Os limites iniciais para cada recurso são 1 no FREE, 5 no ESSENTIAL e 20 no PREMIUM. A migration incremental `V20260720090000001__add_cash_subscription_limits.sql` preserva o checksum original. `CashService` é abstrato no contrato e `CashBusinessService` valida criação e mudança de tipo; edição sem troca de tipo permanece liberada.


## Encerramento — assinatura, geração e autorização (20/07/2026)

`SubscriptionResource` e `SubscriptionFeature` pertencem ao contrato `.gonthera/project.json` e são gerados em `churchlitebackend_gen.enums`; as versões manuais foram removidas. Novos recursos de plano devem começar no contrato, passar por `gonthera-cli:validate` e `gonthera-cli:generate-sources`, e nunca ser implementados diretamente em `_gen`.

O espelhamento de assinatura no tenant administrativo usa o mesmo fluxo multi-tenant da aplicação: `DBMigration.loadMigrateTenants`, `TenantContext.setCurrentTenant` e `TenantSchemaInterceptor.switchSchema()`. Não montar nomes de schema em regra de negócio. Antes de alternar schemas dentro da transação, executar `EntityManager.flush/clear`; restaurar o tenant original em `finally`.

Plano e permissionamento são camadas cumulativas. O catálogo de permissões decide se o usuário pode executar uma operação existente, enquanto `SubscriptionService` decide se o tenant contratou a feature ou ainda possui capacidade. Uma permissão concedida nunca contorna `requireFeature` ou `requireAvailable`. Erros de grupo usam `permission_access_denied`; restrições comerciais usam chaves `subscription_*`.

Validação final realizada com Gonthera CLI 2.0.1 e `JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw -DskipTests compile`.

## Atualização — confirmação de conta por e-mail (11/08/2026)

O cadastro inicial envia a confirmação pelo Resend. A conta permanece com `active=false` e `userConfirm=false`; o schema do tenant continua sendo criado somente depois que `GET /verifyURL` valida o token. Falhas do provedor retornam `502` e provocam rollback do cadastro transacional.

Configuração: `RESEND_KEY` é obrigatória para envio; `RESEND_FROM` aceita o remetente validado e usa `Church Lite <no-reply@smartverse.com.br>` como padrão; `FRONTEND_BASE_URL` define a origem do link e usa `http://localhost:4200` localmente. Em produção, configurar `FRONTEND_BASE_URL=https://app.smartverse.com.br/church-lite`.

`POST /resendConfirmation` é anônimo, recebe `{ "email": "..." }`, rotaciona o token somente para conta pendente e sempre responde de forma neutra. O template `models/email/new-churc.mo` usa HTML inline com identidade Church Lite/SmartVerse e placeholders `{{name}}` e `{{url}}`. Gonthera validado/regenerado e backend compilado com Java 25.

## Atualização — checkout de planos com Smart Payment (15/08/2026)

O checkout dos planos pagos usa exclusivamente o Smart Payment. `POST /createPaymentLink` recebe `planCode` e `billingCycle`, cria ou reutiliza uma cobrança `PENDING` no schema administrativo e chama `POST /paymentLink` por OpenFeign, propagando o JWT normal da requisição. O payload usa `service=CHURCH_LITE`, valor inteiro em centavos e o UUID da cobrança local como `client_id`. `GET /getPaymentHistory` lista apenas as cobranças do tenant autenticado.

Os ciclos disponíveis são `MONTHLY`, `QUARTERLY` e `SEMIANNUAL`, inicialmente configurados com 1 mês/0%, 3 meses/10% e 6 meses/15%. As tabelas `billing_discount`, `subscription_payment` e `payment_event_inbox` existem somente no schema administrativo pela migration `V20260815090000001__create_subscription_billing.sql`.

A confirmação é assíncrona pelo exchange `smart.payment.events`, fila `smart.payment.confirmed.church-lite` e routing key `payment.confirmed.CHURCH_LITE`. O subscriber é gerado pelo Gonthera; configuração, listener e regras ficam fora de `_gen`. O listener aceita apenas eventos `CHURCH_LITE`, persiste o inbox antes de conceder o benefício, valida `paymentId`, `transactionNsu`, `orderNsu`, cobrança e valores, e processa duplicidades de forma idempotente.

Depois da confirmação, a assinatura é ativada/renovada no schema da igreja e espelhada no schema administrativo na mesma transação, a cobrança passa para `PAID`, o inbox para `PROCESSED` e o cache do tenant é invalidado. Falhas ficam como `FAILED` com motivo e são retomadas pelo retry agendado local. Redirecionamento do navegador nunca confirma pagamento.

Configurações: `PAYMENT_SERVICE_BASE_URL` (padrão `https://app.smartverse.com.br/api/payment-service`), `PAYMENT_CONFIRMED_QUEUE`, `smart-payment.retry-delay-ms` e `smart-payment.retry-initial-delay-ms`. Contrato validado, fontes regeneradas e backend compilado com Java 25.

## Atualização — relatórios por tela com SmartReport (15/08/2026)

Cada schema de igreja possui a tabela `screen_report`, que associa um nome exibível e uma rota/tela do Church Lite ao UUID de um relatório no SmartReport. Uma mesma tela aceita qualquer quantidade de relatórios; `display_order` controla a ordem e `active` permite ocultar uma opção sem removê-la. Não existe CRUD público: os registros iniciais e personalizados são administrados diretamente no banco até existir uma ferramenta interna.

`GET /getScreenReports?screen=<rota>` retorna somente `id`, `name` e `screen` dos registros ativos do tenant autenticado. `POST /generateScreenReport` recebe o `reportId` local e o mesmo objeto JSON produzido pela tela em `data`; `SmartReportGenerationService` resolve o UUID remoto e chama `POST /generateReport` do SmartReport por OpenFeign. O nome específico do orquestrador evita colisão com o bean CRUD `churchlitebackend_gen.services.ScreenReportService`. A resposta mantém o contrato `{ "report": "<PDF em Base64>" }`.

A credencial fica exclusivamente em `integration_configuration` no schema administrativo, no registro `service = SMART_REPORT` (ordinal `0`). O campo extensível aceita pares separados por ponto e vírgula; para esta integração são reconhecidas `API_KEY`, `X_API_KEY` e `TOKEN`, por exemplo `API_KEY=sr_live_...`. A chave é enviada somente no header `X-API-Key`, não aparece no DTO do frontend e o JSON/PDF não é registrado em logs.

Configuração: `SMART_REPORT_BASE_URL`, com padrão `https://app.smartverse.com.br/api/smartreport`. A migration `V20260815090000005__create_screen_report.sql` cria a tabela somente fora do tenant administrativo. Contrato Gonthera validado, fontes regeneradas e backend compilado com Java 25.

A migration `V20260815090000006__seed_standard_screen_reports.sql` cadastra em cada tenant os relatórios padrão de membros, receitas, despesas e extrato bancário com os UUIDs dos templates publicados no SmartReport. O seed usa a chave natural `screen + smart_report_id`, atualiza nome, ordem e status quando o vínculo já existe e não cria registros no schema administrativo.

## Atualização — lançamentos financeiros recorrentes (17/08/2026)

`recurringFinancial` representa a regra de parcelamento ou recorrência, enquanto cada ocorrência permanece um `financial` comum. `RecurringFinancialBusinessService` cria regra e ocorrências na mesma transação, aceita parcelamentos de 2 a 60 e cria uma janela inicial de 12 para regras contínuas. Os lançamentos armazenam `recurringFinancial` e `recurrenceNumber`, protegidos por unicidade. Atualizações atingem somente ocorrências futuras não liquidadas; exclusão é recusada quando existe ocorrência liquidada. Migration: `V20260817090000001__create_recurring_financial.sql`. A reposição automática da janela contínua permanece evolução pendente por exigir iteração segura entre tenants.

`planAccount.financialNature` separa receitas e despesas sem substituir o `type` analítico/sintético. A raiz exige `REVENUE` ou `EXPENSE`; filhos herdam o valor do pai; atualizações não podem mudar a natureza e movimentações entre árvores exigem natureza idêntica. `FinancialBusinessService` e `RecurringFinancialBusinessService` rejeitam planos incompatíveis com o tipo do lançamento. A migration `V20260817090000002__add_plan_account_financial_nature.sql` classifica cada árvore histórica pela descrição da raiz: menções a receita, revenue, income ou entrada resultam em `REVENUE`; o restante assume `EXPENSE`. Registros órfãos usam a própria descrição como fallback e podem ser corrigidos manualmente no banco.

## Atualização — consulta de CEP via ViaCEP (17/08/2026)

O endpoint Gonthera autenticado `GET /lookupPostalCode` recebe o CEP, e `PostalCodeLookupService` chama o ViaCEP por OpenFeign. A cidade é obrigatoriamente resolvida por nome e sigla da UF no schema do tenant antes da resposta; o IBGE externo é ignorado. O contrato retorna CEP formatado, logradouro, bairro, complemento e `CityDTO`; o frontend nunca conhece o payload externo. Configure `VIA_CEP_BASE_URL` quando necessário; os timeouts são 3s para conexão e 5s para leitura.
## Integração da foto do perfil social (2026-08-18)

- O endpoint HTTP `POST /member-api/profile-image/apply-if-empty` foi removido. A atualização chega exclusivamente pelo evento `social.profile.image.updated`, na fila padrão `smart.church.member-image.church-lite`.
- O consumidor valida tenant, `accessId`, `memberId` e `personId`; `person.image` só é preenchido quando está nulo ou vazio, portanto uma foto administrativa existente nunca é sobrescrita.
- O fluxo registra logs de recebimento, aplicação e descarte do evento.

## Atualização — identidade, mensageria e Portal do Membro (19/08/2026)

O Church Lite permanece como autoridade de autenticação, identidade administrativa, associação membro–usuário e armazenamento. O Social recebe somente projeções mínimas e nunca consulta diretamente o banco deste serviço.

### Sincronização com o Social

Todos os eventos usam o exchange `smart.church.events` e classes de contrato geradas pelo Gonthera CLI. O Church Lite publica:

- `tenant.synced` para a fila `smart.church.tenant.social`, na criação e na reconciliação dos tenants existentes;
- `member.profile.synced` para a fila `smart.church.member-profile.social`, no cadastro/ativação e na reconciliação dos membros existentes.

A reconciliação retroativa repara também `person_member.access_user_hash` quando existe uma correspondência única: primeiro por CPF normalizado e, como fallback, por e-mail exato em minúsculas. Ausência ou ambiguidade não é resolvida por aproximação; o item é ignorado com log de diagnóstico. Isso permite projetar usuários antigos sem alterar silenciosamente vínculos duvidosos.

Consumers assíncronos devem definir explicitamente o `TenantContext` antes de abrir a transação JPA e restaurá-lo em `finally`. Nunca chamar um service `@Transactional` com tenant nulo, pois o provider multi-tenant precisa do schema antes de obter a conexão.

### Foto social refletida no cadastro administrativo

O evento `social.profile.image.updated` é consumido pela fila `smart.church.member-image.church-lite`. O processamento:

1. valida tenant, acesso, membro e pessoa;
2. recusa combinações inconsistentes;
3. preenche `person.image` somente quando o cadastro administrativo ainda não possui foto;
4. cria uma notificação administrativa `MEMBER_PROFILE_IMAGE_UPDATED` para cada destinatário configurado no tenant;
5. deduplica notificações por evento e destinatário.

O endpoint HTTP que aplicava imagem diretamente foi removido. Esta integração é exclusivamente por mensageria. O fluxo possui logs com identificadores técnicos e resultado, sem JWT, CPF ou conteúdo sensível.

### Storage privado por tenant

Uploads e exclusões feitos pelo perfil `MEMBER` continuam restritos à pasta do próprio UUID de acesso. Para leitura e geração de URL assinada, `StorageImpl` extrai o UUID proprietário da chave e confirma no schema administrativo que ele pertence ao mesmo tenant do solicitante. Assim, membros da mesma igreja podem visualizar avatares e imagens de posts uns dos outros, enquanto o acesso entre tenants permanece negado.

Objetos não são públicos e exigem token para solicitar a URL temporária. Novas chaves sociais seguem `accessUuid/imageUuid.ext`; chaves legadas sem proprietário identificável não devem ser liberadas automaticamente. Decisões de autorização e recusas possuem logs próprios.

### Portal do Membro

`/member-api/dashboard` exige vínculo válido entre o acesso autenticado e `person_member`; `member_portal_access_not_linked` indica ausência desse relacionamento, não falta de permissão administrativa. Rotas `/member-api/**` são reconhecidas pelo interceptor como portal de membro, enquanto rotas gerenciais continuam sujeitas ao perfil administrativo.

O dashboard do membro fornece agenda e histórico/resumo das próprias contribuições. Transparência e aprovações permanecem sob as configurações da igreja e nunca ampliam o acesso aos dados de outro tenant.

### Templates de e-mail

Os e-mails usam modelos HTML em `src/main/resources/models/email/`:

- `new-churc.mo`: confirmação da igreja/conta;
- `member-access-invitation.mo`: convite de acesso do membro;
- `member-access-confirmation.mo`: confirmação do acesso do membro.

`EmailService.renderModel` substitui placeholders a partir de um mapa e falha quando resta placeholder obrigatório sem valor. Não voltar a montar esses e-mails por concatenação de HTML em Java. `POST /resendConfirmation` continua sendo a rota anônima e neutra para reenvio.

### Próximas evoluções relacionadas

- publicar evento de remoção física de objetos quando conteúdo social for excluído;
- ampliar testes de contrato e idempotência RabbitMQ;
- criar métricas/alertas para eventos falhos além dos logs;
- manter grupos da comunidade no Social, sem confundi-los com grupos administrativos de permissão deste backend.
