/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.codeworkflow.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.embedded.integration.IntegrationHandler;
import com.bytechef.embedded.integration.definition.IntegrationDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class IntegrationHandlerPolyglotEngineTest {

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
            componentName="test-component",
            componentVersion=1,
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
        Struct.new(:componentName, :componentVersion, :workflows).new(
          "test-component",
          1,
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
            componentName: 'test-component',
            componentVersion: 1,
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
            componentName: 'test-component',
            componentVersion: 1,
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
            componentName: 'test-component',
            componentVersion: 1,
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
                componentName: 'test-component',
                componentVersion: 1,
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
    void testPerformReadsWorkflowInputsAndPriorTaskOutputsFromContext() throws Exception {
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
        TaskDefinition taskDefinition = loadSingleTask("js", ZERO_ARG_JAVASCRIPT_SOURCE);

        assertTrue(taskDefinition.getConnections()
            .isEmpty());
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
}
