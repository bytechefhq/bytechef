# Connection Credential Store PR 4 — HashiCorp Vault adapter

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Ship a `HashiCorpVaultConnectionCredentialStore` implementation of the SPI introduced in PR 1, the third and final credential store backend in the v1 series (Database in PR 1, AWS Secrets Manager in PR 3).

**Architecture:** New EE module `server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/`. Uses **Spring Vault 4.0.2** (`spring-vault-core`, compatible with Spring Framework 7.x). Token + AppRole authentication. KV v2 secret engine. An `EnvironmentPostProcessor` maps `bytechef.connection.credential-store.hashicorp-vault.*` properties to the corresponding `spring.vault.*` properties so Spring Vault's auto-configuration wires up the `VaultTemplate`.

**Tech Stack:** Spring Vault 4.0.2 (new dependency — added to `libs.versions.toml` in this PR), Caffeine cache, Testcontainers `vault:1.13` for integration testing.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-design.md](../specs/2026-05-19-connection-credential-store-design.md) (Section "HashiCorpVaultConnectionCredentialStore (EE)")

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Branch state at PR 4 start:** Feature branch `claude/amazing-brahmagupta-12e13d` at the tip of PR 3 (commit `a50473f80d6`).

---

## Branch hygiene — applies to every task

1. Run `git branch --show-current` and confirm `claude/amazing-brahmagupta-12e13d` before any change.
2. NEVER run `git checkout`, `git pull`, `git rebase`, `git fetch`.
3. If anything seems wrong, STOP and report.

---

## File Structure

**New EE module:** `server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/`

**New files:**
| File | Responsibility |
|---|---|
| `<module>/build.gradle.kts` | Module deps including spring-vault-core |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/HashiCorpVaultConnectionCredentialStore.java` | Adapter impl |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/config/HashiCorpVaultCredentialStoreConfiguration.java` | `@Configuration` wiring the bean |
| `<module>/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/boot/HashiCorpVaultCredentialStoreEnvironmentPostProcessor.java` | `bytechef.*` → `spring.vault.*` translation |
| `<module>/src/main/resources/META-INF/spring.factories` | Registers the EnvironmentPostProcessor |
| `<module>/src/test/java/.../HashiCorpVaultConnectionCredentialStoreIntTest.java` | Testcontainers Vault dev-mode integration test |
| `<module>/src/test/java/.../HashiCorpVaultCredentialStoreIntTestConfiguration.java` | Spring test config |

**Modified files:**
| File | Change |
|---|---|
| `gradle/libs.versions.toml` | Add `spring-vault = "4.0.2"` version + library entry |
| `settings.gradle.kts` | Include the new EE module |
| `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` | Add `Connection.CredentialStore.HashiCorpVault` nested config class |

---

### Task 1: Add spring-vault dependency to version catalog

**File:** `gradle/libs.versions.toml`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
git log --oneline -1        # top is a50473f80d6 547 Test AWS Secrets Manager credential store against LocalStack
```

- [ ] **Step 2: Add the version**

Open `gradle/libs.versions.toml`. In the `[versions]` block, add (alphabetically sorted with existing entries):

```toml
spring-vault = "4.0.2"
```

(Insert between `spring-shell` and `springaicommunity-agent-judge` based on alphabetical ordering — verify by reading the existing block.)

- [ ] **Step 3: Add the library entry**

In the `[libraries]` block, add:

```toml
org-springframework-vault-spring-vault-core = { module = "org.springframework.vault:spring-vault-core", version.ref = "spring-vault" }
```

(Insert near `org-springframework-...` related entries, alphabetically.)

- [ ] **Step 4: Verify Gradle accepts the catalog change**

```bash
./gradlew help 2>&1 | tail -3
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml
git commit -m "$(cat <<'EOF'
547 Add spring-vault 4.0.2 to version catalog

Spring Vault 4.0.x targets Spring Framework 7.0.7+, compatible with the
project's Spring Boot 4.0.6 (Spring Framework 7.x). Pulled in via the
catalog so subsequent commits can reference it from build.gradle.kts.
Supports both Jackson 2 and Jackson 3 (Jackson 3 preferred — matches
tools.jackson usage in this project).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Extend ApplicationProperties.Connection.CredentialStore with HashiCorpVault

**File:** `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

- [ ] **Step 1: Locate the CredentialStore inner class** (added in PR 3 Task 1)

Inside the `Connection.CredentialStore` class, alongside the existing `awsSecretsManager`, `cache`, `external`, `pathTemplate` fields.

- [ ] **Step 2: Add the new field and getter/setter**

Alphabetical placement: `awsSecretsManager` < `cache` < `external` < `hashicorpVault` < `pathTemplate`. Insert the field, getter, and setter accordingly:

```java
private HashiCorpVault hashicorpVault = new HashiCorpVault();
// ... (existing fields)

public HashiCorpVault getHashicorpVault() {
    return hashicorpVault;
}

public void setHashicorpVault(HashiCorpVault hashicorpVault) {
    this.hashicorpVault = hashicorpVault;
}
```

- [ ] **Step 3: Add the HashiCorpVault nested class**

Place after `AwsSecretsManager` (alphabetical: `AwsSecretsManager` < `Cache` < `External` < `HashiCorpVault`):

```java
public static class HashiCorpVault {

    /** When true, refuses write operations. */
    private boolean readOnly;

    /** Vault HTTP endpoint, e.g. "http://vault:8200". */
    private String uri;

    /** Authentication method: "token" or "approle". */
    private String authentication = "token";

    /** Bearer token when authentication=token. */
    private String token;

    private AppRole approle = new AppRole();

    /** KV v2 mount path. Default "secret". */
    private String kvMount = "secret";

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public AppRole getApprole() {
        return approle;
    }

    public void setApprole(AppRole approle) {
        this.approle = approle;
    }

    public String getKvMount() {
        return kvMount;
    }

    public void setKvMount(String kvMount) {
        this.kvMount = kvMount;
    }

    public static class AppRole {

        private String roleId;
        private String secretId;

        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }
    }
}
```

- [ ] **Step 4: Compile + check**

```bash
./gradlew :server:libs:config:app-config:compileJava
./gradlew :server:libs:config:app-config:check
```
Both expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "$(cat <<'EOF'
547 Add HashiCorpVault config to ApplicationProperties.CredentialStore

Sibling to the awsSecretsManager block added in PR 3. Supports:
- uri (vault endpoint)
- authentication (token | approle)
- token (when authentication=token)
- approle.role-id + approle.secret-id (when authentication=approle)
- kv-mount (KV v2 mount path, default "secret")
- read-only flag

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: Scaffold the EE module

- [ ] **Step 1: Create directory skeleton**

```bash
ROOT=server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/boot
mkdir -p $ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/config
mkdir -p $ROOT/src/main/resources/META-INF
mkdir -p $ROOT/src/test/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault
```

- [ ] **Step 2: build.gradle.kts**

Inspect first: `cat server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/build.gradle.kts` to see how the AWS adapter declares deps.

Create `$ROOT/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.springframework.vault.spring.vault.core)
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:vault")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
```

Notes:
- `libs.org.springframework.vault.spring.vault.core` references the catalog entry from Task 1. The Gradle Kotlin DSL converts the TOML key `org-springframework-vault-spring-vault-core` to that dotted accessor.
- No Spring Cloud equivalent needed — Spring Vault is a standalone module, not part of Spring Cloud.

- [ ] **Step 3: settings.gradle.kts entry**

Find the existing line (around 642 after PR 3):
```kotlin
include("server:ee:libs:platform:platform-connection:platform-connection-credential-store-aws-secrets-manager")
```

Insert directly after (alphabetical: `aws-secrets-manager` < `hashicorp-vault`):
```kotlin
include("server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault")
```

- [ ] **Step 4: Verify Gradle recognizes the module**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:tasks 2>&1 | tail -3
```
Expected: lists tasks. No commit yet — combine with Tasks 4-5.

---

### Task 4: EnvironmentPostProcessor

**File:** `$ROOT/src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/boot/HashiCorpVaultCredentialStoreEnvironmentPostProcessor.java`

- [ ] **Step 1: Inspect the AWS reference**

```bash
cat server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/boot/AwsSecretsManagerCredentialStoreEnvironmentPostProcessor.java
```

- [ ] **Step 2: Write the processor**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Translates {@code bytechef.connection.credential-store.hashicorp-vault.*} properties into
 * {@code spring.vault.*} properties so Spring Vault's auto-configuration wires up the {@code VaultTemplate}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class HashiCorpVaultCredentialStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(
            "bytechef.connection.credential-store.external.provider", String.class);

        if (!Objects.equals(provider, "hashicorp-vault")) {
            return;
        }

        Map<String, Object> source = new HashMap<>();

        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.uri", "spring.vault.uri");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.authentication", "spring.vault.authentication");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.token", "spring.vault.token");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.approle.role-id",
            "spring.vault.app-role.role-id");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.approle.secret-id",
            "spring.vault.app-role.secret-id");

        MapPropertySource mapPropertySource = new MapPropertySource(
            "Custom HashiCorp Vault Credential Store Config", source);

        MutablePropertySources mutablePropertySources = environment.getPropertySources();

        mutablePropertySources.addFirst(mapPropertySource);
    }

    private static void copyIfPresent(
        ConfigurableEnvironment environment, Map<String, Object> target, String sourceKey, String targetKey) {

        String value = environment.getProperty(sourceKey);

        if (value != null) {
            target.put(targetKey, value);
        }
    }
}
```

**NOTE:** Spring Vault's property keys for AppRole authentication may use `spring.vault.app-role.*` (kebab) or `spring.vault.app-role.role-id`. Verify against the Spring Vault Boot auto-config reference if the test fails — adapt the target keys accordingly.

- [ ] **Step 3: spring.factories**

Create `$ROOT/src/main/resources/META-INF/spring.factories`:

```
org.springframework.boot.EnvironmentPostProcessor=\
com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.boot.HashiCorpVaultCredentialStoreEnvironmentPostProcessor
```

Note: uses the new Spring Boot 4 key `org.springframework.boot.EnvironmentPostProcessor` (the AWS adapter uses the same key — confirmed during PR 3).

- [ ] **Step 4: Compile**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:compileJava
```

No commit yet — combine with Task 5.

---

### Task 5: HashiCorpVaultConnectionCredentialStore + Configuration

**Files:**
- `$ROOT/.../HashiCorpVaultConnectionCredentialStore.java`
- `$ROOT/.../config/HashiCorpVaultCredentialStoreConfiguration.java`

- [ ] **Step 1: Write the store**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.hashicorp.vault;

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
import org.springframework.vault.core.VaultKeyValueOperations;
import org.springframework.vault.core.VaultKeyValueOperationsSupport;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

/**
 * HashiCorp Vault-backed {@link ConnectionCredentialStore}. Reads/writes secrets in KV v2 at a path derived from
 * the operator-configured template (default {@code "bytechef/connections/{ref}"}).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class HashiCorpVaultConnectionCredentialStore implements ConnectionCredentialStore {

    private static final Logger logger = LoggerFactory.getLogger(HashiCorpVaultConnectionCredentialStore.class);

    private static final String DEFAULT_PATH_TEMPLATE = "bytechef/connections/{ref}";

    private final Cache<String, Map<String, Object>> cache;
    private final String kvMount;
    private final String pathTemplate;
    private final boolean readOnly;
    private final VaultTemplate vaultTemplate;

    @SuppressFBWarnings("EI2")
    public HashiCorpVaultConnectionCredentialStore(
        ApplicationProperties applicationProperties, VaultTemplate vaultTemplate) {

        ApplicationProperties.Connection.CredentialStore credentialStore = applicationProperties.getConnection()
            .getCredentialStore();
        ApplicationProperties.Connection.CredentialStore.HashiCorpVault vaultConfig = credentialStore.getHashicorpVault();

        String configuredTemplate = credentialStore.getPathTemplate();

        this.pathTemplate = configuredTemplate != null ? configuredTemplate : DEFAULT_PATH_TEMPLATE;
        this.kvMount = vaultConfig.getKvMount();
        this.readOnly = vaultConfig.isReadOnly();
        this.vaultTemplate = vaultTemplate;
        this.cache = Caffeine.newBuilder()
            .expireAfterWrite(credentialStore.getCache()
                .getTtl())
            .build();
    }

    @Override
    public ConnectionCredentialStoreType getType() {
        return ConnectionCredentialStoreType.HASHICORP_VAULT;
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

        String path = resolvePath(ref);

        return cache.get(path, this::fetchSecret);
    }

    @Override
    public void storeParameters(Connection connection, Map<String, ?> parameters) {
        if (readOnly) {
            throw new UnsupportedOperationException("HashiCorp Vault store is configured read-only");
        }

        String ref = connection.getCredentialRef();

        if (ref == null) {
            ref = UUID.randomUUID()
                .toString();

            connection.setCredentialRef(ref);
        }

        String path = resolvePath(ref);

        kvOps().put(path, parameters);

        cache.invalidate(path);

        connection.setParameters(Map.of());
    }

    @Override
    public void deleteParameters(Connection connection) {
        if (readOnly) {
            throw new UnsupportedOperationException("HashiCorp Vault store is configured read-only");
        }

        String ref = connection.getCredentialRef();

        if (ref == null) {
            return;
        }

        String path = resolvePath(ref);

        kvOps().delete(path);

        cache.invalidate(path);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchSecret(String path) {
        try {
            VaultResponse response = kvOps().get(path);

            if (response == null || response.getData() == null) {
                logger.warn("Secret not found in HashiCorp Vault: {}", path);

                return Map.of();
            }

            return (Map<String, Object>) response.getData();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch secret from HashiCorp Vault: " + path, e);
        }
    }

    private VaultKeyValueOperations kvOps() {
        return vaultTemplate.opsForKeyValue(kvMount, VaultKeyValueOperationsSupport.KeyValueBackend.KV_2);
    }

    private String resolvePath(String ref) {
        return CredentialPathResolver.resolve(pathTemplate, TenantContext.getCurrentTenantId(), null, ref);
    }
}
```

- [ ] **Step 2: Write the configuration**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.HashiCorpVaultConnectionCredentialStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.core.VaultTemplate;

/**
 * Wires {@link HashiCorpVaultConnectionCredentialStore} when the operator selects HashiCorp Vault. Spring Vault
 * Boot auto-configures the {@code VaultTemplate} based on the {@code spring.vault.*} properties produced by
 * {@link com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.boot.HashiCorpVaultCredentialStoreEnvironmentPostProcessor}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(
    prefix = "bytechef.connection.credential-store.external", name = "provider", havingValue = "hashicorp-vault")
public class HashiCorpVaultCredentialStoreConfiguration {

    @Bean
    HashiCorpVaultConnectionCredentialStore hashiCorpVaultConnectionCredentialStore(
        ApplicationProperties applicationProperties, VaultTemplate vaultTemplate) {

        return new HashiCorpVaultConnectionCredentialStore(applicationProperties, vaultTemplate);
    }
}
```

- [ ] **Step 3: Compile + check**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:compileJava
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:check
```
Both expected: BUILD SUCCESSFUL.

If compile fails because Spring Vault API differs from the assumed `opsForKeyValue(mount, KeyValueBackend.KV_2)`:
- Check Spring Vault 4.0.2 Javadoc — the method signature may have evolved
- Adapt to whatever the actual API is

- [ ] **Step 4: Commit Tasks 3+4+5 together**

```bash
git add \
  server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/ \
  settings.gradle.kts
git commit -m "$(cat <<'EOF'
547 Add HashiCorp Vault credential store adapter

New EE module implementing ConnectionCredentialStore against HashiCorp
Vault. Activates when bytechef.connection.credential-store.external.provider
= hashicorp-vault — an EnvironmentPostProcessor translates the
bytechef.connection.credential-store.hashicorp-vault.* properties to the
matching spring.vault.* properties so Spring Vault auto-configures
the VaultTemplate.

Supports token + AppRole authentication, KV v2 secret engine, and the
shared CredentialPathResolver template (default
"bytechef/connections/{ref}"). Read-path is Caffeine-cached with the
shared cache.ttl configuration.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Integration test with Vault Testcontainer

**File:** `$ROOT/src/test/java/.../HashiCorpVaultConnectionCredentialStoreIntTest.java`

- [ ] **Step 1: Inspect Testcontainers Vault module availability**

```bash
grep -rn "testcontainers" server/libs/test --include="*.kts" | head -5
grep "vault" gradle/libs.versions.toml
```

The `org.testcontainers:vault` module is part of the standard Testcontainers distribution — version comes from `testcontainers = "1.21.3"` in the catalog (matches existing `localstack` usage).

- [ ] **Step 2: Write the test**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.hashicorp.vault;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionCredentialStoreType;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = HashiCorpVaultCredentialStoreIntTestConfiguration.class)
@Testcontainers
class HashiCorpVaultConnectionCredentialStoreIntTest {

    private static final String VAULT_TOKEN = "test-root-token";

    @Container
    static VaultContainer<?> vault = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.15"))
        .withVaultToken(VAULT_TOKEN);

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("bytechef.connection.credential-store.external.provider", () -> "hashicorp-vault");
        registry.add("bytechef.connection.credential-store.hashicorp-vault.uri", vault::getHttpHostAddress);
        registry.add("bytechef.connection.credential-store.hashicorp-vault.authentication", () -> "token");
        registry.add("bytechef.connection.credential-store.hashicorp-vault.token", () -> VAULT_TOKEN);
    }

    @Autowired
    private HashiCorpVaultConnectionCredentialStore store;

    @Test
    void testGetTypeReportsHashicorpVault() {
        assertThat(store.getType()).isEqualTo(ConnectionCredentialStoreType.HASHICORP_VAULT);
    }

    @Test
    void testIsReadOnlyDefaultsToFalse() {
        assertThat(store.isReadOnly()).isFalse();
    }

    @Test
    void testStoreThenGetParametersRoundTrip() {
        Connection connection = new Connection();

        store.storeParameters(connection, Map.of("apiKey", "secret-value", "extraField", "42"));

        assertThat(connection.getCredentialRef()).isNotBlank();
        assertThat(connection.getParameters()).isEmpty();

        Map<String, ?> retrieved = store.getParameters(connection);

        assertThat(retrieved).containsEntry("apiKey", "secret-value");
        assertThat(retrieved).containsEntry("extraField", "42");
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

- [ ] **Step 3: Write the test config**

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.credential.store.hashicorp.vault;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.connection.credential.store.hashicorp.vault.config.HashiCorpVaultCredentialStoreConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.vault.config.EnvironmentVaultConfiguration;

/**
 * @author Ivica Cardic
 */
@Configuration
@ImportAutoConfiguration
@EnableConfigurationProperties(ApplicationProperties.class)
@ComponentScan(basePackages = "com.bytechef.ee.platform.connection.credential.store.hashicorp.vault")
@Import({
    EnvironmentVaultConfiguration.class, HashiCorpVaultCredentialStoreConfiguration.class
})
class HashiCorpVaultCredentialStoreIntTestConfiguration {
}
```

**NOTE on `EnvironmentVaultConfiguration`**: Spring Vault provides this Spring `@Configuration` class that builds the `VaultTemplate` from `spring.vault.*` properties (the form `EnvironmentPostProcessor` produces). If Spring Vault 4.0.2 exposes a different config class name, adapt — search Spring Vault docs for "auto-configuration".

- [ ] **Step 4: Run the test**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:testIntegration
```
Expected: BUILD SUCCESSFUL, 6 tests pass.

Common failure modes:
- **`VaultTemplate` bean not found**: The test config isn't bringing in Spring Vault's auto-config. Investigate whether Spring Vault's auto-config uses standard `META-INF/spring/...imports` (which `@EnableAutoConfiguration` would pick up) or a different mechanism. If standard, change `@ImportAutoConfiguration` to `@EnableAutoConfiguration` (despite the broader scope).
- **Vault container fails to start**: Image `hashicorp/vault:1.15` not pulled. Try `hashicorp/vault:1.13` (older but stable). LocalStack-style test already requires Docker.
- **KV v2 not enabled**: Vault dev mode enables KV v2 at `/secret` by default — confirm by reading `VaultContainer` source. If KV v2 isn't on the default mount, set it explicitly with `vault kv enable-versioning` in a `withInitCommand(...)` block.

- [ ] **Step 5: Module check**

```bash
./gradlew :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:check
```

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/src/test/
git commit -m "$(cat <<'EOF'
547 Test HashiCorp Vault credential store against Testcontainers Vault

Six int tests covering: store/get round trip, secret update, delete,
no-ref read returns empty, type identification, read-only default.
Uses Testcontainers vault:1.15 with the default KV v2 secret engine
at /secret mount; root token authentication.

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
  :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:spotlessApply
```

- [ ] **Step 2: Full check on touched modules**

```bash
./gradlew \
  :server:libs:config:app-config:check \
  :server:ee:libs:platform:platform-connection:platform-connection-credential-store-hashicorp-vault:check
```

- [ ] **Step 3: Commit any spotless changes**

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
git log --oneline a50473f80d6..HEAD
```

Expected (~5-6 new commits):
```
<hash> 547 Apply spotless formatting           (only if Step 3 was needed)
<hash> 547 Test HashiCorp Vault credential store against Testcontainers Vault
<hash> 547 Add HashiCorp Vault credential store adapter
<hash> 547 Add HashiCorpVault config to ApplicationProperties.CredentialStore
<hash> 547 Add spring-vault 4.0.2 to version catalog
<hash> 547 Add PR 4 implementation plan for HashiCorp Vault adapter
```

PR-4 complete. The credential-store track is now complete:
- PR 1: SPI + Database default
- PR 2: GraphQL info query
- PR 3: AWS Secrets Manager adapter
- PR 4: HashiCorp Vault adapter

The remaining work in the spec (frontend UI, AWS KMS EncryptionKey provider) is sibling tracks.

---

## Self-Review Notes

**Spec coverage:**
- ✓ Spring Vault 4.0.2 dep + version catalog entry → Task 1
- ✓ HashiCorp Vault config in ApplicationProperties → Task 2
- ✓ EnvironmentPostProcessor → Task 4
- ✓ Token + AppRole authentication support → Tasks 2, 4
- ✓ KV v2 secret engine → Task 5
- ✓ Caffeine-cached read path → Task 5
- ✓ Read-only mode → Task 5
- ✓ Testcontainers Vault integration test → Task 6

**Open items resolved:**
- ✓ Spring Vault version: 4.0.2 (compatible with Spring Boot 4 / Spring Framework 7)
- ✓ Vault Testcontainer image: `hashicorp/vault:1.15`
- ✓ Spring Vault config class: `EnvironmentVaultConfiguration` (verify in code)

**Risks:**
- Spring Vault 4.0.2's API for KV v2 operations may have evolved; the plan uses the legacy `opsForKeyValue(mount, KV_2)` API. If 4.0.2 has changed, adapt.
- Spring Vault Boot auto-config behavior in Spring Boot 4 hasn't been verified. If the `VaultTemplate` bean doesn't auto-create from `spring.vault.*` properties, the test config may need an explicit `VaultTemplate @Bean`.
- The test uses `hashicorp/vault:1.15`. If that image version isn't available locally, swap for a known-good version like `1.13`.
