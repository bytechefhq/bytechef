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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
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
            toUrl(scriptPath), Language.JAVASCRIPT, IntegrationHandlerLoader.JavaLoader.CLASS_LOADER, "js-cache-key",
            new ConcurrentMapCacheManager());

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
            toUrl(jarPath), Language.JAVA, IntegrationHandlerLoader.JavaLoader.ESPRESSO, "espresso-cache-key",
            new ConcurrentMapCacheManager());

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

    @Test
    void testLoadIntegrationHandlerFromJavaJarWithClassLoader() throws IOException {
        Path jarPath = buildFixtureJar(tempDir, JAVA_SOURCE, "TestIntegrationHandler");

        IntegrationHandler integrationHandler = IntegrationHandlerLoader.loadIntegrationHandler(
            toUrl(jarPath), Language.JAVA, IntegrationHandlerLoader.JavaLoader.CLASS_LOADER, "class-loader-cache-key",
            new ConcurrentMapCacheManager());

        IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

        assertEquals("test-component", integrationDefinition.getComponentName());
        assertEquals(1, integrationDefinition.getComponentVersion());

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
