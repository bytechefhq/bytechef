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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class AutomationAppInstanceCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String INSTANCES_PATH = "/api/automation/v1/apps/42/instances";

    private static final String SECRET_KEY = "bca_partner_secret_never_printed";

    private static final String CREATED_INSTANCE =
        """
            {"id":8,"uuid":"partner-uuid","appId":42,"appName":"my-app","workspaceId":2,"environment":"DEVELOPMENT","name":"Partner","status":"ACTIVE","userAuthMode":"NONE"}""";

    private static final String CREATED = "{\"appInstance\":" + CREATED_INSTANCE + ",\"secretKey\":\"" + SECRET_KEY
        + "\"}";

    private static final String INSTANCES =
        """
            [{"id":7,"uuid":"instance-uuid","appId":42,"appName":"my-app","workspaceId":1,"environment":"%1$s","name":"Main","status":"ACTIVE","url":"https://my-app.example","userAuthMode":"REQUIRED"},{"id":8,"uuid":"partner-uuid","appId":42,"appName":"my-app","workspaceId":2,"environment":"%1$s","name":"Partner","status":"ACTIVE","userAuthMode":"NONE"}]""";

    private static final String APP_PATH = "/api/automation/v1/apps/42";

    private static final String INSTANCE_PATH = "/api/automation/v1/app-instances/7";

    private static final String DEPLOYMENT_SETTINGS_PATH = "/api/automation/v1/app-instances/7/deployment-settings";

    private static final String APP =
        """
            {"id":42,"uuid":"app-uuid","name":"my-app","instances":[{"id":7,"uuid":"instance-uuid","appId":42,"workspaceId":1,"environment":"%1$s","name":"Main","status":"ACTIVE"}]}""";

    private static final String UPDATED_INSTANCE =
        """
            {"id":7,"uuid":"instance-uuid","appId":42,"appName":"my-app","workspaceId":1,"environment":"DEVELOPMENT","name":"Local","status":"ACTIVE","url":"http://localhost:3000","userAuthMode":"REQUIRED"}""";

    private static final String INSTANCE_WITH_SETTINGS =
        """
            {"id":7,"uuid":"instance-uuid","appId":42,"appName":"my-app","workspaceId":1,"environment":"%1$s","name":"Main","status":"ACTIVE","hostingTarget":"VERCEL","deploymentConnectionId":%2$s,"deploymentSettings":%3$s}""";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    private AutomationAppInstanceCommand command(Path workingDirectory) {
        AutomationAppInstanceCommand automationAppInstanceCommand = new AutomationAppInstanceCommand();

        automationAppInstanceCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppInstanceCommand.setEnvironmentVariables(Map.of());
        automationAppInstanceCommand.setWorkingDirectory(workingDirectory);
        automationAppInstanceCommand.setOutput(
            new PrintStream(stdout, true, StandardCharsets.UTF_8),
            new PrintStream(stderr, true, StandardCharsets.UTF_8));

        return automationAppInstanceCommand;
    }

    private String printed() {
        return stdout.toString(StandardCharsets.UTF_8) + stderr.toString(StandardCharsets.UTF_8);
    }

    private static Path linkedAppDirectory(Path appDirectory, String host) throws IOException {
        AppLinkFile.write(appDirectory, new AppLink(42, "instance-uuid", 7, 1, "DEVELOPMENT", host));

        return appDirectory;
    }

    @Test
    void testCreateOutsideDevelopmentIsRefusedLocally() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            for (String environment : new String[] {
                "STAGING", "PRODUCTION"
            }) {
                CliException cliException = assertThrows(
                    CliException.class,
                    () -> command(tempDir).instanceCreate(
                        42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", environment, 1L,
                        "json"));

                assertEquals(1, cliException.exitCode());
                assertTrue(cliException.getMessage()
                    .contains("DEVELOPMENT only"), cliException.getMessage());
            }

            assertTrue(stub.requests()
                .isEmpty());
            assertFalse(Files.exists(tempDir.resolve(".env.partner")));
        }
    }

    @Test
    void testListWorksInProduction() throws Exception {
        String instances = String.format(INSTANCES, "PRODUCTION");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + INSTANCES_PATH, RoutedStubApi.response(200, instances)))) {

            command(tempDir).instanceList(42L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            RoutedStubApi.Request request = stub.lastRequest("GET", INSTANCES_PATH)
                .orElseThrow();
            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals("PRODUCTION", request.environment());
            assertEquals(2, result.size());
            assertEquals(8, result.get(1)
                .get("id")
                .asLong());
            assertEquals("NONE", result.get(1)
                .get("userAuthMode")
                .asText());
            assertEquals(2, result.get(1)
                .get("workspaceId")
                .asLong());
            assertTrue(stub.requests()
                .stream()
                .allMatch(recorded -> "GET".equals(recorded.method())));
        }
    }

    @Test
    void testListFindsTheAppThroughTheLinkFileAndRendersATable() throws Exception {
        String instances = String.format(INSTANCES, "DEVELOPMENT");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + INSTANCES_PATH, RoutedStubApi.response(200, instances)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());

            command(appDirectory).instanceList(null, null, stub.host(), "btc_x", "DEVELOPMENT", "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("SIGN-IN"), rendered);
            assertTrue(rendered.contains("Partner"), rendered);
        }
    }

    @Test
    void testDeploymentSettingsSetSendsTheConnectionAndEverySetting() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "PUT " + DEPLOYMENT_SETTINGS_PATH,
                RoutedStubApi.response(
                    200,
                    String.format(
                        INSTANCE_WITH_SETTINGS, "DEVELOPMENT", "5",
                        "{\"teamId\":\"team_1\",\"projectName\":\"a=b\"}"))))) {

            command(tempDir).deploymentSettingsSet(
                List.of("teamId=team_1", "projectName=a=b"), 42L, null, 5L, null, stub.host(), "btc_x",
                "DEVELOPMENT", "json");

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", DEPLOYMENT_SETTINGS_PATH)
                    .orElseThrow()
                    .body());
            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals(5, body.get("connectionId")
                .asLong());
            assertEquals("team_1", body.get("settings")
                .get("teamId")
                .asText());
            assertEquals("a=b", body.get("settings")
                .get("projectName")
                .asText());
            assertEquals(7, result.get("instanceId")
                .asLong());
            assertEquals(5, result.get("connectionId")
                .asLong());
            assertEquals("team_1", result.get("settings")
                .get("teamId")
                .asText());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "teamId", "=team_1", " =x"
    })
    void testDeploymentSettingsSetRefusesASettingWithoutAKeyAndValueBeforeAnyRequest(String setting)
        throws Exception {

        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).deploymentSettingsSet(
                    List.of(setting), 42L, 7L, 5L, null, stub.host(), "btc_x", "DEVELOPMENT", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--setting"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "=s3cr3t-value", "s3cr3t-value"
    })
    void testDeploymentSettingsSetDoesNotEchoAMalformedSetting(String setting) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).deploymentSettingsSet(
                    List.of(setting), 42L, 7L, 5L, null, stub.host(), "btc_x", "DEVELOPMENT", "json"));

            assertFalse(cliException.getMessage()
                .contains("s3cr3t"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("key=value"), cliException.getMessage());
        }
    }

    @Test
    void testDeploymentSettingsSetRefusesARepeatedKeyBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).deploymentSettingsSet(
                    List.of("teamId=a", "teamId=b"), 42L, 7L, 5L, null, stub.host(), "btc_x", "DEVELOPMENT",
                    "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("teamId"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testDeploymentSettingsClearSendsNoConnectionAndNoSettings() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "PUT " + DEPLOYMENT_SETTINGS_PATH,
                RoutedStubApi.response(200, String.format(INSTANCE_WITH_SETTINGS, "DEVELOPMENT", "null", "{}"))))) {

            command(tempDir).deploymentSettingsClear(
                42L, 7L, null, stub.host(), "btc_x", "DEVELOPMENT", "json");

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", DEPLOYMENT_SETTINGS_PATH)
                    .orElseThrow()
                    .body());
            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertTrue(body.path("connectionId")
                .isMissingNode()
                || body.get("connectionId")
                    .isNull(),
                body.toString());
            assertEquals(0, body.get("settings")
                .size());
            assertTrue(result.get("connectionId")
                .isNull(), result.toString());
        }
    }

    @Test
    void testDeploymentSettingsSetAndClearOutsideDevelopmentAreRefusedLocally() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            for (String environment : new String[] {
                "STAGING", "PRODUCTION"
            }) {
                CliException setException = assertThrows(
                    CliException.class,
                    () -> command(tempDir).deploymentSettingsSet(
                        List.of("teamId=a"), 42L, 7L, 5L, null, stub.host(), "btc_x", environment, "json"));
                CliException clearException = assertThrows(
                    CliException.class,
                    () -> command(tempDir).deploymentSettingsClear(
                        42L, 7L, null, stub.host(), "btc_x", environment, "json"));

                assertTrue(setException.getMessage()
                    .contains("DEVELOPMENT only"), setException.getMessage());
                assertTrue(clearException.getMessage()
                    .contains("DEVELOPMENT only"), clearException.getMessage());
            }

            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testDeploymentSettingsGetWorksInProduction() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_PATH,
                RoutedStubApi.response(
                    200, String.format(INSTANCE_WITH_SETTINGS, "PRODUCTION", "5", "{\"teamId\":\"team_1\"}"))))) {

            command(tempDir).deploymentSettingsGet(42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals(7, result.get("instanceId")
                .asLong());
            assertEquals("VERCEL", result.get("hostingTarget")
                .asText());
            assertEquals(5, result.get("connectionId")
                .asLong());
            assertEquals("team_1", result.get("settings")
                .get("teamId")
                .asText());
            assertEquals("PRODUCTION", stub.lastRequest("GET", INSTANCE_PATH)
                .orElseThrow()
                .environment());
        }
    }

    @Test
    void testDeploymentSettingsCommandsAreRegisteredWithTheShellAndCollectEveryRepeatedSetting() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "GET " + INSTANCE_PATH,
                RoutedStubApi.response(200, String.format(INSTANCE_WITH_SETTINGS, "DEVELOPMENT", "5", "{}")),
                "PUT " + DEPLOYMENT_SETTINGS_PATH,
                RoutedStubApi.response(200, String.format(INSTANCE_WITH_SETTINGS, "DEVELOPMENT", "5", "{}"))))) {

            String[] connection = {
                "--app-id", "42", "--instance-id", "7", "--host", stub.host(), "--token", "btc_x", "--environment",
                "DEVELOPMENT"
            };

            assertEquals(
                0,
                CliApplication.execute(
                    concat(
                        connection, "automation", "app", "instance", "deployment-settings", "set", "--connection-id",
                        "5", "--setting", "teamId=team_1", "--setting", "projectName=my-app")));

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PUT", DEPLOYMENT_SETTINGS_PATH)
                    .orElseThrow()
                    .body());

            assertEquals("team_1", body.get("settings")
                .get("teamId")
                .asText());
            assertEquals("my-app", body.get("settings")
                .get("projectName")
                .asText());
            assertEquals(
                0,
                CliApplication
                    .execute(concat(connection, "automation", "app", "instance", "deployment-settings", "clear")));
            assertEquals(
                0,
                CliApplication
                    .execute(concat(connection, "automation", "app", "instance", "deployment-settings", "get")));
            assertTrue(stub.lastRequest("GET", INSTANCE_PATH)
                .isPresent());
        }
    }

    private static String[] concat(String[] connection, String... arguments) {
        return Stream.concat(Stream.of(arguments), Stream.of(connection))
            .toArray(String[]::new);
    }

    @Test
    void testUpdateSendsOnlyTheGivenUrlAndName() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "PATCH " + INSTANCE_PATH, RoutedStubApi.response(200, UPDATED_INSTANCE)))) {

            command(tempDir).instanceUpdate(
                42L, 7L, "http://localhost:3000", "Local", null, stub.host(), "btc_x", "DEVELOPMENT", "json");

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PATCH", INSTANCE_PATH)
                    .orElseThrow()
                    .body());
            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals("http://localhost:3000", body.get("url")
                .asText());
            assertEquals("Local", body.get("name")
                .asText());
            assertTrue(body.path("hostingTarget")
                .isMissingNode(), body.toString());
            assertTrue(body.path("configuration")
                .isMissingNode(), body.toString());
            assertEquals("http://localhost:3000", result.get("url")
                .asText());
            assertEquals("Local", result.get("name")
                .asText());
        }
    }

    @Test
    void testUpdateWithOnlyAUrlLeavesTheNameOut() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "PATCH " + INSTANCE_PATH, RoutedStubApi.response(200, UPDATED_INSTANCE)))) {

            command(linkedAppDirectory(tempDir, stub.host())).instanceUpdate(
                null, null, "http://localhost:3000", null, null, stub.host(), "btc_x", "DEVELOPMENT", "json");

            JsonNode body = OBJECT_MAPPER.readTree(
                stub.lastRequest("PATCH", INSTANCE_PATH)
                    .orElseThrow()
                    .body());

            assertEquals("http://localhost:3000", body.get("url")
                .asText());
            assertTrue(body.path("name")
                .isMissingNode(), body.toString());
        }
    }

    @Test
    void testUpdateWithoutUrlOrNameIsRefusedBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceUpdate(
                    42L, 7L, null, null, null, stub.host(), "btc_x", "DEVELOPMENT", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--url"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testUpdateOutsideDevelopmentIsRefusedLocally() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            for (String environment : new String[] {
                "STAGING", "PRODUCTION"
            }) {
                CliException cliException = assertThrows(
                    CliException.class,
                    () -> command(tempDir).instanceUpdate(
                        42L, 7L, "https://my-app.example", null, null, stub.host(), "btc_x", environment, "json"));

                assertTrue(cliException.getMessage()
                    .contains("DEVELOPMENT only"), cliException.getMessage());
            }

            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testUpdateIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT")),
                "PATCH " + INSTANCE_PATH, RoutedStubApi.response(200, UPDATED_INSTANCE)))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "instance", "update", "--app-id", "42", "--instance-id", "7", "--url",
                "http://localhost:3000", "--host", stub.host(), "--token", "btc_x", "--environment", "DEVELOPMENT");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("PATCH", INSTANCE_PATH)
                .isPresent());
        }
    }

    @Test
    void testListIsRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + INSTANCES_PATH, RoutedStubApi.response(200, "[]")))) {

            int exitCode = CliApplication.execute(
                "automation", "app", "instance", "list", "--app-id", "42", "--host", stub.host(), "--token", "btc_x",
                "--environment", "PRODUCTION");

            assertEquals(0, exitCode);
            assertTrue(stub.lastRequest("GET", INSTANCES_PATH)
                .isPresent());
        }
    }

    @Test
    void testListNamesTheProfilesEnvironmentWhenTheKeyIsRefused() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + INSTANCES_PATH,
                RoutedStubApi.response(401, "{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid key\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceList(42L, null, stub.host(), "btc_x", "STAGING", "json"));

            assertTrue(cliException.getMessage()
                .contains("created in STAGING"), cliException.getMessage());
        }
    }

    @Test
    void testListWithoutAppJsonOrAppIdSaysWhatToDo() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceList(null, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--app-id"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateInTheProfilesWorkspaceWritesTheKeyToTheNamedFileOnly() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).instanceCreate(
                42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            RoutedStubApi.Request request = stub.lastRequest("POST", INSTANCES_PATH)
                .orElseThrow();
            JsonNode requestBody = OBJECT_MAPPER.readTree(request.body());

            assertEquals("DEVELOPMENT", request.environment());
            assertEquals(1, requestBody.get("workspaceId")
                .asLong());
            assertEquals("Partner", requestBody.get("name")
                .asText());
            assertEquals("VERCEL", requestBody.get("hostingTarget")
                .asText());
            assertFalse(requestBody.hasNonNull("userAuthMode"), requestBody.toString());
            assertEquals(
                "BYTECHEF_URL=" + stub.host() + "\nBYTECHEF_APP_KEY=" + SECRET_KEY + "\n",
                Files.readString(tempDir.resolve(".env.partner")));
            assertFalse(Files.exists(AppLinkFile.pathIn(tempDir)));
            assertFalse(printed().contains(SECRET_KEY), printed());

            JsonNode result = OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));

            assertEquals(42, result.get("id")
                .asLong());
            assertEquals(8, result.get("instanceId")
                .asLong());
            assertEquals("partner-uuid", result.get("uuid")
                .asText());
            assertEquals("NONE", result.get("userAuthMode")
                .asText());
            assertFalse(result.has("secretKey"));
        }
    }

    @Test
    void testCreateInAnotherWorkspaceInALinkedDirectoryLeavesTheLinkFileAndItsEnvFileAlone() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path linkFile = AppLinkFile.pathIn(appDirectory);
            String linkFileBefore = Files.readString(linkFile);

            Files.writeString(appDirectory.resolve(".env.local"), "BYTECHEF_APP_KEY=existing\n");

            command(appDirectory).instanceCreate(
                null, "Partner", "VERCEL", "NONE", ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 2L,
                "json");

            JsonNode requestBody = OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", INSTANCES_PATH)
                    .orElseThrow()
                    .body());

            assertEquals(2, requestBody.get("workspaceId")
                .asLong());
            assertEquals("NONE", requestBody.get("userAuthMode")
                .asText());
            assertEquals(linkFileBefore, Files.readString(linkFile));
            assertEquals("BYTECHEF_APP_KEY=existing\n", Files.readString(appDirectory.resolve(".env.local")));
            assertTrue(Files.readString(appDirectory.resolve(".env.partner"))
                .contains(SECRET_KEY));
        }
    }

    @Test
    void testCreateInASubdirectoryOfALinkedAppWritesTheEnvFileNextToTheLinkLikeKeyRotate() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            Path appDirectory = linkedAppDirectory(tempDir.resolve("app"), stub.host());
            Path subdirectory = Files.createDirectories(appDirectory.resolve("src"));

            command(subdirectory).instanceCreate(
                null, "Partner", "VERCEL", "NONE", ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 2L,
                "json");

            assertTrue(Files.readString(appDirectory.resolve(".env.partner"))
                .contains(SECRET_KEY));
            assertFalse(Files.exists(subdirectory.resolve(".env.partner")));
        }
    }

    @Test
    void testCreateWarnsOnStderrWhenTheServerPickedNoSignIn() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).instanceCreate(
                42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            String errorOutput = stderr.toString(StandardCharsets.UTF_8);

            assertTrue(errorOutput.contains(
                "The instance has no end-user sign-in because this ByteChef server has no authorization server "
                    + "enabled; anyone with the app's URL can open it."),
                errorOutput);
        }
    }

    @Test
    void testCreateDoesNotWarnWhenTheServerPickedSignIn() throws Exception {
        String createdWithSignIn = CREATED.replace("\"userAuthMode\":\"NONE\"", "\"userAuthMode\":\"REQUIRED\"");

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, createdWithSignIn)))) {

            command(tempDir).instanceCreate(
                42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L, "json");

            assertFalse(stderr.toString(StandardCharsets.UTF_8)
                .contains("end-user sign-in"));
        }
    }

    @Test
    void testCreateDoesNotWarnWhenNoSignInWasAskedFor() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).instanceCreate(
                42L, "Partner", "VERCEL", "NONE", ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                "json");

            assertFalse(stderr.toString(StandardCharsets.UTF_8)
                .contains("end-user sign-in"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "NONE", "OPTIONAL", "REQUIRED", "optional"
    })
    void testCreateSendsTheChosenUserAuthMode(String userAuthMode) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            command(tempDir).instanceCreate(
                42L, "Partner", "VERCEL", userAuthMode, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                "json");

            JsonNode requestBody = OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", INSTANCES_PATH)
                    .orElseThrow()
                    .body());

            assertEquals(userAuthMode.toUpperCase(Locale.ROOT), requestBody.get("userAuthMode")
                .asText());
        }
    }

    @Test
    void testCreateRefusesAnUnknownUserAuthModeBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "VERCEL", "ALWAYS", ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            assertTrue(cliException.getMessage()
                .contains("NONE, OPTIONAL or REQUIRED"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateRefusesAnUnknownHostingTargetBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "NETLIFY", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            assertTrue(cliException.getMessage()
                .contains("VERCEL or SELF_HOSTED"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateNamesTheNoneOptionWhenSignInIsUnavailable() throws Exception {
        String unavailable =
            """
                {"code":"APP_USER_AUTH_UNAVAILABLE","message":"App instance sign-in mode REQUIRED needs the authorization server; set bytechef.oauth2.authorization-server.enabled=true or use NONE"}""";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(409, unavailable)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            String message = cliException.getMessage();

            assertTrue(message.contains("bytechef.oauth2.authorization-server.enabled"), message);
            assertTrue(message.contains("--user-auth-mode NONE"), message);
            assertFalse(Files.exists(tempDir.resolve(".env.partner")));
        }
    }

    @Test
    void testCreateRefusesAnEnvFileInAMissingDirectoryBeforeAnyRequest() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "VERCEL", null, "missing/.env.partner", null, stub.host(), "btc_x",
                    "DEVELOPMENT", 1L, "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWithoutAWorkspaceExitsWithCode4() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", null,
                    "json"));

            assertEquals(4, cliException.exitCode());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateWithoutAppJsonOrAppIdSaysWhatToDo() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    null, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            assertTrue(cliException.getMessage()
                .contains("--app-id"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testCreateReportsKeyRotateWithTheInstanceIdWhenTheEnvFileCannotBeWritten() throws Exception {
        String unwritableKey = "{\"appInstance\":" + CREATED_INSTANCE + ",\"secretKey\":\"bca_split\\nkey\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, unwritableKey)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(tempDir).instanceCreate(
                    42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", 1L,
                    "json"));

            String message = cliException.getMessage();

            assertEquals(1, cliException.exitCode());
            assertTrue(message.contains("Created instance 8 of app 42"), message);
            assertTrue(message.contains("bytechef automation app key rotate --app-id 42 --instance-id 8"), message);
            assertFalse(message.contains("bca_split"), message);
        }
    }

    @Test
    void testCreateDefaultsTheWorkspaceToTheProfilesWorkspace() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + INSTANCES_PATH, RoutedStubApi.response(201, CREATED)))) {

            AutomationAppInstanceCommand automationAppInstanceCommand = command(tempDir);

            automationAppInstanceCommand.setEnvironmentVariables(Map.of("BYTECHEF_WORKSPACE_ID", "5"));

            automationAppInstanceCommand.instanceCreate(
                42L, "Partner", "VERCEL", null, ".env.partner", null, stub.host(), "btc_x", "DEVELOPMENT", null,
                "json");

            JsonNode requestBody = OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", INSTANCES_PATH)
                    .orElseThrow()
                    .body());

            assertEquals(5, requestBody.get("workspaceId")
                .asLong());
        }
    }
}
