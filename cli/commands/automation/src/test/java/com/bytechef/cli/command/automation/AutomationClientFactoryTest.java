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

import com.bytechef.cli.client.automationapp.ApiException;
import com.bytechef.cli.client.automationapp.api.AppApi;
import com.bytechef.cli.client.automationapp.api.AppWorkflowBindingApi;
import com.bytechef.cli.client.automationapp.api.WorkspaceConnectionApi;
import com.bytechef.cli.client.automationapp.model.AppBindableConnectionModel;
import com.bytechef.cli.client.automationapp.model.AppBindableWorkflowModel;
import com.bytechef.cli.client.automationapp.model.AppBindingInputModel;
import com.bytechef.cli.client.automationapp.model.AppComponentOperationsModel;
import com.bytechef.cli.client.automationapp.model.AppWorkspaceConnectionModel;
import com.bytechef.cli.core.config.CliConfig;
import com.bytechef.cli.core.config.Environment;
import com.bytechef.cli.core.error.CliException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The generated management client against the contract: paths, query parameters, headers, the new fields, and how a
 * server error reaches the user.
 *
 * @author Ivica Cardic
 */
class AutomationClientFactoryTest {

    private static CliConfig config(String host) {
        return new CliConfig(host, "btc_x", Environment.DEVELOPMENT, 1L);
    }

    @Test
    void testBindableConnectionsCarryTheComponentFilterAndTheUsableFlag() throws Exception {
        String body =
            """
                [{"id":12,"name":"HubSpot","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/42/bindable-connections", RoutedStubApi.response(200, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            List<AppBindableConnectionModel> connections = appApi.getAppBindableConnections(42L, "hubspot", null);

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/apps/42/bindable-connections")
                .orElseThrow();

            assertEquals("componentName=hubspot", request.query());
            assertEquals("Bearer btc_x", request.authorization());
            assertEquals("DEVELOPMENT", request.environment());
            assertEquals(1, connections.size());

            AppBindableConnectionModel connection = connections.getFirst();

            assertEquals(12L, connection.getId());
            assertEquals(3, connection.getComponentVersion());
            assertEquals(Boolean.TRUE, connection.getUsable());
        }
    }

    @Test
    void testComponentOperationsSendBothQueryParameters() throws Exception {
        String body = """
            {"actionNames":["createContact"],"toolNames":["searchContacts"]}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/component-operations", RoutedStubApi.response(200, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            AppComponentOperationsModel operations = appApi.getAppComponentOperations("hubspot", 3);

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/apps/component-operations")
                .orElseThrow();

            assertEquals("componentName=hubspot&componentVersion=3", request.query());
            assertEquals(List.of("createContact"), operations.getActionNames());
            assertEquals(List.of("searchContacts"), operations.getToolNames());
        }
    }

    @Test
    void testBindableWorkflowsAreReadByUuid() throws Exception {
        String body =
            """
                [{"projectDeploymentId":7,"projectDeploymentName":"Onboarding","projectName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer"}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET /api/automation/v1/apps/42/bindable-workflows", RoutedStubApi.response(200, body)))) {

            AppWorkflowBindingApi appWorkflowBindingApi = AutomationClientFactory.appWorkflowBindingApi(
                config(stub.host()));

            List<AppBindableWorkflowModel> workflows = appWorkflowBindingApi.getAppBindableWorkflows(42L, null);

            assertEquals("wf-uuid", workflows.getFirst()
                .getWorkflowUuid());
            assertEquals(7L, workflows.getFirst()
                .getProjectDeploymentId());
        }
    }

    @Test
    void testWorkspaceConnectionsCarryTheWorkspaceAndTheAiProviderFlag() throws Exception {
        String body =
            """
                [{"id":5,"name":"OpenAI","componentName":"openai","connectionVersion":1,"authorizationType":"API_KEY","credentialStatus":"VALID","aiProvider":true,"usable":true}]""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET /api/automation/v1/connections", RoutedStubApi.response(200, body)))) {

            WorkspaceConnectionApi workspaceConnectionApi = AutomationClientFactory.workspaceConnectionApi(
                config(stub.host()));

            List<AppWorkspaceConnectionModel> connections = workspaceConnectionApi.getWorkspaceConnections(
                1L, null, null);

            RoutedStubApi.Request request = stub.lastRequest("GET", "/api/automation/v1/connections")
                .orElseThrow();

            assertEquals("workspaceId=1", request.query());
            assertEquals(Boolean.TRUE, connections.getFirst()
                .getAiProvider());
        }
    }

    @Test
    void testAnErrorBodyIsCarriedIntoTheCliException() throws Exception {
        String body = """
            {"code":"APP_BINDING_KEY_ALREADY_EXISTS","message":"The binding key crm is already used."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("PUT /api/automation/v1/apps/42/connections/crm", RoutedStubApi.response(409, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));
            AppBindingInputModel appBindingInputModel = new AppBindingInputModel();

            appBindingInputModel.setConnectionId(12L);

            ApiException apiException = assertThrows(
                ApiException.class, () -> appApi.bindAppConnection(42L, "crm", appBindingInputModel, null));

            CliException cliException = AutomationClientFactory.toCliException(apiException);

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("HTTP 409"), cliException.getMessage());
            assertTrue(
                cliException.getMessage()
                    .contains("APP_BINDING_KEY_ALREADY_EXISTS: The binding key crm is already used"),
                cliException.getMessage());
        }
    }

    @Test
    void testForbiddenMapsToExitCode2WithANeutralHint() throws Exception {
        CliException cliException = AutomationClientFactory.toCliException(
            failureOf(403, """
                {"code":"ACCESS_DENIED","message":"Access Denied"}"""));

        assertEquals(2, cliException.exitCode());
        assertTrue(cliException.getMessage()
            .contains("ACCESS_DENIED"), cliException.getMessage());
        assertTrue(cliException.getMessage()
            .contains("Your role in DEVELOPMENT does not allow this; ask a workspace admin."),
            cliException.getMessage());
        assertFalse(cliException.getMessage()
            .contains("Managing apps"), cliException.getMessage());
    }

    @Test
    void testForbiddenCarriesTheHintTheCommandPasses() throws Exception {
        CliException cliException = AutomationClientFactory.toCliException(
            failureOf(403, """
                {"code":"ACCESS_DENIED","message":"Access Denied"}"""),
            AutomationClientFactory.MANAGE_APPS_FORBIDDEN_HINT);

        assertEquals(2, cliException.exitCode());
        assertTrue(cliException.getMessage()
            .contains("workspace admin role in DEVELOPMENT"), cliException.getMessage());
        assertFalse(cliException.getMessage()
            .contains("does not allow this"), cliException.getMessage());
    }

    @Test
    void testUnauthorizedMapsToExitCode2AndNamesTheDevelopmentKey() throws Exception {
        CliException cliException = AutomationClientFactory.toCliException(
            failureOf(401, """
                {"code":"UNAUTHORIZED","message":"Bad credentials"}"""));

        assertEquals(2, cliException.exitCode());
        assertTrue(cliException.getMessage()
            .startsWith("Authentication failed (HTTP 401): UNAUTHORIZED: Bad credentials."),
            cliException.getMessage());
        assertTrue(cliException.getMessage()
            .contains("--environment DEVELOPMENT"), cliException.getMessage());
    }

    @Test
    void testAServerErrorMapsToExitCode1WithItsDetail() throws Exception {
        CliException cliException = AutomationClientFactory.toCliException(
            failureOf(500, """
                {"code":"INTERNAL_ERROR","message":"Something broke."}"""));

        assertEquals(1, cliException.exitCode());
        assertEquals(
            "Request failed (HTTP 500): INTERNAL_ERROR: Something broke.", cliException.getMessage());
    }

    @Test
    void testAnEmptyOrNonJsonErrorBodyLeavesOnlyTheStatus() throws Exception {
        for (String body : new String[] {
            "", "<html><body>Bad Gateway</body></html>"
        }) {
            CliException cliException = AutomationClientFactory.toCliException(failureOf(502, body));

            assertEquals(1, cliException.exitCode());
            assertEquals("Request failed (HTTP 502).", cliException.getMessage());
        }
    }

    @Test
    void testAnErrorCodeWithoutAMessageRendersTheCodeAlone() throws Exception {
        CliException cliException = AutomationClientFactory.toCliException(
            failureOf(409, """
                {"code":"APP_BINDING_KEY_ALREADY_EXISTS"}"""));

        assertEquals("Request failed (HTTP 409): APP_BINDING_KEY_ALREADY_EXISTS.", cliException.getMessage());
    }

    @Test
    void testAnUnreachableHostMapsToExitCode1() throws Exception {
        String host;

        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            host = stub.host();
        }

        AppApi appApi = AutomationClientFactory.appApi(config(host));

        ApiException apiException = assertThrows(ApiException.class, () -> appApi.getApp(42L, null));

        CliException cliException = AutomationClientFactory.toCliException(apiException);

        assertEquals(1, cliException.exitCode());
        assertTrue(cliException.getMessage()
            .startsWith("Cannot reach ByteChef"), cliException.getMessage());
        assertTrue(cliException.getMessage()
            .contains("not a ByteChef server"), cliException.getMessage());
    }

    private static ApiException failureOf(int status, String body) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET /api/automation/v1/apps/42", RoutedStubApi.response(status, body)))) {

            AppApi appApi = AutomationClientFactory.appApi(config(stub.host()));

            return assertThrows(ApiException.class, () -> appApi.getApp(42L, null));
        }
    }
}
