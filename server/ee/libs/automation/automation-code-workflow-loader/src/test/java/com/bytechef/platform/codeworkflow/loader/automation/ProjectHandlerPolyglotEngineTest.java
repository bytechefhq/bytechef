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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
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
