/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.platform.codeworkflow.loader.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotSandboxSettings;
import com.bytechef.workflow.definition.ComponentTaskDefinition;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
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
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class ProjectHandlerPolyglotEngineTest {

    @AfterEach
    void afterEach() {
        PolyglotSandbox.setSettings(PolyglotSandboxSettings.defaults());
    }

    @Test
    void testLoadEnforcesCpuLimitOnDefinitionScript() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, Duration.ofSeconds(1), PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        String source = """
            ({
                name: 'test-project',
                workflows: [],
                filler: (function () {
                    let total = 0;

                    for (let i = 0; i < 200000000; i++) {
                        total += i;
                    }

                    return total;
                })()
            })
            """;

        PolyglotException polyglotException = assertThrows(
            PolyglotException.class, () -> ProjectHandlerPolyglotEngine.load("js", source));

        assertTrue(
            polyglotException.getMessage()
                .contains("CPU time limit"),
            polyglotException.getMessage());
    }

    private static final String CONTEXT_JAVASCRIPT_SOURCE = """
        ({
            name: 'test-project',
            workflows: [
                {
                    name: 'my-workflow',
                    tasks: [
                        {
                            name: 'my-task',
                            perform: (input, context) => {
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
                            "perform": lambda input, context: context.component.mock.doIt({"x": 1}, "conn")
                        }
                    ]
                }
            ]
        )
        """;

    private static final String SCRIPT_TASK_RUNNER_JAVASCRIPT_SOURCE =
        """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                perform: (input, context) => {
                                    const result = context.component.script.python({
                                        script: "def perform(input, context):\\n    return {'doubled': input['value'] * 2}\\n",
                                        input: {value: 21},
                                        taskRunner: {type: 'process'}
                                    });

                                    return result.vars.doubled;
                                }
                            }
                        ]
                    }
                ]
            })
            """;

    private static final String SCRIPT_TASK_RUNNER_PYTHON_SOURCE = """
        def double(input, context):
            result = context.component.script.python({
                "script": "def perform(input, context):\\n    return {'doubled': input['value'] * 2}\\n",
                "input": {"value": 21},
                "taskRunner": {"type": "process"},
            })

            return result["vars"]["doubled"]


        {
            "name": "test-project",
            "workflows": [{"name": "my-workflow", "tasks": [{"name": "my-task", "perform": double}]}],
        }
        """;

    private static final String DICT_PYTHON_SOURCE = """
        def greet(input, context):
            name = input["name"]

            context.log("info", "Greeting " + name)

            return "Hello, " + name + "!"


        project = {
            "name": "test-project",
            "version": "1.0.0",
            "workflows": [
                {
                    "name": "my-workflow",
                    "inputs": [{"name": "name", "label": "Name", "type": "STRING", "required": True}],
                    "tasks": [{"name": "greet", "perform": greet}],
                }
            ],
        }

        project
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
                            perform: (input, context) => {
                                context.log('warn', 'log message');

                                return 'logged';
                            }
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
                            perform: (input, context) => context.component.aiAgent.chat({messages: []}, null, {
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

    @Test
    @SuppressWarnings("unchecked")
    void testPerformPassesClusterElementsComposedAtTheCallSite() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", CLUSTER_ELEMENTS_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext("answer");

        perform(taskDefinition, Map.of(), Map.of(), taskContext);

        Map<String, ?> clusterElements = taskContext.getClusterElements();

        assertNull(taskContext.getConnectionName());
        assertEquals(
            Map.of("type", "openAi/v1/model", "connection", "openai-prod", "parameters", Map.of("model", "gpt-4o")),
            clusterElements.get("model"));
        assertEquals(
            List.of(Map.of("type", "slack/v1/sendMessage", "connection", "slack-prod", "name", "post_to_slack")),
            (List<Map<String, ?>>) clusterElements.get("tools"));
    }

    @Test
    void testLoadsAPythonDictProject() throws Exception {
        ProjectDefinition projectDefinition = ProjectHandlerPolyglotEngine.load("python", DICT_PYTHON_SOURCE)
            .getDefinition();

        assertEquals("test-project", projectDefinition.getName());
        assertEquals("1.0.0", projectDefinition.getVersion());

        WorkflowDefinition workflowDefinition = projectDefinition.getWorkflows()
            .getFirst();

        assertEquals(
            "name", workflowDefinition.getInputs()
                .orElseThrow()
                .getFirst()
                .getName());

        TaskDefinition taskDefinition = loadSingleTask("python", DICT_PYTHON_SOURCE);

        Object result = perform(taskDefinition, Map.of("name", "Ada"), Map.of(), new RecordingTaskContext(null));

        assertEquals("Hello, Ada!", result);
    }

    @Test
    void testLoadsThePythonWorkflowApiExample() throws Exception {
        String source = Files.readString(
            Path.of(System.getProperty("bytechef.sdks.dir"), "backend/python/workflow-api/examples/orders.py"),
            StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition = ProjectHandlerPolyglotEngine.load("python", source)
            .getDefinition();

        assertEquals("orders", projectDefinition.getName());

        WorkflowDefinition workflowDefinition = projectDefinition.getWorkflows()
            .getFirst();

        assertEquals(
            "customerId", workflowDefinition.getInputs()
                .orElseThrow()
                .getFirst()
                .getName());
        assertEquals(
            "summarize", workflowDefinition.getOutputs()
                .orElseThrow()
                .getFirst()
                .getTask());
        assertEquals(
            "schedule/v1/interval", workflowDefinition.getTriggers()
                .orElseThrow()
                .getFirst()
                .getType());

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        assertEquals(3, tasks.size());
        assertEquals(CompositeTaskDefinition.Type.PARALLEL, ((CompositeTaskDefinition) tasks.get(1)).getType());

        RecordingTaskContext taskContext = new RecordingTaskContext(List.of("order-1", "order-2"));

        Object result = perform((TaskDefinition) tasks.getFirst(), Map.of("customerId", "42"), Map.of(), taskContext);

        assertEquals("httpClient", taskContext.getComponentName());
        assertEquals("get", taskContext.getActionName());
        assertEquals("orders_api", taskContext.getConnectionName());
        assertEquals(List.of("order-1", "order-2"), result);
    }

    @Test
    void testLoadsAProjectWithoutWorkflows() {
        ProjectDefinition projectDefinition = ProjectHandlerPolyglotEngine.load("js", "({name: 'empty'})")
            .getDefinition();

        assertEquals("empty", projectDefinition.getName());
        assertEquals(List.of(), projectDefinition.getWorkflows());
    }

    @Test
    void testPerformReceivesComponentCapableContextForJavaScript() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", CONTEXT_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext(Map.of("y", 2));

        Object result = perform(taskDefinition, Map.of(), Map.of(), taskContext);

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

        Object result = perform(taskDefinition, Map.of(), Map.of(), taskContext);

        assertEquals("mock", taskContext.getComponentName());
        assertEquals("doIt", taskContext.getActionName());
        assertEquals("conn", taskContext.getConnectionName());

        Map<String, ?> input = taskContext.getInput();

        assertEquals(1, ((Number) input.get("x")).intValue());
        assertEquals("python result", result);
    }

    @Test
    void testPerformPassesATaskRunnerToTheScriptComponentFromJavaScript() throws Exception {
        assertScriptComponentCallOnTaskRunner(loadSingleTask("js", SCRIPT_TASK_RUNNER_JAVASCRIPT_SOURCE));
    }

    @Test
    void testPerformPassesATaskRunnerToTheScriptComponentFromPython() throws Exception {
        assertScriptComponentCallOnTaskRunner(loadSingleTask("python", SCRIPT_TASK_RUNNER_PYTHON_SOURCE));
    }

    @Test
    void testPerformLogDelegatesToTaskContext() throws Exception {
        TaskDefinition taskDefinition = loadSingleTask("js", LOG_JAVASCRIPT_SOURCE);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        Object result = perform(taskDefinition, Map.of(), Map.of(), taskContext);

        assertEquals("WARN", taskContext.getLogLevel());
        assertEquals("log message", taskContext.getLogMessage());
        assertEquals("logged", result);
    }

    @Test
    void testThreeArgumentPerformReceivesDeclaredConnectionsForJavaScript() throws Exception {
        String source =
            """
                ({
                    name: 'test-project',
                    workflows: [
                        {
                            name: 'my-workflow',
                            tasks: [
                                {
                                    name: 'my-task',
                                    connections: [{componentName: 'slack', name: 'slack-prod'}],
                                    perform: (input, connection, context) => connection['slack-prod'].region + ':' + input.channel
                                }
                            ]
                        }
                    ]
                })
                """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        Object result =
            perform(taskDefinition, Map.of("channel", "general"), Map.of("slack-prod", Map.of("region", "eu")),
                new RecordingTaskContext(null));

        assertEquals("eu:general", result);
    }

    @Test
    void testThreeArgumentPerformReceivesDeclaredConnectionsForPython() throws Exception {
        String source = """
            def post(input, connection, context):
                return connection["slack-prod"]["region"] + ":" + input["channel"]


            {
                "name": "test-project",
                "componentName": "test-component",
                "componentVersion": 1,
                "workflows": [{"name": "my-workflow", "tasks": [{"name": "my-task", "perform": post}]}],
            }
            """;

        TaskDefinition taskDefinition = loadSingleTask("python", source);

        Object result =
            perform(taskDefinition, Map.of("channel", "general"), Map.of("slack-prod", Map.of("region", "eu")),
                new RecordingTaskContext(null));

        assertEquals("eu:general", result);
    }

    @Test
    void testTwoArgumentPerformWithAnOptionalThirdParameterReceivesInputAndContextForPython() throws Exception {
        String source = """
            def post(input, context, suffix="!"):
                context.log("info", "posting")

                return input["channel"] + suffix


            {
                "name": "test-project",
                "componentName": "test-component",
                "componentVersion": 1,
                "workflows": [{"name": "my-workflow", "tasks": [{"name": "my-task", "perform": post}]}],
            }
            """;

        TaskDefinition taskDefinition = loadSingleTask("python", source);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        Object result = perform(taskDefinition, Map.of("channel", "general"),
            Map.of("slack-prod", Map.of("region", "eu")), taskContext);

        assertEquals("general!", result);
        assertEquals("posting", taskContext.getLogMessage());
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
                                perform: (input, context) => context.log('warning', 'log message')
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        RecordingTaskContext taskContext = new RecordingTaskContext(null);

        assertThrows(
            Exception.class, () -> perform(taskDefinition, Map.of(), Map.of(), taskContext));
    }

    @Test
    void testPerformReadsWorkflowInputsAndPriorTaskOutputsFromInput() throws Exception {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'my-task',
                                perform: (input, context) => input['my-task1'].id + ':' + input.input.email
                            }
                        ]
                    }
                ]
            })
            """;

        TaskDefinition taskDefinition = loadSingleTask("js", source);

        Object result = perform(taskDefinition,
            Map.of("input", Map.of("email", "a@b.com"), "my-task1", Map.of("id", "3")), Map.of(),
            new RecordingTaskContext(null));

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
                                "perform": lambda input, context: "x"
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
        TaskDefinition taskDefinition = loadSingleTask("js", LOG_JAVASCRIPT_SOURCE);

        assertTrue(taskDefinition.getConnections()
            .isEmpty());
    }

    @Test
    void testLoadParsesAnInputThatReferencesAComponentInput() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        inputs: [{name: 'channel', componentName: 'slack', componentVersion: 1, groupName: 'channel'}],
                        tasks: [{name: 'post', perform: () => 'c'}]
                    }
                ]
            })
            """;

        Input input = ProjectHandlerPolyglotEngine.load("js", source)
            .getDefinition()
            .getWorkflows()
            .getFirst()
            .getInputs()
            .orElseThrow()
            .getFirst();

        assertEquals(Optional.of(new Input.ComponentInputReference("slack", 1, "channel")), input.getComponentInput());
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
                .map(WorkflowTaskDefinition::getName)
                .toList());

        CompositeTaskDefinition forkJoinTask = (CompositeTaskDefinition) tasks.get(2);

        assertEquals(CompositeTaskDefinition.Type.FORK_JOIN, forkJoinTask.getType());
        assertEquals(2, forkJoinTask.getBranches()
            .size());
        assertEquals(
            List.of("slack", "record"), forkJoinTask.getBranches()
                .getFirst()
                .stream()
                .map(WorkflowTaskDefinition::getName)
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
                                branches: [[{name: 'slack', perform: (input, context) => 'posted'}]]
                            }
                        ]
                    }
                ]
            })
            """;

        CompositeTaskDefinition forkJoinTask = (CompositeTaskDefinition) loadTasks("js", source).getFirst();

        TaskDefinition nestedTask = (TaskDefinition) forkJoinTask.getBranches()
            .getFirst()
            .getFirst();

        assertEquals("posted", perform(nestedTask, Map.of(), Map.of(), new RecordingTaskContext(null)));
    }

    @Test
    void testLoadRejectsInvalidGroups() {
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

    @Test
    void testLoadsAComponentTaskForJavaScript() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {name: 'fetch', perform: () => 'x'},
                            {
                                name: 'respond',
                                label: 'Respond',
                                type: 'apiPlatform/v1/responseToAPIRequest',
                                parameters: {responseType: 'SUCCESS', response: {success: '${fetch}'}}
                            }
                        ]
                    }
                ]
            })
            """;

        List<? extends WorkflowTaskDefinition> tasks = loadTasks("js", source);

        ComponentTaskDefinition componentTaskDefinition = assertInstanceOf(
            ComponentTaskDefinition.class, tasks.get(1));

        assertEquals("respond", componentTaskDefinition.getName());
        assertEquals("apiPlatform/v1/responseToAPIRequest", componentTaskDefinition.getType());
        assertEquals(Optional.of("Respond"), componentTaskDefinition.getLabel());

        Map<String, ?> parameters = componentTaskDefinition.getParameters();

        assertEquals("SUCCESS", parameters.get("responseType"));

        Map<?, ?> response = (Map<?, ?>) parameters.get("response");

        assertEquals("${fetch}", response.get("success"));
    }

    @Test
    void testLoadsAComponentTaskForPython() {
        String source = """
            def fetch(input, context):
                return "x"


            project = {
                "name": "test-project",
                "workflows": [
                    {
                        "name": "my-workflow",
                        "tasks": [
                            {"name": "fetch", "perform": fetch},
                            {
                                "name": "respond",
                                "type": "apiPlatform/v1/responseToAPIRequest",
                                "parameters": {
                                    "responseType": "SUCCESS",
                                    "response": {"success": {"items": [{"id": "y"}]}},
                                },
                            },
                        ],
                    }
                ],
            }

            project
            """;

        List<? extends WorkflowTaskDefinition> tasks = loadTasks("python", source);

        ComponentTaskDefinition componentTaskDefinition = assertInstanceOf(
            ComponentTaskDefinition.class, tasks.get(1));

        Map<String, ?> parameters = componentTaskDefinition.getParameters();

        Map<?, ?> response = assertInstanceOf(Map.class, parameters.get("response"));

        Map<?, ?> success = assertInstanceOf(Map.class, response.get("success"));

        List<?> items = assertInstanceOf(List.class, success.get("items"));

        Map<?, ?> item = assertInstanceOf(Map.class, items.getFirst());

        assertEquals("y", item.get("id"));
    }

    @Test
    void testLoadsComponentTasksNestedInGroups() {
        String source = """
            ({
                name: 'test-project',
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {
                                name: 'enrich',
                                type: 'parallel',
                                tasks: [
                                    {name: 'customer', perform: () => 'c'},
                                    {name: 'log', type: 'logger/v1/info', parameters: {text: 'x'}}
                                ]
                            },
                            {
                                name: 'notify',
                                type: 'forkJoin',
                                branches: [
                                    [{name: 'slack', perform: () => 's'}],
                                    [{name: 'record', type: 'logger/v1/info', parameters: {text: 'y'}}]
                                ]
                            }
                        ]
                    }
                ]
            })
            """;

        List<? extends WorkflowTaskDefinition> tasks = loadTasks("js", source);

        CompositeTaskDefinition parallelTask = (CompositeTaskDefinition) tasks.getFirst();

        ComponentTaskDefinition nestedInParallel = assertInstanceOf(
            ComponentTaskDefinition.class, parallelTask.getTasks()
                .get(1));

        assertEquals("logger/v1/info", nestedInParallel.getType());

        CompositeTaskDefinition forkJoinTask = (CompositeTaskDefinition) tasks.get(1);

        List<? extends List<? extends WorkflowTaskDefinition>> branches = forkJoinTask.getBranches();

        ComponentTaskDefinition nestedInBranch = assertInstanceOf(
            ComponentTaskDefinition.class, branches.get(1)
                .getFirst());

        assertEquals("record", nestedInBranch.getName());
    }

    @Test
    void testLoadRejectsInvalidComponentTasks() {
        assertGroupRejected(
            "{name: 'a', type: 'logger/v1/info', perform: () => 1}", "cannot declare a perform");

        assertGroupRejected(
            "{name: 'a', type: 'logger/v1/info', connections: [{componentName: 'x', name: 'x'}]}",
            "cannot declare connections");

        assertGroupRejected("{name: 'a', type: 'logger/v1/info', parameters: 'x'}", "parameters as a map");

        assertGroupRejected(
            "{name: 'a', type: 'apiPlatform/responseToAPIRequest'}", "<component>/v<version>/<action>");

        assertGroupRejected("{name: 'a', type: 'logger/v01/info'}", "<component>/v<version>/<action>");

        assertGroupRejected("{name: 'a', type: 'logger/v1234567890/info'}", "<component>/v<version>/<action>");

        assertGroupRejected(
            "{name: 'a', type: 'logger/v1/info', tasks: [{name: 'b', perform: () => 1}]}",
            "cannot declare tasks or branches");

        assertGroupRejected(
            "{name: 'a', type: 'logger/v1/info', branches: [[{name: 'b', perform: () => 1}]]}",
            "cannot declare tasks or branches");

        assertGroupRejected(
            "{name: 'a', perform: () => 1}, {name: 'a', type: 'logger/v1/info'}", "declared more than once");
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

    private static void assertScriptComponentCallOnTaskRunner(TaskDefinition taskDefinition) throws Exception {
        RecordingTaskContext taskContext = new RecordingTaskContext(
            Map.of("exitCode", 0, "vars", Map.of("doubled", 42)));

        Object result = perform(taskDefinition, Map.of(), Map.of(), taskContext);

        assertEquals("script", taskContext.getComponentName());
        assertEquals("python", taskContext.getActionName());
        assertNull(taskContext.getConnectionName());

        Map<String, ?> input = taskContext.getInput();

        assertEquals("def perform(input, context):\n    return {'doubled': input['value'] * 2}\n", input.get("script"));
        assertEquals(21, ((Number) ((Map<?, ?>) input.get("input")).get("value")).intValue());
        assertEquals(Map.of("type", "process"), input.get("taskRunner"));
        assertEquals(42, ((Number) result).intValue());
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

    private static Object perform(
        TaskDefinition taskDefinition, Map<String, ?> input, Map<String, ?> connection, TaskContext context)
        throws Exception {

        PerformFunction performFunction = taskDefinition.getPerform();

        return performFunction.apply(input, connection, context);
    }
}
