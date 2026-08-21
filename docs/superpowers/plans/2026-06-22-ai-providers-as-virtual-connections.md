# AI Providers as Read-Only Virtual Connections — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface every enabled AI Provider (from `application.yml` or the EE admin UI) as a regular, workflow-selectable connection that cannot be deleted or edited.

**Architecture:** Connections are *projected* on read (never persisted) by a new `AiProviderConnectionRepository`, composed into `ConnectionServiceImpl` — the single chokepoint every connection reader and mutator funnels through. Projected connections carry a negative id encoding `(provider, environment)`, a `managed` flag, and `{token: <apiKey>}` parameters pulled from the `property` table / `application.yml`. No schema change.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JDBC, JUnit 5, Mockito, AssertJ, Gradle (Kotlin DSL); React 19 + TypeScript client with OpenAPI-generated models.

## Global Constraints

- Apache 2.0 license header on every new file under `server/libs/**` and `client/**`; ByteChef Enterprise header + `@version ee` Javadoc on every file under `server/ee/**`. (Spotless picks the header by file content — `@version ee` — not path.)
- Blank line before control statements and after a variable modification that a later statement uses (Java style rules in CLAUDE.md).
- No method-name `_` prefixes; descriptive variable names (no `u`/`o` single letters).
- Run `./gradlew spotlessApply` before every server commit; `cd client && npm run check` before every client commit.
- Persist JDBC enums as INT ordinals; append new enum values at the end.
- Supported providers (fixed): `OPEN_AI, ANTHROPIC, GROQ, MISTRAL, NVIDIA, PERPLEXITY, DEEPSEEK`. **Gemini, Azure, Stability, HuggingFace are excluded.**
- Projected connections are `type = AUTOMATION` only.
- Virtual id formula (fixed): `id = -(provider.getId() * 100 + environmentId)`, `environmentId ∈ {0,1,2}`.

## Source-of-truth signatures (copy verbatim — do not re-derive)

- `Provider` (`server/libs/modules/components/ai/llm/src/main/java/com/bytechef/component/ai/llm/Provider.java`), Gradle `:server:libs:modules:components:ai:llm`. Ctor `(int id, String name, String key, String label)`. Getters `getId()`, `getName()`, `getKey()`, `getLabel()`. Ids: `ANTHROPIC=7, GROQ=9, MISTRAL=11, NVIDIA=12, OPEN_AI=13, PERPLEXITY=16, DEEPSEEK=17`. `getName()` returns `anthropic, groq, mistral, nvidia, openAi, perplexity, deepseek` (these equal the component names).
- `Authorization` (`sdks/backend/java/component-api/.../component/definition/Authorization.java`): `String TOKEN = "token"`; `AuthorizationType.BEARER_TOKEN`.
- `Environment` (`server/libs/platform/platform-configuration/platform-configuration-api/.../configuration/domain/Environment.java`): `DEVELOPMENT(0), STAGING(1), PRODUCTION(2)`.
- `PropertyService` (`platform-configuration-api`): `List<Property> getProperties(List<String> keys, Scope scope, @Nullable Long scopeId, @Nullable Long environmentId)`; `Optional<Property> fetchProperty(String key, Scope scope, @Nullable Long scopeId, @Nullable Long environmentId)`. `Property.Scope.PLATFORM`. `Property.get(String) → Object`, `Property.isEnabled() → boolean`, `Property.getKey() → String`.
- `ApplicationProperties` (`:server:libs:config:app-config`): `getAi().getProvider().getOpenAi()/.getAnthropic()/.getGroq()/.getMistral()/.getNvidia()/.getPerplexity()/.getDeepSeek()`, each `.getApiKey() → String`.
- `ConnectionService` / `ConnectionServiceImpl` (`platform-connection-service`): read methods `getConnection(long)`, `getConnections(PlatformType)`, `getConnections(String, int, PlatformType)`, `getConnections(String, Integer, Long tagId, Long environmentId, PlatformType)`, `getConnections(List<Long>)`; mutators `delete(long)`, `update(long, List<Long>)`, `update(long, String, List<Long>, int)`, `updateConnectionParameters`, `updateConnectionStatus`, `updateVisibility`, `updateCreatedBy`, `updateConnectionCredentialStatus`. Ctor today: `ConnectionServiceImpl(List<CredentialStore>, ConnectionRepository)`.
- `Connection` (`platform-connection-api`): builder + setters `setComponentName`, `setConnectionVersion`, `setAuthorizationType(AuthorizationType)`, `setEnvironmentId(int)`, `setName`, `setParameters(Map)`, `setType(PlatformType)`, `setId(Long)`, `setCreatedBy(String)`, `setVisibility(ConnectionVisibility)`. `PlatformType.AUTOMATION`. Already has a `@Transient boolean credentialStatusUpdated` (precedent for transient fields).
- `ConnectionDTO` (`platform-connection-api`): canonical record (21 fields, order in file); secondary ctor `(boolean active, Map authorizationParameters, String baseUri, Connection connection, Map connectionParameters, List<Tag> tags)`; `Builder`.
- `ConnectionErrorType` (`platform-connection-api/.../exception`): `extends AbstractErrorType`, instances like `CONNECTION_IS_USED = new ConnectionErrorType(100)`; highest existing key `104`.
- `ConnectionFacadeImpl.toConnectionDTO(boolean active, Connection connection, List<Tag> tags)` (`platform-connection-service`) builds the DTO via the secondary ctor.
- Client: REST model `client/src/shared/middleware/automation/configuration/models/Connection.ts` (generated); list row `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx`.

---

### Task 1: `AiProviderConnectionId` codec

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/AiProviderConnectionId.java`
- Test: `server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/domain/AiProviderConnectionIdTest.java`

**Interfaces:**
- Produces: `AiProviderConnectionId.encode(int providerId, int environmentId) → long`; `AiProviderConnectionId.isAiProviderConnectionId(long id) → boolean`; `AiProviderConnectionId.providerId(long id) → int`; `AiProviderConnectionId.environmentId(long id) → int`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiProviderConnectionIdTest {

    @Test
    void testEncodeIsNegativeAndNeverCollidesWithRealIds() {
        long id = AiProviderConnectionId.encode(13, 0);

        assertThat(id).isNegative();
        assertThat(id).isLessThan(-99L);
    }

    @Test
    void testRoundTrip() {
        long id = AiProviderConnectionId.encode(17, 2);

        assertThat(AiProviderConnectionId.isAiProviderConnectionId(id)).isTrue();
        assertThat(AiProviderConnectionId.providerId(id)).isEqualTo(17);
        assertThat(AiProviderConnectionId.environmentId(id)).isEqualTo(2);
    }

    @Test
    void testRealIdsAreNotAiProviderIds() {
        assertThat(AiProviderConnectionId.isAiProviderConnectionId(1050L)).isFalse();
        assertThat(AiProviderConnectionId.isAiProviderConnectionId(0L)).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*AiProviderConnectionIdTest*"`
Expected: FAIL — `AiProviderConnectionId` does not exist (compilation error).

- [ ] **Step 3: Write minimal implementation** (Apache header)

```java
package com.bytechef.platform.connection.domain;

/**
 * Encodes the synthetic identity of an AI-provider-backed virtual connection as a single negative
 * {@code long}. Real connection ids are positive (autoincrement from 1050); a negative id therefore
 * never collides with the {@code connection} table and is reversible to its {@code (provider, environment)}.
 *
 * @author Ivica Cardic
 */
public final class AiProviderConnectionId {

    private static final int ENVIRONMENT_RADIX = 100;

    private AiProviderConnectionId() {
    }

    public static long encode(int providerId, int environmentId) {
        return -((long) providerId * ENVIRONMENT_RADIX + environmentId);
    }

    public static boolean isAiProviderConnectionId(long id) {
        return id < 0;
    }

    public static int providerId(long id) {
        return (int) (-id / ENVIRONMENT_RADIX);
    }

    public static int environmentId(long id) {
        return (int) (-id % ENVIRONMENT_RADIX);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*AiProviderConnectionIdTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/AiProviderConnectionId.java \
        server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/domain/AiProviderConnectionIdTest.java
git commit -m "Add AiProviderConnectionId codec for virtual connection ids"
```

---

### Task 2: `managed` transient flag on `Connection`

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java`
- Test: `server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/domain/ConnectionManagedTest.java`

**Interfaces:**
- Produces: `Connection.isManaged() → boolean` (default `false`), `Connection.setManaged(boolean)`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConnectionManagedTest {

    @Test
    void testManagedDefaultsToFalse() {
        assertThat(new Connection().isManaged()).isFalse();
    }

    @Test
    void testManagedSetter() {
        Connection connection = new Connection();

        connection.setManaged(true);

        assertThat(connection.isManaged()).isTrue();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*ConnectionManagedTest*"`
Expected: FAIL — `isManaged` not defined.

- [ ] **Step 3: Add the field + accessors**

In `Connection.java`, next to the existing `@Transient private boolean credentialStatusUpdated;` (around line 104) add:

```java
    @Transient
    private boolean managed;
```

Add accessors near the other getters/setters:

```java
    public boolean isManaged() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*ConnectionManagedTest*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java \
        server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/domain/ConnectionManagedTest.java
git commit -m "Add transient managed flag to Connection"
```

---

### Task 3: `AiProviderConnectionRepository` interface + read-only error type

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/repository/AiProviderConnectionRepository.java`
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java`

**Interfaces:**
- Produces:
  - `AiProviderConnectionRepository.findById(long id) → Optional<Connection>`
  - `AiProviderConnectionRepository.find(@Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId) → List<Connection>`
  - `AiProviderConnectionRepository.findAllByIdIn(List<Long> ids) → List<Connection>`
  - `ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY`

- [ ] **Step 1: Create the interface** (Apache header)

```java
package com.bytechef.platform.connection.repository;

import com.bytechef.platform.connection.domain.Connection;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Projects enabled AI providers (from the {@code property} table and {@code application.yml}) into read-only
 * {@link Connection} instances. Not a Spring Data repository — every returned connection is built in memory with a
 * negative {@link com.bytechef.platform.connection.domain.AiProviderConnectionId} and {@code managed = true}.
 *
 * @author Ivica Cardic
 */
public interface AiProviderConnectionRepository {

    Optional<Connection> findById(long id);

    List<Connection> find(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId);

    List<Connection> findAllByIdIn(List<Long> ids);
}
```

- [ ] **Step 2: Add the error type**

In `ConnectionErrorType.java`, alongside the existing constants add:

```java
    public static final ConnectionErrorType AI_PROVIDER_CONNECTION_READ_ONLY = new ConnectionErrorType(105);
```

- [ ] **Step 3: Compile**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/repository/AiProviderConnectionRepository.java \
        server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/exception/ConnectionErrorType.java
git commit -m "Add AiProviderConnectionRepository interface and read-only error type"
```

---

### Task 4: New module scaffold + `AiProviderConnectionSource`

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-ai-provider/build.gradle.kts`
- Modify: `settings.gradle.kts` (after line 207, the `platform-connection-service` include)
- Create: `server/libs/platform/platform-connection/platform-connection-ai-provider/src/main/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionSource.java`
- Create: `.../aiprovider/AiProviderConnectionSourceImpl.java`
- Test: `.../src/test/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionSourceImplTest.java`

**Interfaces:**
- Consumes: `Provider`, `PropertyService`, `ApplicationProperties`.
- Produces:
  - `AiProviderConnectionSource.getSupportedProviders() → List<Provider>` (the 7)
  - `AiProviderConnectionSource.isEnabled(Provider provider, int environmentId) → boolean`
  - `AiProviderConnectionSource.getApiKey(Provider provider, int environmentId) → Optional<String>`

- [ ] **Step 1: Register the module in `settings.gradle.kts`**

After the line `include("server:libs:platform:platform-connection:platform-connection-service")` add:

```kotlin
include("server:libs:platform:platform-connection:platform-connection-ai-provider")
```

- [ ] **Step 2: Create `build.gradle.kts`**

```kotlin
dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":sdks:backend:java:component-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:modules:components:ai:llm"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation(project(":server:libs:core:commons:commons-data"))
}
```

- [ ] **Step 3: Write the failing test** (mocks `PropertyService` + `ApplicationProperties`)

```java
package com.bytechef.platform.connection.aiprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiProviderConnectionSourceImplTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Test
    void testSupportedProvidersExcludeGemini() {
        AiProviderConnectionSourceImpl source = new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.getSupportedProviders())
            .containsExactlyInAnyOrder(
                Provider.OPEN_AI, Provider.ANTHROPIC, Provider.GROQ, Provider.MISTRAL, Provider.NVIDIA,
                Provider.PERPLEXITY, Provider.DEEPSEEK)
            .doesNotContain(Provider.VERTEX_GEMINI);
    }

    @Test
    void testEnabledViaPropertyAndApiKeyReturned() {
        Property property = new Property();

        property.setKey(Provider.OPEN_AI.getKey());
        property.setEnabled(true);
        property.setValue(Map.of("apiKey", "sk-prop"));

        when(propertyService.fetchProperty(Provider.OPEN_AI.getKey(), Scope.PLATFORM, null, 0L))
            .thenReturn(java.util.Optional.of(property));

        AiProviderConnectionSourceImpl source = new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.isEnabled(Provider.OPEN_AI, 0)).isTrue();
        assertThat(source.getApiKey(Provider.OPEN_AI, 0)).contains("sk-prop");
    }

    @Test
    void testDisabledWhenNoPropertyAndNoConfigKey() {
        when(propertyService.fetchProperty(Provider.GROQ.getKey(), Scope.PLATFORM, null, 1L))
            .thenReturn(java.util.Optional.empty());
        when(applicationProperties.getAi()).thenReturn(emptyAi());

        AiProviderConnectionSourceImpl source = new AiProviderConnectionSourceImpl(propertyService, applicationProperties);

        assertThat(source.isEnabled(Provider.GROQ, 1)).isFalse();
        assertThat(source.getApiKey(Provider.GROQ, 1)).isEmpty();
    }

    private static ApplicationProperties.Ai emptyAi() {
        ApplicationProperties.Ai ai = new ApplicationProperties.Ai();
        ApplicationProperties.Ai.Provider provider = new ApplicationProperties.Ai.Provider();

        provider.setGroq(new ApplicationProperties.Ai.Provider.Groq());
        ai.setProvider(provider);

        return ai;
    }
}
```

> Note: if `ApplicationProperties.Ai`/`Provider`/`Groq` setters or no-arg constructors are not public, adjust `emptyAi()` to use the real getter chain via a spy, or use `Mockito.RETURNS_DEEP_STUBS` on the `applicationProperties` mock and stub `applicationProperties.getAi().getProvider().getGroq().getApiKey()` to return `null`. Verify the visibility in `ApplicationProperties.java` before writing.

- [ ] **Step 3b: Create the interface** (Apache header)

```java
package com.bytechef.platform.connection.aiprovider;

import com.bytechef.component.ai.llm.Provider;
import java.util.List;
import java.util.Optional;

/**
 * Single source of truth for which AI providers are enabled and what API key each holds, reading the {@code property}
 * table (per-environment) with an {@code application.yml} fallback. Shared by the connection projector and the EE AI
 * Providers facade so the enable/apiKey logic lives in one place.
 *
 * @author Ivica Cardic
 */
public interface AiProviderConnectionSource {

    List<Provider> getSupportedProviders();

    boolean isEnabled(Provider provider, int environmentId);

    Optional<String> getApiKey(Provider provider, int environmentId);
}
```

- [ ] **Step 3c: Create the implementation** (Apache header)

```java
package com.bytechef.platform.connection.aiprovider;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.configuration.domain.Property;
import com.bytechef.platform.configuration.domain.Property.Scope;
import com.bytechef.platform.configuration.service.PropertyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiProviderConnectionSourceImpl implements AiProviderConnectionSource {

    private static final List<Provider> SUPPORTED_PROVIDERS = List.of(
        Provider.OPEN_AI, Provider.ANTHROPIC, Provider.GROQ, Provider.MISTRAL, Provider.NVIDIA, Provider.PERPLEXITY,
        Provider.DEEPSEEK);

    private final PropertyService propertyService;
    private final ApplicationProperties applicationProperties;

    @SuppressFBWarnings("EI2")
    public AiProviderConnectionSourceImpl(
        PropertyService propertyService, ApplicationProperties applicationProperties) {

        this.propertyService = propertyService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public List<Provider> getSupportedProviders() {
        return SUPPORTED_PROVIDERS;
    }

    @Override
    public boolean isEnabled(Provider provider, int environmentId) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environmentId);

        boolean enabledByProperty = property.map(Property::isEnabled)
            .orElse(false);

        return enabledByProperty || getConfigApiKey(provider) != null;
    }

    @Override
    public Optional<String> getApiKey(Provider provider, int environmentId) {
        Optional<Property> property = propertyService.fetchProperty(
            provider.getKey(), Scope.PLATFORM, null, (long) environmentId);

        if (property.isPresent() && property.get()
            .isEnabled()) {

            Object apiKey = property.get()
                .get("apiKey");

            if (apiKey instanceof String stringApiKey && !stringApiKey.isBlank()) {
                return Optional.of(stringApiKey);
            }
        }

        String configApiKey = getConfigApiKey(provider);

        return configApiKey == null ? Optional.empty() : Optional.of(configApiKey);
    }

    private String getConfigApiKey(Provider provider) {
        ApplicationProperties.Ai.Provider configProvider = applicationProperties.getAi()
            .getProvider();

        String apiKey = switch (provider) {
            case OPEN_AI -> configProvider.getOpenAi()
                .getApiKey();
            case ANTHROPIC -> configProvider.getAnthropic()
                .getApiKey();
            case GROQ -> configProvider.getGroq()
                .getApiKey();
            case MISTRAL -> configProvider.getMistral()
                .getApiKey();
            case NVIDIA -> configProvider.getNvidia()
                .getApiKey();
            case PERPLEXITY -> configProvider.getPerplexity()
                .getApiKey();
            case DEEPSEEK -> configProvider.getDeepSeek()
                .getApiKey();
            default -> null;
        };

        return apiKey == null || apiKey.isBlank() ? null : apiKey;
    }
}
```

- [ ] **Step 4: Run the test**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-ai-provider:test --tests "*AiProviderConnectionSourceImplTest*"`
Expected: PASS (adjust `emptyAi()` per the Step-3 note if `ApplicationProperties` inner setters differ).

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add settings.gradle.kts server/libs/platform/platform-connection/platform-connection-ai-provider
git commit -m "Add platform-connection-ai-provider module with AiProviderConnectionSource"
```

---

### Task 5: `AiProviderConnectionRepositoryImpl`

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-ai-provider/src/main/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionRepositoryImpl.java`
- Test: `.../src/test/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionRepositoryImplTest.java`

**Interfaces:**
- Consumes: `AiProviderConnectionSource`, `AiProviderConnectionId`, `Provider`, `Environment`, `Authorization.TOKEN`, `AuthorizationType.BEARER_TOKEN`.
- Produces: a `@Component` implementing `AiProviderConnectionRepository`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.connection.aiprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiProviderConnectionRepositoryImplTest {

    @Mock
    private AiProviderConnectionSource source;

    @Test
    void testFindByIdProjectsEnabledProvider() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(source.isEnabled(Provider.OPEN_AI, 0)).thenReturn(true);
        when(source.getApiKey(Provider.OPEN_AI, 0)).thenReturn(Optional.of("sk-1"));

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);

        Optional<Connection> connection = repository.findById(id);

        assertThat(connection).isPresent();
        assertThat(connection.get().getId()).isEqualTo(id);
        assertThat(connection.get().getComponentName()).isEqualTo("openAi");
        assertThat(connection.get().getConnectionVersion()).isEqualTo(1);
        assertThat(connection.get().getType()).isEqualTo(PlatformType.AUTOMATION);
        assertThat(connection.get().isManaged()).isTrue();
        assertThat(connection.get().<String>getParameter("token")).isEqualTo("sk-1");
    }

    @Test
    void testFindByIdAbsentWhenDisabled() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI));
        when(source.isEnabled(Provider.OPEN_AI, 0)).thenReturn(false);

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        assertThat(repository.findById(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0))).isEmpty();
    }

    @Test
    void testFindFiltersByComponentName() {
        when(source.getSupportedProviders()).thenReturn(List.of(Provider.OPEN_AI, Provider.ANTHROPIC));
        when(source.isEnabled(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(true);
        when(source.getApiKey(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(Optional.of("k"));

        AiProviderConnectionRepositoryImpl repository = new AiProviderConnectionRepositoryImpl(source);

        List<Connection> connections = repository.find("anthropic", null, 0);

        assertThat(connections)
            .extracting(Connection::getComponentName)
            .containsExactly("anthropic");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-ai-provider:test --tests "*AiProviderConnectionRepositoryImplTest*"`
Expected: FAIL — class does not exist.

- [ ] **Step 3: Write the implementation** (Apache header)

```java
package com.bytechef.platform.connection.aiprovider;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class AiProviderConnectionRepositoryImpl implements AiProviderConnectionRepository {

    private static final int CONNECTION_VERSION = 1;

    private final AiProviderConnectionSource source;

    @SuppressFBWarnings("EI2")
    public AiProviderConnectionRepositoryImpl(AiProviderConnectionSource source) {
        this.source = source;
    }

    @Override
    public Optional<Connection> findById(long id) {
        if (!AiProviderConnectionId.isAiProviderConnectionId(id)) {
            return Optional.empty();
        }

        int providerId = AiProviderConnectionId.providerId(id);
        int environmentId = AiProviderConnectionId.environmentId(id);

        return findProvider(providerId)
            .flatMap(provider -> project(provider, environmentId));
    }

    @Override
    public List<Connection> find(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId) {

        if (connectionVersion != null && connectionVersion != CONNECTION_VERSION) {
            return List.of();
        }

        List<Integer> environmentIds = environmentId != null
            ? List.of(environmentId)
            : java.util.Arrays.stream(Environment.values())
                .map(Enum::ordinal)
                .toList();

        List<Connection> connections = new ArrayList<>();

        for (Provider provider : source.getSupportedProviders()) {
            if (componentName != null && !componentName.equals(provider.getName())) {
                continue;
            }

            for (int currentEnvironmentId : environmentIds) {
                project(provider, currentEnvironmentId).ifPresent(connections::add);
            }
        }

        return connections;
    }

    @Override
    public List<Connection> findAllByIdIn(List<Long> ids) {
        List<Connection> connections = new ArrayList<>();

        for (Long id : ids) {
            if (id != null && AiProviderConnectionId.isAiProviderConnectionId(id)) {
                findById(id).ifPresent(connections::add);
            }
        }

        return connections;
    }

    private Optional<Connection> project(Provider provider, int environmentId) {
        if (!source.isEnabled(provider, environmentId)) {
            return Optional.empty();
        }

        Optional<String> apiKey = source.getApiKey(provider, environmentId);

        if (apiKey.isEmpty()) {
            return Optional.empty();
        }

        Connection connection = new Connection();

        connection.setId(AiProviderConnectionId.encode(provider.getId(), environmentId));
        connection.setName(provider.getLabel());
        connection.setComponentName(provider.getName());
        connection.setConnectionVersion(CONNECTION_VERSION);
        connection.setAuthorizationType(AuthorizationType.BEARER_TOKEN);
        connection.setEnvironmentId(environmentId);
        connection.setType(PlatformType.AUTOMATION);
        connection.setCreatedBy("system");
        connection.setVisibility(ConnectionVisibility.WORKSPACE);
        connection.setParameters(Map.of(Authorization.TOKEN, apiKey.get()));
        connection.setManaged(true);

        return Optional.of(connection);
    }

    private Optional<Provider> findProvider(int providerId) {
        return source.getSupportedProviders()
            .stream()
            .filter(provider -> provider.getId() == providerId)
            .findFirst();
    }
}
```

> `setVisibility(WORKSPACE)` is a plain field assignment here because `id` is set but the connection is never persisted; if `setVisibility` rejects the PRIVATE→WORKSPACE transition on a non-null id, set the id *after* visibility, or use `Connection.builder()...visibility(WORKSPACE)`. Verify against `Connection.setVisibility` (transition guard applies only when `id != null`). Simplest: call `setVisibility` before `setId`.

- [ ] **Step 4: Run the test**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-ai-provider:test --tests "*AiProviderConnectionRepositoryImplTest*"`
Expected: PASS. If a visibility transition error occurs, reorder so `setVisibility` precedes `setId`.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-ai-provider/src/main/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionRepositoryImpl.java \
        server/libs/platform/platform-connection/platform-connection-ai-provider/src/test/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionRepositoryImplTest.java
git commit -m "Add AiProviderConnectionRepositoryImpl projecting providers to connections"
```

---

### Task 6: Compose the projector into `ConnectionServiceImpl`

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java`
- Test: `server/libs/platform/platform-connection/platform-connection-service/src/test/java/com/bytechef/platform/connection/service/ConnectionServiceAiProviderTest.java`

**Interfaces:**
- Consumes: `ObjectProvider<AiProviderConnectionRepository>`, `AiProviderConnectionId`, `ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY`.
- Produces: read overloads that merge projected AUTOMATION connections; mutators that throw `ConfigurationException(AI_PROVIDER_CONNECTION_READ_ONLY)` for negative ids.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.connection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bytechef.component.ai.llm.Provider;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceAiProviderTest {

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private AiProviderConnectionRepository aiProviderConnectionRepository;

    @Mock
    private ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider;

    private ConnectionServiceImpl connectionService() {
        // lenient: the delete test rejects before touching the provider, so this stub is unused there
        org.mockito.Mockito.lenient()
            .when(aiProviderConnectionRepositoryProvider.getIfAvailable())
            .thenReturn(aiProviderConnectionRepository);

        return new ConnectionServiceImpl(List.of(), connectionRepository, aiProviderConnectionRepositoryProvider);
    }

    @Test
    void testGetConnectionRoutesVirtualIdToProjector() {
        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);
        Connection projected = new Connection();

        projected.setId(id);
        projected.setComponentName("openAi");

        when(aiProviderConnectionRepository.findById(id)).thenReturn(Optional.of(projected));

        Connection connection = connectionService().getConnection(id);

        assertThat(connection.getComponentName()).isEqualTo("openAi");
    }

    @Test
    void testDeleteRejectsVirtualId() {
        long id = AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0);

        assertThatThrownBy(() -> connectionService().delete(id))
            .isInstanceOf(ConfigurationException.class)
            .hasMessageContaining("AI provider");
    }

    @Test
    void testGetConnectionsByTypeMergesProjected() {
        Connection projected = new Connection();

        projected.setId(AiProviderConnectionId.encode(Provider.OPEN_AI.getId(), 0));
        projected.setComponentName("openAi");
        projected.setName("Open AI");

        when(connectionRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Sort.class)))
            .thenReturn(List.of());
        when(aiProviderConnectionRepository.find(null, null, null)).thenReturn(List.of(projected));

        List<Connection> connections = connectionService().getConnections(PlatformType.AUTOMATION);

        assertThat(connections)
            .extracting(Connection::getComponentName)
            .contains("openAi");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests "*ConnectionServiceAiProviderTest*"`
Expected: FAIL — three-arg constructor does not exist.

- [ ] **Step 3: Modify `ConnectionServiceImpl`**

Add imports:

```java
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import org.springframework.beans.factory.ObjectProvider;
```

Add a field and extend the constructor (keep `@SuppressFBWarnings("EI2")`):

```java
    private final ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(
        List<CredentialStore> credentialStores, ConnectionRepository connectionRepository,
        ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider) {

        this.credentialStores = credentialStores;
        this.connectionRepository = connectionRepository;
        this.aiProviderConnectionRepositoryProvider = aiProviderConnectionRepositoryProvider;
    }
```

Add a helper near the bottom:

```java
    @Nullable
    private AiProviderConnectionRepository aiProviderConnectionRepository() {
        return aiProviderConnectionRepositoryProvider.getIfAvailable();
    }

    private List<Connection> projectedConnections(
        PlatformType type, @Nullable String componentName, @Nullable Integer connectionVersion,
        @Nullable Long environmentId) {

        AiProviderConnectionRepository repository = aiProviderConnectionRepository();

        if (repository == null || type != PlatformType.AUTOMATION) {
            return List.of();
        }

        return repository.find(
            componentName, connectionVersion, environmentId == null ? null : environmentId.intValue());
    }

    private static List<Connection> mergeByName(List<Connection> connections, List<Connection> projected) {
        if (projected.isEmpty()) {
            return connections;
        }

        List<Connection> merged = new java.util.ArrayList<>(connections);

        merged.addAll(projected);
        merged.sort(java.util.Comparator.comparing(Connection::getName, String.CASE_INSENSITIVE_ORDER));

        return merged;
    }
```

Modify `getConnection(long id)`:

```java
    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            AiProviderConnectionRepository repository = aiProviderConnectionRepository();

            if (repository != null) {
                return repository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Connection does not exist for id=" + id));
            }
        }

        return populateParameters(
            OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id));
    }
```

Modify the read overloads to append projected (only `AUTOMATION`):

```java
    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        List<Connection> connections = populateAll(
            CollectionUtils.filter(
                connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type));

        return mergeByName(connections, projectedConnections(type, null, null, null));
    }
```

```java
    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        List<Connection> connections = populateAll(
            connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                componentName, version, type.ordinal()));

        return mergeByName(connections, projectedConnections(type, componentName, version, null));
    }
```

In the 5-arg `getConnections(...)`, after the existing `environmentId` filter and before `return populateAll(...)`, change the return to merge — but exclude projected when `tagId != null`:

```java
        List<Connection> result = populateAll(CollectionUtils.toList(connections));

        if (tagId != null) {
            return result;
        }

        return mergeByName(result, projectedConnections(type, componentName, connectionVersion, environmentId));
```

Modify `getConnections(List<Long> connectionIds)`:

```java
    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        List<Long> realIds = connectionIds.stream()
            .filter(id -> id != null && !AiProviderConnectionId.isAiProviderConnectionId(id))
            .toList();

        List<Connection> connections = populateAll(connectionRepository.findAllByIdIn(realIds));

        AiProviderConnectionRepository repository = aiProviderConnectionRepository();

        if (repository != null) {
            return mergeByName(connections, repository.findAllByIdIn(connectionIds));
        }

        return connections;
    }
```

Add the guard helper and call it at the top of every mutator (`delete`, both `update`, `updateConnectionParameters`, `updateConnectionStatus`, `updateVisibility`, `updateCreatedBy`, `updateConnectionCredentialStatus`):

```java
    private static void rejectIfAiProviderConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            throw new ConfigurationException(
                "Connection id=%s is an AI provider connection and is read-only".formatted(id),
                ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY);
        }
    }
```

For example, `delete` becomes:

```java
    @Override
    public void delete(long id) {
        rejectIfAiProviderConnection(id);

        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        validateOwnerOrAdmin(connection);

        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (!store.isReadOnly()) {
            store.deleteSecret(connection);
        }

        connectionRepository.delete(connection);
    }
```

Add `rejectIfAiProviderConnection(id);` (or `...(connectionId);`) as the first line of each remaining mutator listed above.

- [ ] **Step 4: Run the test**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test --tests "*ConnectionServiceAiProviderTest*"`
Expected: PASS.

- [ ] **Step 5: Fix existing `ConnectionServiceImpl` constructor callers**

Run: `grep -rn "new ConnectionServiceImpl(" server | grep -v "ConnectionServiceAiProviderTest"`
For each production caller (Spring injects `ObjectProvider` automatically, so no change), and for each **test** caller, pass a stub `ObjectProvider` returning `null` (e.g. `() -> null` won't compile — use a Mockito mock whose `getIfAvailable()` returns `null`, or `org.springframework.beans.factory.ObjectProvider` via a small anonymous class). Update them so they compile.

- [ ] **Step 6: Run the full module test suite**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-service:test`
Expected: PASS (including the unchanged-behavior path where the provider yields `null`).

- [ ] **Step 7: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-service
git commit -m "Compose AiProviderConnectionRepository into ConnectionServiceImpl"
```

---

### Task 7: Thread `managed` through `ConnectionDTO` and the facade

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/dto/ConnectionDTO.java`
- Test: `server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/dto/ConnectionDTOManagedTest.java`

**Interfaces:**
- Produces: `ConnectionDTO.managed()` accessor; secondary ctor copies `connection.isManaged()`; builder default `false`.

- [ ] **Step 1: Write the failing test**

```java
package com.bytechef.platform.connection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.connection.domain.Connection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConnectionDTOManagedTest {

    @Test
    void testManagedCopiedFromConnection() {
        Connection connection = new Connection();

        connection.setId(-1300L);
        connection.setComponentName("openAi");
        connection.setName("Open AI");
        connection.setManaged(true);

        ConnectionDTO dto = new ConnectionDTO(false, Map.of(), "https://api.openai.com", connection, Map.of(), List.of());

        assertThat(dto.managed()).isTrue();
    }

    @Test
    void testManagedDefaultsFalseViaBuilder() {
        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("slack")
            .name("Slack")
            .build();

        assertThat(dto.managed()).isFalse();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*ConnectionDTOManagedTest*"`
Expected: FAIL — `managed()` not defined.

- [ ] **Step 3: Add `managed` to the record**

Add `boolean managed` as the **last** component of the canonical record header (after `visibility`):

```java
public record ConnectionDTO(
    boolean active, @Nullable AuthorizationType authorizationType, Map<String, ?> authorizationParameters,
    String baseUri, String componentName, Map<String, ?> connectionParameters, int connectionVersion, String createdBy,
    Instant createdDate, CredentialStatus credentialStatus, @Nullable CredentialStoreType credentialStoreType,
    int environmentId, Long id, String lastModifiedBy, Instant lastModifiedDate, String name, Map<String, ?> parameters,
    ConnectionStatus status, List<Tag> tags, int version, ConnectionVisibility visibility, boolean managed) {
```

Update the secondary ctor's `this(...)` call to pass `connection.isManaged()` as the final argument:

```java
            connection.getStatus(), tags, connection.getVersion(), connection.getVisibility(), connection.isManaged());
```

Update `Builder.build()` to pass `false` as the final argument:

```java
            name, parameters, status, tags, version, visibility, false);
```

- [ ] **Step 4: Fix other canonical-constructor callers**

Run: `grep -rn "new ConnectionDTO(" server | grep -v "ConnectionDTOManagedTest"`
Any call using the **21-arg canonical** constructor must append a `managed` boolean (`false` for all persisted-connection sites). The 6-arg secondary ctor and the builder are already handled.

- [ ] **Step 5: Run the test + module build**

Run: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "*ConnectionDTOManagedTest*"`
Then: `./gradlew :server:libs:platform:platform-connection:platform-connection-api:compileJava :server:libs:platform:platform-connection:platform-connection-service:compileJava`
Expected: PASS / BUILD SUCCESSFUL. (`ConnectionFacadeImpl.toConnectionDTO` uses the secondary ctor, so `managed` flows through automatically.)

- [ ] **Step 6: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/dto/ConnectionDTO.java \
        server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/dto/ConnectionDTOManagedTest.java
# include any other files touched in Step 4
git commit -m "Thread managed flag through ConnectionDTO"
```

---

### Task 8: Expose `managed` on the REST connection model

**Files:**
- Modify: `server/libs/platform/platform-connection/platform-connection-rest/openapi/components/schemas/objects/connection_base.yaml`
- Regenerated (do not hand-edit): `.../automation-configuration-rest-api/generated/.../model/ConnectionModel.java`

**Interfaces:**
- Produces: `ConnectionModel.getManaged()` (Boolean) in the generated automation model; MapStruct auto-maps `managed → managed`.

- [ ] **Step 1: Add the property to the base schema**

In `connection_base.yaml`, under `properties`, add (mirroring the existing read-only fields like `active`):

```yaml
        managed:
          description: "True when this connection is system-managed (e.g. an enabled AI provider) and cannot be edited or deleted."
          type: boolean
          readOnly: true
```

- [ ] **Step 2: Regenerate models + compile**

Run: `./gradlew :server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-api:compileJava :server:libs:platform:platform-connection:platform-connection-rest:compileJava`
Expected: BUILD SUCCESSFUL; the generated `ConnectionModel` now has `managed`.

- [ ] **Step 3: Verify the mapper compiles (no manual mapping needed)**

Run: `grep -rn "managed" server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-api/generated/ | head`
Expected: `managed` appears in the generated model. The MapStruct `WorkspaceConnectionMapper.convert` maps it by name automatically.

- [ ] **Step 4: Commit**

```bash
./gradlew spotlessApply
git add server/libs/platform/platform-connection/platform-connection-rest/openapi/components/schemas/objects/connection_base.yaml
# stage regenerated model files if they are checked in (the repo commits generated/ dirs)
git add server/libs/automation/automation-configuration/automation-configuration-rest/automation-configuration-rest-api/generated
git commit -m "Expose managed flag on the REST connection model"
```

---

### Task 9: Wire the module into the app and add the integration test

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts` (and any EE app that hosts the real `ConnectionServiceImpl`, e.g. `server/ee/apps/connection-app/build.gradle.kts` — verify with the grep below)
- Test: `server/libs/platform/platform-connection/platform-connection-ai-provider/src/test/java/com/bytechef/platform/connection/aiprovider/AiProviderConnectionRepositoryIntTest.java` (optional integration) **or** a focused `ConnectionService` wiring assertion in `server-app`.

- [ ] **Step 1: Find apps that bundle connection-service**

Run: `grep -rln "platform-connection:platform-connection-service" server/apps server/ee/apps`
Add the new module as `implementation(project(":server:libs:platform:platform-connection:platform-connection-ai-provider"))` to each app build file that lists `platform-connection-service` **and is not** a remote-client-only app.

- [ ] **Step 2: Full server build + checks**

Run: `./gradlew spotlessApply check -x :client:check`
Expected: BUILD SUCCESSFUL. This is the gate that catches Spring context-load regressions (`ObjectProvider` wiring, bean duplication, dependency cycles).

> If a dependency cycle appears (`platform-connection-ai-provider` → `ai:llm` → … → `platform-connection`), break it by confirming `ai:llm` does not transitively depend on `platform-connection`; it does not today. If the build reports the new bean is missing where connections are listed, confirm the app from Step 1 includes the module.

- [ ] **Step 3: Commit**

```bash
git add server/apps/server-app/build.gradle.kts
# plus any EE app build files updated in Step 1
git commit -m "Wire platform-connection-ai-provider into apps hosting connection-service"
```

---

### Task 10: De-duplicate `AiProviderFacadeImpl` onto `AiProviderConnectionSource` (EE)

**Files:**
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/src/main/java/com/bytechef/ee/platform/configuration/facade/AiProviderFacadeImpl.java`
- Modify: `server/ee/libs/platform/platform-configuration/platform-configuration-service/build.gradle.kts`

**Interfaces:**
- Consumes: `AiProviderConnectionSource.isEnabled` / `getApiKey`.

- [ ] **Step 1: Add the dependency**

In the EE `platform-configuration-service` `build.gradle.kts`, add:

```kotlin
    implementation(project(":server:libs:platform:platform-connection:platform-connection-ai-provider"))
```

- [ ] **Step 2: Inject and delegate**

In `AiProviderFacadeImpl`, add a constructor parameter `AiProviderConnectionSource aiProviderConnectionSource`, store it, and replace the inline `hasConfigApiKey(provider)` enable-check usage so the catalog's `enabled` calculation reads:

```java
                boolean enabled = aiProviderConnectionSource.isEnabled(provider, environment);
```

Keep `getAiProviders`/`getAiDefaultModel` behavior intact; only the duplicated enable/apiKey logic is delegated. Leave Gemini/Azure handling in the facade unchanged (the catalog still lists all 8 chat providers — the source only governs the 7 token providers, so for VERTEX_GEMINI keep the existing `hasConfigApiKey`/property path inline).

> Keep this change minimal and behavior-preserving. If delegating risks changing catalog output for Gemini/Azure, leave `getAiProviderCatalog` as-is and only adopt the source where the provider is one of the 7. The dedup is a cleanup, not a behavior change.

- [ ] **Step 3: Run EE module tests**

Run: `./gradlew :server:ee:libs:platform:platform-configuration:platform-configuration-service:test`
Expected: PASS (existing `AiProviderFacadeImpl` tests still green).

- [ ] **Step 4: Commit** (EE license header / `@version ee` already present on the modified file)

```bash
./gradlew spotlessApply
git add server/ee/libs/platform/platform-configuration/platform-configuration-service
git commit -m "Delegate AI provider enable/apiKey resolution to shared source"
```

---

### Task 11: Hide edit/delete for managed connections (client)

**Files:**
- Regenerated: `client/src/shared/middleware/automation/configuration/models/Connection.ts` (via codegen)
- Modify: `client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx`

- [ ] **Step 1: Regenerate the client model**

Run: `cd client && npx openapi-generator ... ` — use the repo's existing codegen command. Confirm via:
Run: `grep -n "managed" client/src/shared/middleware/automation/configuration/models/Connection.ts`
Expected: `managed?: boolean;` present (readonly). If the repo regenerates models through a Gradle/openapi task, run that instead; do not hand-edit the generated file.

- [ ] **Step 2: Gate Edit and Delete on `!connection.managed`**

In `ConnectionListItem.tsx`, wrap the Edit `DropdownMenuItem` and the Delete `Tooltip`/`DropdownMenuItem` so they render only when `!connection.managed`, and add a managed branch to the delete tooltip copy. Example:

```tsx
{!connection.managed && (
    <DropdownMenuItem className="dropdown-menu-item" onClick={() => setShowEditDialog(true)}>
        <EditIcon /> Edit
    </DropdownMenuItem>
)}
```

```tsx
{!connection.managed && (
    <Tooltip>
        <TooltipTrigger asChild>
            <div>
                <DropdownMenuItem
                    className={
                        connection.active === true
                            ? 'dropdown-menu-item-destructive-disabled'
                            : 'dropdown-menu-item-destructive'
                    }
                    disabled={connection.active}
                    onClick={() => setShowDeleteDialog(true)}
                >
                    <Trash2Icon /> Delete
                </DropdownMenuItem>
            </div>
        </TooltipTrigger>

        <TooltipContent side="left">
            {connection.active === true
                ? 'Disconnect from all workflows first to enable deletion.'
                : 'Delete the connection permanently.'}
        </TooltipContent>
    </Tooltip>
)}
```

> Keep object keys and imports alphabetically sorted (ESLint `sort-keys` / `sort-import-destructures`). `EditIcon`/`Trash2Icon` already imported.

- [ ] **Step 3: Run client checks**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests pass.

- [ ] **Step 4: Commit**

```bash
cd client && npm run format
git add client/src/pages/automation/connections/components/connection-list/ConnectionListItem.tsx \
        client/src/shared/middleware/automation/configuration/models/Connection.ts
git commit -m "2917 client - Hide edit/delete for managed AI provider connections"
```

---

### Task 12: End-to-end verification

- [ ] **Step 1: Full build**

Run: `./gradlew spotlessApply check` then `cd client && npm run check`
Expected: all green.

- [ ] **Step 2: Manual smoke (optional, requires infra)**

Start infra + server (`docker compose -f server/docker-compose.dev.infra.yml up -d`, `./gradlew -p server/apps/server-app bootRun`), set `OPENAI_API_KEY` (or enable OpenAI in the EE AI Providers UI), then:
- Open the automation Connections page → an "Open AI" connection appears with no Edit/Delete actions.
- Open a workflow, add the OpenAI component → the projected connection is selectable in the connection dropdown.
- `DELETE /api/automation/internal/connections/<negative-id>` → returns the read-only error; the connection still appears.

---

## Risks / assumptions to confirm during execution

- **EE visibility filtering.** Projected connections use `visibility = WORKSPACE` so EE workspace members (not just an owner) see them. If the EE connection-list filter is stricter, confirm a non-admin member sees the projected connection in the integration smoke; adjust the projected visibility if needed.
- **Null audit dates.** Projected connections have `createdDate`/`lastModifiedDate == null`; the generated REST model marks these optional, so JSON serialization is fine. Confirm the list UI tolerates a blank created date.
- **Negative ids.** Grep for any positivity check on connection ids before merge into a release: `grep -rn "id > 0\|id >= 0" server/libs/platform/platform-connection server/libs/automation/automation-configuration`. None should reject a virtual id on the read path.
- **`ApplicationProperties` inner-class visibility** governs the Task 4 test shape (deep-stub vs. constructed); verify before writing that test.
```
