# Remote Task Runners — Plan 4: Connections as Environment Variables Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give Script and Commands tasks that run on an external runner (Process, Docker, Kubernetes, AWS Batch)
the parameters of the connections they declare as environment variables, and mask those values in the captured
output, the way Kestra scripts receive secrets.

**Architecture:**
- **Builder:** a new CE class, `TaskRunnerConnectionEnvironment`, turns `TaskRunnerRequest.componentConnections()`
  (minus `taskRunner`) into `BYTECHEF_CONNECTION_*` variables plus a `SecretMasker`.
- **Callers:** `ProcessTaskRunner` and `RemoteTaskRunner` both call it. `RemoteTaskRunner` covers Docker, Kubernetes
  and AWS Batch through `RemoteJobSpec.environment()`, so no backend changes.
- **Masking:** applied where captured text is read (`stdout`/`stderr` results and the failure-message tails) and to
  every guest line logged to the server log.
- **Switch:** `bytechef.script.inject-connections` turns injection off for the whole installation.

**Tech Stack:** Java 25, Spring Boot 4, JUnit 5, AssertJ, Mockito, Testcontainers (Docker, k3s).

**Spec:** `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md` §13 (and §4.1 for the switch). Builds on
Plans 1–3, all on branch `0_p_script_runners`.

## Global Constraints

- Work in the main checkout `/Volumes/Data/bytechef/bytechef-master` on branch `0_p_script_runners` (no worktree).
  Never push, never `git stash`, never amend. Run `git rev-parse --abbrev-ref HEAD` before every commit. Commit by
  path only.
- Commit messages are **subject only**: no body, no `Co-Authored-By`, no `Generated with` line.
- Variable names: `BYTECHEF_CONNECTION_<KEY>` (all parameters as one JSON object) and
  `BYTECHEF_CONNECTION_<KEY>_<PARAM>` (one per parameter). `<KEY>` is the connection's declaration key and `<PARAM>`
  the parameter name. Both are camelCase-split first (insert `_` between `[a-z0-9]` and `[A-Z]`), then upper-cased,
  with every character outside `[A-Z0-9_]` replaced by `_`.
- `taskRunner` (`TaskRunnerConstants.TASK_RUNNER`) is never injected.
- Flat values:
  - strings as-is;
  - enums as `name()`;
  - everything else as `JsonUtils.write(value)`;
  - nulls skipped;
  - a string containing `\0` left out of the flat form, with a server-log warning that names the key and parameter,
    never the value.
- Collision: `IllegalArgumentException("The <origin> and the <origin> both map to <VARIABLE>; rename one")`, where an
  origin is `connection '<key>'` or `parameter '<param>' of connection '<key>'`. Keys and names only, never values.
- Precedence, later wins:
  1. `HOME` (remote runners only);
  2. the injected connection variables;
  3. the author's `env`;
  4. the contract variables `BYTECHEF_WORKING_DIR` / `BYTECHEF_INPUT_FILE` / `BYTECHEF_OUTPUT_DIR`, which always win,
     as today.
- Masking:
  - **Mask set:** every string leaf of every injected connection's parameters, except the top-level parameters named
    authorizationType, region, baseUri, baseUrl, url, uri, instanceUrl, endpoint, host, hostname, port and domain
    (compared case-insensitively, ignoring `_`, `-` and `.`; they are still exported), plus each connection's JSON
    form. Each line of a multi-line value is also added. Values shorter than 6 characters are dropped.
  - **Replacement:** each occurrence becomes `******`, longest values first.
- Switch: `bytechef.script.inject-connections` is a `boolean` on `ApplicationProperties.Script`. There is no Java
  initializer, so the Java default is `false`. `true` comes from `application-bytechef.yml`. With it off, nothing is
  injected and nothing is masked.
- GraalVM is unchanged. No `context.connection` helper.
- No `_` in test method names. Unit tests end in `Test`, integration tests in `IntTest`. No prose comments in code.
  Code style follows `CLAUDE.md`.
- Run `./gradlew <module>:spotlessApply` on every touched module before committing. If Gradle reports "offline
  mode", add `--no-daemon`. Long Gradle runs go in the background with a log under the scratchpad. No single tool call
  may exceed 10 minutes.

## Review Focus

1. **An author `env` entry with the same name as an injected variable** must win (the author is explicit). Pinned in
   Task 2, `testTheAuthorsEnvironmentOverridesAnInjectedVariable`, and in Task 3,
   `testTheAuthorsEnvironmentOverridesAnInjectedVariableInTheSpec`.
2. **A secret printed on the failure path** (non-zero exit, timeout) must be masked in the exception message, not only
   in the success result. Pinned in Task 2, `testAFailureMessageMasksTheConnectionValues`, and in Task 3,
   `testAFailedJobsMessageMasksTheConnectionValues`.
3. **A short or non-secret value** (`true`, a region such as `us-east`, the `authorizationType` name) must not blank
   out the log. Pinned in Task 1, `testShortValuesAndTheAuthorizationTypeAreNotMasked`.
4. **Two connections whose keys sanitise alike** (`github-bot`, `github_bot`) must fail before anything runs, and the
   message must carry no value. Pinned in Task 1, `testKeysThatSanitiseAlikeFailNamingBothAndNoValues`.
5. **A task with only a `taskRunner` connection** (every Kubernetes and AWS Batch task) must get no
   `BYTECHEF_CONNECTION_*` variables at all. Pinned in Task 1, `testTheTaskRunnerConnectionIsNeverInjected`.

## File Structure

Path shorthand: `IMPL` = `server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl`,
`EXT` = `IMPL/src/main/java/com/bytechef/platform/component/runner/external`,
`EXT_TEST` = `IMPL/src/test/java/com/bytechef/platform/component/runner/external`.

| Path | Responsibility |
|---|---|
| `EXT/SecretMasker.java` | Masks a set of values in text (Task 1) |
| `EXT/TaskRunnerConnectionEnvironment.java` | Request + switch → variables and masker (Task 1) |
| `server/libs/config/app-config/.../ApplicationProperties.java` | `Script.injectConnections` (Task 1) |
| `server/apps/server-app/src/main/resources/config/application-bytechef.yml` | `inject-connections: true` (Task 1) |
| `EXT/ProcessTaskRunner.java` | Injects and masks; constructor takes `ApplicationProperties` (Task 2) |
| `EXT/RemoteTaskRunner.java` | Injects and masks for Docker, Kubernetes, AWS Batch (Task 3) |
| Script/Commands READMEs, spec §13 | Docs (Task 4) |

## Commit map (new commits on top of `113dc536fd6` plus this plan's commit)

| Task | Commit subject |
|---|---|
| 1 | `Build the connection environment and masker for external task runners` |
| 2 | `Inject and mask task connections in the process task runner` |
| 3 | `Inject and mask task connections in the remote task runners` |
| 4 | `Document task connections in external runners` |

---

### Task 1: The connection environment, the masker and the switch

**Files:**
- Create: `EXT/SecretMasker.java`, `EXT/TaskRunnerConnectionEnvironment.java`
- Modify: `server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java` (class
  `Script`)
- Modify: `server/apps/server-app/src/main/resources/config/application-bytechef.yml`
- Test: `EXT_TEST/SecretMaskerTest.java`, `EXT_TEST/TaskRunnerConnectionEnvironmentTest.java`

**Interfaces:**
- Produces:
  - `public final class SecretMasker` with:
    - `public static final SecretMasker NONE`;
    - `public static SecretMasker of(Collection<String> values)`;
    - `public String mask(String text)`.
  - `public record TaskRunnerConnectionEnvironment(Map<String, String> variables, SecretMasker secretMasker)` with
    `public static TaskRunnerConnectionEnvironment of(TaskRunnerRequest request, ApplicationProperties applicationProperties)`.
    `variables` is an unmodifiable, insertion-ordered map. It throws `IllegalArgumentException` on a collision.
  - `ApplicationProperties.Script#isInjectConnections()` / `setInjectConnections(boolean)`.

- [ ] **Step 1: The switch**

In `ApplicationProperties.Script`, add a field next to `runnerLimits` (no initializer; the default lives in YAML):

```java
        private boolean injectConnections;
```

and, in alphabetical order with the other accessors:

```java
        public boolean isInjectConnections() {
            return injectConnections;
        }

        public void setInjectConnections(boolean injectConnections) {
            this.injectConnections = injectConnections;
        }
```

In `application-bytechef.yml`, under `script:`, between the `runners:` block and the `runner-limits:` comment, add:

```yaml
    # Gives script and commands tasks on the process, docker, kubernetes and aws-batch runners the parameters of the
    # connections they declare, as BYTECHEF_CONNECTION_* environment variables, masked in the captured output.
    inject-connections: true
```

- [ ] **Step 2: Write the failing masker test**

`EXT_TEST/SecretMaskerTest.java` (CE Apache 2.0 header, copied from a sibling test such as
`BoundedOutputCaptureTest.java`, with `@author Ivica Cardic`):

```java
package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class SecretMaskerTest {

    @Test
    void testAValueIsReplacedEverywhereItOccurs() {
        SecretMasker secretMasker = SecretMasker.of(List.of("xoxb-secret"));

        assertThat(secretMasker.mask("token=xoxb-secret and again xoxb-secret"))
            .isEqualTo("token=****** and again ******");
    }

    @Test
    void testValuesShorterThanSixCharactersAreNotMasked() {
        SecretMasker secretMasker = SecretMasker.of(List.of("true", "abcde", "abcdef"));

        assertThat(secretMasker.mask("true abcde abcdef")).isEqualTo("true abcde ******");
    }

    @Test
    void testTheLongestValueIsMaskedFirst() {
        SecretMasker secretMasker = SecretMasker.of(List.of("secret", "secret-and-more"));

        assertThat(secretMasker.mask("x secret-and-more y secret z")).isEqualTo("x ****** y ****** z");
    }

    @Test
    void testNullsAndDuplicatesAreIgnored() {
        SecretMasker secretMasker = SecretMasker.of(Arrays.asList(null, "abcdef", "abcdef"));

        assertThat(secretMasker.mask("abcdef")).isEqualTo("******");
    }

    @Test
    void testNoneLeavesTextUntouched() {
        assertThat(SecretMasker.NONE.mask("anything at all")).isEqualTo("anything at all");
    }
}
```

- [ ] **Step 3: Write the failing environment test**

`EXT_TEST/TaskRunnerConnectionEnvironmentTest.java`:

```java
package com.bytechef.platform.component.runner.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ObjectMapperSetupExtension.class)
class TaskRunnerConnectionEnvironmentTest {

    @Test
    void testEachConnectionGetsAJsonVariableAndOneVariablePerParameter() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        assertThat(environment.variables())
            .containsEntry("BYTECHEF_CONNECTION_SLACK", "{\"token\":\"xoxb-123456\"}")
            .containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456");
    }

    @Test
    void testKeysAndParameterNamesAreSanitised() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("github-bot", connection("github", Map.of("access.token", "gho_abcdef")))), enabled());

        assertThat(environment.variables())
            .containsKey("BYTECHEF_CONNECTION_GITHUB_BOT")
            .containsEntry("BYTECHEF_CONNECTION_GITHUB_BOT_ACCESS_TOKEN", "gho_abcdef");
    }

    @Test
    void testNonStringValuesAreJsonAndNullsAreSkipped() {
        Map<String, Object> parameters = new HashMap<>();

        parameters.put("port", 5432);
        parameters.put("options", Map.of("ssl", true));
        parameters.put("missing", null);

        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("db", connection("postgresql", parameters))), enabled());

        assertThat(environment.variables())
            .containsEntry("BYTECHEF_CONNECTION_DB_PORT", "5432")
            .containsEntry("BYTECHEF_CONNECTION_DB_OPTIONS", "{\"ssl\":true}")
            .doesNotContainKey("BYTECHEF_CONNECTION_DB_MISSING");
    }

    @Test
    void testTheAuthorizationTypeIsExportedByName() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L, Map.of("token", "bearer-abcdef"), AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(environment.variables()).containsEntry("BYTECHEF_CONNECTION_API_AUTHORIZATION_TYPE", "BEARER_TOKEN");
    }

    @Test
    void testTheTaskRunnerConnectionIsNeverInjected() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("taskRunner", connection("aws", Map.of("secretAccessKey", "aws-secret-key")))), enabled());

        assertThat(environment.variables()).isEmpty();
        assertThat(environment.secretMasker()
            .mask("aws-secret-key")).isEqualTo("aws-secret-key");
    }

    @Test
    void testAStringWithANulCharacterIsLeftOutOfTheFlatForm() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("odd", connection("custom", Map.of("value", "abc\u0000def")))), enabled());

        assertThat(environment.variables())
            .containsKey("BYTECHEF_CONNECTION_ODD")
            .doesNotContainKey("BYTECHEF_CONNECTION_ODD_VALUE");
    }

    @Test
    void testKeysThatSanitiseAlikeFailNamingBothAndNoValues() {
        Map<String, ComponentConnection> connections = new LinkedHashMap<>();

        connections.put("github-bot", connection("github", Map.of("token", "first-secret")));
        connections.put("github_bot", connection("github", Map.of("token", "second-secret")));

        assertThatThrownBy(() -> TaskRunnerConnectionEnvironment.of(request(connections), enabled()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("connection 'github-bot'")
            .hasMessageContaining("connection 'github_bot'")
            .hasMessageContaining("BYTECHEF_CONNECTION_GITHUB_BOT")
            .hasMessageNotContaining("first-secret")
            .hasMessageNotContaining("second-secret");
    }

    @Test
    void testValuesAndTheJsonFormAreMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), enabled());

        SecretMasker secretMasker = environment.secretMasker();

        assertThat(secretMasker.mask("token xoxb-123456")).isEqualTo("token ******");
        assertThat(secretMasker.mask("json {\"token\":\"xoxb-123456\"}")).isEqualTo("json ******");
    }

    @Test
    void testNestedStringValuesAreMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("gcp", connection("google", Map.of("serviceAccount", Map.of("privateKey", "pk-abcdef"))))),
            enabled());

        assertThat(environment.secretMasker()
            .mask("key pk-abcdef")).isEqualTo("key ******");
    }

    @Test
    void testShortValuesAndTheAuthorizationTypeAreNotMasked() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of(
                "api", new ComponentConnection(
                    "httpClient", 1, 1L, Map.of("region", "us-e1", "token", "bearer-abcdef"),
                    AuthorizationType.BEARER_TOKEN))),
            enabled());

        assertThat(environment.secretMasker()
            .mask("BEARER_TOKEN us-e1 bearer-abcdef")).isEqualTo("BEARER_TOKEN us-e1 ******");
    }

    @Test
    void testTheSwitchOffInjectsAndMasksNothing() {
        TaskRunnerConnectionEnvironment environment = TaskRunnerConnectionEnvironment.of(
            request(Map.of("slack", connection("slack", Map.of("token", "xoxb-123456")))), new ApplicationProperties());

        assertThat(environment.variables()).isEmpty();
        assertThat(environment.secretMasker()
            .mask("xoxb-123456")).isEqualTo("xoxb-123456");
    }

    private static ComponentConnection connection(String componentName, Map<String, ?> parameters) {
        return new ComponentConnection(componentName, 1, 1L, parameters, null);
    }

    private static ApplicationProperties enabled() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .setInjectConnections(true);

        return applicationProperties;
    }

    private static TaskRunnerRequest request(Map<String, ComponentConnection> connections) {
        return new TaskRunnerRequest(
            "shell", null, List.of("true"), Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), null, connections, mock(ActionContext.class));
    }
}
```

`ComponentConnection` copies its parameters through `getConnectionParameters()`, so `Map.of(...)` inputs are fine.
If `ComponentConnection`'s constructor rejects a map with a null value (as in
`testNonStringValuesAreJsonAndNullsAreSkipped`), keep the `HashMap` as written.

- [ ] **Step 4: Run both tests and watch them fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test --tests '*SecretMaskerTest' --tests '*TaskRunnerConnectionEnvironmentTest'`
Expected: compilation fails because `SecretMasker`, `TaskRunnerConnectionEnvironment` and
`setInjectConnections` do not exist.

- [ ] **Step 5: Implement `SecretMasker`**

`EXT/SecretMasker.java` (CE header, `@author Ivica Cardic`):

```java
package com.bytechef.platform.component.runner.external;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Replaces known secret values in captured text. Best effort: a transformed value is not recognised.
 *
 * @author Ivica Cardic
 */
public final class SecretMasker {

    public static final SecretMasker NONE = new SecretMasker(List.of());

    static final String MASK = "******";
    static final int MINIMUM_LENGTH = 6;

    private final List<String> values;

    private SecretMasker(List<String> values) {
        this.values = values;
    }

    public static SecretMasker of(Collection<String> values) {
        List<String> eligibleValues = values.stream()
            .filter(Objects::nonNull)
            .filter(value -> value.length() >= MINIMUM_LENGTH)
            .distinct()
            .sorted(Comparator.comparingInt(String::length)
                .reversed())
            .toList();

        return eligibleValues.isEmpty() ? NONE : new SecretMasker(eligibleValues);
    }

    public String mask(String text) {
        if (values.isEmpty() || text.isEmpty()) {
            return text;
        }

        String masked = text;

        for (String value : values) {
            masked = masked.replace(value, MASK);
        }

        return masked;
    }
}
```

- [ ] **Step 6: Implement `TaskRunnerConnectionEnvironment`**

`EXT/TaskRunnerConnectionEnvironment.java`:

```java
package com.bytechef.platform.component.runner.external;

import static com.bytechef.platform.component.runner.TaskRunnerConstants.TASK_RUNNER;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.Authorization;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The environment variables and the masker that hand a task's declared connections to an external runner.
 *
 * @author Ivica Cardic
 */
public record TaskRunnerConnectionEnvironment(Map<String, String> variables, SecretMasker secretMasker) {

    static final String PREFIX = "BYTECHEF_CONNECTION_";

    private static final Logger log = LoggerFactory.getLogger(TaskRunnerConnectionEnvironment.class);
    private static final Pattern NOT_ALLOWED = Pattern.compile("[^A-Z0-9_]");

    public static TaskRunnerConnectionEnvironment of(
        TaskRunnerRequest request, ApplicationProperties applicationProperties) {

        ApplicationProperties.Script script = applicationProperties.getScript();

        if (!script.isInjectConnections()) {
            return new TaskRunnerConnectionEnvironment(Map.of(), SecretMasker.NONE);
        }

        Map<String, String> variables = new LinkedHashMap<>();
        Map<String, String> origins = new HashMap<>();
        List<String> secrets = new ArrayList<>();

        Map<String, ComponentConnection> connections = new TreeMap<>(request.componentConnections());

        for (Map.Entry<String, ComponentConnection> entry : connections.entrySet()) {
            String key = entry.getKey();

            if (TASK_RUNNER.equals(key)) {
                continue;
            }

            ComponentConnection componentConnection = entry.getValue();

            Map<String, ?> parameters = componentConnection.getConnectionParameters();

            String connectionVariable = PREFIX + sanitize(key);
            String json = JsonUtils.write(parameters);

            put(variables, origins, connectionVariable, json, "connection '%s'".formatted(key));

            secrets.add(json);

            for (Map.Entry<String, ?> parameter : new TreeMap<>(parameters).entrySet()) {
                String name = parameter.getKey();
                Object value = parameter.getValue();

                if (value == null) {
                    continue;
                }

                if (!Authorization.AUTHORIZATION_TYPE.equals(name)) {
                    collectStrings(value, secrets);
                }

                String flatValue = flatValue(value);

                if (flatValue.indexOf('\0') >= 0) {
                    log.warn(
                        "Parameter '{}' of connection '{}' contains a NUL character, so only {} carries it", name, key,
                        connectionVariable);

                    continue;
                }

                put(
                    variables, origins, connectionVariable + "_" + sanitize(name), flatValue,
                    "parameter '%s' of connection '%s'".formatted(name, key));
            }
        }

        return new TaskRunnerConnectionEnvironment(
            Collections.unmodifiableMap(variables), SecretMasker.of(secrets));
    }

    private static void collectStrings(Object value, List<String> secrets) {
        if (value instanceof String string) {
            secrets.add(string);
        } else if (value instanceof Map<?, ?> map) {
            for (Object nestedValue : map.values()) {
                if (nestedValue != null) {
                    collectStrings(nestedValue, secrets);
                }
            }
        } else if (value instanceof Iterable<?> iterable) {
            for (Object nestedValue : iterable) {
                if (nestedValue != null) {
                    collectStrings(nestedValue, secrets);
                }
            }
        }
    }

    private static String flatValue(Object value) {
        if (value instanceof String string) {
            return string;
        }

        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }

        return JsonUtils.write(value);
    }

    private static void put(
        Map<String, String> variables, Map<String, String> origins, String variable, String value, String origin) {

        String existingOrigin = origins.putIfAbsent(variable, origin);

        if (existingOrigin != null) {
            throw new IllegalArgumentException(
                "The %s and the %s both map to %s; rename one".formatted(existingOrigin, origin, variable));
        }

        variables.put(variable, value);
    }

    private static String sanitize(String name) {
        return NOT_ALLOWED.matcher(name.toUpperCase(Locale.ROOT))
            .replaceAll("_");
    }
}
```

`TreeMap` iteration makes the variable order, and therefore the first origin named in a collision message,
deterministic: `github-bot` sorts before `github_bot`.

- [ ] **Step 7: Run the tests and watch them pass**

Run the Step 4 command. Expected: all `SecretMaskerTest` and `TaskRunnerConnectionEnvironmentTest` tests pass.

- [ ] **Step 8: Format, check, commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:spotlessApply :server:libs:config:app-config:spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:check :server:libs:config:app-config:compileJava
git rev-parse --abbrev-ref HEAD
git add server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl/src/main/java/com/bytechef/platform/component/runner/external/SecretMasker.java server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl/src/main/java/com/bytechef/platform/component/runner/external/TaskRunnerConnectionEnvironment.java server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl/src/test/java/com/bytechef/platform/component/runner/external/SecretMaskerTest.java server/libs/platform/platform-component/platform-component-runner/platform-component-runner-impl/src/test/java/com/bytechef/platform/component/runner/external/TaskRunnerConnectionEnvironmentTest.java server/libs/config/app-config/src/main/java/com/bytechef/config/ApplicationProperties.java server/apps/server-app/src/main/resources/config/application-bytechef.yml
git commit -m "Build the connection environment and masker for external task runners"
```

---

### Task 2: The Process runner injects and masks

**Files:**
- Modify: `EXT/ProcessTaskRunner.java`
- Modify the callers of `new ProcessTaskRunner()` (constructor now takes `ApplicationProperties`):
  - `EXT_TEST/ProcessTaskRunnerTest.java`
  - `server/libs/modules/components/script/src/test/java/com/bytechef/component/script/ScriptExternalRunnerPropertiesTest.java`
  - `server/libs/modules/components/script/src/test/java/com/bytechef/component/script/ScriptComponentHandlerTest.java`
  - `server/libs/modules/components/script/src/test/java/com/bytechef/component/script/action/definition/ScriptActionDefinitionProcessRunnerTest.java`
  - `server/libs/modules/components/commands/src/test/java/com/bytechef/component/commands/CommandsComponentHandlerTest.java`

**Interfaces:**
- Consumes: Task 1's `TaskRunnerConnectionEnvironment.of(TaskRunnerRequest, ApplicationProperties)`, `variables()`,
  `secretMasker()`, and `SecretMasker.mask(String)`.
- Produces: `public ProcessTaskRunner(ApplicationProperties applicationProperties)`, the only constructor. Spring
  injects it, since the class stays `@Component`.

- [ ] **Step 1: Write the failing tests**

In `ProcessTaskRunnerTest`, replace the field `new ProcessTaskRunner()` with
`new ProcessTaskRunner(new ApplicationProperties())`, which keeps every existing test on the switch-off path. Then add:

```java
    @Test
    void testADeclaredConnectionReachesTheProcessAndIsMaskedInStdout() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = connectionRequest(
            List.of(
                "test \"$BYTECHEF_CONNECTION_SLACK_TOKEN\" = 'xoxb-123456' && echo matched",
                "echo \"token=$BYTECHEF_CONNECTION_SLACK_TOKEN\""),
            Map.of());

        TaskRunnerResult result = injectingRunner().run(request);

        assertThat(result.stdout()).contains("matched")
            .contains("token=******")
            .doesNotContain("xoxb-123456");
    }

    @Test
    void testTheAuthorsEnvironmentOverridesAnInjectedVariable() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = connectionRequest(
            List.of("echo \"[$BYTECHEF_CONNECTION_SLACK_TOKEN]\""),
            Map.of("BYTECHEF_CONNECTION_SLACK_TOKEN", "from-the-author"));

        assertThat(injectingRunner().run(request)
            .stdout()).contains("[from-the-author]");
    }

    @Test
    void testAFailureMessageMasksTheConnectionValues() {
        assumeTrue(isExecutable("/bin/sh"));

        TaskRunnerRequest request = connectionRequest(
            List.of("echo \"leak $BYTECHEF_CONNECTION_SLACK_TOKEN\" >&2", "exit 3"), Map.of());

        assertThatThrownBy(() -> injectingRunner().run(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("leak ******")
            .hasMessageNotContaining("xoxb-123456");
    }

    @Test
    void testAPythonScriptReadsTheConnectionAsJson() {
        assumeTrue(isExecutable("/usr/bin/env") && isOnPath("python3"));

        TaskRunnerRequest request = new TaskRunnerRequest(
            "python",
            """
                import json, os

                def perform(input, context):
                    return json.loads(os.environ["BYTECHEF_CONNECTION_SLACK"])["token"] == "xoxb-123456"
                """,
            List.of(), Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Duration.ofSeconds(30), slackConnection(), mock(ActionContext.class));

        assertThat(injectingRunner().run(request)
            .output()).isEqualTo(true);
    }

    private static TaskRunnerRequest connectionRequest(List<String> commands, Map<String, String> env) {
        return new TaskRunnerRequest(
            "shell", null, commands, Map.of(), env, Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of()), Duration.ofSeconds(30), slackConnection(), mock(ActionContext.class));
    }

    private static ProcessTaskRunner injectingRunner() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        applicationProperties.getScript()
            .setInjectConnections(true);

        return new ProcessTaskRunner(applicationProperties);
    }

    private static Map<String, ComponentConnection> slackConnection() {
        return Map.of("slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null));
    }
```

If the test class already has a helper for detecting `python3` (look for how the existing Python test is guarded),
use it in place of `isOnPath`. If none exists, guard with the same `assumeTrue` the existing Python test uses. Add
the imports `com.bytechef.config.ApplicationProperties` and `com.bytechef.platform.component.ComponentConnection`.

In the four component tests, change `new ProcessTaskRunner()` to `new ProcessTaskRunner(applicationProperties)` where
an `applicationProperties` variable is in scope (they already pass it to `GraalVmTaskRunner`). In
`ScriptActionDefinitionProcessRunnerTest`, use `new ProcessTaskRunner(new ApplicationProperties())`.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test --tests '*ProcessTaskRunnerTest'`
Expected: compilation fails because the `ProcessTaskRunner(ApplicationProperties)` constructor does not exist.

- [ ] **Step 3: Implement**

In `ProcessTaskRunner`:

1. Add the field and the constructor:

```java
    private final ApplicationProperties applicationProperties;

    public ProcessTaskRunner(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }
```

2. At the top of the private `run(TaskRunnerRequest, TaskRunnerWorkingDirectory)`, before the source is written:

```java
        TaskRunnerConnectionEnvironment connectionEnvironment = TaskRunnerConnectionEnvironment.of(
            request, applicationProperties);

        SecretMasker secretMasker = connectionEnvironment.secretMasker();
```

3. Pass `connectionEnvironment.variables()` to `applyEnvironment` and apply it before the author's `env`:

```java
    private static void applyEnvironment(
        ProcessBuilder processBuilder, TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory,
        Map<String, String> connectionVariables) {

        Map<String, String> environment = processBuilder.environment();

        boolean inherit = isInheritEnvironmentRequested(request);

        if (!inherit) {
            String path = environment.get("PATH");

            environment.clear();

            if (path != null) {
                environment.put("PATH", path);
            }

            environment.put(
                "HOME", workingDirectory.getPath()
                    .toString());
        }

        environment.putAll(connectionVariables);
        environment.putAll(request.env());
        environment.putAll(workingDirectory.getEnvironment());
    }
```

4. Mask the logged lines: in the two `drain(...)` calls, change the line consumers to log
   `secretMasker.mask(line)` in place of `line`.

5. Mask the captured text everywhere it leaves the runner: in the timeout message, the non-zero-exit message and the
   `TaskRunnerResult` constructor, use `secretMasker.mask(stdout.get())` and `secretMasker.mask(stderr.get())` in place
   of `stdout.get()` and `stderr.get()`.

Add the import `com.bytechef.config.ApplicationProperties`. If SpotBugs flags the stored `ApplicationProperties`
(EI), the class already carries `@SuppressFBWarnings("EI")`.

- [ ] **Step 4: Run and watch them pass**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test --tests '*ProcessTaskRunnerTest'`, then the script and commands module tests:
`./gradlew :server:libs:modules:components:script:test :server:libs:modules:components:commands:test`
Expected: all pass.

- [ ] **Step 5: Format, check, commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:spotlessApply :server:libs:modules:components:script:spotlessApply :server:libs:modules:components:commands:spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:check :server:libs:modules:components:script:check :server:libs:modules:components:commands:check
git rev-parse --abbrev-ref HEAD
git add <ProcessTaskRunner.java, ProcessTaskRunnerTest.java and the four component tests, by path>
git commit -m "Inject and mask task connections in the process task runner"
```

---

### Task 3: The remote runners inject and mask (Docker, Kubernetes, AWS Batch)

**Files:**
- Modify: `EXT/RemoteTaskRunner.java` (`run(TaskRunnerRequest)`, the private `run`, `environment(...)`,
  `CapturingLogSink`)
- Test: `EXT_TEST/RemoteTaskRunnerTest.java`, `EXT_TEST/DockerTaskRunnerBackendIntTest.java`,
  `server/ee/libs/platform/platform-component/platform-component-runner-kubernetes/src/test/java/com/bytechef/ee/platform/component/runner/kubernetes/KubernetesTaskRunnerBackendIntTest.java`,
  `server/ee/libs/platform/platform-component/platform-component-runner-aws-batch/src/test/java/com/bytechef/ee/platform/component/runner/aws/batch/AwsBatchJobFactoryTest.java`

**Interfaces:**
- Consumes: Task 1's `TaskRunnerConnectionEnvironment` and `SecretMasker`.
- Produces: `RemoteJobSpec.environment()` that carries the connection variables, so every backend passes them to its
  workload unchanged. `CapturingLogSink(String type, long maxLogBytes, SecretMasker secretMasker)` is package-private.

- [ ] **Step 1: Write the failing orchestrator tests**

In `RemoteTaskRunnerTest`:
- Add an `applicationProperties` overload, or a parameter on the existing helper, that also calls
  `setInjectConnections(true)`, and a `runner` variant that uses it.
- Leave the existing tests on the switch-off default.

Then add:

```java
    @Test
    void testTheSpecCarriesTheConnectionVariablesBeforeTheAuthorsAndTheContractVariables() {
        FakeBackend backend = new FakeBackend();

        Map<String, ComponentConnection> connections = Map.of(
            "slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null),
            "taskRunner", new ComponentConnection("fake", 1, 2L, Map.of("secret", "runner-secret"), null));

        injectingRunner(backend).run(request(Map.of(), 7L, connections));

        Map<String, String> environment = backend.submittedSpec.environment();

        assertThat(environment).containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456")
            .containsKey("BYTECHEF_CONNECTION_SLACK")
            .doesNotContainKey("BYTECHEF_CONNECTION_TASKRUNNER")
            .containsEntry("BYTECHEF_INPUT_FILE", "/work/input.json");
    }

    @Test
    void testTheAuthorsEnvironmentOverridesAnInjectedVariableInTheSpec() {
        FakeBackend backend = new FakeBackend();

        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getTaskExecutionId()).thenReturn(7L);
        when(actionContext.getJobId()).thenReturn(100L);

        TaskRunnerRequest request = new TaskRunnerRequest(
            "python", SCRIPT, List.of(), Map.of(), Map.of("BYTECHEF_CONNECTION_SLACK_TOKEN", "from-the-author"),
            Map.of(), List.of(), ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), null,
            Map.of("slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null)),
            actionContext);

        injectingRunner(backend).run(request);

        assertThat(backend.submittedSpec.environment())
            .containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "from-the-author");
    }

    @Test
    void testTheJobsOutputIsMaskedInTheResult() {
        FakeBackend backend = new FakeBackend();

        backend.stdout = "token=xoxb-123456\n";
        backend.stderr = "warn xoxb-123456\n";

        TaskRunnerResult result = injectingRunner(backend).run(
            request(Map.of(), 7L, Map.of(
                "slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null))));

        assertThat(result.stdout()).contains("token=******")
            .doesNotContain("xoxb-123456");
        assertThat(result.stderr()).contains("warn ******");
    }

    @Test
    void testAFailedJobsMessageMasksTheConnectionValues() {
        FakeBackend backend = new FakeBackend();

        backend.stderr = "Traceback: bad token xoxb-123456\n";
        backend.exit = RemoteJobExit.of(1);

        assertThatThrownBy(() -> injectingRunner(backend).run(
            request(Map.of(), 7L, Map.of(
                "slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null)))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("bad token ******")
            .hasMessageNotContaining("xoxb-123456");
    }
```

Adjust the use of `FakeBackend`'s `stdout`, `stderr` and `exit` fields to how the existing tests set them. They are
plain fields on the fake, as `RemoteTaskRunnerTest.java:506-610` shows. If a field is `final` or private to the
fake's file scope, set it the way existing tests do. If a test with a connection map trips the remote runner's
existing `taskRunner` connection check (`context.connection() == null`), include the `taskRunner` entry as in the
first test.

- [ ] **Step 2: Run and watch them fail**

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:test --tests '*RemoteTaskRunnerTest'`
Expected: the three new tests fail (no `BYTECHEF_CONNECTION_*` in the spec, unmasked output).

- [ ] **Step 3: Implement**

In `RemoteTaskRunner`:

1. In the public `run(TaskRunnerRequest)`, after `validate(request)`, build the environment before the resources
   open. A collision then fails before anything is created:

```java
        TaskRunnerConnectionEnvironment connectionEnvironment = TaskRunnerConnectionEnvironment.of(
            request, applicationProperties);

        try (TaskRunnerWorkingDirectory workingDirectory = TaskRunnerWorkingDirectory.create(request);
            CapturingLogSink logSink = new CapturingLogSink(
                getType(), limits().getMaxLogBytes(), connectionEnvironment.secretMasker())) {

            return run(request, workingDirectory, logSink, connectionEnvironment.variables());
        }
```

   Keep the existing catch clauses.

2. Thread `Map<String, String> connectionVariables` through the private `run(...)` into `environment(...)`:

```java
    private static Map<String, String> environment(
        TaskRunnerRequest request, String remoteWorkingDirectory, Map<String, String> connectionVariables) {

        String directory = remoteWorkingDirectory.length() > 1 && remoteWorkingDirectory.endsWith("/")
            ? remoteWorkingDirectory.substring(0, remoteWorkingDirectory.length() - 1)
            : remoteWorkingDirectory;

        Map<String, String> environment = new LinkedHashMap<>();

        environment.put("HOME", directory);
        environment.putAll(connectionVariables);
        environment.putAll(request.env());
        environment.put("BYTECHEF_WORKING_DIR", directory);
        environment.put("BYTECHEF_INPUT_FILE", directory + "/" + RemoteJobSpec.INPUT_FILE_NAME);
        environment.put("BYTECHEF_OUTPUT_DIR", directory + "/" + RemoteJobSpec.OUTPUT_DIRECTORY_NAME);

        return environment;
    }
```

3. In `CapturingLogSink`, add a `private final SecretMasker secretMasker` field and a third constructor parameter.
   - Mask the logged lines: `log.info("[{}] {}", type, secretMasker.mask(line))`, and the same in the `warn`
     consumer.
   - Mask what is read back:

```java
        String stdout() {
            return secretMasker.mask(stdout.get());
        }

        String stderr() {
            return secretMasker.mask(stderr.get());
        }
```

   Every failure message and the result already read through `stdout()` and `stderr()`. That includes the `.stderr`
   file copied back from Kubernetes and AWS Batch, which is appended to the same capture.

- [ ] **Step 4: Run and watch them pass**

Run the Step 2 command. Expected: all `RemoteTaskRunnerTest` tests pass.

- [ ] **Step 5: The Docker integration case**

In `DockerTaskRunnerBackendIntTest`:
- Add a `commandsRequest` overload that takes `Map<String, ComponentConnection>`.
- Make the `applicationProperties()` helper call `setInjectConnections(true)`. This is harmless for the existing
  tests, which declare no connections.

Then add:

```java
    @Test
    void testADeclaredConnectionReachesTheContainerAndIsMasked() {
        TaskRunnerRequest request = new TaskRunnerRequest(
            "shell", null,
            List.of(
                "test \"$BYTECHEF_CONNECTION_SLACK_TOKEN\" = 'xoxb-123456' && echo matched",
                "echo \"token=$BYTECHEF_CONNECTION_SLACK_TOKEN\""),
            Map.of(), Map.of(), Map.of(), List.of(), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of("image", IMAGE)), TIMEOUT,
            Map.of("slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null)),
            storingActionContext(new LinkedHashMap<>(), null));

        TaskRunnerResult taskRunnerResult = taskRunner().run(request);

        assertThat(taskRunnerResult.stdout()).contains("matched")
            .contains("token=******")
            .doesNotContain("xoxb-123456");
    }
```

Run: `./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:testIntegration --tests '*DockerTaskRunnerBackendIntTest'` in the background (Docker required).
Expected: pass. The test skips itself when the daemon is unreachable, which its existing `assumeTrue` handles.

- [ ] **Step 6: The Kubernetes (k3s) integration case**

In `KubernetesTaskRunnerBackendIntTest`, give the `connections()` helper an optional extra entry, or add a sibling
helper, so that one test declares
`"slack", new ComponentConnection("slack", 1, 1L, Map.of("token", "xoxb-123456"), null)` next to the existing
`taskRunner` entry. Make `applicationProperties()` call `setInjectConnections(true)`. Then add
`testADeclaredConnectionReachesThePodAndIsMasked`, using the same two commands and assertions as Step 5. Build the
request through the existing `commandsRequest(...)` shape, with `actionContext(null, new LinkedHashMap<>())`, as the
other tests do.

Run: `./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:testIntegration --tests '*KubernetesTaskRunnerBackendIntTest'` in the background (Docker required; k3s takes a few minutes).
Expected: pass.

- [ ] **Step 7: The AWS Batch job-factory case**

In `AwsBatchJobFactoryTest`, add a test that builds the spec with an extra environment entry
`BYTECHEF_CONNECTION_SLACK_TOKEN=xoxb-123456`, through the same `RemoteJobSpec` constructor the existing helper at
line ~191 uses. Give the helper an environment parameter if needed. The test asserts:

```java
        assertThat(environment(containers.get("main"))).containsEntry("BYTECHEF_CONNECTION_SLACK_TOKEN", "xoxb-123456");
        assertThat(environment(containers.get("bytechef-init"))).doesNotContainKey("BYTECHEF_CONNECTION_SLACK_TOKEN");
        assertThat(environment(containers.get("bytechef-output"))).doesNotContainKey("BYTECHEF_CONNECTION_SLACK_TOKEN");
```

Name it `testConnectionVariablesReachOnlyTheMainContainer`. Run:
`./gradlew :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:test --tests '*AwsBatchJobFactoryTest'`
Expected: pass. The factory already copies `spec.environment()` into `main` only. This test pins that.

- [ ] **Step 8: Format, check, commit**

```bash
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:spotlessApply :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:spotlessApply
./gradlew :server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl:check :server:ee:libs:platform:platform-component:platform-component-runner-kubernetes:check :server:ee:libs:platform:platform-component:platform-component-runner-aws-batch:check
git rev-parse --abbrev-ref HEAD
git add <RemoteTaskRunner.java and the four test files, by path>
git commit -m "Inject and mask task connections in the remote task runners"
```

`check` runs the integration tests in some modules. Run it in the background and poll the log.

---

### Task 4: Documentation

**Files:**
- Modify: `server/libs/modules/components/commands/src/main/resources/README.mdx`,
  `server/libs/modules/components/script/src/main/resources/README.mdx`
- Modify: `docs/superpowers/specs/2026-10-14-remote-task-runners-design.md` (§13, only if the implementation
  deviated)

**Interfaces:**
- Consumes: the behaviour of Tasks 1–3.

- [ ] **Step 1: Commands README**

Add a section "Connections in external runners", next to the runner sections. It covers:
- **What is injected:** every connection the task declares, except `taskRunner`, becomes
  `BYTECHEF_CONNECTION_<KEY>`, a JSON object with all parameters, plus `BYTECHEF_CONNECTION_<KEY>_<PARAM>` for each
  parameter. Name the sanitising rule and give the `github-bot` → `GITHUB_BOT` example.
- **Precedence:** the task's own `env` overrides an injected name. A one-line shell remap example is useful for tools
  that expect fixed names: `export AWS_ACCESS_KEY_ID="$BYTECHEF_CONNECTION_AWS_ACCESS_KEY_ID"`.
- **Masking:** values of 6+ characters are replaced by `******` in `stdout`, `stderr`, failure messages and the server
  log. It is best effort: a transformed value is not caught, and `vars` and `outputFiles` are not masked.
- **Exposure:** the table from spec §13.4, giving who can read environment variables on Process, Docker, Kubernetes
  and AWS Batch.
- **Snapshot:** values are read when the run starts. An OAuth token is not refreshed during the run.
- **Operator switch:** `bytechef.script.inject-connections`, default `true`, under the existing operator YAML example.
- **GraalVM:** scripts there use connections through `context.component…` and never see the values.

In the AWS Batch section's manual verification checklist, add: "A task declaring a connection sees
`BYTECHEF_CONNECTION_*` in `main`, and its value appears as `******` in the task's stdout."

- [ ] **Step 2: Script README**

Add a short paragraph with the variable names, precedence and masking. Point to the Commands README for the
exposure table rather than duplicating it.

- [ ] **Step 3: Spec**

Read spec §13 against the code as built. Where it differs, update §13 to match, unless the code is wrong. This
plan already reconciled §13.1's precedence and §13.3's masking points with the code, in the same commit as the plan.

- [ ] **Step 4: Commit**

```bash
git rev-parse --abbrev-ref HEAD
git add server/libs/modules/components/commands/src/main/resources/README.mdx server/libs/modules/components/script/src/main/resources/README.mdx docs/superpowers/specs/2026-10-14-remote-task-runners-design.md
git commit -m "Document task connections in external runners"
```
