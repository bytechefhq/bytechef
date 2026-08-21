# AI Model Catalog Extraction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract the persisted AI model catalog (`ai_gateway_model` → `ai_model`, `AiGatewayModel` → `AiModel`) out of the AI Gateway into a standalone EE module `platform-ai-model-catalog`, then fill `aiProviderCatalog` model labels from the models.dev display names.

**Architecture:** New EE module pair `platform-ai-model-catalog-api`/`-service` under `server/ee/libs/platform/platform-ai/` — the EE twin of the **already-existing CE** `platform-ai-model-catalog` (in-memory models.dev snapshot), same-name CE/EE twin per the `platform-configuration` precedent. The catalog module owns the domain/repository/service and the `ai_model` table; the gateway keeps providers, the reconciler, deployments, tiers, and routing (dependency direction: gateway → catalog, never reverse, per spec D2). Delete-cascade of gateway deployments inverts through a new `AiModelDeleteListener` SPI. P4 labels come from the CE `ModelCatalog` (`CatalogModel.name()`), because `AiModel` has no display-name field and its rows exist only where gateway providers are registered.

**Tech Stack:** Java 25 / Spring Boot 4, Spring Data JDBC, Liquibase, Spring GraphQL, Testcontainers PostgreSQL, React 19 + GraphQL codegen client.

## Global Constraints

- Every file under `server/ee/` gets the ByteChef Enterprise license header (not Apache 2.0) and a `@version ee` Javadoc tag. Spotless enforces the header by the `@version ee` **content**, not the path.
- P1 outcome (verified): `ai_gateway_model` is **unreleased** — absent from the latest release tag `v0.31.3` (= `upstream/stable`, 2026-07-29) and absent from `master`; it exists only on the 0_732 line this branch builds on. Per spec D1 that sanctions **in-place init-changelog edits**.
- Commit prefix: this work has no ticket, so server commits are `- <description>` and client commits are `- client - <description>`.
- Before each commit: `./gradlew spotlessApply` (server) or `cd client && npm run format` (client). Stage only files this task touched.
- Never judge a Gradle run piped into `tail`/`grep`: redirect to a file in the scratchpad dir, check `$?` on its own line, then grep the file for `^> Task .* FAILED`.
- Dependency direction (spec D2): `platform-ai-model-catalog` must not reference any `...ai.gateway...` package or Gradle project. The gateway may depend on the catalog.
- Names deliberately KEPT (gateway-owned model-routing concerns, spec: "The gateway keeps its own model-routing concerns; only the catalog moves"): `AiGatewayModelDeployment*`, `AiGatewayModelTier*`, `AiGatewayModelApiController` (the OpenAI-compatible public REST surface), and the `ai_gateway_model_deployment` table.
- Label semantics (spec D4): `label` = models.dev display name if cataloged, else the component option's label, else the model id. Labels must never be null/blank (GraphQL declares `AiProviderModel.label: String!`).

## Reality corrections (feed these into the spec's "As built" section in Task 6)

1. A **CE** `platform-ai-model-catalog` module already exists (`server/libs/platform/platform-ai/platform-ai-model-catalog`, packages `com.bytechef.platform.ai.model.catalog`) — the in-memory models.dev catalog (`ModelCatalog`, `CatalogModel`). `ai_gateway_model` is the *persisted* catalog the gateway's reconciler populates **from** it. The new EE module is its persisted EE twin.
2. The reconciler (`AiGatewayModelCatalogReconciler*`) iterates **gateway provider registrations** (`AiGatewayProviderService.getEnabledProviders()`) and switches on the gateway's `AiGatewayProviderType` enum. It therefore **stays in the gateway** (renamed `AiModelCatalogReconciler*`); moving it would force a provider SPI the spec never asked for. D2 holds: all dependencies point gateway → catalog.
3. `AiGatewayModelServiceImpl` is today gated on `bytechef.ai.gateway.enabled` — with the gateway off there is **no catalog bean at all**. The moved `AiModelServiceImpl` drops that gate (registers under `@ConditionalOnEEVersion` alone). This is the concrete D2 behavior change.
4. `AiGatewayModelServiceImpl.delete()` cascades into `AiGatewayModelDeploymentService.deleteByModelId()` — a gateway dependency. Inverted via a new `AiModelDeleteListener` SPI in catalog-api, implemented by a gateway-side listener (house pattern: `ProjectBeforeDeleteEventListener`).
5. `AiModel` has **no label/display-name field**, and its rows exist only for registered gateway providers. P4 therefore joins the **CE** `ModelCatalog` (display name = `CatalogModel.name()`), not the moved table. This also means P4 no longer edits "the very catalog P2/P3 move" — the seam-conflict rationale for bundling still holds loosely (same feature area), but the join target differs from the spec text.
6. The spec's `ai-hub-service: 1` reference count is commented-out dead code in `PersonalAgentSaveValidator` (the rename still updates the comments).
7. The `ai_gateway_model.provider_id` NOT NULL FK to `ai_gateway_provider` cannot live in the catalog module's changelog (wrong direction). The catalog creates the column without a constraint; the **gateway's** changelog adds `fk_ai_model_provider` behind a `tableExists ai_model` precondition (mirrors the notification-FK precedent).
8. Spec's "No client change is required" for P4 conflicts with D4's "the id remains as secondary text". D4 wins as a Decision: Task 5 adds a two-line dropdown item to `ModelPicker` (label primary, raw id secondary). Trigger labels stay label-only (no space).

---

### Task 1: EE `platform-ai-model-catalog` module with the moved data layer

The old gateway classes stay in place during this task (deleted in Task 2); both compile side by side, and both tables transiently exist in a fresh schema. Everything is green at the task boundary.

**Files:**
- Modify: `settings.gradle.kts` (two includes, alphabetical among the `server:ee:...platform-ai:*` block — after the `platform-ai-llm-usage` includes, before `platform-ai-observability`)
- Create: `server/ee/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-api/build.gradle.kts`
- Create: `.../platform-ai-model-catalog-api/src/main/java/com/bytechef/ee/platform/ai/model/catalog/domain/AiModel.java`
- Create: `.../platform-ai-model-catalog-api/src/main/java/com/bytechef/ee/platform/ai/model/catalog/domain/package-info.java`
- Create: `.../platform-ai-model-catalog-api/src/main/java/com/bytechef/ee/platform/ai/model/catalog/repository/AiModelRepository.java`
- Create: `.../platform-ai-model-catalog-api/src/main/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelService.java`
- Create: `.../platform-ai-model-catalog-api/src/main/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelDeleteListener.java`
- Create: `server/ee/libs/platform/platform-ai/platform-ai-model-catalog/platform-ai-model-catalog-service/build.gradle.kts`
- Create: `.../platform-ai-model-catalog-service/src/main/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelServiceImpl.java`
- Create: `.../platform-ai-model-catalog-service/src/main/java/com/bytechef/ee/platform/ai/model/catalog/config/AiModelCatalogJdbcRepositoryConfiguration.java`
- Create: `.../platform-ai-model-catalog-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `.../platform-ai-model-catalog-service/src/main/resources/config/liquibase/changelog/platform/ai/model_catalog/00000000000001_ai_model_catalog_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml` (new includeAll before `platform/ai/gateway`)
- Modify: `server/apps/server-app/build.gradle.kts`, `server/ee/apps/ai-gateway-app/build.gradle.kts`
- Test: `.../platform-ai-model-catalog-service/src/test/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelPinningTest.java` (adapted copy of `AiGatewayModelPinningTest`)
- Test: `.../platform-ai-model-catalog-service/src/test/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelServiceDeleteTest.java`
- Test: `.../platform-ai-model-catalog-service/src/test/java/com/bytechef/ee/platform/ai/model/catalog/service/AiModelServiceIntTest.java`
- Test: `.../platform-ai-model-catalog-service/src/test/java/com/bytechef/ee/platform/ai/model/catalog/config/AiModelCatalogIntTestConfiguration.java`
- Test: `.../platform-ai-model-catalog-service/src/test/resources/config/application-testint.yml`

**Interfaces:**
- Consumes: `com.bytechef.ee.platform.ai.llm.usage.Money` (unchanged), `com.bytechef.platform.annotation.ConditionalOnEEVersion`.
- Produces (Task 2+ relies on these exact names): `com.bytechef.ee.platform.ai.model.catalog.domain.AiModel` (same members as `AiGatewayModel`, `@Table("ai_model")`), `...repository.AiModelRepository` (same four finders), `...service.AiModelService` (same methods), `...service.AiModelDeleteListener { void beforeDelete(long modelId); }`.

- [ ] **Step 1: Register the module and write both build files**

`settings.gradle.kts` — add (alphabetical position within the EE `platform-ai` block):

```kotlin
include("server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api")
include("server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service")
```

`platform-ai-model-catalog-api/build.gradle.kts`:

```kotlin
dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
}
```

`platform-ai-model-catalog-service/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
```

- [ ] **Step 2: Copy + adapt the four moved classes**

Copy each source file, then apply exactly these edits (do NOT `git mv` — the originals stay until Task 2):

`AiModel.java` (from `platform-ai-gateway-api/.../domain/AiGatewayModel.java`, 246 lines):
- package → `com.bytechef.ee.platform.ai.model.catalog.domain`
- `@Table("ai_gateway_model")` → `@Table("ai_model")`
- class name, both constructors, the `equals` pattern variable (`aiGatewayModel` → `aiModel`), and the `toString` literal `"AiGatewayModel{"` → `"AiModel{"`
- `isCatalogPinned()` Javadoc `{@link com.bytechef.ee.platform.ai.gateway.service.AiGatewayModelService#unpin(long)}` → `{@link com.bytechef.ee.platform.ai.model.catalog.service.AiModelService#unpin(long)}`
- Everything else (fields, Money accessors, validation) byte-identical.

`AiModelRepository.java` (from `.../repository/AiGatewayModelRepository.java`): package + type renames only; keeps the four finders `findAllByProviderId`, `findAllByEnabled`, `findByProviderIdAndName`, `findFirstByNameOrderByIdAsc`.

`AiModelService.java` (from `.../service/AiGatewayModelService.java`): package + type renames; method set unchanged (`create`, `delete`, `findByModelIdentifier`, `getModel(long)`, `getModel(long, String)`, `getModels`, `getModelsByProviderId`, `getEnabledModels`, `update`, `updateFromCatalog`, `unpin`).

`AiModelServiceImpl.java` (from `platform-ai-gateway-service/.../service/AiGatewayModelServiceImpl.java`): package + type renames PLUS two structural changes:

```java
@Service
@Transactional
@ConditionalOnEEVersion
@SuppressFBWarnings("EI")
class AiModelServiceImpl implements AiModelService {

    private final List<AiModelDeleteListener> aiModelDeleteListeners;
    private final AiModelRepository aiModelRepository;

    AiModelServiceImpl(
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider, AiModelRepository aiModelRepository) {

        this.aiModelDeleteListeners = aiModelDeleteListenerProvider.orderedStream()
            .toList();
        this.aiModelRepository = aiModelRepository;
    }

    @Override
    public void delete(long id) {
        for (AiModelDeleteListener aiModelDeleteListener : aiModelDeleteListeners) {
            aiModelDeleteListener.beforeDelete(id);
        }

        aiModelRepository.deleteById(id);
    }
```

The `@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")` annotation is **dropped, deliberately** — spec D2: the gateway toggle must not gate catalog availability. Add a class Javadoc line stating this. All other methods (`applyAndSave`, `catalogOwnedFieldChanged`, `decimalChanged`, `valueChanged`, `unpin`, the query methods) carry over with type renames only.

- [ ] **Step 3: Write the new SPI, config, package-info**

`AiModelDeleteListener.java`:

```java
package com.bytechef.ee.platform.ai.model.catalog.service;

/**
 * Callback invoked inside {@link AiModelService#delete(long)}'s transaction before the row is removed. Modules that
 * hang their own rows off a model id — the AI Gateway's model deployments, for instance — contribute a bean so the
 * catalog can cascade without depending on them.
 *
 * @version ee
 */
public interface AiModelDeleteListener {

    void beforeDelete(long modelId);
}
```

`AiModelCatalogJdbcRepositoryConfiguration.java` (mirror of `AiGatewayJdbcRepositoryConfiguration`):

```java
package com.bytechef.ee.platform.ai.model.catalog.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 */
@AutoConfiguration(afterName = "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration")
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.ai.model.catalog.repository")
class AiModelCatalogJdbcRepositoryConfiguration {
}
```

`AutoConfiguration.imports` (one line): `com.bytechef.ee.platform.ai.model.catalog.config.AiModelCatalogJdbcRepositoryConfiguration`

`domain/package-info.java`: EE header + short Javadoc — "Domain model for the persisted AI model catalog. {@code AiModel} rows are populated from the models.dev catalog by the AI Gateway's reconciler and consumed by any surface that needs to resolve a model identifier. Spring Data JDBC aggregate; load and persist via the service layer." + `@version ee`.

- [ ] **Step 4: Liquibase — catalog init + master.xml include**

`00000000000001_ai_model_catalog_init.xml` — same XML namespace header as the gateway init, then:

```xml
    <!--
        The persisted AI model catalog, extracted from platform/ai/gateway/00000000000001_ai_gateway_init.xml
        (where the table was named ai_gateway_model). provider_id references ai_gateway_provider, but the FK
        constraint is added by the gateway's own changelog — the catalog must not know the gateway exists, while
        the gateway may know the catalog (see fk_ai_model_provider there).
    -->
    <changeSet id="20260812000001" author="ivicac">
        <preConditions onFail="MARK_RAN">
            <not>
                <tableExists tableName="ai_model"/>
            </not>
        </preConditions>

        <createTable tableName="ai_model">
            <column name="id" type="BIGINT" autoIncrement="true" startWith="1050">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="provider_id" type="BIGINT">
                <constraints nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(256)">
                <constraints nullable="false"/>
            </column>
            <column name="alias" type="VARCHAR(256)"/>
            <column name="context_window" type="INT"/>
            <column name="input_cost_per_m_tokens" type="DECIMAL(10,4)"/>
            <column name="output_cost_per_m_tokens" type="DECIMAL(10,4)"/>
            <column name="capabilities" type="VARCHAR(256)"/>
            <column name="catalog_pinned" type="BOOLEAN" defaultValueBoolean="false">
                <constraints nullable="false"/>
            </column>
            <column name="default_routing_policy_id" type="BIGINT"/>
            <column name="enabled" type="BOOLEAN" defaultValueBoolean="true">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
```

(Column set is byte-identical to the old `ai_gateway_model` createTable minus the inline `foreignKeyName="fk_ai_gateway_model_provider"`.)

`master.xml` — insert immediately BEFORE the `platform/ai/gateway` includeAll (line ~110):

```xml
    <!--
        platform/ai/model_catalog holds ai_model — the persisted AI model catalog, extracted from
        platform/ai/gateway (where it was ai_gateway_model). Loaded BEFORE platform/ai/gateway, whose changesets
        add the provider FK onto ai_model and point ai_gateway_model_deployment.model_id at it.
    -->
    <includeAll path="classpath:config/liquibase/changelog/platform/ai/model_catalog" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or configuration or multitenant" />
```

Do NOT edit the gateway init in this task (Task 2 does).

- [ ] **Step 5: App wiring**

`server/apps/server-app/build.gradle.kts` — next to the existing EE gateway line (~304):

```kotlin
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

`server/ee/apps/ai-gateway-app/build.gradle.kts` — next to line 30:

```kotlin
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

- [ ] **Step 6: Write the failing unit tests**

`AiModelServiceDeleteTest.java` — the genuinely new behavior (listener SPI):

```java
package com.bytechef.ee.platform.ai.model.catalog.service;

// EE header + @version ee; imports: junit, mockito, AiModelRepository, ObjectProvider, Stream, InOrder

class AiModelServiceDeleteTest {

    @Test
    void testDeleteInvokesListenersBeforeRowDelete() {
        AiModelRepository aiModelRepository = mock(AiModelRepository.class);
        AiModelDeleteListener aiModelDeleteListener = mock(AiModelDeleteListener.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider = mock(ObjectProvider.class);

        when(aiModelDeleteListenerProvider.orderedStream()).thenReturn(Stream.of(aiModelDeleteListener));

        AiModelServiceImpl aiModelService = new AiModelServiceImpl(aiModelDeleteListenerProvider, aiModelRepository);

        aiModelService.delete(5L);

        InOrder inOrder = inOrder(aiModelDeleteListener, aiModelRepository);

        inOrder.verify(aiModelDeleteListener).beforeDelete(5L);
        inOrder.verify(aiModelRepository).deleteById(5L);
    }

    @Test
    void testDeleteWithoutListenersStillRemovesRow() {
        AiModelRepository aiModelRepository = mock(AiModelRepository.class);

        @SuppressWarnings("unchecked")
        ObjectProvider<AiModelDeleteListener> aiModelDeleteListenerProvider = mock(ObjectProvider.class);

        when(aiModelDeleteListenerProvider.orderedStream()).thenReturn(Stream.empty());

        new AiModelServiceImpl(aiModelDeleteListenerProvider, aiModelRepository).delete(7L);

        verify(aiModelRepository).deleteById(7L);
    }
}
```

`AiModelPinningTest.java` — copy `platform-ai-gateway-service/src/test/.../service/AiGatewayModelPinningTest.java`, rename types, and replace its `AiGatewayModelDeploymentService` mock with the mocked `ObjectProvider<AiModelDeleteListener>` (stub `orderedStream()` → `Stream.empty()`). Test method names and pinning assertions stay identical — the spec requires this coverage to survive the move intact.

- [ ] **Step 7: Write the schema IntTest (spec Testing bullet 2)**

`AiModelCatalogIntTestConfiguration.java` (mirror `AiEvalDatasetIntTestConfiguration`):

```java
package com.bytechef.ee.platform.ai.model.catalog.config;

// EE header; imports: JacksonConfiguration, LiquibaseConfiguration, AbstractIntTestJdbcConfiguration,
// ComponentScan, Configuration, EnableAutoConfiguration, EnableJdbcAuditing, Import

@ComponentScan(basePackages = "com.bytechef.ee.platform.ai.model.catalog.service")
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AiModelCatalogIntTestConfiguration {

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AiModelCatalogIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
```

`application-testint.yml` — deliberately WITHOUT `bytechef.ai.gateway.enabled`, pinning the D2 availability change:

```yaml
bytechef:
  edition: ee
```

`AiModelServiceIntTest.java`:

```java
package com.bytechef.ee.platform.ai.model.catalog.service;

// EE header + @version ee. Javadoc: "Proves the renamed ai_model table is created from scratch by the module's
// own changelog against a real PostgreSQL via Testcontainers — the liquibase Spring profile applies nothing via
// bootRun, so a fresh-schema IntTest is the real evidence. Runs without bytechef.ai.gateway.enabled, pinning
// that the catalog registers independently of the gateway toggle."

@ActiveProfiles("testint")
@SpringBootTest(classes = AiModelCatalogIntTestConfiguration.class)
@Import(PostgreSQLContainerConfiguration.class)
public class AiModelServiceIntTest {

    @Autowired
    private AiModelService aiModelService;

    @Test
    void testCreateAndFetchModelAgainstFreshSchema() {
        AiModel model = new AiModel(1L, "gpt-5");

        model.setContextWindow(400000);
        model.setInputCostPerMTokens(new BigDecimal("1.2500"));

        AiModel savedModel = aiModelService.create(model);

        assertThat(savedModel.getId()).isNotNull();

        AiModel fetchedModel = aiModelService.getModel(1L, "gpt-5");

        assertThat(fetchedModel.getContextWindow()).isEqualTo(400000);
        assertThat(aiModelService.findByModelIdentifier("gpt-5")).isPresent();
    }

    @Test
    void testDeleteRemovesRow() {
        AiModel savedModel = aiModelService.create(new AiModel(2L, "claude-fable-5"));

        aiModelService.delete(savedModel.getId());

        assertThat(aiModelService.findByModelIdentifier("claude-fable-5")).isEmpty();
    }
}
```

(No FK on `provider_id` exists in this context — the gateway changelog is not on this module's test classpath — so arbitrary provider ids insert cleanly. That asymmetry is the D2 design, not an accident.)

- [ ] **Step 8: Run the module's tests**

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api:compileJava :server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service:test > "$SCRATCH/t1-test.log" 2>&1
echo "exit=$?"
```

Expected: exit=0 (Docker must be running for the IntTest). Then confirm server-app still assembles: `./gradlew :server:apps:server-app:compileJava > "$SCRATCH/t1-app.log" 2>&1; echo "exit=$?"`.

- [ ] **Step 9: Spotless + commit**

```bash
./gradlew spotlessApply
git add settings.gradle.kts server/ee/libs/platform/platform-ai/platform-ai-model-catalog server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml server/apps/server-app/build.gradle.kts server/ee/apps/ai-gateway-app/build.gradle.kts
git commit -m "- Add platform-ai-model-catalog EE module with the persisted AI model catalog"
```

---

### Task 2: Rename `AiGatewayModel` → `AiModel` and repoint every server consumer

One atomic rename across server code, GraphQL schema, and changelogs — splitting it would leave non-compiling task boundaries (the `@BatchMapping(typeName = "AiGatewayModel")` string ties Java to the schema). Old catalog classes are deleted here.

**Files:**
- Delete: `platform-ai-gateway-api/.../domain/AiGatewayModel.java`, `.../repository/AiGatewayModelRepository.java`, `.../service/AiGatewayModelService.java`, `platform-ai-gateway-service/.../service/AiGatewayModelServiceImpl.java`, `platform-ai-gateway-service/src/test/.../service/AiGatewayModelPinningTest.java`
- Rename (git mv): reconciler family (4 prod files + 4 test files), `AiGatewayModelsDevProviderIds(Test)` → `AiModelsDevProviderIds(Test)`, 4 facade files, 2 GraphQL controllers, 2 `.graphqls` files, 4 automation facade test files, 1 GraphQL IntTest
- Create: `platform-ai-gateway-service/.../service/AiGatewayModelDeploymentCleanupListener.java` + test
- Modify (repoint only): every other file from the reference inventory below, the gateway init changelog, three IntTest configurations, `platform-ai-gateway-api/build.gradle.kts`, three `testImplementation` additions

**Interfaces:**
- Consumes: everything Task 1 produced.
- Produces (Tasks 3-4 rely on): GraphQL schema types/fields `AiModel`, `aiModel`, `aiModels`, `aiModelsByProvider`, `createAiModel`, `updateAiModel`, `deleteAiModel`, `unpinAiModel`, `reconcileAiModelCatalog`, `CreateAiModelInput`, `UpdateAiModelInput`, `workspaceAiModels`, `createWorkspaceAiModel`, `deleteWorkspaceAiModel`, `unpinWorkspaceAiModel`, `updateWorkspaceAiModel`, `CreateWorkspaceAiModelInput`; Java types `AiModelFacade`, `WorkspaceAiModelFacade`, `AiModelCatalogReconciler`.

- [ ] **Step 1: TDD the new gateway-side delete listener**

Create `platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelDeploymentCleanupListenerTest.java`:

```java
class AiGatewayModelDeploymentCleanupListenerTest {

    @Test
    void testBeforeDeleteRemovesDeploymentsForModel() {
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService = mock(AiGatewayModelDeploymentService.class);

        new AiGatewayModelDeploymentCleanupListener(aiGatewayModelDeploymentService).beforeDelete(9L);

        verify(aiGatewayModelDeploymentService).deleteByModelId(9L);
    }
}
```

Run it (fails: class missing), then create `AiGatewayModelDeploymentCleanupListener.java` in `platform-ai-gateway-service/.../service/`:

```java
package com.bytechef.ee.platform.ai.gateway.service;

// EE header. Javadoc: "Cascades AI Gateway model deployments when a catalog model row is deleted. Registered as an
// AiModelDeleteListener so the catalog module can cascade without depending on the gateway — the dependency points
// gateway → catalog, per the extraction's design." + @version ee

@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
class AiGatewayModelDeploymentCleanupListener implements AiModelDeleteListener {

    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;

    AiGatewayModelDeploymentCleanupListener(AiGatewayModelDeploymentService aiGatewayModelDeploymentService) {
        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
    }

    @Override
    public void beforeDelete(long modelId) {
        aiGatewayModelDeploymentService.deleteByModelId(modelId);
    }
}
```

Run the test again: passes.

- [ ] **Step 2: Delete the old catalog classes**

```bash
git rm server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/domain/AiGatewayModel.java \
       server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/repository/AiGatewayModelRepository.java \
       server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelService.java \
       server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelServiceImpl.java \
       server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway/service/AiGatewayModelPinningTest.java
```

- [ ] **Step 3: git mv the renamed files**

```bash
GW_API=server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-api/src/main/java/com/bytechef/ee/platform/ai/gateway
GW_SVC=server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/main/java/com/bytechef/ee/platform/ai/gateway
GW_TST=server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/src/test/java/com/bytechef/ee/platform/ai/gateway
AUT=server/ee/libs/automation/automation-ai/automation-ai-gateway

git mv $GW_API/catalog/AiGatewayModelCatalogReconciler.java $GW_API/catalog/AiModelCatalogReconciler.java
git mv $GW_SVC/catalog/AiGatewayModelCatalogReconcilerImpl.java $GW_SVC/catalog/AiModelCatalogReconcilerImpl.java
git mv $GW_SVC/catalog/AiGatewayModelCatalogReconcilerConfiguration.java $GW_SVC/catalog/AiModelCatalogReconcilerConfiguration.java
git mv $GW_SVC/catalog/AiGatewayModelCatalogReconcilerScheduler.java $GW_SVC/catalog/AiModelCatalogReconcilerScheduler.java
git mv $GW_SVC/catalog/AiGatewayModelsDevProviderIds.java $GW_SVC/catalog/AiModelsDevProviderIds.java
git mv $GW_TST/catalog/AiGatewayModelCatalogReconcilerTest.java $GW_TST/catalog/AiModelCatalogReconcilerTest.java
git mv $GW_TST/catalog/AiGatewayModelCatalogReconcilerBundledSnapshotTest.java $GW_TST/catalog/AiModelCatalogReconcilerBundledSnapshotTest.java
git mv $GW_TST/catalog/AiGatewayModelCatalogReconcilerConfigurationTest.java $GW_TST/catalog/AiModelCatalogReconcilerConfigurationTest.java
git mv $GW_TST/catalog/AiGatewayModelsDevProviderIdsTest.java $GW_TST/catalog/AiModelsDevProviderIdsTest.java

git mv $AUT/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacade.java $AUT/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiModelFacade.java
git mv $AUT/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiGatewayModelFacade.java $AUT/automation-ai-gateway-api/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiModelFacade.java
git mv $AUT/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacadeImpl.java $AUT/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/AiModelFacadeImpl.java
git mv $AUT/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiGatewayModelFacadeImpl.java $AUT/automation-ai-gateway-service/src/main/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiModelFacadeImpl.java
git mv $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacadeAsyncProxyTest.java $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/AiModelFacadeAsyncProxyTest.java
git mv $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/AiGatewayModelFacadeAuthorizationTest.java $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/AiModelFacadeAuthorizationTest.java
git mv $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiGatewayModelFacadeAuthorizationTest.java $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiModelFacadeAuthorizationTest.java
git mv $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiGatewayModelFacadeTest.java $AUT/automation-ai-gateway-service/src/test/java/com/bytechef/ee/automation/ai/gateway/facade/WorkspaceAiModelFacadeTest.java
git mv $AUT/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayModelGraphQlController.java $AUT/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiModelGraphQlController.java
git mv $AUT/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/WorkspaceAiGatewayModelGraphQlController.java $AUT/automation-ai-gateway-graphql/src/main/java/com/bytechef/ee/automation/ai/gateway/web/graphql/WorkspaceAiModelGraphQlController.java
git mv $AUT/automation-ai-gateway-graphql/src/test/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiGatewayModelGraphQlControllerIntTest.java $AUT/automation-ai-gateway-graphql/src/test/java/com/bytechef/ee/automation/ai/gateway/web/graphql/AiModelGraphQlControllerIntTest.java
git mv $AUT/automation-ai-gateway-graphql/src/main/resources/graphql/ai-gateway-model.graphqls $AUT/automation-ai-gateway-graphql/src/main/resources/graphql/ai-model.graphqls
git mv $AUT/automation-ai-gateway-graphql/src/main/resources/graphql/workspace-ai-gateway-model.graphqls $AUT/automation-ai-gateway-graphql/src/main/resources/graphql/workspace-ai-model.graphqls
```

- [ ] **Step 4: Mechanical text rename over the affected trees**

Scope = these directories only: `server/ee/libs/platform/platform-ai/platform-ai-gateway`, `server/ee/libs/platform/platform-ai/platform-ai-guardrails`, `server/ee/libs/automation/automation-ai/automation-ai-gateway`, `server/ee/libs/ai/ai-hub/ai-hub-service` (excluding `*/build/*`).

First the three moved-type FQCNs (packages changed), then one unified simple-name rule whose lookahead protects the deliberately-kept names:

```bash
FILES=$(grep -rl 'AiGatewayModel' \
    server/ee/libs/platform/platform-ai/platform-ai-gateway \
    server/ee/libs/platform/platform-ai/platform-ai-guardrails \
    server/ee/libs/automation/automation-ai/automation-ai-gateway \
    server/ee/libs/ai/ai-hub/ai-hub-service \
    --include='*.java' --include='*.graphqls' | grep -v '/build/')

perl -pi -e 's/com\.bytechef\.ee\.platform\.ai\.gateway\.domain\.AiGatewayModel(?!Deployment|Tier)/com.bytechef.ee.platform.ai.model.catalog.domain.AiModel/g' $FILES
perl -pi -e 's/com\.bytechef\.ee\.platform\.ai\.gateway\.repository\.AiGatewayModelRepository/com.bytechef.ee.platform.ai.model.catalog.repository.AiModelRepository/g' $FILES
perl -pi -e 's/com\.bytechef\.ee\.platform\.ai\.gateway\.service\.AiGatewayModelService(?!Impl)/com.bytechef.ee.platform.ai.model.catalog.service.AiModelService/g' $FILES
perl -pi -e 's/([Aa])iGatewayModel(?!Deployment|Tier|ApiController)/${1}iModel/g' $FILES
```

The unified rule intentionally also rewrites: GraphQL type/field/input names inside `.graphqls` files and test text blocks (`aiGatewayModels` → `aiModels`, `reconcileAiGatewayModelCatalog` → `reconcileAiModelCatalog`, `CreateWorkspaceAiGatewayModelInput` → `CreateWorkspaceAiModelInput`), the `@BatchMapping(typeName = "AiGatewayModel")` literal, `AiGatewayModelsDevProviderIds` → `AiModelsDevProviderIds`, camelCase identifiers (`aiGatewayModelService` → `aiModelService`), and the commented-out ai-hub references. It protects `AiGatewayModelDeployment*`, `AiGatewayModelTier*`, `AiGatewayModelApiController` (and the scan-exclusion regex strings naming it), in both cases via the leading `([Aa])`.

- [ ] **Step 5: Manual edits the sed cannot make**

1. GraphQL controller **method names** must match the renamed schema fields. In `AiModelGraphQlController.java` and `WorkspaceAiModelGraphQlController.java`, the sed already renamed identifiers; verify each `@QueryMapping`/`@MutationMapping` method name equals the new schema field (`aiModel`, `aiModels`, `aiModelsByProvider`, `createAiModel`, `updateAiModel`, `deleteAiModel`, `unpinAiModel`, `reconcileAiModelCatalog`, `workspaceAiModels`, `createWorkspaceAiModel`, `updateWorkspaceAiModel`, `deleteWorkspaceAiModel`, `unpinWorkspaceAiModel`). Fix any the sed's lookahead skipped.
2. `platform-ai-gateway-api/.../domain/package-info.java`: the prose list of gateway entities — replace `{@code AiGatewayModel}` (now `{@code AiModel}` after sed) with a sentence noting the persisted model catalog moved to `platform-ai-model-catalog` and the gateway consumes it.
3. `platform-ai-gateway-api/build.gradle.kts`: add

```kotlin
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
```

(`api` scope: `AiModel`/`AiModelService` appear in gateway-api public signatures — reconciler interface, cost calculator, router. Downstream automation modules and guardrails then see the types transitively, so no other build file needs the compile dependency.)

4. `AiModelCatalogReconcilerConfiguration.java`: constructor wiring now references `AiModelService` (sed-handled); confirm the bean method reads cleanly and the class Javadoc still describes gateway-gated reconciliation.

- [ ] **Step 6: Gateway init changelog edits (in place — P1 says unreleased)**

In `platform-ai-gateway-service/src/main/resources/config/liquibase/changelog/platform/ai/gateway/00000000000001_ai_gateway_init.xml`:

1. Delete the whole `<createTable tableName="ai_gateway_model">...</createTable>` block (lines ~60-91) from changeset `20260507000001`.
2. In `<createTable tableName="ai_gateway_model_deployment">`, change the `model_id` column's constraints from `nullable="false" foreignKeyName="fk_ai_llm_gw_deploy_model" references="ai_gateway_model(id)"` to just `nullable="false"`.
3. Append before the closing `</databaseChangeLog>`:

```xml
    <!--
        The persisted model catalog moved to platform/ai/model_catalog as ai_model. These constraints belong to the
        gateway (which knows the catalog; the catalog must not know the gateway): the provider FK onto ai_model and
        the deployment FK pointing at it. tableExists-guarded because module-scoped test contexts may load this
        changelog without the catalog's.
    -->
    <changeSet id="20260812000001" author="ivicac">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="ai_model"/>
        </preConditions>

        <addForeignKeyConstraint constraintName="fk_ai_model_provider" baseTableName="ai_model" baseColumnNames="provider_id" referencedTableName="ai_gateway_provider" referencedColumnNames="id"/>
    </changeSet>

    <changeSet id="20260812000002" author="ivicac">
        <preConditions onFail="MARK_RAN">
            <tableExists tableName="ai_model"/>
        </preConditions>

        <addForeignKeyConstraint constraintName="fk_ai_llm_gw_deploy_model" baseTableName="ai_gateway_model_deployment" baseColumnNames="model_id" referencedTableName="ai_model" referencedColumnNames="id"/>
    </changeSet>
```

4. In `master.xml`, update the `platform/ai/gateway` comment: drop "model" from the table family list and note "ai_model (formerly ai_gateway_model) lives in platform/ai/model_catalog".
5. Delete stale compiled changelog copies so Liquibase can't see old content: `rm -rf server/ee/libs/platform/platform-ai/platform-ai-gateway/platform-ai-gateway-service/build/resources`.

- [ ] **Step 7: Test configurations that boot the real service**

Every test config that component-scans `com.bytechef.ee.platform.ai.gateway` boots `AiGatewayProviderServiceImpl`, which injects the (moved) model service. Enumerate them:

```bash
grep -rln '"com.bytechef.ee.platform.ai.gateway"' server --include='*.java' | grep -v '/build/'
```

For each (known: `AiGatewayIntTestConfiguration` in automation-ai-gateway-service tests, `AiGatewayPublicRestIntTestConfiguration` in automation-ai-gateway-public-rest tests, `AiEvalExperimentPublicRestIntTestConfiguration` in automation-ai-eval-experiment-public-rest tests):
1. Add `"com.bytechef.ee.platform.ai.model.catalog",` to the `@ComponentScan` basePackages array (keep the array's existing order style).
2. Add to that module's `build.gradle.kts`:

```kotlin
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

Note: these contexts set `bytechef.ai.gateway.enabled=true`, and the catalog changelog is now on their classpath, so the FK changesets' `tableExists` preconditions pass and the schema is complete.

- [ ] **Step 8: Verify no strays, compile, run affected tests**

```bash
grep -rn 'AiGatewayModel' server client/src --include='*.java' --include='*.graphqls' --include='*.xml' | grep -v '/build/' | grep -vE 'AiGatewayModelDeployment|AiGatewayModelTier|AiGatewayModelApiController'
```

Expected: only client-side hits (Task 3's scope) — zero server-side hits. Also `grep -rn 'ai_gateway_model' server --include='*.xml' | grep -v '/build/' | grep -v ai_gateway_model_deployment` → zero hits.

```bash
./gradlew compileJava compileTestJava --continue > "$SCRATCH/t2-compile.log" 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' "$SCRATCH/t2-compile.log"
```

Then run the touched modules' tests (single invocation):

```bash
./gradlew :server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service:test \
          :server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-service:test \
          :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service:test \
          :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql:test \
          :server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest:test \
          :server:ee:libs:ai:ai-hub:ai-hub-service:compileJava \
          > "$SCRATCH/t2-test.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 9: Spotless + commit**

```bash
./gradlew spotlessApply
git add -A server/ee server/libs/config/liquibase-config
git commit -m "- Rename AiGatewayModel to AiModel and move the persisted catalog out of the gateway"
```

---

### Task 3: Client — rename GraphQL operations, regenerate, repoint consumers

Two commits, per the spec: operations first, regenerated file + hand-written updates second. After commit 1 the client still compiles (the stale generated file keeps exporting the old hooks).

**Files:**
- Rename: `client/src/graphql/automation/ai-gateway/aiGatewayModels.graphql` → `aiModels.graphql`; `workspaceAiGatewayModels.graphql` → `workspaceAiModels.graphql`
- Regenerate: `client/src/shared/middleware/graphql.ts`, `client/src/shared/middleware/graphql-types.ts`
- Modify: `client/src/pages/automation/ai/gateway/types.ts`, `AiGateway.tsx`, `components/models/AiGatewayModels.tsx`, `components/models/AiGatewayModelDialog.tsx`, `components/models/tests/AiGatewayModelDialog.test.tsx`, `components/playground/AiGatewayPlayground.tsx`, `components/playground/tests/AiGatewayPlayground.test.tsx`

**Interfaces:**
- Consumes: the Task 2 schema names.
- Produces: generated hooks `useWorkspaceAiModelsQuery`, `useCreateWorkspaceAiModelMutation`, `useUpdateWorkspaceAiModelMutation`, `useDeleteWorkspaceAiModelMutation`, `useUnpinWorkspaceAiModelMutation`; generated types `WorkspaceAiModelsQuery`, `CreateWorkspaceAiModelInput`, `UpdateAiModelInput`.

- [ ] **Step 1: Rename the operation documents**

```bash
cd client
git mv src/graphql/automation/ai-gateway/aiGatewayModels.graphql src/graphql/automation/ai-gateway/aiModels.graphql
git mv src/graphql/automation/ai-gateway/workspaceAiGatewayModels.graphql src/graphql/automation/ai-gateway/workspaceAiModels.graphql
perl -pi -e 's/([Aa])iGatewayModel/${1}iModel/g' src/graphql/automation/ai-gateway/aiModels.graphql src/graphql/automation/ai-gateway/workspaceAiModels.graphql
```

This renames every operation (`query aiModels`, `query aiModelsByProvider`, `mutation createAiModel`, `updateAiModel`, `deleteAiModel`, `unpinAiModel`; `query workspaceAiModels`, `mutation createWorkspaceAiModel`, `deleteWorkspaceAiModel`, `unpinWorkspaceAiModel`, `updateWorkspaceAiModel`) and every input type reference. Selection sets are field names only — untouched. `codegen.ts` globs the whole `.graphqls` directory, so no codegen config change is needed.

- [ ] **Step 2: Commit the operations**

```bash
git add src/graphql/automation/ai-gateway
git commit -m "- client - Rename AI gateway model GraphQL operations to AI model"
```

- [ ] **Step 3: Regenerate and repoint consumers**

```bash
npx graphql-codegen
```

Then rename the generated identifiers in the seven consumer files — workspace-scoped hooks/types only, keeping component names (`AiGatewayModels`, `AiGatewayModelDialog` describe the AI Gateway page's Models tab, which still exists):

```bash
perl -pi -e 's/([Ww])orkspaceAiGatewayModel/${1}orkspaceAiModel/g' \
    src/pages/automation/ai/gateway/types.ts \
    src/pages/automation/ai/gateway/AiGateway.tsx \
    src/pages/automation/ai/gateway/components/models/AiGatewayModels.tsx \
    src/pages/automation/ai/gateway/components/models/AiGatewayModelDialog.tsx \
    src/pages/automation/ai/gateway/components/models/tests/AiGatewayModelDialog.test.tsx \
    src/pages/automation/ai/gateway/components/playground/AiGatewayPlayground.tsx \
    src/pages/automation/ai/gateway/components/playground/tests/AiGatewayPlayground.test.tsx
```

Then `grep -n 'AiGatewayModel' <those seven files>` and hand-fix survivors that reference **generated** names (e.g. a local alias built on `CreateAiGatewayModelInput`/`UpdateAiGatewayModelInput` → `...AiModelInput`; any non-workspace generated hooks are unused per the inventory). Local component names, props, and file names stay.

- [ ] **Step 4: Check and commit**

```bash
npm run check > /tmp/t3-check.log 2>&1
echo "exit=$?"
```

Expected: exit=0 (lint + typecheck + tests). Then:

```bash
npm run format
git add src/shared/middleware/graphql.ts src/shared/middleware/graphql-types.ts src/pages/automation/ai/gateway
git commit -m "- client - Regenerate GraphQL client and repoint AI gateway model consumers"
```

---

### Task 4: P4 — fill `aiProviderCatalog` model labels from the models.dev catalog

The join source is the **CE** `ModelCatalog` (see Reality correction 5). Resolved per request via `ObjectProvider` so app variants without the CE catalog on the classpath (and unit tests) degrade to the existing labels instead of failing DI.

**Files:**
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java`
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/build.gradle.kts`
- Modify: `server/ee/apps/configuration-app/build.gradle.kts` (CE catalog api+service so labels work distributed)
- Test: `.../platform-configuration-service/src/test/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeCatalogTest.java` (+ constructor updates in `AiProviderFacadeDefaultModelTest`, `AiProviderFacadeProvidersTest`, `AiProviderFacadeAuthorizationTest`)
- Test: `server/ee/libs/platform/platform-configuration/platform-configuration-graphql/src/test/java/com/bytechef/ee/platform/configuration/web/graphql/AiProviderCatalogGraphQlControllerTest.java`

**Interfaces:**
- Consumes: CE `com.bytechef.platform.ai.model.catalog.ModelCatalog#fetchModel(String providerId, String modelId)` → `Optional<CatalogModel>`; `CatalogModel.name()` (display name); CE `com.bytechef.platform.ai.llm.Provider` enum.
- Produces: `AiProviderCatalogItemDTO.Model.label` always non-blank.

- [ ] **Step 1: Write the failing facade tests**

In `AiProviderFacadeCatalogTest`, clone the harness of `testGetChatProviderCatalogWithModelOptionsYieldsModelsAndSupportsModelByIdFalse` (line ~116 — it builds an `ask` action whose `model` StringProperty carries options; reuse the file's existing fixture helpers). The `setUp` gains the new constructor argument; keep a `@Mock ObjectProvider<ModelCatalog> modelCatalogProvider` with `lenient().when(modelCatalogProvider.getIfAvailable()).thenReturn(null)` as the default so all existing tests pass unchanged. Add:

```java
@Test
void testGetChatProviderCatalogFillsLabelFromModelsDevDisplayName() {
    // fixture: OpenAI provider whose ask action offers option(value "gpt-5", label "gpt-5")
    ModelCatalog modelCatalog = mock(ModelCatalog.class);

    when(modelCatalogProvider.getIfAvailable()).thenReturn(modelCatalog);
    when(modelCatalog.fetchModel("openai", "gpt-5")).thenReturn(Optional.of(catalogModel("gpt-5", "GPT-5")));

    // ...existing catalog invocation...

    assertThat(openAiItem.models()).extracting(AiProviderCatalogItemDTO.Model::label).containsExactly("GPT-5");
}

@Test
void testGetChatProviderCatalogFallsBackToOptionLabelWhenCatalogMisses() {
    ModelCatalog modelCatalog = mock(ModelCatalog.class);

    when(modelCatalogProvider.getIfAvailable()).thenReturn(modelCatalog);
    when(modelCatalog.fetchModel(anyString(), anyString())).thenReturn(Optional.empty());

    // option(value "my-fine-tune", label "My Fine-Tune") → label stays "My Fine-Tune"
}

@Test
void testGetChatProviderCatalogFallsBackToModelIdWhenNoLabelAnywhere() {
    // modelCatalogProvider.getIfAvailable() returns null (default stub);
    // option with null label → label == "my-fine-tune" (the id), never null — the schema declares label: String!
}
```

Fixture helper (record shapes verified against the CE module):

```java
private static CatalogModel catalogModel(String id, String name) {
    return new CatalogModel(
        id, name, null, null, false, false, false, false, false, false, null, null, null,
        CatalogModel.Status.ACTIVE, new Modalities(List.of(Modality.TEXT), List.of(Modality.TEXT)),
        new Limit(null, null, null), null);
}
```

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test --tests '*AiProviderFacadeCatalogTest*' > "$SCRATCH/t4-red.log" 2>&1` — expect compile failure (new constructor arg missing).

- [ ] **Step 2: Implement in `AiProviderFacadeImpl`**

1. Build dep (`platform-configuration-service/build.gradle.kts`):

```kotlin
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
```

2. Constructor: add `ObjectProvider<ModelCatalog> modelCatalogProvider` as the third parameter (after `componentDefinitionService`); store the provider in a field — resolve per call, never in the constructor.
3. At the per-provider assembly (lines ~116-121), wrap the model list:

```java
List<AiProviderCatalogItemDTO.Model> models = labelModels(
    modelCatalogProvider.getIfAvailable(), provider,
    readChatModels(resolveFullComponentDefinition(componentDefinition)));
```

4. New private statics:

```java
private static List<AiProviderCatalogItemDTO.Model> labelModels(
    @Nullable ModelCatalog modelCatalog, Provider provider, List<AiProviderCatalogItemDTO.Model> models) {

    String modelsDevProviderId = resolveModelsDevProviderId(provider);

    return models.stream()
        .map(model -> new AiProviderCatalogItemDTO.Model(
            model.name(), resolveLabel(modelCatalog, modelsDevProviderId, model)))
        .toList();
}

private static String resolveLabel(
    @Nullable ModelCatalog modelCatalog, @Nullable String modelsDevProviderId,
    AiProviderCatalogItemDTO.Model model) {

    if (modelCatalog != null && modelsDevProviderId != null) {
        String displayName = modelCatalog.fetchModel(modelsDevProviderId, model.name())
            .map(CatalogModel::name)
            .orElse(null);

        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
    }

    String label = model.label();

    return label != null && !label.isBlank() ? label : model.name();
}

/**
 * Maps ByteChef AI providers onto models.dev provider ids. Exhaustive with no default arm, deliberately —
 * appending a Provider value breaks compilation here instead of silently shipping unlabeled models. Null means
 * models.dev does not catalog the provider (local Ollama; image-only providers); labels then fall back per D4.
 */
private static @Nullable String resolveModelsDevProviderId(Provider provider) {
    return switch (provider) {
        case ANTHROPIC -> "anthropic";
        case AZURE_OPEN_AI -> "azure";
        case DEEPSEEK -> "deepseek";
        case GROQ -> "groq";
        case HUGGING_FACE -> null;
        case MISTRAL -> "mistral";
        case NVIDIA -> "nvidia";
        case OLLAMA -> null;
        case OPEN_AI -> "openai";
        case PERPLEXITY -> "perplexity";
        case STABILITY -> null;
        case VERTEX_GEMINI -> "google-vertex";
    };
}
```

(All non-null ids verified present in the bundled snapshot `models-dev-api.json`: anthropic, azure, deepseek, groq, mistral, nvidia, openai, perplexity, google-vertex.)

5. `configuration-app/build.gradle.kts` — alongside its EE platform-configuration-service dep:

```kotlin
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
```

- [ ] **Step 3: Run facade tests green, fix siblings**

Update the constructions in `AiProviderFacadeDefaultModelTest`, `AiProviderFacadeProvidersTest`, `AiProviderFacadeAuthorizationTest` (mocked provider, `getIfAvailable()` → null, lenient). Run:

```bash
./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test > "$SCRATCH/t4-green.log" 2>&1
echo "exit=$?"
```

- [ ] **Step 4: Controller test case (spec Testing bullet 3, by letter)**

In `AiProviderCatalogGraphQlControllerTest.testAiProviderCatalogDelegatesToFacade`, extend the fixture item's model list to two entries — `new AiProviderCatalogItemDTO.Model("gpt-5", "GPT-5")` (labelled) and `new AiProviderCatalogItemDTO.Model("my-fine-tune", "my-fine-tune")` (fallback = id) — and add:

```java
assertThat(result.getFirst()
    .models())
        .extracting(AiProviderCatalogItemDTO.Model::label)
        .containsExactly("GPT-5", "my-fine-tune");
```

Run that module's tests; expect pass (the controller is a pass-through — the substantive coverage lives in the facade tests, which the As-built note records).

- [ ] **Step 5: Spotless + commit**

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-configuration server/ee/apps/configuration-app/build.gradle.kts
git commit -m "- Fill AI provider catalog model labels from the models.dev catalog"
```

---

### Task 5: D4 client — raw model id as secondary text in the picker dropdown

Spec D4: "where space allows the id remains as secondary text: it is what API configurations reference." The dropdown item has the space; the compact trigger label does not and stays label-only.

**Files:**
- Modify: `client/src/shared/components/ai/model-picker/ModelPicker.tsx` (~line 352-358)
- Test: `client/src/shared/components/ai/model-picker/ModelPicker.test.tsx`

- [ ] **Step 1: Extend the test first**

The test file's fixture already has `models: [{label: 'GPT-4o', name: 'gpt-4o'}]`. In the existing test that opens the dropdown and asserts the label renders, add an assertion that the raw id is also visible:

```tsx
expect(screen.getByText('GPT-4o')).toBeInTheDocument();
expect(screen.getByText('gpt-4o')).toBeInTheDocument();
```

Run `npm run test -- ModelPicker` — expect the new assertion to fail.

- [ ] **Step 2: Implement the two-line item**

Replace the item body (line ~352-358):

```tsx
{provider.models.map((model) => (
    <DropdownMenuItem
        className="flex flex-col items-start"
        key={model.name}
        onSelect={() => handleSelectModel(provider.key, model.name)}
    >
        <span className="truncate">{model.label || model.name}</span>

        {!!model.label && model.label !== model.name && (
            <span className="truncate text-xs text-muted-foreground">{model.name}</span>
        )}
    </DropdownMenuItem>
))}
```

(Object keys and import order already conform; `twMerge` not needed — classes are static.)

- [ ] **Step 3: Check + commit**

```bash
cd client && npm run check > /tmp/t5-check.log 2>&1
echo "exit=$?"
npm run format
git add src/shared/components/ai/model-picker
git commit -m "- client - Show the raw model id as secondary text in the model picker"
```

---

### Task 6: Dev schema sync, spec As-built notes, full verification

**Files:**
- Modify: `scripts/dev/sync-local-schema-after-collapse.sh`
- Modify: `docs/superpowers/specs/2026-08-12-ai-model-catalog-extraction-design.md` (the "As built" section)

- [ ] **Step 1: Extend the dev sync script**

After the existing ai-observability fixup block (~line 121), before the checksum-clearing section, add:

```bash
# --- 2026-08-12 model-catalog extraction: ai_gateway_model was renamed to ai_model in place. ---
# The old FK constraints are dropped because the edited changelogs re-add them under new ownership
# (fk_ai_model_provider from the gateway changelog; fk_ai_llm_gw_deploy_model re-pointed at ai_model)
# and addForeignKeyConstraint would collide with the renamed table's surviving constraints.
SQL="$SQL
DO \$\$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ai_gateway_model')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ai_model') THEN
        ALTER TABLE ai_gateway_model RENAME TO ai_model;

        ALTER TABLE ai_model DROP CONSTRAINT IF EXISTS fk_ai_gateway_model_provider;

        ALTER TABLE ai_gateway_model_deployment DROP CONSTRAINT IF EXISTS fk_ai_llm_gw_deploy_model;
    END IF;
END
\$\$;"
```

(The script's existing `UPDATE databasechangelog SET md5sum = NULL;` already covers the edited changeset checksums. The unrecorded new changesets then run normally: the catalog init MARK_RANs on `tableExists ai_model`, the FK changesets add the dropped constraints back.)

- [ ] **Step 2: Fill the spec's "As built" section**

Replace the placeholder paragraph under `## As built` with the eight Reality corrections from the top of this plan (rewritten as past-tense findings), plus: the reconciler/`ModelsDev` mapping and `CapabilitiesEncoder` stayed gateway-side; `AiGatewayModelDeployment*`/`AiGatewayModelTier*`/`AiGatewayModelApiController`/`ai_gateway_model_deployment` kept their names; the substantive label tests live in `AiProviderFacadeCatalogTest` because `AiProviderCatalogGraphQlControllerTest` is a pure delegation test; Task 5's picker change implements D4 despite the Design section's "No client change is required".

- [ ] **Step 3: Full verification (spec Testing bullet 4)**

```bash
./gradlew spotlessApply
./gradlew check --continue > "$SCRATCH/final-check.log" 2>&1
echo "exit=$?"
grep '^> Task .* FAILED' "$SCRATCH/final-check.log"
cd client && npm run check > /tmp/final-client.log 2>&1
echo "exit=$?"
```

Both exits must be 0 with no FAILED tasks. Re-run the stray grep from Task 2 Step 8 one last time (now expecting zero client hits too, outside the kept component names `AiGatewayModels`/`AiGatewayModelDialog` and their files).

- [ ] **Step 4: Commit**

```bash
git add scripts/dev/sync-local-schema-after-collapse.sh docs/superpowers/specs/2026-08-12-ai-model-catalog-extraction-design.md
git commit -m "- Extend dev schema sync for the ai_model rename and record the as-built notes"
```
