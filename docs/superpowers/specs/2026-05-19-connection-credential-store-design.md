# Connection credential store — external secret stores & vaults

**Status:** Design | **Owner:** Ivica | **Created:** 2026-05-19 | **Last updated:** 2026-05-19 | **Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

## Why

Today every connection's credential payload is stored encrypted in the `connection.parameters` column. Encryption-at-rest is real (via `EncryptedMapWrapper` converters + the configurable `EncryptionKey` SPI), but the *storage location* is fixed: PostgreSQL.

Enterprise customers — particularly regulated industries and operators with established secret-management practices — need ByteChef to integrate with their existing secret stores (HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault) rather than holding sensitive material in the application database. Reasons commonly cited:

- **Defense in depth.** A database compromise should not directly yield production credentials. Off-loading to a dedicated vault means a separate ACL, separate audit trail, and separate blast radius.
- **Existing policy & tooling.** Secret rotation, lease management, audit logging, IAM policies, and access reviews already live in the vault. ByteChef rendering its own copy in the DB fragments those processes.
- **Bring-your-own-vault for cloud iPaaS.** Customers using ByteChef as an embedded iPaaS expect to plug in their own secret store rather than entrust credentials to ByteChef's database.

Issue #547 captures this requirement but its checklist conflates two distinct architectural concerns (encryption-key sourcing vs. credential storage). This spec disentangles them and lays out the implementation.

## What

Introduce a new `ConnectionCredentialStore` SPI that owns the credential payload lifecycle. The default implementation continues to store credentials in `connection.parameters` (no behavior change for existing deployments). Operators can additionally activate one external store — AWS Secrets Manager or HashiCorp Vault in v1 — and per-connection choose which backend holds the credentials.

In parallel (but as a separate PR track documented in this spec), AWS KMS joins the existing `EncryptionKey` SPI alongside `FileSystemEncryptionKey` and `PropertyEncryptionKey`.

## Goals

1. New `ConnectionCredentialStore` SPI that cleanly separates *credential payload persistence* from `Connection` metadata persistence.
2. `DatabaseConnectionCredentialStore` is the always-active default — zero behavior change for any deployment that does not configure an external store.
3. Database + at most one external store can be active simultaneously. Per-connection choice between them.
4. External stores can be marked read-only by operator policy; UI surfaces the constraint and offers a "register existing credential" affordance.
5. Existing callers using `connection.getParameters()` continue working unchanged. The service eagerly populates the entity field on read.
6. Configuration surface is purely `bytechef.*` namespaced; Spring Cloud AWS / Spring Vault properties are internal mapping targets, not part of the user-facing API.
7. AWS KMS lands as a new `EncryptionKey` provider, in its own PR, on its own schedule.

## Non-goals

- **Multiple external stores simultaneously.** v1 supports Database + at-most-one-external. Multi-external is rejected as scope creep with no clear demand.
- **GCP Secret Manager / Azure Key Vault adapters.** Follow-up PRs once the SPI is proven via AWS + HashiCorp.
- **Automatic migration tooling.** v1 documents a manual operator workaround. A `migrateConnectionToCredentialStore` admin mutation is a follow-up spec.
- **Secret rotation orchestration.** Vaults manage rotation; ByteChef reads.
- **Per-connection cache TTL override.** Single global TTL in v1.
- **Audit logging of credential reads.** Vaults provide their own audit trails; duplicating is wasteful.
- **Frontend UI implementation.** The GraphQL contract is defined here; the actual settings page, connection creation form selector, and "register existing" form ship under a sibling frontend spec.
- **K8s / AWS IAM auth for HashiCorp Vault.** Token + AppRole cover the common cases. Other auth methods are follow-up workstreams.

---

## Issue #547 → SPI mapping

Issue #547's checklist conflates two architectural concerns. This design separates them:

| Issue checklist item | Type of service | ByteChef SPI | Status |
|---|---|---|---|
| File-based encryption key | Encryption key source | `EncryptionKey` | ✓ Already implemented (`FileSystemEncryptionKey`) |
| Environment variable encryption key | Encryption key source | `EncryptionKey` | ✓ Already implemented (`PropertyEncryptionKey`) |
| **AWS Key Management Service** | **Encryption key source** | **`EncryptionKey`** | **In scope (this spec), separate PR — `AwsKmsEncryptionKey`** |
| **Google Cloud Key Management** | **Encryption key source** | **`EncryptionKey`** | Follow-up to AWS KMS (out of scope of this spec; mirrors AWS KMS module shape) |
| Azure Vault | Both — keys *and* secrets | Primarily `ConnectionCredentialStore` (secrets feature); optionally `EncryptionKey` (keys feature, separate workstream) | Follow-up after v1 |
| HashiCorp Vault | Secret storage | `ConnectionCredentialStore` | ✓ In v1 (this spec) |

**Rationale for the split:**

- AWS KMS and GCP Cloud KMS are **key management services**. They manage symmetric/asymmetric keys; they don't store arbitrary secret blobs. Their natural slot is the `EncryptionKey` SPI — the key returned by KMS encrypts the existing `connection.parameters` column; the DB remains the credential storage location.
- AWS Secrets Manager, GCP Secret Manager, Azure Key Vault (secrets API), and HashiCorp Vault are **secret stores**. They hold arbitrary key-value payloads and become the alternative to the `connection.parameters` column. The new `ConnectionCredentialStore` SPI is the right abstraction.
- Azure Key Vault uniquely spans both — "Azure Vault" in the issue text most likely refers to the secrets capability, so it primarily targets `ConnectionCredentialStore`. A future `AzureKeyVaultEncryptionKey` could exist for the keys API.

**PR planning implications:**

- Credential-store track (PR series in this spec): SPI introduction → DB-backed default refactor → AWS Secrets Manager adapter → HashiCorp Vault adapter → GraphQL query → settings UI (sibling frontend spec).
- Encryption-key track (sibling PR series, designed here, shipped separately): `AwsKmsEncryptionKey` is one small EE module mirroring `encryption-filesystem` / `encryption-property`. `GcpKmsEncryptionKey` follows.

The two tracks are independent and ship in any order.

---

## Architecture overview

Two orthogonal SPIs handle two different concerns:

1. **`EncryptionKey` (existing, expanded)** — controls where the symmetric key for the DB `connection.parameters` column comes from.
   - Existing: `FileSystemEncryptionKey`, `PropertyEncryptionKey`
   - New: `AwsKmsEncryptionKey` (uses Spring Cloud AWS `KmsClient`)
   - Selected at boot via `@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "...")`. Exactly one bean active.

2. **`ConnectionCredentialStore` (new)** — controls where the credential payload itself lives.
   - `DatabaseConnectionCredentialStore` (CE, default, always registered)
   - `AwsSecretsManagerConnectionCredentialStore` (EE, opt-in)
   - `HashiCorpVaultConnectionCredentialStore` (EE, opt-in)
   - Database is always available. At most one external is registered, selected via `bytechef.connection.credential-store.external.provider`.
   - Per-connection routing via a new `credential_store_type` discriminator column on the `connection` row.

The two SPIs are independent. A deployment can pair `DatabaseConnectionCredentialStore` with `AwsKmsEncryptionKey`; another can use `AwsSecretsManagerConnectionCredentialStore` with the default `FileSystemEncryptionKey`. The latter combination doesn't actually use the encryption key for the parameters column (AWS Secrets Manager handles encryption-at-rest itself), but the key may still be used elsewhere in the system, so the SPI stays active.

### Storage mode invariant per connection row

| `credential_store_type` | `credential_ref` | `parameters` | Mode |
|---|---|---|---|
| 0 (DATABASE) | null | encrypted payload | DB-backed (default) |
| 1 (AWS_SECRETS_MANAGER) | UUID | empty | AWS-backed |
| 2 (HASHICORP_VAULT) | UUID | empty | Vault-backed |

Enforced at SPI level, not via DB constraint, because both `parameters` empty and `credential_ref` null is also valid for connections with no auth requirements.

---

## ConnectionCredentialStore SPI

```java
package com.bytechef.platform.connection.service;

public interface ConnectionCredentialStore {

    /** The credential store type this implementation handles. Service uses this to dispatch
     *  per-connection based on connection.credentialStoreType. */
    ConnectionCredentialStoreType getType();

    /** Whether this store accepts write operations in the current deployment.
     *  Read-only stores throw UnsupportedOperationException from storeParameters/deleteParameters. */
    boolean isReadOnly();

    /** Resolve the credential payload for the given connection. */
    Map<String, ?> getParameters(Connection connection);

    /** Persist the credential payload. Called BEFORE saving the row; may mutate the entity
     *  (e.g., setting credentialRef, clearing parameters). */
    void storeParameters(Connection connection, Map<String, ?> parameters);

    /** Remove the credential payload. Called BEFORE the row is deleted. */
    void deleteParameters(Connection connection);
}

public enum ConnectionCredentialStoreType {
    DATABASE,             // ordinal 0 — default for every existing and new row
    AWS_SECRETS_MANAGER,  // ordinal 1
    HASHICORP_VAULT       // ordinal 2
}
```

The interface lives in `server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/`.

The enum is persisted as INT ordinal per [enum storage convention](../../.../memory/feedback_enum_storage.md). New values are always appended (never inserted) to preserve ordinal stability.

### Read-only mode

Read-only is a *deployment policy*, not an *adapter capability*. Both AWS Secrets Manager and HashiCorp Vault technically support writes; whether operator policy (IAM, Vault ACL) permits them is configured per deployment via `bytechef.connection.credential-store.<provider>.read-only=true`.

Each adapter reports its mode through `isReadOnly()`. The service-level dispatch (`ConnectionServiceImpl`) and `TokenRefreshHandler` check this flag and refuse writes proactively. The adapter throws `UnsupportedOperationException` as a defensive backstop.

---

## Schema model

### Liquibase migration

```xml
<!-- server/libs/config/liquibase-config/src/main/resources/config/liquibase/changelog/platform/connection/20260519000001_platform_connection_credential_store_columns.xml -->
<changeSet id="20260519000001" author="Ivica Cardic">
    <addColumn tableName="connection">
        <column name="credential_store_type" type="INT" defaultValueNumeric="0">
            <constraints nullable="false"/>
        </column>
        <column name="credential_ref" type="VARCHAR(64)">
            <constraints nullable="true"/>
        </column>
    </addColumn>
</changeSet>
```

Existing rows default to `credential_store_type = 0` (DATABASE) automatically — no backfill.

### `Connection` entity additions

```java
@Column("credential_store_type")
private int credentialStoreType;    // ordinal; defaults to 0 = DATABASE

@Column("credential_ref")
@Nullable
private String credentialRef;       // null when DATABASE-backed, UUID when external

public ConnectionCredentialStoreType getCredentialStoreType() {
    return ConnectionCredentialStoreType.values()[credentialStoreType];
}

public void setCredentialStoreType(ConnectionCredentialStoreType type) {
    this.credentialStoreType = type.ordinal();
}
```

Getters/setters for `credentialRef` follow the standard pattern.

---

## DatabaseConnectionCredentialStore (CE, default)

`server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/DatabaseConnectionCredentialStore.java`

```java
@Component   // no @ConditionalOnProperty — always registered
public class DatabaseConnectionCredentialStore implements ConnectionCredentialStore {

    @Override
    public ConnectionCredentialStoreType getType() {
        return ConnectionCredentialStoreType.DATABASE;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public Map<String, ?> getParameters(Connection connection) {
        return connection.getParameters();   // entity field, populated by JDBC
    }

    @Override
    public void storeParameters(Connection connection, Map<String, ?> parameters) {
        connection.setParameters(parameters);
        // The surrounding ConnectionRepository.save() persists with EncryptedMapWrapper converters.
    }

    @Override
    public void deleteParameters(Connection connection) {
        // No-op: parameters are cleared when the connection row is deleted.
    }
}
```

No caching — local DB reads are already cheap.

---

## AwsSecretsManagerConnectionCredentialStore (EE)

`server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/`

**Dependency:** `io.awspring.cloud:spring-cloud-aws-starter-secrets-manager` (catalog version `4.0.0`, already in `libs.versions.toml`).

**Authentication:** standard AWS credentials chain via existing `bytechef.cloud.aws.*` config consumed by [`AwsCloudProviderConfiguration`](../../server/ee/libs/core/cloud/cloud-aws/src/main/java/com/bytechef/ee/cloud/aws/config/AwsCloudProviderConfiguration.java). No new credential properties.

**Secret format:** JSON-serialized `Map<String, Object>` via the project's Jackson `ObjectMapper`. Stored as `SecretString`.

### Behavior

- `storeParameters(connection, params)`:
  1. If `connection.getCredentialRef() == null`, generate `UUID.randomUUID().toString()` and set on entity.
  2. Resolve secret name from path template (see *Configuration surface*).
  3. `secretsManagerClient.createSecret(...)` (or `putSecretValue` on update) with JSON-serialized params.
  4. `connection.setParameters(Map.of())`.
  5. Invalidate cache entry for this connection's ref.
- `getParameters(connection)`:
  1. Resolve secret name from `connection.credentialRef`.
  2. Cache lookup (Caffeine, default 5-minute TTL).
  3. On miss: `secretsManagerClient.getSecretValue(name)`, deserialize JSON.
- `deleteParameters(connection)`:
  1. `secretsManagerClient.deleteSecret(name, forceDeleteWithoutRecovery=true)`.
  2. Cache evict.

### Module structure

```
server/ee/libs/platform/platform-connection/
  platform-connection-credential-store-aws-secrets-manager/
    build.gradle.kts
    src/main/java/com/bytechef/ee/platform/connection/credential/store/aws/secretsmanager/
      AwsSecretsManagerConnectionCredentialStore.java
      boot/AwsSecretsManagerCredentialStoreEnvironmentPostProcessor.java
      config/AwsSecretsManagerCredentialStoreConfiguration.java
    src/main/resources/META-INF/spring/
      org.springframework.boot.env.EnvironmentPostProcessor.imports
    src/test/java/...
      AwsSecretsManagerConnectionCredentialStoreTest.java   (Mockito)
      AwsSecretsManagerConnectionCredentialStoreIntTest.java (LocalStack via Testcontainers)
```

---

## HashiCorpVaultConnectionCredentialStore (EE)

`server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/`

**Dependency:** `org.springframework.vault:spring-vault-core`. **Version pinned in plan phase** — Spring Vault 3.x targets Spring 6; Spring Vault 4.x (Spring 7-compatible) status against Spring Boot 4.0.6 BOM must be verified before the module is committed. This is the single highest-risk open item in v1.

**Authentication methods supported in v1:** `token` and `approle`. Selected via `bytechef.connection.credential-store.hashicorp-vault.authentication`. K8s / AWS IAM auth deferred.

**KV engine:** KV v2 (default; supports versioning). KV v1 not supported in v1.

**Secret format:** Map serialized as KV v2 data payload — Spring Vault handles JSON serialization.

### Behavior

Identical lifecycle to AWS adapter — generate UUID on first write, write to KV v2 at the template-resolved path, clear entity parameters. Cache shape identical (Caffeine, same TTL property).

### Module structure

```
server/ee/libs/platform/platform-connection/
  platform-connection-credential-store-hashicorp-vault/
    build.gradle.kts
    src/main/java/com/bytechef/ee/platform/connection/credential/store/hashicorp/vault/
      HashiCorpVaultConnectionCredentialStore.java
      boot/HashiCorpVaultCredentialStoreEnvironmentPostProcessor.java
      config/HashiCorpVaultCredentialStoreConfiguration.java
    src/main/resources/META-INF/spring/
      org.springframework.boot.env.EnvironmentPostProcessor.imports
    src/test/java/...
      HashiCorpVaultConnectionCredentialStoreTest.java       (Mockito)
      HashiCorpVaultConnectionCredentialStoreIntTest.java    (Vault dev mode via Testcontainers)
```

---

## ConnectionServiceImpl refactor

```java
@Service("connectionService")
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private final List<ConnectionCredentialStore> connectionCredentialStores;
    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(
        List<ConnectionCredentialStore> connectionCredentialStores,
        ConnectionRepository connectionRepository) {

        this.connectionCredentialStores = connectionCredentialStores;
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(Connection connection) {
        // validation as before
        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        Map<String, ?> parameters = connection.getParameters();

        store.storeParameters(connection, parameters);

        Connection saved = connectionRepository.save(connection);

        return populateParameters(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return populateParameters(
            OptionalUtils.get(connectionRepository.findById(id),
                "Connection does not exist for id=" + id));
    }

    @Override
    public void delete(long id) {
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Connection does not exist for id=" + id));

        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (!store.isReadOnly()) {
            store.deleteParameters(connection);
        }
        // When the active store is read-only, the connection row is deleted but the external
        // secret is left intact — the operator owns the vault's lifecycle. Documented behavior;
        // operator cleanup may be needed if the connection was the only reference.

        connectionRepository.deleteById(id);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        Connection connection = getConnection(connectionId);
        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        Map<String, Object> curParameters = new HashMap<>(store.getParameters(connection));

        curParameters.putAll(parameters);

        store.storeParameters(connection, curParameters);

        return populateParameters(connectionRepository.save(connection));
    }

    @Override
    public Connection registerExisting(
        Connection connection, ConnectionCredentialStoreType storeType, String credentialRef) {

        Assert.isTrue(storeType != ConnectionCredentialStoreType.DATABASE,
            "registerExisting requires an external store");
        Assert.hasText(credentialRef, "'credentialRef' must not be empty");

        ConnectionCredentialStore store = getStore(storeType);

        connection.setCredentialStoreType(storeType);
        connection.setCredentialRef(credentialRef);
        connection.setParameters(Map.of());

        // Probe existence — fail-fast if the secret doesn't actually exist in the external store.
        store.getParameters(connection);

        return connectionRepository.save(connection);
    }

    private ConnectionCredentialStore getStore(ConnectionCredentialStoreType type) {
        return connectionCredentialStores.stream()
            .filter(s -> s.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No ConnectionCredentialStore registered for type %s. ".formatted(type) +
                "Configure bytechef.connection.credential-store.external.provider or migrate this connection to DATABASE."));
    }

    private Connection populateParameters(Connection connection) {
        ConnectionCredentialStore store = getStore(connection.getCredentialStoreType());

        connection.setParameters(store.getParameters(connection));

        return connection;
    }

    // ... existing methods (getConnections variants, update, updateConnectionCredentialStatus)
    //     gain a populateParameters pass through results
}
```

### Read-path eager population

Every read method on `ConnectionService` — `getConnection(id)`, `getConnections(type)`, `getConnections(componentName, ...)`, `getConnections(List<Long>)`, and `updateConnectionParameters`'s implicit refresh — runs results through `populateParameters` before returning. The Connection entity's `parameters` field always reflects the resolved credentials regardless of backend.

- For DB-backed: `store.getParameters(connection)` returns the entity field already populated by JDBC — effectively a no-op redirection.
- For external stores: `store.getParameters(connection)` resolves via cache or vault API. Service stuffs result into `connection.parameters` so downstream callers (`ConnectionFacadeImpl`, `TokenRefreshHandler`, `HttpClientExecutor`, `ActionDefinitionFacadeImpl`, etc.) keep using `connection.getParameters()` unchanged.

**Hot path performance.** List operations populate per element. For a list of N external-backed connections, cache hits dominate after warm-up; only cache misses incur API calls. Cache TTL configurable (5-minute default).

**Lazy alternative considered, rejected.** Migrating ~15 callers to `credentialStore.getParameters(connection)` was high-churn and didn't reduce call count meaningfully — most callers do need credentials.

### TokenRefreshHandler integration

In [`TokenRefreshHandler.java:165`](../../server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/aspect/TokenRefreshHandler.java), before calling `connectionService.updateConnectionParameters(...)`:

```java
ConnectionCredentialStore store = /* obtained via DI */;
if (store.isReadOnly()) {
    log.warn("Cannot refresh token for connection {} — credential store is read-only", connectionId);
    connectionService.updateConnectionCredentialStatus(connectionId, CredentialStatus.INVALID);
    return;
}
```

The user sees the existing "credentials invalid" surface in the UI. Documented constraint: read-only deployments must use long-lived credentials, not OAuth refresh tokens.

---

## Configuration surface

### User-facing (only `bytechef.*`)

```yaml
bytechef:
  cloud:
    provider: aws                              # existing; activates AwsCloudProviderConfiguration

    aws:                                        # existing; reused as-is for credentials/region
      access-key-id: ...
      secret-access-key: ...
      region: us-east-1
      account-id: ...

  connection:
    credential-store:
      external:
        provider: aws-secrets-manager          # or: hashicorp-vault | (unset, default)
      cache:
        ttl: PT5M                              # ISO-8601 duration; PT0S disables cache
      path-template: "bytechef/{ref}"          # provider-specific defaults if unset

      aws-secrets-manager:
        read-only: false

      hashicorp-vault:
        read-only: false
        uri: http://vault:8200
        authentication: token                  # token | approle
        token: ...                             # when authentication=token
        approle:
          role-id: ...                         # when authentication=approle
          secret-id: ...
        kv-mount: secret                       # KV v2 mount path
```

### Internal mapping (via EnvironmentPostProcessor, never set by users)

```yaml
# Set by AwsSecretsManagerCredentialStoreEnvironmentPostProcessor when
# bytechef.connection.credential-store.external.provider == aws-secrets-manager
spring.cloud.aws.secretsmanager.enabled: true

# Set by HashiCorpVaultCredentialStoreEnvironmentPostProcessor when
# bytechef.connection.credential-store.external.provider == hashicorp-vault
spring.vault.uri: ${bytechef.connection.credential-store.hashicorp-vault.uri}
spring.vault.token: ${bytechef.connection.credential-store.hashicorp-vault.token}
spring.vault.authentication: ${bytechef.connection.credential-store.hashicorp-vault.authentication}
# ...other spring.vault.* properties mapped from bytechef.* equivalents
```

### EnvironmentPostProcessor pattern

Each EE adapter ships an `EnvironmentPostProcessor` that:

1. Reads `bytechef.connection.credential-store.external.provider`.
2. If it matches the adapter's provider name, copies ByteChef-namespaced properties into matching `spring.cloud.aws.*` / `spring.vault.*` keys at highest precedence (`MutablePropertySources.addFirst`).
3. Registered in `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports`.

Example shape:

```java
public class HashiCorpVaultCredentialStoreEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String provider = environment.getProperty(
            "bytechef.connection.credential-store.external.provider", String.class);

        if (!"hashicorp-vault".equals(provider)) {
            return;
        }

        Map<String, Object> source = new HashMap<>();

        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.uri", "spring.vault.uri");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.token", "spring.vault.token");
        copyIfPresent(environment, source,
            "bytechef.connection.credential-store.hashicorp-vault.authentication", "spring.vault.authentication");
        // ...approle, kv-mount, etc.

        environment.getPropertySources()
            .addFirst(new MapPropertySource(
                "ByteChef HashiCorp Vault credential store mapping", source));
    }
}
```

The AWS adapter's EnvironmentPostProcessor mirrors this — it sets `spring.cloud.aws.secretsmanager.enabled=true`. Credentials/region are already wired via the existing `AwsCloudProviderConfiguration` consuming `bytechef.cloud.aws.*`.

### Path template resolution

Operators set one property per adapter. Variables available in templates:

- `{prefix}` — operator-configured static prefix (literal token in template, no special handling)
- `{tenant}` — current tenant id, resolved via `TenantContext`
- `{env}` — connection environment (PRODUCTION/STAGING/DEV) lowercased
- `{ref}` — value of `connection.credentialRef`

**Defaults:**
- AWS Secrets Manager: `bytechef/{ref}`
- HashiCorp Vault: `secret/data/bytechef/connections/{ref}` (KV v2)

**Operator override:**
```yaml
bytechef.connection.credential-store.path-template: bytechef/{tenant}/{env}/{ref}
```

Template resolution lives in a shared `CredentialPathResolver` utility in `platform-connection-api` so both adapters share the logic.

### ApplicationProperties additions

```java
public class ApplicationProperties {

    private Connection connection = new Connection();

    public static class Connection {
        private CredentialStore credentialStore = new CredentialStore();

        public static class CredentialStore {
            private External external = new External();
            private Cache cache = new Cache();
            private String pathTemplate;

            private AwsSecretsManager awsSecretsManager = new AwsSecretsManager();
            private HashiCorpVault hashicorpVault = new HashiCorpVault();

            public static class External {
                private String provider;       // null | "aws-secrets-manager" | "hashicorp-vault"
            }

            public static class Cache {
                private Duration ttl = Duration.ofMinutes(5);
            }

            public static class AwsSecretsManager {
                private boolean readOnly;
            }

            public static class HashiCorpVault {
                private boolean readOnly;
                private String uri;
                private String authentication = "token";
                private String token;
                private AppRole approle = new AppRole();
                private String kvMount = "secret";

                public static class AppRole {
                    private String roleId;
                    private String secretId;
                }
            }
        }
    }
}
```

Mounted at `bytechef.connection.credential-store.*`. Misconfiguration fails at startup with clear messages instead of at first API call.

---

## GraphQL info query

UI consumes the list of currently-registered stores to render the per-connection picker:

```graphql
type Query {
    connectionCredentialStores: [ConnectionCredentialStoreInfo!]!
}

type ConnectionCredentialStoreInfo {
    type: ConnectionCredentialStoreType!
    readOnly: Boolean!
}

enum ConnectionCredentialStoreType {
    DATABASE
    AWS_SECRETS_MANAGER
    HASHICORP_VAULT
}
```

Enum values in `SCREAMING_SNAKE_CASE` per project's GraphQL conventions.

### Client wiring

- Operation file: `client/src/graphql/connection/connectionCredentialStores.graphql`
- Schema path added to `client/codegen.ts` `schema` array (if the platform-connection schema isn't already wired — open item for plan phase)
- `cd client && npx graphql-codegen` regenerates `src/shared/middleware/graphql.ts`
- Operation file and generated middleware committed separately per project workflow.

### Resolver

Lives in the existing platform-connection GraphQL resolver module (open item: identify the exact module in plan phase). Backed by a thin service method:

```java
List<ConnectionCredentialStoreInfo> listStores();
```

Iterates the injected `List<ConnectionCredentialStore>`, returns DTOs with type + readOnly.

### UI integration (consumed by sibling frontend spec)

- If list size is 1 (DATABASE only): no picker, every new connection is DB-backed.
- If list size is 2: show picker on connection create. Default to DATABASE. If the external store is read-only, hide the create flow and offer "register existing" instead.
- Settings page surfaces active stores with read-only badges.

---

## AWS KMS as EncryptionKey provider (sibling PR track)

This is documented here for completeness but ships in its own PR with no dependency on the credential-store work.

### Module structure

```
server/ee/libs/core/encryption/encryption-aws-kms/
  build.gradle.kts
  src/main/java/com/bytechef/ee/encryption/aws/kms/
    AwsKmsEncryptionKey.java
    config/AwsKmsEncryptionConfiguration.java
  src/main/resources/META-INF/spring/
    org.springframework.boot.autoconfigure.AutoConfiguration.imports
  src/test/java/...
    AwsKmsEncryptionKeyIntTest.java   (LocalStack via Testcontainers)
```

Mirrors the existing [`encryption-filesystem`](../../server/libs/core/encryption/encryption-filesystem/) and [`encryption-property`](../../server/libs/core/encryption/encryption-property/) modules.

### Configuration

```yaml
bytechef:
  encryption:
    provider: aws-kms             # filesystem | property | aws-kms

    aws-kms:
      key-id: ...                                                # KMS key ARN or alias
      data-key-path: ${user.home}/.bytechef/aws-kms-data-key    # ciphertext storage path; default shown
      # AWS credentials/region reused from bytechef.cloud.aws.* (existing)
```

### Behavior — envelope encryption, not naive caching

The naive "call `GenerateDataKey` and cache the result" pattern is broken: `GenerateDataKey` returns a *new random* plaintext key each call. Caching that for the process lifetime works while the JVM is up, but on restart you get a new random key — and every parameter encrypted with the previous key becomes unreadable.

The correct pattern is **envelope encryption with a persisted ciphertext**:

1. **On first startup** (when the configured `data-key-path` doesn't exist):
   - Call `kmsClient.generateDataKey(keyId, AES_256)`. The response gives both `plaintextBlob` (the actual key) and `ciphertextBlob` (the same key encrypted under the KMS master key).
   - Write `ciphertextBlob` to `data-key-path`. **The plaintext is never written to disk.**
   - Cache `plaintextBlob` in memory; this is what `getKey()` returns for the rest of the process lifetime.

2. **On every subsequent startup** (when `data-key-path` exists):
   - Read the ciphertext from `data-key-path`.
   - Call `kmsClient.decrypt(ciphertextBlob)`. KMS returns the same plaintext as the original `generateDataKey` call.
   - Cache `plaintextBlob` in memory.

3. **Steady state**: `AbstractEncryptionKey.getKey()` returns the cached plaintext (Base64-encoded per the existing `AbstractEncryptionKey` contract). `EncryptedMapWrapper` uses the same key for every connection's parameters column, just like `FileSystemEncryptionKey`.

**Security properties**:
- The plaintext data key never touches disk. An attacker who copies the data key file off the host without also gaining access to the KMS key cannot use it.
- The KMS key itself never leaves AWS. Rotating the underlying KMS key works automatically — KMS preserves the ability to decrypt under the old key version. Hard key destruction (operator deletes the KMS key entirely) destroys the ability to decrypt the data key, which destroys the ability to decrypt parameters. This is intentional.
- The ciphertext file is reusable across restarts and across hosts that share the same KMS access. Operators are responsible for backing it up — losing the file means losing all encrypted parameters (no recovery without the matching KMS access).

**Why not auto-store the ciphertext in the database?** Considered and rejected for v1. Putting the ciphertext in the database would couple credential-store recovery to database availability (which is what KMS-backed encryption is partly trying to decouple from) and would require a new table just for a single blob. Filesystem path is simpler and matches the existing `FileSystemEncryptionKey`'s convention. A future enhancement could add a `database`-backed data-key option behind a config switch.

### Operator workflows

- **Greenfield install**: set `provider: aws-kms` + `key-id`, restart. App generates a fresh data key on first boot.
- **Migrate from `filesystem` to `aws-kms`**: documented as a multi-step manual process (export plaintext data key, wrap with KMS via `aws kms encrypt --plaintext`, write resulting ciphertext to `data-key-path`, flip `provider`). Out of scope for v1 of the AWS KMS PR — separate runbook.
- **KMS key rotation** (KMS-managed): no operator action; AWS handles it transparently via key versions.
- **Disaster recovery** (lost `data-key-path` file): unrecoverable. Operators must back up the file, treat it like other irrecoverable infrastructure secrets. Document this prominently.

### Configuration class

```java
@Configuration
@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "aws-kms")
public class AwsKmsEncryptionConfiguration {

    @Bean
    EncryptionKey encryptionKey(KmsClient kmsClient, ApplicationProperties.Encryption.AwsKms awsKms) {
        return new AwsKmsEncryptionKey(
            kmsClient, awsKms.getKeyId(), Path.of(awsKms.getDataKeyPath()));
    }
}
```

`KmsClient` is auto-configured by Spring Cloud AWS — needs only `bytechef.cloud.provider=aws` to be active (already standard). An `EnvironmentPostProcessor` translates `bytechef.encryption.provider=aws-kms` to `spring.cloud.aws.kms.enabled=true` so the `KmsClient` bean is created.

### Test strategy

- Unit test: `AwsKmsEncryptionKeyTest` with mocked `KmsClient`. Verifies (a) first-boot generates + persists ciphertext, (b) second-boot reads file + decrypts, (c) plaintext is cached after either path.
- Integration test: `AwsKmsEncryptionKeyIntTest` against LocalStack KMS. Round-trips a real generate/decrypt cycle through a real KMS API.

---

## Module placement & PR sequencing

### Credential-store PR series

1. **PR — SPI + DB refactor** (CE)
    - `ConnectionCredentialStore` interface in `platform-connection-api`
    - `ConnectionCredentialStoreType` enum
    - `DatabaseConnectionCredentialStore` in `platform-connection-service`
    - Liquibase migration for `credential_store_type` + `credential_ref` columns
    - `Connection` entity updates
    - `ConnectionServiceImpl` refactor (List-based dispatch, eager populate)
    - `TokenRefreshHandler` read-only check (always false for DATABASE)
    - `registerExisting` service method (no-op practical use yet; sets up SPI)
    - `ReadOnlyCredentialStoreException` typed error
    - Updated integration tests

2. **PR — GraphQL info query** (CE)
    - GraphQL schema + resolver
    - Operation file in `client/`
    - codegen run + committed middleware

3. **PR — AWS Secrets Manager adapter** (EE)
    - Module: `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/`
    - Adapter impl, configuration class, EnvironmentPostProcessor
    - LocalStack-based integration test
    - Updated `ApplicationProperties.Connection.CredentialStore.AwsSecretsManager`

4. **PR — HashiCorp Vault adapter** (EE)
    - Module: `server/ee/libs/platform/platform-connection/platform-connection-credential-store-hashicorp-vault/`
    - Adapter impl, configuration class, EnvironmentPostProcessor
    - Spring Vault dependency added to `libs.versions.toml` (version pinned during this PR's plan phase)
    - Vault dev-mode Testcontainer integration test
    - Updated `ApplicationProperties.Connection.CredentialStore.HashiCorpVault`

5. **Sibling frontend PR series** (separate spec)
    - Settings page (provider + read-only badge)
    - Connection creation form (store picker)
    - "Register existing" form

### Encryption-key PR series (sibling)

6. **PR — AWS KMS EncryptionKey provider** (EE)
    - Module: `server/ee/libs/core/encryption/encryption-aws-kms/`
    - Independent of the credential-store work; ships in any order.

7. **PR — GCP Cloud KMS EncryptionKey provider** (EE, follow-up)

---

## Testing strategy

### Unit tests

- `DatabaseConnectionCredentialStoreTest` — Mockito; verify entity mutations.
- `AwsSecretsManagerConnectionCredentialStoreTest` — Mockito with mocked `SecretsManagerClient`.
- `HashiCorpVaultConnectionCredentialStoreTest` — Mockito with mocked `VaultTemplate` / `VaultOperations`.
- `CredentialPathResolverTest` — covers all template variables + malformed templates.
- `ConnectionServiceImplTest` — Mockito; verifies dispatch logic with multiple stores in the injected list.

### Integration tests

- `ConnectionServiceIntTest` (existing) — extended to cover dispatch with two stores registered. `DatabaseConnectionCredentialStore` always present + a stub `TestExternalCredentialStore` registered in the test config.
- `AwsSecretsManagerConnectionCredentialStoreIntTest` — uses **LocalStack** via Testcontainers (project already uses Testcontainers heavily for PostgreSQL; LocalStack image needs to be added to the relevant test config). Verifies CRUD, cache invalidation, read-only mode failures, path template resolution.
- `HashiCorpVaultConnectionCredentialStoreIntTest` — uses **HashiCorp Vault dev mode** via Testcontainers (`hashicorp/vault` image with `-dev` flag). Same verification matrix as AWS.

### End-to-end

- `ConnectionFacadeIntTest` (existing) — extended to cover `registerExisting` and the GraphQL info query.
- Manual smoke test against real AWS Secrets Manager + real HashiCorp Vault before each adapter PR merge (documented in PR description checklist).

### EE test wiring caveat

Per [memory: SpringBootTest needs explicit ComponentScan for transitive @Service deps](../../.../memory/MEMORY.md), integration tests using the new adapters need their test config to scan the adapter's package. Documented in the adapter PR's plan phase.

---

## Migration story

**Out-of-scope for v1 — flagged as a follow-up workstream.** Reasoning:

- Existing connections all default to `credential_store_type = DATABASE` on schema migration. They keep working unchanged.
- A new connection can be created against either backend (when an external is configured).
- Operators who want to *move* existing connections from DB to vault need tooling. Manual process documented in v1; automation is a follow-up.

### v1 documented operator workaround

1. List DB-backed connections via the GraphQL admin API.
2. For each, decrypt parameters server-side via a helper script (provided in `tools/migrate-credentials/` — open item in plan phase).
3. Provision the equivalent secret in AWS Secrets Manager / Vault via the operator's standard tooling (Terraform, `aws cli`, `vault write`).
4. Update each connection row: set `credential_store_type`, `credential_ref`, clear `parameters`.

### Follow-up spec

"Connection credential migration tooling" — automates the above as an admin GraphQL mutation `migrateConnectionToCredentialStore(connectionId, targetStoreType)`.

---

## Observability

- Cache hit/miss counters per adapter via Micrometer (already in the project).
- External store API call duration timers per adapter.
- Both exposed via the existing `/actuator/metrics` endpoint.
- No additional infrastructure required.

---

## Out of scope

Listed with rationale to capture decisions for future readers:

- **GCP Secret Manager adapter** — follow-up PR. Same shape as AWS Secrets Manager but uses `spring-cloud-gcp-starter-secretmanager`. Trivial once the SPI is proven.
- **Azure Key Vault adapter** — follow-up PR. Same shape, uses Azure SDK for Java.
- **Multi-external activation** — explicitly chosen against. SPI supports it (List dispatch already in place); only configuration and UI would change.
- **Automatic credential migration tooling** — follow-up, see Migration story.
- **Secret rotation orchestration** — vaults manage rotation; ByteChef reads.
- **Per-connection cache TTL override** — single global TTL in v1. Per-connection override would add UI complexity without clear demand.
- **Audit logging of credential reads** — vaults provide their own audit; duplicating is wasteful.
- **GraphQL mutation to switch a connection's store** — see Migration story.
- **K8s service account auth, AWS IAM auth for HashiCorp Vault** — Token + AppRole cover the common cases; other auth methods are follow-up workstreams.
- **Frontend UI** — sibling spec.

---

## Open items for the plan phase

These are decisions that need concrete resolution before implementation starts. Each PR's plan phase resolves the items relevant to its scope.

- **Spring Vault version** — verify compatibility with Spring Boot 4.0.6 / Spring Framework 7. Spring Vault 4.x targets Spring 7; current GA status needs checking against the Spring Boot 4 BOM. **Highest-risk open item.** Falls in the HashiCorp Vault adapter PR.
- **GraphQL resolver module** — identify existing platform-connection GraphQL module or create a new one. Falls in the GraphQL query PR.
- **LocalStack image version** — pin in test config. Falls in the AWS Secrets Manager adapter PR.
- **Vault Testcontainer image version** — pin in test config. Falls in the HashiCorp Vault adapter PR.
- **Migration helper script structure** — `tools/migrate-credentials/` directory layout, language (Java? Shell?), distribution channel. Falls in the migration tooling spec, not v1.
- **AWS KMS data key caching strategy** — single cached key vs. periodic refresh. Falls in the AWS KMS PR.

---

## Implementation references

- [`Connection.java`](../../server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/domain/Connection.java) — entity to extend with `credentialStoreType` + `credentialRef`
- [`ConnectionService.java`](../../server/libs/platform/platform-connection/platform-connection-api/src/main/java/com/bytechef/platform/connection/service/ConnectionService.java) — SPI to extend with `registerExisting`
- [`ConnectionServiceImpl.java`](../../server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/service/ConnectionServiceImpl.java) — implementation to refactor
- [`ConnectionRepository.java`](../../server/libs/platform/platform-connection/platform-connection-service/src/main/java/com/bytechef/platform/connection/repository/ConnectionRepository.java) — entity reads with `credentialRef` populated automatically via JDBC
- [`AwsCloudProviderConfiguration.java`](../../server/ee/libs/core/cloud/cloud-aws/src/main/java/com/bytechef/ee/cloud/aws/config/AwsCloudProviderConfiguration.java) — existing pattern for ByteChef-namespaced AWS config reused by AWS Secrets Manager adapter
- [`AwsFileStorageEnvironmentPostProcessor.java`](../../server/ee/libs/core/file-storage/file-storage-aws/file-storage-aws-impl/src/main/java/com/bytechef/ee/file/storage/aws/boot/AwsFileStorageEnvironmentPostProcessor.java) — template for the credential-store EnvironmentPostProcessors
- [`FileSystemEncryptionConfiguration.java`](../../server/libs/core/encryption/encryption-filesystem/src/main/java/com/bytechef/encryption/filesystem/config/FileSystemEncryptionConfiguration.java) — template for the AWS KMS sibling module
- [`TokenRefreshHandler.java`](../../server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/aspect/TokenRefreshHandler.java) — caller updated to check `isReadOnly()` before refresh
