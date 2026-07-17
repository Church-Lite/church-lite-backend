---
name: entity-generator-project
description: Use this skill when working on the PotatoTech entity-generator Maven plugin, especially when changing Java, .NET, Node.js, SQL generation, properties.json schema, RabbitMQ messaging, relationships, DTO converters, Prisma generation, README, Docusaurus docs, or handoff documentation.
---

# Entity Generator Project

Use this skill for code or documentation changes in the `entity-generator` project.

## Project Purpose

`entity-generator` is a Maven plugin/JAR that reads `properties.json` from the current working directory and generates code for service projects.

Supported targets:

- `JAVA`: Spring/JPA style generation under `src/main/java/<mainPackage>_gen` and resources under `src/main/resources`.
- `DOTNET`: C# generation under `<mainPackage>_gen` and static files under `static`.
- `NODE`: TypeScript generation under `src/generated` plus `prisma/schema.prisma`.
- SQL: PostgreSQL script generation as `postgree.sql`.
- Messaging: RabbitMQ generation under `messaging.RabbitMq`.

## Must Read First

Before making non-trivial changes, inspect:

- `docs/handoff/HANDOFF_ENTITY.md`: implementation-oriented behavior and known limitations.
- `docs/handoff/HANDOFF_FRONTEND_ENTITY.md`: frontend-facing JSON and query contracts.
- `README.md`: user-facing instructions.
- `CHANGELOG.md`: current release notes and future improvements.
- Relevant generator package:
  - Java: `src/main/java/com/potatotech/entitygenerator/service/java`
  - .NET: `src/main/java/com/potatotech/entitygenerator/service/dotNet`
  - Node: `src/main/java/com/potatotech/entitygenerator/service/node`
  - Shared: `src/main/java/com/potatotech/entitygenerator/service/common`
  - Models: `src/main/java/com/potatotech/entitygenerator/model`

For docs-only changes in the Docusaurus project, inspect `docs-docusaurus/docs` and `docs-docusaurus/sidebars.js`.

## properties.json Contract

The minimum valid shape must include arrays even when empty:

```json
{
  "mainPackage": "com.example.service",
  "projectName": "service-name",
  "language": "JAVA",
  "entities": [],
  "endpoints": [],
  "enums": []
}
```

Accepted `language` values are uppercase:

- `JAVA`
- `DOTNET`
- `NODE`

Unknown JSON properties are ignored by Gson. Do not document unsupported properties as active behavior.

## Entity Field Shape

Use the real field shape:

```json
{
  "comment": "Identificador",
  "fieldName": "id",
  "list": false,
  "fieldProperties": {
    "fieldType": "uuid",
    "required": true,
    "valueDefault": ""
  },
  "metadata": {
    "nullable": false,
    "key": true
  }
}
```

Do not use simplified field examples like top-level `fieldType` or top-level `nullable`; the implemented model uses `fieldProperties.fieldType` and `metadata.nullable`.

## Relationships

Java and C# relationship generation are considered mature. Node relationship generation is initial and may require manual Prisma review.

Relationship fields use `relationShips`:

```json
{
  "fieldName": "parentCode",
  "list": false,
  "fieldProperties": {
    "fieldType": "costCenter",
    "required": false,
    "valueDefault": ""
  },
  "metadata": {
    "nullable": true,
    "key": false
  },
  "relationShips": {
    "fetchType": "LAZY",
    "relationShip": "ManyToOne",
    "bidirectional": false,
    "reference": true
  }
}
```

Rules:

- `bidirectional: false`: owner side. Generates FK/column and `@JoinColumn` in Java.
- `bidirectional: true`: inverse side. Uses `mappedBy` and does not generate SQL column.
- `mappedBy`: must point to the owner field, for example `children` maps by `parentCode`.
- `reference: true`: normally set on the FK/reference side; Java converters use it to avoid recursion.
- `list: true`: collection side, usually `OneToMany` or `ManyToMany`.
- For self-relations, declare the owner/reference field before the inverse field.

Example inverse side:

```json
{
  "fieldName": "children",
  "list": true,
  "fieldProperties": {
    "fieldType": "costCenter",
    "required": false,
    "valueDefault": ""
  },
  "metadata": {
    "nullable": false,
    "key": false
  },
  "relationShips": {
    "fetchType": "LAZY",
    "relationShip": "OneToMany",
    "mappedBy": "parentCode",
    "bidirectional": true,
    "reference": false
  }
}
```

## Messaging

Messaging is grouped by provider. Current provider:

```json
{
  "messaging": {
    "RabbitMq": {
      "pub": [],
      "sub": []
    }
  }
}
```

Rules:

- Generate RabbitMQ code only when `messaging.RabbitMq` exists and has at least one `pub` or `sub` channel.
- Do not generate RabbitMQ dependencies/configuration when `RabbitMq` is absent, null, or empty.
- Keep the exchange outside `properties.json`; Java/.NET use annotation/attribute in consumer code, Node uses a concrete config class.
- The old `pup` typo must not be reintroduced. Use `pub`.
- The old `events`/`listeners` model is not implemented; do not document it as current behavior.

## Node.js Scope

Node generation currently includes:

- models/DTO contracts;
- enums;
- Prisma-friendly repositories;
- Express controllers and routes;
- endpoint contracts;
- RabbitMQ when configured;
- static files;
- SQL;
- `prisma/schema.prisma`.

Node does not generate:

- `package.json`;
- `tsconfig.json`;
- migrations;
- Java-style DTO converters.

Relationship caveat:

- Node/Prisma relation generation is a starting point.
- Self-relations, bidirectional relations, and `ManyToMany` should be documented as requiring manual review before migrations.
- Avoid claiming Node relationship parity with Java/C# until `GeneratePrisma` and generated TypeScript models are improved.

## Templates

The generator source uses `.mxsd` templates under `src/main/resources/xsd`.

Use templates for generated code instead of large inline string literals.

Template folders:

- `xsd/java`
- `xsd/dotnet`
- `xsd/node`
- `xsd/sql`

User-facing documentation should not explain internal templates unless the user explicitly asks about generator development.

## CRUD Filters

Treat filtering as target-specific behavior, not as a portable JPA/SQL query language.

- Java `SpecificationFilter` supports `eq`, `isNull`, `notNull`, `and`, `or`, and dotted relationship paths.
- Java string `eq` is a case-insensitive contains operation; UUID uses exact equality.
- Java numeric, boolean, and date equality is not safely converted by the current template.
- Do not claim reliable mixed `and`/`or` precedence, nested parentheses, escaping, comparison operators, or `in` support.
- .NET `DynamicFilter` is a separate dialect with `eq` and one logical operator kind per expression; collection paths use `*`.
- Node repositories currently ignore `filter` and use only pagination parameters.
- When filter behavior changes, update both entity handoffs and the relevant user documentation.

## Documentation Standards

Update docs when behavior changes:

- `README.md`: user-facing usage.
- `docs/handoff/HANDOFF_ENTITY.md`: implementation details and caveats.
- `docs/handoff/HANDOFF_FRONTEND_ENTITY.md`: frontend request construction and limitations.
- `CHANGELOG.md`: release notes and future improvements.
- `docs-docusaurus/docs`: detailed user documentation.

Docusaurus docs should focus on how to use the generator, not how the generator is implemented.

For .NET and Node docs, mention that consumers can download `entity.exe` or `entity-generator-x.x.x.jar` and execute it in the project root, in the same folder as `properties.json`.

## Church Lite Consumer Architecture

When changing generated contracts in `church-lite-backend`, treat the root
`properties.json` as the source of truth.

- Use the generated CRUD for ordinary `POST`, `PUT`, `DELETE`, `GET /{id}`
  and paginated `GET` operations. Do not create a parallel manual controller
  for an entity already represented in `properties.json`.
- Set `generateDefaultHandlers: true` and `handlerAbstract: false` when the
  generated CRUD needs no customization.
- Set `handlerAbstract: true` when a standard CRUD operation needs business
  rules. Implement the generated abstract handler outside `_gen` and override
  only the required methods.
- Declare non-persistent request/response models as entities with
  `onlyDTO: true`. Consume the generated `*DTO`; do not create equivalent
  records or DTO classes manually.
- Remember that generator 1.0.2 still traverses `onlyDTO` contracts while
  producing SQL. Give each such contract one technical key field when generation
  otherwise fails with a missing-key error.
- Declare non-CRUD operations in the `endpoints` array. Implement the generated
  interface in a manual `@RestController`; do not duplicate its route in a
  handwritten controller contract.
- Never edit `com.smartverse.churchlitebackend_gen` directly. Regenerate after
  changing the contract and compile with the JDK configured by the service.

Place manual Church Lite code according to its responsibility:

- endpoint implementations in `com.smartverse.churchlitebackend.handlers.<domain>`;
- business rules in `com.smartverse.churchlitebackend.services.<domain>`;
- repositories in `com.smartverse.churchlitebackend.repository.<domain>`;
- metadata/catalog infrastructure in
  `com.smartverse.churchlitebackend.config.metadata`;
- request interception in the existing
  `com.smartverse.churchlitebackend.config.interceptor.InterceptorConfig`.

Do not create a broad domain package containing controllers, services and
infrastructure together. Extend the existing interceptor instead of registering
a second interceptor for the same authentication/authorization request flow.

## Validation

Prefer focused validation over full `mvn test` when generated `_gen` sources or local dependency state make full tests noisy.

Useful compile check for generator sources:

```bash
find src/main/java/com/potatotech/entitygenerator -name '*.java' ! -path '*entitygenerator_gen*' > /tmp/entity-generator-sources.txt
rm -rf /tmp/entity-generator-compile
mkdir -p /tmp/entity-generator-compile
javac -cp /home/geovane/.m2/repository/org/projectlombok/lombok/1.18.28/lombok-1.18.28.jar:/home/geovane/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar:/home/geovane/.m2/repository/org/apache/logging/log4j/log4j-api/2.23.1/log4j-api-2.23.1.jar:/home/geovane/.m2/repository/org/apache/logging/log4j/log4j-core/2.23.1/log4j-core-2.23.1.jar:/home/geovane/.m2/repository/org/apache/maven/plugin-tools/maven-plugin-annotations/3.9.0/maven-plugin-annotations-3.9.0.jar:/home/geovane/.m2/repository/org/apache/maven/maven-plugin-api/3.9.3/maven-plugin-api-3.9.3.jar -d /tmp/entity-generator-compile @/tmp/entity-generator-sources.txt
```

Docusaurus validation:

```bash
cd docs-docusaurus
npm run build
```

When validating generation, create a temporary project under `/tmp`, copy or create `properties.json`, execute `Main` or the compiled classes from that folder, then inspect generated output.

## Editing Rules

- Use existing package patterns; do not invent a parallel architecture.
- Keep manual generated-code templates in `.mxsd` files.
- Do not change generated output paths unless explicitly requested.
- Do not revert unrelated dirty files.
- Do not edit generated `_gen` output as source of truth.
- Keep Node relationship limitations honest in docs until implementation is complete.


## Church Lite Permission Groups

Ao trabalhar no permissionamento do Church Lite:

- Tratar o `properties.json` da raiz como fonte de contratos e `resources.json` como catálogo gerado; nunca persistir cópia do catálogo.
- Definir `permissionResource` com `onlyDTO: true`, incluindo `resource`, `description` e `permissions`; não criar DTO manual equivalente.
- Preencher `comment` em entidades/endpoints, pois ele vira a descrição apresentada ao administrador.
- Usar CRUD gerado para `permissionGroup`; usar `handlerAbstract: true` somente se uma operação CRUD padrão exigir sobrescrita.
- Declarar `getPermissionResources` em `endpoints`, implementar a interface gerada em `handlers/permissions` e aplicar CORS como nos handlers gerados.
- Manter catálogo em `config/metadata`, consulta de bloqueios em `services/permissions` e autorização no interceptor existente em `config/interceptor`.
- Persistir somente negações. Tudo ausente no banco é permitido; em múltiplos grupos ativos, qualquer negação prevalece.
- Reler `resources.json` ao listar o catálogo para incorporar recursos novos como permitidos por padrão.
- Liberar `OPTIONS` antes de autenticação/autorização e responder negação com `403` + chave `permission_access_denied`.
- Manter `VIEW` para GET por ID e GET paginado enquanto o gerador não fornecer `VIEW_ALL`; ao introduzir `VIEW_ALL`, atualizar catálogo, migration, resolução de URL, frontend, spec e documentação juntos.
- Consultar `spec/FEATURE_PERMISSION_GROUPS_SPEC.md` antes de alterar o modelo.
