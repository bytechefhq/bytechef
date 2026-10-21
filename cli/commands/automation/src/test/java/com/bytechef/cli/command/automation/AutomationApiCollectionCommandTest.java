/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.cli.command.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationApiCollectionCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String COLLECTION_JSON =
        "{\"id\":5,\"name\":\"orders\",\"contextPath\":\"orders\",\"projectId\":3,\"projectVersion\":2,"
            + "\"collectionVersion\":1,\"enabled\":true}";

    private static final String ENDPOINT_JSON =
        "{\"id\":9,\"apiCollectionId\":5,\"name\":\"create-order\",\"httpMethod\":\"POST\",\"path\":\"/orders\","
            + "\"workflowUuid\":\"wf-1\",\"enabled\":true}";

    private static final String VERSION_UPDATE_JSON = "{\"apiCollection\":" + COLLECTION_JSON
        + ",\"removedEndpointNames\":[\"old-endpoint\"],\"disabledEndpointNames\":[\"create-order\"],"
        + "\"missingConnections\":[{\"endpointName\":\"create-order\",\"workflowNodeName\":\"http_1\","
        + "\"key\":\"connection\",\"componentName\":\"http\"}],"
        + "\"missingInputs\":[{\"endpointName\":\"create-order\",\"inputName\":\"orderId\"}]}";

    private record Result(int exitCode, String output) {
    }

    @Test
    void testCreateSendsBody() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "POST /api/automation/v1/workspaces/1/api-collections",
                RoutedStubApi.response(201, COLLECTION_JSON)))) {

            Result result = run(
                stub, "create", "--workspace-id", "1", "--project-id", "3", "--project-version", "2", "--name",
                "orders", "--context-path", "orders", "--description", "OrderApi");

            assertEquals(0, result.exitCode(), result.output());

            RoutedStubApi.Request request = stub.lastRequest("POST", "/api/automation/v1/workspaces/1/api-collections")
                .orElseThrow();
            JsonNode body = OBJECT_MAPPER.readTree(request.body());

            assertEquals(3, body.get("projectId")
                .asLong());
            assertEquals(2, body.get("projectVersion")
                .asInt());
            assertEquals("orders", body.get("name")
                .asText());
            assertEquals("orders", body.get("contextPath")
                .asText());
            assertEquals("OrderApi", body.get("description")
                .asText());
            assertEquals("STAGING", request.environment());
            assertTrue(result.output()
                .contains("\"id\" : 5"), result.output());
        }
    }

    @Test
    void testListForwardsProjectId() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/workspaces/1/api-collections",
                RoutedStubApi.response(200, "[" + COLLECTION_JSON + "]")))) {

            Result result = run(stub, "list", "--workspace-id", "1", "--project-id", "3");

            assertEquals(0, result.exitCode(), result.output());
            assertEquals(
                "projectId=3", stub.lastRequest("GET", "/api/automation/v1/workspaces/1/api-collections")
                    .orElseThrow()
                    .query());
        }
    }

    @Test
    void testUpdateSendsOnlyTheGivenFields() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT /api/automation/v1/api-collections/5", RoutedStubApi.response(200, COLLECTION_JSON)))) {

            Result result = run(stub, "update", "--id", "5", "--name", "renamed");

            assertEquals(0, result.exitCode(), result.output());

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", "/api/automation/v1/api-collections/5")
                    .orElseThrow()
                    .body());

            assertEquals("renamed", body.get("name")
                .asText());
            assertFalse(body.has("description"));
        }
    }

    @Test
    void testDeleteCallsTheCollection() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("DELETE /api/automation/v1/api-collections/5", RoutedStubApi.response(204, "")))) {

            Result result = run(stub, "delete", "--id", "5");

            assertEquals(0, result.exitCode(), result.output());
            assertTrue(stub.lastRequest("DELETE", "/api/automation/v1/api-collections/5")
                .isPresent());
        }
    }

    @Test
    void testEndpointCreateParsesConnectionOptions() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "POST /api/automation/v1/api-collections/5/endpoints", RoutedStubApi.response(201, ENDPOINT_JSON)))) {

            Result result = run(
                stub, "endpoint", "create", "--collection-id", "5", "--name", "create-order", "--method", "post",
                "--path", "/orders", "--workflow-uuid", "wf-1", "--connection", "http_1:connection=7",
                "--connection", "slack_1:auth=8", "--inputs-json", "{\"orderId\":\"abc\"}");

            assertEquals(0, result.exitCode(), result.output());

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", "/api/automation/v1/api-collections/5/endpoints")
                    .orElseThrow()
                    .body());

            assertEquals("POST", body.get("httpMethod")
                .asText());
            assertEquals("wf-1", body.get("workflowUuid")
                .asText());
            assertEquals(2, body.get("connections")
                .size());
            assertEquals("http_1", body.get("connections")
                .get(0)
                .get("workflowNodeName")
                .asText());
            assertEquals("connection", body.get("connections")
                .get(0)
                .get("key")
                .asText());
            assertEquals(7, body.get("connections")
                .get(0)
                .get("connectionId")
                .asLong());
            assertEquals("auth", body.get("connections")
                .get(1)
                .get("key")
                .asText());
            assertEquals("abc", body.get("inputs")
                .get("orderId")
                .asText());
        }
    }

    @Test
    void testEndpointUpdateWithoutConnectionsOrInputsKeepsThem() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT /api/automation/v1/api-collection-endpoints/9", RoutedStubApi.response(200, ENDPOINT_JSON)))) {

            Result result = run(stub, "endpoint", "update", "--id", "9", "--path", "/orders/new");

            assertEquals(0, result.exitCode(), result.output());

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", "/api/automation/v1/api-collection-endpoints/9")
                    .orElseThrow()
                    .body());

            assertEquals("/orders/new", body.get("path")
                .asText());
            assertFalse(body.has("connections"));
            assertFalse(body.has("inputs"));
        }
    }

    @Test
    void testEndpointEnableAndDisable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "PUT /api/automation/v1/api-collection-endpoints/9/enabled",
                RoutedStubApi.response(200, ENDPOINT_JSON)))) {

            assertEquals(0, run(stub, "endpoint", "disable", "--id", "9").exitCode());

            assertEquals(
                "{\"enabled\":false}", stub.lastRequest("PUT", "/api/automation/v1/api-collection-endpoints/9/enabled")
                    .orElseThrow()
                    .body());

            assertEquals(0, run(stub, "endpoint", "enable", "--id", "9").exitCode());

            assertEquals(
                "{\"enabled\":true}", stub.lastRequest("PUT", "/api/automation/v1/api-collection-endpoints/9/enabled")
                    .orElseThrow()
                    .body());
        }
    }

    @Test
    void testUpdateVersionPrintsRemovedAndMissing() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "PUT /api/automation/v1/api-collections/5/project-version",
                RoutedStubApi.response(200, VERSION_UPDATE_JSON)))) {

            Result result = run(
                stub, "update-version", "--id", "5", "--project-version", "3", "--remove-missing-endpoints");

            assertEquals(0, result.exitCode(), result.output());

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", "/api/automation/v1/api-collections/5/project-version")
                    .orElseThrow()
                    .body());

            assertEquals(3, body.get("projectVersion")
                .asInt());
            assertTrue(body.get("removeMissingEndpoints")
                .asBoolean());
            assertTrue(result.output()
                .contains("old-endpoint"), result.output());
            assertTrue(result.output()
                .contains("orderId"), result.output());
            assertTrue(result.output()
                .contains("http_1"), result.output());
        }
    }

    @Test
    void testMalformedConnectionOptionFails() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            Result result = run(
                stub, "endpoint", "create", "--collection-id", "5", "--name", "create-order", "--method", "POST",
                "--path", "/orders", "--workflow-uuid", "wf-1", "--connection", "http_1-connection-7");

            assertEquals(1, result.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testMalformedInputsJsonFails() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            Result result = run(
                stub, "endpoint", "create", "--collection-id", "5", "--name", "create-order", "--method", "POST",
                "--path", "/orders", "--workflow-uuid", "wf-1", "--inputs-json", "[1,2]");

            assertEquals(1, result.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    private static Result run(RoutedStubApi stub, String... arguments) throws IOException {
        String[] commandLine = new String[arguments.length + 8];

        commandLine[0] = "automation";
        commandLine[1] = "api-collection";

        System.arraycopy(arguments, 0, commandLine, 2, arguments.length);

        int offset = arguments.length + 2;

        commandLine[offset] = "--host";
        commandLine[offset + 1] = stub.host();
        commandLine[offset + 2] = "--token";
        commandLine[offset + 3] = "btc_x";
        commandLine[offset + 4] = "--environment";
        commandLine[offset + 5] = "STAGING";

        String[] trimmed = java.util.Arrays.copyOf(commandLine, offset + 6);

        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();

        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));

        try {
            int exitCode = CliApplication.execute(trimmed);

            return new Result(exitCode, captured.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(original);
        }
    }
}
