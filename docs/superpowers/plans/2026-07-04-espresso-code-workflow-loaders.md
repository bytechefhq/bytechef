# Espresso Code-Workflow Loaders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Load Java code-workflow JARs inside a sandboxed GraalVM Espresso guest JVM (replacing the host-JVM classloader path), and add a new `embedded-code-workflow-loader` module so `CodeWorkflowTaskExecutor` handles `PlatformType.EMBEDDED`.

**Architecture:** The automation loader's `JAVA` branch switches from `ProjectHandlerClassLoader` (deleted) to a new Espresso path in `ProjectHandlerPolyglotEngine`: the guest JVM gets `java.Classpath = uploadedJar + bundled SDK jars`, the handler impl class name is read host-side from the JAR's `META-INF/services` entry, and the guest definition graph is walked via interop member invocations into the existing host-side `Polyglot*Definition` records. A new EE module `embedded-code-workflow-loader` mirrors this for `IntegrationHandler` (polyglot-only, all four languages).

**Tech Stack:** Java 25, GraalVM Polyglot 25.0.3 (`org.graalvm.polyglot:java` = Espresso, already declared in the module build files), Gradle Kotlin DSL, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-07-04-espresso-code-workflow-loaders-design.md`

## Global Constraints

- Every file under `server/ee/` gets the ByteChef Enterprise license header AND the `@version ee` Javadoc tag (Spotless picks the header by the `@version ee` CONTENT, not the path — tests included).
- Blank line before control statements (`if`, `for`, `try`, ...) except at block start; blank line between a variable modification and the statement using it; no trailing blank line before a class's closing `}`.
- No `TODO:` comments (Checkstyle `TodoComment`). Test method names camelCase without underscores, applies to ALL methods in test sources.
- Descriptive variable names everywhere (no `e` for entries, `f` for files — exception variables `e` in catch clauses match existing codebase idiom).
- Unit test classes end with `Test` (never `IntTest`).
- Before final commit: `./gradlew spotlessApply` then targeted `check` tasks must pass.
- Commit convention: server-side messages are plain `<description>` (no ticket number known for this work); end commit messages with the `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` trailer.
- Espresso context options: never `allowAllAccess(true)` or any `allowHostAccess`/host-class-lookup grant. Only `allowNativeAccess(true)`, `allowCreateThread(true)`, `allowIO(IOAccess.ALL)` — these are what the Espresso runtime itself needs to boot its guest JDK; host access stays at the default (closed).

---

### Task 1: Automation guest SDK bundling (`guestSdk` configuration + `GuestSdkClasspath`)

The Espresso guest JVM cannot see host classes, so uploaded thin JARs need the SDK
(`project-api`, `workflow-api`) on the guest classpath. This task bundles those SDK
jars into the loader module's resources at build time and adds a runtime helper that
extracts them to a temp dir and returns a classpath string.

**Files:**
- Modify: `server/ee/libs/automation/automation-code-workflow-loader/build.gradle.kts`
- Create: `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/GuestSdkClasspath.java`
- Test: `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/GuestSdkClasspathTest.java`

**Interfaces:**
- Consumes: nothing from other tasks.
- Produces: `static String GuestSdkClasspath.get()` — returns `File.pathSeparator`-joined absolute paths of the extracted SDK jars (`project-api-*.jar`, `workflow-api-*.jar`). Package-private, used by `ProjectHandlerPolyglotEngine` in Task 2.

- [ ] **Step 1: Add the `guestSdk` configuration and resource bundling to the build file**

In `server/ee/libs/automation/automation-code-workflow-loader/build.gradle.kts`, replace the whole file content with:

```kotlin
val guestSdk: Configuration by configurations.creating

dependencies {
    api(project(":sdks:backend:automation:project-api"))
    api(project(":sdks:backend:java:workflow-api"))

    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))

    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation(rootProject.libs.org.graalvm.polyglot.ruby)
    implementation(project(":server:libs:core:class-loader:class-loader-api"))

    guestSdk(project(":sdks:backend:automation:project-api"))
    guestSdk(project(":sdks:backend:java:workflow-api"))
}

tasks.processResources {
    from(guestSdk) {
        into("META-INF/guest-sdk/automation")
    }

    doLast {
        val guestSdkDir = File(destinationDir, "META-INF/guest-sdk/automation")

        val jarNames = guestSdkDir.listFiles { file: File -> file.name.endsWith(".jar") }
            .orEmpty()
            .map { it.name }
            .sorted()

        File(guestSdkDir, "index.txt").writeText(jarNames.joinToString("\n"))
    }
}
```

Notes: `class-loader-api` stays for now — it is removed in Task 3 together with
`ProjectHandlerClassLoader`. The `index.txt` file exists because classpath resources
in a directory cannot be enumerated portably at runtime; the extractor reads it to
learn the jar names. The resources are namespaced under `guest-sdk/automation/`
because the embedded loader module (Task 4) bundles a different jar set and both
modules end up on the same application classpath.

- [ ] **Step 2: Write the failing test**

Create `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/GuestSdkClasspathTest.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GuestSdkClasspathTest {

    @Test
    void testGetReturnsExtractedSdkJars() {
        String classpath = GuestSdkClasspath.get();

        String[] jarPaths = classpath.split(File.pathSeparator);

        assertTrue(jarPaths.length >= 2, "Expected at least project-api and workflow-api jars, got: " + classpath);

        boolean projectApiFound = false;
        boolean workflowApiFound = false;

        for (String jarPath : jarPaths) {
            assertTrue(Files.isRegularFile(Paths.get(jarPath)), "Extracted jar does not exist: " + jarPath);

            if (jarPath.contains("project-api")) {
                projectApiFound = true;
            }

            if (jarPath.contains("workflow-api")) {
                workflowApiFound = true;
            }
        }

        assertTrue(projectApiFound, "project-api jar missing from: " + classpath);
        assertTrue(workflowApiFound, "workflow-api jar missing from: " + classpath);
    }

    @Test
    void testGetIsIdempotent() {
        assertTrue(GuestSdkClasspath.get()
            .equals(GuestSdkClasspath.get()));
    }
}
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test --tests "com.bytechef.platform.codeworkflow.loader.automation.GuestSdkClasspathTest"
```

Expected: compilation FAILURE — `GuestSdkClasspath` does not exist.

- [ ] **Step 4: Implement `GuestSdkClasspath`**

Create `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/GuestSdkClasspath.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the SDK jars bundled under {@code META-INF/guest-sdk/automation/} to a temporary directory so they can be
 * put on the classpath of the GraalVM Espresso guest JVM. Uploaded code workflow jars are thin: they compile against
 * the project-api and workflow-api SDKs, which the guest JVM cannot resolve from the host.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class GuestSdkClasspath {

    private static final String RESOURCE_PREFIX = "META-INF/guest-sdk/automation/";

    private static volatile String classpath;

    private GuestSdkClasspath() {
    }

    static String get() {
        if (classpath == null) {
            synchronized (GuestSdkClasspath.class) {
                if (classpath == null) {
                    classpath = extract();
                }
            }
        }

        return classpath;
    }

    private static String extract() {
        ClassLoader classLoader = GuestSdkClasspath.class.getClassLoader();

        try {
            Path tempDirectory = Files.createTempDirectory("bytechef_guest_sdk_automation");

            List<String> jarPaths = new ArrayList<>();

            for (String jarName : readIndex(classLoader)) {
                try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + jarName)) {
                    if (inputStream == null) {
                        throw new IllegalStateException(
                            "Guest SDK jar resource %s%s is missing".formatted(RESOURCE_PREFIX, jarName));
                    }

                    Path jarPath = tempDirectory.resolve(jarName);

                    Files.copy(inputStream, jarPath);

                    jarPaths.add(jarPath.toString());
                }
            }

            return String.join(File.pathSeparator, jarPaths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readIndex(ClassLoader classLoader) throws IOException {
        try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + "index.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException(
                    "Guest SDK index resource %sindex.txt is missing".formatted(RESOURCE_PREFIX));
            }

            String index = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return index.lines()
                .filter(line -> !line.isBlank())
                .toList();
        }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test --tests "com.bytechef.platform.codeworkflow.loader.automation.GuestSdkClasspathTest"
```

Expected: BUILD SUCCESSFUL, 2 tests pass.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/automation/automation-code-workflow-loader
git commit -m "Bundle guest SDK jars in automation code workflow loader

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: Espresso Java path in `ProjectHandlerPolyglotEngine`

Adds `loadJava(Path jarPath)` to the engine: boots an Espresso context with the
uploaded jar + guest SDK on `java.Classpath`, instantiates the handler impl (class
name read host-side from the jar's `META-INF/services` entry), and walks the guest
definition via interop member invocations into the existing host-side records.
`perform()` re-enters a fresh context per call, mirroring the script languages.

**Files:**
- Modify: `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerPolyglotEngine.java`
- Test: `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerPolyglotEngineTest.java`

**Interfaces:**
- Consumes: `GuestSdkClasspath.get()` (Task 1).
- Produces: `static ProjectHandler ProjectHandlerPolyglotEngine.loadJava(Path jarPath)` (package-private). Throws `IllegalArgumentException` for a missing/empty `META-INF/services/com.bytechef.automation.project.ProjectHandler` entry; `IllegalStateException` (with platform-support message) when the Espresso context cannot be created.

- [ ] **Step 1: Write the failing test**

Create `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerPolyglotEngineTest.java`.

The test compiles a minimal `ProjectHandler` fixture at runtime (`javac` via
`ToolProvider`, classpath assembled from the SDK classes' `CodeSource` locations,
which works both for jars and `build/classes` directories), packages it as a jar
with the `META-INF/services` registration, and loads it through Espresso. An
`assumeEspressoAvailable()` guard turns "Espresso cannot boot on this platform"
into a skipped test instead of a failure.

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerPolyglotEngineTest {

    private static final String FIXTURE_SOURCE = """
        import com.bytechef.automation.project.ProjectHandler;
        import com.bytechef.automation.project.definition.ProjectDefinition;
        import com.bytechef.automation.project.definition.ProjectDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;

        public class TestProjectHandler implements ProjectHandler {

            @Override
            public ProjectDefinition getDefinition() {
                return ProjectDsl.project("test-project")
                    .version("1.2.3")
                    .description("Test project")
                    .workflows(
                        WorkflowDsl.workflow("my-workflow")
                            .label("My Workflow")
                            .tasks(
                                WorkflowDsl.task("my-task")
                                    .label("My Task")
                                    .perform(() -> "hello")));
            }
        }
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testLoadJava() throws IOException {
        assumeEspressoAvailable();

        Path jarPath = buildFixtureJar(tempDir, FIXTURE_SOURCE, "TestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.loadJava(jarPath);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("test-project", projectDefinition.getName());
        assertEquals("1.2.3", projectDefinition.getVersion());
        assertEquals("Test project", projectDefinition.getDescription()
            .orElse(null));

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        assertEquals(1, workflows.size());

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        assertEquals("my-workflow", workflowDefinition.getName());
        assertEquals("My Workflow", workflowDefinition.getLabel()
            .orElse(null));

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        assertEquals(1, tasks.size());

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("my-task", taskDefinition.getName());
        assertEquals("hello", taskDefinition.getPerform()
            .apply());
    }

    @Test
    void testLoadJavaFailsWithoutServiceRegistration() throws IOException {
        Path jarPath = tempDir.resolve("no-service.jar");

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutputStream.putNextEntry(new JarEntry("placeholder.txt"));
            jarOutputStream.write("placeholder".getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();
        }

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> ProjectHandlerPolyglotEngine.loadJava(jarPath));

        assertTrue(exception.getMessage()
            .contains("META-INF/services/com.bytechef.automation.project.ProjectHandler"));
    }

    static void assumeEspressoAvailable() {
        try (Context context = Context.newBuilder("java")
            .allowCreateThread(true)
            .allowNativeAccess(true)
            .allowIO(IOAccess.ALL)
            .build()) {

            context.getBindings("java");
        } catch (RuntimeException e) {
            Assumptions.assumeTrue(false, "GraalVM Espresso is not available on this platform: " + e.getMessage());
        }
    }

    static Path buildFixtureJar(Path directory, String source, String className) throws IOException {
        Path sourcePath = directory.resolve(className + ".java");

        Files.writeString(sourcePath, source);

        Path classesDirectory = Files.createDirectories(directory.resolve("classes"));

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        int compilationResult = javaCompiler.run(
            null, null, null, "-classpath", sdkClasspath(), "-d", classesDirectory.toString(), sourcePath.toString());

        assertEquals(0, compilationResult, "Fixture compilation failed");

        Path jarPath = directory.resolve(className + ".jar");

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutputStream.putNextEntry(
                new JarEntry("META-INF/services/com.bytechef.automation.project.ProjectHandler"));
            jarOutputStream.write((className + "\n").getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();

            try (Stream<Path> classFiles = Files.walk(classesDirectory)) {
                for (Path classFile : classFiles.filter(Files::isRegularFile)
                    .toList()) {

                    String entryName = classesDirectory.relativize(classFile)
                        .toString()
                        .replace(File.separatorChar, '/');

                    jarOutputStream.putNextEntry(new JarEntry(entryName));
                    jarOutputStream.write(Files.readAllBytes(classFile));
                    jarOutputStream.closeEntry();
                }
            }
        }

        return jarPath;
    }

    private static String sdkClasspath() {
        return Stream.of(ProjectHandler.class, WorkflowDefinition.class)
            .map(clazz -> {
                ProtectionDomain protectionDomain = clazz.getProtectionDomain();

                CodeSource codeSource = protectionDomain.getCodeSource();

                return Paths.get(URI.create(String.valueOf(codeSource.getLocation())))
                    .toString();
            })
            .distinct()
            .collect(Collectors.joining(File.pathSeparator));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test --tests "com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerPolyglotEngineTest"
```

Expected: compilation FAILURE — `ProjectHandlerPolyglotEngine.loadJava` does not exist.

- [ ] **Step 3: Implement the Java path in the engine**

Modify `ProjectHandlerPolyglotEngine.java`. Add these imports to the existing ones:

```java
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.io.IOAccess;
```

Add the following methods and record to the class body (keep everything that exists;
the script path is untouched). Place `loadJava` right after the existing `load`
method:

```java
    static ProjectHandler loadJava(Path jarPath) {
        if (engine == null) {
            engine = Engine.create();
        }

        String implClassName = readServiceImplementationClassName(jarPath);

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value projectHandler = newGuestInstance(polyglotContext, implClassName);

            Value projectDefinition = projectHandler.invokeMember("getDefinition");

            String name = Objects.requireNonNull(asString(projectDefinition.invokeMember("getName")));
            String description = asString(unwrapOptional(projectDefinition.invokeMember("getDescription")));
            String version = asString(projectDefinition.invokeMember("getVersion"));

            List<WorkflowDefinition> workflows = new ArrayList<>();

            Value workflowsValue = projectDefinition.invokeMember("getWorkflows");

            for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                String workflowName = asString(workflow.invokeMember("getName"));

                workflows.add(
                    new PolyglotWorkflowDefinition(
                        workflowName, asString(unwrapOptional(workflow.invokeMember("getLabel"))),
                        asString(unwrapOptional(workflow.invokeMember("getDescription"))),
                        toJavaTaskDefinitions(workflowName, workflow, jarPath, implClassName)));
            }

            return () -> new PolyglotProjectDefinition(name, description, version, workflows);
        }
    }

    private static Object executeJavaPerform(
        Path jarPath, String implClassName, String workflowName, String taskName) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value projectHandler = newGuestInstance(polyglotContext, implClassName);

            Value projectDefinition = projectHandler.invokeMember("getDefinition");

            Value workflowsValue = projectDefinition.invokeMember("getWorkflows");

            for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                if (!workflowName.equals(asString(workflow.invokeMember("getName")))) {
                    continue;
                }

                Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

                if (tasksValue == null) {
                    break;
                }

                for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
                    Value task = tasksValue.invokeMember("get", taskIndex);

                    if (taskName.equals(asString(task.invokeMember("getName")))) {
                        Value performFunction = task.invokeMember("getPerform");

                        return toHostValue(performFunction.invokeMember("apply"));
                    }
                }
            }

            throw new IllegalArgumentException(
                "Workflow name=%s, task name=%s not found".formatted(workflowName, taskName));
        }
    }

    private static List<TaskDefinition> toJavaTaskDefinitions(
        String workflowName, Value workflow, Path jarPath, String implClassName) {

        Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

        if (tasksValue == null) {
            return List.of();
        }

        List<TaskDefinition> taskDefinitions = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            Value task = tasksValue.invokeMember("get", taskIndex);

            taskDefinitions.add(
                new JavaTaskDefinition(
                    workflowName, asString(task.invokeMember("getName")),
                    asString(unwrapOptional(task.invokeMember("getLabel"))),
                    asString(unwrapOptional(task.invokeMember("getDescription"))), jarPath, implClassName));
        }

        return taskDefinitions;
    }

    private static Context getJavaContext(Path jarPath) {
        try {
            return Context.newBuilder("java")
                .engine(engine)
                .allowCreateThread(true)
                .allowNativeAccess(true)
                .allowIO(IOAccess.ALL)
                .option("java.Classpath", jarPath + File.pathSeparator + GuestSdkClasspath.get())
                .build();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to create a GraalVM Espresso context. Espresso standalone supports Linux (amd64, aarch64), " +
                    "macOS (aarch64) and, experimentally, Windows (amd64).",
                e);
        }
    }

    private static Value newGuestInstance(Context polyglotContext, String implClassName) {
        Value bindings = polyglotContext.getBindings("java");

        Value handlerClass = bindings.getMember(implClassName);

        if (handlerClass == null) {
            throw new IllegalStateException(
                "Class %s is not present on the guest classpath".formatted(implClassName));
        }

        return handlerClass.newInstance();
    }

    private static String readServiceImplementationClassName(Path jarPath) {
        String serviceEntryName = "META-INF/services/" + ProjectHandler.class.getName();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry jarEntry = jarFile.getJarEntry(serviceEntryName);

            if (jarEntry == null) {
                throw new IllegalArgumentException(
                    "Jar %s is missing the service registration %s".formatted(jarPath, serviceEntryName));
            }

            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                String serviceEntryContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return serviceEntryContent.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Service registration %s in jar %s is empty".formatted(serviceEntryName, jarPath)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String asString(Value value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private static int sizeOf(Value listValue) {
        Value sizeValue = listValue.invokeMember("size");

        return sizeValue.asInt();
    }

    private static Value unwrapOptional(Value optionalValue) {
        if (optionalValue == null || optionalValue.isNull()) {
            return null;
        }

        Value unwrapped = optionalValue.invokeMember("orElse", (Object) null);

        return unwrapped == null || unwrapped.isNull() ? null : unwrapped;
    }

    private static Object toHostValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isNumber()) {
            return value.as(Number.class);
        }

        if (value.isString()) {
            return value.asString();
        }

        throw new IllegalStateException(
            "A Java code workflow perform must return null, a boolean, a number or a string, got: " + value);
    }

    private record JavaTaskDefinition(
        String workflowName, String name, String label, String description, Path jarPath, String implClassName)
        implements TaskDefinition {

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Parameter>> getParameters() {
            return Optional.empty();
        }

        @Override
        public PerformFunction getPerform() {
            return () -> executeJavaPerform(jarPath, implClassName, workflowName, name);
        }
    }
```

Implementation notes (constraints, not suggestions):
- The definition data is **fully materialized before the context closes** — only
  `String`s, host records, and the jar path escape the try-with-resources block.
  A `Value` outliving its context throws `IllegalStateException` on access.
- `allowCreateThread` / `allowNativeAccess` / `allowIO` are what Espresso itself
  needs to boot the guest JDK. Host access (guest calling host classes) stays at
  the closed default — that is the sandbox.
- Guest `List` is accessed via `invokeMember("size")`/`invokeMember("get", i)`,
  not `getArrayElement` — plain public-method interop is guaranteed for guest
  objects; array-element mapping of guest collections is not.
- Guest `Optional` is unwrapped with `orElse(null)` through interop for the same
  reason (host `Optional` is a final class and cannot proxy a guest one).

- [ ] **Step 4: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test --tests "com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerPolyglotEngineTest"
```

Expected: BUILD SUCCESSFUL. `testLoadJava` passes (or is SKIPPED with the
"Espresso is not available" assumption message on unsupported platforms — a skip
here is acceptable only if the platform genuinely lacks Espresso; on Linux amd64/
aarch64 and macOS aarch64 it must PASS). `testLoadJavaFailsWithoutServiceRegistration`
must PASS everywhere (it never boots Espresso). First Espresso boot can take
30-60s; do not kill the test run early.

- [ ] **Step 5: Commit**

```bash
git add server/ee/libs/automation/automation-code-workflow-loader
git commit -m "Load Java code workflows through GraalVM Espresso

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Switch `ProjectHandlerLoader` to polyglot-only, delete the classloader, update call sites

**Files:**
- Modify: `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerLoader.java`
- Delete: `server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerClassLoader.java`
- Modify: `server/ee/libs/automation/automation-code-workflow-loader/build.gradle.kts` (drop `class-loader-api`)
- Modify: `server/ee/libs/automation/automation-configuration/automation-configuration-service/src/main/java/com/bytechef/ee/automation/configuration/facade/ProjectCodeWorkflowFacadeImpl.java`
- Modify: `server/ee/libs/modules/components/code-workflow/src/main/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskExecutor.java`
- Test: `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerLoaderTest.java`

**Interfaces:**
- Consumes: `ProjectHandlerPolyglotEngine.loadJava(Path)` (Task 2), `ProjectHandlerPolyglotEngine.load(String, String)` (existing).
- Produces: `public static ProjectHandler ProjectHandlerLoader.loadProjectHandler(URL url, Language language)` — the `cacheKey`/`cacheManager` parameters are GONE. Task 6 relies on this exact signature.

- [ ] **Step 1: Write the failing loader test**

Create `server/ee/libs/automation/automation-code-workflow-loader/src/test/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerLoaderTest.java`.
It tests the new two-argument signature for both a script language and Java (the
Java case reuses the fixture helpers from Task 2's test class):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ProjectHandlerLoaderTest {

    private static final String JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            version: '1.2.3',
            workflows: [
                {
                    name: 'my-workflow',
                    label: 'My Workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'hello from js'
                        }
                    ]
                }
            ]
        })
        """;

    private static final String JAVA_SOURCE = """
        import com.bytechef.automation.project.ProjectHandler;
        import com.bytechef.automation.project.definition.ProjectDefinition;
        import com.bytechef.automation.project.definition.ProjectDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;

        public class LoaderTestProjectHandler implements ProjectHandler {

            @Override
            public ProjectDefinition getDefinition() {
                return ProjectDsl.project("loader-test-project")
                    .version("2.0.0")
                    .workflows(
                        WorkflowDsl.workflow("loader-workflow")
                            .tasks(
                                WorkflowDsl.task("loader-task")
                                    .perform(() -> "hello from java")));
            }
        }
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testLoadProjectHandlerFromJavaScript() throws IOException {
        Path scriptPath = tempDir.resolve("project.js");

        Files.writeString(scriptPath, JAVASCRIPT_SOURCE);

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
            toUrl(scriptPath), Language.JAVASCRIPT);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("test-project", projectDefinition.getName());
        assertEquals("1.2.3", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        assertEquals(1, workflows.size());

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from js", taskDefinition.getPerform()
            .apply());
    }

    @Test
    void testLoadProjectHandlerFromJavaJar() throws IOException {
        ProjectHandlerPolyglotEngineTest.assumeEspressoAvailable();

        Path jarPath = ProjectHandlerPolyglotEngineTest.buildFixtureJar(
            tempDir, JAVA_SOURCE, "LoaderTestProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(toUrl(jarPath), Language.JAVA);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        assertEquals("loader-test-project", projectDefinition.getName());
        assertEquals("2.0.0", projectDefinition.getVersion());

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from java", taskDefinition.getPerform()
            .apply());
    }

    private static java.net.URL toUrl(Path path) throws MalformedURLException {
        java.net.URI uri = path.toUri();

        return uri.toURL();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test --tests "com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoaderTest"
```

Expected: compilation FAILURE — `loadProjectHandler(URL, Language)` two-argument overload does not exist.

- [ ] **Step 3: Rewrite `ProjectHandlerLoader` and delete the classloader**

Replace the entire content of `ProjectHandlerLoader.java` with:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProjectHandlerLoader {

    /**
     * <b>Security Note:</b> Path traversal is intentional. The URL is derived from internal code workflow container
     * configuration, not from untrusted user input.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static ProjectHandler loadProjectHandler(URL url, Language language) {
        try {
            return switch (language) {
                case JAVA -> ProjectHandlerPolyglotEngine.loadJava(toLocalPath(url));
                case JAVASCRIPT, PYTHON, RUBY -> ProjectHandlerPolyglotEngine.load(
                    getLanguageId(language), Files.readString(toLocalPath(url)));
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getLanguageId(Language language) {
        return switch (language) {
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private static Path toLocalPath(URL url) throws IOException, URISyntaxException {
        java.net.URI uri = url.toURI();

        if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }

        Path tempFile = Files.createTempFile("code_workflow", null);

        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
```

(Use a proper `import java.net.URI;` instead of the inline qualified name — shown
inline above only to keep the import list visibly complete.)

Delete the classloader:

```bash
git rm server/ee/libs/automation/automation-code-workflow-loader/src/main/java/com/bytechef/platform/codeworkflow/loader/automation/ProjectHandlerClassLoader.java
```

In `server/ee/libs/automation/automation-code-workflow-loader/build.gradle.kts`,
delete the line:

```kotlin
    implementation(project(":server:libs:core:class-loader:class-loader-api"))
```

- [ ] **Step 4: Update the two call sites**

In `ProjectCodeWorkflowFacadeImpl.java`
(`server/ee/libs/automation/automation-configuration/automation-configuration-service/.../facade/ProjectCodeWorkflowFacadeImpl.java`):
1. Change the loader call (around line 124):

```java
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(uri.toURL(), language);
```

2. Remove the now-unused `cacheManager` field, its constructor parameter, the
   `org.springframework.cache.CacheManager` import, and the now-unused
   `java.util.UUID` import (verify `UUID` is not used elsewhere in the file first —
   if it is, keep the import). Then search for other constructor callers:

```bash
grep -rn "new ProjectCodeWorkflowFacadeImpl" server/ --include="*.java" | grep -v build/
```

Update any hits (test fixtures) to drop the `CacheManager` argument.

In `CodeWorkflowTaskExecutor.java`
(`server/ee/libs/modules/components/code-workflow/.../task/CodeWorkflowTaskExecutor.java`):
1. Change the loader call in `getWorkflowDefinitions` to:

```java
        if (PlatformType.AUTOMATION.equals(type)) {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage());

            workflows = projectHandler.getWorkflows();
        }
```

2. Remove the `cacheManager` field, constructor parameter, and the
   `org.springframework.cache.CacheManager` and `com.bytechef.commons.util.EncodingUtils`
   imports (both become unused). Keep the `// } else {TODO integration}` comment
   removal for Task 6 — in this task just leave that comment line as is if Checkstyle
   permits (it exists today), or if `spotlessApply`/Checkstyle flags it, delete it now.
3. Search for other constructor callers and update them:

```bash
grep -rn "new CodeWorkflowTaskExecutor" server/ --include="*.java" | grep -v build/
```

- [ ] **Step 5: Run the loader tests and compile the touched modules**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:test \
  :server:ee:libs:automation:automation-configuration:automation-configuration-service:compileJava \
  :server:ee:libs:modules:components:code-workflow:compileJava
```

Expected: BUILD SUCCESSFUL; `ProjectHandlerLoaderTest` passes (Java test may skip
only on Espresso-unsupported platforms).

- [ ] **Step 6: Commit**

```bash
git add -A server/ee/libs/automation server/ee/libs/modules/components/code-workflow
git commit -m "Route Java code workflow loading through polyglot only

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: New module `embedded-code-workflow-loader` — skeleton + guest SDK bundling

**Files:**
- Modify: `settings.gradle.kts` (add include, sorted among the `server:ee:libs:embedded:*` block)
- Create: `server/ee/libs/embedded/embedded-code-workflow-loader/build.gradle.kts`
- Create: `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/GuestSdkClasspath.java`
- Test: `server/ee/libs/embedded/embedded-code-workflow-loader/src/test/java/com/bytechef/ee/embedded/codeworkflow/loader/GuestSdkClasspathTest.java`

**Interfaces:**
- Consumes: nothing from other tasks (deliberately self-contained; mirrors Task 1).
- Produces: module `server:ee:libs:embedded:embedded-code-workflow-loader`; `static String GuestSdkClasspath.get()` (package `com.bytechef.ee.embedded.codeworkflow.loader`) returning extracted `integration-api` + `workflow-api` jar paths.

- [ ] **Step 1: Register the module**

In `settings.gradle.kts`, add this line immediately after the
`include("server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service")` line
(keeps the embedded block sorted):

```kotlin
include("server:ee:libs:embedded:embedded-code-workflow-loader")
```

- [ ] **Step 2: Create the build file**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/build.gradle.kts`:

```kotlin
val guestSdk: Configuration by configurations.creating

dependencies {
    api(project(":sdks:backend:embedded:integration-api"))
    api(project(":sdks:backend:java:workflow-api"))

    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))

    implementation(rootProject.libs.org.graalvm.polyglot.polyglot)
    implementation(rootProject.libs.org.graalvm.polyglot.java)
    implementation(rootProject.libs.org.graalvm.polyglot.js)
    implementation(rootProject.libs.org.graalvm.polyglot.python)
    implementation(rootProject.libs.org.graalvm.polyglot.ruby)

    guestSdk(project(":sdks:backend:embedded:integration-api"))
    guestSdk(project(":sdks:backend:java:workflow-api"))
}

tasks.processResources {
    from(guestSdk) {
        into("META-INF/guest-sdk/embedded")
    }

    doLast {
        val guestSdkDir = File(destinationDir, "META-INF/guest-sdk/embedded")

        val jarNames = guestSdkDir.listFiles { file: File -> file.name.endsWith(".jar") }
            .orEmpty()
            .map { it.name }
            .sorted()

        File(guestSdkDir, "index.txt").writeText(jarNames.joinToString("\n"))
    }
}
```

- [ ] **Step 3: Write the failing test**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/src/test/java/com/bytechef/ee/embedded/codeworkflow/loader/GuestSdkClasspathTest.java` —
identical in shape to Task 1's test, but asserting `integration-api` instead of
`project-api`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GuestSdkClasspathTest {

    @Test
    void testGetReturnsExtractedSdkJars() {
        String classpath = GuestSdkClasspath.get();

        String[] jarPaths = classpath.split(File.pathSeparator);

        assertTrue(
            jarPaths.length >= 2, "Expected at least integration-api and workflow-api jars, got: " + classpath);

        boolean integrationApiFound = false;
        boolean workflowApiFound = false;

        for (String jarPath : jarPaths) {
            assertTrue(Files.isRegularFile(Paths.get(jarPath)), "Extracted jar does not exist: " + jarPath);

            if (jarPath.contains("integration-api")) {
                integrationApiFound = true;
            }

            if (jarPath.contains("workflow-api")) {
                workflowApiFound = true;
            }
        }

        assertTrue(integrationApiFound, "integration-api jar missing from: " + classpath);
        assertTrue(workflowApiFound, "workflow-api jar missing from: " + classpath);
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-code-workflow-loader:test --tests "com.bytechef.ee.embedded.codeworkflow.loader.GuestSdkClasspathTest"
```

Expected: compilation FAILURE — `GuestSdkClasspath` does not exist.

- [ ] **Step 5: Implement `GuestSdkClasspath`**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/GuestSdkClasspath.java` —
identical to Task 1's implementation except for the package, the resource prefix,
the temp-dir prefix, and the Javadoc's SDK names:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts the SDK jars bundled under {@code META-INF/guest-sdk/embedded/} to a temporary directory so they can be
 * put on the classpath of the GraalVM Espresso guest JVM. Uploaded code workflow jars are thin: they compile against
 * the integration-api and workflow-api SDKs, which the guest JVM cannot resolve from the host.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class GuestSdkClasspath {

    private static final String RESOURCE_PREFIX = "META-INF/guest-sdk/embedded/";

    private static volatile String classpath;

    private GuestSdkClasspath() {
    }

    static String get() {
        if (classpath == null) {
            synchronized (GuestSdkClasspath.class) {
                if (classpath == null) {
                    classpath = extract();
                }
            }
        }

        return classpath;
    }

    private static String extract() {
        ClassLoader classLoader = GuestSdkClasspath.class.getClassLoader();

        try {
            Path tempDirectory = Files.createTempDirectory("bytechef_guest_sdk_embedded");

            List<String> jarPaths = new ArrayList<>();

            for (String jarName : readIndex(classLoader)) {
                try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + jarName)) {
                    if (inputStream == null) {
                        throw new IllegalStateException(
                            "Guest SDK jar resource %s%s is missing".formatted(RESOURCE_PREFIX, jarName));
                    }

                    Path jarPath = tempDirectory.resolve(jarName);

                    Files.copy(inputStream, jarPath);

                    jarPaths.add(jarPath.toString());
                }
            }

            return String.join(File.pathSeparator, jarPaths);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readIndex(ClassLoader classLoader) throws IOException {
        try (InputStream inputStream = classLoader.getResourceAsStream(RESOURCE_PREFIX + "index.txt")) {
            if (inputStream == null) {
                throw new IllegalStateException(
                    "Guest SDK index resource %sindex.txt is missing".formatted(RESOURCE_PREFIX));
            }

            String index = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            return index.lines()
                .filter(line -> !line.isBlank())
                .toList();
        }
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:embedded:embedded-code-workflow-loader:test --tests "com.bytechef.ee.embedded.codeworkflow.loader.GuestSdkClasspathTest"
```

Expected: BUILD SUCCESSFUL, 1 test passes.

- [ ] **Step 7: Commit**

```bash
git add settings.gradle.kts server/ee/libs/embedded/embedded-code-workflow-loader
git commit -m "Add embedded-code-workflow-loader module skeleton

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: `IntegrationHandlerPolyglotEngine` + `IntegrationHandlerLoader`

Mirrors the automation engine/loader for `IntegrationHandler`. Differences from
automation: definition members are `componentName` (String, required),
`componentVersion` (int, required), and `getWorkflows()` returns
`Optional<List<WorkflowDefinition>>` (not a bare list); the service registration
entry is `META-INF/services/com.bytechef.embedded.integration.IntegrationHandler`.

**Files:**
- Create: `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerPolyglotEngine.java`
- Create: `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerLoader.java`
- Test: `server/ee/libs/embedded/embedded-code-workflow-loader/src/test/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerLoaderTest.java`

**Interfaces:**
- Consumes: `GuestSdkClasspath.get()` (Task 4).
- Produces: `public static IntegrationHandler IntegrationHandlerLoader.loadIntegrationHandler(URL url, Language language)` — Task 6 relies on this exact signature. `Language` is `com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language`.

- [ ] **Step 1: Write the failing test**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/src/test/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerLoaderTest.java`.
It contains its own fixture-jar helpers (this module intentionally does not depend
on the automation loader's test classes):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerLoaderTest {

    private static final String JAVASCRIPT_SOURCE = """
        ({
            componentName: 'test-component',
            componentVersion: 1,
            version: '1.0.0',
            workflows: [
                {
                    name: 'my-workflow',
                    label: 'My Workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'hello from js'
                        }
                    ]
                }
            ]
        })
        """;

    private static final String JAVA_SOURCE = """
        import com.bytechef.embedded.integration.IntegrationHandler;
        import com.bytechef.embedded.integration.definition.IntegrationDefinition;
        import com.bytechef.embedded.integration.definition.IntegrationDsl;
        import com.bytechef.workflow.definition.WorkflowDsl;

        public class TestIntegrationHandler implements IntegrationHandler {

            @Override
            public IntegrationDefinition getDefinition() {
                return IntegrationDsl.integration("test-component", 1)
                    .version("1.0.0")
                    .description("Test integration")
                    .workflows(
                        WorkflowDsl.workflow("my-workflow")
                            .label("My Workflow")
                            .tasks(
                                WorkflowDsl.task("my-task")
                                    .perform(() -> "hello from java")));
            }
        }
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testLoadIntegrationHandlerFromJavaScript() throws IOException {
        Path scriptPath = tempDir.resolve("integration.js");

        Files.writeString(scriptPath, JAVASCRIPT_SOURCE);

        IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
            toUrl(scriptPath), Language.JAVASCRIPT);

        IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

        assertEquals("test-component", integrationDefinition.getComponentName());
        assertEquals(1, integrationDefinition.getComponentVersion());
        assertEquals("1.0.0", integrationDefinition.getVersion());

        List<WorkflowDefinition> workflows = integrationDefinition.getWorkflows()
            .orElseThrow();

        assertEquals(1, workflows.size());

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        assertEquals("my-workflow", workflowDefinition.getName());

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from js", taskDefinition.getPerform()
            .apply());
    }

    @Test
    void testLoadIntegrationHandlerFromJavaJar() throws IOException {
        assumeEspressoAvailable();

        Path jarPath = buildFixtureJar(tempDir, JAVA_SOURCE, "TestIntegrationHandler");

        IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
            toUrl(jarPath), Language.JAVA);

        IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

        assertEquals("test-component", integrationDefinition.getComponentName());
        assertEquals(1, integrationDefinition.getComponentVersion());
        assertEquals("Test integration", integrationDefinition.getDescription()
            .orElse(null));

        List<WorkflowDefinition> workflows = integrationDefinition.getWorkflows()
            .orElseThrow();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        assertEquals("hello from java", taskDefinition.getPerform()
            .apply());
    }

    private static void assumeEspressoAvailable() {
        try (Context context = Context.newBuilder("java")
            .allowCreateThread(true)
            .allowNativeAccess(true)
            .allowIO(IOAccess.ALL)
            .build()) {

            context.getBindings("java");
        } catch (RuntimeException e) {
            Assumptions.assumeTrue(false, "GraalVM Espresso is not available on this platform: " + e.getMessage());
        }
    }

    private static Path buildFixtureJar(Path directory, String source, String className) throws IOException {
        Path sourcePath = directory.resolve(className + ".java");

        Files.writeString(sourcePath, source);

        Path classesDirectory = Files.createDirectories(directory.resolve("classes"));

        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        int compilationResult = javaCompiler.run(
            null, null, null, "-classpath", sdkClasspath(), "-d", classesDirectory.toString(), sourcePath.toString());

        assertEquals(0, compilationResult, "Fixture compilation failed");

        Path jarPath = directory.resolve(className + ".jar");

        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutputStream.putNextEntry(
                new JarEntry("META-INF/services/com.bytechef.embedded.integration.IntegrationHandler"));
            jarOutputStream.write((className + "\n").getBytes(StandardCharsets.UTF_8));
            jarOutputStream.closeEntry();

            try (Stream<Path> classFiles = Files.walk(classesDirectory)) {
                for (Path classFile : classFiles.filter(Files::isRegularFile)
                    .toList()) {

                    String entryName = classesDirectory.relativize(classFile)
                        .toString()
                        .replace(File.separatorChar, '/');

                    jarOutputStream.putNextEntry(new JarEntry(entryName));
                    jarOutputStream.write(Files.readAllBytes(classFile));
                    jarOutputStream.closeEntry();
                }
            }
        }

        return jarPath;
    }

    private static String sdkClasspath() {
        return Stream.of(IntegrationHandler.class, WorkflowDefinition.class)
            .map(clazz -> {
                ProtectionDomain protectionDomain = clazz.getProtectionDomain();

                CodeSource codeSource = protectionDomain.getCodeSource();

                return Paths.get(URI.create(String.valueOf(codeSource.getLocation())))
                    .toString();
            })
            .distinct()
            .collect(Collectors.joining(File.pathSeparator));
    }

    private static URL toUrl(Path path) throws MalformedURLException {
        URI uri = path.toUri();

        return uri.toURL();
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:embedded:embedded-code-workflow-loader:test --tests "com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoaderTest"
```

Expected: compilation FAILURE — `IntegrationHandlerLoader` does not exist.

- [ ] **Step 3: Implement `IntegrationHandlerPolyglotEngine`**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerPolyglotEngine.java`.
This is the automation engine transposed to `IntegrationDefinition`; both the
script path (member access) and the Java path (interop method invocation) live here:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.Parameter;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.TypeLiteral;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.IOAccess;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerPolyglotEngine {

    private static Engine engine;

    static IntegrationHandler load(String languageId, String script) {
        if (engine == null) {
            engine = Engine.create();
        }

        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            String componentName = Objects.requireNonNull(getMember(value, "componentName"));

            Value componentVersionValue = value.getMember("componentVersion");

            int componentVersion = componentVersionValue.asInt();

            String description = getMember(value, "description");
            String version = getMember(value, "version");

            List<WorkflowDefinition> workflows = getWorkflows(
                value, new TypeLiteral<List<Map<String, Object>>>() {})
                    .stream()
                    .map(workflow -> (WorkflowDefinition) new PolyglotWorkflowDefinition(
                        (String) workflow.get("name"), (String) workflow.get("label"),
                        (String) workflow.get("description"),
                        toTaskDefinitions(
                            (String) workflow.get("name"), (List<?>) workflow.get("tasks"), languageId, script)))
                    .toList();

            return () -> new PolyglotIntegrationDefinition(
                componentName, componentVersion, description, version, workflows);
        }
    }

    static IntegrationHandler loadJava(Path jarPath) {
        if (engine == null) {
            engine = Engine.create();
        }

        String implClassName = readServiceImplementationClassName(jarPath);

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value integrationHandler = newGuestInstance(polyglotContext, implClassName);

            Value integrationDefinition = integrationHandler.invokeMember("getDefinition");

            String componentName = Objects.requireNonNull(
                asString(integrationDefinition.invokeMember("getComponentName")));

            Value componentVersionValue = integrationDefinition.invokeMember("getComponentVersion");

            int componentVersion = componentVersionValue.asInt();

            String description = asString(unwrapOptional(integrationDefinition.invokeMember("getDescription")));
            String version = asString(integrationDefinition.invokeMember("getVersion"));

            List<WorkflowDefinition> workflows = new ArrayList<>();

            Value workflowsValue = unwrapOptional(integrationDefinition.invokeMember("getWorkflows"));

            if (workflowsValue != null) {
                for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                    Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                    String workflowName = asString(workflow.invokeMember("getName"));

                    workflows.add(
                        new PolyglotWorkflowDefinition(
                            workflowName, asString(unwrapOptional(workflow.invokeMember("getLabel"))),
                            asString(unwrapOptional(workflow.invokeMember("getDescription"))),
                            toJavaTaskDefinitions(workflowName, workflow, jarPath, implClassName)));
                }
            }

            return () -> new PolyglotIntegrationDefinition(
                componentName, componentVersion, description, version, workflows);
        }
    }

    @SuppressWarnings("unchecked")
    private static Object executePerform(String workflowName, String taskName, String languageId, String script) {
        try (Context polyglotContext = getContext()) {
            Value value = polyglotContext.eval(languageId, script);

            List<Map<String, Object>> workflows = getWorkflows(value, new TypeLiteral<>() {});

            List<Map<String, Object>> tasks = (List<Map<String, Object>>) workflows.stream()
                .filter(workflow -> workflowName.equals(workflow.get("name")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Workflow name=%s not found".formatted(workflowName)))
                .get("tasks");

            for (Map<String, Object> task : tasks) {
                if (taskName.equals(task.get("name"))) {
                    Function<Object[], Object> perform = (Function<Object[], Object>) task.get("perform");

                    return perform.apply(null);
                }
            }

            throw new IllegalArgumentException("Task name=%s not found".formatted(taskName));
        }
    }

    private static Object executeJavaPerform(
        Path jarPath, String implClassName, String workflowName, String taskName) {

        try (Context polyglotContext = getJavaContext(jarPath)) {
            Value integrationHandler = newGuestInstance(polyglotContext, implClassName);

            Value integrationDefinition = integrationHandler.invokeMember("getDefinition");

            Value workflowsValue = unwrapOptional(integrationDefinition.invokeMember("getWorkflows"));

            if (workflowsValue != null) {
                for (int workflowIndex = 0; workflowIndex < sizeOf(workflowsValue); workflowIndex++) {
                    Value workflow = workflowsValue.invokeMember("get", workflowIndex);

                    if (!workflowName.equals(asString(workflow.invokeMember("getName")))) {
                        continue;
                    }

                    Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

                    if (tasksValue == null) {
                        break;
                    }

                    for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
                        Value task = tasksValue.invokeMember("get", taskIndex);

                        if (taskName.equals(asString(task.invokeMember("getName")))) {
                            Value performFunction = task.invokeMember("getPerform");

                            return toHostValue(performFunction.invokeMember("apply"));
                        }
                    }
                }
            }

            throw new IllegalArgumentException(
                "Workflow name=%s, task name=%s not found".formatted(workflowName, taskName));
        }
    }

    private static List<TaskDefinition> toJavaTaskDefinitions(
        String workflowName, Value workflow, Path jarPath, String implClassName) {

        Value tasksValue = unwrapOptional(workflow.invokeMember("getTasks"));

        if (tasksValue == null) {
            return List.of();
        }

        List<TaskDefinition> taskDefinitions = new ArrayList<>();

        for (int taskIndex = 0; taskIndex < sizeOf(tasksValue); taskIndex++) {
            Value task = tasksValue.invokeMember("get", taskIndex);

            taskDefinitions.add(
                new JavaTaskDefinition(
                    workflowName, asString(task.invokeMember("getName")),
                    asString(unwrapOptional(task.invokeMember("getLabel"))),
                    asString(unwrapOptional(task.invokeMember("getDescription"))), jarPath, implClassName));
        }

        return taskDefinitions;
    }

    private static Context getContext() {
        return Context.newBuilder()
            .engine(engine)
            .build();
    }

    private static Context getJavaContext(Path jarPath) {
        try {
            return Context.newBuilder("java")
                .engine(engine)
                .allowCreateThread(true)
                .allowNativeAccess(true)
                .allowIO(IOAccess.ALL)
                .option("java.Classpath", jarPath + File.pathSeparator + GuestSdkClasspath.get())
                .build();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                "Failed to create a GraalVM Espresso context. Espresso standalone supports Linux (amd64, aarch64), " +
                    "macOS (aarch64) and, experimentally, Windows (amd64).",
                e);
        }
    }

    private static Value newGuestInstance(Context polyglotContext, String implClassName) {
        Value bindings = polyglotContext.getBindings("java");

        Value handlerClass = bindings.getMember(implClassName);

        if (handlerClass == null) {
            throw new IllegalStateException(
                "Class %s is not present on the guest classpath".formatted(implClassName));
        }

        return handlerClass.newInstance();
    }

    private static String readServiceImplementationClassName(Path jarPath) {
        String serviceEntryName = "META-INF/services/" + IntegrationHandler.class.getName();

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry jarEntry = jarFile.getJarEntry(serviceEntryName);

            if (jarEntry == null) {
                throw new IllegalArgumentException(
                    "Jar %s is missing the service registration %s".formatted(jarPath, serviceEntryName));
            }

            try (InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                String serviceEntryContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                return serviceEntryContent.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                        "Service registration %s in jar %s is empty".formatted(serviceEntryName, jarPath)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getMember(Value value, String name) {
        value = value.getMember(name);

        return value == null ? null : value.as(String.class);
    }

    private static <T> T getWorkflows(Value value, TypeLiteral<T> typeLiteral) {
        return value.getMember("workflows")
            .as(typeLiteral);
    }

    private static List<TaskDefinition> toTaskDefinitions(
        String workflowName, List<?> tasks, String languageId, String script) {

        if (tasks == null) {
            return List.of();
        }

        return tasks.stream()
            .map(task -> (Map<?, ?>) task)
            .map(task -> (TaskDefinition) new PolyglotTaskDefinition(
                workflowName, (String) task.get("name"), (String) task.get("label"), (String) task.get("description"),
                languageId, script))
            .toList();
    }

    private static String asString(Value value) {
        return value == null || value.isNull() ? null : value.asString();
    }

    private static int sizeOf(Value listValue) {
        Value sizeValue = listValue.invokeMember("size");

        return sizeValue.asInt();
    }

    private static Value unwrapOptional(Value optionalValue) {
        if (optionalValue == null || optionalValue.isNull()) {
            return null;
        }

        Value unwrapped = optionalValue.invokeMember("orElse", (Object) null);

        return unwrapped == null || unwrapped.isNull() ? null : unwrapped;
    }

    private static Object toHostValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }

        if (value.isBoolean()) {
            return value.asBoolean();
        }

        if (value.isNumber()) {
            return value.as(Number.class);
        }

        if (value.isString()) {
            return value.asString();
        }

        throw new IllegalStateException(
            "A Java code workflow perform must return null, a boolean, a number or a string, got: " + value);
    }

    private record JavaTaskDefinition(
        String workflowName, String name, String label, String description, Path jarPath, String implClassName)
        implements TaskDefinition {

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Parameter>> getParameters() {
            return Optional.empty();
        }

        @Override
        public PerformFunction getPerform() {
            return () -> executeJavaPerform(jarPath, implClassName, workflowName, name);
        }
    }

    private record PolyglotTaskDefinition(
        String workflowName, String name, String label, String description, String languageId, String script)
        implements TaskDefinition {

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Parameter>> getParameters() {
            return Optional.empty();
        }

        @Override
        public PerformFunction getPerform() {
            return () -> executePerform(workflowName, name, languageId, script);
        }
    }

    private record PolyglotIntegrationDefinition(
        String componentName, int componentVersion, String description, String version,
        List<WorkflowDefinition> workflows)
        implements IntegrationDefinition {

        @Override
        public Optional<String> getCategory() {
            return Optional.empty();
        }

        @Override
        public String getComponentName() {
            return componentName;
        }

        @Override
        public int getComponentVersion() {
            return componentVersion;
        }

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public boolean isMultipleInstances() {
            return false;
        }

        @Override
        public Optional<List<String>> getTags() {
            return Optional.empty();
        }

        @Override
        public String getVersion() {
            return version == null ? "0.0.1" : version;
        }

        @Override
        public Optional<List<WorkflowDefinition>> getWorkflows() {
            return Optional.ofNullable(workflows);
        }
    }

    private record PolyglotWorkflowDefinition(
        String name, String label, String description, List<TaskDefinition> taskDefinitions)
        implements WorkflowDefinition {

        @Override
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        @Override
        public Optional<List<? extends Input>> getInputs() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<List<? extends Output>> getOutputs() {
            return Optional.empty();
        }

        @Override
        public Optional<List<? extends TaskDefinition>> getTasks() {
            return Optional.ofNullable(taskDefinitions);
        }

        @Override
        public Optional<List<? extends TriggerDefinition>> getTriggers() {
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: Implement `IntegrationHandlerLoader`**

Create `server/ee/libs/embedded/embedded-code-workflow-loader/src/main/java/com/bytechef/ee/embedded/codeworkflow/loader/IntegrationHandlerLoader.java`:

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.embedded.integration.IntegrationHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationHandlerLoader {

    /**
     * <b>Security Note:</b> Path traversal is intentional. The URL is derived from internal code workflow container
     * configuration, not from untrusted user input.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public static IntegrationHandler loadIntegrationHandler(URL url, Language language) {
        try {
            return switch (language) {
                case JAVA -> IntegrationHandlerPolyglotEngine.loadJava(toLocalPath(url));
                case JAVASCRIPT, PYTHON, RUBY -> IntegrationHandlerPolyglotEngine.load(
                    getLanguageId(language), Files.readString(toLocalPath(url)));
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getLanguageId(Language language) {
        return switch (language) {
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private static Path toLocalPath(URL url) throws IOException, URISyntaxException {
        URI uri = url.toURI();

        if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }

        Path tempFile = Files.createTempFile("code_workflow", null);

        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:embedded:embedded-code-workflow-loader:test --tests "com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoaderTest"
```

Expected: BUILD SUCCESSFUL. The JavaScript test must PASS everywhere; the Java
test passes on Espresso-supported platforms (SKIPPED elsewhere).

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/embedded/embedded-code-workflow-loader
git commit -m "Add IntegrationHandler polyglot loading for embedded code workflows

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: Wire `CodeWorkflowTaskExecutor`'s EMBEDDED branch

**Files:**
- Modify: `server/ee/libs/modules/components/code-workflow/build.gradle.kts` (add embedded loader dependency)
- Modify: `server/ee/libs/modules/components/code-workflow/src/main/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskExecutor.java`
- Test: `server/ee/libs/modules/components/code-workflow/src/test/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskExecutorTest.java`

**Interfaces:**
- Consumes: `IntegrationHandlerLoader.loadIntegrationHandler(URL, Language)` (Task 5), `ProjectHandlerLoader.loadProjectHandler(URL, Language)` (Task 3).
- Produces: `CodeWorkflowTaskExecutor.executePerform(String, String, String, PlatformType)` now works for `PlatformType.EMBEDDED`.

- [ ] **Step 1: Add the module dependency**

In `server/ee/libs/modules/components/code-workflow/build.gradle.kts`, add this
line directly below the existing
`implementation(project(":server:ee:libs:automation:automation-code-workflow-loader"))` line:

```kotlin
    implementation(project(":server:ee:libs:embedded:embedded-code-workflow-loader"))
```

- [ ] **Step 2: Write the failing test**

Create `server/ee/libs/modules/components/code-workflow/src/test/java/com/bytechef/ee/component/codeworkflow/task/CodeWorkflowTaskExecutorTest.java`.
Mockito is provided by the shared test conventions (if compilation says otherwise,
add `testImplementation(rootProject.libs.org.mockito.mockito.core)` — check
`gradle/libs.versions.toml` for the exact alias):

```java
/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.codeworkflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.constant.PlatformType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CodeWorkflowTaskExecutorTest {

    private static final String AUTOMATION_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'automation result'
                        }
                    ]
                }
            ]
        })
        """;

    private static final String EMBEDDED_JAVASCRIPT_SOURCE = """
        ({
            componentName: 'test-component',
            componentVersion: 1,
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'embedded result'
                        }
                    ]
                }
            ]
        })
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testExecutePerformForAutomation() throws IOException {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "automation.js", AUTOMATION_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.AUTOMATION);

        assertEquals("automation result", result);
    }

    @Test
    void testExecutePerformForEmbedded() throws IOException {
        CodeWorkflowTaskExecutor codeWorkflowTaskExecutor = createExecutor(
            "integration.js", EMBEDDED_JAVASCRIPT_SOURCE);

        Object result = codeWorkflowTaskExecutor.executePerform(
            "code-workflow-container-uuid", "my-workflow", "my-task", PlatformType.EMBEDDED);

        assertEquals("embedded result", result);
    }

    private CodeWorkflowTaskExecutor createExecutor(String scriptFileName, String scriptSource) throws IOException {
        Path scriptPath = tempDir.resolve(scriptFileName);

        Files.writeString(scriptPath, scriptSource);

        CodeWorkflowContainer codeWorkflowContainer = mock(CodeWorkflowContainer.class);

        when(codeWorkflowContainer.getLanguage()).thenReturn(Language.JAVASCRIPT);

        CodeWorkflowContainerService codeWorkflowContainerService = mock(CodeWorkflowContainerService.class);

        when(codeWorkflowContainerService.getCodeWorkflowContainer(anyString())).thenReturn(codeWorkflowContainer);

        CodeWorkflowFileStorage codeWorkflowFileStorage = mock(CodeWorkflowFileStorage.class);

        java.net.URI scriptUri = scriptPath.toUri();

        when(codeWorkflowFileStorage.getCodeWorkflowFileURL(any())).thenReturn(scriptUri.toURL());

        return new CodeWorkflowTaskExecutor(codeWorkflowFileStorage, codeWorkflowContainerService);
    }
}
```

(Adjust the `CodeWorkflowTaskExecutor` constructor argument order in the test to
match the real constructor after Task 3 removed `CacheManager` — the remaining
parameters keep their existing relative order: `codeWorkflowFileStorage,
codeWorkflowContainerService`. Verify against the actual file before running. Use
a proper `import java.net.URI;` rather than the inline qualified name.)

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :server:ee:libs:modules:components:code-workflow:test --tests "com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutorTest"
```

Expected: `testExecutePerformForAutomation` PASSES; `testExecutePerformForEmbedded`
FAILS with `IllegalArgumentException: Workflow not found` (the EMBEDDED branch does
not exist, so `workflows` stays empty).

- [ ] **Step 4: Implement the EMBEDDED branch**

In `CodeWorkflowTaskExecutor.java`, add the import:

```java
import com.bytechef.ee.embedded.codeworkflow.loader.IntegrationHandlerLoader;
import com.bytechef.embedded.integration.IntegrationHandler;
```

Replace the body of `getWorkflowDefinitions` with:

```java
    private List<WorkflowDefinition> getWorkflowDefinitions(
        CodeWorkflowContainer codeWorkflowContainer, PlatformType type) {

        List<WorkflowDefinition> workflows = List.of();

        if (PlatformType.AUTOMATION.equals(type)) {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage());

            workflows = projectHandler.getWorkflows();
        } else if (PlatformType.EMBEDDED.equals(type)) {
            IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
                codeWorkflowFileStorage.getCodeWorkflowFileURL(codeWorkflowContainer.getWorkflows()),
                codeWorkflowContainer.getLanguage());

            workflows = integrationHandler.getWorkflows();
        }

        return workflows;
    }
```

(The `// } else {TODO integration}` comment is gone.)

- [ ] **Step 5: Run the test to verify it passes**

```bash
./gradlew :server:ee:libs:modules:components:code-workflow:test --tests "com.bytechef.ee.component.codeworkflow.task.CodeWorkflowTaskExecutorTest"
```

Expected: BUILD SUCCESSFUL, 2 tests pass.

- [ ] **Step 6: Commit**

```bash
git add server/ee/libs/modules/components/code-workflow
git commit -m "Execute embedded code workflow tasks via IntegrationHandlerLoader

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: Formatting, static analysis, and full verification

**Files:**
- Possibly modified by Spotless: any file touched in Tasks 1-6.

**Interfaces:**
- Consumes: everything above.
- Produces: a clean branch where all checks pass.

- [ ] **Step 1: Run Spotless**

```bash
./gradlew spotlessApply
```

Expected: BUILD SUCCESSFUL. Review the diff it produces (`git diff`) — Spotless
must not have stripped the `@version ee` tags or mangled the text blocks.

- [ ] **Step 2: Run checks on all touched modules**

```bash
./gradlew :server:ee:libs:automation:automation-code-workflow-loader:check \
  :server:ee:libs:embedded:embedded-code-workflow-loader:check \
  :server:ee:libs:modules:components:code-workflow:check \
  :server:ee:libs:automation:automation-configuration:automation-configuration-service:check
```

Expected: BUILD SUCCESSFUL — Checkstyle, PMD, SpotBugs, and tests all pass. Fix
any violations (common ones for this change: missing blank line before control
statements, `TodoComment` if any TODO text survived, SpotBugs `PATH_TRAVERSAL_IN`
needing the `@SuppressFBWarnings` annotations shown in the task code).

- [ ] **Step 3: Compile the whole server to catch any missed call sites**

```bash
./gradlew compileJava
```

Expected: BUILD SUCCESSFUL. If any module fails on `loadProjectHandler` arity or
the removed `CacheManager` constructor parameter, fix that call site the same way
as Task 3 Step 4 and rerun.

- [ ] **Step 4: Commit any formatting fallout**

```bash
git add -A
git commit -m "Apply spotless formatting to code workflow loader changes

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

(Skip the commit if `git status` is clean.)
