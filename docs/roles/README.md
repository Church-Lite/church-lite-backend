# Regras obrigatórias — Church Lite Backend

Este diretório é a fonte central das regras arquiteturais do backend. Elas devem ser consultadas antes de implementar,
corrigir ou revisar qualquer funcionalidade.

## Regra de retomada de contexto

- Toda nova sessão deve ler este arquivo por completo antes de analisar ou alterar o backend ou o Church Lite Social.
- Não depender da memória de conversas anteriores para recuperar decisões arquiteturais.
- Cruzar estas regras com o `HANDOFF.md` geral e com o handoff específico do projeto em trabalho.
- Quando uma nova regra for acordada com o responsável pelo projeto, registrá-la aqui na mesma sessão.
- Ao encerrar trabalho que mude arquitetura ou contratos, atualizar também os handoffs afetados.

## Estratégia para código existente

- Não refatorar indiscriminadamente todo o legado apenas para adequá-lo às regras.
- Ao implementar uma funcionalidade ou corrigir uma classe existente, adequar ao padrão todas as partes diretamente
  tocadas pelo trabalho.
- Não ampliar silenciosamente o escopo para módulos sem relação com a tarefa atual.
- Preservar alterações locais do usuário e não limpar o worktree para facilitar uma regeneração.

## Gonthera é a fonte dos contratos

O projeto deve ser construído orientado ao contrato: tudo que puder ser representado pelo Gonthera deve ser declarado
nele — entidades, campos, enums, DTOs, repositories, endpoints e mensageria. O código manual apenas implementa as
regras específicas sobre essa base gerada. Assim, uma alteração de modelo ou uma futura geração para outra linguagem
fica concentrada no contrato, reduzindo mudanças espalhadas pelo projeto.

- Entidades, DTOs, enums, endpoints e mensageria compatíveis com o gerador devem ser declarados primeiro em
  `.gonthera/project.json`.
- Todo endpoint HTTP de negócio deve ser gerado pelo Gonthera.
- Controllers manuais apenas implementam as interfaces geradas e delegam a execução.
- É proibido declarar manualmente `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` ou
  `@RequestMapping` em controllers de negócio.
- Não criar DTOs ou enums manuais quando eles fizerem parte de um contrato gerável.
- Depois de alterar o contrato, executar:

```bash
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw gonthera-cli:validate
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw gonthera-cli:generate-sources
```

## Código gerado

- Nunca editar arquivos dentro de `com.smartverse.churchlitebackend_gen`.
- `_gen` é uma saída descartável e pode ser removida e recriada pelo gerador.
- Regras de negócio, autorização, orquestração e customizações ficam em `com.smartverse.churchlitebackend`.
- Depois de regenerar, revisar os contratos produzidos, os recursos de permissão e os consumidores do frontend.

## Controllers

- Controllers implementam interfaces geradas pelo Gonthera.
- Controllers convertem input/output e delegam; não executam regra de negócio nem persistência.
- Não colocar classes `*Models`, DTOs, entidades ou queries em pacotes de controller.
- Separar controllers de contextos de autorização diferentes, como gestão `STAFF` e Portal do Membro `MEMBER`.
- Não obter tenant de body, query string ou identificador enviado pelo navegador quando ele já estiver disponível no JWT.

## Services

- Services concentram regras de negócio, validações, transações e orquestração.
- Services não contêm SQL ou JPQL.
- É proibido usar `JdbcTemplate`, `EntityManager.createQuery`, `EntityManager.createNativeQuery` ou montar consultas por
  concatenação dentro de services.
- Services dependem de repositories para toda leitura ou alteração persistente.
- Operações que alteram múltiplos registros relacionados devem ser atômicas com `@Transactional`.
- Consultas puras devem usar `@Transactional(readOnly = true)` quando fizer sentido.
- Erros de negócio devem manter chaves estáveis por meio de `ServiceException`; não expor detalhes internos do banco.

## Repositories e consultas

- SQL e JPQL pertencem exclusivamente a repositories.
- Consultas customizadas devem ser declaradas com `@Query`.
- Alterações customizadas devem usar `@Modifying` junto de `@Query`.
- Preferir JPQL tipado quando houver entidade mapeada.
- Usar `nativeQuery = true` somente quando a consulta depender de recurso específico do PostgreSQL ou de estrutura ainda
  não representada por uma entidade.
- Não montar SQL dinamicamente com concatenação de strings.
- Não usar `JdbcTemplate` como atalho para evitar a modelagem do repository.
- Consultas de relatório ou projeção devem retornar DTOs/projections tipados, não `Map<String, Object>` ou arrays de
  `Object` quando houver alternativa clara.
- Repositories não devem conter regra de negócio; apenas persistência e consultas.
- Quando o Gonthera já gerar o repository da entidade, reutilizá-lo diretamente ou criar uma extensão dele para
  consultas customizadas; não criar interfaces manuais paralelas estendendo apenas `Repository<Entity, UUID>`.
- Enums gerados pelo contrato devem respeitar o mapeamento persistente produzido pelo gerador; neste projeto JPA usa
  ordinal (`integer`) para campos enum. Migrations não devem criar esses campos como `varchar` sem uma decisão explícita
  e compatível no contrato/gerador.

## Modelagem e migrations

- Alterações estruturais começam no contrato Gonthera quando a estrutura for gerável.
- Toda mudança de banco deve possuir migration Flyway incremental.
- Nunca alterar uma migration já aplicada para corrigir schema ou checksum; criar uma nova migration.
- Conferir os nomes reais produzidos pelo gerador, principalmente `@JoinColumn`, antes de escrever a migration.
- Não usar o SQL completo gerado como substituto de migrations incrementais.
- Preservar dados existentes e garantir idempotência somente onde ela fizer parte da estratégia da migration.

## Multitenancy

- O tenant autenticado vem do JWT e de `TenantContext`.
- Nunca confiar em tenant recebido pelo body ou pelo frontend como autoridade.
- Toda consulta deve respeitar o schema da igreja atual.
- Ao alternar entre tenant e `ADMIN`, restaurar o contexto original em `finally`.
- Antes de trocar schema dentro da mesma transação JPA, executar `flush/clear` quando necessário para evitar reutilização
  de entidades entre schemas.
- Consumers assíncronos devem configurar explicitamente o tenant antes de abrir a operação transacional e limpar ou
  restaurar o contexto ao terminar.

## Autenticação, perfis e permissões

- Church Lite é a autoridade de autenticação e identidade; nenhum serviço replica senha ou JWT.
- Rotas públicas precisam estar declaradas explicitamente e devem usar somente o tenant administrativo quando o fluxo
  exigir resolução inicial da igreja.
- Os perfis `MEMBER` e `STAFF` não podem atravessar seus limites de API.
- O header `XAccessProfile` seleciona o contexto autorizado, mas nunca concede um perfil que o acesso não possua.
- Permissionamento por grupo e limites de assinatura são camadas cumulativas; uma não contorna a outra.
- Toda nova operação administrativa deve entrar no catálogo gerado de recursos e permissões.
- `OPTIONS` e preflight CORS devem continuar liberados antes da autenticação.

## Segurança e privacidade

- Não registrar JWT, senha, hash de senha, CPF, API key ou payload sensível.
- Não expor identidade de doadores ou contribuintes no Portal do Membro.
- Objetos do storage são privados e usam URL temporária.
- Validar tenant e proprietário autorizado antes de liberar uma chave de storage.
- Identificadores enviados pelo frontend nunca substituem o usuário autenticado como fonte de autoria.
- Não copiar credenciais entre Church Lite e Church Lite Social.

## Integrações e mensageria

- Contratos RabbitMQ devem ser declarados no Gonthera quando suportados.
- Publishers e subscribers gerados são estendidos ou consumidos por implementações fora de `_gen`.
- Consumers devem ser idempotentes e registrar inbox quando duplicidade puder causar efeito repetido.
- Eventos devem transportar apenas os dados mínimos necessários.
- Falhas parciais, retry e confirmação após commit devem ser tratados conforme o risco da integração.

## Validação obrigatória

Depois de alterar contrato ou backend:

```bash
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw gonthera-cli:validate
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw gonthera-cli:generate-sources
JAVA_HOME=/home/geovane/.jdks/ms-25.0.3 ./mvnw -DskipTests compile
```

Além disso:

- executar testes proporcionais ao risco quando existirem;
- verificar que nenhum arquivo `_gen` recebeu edição manual;
- verificar que controllers de negócio não possuem mappings manuais;
- verificar que services não contêm SQL, JPQL, `JdbcTemplate` ou criação direta de queries;
- compilar o frontend quando o contrato HTTP ou o envelope de resposta mudar;
- atualizar handoffs quando a arquitetura, o contrato ou o estado funcional mudar.

## Erros que não devem se repetir

- Criar endpoints do Portal do Membro manualmente para preservar uma rota desejada.
- Tratar uma limitação de rota do gerador como autorização para ignorar o contrato Gonthera.
- Colocar modelos HTTP dentro do pacote de controllers.
- Concentrar SQL, composição de resposta, regra de negócio e persistência em um único service.
- Usar `JdbcTemplate` e blocos de SQL em services ou como implementação habitual de repository.
- Criar enums manuais paralelos aos valores persistidos quando o Gonthera puder gerá-los.
- Corrigir somente a aparência das camadas enquanto o contrato continuar fora do gerador.
