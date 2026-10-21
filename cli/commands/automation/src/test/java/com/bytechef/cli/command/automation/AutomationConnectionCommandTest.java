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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationConnectionCommandTest {

    private static final String CONNECTIONS_PATH = "/api/automation/v1/connections";

    private static final String CONNECTIONS =
        """
            [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"authorizationType":"OAUTH2_AUTHORIZATION_CODE","credentialStatus":"VALID","aiProvider":false,"usable":true},
             {"id":-1,"name":"OpenAI (AI provider)","componentName":"openai","connectionVersion":1,"authorizationType":"API_KEY","credentialStatus":"VALID","aiProvider":true,"usable":true}]""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private AutomationConnectionCommand command() {
        AutomationConnectionCommand automationConnectionCommand = new AutomationConnectionCommand();

        automationConnectionCommand.setConfigPath(tempDir.resolve("no-config"));
        automationConnectionCommand.setEnvironmentVariables(Map.of());
        automationConnectionCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationConnectionCommand;
    }

    @Test
    void testListSendsTheWorkspaceTheComponentAndTheDevelopmentHeaderThroughTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            int exitCode = CliApplication.execute(
                "automation", "connection", "list", "--component-name", "hubspot", "--host", stub.host(), "--token",
                "btc_x", "--environment", "DEVELOPMENT", "--workspace-id", "1");

            RoutedStubApi.Request request = stub.lastRequest("GET", CONNECTIONS_PATH)
                .orElseThrow();

            assertEquals(0, exitCode);
            assertEquals("workspaceId=1&componentName=hubspot", request.query());
            assertEquals("DEVELOPMENT", request.environment());
        }
    }

    @Test
    void testListRendersTheConnectionFieldsAsJson() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            command().workspaceConnectionList(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode connections = new ObjectMapper().readTree(stdout.toString(StandardCharsets.UTF_8));
            JsonNode aiProviderConnection = connections.get(1);

            assertEquals(2, connections.size());
            assertTrue(aiProviderConnection.get("aiProvider")
                .asBoolean());

            for (String fieldName : List.of(
                "id", "name", "componentName", "connectionVersion", "authorizationType", "credentialStatus",
                "aiProvider", "usable")) {

                assertTrue(connections.get(0)
                    .has(fieldName), fieldName);
            }

            assertFalse(connections.get(0)
                .has("parameters"));
        }
    }

    @Test
    void testListTableOutputShowsUsableAndAiProvider() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            command().workspaceConnectionList("hubspot", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("USABLE"), rendered);
            assertTrue(rendered.contains("AIPROVIDER"), rendered);
        }
    }

    @Test
    void testListRefusesProductionBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + CONNECTIONS_PATH, RoutedStubApi.response(200, CONNECTIONS)))) {

            AutomationConnectionCommand automationConnectionCommand = command();

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationConnectionCommand.workspaceConnectionList(null, null, stub.host(), "btc_x",
                    "PRODUCTION", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }
}
