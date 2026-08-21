# ByteChef CLI Public API — Foundation + Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the ByteChef CLI with commands that call the Automation public REST API, on a reusable foundation (config profiles, auth, generated HTTP client, output, errors).

**Architecture:** Spring Boot + Spring Shell app. A new Apache/CE `cli-core` module owns profiles, auth interception, output rendering and error/exit-code mapping. A `clients/automation-configuration` module holds an OpenAPI-generated Java client (native java.net.http). Thin `@Command` classes in `commands/automation` and `commands/config` call the client and hand results to `cli-core`.

**Tech Stack:** Java, Spring Boot, Spring Shell 4.0.1, org.openapi.generator 7.22.0 (`java` generator, `native` library), Jackson, JUnit 5, JDK `com.sun.net.httpserver.HttpServer` for test stubs.

## Global Constraints

- Apache 2.0 license header (copy verbatim from `cli/commands/component/.../ComponentCommand.java`) at the top of every new `.java` file.
- Command style: Spring Shell `@Command(name = "...")` methods with `@Option` parameters, matching `ComponentCommand`.
- Public API base path: host + `/api/automation/v1`.
- Every API request carries `Authorization: Bearer <token>` and `X-Environment: <environment>` (DEVELOPMENT|STAGING|PRODUCTION).
- Config file: `~/.bytechef/config`, INI with named `[profile]` sections, file mode `600`.
- Config resolution precedence (highest first): per-command flag → env var (`BYTECHEF_HOST`, `BYTECHEF_TOKEN`, `BYTECHEF_ENVIRONMENT`, `BYTECHEF_WORKSPACE_ID`) → selected profile (default profile name: `default`).
- Exit codes: `0` success, `1` generic, `2` auth (401/403), `3` not found (404), `4` config missing/invalid.
- Default output format `json`; `--output table` where a table view is defined.
- New Gradle modules use `com.bytechef.java-library-conventions`; register each in `settings.gradle.kts`.
- Package roots: `com.bytechef.cli.core.*`, `com.bytechef.cli.command.automation.*`, `com.bytechef.cli.command.config.*`.

---

### Task 1: `cli-core` config model + INI config file read/write

**Files:**
- Create: `cli/cli-core/build.gradle.kts`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/config/Environment.java`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/config/Profile.java`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/config/ConfigFile.java`
- Modify: `settings.gradle.kts` (add `include("cli:cli-core")` near line 29)
- Test: `cli/cli-core/src/test/java/com/bytechef/cli/core/config/ConfigFileTest.java`

**Interfaces:**
- Produces:
  - `enum Environment { DEVELOPMENT, STAGING, PRODUCTION }`
  - `record Profile(String host, String token, Environment environment, Long workspaceId)`
  - `class ConfigFile` with `static Map<String,Profile> read(Path file)`, `static void write(Path file, String profileName, Profile profile)` (upserts one section, sets POSIX perms `rw-------`).

- [ ] **Step 1: Register the module and create the build file**

Add to `settings.gradle.kts` after `include("cli:cli-app")`:
```kotlin
include("cli:cli-core")
```
Create `cli/cli-core/build.gradle.kts`:
```kotlin
plugins {
    id("com.bytechef.java-library-conventions")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

- [ ] **Step 2: Write the failing test**

Create `ConfigFileTest.java` (with Apache header):
```java
package com.bytechef.cli.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteThenReadRoundTrips() throws Exception {
        Path file = tempDir.resolve("config");

        ConfigFile.write(file, "default",
            new Profile("https://app.bytechef.io", "btc_x", Environment.PRODUCTION, 1L));
        ConfigFile.write(file, "staging",
            new Profile("https://staging.bytechef.io", "btc_y", Environment.STAGING, 4L));

        Map<String, Profile> profiles = ConfigFile.read(file);

        assertEquals("https://app.bytechef.io", profiles.get("default").host());
        assertEquals(Environment.STAGING, profiles.get("staging").environment());
        assertEquals(4L, profiles.get("staging").workspaceId());
    }

    @Test
    void testWriteSetsOwnerOnlyPermissions() throws Exception {
        Path file = tempDir.resolve("config");

        ConfigFile.write(file, "default",
            new Profile("https://app.bytechef.io", "btc_x", Environment.PRODUCTION, 1L));

        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);

        assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
        assertEquals(2, perms.size(), "only owner read/write expected");
    }

    @Test
    void testReadMissingFileReturnsEmpty() throws Exception {
        assertTrue(ConfigFile.read(tempDir.resolve("nope")).isEmpty());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.config.ConfigFileTest"`
Expected: FAIL (classes `Environment`/`Profile`/`ConfigFile` do not exist).

- [ ] **Step 4: Write minimal implementation**

`Environment.java`:
```java
package com.bytechef.cli.core.config;

public enum Environment {
    DEVELOPMENT, STAGING, PRODUCTION
}
```
`Profile.java`:
```java
package com.bytechef.cli.core.config;

public record Profile(String host, String token, Environment environment, Long workspaceId) {
}
```
`ConfigFile.java`:
```java
package com.bytechef.cli.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigFile {

    private ConfigFile() {
    }

    public static Map<String, Profile> read(Path file) throws IOException {
        Map<String, Profile> profiles = new LinkedHashMap<>();

        if (!Files.exists(file)) {
            return profiles;
        }

        String section = null;
        Map<String, String> kv = new LinkedHashMap<>();

        List<String> lines = Files.readAllLines(file);

        for (String raw : lines) {
            String line = raw.trim();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            if (line.startsWith("[") && line.endsWith("]")) {
                if (section != null) {
                    profiles.put(section, toProfile(kv));
                }

                section = line.substring(1, line.length() - 1).trim();
                kv = new LinkedHashMap<>();
            } else {
                int eq = line.indexOf('=');

                if (eq > 0 && section != null) {
                    kv.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        }

        if (section != null) {
            profiles.put(section, toProfile(kv));
        }

        return profiles;
    }

    public static void write(Path file, String profileName, Profile profile) throws IOException {
        Files.createDirectories(file.getParent());

        Map<String, Profile> profiles = read(file);
        profiles.put(profileName, profile);

        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Profile> e : profiles.entrySet()) {
            Profile p = e.getValue();

            sb.append('[').append(e.getKey()).append("]\n");
            sb.append("host = ").append(p.host()).append('\n');
            sb.append("token = ").append(p.token()).append('\n');
            sb.append("environment = ").append(p.environment()).append('\n');

            if (p.workspaceId() != null) {
                sb.append("workspace_id = ").append(p.workspaceId()).append('\n');
            }

            sb.append('\n');
        }

        Files.writeString(file, sb.toString());
        Files.setPosixFilePermissions(
            file, EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
    }

    private static Profile toProfile(Map<String, String> kv) {
        String ws = kv.get("workspace_id");
        String env = kv.get("environment");

        return new Profile(
            kv.get("host"),
            kv.get("token"),
            env == null ? null : Environment.valueOf(env),
            ws == null ? null : Long.valueOf(ws));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.config.ConfigFileTest"`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts cli/cli-core
git commit -m "feat(cli): add cli-core config model and INI config file"
```

---

### Task 2: Profile resolution with flag > env > file precedence

**Files:**
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/config/CliConfig.java`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/config/ProfileResolver.java`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/error/CliException.java`
- Test: `cli/cli-core/src/test/java/com/bytechef/cli/core/config/ProfileResolverTest.java`

**Interfaces:**
- Consumes: `Profile`, `Environment`, `ConfigFile` (Task 1).
- Produces:
  - `record CliConfig(String host, String token, Environment environment, Long workspaceId)`
  - `record Overrides(String host, String token, Environment environment, Long workspaceId, String profileName)` (any field may be null)
  - `class ProfileResolver` with constructor `ProfileResolver(Path configFile, Map<String,String> env)` and `CliConfig resolve(Overrides overrides)`. Throws `CliException(4, msg)` when host or token cannot be resolved.
  - `class CliException extends RuntimeException` with `int exitCode` and `CliException(int exitCode, String message)`.

- [ ] **Step 1: Write the failing test**

`ProfileResolverTest.java`:
```java
package com.bytechef.cli.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.cli.core.error.CliException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileResolverTest {

    @TempDir
    Path tempDir;

    private Path writeDefault() throws Exception {
        Path file = tempDir.resolve("config");
        ConfigFile.write(file, "default",
            new Profile("https://file-host", "file-token", Environment.PRODUCTION, 1L));
        return file;
    }

    @Test
    void testFileValuesUsedWhenNoOverrides() throws Exception {
        ProfileResolver resolver = new ProfileResolver(writeDefault(), Map.of());

        CliConfig config = resolver.resolve(new Overrides(null, null, null, null, null));

        assertEquals("https://file-host", config.host());
        assertEquals("file-token", config.token());
        assertEquals(1L, config.workspaceId());
    }

    @Test
    void testEnvOverridesFile() throws Exception {
        ProfileResolver resolver = new ProfileResolver(
            writeDefault(), Map.of("BYTECHEF_HOST", "https://env-host", "BYTECHEF_TOKEN", "env-token"));

        CliConfig config = resolver.resolve(new Overrides(null, null, null, null, null));

        assertEquals("https://env-host", config.host());
        assertEquals("env-token", config.token());
    }

    @Test
    void testFlagOverridesEnvAndFile() throws Exception {
        ProfileResolver resolver = new ProfileResolver(
            writeDefault(), Map.of("BYTECHEF_HOST", "https://env-host"));

        CliConfig config = resolver.resolve(
            new Overrides("https://flag-host", null, null, 9L, null));

        assertEquals("https://flag-host", config.host());
        assertEquals(9L, config.workspaceId());
    }

    @Test
    void testMissingTokenThrowsExitCode4() throws Exception {
        ProfileResolver resolver = new ProfileResolver(tempDir.resolve("none"), Map.of());

        CliException ex = assertThrows(CliException.class,
            () -> resolver.resolve(new Overrides("https://h", null, null, null, null)));

        assertEquals(4, ex.exitCode());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.config.ProfileResolverTest"`
Expected: FAIL (missing classes).

- [ ] **Step 3: Write minimal implementation**

`CliException.java`:
```java
package com.bytechef.cli.core.error;

public class CliException extends RuntimeException {

    private final int exitCode;

    public CliException(int exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
```
`CliConfig.java`:
```java
package com.bytechef.cli.core.config;

public record CliConfig(String host, String token, Environment environment, Long workspaceId) {
}
```
Create `Overrides.java`:
```java
package com.bytechef.cli.core.config;

public record Overrides(
    String host, String token, Environment environment, Long workspaceId, String profileName) {
}
```
`ProfileResolver.java`:
```java
package com.bytechef.cli.core.config;

import com.bytechef.cli.core.error.CliException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ProfileResolver {

    private final Path configFile;
    private final Map<String, String> env;

    public ProfileResolver(Path configFile, Map<String, String> env) {
        this.configFile = configFile;
        this.env = env;
    }

    public CliConfig resolve(Overrides overrides) {
        Profile profile = readProfile(
            overrides.profileName() == null ? "default" : overrides.profileName());

        String host = pick(overrides.host(), env.get("BYTECHEF_HOST"), profile == null ? null : profile.host());
        String token = pick(overrides.token(), env.get("BYTECHEF_TOKEN"), profile == null ? null : profile.token());

        Environment environment = overrides.environment() != null
            ? overrides.environment()
            : env.containsKey("BYTECHEF_ENVIRONMENT")
                ? Environment.valueOf(env.get("BYTECHEF_ENVIRONMENT"))
                : profile == null || profile.environment() == null ? Environment.PRODUCTION : profile.environment();

        Long workspaceId = overrides.workspaceId() != null
            ? overrides.workspaceId()
            : env.containsKey("BYTECHEF_WORKSPACE_ID")
                ? Long.valueOf(env.get("BYTECHEF_WORKSPACE_ID"))
                : profile == null ? null : profile.workspaceId();

        if (host == null || host.isBlank()) {
            throw new CliException(4, "No host configured. Run 'bytechef configure' or pass --host.");
        }

        if (token == null || token.isBlank()) {
            throw new CliException(4, "No token configured. Run 'bytechef configure' or pass --token.");
        }

        return new CliConfig(host, token, environment, workspaceId);
    }

    private Profile readProfile(String name) {
        try {
            return ConfigFile.read(configFile)
                .get(name);
        } catch (IOException e) {
            throw new CliException(4, "Cannot read config file: " + e.getMessage());
        }
    }

    private static String pick(String flag, String envValue, String fileValue) {
        if (flag != null) {
            return flag;
        }

        if (envValue != null) {
            return envValue;
        }

        return fileValue;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.config.ProfileResolverTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add cli/cli-core
git commit -m "feat(cli): add profile resolution with flag/env/file precedence"
```

---

### Task 3: Output renderer (JSON + table)

**Files:**
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/output/OutputFormat.java`
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/output/OutputRenderer.java`
- Test: `cli/cli-core/src/test/java/com/bytechef/cli/core/output/OutputRendererTest.java`

**Interfaces:**
- Produces:
  - `enum OutputFormat { JSON, TABLE }`
  - `class OutputRenderer` with:
    - `OutputRenderer(java.io.PrintStream out)`
    - `void renderJson(Object value)` — pretty JSON via a shared Jackson `ObjectMapper` (registers `JavaTimeModule`, disables timestamps).
    - `void renderTable(List<String> headers, List<List<String>> rows)` — aligned columns.
    - `void message(String text)` — one-line message (for 204s).

- [ ] **Step 1: Write the failing test**

`OutputRendererTest.java`:
```java
package com.bytechef.cli.core.output;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutputRendererTest {

    @Test
    void testRenderJsonPrettyPrints() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        OutputRenderer renderer = new OutputRenderer(new PrintStream(buffer));

        renderer.renderJson(Map.of("id", 7, "status", "COMPLETED"));

        String out = buffer.toString();

        assertTrue(out.contains("\"status\""), "should contain key");
        assertTrue(out.contains("\n"), "should be pretty (multi-line)");
    }

    @Test
    void testRenderTableAlignsColumns() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        OutputRenderer renderer = new OutputRenderer(new PrintStream(buffer));

        renderer.renderTable(List.of("ID", "STATUS"),
            List.of(List.of("7", "COMPLETED"), List.of("42", "FAILED")));

        String out = buffer.toString();

        assertTrue(out.contains("ID"));
        assertTrue(out.contains("COMPLETED"));
        assertTrue(out.contains("FAILED"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.output.OutputRendererTest"`
Expected: FAIL (missing classes).

- [ ] **Step 3: Add Jackson JSR-310 dependency and implement**

Add to `cli/cli-core/build.gradle.kts` dependencies:
```kotlin
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
```
`OutputFormat.java`:
```java
package com.bytechef.cli.core.output;

public enum OutputFormat {
    JSON, TABLE
}
```
`OutputRenderer.java`:
```java
package com.bytechef.cli.core.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.PrintStream;
import java.util.List;

public class OutputRenderer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT);

    private final PrintStream out;

    public OutputRenderer(PrintStream out) {
        this.out = out;
    }

    public void renderJson(Object value) {
        try {
            out.println(OBJECT_MAPPER.writeValueAsString(value));
        } catch (Exception e) {
            throw new RuntimeException("Failed to render JSON: " + e.getMessage(), e);
        }
    }

    public void renderTable(List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];

        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i)
                .length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                widths[i] = Math.max(widths[i], row.get(i)
                    .length());
            }
        }

        out.println(formatRow(headers, widths));

        for (List<String> row : rows) {
            out.println(formatRow(row, widths));
        }
    }

    public void message(String text) {
        out.println(text);
    }

    private static String formatRow(List<String> cells, int[] widths) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append("  ");
            }

            sb.append(String.format("%-" + widths[i] + "s", cells.get(i)));
        }

        return sb.toString();
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.output.OutputRendererTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add cli/cli-core
git commit -m "feat(cli): add JSON/table output renderer"
```

---

### Task 4: `execute():int` entrypoint + exit-code mapping (refactor cli-app)

**Files:**
- Modify: `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java`
- Modify: `cli/cli-app/build.gradle.kts` (add `implementation(project(":cli:cli-core"))`)
- Modify: `cli/commands/component/src/test/java/com/bytechef/cli/command/component/init/ComponentInitCommandTest.java` (call `execute` instead of `main`)
- Test: `cli/cli-app/src/test/java/com/bytechef/cli/CliApplicationExitCodeTest.java`

**Interfaces:**
- Consumes: `CliException` (Task 2).
- Produces: `static int CliApplication.execute(String... args)` — runs the shell, returns `0` on success, `CliException.exitCode()` on a thrown `CliException`, `1` on any other exception; prints error messages to `System.err`. `main` calls `System.exit(execute(args))`.

- [ ] **Step 1: Write the failing test**

`CliApplicationExitCodeTest.java`:
```java
package com.bytechef.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CliApplicationExitCodeTest {

    @Test
    void testUnknownCommandReturnsNonZero() {
        int code = CliApplication.execute("definitely-not-a-command");

        assertEquals(1, code);
    }

    @Test
    void testNoArgsReturnsZero() {
        int code = CliApplication.execute();

        assertEquals(0, code);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:cli-app:test --tests "com.bytechef.cli.CliApplicationExitCodeTest"`
Expected: FAIL (`execute` not defined).

- [ ] **Step 3: Implement `execute` and update `main`**

Rewrite the body of `CliApplication` (keep the Apache header, `@SpringBootApplication`, `@EnableCommand`, and `shellRunner` bean). Replace `main` and add `execute`:
```java
    public static void main(String... args) {
        System.exit(execute(args));
    }

    public static int execute(String... args) {
        try {
            SpringApplication.run(CliApplication.class, args);

            return 0;
        } catch (Throwable t) {
            Throwable cause = t;

            while (cause != null) {
                if (cause instanceof com.bytechef.cli.core.error.CliException cliException) {
                    System.err.println(cliException.getMessage());

                    return cliException.exitCode();
                }

                cause = cause.getCause();
            }

            System.err.println(t.getMessage());

            return 1;
        }
    }
```
Add to `cli/cli-app/build.gradle.kts` dependencies:
```kotlin
    implementation(project(":cli:cli-core"))
```

- [ ] **Step 4: Update the existing component test to use `execute`**

In `ComponentInitCommandTest.java`, replace both `CliApplication.main(` calls with `CliApplication.execute(`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :cli:cli-app:test :cli:commands:component:test`
Expected: PASS (new exit-code tests + existing component tests).

- [ ] **Step 6: Commit**

```bash
git add cli/cli-app cli/commands/component
git commit -m "feat(cli): add execute() entrypoint with exit-code mapping"
```

---

### Task 5: `bytechef configure` command

**Files:**
- Create: `cli/commands/config/build.gradle.kts`
- Create: `cli/commands/config/src/main/java/com/bytechef/cli/command/config/ConfigureCommand.java`
- Modify: `settings.gradle.kts` (add `include("cli:commands:config")`)
- Modify: `cli/cli-app/src/main/java/com/bytechef/cli/CliApplication.java` (add `ConfigureCommand.class` to `@EnableCommand`)
- Modify: `cli/cli-app/build.gradle.kts` (add `implementation(project(":cli:commands:config"))`)
- Test: `cli/commands/config/src/test/java/com/bytechef/cli/command/config/ConfigureCommandTest.java`

**Interfaces:**
- Consumes: `ConfigFile`, `Profile`, `Environment` (Task 1).
- Produces: `ConfigureCommand` exposing `configure` with options `--profile` (default `default`), `--host`, `--token`, `--environment` (default `PRODUCTION`), `--workspace-id`. Writes the profile to `configPath()` (default `~/.bytechef/config`, overridable via package-visible setter for tests).

- [ ] **Step 1: Create build file and register module**

`cli/commands/config/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":cli:cli-core"))
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```
Add to `settings.gradle.kts`:
```kotlin
include("cli:commands:config")
```

- [ ] **Step 2: Write the failing test**

`ConfigureCommandTest.java`:
```java
package com.bytechef.cli.command.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.cli.core.config.ConfigFile;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Profile;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigureCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testConfigureWritesProfile() throws Exception {
        Path configFile = tempDir.resolve("config");

        ConfigureCommand command = new ConfigureCommand();
        command.setConfigPath(configFile);

        command.configure("default", "https://app.bytechef.io", "btc_x", "STAGING", 3L);

        Map<String, Profile> profiles = ConfigFile.read(configFile);
        Profile profile = profiles.get("default");

        assertEquals("https://app.bytechef.io", profile.host());
        assertEquals(Environment.STAGING, profile.environment());
        assertEquals(3L, profile.workspaceId());
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :cli:commands:config:test`
Expected: FAIL (`ConfigureCommand` missing).

- [ ] **Step 4: Implement `ConfigureCommand`**

```java
package com.bytechef.cli.command.config;

import com.bytechef.cli.core.config.ConfigFile;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Profile;
import java.nio.file.Path;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

@org.springframework.stereotype.Component
public class ConfigureCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");

    @Command(name = "configure", description = "Store host, token and environment in a named profile.")
    public void configure(
        @Option(longName = "profile", description = "profile name", defaultValue = "default") String profile,
        @Option(longName = "host", description = "ByteChef host URL", required = true) String host,
        @Option(longName = "token", description = "public API token", required = true) String token,
        @Option(
            longName = "environment", description = "DEVELOPMENT|STAGING|PRODUCTION",
            defaultValue = "PRODUCTION") String environment,
        @Option(longName = "workspace-id", description = "default workspace id") Long workspaceId) {

        try {
            ConfigFile.write(
                configPath, profile,
                new Profile(host, token, Environment.valueOf(environment), workspaceId));

            System.out.println("Saved profile '" + profile + "'.");
        } catch (Exception e) {
            throw new RuntimeException("Failed to write config: " + e.getMessage(), e);
        }
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }
}
```
Add `ConfigureCommand.class` to the `@EnableCommand({...})` array in `CliApplication.java` and `implementation(project(":cli:commands:config"))` to `cli/cli-app/build.gradle.kts`.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :cli:commands:config:test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts cli/commands/config cli/cli-app
git commit -m "feat(cli): add configure command"
```

---

### Task 6: Generate the automation-configuration Java client

**Files:**
- Create: `cli/clients/automation-configuration/build.gradle.kts`
- Create: `cli/clients/automation-configuration/openapi.yaml` (copy of the automation public spec)
- Modify: `settings.gradle.kts` (add `include("cli:clients:automation-configuration")`)

**Interfaces:**
- Produces (generated into `generated/src/main/java`, committed):
  - Package `com.bytechef.cli.client.automation.api`: `WorkflowExecutionApi`, `ProjectCodeWorkflowApi`, `ProjectGitApi`, and `ApiClient` (native java.net.http; has `updateBaseUri(String)` and `setRequestInterceptor(java.util.function.Consumer<java.net.http.HttpRequest.Builder>)`).
  - Package `com.bytechef.cli.client.automation.model`: `PageModel`, `WorkflowExecutionModel`, `WorkflowExecutionBasicModel`, `EnvironmentModel`, `WorkflowExecutionStatusModel`, `TaskExecutionModel`, `ExecutionErrorModel`.
  - Method shapes the command tasks rely on:
    - `WorkflowExecutionApi.getWorkflowExecutionsPage(Long workspaceId, EnvironmentModel xEnvironment, WorkflowExecutionStatusModel status, java.time.OffsetDateTime startDate, java.time.OffsetDateTime endDate, Long projectId, Long projectDeploymentId, String workflowId, Integer pageNumber)` → `PageModel`
    - `WorkflowExecutionApi.getWorkflowExecution(Long id)` → `WorkflowExecutionModel`
    - `ProjectCodeWorkflowApi.deployProject(Long workspaceId, java.io.File projectFile)` → `void`
    - `ProjectGitApi.pullProjectFromGit(Long id)` → `void`

- [ ] **Step 1: Copy the spec and register the module**

```bash
mkdir -p cli/clients/automation-configuration
cp server/ee/libs/automation/automation-configuration/automation-configuration-public-rest/openapi.yaml \
   cli/clients/automation-configuration/openapi.yaml
```
Add to `settings.gradle.kts`:
```kotlin
include("cli:clients:automation-configuration")
```

- [ ] **Step 2: Write the client generation build file**

`cli/clients/automation-configuration/build.gradle.kts`:
```kotlin
plugins {
    id("com.bytechef.java-library-conventions")
    alias(libs.plugins.org.openapi.generator)
}

val generateClient by tasks.registering(org.openapitools.generator.gradle.plugin.tasks.GenerateTask::class) {
    generatorName.set("java")
    library.set("native")
    inputSpec.set("$projectDir/openapi.yaml")
    outputDir.set("$projectDir/generated")
    apiPackage.set("com.bytechef.cli.client.automation.api")
    modelPackage.set("com.bytechef.cli.client.automation.model")
    invokerPackage.set("com.bytechef.cli.client.automation")
    modelNameSuffix.set("Model")
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "useTags" to "true",
            "hideGenerationTimestamp" to "true",
            "openApiNullable" to "false"
        )
    )
}

sourceSets.main.get().java.srcDir("$projectDir/generated/src/main/java")

tasks.compileJava {
    dependsOn(generateClient)
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
```

- [ ] **Step 3: Generate and compile the client**

Run: `./gradlew :cli:clients:automation-configuration:compileJava`
Expected: BUILD SUCCESSFUL; `cli/clients/automation-configuration/generated/src/main/java/com/bytechef/cli/client/automation/api/WorkflowExecutionApi.java` exists.

- [ ] **Step 4: Verify the generated method signatures**

Run: `grep -n "public PageModel getWorkflowExecutionsPage\|public WorkflowExecutionModel getWorkflowExecution\|deployProject\|pullProjectFromGit" cli/clients/automation-configuration/generated/src/main/java/com/bytechef/cli/client/automation/api/*.java`
Expected: the four methods with the signatures listed in Interfaces. If a name differs (e.g. `xEnvironment` param type), note the actual signature — later tasks call these methods.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts cli/clients/automation-configuration
git commit -m "feat(cli): generate automation-configuration java client"
```

---

### Task 7: Auth interceptor + client factory in `cli-core`

**Files:**
- Create: `cli/cli-core/src/main/java/com/bytechef/cli/core/http/AuthInterceptor.java`
- Test: `cli/cli-core/src/test/java/com/bytechef/cli/core/http/AuthInterceptorTest.java`

**Interfaces:**
- Consumes: `CliConfig`, `Environment` (Task 2).
- Produces: `class AuthInterceptor implements java.util.function.Consumer<java.net.http.HttpRequest.Builder>` constructed with `AuthInterceptor(CliConfig config)`; on `accept`, adds headers `Authorization: Bearer <token>` and `X-Environment: <environment>`. Also `static String baseUri(CliConfig config)` → `config.host() + "/api/automation/v1"`.

- [ ] **Step 1: Write the failing test**

`AuthInterceptorTest.java`:
```java
package com.bytechef.cli.core.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import java.net.URI;
import java.net.http.HttpRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuthInterceptorTest {

    @Test
    void testAddsAuthorizationAndEnvironmentHeaders() {
        CliConfig config = new CliConfig("https://h", "btc_x", Environment.STAGING, 1L);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("https://h/api/automation/v1/ping"));

        new AuthInterceptor(config).accept(builder);

        HttpRequest request = builder.GET()
            .build();

        assertEquals(List.of("Bearer btc_x"), request.headers()
            .allValues("Authorization"));
        assertEquals(List.of("STAGING"), request.headers()
            .allValues("X-Environment"));
    }

    @Test
    void testBaseUriAppendsApiPath() {
        assertTrue(AuthInterceptor.baseUri(
            new CliConfig("https://h", "t", Environment.PRODUCTION, null))
            .endsWith("/api/automation/v1"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.http.AuthInterceptorTest"`
Expected: FAIL (`AuthInterceptor` missing).

- [ ] **Step 3: Implement `AuthInterceptor`**

```java
package com.bytechef.cli.core.http;

import com.bytechef.cli.core.config.CliConfig;
import java.net.http.HttpRequest;
import java.util.function.Consumer;

public class AuthInterceptor implements Consumer<HttpRequest.Builder> {

    private final CliConfig config;

    public AuthInterceptor(CliConfig config) {
        this.config = config;
    }

    @Override
    public void accept(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + config.token());
        builder.header("X-Environment", config.environment()
            .name());
    }

    public static String baseUri(CliConfig config) {
        return config.host() + "/api/automation/v1";
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :cli:cli-core:test --tests "com.bytechef.cli.core.http.AuthInterceptorTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add cli/cli-core
git commit -m "feat(cli): add auth interceptor and base-uri helper"
```

---

### Task 8: `automation execution list|get` commands

**Files:**
- Create: `cli/commands/automation/build.gradle.kts`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationClientFactory.java`
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationExecutionCommand.java`
- Create (test helper): `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/StubApi.java`
- Modify: `settings.gradle.kts` (add `include("cli:commands:automation")`)
- Modify: `cli/cli-app/CliApplication.java` (+`AutomationExecutionCommand.class`) and `cli/cli-app/build.gradle.kts` (+`project(":cli:commands:automation")`)
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationExecutionCommandTest.java`

**Interfaces:**
- Consumes: generated `WorkflowExecutionApi`/`ApiClient` (Task 6); `AuthInterceptor`, `ProfileResolver`, `Overrides`, `CliConfig`, `OutputRenderer`, `OutputFormat`, `CliException` (Tasks 2–7).
- Produces:
  - `AutomationClientFactory.workflowExecutionApi(CliConfig config)` → configured `WorkflowExecutionApi` (sets base uri + interceptor; maps HTTP 401/403→`CliException(2)`, 404→`CliException(3)` via the ApiClient response interceptor).
  - `AutomationExecutionCommand` with `execution list` and `execution get` methods. Package-visible setters `setConfigPath(Path)` and `setEnv(Map)` for tests; base host/token/env come from flags in tests.

- [ ] **Step 1: Create build file and register module**

`cli/commands/automation/build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":cli:cli-core"))
    implementation(project(":cli:clients:automation-configuration"))
    implementation("org.springframework.shell:spring-shell-core:${rootProject.libs.versions.spring.shell.get()}")

    testImplementation(project(":cli:cli-app"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```
Add to `settings.gradle.kts`: `include("cli:commands:automation")`.

- [ ] **Step 2: Write the failing test (with a JDK HttpServer stub)**

`StubApi.java`:
```java
package com.bytechef.cli.command.automation;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class StubApi implements AutoCloseable {

    private final HttpServer server;

    String lastPath;
    String lastAuthorization;
    String lastEnvironment;

    private StubApi(HttpServer server) {
        this.server = server;
    }

    static StubApi start(int status, String jsonBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        StubApi stub = new StubApi(server);

        server.createContext("/", exchange -> {
            stub.lastPath = exchange.getRequestURI()
                .toString();
            stub.lastAuthorization = exchange.getRequestHeaders()
                .getFirst("Authorization");
            stub.lastEnvironment = exchange.getRequestHeaders()
                .getFirst("X-Environment");

            byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders()
                .add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);

            exchange.getResponseBody()
                .write(body);
            exchange.close();
        });

        server.start();

        return stub;
    }

    String host() {
        return "http://localhost:" + server.getAddress()
            .getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
```
`AutomationExecutionCommandTest.java`:
```java
package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import org.junit.jupiter.api.Test;

class AutomationExecutionCommandTest {

    @Test
    void testExecutionListSendsAuthAndEnvironment() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "automation", "execution", "list",
                "--host", stub.host(), "--token", "btc_x", "--environment", "STAGING",
                "--workspace-id", "1");

            assertEquals(0, code);
            assertTrue(stub.lastPath.startsWith("/api/automation/v1/workflow-executions"), stub.lastPath);
            assertTrue(stub.lastPath.contains("workspaceId=1"));
            assertEquals("Bearer btc_x", stub.lastAuthorization);
            assertEquals("STAGING", stub.lastEnvironment);
        }
    }

    @Test
    void testExecutionGetHitsByIdPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"status\":\"COMPLETED\"}")) {
            int code = CliApplication.execute(
                "automation", "execution", "get", "7",
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(stub.lastPath.startsWith("/api/automation/v1/workflow-executions/7"), stub.lastPath);
        }
    }

    @Test
    void testUnauthorizedReturnsExitCode2() throws Exception {
        try (StubApi stub = StubApi.start(401, "")) {
            int code = CliApplication.execute(
                "automation", "execution", "get", "7",
                "--host", stub.host(), "--token", "bad", "--environment", "PRODUCTION");

            assertEquals(2, code);
        }
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :cli:commands:automation:test`
Expected: FAIL (command classes missing).

- [ ] **Step 4: Implement the client factory**

`AutomationClientFactory.java` (adjust type names to the actual generated `ApiClient` API surface verified in Task 6, Step 4):
```java
package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automation.ApiClient;
import com.bytechef.cli.client.automation.api.ProjectCodeWorkflowApi;
import com.bytechef.cli.client.automation.api.ProjectGitApi;
import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.error.CliException;
import com.bytechef.cli.core.http.AuthInterceptor;

final class AutomationClientFactory {

    private AutomationClientFactory() {
    }

    static ApiClient apiClient(CliConfig config) {
        ApiClient apiClient = new ApiClient();

        apiClient.updateBaseUri(AuthInterceptor.baseUri(config));
        apiClient.setRequestInterceptor(new AuthInterceptor(config));

        apiClient.setResponseInterceptor(response -> {
            int status = response.statusCode();

            if (status == 401 || status == 403) {
                throw new CliException(2, "Authentication failed (HTTP " + status + ").");
            }

            if (status == 404) {
                throw new CliException(3, "Not found (HTTP 404).");
            }

            if (status >= 400) {
                throw new CliException(1, "Request failed (HTTP " + status + ").");
            }
        });

        return apiClient;
    }

    static WorkflowExecutionApi workflowExecutionApi(CliConfig config) {
        return new WorkflowExecutionApi(apiClient(config));
    }

    static ProjectCodeWorkflowApi projectCodeWorkflowApi(CliConfig config) {
        return new ProjectCodeWorkflowApi(apiClient(config));
    }

    static ProjectGitApi projectGitApi(CliConfig config) {
        return new ProjectGitApi(apiClient(config));
    }
}
```
Note: if the generated `ApiClient` exposes response handling differently than `setResponseInterceptor(Consumer<HttpResponse<?>>)`, adapt using the actual method observed in Task 6 Step 4 (the native generator provides `setResponseInterceptor`). Keep the status→exit-code mapping identical.

- [ ] **Step 5: Implement `AutomationExecutionCommand`**

```java
package com.bytechef.cli.command.automation;

import com.bytechef.cli.client.automation.api.WorkflowExecutionApi;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.output.OutputRenderer;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

@org.springframework.stereotype.Component
public class AutomationExecutionCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> env = System.getenv();

    @Command(name = "automation execution list", description = "List workflow executions.")
    public void list(
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "status") String status,
        @Option(longName = "project-id") Long projectId,
        @Option(longName = "workflow-id") String workflowId,
        @Option(longName = "page", defaultValue = "0") Integer page,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        WorkflowExecutionApi api = AutomationClientFactory.workflowExecutionApi(config);

        Object result = api.getWorkflowExecutionsPage(
            config.workspaceId(), null,
            status == null ? null
                : com.bytechef.cli.client.automation.model.WorkflowExecutionStatusModel.fromValue(status),
            null, null, projectId, null, workflowId, page);

        new OutputRenderer(System.out).renderJson(result);
    }

    @Command(name = "automation execution get", description = "Get a workflow execution by id.")
    public void get(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment,
        @Option(longName = "output", defaultValue = "json") String output) {

        CliConfig config = resolve(profile, host, token, environment, null);

        WorkflowExecutionApi api = AutomationClientFactory.workflowExecutionApi(config);

        Object result = api.getWorkflowExecution(id);

        new OutputRenderer(System.out).renderJson(result);
    }

    private CliConfig resolve(String profile, String host, String token, String environment, Long workspaceId) {
        return new ProfileResolver(configPath, env).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), workspaceId, profile));
    }

    void setConfigPath(Path configPath) {
        this.configPath = configPath;
    }

    void setEnv(Map<String, String> env) {
        this.env = env;
    }
}
```
Note on `execution get <id>`: Spring Shell 4 binds positional args via `@Option`; if a bare positional is required instead of `--id`, mirror the pattern used by `ComponentCommand` (which uses named options). The test passes `"7"` positionally — if binding fails, change the test to `--id 7` and keep `--id` required. Verify against Spring Shell 4.0.1 behavior when implementing.

Register `AutomationExecutionCommand.class` in `@EnableCommand` and add the module dependency to `cli/cli-app/build.gradle.kts`.

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :cli:commands:automation:test`
Expected: PASS (3 tests). If the positional-id test fails to bind, apply the `--id` fallback noted above and re-run.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts cli/commands/automation cli/cli-app
git commit -m "feat(cli): add automation execution list/get commands"
```

---

### Task 9: `automation project deploy|pull` commands

**Files:**
- Create: `cli/commands/automation/src/main/java/com/bytechef/cli/command/automation/AutomationProjectCommand.java`
- Modify: `cli/cli-app/CliApplication.java` (+`AutomationProjectCommand.class`)
- Test: `cli/commands/automation/src/test/java/com/bytechef/cli/command/automation/AutomationProjectCommandTest.java`

**Interfaces:**
- Consumes: `AutomationClientFactory.projectCodeWorkflowApi`/`projectGitApi` (Task 8); generated `deployProject(Long, File)` and `pullProjectFromGit(Long)` (Task 6).
- Produces: `AutomationProjectCommand` with `automation project deploy` and `automation project pull` methods; both print a success message on HTTP 204 and exit 0.

- [ ] **Step 1: Write the failing test**

`AutomationProjectCommandTest.java`:
```java
package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AutomationProjectCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testProjectPullHitsGitPullPath() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "project", "pull", "5",
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(stub.lastPath.startsWith("/api/automation/v1/projects/5/git/pull"), stub.lastPath);
        }
    }

    @Test
    void testProjectDeployUploadsFile() throws Exception {
        Path projectFile = tempDir.resolve("project.zip");
        Files.writeString(projectFile, "dummy");

        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "project", "deploy",
                "--workspace-id", "1", "--project-file", projectFile.toString(),
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(stub.lastPath.startsWith("/api/automation/v1/projects/deploy"), stub.lastPath);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:commands:automation:test --tests "*AutomationProjectCommandTest"`
Expected: FAIL (`AutomationProjectCommand` missing).

- [ ] **Step 3: Implement `AutomationProjectCommand`**

```java
package com.bytechef.cli.command.automation;

import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.config.Overrides;
import com.bytechef.cli.core.config.ProfileResolver;
import com.bytechef.cli.core.error.CliException;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;

@org.springframework.stereotype.Component
public class AutomationProjectCommand {

    private Path configPath = Path.of(System.getProperty("user.home"), ".bytechef", "config");
    private Map<String, String> env = System.getenv();

    @Command(name = "automation project deploy", description = "Deploy a code-based project.")
    public void deploy(
        @Option(longName = "workspace-id") Long workspaceId,
        @Option(longName = "project-file", required = true) String projectFile,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        File file = new File(projectFile);

        if (!file.exists()) {
            throw new CliException(1, "Project file not found: " + projectFile);
        }

        CliConfig config = resolve(profile, host, token, environment, workspaceId);

        AutomationClientFactory.projectCodeWorkflowApi(config)
            .deployProject(config.workspaceId(), file);

        System.out.println("Project deployed.");
    }

    @Command(name = "automation project pull", description = "Pull a project from its git repository.")
    public void pull(
        @Option(longName = "id", required = true) Long id,
        @Option(longName = "profile") String profile,
        @Option(longName = "host") String host,
        @Option(longName = "token") String token,
        @Option(longName = "environment") String environment) {

        CliConfig config = resolve(profile, host, token, environment, null);

        AutomationClientFactory.projectGitApi(config)
            .pullProjectFromGit(id);

        System.out.println("Project pulled from git.");
    }

    private CliConfig resolve(String profile, String host, String token, String environment, Long workspaceId) {
        return new ProfileResolver(configPath, env).resolve(
            new Overrides(
                host, token, environment == null ? null : Environment.valueOf(environment), workspaceId, profile));
    }
}
```
Register `AutomationProjectCommand.class` in `@EnableCommand`. (Same positional-vs-`--id` caveat as Task 8 applies to `pull <id>`.)

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :cli:commands:automation:test`
Expected: PASS (all automation command tests).

- [ ] **Step 5: Commit**

```bash
git add cli/commands/automation cli/cli-app
git commit -m "feat(cli): add automation project deploy/pull commands"
```

---

### Task 10: Wire-up verification, table output, and CLI docs

**Files:**
- Modify: `cli/commands/automation/.../AutomationExecutionCommand.java` (add `--output table` rendering for `list`)
- Modify: `cli/README.md` (replace `// TODO` with usage)
- Test: `cli/commands/automation/.../AutomationExecutionCommandTest.java` (add a table-output assertion)

**Interfaces:**
- Consumes: `OutputRenderer.renderTable`, `OutputFormat` (Task 3); generated `PageModel` with `getContent()` returning `List<WorkflowExecutionBasicModel>` (verify getter names from Task 6 output).

- [ ] **Step 1: Write the failing test**

Add to `AutomationExecutionCommandTest.java`:
```java
    @Test
    void testExecutionListTableOutput() throws Exception {
        String body = "{\"content\":[{\"id\":7,\"status\":\"COMPLETED\"}],\"totalElements\":1}";

        try (StubApi stub = StubApi.start(200, body)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            java.io.PrintStream original = System.out;
            System.setOut(new java.io.PrintStream(out));

            try {
                CliApplication.execute(
                    "automation", "execution", "list",
                    "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION",
                    "--workspace-id", "1", "--output", "table");
            } finally {
                System.setOut(original);
            }

            assertTrue(out.toString()
                .contains("COMPLETED"), out.toString());
            assertTrue(out.toString()
                .contains("ID"), "table header expected");
        }
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :cli:commands:automation:test --tests "*AutomationExecutionCommandTest.testExecutionListTableOutput"`
Expected: FAIL (list currently only renders JSON, no "ID" header).

- [ ] **Step 3: Add table rendering to `list`**

Replace the `new OutputRenderer(System.out).renderJson(result);` line in `list` with:
```java
        OutputRenderer renderer = new OutputRenderer(System.out);

        if ("table".equalsIgnoreCase(output)) {
            com.bytechef.cli.client.automation.model.PageModel pageModel =
                (com.bytechef.cli.client.automation.model.PageModel) result;

            java.util.List<java.util.List<String>> rows = new java.util.ArrayList<>();

            if (pageModel.getContent() != null) {
                pageModel.getContent()
                    .forEach(e -> rows.add(java.util.List.of(
                        String.valueOf(e.getId()), String.valueOf(e.getStatus()))));
            }

            renderer.renderTable(java.util.List.of("ID", "STATUS"), rows);
        } else {
            renderer.renderJson(result);
        }
```
Change the `list` body so `getWorkflowExecutionsPage(...)` is assigned to a `PageModel` (not `Object`). Verify `getContent()`/`getId()`/`getStatus()` getter names against the generated models from Task 6; adjust if the suffix/casing differs.

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :cli:commands:automation:test`
Expected: PASS (all tests including table output).

- [ ] **Step 5: Write CLI usage docs**

Replace the contents of `cli/README.md` with:
```markdown
# ByteChef CLI

## Configure a profile

```bash
bytechef configure --host https://app.bytechef.io --token <public-api-token> \
  --environment PRODUCTION --workspace-id 1
```
Credentials are stored in `~/.bytechef/config` (mode 600). Override any value per command with `--host/--token/--environment/--workspace-id` or the `BYTECHEF_HOST/TOKEN/ENVIRONMENT/WORKSPACE_ID` env vars.

## Automation commands

```bash
bytechef automation execution list --status COMPLETED --output table
bytechef automation execution get --id 42
bytechef automation project deploy --workspace-id 1 --project-file ./project.zip
bytechef automation project pull --id 5
```

Output is JSON by default; add `--output table` on list commands for a summary. Exit codes: 0 success, 1 error, 2 auth, 3 not found, 4 config.
```

- [ ] **Step 6: Full CLI build and commit**

Run: `./gradlew :cli:cli-app:build`
Expected: BUILD SUCCESSFUL.
```bash
git add cli
git commit -m "feat(cli): add table output for execution list and CLI usage docs"
```

---

## Self-Review Notes

- **Spec coverage:** foundation (cli-core: config §4, auth §5, output §7, errors §7) → Tasks 1–4, 7; `configure` §4 → Task 5; client generation §3 → Task 6; automation commands §6 → Tasks 8–10. Embedded (§9) intentionally excluded (sub-project #2).
- **Known verification points (resolve during implementation, not placeholders):** exact generated method/getter signatures (Task 6 Step 4 prints them); Spring Shell 4.0.1 positional-vs-named arg binding for `<id>` (Tasks 8–9 note the `--id` fallback and the tests to adjust); the native `ApiClient` response-interceptor method name (Task 8 Step 4). Each has a concrete fallback specified.
- **Environment header rule:** `X-Environment` is always set by `AuthInterceptor`; the `getWorkflowExecutionsPage` `xEnvironment` argument is always passed `null` to avoid duplication (Task 8, Step 5).
