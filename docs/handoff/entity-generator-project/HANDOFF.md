# Handoff — entity-generator

## Objetivo

`entity-generator` é um gerador de código distribuído como Maven Plugin (`com.potatotech:entity-generator`). Ele lê `properties.json` no diretório em que o Maven/JAR foi executado e gera uma camada de persistência/API para projetos Java ou .NET, além de um script PostgreSQL e metadados de permissões.

Este documento descreve o comportamento observado no código da versão `1.0.0`. Em caso de divergência com o `README.md`, considere este handoff mais próximo da implementação atual.

## Como consumir

Pré-requisitos do gerador:

- JDK 11 ou superior;
- Maven com acesso ao repositório onde o plugin foi publicado;
- um `properties.json` na raiz do serviço consumidor;
- para .NET, uma pasta `static/` já criada na raiz do serviço.

Exemplo de declaração no `pom.xml` do serviço Java:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.potatotech</groupId>
      <artifactId>entity-generator</artifactId>
      <version>1.0.0</version>
    </plugin>
  </plugins>
</build>
```

Execute na mesma pasta do `properties.json`:

```bash
mvn entity-generator:generate-sources
```

O plugin não está associado automaticamente a uma fase do lifecycle. Se a geração precisar ocorrer em todo build, declare uma `execution` com o goal `generate-sources` e uma fase apropriada no serviço consumidor.

Também existe execução como JAR (`Main`), com o mesmo diretório de trabalho e o mesmo arquivo de configuração.

## Configuração mínima segura

As três coleções abaixo devem estar presentes, mesmo vazias. A implementação percorre todas elas sem proteção contra `null`.

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

Valores aceitos em `language`: `JAVA`, `DOTNET` e `NODE` (maiúsculos).

Notas sobre propriedades documentadas anteriormente:

- `defaultTypeId` não existe no modelo atual e é ignorado pelo Gson;
- `events` e `listeners` não existem no modelo atual e não geram código;
- `projectName` é desserializado, mas praticamente não participa da geração; os namespaces/pacotes usam `mainPackage`;
- propriedades desconhecidas são silenciosamente ignoradas.

## Entidades

Exemplo completo:

```json
{
  "comment": "Cadastro de clientes",
  "entityName": "customer",
  "tableName": "customer",
  "classExtends": "",
  "generateDefaultHandlers": true,
  "handlerAbstract": false,
  "onlyDTO": false,
  "entityFields": [
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
  ]
}
```

Contrato efetivo:

- `entityName`: nome base das classes e, quando `tableName` é `null`, da tabela em snake_case;
- `tableName`: nome da tabela. Use `null`/omita para derivar o nome; string vazia não ativa a derivação;
- `comment`: usado na descrição do recurso gerado;
- `entityFields`: precisa conter ao menos um campo e, para geração normal, uma chave com `metadata.key: true`;
- `generateDefaultHandlers`: padrão `true`; controla handler CRUD Java e implementação de handler .NET;
- `handlerAbstract`: afeta apenas o handler Java;
- `onlyDTO`: implementado somente no fluxo Java; evita Entity, converter, repository e handler, mas ainda gera DTO. A entidade continua entrando no SQL gerado;
- `classExtends`: está no modelo, mas não é aplicado pelos geradores atuais.

Contrato dos campos:

- `comment`, `fieldName`, `fieldProperties` e `metadata` devem ser informados;
- `list` envolve o tipo em `List<T>`;
- `fieldProperties.required` e `valueDefault` existem no modelo, mas não controlam de forma consistente o código gerado;
- `metadata.nullable` controla anotações/nullable em partes dos geradores;
- `metadata.key` identifica a PK;
- para tipos que representam outra entidade, `relationShips` deve existir para evitar falhas durante a geração.

Relacionamentos:

```json
"relationShips": {
  "fetchType": "LAZY",
  "relationShip": "ManyToOne",
  "mappedBy": "",
  "bidirectional": false,
  "reference": true
}
```

- `relationShip`: esperado como `OneToOne`, `OneToMany`, `ManyToOne` ou `ManyToMany`;
- `fetchType`: inserido nas anotações Java, normalmente `LAZY` ou `EAGER`;
- `bidirectional: false`: lado dono. Gera `@JoinColumn`, coluna SQL e FK;
- `bidirectional: true`: lado inverso. Gera anotação com `mappedBy`, `cascade = CascadeType.ALL`, `orphanRemoval = true` e não gera coluna SQL;
- `mappedBy`: nome do campo proprietário, especialmente em autorrelacionamentos. Exemplo: `children` usa `mappedBy: "parentCode"`;
- `reference`: usado pelo conversor Java para impedir recursão. Normalmente fica `true` no lado dono/FK;
- em autorrelacionamentos, declare primeiro o campo proprietário/referência e depois o inverso.

Exemplo autorreferenciado `ManyToOne`/`OneToMany`:

```json
{
  "fieldName": "parentCode",
  "list": false,
  "fieldProperties": { "fieldType": "costCenter" },
  "relationShips": {
    "fetchType": "LAZY",
    "relationShip": "ManyToOne",
    "bidirectional": false,
    "reference": true
  }
}
```

```json
{
  "fieldName": "children",
  "list": true,
  "fieldProperties": { "fieldType": "costCenter" },
  "relationShips": {
    "fetchType": "LAZY",
    "relationShip": "OneToMany",
    "mappedBy": "parentCode",
    "bidirectional": true,
    "reference": false
  }
}
```

Saída Java observada:

```java
@JoinColumn(name = "parent_code")
@ManyToOne(fetch = FetchType.LAZY)
private CostCenterEntity parentCode;

@OneToMany(mappedBy = "parentCode", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<CostCenterEntity> children;
```

Saída SQL observada:

```sql
parent_code uuid
ALTER TABLE cost_center ADD CONSTRAINT fk_cost_center_cost_center_parent_code FOREIGN KEY (parent_code) REFERENCES cost_center(id);
```

O converter Java reamarra o lado inverso ao lado dono:

```java
entity.setChildren(childrenDtoConverter.toEntity(dto.children, null));
if (entity.getChildren() != null) entity.getChildren().forEach(e -> e.setParentCode(entity));
```

## Tipos reconhecidos

| Configuração | Java | .NET | PostgreSQL |
|---|---|---|---|
| `uuid` | `UUID` | `Guid?` | `uuid` |
| `string`, `password` | `String` | `string` | `varchar` |
| `datetime` | `LocalDateTime` | `DateTime` | `timestamp` |
| `date` | `LocalDate` | `DateTime` | `date` |
| `int` | `Integer` | `int?` | `integer`/`serial` para PK |
| `integer` | `int` | `int` | `integer`/`serial` para PK |
| `long` | `Long` | `Long` | `integer` |
| `decimal`, `double` | `Double` | `Double` | `numeric` |
| `boolean` | `boolean` | `bool` | `boolean` |
| `byte`, `byte[]`, `inputStream`, `map` | suportados apenas no mapeamento Java | não suportados corretamente | sem mapeamento completo |

Um tipo que não seja primitivo nem enum é tratado como entidade: `FooEntity`/`FooDTO`. Portanto, o nome deve corresponder a um `entityName` configurado. Enums são comparados sem diferenciar maiúsculas/minúsculas.

## Endpoints

```json
{
  "comment": "Consulta clientes",
  "methodName": "listCustomer",
  "httpMethod": "GET",
  "grouper": "customer",
  "metadata": {
    "anonymous": false,
    "input": [],
    "output": [
      {
        "parameterName": "customers",
        "parameterType": "customer",
        "list": true
      }
    ]
  },
  "permissions": {
    "description": "",
    "resource": "",
    "premissions": ["VIEW"],
    "permissionDefault": false
  }
}
```

- `metadata.input` e `metadata.output` devem existir, mesmo vazios;
- use `GET` ou `POST`; outras strings podem produzir código inválido;
- Java gera uma interface por endpoint e ignora `grouper`;
- .NET usa `grouper` para reunir métodos em uma classe `*Primitive`; use `""` quando não houver grupo;
- `metadata.anonymous: true` gera `@Anonymous` no Java ou `[AllowAnonymous]` no .NET;
- um parâmetro de entrada com tipo `requestdata`/`responsedata` ativa os envelopes genéricos e impede a geração da classe Input/Output específica;
- a grafia `premissions` está errada no código, mas é a chave JSON que deve ser usada;
- permissões aceitas: `ALL`, `VIEW`, `CREATE`, `UPDATE`, `DELETE`.

## Enums

```json
{
  "enumName": "Status",
  "values": ["ACTIVE", "INACTIVE"]
}
```

`values` não pode estar vazio, pois o gerador remove a última vírgula assumindo que existe ao menos um item.

## Filtros do CRUD gerado

O `GET /<entityName>` do CRUD Java recebe o filtro no parâmetro de query string `filter` e o entrega a `SpecificationFilter`. A classe converte a expressão em um `Specification<T>` do Spring Data JPA. Isso se parece com uma cláusula de consulta, mas **não aceita JPQL, SQL nem o conjunto completo de operadores do JPA**.

Exemplo de chamada (a biblioteca HTTP deve fazer o URL encoding):

```text
GET /costCenter?size=20&offset=1&filter=description eq matriz and parentCode.id eq 550e8400-e29b-41d4-a716-446655440000
```

Com `URLSearchParams` no frontend:

```javascript
const params = new URLSearchParams({
  size: "20",
  offset: "1",
  filter: "description eq matriz and parentCode.id eq 550e8400-e29b-41d4-a716-446655440000",
  displayFields: "*"
});

fetch(`/costCenter?${params.toString()}`);
```

Sintaxe Java efetivamente implementada:

| Expressão | Efeito observado |
|---|---|
| `field eq value` | Para `String`, `lower(field) like '%value%'`, sem diferenciar maiúsculas/minúsculas. Para `UUID`, igualdade exata. Para enum, tenta igualdade. |
| `relation.field eq value` | Cria joins JPA pelo caminho pontuado; UUID é exato e os demais tipos são tratados como texto parcial. |
| `field isNull` | `field IS NULL`. Também aceita caminho relacionado. |
| `field notNull` | `field IS NOT NULL`. Também aceita caminho relacionado. |
| `expr and expr` | Combina predicados com `AND`. A palavra precisa estar separada por espaços. |
| `expr or expr` | Combina predicados com `OR`. A palavra precisa estar separada por espaços. |

Exemplos seguros:

```text
description eq matriz
id eq 550e8400-e29b-41d4-a716-446655440000
parentCode.id eq 550e8400-e29b-41d4-a716-446655440000
parentCode isNull
parentCode notNull
description eq matriz and parentCode notNull
description eq matriz or description eq filial
```

Regras e limitações importantes do parser Java:

- use os nomes dos atributos Java/JSON em `lowerCamelCase`, não os nomes das colunas SQL;
- não envolva valores em aspas; tudo depois de `eq` até o próximo operador lógico é o valor textual;
- `eq`, `isNull` e `notNull` são sensíveis a maiúsculas/minúsculas e devem ser enviados exatamente assim; `and`/`or` são reconhecidos sem diferenciar caixa;
- `eq` em `String` significa **contém**, não igualdade exata;
- UUID deve ser válido; filtro inválido resulta em HTTP 400 com `Invalid filter: <expressão>`;
- joins são criados com o tipo padrão do JPA, normalmente `INNER JOIN`, portanto relações ausentes podem excluir o registro;
- números, booleanos e datas não têm conversão implementada de forma segura no Java. O caminho genérico aplica `lower`/`like` e pode falhar em tempo de execução; restrinja o frontend a texto, UUID, enum e nulidade até o template ser ampliado;
- não existem atualmente `ne`, `gt`, `gte`, `lt`, `lte`, `in`, `between`, `like` explícito ou `not`;
- parênteses e precedência mista não são analisados de forma confiável. Não misture `and` e `or` na mesma expressão e não gere grupos aninhados;
- valores contendo as palavras ` and ` ou ` or ` não podem ser escapados e serão divididos pelo parser;
- filtro ausente ou vazio não restringe os resultados;
- `size` e `offset` devem ser enviados no CRUD Java. `offset` é baseado em 1 na requisição; internamente é convertido para a página baseada em 0;
- apesar de existir em `RequestData`, `order` é lido pelo handler Java, mas não é aplicado ao `PageRequest` atual;
- `displayFields` controla a projeção do DTO e não participa do filtro.

No .NET, `DynamicFilter` é uma implementação separada: suporta apenas `eq`, `and` ou `or`; texto também usa `Contains` sem diferenciar caixa, UUID é exato, e coleção usa um caminho com `*` (por exemplo, `children*.description eq matriz`). Não há `isNull`/`notNull`, o parser só escolhe um operador lógico por expressão e os nomes das propriedades C# são sensíveis à forma gerada. Portanto, o frontend deve selecionar o dialeto conforme `language`; uma expressão Java não é portável por garantia para .NET.

O CRUD Node atual ignora `filter`: o repository gerado usa somente `size` e `offset` no `findMany` do Prisma.

## Messaging RabbitMQ

Implementado nos fluxos Java, .NET e Node. O contrato atual agrupa RabbitMQ em `messaging.RabbitMq`, com `pub` para publishers e `sub` para subscribers.

```json
{
  "messaging": {
    "RabbitMq": {
      "pub": [
        {
          "name": "notification",
          "queue": "4libert.queue.profile.notification",
          "routingKey": "4libert.routingKey.profile.notification"
        }
      ],
      "sub": [
        {
          "name": "notificationChat",
          "queue": "4libert.queue.chat.notification"
        }
      ]
    }
  }
}
```

Contrato efetivo:

- `name`: nome base da classe gerada. `notification` gera `NotificationPub`; `notificationChat` gera `NotificationChatSub`;
- `className`: opcional. Sobrescreve o nome base quando informado; o sufixo `Pub`/`Sub` é adicionado se faltar;
- `queue`: nome da fila;
- `routingKey`: obrigatório para `pub`; opcional para `sub` no .NET, onde declara também o binding da fila com a exchange;
- a exchange não fica fixa no JSON. O gerador cria uma anotação/atributo de exchange e uma configuração abstrata `RabbitConfig`.
- nenhuma configuração RabbitMQ é gerada se `messaging.RabbitMq` estiver ausente, nulo ou sem canais.

O serviço consumidor deve criar uma configuração concreta fora do diretório `_gen`, por exemplo:

```java
package com.example.service.messaging;

import com.example.service_gen.messaging.RabbitConfig;
import com.example.service_gen.messaging.RabbitExchange;
import org.springframework.context.annotation.Configuration;

@Configuration
@RabbitExchange("4libert.profile")
public class AppRabbitConfig extends RabbitConfig {
}
```

Em .NET, o equivalente usa `RabbitExchangeAttribute`:

```csharp
using ExampleBackend.ExampleBackend_Gen.Messaging;
using Microsoft.Extensions.Configuration;

namespace ExampleBackend.Messaging
{
    [RabbitExchange("4libert.profile")]
    public class AppRabbitConfig : RabbitConfig
    {
        public AppRabbitConfig(IConfiguration configuration) : base(configuration)
        {
        }
    }
}
```

No `Program.cs`, registre a configuração concreta e os publishers gerados:

```csharp
builder.Services.AddSingleton<RabbitConfig, AppRabbitConfig>();
AddRabbitMessaging.AddRabbitMessagingGenerate(builder);
```

Subscribers são gerados como classes abstratas. No Java, o serviço consumidor deve implementar o handler fora de `_gen` e registrá-lo como bean Spring:

```java
package com.example.service.messaging;

import com.example.service_gen.messaging.sub.NotificationChatSub;
import org.springframework.stereotype.Component;

@Component
public class NotificationChatListener extends NotificationChatSub {
    @Override
    protected void onMessage(String message) {
        // tratar mensagem
    }
}
```

No .NET, implemente a classe abstrata e registre o listener concreto como hosted service:

```csharp
builder.Services.AddHostedService<NotificationChatListener>();
```

## Saídas geradas

### Java

O diretório inteiro abaixo é apagado e recriado a cada execução:

```text
src/main/java/<mainPackage convertido em caminho>_gen/
```

São gerados, conforme a configuração:

- `*Entity`, `*DTO`, `*DTOConverter`, `*Repository` e `*Handler`;
- interfaces de endpoint e seus modelos `*Input`/`*Output`;
- abstrações RabbitMQ em `messaging/`, `messaging/pub/` e `messaging/sub/` quando `messaging.RabbitMq` é configurado;
- enums;
- `HandlerBase`, `RestConfig`, `SpecificationFilter`, `RequestData` e `ResponseData`.

Também são sobrescritos:

```text
src/main/resources/properties.json
src/main/resources/resources.json
src/main/resources/postgree.sql
```

O nome `postgree.sql` é o nome efetivamente usado (incluindo a grafia).

### .NET

O diretório inteiro abaixo é apagado e recriado:

```text
<mainPackage com pontos convertidos em barras>_gen/
```

São gerados Entities, DTOs, converters, repositories, handlers/controllers, primitives de endpoint, `CustomDbContext`, registro de DI e classes auxiliares. A pasta `static/` não é criada automaticamente; nela são sobrescritos `properties.json`, `resources.json` e `postgree.sql`.

Quando `messaging.RabbitMq` é configurado, também são gerados:

- `Messaging/RabbitExchangeAttribute.cs`;
- `Messaging/RabbitConfig.cs`;
- `Messaging/AddRabbitMessaging.cs`;
- `Messaging/Pub/RabbitPublisher.cs` e `Messaging/Pub/*Pub.cs`;
- `Messaging/Sub/*Sub.cs`.

### Node

O diretório inteiro abaixo é apagado e recriado:

```text
src/generated/
```

São gerados modelos TypeScript, enums, repositories Prisma-friendly, controllers/rotas Express, contratos de endpoints, `prisma/schema.prisma`, arquivos estáticos (`properties.json`, `resources.json`, `postgree.sql`) e, quando `messaging.RabbitMq` é configurado, abstrações RabbitMQ em `messaging/rabbitmq/`. Os arquivos TypeScript e Prisma usam templates em `src/main/resources/xsd/node/`.

O gerador Node não cria `package.json`, `tsconfig.json` nem migrations. O `schema.prisma` gerado cobre datasource PostgreSQL, generator Prisma Client, enums, models, campos escalares e suporte inicial a relacionamentos; relações complexas podem exigir revisão manual.

## Dependências exigidas pelo código gerado

Java pressupõe, no mínimo:

- Spring Web, Spring Data JPA e Spring Transactions;
- quando `messaging.RabbitMq` for usado: Spring AMQP/RabbitMQ;
- Jakarta Persistence;
- Lombok;
- classes do pacote interno `com.potatotech.authorization` (`TenantContext`, `ServiceException` e, para endpoint anônimo, `@Anonymous`).

.NET pressupõe, no mínimo:

- ASP.NET Core MVC/Authorization;
- Entity Framework Core;
- quando `messaging.RabbitMq` for usado: RabbitMQ.Client, Microsoft.Extensions.Hosting, Microsoft.Extensions.Configuration e Microsoft.Extensions.Logging;
- infraestrutura específica referenciada pelos templates de contexto (`<Projeto>.Config.Database` e `<Projeto>.Config.DatabaseMigration`).

Node pressupõe, no mínimo:

- TypeScript;
- Express;
- Prisma Client (`@prisma/client`);
- quando `messaging.RabbitMq` for usado: `amqplib`.

Os namespaces fixos do template `CustomDbContext` atualmente usam `DataOnBackend.Config.*`, independentemente de `mainPackage`; serviços com outro nome provavelmente precisarão ajustar o arquivo gerado ou o template.

## SQL e permissões

O SQL é direcionado a PostgreSQL. Ele gera tabelas, PKs, FKs e tabelas de junção Many-to-Many. Não há seleção de banco no contrato atual e não há migrations incrementais: o arquivo é recriado por completo.

`resources.json` contém:

- CRUD (`CREATE`, `VIEW`, `UPDATE`, `DELETE`) para cada entidade;
- uma entrada para cada endpoint, usando `permissions` quando configurado.

## Cuidados operacionais

- A geração é destrutiva nos diretórios `_gen`; não coloque código manual neles.
- Execute sempre na raiz correta: a resolução usa `System.getProperty("user.dir")`.
- Mantenha nomes em lower camel case. Vários trechos apenas alteram o primeiro caractere e não sanitizam identificadores.
- Garanta uma única PK por entidade. PK ausente causa `NullPointerException`; múltiplas PKs não têm suporte coerente.
- Listas nulas, metadata ausente e relacionamentos incompletos normalmente causam falhas sem mensagem de validação útil.
- O teste existente executa geração sobre arquivos reais, captura exceções e não possui assertions; ele não garante a validade/compilação do resultado.
- O plugin compila em Java 11, mas o código Java gerado usa Jakarta/Spring 6, o que normalmente implica runtime Java 17 no serviço consumidor.

## Limitações e defeitos conhecidos

- README anterior menciona eventos/listeners; o fluxo atual implementa a abstração RabbitMQ via `messaging.RabbitMq`, não o modelo antigo de `events`/`listeners`.
- `onlyDTO` não é respeitado pelo gerador .NET.
- não existe validação formal do JSON antes de apagar/recriar as saídas;
- o mapeamento de nullable .NET contém condições frágeis e pode produzir tipos inesperados;
- o gerador de endpoint .NET possui verificações inconsistentes entre método e HTTP method;
- alguns templates possuem imports/namespaces específicos da infraestrutura PotatoTech/DataOn;
- arquivos são escritos com `CREATE`, sem `TRUNCATE_EXISTING`; a limpeza do diretório reduz esse risco para fontes, mas arquivos de recursos dependem do `dropFile` anterior;
- a geração SQL assume campos e metadados válidos e pode falhar para entidade vazia ou relacionamento incompleto.

## Checklist para integrar um serviço

1. Fixar uma versão publicada do plugin no `pom.xml` ou pipeline.
2. Criar `properties.json` com `entities`, `endpoints` e `enums`, mesmo quando vazios.
3. Criar `static/` antes de gerar para .NET.
4. Confirmar uma PK e metadata completa em cada entidade.
5. Confirmar ambos os lados e a ordem dos autorrelacionamentos.
6. Executar o gerador em uma branch limpa e revisar todos os arquivos `_gen`, SQL e permissões.
7. Compilar o serviço consumidor; a compilação é a validação efetiva que falta ao gerador.
8. Nunca editar fontes `_gen` manualmente; customizações devem ficar fora deles ou ser incorporadas aos templates desta biblioteca.
