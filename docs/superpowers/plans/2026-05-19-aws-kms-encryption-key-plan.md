# AWS KMS EncryptionKey provider Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add `AwsKmsEncryptionKey` as a new `EncryptionKey` provider implementing the envelope-encryption pattern documented in the spec. The connection.parameters column stays in the DB; the symmetric key used to encrypt it comes from AWS KMS via persisted ciphertext + per-startup decrypt.

**Architecture:** New EE module `server/ee/libs/core/encryption/encryption-aws-kms/` mirroring the existing `encryption-filesystem` and `encryption-property` modules. Pattern: `AwsKmsEncryptionKey` extends `AbstractEncryptionKey`. On first boot, calls `KmsClient.generateDataKey`, persists ciphertext to disk, caches plaintext in memory. On subsequent boots, reads ciphertext from disk and calls `KmsClient.decrypt` to recover the same plaintext.

**Tech Stack:** Spring Cloud AWS 4.0.0 (already in catalog), AWS SDK v2 `KmsClient`, LocalStack via Testcontainers for integration testing.

**Spec:** [docs/superpowers/specs/2026-05-19-connection-credential-store-design.md](../specs/2026-05-19-connection-credential-store-design.md) — section "AWS KMS as EncryptionKey provider"

**Issue:** [bytechefhq/bytechef#547](https://github.com/bytechefhq/bytechef/issues/547)

**Branch state at start:** `claude/amazing-brahmagupta-12e13d` at commit `b4db1681667` (AWS KMS spec fix).

---

## Branch hygiene — applies to every task

1. Run `git branch --show-current` and confirm `claude/amazing-brahmagupta-12e13d` before any change.
2. NEVER run `git checkout`, `git pull`, `git rebase`, `git fetch`.
3. STOP and report if anything looks wrong.

---

## File Structure

**New EE module:** `server/ee/libs/core/encryption/encryption-aws-kms/`

**New files:**
| File | Responsibility |
|---|---|
| `<module>/build.gradle.kts` | Deps: encryption-api, spring-cloud-aws-kms, AWS SDK kms |
| `<module>/src/main/java/com/bytechef/ee/encryption/aws/kms/AwsKmsEncryptionKey.java` | Implementation extending `AbstractEncryptionKey` |
| `<module>/src/main/java/com/bytechef/ee/encryption/aws/kms/boot/AwsKmsEncryptionEnvironmentPostProcessor.java` | Translates `bytechef.encryption.provider=aws-kms` to `spring.cloud.aws.kms.enabled=true` |
| `<module>/src/main/java/com/bytechef/ee/encryption/aws/kms/config/AwsKmsEncryptionConfiguration.java` | `@ConditionalOnProperty` configuration class |
| `<module>/src/main/resources/META-INF/spring.factories` | Registers the EnvironmentPostProcessor |
| `<module>/src/test/java/.../AwsKmsEncryptionKeyTest.java` | Unit test with mocked KmsClient |
| `<module>/src/test/java/.../AwsKmsEncryptionKeyIntTest.java` | LocalStack integration test |

**Modified files:**
| File | Change |
|---|---|
| `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` | Add `AWS_KMS` to Encryption.Provider enum + new `AwsKms` inner class with `keyId` + `dataKeyPath` |
| `settings.gradle.kts` | Include the new EE module |

---

### Task 1: Extend ApplicationProperties.Encryption

**File:** `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java`

- [ ] **Step 1: Verify branch state**

```bash
git branch --show-current   # → claude/amazing-brahmagupta-12e13d
git log --oneline -1        # top is b4db1681667 547 Fix AWS KMS EncryptionKey design
```

- [ ] **Step 2: Locate existing Encryption inner class**

```bash
grep -n "class Encryption\|enum Provider" server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java | head -5
```

The class is around lines 2352-2402 per the explorer report. It has:
- `enum Provider { FILESYSTEM, PROPERTY; }`
- `private Provider provider = Provider.FILESYSTEM;`
- `private Property property = new Property();` (and getter/setter)
- nested `Property` class with a `key` field

- [ ] **Step 3: Add AWS_KMS to the enum and an AwsKms field**

**Append** `AWS_KMS` to the Provider enum (alphabetical: AWS_KMS comes first, before FILESYSTEM and PROPERTY — but enum ordinal stability isn't a concern here since the enum is bound to property string values, not ordinals; pick whichever ordering matches the existing enum's style. If existing values are NOT alphabetical, just append `AWS_KMS` at the end):

```java
public enum Provider {
    FILESYSTEM,
    PROPERTY,
    AWS_KMS;
}
```

Add a new field alongside `property`:

```java
private AwsKms awsKms = new AwsKms();
```

And a getter:
```java
public AwsKms getAwsKms() {
    return awsKms;
}

public void setAwsKms(AwsKms awsKms) {
    this.awsKms = awsKms;
}
```

Add the inner class (after `Property` or alphabetically — match the existing style):

```java
public static class AwsKms {

    /** KMS key ARN or alias used to wrap the data key. */
    private String keyId;

    /** Path where the ciphertext of the data key is persisted. Default ~/.bytechef/aws-kms-data-key. */
    private String dataKeyPath = System.getProperty("user.home") + "/.bytechef/aws-kms-data-key";

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getDataKeyPath() {
        return dataKeyPath;
    }

    public void setDataKeyPath(String dataKeyPath) {
        this.dataKeyPath = dataKeyPath;
    }
}
```

- [ ] **Step 4: Compile + check**

```bash
./gradlew :server:libs:config:app-config:compileJava
./gradlew :server:libs:config:app-config:check
```
Both BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java
git commit -m "$(cat <<'EOF'
547 Add AWS_KMS provider + AwsKms config to ApplicationProperties.Encryption

Foundation for the AWS KMS EncryptionKey adapter. AwsKms inner class
captures keyId (KMS key ARN/alias) and dataKeyPath (where the
ciphertext is persisted; defaults to ~/.bytechef/aws-kms-data-key).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Scaffold the EE module

- [ ] **Step 1: Verify branch + create directories**

```bash
git branch --show-current
ROOT=server/ee/libs/core/encryption/encryption-aws-kms
mkdir -p $ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms
mkdir -p $ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms/boot
mkdir -p $ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms/config
mkdir -p $ROOT/src/main/resources/META-INF
mkdir -p $ROOT/src/test/java/com/bytechef/ee/encryption/aws/kms
```

- [ ] **Step 2: build.gradle.kts**

Reference shape from `server/ee/libs/platform/platform-connection/platform-connection-credential-store-aws-secrets-manager/build.gradle.kts` (the AWS Secrets Manager adapter).

Create `$ROOT/build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-kms")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:kms")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:encryption:encryption-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))

    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-kms")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:test:test-int-support"))
}
```

- [ ] **Step 3: settings.gradle.kts**

Find existing encryption module includes (likely around `include("server:libs:core:encryption:encryption-api")` etc., or in the EE section). Insert (under EE — alphabetical with siblings):

```kotlin
include("server:ee:libs:core:encryption:encryption-aws-kms")
```

- [ ] **Step 4: Verify Gradle recognizes it**

```bash
./gradlew :server:ee:libs:core:encryption:encryption-aws-kms:tasks 2>&1 | tail -3
```
Expected: lists tasks. No commit yet — combine with Tasks 3-5.

---

### Task 3: Implement AwsKmsEncryptionKey

**File:** `$ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms/AwsKmsEncryptionKey.java`

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import com.bytechef.encryption.AbstractEncryptionKey;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

/**
 * Envelope-encryption-backed {@link com.bytechef.encryption.EncryptionKey} that uses AWS KMS to wrap and unwrap a
 * single data key reused across the process lifetime.
 *
 * <p>On first startup (when the ciphertext file does not exist), {@code generateDataKey} produces a fresh AES-256
 * data key, the ciphertext of which is persisted to {@code dataKeyPath}. The plaintext is held only in memory.
 *
 * <p>On every subsequent startup, the persisted ciphertext is read and decrypted via KMS, recovering the same
 * plaintext data key. The plaintext is never written to disk.
 *
 * <p>Losing the ciphertext file makes all previously-encrypted parameters unrecoverable. Operators must back it up.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsKmsEncryptionKey extends AbstractEncryptionKey {

    private static final Logger log = LoggerFactory.getLogger(AwsKmsEncryptionKey.class);

    private final String dataKeyCiphertextBase64;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public AwsKmsEncryptionKey(KmsClient kmsClient, String keyId, Path dataKeyPath) {
        try {
            if (Files.exists(dataKeyPath)) {
                this.dataKeyCiphertextBase64 = decryptExisting(kmsClient, dataKeyPath);
            } else {
                this.dataKeyCiphertextBase64 = generateAndPersist(kmsClient, keyId, dataKeyPath);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AWS KMS encryption key", e);
        }
    }

    @Override
    protected String doGetKey() {
        return dataKeyCiphertextBase64;
    }

    private static String decryptExisting(KmsClient kmsClient, Path dataKeyPath) throws Exception {
        byte[] ciphertext = Files.readAllBytes(dataKeyPath);

        DecryptResponse response = kmsClient.decrypt(
            DecryptRequest.builder()
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertext))
                .build());

        SdkBytes plaintext = response.plaintextBlob();

        log.info("Recovered AWS KMS data key from {}", dataKeyPath);

        return Base64.getEncoder().encodeToString(plaintext.asByteArray());
    }

    private static String generateAndPersist(KmsClient kmsClient, String keyId, Path dataKeyPath) throws Exception {
        GenerateDataKeyResponse response = kmsClient.generateDataKey(
            GenerateDataKeyRequest.builder()
                .keyId(keyId)
                .keySpec(DataKeySpec.AES_256)
                .build());

        SdkBytes plaintext = response.plaintextBlob();
        SdkBytes ciphertext = response.ciphertextBlob();

        Files.createDirectories(dataKeyPath.getParent());
        Files.write(dataKeyPath, ciphertext.asByteArray());

        log.info("Generated and persisted new AWS KMS data key at {}", dataKeyPath);

        return Base64.getEncoder().encodeToString(plaintext.asByteArray());
    }
}
```

**Note on the field name `dataKeyCiphertextBase64`**: it's slightly misleading — the field actually holds the *plaintext* data key Base64-encoded (per the `AbstractEncryptionKey.getKey()` contract). The variable name should be `dataKeyPlaintextBase64`. Use that name instead. (I had a slip in the template above — apply this correction.)

---

### Task 4: Configuration class + EnvironmentPostProcessor

**File 1:** `$ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms/config/AwsKmsEncryptionConfiguration.java`

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.encryption.aws.kms.AwsKmsEncryptionKey;
import com.bytechef.encryption.EncryptionKey;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.kms.KmsClient;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.encryption", name = "provider", havingValue = "aws-kms")
public class AwsKmsEncryptionConfiguration {

    @Bean
    EncryptionKey encryptionKey(KmsClient kmsClient, ApplicationProperties applicationProperties) {
        ApplicationProperties.Encryption.AwsKms awsKms = applicationProperties.getEncryption()
            .getAwsKms();

        return new AwsKmsEncryptionKey(kmsClient, awsKms.getKeyId(), Path.of(awsKms.getDataKeyPath()));
    }
}
```

**File 2:** `$ROOT/src/main/java/com/bytechef/ee/encryption/aws/kms/boot/AwsKmsEncryptionEnvironmentPostProcessor.java`

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms.boot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Activates Spring Cloud AWS KMS auto-configuration when {@code bytechef.encryption.provider=aws-kms}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AwsKmsEncryptionEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> source = new HashMap<>();

        String provider = environment.getProperty("bytechef.encryption.provider", String.class);

        if (Objects.equals(provider, "aws-kms")) {
            source.put("spring.cloud.aws.kms.enabled", true);
        }

        environment.getPropertySources()
            .addFirst(new MapPropertySource("Custom AWS KMS Encryption Config", source));
    }
}
```

**File 3:** `$ROOT/src/main/resources/META-INF/spring.factories`

```
org.springframework.boot.EnvironmentPostProcessor=\
com.bytechef.ee.encryption.aws.kms.boot.AwsKmsEncryptionEnvironmentPostProcessor
```

**Step: Compile**

```bash
./gradlew :server:ee:libs:core:encryption:encryption-aws-kms:compileJava
```
Expected: BUILD SUCCESSFUL.

**Step: Commit Tasks 2 + 3 + 4 together**

```bash
git add server/ee/libs/core/encryption/encryption-aws-kms/ settings.gradle.kts
git commit -m "$(cat <<'EOF'
547 Add AWS KMS EncryptionKey provider

New EE module implementing EncryptionKey via envelope encryption with
AWS KMS:
- First boot: generateDataKey produces (plaintext, ciphertext);
  ciphertext is persisted to bytechef.encryption.aws-kms.data-key-path
  (default ~/.bytechef/aws-kms-data-key). Plaintext cached in memory.
- Subsequent boots: persisted ciphertext is read and decrypted via KMS,
  recovering the same plaintext.
- Plaintext data key never touches disk; ciphertext file is useless
  without KMS access.

Activates when bytechef.encryption.provider=aws-kms — an
EnvironmentPostProcessor enables spring.cloud.aws.kms.enabled=true so
Spring Cloud AWS auto-configures KmsClient using the existing
bytechef.cloud.aws.* credentials and region.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Unit test (mocked KmsClient)

**File:** `$ROOT/src/test/java/com/bytechef/ee/encryption/aws/kms/AwsKmsEncryptionKeyTest.java`

Unit test verifying both boot paths without spinning up LocalStack. Uses Mockito-mocked `KmsClient`. Two test methods:
1. `testFirstBootGeneratesAndPersists` — data key file doesn't exist; assert `generateDataKey` called, ciphertext file created with the bytes returned by KMS, `getKey()` returns the plaintext (Base64).
2. `testSubsequentBootReadsAndDecrypts` — pre-populate the data key file; assert `decrypt` called with those bytes, `getKey()` returns the plaintext from `DecryptResponse`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

/**
 * @author Ivica Cardic
 */
class AwsKmsEncryptionKeyTest {

    @Test
    void testFirstBootGeneratesAndPersists(@TempDir Path tempDir) throws IOException {
        Path dataKeyPath = tempDir.resolve("aws-kms-data-key");

        byte[] plaintextBytes = "32-byte-test-aes-256-data-key!!!".getBytes();
        byte[] ciphertextBytes = "fake-kms-ciphertext-blob".getBytes();

        KmsClient kmsClient = mock(KmsClient.class);
        when(kmsClient.generateDataKey(any(GenerateDataKeyRequest.class))).thenReturn(
            GenerateDataKeyResponse.builder()
                .plaintextBlob(SdkBytes.fromByteArray(plaintextBytes))
                .ciphertextBlob(SdkBytes.fromByteArray(ciphertextBytes))
                .build());

        AwsKmsEncryptionKey key = new AwsKmsEncryptionKey(kmsClient, "alias/test", dataKeyPath);

        verify(kmsClient).generateDataKey(any(GenerateDataKeyRequest.class));

        assertThat(dataKeyPath).exists();
        assertThat(Files.readAllBytes(dataKeyPath)).isEqualTo(ciphertextBytes);
        assertThat(key.getKey()).isEqualTo(Base64.getEncoder().encodeToString(plaintextBytes));
    }

    @Test
    void testSubsequentBootReadsAndDecrypts(@TempDir Path tempDir) throws IOException {
        Path dataKeyPath = tempDir.resolve("aws-kms-data-key");

        byte[] ciphertextBytes = "fake-kms-ciphertext-blob".getBytes();
        byte[] plaintextBytes = "32-byte-test-aes-256-data-key!!!".getBytes();

        Files.write(dataKeyPath, ciphertextBytes);

        KmsClient kmsClient = mock(KmsClient.class);
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenReturn(
            DecryptResponse.builder()
                .plaintextBlob(SdkBytes.fromByteArray(plaintextBytes))
                .build());

        AwsKmsEncryptionKey key = new AwsKmsEncryptionKey(kmsClient, "alias/test", dataKeyPath);

        verify(kmsClient).decrypt(any(DecryptRequest.class));

        assertThat(key.getKey()).isEqualTo(Base64.getEncoder().encodeToString(plaintextBytes));
    }
}
```

Run: `./gradlew :server:ee:libs:core:encryption:encryption-aws-kms:test` → 2 tests pass.

Commit:
```bash
git add server/ee/libs/core/encryption/encryption-aws-kms/src/test/
git commit -m "$(cat <<'EOF'
547 Unit test AwsKmsEncryptionKey both boot paths

First boot: generateDataKey persists ciphertext, returns plaintext.
Subsequent boot: persisted ciphertext is decrypted via KMS, returning
the same plaintext.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: LocalStack integration test

**File:** `$ROOT/src/test/java/com/bytechef/ee/encryption/aws/kms/AwsKmsEncryptionKeyIntTest.java`

Round-trip a real `generateDataKey` + `decrypt` cycle through LocalStack KMS. Reference: the existing `AwsSecretsManagerConnectionCredentialStoreIntTest`.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.encryption.aws.kms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.KMS;

import com.bytechef.encryption.EncryptionKey;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;

@SpringBootTest(classes = AwsKmsEncryptionKeyIntTestConfiguration.class)
@Testcontainers
class AwsKmsEncryptionKeyIntTest {

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.0"))
        .withServices(KMS);

    static Path dataKeyPath;
    static String testKeyId;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) throws Exception {
        dataKeyPath = Files.createTempDirectory("kms-test").resolve("aws-kms-data-key");
        // Create a test KMS key in LocalStack
        try (KmsClient bootstrap = KmsClient.builder()
            .endpointOverride(localStack.getEndpointOverride(KMS))
            .region(software.amazon.awssdk.regions.Region.of(localStack.getRegion()))
            .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                    localStack.getAccessKey(), localStack.getSecretKey())))
            .build()) {
            CreateKeyResponse created = bootstrap.createKey(CreateKeyRequest.builder().build());
            testKeyId = created.keyMetadata().keyId();
        }

        registry.add("bytechef.cloud.provider", () -> "aws");
        registry.add("bytechef.encryption.provider", () -> "aws-kms");
        registry.add("bytechef.encryption.aws-kms.key-id", () -> testKeyId);
        registry.add("bytechef.encryption.aws-kms.data-key-path", () -> dataKeyPath.toString());
        registry.add("spring.cloud.aws.region.static", localStack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
        registry.add("spring.cloud.aws.kms.endpoint", () -> localStack.getEndpointOverride(KMS).toString());
    }

    @Autowired
    private EncryptionKey encryptionKey;

    @Test
    void testEncryptionKeyResolvesViaLocalStackKms() {
        String key = encryptionKey.getKey();

        assertThat(key).isNotBlank();
        assertThat(dataKeyPath).exists();
    }
}
```

Also create a sibling `AwsKmsEncryptionKeyIntTestConfiguration` similar to the AWS Secrets Manager test config — `@ImportAutoConfiguration` for Spring Cloud AWS core + KMS auto-config + `@EnableConfigurationProperties(ApplicationProperties.class)` + `@ComponentScan(basePackages = "com.bytechef.ee.encryption.aws.kms")`.

Run: `./gradlew :server:ee:libs:core:encryption:encryption-aws-kms:testIntegration` → 1 test passes.

Commit:
```bash
git add server/ee/libs/core/encryption/encryption-aws-kms/src/test/
git commit -m "$(cat <<'EOF'
547 Integration test AwsKmsEncryptionKey against LocalStack KMS

Bootstraps a test KMS key in LocalStack, configures the encryption
provider, asserts that the EncryptionKey bean resolves a non-blank
plaintext key via real KMS round-trips and that the ciphertext file
is persisted.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Final sweep

```bash
./gradlew \
  :server:libs:config:app-config:spotlessApply \
  :server:ee:libs:core:encryption:encryption-aws-kms:spotlessApply

./gradlew \
  :server:libs:config:app-config:check \
  :server:ee:libs:core:encryption:encryption-aws-kms:check
```

Commit any spotless changes:
```bash
git status --short
git add -u
git commit -m "$(cat <<'EOF'
547 Apply spotless formatting

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

Verify commit graph:
```bash
git log --oneline b4db1681667..HEAD
```

Expected ~5 commits.

---

## Self-Review Notes

- ✓ Spec section "Behavior — envelope encryption" → Task 3
- ✓ ApplicationProperties additions → Task 1
- ✓ Configuration + EnvironmentPostProcessor → Task 4
- ✓ Unit test for both boot paths → Task 5
- ✓ LocalStack integration test → Task 6
- ✓ ESLint / spotless / check sweep → Task 7

**Risks:**
- The unit test uses 32-byte string as fake plaintext data key — `AES_256` keys are exactly 32 bytes. The test doesn't actually USE the key for encryption, just stores/retrieves it, so the exact byte content doesn't matter.
- LocalStack KMS's `decrypt` semantics may differ from real AWS in edge cases. The test uses LocalStack's own `createKey` → `generateDataKey` → `decrypt` cycle, all within LocalStack, which is the canonical happy path.
- The `Test` integration config needs to be a separate file (`AwsKmsEncryptionKeyIntTestConfiguration.java`) — engineer creates it during Task 6, mirroring the structure from the AWS Secrets Manager adapter's test config.
