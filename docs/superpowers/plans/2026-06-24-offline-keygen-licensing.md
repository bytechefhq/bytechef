# Offline Keygen.sh Licensing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the three licence checkers with a single edition-aware `LicenceManager` built on Keygen.sh signed `.lic` files verified fully offline (Ed25519), carrying typed feature entitlements + a per-month `allowedJobs` limit, gated at runtime with a central EE guard + job metering, and managed via an admin Settings UI.

**Architecture:** A CE-visible `licence-api` defines `LicenceManager`; CE wires a `NoOpLicenceManager`, EE wires an `OfflineLicenceManager` (Ed25519 verify + DB persistence + env/file bootstrap + optional online check-in). A central EE guard (REST `HandlerInterceptor` + GraphQL `Instrumentation`) refuses EE endpoints when the licence status ∉ {VALID, GRACE}. Job creation is metered per-tenant/per-month at the platform `PrincipalJobFacadeImpl` against a `licence_job_usage` counter.

**Tech Stack:** Java 25, Spring Boot 4.0.7, Spring Data JDBC, Spring for GraphQL, graphql-java `Instrumentation`, JDK-native Ed25519 (`java.security.Signature` "Ed25519"), Jackson (`tools.jackson`), Liquibase, React 19 + TypeScript + GraphQL codegen, JUnit 5.

## Global Constraints

- **Spec:** `docs/superpowers/specs/2026-06-24-offline-keygen-licensing-design.md`.
- **Naming:** British `licence` for all Java packages/classes/modules/config keys; "License" only in user-facing UI copy.
- **EE files:** every file under `server/ee/` MUST use the ByteChef Enterprise license header (4-line form below) AND carry a `@version ee` Javadoc tag. CE files keep the Apache header. Spotless selects the header by file content.
- **EE Enterprise header (verbatim):**
  ```
  /*
   * Copyright 2025 ByteChef
   *
   * Licensed under the ByteChef Enterprise license (the "Enterprise License");
   * you may not use this file except in compliance with the Enterprise License.
   */
  ```
- **Edition gating:** `@ConditionalOnEEVersion` (`com.bytechef.platform.annotation.ConditionalOnEEVersion`) on EE beans; `@ConditionalOnCEVersion` on the CE NoOp.
- **Enum storage:** persist enums as INT ordinals; append new values at the end (ordinal stability).
- **Repositories** live in `*-service` modules, never `*-api`.
- **New Spring Data JDBC module:** add `@AutoConfiguration` + `@EnableJdbcRepositories(basePackages=...)` + `@ConditionalOnBean(AbstractJdbcConfiguration.class)`, register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, depend on `spring-boot-autoconfigure`.
- **Blank-line/style conventions** from CLAUDE.md (blank line before control statements, after variable modification, no trailing blank line in class body, no `_` prefixes, descriptive names, no over-chaining). Run `./gradlew spotlessApply` before every commit.
- **Test naming:** unit tests end `Test` (drop `Impl`); integration tests end `IntTest`; test methods camelCase, no underscores.
- **Commit message convention:** server-side `<description>`; end every commit body with `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
- **Build/verify:** `./gradlew spotlessApply && ./gradlew check` server-side; `cd client && npm run check` client-side.
- **JSON-in-Java literals:** where a task shows JSON embedded in a Java string (test fixtures building licence envelopes/datasets, the interceptor body), prefer Java text blocks (`"""`) to avoid `\"` escaping mistakes. Some illustrative snippets below use escaped quotes for brevity; convert them to text blocks or `String.formatted(...)` when writing the real file, and let compilation/tests confirm correctness.

---

## File Structure

**`server/libs/licence/licence-api`** (CE-visible, no Spring):
- `LicenceFeature.java` — enum + wire keys.
- `LicenceStatus.java` — enum.
- `Licence.java` — immutable record.
- `LicenceException.java` — `RuntimeException`.
- `LicenceManager.java` — interface.
- DELETE `LicenceChecker.java`.

**`server/libs/licence/licence-service`** (renamed from `licence-impl`, CE):
- `NoOpLicenceManager.java` — `@ConditionalOnCEVersion`.
- DELETE `NoOpLicenceChecker.java`.

**`server/ee/libs/licence/licence-service`** (renamed from `licence-impl`, EE):
- `Ed25519Verifier.java`, `LicenceFileParser.java`.
- `LicenceProperties.java`, `OfflineLicenceManager.java`, `LicenceCheckInTask.java`.
- `domain/LicenceEntity.java`, `repository/LicenceRepository.java`.
- `config/LicenceConfiguration.java` (`@AutoConfiguration`).
- `resources/config/liquibase/changelog/platform/licence/20260624000001_licence_init.xml`.
- `resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- DELETE `CloudLicenceChecker.java`, `SelfHostedLicenceChecker.java`.

**`server/ee/libs/licence/licence-web`** (NEW, EE): central guard.
- `LicenceEnforcementHandlerInterceptor.java`, `EeGraphQlFieldRegistry.java`, `LicenceEnforcementInstrumentation.java`, `config/LicenceWebConfiguration.java`.

**`server/ee/libs/licence/licence-graphql`** (NEW, EE): admin API.
- `LicenceGraphQlController.java`, `resources/graphql/licence.graphqls`.

**`platform-workflow-execution-service`** (existing platform module): job metering.
- `domain/LicenceJobUsage.java`, `repository/LicenceJobUsageRepository.java`, `service/LicenceJobUsageService.java`, `exception/JobLimitExceededException.java`.
- Modify `PrincipalJobFacadeImpl.java`, its config, `build.gradle.kts`.
- `resources/.../changelog/platform/licence_job_usage/20260624000002_licence_job_usage_init.xml`.

**Client:** `client/src/graphql/platform/license/*.graphql`, `client/src/ee/pages/settings/platform/license/*`, modify `client/src/routes.tsx`, `client/codegen.ts`.

**Root:** `settings.gradle.kts`, `server/libs/config/liquibase-config/.../master.xml`.

---

## PHASE 1 — CE foundation (`licence-api` + CE NoOp)

### Task 1: `licence-api` domain types + manager interface

**Files:**
- Create: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/LicenceFeature.java`
- Create: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/LicenceStatus.java`
- Create: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/Licence.java`
- Create: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/LicenceException.java`
- Create: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/LicenceManager.java`
- Delete: `server/libs/licence/licence-api/src/main/java/com/bytechef/platform/licence/LicenceChecker.java`
- Test: `server/libs/licence/licence-api/src/test/java/com/bytechef/platform/licence/LicenceFeatureTest.java`

**Interfaces:**
- Produces: `LicenceFeature` (enum, methods `String getKey()`, static `Optional<LicenceFeature> ofKey(String)`); `LicenceStatus` (enum `CE, VALID, GRACE, EXPIRED, MISSING, INVALID`, method `boolean isActive()` true for VALID+GRACE); `Licence` (record); `LicenceException extends RuntimeException`; `LicenceManager` (interface).

- [ ] **Step 1: Write the failing test** (Apache header)

```java
package com.bytechef.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class LicenceFeatureTest {

    @Test
    void testOfKeyResolvesKnownKey() {
        assertThat(LicenceFeature.ofKey("sso")).contains(LicenceFeature.SSO);
    }

    @Test
    void testOfKeyReturnsEmptyForUnknownKey() {
        assertThat(LicenceFeature.ofKey("does-not-exist")).isEmpty();
    }

    @Test
    void testEveryFeatureHasUniqueLowerCaseKey() {
        long distinct = java.util.Arrays.stream(LicenceFeature.values())
            .map(LicenceFeature::getKey)
            .distinct()
            .count();

        assertThat(distinct).isEqualTo(LicenceFeature.values().length);
    }

    @Test
    void testStatusIsActive() {
        assertThat(LicenceStatus.VALID.isActive()).isTrue();
        assertThat(LicenceStatus.GRACE.isActive()).isTrue();
        assertThat(LicenceStatus.EXPIRED.isActive()).isFalse();
        assertThat(LicenceStatus.MISSING.isActive()).isFalse();
        assertThat(LicenceStatus.INVALID.isActive()).isFalse();
        assertThat(LicenceStatus.CE.isActive()).isFalse();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:libs:licence:licence-api:test --tests '*LicenceFeatureTest'`
Expected: FAIL — compilation error, classes don't exist.

- [ ] **Step 3: Create the types** (all Apache header)

`LicenceFeature.java`:
```java
package com.bytechef.platform.licence;

import java.util.Optional;

public enum LicenceFeature {

    SSO("sso"),
    AUDIT_LOG("audit-log"),
    CUSTOM_COMPONENTS("custom-components"),
    COMPONENT_POLICIES("component-policies"),
    API_CONNECTORS("api-connectors"),
    AI_PROVIDERS("ai-providers"),
    AI_COPILOT("ai-copilot"),
    GIT_SYNC("git-sync"),
    ADMIN_API_KEYS("admin-api-keys"),
    CONNECTION_VISIBILITY("connection-visibility"),
    MCP_SERVER("mcp-server");

    private final String key;

    LicenceFeature(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Optional<LicenceFeature> ofKey(String key) {
        for (LicenceFeature feature : values()) {
            if (feature.key.equals(key)) {
                return Optional.of(feature);
            }
        }

        return Optional.empty();
    }
}
```

`LicenceStatus.java`:
```java
package com.bytechef.platform.licence;

public enum LicenceStatus {

    CE,
    VALID,
    GRACE,
    EXPIRED,
    MISSING,
    INVALID;

    public boolean isActive() {
        return this == VALID || this == GRACE;
    }
}
```

`Licence.java`:
```java
package com.bytechef.platform.licence;

import java.time.Instant;
import java.util.Set;

public record Licence(
    String id, String holderName, String holderEmail, Instant issuedAt, Instant expiresAt,
    Set<LicenceFeature> features, long allowedJobs, Integer maxUsers) {
}
```

`LicenceException.java`:
```java
package com.bytechef.platform.licence;

public class LicenceException extends RuntimeException {

    public LicenceException(String message) {
        super(message);
    }

    public LicenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

`LicenceManager.java`:
```java
package com.bytechef.platform.licence;

import java.util.Optional;

public interface LicenceManager {

    LicenceStatus getStatus();

    Optional<Licence> getLicence();

    boolean isFeatureEnabled(LicenceFeature licenceFeature);

    void checkFeature(LicenceFeature licenceFeature);

    long getAllowedJobs();

    Licence upload(byte[] licenceFileBytes);

    void delete();
}
```

Delete `LicenceChecker.java`.

- [ ] **Step 4: Ensure the test module can compile tests** — add JUnit/AssertJ test deps to `server/libs/licence/licence-api/build.gradle.kts`:

```kotlin
dependencies {
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:libs:licence:licence-api:test --tests '*LicenceFeatureTest'`
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add server/libs/licence/licence-api
git commit -m "Add LicenceManager api: Licence, LicenceFeature, LicenceStatus

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 2: CE `NoOpLicenceManager` + module rename to `licence-service`

**Files:**
- Create dir/module: `server/libs/licence/licence-service/` (rename from `licence-impl`).
- Create: `server/libs/licence/licence-service/src/main/java/com/bytechef/platform/licence/NoOpLicenceManager.java`
- Create: `server/libs/licence/licence-service/build.gradle.kts`
- Delete: `server/libs/licence/licence-impl/` (incl. `NoOpLicenceChecker.java`).
- Modify: `settings.gradle.kts:171` (`licence-impl` → `licence-service`).
- Test: `server/libs/licence/licence-service/src/test/java/com/bytechef/platform/licence/NoOpLicenceManagerTest.java`

**Interfaces:**
- Consumes: `LicenceManager`, `LicenceStatus`, `LicenceFeature`, `LicenceException` (Task 1).
- Produces: `NoOpLicenceManager implements LicenceManager`, `@Component @ConditionalOnCEVersion`.

- [ ] **Step 1: Write the failing test** (Apache header)

```java
package com.bytechef.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NoOpLicenceManagerTest {

    private final NoOpLicenceManager licenceManager = new NoOpLicenceManager();

    @Test
    void testStatusIsCe() {
        assertThat(licenceManager.getStatus()).isEqualTo(LicenceStatus.CE);
    }

    @Test
    void testAllowedJobsUnlimited() {
        assertThat(licenceManager.getAllowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testNoFeaturesEnabled() {
        assertThat(licenceManager.isFeatureEnabled(LicenceFeature.SSO)).isFalse();
    }

    @Test
    void testCheckFeatureThrows() {
        assertThatThrownBy(() -> licenceManager.checkFeature(LicenceFeature.SSO))
            .isInstanceOf(LicenceException.class);
    }

    @Test
    void testGetLicenceEmpty() {
        assertThat(licenceManager.getLicence()).isEmpty();
    }

    @Test
    void testUploadUnsupported() {
        assertThatThrownBy(() -> licenceManager.upload(new byte[0]))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: Move/rename the module directory**

```bash
git mv server/libs/licence/licence-impl server/libs/licence/licence-service
git rm server/libs/licence/licence-service/src/main/java/com/bytechef/platform/licence/NoOpLicenceChecker.java
```

- [ ] **Step 3: Update `settings.gradle.kts`** — change line `include("server:libs:licence:licence-impl")` to `include("server:libs:licence:licence-service")`.

- [ ] **Step 4: Write `build.gradle.kts`** for `licence-service`:

```kotlin
dependencies {
    implementation(project(":server:libs:licence:licence-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation("org.springframework:spring-context")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```

- [ ] **Step 5: Implement `NoOpLicenceManager`** (Apache header):

```java
package com.bytechef.platform.licence;

import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnCEVersion
public class NoOpLicenceManager implements LicenceManager {

    @Override
    public LicenceStatus getStatus() {
        return LicenceStatus.CE;
    }

    @Override
    public Optional<Licence> getLicence() {
        return Optional.empty();
    }

    @Override
    public boolean isFeatureEnabled(LicenceFeature licenceFeature) {
        return false;
    }

    @Override
    public void checkFeature(LicenceFeature licenceFeature) {
        throw new LicenceException("Feature %s requires an Enterprise licence".formatted(licenceFeature.getKey()));
    }

    @Override
    public long getAllowedJobs() {
        return -1;
    }

    @Override
    public Licence upload(byte[] licenceFileBytes) {
        throw new UnsupportedOperationException("Licence upload is not available in the Community Edition");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("Licence delete is not available in the Community Edition");
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :server:libs:licence:licence-service:test --tests '*NoOpLicenceManagerTest'`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/libs/licence settings.gradle.kts
git commit -m "Add CE NoOpLicenceManager; rename licence-impl to licence-service

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 2 — EE offline verification (pure, no Spring/DB)

### Task 3: `Ed25519Verifier`

**Files:**
- Module rename: `git mv server/ee/libs/licence/licence-impl server/ee/libs/licence/licence-service`; update `settings.gradle.kts:702`.
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/Ed25519Verifier.java`
- Test: `server/ee/libs/licence/licence-service/src/test/java/com/bytechef/ee/platform/licence/Ed25519VerifierTest.java`

**Interfaces:**
- Produces: `Ed25519Verifier` with `Ed25519Verifier(String publicKeyHex)` and `boolean verify(byte[] message, byte[] signature)`.

- [ ] **Step 1: Write the failing test** (EE header + `@version ee`). The test generates a throwaway Ed25519 keypair, signs a message, and asserts verify true for good sig / false for tampered.

```java
package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.EdECPublicKey;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class Ed25519VerifierTest {

    @Test
    void testVerifyAcceptsValidSignatureAndRejectsTampered() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
        KeyPair keyPair = generator.generateKeyPair();

        byte[] message = "license/payload".getBytes();

        Signature signer = Signature.getInstance("Ed25519");
        signer.initSign(keyPair.getPrivate());
        signer.update(message);

        byte[] signature = signer.sign();

        String publicKeyHex = encodeRawPublicKey(keyPair.getPublic());

        Ed25519Verifier verifier = new Ed25519Verifier(publicKeyHex);

        assertThat(verifier.verify(message, signature)).isTrue();

        byte[] tampered = "license/tampered".getBytes();

        assertThat(verifier.verify(tampered, signature)).isFalse();
    }

    // Keygen distributes the raw 32-byte Ed25519 public key as hex; encode the test key the same way.
    private static String encodeRawPublicKey(PublicKey publicKey) {
        EdECPublicKey edECPublicKey = (EdECPublicKey) publicKey;

        byte[] yReversed = edECPublicKey.getPoint()
            .getY()
            .toByteArray();

        byte[] raw = new byte[32];

        for (int i = 0; i < yReversed.length && i < 32; i++) {
            raw[i] = yReversed[yReversed.length - 1 - i];
        }

        if (edECPublicKey.getPoint()
            .isXOdd()) {
            raw[31] |= (byte) 0x80;
        }

        return HexFormat.of()
            .formatHex(raw);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*Ed25519VerifierTest'`
Expected: FAIL — `Ed25519Verifier` missing.

- [ ] **Step 3: Implement `Ed25519Verifier`** (EE header + `@version ee`). Builds an `EdECPublicKey` from the raw 32-byte hex Keygen key.

```java
package com.bytechef.ee.platform.licence;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.EdECPoint;
import java.security.spec.EdECPublicKeySpec;
import java.security.spec.NamedParameterSpec;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Verifies Ed25519 signatures using a raw 32-byte public key distributed by Keygen as a hex string.
 *
 * @version ee
 */
public class Ed25519Verifier {

    private final PublicKey publicKey;

    public Ed25519Verifier(String publicKeyHex) {
        this.publicKey = toPublicKey(publicKeyHex);
    }

    public boolean verify(byte[] message, byte[] signature) {
        try {
            Signature verifier = Signature.getInstance("Ed25519");

            verifier.initVerify(publicKey);
            verifier.update(message);

            return verifier.verify(signature);
        } catch (Exception exception) {
            return false;
        }
    }

    private static PublicKey toPublicKey(String publicKeyHex) {
        try {
            byte[] raw = HexFormat.of()
                .parseHex(publicKeyHex.trim());

            byte[] reversed = new byte[raw.length];

            for (int i = 0; i < raw.length; i++) {
                reversed[i] = raw[raw.length - 1 - i];
            }

            boolean xOdd = (reversed[0] & 0x80) != 0;
            reversed[0] &= (byte) 0x7F;

            BigInteger y = new BigInteger(1, reversed);

            NamedParameterSpec parameterSpec = NamedParameterSpec.ED25519;
            EdECPoint point = new EdECPoint(xOdd, y);
            EdECPublicKeySpec keySpec = new EdECPublicKeySpec(parameterSpec, point);

            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");

            return keyFactory.generatePublic(keySpec);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid Ed25519 public key", exception);
        }
    }

    static byte[] reverse(byte[] input) {
        byte[] copy = Arrays.copyOf(input, input.length);

        for (int i = 0; i < copy.length / 2; i++) {
            byte tmp = copy[i];
            copy[i] = copy[copy.length - 1 - i];
            copy[copy.length - 1 - i] = tmp;
        }

        return copy;
    }
}
```

- [ ] **Step 4: Update EE `build.gradle.kts`** (`server/ee/libs/licence/licence-service/build.gradle.kts`) — keep existing deps, add test deps:

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:licence:licence-api"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*Ed25519VerifierTest'`
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence settings.gradle.kts
git commit -m "Add Ed25519Verifier for offline licence verification

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 4: `LicenceFileParser` (envelope decode + dataset mapping)

**Files:**
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/LicenceFileParser.java`
- Test: `server/ee/libs/licence/licence-service/src/test/java/com/bytechef/ee/platform/licence/LicenceFileParserTest.java`

**Interfaces:**
- Consumes: `Ed25519Verifier` (Task 3), `Licence`, `LicenceFeature`, `LicenceException` (Task 1).
- Produces: `LicenceFileParser(Ed25519Verifier verifier)` with `Licence parse(byte[] licenceFileBytes)`. Throws `LicenceException` on armor/format/signature failure. Maps Keygen license-file dataset → `Licence`. Unknown feature keys are skipped (logged). `allowedJobs` accepts number or string; absent → `-1`.

A Keygen signed license file (alg `base64+ed25519`) is base64 of a JSON envelope `{ "enc": "<base64 dataset>", "sig": "<base64 sig>", "alg": "base64+ed25519" }`, wrapped in `-----BEGIN LICENSE FILE----- ... -----END LICENSE FILE-----`. The signed bytes are the ASCII string `license/<enc>`. The decoded dataset JSON shape (Keygen "license" object): `data.attributes.{ expiry, created, metadata }` and holder under `data.relationships`/`included`; for our purposes we read `data.attributes.metadata.{ allowedJobs, features, holderName, holderEmail, maxUsers }`, `data.attributes.expiry`, `data.attributes.created`, `data.id`.

- [ ] **Step 1: Write the failing test** (EE header + `@version ee`). A test helper builds a valid armored license file from a generated keypair so the parser can be exercised end-to-end.

```java
package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class LicenceFileParserTest {

    private final KeyPair keyPair = generateKeyPair();
    private final LicenceFileParser parser =
        new LicenceFileParser(new Ed25519Verifier(Ed25519VerifierTestSupport.publicKeyHex(keyPair)));

    @Test
    void testParseValidLicence() {
        String dataset = """
            {"data":{"id":"lic_123","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{"allowedJobs":1000,
            "features":["sso","audit-log","unknown-feature"],"holderName":"Acme",
            "holderEmail":"ops@acme.example","maxUsers":25}}}}""";

        byte[] file = sign(dataset);

        Licence licence = parser.parse(file);

        assertThat(licence.id()).isEqualTo("lic_123");
        assertThat(licence.allowedJobs()).isEqualTo(1000L);
        assertThat(licence.holderName()).isEqualTo("Acme");
        assertThat(licence.maxUsers()).isEqualTo(25);
        assertThat(licence.features()).containsExactlyInAnyOrder(LicenceFeature.SSO, LicenceFeature.AUDIT_LOG);
    }

    @Test
    void testParseAllowedJobsAbsentDefaultsUnlimited() {
        String dataset = """
            {"data":{"id":"lic_1","attributes":{"expiry":"2999-01-01T00:00:00.000Z",
            "created":"2026-01-01T00:00:00.000Z","metadata":{"features":[]}}}}""";

        Licence licence = parser.parse(sign(dataset));

        assertThat(licence.allowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testParseRejectsTamperedSignature() {
        String dataset = "{\\"data\\":{\\"id\\":\\"x\\",\\"attributes\\":{\\"expiry\\":\\"2999-01-01T00:00:00.000Z\\"," +
            "\\"created\\":\\"2026-01-01T00:00:00.000Z\\",\\"metadata\\":{}}}}";

        byte[] file = sign(dataset);
        // flip a byte inside the armored payload
        file[file.length / 2] ^= 0x01;

        assertThatThrownBy(() -> parser.parse(file)).isInstanceOf(LicenceException.class);
    }

    private byte[] sign(String dataset) {
        try {
            String enc = Base64.getEncoder()
                .encodeToString(dataset.getBytes(StandardCharsets.UTF_8));

            Signature signer = Signature.getInstance("Ed25519");
            signer.initSign(keyPair.getPrivate());
            signer.update(("license/" + enc).getBytes(StandardCharsets.UTF_8));

            String sig = Base64.getEncoder()
                .encodeToString(signer.sign());

            String envelope = "{\\"enc\\":\\"" + enc + "\\",\\"sig\\":\\"" + sig + "\\",\\"alg\\":\\"base64+ed25519\\"}";
            String armored = "-----BEGIN LICENSE FILE-----\\n" +
                Base64.getEncoder()
                    .encodeToString(envelope.getBytes(StandardCharsets.UTF_8)) +
                "\\n-----END LICENSE FILE-----\\n";

            return armored.getBytes(StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private static KeyPair generateKeyPair() {
        try {
            return KeyPairGenerator.getInstance("Ed25519")
                .generateKeyPair();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
```

Also create a tiny shared test helper `Ed25519VerifierTestSupport` (EE header + `@version ee`) exposing `static String publicKeyHex(KeyPair)` — move the `encodeRawPublicKey` logic from Task 3's test into it and have both tests use it (DRY).

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*LicenceFileParserTest'`
Expected: FAIL — `LicenceFileParser` missing.

- [ ] **Step 3: Implement `LicenceFileParser`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence;

import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Parses and verifies a Keygen signed license file (alg base64+ed25519) fully offline.
 *
 * @version ee
 */
public class LicenceFileParser {

    private static final Logger log = LoggerFactory.getLogger(LicenceFileParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Ed25519Verifier verifier;

    public LicenceFileParser(Ed25519Verifier verifier) {
        this.verifier = verifier;
    }

    public Licence parse(byte[] licenceFileBytes) {
        String armored = new String(licenceFileBytes, StandardCharsets.UTF_8);

        String envelopeBase64 = armored
            .replace("-----BEGIN LICENSE FILE-----", "")
            .replace("-----END LICENSE FILE-----", "")
            .replaceAll("\\s", "");

        if (envelopeBase64.isBlank()) {
            throw new LicenceException("Empty or malformed licence file");
        }

        try {
            byte[] envelopeBytes = Base64.getDecoder()
                .decode(envelopeBase64);

            JsonNode envelope = OBJECT_MAPPER.readTree(envelopeBytes);

            String enc = text(envelope, "enc");
            String sig = text(envelope, "sig");

            if (enc == null || sig == null) {
                throw new LicenceException("Licence file missing enc/sig");
            }

            byte[] signedBytes = ("license/" + enc).getBytes(StandardCharsets.UTF_8);
            byte[] signature = Base64.getDecoder()
                .decode(sig);

            if (!verifier.verify(signedBytes, signature)) {
                throw new LicenceException("Licence signature verification failed");
            }

            byte[] datasetBytes = Base64.getDecoder()
                .decode(enc);

            return toLicence(OBJECT_MAPPER.readTree(datasetBytes));
        } catch (LicenceException licenceException) {
            throw licenceException;
        } catch (Exception exception) {
            throw new LicenceException("Failed to parse licence file", exception);
        }
    }

    private static Licence toLicence(JsonNode root) {
        JsonNode attributes = root.path("data")
            .path("attributes");
        JsonNode metadata = attributes.path("metadata");

        String id = text(root.path("data"), "id");
        Instant expiresAt = parseInstant(text(attributes, "expiry"));
        Instant issuedAt = parseInstant(text(attributes, "created"));

        long allowedJobs = parseAllowedJobs(metadata.get("allowedJobs"));
        Set<LicenceFeature> features = parseFeatures(metadata.get("features"));

        Integer maxUsers = metadata.has("maxUsers") && metadata.get("maxUsers")
            .isNumber() ? metadata.get("maxUsers")
                .asInt() : null;

        return new Licence(
            id, text(metadata, "holderName"), text(metadata, "holderEmail"), issuedAt, expiresAt, features,
            allowedJobs, maxUsers);
    }

    private static Set<LicenceFeature> parseFeatures(JsonNode featuresNode) {
        Set<LicenceFeature> features = EnumSet.noneOf(LicenceFeature.class);

        if (featuresNode != null && featuresNode.isArray()) {
            for (JsonNode featureNode : featuresNode) {
                String key = featureNode.asString();

                LicenceFeature.ofKey(key)
                    .ifPresentOrElse(features::add, () -> log.warn("Unknown licence feature key '{}'; ignoring", key));
            }
        }

        return features;
    }

    private static long parseAllowedJobs(JsonNode node) {
        if (node == null || node.isNull()) {
            return -1;
        }

        if (node.isNumber()) {
            return node.asLong();
        }

        try {
            return Long.parseLong(node.asString()
                .trim());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Instant.parse(value);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);

        return value == null || value.isNull() ? null : value.asString();
    }
}
```

> Note: verify the Jackson `tools.jackson` API method names against a sibling EE class that uses `tools.jackson.databind` (e.g. the old `SelfHostedLicenceChecker` used `JsonNode.asText()`; in `tools.jackson` 3.x the text accessor is `asString()`). If `asString()` does not resolve, use `asText()` to match the installed Jackson version, and adjust the test's `featureNode.asString()` accordingly.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*LicenceFileParserTest'`
Expected: PASS.

- [ ] **Step 5: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add LicenceFileParser for signed Keygen licence files

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 3 — EE persistence + manager + bootstrap

### Task 5: `licence` table + JDBC entity + repository + autoconfig

**Files:**
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/domain/LicenceEntity.java`
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/repository/LicenceRepository.java`
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/config/LicenceConfiguration.java`
- Create: `server/ee/libs/licence/licence-service/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `server/ee/libs/licence/licence-service/src/main/resources/config/liquibase/changelog/platform/licence/20260624000001_licence_init.xml`
- Modify: `server/libs/config/liquibase-config/src/main/resources/config/liquibase/master.xml`
- Modify: `server/ee/libs/licence/licence-service/build.gradle.kts`

**Interfaces:**
- Produces: `LicenceEntity` (single-row, `@Table("licence")`, fields `Long id`, `String rawFile`, audit columns, `@Version Long version`); `LicenceRepository extends CrudRepository<LicenceEntity, Long>`; `LicenceConfiguration` (`@AutoConfiguration @ConditionalOnEEVersion @EnableJdbcRepositories`).

- [ ] **Step 1: Add the Liquibase changelog** `20260624000001_licence_init.xml` (mirror the component_policy changelog structure):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260624000001" author="Ivica Cardic">
        <createTable tableName="licence">
            <column name="id" type="BIGINT" autoIncrement="true">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="raw_file" type="TEXT">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Register the changelog in `master.xml`** — add near the other `platform/` includes:

```xml
    <!-- licence -->
    <includeAll path="classpath:config/liquibase/changelog/platform/licence" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or configuration or multitenant" />
```

- [ ] **Step 3: Write the entity** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.domain;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;

/**
 * @version ee
 */
@Table("licence")
public class LicenceEntity {

    @Id
    private Long id;

    private String rawFile;

    @CreatedBy
    private String createdBy;

    @CreatedDate
    private Instant createdDate;

    @LastModifiedBy
    private String lastModifiedBy;

    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRawFile() {
        return rawFile;
    }

    public void setRawFile(String rawFile) {
        this.rawFile = rawFile;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
```

- [ ] **Step 4: Write the repository** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.repository;

import com.bytechef.ee.platform.licence.domain.LicenceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * @version ee
 */
@Repository
public interface LicenceRepository extends CrudRepository<LicenceEntity, Long> {
}
```

- [ ] **Step 5: Write the autoconfiguration** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.config;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;

/**
 * @version ee
 */
@AutoConfiguration
@ConditionalOnEEVersion
@ConditionalOnBean(AbstractJdbcConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.bytechef.ee.platform.licence.repository")
public class LicenceConfiguration {
}
```

- [ ] **Step 6: Register autoconfig** — create the `...AutoConfiguration.imports` file with one line:

```
com.bytechef.ee.platform.licence.config.LicenceConfiguration
```

- [ ] **Step 7: Update EE `build.gradle.kts`** — add Spring Data JDBC + autoconfigure deps:

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:licence:licence-api"))
    implementation(project(":server:libs:platform:platform-api"))
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```

- [ ] **Step 8: Compile to verify wiring**

Run: `./gradlew :server:ee:libs:licence:licence-service:compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence server/libs/config/liquibase-config
git commit -m "Add licence table, entity, repository, and JDBC autoconfig

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 6: `LicenceProperties` + `OfflineLicenceManager` (core, no check-in)

**Files:**
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/LicenceProperties.java`
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/OfflineLicenceManager.java`
- Modify: `LicenceConfiguration.java` (register beans + `@EnableConfigurationProperties`).
- Test: `server/ee/libs/licence/licence-service/src/test/java/com/bytechef/ee/platform/licence/OfflineLicenceManagerTest.java`

**Interfaces:**
- Consumes: `LicenceFileParser`, `LicenceRepository`, `LicenceEntity`, `LicenceManager`, `Licence`, `LicenceStatus`, `LicenceFeature`, `LicenceException`.
- Produces: `OfflineLicenceManager implements LicenceManager` with constructor `(LicenceFileParser parser, LicenceRepository repository, Clock clock, int gracePeriodDays)`; caches decoded `Licence` in a `volatile` field; computes status with grace logic; `upload` verifies → persists single row (delete-all then save) → swaps cache → returns `Licence`; `delete` clears row + cache.
- `LicenceProperties` (`@ConfigurationProperties("bytechef.licence")`): `String publicKey`, `String path`, `int gracePeriodDays = 14`, `String accountId`, nested `CheckIn { boolean enabled = false; Duration interval = Duration.ofHours(24); }`.

- [ ] **Step 1: Write the failing test** (EE header + `@version ee`). Uses an in-memory fake `LicenceRepository`, a fixed `Clock`, and the Task 4 `sign(...)` helper (extract it into a shared `LicenceTestFiles` helper class in test sources so both tests use it — DRY).

```java
package com.bytechef.ee.platform.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class OfflineLicenceManagerTest {

    private final LicenceTestFiles licenceTestFiles = new LicenceTestFiles();
    private final LicenceFileParser parser =
        new LicenceFileParser(new Ed25519Verifier(licenceTestFiles.publicKeyHex()));

    @Test
    void testUploadValidLicenceBecomesValid() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));

        byte[] file = licenceTestFiles.signLicence("lic_1", "2999-01-01T00:00:00.000Z", 100, "sso");

        Licence licence = manager.upload(file);

        assertThat(licence.allowedJobs()).isEqualTo(100L);
        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isTrue();
        assertThat(manager.isFeatureEnabled(LicenceFeature.AUDIT_LOG)).isFalse();
        assertThat(manager.getAllowedJobs()).isEqualTo(100L);
    }

    @Test
    void testExpiredWithinGraceIsGrace() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        // expired 5 days ago, grace 14 days -> GRACE
        manager.upload(licenceTestFiles.signLicence("lic_2", "2026-06-19T00:00:00.000Z", 100, "sso"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.GRACE);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isTrue();
    }

    @Test
    void testExpiredPastGraceIsExpired() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        // expired 30 days ago, past 14-day grace -> EXPIRED
        manager.upload(licenceTestFiles.signLicence("lic_3", "2026-05-25T00:00:00.000Z", 100, "sso"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.EXPIRED);
        assertThat(manager.isFeatureEnabled(LicenceFeature.SSO)).isFalse();
    }

    @Test
    void testMissingWhenNoLicence() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.MISSING);
        assertThat(manager.getAllowedJobs()).isEqualTo(-1L);
    }

    @Test
    void testCheckFeatureThrowsWhenNotEntitled() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_4", "2999-01-01T00:00:00.000Z", 100, "sso"));

        assertThatThrownBy(() -> manager.checkFeature(LicenceFeature.AUDIT_LOG))
            .isInstanceOf(LicenceException.class);
    }

    @Test
    void testDeleteResetsToMissing() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_5", "2999-01-01T00:00:00.000Z", 100, "sso"));

        manager.delete();

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.MISSING);
    }

    private OfflineLicenceManager newManager(Instant now) {
        return new OfflineLicenceManager(
            parser, new InMemoryLicenceRepository(), Clock.fixed(now, ZoneOffset.UTC), 14);
    }
}
```

Create test helpers in test sources (EE header + `@version ee`): `LicenceTestFiles` (holds a generated keypair; `publicKeyHex()`, `signLicence(id, expiryIso, allowedJobs, features...)` building an armored file) and `InMemoryLicenceRepository implements LicenceRepository` (HashMap-backed; supports `save`, `findAll`, `deleteAll`, `count`). Refactor Task 4's test to reuse `LicenceTestFiles`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*OfflineLicenceManagerTest'`
Expected: FAIL — `OfflineLicenceManager` / `LicenceProperties` missing.

- [ ] **Step 3: Implement `LicenceProperties`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @version ee
 */
@ConfigurationProperties("bytechef.licence")
public class LicenceProperties {

    private String publicKey;
    private String path;
    private int gracePeriodDays = 14;
    private String accountId;
    private CheckIn checkIn = new CheckIn();

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public CheckIn getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(CheckIn checkIn) {
        this.checkIn = checkIn;
    }

    public static class CheckIn {

        private boolean enabled = false;
        private Duration interval = Duration.ofHours(24);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }
    }
}
```

- [ ] **Step 4: Implement `OfflineLicenceManager`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence;

import com.bytechef.ee.platform.licence.domain.LicenceEntity;
import com.bytechef.ee.platform.licence.repository.LicenceRepository;
import com.bytechef.platform.licence.Licence;
import com.bytechef.platform.licence.LicenceException;
import com.bytechef.platform.licence.LicenceFeature;
import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.licence.LicenceStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 */
public class OfflineLicenceManager implements LicenceManager {

    private static final Logger log = LoggerFactory.getLogger(OfflineLicenceManager.class);

    private final LicenceFileParser parser;
    private final LicenceRepository licenceRepository;
    private final Clock clock;
    private final int gracePeriodDays;

    private volatile Licence licence;
    private volatile boolean invalid;

    public OfflineLicenceManager(
        LicenceFileParser parser, LicenceRepository licenceRepository, Clock clock, int gracePeriodDays) {

        this.parser = parser;
        this.licenceRepository = licenceRepository;
        this.clock = clock;
        this.gracePeriodDays = gracePeriodDays;

        loadFromRepository();
    }

    @Override
    public LicenceStatus getStatus() {
        if (invalid) {
            return LicenceStatus.INVALID;
        }

        Licence current = licence;

        if (current == null) {
            return LicenceStatus.MISSING;
        }

        Instant expiresAt = current.expiresAt();

        if (expiresAt == null) {
            return LicenceStatus.VALID;
        }

        Instant now = Instant.now(clock);

        if (!now.isAfter(expiresAt)) {
            return LicenceStatus.VALID;
        }

        Instant graceEnd = expiresAt.plus(gracePeriodDays, ChronoUnit.DAYS);

        if (!now.isAfter(graceEnd)) {
            return LicenceStatus.GRACE;
        }

        return LicenceStatus.EXPIRED;
    }

    @Override
    public Optional<Licence> getLicence() {
        return Optional.ofNullable(licence);
    }

    @Override
    public boolean isFeatureEnabled(LicenceFeature licenceFeature) {
        Licence current = licence;

        return getStatus().isActive() && current != null && current.features()
            .contains(licenceFeature);
    }

    @Override
    public void checkFeature(LicenceFeature licenceFeature) {
        if (!isFeatureEnabled(licenceFeature)) {
            throw new LicenceException(
                "Feature %s is not enabled by the current licence (status=%s)".formatted(
                    licenceFeature.getKey(), getStatus()));
        }
    }

    @Override
    public long getAllowedJobs() {
        Licence current = licence;

        if (current == null || !getStatus().isActive()) {
            return -1;
        }

        return current.allowedJobs();
    }

    @Override
    public Licence upload(byte[] licenceFileBytes) {
        Licence parsed = parser.parse(licenceFileBytes);

        String rawFile = new String(licenceFileBytes, java.nio.charset.StandardCharsets.UTF_8);

        licenceRepository.deleteAll();

        LicenceEntity entity = new LicenceEntity();
        entity.setRawFile(rawFile);

        licenceRepository.save(entity);

        this.licence = parsed;
        this.invalid = false;

        return parsed;
    }

    @Override
    public void delete() {
        licenceRepository.deleteAll();

        this.licence = null;
        this.invalid = false;
    }

    void markInvalid() {
        this.invalid = true;
    }

    private void loadFromRepository() {
        for (LicenceEntity entity : licenceRepository.findAll()) {
            try {
                this.licence = parser.parse(entity.getRawFile()
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (LicenceException licenceException) {
                log.warn("Persisted licence failed verification: {}", licenceException.getMessage());

                this.invalid = true;
            }

            return;
        }
    }
}
```

- [ ] **Step 5: Wire beans in `LicenceConfiguration`** — add (keep `@AutoConfiguration @ConditionalOnEEVersion @ConditionalOnBean(AbstractJdbcConfiguration.class) @EnableJdbcRepositories`), plus `@EnableConfigurationProperties(LicenceProperties.class)`:

```java
    private static final String DEFAULT_PUBLIC_KEY =
        "REPLACE_WITH_PRODUCTION_KEYGEN_ED25519_PUBLIC_KEY_HEX";

    @Bean
    Ed25519Verifier ed25519Verifier(LicenceProperties licenceProperties) {
        String publicKey = licenceProperties.getPublicKey();

        if (publicKey == null || publicKey.isBlank()) {
            publicKey = DEFAULT_PUBLIC_KEY;
        }

        return new Ed25519Verifier(publicKey);
    }

    @Bean
    LicenceFileParser licenceFileParser(Ed25519Verifier ed25519Verifier) {
        return new LicenceFileParser(ed25519Verifier);
    }

    @Bean
    OfflineLicenceManager licenceManager(
        LicenceFileParser licenceFileParser, LicenceRepository licenceRepository,
        LicenceProperties licenceProperties) {

        return new OfflineLicenceManager(
            licenceFileParser, licenceRepository, Clock.systemUTC(), licenceProperties.getGracePeriodDays());
    }
```

Add imports (`org.springframework.context.annotation.Bean`, `java.time.Clock`, the licence + properties classes). Leave a code comment by `DEFAULT_PUBLIC_KEY`: "Replace with the production Keygen account Ed25519 verify key (raw 32-byte hex)."

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*OfflineLicenceManagerTest'`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add OfflineLicenceManager with grace handling and persistence

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 7: env/file bootstrap

**Files:**
- Modify: `OfflineLicenceManager.java` — add a `bootstrap(String path, String inlineContents)` method that seeds the DB if empty.
- Modify: `LicenceConfiguration.java` — call bootstrap via an `ApplicationRunner` or `@PostConstruct`-style bean.
- Test: extend `OfflineLicenceManagerTest` with a bootstrap test.

**Interfaces:**
- Produces: `OfflineLicenceManager.bootstrap(String path, String inlineContents)` — if no licence cached AND repository empty, read bytes from `inlineContents` (preferred) or `path`, then `upload(...)` them. No-op if a licence already exists.

- [ ] **Step 1: Write the failing test** — add to `OfflineLicenceManagerTest`:

```java
    @Test
    void testBootstrapFromInlineContentsSeedsRepository() {
        InMemoryLicenceRepository repository = new InMemoryLicenceRepository();
        OfflineLicenceManager manager = new OfflineLicenceManager(
            parser, repository, Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC), 14);

        String contents = new String(
            licenceTestFiles.signLicence("lic_boot", "2999-01-01T00:00:00.000Z", 50, "sso"),
            java.nio.charset.StandardCharsets.UTF_8);

        manager.bootstrap(null, contents);

        assertThat(manager.getStatus()).isEqualTo(LicenceStatus.VALID);
        assertThat(repository.count()).isEqualTo(1L);
    }

    @Test
    void testBootstrapNoOpWhenLicencePresent() {
        OfflineLicenceManager manager = newManager(Instant.parse("2026-06-24T00:00:00Z"));
        manager.upload(licenceTestFiles.signLicence("lic_existing", "2999-01-01T00:00:00.000Z", 10, "sso"));

        manager.bootstrap(null, new String(
            licenceTestFiles.signLicence("lic_other", "2999-01-01T00:00:00.000Z", 999, "audit-log"),
            java.nio.charset.StandardCharsets.UTF_8));

        assertThat(manager.getAllowedJobs()).isEqualTo(10L);
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*OfflineLicenceManagerTest'`
Expected: FAIL — `bootstrap` missing.

- [ ] **Step 3: Implement `bootstrap`** in `OfflineLicenceManager`:

```java
    public void bootstrap(String path, String inlineContents) {
        if (licence != null || licenceRepository.count() > 0) {
            return;
        }

        byte[] bytes = null;

        if (inlineContents != null && !inlineContents.isBlank()) {
            bytes = inlineContents.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else if (path != null && !path.isBlank()) {
            try {
                bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
            } catch (java.io.IOException ioException) {
                log.warn("Failed to read bootstrap licence file at {}: {}", path, ioException.getMessage());

                return;
            }
        }

        if (bytes != null) {
            try {
                upload(bytes);
            } catch (LicenceException licenceException) {
                log.warn("Bootstrap licence failed verification: {}", licenceException.getMessage());
            }
        }
    }
```

- [ ] **Step 4: Invoke bootstrap at startup in `LicenceConfiguration`** — add a runner bean:

```java
    @Bean
    org.springframework.boot.ApplicationRunner licenceBootstrapRunner(
        OfflineLicenceManager licenceManager, LicenceProperties licenceProperties) {

        return args -> {
            String inline = System.getenv("BYTECHEF_LICENSE");

            licenceManager.bootstrap(licenceProperties.getPath(), inline);
        };
    }
```

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*OfflineLicenceManagerTest'`
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add env/file licence bootstrap on startup

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 8: persistence IntTest

**Files:**
- Test: `server/ee/libs/licence/licence-service/src/test/java/com/bytechef/ee/platform/licence/OfflineLicenceManagerPersistenceIntTest.java`
- Possibly create `src/test/resources/config/application-testint.yml` for the module.

**Interfaces:**
- Consumes: real `LicenceRepository` (Testcontainers PostgreSQL), `OfflineLicenceManager`.

- [ ] **Step 1: Write the IntTest** (EE header + `@version ee`) using `@SpringBootTest` with the module's JDBC config + Testcontainers. Mirror an existing EE `*IntTest` in a sibling service module for the exact `@SpringBootTest(classes=...)` + `@ComponentScan(basePackages="com.bytechef.ee.platform.licence")` + Testcontainers setup. Assert: upload persists a row; a freshly constructed manager over the same repository reports `VALID` (reload path).

```java
    @Test
    void testUploadPersistsAndReloads() {
        byte[] file = licenceTestFiles.signLicence("lic_int", "2999-01-01T00:00:00.000Z", 100, "sso");

        offlineLicenceManager.upload(file);

        assertThat(licenceRepository.count()).isEqualTo(1L);

        OfflineLicenceManager reloaded = new OfflineLicenceManager(
            licenceFileParser, licenceRepository, java.time.Clock.systemUTC(), 14);

        assertThat(reloaded.getStatus()).isEqualTo(LicenceStatus.VALID);
    }
```

> The test must inject a parser whose verifier trusts `licenceTestFiles.publicKeyHex()` — provide it via `@TestConfiguration` overriding the `ed25519Verifier` bean with the test public key, so signed test files verify.

- [ ] **Step 2: Run the IntTest**

Run: `./gradlew :server:ee:libs:licence:licence-service:testIntegration --tests '*OfflineLicenceManagerPersistenceIntTest'`
Expected: PASS (Docker required).

- [ ] **Step 3: Commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add licence persistence integration test

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 4 — optional online check-in

### Task 9: `LicenceCheckInTask`

**Files:**
- Create: `server/ee/libs/licence/licence-service/src/main/java/com/bytechef/ee/platform/licence/LicenceCheckInTask.java`
- Modify: `LicenceConfiguration.java` (register the task conditionally).
- Test: `server/ee/libs/licence/licence-service/src/test/java/com/bytechef/ee/platform/licence/LicenceCheckInTaskTest.java`

**Interfaces:**
- Consumes: `OfflineLicenceManager` (uses `markInvalid()` + `getLicence()`), `LicenceProperties`, a `java.net.http.HttpClient`.
- Produces: `LicenceCheckInTask(OfflineLicenceManager manager, LicenceProperties properties, HttpClient httpClient)` with `void checkIn()`. Calls Keygen `validate` for the cached licence id/key; on a response indicating `SUSPENDED`/`REVOKED`/`EXPIRED` (or HTTP 4xx forbidden) → `manager.markInvalid()`. Network failure → no status change (logged).

- [ ] **Step 1: Write the failing test** (EE header + `@version ee`) with a mocked `HttpClient` (Mockito) returning a revoked validation body → assert `manager.markInvalid()` reached (status INVALID); and a network exception → status unchanged.

```java
    @Test
    void testRevokedResponseMarksInvalid() throws Exception {
        // given a manager holding a valid licence and an HttpClient returning a revoked validation
        // when checkIn() runs
        // then manager.getStatus() == INVALID
    }

    @Test
    void testNetworkFailureLeavesStatusUnchanged() throws Exception {
        // HttpClient.send throws IOException -> status stays VALID
    }
```

Fill these with concrete Mockito stubs: mock `HttpClient.send(...)` to return an `HttpResponse` whose `body()` is `{"meta":{"valid":false,"code":"SUSPENDED"}}` (status 200) for the first, and to throw `java.io.IOException` for the second. Build the manager via `LicenceTestFiles` + `InMemoryLicenceRepository` with a far-future expiry so its baseline status is `VALID`.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*LicenceCheckInTaskTest'`
Expected: FAIL — `LicenceCheckInTask` missing.

- [ ] **Step 3: Implement `LicenceCheckInTask`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence;

import com.bytechef.platform.licence.Licence;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Optional online check-in that flips the cached licence to INVALID when Keygen reports it
 * revoked/suspended/expired. Disabled by default; network failures are non-fatal.
 *
 * @version ee
 */
public class LicenceCheckInTask {

    private static final Logger log = LoggerFactory.getLogger(LicenceCheckInTask.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final OfflineLicenceManager licenceManager;
    private final LicenceProperties licenceProperties;
    private final HttpClient httpClient;

    public LicenceCheckInTask(
        OfflineLicenceManager licenceManager, LicenceProperties licenceProperties, HttpClient httpClient) {

        this.licenceManager = licenceManager;
        this.licenceProperties = licenceProperties;
        this.httpClient = httpClient;
    }

    public void checkIn() {
        Optional<Licence> current = licenceManager.getLicence();

        if (current.isEmpty()) {
            return;
        }

        String accountId = licenceProperties.getAccountId();
        String licenceId = current.get()
            .id();

        if (accountId == null || accountId.isBlank() || licenceId == null) {
            return;
        }

        try {
            String url = "https://api.keygen.sh/v1/accounts/%s/licenses/%s".formatted(accountId, licenceId);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 403 || response.statusCode() == 404) {
                licenceManager.markInvalid();

                return;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response.body());
            JsonNode status = root.path("data")
                .path("attributes")
                .path("status");

            if (isRevokedStatus(status.asString(null))) {
                licenceManager.markInvalid();
            }
        } catch (Exception exception) {
            log.warn("Licence check-in failed (keeping offline status): {}", exception.getMessage());
        }
    }

    private static boolean isRevokedStatus(String status) {
        return "SUSPENDED".equals(status) || "REVOKED".equals(status) || "EXPIRED".equals(status) ||
            "BANNED".equals(status);
    }
}
```

> Adjust `status.asString(null)` to the installed Jackson API (`asText(null)` if needed), matching whatever Task 4 resolved.

- [ ] **Step 4: Register conditionally in `LicenceConfiguration`**:

```java
    @Bean
    @ConditionalOnProperty(prefix = "bytechef.licence.check-in", name = "enabled", havingValue = "true")
    LicenceCheckInTask licenceCheckInTask(OfflineLicenceManager licenceManager, LicenceProperties licenceProperties) {
        return new LicenceCheckInTask(licenceManager, licenceProperties, java.net.http.HttpClient.newHttpClient());
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.licence.check-in", name = "enabled", havingValue = "true")
    org.springframework.scheduling.annotation.SchedulingConfigurer licenceCheckInScheduler(
        LicenceCheckInTask licenceCheckInTask, LicenceProperties licenceProperties) {

        return taskRegistrar -> taskRegistrar.addFixedRateTask(
            licenceCheckInTask::checkIn, licenceProperties.getCheckIn()
                .getInterval());
    }
```

Add imports (`org.springframework.boot.autoconfigure.condition.ConditionalOnProperty`).

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-service:test --tests '*LicenceCheckInTaskTest'`
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add optional online licence check-in for revocation

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 5 — job-count metering (platform)

### Task 10: `licence_job_usage` table + entity + repository (atomic guarded increment)

**Files:**
- Create: `.../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/domain/LicenceJobUsage.java`
- Create: `.../platform-workflow-execution-service/src/main/java/com/bytechef/platform/workflow/execution/repository/LicenceJobUsageRepository.java`
- Create: `.../platform-workflow-execution-service/src/main/resources/config/liquibase/changelog/platform/licence_job_usage/20260624000002_licence_job_usage_init.xml`
- Modify: `master.xml`, `platform-workflow-execution-service/build.gradle.kts`.

Resolve the exact module path first: `find server/libs/platform -type d -name 'platform-workflow-execution-service'`.

**Interfaces:**
- Produces: `LicenceJobUsage` (`@Table("licence_job_usage")`, `@Id String yearMonth`, `long count`, audit cols, `@Version Long version` — note `@Id` non-generated String PK requires `Persistable` handling; see step note); `LicenceJobUsageRepository` with `@Modifying @Query` methods `insertIgnore(String yearMonth)` and `int incrementIfBelow(String yearMonth, long allowed)`.

- [ ] **Step 1: Add changelog** `20260624000002_licence_job_usage_init.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                   http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">
    <changeSet id="20260624000002" author="Ivica Cardic">
        <createTable tableName="licence_job_usage">
            <column name="year_month" type="VARCHAR(7)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="count" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
            <column name="created_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="created_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_date" type="TIMESTAMP">
                <constraints nullable="false"/>
            </column>
            <column name="last_modified_by" type="VARCHAR(50)">
                <constraints nullable="false"/>
            </column>
            <column name="version" type="BIGINT" defaultValueNumeric="0">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

- [ ] **Step 2: Register in `master.xml`** with the execution context filter (mirror `atlas/execution/`):

```xml
    <!-- licence job usage -->
    <includeAll path="classpath:config/liquibase/changelog/platform/licence_job_usage" relativeToChangelogFile="false" errorIfMissingOrEmpty="false" contextFilter="mono or execution or coordinator or multitenant" />
```

- [ ] **Step 3: Write the repository** (Apache header) using raw SQL `@Modifying @Query` for atomic upsert + guarded increment, avoiding entity-PK insert complexity:

```java
package com.bytechef.platform.workflow.execution.repository;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LicenceJobUsageRepository extends CrudRepository<Object, String> {

    @Modifying
    @Query("""
        INSERT INTO licence_job_usage (year_month, count, created_date, created_by, last_modified_date,
            last_modified_by, version)
        VALUES (:yearMonth, 0, now(), 'system', now(), 'system', 0)
        ON CONFLICT (year_month) DO NOTHING
        """)
    void insertIgnore(@Param("yearMonth") String yearMonth);

    @Modifying
    @Query("""
        UPDATE licence_job_usage
        SET count = count + 1, last_modified_date = now(), version = version + 1
        WHERE year_month = :yearMonth AND count < :allowed
        """)
    int incrementIfBelow(@Param("yearMonth") String yearMonth, @Param("allowed") long allowed);

    @Query("SELECT count FROM licence_job_usage WHERE year_month = :yearMonth")
    Long findCount(@Param("yearMonth") String yearMonth);
}
```

> `CrudRepository<Object, String>` is a placeholder generic — since we only use `@Query` methods, define a minimal marker entity instead: create `LicenceJobUsage` as the domain type and type the repository `CrudRepository<LicenceJobUsage, String>`. Implement `LicenceJobUsage` as a class with the columns above, implementing `org.springframework.data.domain.Persistable<String>` (`getId()` returns `yearMonth`, `isNew()` returns a transient flag) so Spring Data JDBC handles the String PK. The `@Query` methods do not rely on entity mapping.

- [ ] **Step 4: Write `LicenceJobUsage`** (Apache header) implementing `Persistable<String>` with fields `yearMonth`, `count`, audit columns, `@Version Long version`, and a transient `boolean isNew`.

- [ ] **Step 5: Add deps to `platform-workflow-execution-service/build.gradle.kts`** — add `implementation(project(":server:libs:licence:licence-api"))`. (Spring Data JDBC already present in this module.)

- [ ] **Step 6: Compile**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:compileJava`
(Confirm the exact Gradle path from the directory found in step 0.)
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/libs/platform server/libs/config/liquibase-config
git commit -m "Add licence_job_usage table, entity, and repository

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 11: `LicenceJobUsageService` + `JobLimitExceededException`

**Files:**
- Create: `.../platform-workflow-execution-service/.../exception/JobLimitExceededException.java`
- Create: `.../platform-workflow-execution-service/.../service/LicenceJobUsageService.java`
- Test: `.../platform-workflow-execution-service/src/test/java/.../service/LicenceJobUsageServiceTest.java`

**Interfaces:**
- Consumes: `LicenceManager` (`getAllowedJobs()`), `LicenceJobUsageRepository`, `Clock`.
- Produces: `JobLimitExceededException extends RuntimeException`; `LicenceJobUsageService(LicenceManager licenceManager, LicenceJobUsageRepository repository, Clock clock)` with `void consumeOrThrow()` (returns immediately if `getAllowedJobs() < 0`; else `insertIgnore(ym)` then `incrementIfBelow(ym, allowed)`; if 0 rows updated → throw `JobLimitExceededException`) and `long currentMonthUsage()` (reads `findCount`, null → 0).

- [ ] **Step 1: Write the failing test** (Apache header) with a mocked repository + mocked `LicenceManager` + fixed `Clock`:

```java
    @Test
    void testConsumeUnlimitedSkipsRepository() {
        when(licenceManager.getAllowedJobs()).thenReturn(-1L);

        licenceJobUsageService.consumeOrThrow();

        verifyNoInteractions(licenceJobUsageRepository);
    }

    @Test
    void testConsumeUnderLimitIncrements() {
        when(licenceManager.getAllowedJobs()).thenReturn(100L);
        when(licenceJobUsageRepository.incrementIfBelow("2026-06", 100L)).thenReturn(1);

        licenceJobUsageService.consumeOrThrow();

        verify(licenceJobUsageRepository).insertIgnore("2026-06");
        verify(licenceJobUsageRepository).incrementIfBelow("2026-06", 100L);
    }

    @Test
    void testConsumeAtLimitThrows() {
        when(licenceManager.getAllowedJobs()).thenReturn(100L);
        when(licenceJobUsageRepository.incrementIfBelow("2026-06", 100L)).thenReturn(0);

        assertThatThrownBy(() -> licenceJobUsageService.consumeOrThrow())
            .isInstanceOf(JobLimitExceededException.class);
    }
```

Set up `licenceJobUsageService = new LicenceJobUsageService(licenceManager, licenceJobUsageRepository, Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC))`.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test --tests '*LicenceJobUsageServiceTest'`
Expected: FAIL.

- [ ] **Step 3: Implement the exception + service** (Apache header):

`JobLimitExceededException`:
```java
package com.bytechef.platform.workflow.execution.exception;

public class JobLimitExceededException extends RuntimeException {

    public JobLimitExceededException(long allowed) {
        super("Monthly job execution limit reached (allowed=%d). Upgrade your licence to run more jobs."
            .formatted(allowed));
    }
}
```

`LicenceJobUsageService`:
```java
package com.bytechef.platform.workflow.execution.service;

import com.bytechef.platform.licence.LicenceManager;
import com.bytechef.platform.workflow.execution.exception.JobLimitExceededException;
import com.bytechef.platform.workflow.execution.repository.LicenceJobUsageRepository;
import java.time.Clock;
import java.time.YearMonth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicenceJobUsageService {

    private final LicenceManager licenceManager;
    private final LicenceJobUsageRepository licenceJobUsageRepository;
    private final Clock clock;

    public LicenceJobUsageService(
        LicenceManager licenceManager, LicenceJobUsageRepository licenceJobUsageRepository, Clock clock) {

        this.licenceManager = licenceManager;
        this.licenceJobUsageRepository = licenceJobUsageRepository;
        this.clock = clock;
    }

    @Transactional
    public void consumeOrThrow() {
        long allowed = licenceManager.getAllowedJobs();

        if (allowed < 0) {
            return;
        }

        String yearMonth = YearMonth.now(clock)
            .toString();

        licenceJobUsageRepository.insertIgnore(yearMonth);

        int updated = licenceJobUsageRepository.incrementIfBelow(yearMonth, allowed);

        if (updated == 0) {
            throw new JobLimitExceededException(allowed);
        }
    }

    @Transactional(readOnly = true)
    public long currentMonthUsage() {
        Long count = licenceJobUsageRepository.findCount(YearMonth.now(clock)
            .toString());

        return count == null ? 0 : count;
    }
}
```

- [ ] **Step 4: Provide the `Clock` bean** — in the module's existing `@Configuration`, add `@Bean @ConditionalOnMissingBean Clock clock() { return Clock.systemUTC(); }` if no `Clock` bean exists (check first; many apps already define one). If a `Clock` bean already exists app-wide, skip and let it inject.

- [ ] **Step 5: Run to verify it passes**

Run: `./gradlew :server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service:test --tests '*LicenceJobUsageServiceTest'`
Expected: PASS.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/libs/platform
git commit -m "Add LicenceJobUsageService monthly job metering

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 12: wire metering into `PrincipalJobFacadeImpl`

**Files:**
- Modify: `PrincipalJobFacadeImpl.java` (inject `LicenceJobUsageService`; call `consumeOrThrow()` at the start of `createJob(...)` and `createJobWithoutDispatch(...)`).
- Modify: the bean's `@Configuration` if it's constructor-wired there (it's `@Service`, so constructor injection just works once the dependency is a bean).
- Test: add/extend a unit test for `PrincipalJobFacadeImpl` verifying `consumeOrThrow()` is invoked before job creation and that its exception propagates (job not created).

**Interfaces:**
- Consumes: `LicenceJobUsageService.consumeOrThrow()`.

- [ ] **Step 1: Write the failing test** — mock `LicenceJobUsageService`, `JobFacade`, `PrincipalJobService`, `JobService`, `WorkflowService`:

```java
    @Test
    void testCreateJobConsumesQuotaBeforeCreating() {
        principalJobFacade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(licenceJobUsageService, jobFacade);
        inOrder.verify(licenceJobUsageService).consumeOrThrow();
        inOrder.verify(jobFacade).createJob(jobParametersDTO);
    }

    @Test
    void testCreateJobBlockedWhenLimitReached() {
        org.mockito.Mockito.doThrow(new JobLimitExceededException(100))
            .when(licenceJobUsageService).consumeOrThrow();

        assertThatThrownBy(() -> principalJobFacade.createJob(jobParametersDTO, 1L, PlatformType.AUTOMATION))
            .isInstanceOf(JobLimitExceededException.class);

        verifyNoInteractions(jobFacade);
    }
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :...:platform-workflow-execution-service:test --tests '*PrincipalJobFacade*'`
Expected: FAIL — constructor arity / missing call.

- [ ] **Step 3: Modify `PrincipalJobFacadeImpl`** — add the field + constructor param, and call at the top of the two metered methods:

```java
    private final LicenceJobUsageService licenceJobUsageService;
```
Constructor gains `LicenceJobUsageService licenceJobUsageService` (assign it). In `createJob(JobParametersDTO, long, PlatformType)` and `createJobWithoutDispatch(...)`, insert as the first statement:
```java
        licenceJobUsageService.consumeOrThrow();
```
Leave `createChildJob` and `createPrincipalLinkedJob` unmetered (sub-flows don't count).

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :...:platform-workflow-execution-service:test --tests '*PrincipalJobFacade*'`
Expected: PASS.

- [ ] **Step 5: Build the whole server to catch wiring regressions**

Run: `./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/libs/platform
git commit -m "Meter top-level job creation against the licence job limit

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 6 — central EE guard

### Task 13: REST `LicenceEnforcementHandlerInterceptor`

**Files:**
- Create module `server/ee/libs/licence/licence-web/` + `build.gradle.kts`; add to `settings.gradle.kts`.
- Create: `.../licence-web/.../LicenceEnforcementHandlerInterceptor.java`
- Create: `.../licence-web/.../config/LicenceWebConfiguration.java`
- Test: `.../licence-web/src/test/java/.../LicenceEnforcementHandlerInterceptorTest.java`

**Interfaces:**
- Consumes: `LicenceManager.getStatus()`, `ConditionalOnEEVersion`.
- Produces: `LicenceEnforcementHandlerInterceptor implements HandlerInterceptor` — in `preHandle`, if `handler instanceof HandlerMethod hm` and `hm.getBeanType().isAnnotationPresent(ConditionalOnEEVersion.class)` (or method-level) and `!licenceManager.getStatus().isActive()` → write HTTP 402 JSON `{"message":"...","errorKey":"LICENCE_REQUIRED","status":"<status>"}` and return `false`; else `true`. `LicenceWebConfiguration implements WebMvcConfigurer @ConditionalOnEEVersion` registering the interceptor (exclude the licence admin endpoints path so admins can still upload — exclude `/api/platform/**/licence/**` and the GraphQL endpoint is handled separately in Task 14).

- [ ] **Step 1: Write the failing test** (EE header + `@version ee`) — construct a `HandlerMethod` for a dummy `@ConditionalOnEEVersion`-annotated controller bean, a `MockHttpServletRequest`/`MockHttpServletResponse`, a stubbed `LicenceManager`:

```java
    @Test
    void testEeHandlerBlockedWhenLicenceInactive() throws Exception {
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.MISSING);

        boolean proceed = interceptor.preHandle(request, response, eeHandlerMethod);

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(402);
    }

    @Test
    void testEeHandlerAllowedWhenValid() throws Exception {
        when(licenceManager.getStatus()).thenReturn(LicenceStatus.VALID);

        assertThat(interceptor.preHandle(request, response, eeHandlerMethod)).isTrue();
    }

    @Test
    void testNonEeHandlerAlwaysAllowed() throws Exception {
        assertThat(interceptor.preHandle(request, response, ceHandlerMethod)).isTrue();
        verifyNoInteractions(licenceManager);
    }
```

Provide `@ConditionalOnEEVersion`-annotated `EeController` and a plain `CeController` as static nested test classes; build `HandlerMethod`s via `new HandlerMethod(bean, method)`.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-web:test`
Expected: FAIL — classes missing.

- [ ] **Step 3: Implement the interceptor** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.LicenceManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @version ee
 */
public class LicenceEnforcementHandlerInterceptor implements HandlerInterceptor {

    private final LicenceManager licenceManager;

    public LicenceEnforcementHandlerInterceptor(LicenceManager licenceManager) {
        this.licenceManager = licenceManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        boolean eeEndpoint = handlerMethod.getBeanType()
            .isAnnotationPresent(ConditionalOnEEVersion.class) ||
            handlerMethod.hasMethodAnnotation(ConditionalOnEEVersion.class);

        if (!eeEndpoint) {
            return true;
        }

        if (licenceManager.getStatus()
            .isActive()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        response.setContentType("application/json");

        String body = """
            {"message":"A valid Enterprise licence is required","errorKey":"LICENCE_REQUIRED","status":"%s"}"""
            .formatted(licenceManager.getStatus());

        response.getWriter()
            .write(body);

        return false;
    }
}
```

- [ ] **Step 4: Implement `LicenceWebConfiguration`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.web.config;

import com.bytechef.ee.platform.licence.web.LicenceEnforcementHandlerInterceptor;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.licence.LicenceManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @version ee
 */
@Configuration
@ConditionalOnEEVersion
public class LicenceWebConfiguration implements WebMvcConfigurer {

    private final LicenceManager licenceManager;

    public LicenceWebConfiguration(LicenceManager licenceManager) {
        this.licenceManager = licenceManager;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LicenceEnforcementHandlerInterceptor(licenceManager))
            .excludePathPatterns("/api/platform/**/licence/**");
    }
}
```

- [ ] **Step 5: `build.gradle.kts`** for `licence-web`:

```kotlin
dependencies {
    implementation(project(":server:libs:licence:licence-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webmvc")
    implementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
```
Add `include("server:ee:libs:licence:licence-web")` to `settings.gradle.kts`.

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-web:test`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence settings.gradle.kts
git commit -m "Add central REST EE licence enforcement interceptor

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 14: GraphQL `EeGraphQlFieldRegistry` + `LicenceEnforcementInstrumentation`

**Files:**
- Create: `.../licence-web/.../EeGraphQlFieldRegistry.java`
- Create: `.../licence-web/.../LicenceEnforcementInstrumentation.java`
- Modify: `LicenceWebConfiguration.java` (register both beans).
- Modify: `licence-web/build.gradle.kts` (add spring-graphql + graphql-java + spring-context deps).
- Test: `.../licence-web/src/test/java/.../EeGraphQlFieldRegistryTest.java`

**Interfaces:**
- Produces: `EeGraphQlFieldRegistry` — at construction, scans `ApplicationContext` for beans annotated with both `@Controller` and `@ConditionalOnEEVersion`, collecting GraphQL field names from `@QueryMapping`/`@MutationMapping` (field = annotation `value()` or, if blank, the method name) and `@SchemaMapping(typeName="Query"|"Mutation")`. Exposes `boolean isEeField(String fieldName)`. `LicenceEnforcementInstrumentation extends SimpleInstrumentation` — in `beginExecution`, walk the parsed operation's top-level selection-set field names; if any `registry.isEeField(name)` and `!licenceManager.getStatus().isActive()` → throw a `GraphQLError`-bearing exception (use `graphql.GraphqlErrorException` or abort via `instrumentationState`).

- [ ] **Step 1: Write the failing test** for the registry (EE header + `@version ee`) — build a tiny `AnnotationConfigApplicationContext` registering a nested `@Controller @ConditionalOnEEVersion` class with a `@QueryMapping LicenceType licence()` method, then assert `registry.isEeField("licence")` is true and `isEeField("someCeField")` is false.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-web:test --tests '*EeGraphQlFieldRegistryTest'`
Expected: FAIL.

- [ ] **Step 3: Implement `EeGraphQlFieldRegistry`** (EE header + `@version ee`):

```java
package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 */
public class EeGraphQlFieldRegistry {

    private final Set<String> eeFieldNames = new HashSet<>();

    public EeGraphQlFieldRegistry(ApplicationContext applicationContext) {
        Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);

        for (Object bean : controllers.values()) {
            Class<?> beanClass = AopUtils.getTargetClass(bean);

            if (AnnotationUtils.findAnnotation(beanClass, ConditionalOnEEVersion.class) == null) {
                continue;
            }

            for (Method method : beanClass.getMethods()) {
                collectFieldName(method);
            }
        }
    }

    public boolean isEeField(String fieldName) {
        return eeFieldNames.contains(fieldName);
    }

    private void collectFieldName(Method method) {
        QueryMapping queryMapping = AnnotationUtils.findAnnotation(method, QueryMapping.class);

        if (queryMapping != null) {
            eeFieldNames.add(fieldName(queryMapping.value(), method));
        }

        MutationMapping mutationMapping = AnnotationUtils.findAnnotation(method, MutationMapping.class);

        if (mutationMapping != null) {
            eeFieldNames.add(fieldName(mutationMapping.value(), method));
        }

        SchemaMapping schemaMapping = AnnotationUtils.findAnnotation(method, SchemaMapping.class);

        if (schemaMapping != null) {
            String typeName = schemaMapping.typeName();

            if ("Query".equals(typeName) || "Mutation".equals(typeName)) {
                eeFieldNames.add(fieldName(schemaMapping.field(), method));
            }
        }
    }

    private static String fieldName(String declared, Method method) {
        return declared == null || declared.isBlank() ? method.getName() : declared;
    }
}
```

- [ ] **Step 4: Implement `LicenceEnforcementInstrumentation`** (EE header + `@version ee`) — guards EE GraphQL fields. Use graphql-java `Instrumentation.beginExecution` to inspect the operation's top-level fields:

```java
package com.bytechef.ee.platform.licence.web;

import com.bytechef.platform.licence.LicenceManager;
import graphql.ExecutionResult;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.SimpleInstrumentationContext;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.Field;
import graphql.language.OperationDefinition;
import graphql.language.Selection;
import com.bytechef.platform.licence.LicenceException;

/**
 * @version ee
 */
public class LicenceEnforcementInstrumentation extends SimplePerformantInstrumentation {

    private final EeGraphQlFieldRegistry registry;
    private final LicenceManager licenceManager;

    public LicenceEnforcementInstrumentation(EeGraphQlFieldRegistry registry, LicenceManager licenceManager) {
        this.registry = registry;
        this.licenceManager = licenceManager;
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(
        InstrumentationExecuteOperationParameters parameters) {

        OperationDefinition operationDefinition = parameters.getExecutionContext()
            .getOperationDefinition();

        for (Selection<?> selection : operationDefinition.getSelectionSet()
            .getSelections()) {

            if (selection instanceof Field field && registry.isEeField(field.getName()) &&
                !licenceManager.getStatus()
                    .isActive()) {

                throw new LicenceException(
                    "A valid Enterprise licence is required (status=%s)".formatted(licenceManager.getStatus()));
            }
        }

        return SimpleInstrumentationContext.noOp();
    }
}
```

> Verify `SimplePerformantInstrumentation` / `beginExecuteOperation` signatures against the installed graphql-java version (Spring GraphQL bundles it). If `beginExecuteOperation` isn't available, use `instrumentExecutionInput` or `beginExecution` and read the document from `parameters.getQuery()` parsed via `Parser`. The thrown `LicenceException` is surfaced by the existing `GlobalDataFetcherExceptionResolver` only if it extends `AbstractException`; otherwise add a small `DataFetcherExceptionResolverAdapter` in this module mapping `LicenceException` → a `GraphQLError` with `errorType=FORBIDDEN` and `extensions.errorKey="LICENCE_REQUIRED"`.

- [ ] **Step 5: Register beans in `LicenceWebConfiguration`**:

```java
    @Bean
    EeGraphQlFieldRegistry eeGraphQlFieldRegistry(org.springframework.context.ApplicationContext applicationContext) {
        return new EeGraphQlFieldRegistry(applicationContext);
    }

    @Bean
    LicenceEnforcementInstrumentation licenceEnforcementInstrumentation(
        EeGraphQlFieldRegistry eeGraphQlFieldRegistry, LicenceManager licenceManager) {

        return new LicenceEnforcementInstrumentation(eeGraphQlFieldRegistry, licenceManager);
    }
```

- [ ] **Step 6: Add deps** to `licence-web/build.gradle.kts`: `implementation("org.springframework.graphql:spring-graphql")`, `implementation("com.graphql-java:graphql-java")`, `implementation("org.springframework:spring-context")`.

- [ ] **Step 7: Run to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-web:test`
Expected: PASS.

- [ ] **Step 8: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence
git commit -m "Add central GraphQL EE licence enforcement instrumentation

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 7 — admin GraphQL API

### Task 15: `licence-graphql` module + `LicenceGraphQlController`

**Files:**
- Create module `server/ee/libs/licence/licence-graphql/` + `build.gradle.kts`; add to `settings.gradle.kts`.
- Create: `.../licence-graphql/.../LicenceGraphQlController.java`
- Create: `.../licence-graphql/src/main/resources/graphql/licence.graphqls`
- Create: `.../licence-graphql/src/test/resources/graphql/base.graphqls` (test-only base Query/Mutation, mirroring the audit module).
- Test: slice test `.../licence-graphql/src/test/java/.../LicenceGraphQlControllerTest.java`.

**Interfaces:**
- Consumes: `LicenceManager` (`getLicence`, `getStatus`, `upload`, `delete`), `LicenceJobUsageService.currentMonthUsage()`.
- Produces: GraphQL `licence: LicenceType` query (returns null when none), `uploadLicence(contents: String!): LicenceType` mutation (base64 or raw `.lic` contents), `deleteLicence: Boolean!` mutation. Admin-only via `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on each mapping (match the repo's GraphQL auth convention — confirm against `UserGraphQlController`).

- [ ] **Step 1: Write `licence.graphqls`**. The schema uses the `Long` scalar — confirm a global `scalar Long` is registered app-wide (the audit module relies on it too; grep `scalar Long` under `server/ee`). If it is globally declared, do NOT redeclare it here (duplicate scalar = schema error); if not, add `scalar Long` in this file. For the slice test, declare `scalar Long` plus base `type Query`/`type Mutation` in the test-only `base.graphqls` (mirror the audit module's test base).

```graphql
extend type Query {
    licence: LicenceType
}

extend type Mutation {
    uploadLicence(contents: String!): LicenceType!
    deleteLicence: Boolean!
}

type LicenceType {
    id: String
    holderName: String
    holderEmail: String
    issuedAt: String
    expiresAt: String
    status: String!
    features: [String!]!
    allowedJobs: Long!
    maxUsers: Int
    currentMonthJobUsage: Long!
}
```

- [ ] **Step 2: Write the failing slice test** (EE header + `@version ee`) using `@GraphQlTest(LicenceGraphQlController.class)` + a mocked `LicenceManager` + mocked `LicenceJobUsageService`, asserting the `licence` query returns the holder + status and `uploadLicence` calls `manager.upload`.

- [ ] **Step 3: Run to verify it fails**

Run: `./gradlew :server:ee:libs:licence:licence-graphql:test`
Expected: FAIL.

- [ ] **Step 4: Implement `LicenceGraphQlController`** (EE header + `@version ee`) with `@Controller @ConditionalOnEEVersion`, `@QueryMapping licence()`, `@MutationMapping uploadLicence(@Argument String contents)`, `@MutationMapping deleteLicence()`. Map `Licence` + status + usage → a `LicenceType` record (define it as a nested record or DTO). `uploadLicence` converts `contents` to bytes (UTF-8) and calls `manager.upload`.

- [ ] **Step 5: `build.gradle.kts`** — deps: `licence-api`, `platform-api`, `platform-workflow-execution-service` (for `LicenceJobUsageService`) or better its api module, `spring-graphql`, `spring-security-core` (for `@PreAuthorize`). Add `include(...)` to `settings.gradle.kts`.

- [ ] **Step 6: Run to verify it passes**

Run: `./gradlew :server:ee:libs:licence:licence-graphql:test`
Expected: PASS.

- [ ] **Step 7: Format + commit**

```bash
./gradlew spotlessApply
git add -A server/ee/libs/licence settings.gradle.kts
git commit -m "Add LicenceGraphQlController admin API

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 16: register new modules in the server app

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts` (and any EE assembly module) to depend on `licence-web` and `licence-graphql` so the beans load. Confirm how existing EE graphql/web modules are pulled into the server app (grep for `licence-service` / `platform-component-policy-graphql` in build files).
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml` — document the new `bytechef.licence.*` keys (commented defaults).

- [ ] **Step 1: Find how EE modules are aggregated**

Run: `grep -rn "platform-component-policy-graphql\|licence-service" server/apps server/ee --include=build.gradle.kts`

- [ ] **Step 2: Add the new module dependencies** following that same pattern (impl/runtimeOnly as appropriate).

- [ ] **Step 3: Add commented config** to `application-bytechef.yml` under a new `licence:` block:

```yaml
  # Licence (EE). Mandatory when edition=ee; no enable/disable switch.
  licence:
    # public-key:            # override the baked-in Keygen Ed25519 verify key (raw 32-byte hex)
    # path:                  # bootstrap .lic file path (seeds DB on first boot if empty)
    grace-period-days: 14
    # account-id:            # Keygen account id (only needed for online check-in)
    check-in:
      enabled: false
      interval: PT24H
```

- [ ] **Step 4: Build + boot smoke test**

Run: `./gradlew compileJava` then `./gradlew :server:apps:server-app:test` (or the lightest app-context test available).
Expected: BUILD SUCCESSFUL; application context loads with edition=ee.

- [ ] **Step 5: Commit**

```bash
./gradlew spotlessApply
git add -A server/apps server/ee
git commit -m "Wire licence-web and licence-graphql into the server app

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 8 — client Settings UI

### Task 17: GraphQL operations + codegen

**Files:**
- Create: `client/src/graphql/platform/license/licence.graphql`, `uploadLicence.graphql`, `deleteLicence.graphql`.
- Modify: `client/codegen.ts` — add the EE `licence.graphqls` schema path to the schema array.

- [ ] **Step 1: Add the schema path** to `client/codegen.ts` `schema` array:

```ts
'../server/ee/libs/licence/licence-graphql/src/main/resources/graphql/*.graphqls',
```

- [ ] **Step 2: Write the operation files** (keys sorted alphabetically per ESLint sort-keys):

`licence.graphql`:
```graphql
query licence {
    licence {
        allowedJobs
        currentMonthJobUsage
        expiresAt
        features
        holderEmail
        holderName
        id
        issuedAt
        maxUsers
        status
    }
}
```

`uploadLicence.graphql`:
```graphql
mutation uploadLicence($contents: String!) {
    uploadLicence(contents: $contents) {
        allowedJobs
        currentMonthJobUsage
        expiresAt
        features
        holderEmail
        holderName
        id
        issuedAt
        maxUsers
        status
    }
}
```

`deleteLicence.graphql`:
```graphql
mutation deleteLicence {
    deleteLicence
}
```

- [ ] **Step 3: Regenerate**

Run: `cd client && npx graphql-codegen`
Expected: `src/shared/middleware/graphql.ts` updated with `useLicenceQuery`, `useUploadLicenceMutation`, `useDeleteLicenceMutation`.

- [ ] **Step 4: Commit operations + generated file**

```bash
cd client && npm run format
git add client/src/graphql/platform/license client/codegen.ts client/src/shared/middleware/graphql.ts client/src/shared/middleware/graphql-types.ts
git commit -m "client - Add licence GraphQL operations and codegen

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

### Task 18: License Settings page + route

**Files:**
- Create: `client/src/ee/pages/settings/platform/license/License.tsx`
- Create: `client/src/ee/pages/settings/platform/license/components/LicenseDetails.tsx`
- Create: `client/src/ee/pages/settings/platform/license/components/LicenseUpload.tsx`
- Create: `client/src/ee/pages/settings/platform/license/components/hooks/useUploadLicense.ts`
- Modify: `client/src/routes.tsx` (add child route + navItem).
- Test: `client/src/ee/pages/settings/platform/license/License.test.tsx`

**Interfaces:**
- Consumes: `useLicenceQuery`, `useUploadLicenceMutation`, `useDeleteLicenceMutation` (Task 17), `EEVersion`, `PrivateRoute`, `AUTHORITIES.ADMIN`.

- [ ] **Step 1: Write the failing test** (`License.test.tsx`) — render `License` with a mocked `useLicenceQuery` returning (a) no licence → upload UI shown; (b) a valid licence → holder + status badge + features chips + `allowedJobs`/usage shown. Mock the generated hooks via `vi.mock` using `vi.hoisted` for any module-scope refs (per CLAUDE.md Vitest hoisting note).

- [ ] **Step 2: Run to verify it fails**

Run: `cd client && npm run test -- License.test`
Expected: FAIL — component missing.

- [ ] **Step 3: Implement the components.** `License.tsx`: `useLicenceQuery`; if `data?.licence` render `<LicenseDetails>` else `<LicenseUpload>`; wrap in `LayoutContainer` + `Header title="License"` (mirror `AiProviders.tsx`). `LicenseUpload.tsx`: drag-drop `.lic` input (mirror `UploadCustomComponentDialog`, `accept=".lic"`), read file text via `file.text()`, call `useUploadLicenceMutation`. `LicenseDetails.tsx`: render holder, a status `<Badge>` (Valid/In grace/Expired/Invalid), issued/expiry, feature chips, `allowedJobs` + `currentMonthJobUsage`, `maxUsers`, plus Replace (re-upload) and Remove (`useDeleteLicenceMutation` behind a confirm `AlertDialog`). Follow CLAUDE.md client conventions (twMerge not cn, Lucide `*Icon` imports, hook ordering, interface names end `I`/`Props`, sort-keys).

- [ ] **Step 4: Add the route + navItem** in `client/src/routes.tsx` `platformSettingsRoutes`:

children entry:
```tsx
{
    element: (
        <PrivateRoute hasAnyAuthorities={[AUTHORITIES.ADMIN]}>
            <EEVersion>
                <LazyLoadWrapper>
                    <License />
                </LazyLoadWrapper>
            </EEVersion>
        </PrivateRoute>
    ),
    path: 'license',
},
```
navItems entry:
```tsx
{
    href: 'license',
    title: 'License',
},
```
Add the lazy import for `License` next to the other platform settings lazy imports.

- [ ] **Step 5: Run to verify it passes**

Run: `cd client && npm run test -- License.test`
Expected: PASS.

- [ ] **Step 6: Full client check**

Run: `cd client && npm run check`
Expected: lint + typecheck + tests PASS.

- [ ] **Step 7: Commit**

```bash
cd client && npm run format
git add client/src/ee/pages/settings/platform/license client/src/routes.tsx
git commit -m "client - Add License settings page (upload + view)

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## PHASE 9 — final verification

### Task 19: full build + manual smoke

- [ ] **Step 1: Server checks**

Run: `./gradlew spotlessApply && ./gradlew check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Integration tests**

Run: `./gradlew testIntegration` (Docker required).
Expected: PASS (incl. licence persistence IntTest).

- [ ] **Step 3: Client checks**

Run: `cd client && npm run check`
Expected: PASS.

- [ ] **Step 4: Manual smoke (documented, optional but recommended)**
  - Boot with `bytechef.edition=ee`, no licence → confirm an EE settings page (e.g. Audit Events) returns 402 / "licence required"; CE-equivalent features still work; job creation still works (allowedJobs unlimited when MISSING returns -1, so jobs are NOT blocked when no licence — note: MISSING means EE features blocked by the guard, but `getAllowedJobs()` returns -1 → jobs unmetered; this is intended, job metering only kicks in with a valid finite-limit licence).
  - Generate a test `.lic` with your Keygen account (and set the production public key), upload via Settings → confirm details render and EE pages unlock.

- [ ] **Step 5: Final commit (docs/notes if any)**

```bash
git add -A
git commit -m "Finalize offline Keygen licensing

Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>"
```

---

## Notes / follow-ups (out of scope for this plan)

- **Per-sub-feature gating rollout:** applying `licenceManager.checkFeature(LicenceFeature.X)` at each of the 11 EE feature facades (for tiered packages below full EE) is a separate follow-up plan. This plan delivers the mechanism (`checkFeature`) and the central EE main guard; the per-feature entitlement enforcement at facade level is the next increment.
- **Seat (`maxUsers`) enforcement:** carried + displayed only; enforcement deferred.
- **Production Keygen public key:** `DEFAULT_PUBLIC_KEY` in `LicenceConfiguration` MUST be replaced with the real account verify key before release.
- **Jackson API drift:** `tools.jackson` 3.x vs `com.fasterxml.jackson` — confirm `asString()` vs `asText()` against the installed version (both appear in deps).
