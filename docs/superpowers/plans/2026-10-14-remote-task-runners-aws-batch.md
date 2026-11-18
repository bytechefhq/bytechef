# Remote Task Runners — Plan 3: AWS Batch EE Runner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Enterprise AWS Batch task runner, so that Script and Commands tasks can run as an ECS multi-container Batch job (Fargate or EC2) in a customer's AWS account, reached through an `aws` connection. It has the same result shape, `resume`/`delete` semantics and deadlines as the Docker and Kubernetes runners.

**Architecture:** A new EE module provides a `RemoteTaskRunnerBackend` bean, `AwsBatchTaskRunnerBackend`, which the CE `TaskRunnerRegistryImpl` wraps in the existing `RemoteTaskRunner` orchestrator.
- **Seam:** the backend talks to AWS only through a small `AwsBatchOperations` interface. A fake drives the unit tests. `SdkAwsBatchOperations` is the production adapter, and a LocalStack integration test proves its S3 staging. End-to-end Batch stays a documented manual check, because LocalStack's Batch emulation is a paid feature.
- **Staging:** inputs and outputs move through a per-run S3 prefix, using presigned URLs so the job itself needs no S3 permissions.
- **Job:** a job definition is registered per run with three containers sharing an ephemeral volume:
  - a `curl` init container that downloads and unpacks the inputs;
  - the author's image as `main`, with stderr redirected to `.stderr`;
  - an essential output container that tars and uploads `output/` and `.stderr`.
- **Connection:** a thin EE `aws` component owns the connection definition. It has no actions.

This plan mirrors the Kubernetes runner already on the branch (`server/ee/libs/platform/platform-component/platform-component-runner-kubernetes`). It follows the same module layout, the same seam/fake testing style, the same secret-hygiene rules and the same interrupt-aware error wrapping. When a step says "as the Kubernetes backend does", open that file and follow it.

**Tech Stack:** Java 25, Spring Boot 4, AWS SDK v2 2.54.20 (`batch`, `s3`, `cloudwatchlogs`, `sts`, `apache-client`; managed by the `software.amazon.awssdk:bom` import in `server/build.gradle.kts`), JUnit 5, AssertJ, Mockito, and Testcontainers LocalStack from the 2.0.5 line.

**Spec:** `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md`, §7 (AWS Batch), with §3 (backend contract), §8 (errors) and §6 (the Kubernetes runner's as-built conventions). Builds on Plan 1 (CE core) and Plan 2 (Kubernetes), both on branch `0_p_script_runners`.

## Global Constraints

- Branch `0_p_script_runners` is local and unpushed. Never push, never `git stash`, never amend existing commits. Run `git rev-parse --abbrev-ref HEAD` before every commit.
- Commit messages are **subject only** (`git commit -m "<subject>"`): no body, no `Co-Authored-By`, no `Generated with` line.
- Every file under `server/ee/`, tests included, gets the ByteChef Enterprise license header, copied verbatim from `server/ee/libs/modules/components/request/src/main/java/com/bytechef/ee/component/request/RequestComponentHandler.java`, and class Javadoc with `@author Ivica Cardic` and `@version ee`.
- Every Spring bean under `server/ee/` is `@ConditionalOnEEVersion` (`com.bytechef.platform.annotation.ConditionalOnEEVersion`).
- Runner type ID: `aws-batch`. Operator switch: `bytechef.script.runners.aws-batch.enabled` (default `false`, in `application-bytechef.yml`).
- Remote working directory: `/bytechef/work`. Fixed layout under it: `input.json`, `output/`, `.stderr`, `output.tar` (written by the output container).
- Job and job-definition names: `bytechef-<name value>`, where the name value is the key itself when it matches `[A-Za-z0-9_-]{1,119}` and starts alphanumeric. Otherwise it is `k-` plus the first 16 hex characters of the key's SHA-256. Tags: `bytechef.io/managed-by=bytechef`, `bytechef.io/key=<full key>`, `bytechef.io/job-id=<jobId>` (when present), `bytechef.io/prefix=<S3 prefix>`. `find` verifies the full key from the `bytechef.io/key` tag.
- S3 prefix per run: `bytechef/<jobId or none>/<name value>-<8 random hex>/`. Objects: `input.tar` (uploaded by ByteChef) and `output.tar` (uploaded by the job).
- Presigned URLs are valid for the completion budget plus 10 minutes. With `roleArn`, the STS session is requested for the same duration, at least 15 minutes and at most 12 hours, and `validate` rejects a budget that does not fit.
- No endpoint override reaches users. The adapter's endpoint override is package-private and used only by the LocalStack test.
- Base64 goes through `com.bytechef.commons.util.EncodingUtils`. Hashing uses SHA-256.
- Access keys, secret keys, session tokens, presigned URLs and connection parameters never appear in exception messages or logs. Wrap AWS SDK failures as `IllegalStateException("The aws-batch runner failed while <phase>: <error code> (HTTP <status>)")`. When there was no response, use `"…: no response from AWS"`. Keep the SDK exception as the cause, and never interpolate its message.
- Interrupts follow the Kubernetes backend's rules. A cause chain containing `InterruptedException`, an `InterruptedIOException` that is not a `SocketTimeoutException`, or `software.amazon.awssdk.core.exception.AbortedException`, or a set interrupt flag, means interrupted. On an interrupt, clear the flag and throw `InterruptedException` (in `find`: restore the flag and throw `IllegalStateException`). `cleanup` and submit's failure cleanup clear the flag during their AWS calls and restore it afterwards.
- No `_` in test method names. Unit tests end in `Test`, integration tests in `IntTest`. No prose comments in code (short Javadoc is fine). `TODO:` is forbidden. Code style follows `CLAUDE.md`.
- Run `./gradlew <module>:spotlessApply` on every touched module before committing. Revert reformatting of files the task does not touch.
- Long Gradle runs go in the background with output to a log under the scratch directory (`$SCRATCH`), polled. No single tool call may exceed 10 minutes.

## Review Focus

1. **An exit-code-0 `SUCCEEDED` job whose `main` exited non-zero** must fail with `main`'s code, not succeed. Success is read from `main`, never from the job status. Pinned in Task 4, `testMainsExitCodeWinsOverTheJobStatus`.
2. **A queue with no capacity** (the job sits in `RUNNABLE`) must fail after `waitUntilRunning`, saying the queue may have no capacity and quoting `statusReason`. It must not wait out the completion budget. Pinned in Task 4, `testAJobThatNeverLeavesRunnableFailsAfterWaitUntilRunning`.
3. **A Fargate size that is not a valid combination** (for example 1 vCPU with 1024 MiB), or one that leaves `main` less than 0.25 vCPU or 128 MiB, must be rejected at validate time with the allowed values. Pinned in Task 3, `testAnInvalidFargateCombinationIsRejectedWithTheAllowedMemories`.
4. **A `roleArn` with a completion budget over 11 h 50 m** must be rejected up front, because the presigned URLs could not outlive the session. Pinned in Task 2, `testValidateRejectsABudgetTheAssumedRoleCannotCover`.
5. **A redelivered task whose job already finished** must resubmit, not re-attach to a terminal job; `find` returns only non-terminal jobs. Another tenant's job with the same job name must be ignored. Pinned in Task 4, `testFindIgnoresTerminalJobsAndJobsWithAnotherKey`.

## File Structure

| Path | Responsibility |
|---|---|
| `server/ee/libs/modules/components/aws/…/AwsComponentHandler.java`, `connection/AwsConnection.java`, `constant/AwsConstants.java`, `assets/aws.svg`, `definition/aws_v1.json` | Thin connection component (Task 1) |
| `server/ee/libs/platform/platform-component/platform-component-runner-aws-batch/` | New EE module, package `com.bytechef.ee.platform.component.runner.aws.batch` |
| `…/AwsConnectionSettings.java` | Parses and validates the `aws` connection; builds the credentials provider (static or STS) (Task 2) |
| `…/AwsBatchJobNames.java` | Key → job name value, tag names, S3 prefix (Task 3) |
| `…/AwsBatchRunnerSettings.java` | Parses and validates the runner's task properties, including the Fargate sizing (Task 3) |
| `…/AwsBatchJobFactory.java` | Builds the `RegisterJobDefinitionRequest` and `SubmitJobRequest` (Task 3) |
| `…/AwsBatchOperations.java`, `…/AwsBatchOperationsFactory.java` | Seam over AWS (Task 4) |
| `…/AwsBatchTaskRunnerBackend.java` | The `RemoteTaskRunnerBackend` (Task 4) |
| `…/SdkAwsBatchOperations.java` | AWS SDK v2 adapter (Task 5) |
| `server/apps/server-app/build.gradle.kts`, `server/ee/apps/worker-app/build.gradle.kts`, `application-bytechef.yml`, `settings.gradle.kts` | Wiring (Tasks 1, 2, 6) |
| `commands/README.mdx`, `script/README.mdx`, the spec | Docs (Task 6) |

Path shorthands: `BATCH` = `server/ee/libs/platform/platform-component/platform-component-runner-aws-batch`, `BATCH_PKG` = `BATCH/src/main/java/com/bytechef/ee/platform/component/runner/aws/batch`, `BATCH_TEST` = `BATCH/src/test/java/com/bytechef/ee/platform/component/runner/aws/batch`. Gradle: `:BATCH` = `:server:ee:libs:platform:platform-component:platform-component-runner-aws-batch`, `:AWS_COMPONENT` = `:server:ee:libs:modules:components:aws`.

## Commit map (all new commits on top of `7ffb6bce42d`)

| Task | Commit subject |
|---|---|
| 1 | `Add the aws connection component` |
| 2 | `Add the aws batch runner module and its connection settings` |
| 3 | `Build the aws batch runner's job definition` |
| 4 | `Add the aws batch task runner backend` |
| 5 | `Talk to aws through the sdk and prove the s3 staging on localstack` |
| 6 | `Enable the aws batch runner in the server and worker apps and document it` |

---
### Task 1: The `aws` connection component (EE)

**Files:**
- Create: `server/ee/libs/modules/components/aws/build.gradle.kts`
- Create: `server/ee/libs/modules/components/aws/src/main/java/com/bytechef/ee/component/aws/AwsComponentHandler.java`
- Create: `…/aws/connection/AwsConnection.java`, `…/aws/constant/AwsConstants.java`
- Create: `server/ee/libs/modules/components/aws/src/main/resources/assets/aws.svg`
- Create: `server/ee/libs/modules/components/aws/src/test/java/com/bytechef/ee/component/aws/AwsComponentHandlerTest.java`
- Generated: `server/ee/libs/modules/components/aws/src/test/resources/definition/aws_v1.json`
- Modify: `settings.gradle.kts`, adding `include("server:ee:libs:modules:components:aws")` first in alphabetical order among the `server:ee:libs:modules:components:*` lines.

**Interfaces:**
- Produces: component `aws`, version 1. Connection property names as public constants on `AwsConstants`: `ACCESS_KEY_ID = "accessKeyId"`, `SECRET_ACCESS_KEY = "secretAccessKey"`, `REGION = "region"`, `ROLE_ARN = "roleArn"`, `EXTERNAL_ID = "externalId"`. Task 2 parses these exact names and pins them with a test.

- [ ] **Step 1: Build file**

```kotlin
version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.ee.component.aws;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class AwsComponentHandlerTest {

    private final ComponentDefinition componentDefinition = new AwsComponentHandler().getDefinition();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/aws_v1.json", componentDefinition);
    }

    @Test
    public void testTheConnectionOffersOneCustomAuthorizationWithEveryAccountField() {
        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations()
            .orElseThrow();

        assertThat(authorizations).hasSize(1);

        Authorization authorization = authorizations.getFirst();

        assertThat(authorization.getType()).isEqualTo(Authorization.AuthorizationType.CUSTOM);

        List<String> propertyNames = authorization.getProperties()
            .orElseThrow()
            .stream()
            .map(Property::getName)
            .toList();

        assertThat(propertyNames).containsExactly(
            "accessKeyId", "secretAccessKey", "region", "roleArn", "externalId");
    }
}
```

(Plan 2's `KubernetesComponentHandlerTest` has the same shape; if the navigation API differs, copy what that test does.)

- [ ] **Step 3: Run, expect a compile failure**

Run: `./gradlew :server:ee:libs:modules:components:aws:test -q`

- [ ] **Step 4: Implement**

`AwsConstants.java`:

```java
package com.bytechef.ee.component.aws.constant;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class AwsConstants {

    public static final String ACCESS_KEY_ID = "accessKeyId";
    public static final String EXTERNAL_ID = "externalId";
    public static final String REGION = "region";
    public static final String ROLE_ARN = "roleArn";
    public static final String SECRET_ACCESS_KEY = "secretAccessKey";

    private AwsConstants() {
    }
}
```

`AwsConnection.java`:

```java
package com.bytechef.ee.component.aws.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.aws.constant.AwsConstants.ACCESS_KEY_ID;
import static com.bytechef.ee.component.aws.constant.AwsConstants.EXTERNAL_ID;
import static com.bytechef.ee.component.aws.constant.AwsConstants.REGION;
import static com.bytechef.ee.component.aws.constant.AwsConstants.ROLE_ARN;
import static com.bytechef.ee.component.aws.constant.AwsConstants.SECRET_ACCESS_KEY;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class AwsConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Access Keys")
                .properties(
                    string(ACCESS_KEY_ID)
                        .label("Access Key ID")
                        .description("The IAM access key ByteChef signs its AWS calls with.")
                        .required(true),
                    string(SECRET_ACCESS_KEY)
                        .label("Secret Access Key")
                        .controlType(ControlType.PASSWORD)
                        .required(true),
                    string(REGION)
                        .label("Region")
                        .description("The AWS region of the job queue and the staging bucket, for example eu-west-1.")
                        .required(true),
                    string(ROLE_ARN)
                        .label("Role ARN")
                        .description(
                            "A role to assume with the access keys before every run. Its maximum session duration " +
                                "must cover the run's completion budget plus 10 minutes.")
                        .required(false),
                    string(EXTERNAL_ID)
                        .label("External ID")
                        .description("The external ID the role's trust policy expects, if any.")
                        .required(false)));

    private AwsConnection() {
    }
}
```

(If `.title(...)` is not available on the authorization, drop it, as Plan 2 did.)

`AwsComponentHandler.java`:

```java
package com.bytechef.ee.component.aws;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.aws.connection.AwsConnection;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * The connection an AWS Batch task runner uses to reach an AWS account. It has no actions of its own.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component("aws_v1_ComponentHandler")
@ConditionalOnEEVersion
public class AwsComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("aws")
        .title("AWS")
        .description("Runs script and commands tasks as AWS Batch jobs in an AWS account.")
        .icon("path:assets/aws.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(AwsConnection.CONNECTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

`aws.svg`: copy the existing AWS icon used by the CE `aws-s3` component if it has one (`server/libs/modules/components/aws/aws-s3/src/main/resources/assets/`); otherwise draw a simple 24×24 icon in `#FF9900` with a `viewBox`.

- [ ] **Step 5: Generate the snapshot and run**

```bash
rm -f server/ee/libs/modules/components/aws/src/test/resources/definition/aws_v1.json
rm -rf server/ee/libs/modules/components/aws/build/resources/test/definition
./gradlew :server:ee:libs:modules:components:aws:spotlessApply :server:ee:libs:modules:components:aws:test -q
./gradlew :server:ee:libs:modules:components:aws:spotlessApply :server:ee:libs:modules:components:aws:test -q
```

The second run passes against the generated file. Check that `secretAccessKey` has the `PASSWORD` control type, and that spotless leaves the JSON stable (Plan 2 needed one spotless pass on its snapshot).

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts server/ee/libs/modules/components/aws
git commit -m "Add the aws connection component"
```

---

### Task 2: Runner module scaffold and connection settings (EE)

**Files:**
- Create: `BATCH/build.gradle.kts`
- Modify: `settings.gradle.kts`, adding `include("server:ee:libs:platform:platform-component:platform-component-runner-aws-batch")` in alphabetical order with the other `server:ee:libs:platform:platform-component:*` lines.
- Create: `BATCH_PKG/AwsConnectionSettings.java`
- Test: `BATCH_TEST/AwsConnectionSettingsTest.java`

**Interfaces:**
- Consumes: `ComponentConnection` (`getParameters()`); Task 1's `AwsConstants` (tests only).
- Produces: `record AwsConnectionSettings(String accessKeyId, String secretAccessKey, String region, @Nullable String roleArn, @Nullable String externalId)` with:
  - `static AwsConnectionSettings of(ComponentConnection connection)`;
  - `void validate()`;
  - `void validateBudget(Duration completionBudget)`, which rejects a budget whose URL validity (budget plus 10 minutes) exceeds 12 h when `roleArn` is set;
  - `AwsCredentialsProvider credentialsProvider(Duration sessionDuration)`, which returns a `StaticCredentialsProvider`, or an `StsAssumeRoleCredentialsProvider` wrapping it when `roleArn` is set (the caller closes it when it is `SdkAutoCloseable`);
  - `Region awsRegion()`;
  - `static Duration urlValidity(Duration completionBudget)`, which returns budget plus 10 minutes;
  - a `toString()` that masks the secret key.

- [ ] **Step 1: Build file**

```kotlin
dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("software.amazon.awssdk:apache-client")
    implementation("software.amazon.awssdk:batch")
    implementation("software.amazon.awssdk:cloudwatchlogs")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sts")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    testImplementation(project(":server:ee:libs:modules:components:aws"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:test:test-support"))
}
```

The versions come from the `software.amazon.awssdk:bom` platform import in `server/build.gradle.kts` (2.54.20). Confirm with `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:dependencies --configuration compileClasspath | grep -E "awssdk:(batch|s3|sts)"`.

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.component.aws.constant.AwsConstants;
import com.bytechef.platform.component.ComponentConnection;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AwsConnectionSettingsTest {

    @Test
    void testEveryFieldIsRead() {
        AwsConnectionSettings settings = settings(
            Map.of(
                "accessKeyId", " AKIAEXAMPLE ", "secretAccessKey", "secret-key", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef", "externalId", "ext"));

        assertThat(settings.accessKeyId()).isEqualTo("AKIAEXAMPLE");
        assertThat(settings.secretAccessKey()).isEqualTo("secret-key");
        assertThat(settings.awsRegion()).isEqualTo(Region.EU_WEST_1);
        assertThat(settings.roleArn()).isEqualTo("arn:aws:iam::123456789012:role/bytechef");
        assertThat(settings.externalId()).isEqualTo("ext");
    }

    @Test
    void testValidateRequiresKeysAndARegion() {
        assertThatThrownBy(() -> settings(Map.of("secretAccessKey", "s", "region", "eu-west-1")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("access key");

        assertThatThrownBy(() -> settings(Map.of("accessKeyId", "a", "region", "eu-west-1")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("secret access key");

        assertThatThrownBy(() -> settings(Map.of("accessKeyId", "a", "secretAccessKey", "s")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("region");

        settings(Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1")).validate();
    }

    @Test
    void testValidateRejectsARoleArnThatIsNotAnIamRole() {
        assertThatThrownBy(
            () -> settings(
                Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn", "bytechef"))
                    .validate())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("arn:aws");
    }

    @Test
    void testValidateRejectsABudgetTheAssumedRoleCannotCover() {
        AwsConnectionSettings withRole = settings(
            Map.of(
                "accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef"));

        withRole.validateBudget(Duration.ofHours(11)
            .plusMinutes(50));

        assertThatThrownBy(() -> withRole.validateBudget(Duration.ofHours(11)
            .plusMinutes(51)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 hours");

        settings(Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1"))
            .validateBudget(Duration.ofHours(48));
    }

    @Test
    void testStaticKeysWithoutARoleAreUsedAsTheyAre() {
        AwsCredentialsProvider credentialsProvider = settings(
            Map.of("accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1"))
                .credentialsProvider(Duration.ofHours(1));

        assertThat(credentialsProvider).isInstanceOf(StaticCredentialsProvider.class);

        AwsCredentials credentials = credentialsProvider.resolveCredentials();

        assertThat(credentials.accessKeyId()).isEqualTo("a");
        assertThat(credentials.secretAccessKey()).isEqualTo("s");
    }

    @Test
    void testARoleIsAssumedThroughSts() {
        AwsCredentialsProvider credentialsProvider = settings(
            Map.of(
                "accessKeyId", "a", "secretAccessKey", "s", "region", "eu-west-1", "roleArn",
                "arn:aws:iam::123456789012:role/bytechef"))
                    .credentialsProvider(Duration.ofHours(1));

        assertThat(credentialsProvider).isInstanceOf(StsAssumeRoleCredentialsProvider.class);

        ((StsAssumeRoleCredentialsProvider) credentialsProvider).close();
    }

    @Test
    void testTheUrlValidityIsTheBudgetPlusTenMinutes() {
        assertThat(AwsConnectionSettings.urlValidity(Duration.ofHours(1))).isEqualTo(Duration.ofMinutes(70));
    }

    @Test
    void testToStringNeverShowsTheSecretKey() {
        String text = settings(Map.of("accessKeyId", "AKIA", "secretAccessKey", "secret-key", "region", "eu-west-1"))
            .toString();

        assertThat(text).doesNotContain("secret-key")
            .contains("eu-west-1");
    }

    @Test
    void testTheConnectionComponentDeclaresEveryParsedField() {
        assertThat(AwsConnectionSettings.ACCESS_KEY_ID).isEqualTo(AwsConstants.ACCESS_KEY_ID);
        assertThat(AwsConnectionSettings.SECRET_ACCESS_KEY).isEqualTo(AwsConstants.SECRET_ACCESS_KEY);
        assertThat(AwsConnectionSettings.REGION).isEqualTo(AwsConstants.REGION);
        assertThat(AwsConnectionSettings.ROLE_ARN).isEqualTo(AwsConstants.ROLE_ARN);
        assertThat(AwsConnectionSettings.EXTERNAL_ID).isEqualTo(AwsConstants.EXTERNAL_ID);
    }

    private static AwsConnectionSettings settings(Map<String, ?> parameters) {
        return AwsConnectionSettings.of(new ComponentConnection("aws", 1, 1L, new HashMap<>(parameters), null));
    }
}
```

- [ ] **Step 3: Run, expect a compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`

- [ ] **Step 4: Implement**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.ComponentConnection;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

/**
 * The AWS account an AWS Batch task runner works in, read from the task's {@code aws} connection.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record AwsConnectionSettings(
    String accessKeyId, String secretAccessKey, String region, @Nullable String roleArn,
    @Nullable String externalId) {

    static final String ACCESS_KEY_ID = "accessKeyId";
    static final String EXTERNAL_ID = "externalId";
    static final String REGION = "region";
    static final String ROLE_ARN = "roleArn";
    static final String SECRET_ACCESS_KEY = "secretAccessKey";

    private static final Duration MAX_SESSION = Duration.ofHours(12);
    private static final Duration MIN_SESSION = Duration.ofMinutes(15);
    private static final Pattern ROLE_ARN_PATTERN = Pattern.compile("^arn:aws[a-z-]*:iam::\\d{12}:role/.+$");
    private static final String ROLE_SESSION_NAME = "bytechef-task-runner";
    private static final Duration URL_MARGIN = Duration.ofMinutes(10);

    public static AwsConnectionSettings of(ComponentConnection connection) {
        Map<String, ?> parameters = connection.getParameters();

        return new AwsConnectionSettings(
            textOrEmpty(parameters, ACCESS_KEY_ID), textOrEmpty(parameters, SECRET_ACCESS_KEY),
            textOrEmpty(parameters, REGION), text(parameters, ROLE_ARN), text(parameters, EXTERNAL_ID));
    }

    static Duration urlValidity(Duration completionBudget) {
        return completionBudget.plus(URL_MARGIN);
    }

    public Region awsRegion() {
        return Region.of(region);
    }

    public AwsCredentialsProvider credentialsProvider(Duration sessionDuration) {
        StaticCredentialsProvider staticCredentialsProvider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey));

        if (roleArn == null) {
            return staticCredentialsProvider;
        }

        Duration clamped = sessionDuration.compareTo(MIN_SESSION) < 0 ? MIN_SESSION : sessionDuration;

        if (clamped.compareTo(MAX_SESSION) > 0) {
            clamped = MAX_SESSION;
        }

        StsClient stsClient = StsClient.builder()
            .region(awsRegion())
            .credentialsProvider(staticCredentialsProvider)
            .build();

        AssumeRoleRequest.Builder assumeRoleRequestBuilder = AssumeRoleRequest.builder()
            .roleArn(roleArn)
            .roleSessionName(ROLE_SESSION_NAME)
            .durationSeconds((int) clamped.toSeconds());

        if (externalId != null) {
            assumeRoleRequestBuilder.externalId(externalId);
        }

        return StsAssumeRoleCredentialsProvider.builder()
            .stsClient(stsClient)
            .refreshRequest(assumeRoleRequestBuilder.build())
            .build();
    }

    public void validate() {
        if (accessKeyId.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs an access key ID");
        }

        if (secretAccessKey.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs a secret access key");
        }

        if (region.isEmpty()) {
            throw new IllegalArgumentException("The aws connection needs a region");
        }

        if (roleArn != null && !ROLE_ARN_PATTERN.matcher(roleArn)
            .matches()) {

            throw new IllegalArgumentException(
                "The aws connection's role ARN must look like arn:aws:iam::<account>:role/<name>");
        }
    }

    public void validateBudget(Duration completionBudget) {
        if (roleArn != null && urlValidity(completionBudget).compareTo(MAX_SESSION) > 0) {
            throw new IllegalArgumentException(
                ("The aws-batch runner cannot run a job for %s with an assumed role: its presigned URLs must outlive " +
                    "the job by 10 minutes, and an assumed-role session lasts at most 12 hours").formatted(
                        completionBudget));
        }
    }

    @Override
    public String toString() {
        return "AwsConnectionSettings[accessKeyId=%s, secretAccessKey=***, region=%s, roleArn=%s]".formatted(
            accessKeyId, region, roleArn);
    }

    @Nullable
    private static String text(Map<String, ?> parameters, String name) {
        Object value = parameters.get(name);

        if (value == null) {
            return null;
        }

        String trimmed = value.toString()
            .trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String textOrEmpty(Map<String, ?> parameters, String name) {
        String value = text(parameters, name);

        return value == null ? "" : value;
    }
}
```

If SpotBugs reports REDOS on `ROLE_ARN_PATTERN`: the pattern has no ambiguous quantifiers, and role ARNs are capped by IAM at 2048 characters. Reject longer values before matching, and suppress narrowly with that justification, as the Kubernetes module's `QuantityPattern` holder does.

- [ ] **Step 5: Run tests and `check`**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`, then the module's `check` in the background.
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts server/ee/libs/platform/platform-component/platform-component-runner-aws-batch
git commit -m "Add the aws batch runner module and its connection settings"
```

---
### Task 3: Names, runner settings and the job definition (EE, pure)

**Files:**
- Create: `BATCH_PKG/AwsBatchJobNames.java`, `BATCH_PKG/AwsBatchRunnerSettings.java`, `BATCH_PKG/AwsBatchJobFactory.java`
- Test: `BATCH_TEST/AwsBatchJobNamesTest.java`, `BATCH_TEST/AwsBatchRunnerSettingsTest.java`, `BATCH_TEST/AwsBatchJobFactoryTest.java`

**Interfaces:**
- Consumes: `RemoteJobSpec` (`key()`, `languageId()`, `interpreter()`, `environment()`, `remoteSourcePath()`, `remoteStderrPath()`, `runnerParameters()`), `RemoteJobKey(value, jobId)`, `Parameters`, and `TaskRunnerConstants.ENTRYPOINT`/`IMAGE`.
- Produces:
  - `final class AwsBatchJobNames` with:
    - constants `TAG_MANAGED_BY = "bytechef.io/managed-by"`, `TAG_KEY = "bytechef.io/key"`, `TAG_JOB_ID = "bytechef.io/job-id"`, `TAG_PREFIX = "bytechef.io/prefix"`, `MANAGED_BY = "bytechef"`, `INPUT_OBJECT = "input.tar"`, `OUTPUT_OBJECT = "output.tar"`;
    - `static String nameValue(String key)`, `static String jobName(RemoteJobKey key)`, `static String newPrefix(RemoteJobKey key)`, `static Map<String, String> tags(RemoteJobKey key, String prefix)`.
  - `record AwsBatchRunnerSettings(String image, String jobQueue, String bucket, Platform platform, @Nullable String executionRoleArn, @Nullable String taskRoleArn, BigDecimal vcpu, int memory, boolean assignPublicIp, Duration completionCheckInterval, String fileSidecarImage)` with:
    - nested `enum Platform { FARGATE, EC2 }`;
    - `static AwsBatchRunnerSettings of(String languageId, Parameters parameters)` (validates);
    - `static List<ModifiableValueProperty<?, ?>> properties()`;
    - `static String jobQueue(Parameters)`, `static String bucket(Parameters)` and `static Duration completionCheckInterval(Parameters)` (package-private readers that `find`, `await` and `cleanup` use);
    - `BigDecimal mainVcpu()`, `int mainMemory()`.
  - `final class AwsBatchJobFactory` with:
    - constants `WORK_DIRECTORY = "/bytechef/work"`, `INIT_CONTAINER = "bytechef-init"`, `MAIN_CONTAINER = "main"`, `OUTPUT_CONTAINER = "bytechef-output"`, `INPUT_URL_VARIABLE = "BYTECHEF_INPUT_URL"`, `OUTPUT_URL_VARIABLE = "BYTECHEF_OUTPUT_URL"`;
    - `static RegisterJobDefinitionRequest jobDefinition(RemoteJobSpec spec, AwsBatchRunnerSettings settings, Map<String, String> tags, URL inputUrl, URL outputUrl, Duration completionBudget)`;
    - `static SubmitJobRequest submitJob(RemoteJobKey key, String jobDefinitionArn, String jobQueue, Map<String, String> tags)`.

- [ ] **Step 1: Write the failing tests**

`AwsBatchJobNamesTest.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AwsBatchJobNamesTest {

    @Test
    void testAKeyThatFitsAJobNameIsUsedAsIs() {
        assertThat(AwsBatchJobNames.nameValue("public-100-7")).isEqualTo("public-100-7");
        assertThat(AwsBatchJobNames.jobName(new RemoteJobKey("public-100-7", 100L))).isEqualTo("bytechef-public-100-7");
    }

    @Test
    void testAKeyThatDoesNotFitIsHashed() {
        assertThat(AwsBatchJobNames.nameValue("t".repeat(120))).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("tenant.a-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("-tenant-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(AwsBatchJobNames.nameValue("tenant.a-1-7")).isEqualTo(AwsBatchJobNames.nameValue("tenant.a-1-7"));
    }

    @Test
    void testEachRunGetsItsOwnPrefixUnderTheJob() {
        RemoteJobKey key = new RemoteJobKey("public-100-7", 100L);

        String first = AwsBatchJobNames.newPrefix(key);
        String second = AwsBatchJobNames.newPrefix(key);

        assertThat(first).matches("bytechef/100/public-100-7-[0-9a-f]{8}/");
        assertThat(first).isNotEqualTo(second);
        assertThat(AwsBatchJobNames.newPrefix(new RemoteJobKey("random", null))).startsWith("bytechef/none/random-");
    }

    @Test
    void testTagsCarryTheManagerTheFullKeyTheJobAndThePrefix() {
        assertThat(AwsBatchJobNames.tags(new RemoteJobKey("tenant.a-1-7", 1L), "bytechef/1/x/"))
            .containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/key", "tenant.a-1-7")
            .containsEntry("bytechef.io/job-id", "1")
            .containsEntry("bytechef.io/prefix", "bytechef/1/x/");

        assertThat(AwsBatchJobNames.tags(new RemoteJobKey("random", null), "p/")).doesNotContainKey("bytechef.io/job-id");
    }
}
```

`AwsBatchRunnerSettingsTest.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchRunnerSettingsTest {

    private static final String QUEUE = "arn:aws:batch:eu-west-1:123456789012:job-queue/bytechef";
    private static final String EXECUTION_ROLE = "arn:aws:iam::123456789012:role/ecsTaskExecutionRole";

    @Test
    void testDefaultsFollowTheLanguageAndFargate() {
        AwsBatchRunnerSettings settings = settings("python", Map.of());

        assertThat(settings.image()).isEqualTo("python:3.12-slim");
        assertThat(settings.platform()).isEqualTo(AwsBatchRunnerSettings.Platform.FARGATE);
        assertThat(settings.vcpu()).isEqualByComparingTo("1");
        assertThat(settings.memory()).isEqualTo(2048);
        assertThat(settings.assignPublicIp()).isFalse();
        assertThat(settings.completionCheckInterval()).isEqualTo(Duration.ofSeconds(5));
        assertThat(settings.fileSidecarImage()).isEqualTo("curlimages/curl:8.11.1");
        assertThat(settings("shell", Map.of()).image()).isEqualTo("ubuntu:24.04");
        assertThat(settings("js", Map.of()).image()).isEqualTo("node:22-alpine");
    }

    @Test
    void testTheSidecarsAreSubtractedFromMain() {
        AwsBatchRunnerSettings settings = settings("python", Map.of("vcpu", "2", "memory", 4096));

        assertThat(settings.mainVcpu()).isEqualByComparingTo(new BigDecimal("1.5"));
        assertThat(settings.mainMemory()).isEqualTo(4096 - 128);
    }

    @Test
    void testAnInvalidFargateCombinationIsRejectedWithTheAllowedMemories() {
        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "1", "memory", 1024)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("2048")
            .hasMessageContaining("8192");

        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "3", "memory", 8192)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("vcpu");
    }

    @Test
    void testATotalThatLeavesMainTooLittleIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("vcpu", "0.5", "memory", 1024)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("0.25");

        assertThatThrownBy(
            () -> settings("python", Map.of("platform", "EC2", "vcpu", "1", "memory", 200)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128");
    }

    @Test
    void testEveryFargateSizeIsFoundWhateverItsScale() {
        assertThat(settings("python", Map.of("vcpu", "16", "memory", 32768)).mainVcpu()).isEqualByComparingTo("15.5");
        assertThat(settings("python", Map.of("vcpu", "2.0", "memory", 4096)).mainVcpu()).isEqualByComparingTo("1.5");
    }

    @Test
    void testEc2AcceptsAnySizeThatLeavesMainEnough() {
        AwsBatchRunnerSettings settings = settings("python", Map.of("platform", "EC2", "vcpu", "3", "memory", 3000));

        assertThat(settings.mainVcpu()).isEqualByComparingTo("2.5");
        assertThat(settings.executionRoleArn()).isNull();
    }

    @Test
    void testFargateNeedsAnExecutionRole() {
        Map<String, Object> parameters = new HashMap<>(base());

        parameters.remove("executionRoleArn");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(parameters)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("execution role");
    }

    @Test
    void testTheQueueAndTheBucketAreRequiredAndChecked() {
        Map<String, Object> noQueue = new HashMap<>(base());

        noQueue.remove("jobQueue");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(noQueue)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("job queue");

        Map<String, Object> badBucket = new HashMap<>(base());

        badBucket.put("bucket", "Not_A_Bucket");

        assertThatThrownBy(() -> AwsBatchRunnerSettings.of("python", ParametersFactory.create(badBucket)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Not_A_Bucket");
    }

    @Test
    void testAQueueNameOrAnArnIsAccepted() {
        assertThat(settings("python", Map.of("jobQueue", "bytechef")).jobQueue()).isEqualTo("bytechef");
        assertThat(settings("python", Map.of()).jobQueue()).isEqualTo(QUEUE);
    }

    @Test
    void testAMalformedIntervalOrPlatformIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("completionCheckInterval", "often")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("completionCheckInterval");

        assertThatThrownBy(() -> settings("python", Map.of("platform", "LAMBDA")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("LAMBDA");
    }

    @Test
    void testPropertiesAreFreshAndCoverEverySetting() {
        assertThat(AwsBatchRunnerSettings.properties()
            .getFirst()).isNotSameAs(AwsBatchRunnerSettings.properties()
                .getFirst());
        assertThat(AwsBatchRunnerSettings.properties()).extracting(ValueProperty::getName)
            .containsExactly(
                "image", "entrypoint", "jobQueue", "bucket", "platform", "executionRoleArn", "taskRoleArn", "vcpu",
                "memory", "assignPublicIp", "completionCheckInterval", "fileSidecarImage");
    }

    private static Map<String, Object> base() {
        return Map.of("jobQueue", QUEUE, "bucket", "bytechef-staging", "executionRoleArn", EXECUTION_ROLE);
    }

    private static AwsBatchRunnerSettings settings(String languageId, Map<String, ?> overrides) {
        Map<String, Object> parameters = new HashMap<>(base());

        parameters.putAll(overrides);

        return AwsBatchRunnerSettings.of(languageId, ParametersFactory.create(parameters));
    }
}
```

`AwsBatchJobFactoryTest.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.batch.model.AssignPublicIp;
import software.amazon.awssdk.services.batch.model.JobDefinitionType;
import software.amazon.awssdk.services.batch.model.KeyValuePair;
import software.amazon.awssdk.services.batch.model.PlatformCapability;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.ResourceRequirement;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerProperties;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchJobFactoryTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);

    @Test
    void testTheDefinitionIsAnEcsMultiContainerJobOnFargate() throws Exception {
        RegisterJobDefinitionRequest request = definition(Map.of());

        assertThat(request.jobDefinitionName()).isEqualTo("bytechef-public-100-7");
        assertThat(request.type()).isEqualTo(JobDefinitionType.CONTAINER);
        assertThat(request.platformCapabilities()).containsExactly(PlatformCapability.FARGATE);
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .executionRoleArn()).isEqualTo("arn:aws:iam::123456789012:role/exec");
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .networkConfiguration()
            .assignPublicIp()).isEqualTo(AssignPublicIp.DISABLED);
        assertThat(request.timeout()
            .attemptDurationSeconds()).isEqualTo(3600 + 60);
        assertThat(request.retryStrategy()
            .attempts()).isEqualTo(1);
        assertThat(request.propagateTags()).isTrue();
        assertThat(request.tags()).containsEntry("bytechef.io/key", "public-100-7");
    }

    @Test
    void testOnlyTheOutputContainerIsEssentialAndTheOrderIsEnforced() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of()));

        assertThat(containers).containsOnlyKeys("bytechef-init", "main", "bytechef-output");
        assertThat(containers.get("bytechef-init")
            .essential()).isFalse();
        assertThat(containers.get("main")
            .essential()).isFalse();
        assertThat(containers.get("bytechef-output")
            .essential()).isTrue();
        assertThat(containers.get("main")
            .dependsOn()
            .getFirst()
            .containerName()).isEqualTo("bytechef-init");
        assertThat(containers.get("main")
            .dependsOn()
            .getFirst()
            .condition()).isEqualTo("SUCCESS");
        assertThat(containers.get("bytechef-output")
            .dependsOn()
            .getFirst()
            .containerName()).isEqualTo("main");
        assertThat(containers.get("bytechef-output")
            .dependsOn()
            .getFirst()
            .condition()).isEqualTo("COMPLETE");
    }

    @Test
    void testTheContainersMoveTheFilesThroughThePresignedUrls() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of()));

        assertThat(String.join(" ", containers.get("bytechef-init")
            .command())).contains("curl -fsS \"$BYTECHEF_INPUT_URL\"")
                .contains("tar -xf - -C /bytechef/work");
        assertThat(environment(containers.get("bytechef-init"))).containsEntry(
            "BYTECHEF_INPUT_URL", "https://bucket.s3.amazonaws.com/in?sig=1");

        String outputCommand = String.join(" ", containers.get("bytechef-output")
            .command());

        assertThat(outputCommand).contains("tar -cf output.tar")
            .contains("curl -fsS -T output.tar \"$BYTECHEF_OUTPUT_URL\"");
        assertThat(environment(containers.get("bytechef-output"))).containsEntry(
            "BYTECHEF_OUTPUT_URL", "https://bucket.s3.amazonaws.com/out?sig=2");

        assertThat(environment(containers.get("main"))).doesNotContainKeys("BYTECHEF_INPUT_URL", "BYTECHEF_OUTPUT_URL")
            .containsEntry("BYTECHEF_INPUT_FILE", "/bytechef/work/input.json");
    }

    @Test
    void testMainRunsTheInterpreterWithStderrRedirectedInTheAuthorsImage() throws Exception {
        TaskContainerProperties main = containers(definition(Map.of())).get("main");

        assertThat(main.image()).isEqualTo("python:3.12-slim");
        assertThat(main.command()).containsExactly(
            "sh", "-c", "exec \"$@\" 2>/bytechef/work/.stderr", "sh", "python3", "/bytechef/work/script.py");
        assertThat(main.mountPoints()
            .getFirst()
            .containerPath()).isEqualTo("/bytechef/work");
    }

    @Test
    void testResourcesSplitTheTotalBetweenMainAndTheSidecars() throws Exception {
        Map<String, TaskContainerProperties> containers = containers(definition(Map.of("vcpu", "2", "memory", 4096)));

        assertThat(resources(containers.get("main"))).containsEntry("VCPU", "1.5")
            .containsEntry("MEMORY", "3968");
        assertThat(resources(containers.get("bytechef-init"))).containsEntry("VCPU", "0.25")
            .containsEntry("MEMORY", "64");
        assertThat(resources(containers.get("bytechef-output"))).containsEntry("VCPU", "0.25")
            .containsEntry("MEMORY", "64");
    }

    @Test
    void testEc2HasNoNetworkConfigurationOrExecutionRoleRequirement() throws Exception {
        RegisterJobDefinitionRequest request = definition(Map.of("platform", "EC2", "executionRoleArn", ""));

        assertThat(request.platformCapabilities()).containsExactly(PlatformCapability.EC2);
        assertThat(request.ecsProperties()
            .taskProperties()
            .getFirst()
            .networkConfiguration()).isNull();
    }

    @Test
    void testTheSubmitCarriesTheJobNameQueueDefinitionAndTags() {
        SubmitJobRequest request = AwsBatchJobFactory.submitJob(
            KEY, "arn:aws:batch:eu-west-1:1:job-definition/bytechef-public-100-7:1", "bytechef",
            Map.of("bytechef.io/key", "public-100-7"));

        assertThat(request.jobName()).isEqualTo("bytechef-public-100-7");
        assertThat(request.jobQueue()).isEqualTo("bytechef");
        assertThat(request.jobDefinition()).endsWith(":1");
        assertThat(request.tags()).containsEntry("bytechef.io/key", "public-100-7");
        assertThat(request.propagateTags()).isTrue();
    }

    private static RegisterJobDefinitionRequest definition(Map<String, ?> overrides) throws Exception {
        Map<String, Object> parameters = new java.util.HashMap<>(
            Map.of(
                "jobQueue", "bytechef", "bucket", "bytechef-staging", "executionRoleArn",
                "arn:aws:iam::123456789012:role/exec"));

        parameters.putAll(overrides);

        AwsBatchRunnerSettings settings = AwsBatchRunnerSettings.of("python", ParametersFactory.create(parameters));

        RemoteJobSpec spec = new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"),
            Map.of("HOME", "/bytechef/work", "BYTECHEF_INPUT_FILE", "/bytechef/work/input.json"), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(parameters));

        URL inputUrl = URI.create("https://bucket.s3.amazonaws.com/in?sig=1")
            .toURL();
        URL outputUrl = URI.create("https://bucket.s3.amazonaws.com/out?sig=2")
            .toURL();

        return AwsBatchJobFactory.jobDefinition(
            spec, settings, AwsBatchJobNames.tags(KEY, "bytechef/100/x/"), inputUrl, outputUrl, Duration.ofHours(1));
    }

    private static Map<String, TaskContainerProperties> containers(RegisterJobDefinitionRequest request) {
        return request.ecsProperties()
            .taskProperties()
            .getFirst()
            .containers()
            .stream()
            .collect(Collectors.toMap(TaskContainerProperties::name, container -> container));
    }

    private static Map<String, String> environment(TaskContainerProperties container) {
        return container.environment()
            .stream()
            .collect(Collectors.toMap(KeyValuePair::name, KeyValuePair::value));
    }

    private static Map<String, String> resources(TaskContainerProperties container) {
        return container.resourceRequirements()
            .stream()
            .collect(Collectors.toMap(ResourceRequirement::typeAsString, ResourceRequirement::value));
    }
}
```

(Import `java.util.HashMap` rather than writing it fully qualified.)

- [ ] **Step 2: Run, expect a compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`

- [ ] **Step 3: Implement `AwsBatchJobNames`**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Job names, tags and S3 locations an AWS Batch run uses.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class AwsBatchJobNames {

    static final String INPUT_OBJECT = "input.tar";
    static final String MANAGED_BY = "bytechef";
    static final String OUTPUT_OBJECT = "output.tar";
    static final String TAG_JOB_ID = "bytechef.io/job-id";
    static final String TAG_KEY = "bytechef.io/key";
    static final String TAG_MANAGED_BY = "bytechef.io/managed-by";
    static final String TAG_PREFIX = "bytechef.io/prefix";

    private static final String JOB_NAME_PREFIX = "bytechef-";
    private static final Pattern NAME_VALUE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{0,118}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AwsBatchJobNames() {
    }

    static String jobName(RemoteJobKey key) {
        return JOB_NAME_PREFIX + nameValue(key.value());
    }

    static String nameValue(String key) {
        if (NAME_VALUE.matcher(key)
            .matches()) {

            return key;
        }

        return "k-" + sha256Hex(key).substring(0, 16);
    }

    static String newPrefix(RemoteJobKey key) {
        Long jobId = key.jobId();

        byte[] randomBytes = new byte[4];

        SECURE_RANDOM.nextBytes(randomBytes);

        return "bytechef/%s/%s-%s/".formatted(
            jobId == null ? "none" : String.valueOf(jobId), nameValue(key.value()), HexFormat.of()
                .formatHex(randomBytes));
    }

    static Map<String, String> tags(RemoteJobKey key, String prefix) {
        Map<String, String> tags = new LinkedHashMap<>();

        tags.put(TAG_MANAGED_BY, MANAGED_BY);
        tags.put(TAG_KEY, key.value());
        tags.put(TAG_PREFIX, prefix);

        Long jobId = key.jobId();

        if (jobId != null) {
            tags.put(TAG_JOB_ID, String.valueOf(jobId));
        }

        return tags;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of()
                .formatHex(messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
```

A Batch job name (and job definition name) allows up to 128 letters, digits, hyphens and underscores; `bytechef-` plus at most 119 characters stays inside that.

- [ ] **Step 4: Implement `AwsBatchRunnerSettings`**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The AWS Batch runner's task properties, read and checked once.
 *
 * @author Ivica Cardic
 * @version ee
 */
record AwsBatchRunnerSettings(
    String image, String jobQueue, String bucket, Platform platform, @Nullable String executionRoleArn,
    @Nullable String taskRoleArn, BigDecimal vcpu, int memory, boolean assignPublicIp,
    Duration completionCheckInterval, String fileSidecarImage) {

    static final String ASSIGN_PUBLIC_IP = "assignPublicIp";
    static final String BUCKET = "bucket";
    static final String COMPLETION_CHECK_INTERVAL = "completionCheckInterval";
    static final String EXECUTION_ROLE_ARN = "executionRoleArn";
    static final String FILE_SIDECAR_IMAGE = "fileSidecarImage";
    static final String JOB_QUEUE = "jobQueue";
    static final String MEMORY = "memory";
    static final String PLATFORM = "platform";
    static final String TASK_ROLE_ARN = "taskRoleArn";
    static final String VCPU = "vcpu";

    static final BigDecimal SIDECAR_VCPU = new BigDecimal("0.25");
    static final int SIDECAR_MEMORY = 64;

    private static final Pattern BUCKET_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$");
    private static final Duration DEFAULT_COMPLETION_CHECK_INTERVAL = Duration.ofSeconds(5);
    private static final String DEFAULT_FILE_SIDECAR_IMAGE = "curlimages/curl:8.11.1";
    private static final Map<String, String> DEFAULT_IMAGES = Map.of(
        "javascript", "node:22-alpine", "js", "node:22-alpine", "python", "python:3.12-slim", "shell",
        "ubuntu:24.04");
    private static final int DEFAULT_MEMORY = 2048;
    private static final BigDecimal DEFAULT_VCPU = BigDecimal.ONE;
    private static final BigDecimal MIN_MAIN_VCPU = new BigDecimal("0.25");
    private static final int MIN_MAIN_MEMORY = 128;
    private static final NavigableMap<BigDecimal, List<Integer>> FARGATE_MEMORIES = fargateMemories();

    enum Platform {

        FARGATE, EC2
    }

    static AwsBatchRunnerSettings of(String languageId, Parameters parameters) {
        String image = text(parameters.getString(IMAGE));

        if (image == null) {
            image = DEFAULT_IMAGES.get(languageId);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        String jobQueue = jobQueue(parameters);

        if (jobQueue.isEmpty()) {
            throw new IllegalArgumentException("The aws-batch runner needs a job queue (its name or ARN)");
        }

        String bucket = bucket(parameters);

        if (!BUCKET_PATTERN.matcher(bucket)
            .matches()) {

            throw new IllegalArgumentException(
                "The aws-batch runner's staging bucket '%s' is not a valid S3 bucket name".formatted(bucket));
        }

        Platform platform = platform(parameters);

        String executionRoleArn = text(parameters.getString(EXECUTION_ROLE_ARN));

        if (platform == Platform.FARGATE && executionRoleArn == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner needs an execution role on Fargate, so the task can pull images and write logs");
        }

        BigDecimal vcpu = vcpu(parameters);
        int memory = parameters.getInteger(MEMORY, DEFAULT_MEMORY);

        checkSize(platform, vcpu, memory);

        String fileSidecarImage = text(parameters.getString(FILE_SIDECAR_IMAGE));

        return new AwsBatchRunnerSettings(
            image, jobQueue, bucket, platform, executionRoleArn, text(parameters.getString(TASK_ROLE_ARN)), vcpu,
            memory, parameters.getBoolean(ASSIGN_PUBLIC_IP, false), completionCheckInterval(parameters),
            fileSidecarImage == null ? DEFAULT_FILE_SIDECAR_IMAGE : fileSidecarImage);
    }

    static String bucket(Parameters parameters) {
        String bucket = text(parameters.getString(BUCKET));

        return bucket == null ? "" : bucket;
    }

    static String jobQueue(Parameters parameters) {
        String jobQueue = text(parameters.getString(JOB_QUEUE));

        return jobQueue == null ? "" : jobQueue;
    }

    static List<ModifiableValueProperty<?, ?>> properties() {
        List<ModifiableValueProperty<?, ?>> properties = new ArrayList<>();

        properties.add(
            string(IMAGE)
                .label("Image")
                .description("The container image the task runs in. Defaults to the image for the action's language.")
                .required(false));
        properties.add(
            array(ENTRYPOINT)
                .label("Entrypoint")
                .description(
                    "Replaces the interpreter the source is handed to. Defaults to the interpreter for the action's " +
                        "language; the source file is appended either way. The image must provide /bin/sh.")
                .items(string())
                .required(false));
        properties.add(
            string(JOB_QUEUE)
                .label("Job Queue")
                .description("The name or ARN of an ECS-backed AWS Batch job queue.")
                .required(true));
        properties.add(
            string(BUCKET)
                .label("Staging Bucket")
                .description(
                    "The S3 bucket inputs and outputs are staged in, under bytechef/. The job reaches it only through " +
                        "presigned URLs.")
                .required(true));
        properties.add(
            string(PLATFORM)
                .label("Platform")
                .description("The compute the job queue runs on.")
                .options(option("Fargate", Platform.FARGATE.name()), option("EC2", Platform.EC2.name()))
                .defaultValue(Platform.FARGATE.name())
                .required(false));
        properties.add(
            string(EXECUTION_ROLE_ARN)
                .label("Execution Role ARN")
                .description("The role ECS uses to pull the images and write the logs. Required on Fargate.")
                .required(false));
        properties.add(
            string(TASK_ROLE_ARN)
                .label("Task Role ARN")
                .description("The role the task's own code runs as, for its AWS calls.")
                .required(false));
        properties.add(
            string(VCPU)
                .label("vCPU")
                .description(
                    "The task's total vCPU, sidecars included (they take 0.5). On Fargate: 1, 2, 4, 8 or 16.")
                .defaultValue("1")
                .required(false));
        properties.add(
            integer(MEMORY)
                .label("Memory (MiB)")
                .description(
                    "The task's total memory in MiB, sidecars included (they take 128). On Fargate it must suit the " +
                        "vCPU.")
                .defaultValue(DEFAULT_MEMORY)
                .required(false));
        properties.add(
            bool(ASSIGN_PUBLIC_IP)
                .label("Assign Public IP")
                .description(
                    "Gives a Fargate task a public IP, which it needs to pull public images and reach S3 from a " +
                        "public subnet without a NAT gateway.")
                .defaultValue(false)
                .required(false));
        properties.add(
            string(COMPLETION_CHECK_INTERVAL)
                .label("Completion Check Interval")
                .description("How often the job's state and logs are polled, as an ISO-8601 duration.")
                .defaultValue("PT5S")
                .required(false));
        properties.add(
            string(FILE_SIDECAR_IMAGE)
                .label("File Sidecar Image")
                .description("The image that moves files in and out of the task; it needs sh, curl and tar.")
                .defaultValue(DEFAULT_FILE_SIDECAR_IMAGE)
                .required(false));

        return properties;
    }

    BigDecimal mainVcpu() {
        return vcpu.subtract(SIDECAR_VCPU.multiply(BigDecimal.valueOf(2)));
    }

    int mainMemory() {
        return memory - SIDECAR_MEMORY * 2;
    }

    private static void checkSize(Platform platform, BigDecimal vcpu, int memory) {
        BigDecimal mainVcpu = vcpu.subtract(SIDECAR_VCPU.multiply(BigDecimal.valueOf(2)));
        int mainMemory = memory - SIDECAR_MEMORY * 2;

        if (mainVcpu.compareTo(MIN_MAIN_VCPU) < 0 || mainMemory < MIN_MAIN_MEMORY) {
            throw new IllegalArgumentException(
                ("The aws-batch runner's task size %s vCPU / %d MiB leaves the task's own container less than " +
                    "0.25 vCPU / 128 MiB after the two file sidecars (0.25 vCPU / 64 MiB each)").formatted(
                        vcpu.toPlainString(), memory));
        }

        if (platform != Platform.FARGATE) {
            return;
        }

        List<Integer> memories = FARGATE_MEMORIES.get(vcpu);

        if (memories == null) {
            throw new IllegalArgumentException(
                "The aws-batch runner's vcpu %s is not a Fargate size; use 1, 2, 4, 8 or 16".formatted(
                    vcpu.toPlainString()));
        }

        if (!memories.contains(memory)) {
            throw new IllegalArgumentException(
                "The aws-batch runner's memory %d MiB does not suit %s vCPU on Fargate; use one of %s".formatted(
                    memory, vcpu.toPlainString(), memories));
        }
    }

    static Duration completionCheckInterval(Parameters parameters) {
        String value = text(parameters.getString(COMPLETION_CHECK_INTERVAL));

        if (value == null) {
            return DEFAULT_COMPLETION_CHECK_INTERVAL;
        }

        Duration duration;

        try {
            duration = Duration.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    value, COMPLETION_CHECK_INTERVAL),
                exception);
        }

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                "The aws-batch runner's %s must be greater than zero".formatted(COMPLETION_CHECK_INTERVAL));
        }

        return duration;
    }

    private static NavigableMap<BigDecimal, List<Integer>> fargateMemories() {
        NavigableMap<BigDecimal, List<Integer>> fargateMemories = new TreeMap<>();

        fargateMemories.put(new BigDecimal("1"), range(2048, 8192, 1024));
        fargateMemories.put(new BigDecimal("2"), range(4096, 16384, 1024));
        fargateMemories.put(new BigDecimal("4"), range(8192, 30720, 1024));
        fargateMemories.put(new BigDecimal("8"), range(16384, 61440, 4096));
        fargateMemories.put(new BigDecimal("16"), range(32768, 122880, 8192));

        return Collections.unmodifiableNavigableMap(fargateMemories);
    }

    private static Platform platform(Parameters parameters) {
        String value = text(parameters.getString(PLATFORM));

        if (value == null) {
            return Platform.FARGATE;
        }

        try {
            return Platform.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner does not know the platform '%s'; use FARGATE or EC2".formatted(value),
                exception);
        }
    }

    private static List<Integer> range(int from, int to, int step) {
        List<Integer> values = new ArrayList<>();

        for (int value = from; value <= to; value += step) {
            values.add(value);
        }

        return List.copyOf(values);
    }

    @Nullable
    private static String text(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal vcpu(Parameters parameters) {
        String value = text(parameters.getString(VCPU));

        if (value == null) {
            return DEFAULT_VCPU;
        }

        try {
            return new BigDecimal(value).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                "The aws-batch runner cannot read '%s' as a vcpu amount".formatted(value), exception);
        }
    }
}
```

`FARGATE_MEMORIES` is a `TreeMap` because `BigDecimal` keys must be compared numerically: `new BigDecimal("16").stripTrailingZeros()` is `1.6E+1`, which a `HashMap` lookup would never match. Add a test case for `vcpu` `"16"` with `memory` `32768` to `testEc2AcceptsAnySizeThatLeavesMainEnough`'s neighbour (a positive Fargate case), so the lookup is pinned. Fargate task sizes below 1 vCPU (0.25 and 0.5) are left out on purpose. The two sidecars take 0.5 vCPU between them, so a smaller task would leave `main` nothing. `parameters.getString(VCPU)` must also accept a numeric value. If `Parameters.getString` does not convert a number, read it with `parameters.get(VCPU)` and use `String.valueOf`.

- [ ] **Step 5: Implement `AwsBatchJobFactory`**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.batch.model.AssignPublicIp;
import software.amazon.awssdk.services.batch.model.EcsProperties;
import software.amazon.awssdk.services.batch.model.EcsTaskProperties;
import software.amazon.awssdk.services.batch.model.JobDefinitionType;
import software.amazon.awssdk.services.batch.model.JobTimeout;
import software.amazon.awssdk.services.batch.model.KeyValuePair;
import software.amazon.awssdk.services.batch.model.MountPoint;
import software.amazon.awssdk.services.batch.model.NetworkConfiguration;
import software.amazon.awssdk.services.batch.model.PlatformCapability;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.ResourceRequirement;
import software.amazon.awssdk.services.batch.model.ResourceType;
import software.amazon.awssdk.services.batch.model.RetryStrategy;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerDependency;
import software.amazon.awssdk.services.batch.model.TaskContainerProperties;
import software.amazon.awssdk.services.batch.model.Volume;

/**
 * Builds the job definition an AWS Batch run registers: a file sidecar that downloads the inputs, the author's image
 * as {@code main}, and an essential file sidecar that uploads the outputs, so the job ends only after the upload.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class AwsBatchJobFactory {

    static final String INIT_CONTAINER = "bytechef-init";
    static final String INPUT_URL_VARIABLE = "BYTECHEF_INPUT_URL";
    static final String MAIN_CONTAINER = "main";
    static final String OUTPUT_CONTAINER = "bytechef-output";
    static final String OUTPUT_URL_VARIABLE = "BYTECHEF_OUTPUT_URL";
    static final String WORK_DIRECTORY = "/bytechef/work";

    private static final String INIT_SCRIPT =
        "curl -fsS \"$" + INPUT_URL_VARIABLE + "\" | tar -xf - -C " + WORK_DIRECTORY + " && mkdir -p " +
            WORK_DIRECTORY + "/output";
    private static final String OUTPUT_SCRIPT =
        "cd " + WORK_DIRECTORY + " && if [ -f .stderr ]; then tar -cf output.tar output .stderr; " +
            "else tar -cf output.tar output; fi && curl -fsS -T output.tar \"$" + OUTPUT_URL_VARIABLE + "\"";
    private static final int TIMEOUT_MARGIN_SECONDS = 60;
    private static final String VOLUME_NAME = "work";

    private AwsBatchJobFactory() {
    }

    static RegisterJobDefinitionRequest jobDefinition(
        RemoteJobSpec spec, AwsBatchRunnerSettings settings, Map<String, String> tags, URL inputUrl, URL outputUrl,
        Duration completionBudget) {

        List<String> mainCommand = new ArrayList<>(
            List.of("sh", "-c", "exec \"$@\" 2>" + spec.remoteStderrPath(), "sh"));

        mainCommand.addAll(spec.interpreter());
        mainCommand.add(spec.remoteSourcePath());

        TaskContainerProperties init = TaskContainerProperties.builder()
            .name(INIT_CONTAINER)
            .image(settings.fileSidecarImage())
            .essential(false)
            .command("sh", "-c", INIT_SCRIPT)
            .environment(KeyValuePair.builder()
                .name(INPUT_URL_VARIABLE)
                .value(inputUrl.toString())
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(AwsBatchRunnerSettings.SIDECAR_VCPU, AwsBatchRunnerSettings.SIDECAR_MEMORY))
            .build();

        TaskContainerProperties main = TaskContainerProperties.builder()
            .name(MAIN_CONTAINER)
            .image(settings.image())
            .essential(false)
            .command(mainCommand)
            .environment(
                spec.environment()
                    .entrySet()
                    .stream()
                    .map(entry -> KeyValuePair.builder()
                        .name(entry.getKey())
                        .value(entry.getValue())
                        .build())
                    .toList())
            .dependsOn(TaskContainerDependency.builder()
                .containerName(INIT_CONTAINER)
                .condition("SUCCESS")
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(settings.mainVcpu(), settings.mainMemory()))
            .build();

        TaskContainerProperties output = TaskContainerProperties.builder()
            .name(OUTPUT_CONTAINER)
            .image(settings.fileSidecarImage())
            .essential(true)
            .command("sh", "-c", OUTPUT_SCRIPT)
            .environment(KeyValuePair.builder()
                .name(OUTPUT_URL_VARIABLE)
                .value(outputUrl.toString())
                .build())
            .dependsOn(TaskContainerDependency.builder()
                .containerName(MAIN_CONTAINER)
                .condition("COMPLETE")
                .build())
            .mountPoints(workMount())
            .resourceRequirements(resources(AwsBatchRunnerSettings.SIDECAR_VCPU, AwsBatchRunnerSettings.SIDECAR_MEMORY))
            .build();

        EcsTaskProperties.Builder taskPropertiesBuilder = EcsTaskProperties.builder()
            .containers(init, main, output)
            .volumes(Volume.builder()
                .name(VOLUME_NAME)
                .build())
            .executionRoleArn(settings.executionRoleArn())
            .taskRoleArn(settings.taskRoleArn());

        boolean fargate = settings.platform() == AwsBatchRunnerSettings.Platform.FARGATE;

        if (fargate) {
            taskPropertiesBuilder.networkConfiguration(NetworkConfiguration.builder()
                .assignPublicIp(settings.assignPublicIp() ? AssignPublicIp.ENABLED : AssignPublicIp.DISABLED)
                .build());
        }

        return RegisterJobDefinitionRequest.builder()
            .jobDefinitionName(AwsBatchJobNames.jobName(spec.key()))
            .type(JobDefinitionType.CONTAINER)
            .platformCapabilities(fargate ? PlatformCapability.FARGATE : PlatformCapability.EC2)
            .ecsProperties(EcsProperties.builder()
                .taskProperties(taskPropertiesBuilder.build())
                .build())
            .timeout(JobTimeout.builder()
                .attemptDurationSeconds((int) completionBudget.toSeconds() + TIMEOUT_MARGIN_SECONDS)
                .build())
            .retryStrategy(RetryStrategy.builder()
                .attempts(1)
                .build())
            .propagateTags(true)
            .tags(tags)
            .build();
    }

    static SubmitJobRequest submitJob(
        RemoteJobKey key, String jobDefinitionArn, String jobQueue, Map<String, String> tags) {

        return SubmitJobRequest.builder()
            .jobName(AwsBatchJobNames.jobName(key))
            .jobDefinition(jobDefinitionArn)
            .jobQueue(jobQueue)
            .propagateTags(true)
            .tags(tags)
            .build();
    }

    private static List<ResourceRequirement> resources(BigDecimal vcpu, int memory) {
        return List.of(
            ResourceRequirement.builder()
                .type(ResourceType.VCPU)
                .value(vcpu.stripTrailingZeros()
                    .toPlainString())
                .build(),
            ResourceRequirement.builder()
                .type(ResourceType.MEMORY)
                .value(String.valueOf(memory))
                .build());
    }

    private static MountPoint workMount() {
        return MountPoint.builder()
            .sourceVolume(VOLUME_NAME)
            .containerPath(WORK_DIRECTORY)
            .build();
    }
}
```

The presigned URLs sit in the job definition's environment, so anyone with `batch:DescribeJobDefinitions` in the account can read them. They are time-limited and scoped to one run's two objects, and `cleanup` deregisters the definition. The docs (Task 6) state this. The SDK model names above match AWS SDK 2.54. If one differs (for example `TaskContainerDependency.condition` taking an enum), use that version's equivalent and say so in the report.

- [ ] **Step 6: Run tests and `check`**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`, then `check` in the background.
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-component/platform-component-runner-aws-batch
git commit -m "Build the aws batch runner's job definition"
```

---
### Task 4: The backend over an AWS seam (EE)

**Files:**
- Create: `BATCH_PKG/AwsBatchOperations.java`, `BATCH_PKG/AwsBatchOperationsFactory.java`, `BATCH_PKG/AwsBatchTaskRunnerBackend.java`
- Test: `BATCH_TEST/FakeAwsBatchOperations.java`, `BATCH_TEST/AwsBatchTaskRunnerBackendTest.java`

**Interfaces:**
- Consumes:
  - Task 2's `AwsConnectionSettings.of/validate/validateBudget/urlValidity`.
  - Task 3's `AwsBatchRunnerSettings.of/jobQueue/bucket/completionCheckInterval`, `AwsBatchJobFactory.jobDefinition/submitJob` and its constants, and `AwsBatchJobNames.jobName/newPrefix/tags`, `INPUT_OBJECT`, `OUTPUT_OBJECT`, `TAG_KEY`, `TAG_PREFIX`.
  - The CE SPI: `RemoteTaskRunnerBackend`, `RemoteJobContext` (`connection()`, `runnerParameters()`, `completionBudget()`), `TaskRunnerConnection("aws", 1)`, `CleanupMode`, `Deadline`, `RemoteJobExit`, `RemoteJobHandle`, `RemoteLogSink`.
- Produces:
  - `interface AwsBatchOperations extends AutoCloseable` (the methods below; Task 5 implements it);
  - `interface AwsBatchOperationsFactory { AwsBatchOperations create(AwsConnectionSettings connectionSettings, Duration sessionDuration); }`;
  - `AwsBatchTaskRunnerBackend(AwsBatchOperationsFactory operationsFactory, Duration minimumPollInterval)`, package-private. Task 5 adds the public Spring constructor.
  - Handle attributes `bucket`, `prefix` and `jobDefinitionArn`.

The full backend lifecycle:
- **`submit`:**
  1. Copy the input archive to a temp file.
  2. `putObject` it as `<prefix>input.tar`.
  3. Presign a GET for it and a PUT for `<prefix>output.tar`, both valid for the budget plus 10 minutes.
  4. Register the job definition.
  5. Submit the job.
  6. On any failure, deregister the definition (if registered) and delete the prefix (if staged).
- **`await`:**
  - Poll `describeJob` every `completionCheckInterval` (never faster than `minimumPollInterval`, so tests can use 5 ms).
  - Tail `main`'s CloudWatch stream from `/aws/batch/job` into stdout.
  - `SUBMITTED`, `PENDING`, `RUNNABLE` and `STARTING` are bounded by `runningDeadline`; when it passes, fail with "the job queue may have no capacity" plus `statusReason`. `RUNNING` is bounded by `completionDeadline` (`timedOut()`).
  - On `SUCCEEDED` or `FAILED`, drain the log, then return `main`'s exit code and reason. If `main` never ran, return a null exit code with the job's `statusReason`.
- **`collect`:** stream `<prefix>output.tar`, or return `null` when the object does not exist.
- **`find`:** list jobs on the queue named `bytechef-<name value>`. Take the newest non-terminal one whose `bytechef.io/key` tag equals the key.
- **`cleanup`:** `DETACH` does nothing. `KILL_AND_DELETE` terminates the job first. Both then deregister the definition and delete the prefix. Cleanup never throws, and it clears and restores the interrupt flag around its calls.
- Interrupt-aware SDK wrapping applies throughout (Global Constraints).

- [ ] **Step 1: The seam**

`AwsBatchOperations.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;

/**
 * The AWS calls the AWS Batch runner makes, kept narrow so the runner can be tested without AWS.
 *
 * @author Ivica Cardic
 * @version ee
 */
interface AwsBatchOperations extends AutoCloseable {

    void putObject(String bucket, String key, Path file);

    URL presignGet(String bucket, String key, Duration validity);

    URL presignPut(String bucket, String key, Duration validity);

    /**
     * Returns the object's content, or {@code null} when it does not exist.
     */
    @Nullable
    InputStream getObject(String bucket, String key);

    void deletePrefix(String bucket, String prefix);

    /**
     * Registers the job definition and returns its ARN.
     */
    String registerJobDefinition(RegisterJobDefinitionRequest request);

    void deregisterJobDefinition(String jobDefinitionArn);

    /**
     * Submits the job and returns its ID.
     */
    String submitJob(SubmitJobRequest request);

    @Nullable
    JobDetail describeJob(String jobId);

    /**
     * Lists every job on the queue with this name, whatever its status.
     */
    List<JobSummary> listJobs(String jobQueue, String jobName);

    void terminateJob(String jobId, String reason);

    /**
     * Reads the next page of a log stream; an absent stream reads as an empty page with the same token.
     */
    LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken);

    @Override
    void close();

    record LogPage(List<String> messages, @Nullable String nextForwardToken) {

        public LogPage {
            messages = List.copyOf(messages);
        }
    }
}
```

`AwsBatchOperationsFactory.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import java.time.Duration;

/**
 * @author Ivica Cardic
 * @version ee
 */
@FunctionalInterface
interface AwsBatchOperationsFactory {

    AwsBatchOperations create(AwsConnectionSettings connectionSettings, Duration sessionDuration);
}
```

- [ ] **Step 2: The test double**

`FakeAwsBatchOperations.java` (test source):

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.services.batch.model.EcsPropertiesDetail;
import software.amazon.awssdk.services.batch.model.EcsTaskDetails;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.batch.model.TaskContainerDetails;

/**
 * @author Ivica Cardic
 * @version ee
 */
final class FakeAwsBatchOperations implements AwsBatchOperations {

    static final String JOB_DEFINITION_ARN = "arn:aws:batch:eu-west-1:123456789012:job-definition/bytechef-x:1";
    static final String JOB_ID = "job-1";

    final Map<String, byte[]> objects = new LinkedHashMap<>();
    final Deque<JobDetail> jobSnapshots = new ArrayDeque<>();
    final Deque<LogPage> logPages = new ArrayDeque<>();
    final List<RegisterJobDefinitionRequest> registered = new ArrayList<>();
    final List<SubmitJobRequest> submitted = new ArrayList<>();
    final List<String> deregistered = new ArrayList<>();
    final List<String> deletedPrefixes = new ArrayList<>();
    final List<String> terminated = new ArrayList<>();
    final List<Duration> presignValidities = new ArrayList<>();
    final List<Duration> sessionDurations = new ArrayList<>();
    final Map<String, JobDetail> describedById = new LinkedHashMap<>();

    List<JobSummary> listedJobs = List.of();
    @Nullable RuntimeException failPutObject;
    @Nullable RuntimeException failRegister;
    @Nullable RuntimeException failSubmit;
    @Nullable RuntimeException failDescribe;
    @Nullable RuntimeException failCleanupCalls;
    boolean failWhileInterrupted;
    int closeCount;

    AwsBatchOperationsFactory factory() {
        return (connectionSettings, sessionDuration) -> {
            sessionDurations.add(sessionDuration);

            return this;
        };
    }

    @Override
    public void putObject(String bucket, String key, Path file) {
        if (failPutObject != null) {
            throw failPutObject;
        }

        try {
            objects.put(bucket + "/" + key, Files.readAllBytes(file));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    @Override
    public URL presignGet(String bucket, String key, Duration validity) {
        presignValidities.add(validity);

        return url("https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Signature=get");
    }

    @Override
    public URL presignPut(String bucket, String key, Duration validity) {
        presignValidities.add(validity);

        return url("https://" + bucket + ".s3.amazonaws.com/" + key + "?X-Amz-Signature=put");
    }

    @Override
    public @Nullable InputStream getObject(String bucket, String key) {
        byte[] bytes = objects.get(bucket + "/" + key);

        return bytes == null ? null : new ByteArrayInputStream(bytes);
    }

    @Override
    public void deletePrefix(String bucket, String prefix) {
        failIfInterruptedOrBroken();

        deletedPrefixes.add(bucket + "/" + prefix);
    }

    @Override
    public String registerJobDefinition(RegisterJobDefinitionRequest request) {
        if (failRegister != null) {
            throw failRegister;
        }

        registered.add(request);

        return JOB_DEFINITION_ARN;
    }

    @Override
    public void deregisterJobDefinition(String jobDefinitionArn) {
        failIfInterruptedOrBroken();

        deregistered.add(jobDefinitionArn);
    }

    @Override
    public String submitJob(SubmitJobRequest request) {
        if (failSubmit != null) {
            throw failSubmit;
        }

        submitted.add(request);

        return JOB_ID;
    }

    @Override
    public @Nullable JobDetail describeJob(String jobId) {
        if (failDescribe != null) {
            throw failDescribe;
        }

        JobDetail described = describedById.get(jobId);

        if (described != null) {
            return described;
        }

        return jobSnapshots.size() > 1 ? jobSnapshots.poll() : jobSnapshots.peek();
    }

    @Override
    public List<JobSummary> listJobs(String jobQueue, String jobName) {
        return listedJobs;
    }

    @Override
    public void terminateJob(String jobId, String reason) {
        failIfInterruptedOrBroken();

        terminated.add(jobId);
    }

    @Override
    public LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken) {
        LogPage logPage = logPages.poll();

        return logPage == null ? new LogPage(List.of(), nextToken) : logPage;
    }

    @Override
    public void close() {
        closeCount++;
    }

    static JobDetail job(JobStatus status, @Nullable Integer mainExitCode, @Nullable String reason, long createdAt,
        Map<String, String> tags) {

        TaskContainerDetails.Builder mainBuilder = TaskContainerDetails.builder()
            .name("main")
            .logStreamName("bytechef/main/abc");

        if (mainExitCode != null) {
            mainBuilder.exitCode(mainExitCode);
        }

        if (reason != null) {
            mainBuilder.reason(reason);
        }

        return JobDetail.builder()
            .jobId(JOB_ID)
            .jobName("bytechef-public-100-7")
            .status(status)
            .statusReason(reason)
            .createdAt(createdAt)
            .jobDefinition(JOB_DEFINITION_ARN)
            .tags(tags)
            .ecsProperties(EcsPropertiesDetail.builder()
                .taskProperties(EcsTaskDetails.builder()
                    .containers(mainBuilder.build())
                    .build())
                .build())
            .build();
    }

    private void failIfInterruptedOrBroken() {
        if (failWhileInterrupted && Thread.currentThread()
            .isInterrupted()) {

            throw new IllegalStateException("interrupted");
        }

        if (failCleanupCalls != null) {
            throw failCleanupCalls;
        }
    }

    private static URL url(String value) {
        try {
            return URI.create(value)
                .toURL();
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
```

(Import `java.net.MalformedURLException`. If an SDK 2.54 model name differs, for example `EcsPropertiesDetail` or `TaskContainerDetails`, use the actual class the Batch model exposes for `JobDetail.ecsProperties()` and its containers, and note it in the report.)

- [ ] **Step 3: Write the failing backend tests**

`AwsBatchTaskRunnerBackendTest.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.JOB_DEFINITION_ARN;
import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.JOB_ID;
import static com.bytechef.ee.platform.component.runner.aws.batch.FakeAwsBatchOperations.job;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.services.batch.model.BatchException;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;

/**
 * @author Ivica Cardic
 * @version ee
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AwsBatchTaskRunnerBackendTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);
    private static final Map<String, String> KEY_TAGS = Map.of(
        "bytechef.io/key", "public-100-7", "bytechef.io/prefix", "bytechef/100/public-100-7-abcd1234/");

    private final FakeAwsBatchOperations operations = new FakeAwsBatchOperations();
    private final AwsBatchTaskRunnerBackend backend = new AwsBatchTaskRunnerBackend(
        operations.factory(), Duration.ofMillis(5));

    @Test
    void testIdentity() {
        assertThat(backend.getType()).isEqualTo("aws-batch");
        assertThat(backend.getTitle()).isEqualTo("AWS Batch");
        assertThat(backend.getRemoteWorkingDirectory()).isEqualTo("/bytechef/work");
        assertThat(backend.getConnection()).hasValueSatisfying(connection -> {
            assertThat(connection.componentName()).isEqualTo("aws");
            assertThat(connection.componentVersion()).isEqualTo(1);
        });
    }

    @Test
    void testValidateChecksTheConnectionTheBudgetAndTheSettings() {
        assertThatThrownBy(() -> backend.validate(request(base()), context(base(), Map.of("region", "eu-west-1"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("access key");

        Map<String, Object> roleConnection = new HashMap<>(connection());

        roleConnection.put("roleArn", "arn:aws:iam::123456789012:role/bytechef");

        assertThatThrownBy(
            () -> backend.validate(request(base()), context(base(), roleConnection, Duration.ofHours(12))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 hours");

        backend.validate(request(base()), context(base(), connection()));
    }

    @Test
    void testSubmitStagesTheInputsRegistersTheDefinitionAndSubmits() throws Exception {
        byte[] archive = "tar-bytes".getBytes(StandardCharsets.UTF_8);

        RemoteJobHandle handle = backend.submit(
            spec(base()), new ByteArrayInputStream(archive), Deadline.after(Duration.ofMinutes(1)),
            context(base(), connection()));

        assertThat(handle.id()).isEqualTo(JOB_ID);
        assertThat(handle.attributes()).containsEntry("bucket", "bytechef-staging")
            .containsEntry("jobDefinitionArn", JOB_DEFINITION_ARN)
            .containsKey("prefix");

        String prefix = handle.attributes()
            .get("prefix");

        assertThat(prefix).startsWith("bytechef/100/public-100-7-");
        assertThat(operations.objects).containsEntry("bytechef-staging/" + prefix + "input.tar", archive);
        assertThat(operations.presignValidities).containsOnly(Duration.ofMinutes(70));
        assertThat(operations.registered.getFirst()
            .tags()).containsEntry("bytechef.io/prefix", prefix);
        assertThat(operations.submitted.getFirst()
            .jobQueue()).isEqualTo("bytechef");
        assertThat(operations.sessionDurations).contains(Duration.ofMinutes(70));
    }

    @Test
    void testSubmitCleansUpWhenTheSubmitFails() {
        operations.failSubmit = awsError("ClientException", 400, "denied for secret-key");

        assertThatThrownBy(
            () -> backend.submit(
                spec(base()), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("submitting the job")
                    .hasMessageContaining("ClientException")
                    .hasMessageContaining("HTTP 400")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-key"));

        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);
        assertThat(operations.deletedPrefixes).hasSize(1);
    }

    @Test
    void testSubmitCleansUpWhenStagingFailsWithoutRegisteringAnything() {
        operations.failPutObject = awsError("AccessDenied", 403, "nope");

        assertThatThrownBy(
            () -> backend.submit(
                spec(base()), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AccessDenied");

        assertThat(operations.registered).isEmpty();
        assertThat(operations.deregistered).isEmpty();
    }

    @Test
    void testASuccessfulJobTailsItsLogAndReturnsMainsExitCode() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.RUNNABLE, null, null, 1L, KEY_TAGS));
        operations.jobSnapshots.add(job(JobStatus.RUNNING, null, null, 1L, KEY_TAGS));
        operations.jobSnapshots.add(job(JobStatus.SUCCEEDED, 0, null, 1L, KEY_TAGS));
        operations.logPages.add(new AwsBatchOperations.LogPage(List.of("hello", "from batch"), "t1"));

        CollectingSink logSink = new CollectingSink();

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), logSink,
            context(base(), connection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isZero();
        assertThat(logSink.stdout.toString()).contains("hello\n")
            .contains("from batch\n");
    }

    @Test
    void testMainsExitCodeWinsOverTheJobStatus() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.SUCCEEDED, 3, null, 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), new CollectingSink(),
            context(base(), connection()));

        assertThat(exit.exitCode()).isEqualTo(3);
    }

    @Test
    void testAFailedJobWhoseMainNeverRanReportsTheStatusReason() throws Exception {
        operations.jobSnapshots.add(
            job(JobStatus.FAILED, null, "CannotPullContainerError: pull access denied", 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), new CollectingSink(),
            context(base(), connection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason()).contains("CannotPullContainerError");
    }

    @Test
    void testAJobThatNeverLeavesRunnableFailsAfterWaitUntilRunning() {
        operations.jobSnapshots.add(job(JobStatus.RUNNABLE, null, "Waiting for capacity", 1L, KEY_TAGS));

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMillis(50)), Deadline.after(Duration.ofHours(1)),
                new CollectingSink(), context(base(), connection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("did not start")
                    .hasMessageContaining("no capacity")
                    .hasMessageContaining("Waiting for capacity");
    }

    @Test
    void testARunningJobTimesOutAtTheCompletionDeadline() throws Exception {
        operations.jobSnapshots.add(job(JobStatus.RUNNING, null, null, 1L, KEY_TAGS));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMillis(50)),
            new CollectingSink(), context(base(), connection()));

        assertThat(exit.finished()).isFalse();
    }

    @Test
    void testAnAbortedCallIsAnInterrupt() {
        operations.failDescribe = AbortedException.builder()
            .message("Thread was interrupted")
            .build();

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)),
                new CollectingSink(), context(base(), connection())))
                    .isInstanceOf(InterruptedException.class);

        assertThat(Thread.currentThread()
            .isInterrupted()).isFalse();
    }

    @Test
    void testCollectStreamsTheOutputArchiveOrReturnsNull() throws Exception {
        operations.objects.put("bytechef-staging/bytechef/100/public-100-7-abcd1234/output.tar", "out".getBytes());

        try (InputStream inputStream = backend.collect(
            handle(), Deadline.after(Duration.ofMinutes(1)), context(base(), connection()))) {

            assertThat(inputStream).isNotNull();
            assertThat(inputStream.readAllBytes()).isEqualTo("out".getBytes());
        }

        assertThat(operations.closeCount).isPositive();

        operations.objects.clear();

        assertThat(backend.collect(handle(), Deadline.after(Duration.ofMinutes(1)), context(base(), connection())))
            .isNull();
    }

    @Test
    void testFindReturnsTheNewestRunningJobWithTheKey() {
        operations.listedJobs = List.of(summary("old", JobStatus.RUNNING, 1L), summary("new", JobStatus.RUNNING, 2L));
        operations.describedById.put("old", jobWithId("old", JobStatus.RUNNING, 1L, KEY_TAGS));
        operations.describedById.put("new", jobWithId("new", JobStatus.RUNNING, 2L, KEY_TAGS));

        Optional<RemoteJobHandle> handle = backend.find(KEY, context(base(), connection()));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains("new");
        assertThat(handle.orElseThrow()
            .attributes()).containsEntry("prefix", "bytechef/100/public-100-7-abcd1234/")
            .containsEntry("bucket", "bytechef-staging")
            .containsEntry("jobDefinitionArn", JOB_DEFINITION_ARN);
    }

    @Test
    void testFindIgnoresTerminalJobsAndJobsWithAnotherKey() {
        operations.listedJobs = List.of(summary("done", JobStatus.SUCCEEDED, 3L), summary("other", JobStatus.RUNNING, 2L));
        operations.describedById.put(
            "other", jobWithId("other", JobStatus.RUNNING, 2L, Map.of("bytechef.io/key", "other-tenant-100-7")));

        assertThat(backend.find(KEY, context(base(), connection()))).isEmpty();
        assertThat(operations.terminated).isEmpty();
    }

    @Test
    void testCleanupModes() {
        backend.cleanup(handle(), CleanupMode.DELETE, context(base(), connection()));

        assertThat(operations.terminated).isEmpty();
        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);
        assertThat(operations.deletedPrefixes).containsExactly("bytechef-staging/bytechef/100/public-100-7-abcd1234/");

        operations.deregistered.clear();

        backend.cleanup(handle(), CleanupMode.KILL_AND_DELETE, context(base(), connection()));

        assertThat(operations.terminated).containsExactly(JOB_ID);
        assertThat(operations.deregistered).containsExactly(JOB_DEFINITION_ARN);

        operations.terminated.clear();
        operations.deregistered.clear();

        backend.cleanup(handle(), CleanupMode.DETACH, context(base(), connection()));

        assertThat(operations.terminated).isEmpty();
        assertThat(operations.deregistered).isEmpty();
    }

    @Test
    void testCleanupNeverThrowsAndRunsEvenWhileInterrupted() {
        operations.failWhileInterrupted = true;

        Thread.currentThread()
            .interrupt();

        try {
            assertThatCode(() -> backend.cleanup(handle(), CleanupMode.KILL_AND_DELETE, context(base(), connection())))
                .doesNotThrowAnyException();

            assertThat(operations.terminated).containsExactly(JOB_ID);
            assertThat(Thread.currentThread()
                .isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        operations.failCleanupCalls = new IllegalStateException("boom");

        assertThatCode(() -> backend.cleanup(handle(), CleanupMode.DELETE, context(base(), connection())))
            .doesNotThrowAnyException();
    }

    private static BatchException awsError(String errorCode, int statusCode, String message) {
        return (BatchException) BatchException.builder()
            .message(message)
            .statusCode(statusCode)
            .awsErrorDetails(AwsErrorDetails.builder()
                .errorCode(errorCode)
                .errorMessage(message)
                .build())
            .build();
    }

    private static Map<String, Object> base() {
        return Map.of(
            "jobQueue", "bytechef", "bucket", "bytechef-staging", "executionRoleArn",
            "arn:aws:iam::123456789012:role/exec");
    }

    private static Map<String, Object> connection() {
        return Map.of("accessKeyId", "AKIA", "secretAccessKey", "secret-key", "region", "eu-west-1");
    }

    private static RemoteJobContext context(Map<String, ?> runnerParameters, Map<String, ?> connectionParameters) {
        return context(runnerParameters, connectionParameters, Duration.ofHours(1));
    }

    private static RemoteJobContext context(
        Map<String, ?> runnerParameters, Map<String, ?> connectionParameters, Duration completionBudget) {

        return new RemoteJobContext(
            new ComponentConnection("aws", 1, 1L, new HashMap<>(connectionParameters), null), Map.of(), null,
            ParametersFactory.create(runnerParameters), 1024L * 1024, completionBudget);
    }

    private static RemoteJobHandle handle() {
        return new RemoteJobHandle(
            JOB_ID,
            Map.of(
                "bucket", "bytechef-staging", "prefix", "bytechef/100/public-100-7-abcd1234/", "jobDefinitionArn",
                JOB_DEFINITION_ARN));
    }

    private static software.amazon.awssdk.services.batch.model.JobDetail jobWithId(
        String jobId, JobStatus status, long createdAt, Map<String, String> tags) {

        return job(status, null, null, createdAt, tags).toBuilder()
            .jobId(jobId)
            .build();
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, Map.of(),
            mock(ActionContext.class));
    }

    private static RemoteJobSpec spec(Map<String, ?> runnerParameters) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"), Map.of("HOME", "/bytechef/work"), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(runnerParameters));
    }

    private static JobSummary summary(String jobId, JobStatus status, long createdAt) {
        return JobSummary.builder()
            .jobId(jobId)
            .jobName("bytechef-public-100-7")
            .status(status)
            .createdAt(createdAt)
            .build();
    }

    static final class CollectingSink implements RemoteLogSink {

        final StringBuilder stdout = new StringBuilder();
        final StringBuilder stderr = new StringBuilder();

        @Override
        public void appendStdout(byte[] buffer, int length) {
            stdout.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }

        @Override
        public void appendStderr(byte[] buffer, int length) {
            stderr.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }
    }
}
```

(Import `software.amazon.awssdk.services.batch.model.JobDetail` rather than fully qualifying it. If `BatchException.builder()` in 2.54 returns a builder whose `build()` is already typed, drop the cast.)

- [ ] **Step 4: Run, expect a compile failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`

- [ ] **Step 5: Implement the backend**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static com.bytechef.ee.platform.component.runner.aws.batch.AwsBatchJobFactory.MAIN_CONTAINER;
import static com.bytechef.ee.platform.component.runner.aws.batch.AwsBatchJobFactory.WORK_DIRECTORY;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.runner.TaskRunnerConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.remote.CleanupMode;
import com.bytechef.platform.component.runner.remote.Deadline;
import com.bytechef.platform.component.runner.remote.RemoteJobContext;
import com.bytechef.platform.component.runner.remote.RemoteJobExit;
import com.bytechef.platform.component.runner.remote.RemoteJobHandle;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemoteLogSink;
import com.bytechef.platform.component.runner.remote.RemoteTaskRunnerBackend;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobStatus;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.TaskContainerDetails;

/**
 * Runs a task as an AWS Batch job in the account the task's {@code aws} connection names.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class AwsBatchTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String BUCKET_ATTRIBUTE = "bucket";
    static final String JOB_DEFINITION_ATTRIBUTE = "jobDefinitionArn";
    static final String LOG_GROUP = "/aws/batch/job";
    static final String PREFIX_ATTRIBUTE = "prefix";
    static final String TYPE = "aws-batch";

    private static final Logger log = LoggerFactory.getLogger(AwsBatchTaskRunnerBackend.class);

    private static final Duration CLIENT_GRACE = Duration.ofMinutes(10);
    private static final int MAX_CAUSE_DEPTH = 16;
    private static final int MAX_LOG_PAGES_PER_POLL = 10;
    private static final Set<JobStatus> NOT_STARTED = Set.of(
        JobStatus.SUBMITTED, JobStatus.PENDING, JobStatus.RUNNABLE, JobStatus.STARTING);
    private static final Set<JobStatus> TERMINAL = Set.of(JobStatus.SUCCEEDED, JobStatus.FAILED);

    private final AwsBatchOperationsFactory operationsFactory;
    private final Duration minimumPollInterval;

    AwsBatchTaskRunnerBackend(AwsBatchOperationsFactory operationsFactory, Duration minimumPollInterval) {
        this.operationsFactory = operationsFactory;
        this.minimumPollInterval = minimumPollInterval;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTitle() {
        return "AWS Batch";
    }

    @Override
    public String getRemoteWorkingDirectory() {
        return WORK_DIRECTORY;
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
        return AwsBatchRunnerSettings.properties();
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return Optional.of(new TaskRunnerConnection("aws", 1));
    }

    @Override
    public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        AwsConnectionSettings connectionSettings = connectionSettings(context);

        connectionSettings.validate();
        connectionSettings.validateBudget(context.completionBudget());

        AwsBatchRunnerSettings.of(request.languageId(), request.runnerParameters());
    }

    @Override
    public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
        Parameters runnerParameters = context.runnerParameters();

        String bucket = AwsBatchRunnerSettings.bucket(runnerParameters);

        try (AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), AwsConnectionSettings.urlValidity(context.completionBudget()))) {

            List<JobSummary> candidates = operations
                .listJobs(AwsBatchRunnerSettings.jobQueue(runnerParameters), AwsBatchJobNames.jobName(key))
                .stream()
                .filter(summary -> !TERMINAL.contains(summary.status()))
                .sorted(Comparator.comparing(JobSummary::createdAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .reversed())
                .toList();

            for (JobSummary candidate : candidates) {
                JobDetail jobDetail = operations.describeJob(candidate.jobId());

                if (jobDetail == null || TERMINAL.contains(jobDetail.status())) {
                    continue;
                }

                Map<String, String> tags = jobDetail.tags();

                if (Objects.equals(tags.get(AwsBatchJobNames.TAG_KEY), key.value())) {
                    return Optional.of(
                        handle(jobDetail.jobId(), bucket, tags.get(AwsBatchJobNames.TAG_PREFIX),
                            jobDetail.jobDefinition()));
                }
            }

            return Optional.empty();
        } catch (SdkException exception) {
            if (isInterrupt(exception)) {
                Thread.currentThread()
                    .interrupt();

                throw new IllegalStateException(
                    "Interrupted while the aws-batch runner was looking for a job to resume", exception);
            }

            throw clientFailure("looking for a job to resume", exception);
        }
    }

    @Override
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        AwsConnectionSettings connectionSettings = connectionSettings(context);
        AwsBatchRunnerSettings runnerSettings = AwsBatchRunnerSettings.of(spec.languageId(), spec.runnerParameters());

        Duration urlValidity = AwsConnectionSettings.urlValidity(context.completionBudget());

        RemoteJobKey key = spec.key();

        String prefix = AwsBatchJobNames.newPrefix(key);
        String bucket = runnerSettings.bucket();

        Map<String, String> tags = AwsBatchJobNames.tags(key, prefix);

        Path archivePath = Files.createTempFile("bytechef-aws-batch-input-" + UUID.randomUUID() + "-", ".tar");

        try (AwsBatchOperations operations = operationsFactory.create(connectionSettings, urlValidity)) {
            Files.copy(inputArchive, archivePath, StandardCopyOption.REPLACE_EXISTING);

            boolean staged = false;
            String jobDefinitionArn = null;

            try {
                operations.putObject(bucket, prefix + AwsBatchJobNames.INPUT_OBJECT, archivePath);

                staged = true;

                URL inputUrl = operations.presignGet(bucket, prefix + AwsBatchJobNames.INPUT_OBJECT, urlValidity);
                URL outputUrl = operations.presignPut(bucket, prefix + AwsBatchJobNames.OUTPUT_OBJECT, urlValidity);

                jobDefinitionArn = operations.registerJobDefinition(
                    AwsBatchJobFactory.jobDefinition(
                        spec, runnerSettings, tags, inputUrl, outputUrl, context.completionBudget()));

                String jobId = operations.submitJob(
                    AwsBatchJobFactory.submitJob(key, jobDefinitionArn, runnerSettings.jobQueue(), tags));

                return handle(jobId, bucket, prefix, jobDefinitionArn);
            } catch (SdkException exception) {
                discard(operations, jobDefinitionArn, bucket, staged ? prefix : null);

                throw failure("submitting the job", exception);
            } catch (RuntimeException exception) {
                discard(operations, jobDefinitionArn, bucket, staged ? prefix : null);

                throw exception;
            }
        } finally {
            Files.deleteIfExists(archivePath);
        }
    }

    @Override
    public RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws InterruptedException {

        Duration pollInterval = pollInterval(context.runnerParameters());

        try (AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            String logStreamName = null;
            String nextToken = null;

            while (true) {
                JobDetail jobDetail = operations.describeJob(handle.id());

                if (jobDetail == null) {
                    throw new IllegalStateException("The aws-batch job %s disappeared".formatted(handle.id()));
                }

                TaskContainerDetails main = mainContainer(jobDetail);

                if (logStreamName == null && main != null && main.logStreamName() != null) {
                    logStreamName = main.logStreamName();
                }

                if (logStreamName != null) {
                    nextToken = drainLog(operations, logStreamName, nextToken, logSink);
                }

                JobStatus status = jobDetail.status();

                if (TERMINAL.contains(status)) {
                    return exit(jobDetail, main);
                }

                if (NOT_STARTED.contains(status) && runningDeadline.isExpired()) {
                    throw new IllegalStateException(
                        "The aws-batch job did not start within %s; the job queue may have no capacity: %s"
                            .formatted(runningDeadline.getBudget(), jobDetail.statusReason()));
                }

                if (completionDeadline.isExpired()) {
                    return RemoteJobExit.timedOut();
                }

                Thread.sleep(pollInterval.toMillis());
            }
        } catch (SdkException exception) {
            throw failure("waiting for the job", exception);
        }
    }

    @Override
    public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws InterruptedException {

        AwsBatchOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE));

        try {
            InputStream inputStream = operations.getObject(
                attribute(handle, BUCKET_ATTRIBUTE), attribute(handle, PREFIX_ATTRIBUTE) + AwsBatchJobNames.OUTPUT_OBJECT);

            if (inputStream == null) {
                operations.close();

                return null;
            }

            return new FilterInputStream(inputStream) {

                @Override
                public void close() throws IOException {
                    try {
                        super.close();
                    } finally {
                        operations.close();
                    }
                }
            };
        } catch (SdkException exception) {
            operations.close();

            throw failure("collecting the outputs", exception);
        } catch (RuntimeException exception) {
            operations.close();

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        boolean interrupted = Thread.interrupted();

        try (AwsBatchOperations operations = operationsFactory.create(connectionSettings(context), CLIENT_GRACE)) {
            if (cleanupMode == CleanupMode.KILL_AND_DELETE) {
                quietly("terminate the job", () -> operations.terminateJob(handle.id(), "Stopped by ByteChef"));
            }

            discard(
                operations, handle.attributes()
                    .get(JOB_DEFINITION_ATTRIBUTE),
                handle.attributes()
                    .get(BUCKET_ATTRIBUTE),
                handle.attributes()
                    .get(PREFIX_ATTRIBUTE));
        } catch (RuntimeException exception) {
            log.warn("Could not clean up the aws-batch job {}", handle.id(), exception);
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    private Duration pollInterval(Parameters runnerParameters) {
        Duration interval = AwsBatchRunnerSettings.completionCheckInterval(runnerParameters);

        return interval.compareTo(minimumPollInterval) < 0 ? minimumPollInterval : interval;
    }

    private static String attribute(RemoteJobHandle handle, String name) {
        String value = handle.attributes()
            .get(name);

        if (value == null) {
            throw new IllegalStateException("The aws-batch job handle %s has no %s".formatted(handle.id(), name));
        }

        return value;
    }

    private static IllegalStateException clientFailure(String phase, SdkException exception) {
        if (exception instanceof AwsServiceException awsServiceException &&
            awsServiceException.awsErrorDetails() != null) {

            return new IllegalStateException(
                "The aws-batch runner failed while %s: %s (HTTP %d)".formatted(
                    phase, awsServiceException.awsErrorDetails()
                        .errorCode(),
                    awsServiceException.statusCode()),
                exception);
        }

        return new IllegalStateException(
            "The aws-batch runner failed while %s: no response from AWS".formatted(phase), exception);
    }

    private static AwsConnectionSettings connectionSettings(RemoteJobContext context) {
        ComponentConnection connection = context.connection();

        if (connection == null) {
            throw new IllegalArgumentException(
                "The AWS Batch runner needs a connection; select one in the task's connections");
        }

        return AwsConnectionSettings.of(connection);
    }

    private static void discard(
        AwsBatchOperations operations, @Nullable String jobDefinitionArn, @Nullable String bucket,
        @Nullable String prefix) {

        boolean interrupted = Thread.interrupted();

        try {
            if (jobDefinitionArn != null) {
                quietly("deregister the job definition", () -> operations.deregisterJobDefinition(jobDefinitionArn));
            }

            if (bucket != null && prefix != null) {
                quietly("delete the staging prefix", () -> operations.deletePrefix(bucket, prefix));
            }
        } finally {
            if (interrupted) {
                Thread.currentThread()
                    .interrupt();
            }
        }
    }

    @Nullable
    private static String drainLog(
        AwsBatchOperations operations, String logStreamName, @Nullable String nextToken, RemoteLogSink logSink) {

        String token = nextToken;

        for (int page = 0; page < MAX_LOG_PAGES_PER_POLL; page++) {
            AwsBatchOperations.LogPage logPage = operations.getLogEvents(LOG_GROUP, logStreamName, token);

            for (String message : logPage.messages()) {
                byte[] bytes = (message + "\n").getBytes(StandardCharsets.UTF_8);

                logSink.appendStdout(bytes, bytes.length);
            }

            String next = logPage.nextForwardToken();

            if (logPage.messages()
                .isEmpty() || Objects.equals(next, token)) {

                return next == null ? token : next;
            }

            token = next;
        }

        return token;
    }

    private static RemoteJobExit exit(JobDetail jobDetail, @Nullable TaskContainerDetails main) {
        if (main != null && main.exitCode() != null) {
            String reason = main.reason();

            return new RemoteJobExit(true, main.exitCode(), reason == null || reason.isBlank() ? null : reason);
        }

        String statusReason = jobDetail.statusReason();

        return new RemoteJobExit(true, null, statusReason == null ? jobDetail.statusAsString() : statusReason);
    }

    private static IllegalStateException failure(String phase, SdkException exception)
        throws InterruptedException {

        if (isInterrupt(exception)) {
            Thread.interrupted();

            InterruptedException interruptedException = new InterruptedException(
                "Interrupted while the aws-batch runner was " + phase);

            interruptedException.initCause(exception);

            throw interruptedException;
        }

        return clientFailure(phase, exception);
    }

    private static RemoteJobHandle handle(
        String jobId, String bucket, @Nullable String prefix, @Nullable String jobDefinitionArn) {

        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(BUCKET_ATTRIBUTE, bucket);

        if (prefix != null) {
            attributes.put(PREFIX_ATTRIBUTE, prefix);
        }

        if (jobDefinitionArn != null) {
            attributes.put(JOB_DEFINITION_ATTRIBUTE, jobDefinitionArn);
        }

        return new RemoteJobHandle(jobId, attributes);
    }

    private static boolean isInterrupt(Throwable exception) {
        if (Thread.currentThread()
            .isInterrupted()) {

            return true;
        }

        Throwable cause = exception;

        for (int depth = 0; cause != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (cause instanceof InterruptedException || cause instanceof AbortedException ||
                (cause instanceof InterruptedIOException && !(cause instanceof SocketTimeoutException))) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    @Nullable
    private static TaskContainerDetails mainContainer(JobDetail jobDetail) {
        if (jobDetail.ecsProperties() == null) {
            return null;
        }

        return jobDetail.ecsProperties()
            .taskProperties()
            .stream()
            .flatMap(taskDetails -> taskDetails.containers()
                .stream())
            .filter(container -> MAIN_CONTAINER.equals(container.name()))
            .findFirst()
            .orElse(null);
    }

    private static void quietly(String action, Runnable call) {
        try {
            call.run();
        } catch (RuntimeException exception) {
            log.warn("The aws-batch runner could not {}", action, exception);
        }
    }
}
```

Notes for the implementer:
- `failure(...)` has the Kubernetes backend's shape. It throws `InterruptedException` on an interrupt and otherwise returns the phase-named `IllegalStateException` for the caller to `throw`.
- `collect` declares `throws InterruptedException` (the SPI also declares `IOException`), so the interrupt path compiles.
- `await`'s `catch (SdkException)` sits outside the try-with-resources body. The operations are closed before the failure is wrapped, which is fine.

- [ ] **Step 6: Run tests and `check`**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q`, then `check` in the background.
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-component/platform-component-runner-aws-batch
git commit -m "Add the aws batch task runner backend"
```

---
### Task 5: The AWS SDK adapter, the Spring bean, and the LocalStack staging test (EE)

**Files:**
- Create: `BATCH_PKG/SdkAwsBatchOperations.java`
- Modify: `BATCH_PKG/AwsBatchTaskRunnerBackend.java` (public Spring constructor and bean annotations)
- Modify: `BATCH/build.gradle.kts` (integration-test dependencies)
- Test: `BATCH_TEST/SdkAwsBatchOperationsIntTest.java`

**Interfaces:**
- Consumes: Task 4's `AwsBatchOperations`/`AwsBatchOperationsFactory`, and Task 2's `AwsConnectionSettings.credentialsProvider(Duration)`/`awsRegion()`.
- Produces: the `@Component @ConditionalOnEEVersion` bean `AwsBatchTaskRunnerBackend`, which `TaskRunnerRegistryImpl` wraps under type `aws-batch`.

- [ ] **Step 1: Dependencies**

Add to `BATCH/build.gradle.kts`:

```kotlin
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:testcontainers-localstack")
```

Plan 2 found that the Spring Boot BOM resolves Testcontainers core to the 2.0.5 line, and that the 1.21.3 module artifacts (`org.testcontainers:k3s`, `org.testcontainers:localstack`) break against it. Use the 2.0.5 module `testcontainers-localstack`, exactly as the Kubernetes module uses `testcontainers-k3s`. Check its `LocalStackContainer` package in the resolved jar: it may be `org.testcontainers.localstack.LocalStackContainer` rather than the 1.x `org.testcontainers.containers.localstack.LocalStackContainer`. Use the one that exists.

- [ ] **Step 2: Write the staging integration test**

`SdkAwsBatchOperationsIntTest.java`:

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.bytechef.platform.component.ComponentConnection;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Proves the S3 staging half of the AWS Batch runner against LocalStack. Batch itself is verified by hand, because
 * LocalStack's Batch emulation is not part of its free edition.
 *
 * @author Ivica Cardic
 * @version ee
 */
class SdkAwsBatchOperationsIntTest {

    private static final String BUCKET = "bytechef-staging";

    private static LocalStackContainer localStackContainer;
    private static URI endpoint;

    @BeforeAll
    static void startLocalStack() {
        assumeTrue(
            DockerClientFactory.instance()
                .isDockerAvailable(),
            "no docker daemon, so LocalStack cannot start");

        localStackContainer = new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8"))
            .withServices("s3");

        localStackContainer.start();

        endpoint = localStackContainer.getEndpoint();

        try (S3Client s3Client = S3Client.builder()
            .endpointOverride(endpoint)
            .region(Region.US_EAST_1)
            .forcePathStyle(true)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
            .build()) {

            s3Client.createBucket(builder -> builder.bucket(BUCKET));
        }
    }

    @AfterAll
    static void stopLocalStack() {
        if (localStackContainer != null) {
            localStackContainer.stop();
        }
    }

    @Test
    void testAnUploadedInputIsReadableThroughItsPresignedGet() throws Exception {
        Path file = Files.createTempFile("bytechef-int-", ".tar");

        Files.writeString(file, "input-bytes", StandardCharsets.UTF_8);

        try (SdkAwsBatchOperations operations = operations()) {
            operations.putObject(BUCKET, "bytechef/1/k-1/input.tar", file);

            URL url = operations.presignGet(BUCKET, "bytechef/1/k-1/input.tar", Duration.ofMinutes(10));

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(url.toURI())
                    .GET()
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("input-bytes");
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void testAnOutputPutThroughItsPresignedUrlIsCollected() throws Exception {
        try (SdkAwsBatchOperations operations = operations()) {
            URL url = operations.presignPut(BUCKET, "bytechef/1/k-2/output.tar", Duration.ofMinutes(10));

            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(url.toURI())
                    .PUT(HttpRequest.BodyPublishers.ofString("output-bytes"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            assertThat(response.statusCode()).isEqualTo(200);

            try (InputStream inputStream = operations.getObject(BUCKET, "bytechef/1/k-2/output.tar")) {
                assertThat(inputStream).isNotNull();
                assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("output-bytes");
            }
        }
    }

    @Test
    void testAMissingOutputReadsAsNull() {
        try (SdkAwsBatchOperations operations = operations()) {
            assertThat(operations.getObject(BUCKET, "bytechef/1/missing/output.tar")).isNull();
        }
    }

    @Test
    void testDeletingAPrefixRemovesEveryObjectUnderIt() throws Exception {
        Path file = Files.createTempFile("bytechef-int-", ".tar");

        Files.writeString(file, "x", StandardCharsets.UTF_8);

        try (SdkAwsBatchOperations operations = operations()) {
            operations.putObject(BUCKET, "bytechef/1/k-3/input.tar", file);
            operations.putObject(BUCKET, "bytechef/1/k-3/output.tar", file);
            operations.putObject(BUCKET, "bytechef/1/k-4/input.tar", file);

            operations.deletePrefix(BUCKET, "bytechef/1/k-3/");

            assertThat(operations.getObject(BUCKET, "bytechef/1/k-3/input.tar")).isNull();
            assertThat(operations.getObject(BUCKET, "bytechef/1/k-3/output.tar")).isNull();

            try (InputStream inputStream = operations.getObject(BUCKET, "bytechef/1/k-4/input.tar")) {
                assertThat(inputStream).isNotNull();
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static SdkAwsBatchOperations operations() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("accessKeyId", "test");
        parameters.put("secretAccessKey", "test");
        parameters.put("region", "us-east-1");

        AwsConnectionSettings connectionSettings = AwsConnectionSettings.of(
            new ComponentConnection("aws", 1, 1L, parameters, null));

        return new SdkAwsBatchOperations(connectionSettings, Duration.ofMinutes(15), endpoint);
    }
}
```

(Import the `LocalStackContainer` class from whichever package the 2.0.5 jar provides. If its API has no `withServices(String…)`, use whatever it offers for enabling S3, or nothing if S3 is on by default. If `getEndpoint()` is named differently, use the equivalent.)

- [ ] **Step 3: Implement the adapter**

```java
package com.bytechef.ee.platform.component.runner.aws.batch;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.batch.BatchClient;
import software.amazon.awssdk.services.batch.BatchClientBuilder;
import software.amazon.awssdk.services.batch.model.DescribeJobsResponse;
import software.amazon.awssdk.services.batch.model.JobDetail;
import software.amazon.awssdk.services.batch.model.JobSummary;
import software.amazon.awssdk.services.batch.model.KeyValuesPair;
import software.amazon.awssdk.services.batch.model.ListJobsRequest;
import software.amazon.awssdk.services.batch.model.RegisterJobDefinitionRequest;
import software.amazon.awssdk.services.batch.model.SubmitJobRequest;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * {@link AwsBatchOperations} over the AWS SDK v2.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class SdkAwsBatchOperations implements AwsBatchOperations {

    private static final int DELETE_BATCH_SIZE = 1000;

    private final BatchClient batchClient;
    private final AwsCredentialsProvider credentialsProvider;
    private final SdkHttpClient httpClient;
    private final CloudWatchLogsClient logsClient;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    SdkAwsBatchOperations(AwsConnectionSettings connectionSettings, Duration sessionDuration) {
        this(connectionSettings, sessionDuration, null);
    }

    SdkAwsBatchOperations(
        AwsConnectionSettings connectionSettings, Duration sessionDuration, @Nullable URI endpointOverride) {

        Region region = connectionSettings.awsRegion();

        this.credentialsProvider = connectionSettings.credentialsProvider(sessionDuration);
        this.httpClient = ApacheHttpClient.builder()
            .build();

        BatchClientBuilder batchClientBuilder = BatchClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        CloudWatchLogsClientBuilder logsClientBuilder = CloudWatchLogsClient.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
            .region(region)
            .credentialsProvider(credentialsProvider)
            .httpClient(httpClient);
        S3Presigner.Builder s3PresignerBuilder = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentialsProvider);

        if (endpointOverride != null) {
            batchClientBuilder.endpointOverride(endpointOverride);
            logsClientBuilder.endpointOverride(endpointOverride);
            s3ClientBuilder.endpointOverride(endpointOverride)
                .forcePathStyle(true);
            s3PresignerBuilder.endpointOverride(endpointOverride)
                .serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }

        this.batchClient = batchClientBuilder.build();
        this.logsClient = logsClientBuilder.build();
        this.s3Client = s3ClientBuilder.build();
        this.s3Presigner = s3PresignerBuilder.build();
    }

    @Override
    public void putObject(String bucket, String key, Path file) {
        s3Client.putObject(builder -> builder.bucket(bucket)
            .key(key), RequestBody.fromFile(file));
    }

    @Override
    public URL presignGet(String bucket, String key, Duration validity) {
        return s3Presigner.presignGetObject(builder -> builder.signatureDuration(validity)
            .getObjectRequest(request -> request.bucket(bucket)
                .key(key)))
            .url();
    }

    @Override
    public URL presignPut(String bucket, String key, Duration validity) {
        return s3Presigner.presignPutObject(builder -> builder.signatureDuration(validity)
            .putObjectRequest(request -> request.bucket(bucket)
                .key(key)))
            .url();
    }

    @Override
    public @Nullable InputStream getObject(String bucket, String key) {
        try {
            return s3Client.getObject(builder -> builder.bucket(bucket)
                .key(key));
        } catch (NoSuchKeyException exception) {
            return null;
        }
    }

    @Override
    public void deletePrefix(String bucket, String prefix) {
        List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();

        for (S3Object s3Object : s3Client.listObjectsV2Paginator(builder -> builder.bucket(bucket)
            .prefix(prefix))
            .contents()) {

            objectIdentifiers.add(ObjectIdentifier.builder()
                .key(s3Object.key())
                .build());

            if (objectIdentifiers.size() == DELETE_BATCH_SIZE) {
                deleteObjects(bucket, objectIdentifiers);

                objectIdentifiers.clear();
            }
        }

        if (!objectIdentifiers.isEmpty()) {
            deleteObjects(bucket, objectIdentifiers);
        }
    }

    @Override
    public String registerJobDefinition(RegisterJobDefinitionRequest request) {
        return batchClient.registerJobDefinition(request)
            .jobDefinitionArn();
    }

    @Override
    public void deregisterJobDefinition(String jobDefinitionArn) {
        batchClient.deregisterJobDefinition(builder -> builder.jobDefinition(jobDefinitionArn));
    }

    @Override
    public String submitJob(SubmitJobRequest request) {
        return batchClient.submitJob(request)
            .jobId();
    }

    @Override
    public @Nullable JobDetail describeJob(String jobId) {
        DescribeJobsResponse describeJobsResponse = batchClient.describeJobs(builder -> builder.jobs(jobId));

        List<JobDetail> jobs = describeJobsResponse.jobs();

        return jobs.isEmpty() ? null : jobs.getFirst();
    }

    @Override
    public List<JobSummary> listJobs(String jobQueue, String jobName) {
        ListJobsRequest listJobsRequest = ListJobsRequest.builder()
            .jobQueue(jobQueue)
            .filters(KeyValuesPair.builder()
                .name("JOB_NAME")
                .values(jobName)
                .build())
            .build();

        List<JobSummary> jobSummaries = new ArrayList<>();

        batchClient.listJobsPaginator(listJobsRequest)
            .jobSummaryList()
            .forEach(jobSummaries::add);

        return jobSummaries;
    }

    @Override
    public void terminateJob(String jobId, String reason) {
        batchClient.terminateJob(builder -> builder.jobId(jobId)
            .reason(reason));
    }

    @Override
    public LogPage getLogEvents(String logGroupName, String logStreamName, @Nullable String nextToken) {
        try {
            GetLogEventsResponse getLogEventsResponse = logsClient.getLogEvents(builder -> builder
                .logGroupName(logGroupName)
                .logStreamName(logStreamName)
                .nextToken(nextToken)
                .startFromHead(true));

            return new LogPage(
                getLogEventsResponse.events()
                    .stream()
                    .map(OutputLogEvent::message)
                    .toList(),
                getLogEventsResponse.nextForwardToken());
        } catch (ResourceNotFoundException exception) {
            return new LogPage(List.of(), nextToken);
        }
    }

    @Override
    public void close() {
        s3Presigner.close();
        s3Client.close();
        logsClient.close();
        batchClient.close();
        httpClient.close();

        if (credentialsProvider instanceof SdkAutoCloseable sdkAutoCloseable) {
            sdkAutoCloseable.close();
        }
    }

    private void deleteObjects(String bucket, List<ObjectIdentifier> objectIdentifiers) {
        s3Client.deleteObjects(builder -> builder.bucket(bucket)
            .delete(delete -> delete.objects(objectIdentifiers)));
    }
}
```

AWS SDK request builders and consumer-builder lambdas are sanctioned fluent APIs.
- **Error wrapping:** none of these methods wraps SDK exceptions. The backend does, interrupt-aware (Task 4).
- **Version differences:** if a 2.54 method name differs, use that version's equivalent and note it in the report. Examples: `S3ClientBuilder.forcePathStyle`, `listJobsPaginator(…).jobSummaryList()`.

- [ ] **Step 4: Make the backend a Spring bean**

In `AwsBatchTaskRunnerBackend`:

```java
@Component
@ConditionalOnEEVersion
public class AwsBatchTaskRunnerBackend implements RemoteTaskRunnerBackend {

    …

    @Autowired
    public AwsBatchTaskRunnerBackend() {
        this(SdkAwsBatchOperations::new, Duration.ofSeconds(1));
    }

    AwsBatchTaskRunnerBackend(AwsBatchOperationsFactory operationsFactory, Duration minimumPollInterval) {
        …
    }
```

(Imports: `ConditionalOnEEVersion`, `org.springframework.beans.factory.annotation.Autowired`, `org.springframework.stereotype.Component`.)

- [ ] **Step 5: Run the unit tests, then the integration test**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test -q` → PASS.

Then in the background:

```bash
nohup ./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:testIntegration > "$SCRATCH/batch-int.log" 2>&1 &
```

Poll the log. Expected: 4 tests pass. If Docker is unavailable, the tests are skipped by the assumption; report that, not a pass. Then run the module's `check` in the background.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/platform/platform-component/platform-component-runner-aws-batch
git commit -m "Talk to aws through the sdk and prove the s3 staging on localstack"
```

---

### Task 6: Wire the runner into the apps, switch it off by default, document it

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts` and `server/ee/apps/worker-app/build.gradle.kts`: add `implementation(project(":server:ee:libs:platform:platform-component:platform-component-runner-aws-batch"))` in alphabetical order next to the Kubernetes runner line.
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`
- Modify: `server/libs/modules/components/commands/src/main/resources/README.mdx`, `server/libs/modules/components/script/src/main/resources/README.mdx`, `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md` (§7)

**Interfaces:**
- Consumes: the bean from Task 5.
- Produces: an installation where `bytechef.script.runners.aws-batch.enabled=true` (EE) offers the AWS Batch runner in every Script and Commands task.

- [ ] **Step 1: Configuration**

In `application-bytechef.yml`, under `script: runners:`, add before `graalvm:` (alphabetical order):

```yaml
      aws-batch:
        # Enterprise. Runs script and commands tasks as AWS Batch jobs in the account the task's aws connection names.
        enabled: false
```

- [ ] **Step 2: Boot smoke**

With the infra running, start the server in the background:

```bash
nohup ./gradlew -p server/apps/server-app bootRun --args='--bytechef.script.runners.aws-batch.enabled=true' > "$SCRATCH/boot.log" 2>&1 &
```

Wait for `Started ServerApplication` or a failure, then confirm:
- the registry warning `Task runner 'aws-batch' is enabled; workflows may execute code outside the script sandbox`;
- no bean errors;
- no `Multiple HTTP implementations were found on the classpath` error from the AWS SDK. The adapter sets the Apache client explicitly, but report it if it appears.

Stop only the process you started.

- [ ] **Step 3: Docs**

`commands/README.mdx`:
- In `## Task runners`, add the table row `| AWS Batch | `aws-batch` | Enterprise. Runs the commands as an AWS Batch job in the account the task's **AWS** connection names. |` and update the runner count and the "all are **off**" sentence.
- In `### Operator configuration`, add `aws-batch: enabled: false` to the yml, and a bullet: "`aws-batch.enabled`: any workflow author with an AWS connection can run any image on that account's Batch queues, as any job and task role the connection's identity may pass. Give the connection's identity a policy scoped to the queues, bucket and roles below."
- Add a section `### The AWS Batch runner (Enterprise)` after the Kubernetes runner's, covering:
  1. **What it needs.**
     - An `aws` connection: access key ID, secret access key, region, and optionally a role ARN and external ID. With a role, its maximum session duration must cover the completion budget plus 10 minutes, at most 12 hours.
     - An **ECS-backed** job queue (Fargate or EC2; EKS queues are not supported).
     - A staging bucket.
     - On Fargate, an execution role. If the subnets have no NAT gateway, use public subnets with **Assign Public IP**, so the task can pull images and reach S3.
     - The task's image must provide `/bin/sh`.
  2. **Its properties.** A table with `image`, `entrypoint`, `jobQueue`, `bucket`, `platform`, `executionRoleArn`, `taskRoleArn`, `vcpu`, `memory`, `assignPublicIp`, `completionCheckInterval` and `fileSidecarImage` (default `curlimages/curl:8.11.1`; needs `sh`, `curl`, `tar`), plus the shared `pullPolicy` (ignored by Batch), `resume`, `delete`, `waitUntilRunning` and `waitUntilCompletion`.
  3. **Sizing.**
     - `vcpu` and `memory` are the task's totals. The two file sidecars take 0.25 vCPU / 64 MiB each, and the task's own container gets the rest.
     - On Fargate the total must be a Fargate combination of at least 1 vCPU: a table listing 1, 2, 4, 8 and 16 vCPU with their memory ranges and steps.
     - EC2 queues are not checked beyond the sidecar minimum.
  4. **How a run works.**
     - Inputs go to `s3://<bucket>/bytechef/<job>/<name>-<random>/input.tar`. A job definition is registered for the run, with three containers sharing an ephemeral volume:
       - the file sidecar downloads and unpacks the inputs through a presigned GET;
       - `main` runs the task with stderr redirected to `.stderr`;
       - the essential output sidecar tars `output/` and `.stderr` and uploads them through a presigned PUT, then the job ends.
     - The presigned URLs are valid for the completion budget plus 10 minutes. They sit in the job definition's environment, readable by anyone allowed `batch:DescribeJobDefinitions`, and the definition is deregistered after the run.
     - The job's attempt timeout is the completion budget plus one minute.
     - The result's exit code is `main`'s, even when the job itself reports `SUCCEEDED`.
     - A job stuck in `RUNNABLE` fails after `waitUntilRunning` with "the job queue may have no capacity".
     - Logs are tailed from CloudWatch `/aws/batch/job`.
  5. **Resume.**
     - Jobs are named `bytechef-<key>` (`k-` plus a SHA-256 prefix when the key does not fit) and tagged `bytechef.io/key`.
     - A redelivered task re-attaches to the newest job with that key that has not finished.
     - The same durable-broker caveat as the other runners applies, and two installations must not share one queue with resume on.
     - Clean up leftovers by terminating jobs named `bytechef-*`, deregistering `bytechef-*` job definitions, and deleting `s3://<bucket>/bytechef/`.
  6. **IAM for the connection's identity:**

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "batch:RegisterJobDefinition", "batch:DeregisterJobDefinition", "batch:SubmitJob", "batch:DescribeJobs",
        "batch:ListJobs", "batch:TerminateJob", "batch:TagResource"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::<bucket>/bytechef/*"
    },
    {
      "Effect": "Allow",
      "Action": "s3:ListBucket",
      "Resource": "arn:aws:s3:::<bucket>",
      "Condition": {"StringLike": {"s3:prefix": "bytechef/*"}}
    },
    {
      "Effect": "Allow",
      "Action": "logs:GetLogEvents",
      "Resource": "arn:aws:logs:*:*:log-group:/aws/batch/job:*"
    },
    {
      "Effect": "Allow",
      "Action": "iam:PassRole",
      "Resource": ["<execution role ARN>", "<task role ARN>"]
    }
  ]
}
```

  7. **Verifying it.** The automated tests cover the job definition and the S3 staging (against LocalStack), but not Batch itself. Give a short manual checklist:
     - run a Python script and a shell command with an output file on a Fargate queue;
     - confirm the job definition and the `bytechef/` prefix are gone afterwards;
     - confirm a stuck queue fails after `waitUntilRunning`.

`script/README.mdx`, `### Task Runners` table: add `| AWS Batch | `aws-batch` | Enterprise. Runs the script as an AWS Batch job in the account the task's **AWS** connection names. Enable with `bytechef.script.runners.aws-batch.enabled=true`; the **Commands** component's README documents its properties, sizing, IAM and resume behaviour. |`. Also add AWS Batch to the "Result Shape" sentence that names the external runners.

Spec §7: update it to match the code:
- the job and definition names, with hashing;
- the per-run random prefix suffix and the `bytechef.io/prefix` tag;
- Fargate needs at least 1 vCPU because of the sidecars;
- `assignPublicIp`;
- `completionCheckInterval`;
- the attempt timeout;
- no private-registry credentials in v1;
- `find` verifies the `bytechef.io/key` tag;
- the interrupt-aware error wrapping;
- the presigned URLs live in the definition's environment;
- the adapter uses the Apache HTTP client explicitly.

- [ ] **Step 4: Checks**

Run in the background, then poll: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:check :server:ee:libs:modules:components:aws:check :server:libs:modules:components:commands:check :server:libs:modules:components:script:check -q`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add server/apps/server-app/build.gradle.kts server/ee/apps/worker-app/build.gradle.kts server/apps/server-app/src/main/resources/config/application-bytechef.yml server/libs/modules/components/commands/src/main/resources/README.mdx server/libs/modules/components/script/src/main/resources/README.mdx docs/superpowers/specs/2026-10-14-remote-task-runners-design.md
git commit -m "Enable the aws batch runner in the server and worker apps and document it"
```
