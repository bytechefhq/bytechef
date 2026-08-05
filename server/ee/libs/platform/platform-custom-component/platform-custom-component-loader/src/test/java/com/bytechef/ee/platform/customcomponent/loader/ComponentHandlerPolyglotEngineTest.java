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
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Log;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.platform.component.definition.ParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ComponentHandlerPolyglotEngineTest {

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
