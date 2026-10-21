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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationAppBindingCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String BINDABLE_PATH = "/api/automation/v1/apps/42/bindable-connections";
    private static final String OPERATIONS_PATH = "/api/automation/v1/apps/component-operations";
    private static final String BINDING_PATH = "/api/automation/v1/apps/42/connections/crm";
    private static final String WORKFLOW_BINDING_PATH = "/api/automation/v1/apps/42/workflows/onboard-customer";
    private static final String BINDABLE_WORKFLOWS_PATH = "/api/automation/v1/apps/42/bindable-workflows";
    private static final String API_COLLECTION_BINDING_PATH = "/api/automation/v1/apps/42/api-collections/9";
    private static final String BINDABLE_API_COLLECTIONS_PATH = "/api/automation/v1/apps/42/bindable-api-collections";

    private static final String API_COLLECTION_BINDING =
        """
            {"id":5,"appId":42,"apiCollectionId":9,"name":"Orders API","contextPath":"orders","collectionVersion":2,"projectId":3,"projectName":"Orders","available":true}""";

    private static final String ONE_USABLE =
        """
            [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true},
             {"id":13,"name":"HubSpot old","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":false}]""";

    private static final String TWO_USABLE =
        """
            [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true},
             {"id":13,"name":"HubSpot test","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

    private static final String OPERATIONS = """
        {"actionNames":["createContact","getContact"],"toolNames":["searchContacts"]}""";

    private static final String BINDING =
        """
            {"id":1,"appId":42,"bindingKey":"crm","connectionId":12,"connectionName":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"actionNames":["createContact"],"toolNames":[],"proxyEnabled":false,"proxyAllowedMethods":[]}""";

    private static final String APP_PATH = "/api/automation/v1/apps/42";

    private static final String DEVELOPMENT_APP = """
        {"id":42,"uuid":"app-uuid","name":"my-app","workspaceId":1,"environment":"DEVELOPMENT","status":"ENABLED"}""";

    private static final String WORKFLOW_BINDING =
        """
            {"id":3,"appId":42,"workflowKey":"onboard-customer","projectDeploymentId":7,"workflowUuid":"wf-uuid","available":true}""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    private AutomationAppBindingCommand command(RoutedStubApi stub) throws IOException {
        Path appDirectory = tempDir.resolve("app");

        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()));

        AutomationAppBindingCommand automationAppBindingCommand = new AutomationAppBindingCommand();

        automationAppBindingCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppBindingCommand.setEnvironmentVariables(Map.of());
        automationAppBindingCommand.setWorkingDirectory(appDirectory);
        automationAppBindingCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationAppBindingCommand;
    }

    private AutomationAppBindingCommand unlinkedCommand() throws IOException {
        AutomationAppBindingCommand automationAppBindingCommand = new AutomationAppBindingCommand();

        automationAppBindingCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppBindingCommand.setEnvironmentVariables(Map.of());
        automationAppBindingCommand.setWorkingDirectory(Files.createDirectories(tempDir.resolve("unlinked")));
        automationAppBindingCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationAppBindingCommand;
    }

    private static List<RoutedStubApi.Request> mutatingRequests(RoutedStubApi stub) {
        return stub.requests()
            .stream()
            .filter(request -> !"GET".equals(request.method()))
            .toList();
    }

    private static void bindConnection(
        AutomationAppBindingCommand automationAppBindingCommand, RoutedStubApi stub, String componentName,
        Long connectionId, String actions, String tools, String proxy) {

        automationAppBindingCommand.bindConnection(
            "crm", componentName, connectionId, actions, tools, proxy, null, null, stub.host(), "btc_x",
            "DEVELOPMENT", 1L, "json");
    }

    private static List<String> strings(JsonNode arrayNode) {
        List<String> values = new ArrayList<>();

        for (JsonNode valueNode : arrayNode) {
            values.add(valueNode.asText());
        }

        return values;
    }

    private static JsonNode lastBody(RoutedStubApi stub, String method, String path) throws IOException {
        RoutedStubApi.Request request = stub.lastRequest(method, path)
            .orElseThrow();

        return OBJECT_MAPPER.readTree(request.body());
    }

    @Test
    void testBindConnectionPicksTheOnlyUsableConnectionAndSendsAFullReplaceBody() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, "hubspot", null, "createContact", null, null);

            assertEquals(
                "componentName=hubspot", stub.lastRequest("GET", BINDABLE_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(
                "componentName=hubspot&componentVersion=3", stub.lastRequest("GET", OPERATIONS_PATH)
                    .orElseThrow()
                    .query());

            JsonNode body = lastBody(stub, "PUT", BINDING_PATH);

            assertEquals(12, body.get("connectionId")
                .asLong());
            assertEquals(List.of("createContact"), strings(body.get("actionNames")));
            assertEquals(List.of(), strings(body.get("toolNames")));
            assertFalse(body.get("proxyEnabled")
                .asBoolean());
            assertEquals(List.of(), strings(body.get("proxyAllowedMethods")));
        }
    }

    @Test
    void testBindConnectionListsTheCandidatesWhenSeveralAreUsable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, TWO_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, "hubspot", null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--connection-id"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("12 (HubSpot prod"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("13 (HubSpot test"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionListsTheAllowedNamesForAnUnknownAction() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(
                    automationAppBindingCommand, stub, "hubspot", null, "createContact,deleteEverything", null, null));

            assertTrue(cliException.getMessage()
                .contains("deleteEverything"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("createContact, getContact"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("searchContacts"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionUsesAnExplicitConnectionIdAndItsComponent() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, TWO_USABLE),
                "GET " + OPERATIONS_PATH, RoutedStubApi.response(200, OPERATIONS),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, null, 13L, "getContact", null, null);

            assertNull(
                stub.lastRequest("GET", BINDABLE_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(
                "componentName=hubspot&componentVersion=3", stub.lastRequest("GET", OPERATIONS_PATH)
                    .orElseThrow()
                    .query());
            assertEquals(13, lastBody(stub, "PUT", BINDING_PATH).get("connectionId")
                .asLong());
        }
    }

    @Test
    void testBindConnectionRefusesAnExplicitConnectionThatIsNotUsable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, null, 13L, null, null, null));

            assertTrue(cliException.getMessage()
                .contains("not usable"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionNeedsAComponentOrAConnectionBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, null, null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionUppercasesProxyMethodsAndEnablesTheProxy() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            bindConnection(command(stub), stub, "hubspot", null, null, null, "get,post");

            JsonNode body = lastBody(stub, "PUT", BINDING_PATH);

            assertTrue(body.get("proxyEnabled")
                .asBoolean());
            assertEquals(List.of("GET", "POST"), strings(body.get("proxyAllowedMethods")));
            assertEquals(List.of(), strings(body.get("actionNames")));
            assertTrue(stub.lastRequest("GET", OPERATIONS_PATH)
                .isEmpty());
        }
    }

    @Test
    void testBindConnectionSurfacesTheServerErrorCode() throws Exception {
        String conflict = """
            {"code":"APP_BINDING_KEY_ALREADY_EXISTS","message":"The binding key crm is already used."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(409, conflict)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, "hubspot", null, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("APP_BINDING_KEY_ALREADY_EXISTS"), cliException.getMessage());
        }
    }

    @Test
    void testBindWorkflowPutsTheDeploymentAndTheUuid() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(200, WORKFLOW_BINDING)))) {

            command(stub).bindWorkflow(
                "onboard-customer", 7L, "wf-uuid", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode body = lastBody(stub, "PUT", WORKFLOW_BINDING_PATH);

            assertEquals(7, body.get("projectDeploymentId")
                .asLong());
            assertEquals("wf-uuid", body.get("workflowUuid")
                .asText());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("onboard-customer"));
        }
    }

    @Test
    void testBindWorkflowListsTheBindableWorkflowsWhenTheServerRefuses() throws Exception {
        String notBindable = """
            {"code":"APP_WORKFLOW_NOT_BINDABLE","message":"Workflow wf-x of deployment 7 cannot be bound."}""";
        String bindableWorkflows =
            """
                [{"projectDeploymentId":7,"projectDeploymentName":"Onboarding","projectName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer"}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "PUT " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(400, notBindable),
                "GET " + BINDABLE_WORKFLOWS_PATH, RoutedStubApi.response(200, bindableWorkflows)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.bindWorkflow(
                    "onboard-customer", 7L, "wf-x", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("APP_WORKFLOW_NOT_BINDABLE"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("--deployment-id 7 --workflow-uuid wf-uuid"), cliException.getMessage());
        }
    }

    @Test
    void testUnbindSendsDeletesForBothBindingKinds() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, ""),
                "DELETE " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(204, "")))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            automationAppBindingCommand.unbindConnection(
                "crm", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");
            automationAppBindingCommand.unbindWorkflow(
                "onboard-customer", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stub.lastRequest("DELETE", BINDING_PATH)
                .isPresent());
            assertTrue(stub.lastRequest("DELETE", WORKFLOW_BINDING_PATH)
                .isPresent());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("\"removed\" : true"));
        }
    }

    @Test
    void testUnbindConnectionIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, DEVELOPMENT_APP),
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, "")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "unbind", "connection", "--binding-key", "crm", "--app-id", "42", "--host",
                stub.host(), "--token", "btc_x", "--environment", "DEVELOPMENT");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("DELETE", BINDING_PATH)
                .isPresent());
        }
    }

    @Test
    void testBindConnectionRefusesAnExplicitConnectionThatIsNotACandidate() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, TWO_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, null, 99L, null, null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("Connection 99 cannot be bound"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("12 (HubSpot prod"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("13 (HubSpot test"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }

    @Test
    void testUnbindOfAMissingBindingExitsWithCode3() throws Exception {
        String notFound = """
            {"code":"APP_BINDING_NOT_FOUND","message":"No binding crm."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "DELETE " + BINDING_PATH, RoutedStubApi.response(404, notFound),
                "DELETE " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(404, notFound)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException connectionFailure = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.unbindConnection(
                    "crm", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));
            CliException workflowFailure = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.unbindWorkflow(
                    "onboard-customer", null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(3, connectionFailure.exitCode());
            assertEquals(3, workflowFailure.exitCode());
        }
    }

    @Test
    void testBindApiCollectionPutsBinding() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT " + API_COLLECTION_BINDING_PATH, RoutedStubApi.response(200, API_COLLECTION_BINDING)))) {

            command(stub).bindApiCollection(9L, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stub.lastRequest("PUT", API_COLLECTION_BINDING_PATH)
                .isPresent());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("Orders API"));
        }
    }

    @Test
    void testBindApiCollectionWithoutIdListsBindable() throws Exception {
        String bindable =
            """
                [{"apiCollectionId":9,"name":"Orders API","contextPath":"orders","collectionVersion":2,"projectId":3,"projectName":"Orders","available":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "PUT " + API_COLLECTION_BINDING_PATH, RoutedStubApi.response(200, API_COLLECTION_BINDING),
                "GET " + BINDABLE_API_COLLECTIONS_PATH, RoutedStubApi.response(200, bindable)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.bindApiCollection(
                    null, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--api-collection-id 9"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("Orders API"), cliException.getMessage());
            assertTrue(mutatingRequests(stub).isEmpty());
        }
    }

    @Test
    void testUnbindApiCollection() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("DELETE " + API_COLLECTION_BINDING_PATH, RoutedStubApi.response(204, "")))) {

            command(stub).unbindApiCollection(9L, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stub.lastRequest("DELETE", API_COLLECTION_BINDING_PATH)
                .isPresent());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("\"removed\""));
        }
    }

    @Test
    void testBindApiCollectionRefusesNonDevelopment() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.bindApiCollection(
                    9L, null, null, stub.host(), "btc_x", "STAGING", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("DEVELOPMENT only"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testEveryBindingCommandIsRefusedOutsideDevelopmentBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            List<Runnable> invocations = List.of(
                () -> automationAppBindingCommand.bindConnection(
                    "crm", "hubspot", null, null, null, null, null, null, stub.host(), "btc_x", "PRODUCTION", 1L,
                    "json"),
                () -> automationAppBindingCommand.bindWorkflow(
                    "onboard-customer", 7L, "wf-uuid", null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"),
                () -> automationAppBindingCommand.unbindConnection(
                    "crm", null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"),
                () -> automationAppBindingCommand.unbindWorkflow(
                    "onboard-customer", null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"),
                () -> automationAppBindingCommand.bindApiCollection(
                    9L, null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"),
                () -> automationAppBindingCommand.unbindApiCollection(
                    9L, null, null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"));

            for (Runnable invocation : invocations) {
                CliException cliException = assertThrows(CliException.class, invocation::run);

                assertEquals(1, cliException.exitCode());
                assertTrue(cliException.getMessage()
                    .contains("DEVELOPMENT only"), cliException.getMessage());
            }

            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testEveryBindingCommandRefusesAProductionAppIdBeforeAnyChange() throws Exception {
        String productionApp = DEVELOPMENT_APP.replace("DEVELOPMENT", "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, productionApp),
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, ONE_USABLE),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING),
                "PUT " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(200, WORKFLOW_BINDING),
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, ""),
                "DELETE " + WORKFLOW_BINDING_PATH, RoutedStubApi.response(204, "")))) {

            AutomationAppBindingCommand automationAppBindingCommand = unlinkedCommand();

            List<Runnable> invocations = List.of(
                () -> automationAppBindingCommand.bindConnection(
                    "crm", "hubspot", null, null, null, null, 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"),
                () -> automationAppBindingCommand.bindWorkflow(
                    "onboard-customer", 7L, "wf-uuid", 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"),
                () -> automationAppBindingCommand.unbindConnection(
                    "crm", 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"),
                () -> automationAppBindingCommand.unbindWorkflow(
                    "onboard-customer", 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            for (Runnable invocation : invocations) {
                CliException cliException = assertThrows(CliException.class, invocation::run);

                assertEquals(1, cliException.exitCode());
                assertTrue(cliException.getMessage()
                    .contains("PRODUCTION"), cliException.getMessage());
            }

            assertEquals(List.of(), mutatingRequests(stub));
        }
    }

    @Test
    void testABindingCommandRefusesAnAppIdOfAnotherWorkspaceBeforeAnyChange() throws Exception {
        String otherWorkspaceApp = DEVELOPMENT_APP.replace("\"workspaceId\":1", "\"workspaceId\":2");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, otherWorkspaceApp),
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, "")))) {

            AutomationAppBindingCommand automationAppBindingCommand = unlinkedCommand();

            CliException cliException = assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.unbindConnection(
                    "crm", 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("workspace 2"), cliException.getMessage());
            assertEquals(List.of(), mutatingRequests(stub));
        }
    }

    @Test
    void testABindingCommandRefusesAnAppIdThatDiffersFromTheLinkedProductionApp() throws Exception {
        String productionApp = DEVELOPMENT_APP.replace("DEVELOPMENT", "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, productionApp),
                "DELETE " + BINDING_PATH, RoutedStubApi.response(204, "")))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            AppLinkFile.write(
                tempDir.resolve("other"), new AppLink(7, "other-uuid", 1, "DEVELOPMENT", stub.host()));
            automationAppBindingCommand.setWorkingDirectory(tempDir.resolve("other"));

            assertThrows(
                CliException.class,
                () -> automationAppBindingCommand.unbindConnection(
                    "crm", 42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(List.of(), mutatingRequests(stub));
        }
    }

    @Test
    void testBindConnectionSaysTheNamesCannotBeCheckedWithoutAComponentVersion() throws Exception {
        String noVersion = """
            [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"usable":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + BINDABLE_PATH, RoutedStubApi.response(200, noVersion),
                "PUT " + BINDING_PATH, RoutedStubApi.response(200, BINDING)))) {

            AutomationAppBindingCommand automationAppBindingCommand = command(stub);

            CliException cliException = assertThrows(
                CliException.class,
                () -> bindConnection(automationAppBindingCommand, stub, "hubspot", null, "createContact", null, null));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("component version of connection 12 (HubSpot prod) is unknown"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains("empty response"), cliException.getMessage());
            assertTrue(stub.lastRequest("PUT", BINDING_PATH)
                .isEmpty());
        }
    }
}
