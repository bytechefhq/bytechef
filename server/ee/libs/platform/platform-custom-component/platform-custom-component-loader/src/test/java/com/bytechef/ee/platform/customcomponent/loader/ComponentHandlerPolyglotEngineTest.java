/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Log;
import com.bytechef.component.definition.DynamicOptionsProperty;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.OutputDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.polyglot.PolyglotSandbox;
import com.bytechef.platform.component.polyglot.PolyglotSandboxSettings;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.graalvm.polyglot.PolyglotException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerPolyglotEngineTest {

    @AfterEach
    void afterEach() {
        PolyglotSandbox.setSettings(PolyglotSandboxSettings.defaults());
    }

    /**
     * A custom component's definition is user-supplied script, evaluated in full at load time - top-level work
     * included. It gets the same sandbox and the same ceilings the component's perform functions get, or an uploaded
     * component can burn a thread before it ever declares an action.
     */
    @Test
    void testLoadEnforcesCpuLimitOnDefinitionScript() {
        PolyglotSandbox.setSettings(
            new PolyglotSandboxSettings(
                true, Duration.ofSeconds(1), PolyglotSandboxSettings.DEFAULT_MAX_HEAP_MEMORY,
                PolyglotSandboxSettings.DEFAULT_MAX_CONCURRENT_EXECUTIONS));

        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [],
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
            PolyglotException.class, () -> ComponentHandlerPolyglotEngine.load("js", script));

        assertTrue(
            polyglotException.getMessage()
                .contains("CPU time limit"),
            polyglotException.getMessage());
    }

    @Test
    void testLoadRegistersDeclaredStaticConnection() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                connection: {
                    baseUri: "https://api.example.com",
                    authorizations: [
                        {
                            type: "OAUTH2_AUTHORIZATION_CODE",
                            authorizationUrl: "https://example.com/oauth/authorize",
                            tokenUrl: "https://example.com/oauth/token",
                            refreshUrl: "https://example.com/oauth/token",
                            scopes: ["read", "write"]
                        }
                    ],
                    properties: [
                        {name: "clientId", type: "STRING", required: true}
                    ]
                },
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        ConnectionDefinition.BaseUriFunction baseUriFunction = connectionDefinition.getBaseUri()
            .orElseThrow();

        assertEquals("https://api.example.com", baseUriFunction.apply(null, null));

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        assertEquals(1, authorizations.size());

        Authorization authorization = authorizations.getFirst();

        assertEquals(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE, authorization.getType());

        Authorization.AuthorizationUrlFunction authorizationUrlFunction = authorization.getAuthorizationUrl()
            .orElseThrow();

        assertEquals("https://example.com/oauth/authorize", authorizationUrlFunction.apply(null, null));

        Authorization.TokenUrlFunction tokenUrlFunction = authorization.getTokenUrl()
            .orElseThrow();

        assertEquals("https://example.com/oauth/token", tokenUrlFunction.apply(null, null));

        Authorization.ScopesFunction scopesFunction = authorization.getScopes()
            .orElseThrow();

        assertEquals(Map.of("read", true, "write", true), scopesFunction.apply(null, null));

        List<? extends Property> properties = authorization.getProperties();

        assertEquals(1, properties.size());
        assertEquals("clientId", properties.getFirst()
            .getName());
    }

    @Test
    void testLoadWithoutConnectionKeepsGetConnectionEmpty() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        assertTrue(componentDefinition.getConnection()
            .isEmpty());
    }

    @Test
    void testFunctionValuedTokenUrlSeamComputesFromConnectionParameters() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                connection: {
                    authorizations: [
                        {
                            type: "OAUTH2_AUTHORIZATION_CODE",
                            authorizationUrl: "https://example.com/oauth/authorize",
                            tokenUrl: function (connectionParameters) {
                                return "https://" + connectionParameters.region + ".example.com/oauth/token";
                            }
                        }
                    ]
                },
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        Authorization.TokenUrlFunction tokenUrlFunction = authorizations.getFirst()
            .getTokenUrl()
            .orElseThrow();

        String tokenUrl = tokenUrlFunction.apply(ParametersFactory.create(Map.of("region", "eu")), null);

        assertEquals("https://eu.example.com/oauth/token", tokenUrl);
    }

    @Test
    void testFunctionValuedApplySeamDecoratesRequest() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                connection: {
                    authorizations: [
                        {
                            type: "CUSTOM",
                            apply: function (connectionParameters) {
                                return {
                                    headers: {"Authorization": "Bearer " + connectionParameters.accessToken},
                                    queryParameters: {"tenant": ["acme"]}
                                };
                            }
                        }
                    ]
                },
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        Authorization.ApplyFunction applyFunction = authorizations.getFirst()
            .getApply()
            .orElseThrow();

        Authorization.ApplyResponse applyResponse = applyFunction.apply(
            ParametersFactory.create(Map.of("accessToken", "token")), null);

        assertEquals(Map.of("Authorization", List.of("Bearer token")), applyResponse.getHeaders());
        assertEquals(Map.of("tenant", List.of("acme")), applyResponse.getQueryParameters());
    }

    @Test
    void testFunctionValuedUrlSeamWithNonStringResultFailsNamingTheSeam() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                connection: {
                    authorizations: [
                        {
                            type: "OAUTH2_AUTHORIZATION_CODE",
                            tokenUrl: function (connectionParameters) {
                                return 42;
                            }
                        }
                    ]
                },
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection()
            .orElseThrow();

        List<? extends Authorization> authorizations = connectionDefinition.getAuthorizations();

        Authorization.TokenUrlFunction tokenUrlFunction = authorizations.getFirst()
            .getTokenUrl()
            .orElseThrow();

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> tokenUrlFunction.apply(ParametersFactory.create(Map.of()), null));

        assertTrue(exception.getMessage()
            .contains("tokenUrl"));
    }

    @Test
    void testLoadParsesActionProperties() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [
                    {
                        name: 'echo',
                        title: 'Echo',
                        properties: [
                            {name: 'foo', type: 'STRING', label: 'Foo', required: true},
                            {name: 'count', type: 'INTEGER', label: 'Count'}
                        ],
                        perform: function(input, connection, context) {
                            return input.foo;
                        }
                    }
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        List<? extends Property> properties = actionDefinition.getProperties();

        assertEquals(2, properties.size());
        assertEquals("foo", properties.getFirst()
            .getName());
        assertEquals(Property.Type.STRING, properties.getFirst()
            .getType());
        assertEquals("count", properties.get(1)
            .getName());
        assertEquals(Property.Type.INTEGER, properties.get(1)
            .getType());
    }

    @Test
    void testLoadWithoutActionPropertiesIsEmpty() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [
                    {name: 'echo', title: 'Echo', perform: function(input, connection, context) { return 'x'; }}
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        assertTrue(actionDefinition.getProperties()
            .isEmpty());
    }

    @Test
    void testLoadParsesActionOutputSchemaAndSample() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [
                    {
                        name: 'echo',
                        output: {
                            type: 'OBJECT',
                            properties: [
                                {name: 'id', type: 'STRING'},
                                {name: 'count', type: 'INTEGER'}
                            ]
                        },
                        sampleOutput: {id: 'abc', count: 2},
                        perform: function(input, connection, context) { return {}; }
                    }
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        OutputDefinition outputDefinition = actionDefinition.getOutputDefinition()
            .orElseThrow();

        Property outputSchema = (Property) outputDefinition.getOutputSchema();

        assertEquals(Property.Type.OBJECT, outputSchema.getType());
        assertEquals(Map.of("id", "abc", "count", 2), outputDefinition.getSampleOutput());
    }

    @Test
    void testLoadWithoutActionOutputKeepsOutputDefinitionEmpty() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [
                    {name: 'echo', perform: function(input, connection, context) { return 'x'; }}
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        assertTrue(actionDefinition.getOutputDefinition()
            .isEmpty());
    }

    @Test
    void testLoadParsesIconAndToolClusterElements() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                icon: '<svg viewBox="0 0 24 24"></svg>',
                actions: [
                    {
                        name: 'lookupCustomer',
                        title: 'Lookup Customer',
                        description: 'Finds a customer by email.',
                        tool: true,
                        properties: [{name: 'email', type: 'STRING', required: true}],
                        perform: function(input, connection, context) { return {}; }
                    },
                    {
                        name: 'plainAction',
                        perform: function(input, connection, context) { return {}; }
                    }
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        assertEquals(
            "<svg viewBox=\"0 0 24 24\"></svg>", componentDefinition.getIcon()
                .orElseThrow());

        List<? extends ClusterElementDefinition<?>> clusterElements = componentDefinition.getClusterElements();

        assertEquals(1, clusterElements.size());

        ClusterElementDefinition<?> clusterElementDefinition = clusterElements.getFirst();

        assertEquals("lookupCustomer", clusterElementDefinition.getName());
        assertEquals(BaseToolFunction.TOOLS, clusterElementDefinition.getType());
        assertEquals(1, clusterElementDefinition.getProperties()
            .size());

        // Both actions stay callable as actions; the tool flag only adds a cluster element.
        assertEquals(2, componentDefinition.getActions()
            .size());
    }

    @Test
    void testLoadWithoutIconOrToolsKeepsThemEmpty() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [{name: 'echo', perform: function(input, connection, context) { return 'x'; }}]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        assertTrue(componentDefinition.getIcon()
            .isEmpty());
        assertTrue(componentDefinition.getClusterElements()
            .isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadSupportsDynamicPropertyOptions() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                actions: [
                    {
                        name: 'pick',
                        properties: [
                            {
                                name: 'region',
                                type: 'STRING',
                                options: function (inputParameters, connectionParameters, searchText) {
                                    return [
                                        {label: 'EU ' + searchText, value: 'eu'},
                                        {label: 'US ' + inputParameters.tenant, value: 'us'}
                                    ];
                                }
                            }
                        ],
                        perform: function(input, connection, context) { return {}; }
                    }
                ]
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .getFirst();

        DynamicOptionsProperty<?> property = (DynamicOptionsProperty<?>) actionDefinition.getProperties()
            .getFirst();

        OptionsDataSource<?> optionsDataSource = property.getOptionsDataSource()
            .orElseThrow();

        ActionDefinition.OptionsFunction<String> optionsFunction =
            (ActionDefinition.OptionsFunction<String>) optionsDataSource.getOptions();

        List<? extends Option<String>> options = optionsFunction.apply(
            ParametersFactory.create(Map.of("tenant", "acme")), ParametersFactory.create(Map.of()), Map.of(), "x",
            mock(ActionContext.class));

        assertEquals(2, options.size());
        assertEquals("EU x", options.getFirst()
            .getLabel());
        assertEquals("eu", options.getFirst()
            .getValue());
        assertEquals("US acme", options.get(1)
            .getLabel());
    }

    @Test
    void testLoadRegistersPollingTriggerAndExecutesPoll() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                triggers: [
                    {
                        name: 'newItem',
                        title: 'New Item',
                        type: 'POLLING',
                        properties: [{name: 'folder', type: 'STRING'}],
                        output: {type: 'OBJECT', properties: [{name: 'id', type: 'STRING'}]},
                        poll: function (inputParameters, connectionParameters, closureParameters) {
                            return {
                                records: [{id: inputParameters.folder + '-1'}],
                                closureParameters: {cursor: (closureParameters.cursor || 0) + 1}
                            };
                        }
                    }
                ],
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
            .getFirst();

        assertEquals("newItem", triggerDefinition.getName());
        assertEquals(TriggerDefinition.TriggerType.POLLING, triggerDefinition.getType());
        assertEquals(1, triggerDefinition.getProperties()
            .size());
        assertTrue(triggerDefinition.getOutputDefinition()
            .isPresent());

        TriggerDefinition.PollFunction pollFunction = (TriggerDefinition.PollFunction) triggerDefinition.getPoll()
            .orElseThrow();

        TriggerDefinition.PollOutput pollOutput = pollFunction.apply(
            ParametersFactory.create(Map.of("folder", "inbox")), ParametersFactory.create(Map.of()),
            ParametersFactory.create(Map.of("cursor", 4)), mock(TriggerContext.class));

        assertEquals(List.of(Map.of("id", "inbox-1")), pollOutput.records());
        assertEquals(5, ((Number) pollOutput.closureParameters()
            .get("cursor")).intValue());
    }

    @Test
    void testLoadRegistersStaticWebhookTriggerAndExecutesTheRequest() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                triggers: [
                    {
                        name: 'onPush',
                        type: 'STATIC_WEBHOOK',
                        webhookRequest: function (inputParameters, connectionParameters, request) {
                            return {event: request.body.event, method: request.method};
                        }
                    }
                ],
                actions: []
            })
            """;

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        TriggerDefinition triggerDefinition = componentHandler.getDefinition()
            .getTriggers()
            .getFirst();

        assertEquals(TriggerDefinition.TriggerType.STATIC_WEBHOOK, triggerDefinition.getType());

        TriggerDefinition.WebhookRequestFunction webhookRequestFunction =
            triggerDefinition.getWebhookRequest()
                .orElseThrow();

        TriggerDefinition.WebhookBody body = mock(TriggerDefinition.WebhookBody.class);

        when(body.getContent()).thenReturn(Map.of("event", "push"));

        // The guest sees one request object rather than the host's four arguments.
        Object output = webhookRequestFunction.apply(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()),
            mock(TriggerDefinition.HttpHeaders.class), mock(TriggerDefinition.HttpParameters.class), body,
            TriggerDefinition.WebhookMethod.POST, ParametersFactory.create(Map.of()), mock(TriggerContext.class));

        assertEquals(Map.of("event", "push", "method", "POST"), output);
    }

    @Test
    void testLoadRegistersDynamicWebhookTriggerAndRunsEnable() throws Exception {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                triggers: [
                    {
                        name: 'onPush',
                        type: 'DYNAMIC_WEBHOOK',
                        webhookEnable: function (inputParameters, connectionParameters, args) {
                            return {subscriptionId: 'sub-for-' + args.webhookUrl};
                        },
                        webhookDisable: function () {},
                        webhookRequest: function (inputParameters, connectionParameters, request) {
                            return request.body;
                        }
                    }
                ],
                actions: []
            })
            """;

        TriggerDefinition triggerDefinition = ComponentHandlerPolyglotEngine.load("js", script)
            .getDefinition()
            .getTriggers()
            .getFirst();

        assertEquals(TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK, triggerDefinition.getType());

        TriggerDefinition.WebhookEnableFunction webhookEnableFunction = triggerDefinition.getWebhookEnable()
            .orElseThrow();

        // Whatever enable returns is what the platform hands back on each request and on disable.
        TriggerDefinition.WebhookEnableOutput output = webhookEnableFunction.apply(
            ParametersFactory.create(Map.of()), ParametersFactory.create(Map.of()), "https://hook.test/1",
            "execution-1", mock(TriggerContext.class));

        assertEquals(Map.of("subscriptionId", "sub-for-https://hook.test/1"), output.parameters());
        assertTrue(triggerDefinition.getWebhookRequest()
            .isPresent());
    }

    @Test
    void testLoadRejectsTriggerTypesNeedingAnEnableLifecycle() {
        String script = """
            ({
                name: 'test-component',
                version: 1,
                triggers: [{name: 'onPush', type: 'LISTENER'}],
                actions: []
            })
            """;

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> ComponentHandlerPolyglotEngine.load("js", script));

        assertTrue(exception.getMessage()
            .contains("LISTENER"));
    }

    @Test
    void testExecutePerformReadsInputParameter() {
        String script = """
            ({
                name: 'test-component',
                title: 'Test Component',
                version: 1,
                actions: [
                    {
                        name: 'echo',
                        title: 'Echo',
                        perform: function(input, connection, context) {
                            return input.foo;
                        }
                    }
                ]
            })
            """;

        Object result = executePerform(
            script, "echo", Map.of("foo", "bar"), Map.of(), mock(ActionContext.class));

        assertEquals("bar", result);
    }

    @Test
    void testExecutePerformCallsHttp() {
        String script = """
            ({
                name: 'test-component',
                title: 'Test Component',
                version: 1,
                actions: [
                    {
                        name: 'ping',
                        title: 'Ping',
                        perform: function(input, connection, context) {
                            var response = context.http({method: 'GET', url: 'https://api.example.com/ping'});

                            return {statusCode: response.statusCode, headers: response.headers, body: response.body};
                        }
                    }
                ]
            })
            """;

        ActionContext actionContext = mock(ActionContext.class);
        Http.Response httpResponse = mock(Http.Response.class);

        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getHeaders()).thenReturn(Map.of("X-Request-Id", List.of("abc")));
        when(httpResponse.getBody()).thenReturn("pong");
        when(actionContext.http(any())).thenReturn(httpResponse);

        Object result = executePerform(script, "ping", Map.of(), Map.of(), actionContext);

        Map<?, ?> resultMap = (Map<?, ?>) result;

        assertEquals(200L, ((Number) resultMap.get("statusCode")).longValue());
        assertEquals("pong", resultMap.get("body"));
        assertEquals(Map.of("X-Request-Id", List.of("abc")), resultMap.get("headers"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testExecutePerformCallsHttpCoercesScalarHeadersAndQueryParameters() {
        String script = """
            ({
                name: 'test-component',
                title: 'Test Component',
                version: 1,
                actions: [
                    {
                        name: 'ping',
                        title: 'Ping',
                        perform: function(input, connection, context) {
                            return context.http({
                                method: 'GET',
                                url: 'https://api.example.com/ping',
                                headers: {'X-Foo': 'bar'},
                                queryParameters: {q: 'value'}
                            });
                        }
                    }
                ]
            })
            """;

        ActionContext actionContext = mock(ActionContext.class);
        Http http = mock(Http.class);
        Http.Executor executor = mock(Http.Executor.class);
        Http.Response httpResponse = mock(Http.Response.class);

        when(actionContext.http(any())).thenAnswer(invocation -> {
            ContextFunction<Http, Http.Response> httpFunction = invocation.getArgument(0);

            try {
                return httpFunction.apply(http);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        when(http.exchange(any(), any())).thenReturn(executor);
        when(executor.headers(any(Map.class))).thenReturn(executor);
        when(executor.queryParameters(any(Map.class))).thenReturn(executor);
        when(executor.execute()).thenReturn(httpResponse);
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getHeaders()).thenReturn(Map.of());
        when(httpResponse.getBody()).thenReturn(null);

        executePerform(script, "ping", Map.of(), Map.of(), actionContext);

        ArgumentCaptor<Map<String, List<String>>> headersCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Map<String, List<String>>> queryParametersCaptor = ArgumentCaptor.forClass(Map.class);

        verify(executor).headers(headersCaptor.capture());
        verify(executor).queryParameters(queryParametersCaptor.capture());

        assertEquals(Map.of("X-Foo", List.of("bar")), headersCaptor.getValue());
        assertEquals(Map.of("q", List.of("value")), queryParametersCaptor.getValue());
    }

    @Test
    void testExecutePerformCallsLog() {
        String script = """
            ({
                name: 'test-component',
                title: 'Test Component',
                version: 1,
                actions: [
                    {
                        name: 'logAction',
                        title: 'Log',
                        perform: function(input, connection, context) {
                            context.log('INFO', 'x');

                            return null;
                        }
                    }
                ]
            })
            """;

        ActionContext actionContext = mock(ActionContext.class);
        Log log = mock(Log.class);

        doAnswer(invocation -> {
            ContextConsumer<Log> logConsumer = invocation.getArgument(0);

            logConsumer.accept(log);

            return null;
        }).when(actionContext)
            .log(any());

        executePerform(script, "logAction", Map.of(), Map.of(), actionContext);

        verify(log).info("x");
    }

    @Test
    void testExecutePerformToleratesLegacyZeroArgFunction() {
        String script = """
            ({
                name: 'test-component',
                title: 'Test Component',
                version: 1,
                actions: [
                    {
                        name: 'legacy',
                        title: 'Legacy',
                        perform: function() {
                            return 42;
                        }
                    }
                ]
            })
            """;

        Object result = executePerform(script, "legacy", Map.of(), Map.of(), mock(ActionContext.class));

        assertEquals(42, ((Number) result).intValue());
    }

    private static Object executePerform(
        String script, String actionName, Map<String, ?> inputParameters, Map<String, ?> connectionParameters,
        ActionContext actionContext) {

        ComponentHandler componentHandler = ComponentHandlerPolyglotEngine.load("js", script);

        ComponentDefinition componentDefinition = componentHandler.getDefinition();

        ActionDefinition actionDefinition = componentDefinition.getActions()
            .stream()
            .filter(action -> actionName.equals(action.getName()))
            .findFirst()
            .orElseThrow();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        Parameters inputParametersInstance = ParametersFactory.create(inputParameters);
        Parameters connectionParametersInstance = ParametersFactory.create(connectionParameters);

        try {
            return performFunction.apply(inputParametersInstance, connectionParametersInstance, actionContext);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
