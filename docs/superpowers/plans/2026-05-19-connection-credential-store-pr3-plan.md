# Connection Credential Store PR 3 — AWS Secrets Manager adapter

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Ship an `AwsSecretsManagerConnectionCredentialStore` implementation of the SPI introduced in PR 1, plus all the config/auto-wiring needed for an operator to activate it via `bytechef.connection.credential-store.external.provider=aws-secrets-manager`.

**Architecture:** New EE module `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/` mirroring the existing `file-storage-aws-impl` pattern. An `EnvironmentPostProcessor` translates the ByteChef config property to `spring.cloud.aws.secretsmanager.enabled=true`, which causes Spring Cloud AWS to auto-configure a `SecretsManagerClient`. The adapter uses that client, the project's shared `ObjectMapper` (for JSON serialization), and a Caffeine cache for read-path performance.

**Tech Stack:** Spring Cloud AWS 4.0.2 (already in catalog), AWS SDK v2 `SecretsManagerClient`, Caffeine cache, LocalStack via Testcontainers for integration testing. Reuses existing `bytechef.cloud.aws.*` credentials/region config — no new AWS auth properties.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-design.md](../specs/2026-05-19-connection-credential-store-design.md) (Section "AwsSecretsManagerConnectionCredentialStore (EE)")

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Branch state at PR 3 start:** Feature branch `claude/amazing-brahmagupta-12e13d` at the tip of PR 2 (commit `5d599563b4b`).

---

## Branch hygiene — applies to every task

Every subagent dispatch MUST:
1. Run `git branch --show-current` and confirm `claude/amazing-brahmagupta-12e13d` before any change.
2. NEVER run `git checkout`, `git pull`, `git rebase`, `git fetch`.
3. If anything seems wrong with the branch state, STOP and report — don't try to fix with git operations.

---

## File Structure

**New EE module:** `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/`

**New files:**
| File | Responsibility |
|---|---|
| `<module>/build.gradle.kts` | Module deps |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/AwsSecretsManagerConnectionCredentialStore.java` | The adapter — Spring `@Component` gated by `@ConditionalOnProperty` |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/config/AwsSecretsManagerCredentialStoreConfiguration.java` | `@Configuration` wiring the store bean conditionally |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/boot/AwsSecretsManagerCredentialStoreEnvironmentPostProcessor.java` | Translates `bytechef.*` → `spring.cloud.aws.secretsmanager.enabled=true` |
| `<module>/src/main/resources/META-INF/spring.factories` | Registers the EnvironmentPostProcessor |
| `<module>/src/test/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/AwsSecretsManagerConnectionCredentialStoreIntTest.java` | LocalStack-based int test |
| `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/util/CredentialPathResolver.java` | Shared template resolver (also consumed by PR 4's Vault adapter) |
| `server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/util/CredentialPathResolverTest.java` | Unit test for the template resolver |

**Modified files:**
| File | Change |
|---|---|
| `settings.gradle.kts` | Include the new EE module |
| `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` | Add `Connection.CredentialStore.External` + `Connection.CredentialStore.Cache` + `Connection.CredentialStore.AwsSecretsManager` typed config classes |

---

### Task 1: Add ApplicationProperties.Connection.CredentialStore typed config

**File:** `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
git log --oneline -1        # top is 5d599563b4b 547 Test connectionCredentialStores GraphQL query
```

- [ ] **Step 2: Read ApplicationProperties.java structure**

Run: `grep -n "public static class\|private [A-Z]" server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java | head -40`

Note the existing inner-class pattern (e.g., `FileStorage`, `Encryption`, `Cache`). Identify where alphabetically (by domain) a new `Connection` inner class would go — probably between `Cache` and `Coordinator`. Confirm the exact insertion point.

- [ ] **Step 3: Add the Connection field declaration**

In the outer `ApplicationProperties` class body, alongside other private fields like `private Cache cache = new Cache();`, add:

```java
private Connection connection = new Connection();
```

Add the getter (alongside other getters):

```java
public Connection getConnection() {
    return connection;
}
```

- [ ] **Step 4: Add the Connection inner class**

Add `Connection`, `Connection.CredentialStore`, and sub-classes. Place near other inner classes following the existing alphabetical ordering (likely between `Cache` and `Coordinator` / `DataStorage`):

```java
public static class Connection {

    private CredentialStore credentialStore = new CredentialStore();

    public CredentialStore getCredentialStore() {
        return credentialStore;
    }

    public void setCredentialStore(CredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    public static class CredentialStore {

        private External external = new External();
        private Cache cache = new Cache();
        private String pathTemplate;
        private AwsSecretsManager awsSecretsManager = new AwsSecretsManager();

        public External getExternal() {
            return external;
        }

        public void setExternal(External external) {
            this.external = external;
        }

        public Cache getCache() {
            return cache;
        }

        public void setCache(Cache cache) {
            this.cache = cache;
        }

        public String getPathTemplate() {
            return pathTemplate;
        }

        public void setPathTemplate(String pathTemplate) {
            this.pathTemplate = pathTemplate;
        }

        public AwsSecretsManager getAwsSecretsManager() {
            return awsSecretsManager;
        }

        public void setAwsSecretsManager(AwsSecretsManager awsSecretsManager) {
            this.awsSecretsManager = awsSecretsManager;
        }

        public static class External {

            /** Active external store provider. Unset = database only. */
            private String provider;

            public String getProvider() {
                return provider;
            }

            public void setProvider(String provider) {
                this.provider = provider;
            }
        }

        public static class Cache {

            /** Per-adapter read-path cache TTL. Default 5 minutes. */
            private java.time.Duration ttl = java.time.Duration.ofMinutes(5);

            public java.time.Duration getTtl() {
                return ttl;
            }

            public void setTtl(java.time.Duration ttl) {
                this.ttl = ttl;
            }
        }

        public static class AwsSecretsManager {

            /** When true, refuses write operations (operator IAM policy enforcement). */
            private boolean readOnly;

            public boolean isReadOnly() {
                return readOnly;
            }

            public void setReadOnly(boolean readOnly) {
                this.readOnly = readOnly;
            }
        }
    }
}
```

**NOTE:** Replace `java.time.Duration` with a proper `import` at the top of the file if no other import already brings it in.

- [ ] **Step 5: Compile**

```bash
./gradlew :server:libs:config:app-config:compileJava
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Module check**

```bash
./gradlew :server:libs:config:app-config:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "$(cat <<'EOF'
547 Add ApplicationProperties.Connection.CredentialStore config

Typed config for bytechef.connection.credential-store.* properties:
- external.provider (selects which adapter to activate)
- cache.ttl (read-path cache duration, default PT5M)
- path-template (operator-overridable secret path template)
- aws-secrets-manager.read-only (per-adapter read-only flag)

Foundation for PR 3 (AWS Secrets Manager adapter); PR 4 will add a
hashicorp-vault sibling here. Setters in place even where the property
is consumed by a not-yet-existing bean — typed binding fails fast at
startup if misconfigured.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Add the CredentialPathResolver utility + unit test

**Files:**
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/util/CredentialPathResolver.java`
- Create: `server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/util/CredentialPathResolverTest.java`

This utility is shared by AWS Secrets Manager (PR 3) and HashiCorp Vault (PR 4). Adding it in PR 3 because PR 3 is its first consumer.

- [ ] **Step 1: Write the unit test first (TDD)**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CredentialPathResolverTest {

    @Test
    void testResolveSimpleTemplate() {
        String result = CredentialPathResolver.resolve("bytechef/{ref}", null, null, "abc-123");

        assertThat(result).isEqualTo("bytechef/abc-123");
    }

    @Test
    void testResolveWithTenantAndEnvironment() {
        String result = CredentialPathResolver.resolve(
            "bytechef/{tenant}/{env}/{ref}", "acme-corp", "production", "abc-123");

        assertThat(result).isEqualTo("bytechef/acme-corp/production/abc-123");
    }

    @Test
    void testResolveWithMissingTenantPlaceholderThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{tenant}/{ref}", null, null, "abc-123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("tenant");
    }

    @Test
    void testResolveWithMissingEnvironmentPlaceholderThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{env}/{ref}", "acme", null, "abc-123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("env");
    }

    @Test
    void testResolveWithMissingRefThrows() {
        assertThatThrownBy(() -> CredentialPathResolver.resolve("bytechef/{ref}", null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ref");
    }

    @Test
    void testResolveLeavesNonTemplateLiteralsUnchanged() {
        String result = CredentialPathResolver.resolve("secret/data/bytechef/connections/{ref}", null, null, "uuid");

        assertThat(result).isEqualTo("secret/data/bytechef/connections/uuid");
    }
}
```

- [ ] **Step 2: Run test to verify failure**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "CredentialPathResolverTest"
```
Expected: COMPILE FAILURE — `CredentialPathResolver` not found.

- [ ] **Step 3: Write the implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.connection.util;

import org.jspecify.annotations.Nullable;

/**
 * Resolves a path template against context variables. Used by external credential store adapters to compute the
 * secret name / vault path from operator-configured templates.
 *
 * <p>Supported placeholders:
 * <ul>
 *   <li>{@code {tenant}} — current tenant identifier</li>
 *   <li>{@code {env}} — connection environment (production / staging / development), lowercased</li>
 *   <li>{@code {ref}} — value of {@code connection.credentialRef}</li>
 * </ul>
 *
 * <p>Throws {@link IllegalArgumentException} if a referenced placeholder has no value supplied.
 *
 * @author Ivica Cardic
 */
public final class CredentialPathResolver {

    private CredentialPathResolver() {
    }

    public static String resolve(
        String template, @Nullable String tenant, @Nullable String environment, @Nullable String ref) {

        String result = template;

        if (template.contains("{tenant}")) {
            if (tenant == null) {
                throw new IllegalArgumentException(
                    "Path template references {tenant} but no tenant is available");
            }

            result = result.replace("{tenant}", tenant);
        }

        if (template.contains("{env}")) {
            if (environment == null) {
                throw new IllegalArgumentException(
                    "Path template references {env} but no environment is available");
            }

            result = result.replace("{env}", environment);
        }

        if (template.contains("{ref}")) {
            if (ref == null) {
                throw new IllegalArgumentException(
                    "Path template references {ref} but no credentialRef is available");
            }

            result = result.replace("{ref}", ref);
        }

        return result;
    }
}
```

- [ ] **Step 4: Run test to verify pass**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-api:test --tests "CredentialPathResolverTest"
```
Expected: BUILD SUCCESSFUL, 6 tests pass.

- [ ] **Step 5: Module check**

```bash
./gradlew :server:libs:platform:platform-connection:platform-connection-api:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add \
  server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/util/CredentialPathResolver.java \
  server/libs/platform/platform-connection/platform-connection-api/src/test/java/com/bytechef/platform/connection/util/CredentialPathResolverTest.java
git commit -m "$(cat <<'EOF'
547 Add CredentialPathResolver template utility

Shared helper for external credential store adapters to resolve
operator-configured path templates (e.g. "bytechef/{tenant}/{env}/{ref}")
against runtime context. Fail-fast on missing placeholders so config
errors surface early.

Consumed by the AWS Secrets Manager adapter (PR 3) and the HashiCorp
Vault adapter (PR 4). Living in platform-connection-api so both EE
modules can import it without circular dependencies.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Scaffold the EE module

**Directory:** `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/`

- [ ] **Step 1: Create directory skeleton**

```bash
ROOT=server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/boot
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/config
mkdir -p $ROOT/src/main/resources/META-INF
mkdir -p $ROOT/src/test/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager
```

- [ ] **Step 2: build.gradle.kts**

```kotlin
dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.awspring.cloud:spring-cloud-aws-secrets-manager")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:secretsmanager")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))

    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
```

- [ ] **Step 3: settings.gradle.kts entry**

Find the existing EE platform-connection includes (around line 641):
```
include("server:ee:libs:platform:platform-connection:platform-connection-remote-client")
include("server:ee:libs:platform:platform-connection:platform-connection-remote-rest")
```

Insert before them (alphabetical):
```kotlin
include("server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager")
```

- [ ] **Step 4: Verify Gradle recognizes the module**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:tasks 2>&1 | tail -3
```
Expected: lists tasks. No commit yet — content fills tasks 4-6.

---

### Task 4: EnvironmentPostProcessor

**File:** `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/boot/AwsSecretsManagerCredentialStoreEnvironmentPostProcessor.java`

- [ ] **Step 1: Inspect the reference implementation**

```bash
cat server/ee/libs/core/file-storage/file-storage-aws/file-storage-aws-impl/src/main/java/com/bytechef/ee/file/storage/aws/boot/AwsFileStorageEnvironmentPostProcessor.java
```

Match its license header (EE license) and class structure.

- [ ] **Step 2: Write the EnvironmentPostProcessor**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Translates {@code bytechef.connection.credential-store.external.provider=aws-secrets-manager} into
 * {@code spring.cloud.aws.secretsmanager.enabled=true} so that Spring Cloud AWS auto-configures a
 * {@code SecretsManagerClient}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsSecretsManagerCredentialStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        String provider = environment.getProperty(
            "bytechef.connection.credential-store.external.provider", String.class);

        if (Objects.equals(provider, "aws-secrets-manager")) {
            source.put("spring.cloud.aws.secretsmanager.enabled", true);
        }

        MapPropertySource mapPropertySource = new MapPropertySource(
            "Custom Spring Cloud AWS Secrets Manager Credential Store Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }
}
```

- [ ] **Step 3: Register via spring.factories**

Create `<module>/src/main/resources/META-INF/spring.factories`:

```
org.springframework.boot.env.EnvironmentPostProcessor=\
com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.boot.AwsSecretsManagerCredentialStoreEnvironmentPostProcessor
```

- [ ] **Step 4: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:compileJava
```
Expected: BUILD SUCCESSFUL.

(No commit yet — combine with tasks 5+6.)

---

### Task 5: AwsSecretsManagerConnectionCredentialStore + Configuration

**Files:**
- `<module>/src/main/java/.../AwsSecretsManagerConnectionCredentialStore.java`
- `<module>/src/main/java/.../config/AwsSecretsManagerCredentialStoreConfiguration.java`

- [ ] **Step 1: Write the store implementation**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionCredentialStore;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import com.bytechef.platform.connection.util.CredentialPathResolver;
import com.bytechef.tenant.TenantContext;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DeleteSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;
import tools.jackson.databind.ObjectMapper;

/**
 * AWS Secrets Manager-backed {@link ConnectionCredentialStore}. Writes the credential payload as a JSON-serialized
 * map under a path derived from the operator-configured template (default {@code "bytechef/{ref}"}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsSecretsManagerConnectionCredentialStore implements ConnectionCredentialStore {

    private static final Logger logger = LoggerFactory.getLogger(AwsSecretsManagerConnectionCredentialStore.class);

    private static final String DEFAULT_PATH_TEMPLATE = "bytechef/{ref}";

    private final Cache<String, Map<String, Object>> cache;
    private final ObjectMapper objectMapper;
    private final String pathTemplate;
    private final boolean readOnly;
    private final SecretsManagerClient secretsManagerClient;

    @SuppressFBWarnings("EI2")
    public AwsSecretsManagerConnectionCredentialStore(
        ApplicationProperties applicationProperties, ObjectMapper objectMapper,
        SecretsManagerClient secretsManagerClient) {

        ApplicationProperties.Connection.CredentialStore credentialStore = applicationProperties.getConnection()
            .getCredentialStore();

        String configuredTemplate = credentialStore.getPathTemplate();

        this.pathTemplate = configuredTemplate != null ? configuredTemplate : DEFAULT_PATH_TEMPLATE;
        this.readOnly = credentialStore.getAwsSecretsManager()
            .isReadOnly();
        this.objectMapper = objectMapper;
        this.secretsManagerClient = secretsManagerClient;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(credentialStore.getCache()
                .getTtl())
            .build();
    }

    @Override
    public ConnectionCredentialStoreType getType() {
        return ConnectionCredentialStoreType.AWS_SECRETS_MANAGER;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public Map<String, ?> getParameters(Connection connection) {
        String ref = connection.getCredentialRef();

        if (ref == null) {
            return Map.of();
        }

        String secretName = resolvePath(ref);

        return cache.get(secretName, key -> fetchSecret(key));
    }

    @Override
    public void storeParameters(Connection connection, Map<String, ?> parameters) {
        if (readOnly) {
            throw new UnsupportedOperationException("AWS Secrets Manager store is configured read-only");
        }

        String ref = connection.getCredentialRef();
        boolean isNewSecret = ref == null;

        if (isNewSecret) {
            ref = UUID.randomUUID()
                .toString();

            connection.setCredentialRef(ref);
        }

        String secretName = resolvePath(ref);
        String secretJson = serialize(parameters);

        if (isNewSecret) {
            secretsManagerClient.createSecret(
                CreateSecretRequest.builder()
                    .name(secretName)
                    .secretString(secretJson)
                    .build());
        } else {
            secretsManagerClient.putSecretValue(
                PutSecretValueRequest.builder()
                    .secretId(secretName)
                    .secretString(secretJson)
                    .build());
        }

        cache.invalidate(secretName);

        connection.setParameters(Map.of());
    }

    @Override
    public void deleteParameters(Connection connection) {
        if (readOnly) {
            throw new UnsupportedOperationException("AWS Secrets Manager store is configured read-only");
        }

        String ref = connection.getCredentialRef();

        if (ref == null) {
            return;
        }

        String secretName = resolvePath(ref);

        secretsManagerClient.deleteSecret(
            DeleteSecretRequest.builder()
                .secretId(secretName)
                .forceDeleteWithoutRecovery(true)
                .build());

        cache.invalidate(secretName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSecret(String secretName) {
        try {
            GetSecretValueResponse response = secretsManagerClient.getSecretValue(
                GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build());

            return objectMapper.readValue(response.secretString(), Map.class);
        } catch (ResourceNotFoundException e) {
            logger.warn("Secret not found in AWS Secrets Manager: {}", secretName);

            return Map.of();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to fetch secret from AWS Secrets Manager: " + secretName, e);
        }
    }

    private String serialize(Map<String, ?> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize parameters to JSON", e);
        }
    }

    private String resolvePath(String ref) {
        return CredentialPathResolver.resolve(pathTemplate, TenantContext.getCurrentTenantId(), null, ref);
    }
}
```

**NOTE:** `TenantContext.getCurrentTenantId()` is a guess at the API. If it doesn't exist with that signature, look in `server/libs/core/tenant/tenant-api/` for the actual accessor (likely a static method) and adapt. If no tenant context is available in this layer, pass `null` (the template just won't be able to resolve `{tenant}`, which is acceptable for v1).

- [ ] **Step 2: Write the configuration class**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.AwsSecretsManagerConnectionCredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Wires {@link AwsSecretsManagerConnectionCredentialStore} when the operator selects the AWS Secrets Manager
 * external store provider. Spring Cloud AWS provides the {@link SecretsManagerClient} bean automatically once
 * the {@link com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.boot.AwsSecretsManagerCredentialStoreEnvironmentPostProcessor}
 * enables {@code spring.cloud.aws.secretsmanager.enabled=true}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef.connection.credential-store.external", name = "provider", havingValue = "aws-secrets-manager")
public class AwsSecretsManagerCredentialStoreConfiguration {

    @Bean
    AwsSecretsManagerConnectionCredentialStore awsSecretsManagerConnectionCredentialStore(
        ApplicationProperties applicationProperties, ObjectMapper objectMapper,
        SecretsManagerClient secretsManagerClient) {

        return new AwsSecretsManagerConnectionCredentialStore(
            applicationProperties, objectMapper, secretsManagerClient);
    }
}
```

- [ ] **Step 3: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:compileJava
```
Expected: BUILD SUCCESSFUL.

If TenantContext API differs from the assumed `TenantContext.getCurrentTenantId()`, fix here.

- [ ] **Step 4: Module check (no tests yet)**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit tasks 3+4+5 together (module scaffold + EnvironmentPostProcessor + store + config)**

```bash
git add \
  server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/ \
  settings.gradle.kts
git commit -m "$(cat <<'EOF'
547 Add AWS Secrets Manager credential store adapter

New EE module implementing ConnectionCredentialStore against AWS Secrets
Manager. Activates when bytechef.connection.credential-store.external.provider
= aws-secrets-manager — an EnvironmentPostProcessor translates that to
spring.cloud.aws.secretsmanager.enabled=true so Spring Cloud AWS
auto-configures SecretsManagerClient using the existing bytechef.cloud.aws.*
credentials and region.

The store serializes the credential payload as JSON and writes to a path
derived from an operator-configured template (default "bytechef/{ref}").
Read-path is Caffeine-cached with a configurable TTL (default PT5M).
Operator IAM can mark the deployment read-only via the per-adapter
read-only property; writes then throw UnsupportedOperationException.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: LocalStack-based integration test

**File:** `<module>/src/test/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/AwsSecretsManagerConnectionCredentialStoreIntTest.java`

- [ ] **Step 1: Inspect the reference test pattern**

```bash
cat server/ee/libs/core/file-storage/file-storage-aws/file-storage-aws-impl/src/test/java/com/bytechef/ee/file/storage/aws/service/AwsFileStorageIntTest.java | head -80
```

Note:
- LocalStack image version (`localstack/localstack:3.0` or similar)
- `@Container` static field declaration
- `@DynamicPropertySource` overrides
- How buckets/resources are created in `@BeforeAll`

- [ ] **Step 2: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SECRETSMANAGER;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AwsSecretsManagerCredentialStoreIntTestConfiguration.class)
@Testcontainers
class AwsSecretsManagerConnectionCredentialStoreIntTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
        DockerImageName.parse("localstack/localstack:3.0"))
            .withServices(SECRETSMANAGER);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("bytechef.cloud.provider", () -> "aws");
        registry.add("bytechef.connection.credential-store.external.provider", () -> "aws-secrets-manager");
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add(
            "spring.cloud.aws.secretsmanager.endpoint",
            () -> localStack.getEndpointOverride(SECRETSMANAGER).toString());
    }

    @Autowired
    private AwsSecretsManagerConnectionCredentialStore store;

    @Test
    void testGetTypeReportsAwsSecretsManager() {
        assertThat(store.getType()).isEqualTo(ConnectionCredentialStoreType.AWS_SECRETS_MANAGER);
    }

    @Test
    void testIsReadOnlyDefaultsToFalse() {
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    void testStoreThenGetParametersRoundTrip() {
        Connection connection = new Connection();

        store.storeParameters(connection, Map.of("apiKey", "secret-value", "extraField", 42));

        assertThat(connection.getCredentialRef()).isNotBlank();
        assertThat(connection.getParameters()).isEmpty();

        Map<String, ?> retrieved = store.getParameters(connection);

        assertThat(retrieved).containsEntry("apiKey", "secret-value");
        assertThat(retrieved).containsEntry("extraField", 42);
    }

    @Test
    void testUpdateExistingSecret() {
        Connection connection = new Connection();

        store.storeParameters(connection, Map.of("apiKey", "v1"));
        store.storeParameters(connection, Map.of("apiKey", "v2"));

        Map<String, ?> retrieved = store.getParameters(connection);

        assertThat(retrieved).containsEntry("apiKey", "v2");
    }

    @Test
    void testDeleteParametersRemovesSecret() {
        Connection connection = new Connection();

        store.storeParameters(connection, Map.of("apiKey", "to-be-deleted"));

        String ref = connection.getCredentialRef();

        store.deleteParameters(connection);

        // After delete, getParameters returns empty map (ResourceNotFoundException is caught).
        Connection probe = new Connection();
        probe.setCredentialRef(ref);

        assertThat(store.getParameters(probe)).isEmpty();
    }

    @Test
    void testGetParametersWithNoCredentialRefReturnsEmpty() {
        Connection connection = new Connection();

        assertThat(store.getParameters(connection)).isEmpty();
    }
}
```

Then create the test configuration class `AwsSecretsManagerCredentialStoreIntTestConfiguration.java` in the same package:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager.config.AwsSecretsManagerCredentialStoreConfiguration;
import com.bytechef.jackson.config.JacksonConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author Ivica Cardic
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@ComponentScan(basePackages = "com.bytechef.ee.platform.connection.credential.store.aws.secretsmanager")
@Import({JacksonConfiguration.class, AwsSecretsManagerCredentialStoreConfiguration.class})
class AwsSecretsManagerCredentialStoreIntTestConfiguration {
}
```

- [ ] **Step 3: Run the test**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:testIntegration
```
Expected: BUILD SUCCESSFUL, 6 tests pass.

Common failure modes:
- **LocalStack container fails to start:** Docker isn't running, or the image version isn't available. Check `docker ps`.
- **`SecretsManagerClient` bean not found:** The EnvironmentPostProcessor isn't being picked up. Verify `META-INF/spring.factories` content and that `bytechef.connection.credential-store.external.provider=aws-secrets-manager` is set in `@DynamicPropertySource`.
- **`AwsCredentialsProvider` bean not found:** The test config doesn't import `AwsCloudProviderConfiguration`. Add `@Import(com.bytechef.ee.cloud.aws.config.AwsCloudProviderConfiguration.class)` or set the credentials directly via `spring.cloud.aws.credentials.*` properties (which the test already does via LocalStack overrides).

- [ ] **Step 4: Check**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/src/test/
git commit -m "$(cat <<'EOF'
547 Test AWS Secrets Manager credential store against LocalStack

Six int tests covering: store/get round trip, secret update, delete,
no-ref read returns empty, type identification, read-only default.
Uses Testcontainers LocalStack 3.0 with the SECRETSMANAGER service.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Final spotless + check sweep

- [ ] **Step 1: Spotless on touched modules**

```bash
./gradlew \
  :server:libs:config:app-config:spotlessApply \
  :server:libs:platform:platform-connection:platform-connection-api:spotlessApply \
  :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:spotlessApply
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Full check on all touched modules**

```bash
./gradlew \
  :server:libs:config:app-config:check \
  :server:libs:platform:platform-connection:platform-connection-api:check \
  :server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager:check
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: If spotless changed anything, commit**

```bash
git status --short
git add -u
git commit -m "$(cat <<'EOF'
547 Apply spotless formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

- [ ] **Step 4: Verify commit graph**

```bash
git log --oneline 5d599563b4b..HEAD
```

Expected (~4-5 new commits):
```
<hash> 547 Apply spotless formatting           (only if Step 3 was needed)
<hash> 547 Test AWS Secrets Manager credential store against LocalStack
<hash> 547 Add AWS Secrets Manager credential store adapter
<hash> 547 Add CredentialPathResolver template utility
<hash> 547 Add ApplicationProperties.Connection.CredentialStore config
```

PR-3 complete.

---

## Self-Review Notes

**Spec coverage:**
- ✓ `AwsSecretsManagerConnectionCredentialStore` implementation → Task 5
- ✓ Spring Cloud AWS dep + EnvironmentPostProcessor → Tasks 3-4
- ✓ `ApplicationProperties.Connection.CredentialStore.AwsSecretsManager` typed config → Task 1
- ✓ JSON-serialized secret format → Task 5 (uses `ObjectMapper.writeValueAsString`)
- ✓ UUID-keyed credential_ref, path template resolution → Tasks 2, 5
- ✓ Caffeine-cached read path → Task 5
- ✓ Read-only mode honoring → Task 5 (throws on storeParameters/deleteParameters)
- ✓ LocalStack-based integration test → Task 6
- ✓ Module placement in EE → Task 3

**Open items resolved:**
- ✓ Spring Cloud AWS version: confirmed 4.0.2 from `libs.versions.toml`
- ✓ LocalStack image: `localstack/localstack:3.0` per existing `AwsFileStorageIntTest`

**Out-of-scope for PR 3:**
- HashiCorp Vault adapter (PR 4)
- Frontend UI (sibling spec)
- AWS KMS EncryptionKey provider (sibling track)

**Risks:**
- Task 5's `TenantContext.getCurrentTenantId()` API name is a guess. Step 3 verifies via compile and the engineer adapts to whatever the real accessor is.
- The integration test needs Docker running locally. Document in PR description.
- `AwsCloudProviderConfiguration` import path may need to be added to the test config if the standard auto-config doesn't pick it up.
