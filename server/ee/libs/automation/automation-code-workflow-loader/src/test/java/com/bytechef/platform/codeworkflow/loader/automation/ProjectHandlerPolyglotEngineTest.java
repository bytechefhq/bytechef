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
import com.bytechef.workflow.definition.ConnectionRequirement;
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
import java.util.Map;
import java.util.OptionalInt;
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
                                    .connections(WorkflowDsl.connection("slack", "slack-prod"))
                                    .perform(() -> "hello")));
            }
        }
        """;

    private static final String CONTEXT_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: (context) => {
                                const result = context.component.mock.doIt({x: 1}, 'conn');

                                return result.y;
                            }
                        }
                    ]
                }
            ]
        })
        """;

    private static final String CONTEXT_PYTHON_SOURCE = """
        import types

        types.SimpleNamespace(
            name="test-project",
            workflows=[
                {
                    "name": "my-workflow",
                    "tasks": [
                        {
                            "name": "my-task",
                            "perform": lambda *args: args[0].component.mock.doIt({"x": 1}, "conn")
                        }
                    ]
                }
            ]
        )
        """;

    private static final String CONTEXT_RUBY_SOURCE = """
        Struct.new(:name, :workflows).new(
          "test-project",
          [
            {
              "name" => "my-workflow",
              "tasks" => [
                {
                  "name" => "my-task",
                  "perform" => lambda { |context| context.component.mock.doIt({ "x" => 1 }, "conn") }
                }
              ]
            }
          ]
        )
        """;

    private static final String LOG_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: (context) => {
                                context.log('warn', 'log message');

                                return 'logged';
                            }
                        }
                    ]
                }
            ]
        })
        """;

    private static final String ZERO_ARG_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: () => 'legacy'
                        }
                    ]
                }
            ]
        })
        """;

    @TempDir
    private Path tempDir;

    @Test
    void testPerformReceivesComponentCapableContextForJavaScript() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", CONTEXT_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext(Map.of("y", 2));

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals("conn", taskContext.getConnectionName());

        Map<String, ?> input = taskContext.getInput();

        assertEquals(1, ((Number) input.get("x")).intValue());
        assertEquals(2, ((Number) result).intValue());
    }

    @Test
    void testPerformReceivesComponentCapableContextForPython() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("python", CONTEXT_PYTHON_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext("python result");

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals("conn", taskContext.getConnectionName());

        Map<String, ?> input = taskContext.getInput();

        assertEquals(1, ((Number) input.get("x")).intValue());
        assertEquals("python result", result);
    }

    @Test
    void testPerformReceivesComponentCapableContextForRuby() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("ruby", CONTEXT_RUBY_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext("ruby result");

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals("conn", taskContext.getConnectionName());

        Map<String, ?> input = taskContext.getInput();

        assertEquals(1, ((Number) input.get("x")).intValue());
        assertEquals("ruby result", result);
    }

    @Test
    void testPerformLogDelegatesToTaskContext() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", LOG_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("warn", taskContext.getLogLevel());
        assertEquals("log message", taskContext.getLogMessage());
        assertEquals("logged", result);
    }

    @Test
    void testZeroArgPerformStillRunsWithTaskContext() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", ZERO_ARG_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("legacy", result);
    }

    @Test
    void testPerformReadsConnectionParametersFromContext() throws Exception {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                connections: [{componentName: 'slack', name: 'slack-prod'}],
                                perform: (context) => context.connection('slack-prod').region
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        RecordingTaskContext taskContext = new RecordingTaskContext(null, Map.of("region", "eu"));

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("slack-prod", taskContext.getConnectionName());
        assertEquals("eu", result);
    }

    @Test
    void testLoadParsesDeclaredTaskConnectionsForJavaScript() {
        String connectionsSource = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                connections: [
                                    {componentName: 'slack', name: 'slack-prod'},
                                    {componentName: 'httpClient', componentVersion: 2, name: 'billing-api'}
                                ],
                                perform: () => 'x'
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", connectionsSource);

        List<? extends ConnectionRequirement> connections = taskDefinition.getConnections()
            .orElseThrow();

        assertEquals(2, connections.size());

        ConnectionRequirement first = connections.getFirst();

        assertEquals("slack", first.getComponentName());
        assertEquals(OptionalInt.empty(), first.getComponentVersion());
        assertEquals("slack-prod", first.getName());

        ConnectionRequirement second = connections.get(1);

        assertEquals("httpClient", second.getComponentName());
        assertEquals(OptionalInt.of(2), second.getComponentVersion());
        assertEquals("billing-api", second.getName());
    }

    @Test
    void testLoadParsesDeclaredTaskConnectionsForPython() {
        String connectionsSource = """
            import types

            types.SimpleNamespace(
                name="test-project",
                workflows=[
                    {
                        "name": "my-workflow",
                        "tasks": [
                            {
                                "name": "my-task",
                                "connections": [
                                    {"componentName": "slack", "name": "slack-prod"}
                                ],
                                "perform": lambda context: "x"
                            }
                        ]
                    }
                ]
            )
            """;

        TaskDefinition taskDefinition = loadSingleTask("python", connectionsSource);

        List<? extends ConnectionRequirement> connections = taskDefinition.getConnections()
            .orElseThrow();

        assertEquals(1, connections.size());

        ConnectionRequirement connectionRequirement = connections.getFirst();

        assertEquals("slack", connectionRequirement.getComponentName());
        assertEquals("slack-prod", connectionRequirement.getName());
    }

    @Test
    void testLoadParsesDeclaredTaskConnectionsDeclaredAsMap() {
        String connectionsSource = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                connections: {
                                    'slack-prod': {componentName: 'slack'},
                                    'billing-api': {componentName: 'httpClient', componentVersion: 2}
                                },
                                perform: () => 'x'
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", connectionsSource);

        List<? extends ConnectionRequirement> connections = taskDefinition.getConnections()
            .orElseThrow();

        assertEquals(2, connections.size());

        ConnectionRequirement slackConnection = connections.stream()
            .filter(connection -> "slack-prod".equals(connection.getName()))
            .findFirst()
            .orElseThrow();

        assertEquals("slack", slackConnection.getComponentName());
        assertEquals(OptionalInt.empty(), slackConnection.getComponentVersion());

        ConnectionRequirement billingConnection = connections.stream()
            .filter(connection -> "billing-api".equals(connection.getName()))
            .findFirst()
            .orElseThrow();

        assertEquals("httpClient", billingConnection.getComponentName());
        assertEquals(OptionalInt.of(2), billingConnection.getComponentVersion());
    }

    @Test
    void testLoadWithoutConnectionsKeepsGetConnectionsEmpty() {
        TaskDefinition taskDefinition = loadSingleTask("js", ZERO_ARG_JAVASCRIPT_SOURCE);

        assertTrue(taskDefinition.getConnections()
            .isEmpty());
    }

    private static TaskDefinition loadSingleTask(String languageId, String source) {
        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.load(languageId, source);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        return tasks.getFirst();
    }

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

        List<? extends ConnectionRequirement> connections = taskDefinition.getConnections()
            .orElseThrow();

        assertEquals(1, connections.size());

        ConnectionRequirement connectionRequirement = connections.getFirst();

        assertEquals("slack", connectionRequirement.getComponentName());
        assertEquals(OptionalInt.empty(), connectionRequirement.getComponentVersion());
        assertEquals("slack-prod", connectionRequirement.getName());
    }

    @Test
    void testExecuteJavaPerformThreadsTaskContextThroughEspressoBridge() throws Exception {
        assumeEspressoAvailable();

        String contextFixtureSource = """
            import com.bytechef.automation.project.ProjectHandler;
            import com.bytechef.automation.project.definition.ProjectDefinition;
            import com.bytechef.automation.project.definition.ProjectDsl;
            import com.bytechef.workflow.definition.WorkflowDsl;
            import java.util.Map;

            public class ContextEspressoProjectHandler implements ProjectHandler {

                @Override
                public ProjectDefinition getDefinition() {
                    return ProjectDsl.project("context-espresso-project")
                        .version("1.0.0")
                        .workflows(
                            WorkflowDsl.workflow("my-workflow")
                                .tasks(
                                    WorkflowDsl.task("my-task")
                                        .perform(context -> context.component(
                                            "mock", "doIt", Map.of("x", 1), "conn"))));
                }
            }
            """;

        Path jarPath = buildFixtureJar(tempDir, contextFixtureSource, "ContextEspressoProjectHandler");

        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.loadJava(jarPath);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends TaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = tasks.getFirst();

        RecordingTaskContext taskContext = new RecordingTaskContext("espresso result");

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("espresso result", result);
        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals(Map.of("x", 1), taskContext.getInput());
        assertEquals("conn", taskContext.getConnectionName());
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
