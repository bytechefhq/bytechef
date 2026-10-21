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
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationAppCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String APP = """
        {"id":42,"uuid":"app-uuid","name":"my-app","workspaceId":1,"environment":"DEVELOPMENT","status":"ENABLED"}""";

    private static final String SECRET_KEY = "bca_secret_value_never_printed";

    private static final String CREATED = "{\"app\":" + APP + ",\"secretKey\":\"" + SECRET_KEY + "\"}";

    private static final String APPS_PATH = "/api/automation/v1/apps";

    private static final String BINDINGS =
        """
            [{"id":1,"appId":42,"bindingKey":"crm","connectionId":12,"connectionName":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"actionNames":["createContact"],"toolNames":["searchContacts"],"proxyEnabled":true,"proxyAllowedMethods":["GET"]}]""";

    private static final String BINDABLE_CONNECTIONS =
        """
            [{"id":12,"name":"HubSpot prod","componentName":"hubspot","connectionVersion":1,"componentVersion":3,"usable":true}]""";

    private static final String WORKFLOW_BINDINGS =
        """
            [{"id":3,"appId":42,"workflowKey":"onboard-customer","projectDeploymentId":7,"projectDeploymentName":"Onboarding","workflowUuid":"wf-uuid","workflowName":"Onboard customer","available":true}]""";

    private static final String ROTATED_KEY = "bca_rotated_value_never_printed";

    private static final String ROTATED = "{\"app\":" + APP + ",\"secretKey\":\"" + ROTATED_KEY + "\"}";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private AutomationAppCommand command(Path workingDirectory) {
        AutomationAppCommand automationAppCommand = new AutomationAppCommand();

        automationAppCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppCommand.setEnvironmentVariables(Map.of());
        automationAppCommand.setWorkingDirectory(workingDirectory);
        automationAppCommand.setOutput(
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));

        return automationAppCommand;
    }

    private String printed() {
        return stdout.toString(StandardCharsets.UTF_8) + stderr.toString(StandardCharsets.UTF_8);
    }

    private static Path linkedAppDirectory(Path appDirectory, String host) throws IOException {
        AppLinkFile.write(appDirectory, new AppLink(42, "app-uuid", 1, "DEVELOPMENT", host));

        return appDirectory;
    }

    private static final String API_COLLECTION_BINDINGS =
        """
            [{"id":5,"appId":42,"apiCollectionId":9,"name":"Orders API","contextPath":"orders","collectionVersion":2,"projectId":3,"projectName":"Orders","available":true}]""";

    private static Map<String, RoutedStubApi.Response> statusRoutes(int bindableStatus, String bindableBody) {
        return Map.of(
            "GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP),
            "GET " + APPS_PATH + "/42/connections", RoutedStubApi.response(200, BINDINGS),
            "GET " + APPS_PATH + "/42/bindable-connections", RoutedStubApi.response(bindableStatus, bindableBody),
            "GET " + APPS_PATH + "/42/workflows", RoutedStubApi.response(200, WORKFLOW_BINDINGS),
            "GET " + APPS_PATH + "/42/api-collections", RoutedStubApi.response(200, API_COLLECTION_BINDINGS));
    }

    @Test
    void testCreateWritesTheEnvFileAndAppJsonAndNeverPrintsTheKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            RoutedStubApi.Request request = stub.lastRequest("POST", APPS_PATH)
                .orElseThrow();
            JsonNode requestBody = OBJECT_MAPPER.readTree(request.body());

            assertEquals("workspaceId=1", request.query());
            assertEquals("DEVELOPMENT", request.environment());
            assertEquals("my-app", requestBody.get("name")
                .asText());
            assertEquals("VERCEL", requestBody.get("hostingTarget")
                .asText());
            assertEquals(
                "BYTECHEF_URL=" + stub.host() + "\nBYTECHEF_APP_KEY=" + SECRET_KEY + "\n",
                Files.readString(tempDir.resolve(".env.local")));
            assertEquals(new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()), AppLinkFile.read(tempDir));
            assertFalse(printed().contains(SECRET_KEY), printed());

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals(42, result.get("id")
                .asLong());
            assertFalse(result.has("secretKey"));
        }
    }

    @Test
    void testCreateRefusesWhenAParentDirectoryAlreadyHoldsAnApp() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(nestedDirectory).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("app.json"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateIsRefusedOutsideDevelopmentBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("DEVELOPMENT only"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testDevelopmentIsRequiredWhenTheEnvironmentComesFromTheEnvironmentVariable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            AutomationAppCommand automationAppCommand = command(tempDir);

            automationAppCommand.setEnvironmentVariables(Map.of("BYTECHEF_ENVIRONMENT", "STAGING"));

            CliException cliException = assertThrows(
                CliException.class, () -> automationAppCommand.appList(null, stub.host(), "btc_x", null, 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("STAGING"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateReportsTheAppIdWhenAppJsonCannotBeWritten() throws Exception {
        Files.writeString(tempDir.resolve(".bytechef"), "a file where the directory should be");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("Created app 42"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app link --app-id 42"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(SECRET_KEY));
            assertFalse(printed().contains(SECRET_KEY));
        }
    }

    @Test
    void testCreateReportsKeyRotateWhenTheEnvFileCannotBeWritten() throws Exception {
        String unwritableKey = "{\"app\":" + APP + ",\"secretKey\":\"bca_split\\nkey\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, unwritableKey)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app key rotate"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains("bca_split"));
            assertEquals(42, AppLinkFile.read(tempDir)
                .id());
        }
    }

    @Test
    void testCreateRefusesAnEnvFileInAMissingDirectoryBeforeCreatingTheApp() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", "missing/.env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWithoutAWorkspaceExitsWithCode4() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", null, "json"));

            assertEquals(4, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateRefusesAnUnknownHostingTargetBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "NETLIFY", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("VERCEL or SELF_HOSTED"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWarnsWhenTheEnvFileIsNotGitIgnored() throws Exception {
        Path repository = GitTestRepository.create(tempDir.resolve("repository"), "!.env.local\n");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(repository).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(stderr.toString(StandardCharsets.UTF_8)
                .contains("is not git-ignored"), printed());
        }
    }

    @Test
    void testCreateIgnoresTheCliConfigDirectoryAbove() throws Exception {
        Path homeDirectory = tempDir.resolve("home");

        Files.createDirectories(homeDirectory.resolve(".bytechef"));
        Files.writeString(homeDirectory.resolve(".bytechef/config"), "[default]\n");

        Path projectDirectory = Files.createDirectories(homeDirectory.resolve("projects/my-app"));

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(projectDirectory).create(
                "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(42, AppLinkFile.read(projectDirectory)
                .id());
            assertTrue(Files.isRegularFile(projectDirectory.resolve(".env.local")));
        }
    }

    @Test
    void testLinkWritesAppJsonWithoutAKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP)))) {

            command(tempDir).link(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(new AppLink(42, "app-uuid", 1, "DEVELOPMENT", stub.host()), AppLinkFile.read(tempDir));
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
        }
    }

    @Test
    void testLinkRefusesAnAppOfAnotherEnvironment() throws Exception {
        String productionApp = APP.replace("DEVELOPMENT", "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, productionApp)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).link(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertFalse(Files.exists(AppLinkFile.pathIn(tempDir)));
        }
    }

    @Test
    void testListSendsTheWorkspaceAndTheDevelopmentHeader() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[" + APP + "]")))) {

            command(tempDir).appList(null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            RoutedStubApi.Request request = stub.lastRequest("GET", APPS_PATH)
                .orElseThrow();

            assertEquals("workspaceId=1", request.query());
            assertEquals("DEVELOPMENT", request.environment());
            assertTrue(stdout.toString(StandardCharsets.UTF_8)
                .contains("my-app"));
        }
    }

    @Test
    void testListIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "DEVELOPMENT", "--workspace-id", "1");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("GET", APPS_PATH)
                .isPresent());
        }
    }

    @Test
    void testListRefusesProductionThroughTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH, RoutedStubApi.response(200, "[]")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION", "--workspace-id", "1");

            assertEquals(1, exitCode);
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testStatusFindsTheAppByWalkingUpAndRendersJson() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/routes"));

            command(nestedDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));
            JsonNode connection = result.get("connections")
                .get(0);
            JsonNode workflow = result.get("workflows")
                .get(0);

            assertEquals(42, result.get("app")
                .get("id")
                .asLong());
            assertEquals("crm", connection.get("bindingKey")
                .asText());
            assertEquals(3, connection.get("componentVersion")
                .asInt());
            assertEquals("createContact", connection.get("actions")
                .get(0)
                .asText());
            assertEquals("onboard-customer", workflow.get("workflowKey")
                .asText());
            assertTrue(workflow.get("available")
                .asBoolean());
        }
    }

    @Test
    void testStatusShowsApiCollectionsAndEndpoints() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode apiCollection = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8))
                .get("apiCollections")
                .get(0);

            assertEquals(9, apiCollection.get("apiCollectionId")
                .asLong());
            assertEquals("Orders API", apiCollection.get("name")
                .asText());
            assertEquals("v2/orders", apiCollection.get("path")
                .asText());
            assertTrue(apiCollection.get("available")
                .asBoolean());

            stdout.reset();

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("API collections"), rendered);
            assertTrue(rendered.contains("v2/orders"), rendered);
        }
    }

    @Test
    void testStatusShowsNoComponentVersionWhenTheLookupIsForbidden() throws Exception {
        String forbidden = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(403, forbidden))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertTrue(result.get("connections")
                .get(0)
                .get("componentVersion")
                .isNull());
        }
    }

    @Test
    void testStatusTableOutputShowsBothBindingKinds() throws Exception {
        String forbidden = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(403, forbidden))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("BINDING KEY"), rendered);
            assertTrue(rendered.contains("crm"), rendered);
            assertTrue(rendered.contains("none"), rendered);
            assertTrue(rendered.contains("onboard-customer"), rendered);
        }
    }

    @Test
    void testStatusRefusesAnAppJsonWrittenForAnotherHost() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), "http://other.example");

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("another ByteChef target"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testStatusWithoutAppJsonOrAppIdSaysWhatToDo() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).status(null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--app-id"), cliException.getMessage());
        }
    }

    @Test
    void testLinkRefusesWhenAppJsonAlreadyExistsBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP)))) {

            linkedAppDirectory(tempDir, stub.host());

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).link(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("app.json"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateRefusesASymbolicLinkEnvFileBeforeCreatingTheApp() throws Exception {
        Path realFile = Files.writeString(tempDir.resolve("real.env"), "OTHER=1\n");

        Files.createSymbolicLink(tempDir.resolve(".env.local"), realFile);

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("symbolic link"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
            assertEquals("OTHER=1\n", Files.readString(realFile));
        }
    }

    @Test
    void testCreateReportsAnUnusableKeyWithoutEchoingIt() throws Exception {
        String badKey = "bca_bad\\nkey";
        String created = "{\"app\":" + APP + ",\"secretKey\":\"" + badKey + "\"}";

        try (RoutedStubApi stub =
            RoutedStubApi.start(Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, created)))) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("Created app 42"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app key rotate"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains("bca_bad"));
            assertFalse(printed().contains("bca_bad"));
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
        }
    }

    @Test
    void testKeyRotateRewritesTheEnvFileInTheAppDirectoryAndNeverPrintsTheKey() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path nestedDirectory = Files.createDirectories(appDirectory.resolve("src/routes"));

            Files.writeString(appDirectory.resolve(".env.local"), "OTHER=1\nBYTECHEF_APP_KEY=old\n");

            command(nestedDirectory).keyRotate(
                null, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals(
                "OTHER=1\nBYTECHEF_APP_KEY=" + ROTATED_KEY + "\nBYTECHEF_URL=" + stub.host() + "\n",
                Files.readString(appDirectory.resolve(".env.local")));
            assertFalse(Files.exists(nestedDirectory.resolve(".env.local")));
            assertFalse(printed().contains(ROTATED_KEY), printed());

            if (appDirectory.getFileSystem()
                .supportedFileAttributeViews()
                .contains("posix")) {

                assertEquals(
                    PosixFilePermissions.fromString("rw-------"),
                    Files.getPosixFilePermissions(appDirectory.resolve(".env.local")));
            }
        }
    }

    @Test
    void testKeyRotateWriteFailureNamesTheNextStepWithoutTheKey() throws Exception {
        String unwritableKey = "{\"app\":" + APP + ",\"secretKey\":\"bca_split\\nkey\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, unwritableKey)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    null, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app key rotate"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("may already be rotated"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains("bca_split"), cliException.getMessage());
            assertFalse(printed().contains("bca_split"), printed());
        }
    }

    @Test
    void testKeyRotateWithoutAKeyInTheResponseNamesTheNextStep() throws Exception {
        for (String body : new String[] {
            "{\"app\":" + APP + "}", ""
        }) {
            try (RoutedStubApi stub = RoutedStubApi.start(
                Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, body)))) {

                Path appDirectory = linkedAppDirectory(tempDir.resolve("app" + body.length()), stub.host());

                CliException cliException = assertThrows(
                    CliException.class,
                    () -> command(appDirectory).keyRotate(
                        null, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

                assertEquals(1, cliException.exitCode());
                assertTrue(cliException.getMessage()
                    .contains("bytechef automation app key rotate"), cliException.getMessage());
                assertTrue(cliException.getMessage()
                    .contains("may already be rotated"), cliException.getMessage());
                assertFalse(Files.exists(appDirectory.resolve(".env.local")));
            }
        }
    }

    @Test
    void testKeyRotateRefusesADirectoryEnvFileBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            Files.createDirectories(appDirectory.resolve("env-dir/inside"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    null, "env-dir", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("is a directory"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateRefusesADirectoryEnvFileBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            Files.createDirectories(tempDir.resolve("env-dir"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", "env-dir", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testKeyRotateAndCreateRefuseAnUnwritableEnvFileBeforeAnyRequest() throws Exception {
        Path readOnlyDirectory = Files.createDirectories(tempDir.resolve("app/read-only"));
        Path unwritableFile = Files.writeString(tempDir.resolve("app/locked.env"), "BYTECHEF_APP_KEY=old\n");

        Assumptions.assumeTrue(
            readOnlyDirectory.getFileSystem()
                .supportedFileAttributeViews()
                .contains("posix"));

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED),
                "POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            Files.setPosixFilePermissions(readOnlyDirectory, PosixFilePermissions.fromString("r-x------"));
            Files.setPosixFilePermissions(unwritableFile, PosixFilePermissions.fromString("r--------"));

            try {
                Assumptions.assumeFalse(Files.isWritable(readOnlyDirectory));

                CliException directoryFailure = assertThrows(
                    CliException.class,
                    () -> command(appDirectory).keyRotate(
                        null, "read-only/.env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));
                CliException fileFailure = assertThrows(
                    CliException.class,
                    () -> command(appDirectory).keyRotate(
                        null, "locked.env", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));
                CliException createFailure = assertThrows(
                    CliException.class,
                    () -> command(tempDir.resolve("fresh")).create(
                        "my-app", null, "VERCEL", "../app/read-only/.env.local", null, stub.host(), "btc_x",
                        "DEVELOPMENT", 1L, "json"));

                assertTrue(directoryFailure.getMessage()
                    .contains("cannot be written"), directoryFailure.getMessage());
                assertTrue(fileFailure.getMessage()
                    .contains("cannot be written"), fileFailure.getMessage());
                assertEquals(1, createFailure.exitCode());
                assertTrue(stub.requests()
                    .isEmpty());
            } finally {
                Files.setPosixFilePermissions(readOnlyDirectory, PosixFilePermissions.fromString("rwx------"));
                Files.setPosixFilePermissions(unwritableFile, PosixFilePermissions.fromString("rw-------"));
            }
        }
    }

    @Test
    void testKeyRotateIsRefusedOutsideDevelopmentBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    null, ".env.local", null, stub.host(), "btc_x", "PRODUCTION", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testKeyRotateRefusesAProductionAppIdBeforeRotating() throws Exception {
        String productionApp = APP.replace("DEVELOPMENT", "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APPS_PATH + "/42", RoutedStubApi.response(200, productionApp),
                "POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path workingDirectory = Files.createDirectories(tempDir.resolve("unlinked"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(workingDirectory).keyRotate(
                    42L, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("PRODUCTION"), cliException.getMessage());
            assertTrue(stub.lastRequest("POST", APPS_PATH + "/42/secret-key")
                .isEmpty());
            assertEquals(List.of("GET"), stub.requests()
                .stream()
                .map(RoutedStubApi.Request::method)
                .toList());
            assertFalse(Files.exists(workingDirectory.resolve(".env.local")));
        }
    }

    @Test
    void testKeyRotateRefusesAnAppIdOfAnotherWorkspaceBeforeRotating() throws Exception {
        String otherWorkspaceApp = APP.replace("\"workspaceId\":1", "\"workspaceId\":2");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APPS_PATH + "/42", RoutedStubApi.response(200, otherWorkspaceApp),
                "POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path workingDirectory = Files.createDirectories(tempDir.resolve("unlinked"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(workingDirectory).keyRotate(
                    42L, ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertTrue(cliException.getMessage()
                .contains("workspace 2"), cliException.getMessage());
            assertTrue(stub.lastRequest("POST", APPS_PATH + "/42/secret-key")
                .isEmpty());
        }
    }

    @Test
    void testStatusRefusesAnAppIdOfAnotherWorkspaceAfterFetchingOnlyTheApp() throws Exception {
        String otherWorkspaceApp = APP.replace("\"workspaceId\":1", "\"workspaceId\":2");

        Map<String, RoutedStubApi.Response> routes = new HashMap<>(statusRoutes(200, BINDABLE_CONNECTIONS));

        routes.put("GET " + APPS_PATH + "/42", RoutedStubApi.response(200, otherWorkspaceApp));

        try (RoutedStubApi stub = RoutedStubApi.start(routes)) {
            Path workingDirectory = Files.createDirectories(tempDir.resolve("unlinked"));

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(workingDirectory).status(42L, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("workspace 2"), cliException.getMessage());
            assertEquals(List.of("GET " + APPS_PATH + "/42"), stub.requests()
                .stream()
                .map(request -> request.method() + " " + request.path())
                .toList());
        }
    }

    @Test
    void testKeyRotateOfAnUnlinkedDevelopmentAppIdWritesTheEnvFileInTheWorkingDirectory() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APPS_PATH + "/42", RoutedStubApi.response(200, APP),
                "POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path workingDirectory = Files.createDirectories(tempDir.resolve("unlinked"));

            command(workingDirectory).keyRotate(42L, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertTrue(Files.readString(workingDirectory.resolve(".env.local"))
                .contains("BYTECHEF_APP_KEY=" + ROTATED_KEY));
            assertFalse(printed().contains(ROTATED_KEY), printed());
        }
    }

    @Test
    void testKeyRotateRefusesAnAppIdThatDiffersFromTheLinkedAppWithoutAnEnvFile() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APPS_PATH + "/7", RoutedStubApi.response(200, APP.replace("\"id\":42", "\"id\":7")),
                "POST " + APPS_PATH + "/7/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            Files.writeString(appDirectory.resolve(".env.local"), "BYTECHEF_APP_KEY=app-42-key\n");

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    7L, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--env-file"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
            assertEquals("BYTECHEF_APP_KEY=app-42-key\n", Files.readString(appDirectory.resolve(".env.local")));
        }
    }

    @Test
    void testKeyRotateOfAnotherAppIdInALinkedDirectoryUsesTheExplicitEnvFile() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APPS_PATH + "/7", RoutedStubApi.response(200, APP.replace("\"id\":42", "\"id\":7")),
                "POST " + APPS_PATH + "/7/secret-key", RoutedStubApi.response(200, ROTATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            Files.writeString(appDirectory.resolve(".env.local"), "BYTECHEF_APP_KEY=app-42-key\n");

            command(appDirectory).keyRotate(7L, "other.env", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertEquals("BYTECHEF_APP_KEY=app-42-key\n", Files.readString(appDirectory.resolve(".env.local")));
            assertTrue(Files.readString(appDirectory.resolve("other.env"))
                .contains("BYTECHEF_APP_KEY=" + ROTATED_KEY));
        }
    }

    @Test
    void testCreateLeavesTheEnvFileAloneWhenAnotherRunLinksTheDirectoryFirst() throws Exception {
        AppLink otherAppLink = new AppLink(7, "other-uuid", 1, "DEVELOPMENT", "http://localhost:9555");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, CREATED)),
            request -> {
                try {
                    AppLinkFile.write(tempDir, otherAppLink);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            })) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("Created app 42"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("another app"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains("link --app-id 42' in " + tempDir), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(SECRET_KEY));
            assertFalse(printed().contains(SECRET_KEY));
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
            assertEquals(otherAppLink, AppLinkFile.read(tempDir));
        }
    }

    @Test
    void testCreateWithAnUnreadableReplySaysTheAppMayExistWithoutTheKey() throws Exception {
        String unreadable = "{\"app\":{\"id\":\"" + SECRET_KEY + "\"},\"secretKey\":\"" + SECRET_KEY + "\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(201, unreadable)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("bytechef automation app list"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("automation app link --app-id <id>"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(SECRET_KEY), cliException.getMessage());
            assertFalse(printed().contains(SECRET_KEY));
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
            assertFalse(Files.exists(AppLinkFile.pathIn(tempDir)));
        }
    }

    @Test
    void testKeyRotateWithAnUnreadableReplySaysTheKeyMayBeRotatedWithoutTheKey() throws Exception {
        String unreadable = "{\"app\":{\"id\":\"" + ROTATED_KEY + "\"},\"secretKey\":\"" + ROTATED_KEY + "\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH + "/42/secret-key", RoutedStubApi.response(200, unreadable)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory).keyRotate(
                    null, null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("may already be rotated"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(ROTATED_KEY), cliException.getMessage());
            assertFalse(printed().contains(ROTATED_KEY));
            assertFalse(Files.exists(appDirectory.resolve(".env.local")));
        }
    }

    @Test
    void testCreateSurfacesAServerErrorAndWritesNothing() throws Exception {
        String serverError = """
            {"code":"INTERNAL_ERROR","message":"Something broke."}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(500, serverError)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("HTTP 500"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("INTERNAL_ERROR"), cliException.getMessage());
            assertFalse(Files.exists(tempDir.resolve(".env.local")));
            assertFalse(Files.exists(AppLinkFile.pathIn(tempDir)));
        }
    }

    @Test
    void testCreateForbiddenNamesTheAdminRole() throws Exception {
        String forbidden = """
            {"code":"ACCESS_DENIED","message":"Access Denied"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + APPS_PATH, RoutedStubApi.response(403, forbidden)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).create(
                    "my-app", null, "VERCEL", ".env.local", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            assertEquals(2, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("workspace admin role in DEVELOPMENT"), cliException.getMessage());
        }
    }

    @Test
    void testStatusNamesAMalformedAppJsonOnce() throws Exception {
        Path appDirectory = Files.createDirectories(tempDir.resolve("app/.bytechef"));

        Files.writeString(appDirectory.resolve("app.json"), "{not json");

        try (RoutedStubApi stub = RoutedStubApi.start(statusRoutes(200, BINDABLE_CONNECTIONS))) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(appDirectory.getParent()).status(
                    null, null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json"));

            String message = cliException.getMessage();

            assertTrue(message.startsWith("Cannot read "), message);
            assertEquals(message.indexOf("Cannot read"), message.lastIndexOf("Cannot read"), message);
            assertTrue(stub.requests()
                .isEmpty());
        }
    }
}
