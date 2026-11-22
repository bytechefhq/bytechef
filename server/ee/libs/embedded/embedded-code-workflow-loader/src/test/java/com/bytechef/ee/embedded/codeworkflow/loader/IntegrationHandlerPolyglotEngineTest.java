/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotSandboxSettings;
import com.bytechef.workflow.definition.ComponentTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.TaskContext;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TaskDefinition.PerformFunction;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Timeout(120)
class IntegrationHandlerPolyglotEngineTest {

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
                componentName: 'test-component',
                componentVersion: 1,
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
            PolyglotException.class, () -> IntegrationHandlerPolyglotEngine.load("js", source));

        assertTrue(
            polyglotException.getMessage()
                .contains("CPU time limit"),
            polyglotException.getMessage());
    }

    private static final String CONTEXT_JAVASCRIPT_SOURCE = """
        ({
            componentName: 'test-component',
            componentVersion: 1,
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
            componentName="test-component",
            componentVersion=1,
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

    private static final String LOG_JAVASCRIPT_SOURCE = """
        ({
            componentName: 'test-component',
            componentVersion: 1,
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
            componentName: 'test-component',
            componentVersion: 1,
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
                    componentName: 'test-component',
                    componentVersion: 1,
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
    void testPerformReadsWorkflowInputsAndPriorTaskOutputsFromInput() throws Exception {
        String source = """
            ({
                componentName: 'test-component',
                componentVersion: 1,
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
                componentName: 'test-component',
                componentVersion: 1,
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
    void testLoadParsesDeclaredTaskConnectionsDeclaredAsMap() {
        String connectionsSource = """
            ({
                componentName: 'test-component',
                componentVersion: 1,
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
    void testLoadsAPythonDictIntegration() throws Exception {
        String source = """
            def echo(input, context):
                return input["message"]


            integration = {
                "componentName": "sampleIntegration",
                "componentVersion": 1,
                "workflows": [{"name": "echo-message", "tasks": [{"name": "echo", "perform": echo}]}],
            }

            integration
            """;

        IntegrationDefinition integrationDefinition = IntegrationHandlerPolyglotEngine.load("python", source)
            .getDefinition();

        assertEquals("sampleIntegration", integrationDefinition.getComponentName());
        assertEquals(1, integrationDefinition.getComponentVersion());

        Object result = perform(loadSingleTask("python", source), Map.of("message", "hi"), Map.of(),
            new RecordingTaskContext(null));

        assertEquals("hi", result);
    }

    @Test
    void testLoadsThePythonWorkflowApiIntegrationExample() throws Exception {
        String source = Files.readString(
            Path.of(System.getProperty("bytechef.sdks.dir"), "backend/python/workflow-api/examples/integration.py"),
            StandardCharsets.UTF_8);

        IntegrationDefinition integrationDefinition = IntegrationHandlerPolyglotEngine.load("python", source)
            .getDefinition();

        assertEquals("httpClient", integrationDefinition.getComponentName());
        assertEquals(1, integrationDefinition.getComponentVersion());

        Object result = perform(loadSingleTask("python", source), Map.of("message", "hi"), Map.of(),
            new RecordingTaskContext(null));

        assertEquals("hi", result);
    }

    @Test
    void testLoadsAnInputThatReferencesAComponentInput() {
        String source = """
            def post(input, context):
                return input["channel"]


            integration = {
                "componentName": "googleMail",
                "componentVersion": 1,
                "workflows": [{
                    "name": "notify",
                    "inputs": [
                        {"name": "channel", "componentName": "slack", "componentVersion": 1, "groupName": "channel"},
                        {"name": "note"},
                    ],
                    "tasks": [{"name": "post", "perform": post}],
                }],
            }

            integration
            """;

        WorkflowDefinition workflowDefinition = IntegrationHandlerPolyglotEngine.load("python", source)
            .getDefinition()
            .getWorkflows()
            .orElseThrow()
            .getFirst();

        List<? extends Input> inputs = workflowDefinition.getInputs()
            .orElseThrow();

        assertEquals(
            Optional.of(new Input.ComponentInputReference("slack", 1, "channel")), inputs.getFirst()
                .getComponentInput());
        assertEquals(Optional.empty(), inputs.get(1)
            .getComponentInput());
    }

    @Test
    void testLoadsAnInternalOnlyInput() {
        String source = """
            ({
                componentName: 'googleMail',
                componentVersion: 1,
                workflows: [{
                    name: 'notify',
                    inputs: [{name: 'contactPageUrl', internalOnly: true}, {name: 'note'}],
                    tasks: [{name: 'post', perform: (input, context) => input.note}],
                }],
            })
            """;

        WorkflowDefinition workflowDefinition = IntegrationHandlerPolyglotEngine.load("js", source)
            .getDefinition()
            .getWorkflows()
            .orElseThrow()
            .getFirst();

        List<? extends Input> inputs = workflowDefinition.getInputs()
            .orElseThrow();

        assertTrue(inputs.getFirst()
            .isInternalOnly());
        assertFalse(inputs.get(1)
            .isInternalOnly());
    }

    @Test
    void testRefusesAnInputWithAPartialComponentInputReference() {
        String source = """
            ({
                componentName: 'googleMail',
                componentVersion: 1,
                workflows: [{
                    name: 'notify',
                    inputs: [{name: 'channel', componentName: 'slack'}],
                    tasks: [{name: 'post', perform: () => 'ok'}]
                }]
            })
            """;

        Exception exception = assertThrows(
            Exception.class, () -> IntegrationHandlerPolyglotEngine.load("js", source)
                .getDefinition()
                .getWorkflows());

        assertTrue(
            String.valueOf(exception.getMessage())
                .contains("componentName, componentVersion and groupName together"),
            exception.getMessage());
    }

    @Test
    void testLoadsAComponentTaskForJavaScript() {
        String source = """
            ({
                componentName: 'test-component',
                componentVersion: 1,
                workflows: [
                    {
                        name: 'my-workflow',
                        tasks: [
                            {name: 'fetch', perform: () => 'x'},
                            {
                                name: 'respond',
                                type: 'apiPlatform/v1/responseToAPIRequest',
                                parameters: {responseType: 'SUCCESS', response: {success: '${fetch}'}}
                            }
                        ]
                    }
                ]
            })
            """;

        IntegrationHandler integrationHandler = IntegrationHandlerPolyglotEngine.load("js", source);

        IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

        List<WorkflowDefinition> workflows = integrationDefinition.getWorkflows()
            .orElseThrow();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        ComponentTaskDefinition componentTaskDefinition = assertInstanceOf(
            ComponentTaskDefinition.class, tasks.get(1));

        assertEquals("apiPlatform/v1/responseToAPIRequest", componentTaskDefinition.getType());

        Map<String, ?> parameters = componentTaskDefinition.getParameters();

        assertEquals("SUCCESS", parameters.get("responseType"));

        Map<?, ?> response = (Map<?, ?>) parameters.get("response");

        assertEquals("${fetch}", response.get("success"));
    }

    private static TaskDefinition loadSingleTask(String languageId, String source) {
        IntegrationHandler integrationHandler = IntegrationHandlerPolyglotEngine.load(languageId, source);

        IntegrationDefinition integrationDefinition = integrationHandler.getDefinition();

        List<WorkflowDefinition> workflows = integrationDefinition.getWorkflows()
            .orElseThrow();

        WorkflowDefinition workflowDefinition = workflows.getFirst();

        List<? extends WorkflowTaskDefinition> tasks = workflowDefinition.getTasks()
            .orElseThrow();

        return (TaskDefinition) tasks.getFirst();
    }

    private static Object perform(
        TaskDefinition taskDefinition, Map<String, ?> input, Map<String, ?> connection, TaskContext context)
        throws Exception {

        PerformFunction performFunction = taskDefinition.getPerform();

        return performFunction.apply(input, connection, context);
    }
}
