/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Authorization;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
class GuestComponentBridgeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RecordingHostBridge hostBridge = new RecordingHostBridge();

    @BeforeEach
    void setUp() {
        GuestComponentBridge.hostBridgeOverride = hostBridge;
    }

    @AfterEach
    void tearDown() {
        GuestComponentBridge.hostBridgeOverride = null;
    }

    @Test
    void testDescribeComponent() throws Exception {
        String json = GuestComponentBridge.describeComponent(SampleComponentHandler.class.getName());

        Map<String, ?> component = OBJECT_MAPPER.readValue(json, Map.class);

        assertEquals("sample", component.get("name"));
        assertEquals("Sample", component.get("title"));
        assertEquals(1, component.get("version"));

        List<Map<String, ?>> actions = (List<Map<String, ?>>) component.get("actions");

        assertEquals(1, actions.size());

        Map<String, ?> action = actions.getFirst();

        assertEquals("greet", action.get("name"));
        assertEquals("Greet", action.get("title"));

        List<Map<String, ?>> properties = (List<Map<String, ?>>) action.get("properties");

        assertEquals(3, properties.size());

        Map<String, ?> nameProperty = properties.getFirst();

        assertEquals("name", nameProperty.get("name"));
        assertEquals("STRING", nameProperty.get("type"));
        assertEquals("Name", nameProperty.get("label"));
        assertEquals(Boolean.TRUE, nameProperty.get("required"));

        Map<String, ?> modeProperty = properties.get(1);

        List<Map<String, ?>> options = (List<Map<String, ?>>) modeProperty.get("options");

        assertEquals(2, options.size());
        assertEquals("Formal", options.getFirst()
            .get("label"));

        Map<String, ?> addressProperty = properties.get(2);

        assertEquals("OBJECT", addressProperty.get("type"));

        List<Map<String, ?>> addressChildren = (List<Map<String, ?>>) addressProperty.get("properties");

        assertEquals("city", addressChildren.getFirst()
            .get("name"));

        List<Map<String, ?>> triggers = (List<Map<String, ?>>) component.get("triggers");

        assertEquals(1, triggers.size());
        assertEquals("onEvent", triggers.getFirst()
            .get("name"));
        assertEquals("POLLING", triggers.getFirst()
            .get("type"));

        List<String> unsupported = (List<String>) component.get("unsupported");

        assertTrue(unsupported.stream()
            .anyMatch(entry -> entry.contains("trigger onEvent execution")));
    }

    @Test
    void testExecuteActionPerform() throws Exception {
        String resultJson = GuestComponentBridge.executeActionPerform(
            SampleComponentHandler.class.getName(), "greet",
            "{\"name\": \"World\", \"repeat\": 2}", "{}", "trace-42");

        Map<String, ?> result = OBJECT_MAPPER.readValue(resultJson, Map.class);

        assertEquals("Hello World x2", result.get("greeting"));
        assertEquals("trace-42", result.get("traceId"));
        assertEquals(List.of("INFO|greeting World|null"), hostBridge.logEntries);
    }

    @Test
    void testDescribeComponentSerializesStaticConnection() throws Exception {
        String json = GuestComponentBridge.describeComponent(ConnectionSampleComponentHandler.class.getName());

        Map<String, ?> component = OBJECT_MAPPER.readValue(json, Map.class);

        Map<String, ?> connection = (Map<String, ?>) component.get("connection");

        assertEquals("https://api.example.com", connection.get("baseUri"));

        List<Map<String, ?>> authorizations = (List<Map<String, ?>>) connection.get("authorizations");

        assertEquals(1, authorizations.size());

        Map<String, ?> authorization = authorizations.getFirst();

        assertEquals("OAUTH2_AUTHORIZATION_CODE", authorization.get("type"));
        assertEquals("https://example.com/oauth/authorize", authorization.get("authorizationUrl"));
        assertEquals("https://example.com/oauth/token", authorization.get("tokenUrl"));
        assertEquals(List.of("read", "write"), authorization.get("scopes"));

        List<Map<String, ?>> properties = (List<Map<String, ?>>) authorization.get("properties");

        assertEquals("clientId", properties.getFirst()
            .get("name"));

        List<String> unsupported = (List<String>) component.get("unsupported");

        assertTrue(unsupported.stream()
            .noneMatch(entry -> entry.equals("connection")));
    }

    @Test
    void testDescribeComponentReportsDynamicConnectionUnsupported() throws Exception {
        String json = GuestComponentBridge.describeComponent(DynamicConnectionComponentHandler.class.getName());

        Map<String, ?> component = OBJECT_MAPPER.readValue(json, Map.class);

        assertEquals(null, component.get("connection"));

        List<String> unsupported = (List<String>) component.get("unsupported");

        assertTrue(unsupported.contains("connection"));
    }

    public static class ConnectionSampleComponentHandler implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return component("connection-sample")
                .title("Connection Sample")
                .version(1)
                .connection(
                    ComponentDsl.connection()
                        .baseUri((connectionParameters, context) -> "https://api.example.com")
                        .authorizations(
                            ComponentDsl.authorization(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                                .authorizationUrl(
                                    (connectionParameters, context) -> "https://example.com/oauth/authorize")
                                .tokenUrl((connectionParameters, context) -> "https://example.com/oauth/token")
                                .scopes((connectionParameters, context) -> {
                                    Map<String, Boolean> scopes = new java.util.LinkedHashMap<>();

                                    scopes.put("read", true);
                                    scopes.put("write", true);

                                    return scopes;
                                })
                                .properties(
                                    string("clientId")
                                        .label("Client Id")
                                        .required(true))))
                .actions(
                    action("noop")
                        .title("Noop"));
        }
    }

    public static class DynamicConnectionComponentHandler implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return component("dynamic-connection-sample")
                .title("Dynamic Connection Sample")
                .version(1)
                .connection(
                    ComponentDsl.connection()
                        .authorizations(
                            ComponentDsl.authorization(Authorization.AuthorizationType.OAUTH2_AUTHORIZATION_CODE)
                                .tokenUrl((connectionParameters, context) -> "https://"
                                    + connectionParameters.getRequiredString("region") + ".example.com/oauth/token")))
                .actions(
                    action("noop")
                        .title("Noop"));
        }
    }

    public static class SampleComponentHandler implements ComponentHandler {

        @Override
        public ComponentDefinition getDefinition() {
            return component("sample")
                .title("Sample")
                .description("Sample component")
                .version(1)
                .actions(
                    action("greet")
                        .title("Greet")
                        .description("Greets someone")
                        .properties(
                            string("name")
                                .label("Name")
                                .required(true),
                            string("mode")
                                .label("Mode")
                                .options(
                                    option("Formal", "formal"),
                                    option("Casual", "casual")),
                            object("address")
                                .label("Address")
                                .properties(
                                    string("city")
                                        .label("City")))
                        .perform((PerformFunction) (inputParameters, connectionParameters, context) -> {
                            String name = inputParameters.getRequiredString("name");

                            context.log(log -> log.info("greeting {}", name));

                            int repeat = inputParameters.getInteger("repeat", 1);

                            return Map.of(
                                "greeting", "Hello %s x%d".formatted(name, repeat),
                                "traceId", context.getTraceId());
                        }))
                .triggers(
                    trigger("onEvent")
                        .title("On Event")
                        .type(TriggerType.POLLING)
                        .properties(
                            integer("interval")
                                .label("Interval")));
        }
    }

    private static class RecordingHostBridge implements HostBridge {

        private final List<String> logEntries = new ArrayList<>();

        @Override
        public String httpExecute(String requestJson) {
            return "{\"statusCode\": 200, \"headers\": {}, \"body\": null}";
        }

        @Override
        public boolean isEditorEnvironment() {
            return false;
        }

        @Override
        public void log(String level, String message, String exceptionMessage) {
            logEntries.add(level + "|" + message + "|" + exceptionMessage);
        }
    }
}
