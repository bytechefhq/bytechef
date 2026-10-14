# Remote Task Runners — Plan 2: Kubernetes EE Runner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the Enterprise Kubernetes task runner. Script and Commands tasks can run as a pod on a customer's cluster, reached through a `kubernetes` connection. It has the same result shape, `resume`/`delete` semantics and deadlines as the Docker runner.

**Architecture:** A new EE module provides a `RemoteTaskRunnerBackend` bean, `KubernetesTaskRunnerBackend`. The CE `TaskRunnerRegistryImpl` wraps it in the existing `RemoteTaskRunner` orchestrator.
- **Seam:** the backend talks to the cluster only through a small `KubernetesOperations` interface. A fake drives the unit tests, and `Fabric8KubernetesOperations` is the production adapter, proven by a k3s integration test. This replaces spec §9's `KubernetesMockServer`, which cannot script exec and pod-status transitions well.
- **Pod:**
  - a busybox init container that receives the input tar over `pods/exec`;
  - the author's image as `main`, with stderr redirected to `.stderr`;
  - a busybox sidecar that hands back `output/` and `.stderr` before the pod completes.
- **Connection:** a thin EE `kubernetes` component owns the connection definition. It has no actions.

**Tech Stack:** Java 25, Spring Boot 4, fabric8 `kubernetes-client` (its version is managed by the Spring Cloud 2025.1 BOM already imported in `server/build.gradle.kts`), JUnit 5, AssertJ, Mockito, Testcontainers `k3s` 1.21.3.

**Spec:** `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md`, §6 (Kubernetes), with §3 (backend contract) and §8 (errors). Builds on Plan 1, `docs/superpowers/plans/2026-10-14-remote-task-runners-core.md`, which is already on branch `0_p_script_runners`.

## Global Constraints

- Branch `0_p_script_runners` is local and unpushed. Never push, never `git stash`, never amend existing commits. Run `git rev-parse --abbrev-ref HEAD` before every commit.
- Commit messages are **subject only** (`git commit -m "<subject>"`): no body, no `Co-Authored-By`, no `Generated with` line.
- Every file under `server/ee/` gets the ByteChef Enterprise license header, copied verbatim from `server/ee/libs/modules/components/request/src/main/java/com/bytechef/ee/component/request/RequestComponentHandler.java`. Its class Javadoc carries `@version ee` (and `@author Ivica Cardic`, as the neighbours do).
- Every Spring bean under `server/ee/` is annotated `@ConditionalOnEEVersion` (`com.bytechef.platform.annotation.ConditionalOnEEVersion`).
- Runner type ID: `kubernetes`. Operator switch: `bytechef.script.runners.kubernetes.enabled` (default `false`, set in `application-bytechef.yml`).
- Remote working directory: `/bytechef/work`. Fixed layout under it: `input.json`, `output/`, `.stderr`, plus the markers `.ready` and `.done`.
- Pod labels: `bytechef.io/managed-by=bytechef`, `bytechef.io/task-execution-id=<label value>`, `bytechef.io/job-id=<jobId>`. Annotations: `bytechef.io/key=<full key>`, `bytechef.io/kill-grace-period=<ISO-8601>` (only when non-zero), `bytechef.io/pull-secret=<secret name>` (only when one was created).
- Kubernetes label values are at most 63 characters of `[A-Za-z0-9._-]`, starting and ending alphanumeric. A key that does not fit becomes `k-` + the first 16 hex characters of its SHA-256. `find` verifies the full key against the `bytechef.io/key` annotation.
- Cleanup follows the Docker ruling. `DELETE` deletes the pod at once, because the job has already finished. `KILL_AND_DELETE` deletes it with `killGracePeriod` as the grace period. `DETACH` does nothing.
- Base64 goes through `com.bytechef.commons.util.EncodingUtils`, never `java.util.Base64`. Hashing uses SHA-256 (`java.security.MessageDigest`).
- Connection tokens, client keys and registry passwords never appear in exception messages or logs. Wrap fabric8 `KubernetesClientException` as `IllegalStateException("The kubernetes runner failed while <phase>: HTTP <code> <reason>")`, keeping it as the cause.
- No `_` in test method names; unit tests end in `Test`, integration tests in `IntTest`. No prose comments in code; short Javadoc is fine. `TODO:` is forbidden.
- Code style per `CLAUDE.md`: a blank line before control statements and after a variable assignment that is then used; descriptive names, including lambda parameters; chaining only where idiomatic.
- Run `./gradlew <module>:spotlessApply` on every touched module before committing, and revert any reformatting of files the task does not touch.
- Long Gradle runs (integration tests, `check`, `bootRun`) go in the background with output to a log under your scratch directory (`$SCRATCH` below). Poll the log; no single tool call may exceed 10 minutes.

## Review Focus

1. **A key that does not fit a label value** (a long tenant ID, or a `.` or `_` at an end) must still resume the right pod and never another one. Pinned in Task 4, `KubernetesJobLabelsTest`, and in Task 5, `testFindIgnoresAPodWhoseKeyAnnotationDiffers`.
2. **A wrong image name** must fail within seconds with the pull reason, not after `waitUntilRunning` (10 minutes by default). Pinned in Task 5, `testAnImagePullFailureFailsFast`.
3. **An `OOMKilled` or evicted job** must fail with that reason in the message. Pinned in Task 5, `testAnOomKilledContainerReportsItsReason` and `testAnEvictedPodIsAFailureNamingTheEviction`.
4. **A redelivered task whose pod already handed back its outputs** (the output sidecar has terminated) must be deleted and resubmitted, not awaited forever. Pinned in Task 5, `testFindDeletesAPodWhoseOutputSidecarAlreadyExited`.
5. **A CA certificate pasted as PEM rather than base64** must work. Pinned in Task 3, `testAPemCertificateIsBase64EncodedForTheClient`.

## File Structure

| Path | Responsibility |
|---|---|
| `R_API/…/runner/remote/RemoteJobContext.java` | Gains `Parameters runnerParameters` and `long maxTransferBytes` (Task 1) |
| `R_IMPL/…/runner/external/RemoteTaskRunner.java` | Fills the new context fields; streams `.stderr` into the sink instead of reading it whole (Task 1) |
| `server/ee/libs/modules/components/kubernetes/…/KubernetesComponentHandler.java`, `connection/KubernetesConnection.java`, `constant/KubernetesConstants.java`, `assets/kubernetes.svg`, `definition/kubernetes_v1.json` | Thin connection component (Task 2) |
| `server/ee/libs/platform/platform-component/platform-component-runner-kubernetes/` | New EE module, package `com.bytechef.ee.platform.component.runner.kubernetes` |
| `…/KubernetesConnectionSettings.java` | Parses and validates the `kubernetes` connection; builds the fabric8 `Config` (Task 3) |
| `…/KubernetesJobLabels.java` | Key → label value, label and annotation names (Task 4) |
| `…/KubernetesRunnerSettings.java` | Parses and validates the runner's task properties (Task 4) |
| `…/KubernetesPodFactory.java` | Builds the pod and the optional pull secret (Task 4) |
| `…/KubernetesOperations.java` | Seam interface over the cluster (Task 5) |
| `…/KubernetesTaskRunnerBackend.java` | The `RemoteTaskRunnerBackend` (Task 5) |
| `…/Fabric8KubernetesOperations.java` | fabric8 adapter (Task 6) |
| `server/apps/server-app/build.gradle.kts`, `server/ee/apps/worker-app/build.gradle.kts`, `application-bytechef.yml`, `settings.gradle.kts`, `server/build.gradle.kts` | Wiring (Tasks 2, 3, 6, 7) |
| `commands/README.mdx`, `script/README.mdx` | Docs (Task 7) |

Path shorthands: `R_API` = `server/libs/platform/platform-component/platform-component-runner/platform-component-runner-api`, `R_IMPL` = the same with `-impl`, `K8S` = `server/ee/libs/platform/platform-component/platform-component-runner-kubernetes`, `K8S_PKG` = `K8S/src/main/java/com/bytechef/ee/platform/component/runner/kubernetes`, `K8S_TEST` = `K8S/src/test/java/com/bytechef/ee/platform/component/runner/kubernetes`.

Gradle shorthands: `:R_API`, `:R_IMPL` as in Plan 1. `:K8S` = `:server:ee:libs:platform:platform-component:platform-component-runner-kubernetes`. `:K8S_COMPONENT` = `:server:ee:libs:modules:components:kubernetes`.

## Commit map (all new commits on top of `3dd70f2e100`)

| Task | Commit subject |
|---|---|
| 1 | `Give remote backends the runner parameters and the transfer ceiling` |
| 2 | `Add the kubernetes connection component` |
| 3 | `Add the kubernetes runner module and its connection settings` |
| 4 | `Build the kubernetes runner's pod` |
| 5 | `Add the kubernetes task runner backend` |
| 6 | `Talk to the cluster through fabric8 and prove the kubernetes runner on k3s` |
| 7 | `Enable the kubernetes runner in the server and worker apps and document it` |

---
### Task 1: Give backends the runner parameters and the transfer ceiling; stream `.stderr` (CE)

`find` and `cleanup` receive only a key/handle plus the context, but the Kubernetes backend needs the task's `namespace` property in `find`, the transfer ceiling for its `emptyDir` size limit, and the effective completion budget for the pod's `activeDeadlineSeconds` (the safety net that stops a pod whose ByteChef worker died). Plan 1's final review also deferred "`.stderr` is read whole into memory". Both are small CE changes.

**Files:**
- Modify: `R_API/src/main/java/com/bytechef/platform/component/runner/remote/RemoteJobContext.java`
- Modify: `R_IMPL/src/main/java/com/bytechef/platform/component/runner/external/RemoteTaskRunner.java` (`context(...)` around line 331, `collectOutput(...)` around line 304)
- Modify: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/DockerTaskRunnerBackendTest.java` (helper `context(...)` around line 928)
- Test: `R_IMPL/src/test/java/com/bytechef/platform/component/runner/external/RemoteTaskRunnerTest.java`

**Interfaces:**
- Produces: `record RemoteJobContext(@Nullable ComponentConnection connection, Map<String, String> operatorProperties, @Nullable String taskHost, Parameters runnerParameters, long maxTransferBytes, Duration completionBudget)`. `completionBudget` is the effective `waitUntilCompletion` after the task timeout and the operator's `max-wait` (`RemoteTaskRunnerSettings.waitUntilCompletion()`). Tasks 3–6 read all three new fields.

- [ ] **Step 1: Write the failing tests** in `RemoteTaskRunnerTest`

Make `FakeBackend` record the context it is given: add a field `private @Nullable RemoteJobContext lastContext;` and set `lastContext = context;` at the top of `find`, `submit`, `await`, `collect` and `cleanup`. Then add:

```java
    @Test
    void testTheContextCarriesTheRunnerParametersAndTheTransferCeiling() {
        FakeBackend backend = new FakeBackend();

        RemoteTaskRunner runner = new RemoteTaskRunner(
            backend, applicationProperties(Map.of("max-transfer-bytes", "3MB")), () -> "k");

        runner.run(request(Map.of("namespace", "jobs"), 7L));

        assertThat(backend.lastContext.runnerParameters()
            .getString("namespace")).isEqualTo("jobs");
        assertThat(backend.lastContext.maxTransferBytes()).isEqualTo(3L * 1024 * 1024);
    }

    @Test
    void testTheContextCarriesTheEffectiveCompletionBudget() {
        FakeBackend backend = new FakeBackend();

        RemoteTaskRunner runner = new RemoteTaskRunner(
            backend, applicationProperties(Map.of("max-wait", "PT20M")), () -> "k");

        runner.run(request(Map.of("waitUntilCompletion", "PT2H"), 7L));

        assertThat(backend.lastContext.completionBudget()).isEqualTo(Duration.ofMinutes(20));
    }

    @Test
    void testALargeStderrFileReachesTheSinkWithoutBreakingTheTail() {
        FakeBackend backend = new FakeBackend();

        backend.exit = RemoteJobExit.of(1);
        backend.stderrFile = "x".repeat(300_000) + "END-OF-TRACEBACK";

        RemoteTaskRunner runner = runner(backend);

        assertThatThrownBy(() -> runner.run(request(Map.of(), 7L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("END-OF-TRACEBACK");
    }
```

- [ ] **Step 2: Run them, expect failure**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test --tests '*RemoteTaskRunnerTest' -q`
Expected: compile failure (`runnerParameters()` / `maxTransferBytes()` / `completionBudget()` are undefined). The second test passes once the first compiles; it guards the streaming rewrite in Step 4.

- [ ] **Step 3: Extend `RemoteJobContext`**

```java
public record RemoteJobContext(
    @Nullable ComponentConnection connection, Map<String, String> operatorProperties, @Nullable String taskHost,
    Parameters runnerParameters, long maxTransferBytes, Duration completionBudget) {

    public RemoteJobContext {
        operatorProperties = Map.copyOf(operatorProperties);

        if (maxTransferBytes <= 0) {
            throw new IllegalArgumentException("maxTransferBytes must be greater than zero");
        }

        if (completionBudget.isZero() || completionBudget.isNegative()) {
            throw new IllegalArgumentException("completionBudget must be greater than zero");
        }
    }

    public boolean isOperatorFlagEnabled(String propertyName) {
        return Boolean.parseBoolean(operatorProperties.get(propertyName));
    }
}
```

Import `com.bytechef.component.definition.Parameters` and `java.time.Duration`. Update the class Javadoc to say the context also carries the task's runner parameters (for calls that only receive a key or handle), the operator's transfer ceiling, and the effective completion budget.

- [ ] **Step 4: Fill the context and stream `.stderr` in `RemoteTaskRunner`**

In `context(TaskRunnerRequest request)`:

```java
        TaskRunnerLimits limits = limits();

        return new RemoteJobContext(
            connection, operatorProperties(), taskHost == null || taskHost.isBlank() ? null : taskHost.trim(),
            request.runnerParameters(), limits.getMaxTransferBytes(),
            RemoteTaskRunnerSettings.of(request, limits, getType())
                .waitUntilCompletion());
```

In `collectOutput(...)`, replace the `Files.readAllBytes` block with a buffered copy into the sink. `BoundedOutputCapture` keeps only head and tail, so memory stays bounded whatever the file size:

```java
        if (Files.isRegularFile(stderrPath, LinkOption.NOFOLLOW_LINKS)) {
            try (InputStream inputStream = Files.newInputStream(stderrPath, LinkOption.NOFOLLOW_LINKS)) {
                byte[] buffer = new byte[STDERR_BUFFER_SIZE];

                int read = inputStream.read(buffer);

                while (read != -1) {
                    logSink.appendStderr(buffer, read);

                    read = inputStream.read(buffer);
                }
            }
        }
```

Add `private static final int STDERR_BUFFER_SIZE = 8 * 1024;`.

In `DockerTaskRunnerBackendTest`, the helper becomes:

```java
    private static RemoteJobContext context(Map<String, String> operatorProperties, @Nullable String taskHost) {
        return new RemoteJobContext(
            null, operatorProperties, taskHost, ParametersFactory.create(Map.of()), 512L * 1024 * 1024,
            Duration.ofHours(1));
    }
```

Check for any other construction with `grep -rn "new RemoteJobContext(" server --include='*.java'`.

- [ ] **Step 5: Run tests**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api:spotlessApply :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:spotlessApply :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api:test :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test -q`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add server/libs/platform/platform-component/platform-component-runner
git commit -m "Give remote backends the runner parameters and the transfer ceiling"
```

---

### Task 2: The `kubernetes` connection component (EE)

**Files:**
- Create: `server/ee/libs/modules/components/kubernetes/build.gradle.kts`
- Create: `server/ee/libs/modules/components/kubernetes/src/main/java/com/bytechef/ee/component/kubernetes/KubernetesComponentHandler.java`
- Create: `…/kubernetes/connection/KubernetesConnection.java`
- Create: `…/kubernetes/constant/KubernetesConstants.java`
- Create: `server/ee/libs/modules/components/kubernetes/src/main/resources/assets/kubernetes.svg`
- Create: `server/ee/libs/modules/components/kubernetes/src/test/java/com/bytechef/ee/component/kubernetes/KubernetesComponentHandlerTest.java`
- Generated: `server/ee/libs/modules/components/kubernetes/src/test/resources/definition/kubernetes_v1.json`
- Modify: `settings.gradle.kts`, adding `include("server:ee:libs:modules:components:kubernetes")` in alphabetical order among the other `server:ee:libs:modules:components:*` lines (around line 720)

**Interfaces:**
- Produces: component `kubernetes`, version 1. Connection property names, as public constants on `KubernetesConstants`: `MASTER_URL = "masterUrl"`, `CA_CERT_DATA = "caCertData"`, `TOKEN = "token"`, `CLIENT_CERT_DATA = "clientCertData"`, `CLIENT_KEY_DATA = "clientKeyData"`, `NAMESPACE = "namespace"`, `INSECURE_SKIP_TLS_VERIFY = "insecureSkipTlsVerify"`. Task 3 parses these exact names and pins them with a test.

- [ ] **Step 1: Build file**

```kotlin
version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))
}
```

(The parent `server/ee/libs/modules/components/build.gradle.kts` already adds the component API, auto-service and the test dependencies.)

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.ee.component.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Property;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import java.util.List;
import org.junit.jupiter.api.Test;

public class KubernetesComponentHandlerTest {

    private final ComponentDefinition componentDefinition = new KubernetesComponentHandler().getDefinition();

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/kubernetes_v1.json", componentDefinition);
    }

    @Test
    public void testTheConnectionOffersOneCustomAuthorizationWithEveryClusterField() {
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
            "masterUrl", "caCertData", "token", "clientCertData", "clientKeyData", "namespace",
            "insecureSkipTlsVerify");
    }
}
```

If `ConnectionDefinition.getAuthorizations()` or `Authorization.getProperties()` have different return shapes in `sdks/backend/java/component-api`, adapt the navigation and keep the assertions.

- [ ] **Step 3: Run, expect failure**

Run: `./gradlew :server:ee:libs:modules:components:kubernetes:test -q`
Expected: compile failure, because `KubernetesComponentHandler` is missing.

- [ ] **Step 4: Implement**

`KubernetesConstants.java`:

```java
package com.bytechef.ee.component.kubernetes.constant;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class KubernetesConstants {

    public static final String CA_CERT_DATA = "caCertData";
    public static final String CLIENT_CERT_DATA = "clientCertData";
    public static final String CLIENT_KEY_DATA = "clientKeyData";
    public static final String INSECURE_SKIP_TLS_VERIFY = "insecureSkipTlsVerify";
    public static final String MASTER_URL = "masterUrl";
    public static final String NAMESPACE = "namespace";
    public static final String TOKEN = "token";

    private KubernetesConstants() {
    }
}
```

`KubernetesConnection.java`:

```java
package com.bytechef.ee.component.kubernetes.connection;

import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CA_CERT_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CLIENT_CERT_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.CLIENT_KEY_DATA;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.INSECURE_SKIP_TLS_VERIFY;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.MASTER_URL;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.NAMESPACE;
import static com.bytechef.ee.component.kubernetes.constant.KubernetesConstants.TOKEN;

import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 * @version ee
 */
public final class KubernetesConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(Authorization.AuthorizationType.CUSTOM)
                .title("Cluster Credentials")
                .properties(
                    string(MASTER_URL)
                        .label("API Server URL")
                        .description("The cluster's API server, for example https://my-cluster.example.com:6443.")
                        .required(true),
                    string(CA_CERT_DATA)
                        .label("CA Certificate")
                        .description(
                            "The certificate authority that signed the API server's certificate, as PEM or base64 " +
                                "PEM. Leave blank when the API server uses a publicly trusted certificate.")
                        .controlType(ControlType.TEXT_AREA)
                        .required(false),
                    string(TOKEN)
                        .label("Bearer Token")
                        .description(
                            "A service account token. Give either a token or a client certificate and key.")
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(CLIENT_CERT_DATA)
                        .label("Client Certificate")
                        .description("The client certificate, as PEM or base64 PEM.")
                        .controlType(ControlType.TEXT_AREA)
                        .required(false),
                    string(CLIENT_KEY_DATA)
                        .label("Client Key")
                        .description("The client certificate's private key, as PEM or base64 PEM.")
                        .controlType(ControlType.PASSWORD)
                        .required(false),
                    string(NAMESPACE)
                        .label("Namespace")
                        .description("The namespace jobs run in unless a task names another.")
                        .defaultValue("default")
                        .required(false),
                    bool(INSECURE_SKIP_TLS_VERIFY)
                        .label("Skip TLS Verification")
                        .description(
                            "Trusts any API server certificate. Use it only to reach a test cluster.")
                        .defaultValue(false)
                        .required(false)));

    private KubernetesConnection() {
    }
}
```

If `ControlType.TEXT_AREA` does not exist under that name, use the multi-line text control the enum offers. If `authorization(...).title(...)` is not available, drop that line.

`KubernetesComponentHandler.java`:

```java
package com.bytechef.ee.component.kubernetes;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.kubernetes.connection.KubernetesConnection;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * The connection a Kubernetes task runner uses to reach a cluster. It has no actions of its own.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component("kubernetes_v1_ComponentHandler")
@ConditionalOnEEVersion
public class KubernetesComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("kubernetes")
        .title("Kubernetes")
        .description("Runs script and commands tasks as pods on a Kubernetes cluster.")
        .icon("path:assets/kubernetes.svg")
        .categories(ComponentCategory.DEVELOPER_TOOLS)
        .connection(KubernetesConnection.CONNECTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
```

`kubernetes.svg`: a simple 24×24 icon, the Kubernetes helm wheel. Use the official logo mark (`https://github.com/kubernetes/kubernetes/blob/master/logo/logo.svg`, Apache-2.0) if it can be fetched; otherwise draw a seven-spoke wheel in `#326CE5`. It must be a valid standalone SVG with a `viewBox`.

- [ ] **Step 5: Generate the snapshot and run**

```bash
rm -f server/ee/libs/modules/components/kubernetes/src/test/resources/definition/kubernetes_v1.json
rm -rf server/ee/libs/modules/components/kubernetes/build/resources/test/definition
./gradlew :server:ee:libs:modules:components:kubernetes:spotlessApply :server:ee:libs:modules:components:kubernetes:test -q
```

The first run writes the JSON; run `test` once more to see it pass against the file. Read the generated JSON: the connection's seven properties must appear, and `token` and `clientKeyData` must carry the `PASSWORD` control type.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts server/ee/libs/modules/components/kubernetes
git commit -m "Add the kubernetes connection component"
```

---
### Task 3: Runner module scaffold and connection settings (EE)

**Files:**
- Create: `K8S/build.gradle.kts`
- Modify: `settings.gradle.kts`, adding `include("server:ee:libs:platform:platform-component:platform-component-runner-kubernetes")` next to the other `server:ee:libs:platform:platform-component:*` lines (around line 683)
- Create: `K8S_PKG/KubernetesConnectionSettings.java`
- Test: `K8S_TEST/KubernetesConnectionSettingsTest.java`

**Interfaces:**
- Consumes: `com.bytechef.platform.component.ComponentConnection` (`getParameters()`), and Task 2's `KubernetesConstants` (in tests only).
- Produces: `record KubernetesConnectionSettings(String masterUrl, @Nullable String caCertData, @Nullable String token, @Nullable String clientCertData, @Nullable String clientKeyData, String namespace, boolean insecureSkipTlsVerify)`, with:
  - `static KubernetesConnectionSettings of(ComponentConnection connection)`
  - `void validate()`
  - `io.fabric8.kubernetes.client.Config toConfig(Duration requestTimeout)`
  - a `toString()` that redacts the token and the client key.

- [ ] **Step 1: Build file**

```kotlin
dependencies {
    implementation("io.fabric8:kubernetes-client")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    testImplementation(project(":server:ee:libs:modules:components:kubernetes"))
    testImplementation(project(":server:libs:test:test-support"))
}
```

`io.fabric8:kubernetes-client` has no version because the Spring Cloud 2025.1 BOM imported in `server/build.gradle.kts` manages the fabric8 BOM. Confirm with `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:dependencies --configuration compileClasspath | grep fabric8`. If it does not resolve, add `fabric8-kubernetes-client = "7.4.1"` under `[versions]` in `gradle/libs.versions.toml` plus a library alias, and use that alias instead.

- [ ] **Step 2: Write the failing test**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.ee.component.kubernetes.constant.KubernetesConstants;
import com.bytechef.platform.component.ComponentConnection;
import io.fabric8.kubernetes.client.Config;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KubernetesConnectionSettingsTest {

    private static final String PEM = "-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIBADAK\n-----END CERTIFICATE-----\n";

    @Test
    void testEveryFieldIsReadFromTheConnection() {
        KubernetesConnectionSettings settings = KubernetesConnectionSettings.of(
            connection(
                Map.of(
                    "masterUrl", "https://cluster:6443", "caCertData", PEM, "token", "secret-token", "namespace",
                    "jobs", "insecureSkipTlsVerify", true)));

        assertThat(settings.masterUrl()).isEqualTo("https://cluster:6443");
        assertThat(settings.caCertData()).isEqualTo(PEM);
        assertThat(settings.token()).isEqualTo("secret-token");
        assertThat(settings.namespace()).isEqualTo("jobs");
        assertThat(settings.insecureSkipTlsVerify()).isTrue();
    }

    @Test
    void testABlankNamespaceFallsBackToDefault() {
        KubernetesConnectionSettings settings = KubernetesConnectionSettings.of(
            connection(Map.of("masterUrl", "https://cluster:6443", "token", "t", "namespace", " ")));

        assertThat(settings.namespace()).isEqualTo("default");
    }

    @Test
    void testValidateRequiresAnHttpApiServerUrl() {
        assertThatThrownBy(() -> settings(Map.of("token", "t")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("API server URL");

        assertThatThrownBy(() -> settings(Map.of("masterUrl", "cluster:6443", "token", "t")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("https://");
    }

    @Test
    void testValidateRequiresATokenOrAClientCertificateWithItsKey() {
        assertThatThrownBy(() -> settings(Map.of("masterUrl", "https://cluster:6443")).validate())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("token or a client certificate");

        assertThatThrownBy(
            () -> settings(Map.of("masterUrl", "https://cluster:6443", "clientCertData", PEM)).validate())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client key");

        settings(Map.of("masterUrl", "https://cluster:6443", "token", "t")).validate();
        settings(Map.of("masterUrl", "https://cluster:6443", "clientCertData", PEM, "clientKeyData", PEM)).validate();
    }

    @Test
    void testAPemCertificateIsBase64EncodedForTheClient() {
        Config config = settings(Map.of("masterUrl", "https://cluster:6443", "token", "t", "caCertData", PEM))
            .toConfig(Duration.ofSeconds(30));

        assertThat(config.getCaCertData()).isEqualTo(EncodingUtils.base64EncodeToString(PEM));
    }

    @Test
    void testABase64CertificatePassesThroughUnchanged() {
        String base64 = EncodingUtils.base64EncodeToString(PEM);

        Config config = settings(Map.of("masterUrl", "https://cluster:6443", "token", "t", "caCertData", base64))
            .toConfig(Duration.ofSeconds(30));

        assertThat(config.getCaCertData()).isEqualTo(base64);
    }

    @Test
    void testTheConfigCarriesTheConnectionAndNothingFromTheAmbientKubeconfig() {
        Config config = settings(
            Map.of(
                "masterUrl", "https://cluster:6443", "token", "secret-token", "namespace", "jobs",
                "insecureSkipTlsVerify", true))
                    .toConfig(Duration.ofSeconds(30));

        assertThat(config.getMasterUrl()).startsWith("https://cluster:6443");
        assertThat(config.getOauthToken()).isEqualTo("secret-token");
        assertThat(config.getNamespace()).isEqualTo("jobs");
        assertThat(config.isTrustCerts()).isTrue();
        assertThat(config.getRequestTimeout()).isEqualTo(30_000);
        assertThat(config.getCurrentContext()).isNull();
    }

    @Test
    void testToStringNeverShowsTheTokenOrTheClientKey() {
        String text = settings(
            Map.of(
                "masterUrl", "https://cluster:6443", "token", "secret-token", "clientCertData", PEM, "clientKeyData",
                "secret-key")).toString();

        assertThat(text).doesNotContain("secret-token")
            .doesNotContain("secret-key")
            .contains("https://cluster:6443");
    }

    @Test
    void testTheConnectionComponentDeclaresEveryParsedField() {
        assertThat(KubernetesConnectionSettings.MASTER_URL).isEqualTo(KubernetesConstants.MASTER_URL);
        assertThat(KubernetesConnectionSettings.CA_CERT_DATA).isEqualTo(KubernetesConstants.CA_CERT_DATA);
        assertThat(KubernetesConnectionSettings.TOKEN).isEqualTo(KubernetesConstants.TOKEN);
        assertThat(KubernetesConnectionSettings.CLIENT_CERT_DATA).isEqualTo(KubernetesConstants.CLIENT_CERT_DATA);
        assertThat(KubernetesConnectionSettings.CLIENT_KEY_DATA).isEqualTo(KubernetesConstants.CLIENT_KEY_DATA);
        assertThat(KubernetesConnectionSettings.NAMESPACE).isEqualTo(KubernetesConstants.NAMESPACE);
        assertThat(KubernetesConnectionSettings.INSECURE_SKIP_TLS_VERIFY)
            .isEqualTo(KubernetesConstants.INSECURE_SKIP_TLS_VERIFY);
    }

    private static ComponentConnection connection(Map<String, ?> parameters) {
        return new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(parameters), null);
    }

    private static KubernetesConnectionSettings settings(Map<String, ?> parameters) {
        return KubernetesConnectionSettings.of(connection(parameters));
    }
}
```

- [ ] **Step 3: Run, expect failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: compile failure.

- [ ] **Step 4: Implement**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.ComponentConnection;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import java.time.Duration;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The cluster a Kubernetes task runner talks to, read from the task's {@code kubernetes} connection.
 *
 * @author Ivica Cardic
 * @version ee
 */
public record KubernetesConnectionSettings(
    String masterUrl, @Nullable String caCertData, @Nullable String token, @Nullable String clientCertData,
    @Nullable String clientKeyData, String namespace, boolean insecureSkipTlsVerify) {

    static final String CA_CERT_DATA = "caCertData";
    static final String CLIENT_CERT_DATA = "clientCertData";
    static final String CLIENT_KEY_DATA = "clientKeyData";
    static final String INSECURE_SKIP_TLS_VERIFY = "insecureSkipTlsVerify";
    static final String MASTER_URL = "masterUrl";
    static final String NAMESPACE = "namespace";
    static final String TOKEN = "token";

    private static final String DEFAULT_NAMESPACE = "default";
    private static final String PEM_PREFIX = "-----BEGIN";

    public static KubernetesConnectionSettings of(ComponentConnection connection) {
        Map<String, ?> parameters = connection.getParameters();

        String namespace = text(parameters, NAMESPACE);

        return new KubernetesConnectionSettings(
            String.valueOf(parameters.getOrDefault(MASTER_URL, ""))
                .trim(),
            text(parameters, CA_CERT_DATA), text(parameters, TOKEN), text(parameters, CLIENT_CERT_DATA),
            text(parameters, CLIENT_KEY_DATA), namespace == null ? DEFAULT_NAMESPACE : namespace,
            Boolean.parseBoolean(String.valueOf(parameters.get(INSECURE_SKIP_TLS_VERIFY))));
    }

    public void validate() {
        if (masterUrl.isEmpty()) {
            throw new IllegalArgumentException("The kubernetes connection needs the cluster's API server URL");
        }

        if (!masterUrl.startsWith("https://") && !masterUrl.startsWith("http://")) {
            throw new IllegalArgumentException(
                "The kubernetes connection's API server URL must start with https:// (or http://)");
        }

        if (clientCertData != null && clientKeyData == null) {
            throw new IllegalArgumentException(
                "The kubernetes connection has a client certificate but no client key");
        }

        if (token == null && clientCertData == null) {
            throw new IllegalArgumentException(
                "The kubernetes connection needs a bearer token or a client certificate and key");
        }
    }

    public Config toConfig(Duration requestTimeout) {
        ConfigBuilder configBuilder = new ConfigBuilder(Config.empty());

        configBuilder.withMasterUrl(masterUrl);
        configBuilder.withNamespace(namespace);
        configBuilder.withTrustCerts(insecureSkipTlsVerify);
        configBuilder.withDisableHostnameVerification(insecureSkipTlsVerify);
        configBuilder.withRequestTimeout((int) requestTimeout.toMillis());
        configBuilder.withConnectionTimeout((int) Math.min(requestTimeout.toMillis(), 30_000));

        if (caCertData != null) {
            configBuilder.withCaCertData(toBase64Pem(caCertData));
        }

        if (token != null) {
            configBuilder.withOauthToken(token);
        }

        if (clientCertData != null && clientKeyData != null) {
            configBuilder.withClientCertData(toBase64Pem(clientCertData));
            configBuilder.withClientKeyData(toBase64Pem(clientKeyData));
        }

        return configBuilder.build();
    }

    @Override
    public String toString() {
        return "KubernetesConnectionSettings[masterUrl=%s, namespace=%s, token=%s, clientKey=%s, insecure=%s]"
            .formatted(
                masterUrl, namespace, token == null ? "none" : "***", clientKeyData == null ? "none" : "***",
                insecureSkipTlsVerify);
    }

    private static String toBase64Pem(String value) {
        return value.startsWith(PEM_PREFIX) ? EncodingUtils.base64EncodeToString(value) : value;
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
}
```

`ConfigBuilder` method names follow fabric8 7.x. If one differs, use the equivalent setter on the built `Config` and say so in the report. Mark the record `@SuppressFBWarnings` only if SpotBugs complains.

- [ ] **Step 5: Run tests**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts server/ee/libs/platform/platform-component/platform-component-runner-kubernetes
git commit -m "Add the kubernetes runner module and its connection settings"
```

---
### Task 4: Labels, runner settings and the pod (EE, pure)

Everything here is pure: no cluster calls. Task 5 drives it.

**Files:**
- Create: `K8S_PKG/KubernetesJobLabels.java`, `K8S_PKG/KubernetesRunnerSettings.java`, `K8S_PKG/KubernetesPodFactory.java`
- Test: `K8S_TEST/KubernetesJobLabelsTest.java`, `K8S_TEST/KubernetesRunnerSettingsTest.java`, `K8S_TEST/KubernetesPodFactoryTest.java`

**Interfaces:**
- Consumes: `RemoteJobSpec` (`key()`, `languageId()`, `interpreter()`, `environment()`, `remoteSourcePath()`, `pullPolicy()`), `RemoteJobKey(value, jobId)`, `RemotePullPolicy`, `com.bytechef.component.definition.Parameters`, `TaskRunnerConstants.ENTRYPOINT/IMAGE/KILL_GRACE_PERIOD/CREDENTIALS/REGISTRY/USERNAME/PASSWORD`, and `JsonUtils.write`.
- Produces:
  - `final class KubernetesJobLabels`:
    - constants `LABEL_MANAGED_BY = "bytechef.io/managed-by"`, `LABEL_TASK_EXECUTION_ID = "bytechef.io/task-execution-id"`, `LABEL_JOB_ID = "bytechef.io/job-id"`, `ANNOTATION_KEY = "bytechef.io/key"`, `ANNOTATION_KILL_GRACE_PERIOD = "bytechef.io/kill-grace-period"`, `ANNOTATION_PULL_SECRET = "bytechef.io/pull-secret"`, `MANAGED_BY = "bytechef"`, `RESERVED_PREFIX = "bytechef.io/"`;
    - `static String labelValue(String key)`, `static Map<String, String> labels(RemoteJobKey key)`, `static Map<String, String> selector(RemoteJobKey key)`.
  - `record KubernetesRunnerSettings(String image, @Nullable String namespace, @Nullable String serviceAccountName, Map<String, String> resourceRequests, Map<String, String> resourceLimits, Map<String, String> nodeSelector, List<Toleration> tolerations, Map<String, String> labels, Map<String, String> annotations, @Nullable RegistryCredentials credentials, String fileSidecarImage, Duration killGracePeriod)`, with:
    - nested `record Toleration(String key, String operator, @Nullable String value, @Nullable String effect)` and `record RegistryCredentials(String registry, String username, String password)`;
    - `static KubernetesRunnerSettings of(String languageId, Parameters parameters)` (validates; throws `IllegalArgumentException`);
    - `static List<ModifiableValueProperty<?, ?>> properties()`;
    - `String namespaceOr(String connectionNamespace)`.
  - `final class KubernetesPodFactory`:
    - constants `WORK_DIRECTORY = "/bytechef/work"`, `INIT_CONTAINER = "bytechef-init"`, `MAIN_CONTAINER = "main"`, `OUTPUT_CONTAINER = "bytechef-output"`, `READY_MARKER = "/bytechef/work/.ready"`, `DONE_MARKER = "/bytechef/work/.done"`;
    - `static Pod pod(RemoteJobSpec spec, KubernetesRunnerSettings settings, String namespace, Duration completionBudget, long maxTransferBytes, @Nullable String pullSecretName)`;
    - `static Secret pullSecret(String namespace, RemoteJobKey key, RegistryCredentials credentials)`.

- [ ] **Step 1: Write the failing tests**

`KubernetesJobLabelsTest.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import org.junit.jupiter.api.Test;

class KubernetesJobLabelsTest {

    @Test
    void testAKeyThatIsAValidLabelValueIsUsedAsIs() {
        assertThat(KubernetesJobLabels.labelValue("public-100-7")).isEqualTo("public-100-7");
    }

    @Test
    void testATooLongKeyIsHashed() {
        String key = "t".repeat(60) + "-100-7";

        String labelValue = KubernetesJobLabels.labelValue(key);

        assertThat(labelValue).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue(key)).isEqualTo(labelValue);
        assertThat(KubernetesJobLabels.labelValue(key + "x")).isNotEqualTo(labelValue);
    }

    @Test
    void testAKeyWithCharactersALabelCannotHoldIsHashed() {
        assertThat(KubernetesJobLabels.labelValue("tenant/a-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue("_tenant-1-7")).matches("k-[0-9a-f]{16}");
        assertThat(KubernetesJobLabels.labelValue("tenant-1-7.")).matches("k-[0-9a-f]{16}");
    }

    @Test
    void testLabelsCarryTheManagerTheKeyAndTheJob() {
        assertThat(KubernetesJobLabels.labels(new RemoteJobKey("public-100-7", 100L)))
            .containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/task-execution-id", "public-100-7")
            .containsEntry("bytechef.io/job-id", "100");

        assertThat(KubernetesJobLabels.labels(new RemoteJobKey("random", null)))
            .doesNotContainKey("bytechef.io/job-id");
    }

    @Test
    void testTheSelectorIsTheManagerAndTheKey() {
        assertThat(KubernetesJobLabels.selector(new RemoteJobKey("public-100-7", 100L)))
            .containsOnlyKeys("bytechef.io/managed-by", "bytechef.io/task-execution-id");
    }
}
```

`KubernetesRunnerSettingsTest.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KubernetesRunnerSettingsTest {

    @Test
    void testDefaultsFollowTheLanguage() {
        assertThat(settings("python", Map.of()).image()).isEqualTo("python:3.12-slim");
        assertThat(settings("javascript", Map.of()).image()).isEqualTo("node:22-alpine");
        assertThat(settings("js", Map.of()).image()).isEqualTo("node:22-alpine");
        assertThat(settings("shell", Map.of()).image()).isEqualTo("ubuntu:24.04");

        KubernetesRunnerSettings settings = settings("python", Map.of());

        assertThat(settings.fileSidecarImage()).isEqualTo("busybox:1.36");
        assertThat(settings.killGracePeriod()).isEqualTo(Duration.ZERO);
        assertThat(settings.namespace()).isNull();
        assertThat(settings.namespaceOr("default")).isEqualTo("default");
    }

    @Test
    void testEveryPropertyIsRead() {
        KubernetesRunnerSettings settings = settings(
            "python",
            Map.ofEntries(
                Map.entry("image", "registry.example.com/team/python:3.12"), Map.entry("namespace", "jobs"),
                Map.entry("serviceAccountName", "runner"),
                Map.entry(
                    "resources",
                    Map.of("cpuRequest", "250m", "cpuLimit", "1", "memoryRequest", "128Mi", "memoryLimit", "1Gi")),
                Map.entry("nodeSelector", Map.of("pool", "batch")),
                Map.entry(
                    "tolerations",
                    List.of(Map.of("key", "dedicated", "operator", "Equal", "value", "batch", "effect", "NoSchedule"))),
                Map.entry("labels", Map.of("team", "data")), Map.entry("annotations", Map.of("owner", "data-team")),
                Map.entry(
                    "credentials", Map.of("registry", "registry.example.com", "username", "u", "password", "p")),
                Map.entry("fileSidecarImage", "busybox:1.37"), Map.entry("killGracePeriod", "PT10S")));

        assertThat(settings.image()).isEqualTo("registry.example.com/team/python:3.12");
        assertThat(settings.namespaceOr("default")).isEqualTo("jobs");
        assertThat(settings.serviceAccountName()).isEqualTo("runner");
        assertThat(settings.resourceRequests()).containsEntry("cpu", "250m")
            .containsEntry("memory", "128Mi");
        assertThat(settings.resourceLimits()).containsEntry("cpu", "1")
            .containsEntry("memory", "1Gi");
        assertThat(settings.nodeSelector()).containsEntry("pool", "batch");
        assertThat(settings.tolerations()).containsExactly(
            new KubernetesRunnerSettings.Toleration("dedicated", "Equal", "batch", "NoSchedule"));
        assertThat(settings.labels()).containsEntry("team", "data");
        assertThat(settings.annotations()).containsEntry("owner", "data-team");
        assertThat(settings.credentials()).isEqualTo(
            new KubernetesRunnerSettings.RegistryCredentials("registry.example.com", "u", "p"));
        assertThat(settings.fileSidecarImage()).isEqualTo("busybox:1.37");
        assertThat(settings.killGracePeriod()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void testAMalformedQuantityNamesTheResource() {
        assertThatThrownBy(() -> settings("python", Map.of("resources", Map.of("memoryLimit", "lots"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("memoryLimit")
            .hasMessageContaining("lots");
    }

    @Test
    void testALabelOrAnnotationCannotUseTheReservedPrefix() {
        assertThatThrownBy(() -> settings("python", Map.of("labels", Map.of("bytechef.io/managed-by", "me"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.io/");

        assertThatThrownBy(() -> settings("python", Map.of("annotations", Map.of("bytechef.io/key", "x"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bytechef.io/");
    }

    @Test
    void testATolerationNeedsAKnownOperatorAndEffect() {
        assertThatThrownBy(
            () -> settings("python", Map.of("tolerations", List.of(Map.of("key", "k", "operator", "Near")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Near");

        assertThatThrownBy(
            () -> settings(
                "python", Map.of("tolerations", List.of(Map.of("key", "k", "operator", "Exists", "effect", "Sometimes")))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Sometimes");
    }

    @Test
    void testCredentialsNeedAUsernameAndAPassword() {
        assertThatThrownBy(() -> settings("python", Map.of("credentials", Map.of("registry", "r", "username", "u"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("password");
    }

    @Test
    void testAMalformedOrNegativeKillGracePeriodIsRejected() {
        assertThatThrownBy(() -> settings("python", Map.of("killGracePeriod", "soon")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("killGracePeriod");

        assertThatThrownBy(() -> settings("python", Map.of("killGracePeriod", "-PT1S")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("killGracePeriod");
    }

    @Test
    void testPropertiesAreFreshAndCoverEverySetting() {
        List<ModifiableValueProperty<?, ?>> properties = KubernetesRunnerSettings.properties();

        assertThat(properties.getFirst()).isNotSameAs(KubernetesRunnerSettings.properties()
            .getFirst());
        assertThat(properties).extracting(ValueProperty::getName)
            .containsExactly(
                "image", "entrypoint", "namespace", "serviceAccountName", "resources", "nodeSelector", "tolerations",
                "labels", "annotations", "credentials", "fileSidecarImage", "killGracePeriod");
    }

    private static KubernetesRunnerSettings settings(String languageId, Map<String, ?> parameters) {
        return KubernetesRunnerSettings.of(languageId, ParametersFactory.create(parameters));
    }
}
```

`KubernetesPodFactoryTest.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class KubernetesPodFactoryTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);

    @Test
    void testThePodHasTheInitMainAndOutputContainersSharingTheWorkVolume() {
        Pod pod = pod(Map.of(), null);

        assertThat(pod.getSpec()
            .getRestartPolicy()).isEqualTo("Never");
        assertThat(pod.getSpec()
            .getInitContainers()).extracting(Container::getName)
            .containsExactly("bytechef-init");
        assertThat(pod.getSpec()
            .getContainers()).extracting(Container::getName)
            .containsExactly("main", "bytechef-output");

        assertThat(pod.getSpec()
            .getVolumes()
            .getFirst()
            .getEmptyDir()
            .getSizeLimit()).isEqualTo(new Quantity(String.valueOf(2L * 1024 * 1024)));

        for (Container container : allContainers(pod)) {
            assertThat(container.getVolumeMounts()
                .getFirst()
                .getMountPath()).isEqualTo("/bytechef/work");
        }
    }

    @Test
    void testTheInitAndOutputContainersWaitForTheirMarkers() {
        Pod pod = pod(Map.of(), null);

        Container init = pod.getSpec()
            .getInitContainers()
            .getFirst();
        Container output = pod.getSpec()
            .getContainers()
            .get(1);

        assertThat(init.getImage()).isEqualTo("busybox:1.36");
        assertThat(String.join(" ", init.getCommand())).contains("/bytechef/work/.ready");
        assertThat(String.join(" ", output.getCommand())).contains("/bytechef/work/.done");
    }

    @Test
    void testMainRunsTheInterpreterOnTheSourceWithStderrRedirected() {
        Container main = pod(Map.of(), null).getSpec()
            .getContainers()
            .getFirst();

        assertThat(main.getImage()).isEqualTo("python:3.12-slim");
        assertThat(main.getImagePullPolicy()).isEqualTo("IfNotPresent");
        assertThat(main.getWorkingDir()).isEqualTo("/bytechef/work");
        assertThat(main.getCommand()).containsExactly(
            "sh", "-c", "exec \"$@\" 2>/bytechef/work/.stderr", "sh", "python3", "/bytechef/work/script.py");
        assertThat(main.getEnv()).anySatisfy(envVar -> {
            assertThat(envVar.getName()).isEqualTo("BYTECHEF_INPUT_FILE");
            assertThat(envVar.getValue()).isEqualTo("/bytechef/work/input.json");
        });
    }

    @Test
    void testLabelsAndAnnotationsCarryTheKeyAndTheAuthorsOwn() {
        Pod pod = pod(Map.of("labels", Map.of("team", "data"), "annotations", Map.of("owner", "x"),
            "killGracePeriod", "PT10S"), "bytechef-pull-abc");

        assertThat(pod.getMetadata()
            .getGenerateName()).isEqualTo("bytechef-run-");
        assertThat(pod.getMetadata()
            .getNamespace()).isEqualTo("jobs");
        assertThat(pod.getMetadata()
            .getLabels()).containsEntry("team", "data")
            .containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/task-execution-id", "public-100-7")
            .containsEntry("bytechef.io/job-id", "100");
        assertThat(pod.getMetadata()
            .getAnnotations()).containsEntry("owner", "x")
            .containsEntry("bytechef.io/key", "public-100-7")
            .containsEntry("bytechef.io/kill-grace-period", "PT10S")
            .containsEntry("bytechef.io/pull-secret", "bytechef-pull-abc");
        assertThat(pod.getSpec()
            .getImagePullSecrets()
            .getFirst()
            .getName()).isEqualTo("bytechef-pull-abc");
    }

    @Test
    void testNoGracePeriodOrPullSecretMeansNoAnnotation() {
        Pod pod = pod(Map.of(), null);

        assertThat(pod.getMetadata()
            .getAnnotations()).doesNotContainKeys("bytechef.io/kill-grace-period", "bytechef.io/pull-secret");
        assertThat(pod.getSpec()
            .getImagePullSecrets()).isNullOrEmpty();
    }

    @Test
    void testTheDeadlineIsTheCompletionBudgetPlusAMinute() {
        assertThat(pod(Map.of(), null).getSpec()
            .getActiveDeadlineSeconds()).isEqualTo(3600L + 60);
    }

    @Test
    void testWithoutAServiceAccountTheTokenIsNotMounted() {
        assertThat(pod(Map.of(), null).getSpec()
            .getAutomountServiceAccountToken()).isFalse();

        Pod pod = pod(Map.of("serviceAccountName", "runner"), null);

        assertThat(pod.getSpec()
            .getServiceAccountName()).isEqualTo("runner");
        assertThat(pod.getSpec()
            .getAutomountServiceAccountToken()).isNull();
    }

    @Test
    void testResourcesSchedulingAndPullPolicyReachThePod() {
        Pod pod = KubernetesPodFactory.pod(
            spec(RemotePullPolicy.ALWAYS),
            KubernetesRunnerSettings.of(
                "python",
                ParametersFactory.create(
                    Map.of(
                        "resources", Map.of("cpuLimit", "2", "memoryRequest", "256Mi"),
                        "nodeSelector", Map.of("pool", "batch"),
                        "tolerations", List.of(Map.of("key", "dedicated", "operator", "Exists"))))),
            "jobs", Duration.ofHours(1), 1024L * 1024, null);

        Container main = pod.getSpec()
            .getContainers()
            .getFirst();

        assertThat(main.getImagePullPolicy()).isEqualTo("Always");
        assertThat(main.getResources()
            .getLimits()).containsEntry("cpu", new Quantity("2"));
        assertThat(main.getResources()
            .getRequests()).containsEntry("memory", new Quantity("256Mi"));
        assertThat(pod.getSpec()
            .getNodeSelector()).containsEntry("pool", "batch");
        assertThat(pod.getSpec()
            .getTolerations()
            .getFirst()
            .getOperator()).isEqualTo("Exists");
    }

    @Test
    void testThePullSecretIsADockerConfigJsonForTheRegistry() {
        Secret secret = KubernetesPodFactory.pullSecret(
            "jobs", KEY, new KubernetesRunnerSettings.RegistryCredentials("registry.example.com", "u", "p"));

        assertThat(secret.getType()).isEqualTo("kubernetes.io/dockerconfigjson");
        assertThat(secret.getMetadata()
            .getGenerateName()).isEqualTo("bytechef-pull-");
        assertThat(secret.getMetadata()
            .getLabels()).containsEntry("bytechef.io/managed-by", "bytechef");

        String json = new String(
            EncodingUtils.base64Decode(secret.getData()
                .get(".dockerconfigjson")),
            StandardCharsets.UTF_8);

        assertThat(json).contains("registry.example.com")
            .contains(EncodingUtils.base64EncodeToString("u:p"));
    }

    private static List<Container> allContainers(Pod pod) {
        return java.util.stream.Stream.concat(
            pod.getSpec()
                .getInitContainers()
                .stream(),
            pod.getSpec()
                .getContainers()
                .stream())
            .toList();
    }

    private static Pod pod(Map<String, ?> runnerParameters, String pullSecretName) {
        return KubernetesPodFactory.pod(
            spec(RemotePullPolicy.IF_NOT_PRESENT),
            KubernetesRunnerSettings.of("python", ParametersFactory.create(runnerParameters)), "jobs",
            Duration.ofHours(1), 1024L * 1024, pullSecretName);
    }

    private static RemoteJobSpec spec(RemotePullPolicy pullPolicy) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"),
            Map.of("HOME", "/bytechef/work", "BYTECHEF_INPUT_FILE", "/bytechef/work/input.json"), "/bytechef/work",
            pullPolicy, ParametersFactory.create(Map.of()));
    }
}
```

(Use `import java.util.stream.Stream;` rather than the fully qualified name in `allContainers`.)

- [ ] **Step 2: Run, expect compilation failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: FAIL (classes missing). Add `testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))` to the build file if `ParametersFactory` is not visible to the tests (it lives in `com.bytechef.platform.component.definition`).

- [ ] **Step 3: Implement `KubernetesJobLabels`**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Label and annotation names the Kubernetes runner puts on what it creates, and the label value a job key maps to.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class KubernetesJobLabels {

    static final String ANNOTATION_KEY = "bytechef.io/key";
    static final String ANNOTATION_KILL_GRACE_PERIOD = "bytechef.io/kill-grace-period";
    static final String ANNOTATION_PULL_SECRET = "bytechef.io/pull-secret";
    static final String LABEL_JOB_ID = "bytechef.io/job-id";
    static final String LABEL_MANAGED_BY = "bytechef.io/managed-by";
    static final String LABEL_TASK_EXECUTION_ID = "bytechef.io/task-execution-id";
    static final String MANAGED_BY = "bytechef";
    static final String RESERVED_PREFIX = "bytechef.io/";

    private static final Pattern LABEL_VALUE = Pattern.compile("^[A-Za-z0-9]([-A-Za-z0-9_.]{0,61}[A-Za-z0-9])?$");

    private KubernetesJobLabels() {
    }

    static String labelValue(String key) {
        if (LABEL_VALUE.matcher(key)
            .matches()) {

            return key;
        }

        return "k-" + sha256Hex(key).substring(0, 16);
    }

    static Map<String, String> labels(RemoteJobKey key) {
        Map<String, String> labels = selector(key);

        Long jobId = key.jobId();

        if (jobId != null) {
            labels.put(LABEL_JOB_ID, String.valueOf(jobId));
        }

        return labels;
    }

    static Map<String, String> selector(RemoteJobKey key) {
        Map<String, String> selector = new LinkedHashMap<>();

        selector.put(LABEL_MANAGED_BY, MANAGED_BY);
        selector.put(LABEL_TASK_EXECUTION_ID, labelValue(key.value()));

        return selector;
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(value.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of()
                .formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
```

- [ ] **Step 4: Implement `KubernetesRunnerSettings`**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.CREDENTIALS;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.ENTRYPOINT;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.IMAGE;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.KILL_GRACE_PERIOD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.PASSWORD;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.REGISTRY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.USERNAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableValueProperty;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import io.fabric8.kubernetes.api.model.Quantity;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * The Kubernetes runner's task properties, read and checked once.
 *
 * @author Ivica Cardic
 * @version ee
 */
record KubernetesRunnerSettings(
    String image, @Nullable String namespace, @Nullable String serviceAccountName,
    Map<String, String> resourceRequests, Map<String, String> resourceLimits, Map<String, String> nodeSelector,
    List<Toleration> tolerations, Map<String, String> labels, Map<String, String> annotations,
    @Nullable RegistryCredentials credentials, String fileSidecarImage, Duration killGracePeriod) {

    static final String ANNOTATIONS = "annotations";
    static final String CPU_LIMIT = "cpuLimit";
    static final String CPU_REQUEST = "cpuRequest";
    static final String FILE_SIDECAR_IMAGE = "fileSidecarImage";
    static final String LABELS = "labels";
    static final String MEMORY_LIMIT = "memoryLimit";
    static final String MEMORY_REQUEST = "memoryRequest";
    static final String NAMESPACE = "namespace";
    static final String NODE_SELECTOR = "nodeSelector";
    static final String RESOURCES = "resources";
    static final String SERVICE_ACCOUNT_NAME = "serviceAccountName";
    static final String TOLERATIONS = "tolerations";

    private static final String DEFAULT_FILE_SIDECAR_IMAGE = "busybox:1.36";
    private static final Map<String, String> DEFAULT_IMAGES = Map.of(
        "javascript", "node:22-alpine", "js", "node:22-alpine", "python", "python:3.12-slim", "shell",
        "ubuntu:24.04");
    private static final Set<String> TOLERATION_EFFECTS = Set.of("NoSchedule", "PreferNoSchedule", "NoExecute");
    private static final Set<String> TOLERATION_OPERATORS = Set.of("Equal", "Exists");

    record Toleration(String key, String operator, @Nullable String value, @Nullable String effect) {
    }

    record RegistryCredentials(String registry, String username, String password) {

        @Override
        public String toString() {
            return "RegistryCredentials[registry=%s, username=%s, password=***]".formatted(registry, username);
        }
    }

    static KubernetesRunnerSettings of(String languageId, Parameters parameters) {
        String image = text(parameters.getString(IMAGE));

        if (image == null) {
            image = DEFAULT_IMAGES.get(languageId);
        }

        if (image == null) {
            throw new IllegalArgumentException(
                "The kubernetes runner has no default image for language '%s'; name one on the task runner"
                    .formatted(languageId));
        }

        Map<String, String> resources = parameters.getMap(RESOURCES, String.class, Map.of());

        Map<String, String> resourceRequests = new LinkedHashMap<>();
        Map<String, String> resourceLimits = new LinkedHashMap<>();

        putQuantity(resourceRequests, "cpu", resources, CPU_REQUEST);
        putQuantity(resourceRequests, "memory", resources, MEMORY_REQUEST);
        putQuantity(resourceLimits, "cpu", resources, CPU_LIMIT);
        putQuantity(resourceLimits, "memory", resources, MEMORY_LIMIT);

        Map<String, String> labels = parameters.getMap(LABELS, String.class, Map.of());
        Map<String, String> annotations = parameters.getMap(ANNOTATIONS, String.class, Map.of());

        checkNotReserved(labels, LABELS);
        checkNotReserved(annotations, ANNOTATIONS);

        String fileSidecarImage = text(parameters.getString(FILE_SIDECAR_IMAGE));

        return new KubernetesRunnerSettings(
            image, text(parameters.getString(NAMESPACE)), text(parameters.getString(SERVICE_ACCOUNT_NAME)),
            Map.copyOf(resourceRequests), Map.copyOf(resourceLimits),
            Map.copyOf(parameters.getMap(NODE_SELECTOR, String.class, Map.of())), tolerations(parameters),
            Map.copyOf(labels), Map.copyOf(annotations), credentials(parameters),
            fileSidecarImage == null ? DEFAULT_FILE_SIDECAR_IMAGE : fileSidecarImage, killGracePeriod(parameters));
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
            string(NAMESPACE)
                .label("Namespace")
                .description("The namespace the pod runs in. Defaults to the connection's namespace.")
                .required(false));
        properties.add(
            string(SERVICE_ACCOUNT_NAME)
                .label("Service Account")
                .description(
                    "The service account the pod runs as. Without one, no service account token is mounted.")
                .required(false));
        properties.add(
            object(RESOURCES)
                .label("Resources")
                .description("CPU and memory requests and limits for the task's container, as Kubernetes quantities.")
                .properties(
                    string(CPU_REQUEST).label("CPU Request")
                        .required(false),
                    string(CPU_LIMIT).label("CPU Limit")
                        .required(false),
                    string(MEMORY_REQUEST).label("Memory Request")
                        .required(false),
                    string(MEMORY_LIMIT).label("Memory Limit")
                        .required(false))
                .required(false));
        properties.add(
            object(NODE_SELECTOR)
                .label("Node Selector")
                .description("Node labels the pod must be scheduled onto.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            array(TOLERATIONS)
                .label("Tolerations")
                .description("Taints the pod tolerates.")
                .items(
                    object().properties(
                        string("key").label("Key")
                            .required(false),
                        string("operator").label("Operator")
                            .options(option("Equal", "Equal"), option("Exists", "Exists"))
                            .required(true),
                        string("value").label("Value")
                            .required(false),
                        string("effect").label("Effect")
                            .options(
                                option("No Schedule", "NoSchedule"), option("Prefer No Schedule", "PreferNoSchedule"),
                                option("No Execute", "NoExecute"))
                            .required(false)))
                .required(false));
        properties.add(
            object(LABELS)
                .label("Labels")
                .description("Extra labels on the pod. Keys under bytechef.io/ are reserved.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            object(ANNOTATIONS)
                .label("Annotations")
                .description("Extra annotations on the pod. Keys under bytechef.io/ are reserved.")
                .additionalProperties(string())
                .required(false));
        properties.add(
            object(CREDENTIALS)
                .label("Registry Credentials")
                .description("How the image is pulled from a private registry; a temporary pull secret is created.")
                .properties(
                    string(REGISTRY).label("Registry")
                        .required(true),
                    string(USERNAME).label("Username")
                        .required(true),
                    string(PASSWORD).label("Password")
                        .controlType(ControlType.PASSWORD)
                        .required(true))
                .required(false));
        properties.add(
            string(FILE_SIDECAR_IMAGE)
                .label("File Sidecar Image")
                .description(
                    "The image that moves files in and out of the pod; it needs sh, tar, test, sleep and touch.")
                .defaultValue(DEFAULT_FILE_SIDECAR_IMAGE)
                .required(false));
        properties.add(
            string(KILL_GRACE_PERIOD)
                .label("Kill Grace Period")
                .description(
                    "How long a still-running pod is given to stop on its own when it is killed - on a timeout or a " +
                        "non-resumable shutdown - before it is forced, as an ISO-8601 duration.")
                .defaultValue("PT0S")
                .required(false));

        return properties;
    }

    String namespaceOr(String connectionNamespace) {
        return namespace == null ? connectionNamespace : namespace;
    }

    private static void checkNotReserved(Map<String, String> entries, String propertyName) {
        for (String key : entries.keySet()) {
            if (key.startsWith(KubernetesJobLabels.RESERVED_PREFIX)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner's %s cannot use the reserved key '%s' (keys under %s belong to ByteChef)"
                        .formatted(propertyName, key, KubernetesJobLabels.RESERVED_PREFIX));
            }
        }
    }

    @Nullable
    private static RegistryCredentials credentials(Parameters parameters) {
        Map<String, String> credentials = parameters.getMap(CREDENTIALS, String.class, Map.of());

        if (credentials.isEmpty()) {
            return null;
        }

        String registry = text(credentials.get(REGISTRY));
        String username = text(credentials.get(USERNAME));
        String password = credentials.get(PASSWORD);

        if (registry == null || username == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                "The kubernetes runner's registry credentials need a registry, a username and a password");
        }

        return new RegistryCredentials(registry, username, password);
    }

    private static Duration killGracePeriod(Parameters parameters) {
        String value = text(parameters.getString(KILL_GRACE_PERIOD));

        if (value == null) {
            return Duration.ZERO;
        }

        Duration duration;

        try {
            duration = Duration.parse(value);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                "The kubernetes runner cannot read '%s' as the ISO-8601 duration %s expects".formatted(
                    value, KILL_GRACE_PERIOD),
                exception);
        }

        if (duration.isNegative()) {
            throw new IllegalArgumentException(
                "The kubernetes runner's %s must not be negative".formatted(KILL_GRACE_PERIOD));
        }

        return duration;
    }

    private static void putQuantity(
        Map<String, String> target, String resourceName, Map<String, String> resources, String propertyName) {

        String value = text(resources.get(propertyName));

        if (value == null) {
            return;
        }

        try {
            Quantity.parse(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "The kubernetes runner cannot read '%s' as the quantity %s expects".formatted(value, propertyName),
                exception);
        }

        target.put(resourceName, value);
    }

    @Nullable
    private static String text(@Nullable String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static List<Toleration> tolerations(Parameters parameters) {
        List<Toleration> tolerations = new ArrayList<>();

        for (Map<?, ?> toleration : parameters.getList(TOLERATIONS, Map.class, List.of())) {
            String operator = text(stringValue(toleration.get("operator")));
            String effect = text(stringValue(toleration.get("effect")));

            if (operator == null || !TOLERATION_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner does not know the toleration operator '%s'; use Equal or Exists"
                        .formatted(operator));
            }

            if (effect != null && !TOLERATION_EFFECTS.contains(effect)) {
                throw new IllegalArgumentException(
                    "The kubernetes runner does not know the toleration effect '%s'".formatted(effect));
            }

            tolerations.add(
                new Toleration(
                    String.valueOf(toleration.get("key") == null ? "" : toleration.get("key")), operator,
                    text(stringValue(toleration.get("value"))), effect));
        }

        return List.copyOf(tolerations);
    }

    @Nullable
    private static String stringValue(@Nullable Object value) {
        return value == null ? null : value.toString();
    }
}
```

`Quantity.parse` throws `IllegalArgumentException` for malformed input in fabric8 7.x. If it accepts `lots` silently, validate with the regex `^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+|[KMGTPE]i?|[mkMGTPE])?$` instead.

- [ ] **Step 5: Implement `KubernetesPodFactory`**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.component.runner.remote.RemoteJobKey;
import com.bytechef.platform.component.runner.remote.RemoteJobSpec;
import com.bytechef.platform.component.runner.remote.RemotePullPolicy;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Builds the pod a Kubernetes task runs in: a file sidecar as init container that receives the inputs, the author's
 * image as {@code main}, and a file sidecar that hands the outputs back before the pod completes.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class KubernetesPodFactory {

    static final String DONE_MARKER = "/bytechef/work/.done";
    static final String INIT_CONTAINER = "bytechef-init";
    static final String MAIN_CONTAINER = "main";
    static final String OUTPUT_CONTAINER = "bytechef-output";
    static final String READY_MARKER = "/bytechef/work/.ready";
    static final String WORK_DIRECTORY = "/bytechef/work";

    private static final Duration DEADLINE_MARGIN = Duration.ofSeconds(60);
    private static final String POD_NAME_PREFIX = "bytechef-run-";
    private static final String PULL_SECRET_PREFIX = "bytechef-pull-";
    private static final String VOLUME_NAME = "work";

    private KubernetesPodFactory() {
    }

    static Pod pod(
        RemoteJobSpec spec, KubernetesRunnerSettings settings, String namespace, Duration completionBudget,
        long maxTransferBytes, @Nullable String pullSecretName) {

        Map<String, String> labels = new LinkedHashMap<>(settings.labels());

        labels.putAll(KubernetesJobLabels.labels(spec.key()));

        Map<String, String> annotations = new LinkedHashMap<>(settings.annotations());

        annotations.put(KubernetesJobLabels.ANNOTATION_KEY, spec.key()
            .value());

        if (!settings.killGracePeriod()
            .isZero()) {

            annotations.put(KubernetesJobLabels.ANNOTATION_KILL_GRACE_PERIOD, settings.killGracePeriod()
                .toString());
        }

        if (pullSecretName != null) {
            annotations.put(KubernetesJobLabels.ANNOTATION_PULL_SECRET, pullSecretName);
        }

        PodBuilder podBuilder = new PodBuilder();

        podBuilder.withNewMetadata()
            .withGenerateName(POD_NAME_PREFIX)
            .withNamespace(namespace)
            .withLabels(labels)
            .withAnnotations(annotations)
            .endMetadata();

        podBuilder.withNewSpec()
            .withRestartPolicy("Never")
            .withActiveDeadlineSeconds(completionBudget.plus(DEADLINE_MARGIN)
                .toSeconds())
            .withServiceAccountName(settings.serviceAccountName())
            .withAutomountServiceAccountToken(settings.serviceAccountName() == null ? Boolean.FALSE : null)
            .withNodeSelector(settings.nodeSelector()
                .isEmpty() ? null : settings.nodeSelector())
            .withTolerations(
                settings.tolerations()
                    .stream()
                    .map(toleration -> new TolerationBuilder()
                        .withKey(toleration.key()
                            .isEmpty() ? null : toleration.key())
                        .withOperator(toleration.operator())
                        .withValue(toleration.value())
                        .withEffect(toleration.effect())
                        .build())
                    .toList())
            .withImagePullSecrets(
                pullSecretName == null ? List.of() : List.of(new LocalObjectReference(pullSecretName)))
            .addNewVolume()
            .withName(VOLUME_NAME)
            .withNewEmptyDir()
            .withSizeLimit(new Quantity(String.valueOf(Math.multiplyExact(maxTransferBytes, 2L))))
            .endEmptyDir()
            .endVolume()
            .withInitContainers(
                fileSidecar(INIT_CONTAINER, settings.fileSidecarImage(), "until [ -f " + READY_MARKER +
                    " ]; do sleep 1; done"))
            .withContainers(
                main(spec, settings),
                fileSidecar(OUTPUT_CONTAINER, settings.fileSidecarImage(), "until [ -f " + DONE_MARKER +
                    " ]; do sleep 1; done"))
            .endSpec();

        return podBuilder.build();
    }

    static Secret pullSecret(String namespace, RemoteJobKey key, KubernetesRunnerSettings.RegistryCredentials credentials) {
        Map<String, Object> registryEntry = new LinkedHashMap<>();

        registryEntry.put("username", credentials.username());
        registryEntry.put("password", credentials.password());
        registryEntry.put("auth", EncodingUtils.base64EncodeToString(credentials.username() + ":" + credentials.password()));

        String dockerConfigJson = JsonUtils.write(Map.of("auths", Map.of(credentials.registry(), registryEntry)));

        SecretBuilder secretBuilder = new SecretBuilder();

        secretBuilder.withNewMetadata()
            .withGenerateName(PULL_SECRET_PREFIX)
            .withNamespace(namespace)
            .withLabels(KubernetesJobLabels.labels(key))
            .endMetadata();

        secretBuilder.withType("kubernetes.io/dockerconfigjson");
        secretBuilder.withData(Map.of(".dockerconfigjson", EncodingUtils.base64EncodeToString(dockerConfigJson)));

        return secretBuilder.build();
    }

    private static Container fileSidecar(String name, String image, String script) {
        return new ContainerBuilder()
            .withName(name)
            .withImage(image)
            .withImagePullPolicy("IfNotPresent")
            .withCommand("sh", "-c", script)
            .withVolumeMounts(workMount())
            .withResources(
                new ResourceRequirementsBuilder()
                    .withRequests(Map.of("cpu", new Quantity("10m"), "memory", new Quantity("16Mi")))
                    .withLimits(Map.of("cpu", new Quantity("200m"), "memory", new Quantity("64Mi")))
                    .build())
            .build();
    }

    private static Container main(RemoteJobSpec spec, KubernetesRunnerSettings settings) {
        List<String> command = new ArrayList<>(List.of("sh", "-c", "exec \"$@\" 2>" + spec.remoteStderrPath(), "sh"));

        command.addAll(spec.interpreter());
        command.add(spec.remoteSourcePath());

        List<EnvVar> environment = spec.environment()
            .entrySet()
            .stream()
            .map(entry -> new EnvVar(entry.getKey(), entry.getValue(), null))
            .toList();

        Map<String, Quantity> requests = new LinkedHashMap<>();
        Map<String, Quantity> limits = new LinkedHashMap<>();

        settings.resourceRequests()
            .forEach((resourceName, value) -> requests.put(resourceName, new Quantity(value)));
        settings.resourceLimits()
            .forEach((resourceName, value) -> limits.put(resourceName, new Quantity(value)));

        return new ContainerBuilder()
            .withName(MAIN_CONTAINER)
            .withImage(settings.image())
            .withImagePullPolicy(pullPolicy(spec.pullPolicy()))
            .withCommand(command)
            .withWorkingDir(WORK_DIRECTORY)
            .withEnv(environment)
            .withVolumeMounts(workMount())
            .withResources(new ResourceRequirementsBuilder()
                .withRequests(requests)
                .withLimits(limits)
                .build())
            .build();
    }

    private static String pullPolicy(RemotePullPolicy pullPolicy) {
        return switch (pullPolicy) {
            case ALWAYS -> "Always";
            case IF_NOT_PRESENT -> "IfNotPresent";
            case NEVER -> "Never";
        };
    }

    private static VolumeMount workMount() {
        return new VolumeMountBuilder()
            .withName(VOLUME_NAME)
            .withMountPath(WORK_DIRECTORY)
            .build();
    }
}
```

`spec.remoteStderrPath()` is `/bytechef/work/.stderr` because the backend's remote working directory is `/bytechef/work`. The test's `RemoteJobSpec` uses the same directory. If a fabric8 builder method name differs in 7.x (for example `withNewEmptyDir`), use that version's equivalent fluent method and say so in the report. Fabric8 builders are a sanctioned chaining exception (builder pattern).

- [ ] **Step 6: Run tests**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-component/platform-component-runner-kubernetes
git commit -m "Build the kubernetes runner's pod"
```

---
### Task 5: The backend over a cluster seam (EE)

**Files:**
- Create: `K8S_PKG/KubernetesOperations.java` (the seam), `K8S_PKG/KubernetesOperationsFactory.java`
- Create: `K8S_PKG/KubernetesTaskRunnerBackend.java`
- Test: `K8S_TEST/KubernetesTaskRunnerBackendTest.java`, `K8S_TEST/FakeKubernetesOperations.java`

**Interfaces:**
- Consumes:
  - Task 1's `RemoteJobContext.runnerParameters()`, `maxTransferBytes()` and `completionBudget()`.
  - Task 3's `KubernetesConnectionSettings.of/validate`.
  - Task 4's `KubernetesRunnerSettings.of/namespaceOr`, `KubernetesRunnerSettings.NAMESPACE`, `KubernetesPodFactory.pod/pullSecret` and its constants, and `KubernetesJobLabels.*`.
  - The SPI `RemoteTaskRunnerBackend`, `TaskRunnerConnection("kubernetes", 1)`, `CleanupMode`, `Deadline`, `RemoteJobExit`, `RemoteJobHandle`, `RemoteLogSink`.
- Produces:
  - `interface KubernetesOperations extends AutoCloseable` (the exact methods below; Task 6 implements it);
  - `interface KubernetesOperationsFactory { KubernetesOperations create(KubernetesConnectionSettings connectionSettings, Duration requestTimeout); }`;
  - `KubernetesTaskRunnerBackend(KubernetesOperationsFactory operationsFactory, Duration pollInterval)`, package-private. Task 6 adds the public Spring constructor and the bean annotations.
  - Handle attribute names `NAMESPACE_ATTRIBUTE = "namespace"`, `KILL_GRACE_PERIOD` (from `TaskRunnerConstants`) and `PULL_SECRET_ATTRIBUTE = "pullSecret"`.

The full backend lifecycle:
- **`submit`:** create the pull secret (if any), create the pod, wait for `bytechef-init` to run (failing fast on image or config errors), `tar -xf -` the inputs into it, then `touch .ready`. If anything fails, delete what was created.
- **`await`:**
  - **Phase 1:** wait for `main` to run, failing fast on `ErrImagePull`, `ImagePullBackOff`, `InvalidImageName`, `CreateContainerConfigError` or `CreateContainerError`. An evicted pod is a failure. When `runningDeadline` passes, fail with "did not start within …" plus the latest warning event.
  - **Phase 2:** follow `main`'s logs into stdout until it terminates (exit code, and its reason unless `Completed`), the pod is evicted, or `completionDeadline` passes (`timedOut()`).
- **`collect`:** run `tar -cf -` of `output` and `.stderr` in `bytechef-output` into a temp file capped at twice the transfer ceiling, then best-effort `touch .done`.
- **`find`:** list pods by the selector. Ignore those whose `bytechef.io/key` annotation differs. Return the first whose init container exited 0 and whose output sidecar is still running. Delete every other match, and its pull secret.
- **`cleanup`:** `DETACH` does nothing. `DELETE` deletes with grace 0. `KILL_AND_DELETE` deletes with the pod's `killGracePeriod`. Both also delete the pull secret. Cleanup never throws.

- [ ] **Step 1: The seam**

`KubernetesOperations.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The cluster calls the Kubernetes runner makes, kept narrow so the runner can be tested without a cluster.
 *
 * @author Ivica Cardic
 * @version ee
 */
interface KubernetesOperations extends AutoCloseable {

    /**
     * Creates the secret and returns it as stored, with its generated name.
     */
    Secret createSecret(Secret secret);

    /**
     * Creates the pod and returns it as stored, with its generated name.
     */
    Pod createPod(Pod pod);

    @Nullable
    Pod getPod(String namespace, String name);

    List<Pod> listPods(String namespace, Map<String, String> labels);

    void deletePod(String namespace, String name, Duration gracePeriod);

    void deleteSecret(String namespace, String name);

    /**
     * Runs a command in a container, streaming {@code stdin} (when given) to it and its stdout to {@code stdout}, and
     * waits up to {@code timeout} for it to exit.
     */
    ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException, InterruptedException;

    /**
     * Follows a container's log into {@code outputStream} until the container ends or the stream is closed.
     */
    LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream);

    /**
     * Returns the message of the most recent warning event about the pod, such as a scheduling failure.
     */
    @Nullable
    String lastWarningEvent(String namespace, String podName);

    @Override
    void close();

    record ExecResult(int exitCode, String stderr) {
    }

    interface LogStream extends Closeable {

        /**
         * Waits up to {@code timeout} for the log to reach its end; returns whether it did.
         */
        boolean awaitCompletion(Duration timeout) throws InterruptedException;

        @Override
        void close();
    }
}
```

`KubernetesOperationsFactory.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import java.time.Duration;

/**
 * @author Ivica Cardic
 * @version ee
 */
@FunctionalInterface
interface KubernetesOperationsFactory {

    KubernetesOperations create(KubernetesConnectionSettings connectionSettings, Duration requestTimeout);
}
```

- [ ] **Step 2: The test double**

`FakeKubernetesOperations.java` (test source) is an in-memory cluster driven by scripted pod snapshots. Its API:
- `podSnapshots` (a queue of pod statuses served in order by `getPod`; the last one repeats);
- `listedPods`;
- `execResults` (a queue of exit codes per call);
- `execCalls` (recorded);
- `logText`;
- `deletedPods` (name → grace);
- `deletedSecrets`;
- `createdSecrets` and `createdPods`;
- `failCreatePod`;
- `failDeletePod`;
- `warningEvent`;
- `collectBytes` (written to stdout for the collect exec);
- `requestTimeouts` (recorded by the factory).

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStateBuilder;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

final class FakeKubernetesOperations implements KubernetesOperations {

    static final String POD_NAME = "bytechef-run-abc12";
    static final String SECRET_NAME = "bytechef-pull-xyz34";

    final Deque<Pod> podSnapshots = new ArrayDeque<>();
    final Deque<Integer> execResults = new ArrayDeque<>();
    final List<ExecCall> execCalls = new ArrayList<>();
    final Map<String, Duration> deletedPods = new LinkedHashMap<>();
    final List<String> deletedSecrets = new ArrayList<>();
    final List<Secret> createdSecrets = new ArrayList<>();
    final List<Pod> createdPods = new ArrayList<>();
    final List<Duration> requestTimeouts = new ArrayList<>();

    List<Pod> listedPods = List.of();
    String logText = "";
    byte[] collectBytes = new byte[0];
    @Nullable String warningEvent;
    @Nullable RuntimeException failCreatePod;
    boolean failDeletePod;
    int closeCount;
    int closedLogs;
    @Nullable Map<String, String> listedLabels;
    @Nullable String listedNamespace;
    @Nullable Pod lastServed;

    KubernetesOperationsFactory factory() {
        return (connectionSettings, requestTimeout) -> {
            requestTimeouts.add(requestTimeout);

            return this;
        };
    }

    @Override
    public Secret createSecret(Secret secret) {
        createdSecrets.add(secret);

        return new SecretBuilder(secret).editMetadata()
            .withName(SECRET_NAME)
            .endMetadata()
            .build();
    }

    @Override
    public Pod createPod(Pod pod) {
        if (failCreatePod != null) {
            throw failCreatePod;
        }

        createdPods.add(pod);

        return new PodBuilder(pod).editMetadata()
            .withName(POD_NAME)
            .endMetadata()
            .build();
    }

    @Override
    public @Nullable Pod getPod(String namespace, String name) {
        if (podSnapshots.size() > 1) {
            lastServed = podSnapshots.poll();
        } else {
            lastServed = podSnapshots.peek();
        }

        return lastServed;
    }

    @Override
    public List<Pod> listPods(String namespace, Map<String, String> labels) {
        listedNamespace = namespace;
        listedLabels = labels;

        return listedPods;
    }

    @Override
    public void deletePod(String namespace, String name, Duration gracePeriod) {
        if (failDeletePod) {
            throw new KubernetesClientException("boom");
        }

        deletedPods.put(name, gracePeriod);
    }

    @Override
    public void deleteSecret(String namespace, String name) {
        deletedSecrets.add(name);
    }

    @Override
    public ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException {

        byte[] input = stdin == null ? null : stdin.readAllBytes();

        execCalls.add(new ExecCall(container, command, input));

        if (String.join(" ", command)
            .contains("tar -cf -")) {

            stdout.write(collectBytes);
        }

        Integer exitCode = execResults.poll();

        return new ExecResult(exitCode == null ? 0 : exitCode, exitCode == null || exitCode == 0 ? "" : "tar: error");
    }

    @Override
    public LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream) {
        try {
            outputStream.write(logText.getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

        return new LogStream() {

            @Override
            public boolean awaitCompletion(Duration timeout) {
                return true;
            }

            @Override
            public void close() {
                closedLogs++;
            }
        };
    }

    @Override
    public @Nullable String lastWarningEvent(String namespace, String podName) {
        return warningEvent;
    }

    @Override
    public void close() {
        closeCount++;
    }

    record ExecCall(String container, List<String> command, byte @Nullable [] stdin) {
    }

    static Pod pod(
        @Nullable ContainerState init, @Nullable ContainerState main, @Nullable ContainerState output,
        Map<String, String> annotations) {

        List<ContainerStatus> initStatuses = new ArrayList<>();
        List<ContainerStatus> statuses = new ArrayList<>();

        if (init != null) {
            initStatuses.add(status("bytechef-init", init));
        }

        if (main != null) {
            statuses.add(status("main", main));
        }

        if (output != null) {
            statuses.add(status("bytechef-output", output));
        }

        return new PodBuilder().withNewMetadata()
            .withName(POD_NAME)
            .withNamespace("jobs")
            .withAnnotations(annotations)
            .endMetadata()
            .withNewStatus()
            .withPhase("Running")
            .withInitContainerStatuses(initStatuses)
            .withContainerStatuses(statuses)
            .endStatus()
            .build();
    }

    static ContainerState running() {
        return new ContainerStateBuilder().withNewRunning()
            .endRunning()
            .build();
    }

    static ContainerState waiting(String reason, String message) {
        return new ContainerStateBuilder().withNewWaiting()
            .withReason(reason)
            .withMessage(message)
            .endWaiting()
            .build();
    }

    static ContainerState terminated(int exitCode, String reason) {
        return new ContainerStateBuilder().withNewTerminated()
            .withExitCode(exitCode)
            .withReason(reason)
            .endTerminated()
            .build();
    }

    private static ContainerStatus status(String name, ContainerState state) {
        return new ContainerStatusBuilder().withName(name)
            .withState(state)
            .build();
    }
}
```

(The counters keep `close()` bodies non-empty for Checkstyle's empty-block rule and let a test assert that the backend closes what it opens.)

- [ ] **Step 3: Write the failing backend tests**

`KubernetesTaskRunnerBackendTest.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.POD_NAME;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.SECRET_NAME;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.pod;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.running;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.terminated;
import static com.bytechef.ee.platform.component.runner.kubernetes.FakeKubernetesOperations.waiting;
import static org.assertj.core.api.Assertions.assertThat;
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
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class KubernetesTaskRunnerBackendTest {

    private static final RemoteJobKey KEY = new RemoteJobKey("public-100-7", 100L);
    private static final Map<String, String> KEY_ANNOTATION = Map.of("bytechef.io/key", "public-100-7");

    private final FakeKubernetesOperations operations = new FakeKubernetesOperations();
    private final KubernetesTaskRunnerBackend backend = new KubernetesTaskRunnerBackend(
        operations.factory(), Duration.ofMillis(5));

    @Test
    void testIdentity() {
        assertThat(backend.getType()).isEqualTo("kubernetes");
        assertThat(backend.getTitle()).isEqualTo("Kubernetes");
        assertThat(backend.getRemoteWorkingDirectory()).isEqualTo("/bytechef/work");
        assertThat(backend.getConnection()).hasValueSatisfying(connection -> {
            assertThat(connection.componentName()).isEqualTo("kubernetes");
            assertThat(connection.componentVersion()).isEqualTo(1);
        });
    }

    @Test
    void testValidateChecksTheConnectionAndTheSettings() {
        assertThatThrownBy(() -> backend.validate(request(Map.of()), context(Map.of(), Map.of("masterUrl", "x"))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("https://");

        assertThatThrownBy(
            () -> backend.validate(request(Map.of("killGracePeriod", "soon")), context(Map.of(), validConnection())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("killGracePeriod");

        backend.validate(request(Map.of()), context(Map.of(), validConnection()));
    }

    @Test
    void testSubmitCreatesThePodDeliversTheInputsAndMarksItReady() throws Exception {
        operations.podSnapshots.add(pod(waiting("PodInitializing", ""), null, null, KEY_ANNOTATION));
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));

        byte[] archive = "tar-bytes".getBytes(StandardCharsets.UTF_8);

        Map<String, ?> runnerParameters = Map.of("namespace", "jobs", "killGracePeriod", "PT10S");

        RemoteJobHandle handle = backend.submit(
            spec(runnerParameters), new ByteArrayInputStream(archive), Deadline.after(Duration.ofMinutes(1)),
            context(runnerParameters, validConnection()));

        assertThat(handle.id()).isEqualTo(POD_NAME);
        assertThat(handle.attributes()).containsEntry("namespace", "jobs")
            .containsEntry("killGracePeriod", "PT10S");

        Pod created = operations.createdPods.getFirst();

        assertThat(created.getMetadata()
            .getNamespace()).isEqualTo("jobs");
        assertThat(created.getSpec()
            .getActiveDeadlineSeconds()).isEqualTo(3600L + 60);

        assertThat(operations.execCalls).hasSize(2);
        assertThat(operations.execCalls.get(0)
            .container()).isEqualTo("bytechef-init");
        assertThat(operations.execCalls.get(0)
            .command()).containsExactly("tar", "-xf", "-", "-C", "/bytechef/work");
        assertThat(operations.execCalls.get(0)
            .stdin()).isEqualTo(archive);
        assertThat(operations.execCalls.get(1)
            .command()).containsExactly("touch", "/bytechef/work/.ready");
    }

    @Test
    void testSubmitCreatesAPullSecretFirstWhenCredentialsAreGiven() throws Exception {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r.example.com", "username", "u", "password", "p"));

        RemoteJobHandle handle = backend.submit(
            spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
            context(runnerParameters, validConnection()));

        assertThat(operations.createdSecrets).hasSize(1);
        assertThat(operations.createdPods.getFirst()
            .getMetadata()
            .getAnnotations()).containsEntry("bytechef.io/pull-secret", SECRET_NAME);
        assertThat(handle.attributes()).containsEntry("pullSecret", SECRET_NAME);
    }

    @Test
    void testSubmitCleansUpWhenDeliveringTheInputsFails() {
        operations.podSnapshots.add(pod(running(), null, null, KEY_ANNOTATION));
        operations.execResults.add(2);

        Map<String, ?> runnerParameters = Map.of(
            "credentials", Map.of("registry", "r", "username", "u", "password", "p"));

        assertThatThrownBy(
            () -> backend.submit(
                spec(runnerParameters), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(runnerParameters, validConnection())))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("delivering the inputs")
                        .hasMessageContaining("2");

        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testAnImagePullFailureFailsFast() {
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), waiting("ErrImagePull", "pull access denied for nope/nope"),
                running(), KEY_ANNOTATION));

        long startNanos = System.nanoTime();

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMinutes(10)), Deadline.after(Duration.ofHours(1)), sink(),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ErrImagePull")
                    .hasMessageContaining("pull access denied");

        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void testAPodThatNeverStartsFailsWithTheLatestWarningEvent() {
        operations.podSnapshots.add(pod(waiting("PodInitializing", ""), null, null, KEY_ANNOTATION));
        operations.warningEvent = "0/3 nodes are available: 3 Insufficient memory.";

        assertThatThrownBy(
            () -> backend.await(
                handle(), Deadline.after(Duration.ofMillis(50)), Deadline.after(Duration.ofHours(1)), sink(),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("did not start")
                    .hasMessageContaining("Insufficient memory");
    }

    @Test
    void testASuccessfulJobFollowsItsLogsAndReturnsTheExitCode() throws Exception {
        operations.logText = "hello from the pod";
        operations.podSnapshots.add(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION));
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), terminated(0, "Completed"), running(), KEY_ANNOTATION));

        CollectingSink logSink = new CollectingSink();

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), logSink,
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isZero();
        assertThat(exit.reason()).isNull();
        assertThat(logSink.stdout.toString()).contains("hello from the pod");
    }

    @Test
    void testAnOomKilledContainerReportsItsReason() throws Exception {
        operations.podSnapshots.add(
            pod(terminated(0, "Completed"), terminated(137, "OOMKilled"), running(), KEY_ANNOTATION));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.exitCode()).isEqualTo(137);
        assertThat(exit.reason()).isEqualTo("OOMKilled");
    }

    @Test
    void testAnEvictedPodIsAFailureNamingTheEviction() throws Exception {
        Pod evicted = new PodBuilder(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION))
            .editStatus()
            .withPhase("Failed")
            .withReason("Evicted")
            .withMessage("The node was low on resource: memory.")
            .endStatus()
            .build();

        operations.podSnapshots.add(evicted);

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMinutes(1)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isTrue();
        assertThat(exit.exitCode()).isNull();
        assertThat(exit.reason()).contains("Evicted")
            .contains("low on resource");
    }

    @Test
    void testAwaitTimesOutWhenTheJobOutlivesTheCompletionDeadline() throws Exception {
        operations.podSnapshots.add(pod(terminated(0, "Completed"), running(), running(), KEY_ANNOTATION));

        RemoteJobExit exit = backend.await(
            handle(), Deadline.after(Duration.ofMinutes(1)), Deadline.after(Duration.ofMillis(50)), sink(),
            context(Map.of(), validConnection()));

        assertThat(exit.finished()).isFalse();
    }

    @Test
    void testCollectTarsTheOutputsFromTheSidecarAndMarksItDone() throws Exception {
        operations.collectBytes = "output-tar".getBytes(StandardCharsets.UTF_8);

        try (InputStream inputStream = backend.collect(
            handle(), Deadline.after(Duration.ofMinutes(1)), context(Map.of(), validConnection()))) {

            assertThat(inputStream).isNotNull();
            assertThat(inputStream.readAllBytes()).isEqualTo(operations.collectBytes);
        }

        assertThat(operations.execCalls.get(0)
            .container()).isEqualTo("bytechef-output");
        assertThat(String.join(" ", operations.execCalls.get(0)
            .command())).contains("tar -cf - output");
        assertThat(operations.execCalls.get(1)
            .command()).containsExactly("touch", "/bytechef/work/.done");
    }

    @Test
    void testCollectFailsWhenTheSidecarCannotTarTheOutputs() {
        operations.execResults.add(1);

        assertThatThrownBy(
            () -> backend.collect(handle(), Deadline.after(Duration.ofMinutes(1)), context(Map.of(), validConnection())))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("1");
    }

    @Test
    void testFindReturnsAPodPastTheInputStageWithItsSidecarRunning() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), running(), running(), annotations("PT10S", null)));

        Optional<RemoteJobHandle> handle = backend.find(KEY, context(Map.of(), validConnection()));

        assertThat(handle).map(RemoteJobHandle::id)
            .contains(POD_NAME);
        assertThat(handle.orElseThrow()
            .attributes()).containsEntry("killGracePeriod", "PT10S");
        assertThat(operations.listedLabels).containsEntry("bytechef.io/managed-by", "bytechef")
            .containsEntry("bytechef.io/task-execution-id", "public-100-7");
        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testFindDeletesAPodStillWaitingForItsInputs() {
        operations.listedPods = List.of(pod(running(), null, null, annotations(null, SECRET_NAME)));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).containsKey(POD_NAME);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);
    }

    @Test
    void testFindDeletesAPodWhoseOutputSidecarAlreadyExited() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), terminated(0, "Completed"), terminated(0, "Completed"), KEY_ANNOTATION));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).containsKey(POD_NAME);
    }

    @Test
    void testFindIgnoresAPodWhoseKeyAnnotationDiffers() {
        operations.listedPods = List.of(
            pod(terminated(0, "Completed"), running(), running(), Map.of("bytechef.io/key", "other-tenant-100-7")));

        assertThat(backend.find(KEY, context(Map.of(), validConnection()))).isEmpty();
        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testFindUsesTheTasksNamespaceOverTheConnections() {
        backend.find(KEY, context(Map.of("namespace", "task-namespace"), validConnection()));

        assertThat(operations.listedNamespace).isEqualTo("task-namespace");

        backend.find(KEY, context(Map.of(), validConnection()));

        assertThat(operations.listedNamespace).isEqualTo("jobs");
    }

    @Test
    void testDeleteRemovesThePodAtOnceAndKillHonoursTheGracePeriod() {
        RemoteJobHandle handle = new RemoteJobHandle(
            POD_NAME, Map.of("namespace", "jobs", "killGracePeriod", "PT10S", "pullSecret", SECRET_NAME));

        backend.cleanup(handle, CleanupMode.DELETE, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).containsEntry(POD_NAME, Duration.ZERO);
        assertThat(operations.deletedSecrets).containsExactly(SECRET_NAME);

        operations.deletedPods.clear();

        backend.cleanup(handle, CleanupMode.KILL_AND_DELETE, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).containsEntry(POD_NAME, Duration.ofSeconds(10));

        operations.deletedPods.clear();

        backend.cleanup(handle, CleanupMode.DETACH, context(Map.of(), validConnection()));

        assertThat(operations.deletedPods).isEmpty();
    }

    @Test
    void testCleanupNeverThrows() {
        operations.failDeletePod = true;

        backend.cleanup(handle(), CleanupMode.KILL_AND_DELETE, context(Map.of(), validConnection()));
    }

    @Test
    void testAClientErrorIsWrappedWithThePhaseAndNoSecrets() {
        Status status = new StatusBuilder().withCode(403)
            .withReason("Forbidden")
            .build();

        operations.failCreatePod = new KubernetesClientException("pods is forbidden for token secret-token", 403, status);

        assertThatThrownBy(
            () -> backend.submit(
                spec(), new ByteArrayInputStream(new byte[0]), Deadline.after(Duration.ofMinutes(1)),
                context(Map.of(), validConnection())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("creating the pod")
                    .hasMessageContaining("HTTP 403")
                    .hasMessageContaining("Forbidden")
                    .satisfies(exception -> assertThat(exception.getMessage()).doesNotContain("secret-token"));
    }

    private static Map<String, String> annotations(String killGracePeriod, String pullSecret) {
        Map<String, String> annotations = new HashMap<>(KEY_ANNOTATION);

        if (killGracePeriod != null) {
            annotations.put("bytechef.io/kill-grace-period", killGracePeriod);
        }

        if (pullSecret != null) {
            annotations.put("bytechef.io/pull-secret", pullSecret);
        }

        return annotations;
    }

    private static RemoteJobContext context(Map<String, ?> runnerParameters, Map<String, ?> connectionParameters) {
        return new RemoteJobContext(
            new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(connectionParameters), null), Map.of(), null,
            ParametersFactory.create(runnerParameters), 1024L * 1024, Duration.ofHours(1));
    }

    private static RemoteJobHandle handle() {
        return new RemoteJobHandle(POD_NAME, Map.of("namespace", "jobs"));
    }

    private static TaskRunnerRequest request(Map<String, ?> runnerParameters) {
        return new TaskRunnerRequest(
            "python", "def perform(input, context):\n    return 1\n", List.of(), Map.of(), Map.of(), Map.of(),
            List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, Map.of(),
            mock(ActionContext.class));
    }

    private static RemoteLogSink sink() {
        return new CollectingSink();
    }

    private static RemoteJobSpec spec() {
        return spec(Map.of());
    }

    private static RemoteJobSpec spec(Map<String, ?> runnerParameters) {
        return new RemoteJobSpec(
            KEY, "python", "script.py", List.of("python3"), Map.of("HOME", "/bytechef/work"), "/bytechef/work",
            RemotePullPolicy.IF_NOT_PRESENT, ParametersFactory.create(runnerParameters));
    }

    private static Map<String, ?> validConnection() {
        return Map.of("masterUrl", "https://cluster:6443", "token", "secret-token", "namespace", "jobs");
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

The backend reads runner settings in `submit` from `spec.runnerParameters()`, and in `find` and `cleanup` from `context.runnerParameters()`. In production both hold the same task parameters, so the submit tests pass them to both.

- [ ] **Step 4: Run, expect compilation failure**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: FAIL (backend missing).

- [ ] **Step 5: Implement the backend**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.DONE_MARKER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.INIT_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.MAIN_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.OUTPUT_CONTAINER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.READY_MARKER;
import static com.bytechef.ee.platform.component.runner.kubernetes.KubernetesPodFactory.WORK_DIRECTORY;
import static com.bytechef.platform.component.runner.TaskRunnerConstants.KILL_GRACE_PERIOD;

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
import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Status;
import io.fabric8.kubernetes.client.KubernetesClientException;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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

/**
 * Runs a task as a pod on the cluster the task's {@code kubernetes} connection names.
 *
 * @author Ivica Cardic
 * @version ee
 */
public class KubernetesTaskRunnerBackend implements RemoteTaskRunnerBackend {

    static final String NAMESPACE_ATTRIBUTE = "namespace";
    static final String PULL_SECRET_ATTRIBUTE = "pullSecret";
    static final String TYPE = "kubernetes";

    private static final Logger log = LoggerFactory.getLogger(KubernetesTaskRunnerBackend.class);

    private static final Duration CLIENT_GRACE = Duration.ofSeconds(30);
    private static final String COLLECT_SCRIPT =
        "cd " + WORK_DIRECTORY + " && if [ -f .stderr ]; then tar -cf - output .stderr; else tar -cf - output; fi";
    private static final Set<String> FAIL_FAST_REASONS = Set.of(
        "ErrImagePull", "ImagePullBackOff", "InvalidImageName", "CreateContainerConfigError", "CreateContainerError");
    private static final Duration LOG_DRAIN_GRACE = Duration.ofSeconds(5);

    private final KubernetesOperationsFactory operationsFactory;
    private final Duration pollInterval;

    KubernetesTaskRunnerBackend(KubernetesOperationsFactory operationsFactory, Duration pollInterval) {
        this.operationsFactory = operationsFactory;
        this.pollInterval = pollInterval;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTitle() {
        return "Kubernetes";
    }

    @Override
    public String getRemoteWorkingDirectory() {
        return WORK_DIRECTORY;
    }

    @Override
    public List<? extends ModifiableValueProperty<?, ?>> getProperties(Map<String, String> operatorProperties) {
        return KubernetesRunnerSettings.properties();
    }

    @Override
    public Optional<TaskRunnerConnection> getConnection() {
        return Optional.of(new TaskRunnerConnection(TYPE, 1));
    }

    @Override
    public void validate(TaskRunnerRequest request, RemoteJobContext context) {
        connectionSettings(context).validate();

        KubernetesRunnerSettings.of(request.languageId(), request.runnerParameters());
    }

    @Override
    public Optional<RemoteJobHandle> find(RemoteJobKey key, RemoteJobContext context) {
        KubernetesConnectionSettings connectionSettings = connectionSettings(context);

        String namespace = namespace(context.runnerParameters(), connectionSettings);

        try (KubernetesOperations operations = operationsFactory.create(connectionSettings, CLIENT_GRACE)) {
            RemoteJobHandle resumable = null;

            for (Pod pod : operations.listPods(namespace, KubernetesJobLabels.selector(key))) {
                Map<String, String> annotations = annotations(pod);

                if (!Objects.equals(annotations.get(KubernetesJobLabels.ANNOTATION_KEY), key.value())) {
                    continue;
                }

                if (resumable == null && isPastInputStage(pod) && isRunning(pod, OUTPUT_CONTAINER)) {
                    resumable = handle(pod, namespace);
                } else {
                    deleteQuietly(operations, namespace, pod.getMetadata()
                        .getName(), annotations.get(KubernetesJobLabels.ANNOTATION_PULL_SECRET), Duration.ZERO);
                }
            }

            return Optional.ofNullable(resumable);
        } catch (KubernetesClientException exception) {
            throw failure("looking for a job to resume", exception);
        }
    }

    @Override
    public RemoteJobHandle submit(
        RemoteJobSpec spec, InputStream inputArchive, Deadline runningDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        KubernetesConnectionSettings connectionSettings = connectionSettings(context);
        KubernetesRunnerSettings runnerSettings = KubernetesRunnerSettings.of(
            spec.languageId(), spec.runnerParameters());

        String namespace = runnerSettings.namespaceOr(connectionSettings.namespace());

        try (KubernetesOperations operations = operationsFactory.create(
            connectionSettings, runningDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            String pullSecretName = null;
            String podName = null;

            try {
                KubernetesRunnerSettings.RegistryCredentials credentials = runnerSettings.credentials();

                if (credentials != null) {
                    Secret secret = operations.createSecret(
                        KubernetesPodFactory.pullSecret(namespace, spec.key(), credentials));

                    pullSecretName = secret.getMetadata()
                        .getName();
                }

                Pod pod = createPod(operations, spec, runnerSettings, namespace, context, pullSecretName);

                podName = pod.getMetadata()
                    .getName();

                awaitInitContainer(operations, namespace, podName, runningDeadline);

                deliverInputs(operations, namespace, podName, inputArchive, runningDeadline);

                return handle(pod, namespace);
            } catch (IOException | InterruptedException | RuntimeException exception) {
                if (podName != null) {
                    deleteQuietly(operations, namespace, podName, null, Duration.ZERO);
                }

                if (pullSecretName != null) {
                    deleteSecretQuietly(operations, namespace, pullSecretName);
                }

                throw exception;
            }
        }
    }

    @Override
    public RemoteJobExit await(
        RemoteJobHandle handle, Deadline runningDeadline, Deadline completionDeadline, RemoteLogSink logSink,
        RemoteJobContext context) throws InterruptedException {

        String namespace = handleNamespace(handle);

        try (KubernetesOperations operations = operationsFactory.create(
            connectionSettings(context), completionDeadline.remaining()
                .plus(CLIENT_GRACE))) {

            RemoteJobExit evicted = awaitMainStarted(operations, namespace, handle.id(), runningDeadline);

            if (evicted != null) {
                return evicted;
            }

            KubernetesOperations.LogStream logStream = operations.followLogs(
                namespace, handle.id(), MAIN_CONTAINER, new SinkOutputStream(logSink));

            boolean finished = false;

            try {
                RemoteJobExit exit = awaitMainEnded(operations, namespace, handle.id(), completionDeadline);

                finished = exit.finished();

                return exit;
            } finally {
                if (finished) {
                    logStream.awaitCompletion(LOG_DRAIN_GRACE);
                }

                logStream.close();
            }
        } catch (KubernetesClientException exception) {
            throw failure("waiting for the job", exception);
        }
    }

    @Override
    public @Nullable InputStream collect(RemoteJobHandle handle, Deadline completionDeadline, RemoteJobContext context)
        throws IOException, InterruptedException {

        String namespace = handleNamespace(handle);

        Duration timeout = completionDeadline.remaining()
            .plus(CLIENT_GRACE);

        Path archivePath = Files.createTempFile("bytechef-kubernetes-output-" + UUID.randomUUID() + "-", ".tar");

        try (KubernetesOperations operations = operationsFactory.create(connectionSettings(context), timeout)) {
            KubernetesOperations.ExecResult execResult;

            try (OutputStream outputStream = new CeilingOutputStream(
                Files.newOutputStream(archivePath), Math.multiplyExact(context.maxTransferBytes(), 2L))) {

                execResult = operations.exec(
                    namespace, handle.id(), OUTPUT_CONTAINER, List.of("sh", "-c", COLLECT_SCRIPT), null, outputStream,
                    timeout);
            }

            if (execResult.exitCode() != 0) {
                throw new IOException(
                    "The kubernetes runner could not collect the outputs: tar exited with code %d: %s".formatted(
                        execResult.exitCode(), execResult.stderr()));
            }

            markDone(operations, namespace, handle.id());

            return selfDeleting(archivePath);
        } catch (KubernetesClientException exception) {
            Files.deleteIfExists(archivePath);

            throw failure("collecting the outputs", exception);
        } catch (IOException | InterruptedException | RuntimeException exception) {
            Files.deleteIfExists(archivePath);

            throw exception;
        }
    }

    @Override
    public void cleanup(RemoteJobHandle handle, CleanupMode cleanupMode, RemoteJobContext context) {
        if (cleanupMode == CleanupMode.DETACH) {
            return;
        }

        try {
            Duration gracePeriod = cleanupMode == CleanupMode.KILL_AND_DELETE
                ? Duration.parse(handle.attributes()
                    .getOrDefault(KILL_GRACE_PERIOD, "PT0S"))
                : Duration.ZERO;

            try (KubernetesOperations operations = operationsFactory.create(
                connectionSettings(context), CLIENT_GRACE)) {

                deleteQuietly(
                    operations, handleNamespace(handle), handle.id(), handle.attributes()
                        .get(PULL_SECRET_ATTRIBUTE),
                    gracePeriod);
            }
        } catch (RuntimeException exception) {
            log.warn("Could not clean up the kubernetes job {}", handle.id(), exception);
        }
    }

    private RemoteJobExit awaitMainEnded(
        KubernetesOperations operations, String namespace, String podName, Deadline completionDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            RemoteJobExit evicted = evicted(pod);

            if (evicted != null) {
                return evicted;
            }

            ContainerState state = state(pod, MAIN_CONTAINER, false);

            if (state != null && state.getTerminated() != null) {
                Integer exitCode = state.getTerminated()
                    .getExitCode();

                String reason = state.getTerminated()
                    .getReason();

                return new RemoteJobExit(true, exitCode, "Completed".equals(reason) ? null : reason);
            }

            if (completionDeadline.isExpired()) {
                return RemoteJobExit.timedOut();
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    @Nullable
    private RemoteJobExit awaitMainStarted(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            RemoteJobExit evicted = evicted(pod);

            if (evicted != null) {
                return evicted;
            }

            failFast(pod);

            ContainerState state = state(pod, MAIN_CONTAINER, false);

            if (state != null && (state.getRunning() != null || state.getTerminated() != null)) {
                return null;
            }

            if (runningDeadline.isExpired()) {
                throw notStarted(operations, namespace, podName, runningDeadline);
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    private void awaitInitContainer(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline)
        throws InterruptedException {

        while (true) {
            Pod pod = requirePod(operations, namespace, podName);

            failFast(pod);

            ContainerState state = state(pod, INIT_CONTAINER, true);

            if (state != null && state.getRunning() != null) {
                return;
            }

            if (state != null && state.getTerminated() != null) {
                throw new IllegalStateException(
                    "The kubernetes runner's file sidecar ended before it received the inputs: %s".formatted(
                        state.getTerminated()
                            .getReason()));
            }

            if (runningDeadline.isExpired()) {
                throw notStarted(operations, namespace, podName, runningDeadline);
            }

            Thread.sleep(pollInterval.toMillis());
        }
    }

    private Pod createPod(
        KubernetesOperations operations, RemoteJobSpec spec, KubernetesRunnerSettings runnerSettings,
        String namespace, RemoteJobContext context, @Nullable String pullSecretName) {

        try {
            return operations.createPod(
                KubernetesPodFactory.pod(
                    spec, runnerSettings, namespace, context.completionBudget(), context.maxTransferBytes(),
                    pullSecretName));
        } catch (KubernetesClientException exception) {
            throw failure("creating the pod", exception);
        }
    }

    private static void deliverInputs(
        KubernetesOperations operations, String namespace, String podName, InputStream inputArchive,
        Deadline runningDeadline) throws IOException, InterruptedException {

        Duration timeout = runningDeadline.remaining()
            .plus(CLIENT_GRACE);

        KubernetesOperations.ExecResult untar = operations.exec(
            namespace, podName, INIT_CONTAINER, List.of("tar", "-xf", "-", "-C", WORK_DIRECTORY), inputArchive,
            OutputStream.nullOutputStream(), timeout);

        if (untar.exitCode() != 0) {
            throw new IllegalStateException(
                "The kubernetes runner failed while delivering the inputs: tar exited with code %d: %s".formatted(
                    untar.exitCode(), untar.stderr()));
        }

        KubernetesOperations.ExecResult ready = operations.exec(
            namespace, podName, INIT_CONTAINER, List.of("touch", READY_MARKER), null, OutputStream.nullOutputStream(),
            timeout);

        if (ready.exitCode() != 0) {
            throw new IllegalStateException(
                "The kubernetes runner failed while delivering the inputs: the ready marker could not be written: %s"
                    .formatted(ready.stderr()));
        }
    }

    private static void deleteQuietly(
        KubernetesOperations operations, String namespace, String podName, @Nullable String pullSecretName,
        Duration gracePeriod) {

        try {
            operations.deletePod(namespace, podName, gracePeriod);
        } catch (RuntimeException exception) {
            log.warn("Could not delete the kubernetes pod {}", podName, exception);
        }

        if (pullSecretName != null) {
            deleteSecretQuietly(operations, namespace, pullSecretName);
        }
    }

    private static void deleteSecretQuietly(KubernetesOperations operations, String namespace, String secretName) {
        try {
            operations.deleteSecret(namespace, secretName);
        } catch (RuntimeException exception) {
            log.warn("Could not delete the kubernetes pull secret {}", secretName, exception);
        }
    }

    @Nullable
    private static RemoteJobExit evicted(Pod pod) {
        PodStatus status = pod.getStatus();

        if (status != null && "Failed".equals(status.getPhase()) && status.getReason() != null) {
            return new RemoteJobExit(true, null, "%s: %s".formatted(status.getReason(), status.getMessage()));
        }

        return null;
    }

    private static IllegalStateException failure(String phase, KubernetesClientException exception) {
        Status status = exception.getStatus();

        String reason = status == null || status.getReason() == null ? "" : " " + status.getReason();

        return new IllegalStateException(
            "The kubernetes runner failed while %s: HTTP %d%s".formatted(phase, exception.getCode(), reason),
            exception);
    }

    private static void failFast(Pod pod) {
        PodStatus status = pod.getStatus();

        if (status == null) {
            return;
        }

        List<ContainerStatus> containerStatuses = new java.util.ArrayList<>();

        if (status.getInitContainerStatuses() != null) {
            containerStatuses.addAll(status.getInitContainerStatuses());
        }

        if (status.getContainerStatuses() != null) {
            containerStatuses.addAll(status.getContainerStatuses());
        }

        for (ContainerStatus containerStatus : containerStatuses) {
            ContainerState state = containerStatus.getState();

            if (state != null && state.getWaiting() != null && FAIL_FAST_REASONS.contains(state.getWaiting()
                .getReason())) {

                throw new IllegalStateException(
                    "The kubernetes runner's container %s cannot start: %s: %s".formatted(
                        containerStatus.getName(), state.getWaiting()
                            .getReason(),
                        state.getWaiting()
                            .getMessage()));
            }
        }
    }

    private static Map<String, String> annotations(Pod pod) {
        Map<String, String> annotations = pod.getMetadata()
            .getAnnotations();

        return annotations == null ? Map.of() : annotations;
    }

    private static RemoteJobHandle handle(Pod pod, String namespace) {
        Map<String, String> annotations = annotations(pod);

        Map<String, String> attributes = new LinkedHashMap<>();

        attributes.put(NAMESPACE_ATTRIBUTE, namespace);
        attributes.put(
            KILL_GRACE_PERIOD, annotations.getOrDefault(KubernetesJobLabels.ANNOTATION_KILL_GRACE_PERIOD, "PT0S"));

        String pullSecretName = annotations.get(KubernetesJobLabels.ANNOTATION_PULL_SECRET);

        if (pullSecretName != null) {
            attributes.put(PULL_SECRET_ATTRIBUTE, pullSecretName);
        }

        return new RemoteJobHandle(pod.getMetadata()
            .getName(), attributes);
    }

    private static String handleNamespace(RemoteJobHandle handle) {
        String namespace = handle.attributes()
            .get(NAMESPACE_ATTRIBUTE);

        if (namespace == null) {
            throw new IllegalStateException("The kubernetes job handle %s names no namespace".formatted(handle.id()));
        }

        return namespace;
    }

    private static boolean isPastInputStage(Pod pod) {
        ContainerState state = state(pod, INIT_CONTAINER, true);

        return state != null && state.getTerminated() != null && Integer.valueOf(0)
            .equals(state.getTerminated()
                .getExitCode());
    }

    private static boolean isRunning(Pod pod, String containerName) {
        ContainerState state = state(pod, containerName, false);

        return state != null && state.getRunning() != null;
    }

    private static KubernetesConnectionSettings connectionSettings(RemoteJobContext context) {
        ComponentConnection connection = context.connection();

        if (connection == null) {
            throw new IllegalArgumentException(
                "The Kubernetes runner needs a connection; select one in the task's connections");
        }

        return KubernetesConnectionSettings.of(connection);
    }

    private static void markDone(KubernetesOperations operations, String namespace, String podName)
        throws InterruptedException {

        try {
            operations.exec(
                namespace, podName, OUTPUT_CONTAINER, List.of("touch", DONE_MARKER), null,
                OutputStream.nullOutputStream(), CLIENT_GRACE);
        } catch (IOException | RuntimeException exception) {
            log.debug("Could not mark the kubernetes pod {} done; cleanup removes it", podName, exception);
        }
    }

    private static String namespace(Parameters runnerParameters, KubernetesConnectionSettings connectionSettings) {
        String namespace = runnerParameters.getString(KubernetesRunnerSettings.NAMESPACE);

        return namespace == null || namespace.isBlank() ? connectionSettings.namespace() : namespace.trim();
    }

    private static IllegalStateException notStarted(
        KubernetesOperations operations, String namespace, String podName, Deadline runningDeadline) {

        String event = operations.lastWarningEvent(namespace, podName);

        return new IllegalStateException(
            "The kubernetes job did not start within %s%s".formatted(
                runningDeadline.getBudget(), event == null ? "" : ": " + event));
    }

    private static Pod requirePod(KubernetesOperations operations, String namespace, String podName) {
        Pod pod = operations.getPod(namespace, podName);

        if (pod == null) {
            throw new IllegalStateException("The kubernetes pod %s disappeared".formatted(podName));
        }

        return pod;
    }

    private static InputStream selfDeleting(Path path) throws IOException {
        return new FilterInputStream(Files.newInputStream(path)) {

            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    Files.deleteIfExists(path);
                }
            }
        };
    }

    @Nullable
    private static ContainerState state(Pod pod, String containerName, boolean initContainer) {
        PodStatus status = pod.getStatus();

        if (status == null) {
            return null;
        }

        List<ContainerStatus> containerStatuses = initContainer
            ? status.getInitContainerStatuses() : status.getContainerStatuses();

        if (containerStatuses == null) {
            return null;
        }

        for (ContainerStatus containerStatus : containerStatuses) {
            if (containerName.equals(containerStatus.getName())) {
                return containerStatus.getState();
            }
        }

        return null;
    }

    private static final class CeilingOutputStream extends FilterOutputStream {

        private final long ceiling;

        private long written;

        CeilingOutputStream(OutputStream outputStream, long ceiling) {
            super(outputStream);

            this.ceiling = ceiling;
        }

        @Override
        public void write(int value) throws IOException {
            count(1);

            out.write(value);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            count(length);

            out.write(buffer, offset, length);
        }

        private void count(int length) throws IOException {
            written += length;

            if (written > ceiling) {
                throw new IOException(
                    "The kubernetes job's outputs exceed the %d byte transfer ceiling".formatted(ceiling));
            }
        }
    }

    private static final class SinkOutputStream extends OutputStream {

        private final RemoteLogSink logSink;

        SinkOutputStream(RemoteLogSink logSink) {
            this.logSink = logSink;
        }

        @Override
        public void write(int value) {
            logSink.appendStdout(new byte[] {
                (byte) value
            }, 1);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) {
            byte[] copy = java.util.Arrays.copyOfRange(buffer, offset, offset + length);

            logSink.appendStdout(copy, length);
        }
    }
}
```

Tidy-ups while typing it in: import `java.util.ArrayList` and `java.util.Arrays` rather than fully qualifying them, and remove imports the final code does not use. `SinkOutputStream` lets the orchestrator's `CapturingLogSink` keep only head and tail, so log memory stays bounded.

- [ ] **Step 6: Run tests**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add server/ee/libs/platform/platform-component/platform-component-runner-kubernetes
git commit -m "Add the kubernetes task runner backend"
```

---
### Task 6: The fabric8 adapter, the Spring bean, and a k3s end-to-end test (EE)

**Files:**
- Create: `K8S_PKG/Fabric8KubernetesOperations.java`
- Modify: `K8S_PKG/KubernetesTaskRunnerBackend.java` (public Spring constructor and bean annotations)
- Modify: `K8S/build.gradle.kts` (integration-test dependencies)
- Modify: `server/build.gradle.kts` (manage `org.testcontainers:k3s` like the other Testcontainers modules)
- Test: `K8S_TEST/KubernetesTaskRunnerBackendIntTest.java`

**Interfaces:**
- Consumes: Task 5's `KubernetesOperations`/`KubernetesOperationsFactory`, Task 3's `KubernetesConnectionSettings.toConfig(Duration)`, and the CE `RemoteTaskRunner(backend, applicationProperties, keySupplier)`, `ApplicationProperties`, `ComponentConnection`, `ActionContextAware`.
- Produces: the `@Component @ConditionalOnEEVersion` bean `KubernetesTaskRunnerBackend`. `TaskRunnerRegistryImpl` picks it up as a `RemoteTaskRunnerBackend` and wraps it under type `kubernetes`.

- [ ] **Step 1: Dependencies**

In `server/build.gradle.kts`, inside `dependencyManagement { dependencies { … } }`, add after the `localstack` line:

```kotlin
            dependency("org.testcontainers:k3s:${rootProject.libs.versions.testcontainers.get()}")
```

In `K8S/build.gradle.kts`, add:

```kotlin
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:k3s")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
```

- [ ] **Step 2: Write the end-to-end test (it fails to compile until Step 3)**

`KubernetesTaskRunnerBackendIntTest.java`:

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.File;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.platform.component.runner.TaskRunnerResult;
import com.bytechef.platform.component.runner.external.RemoteTaskRunner;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

@ExtendWith(ObjectMapperSetupExtension.class)
@Timeout(value = 15, unit = TimeUnit.MINUTES)
class KubernetesTaskRunnerBackendIntTest {

    private static final String SHELL_IMAGE = "busybox:1.36";

    private static K3sContainer k3sContainer;
    private static Map<String, Object> connectionParameters;
    private static KubernetesClient kubernetesClient;

    @BeforeAll
    static void startCluster() {
        assumeTrue(
            DockerClientFactory.instance()
                .isDockerAvailable(),
            "no docker daemon, so k3s cannot start");

        k3sContainer = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.31.4-k3s1"));

        k3sContainer.start();

        Config config = Config.fromKubeconfig(k3sContainer.getKubeConfigYaml());

        connectionParameters = new HashMap<>();

        connectionParameters.put("masterUrl", config.getMasterUrl());
        connectionParameters.put("caCertData", config.getCaCertData());
        connectionParameters.put("clientCertData", config.getClientCertData());
        connectionParameters.put("clientKeyData", config.getClientKeyData());
        connectionParameters.put("namespace", "default");

        kubernetesClient = new KubernetesClientBuilder().withConfig(config)
            .build();
    }

    @AfterAll
    static void stopCluster() {
        if (kubernetesClient != null) {
            kubernetesClient.close();
        }

        if (k3sContainer != null) {
            k3sContainer.stop();
        }
    }

    @Test
    void testAPythonScriptReturnsItsValue() {
        TaskRunnerResult result = runner(() -> "python-key").run(
            scriptRequest(
                """
                    def perform(input, context):
                        return {"doubled": input["value"] * 2}
                    """,
                Map.of("value", 21), Map.of(), actionContext(null, new LinkedHashMap<>())));

        assertThat(result.exitCode()).isZero();
        assertThat(result.output()).isEqualTo(Map.of("doubled", 42));
    }

    @Test
    void testAnInputFileGoesInAndAnOutputFileComesBack() {
        Map<String, byte[]> storedFiles = new LinkedHashMap<>();

        TaskRunnerResult result = runner(() -> "files-key").run(
            commandsRequest(
                List.of(
                    "cat \"$BYTECHEF_WORKING_DIR/greeting.txt\" > \"$BYTECHEF_OUTPUT_DIR/echoed.txt\"",
                    "echo collected"),
                Map.of("greeting.txt", "hello from the host"), List.of("*.txt"), Map.of("image", SHELL_IMAGE),
                actionContext(null, storedFiles)));

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains("collected");
        assertThat(new String(storedFiles.get("echoed.txt"), StandardCharsets.UTF_8)).isEqualTo("hello from the host");
    }

    @Test
    void testAFailingCommandReportsItsCodeAndStderr() {
        RemoteTaskRunner runner = runner(() -> "failing-key");

        TaskRunnerRequest request = commandsRequest(
            List.of("echo 'the interpreter did not like that' >&2", "exit 7"), Map.of(), List.of(),
            Map.of("image", SHELL_IMAGE), actionContext(null, new LinkedHashMap<>()));

        assertThatThrownBy(() -> runner.run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("code 7")
            .hasMessageContaining("the interpreter did not like that");
    }

    @Test
    void testAJobThatOutlivesItsDeadlineIsKilledAndRemoved() throws InterruptedException {
        RemoteTaskRunner runner = runner(() -> "timeout-key");

        TaskRunnerRequest request = commandsRequest(
            List.of("sleep 300"), Map.of(), List.of(), Map.of("image", SHELL_IMAGE, "waitUntilCompletion", "PT20S"),
            actionContext(null, new LinkedHashMap<>()));

        assertThatThrownBy(() -> runner.run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("timed out");

        assertThat(awaitNoPods("timeout-key")).isTrue();
    }

    @Test
    void testAnInterruptedRunIsResumedByTheNextRunWithTheSameKey() throws InterruptedException {
        TaskRunnerRequest first = commandsRequest(
            List.of("sleep 15", "echo '{\"ok\": true}' > \"$BYTECHEF_OUTPUT_DIR/output.json\"", "echo done"),
            Map.of(), List.of(), Map.of("image", SHELL_IMAGE), actionContext(42L, new LinkedHashMap<>()));

        AtomicReference<RuntimeException> failure = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                runner(() -> "unused").run(first);
            } catch (RuntimeException exception) {
                failure.set(exception);
            }
        });

        thread.start();

        assertThat(awaitMainRunning()).isTrue();

        thread.interrupt();
        thread.join(Duration.ofSeconds(30)
            .toMillis());

        assertThat(failure.get()).isInstanceOf(IllegalStateException.class);

        TaskRunnerRequest second = commandsRequest(
            List.of("echo never-runs"), Map.of(), List.of(), Map.of("image", SHELL_IMAGE),
            actionContext(42L, new LinkedHashMap<>()));

        try {
            TaskRunnerResult result = runner(() -> "unused").run(second);

            assertThat(result.output()).isEqualTo(Map.of("ok", true));
            assertThat(result.stdout()).contains("done")
                .doesNotContain("never-runs");
        } finally {
            kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .withGracePeriod(0)
                .delete();
        }
    }

    private static boolean awaitMainRunning() throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(5)
            .toNanos();

        while (System.nanoTime() < deadline) {
            for (Pod pod : kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .list()
                .getItems()) {

                if (pod.getStatus()
                    .getContainerStatuses()
                    .stream()
                    .anyMatch(status -> "main".equals(status.getName()) && status.getState()
                        .getRunning() != null)) {

                    return true;
                }
            }

            Thread.sleep(500);
        }

        return false;
    }

    private static boolean awaitNoPods(String key) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofMinutes(2)
            .toNanos();

        while (System.nanoTime() < deadline) {
            boolean anyLeft = kubernetesClient.pods()
                .inNamespace("default")
                .withLabel("bytechef.io/managed-by", "bytechef")
                .list()
                .getItems()
                .stream()
                .anyMatch(pod -> key.equals(pod.getMetadata()
                    .getAnnotations()
                    .get("bytechef.io/key")) && pod.getMetadata()
                        .getDeletionTimestamp() == null);

            if (!anyLeft) {
                return true;
            }

            Thread.sleep(500);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static ActionContextAware actionContext(Long taskExecutionId, Map<String, byte[]> storedFiles) {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        File file = mock(File.class);

        when(actionContext.getTaskExecutionId()).thenReturn(taskExecutionId);
        when(actionContext.getJobId()).thenReturn(taskExecutionId == null ? null : 900L);

        ArgumentCaptor<ContextFunction<File, ?>> fileFunctionArgumentCaptor =
            ArgumentCaptor.forClass(ContextFunction.class);

        try {
            when(actionContext.file(fileFunctionArgumentCaptor.capture()))
                .thenAnswer(invocation -> fileFunctionArgumentCaptor.getValue()
                    .apply(file));
            when(file.storeContent(anyString(), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    String fileName = invocation.getArgument(0);
                    InputStream inputStream = invocation.getArgument(1);

                    storedFiles.put(fileName, inputStream.readAllBytes());

                    return mock(FileEntry.class);
                });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not arrange the storing action context", exception);
        }

        return actionContext;
    }

    private static ApplicationProperties applicationProperties() {
        ApplicationProperties.Script.Runner runner = new ApplicationProperties.Script.Runner();

        runner.setEnabled(true);

        ApplicationProperties applicationProperties = new ApplicationProperties();

        Map<String, ApplicationProperties.Script.Runner> runners = new HashMap<>();

        runners.put("kubernetes", runner);

        applicationProperties.getScript()
            .setRunners(runners);

        return applicationProperties;
    }

    private static TaskRunnerRequest commandsRequest(
        List<String> commands, Map<String, ?> inputFiles, List<String> outputFilePatterns,
        Map<String, ?> runnerParameters, ActionContextAware actionContext) {

        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), Map.of(), inputFiles, outputFilePatterns,
            ParametersFactory.create(Map.of()), ParametersFactory.create(runnerParameters), null, connections(),
            actionContext);
    }

    private static Map<String, ComponentConnection> connections() {
        return Map.of(
            "taskRunner", new ComponentConnection("kubernetes", 1, 1L, new HashMap<>(connectionParameters), null));
    }

    private static RemoteTaskRunner runner(java.util.function.Supplier<String> keySupplier) {
        return new RemoteTaskRunner(
            new KubernetesTaskRunnerBackend(Fabric8KubernetesOperations::new, Duration.ofMillis(500)),
            applicationProperties(), keySupplier);
    }

    private static TaskRunnerRequest scriptRequest(
        String script, Map<String, ?> input, Map<String, ?> runnerParameters, ActionContextAware actionContext) {

        return new TaskRunnerRequest(
            "python", script, List.of(), input, Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(runnerParameters), null, connections(), actionContext);
    }
}
```

Notes for the implementer:
- `RemoteTaskRunner`'s three-argument constructor is package-private in `com.bytechef.platform.component.runner.external`. Make it `public`, a one-word CE change in `R_IMPL`, so this EE test can supply a deterministic key, and include that file in this task's commit.
- Use `import java.util.function.Supplier;` rather than the fully qualified name.
- `testAJobThatOutlivesItsDeadlineIsKilledAndRemoved` and `awaitNoPods` match on the `bytechef.io/key` annotation. The key is built from the tenant (`public` by default), so with a null task execution ID it is the random key `timeout-key`.

- [ ] **Step 3: Implement the adapter**

```java
package com.bytechef.ee.platform.component.runner.kubernetes;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ContainerResource;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link KubernetesOperations} over the fabric8 client.
 *
 * @author Ivica Cardic
 * @version ee
 */
final class Fabric8KubernetesOperations implements KubernetesOperations {

    private static final Logger log = LoggerFactory.getLogger(Fabric8KubernetesOperations.class);

    private static final int MAX_STDERR_BYTES = 8 * 1024;

    private final KubernetesClient kubernetesClient;

    Fabric8KubernetesOperations(KubernetesConnectionSettings connectionSettings, Duration requestTimeout) {
        this.kubernetesClient = new KubernetesClientBuilder().withConfig(connectionSettings.toConfig(requestTimeout))
            .build();
    }

    @Override
    public Secret createSecret(Secret secret) {
        return kubernetesClient.secrets()
            .inNamespace(secret.getMetadata()
                .getNamespace())
            .resource(secret)
            .create();
    }

    @Override
    public Pod createPod(Pod pod) {
        return kubernetesClient.pods()
            .inNamespace(pod.getMetadata()
                .getNamespace())
            .resource(pod)
            .create();
    }

    @Override
    public @Nullable Pod getPod(String namespace, String name) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(name)
            .get();
    }

    @Override
    public List<Pod> listPods(String namespace, Map<String, String> labels) {
        return kubernetesClient.pods()
            .inNamespace(namespace)
            .withLabels(labels)
            .list()
            .getItems();
    }

    @Override
    public void deletePod(String namespace, String name, Duration gracePeriod) {
        kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(name)
            .withGracePeriod(gracePeriod.toSeconds())
            .delete();
    }

    @Override
    public void deleteSecret(String namespace, String name) {
        kubernetesClient.secrets()
            .inNamespace(namespace)
            .withName(name)
            .delete();
    }

    @Override
    public ExecResult exec(
        String namespace, String podName, String container, List<String> command, @Nullable InputStream stdin,
        OutputStream stdout, Duration timeout) throws IOException, InterruptedException {

        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ContainerResource containerResource = kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .inContainer(container);

        String[] commandArray = command.toArray(new String[0]);

        try (ExecWatch execWatch = stdin == null
            ? containerResource.writingOutput(stdout)
                .writingError(stderr)
                .exec(commandArray)
            : containerResource.redirectingInput()
                .writingOutput(stdout)
                .writingError(stderr)
                .exec(commandArray)) {

            if (stdin != null) {
                try (OutputStream input = execWatch.getInput()) {
                    stdin.transferTo(input);
                }
            }

            Integer exitCode = execWatch.exitCode()
                .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            return new ExecResult(exitCode == null ? -1 : exitCode, tail(stderr));
        } catch (ExecutionException exception) {
            throw new IOException("The command failed in container " + container, exception.getCause());
        } catch (TimeoutException exception) {
            throw new IOException(
                "The command did not finish within %s in container %s".formatted(timeout, container), exception);
        }
    }

    @Override
    public LogStream followLogs(String namespace, String podName, String container, OutputStream outputStream) {
        LogWatch logWatch = kubernetesClient.pods()
            .inNamespace(namespace)
            .withName(podName)
            .inContainer(container)
            .watchLog(outputStream);

        return new LogStream() {

            @Override
            public boolean awaitCompletion(Duration timeout) throws InterruptedException {
                try {
                    logWatch.onClose()
                        .toCompletableFuture()
                        .get(timeout.toMillis(), TimeUnit.MILLISECONDS);

                    return true;
                } catch (ExecutionException | TimeoutException exception) {
                    log.debug("The log of {} did not reach its end", podName, exception);

                    return false;
                }
            }

            @Override
            public void close() {
                logWatch.close();
            }
        };
    }

    @Override
    public @Nullable String lastWarningEvent(String namespace, String podName) {
        try {
            return kubernetesClient.v1()
                .events()
                .inNamespace(namespace)
                .withField("involvedObject.name", podName)
                .list()
                .getItems()
                .stream()
                .filter(event -> "Warning".equals(event.getType()))
                .max(Comparator.comparing(Fabric8KubernetesOperations::eventTime))
                .map(Event::getMessage)
                .orElse(null);
        } catch (KubernetesClientException exception) {
            log.debug("Could not read the events of {}", podName, exception);

            return null;
        }
    }

    @Override
    public void close() {
        kubernetesClient.close();
    }

    private static String eventTime(Event event) {
        String lastTimestamp = event.getLastTimestamp();

        return lastTimestamp == null ? "" : lastTimestamp;
    }

    private static String tail(ByteArrayOutputStream stderr) {
        byte[] bytes = stderr.toByteArray();

        int start = Math.max(0, bytes.length - MAX_STDERR_BYTES);

        return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
    }
}
```

Fabric8 7.x API notes:
- If `LogWatch.onClose()` does not exist, implement `awaitCompletion` by polling `getPod` until the `main` container is terminated plus 2 s, and say so in the report.
- If `ExecWatch.exitCode()` returns `CompletableFuture<Integer>` under another name, use that.
- **Input delivery fallback:** if delivering the inputs hangs, because the cluster's exec protocol cannot signal end-of-stdin (Kubernetes before 1.29 without the v5 channel), switch `deliverInputs` in the backend to buffer the archive to a temp file and run `sh -c "head -c <size> | tar -xf - -C /bytechef/work"`. `head` stops reading at the exact size, so EOF is not needed. Record the switch in the report. k3s 1.31 supports the v5 channel, so the direct form should work there.

- [ ] **Step 4: Make the backend a Spring bean**

In `KubernetesTaskRunnerBackend` add the annotations and a public constructor:

```java
@Component
@ConditionalOnEEVersion
public class KubernetesTaskRunnerBackend implements RemoteTaskRunnerBackend {

    …

    @Autowired
    public KubernetesTaskRunnerBackend() {
        this(Fabric8KubernetesOperations::new, Duration.ofSeconds(1));
    }

    KubernetesTaskRunnerBackend(KubernetesOperationsFactory operationsFactory, Duration pollInterval) {
        …
    }
```

(Imports: `com.bytechef.platform.annotation.ConditionalOnEEVersion`, `org.springframework.beans.factory.annotation.Autowired`, `org.springframework.stereotype.Component`.)

- [ ] **Step 5: Run the unit tests, then the integration test**

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:test -q` → PASS.

Then, in the background (k3s and image pulls take minutes):

```bash
nohup ./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:testIntegration > "$SCRATCH/k8s-int.log" 2>&1 &
```

Poll the log. Expected: 5 tests pass. If Docker is unavailable, the tests are skipped by the assumption; report that, not a pass. After the run, confirm that no `bytechef.io/managed-by=bytechef` pods remain. k3s is torn down with the container, so this is only a check that the tests clean up after themselves.

- [ ] **Step 6: Commit**

```bash
git add server/build.gradle.kts server/ee/libs/platform/platform-component/platform-component-runner-kubernetes server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl/src/main/java/com/bytechef/platform/component/runner/external/RemoteTaskRunner.java
git commit -m "Talk to the cluster through fabric8 and prove the kubernetes runner on k3s"
```

---

### Task 7: Wire the runner into the apps, switch it off by default, document it

**Files:**
- Modify: `server/apps/server-app/build.gradle.kts`: add `implementation(project(":server:ee:libs:platform:platform-component:platform-component-runner-kubernetes"))` in alphabetical order with the other `:server:ee:libs:platform:*` lines.
- Modify: `server/ee/apps/worker-app/build.gradle.kts`: the same line, with the other `:server:ee:libs:platform:*` lines.
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`: under `script.runners`.
- Modify: `server/libs/modules/components/commands/src/main/resources/README.mdx`, `server/libs/modules/components/script/src/main/resources/README.mdx`

**Interfaces:**
- Consumes: the bean from Task 6.
- Produces: an installation where `bytechef.script.runners.kubernetes.enabled=true` (with `bytechef.edition=ee`) offers the Kubernetes runner in every Script and Commands task.

- [ ] **Step 1: Configuration**

In `application-bytechef.yml`, under `script: runners:` and after `graalvm:`, add:

```yaml
      kubernetes:
        # Enterprise. Runs script and commands tasks as pods on the cluster the task's kubernetes connection names.
        enabled: false
```

- [ ] **Step 2: Boot smoke**

With the infra running (`cd server && docker compose -f docker-compose.dev.infra.yml up -d`), start the server in the background with the runner switched on:

```bash
nohup ./gradlew -p server/apps/server-app bootRun --args='--bytechef.script.runners.kubernetes.enabled=true' > "$SCRATCH/boot.log" 2>&1 &
```

Wait for `Started ServerApplication` or a failure. Then:
- Confirm the log contains the registry's warning `Task runner 'kubernetes' is enabled; workflows may execute code outside the script sandbox`. That proves the bean was found and wrapped.
- Confirm there is no `BeanCurrentlyInCreationException` or `NoSuchBeanDefinitionException`.

Stop only the process you started.

- [ ] **Step 3: Docs**

`commands/README.mdx`:
- In `## Task runners`, add a table row: `| Kubernetes | `kubernetes` | Enterprise. Runs the commands as a pod on the cluster the task's **Kubernetes** connection names. |`.
- In `### Operator configuration`, add `kubernetes: enabled: false` to the yml. Add a bullet: "`kubernetes.enabled`: any workflow author with a Kubernetes connection can run any image on that connection's cluster, within what the connection's credentials allow. ByteChef adds no limits of its own beyond `runner-limits`."
- Add a section `### The Kubernetes runner (Enterprise)` after the Docker runner's section, covering:
  1. **What it needs.** A `kubernetes` connection with the API server URL, an optional CA certificate (PEM or base64 PEM), a bearer token or a client certificate and key, a default namespace, and "Skip TLS Verification" for test clusters only. The task's image must provide `/bin/sh`.
  2. **Its properties.** A table with `image`, `entrypoint`, `namespace`, `serviceAccountName` (without one, no service account token is mounted), `resources` (`cpuRequest`, `cpuLimit`, `memoryRequest`, `memoryLimit` as Kubernetes quantities), `nodeSelector`, `tolerations`, `labels`, `annotations` (keys under `bytechef.io/` are reserved), `credentials` (a temporary image pull secret, removed with the pod), `fileSidecarImage` (default `busybox:1.36`; needs `sh`, `tar`, `test`, `sleep`, `touch`) and `killGracePeriod`, plus the shared `pullPolicy`, `resume`, `delete`, `waitUntilRunning` and `waitUntilCompletion`.
  3. **How a run works.**
     - The pod has three containers sharing an `emptyDir` sized at twice `max-transfer-bytes`: the file sidecar (init container) receives the inputs, `main` runs the task with stderr redirected to `.stderr`, and the output sidecar hands back `output/` and `.stderr` before the pod completes.
     - `activeDeadlineSeconds` is the completion budget plus one minute, so a pod whose ByteChef worker died still stops.
     - Image pull and configuration errors fail within seconds.
     - A pod that cannot be scheduled fails after `waitUntilRunning` with the cluster's scheduling message.
     - `OOMKilled` and evictions are named in the failure.
  4. **Resume.**
     - Pods carry `bytechef.io/managed-by=bytechef` and `bytechef.io/task-execution-id` (the tenant-job-task key, or a `k-` SHA-256 prefix when it does not fit a label value). The full key is in the annotation `bytechef.io/key`.
     - A redelivered task re-attaches to a pod whose inputs were delivered and whose outputs were not yet collected. Any other match is deleted and the task resubmitted.
     - The same durable-broker caveat as Docker applies. Clean up leftovers with `kubectl delete pod -l bytechef.io/managed-by=bytechef`.
  5. **RBAC.** A Role for the connection's identity:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: bytechef-task-runner
  namespace: <namespace>
rules:
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["create", "get", "list", "watch", "delete"]
  - apiGroups: [""]
    resources: ["pods/log"]
    verbs: ["get"]
  - apiGroups: [""]
    resources: ["pods/exec"]
    verbs: ["create", "get"]
  - apiGroups: [""]
    resources: ["secrets"]
    verbs: ["create", "delete"]
  - apiGroups: [""]
    resources: ["events"]
    verbs: ["list"]
```

  (Secrets are only needed when tasks use `credentials`. Without `events`, scheduling failures are reported without the cluster's message.)

`script/README.mdx`, `### Task Runners` table: add the row `| Kubernetes | `kubernetes` | Enterprise. Runs the script as a pod on the cluster the task's **Kubernetes** connection names. Enable with `bytechef.script.runners.kubernetes.enabled=true`; the **Commands** component's README documents its properties, RBAC and resume behaviour. |`. Also extend the "Result Shape" sentence that names Process and Docker to include Kubernetes.

- [ ] **Step 4: Checks**

Run in the background, then poll: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:check :server:ee:libs:modules:components:kubernetes:check :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:check :server:libs:modules:components:commands:check :server:libs:modules:components:script:check -q`
Expected: PASS. Revert any unrelated spotless reformatting.

- [ ] **Step 5: Commit**

```bash
git add server/apps/server-app/build.gradle.kts server/ee/apps/worker-app/build.gradle.kts server/apps/server-app/src/main/resources/config/application-bytechef.yml server/libs/modules/components/commands/src/main/resources/README.mdx server/libs/modules/components/script/src/main/resources/README.mdx
git commit -m "Enable the kubernetes runner in the server and worker apps and document it"
```
