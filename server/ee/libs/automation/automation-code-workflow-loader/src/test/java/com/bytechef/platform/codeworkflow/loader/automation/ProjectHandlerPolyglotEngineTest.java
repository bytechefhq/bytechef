/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
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
import org.junit.jupiter.api.Disabled;
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

    private static final String CLUSTER_ELEMENTS_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: (context) => context.component.aiAgent.chat({messages: []}, null, {
                                model: {
                                    type: 'openAi/v1/model',
                                    connection: 'openai-prod',
                                    parameters: {model: 'gpt-4o'}
                                },
                                tools: [
                                    {type: 'slack/v1/sendMessage', connection: 'slack-prod', name: 'post_to_slack'}
                                ]
                            })
                        }
                    ]
                }
            ]
        })
        """;

    @TempDir
    private Path tempDir;

    @Test
    @SuppressWarnings("unchecked")
    void testPerformPassesClusterElementsComposedAtTheCallSite() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", CLUSTER_ELEMENTS_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext("answer");

        taskDefinition.getPerform()
            .apply(taskContext);

        Map<String, ?> clusterElements = taskContext.getClusterElements();

        // A null connection name still has to reach the host as null rather than failing the third argument's arity.
        assertNull(taskContext.getConnectionName());
        assertEquals(
            Map.of("type", "openAi/v1/model", "connection", "openai-prod", "parameters", Map.of("model", "gpt-4o")),
            clusterElements.get("model"));
        assertEquals(
            List.of(Map.of("type", "slack/v1/sendMessage", "connection", "slack-prod", "name", "post_to_slack")),
            (List<Map<String, ?>>) clusterElements.get("tools"));
    }

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

    // RUBY-DISABLED: org.graalvm.polyglot:ruby is published only up to 25.0.0 and crashes on the pinned
    // Truffle 25.2.4; the ruby dependency is commented out so the language is not even installed. Remove
    // this @Disabled once a polyglot ruby jar built on Truffle 25.2+ is published (or GraalVM is
    // downgraded). Grep RUBY-DISABLED.
    @Disabled("RUBY-DISABLED")
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

        assertEquals("WARN", taskContext.getLogLevel());
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
    void testPerformRejectsAnUnknownLogLevel() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                perform: (context) => context.log('warning', 'log message')
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        assertThrows(
            Exception.class, () -> taskDefinition.getPerform()
                .apply(taskContext));
    }

    @Test
    void testPerformReadsWorkflowInputsAndPriorTaskOutputsFromContext() throws Exception {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                perform: (context) =>
                                    context.input('my-task1').id + ':' + context.input().input.email
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        RecordingTaskContext taskContext = new RecordingTaskContext(
            null, Map.of(), Map.of("input", Map.of("email", "a@b.com"), "my-task1", Map.of("id", "3")));

        Object result = taskDefinition.getPerform()
            .apply(taskContext);

        assertEquals("3:a@b.com", result);
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

    @Test
    void testLoadParsesDeclaredInputsAndOutputs() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        inputs: [{name: 'orderId', label: 'Order ID', type: 'STRING', required: true}],
                        outputs: [{name: 'customer', task: 'fetch-customer'}, {name: 'ok', value: true}],
                        tasks: [{name: 'fetch-customer', perform: () => 'c'}]
                    }
                ]
            })
            """;

        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.load("js", source);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        WorkflowDefinition workflowDefinition = projectDefinition.getWorkflows()
            .getFirst();

        Input input = workflowDefinition.getInputs()
            .orElseThrow()
            .getFirst();

        assertEquals("orderId", input.getName());
        assertEquals("Order ID", input.getLabel());
        assertEquals("STRING", input.getType());
        assertTrue(input.isRequired());

        List<? extends Output> outputs = workflowDefinition.getOutputs()
            .orElseThrow();

        assertEquals("fetch-customer", outputs.getFirst()
            .getTask());
        assertEquals(Boolean.TRUE, outputs.get(1)
            .getValue());
    }

    @Test
    void testLoadParsesDeclaredTriggers() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        triggers: [
                            {name: 'daily', type: 'schedule/v1/interval', parameters: {interval: 1, unit: 'DAY'}}
                        ],
                        tasks: [{name: 'my-task', perform: () => 'x'}]
                    }
                ]
            })
            """;

        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.load("js", source);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        WorkflowDefinition workflowDefinition = projectDefinition.getWorkflows()
            .getFirst();

        // A trigger names a component the platform already provides, so only its type and parameters cross.
        com.bytechef.workflow.definition.TriggerDefinition triggerDefinition = workflowDefinition.getTriggers()
            .orElseThrow()
            .getFirst();

        assertEquals("daily", triggerDefinition.getName());
        assertEquals("schedule/v1/interval", triggerDefinition.getType());
        assertEquals("DAY", triggerDefinition.getParameters()
            .get("unit"));
    }

    @Test
    void testLoadParsesParallelAndForkJoinGroups() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {name: 'fetch', perform: () => 'fetched'},
                            {
                                name: 'enrich',
                                type: 'parallel',
                                tasks: [
                                    {name: 'customer', perform: () => 'c'},
                                    {name: 'inventory', perform: () => 'i'}
                                ]
                            },
                            {
                                name: 'notify',
                                type: 'forkJoin',
                                branches: [
                                    [{name: 'slack', perform: () => 's'}, {name: 'record', perform: () => 'r'}],
                                    [{name: 'email', perform: () => 'e'}]
                                ]
                            }
                        ]
                    }
                ]
            })
            """;

        List<? extends WorkflowTaskDefinition> tasks = loadTasks("js", source);

        assertEquals(3, tasks.size());

        CompositeTaskDefinition parallelTask = (CompositeTaskDefinition) tasks.get(1);

        assertEquals(CompositeTaskDefinition.Type.PARALLEL, parallelTask.getType());
        assertEquals(
            List.of("customer", "inventory"), parallelTask.getTasks()
                .stream()
                .map(TaskDefinition::getName)
                .toList());

        CompositeTaskDefinition forkJoinTask = (CompositeTaskDefinition) tasks.get(2);

        assertEquals(CompositeTaskDefinition.Type.FORK_JOIN, forkJoinTask.getType());
        assertEquals(2, forkJoinTask.getBranches()
            .size());
        assertEquals(
            List.of("slack", "record"), forkJoinTask.getBranches()
                .getFirst()
                .stream()
                .map(TaskDefinition::getName)
                .toList());
    }

    @Test
    void testPerformResolvesATaskNestedInAGroup() throws Exception {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'notify',
                                type: 'forkJoin',
                                branches: [[{name: 'slack', perform: (context) => 'posted'}]]
                            }
                        ]
                    }
                ]
            })
            """;

        CompositeTaskDefinition forkJoinTask = (CompositeTaskDefinition) loadTasks("js", source).getFirst();

        TaskDefinition nestedTask = forkJoinTask.getBranches()
            .getFirst()
            .getFirst();

        assertEquals("posted", nestedTask.getPerform()
            .apply(new RecordingTaskContext(null)));
    }

    @Test
    void testLoadRejectsInvalidGroups() {
        // A duplicate name would make one task's output unreachable through context.input(name).
        assertGroupRejected(
            "{name: 'a', type: 'parallel', tasks: [{name: 'x', perform: () => 1}, {name: 'x', perform: () => 2}]}",
            "declared more than once");

        assertGroupRejected(
            "{name: 'a', type: 'parallel', perform: () => 1, tasks: [{name: 'x', perform: () => 2}]}",
            "cannot declare a perform of its own");

        assertGroupRejected(
            "{name: 'a', type: 'parallel', tasks: [{name: 'b', type: 'parallel', tasks: []}]}",
            "a group inside a group is not supported");

        assertGroupRejected("{name: 'a', type: 'sequence', tasks: []}", "may only be parallel or forkJoin");

        assertGroupRejected("{name: 'a', type: 'parallel', tasks: []}", "non-empty tasks list");

        assertGroupRejected("{name: 'a', type: 'forkJoin', branches: [[]]}", "non-empty list of tasks");
    }

    private static void assertGroupRejected(String taskSource, String expectedMessage) {
        String source = "({name: 'test-project', workflows: [{name: 'my-workflow', tasks: [" + taskSource
            + "]}]})";

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> loadTasks("js", source));

        assertTrue(
            exception.getMessage()
                .contains(expectedMessage),
            exception.getMessage());
    }

    private static List<? extends WorkflowTaskDefinition> loadTasks(String languageId, String source) {
        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.load(languageId, source);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        return workflowDefinition.getTasks()
            .orElseThrow();
    }

    private static TaskDefinition loadSingleTask(String languageId, String source) {
        ProjectHandler projectHandler = ProjectHandlerPolyglotEngine.load(languageId, source);

        ProjectDefinition projectDefinition = projectHandler.getDefinition();

        List<WorkflowDefinition> workflows = projectDefinition.getWorkflows();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        return (TaskDefinition) tasks.getFirst();
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

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        assertEquals(1, tasks.size());

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

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

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        TaskDefinition taskDefinition = (TaskDefinition) tasks.getFirst();

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
