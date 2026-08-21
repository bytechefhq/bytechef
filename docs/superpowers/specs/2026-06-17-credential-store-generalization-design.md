# Credential store generalization — one SPI for connections and properties

**Status:** Design | **Owner:** Ivica | **Created:** 2026-06-17 | **Last updated:** 2026-06-17 | **Predecessor:** [2026-05-19 Connection credential store](2026-05-19-connection-credential-store-design.md)

## Why

The [connection credential store](2026-05-19-connection-credential-store-design.md) (shipped starting with commit `cad7c7a`) introduced a `ConnectionCredentialStore` SPI so a connection's credential payload can live in PostgreSQL (default) or an external secret store (AWS Secrets Manager / HashiCorp Vault) instead of the encrypted `connection.parameters` column. That design was deliberately scoped to connections: the SPI, the discriminator enum, the exception, the `bytechef.connection.credential-store.*` config tree, and the `CredentialPathResolver` all live in connection-namespaced packages, and the AWS/Vault adapters are typed to `Connection`.

`Property` (the scoped key/value configuration entity in `platform-configuration`) holds the same *kind* of sensitive material — AI-provider API keys, OAuth app client secrets — encrypted in a `property.value` column via the identical `EncryptedMapWrapper` mechanism. Operators who off-load connection credentials to a vault have the same defense-in-depth, bring-your-own-vault, and existing-policy reasons (see the predecessor's *Why*) for wanting property secrets out of the application database too.

A first pass added a parallel, property-specific `PropertyCredentialStore` SPI (DATABASE-only) plus the `credential_store_type` / `credential_ref` columns on the `property` table. That mechanism works but stops at the database; reaching the external stores by duplicating the AWS/Vault adapters per entity would mean two adapter codebases, two AWS clients, two caches, and two config trees. This spec instead **generalizes** the credential-store mechanism so a single adapter per provider serves every credential-bearing entity.

## What

Lift the credential-store abstraction out of `platform-connection` into a neutral `platform-credential-store` module. Introduce a small entity-facing seam — `CredentialSecret` — that both `Connection` and `Property` implement, retype the SPI and the AWS/Vault adapters to operate on `CredentialSecret`, and collapse the two discriminator enums into one shared `CredentialStoreType`. An operator activates at most one external store via a single neutral `bytechef.credential-store.external.provider` switch; the resulting adapter bean is injected into both `ConnectionServiceImpl` and `PropertyServiceImpl`.

## Goals

1. A single `CredentialStore` SPI, operating on a domain-neutral `CredentialSecret`, that owns the credential-payload lifecycle for any credential-bearing entity.
2. One `DatabaseCredentialStore` (always registered) and one external adapter per provider (AWS Secrets Manager, HashiCorp Vault), each a single bean serving both connections and properties.
3. One neutral config namespace, `bytechef.credential-store.*`, replacing `bytechef.connection.credential-store.*`. Turning on an external store covers all credential-bearing entities at once.
4. Zero behavior change for connections beyond retyping: a deployment with no external store behaves exactly as today; existing connection rows are untouched (ordinals preserved).
5. Property write routing: when an external store is active, **all** property writes go to it; otherwise they stay in the encrypted DATABASE column. Existing DATABASE rows migrate to the external store the next time they are written.
6. Existing callers (`connection.getParameters()`, `property.getValue()`) keep working unchanged — services eagerly populate the inline payload field on read.

## Non-goals

- **Multiple simultaneous external stores.** Inherited from the predecessor: Database + at-most-one-external. List-based dispatch already supports more; only config/UI would change.
- **GCP Secret Manager / Azure Key Vault adapters.** Follow-up, unchanged from predecessor.
- **A property store picker UI.** Properties are written server-side and auto-route; there is no per-property UI affordance. (Connections keep their existing per-connection picker.)
- **Automatic bulk migration tooling.** Existing rows migrate lazily on next write. A bulk admin migration remains a follow-up, as in the predecessor.
- **Per-scope / per-key property routing.** v1 routes *all* property writes to the active external store (explicit decision — see *Property write routing*). A scope/key allowlist is a possible follow-up if vault-entry volume for non-secret config becomes a concern.
- **AWS KMS `EncryptionKey` work.** Orthogonal sibling track from the predecessor; untouched here.

---

## Architecture overview

The two orthogonal SPIs from the predecessor remain orthogonal. This spec only touches the second one and renames nothing about the first:

1. **`EncryptionKey`** (existing) — where the symmetric key for the inline encrypted column comes from. Unchanged.
2. **`CredentialStore`** (was `ConnectionCredentialStore`) — where the credential payload itself lives. Generalized here.

### Module layout

```
server/libs/platform/platform-credential-store/
  platform-credential-store-api/         <- CredentialSecret, CredentialStore, CredentialStoreType,
                                             CredentialPathResolver, ReadOnlyCredentialStoreException,
                                             CredentialStoreErrorType
  platform-credential-store-service/      <- DatabaseCredentialStore (always-registered @Component)

server/ee/libs/platform/platform-credential-store/
  credential-store-aws-secrets-manager/   <- relocated from platform-connection-credential-store-aws-secrets-manager
  credential-store-hashicorp-vault/        <- relocated from platform-connection-credential-store-hashicorp-vault
```

Dependency direction:

- `platform-connection-api` and `platform-configuration-api` depend on `platform-credential-store-api` (their entities implement `CredentialSecret`).
- `platform-connection-service` and `platform-configuration-service` depend on `platform-credential-store-service` (to pull the always-registered `DatabaseCredentialStore` onto the classpath).
- The EE adapter modules depend on `platform-credential-store-api` only — no longer on `platform-connection-*`.

`CredentialStore` references `CredentialSecret` only (never `Connection` or `Property`), so the SPI sits cleanly in the neutral module with no back-dependency on either domain.

### Storage mode invariant (per credential-bearing row)

Identical to the predecessor, now applied to both `connection` and `property` rows:

| `credential_store_type` | `credential_ref` | inline payload column | Mode |
|---|---|---|---|
| 0 (DATABASE) | null | encrypted payload | DB-backed |
| 1 (AWS_SECRETS_MANAGER) | UUID | empty | AWS-backed |
| 2 (HASHICORP_VAULT) | UUID | empty | Vault-backed |

Enforced at the SPI level, not via DB constraint (empty payload + null ref is also valid for entities with no secret material).

---

## The `CredentialSecret` seam

The entity-facing interface that makes one adapter serve many entities. It is exactly the surface the stores touch — nothing more.

```java
package com.bytechef.platform.credential.store;

import java.util.Map;
import org.jspecify.annotations.Nullable;

public interface CredentialSecret {

    @Nullable
    String getCredentialRef();

    void setCredentialRef(@Nullable String credentialRef);

    /** The inline (in-entity) decrypted payload. Read by DatabaseCredentialStore. */
    Map<String, ?> getPayload();

    /** Set or clear the inline payload. External stores call this with Map.of() to clear. */
    void setPayload(Map<String, ?> payload);

    CredentialStoreType getCredentialStoreType();

    void setCredentialStoreType(CredentialStoreType credentialStoreType);
}
```

### Entity adaptations

`Connection` (a `final` class — implementing an interface is fine):

```java
public final class Connection implements CredentialSecret {
    // credentialRef, credentialStoreType already exist (retyped to shared CredentialStoreType)

    @Override
    public Map<String, ?> getPayload() {
        return getParameters();
    }

    @Override
    public void setPayload(Map<String, ?> payload) {
        setParameters(payload);
    }
}
```

`Property`:

```java
public class Property implements CredentialSecret {
    // credentialRef, credentialStoreType columns added in the first pass (retyped to shared enum)

    @Override
    public Map<String, ?> getPayload() {
        return getValue();
    }

    @Override
    public void setPayload(Map<String, ?> payload) {
        setValue(payload);
    }
}
```

`Connection.getParameters()` and `Property.getValue()` both already return `Map<String, ?>`, matching `getPayload()`.

> **Decision — non-generic seam.** A generic `CredentialStore<T extends CredentialSecret>` was considered and rejected: the adapters never need the concrete subtype, and the plain interface keeps the SPI, the `List<CredentialStore>` injection, and the GraphQL controller free of type parameters.

---

## The `CredentialStore` SPI

```java
package com.bytechef.platform.credential.store;

import java.util.Map;

public interface CredentialStore {

    CredentialStoreType getType();

    boolean isReadOnly();

    Map<String, ?> getSecret(CredentialSecret secret);

    /** Persist the payload. Called BEFORE the row is saved; may mutate the entity
     *  (set credentialRef, clear inline payload). Throws UnsupportedOperationException on read-only stores. */
    void storeSecret(CredentialSecret secret, Map<String, ?> payload);

    /** Remove the payload. Called BEFORE the row is deleted. Throws on read-only stores. */
    void deleteSecret(CredentialSecret secret);
}

public enum CredentialStoreType {
    DATABASE,             // ordinal 0
    AWS_SECRETS_MANAGER,  // ordinal 1
    HASHICORP_VAULT       // ordinal 2
}
```

Ordinals are identical to the predecessor's `ConnectionCredentialStoreType`, so existing `connection.credential_store_type` and `property.credential_store_type` values remain valid with no data migration. New values are appended (never inserted) per the enum-storage convention.

### `DatabaseCredentialStore` (CE, always registered)

```java
@Component
public class DatabaseCredentialStore implements CredentialStore {

    @Override public CredentialStoreType getType() { return CredentialStoreType.DATABASE; }

    @Override public boolean isReadOnly() { return false; }

    @Override public Map<String, ?> getSecret(CredentialSecret secret) { return secret.getPayload(); }

    @Override public void storeSecret(CredentialSecret secret, Map<String, ?> payload) { secret.setPayload(payload); }

    @Override public void deleteSecret(CredentialSecret secret) { /* cleared with the row */ }
}
```

### External adapters (EE)

`AwsSecretsManagerCredentialStore` and `HashiCorpVaultCredentialStore` are the predecessor's adapters with `Connection` replaced by `CredentialSecret`. The only call sites that change:

- `connection.getCredentialRef()` → `secret.getCredentialRef()`
- `connection.setCredentialRef(ref)` → `secret.setCredentialRef(ref)`
- `connection.setParameters(Map.of())` → `secret.setPayload(Map.of())`
- the read of config moves from `applicationProperties.getConnection().getCredentialStore()` to `applicationProperties.getCredentialStore()`

Lifecycle, caching (Caffeine, global TTL), path-template resolution, read-only behavior, and the `EnvironmentPostProcessor` translation to `spring.cloud.aws.*` / `spring.vault.*` are unchanged. Each adapter remains a single opt-in bean selected by `@ConditionalOnProperty(prefix = "bytechef.credential-store.external", name = "provider", havingValue = "...")`.

---

## Service wiring

### `ConnectionServiceImpl`

`List<ConnectionCredentialStore>` → `List<CredentialStore>`; `getStore(...)`, `populateParameters(...)`, `registerExisting(...)` retyped to the shared `CredentialStore` / `CredentialStoreType`. Routing semantics unchanged: new connections default to DATABASE and the existing per-connection picker chooses the backend. No behavioral change.

### `PropertyServiceImpl`

Injected `List<CredentialStore>`. The dispatch helpers mirror the connection service, plus a routing helper:

```java
/** The active external store if one is registered, else the DATABASE store. */
private CredentialStore resolveTargetStore() {
    return credentialStores.stream()
        .filter(store -> store.getType() != CredentialStoreType.DATABASE)
        .findFirst()
        .orElseGet(() -> getStore(CredentialStoreType.DATABASE));
}
```

`save(...)` determines the target store via `resolveTargetStore()`, sets `credentialStoreType` on the entity, then `storeSecret`. Because the target is recomputed on every write, a re-saved DATABASE property migrates to the external store when one becomes active, and an external property re-saved while no external is registered migrates back to DATABASE — see *Re-save store switch*.

`fetchProperty` / `getProperties` run results through `populateValue` (dispatching on the row's own `credentialStoreType`). `delete` calls `deleteSecret` when the store is not read-only. Read/write read-only guards throw `ReadOnlyCredentialStoreException`.

> The first-pass property-specific `PropertyCredentialStore`, `PropertyCredentialStoreType`, `DatabasePropertyCredentialStore`, and `com.bytechef.platform.configuration.exception.ReadOnlyCredentialStoreException` are deleted; their roles move to the shared module. The `property.credential_store_type` / `credential_ref` columns and their Liquibase migration **stay**.

### Re-save store switch

When `save(...)` targets a store different from the row's current `credentialStoreType`, the old payload is removed before switching, so migrating back from an external store to DATABASE does not orphan the external secret:

```java
CredentialStore target = resolveTargetStore();
CredentialStore current = getStore(property.getCredentialStoreType());

if (current.getType() != target.getType() && !current.isReadOnly()) {
    current.deleteSecret(property);   // remove old (external secret or no-op for DB)
}

property.setCredentialStoreType(target.getType());
property.setCredentialRef(null);      // target mints a fresh ref if external

target.storeSecret(property, value);
```

If the current store is read-only, the old external secret is left intact (operator owns the vault lifecycle), matching the predecessor's delete-path behavior.

---

## Property write routing — explicit trade-off

v1 routes **all** property writes to the active external store, not just secret-bearing keys. The `property` table also holds non-secret scoped configuration; under this policy those values land in the vault too, consuming vault entries for material that has no confidentiality requirement.

This was chosen for simplicity and predictability (one rule, no per-key classification). The cost is documented here so a future reader understands it was deliberate. If vault-entry volume or operator confusion over non-secret config in the vault becomes a problem, the follow-up is a scope/key allowlist that keeps non-secret properties in DATABASE — the SPI already supports per-row routing, so only `resolveTargetStore()` and a config rule would change.

---

## Configuration surface

The predecessor's `bytechef.connection.credential-store.*` tree moves verbatim (same fields) to `bytechef.credential-store.*`:

```yaml
bytechef:
  cloud:
    provider: aws                      # existing; unchanged

  credential-store:                    # was: bytechef.connection.credential-store
    external:
      provider: aws-secrets-manager    # or: hashicorp-vault | (unset, default)
    cache:
      ttl: PT5M
    path-template: "bytechef/{ref}"    # provider defaults apply if unset

    aws-secrets-manager:
      read-only: false

    hashicorp-vault:
      read-only: false
      uri: http://vault:8200
      authentication: token            # token | approle
      token: ...
      approle:
        role-id: ...
        secret-id: ...
      kv-mount: secret
```

`ApplicationProperties.Connection.CredentialStore` becomes top-level `ApplicationProperties.CredentialStore` (nested classes `External`, `Cache`, `AwsSecretsManager`, `HashiCorpVault`, `AppRole` unchanged). Both `EnvironmentPostProcessor`s and both adapter `@ConditionalOnProperty` prefixes change from `bytechef.connection.credential-store.external` to `bytechef.credential-store.external`.

> **Breaking config change (acceptable pre-release on `0_732`).** Any deployment setting `bytechef.connection.credential-store.*` must move to `bytechef.credential-store.*`. There is no compatibility shim; the predecessor only just merged and has no released consumers.

### Path template

Default `bytechef/{ref}` (AWS) / `secret/data/bytechef/{ref}` (Vault KV v2). `{ref}` is a per-secret UUID, so connection and property secrets never collide under one template. Operators may add `{tenant}` / `{env}` as before. `CredentialPathResolver` moves to `platform-credential-store-api` unchanged.

---

## Client-facing surface

- **GraphQL.** The `connectionCredentialStores` query and its resolver are retyped Java-side to the shared `CredentialStore` / `CredentialStoreType`. The GraphQL type names (`ConnectionCredentialStoreInfo`, `ConnectionCredentialStoreType`) and query name are **kept** to avoid client codegen churn; they map to the shared Java enum. No property-side GraphQL query is added (properties auto-route, no picker).
- **REST.** `CredentialStoreTypeMapper` in the automation and embedded REST modules retypes to the shared enum. The OpenAPI `credentialStoreType` field is unchanged on the wire.

---

## Migration & compatibility

- **No new Liquibase migration.** Both `connection` and `property` already carry `credential_store_type` (default 0) and `credential_ref`. Shared-enum ordinals match the old connection enum, so all existing rows remain valid.
- **Connections:** purely a retype + module move. Existing rows and behavior unchanged.
- **Properties:** existing rows are DATABASE (ordinal 0). With no external store configured they stay DB-backed. With an external store configured they migrate to it on next write (see *Re-save store switch*).
- **EE adapter module relocation** updates `settings.gradle.kts` and any app `build.gradle.kts` that wired the old `platform-connection-credential-store-*` modules.

---

## Testing strategy

### Unit
- `CredentialPathResolverTest` — moved to the neutral module; coverage unchanged.
- `DatabaseCredentialStoreTest` — Mockito over a `CredentialSecret` fixture; verifies payload round-trip and no-op delete.
- `AwsSecretsManagerCredentialStoreTest` / `HashiCorpVaultCredentialStoreTest` — retyped to `CredentialSecret`; mocked `SecretsManagerClient` / `VaultTemplate`.
- `ConnectionServiceImplTest` — retyped; dispatch with multiple stores in the list (unchanged intent).
- `PropertyServiceImplTest` — **new**; verifies `resolveTargetStore()` routes to a stub external store when registered, falls back to DATABASE otherwise, and that re-save migrates and clears the old store.

### Integration
- `ConnectionServiceIntTest` — retyped; stays green with `DatabaseCredentialStore` + a `TestExternalCredentialStore` stub.
- `PropertyServiceIntTest` — **new**; property CRUD with DATABASE only, and with a stub external registered to exercise the route-to-external policy and migration-on-write.
- `AwsSecretsManagerCredentialStoreIntTest` / `HashiCorpVaultCredentialStoreIntTest` — relocated; LocalStack / Vault dev-mode Testcontainers, exercised through a `CredentialSecret` (a `Connection` or `Property` fixture).

### EE test wiring
Per the established caveat, EE integration tests using the relocated adapters scan the adapter package in their test config.

---

## Module placement & sequencing

A natural commit sequence (each step compiles and keeps tests green):

1. **Neutral module + seam (CE).** Create `platform-credential-store-{api,service}` with `CredentialSecret`, `CredentialStore`, `CredentialStoreType`, `CredentialPathResolver` (moved), `DatabaseCredentialStore`, `ReadOnlyCredentialStoreException`, `CredentialStoreErrorType`.
2. **Retype connection (CE).** `Connection implements CredentialSecret`; `ConnectionServiceImpl`, `registerExisting`, GraphQL/REST mappers, and the old connection SPI/enum/exception switch to the shared types; delete the connection-namespaced `ConnectionCredentialStore` / `ConnectionCredentialStoreType` / connection `ReadOnlyCredentialStoreException`.
3. **Retype property (CE).** `Property implements CredentialSecret`; `PropertyServiceImpl` route-to-external policy; delete the first-pass property SPI/enum/store/exception.
4. **Config namespace (CE).** `ApplicationProperties.CredentialStore` top-level; update YAML and references.
5. **Relocate EE adapters (EE).** Move both adapter modules under `platform-credential-store`, retype to `CredentialSecret`, repoint `@ConditionalOnProperty` and `EnvironmentPostProcessor` prefixes, update `settings.gradle.kts` / app wiring, relocate int tests.

Steps 2–4 can land together if splitting proves awkward, but 1 must precede them and 5 must come last.

---

## Open items for the plan phase

- **Exact home of `ApplicationProperties.CredentialStore`** — it currently lives in `app-config`'s single `ApplicationProperties`; confirm the top-level placement and field-move keeps Spring Boot relaxed-binding intact.
- **GraphQL type rename vs keep** — confirm keeping `ConnectionCredentialStoreType` as the GraphQL enum name (mapping to shared Java enum) is acceptable, or rename to a neutral `CredentialStoreType` GraphQL type and regenerate the client.
- **App `build.gradle.kts` wiring** — enumerate every app that depended on the old `platform-connection-credential-store-*` modules and repoint to the relocated EE modules + `platform-credential-store-service`.
- **Spotless EE headers** — relocated adapter files keep their `@version ee` Javadoc / Enterprise header; the new CE neutral-module files take the Apache header.
