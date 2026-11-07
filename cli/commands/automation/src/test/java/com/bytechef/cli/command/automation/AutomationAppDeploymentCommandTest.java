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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ivica Cardic
 */
class AutomationAppDeploymentCommandTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String APP_PATH = "/api/automation/v1/apps/42";
    private static final String VERSIONS_PATH = "/api/automation/v1/apps/42/versions";
    private static final String INSTANCE_PATH = "/api/automation/v1/app-instances/7";
    private static final String INSTANCE_VERSIONS_PATH = "/api/automation/v1/app-instances/7/versions";
    private static final String DEPLOYMENTS_PATH = "/api/automation/v1/app-instances/7/deployments";

    private static final String APP =
        """
            {"id":42,"uuid":"app-uuid","name":"my-app","instances":[{"id":7,"uuid":"instance-uuid","appId":42,"workspaceId":1,"environment":"%1$s","name":"Main","status":"ACTIVE"}]}""";

    private static final String APP_WITH_TWO_INSTANCES =
        """
            {"id":42,"uuid":"app-uuid","name":"my-app","instances":[{"id":7,"appId":42,"workspaceId":1,"environment":"%1$s","name":"Main","status":"ACTIVE"},{"id":8,"appId":42,"workspaceId":1,"environment":"%1$s","name":"Partner","status":"ACTIVE"}]}""";

    private static final String VERSION =
        """
            {"id":70,"appId":42,"name":"1.1","gitRef":"main","gitCommitSha":"abc123","description":"First release","createdBy":"ci","createdDate":"2026-01-01T00:00:00Z"}""";

    private static final String DEPLOYMENT =
        "{\"id\":90,\"appInstanceId\":7,\"appVersion\":" + VERSION + ",\"provider\":\"EXTERNAL\","
            + "\"url\":\"https://my-app.example\",\"status\":\"READY\",\"current\":true,\"createdBy\":\"ci\","
            + "\"createdDate\":\"2026-01-01T00:00:00Z\",\"finishedDate\":\"2026-01-01T00:00:00Z\"}";

    private static final String DEPLOY_PATH = "/api/automation/v1/app-instances/7/deploy";
    private static final String ROLLBACK_PATH = "/api/automation/v1/app-instances/7/rollback";
    private static final String DEPLOYMENT_PATH = "/api/automation/v1/app-instances/7/deployments/90";
    private static final String CANCEL_PATH = "/api/automation/v1/app-instances/7/deployments/90/cancel";

    private static final String PROVIDER_DEPLOYMENT =
        "{\"id\":90,\"appInstanceId\":7,\"appVersion\":" + VERSION + ",\"provider\":\"VERCEL\","
            + "\"url\":%2$s,\"status\":\"%1$s\",\"errorMessage\":%3$s,\"current\":false,\"createdBy\":\"ci\","
            + "\"createdDate\":\"2026-01-01T00:00:00Z\"}";

    private static final String VERSION_NAME_RACE =
        "{\"code\":\"APP_VERSION_NAME_ALREADY_EXISTS\",\"message\":\"App version '1.1' already exists\"}";

    @TempDir
    Path tempDir;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private final AtomicLong fakeNanos = new AtomicLong();
    private final List<Duration> sleeps = new ArrayList<>();

    private static String providerDeployment(String status) {
        return String.format(PROVIDER_DEPLOYMENT, status, "null", "null");
    }

    private static String providerDeployment(String status, String url, String errorMessage) {
        return String.format(
            PROVIDER_DEPLOYMENT, status, url == null ? "null" : "\"" + url + "\"",
            errorMessage == null ? "null" : "\"" + errorMessage + "\"");
    }

    private AutomationAppDeploymentCommand command(Path workingDirectory) {
        AutomationAppDeploymentCommand automationAppDeploymentCommand = new AutomationAppDeploymentCommand();

        automationAppDeploymentCommand.setNanoClock(fakeNanos::get);
        automationAppDeploymentCommand.setPollInterval(Duration.ofSeconds(5));
        automationAppDeploymentCommand.setSleeper(duration -> {
            sleeps.add(duration);
            fakeNanos.addAndGet(duration.toNanos());
        });
        automationAppDeploymentCommand.setErrorOutput(new PrintStream(stderr, true, StandardCharsets.UTF_8));

        automationAppDeploymentCommand.setConfigPath(tempDir.resolve("no-config"));
        automationAppDeploymentCommand.setEnvironmentVariables(Map.of());
        automationAppDeploymentCommand.setWorkingDirectory(workingDirectory);
        automationAppDeploymentCommand.setOutput(new PrintStream(stdout, true, StandardCharsets.UTF_8));

        return automationAppDeploymentCommand;
    }

    private JsonNode printedJson() throws IOException {
        return OBJECT_MAPPER.readTree(stdout.toString(StandardCharsets.UTF_8));
    }

    private Path ciDirectory() throws IOException {
        return Files.createDirectory(tempDir.resolve("ci"));
    }

    private static List<Path> filesIn(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                .toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testRecordWorksInProduction(String environment) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(201, DEPLOYMENT)))) {

            Path ciDirectory = ciDirectory();

            command(ciDirectory).deploymentRecord(
                "1.1", "https://my-app.example", "abc123", 42L, 7L, null, stub.host(), "btc_x", environment, "json");

            RoutedStubApi.Request request = stub.lastRequest("POST", DEPLOYMENTS_PATH)
                .orElseThrow();
            JsonNode body = OBJECT_MAPPER.readTree(request.body());
            JsonNode result = printedJson();

            assertEquals(environment, request.environment());
            assertEquals("1.1", body.get("versionName")
                .asText());
            assertEquals("https://my-app.example", body.get("url")
                .asText());
            assertEquals("abc123", body.get("gitCommitSha")
                .asText());
            assertEquals(90, result.get("id")
                .asLong());
            assertEquals(7, result.get("instanceId")
                .asLong());
            assertEquals("1.1", result.get("version")
                .asText());
            assertEquals("READY", result.get("status")
                .asText());
            assertTrue(result.get("current")
                .asBoolean());
            assertTrue(stub.requests()
                .stream()
                .allMatch(recorded -> environment.equals(recorded.environment())));
            assertTrue(filesIn(tempDir).isEmpty(), filesIn(tempDir).toString());
        }
    }

    @Test
    void testRecordWithOnlyAnAppIdUsesTheSingleInstanceOfTheEnvironment() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(201, DEPLOYMENT)))) {

            command(ciDirectory()).deploymentRecord(
                "1.1", null, null, 42L, null, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode body = OBJECT_MAPPER.readTree(stub.lastRequest("POST", DEPLOYMENTS_PATH)
                .orElseThrow()
                .body());

            assertEquals("1.1", body.get("versionName")
                .asText());
            assertFalse(body.hasNonNull("url"));
            assertFalse(body.hasNonNull("gitCommitSha"));
        }
    }

    @Test
    void testRecordOfAnAppWithSeveralInstancesNeedsAnInstanceId() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP_WITH_TWO_INSTANCES, "STAGING"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRecord(
                    "1.1", null, null, 42L, null, null, stub.host(), "btc_x", "STAGING", "json"));

            assertTrue(cliException.getMessage()
                .contains("--instance-id"), cliException.getMessage());
            assertTrue(stub.lastRequest("POST", DEPLOYMENTS_PATH)
                .isEmpty());
        }
    }

    @Test
    void testRecordOfAnInstanceOfAnotherEnvironmentIsRefusedBeforeRecording() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "DEVELOPMENT"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRecord(
                    "1.1", null, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertTrue(cliException.getMessage()
                .contains("no instance 7 in PRODUCTION"), cliException.getMessage());
            assertTrue(stub.lastRequest("POST", DEPLOYMENTS_PATH)
                .isEmpty());
        }
    }

    @Test
    void testRecordInALinkedDirectoryUsesTheLinkedInstance() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(201, DEPLOYMENT)))) {

            Path appDirectory = tempDir.resolve("app");

            AppLinkFile.write(appDirectory, new AppLink(42, "instance-uuid", 7, 1, "DEVELOPMENT", stub.host()));

            command(appDirectory).deploymentRecord(
                "1.1", null, null, null, null, null, stub.host(), "btc_x", "DEVELOPMENT", "json");

            assertEquals(1, stub.requests()
                .size());
            assertEquals("DEVELOPMENT", stub.lastRequest("POST", DEPLOYMENTS_PATH)
                .orElseThrow()
                .environment());
        }
    }

    @Test
    void testRecordRetriesOnceOnAVersionNameRace() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.startWithSequences(
            Map.of("GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION"))),
            Map.of(
                "POST " + DEPLOYMENTS_PATH,
                List.of(RoutedStubApi.response(409, VERSION_NAME_RACE), RoutedStubApi.response(201, DEPLOYMENT))))) {

            command(ciDirectory()).deploymentRecord(
                "1.1", null, "abc123", 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals(2, countRequests(stub, "POST", DEPLOYMENTS_PATH));
            assertEquals(90, printedJson().get("id")
                .asLong());
        }
    }

    @Test
    void testRecordRetriesOnlyOnce() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(409, VERSION_NAME_RACE)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRecord(
                    "1.1", null, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(2, countRequests(stub, "POST", DEPLOYMENTS_PATH));
            assertTrue(cliException.getMessage()
                .contains("APP_VERSION_NAME_ALREADY_EXISTS"), cliException.getMessage());
        }
    }

    @Test
    void testRecordDoesNotRetryAnotherConflict() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "POST " + DEPLOYMENTS_PATH,
                RoutedStubApi.response(
                    409,
                    "{\"code\":\"APP_VERSION_COMMIT_MISMATCH\",\"message\":\"App version '1.1' is commit a\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRecord(
                    "1.1", null, "def456", 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, countRequests(stub, "POST", DEPLOYMENTS_PATH));
            assertTrue(cliException.getMessage()
                .contains("APP_VERSION_COMMIT_MISMATCH"), cliException.getMessage());
        }
    }

    @Test
    void testRecordForbiddenNamesTheDeployRole() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "POST " + DEPLOYMENTS_PATH,
                RoutedStubApi.response(403, "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRecord(
                    "1.1", null, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(2, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("role in PRODUCTION"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("APP_DEPLOY"), cliException.getMessage());
        }
    }

    @Test
    void testDeploymentListWorksInProduction() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[" + DEPLOYMENT + "]")))) {

            command(ciDirectory()).deploymentList(42L, 7L, null, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode result = printedJson();

            assertEquals("PRODUCTION", stub.lastRequest("GET", DEPLOYMENTS_PATH)
                .orElseThrow()
                .environment());
            assertEquals(1, result.size());
            assertEquals("1.1", result.get(0)
                .get("version")
                .asText());
            assertEquals("abc123", result.get(0)
                .get("commit")
                .asText());
            assertTrue(stub.requests()
                .stream()
                .allMatch(recorded -> "GET".equals(recorded.method())));
        }
    }

    @Test
    void testDeploymentListRendersATable() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "STAGING")),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[" + DEPLOYMENT + "]")))) {

            command(ciDirectory()).deploymentList(42L, 7L, null, null, stub.host(), "btc_x", "STAGING", "table");

            String rendered = stdout.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("VERSION"), rendered);
            assertTrue(rendered.contains("READY"), rendered);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testVersionCreateWorksInProduction(String environment) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + VERSIONS_PATH, RoutedStubApi.response(201, VERSION)))) {

            command(ciDirectory()).versionCreate(
                "1.1", "abc123", "main", "First release", 42L, null, stub.host(), "btc_x", environment, "json");

            RoutedStubApi.Request request = stub.lastRequest("POST", VERSIONS_PATH)
                .orElseThrow();
            JsonNode body = OBJECT_MAPPER.readTree(request.body());
            JsonNode result = printedJson();

            assertEquals(environment, request.environment());
            assertEquals("1.1", body.get("name")
                .asText());
            assertEquals("abc123", body.get("gitCommitSha")
                .asText());
            assertEquals("main", body.get("gitRef")
                .asText());
            assertEquals("First release", body.get("description")
                .asText());
            assertEquals(70, result.get("id")
                .asLong());
            assertEquals("abc123", result.get("commit")
                .asText());
            assertEquals(1, stub.requests()
                .size());
            assertTrue(filesIn(tempDir).isEmpty(), filesIn(tempDir).toString());
        }
    }

    @Test
    void testVersionCreateOfATakenNameFails() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("POST " + VERSIONS_PATH, RoutedStubApi.response(409, VERSION_NAME_RACE)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).versionCreate(
                    "1.1", null, null, null, 42L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertEquals(1, stub.requests()
                .size());
            assertTrue(cliException.getMessage()
                .contains("APP_VERSION_NAME_ALREADY_EXISTS"), cliException.getMessage());
        }
    }

    @Test
    void testVersionListWorksInProduction() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]")))) {

            command(ciDirectory()).versionList(42L, null, null, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode result = printedJson();

            assertEquals("PRODUCTION", stub.lastRequest("GET", VERSIONS_PATH)
                .orElseThrow()
                .environment());
            assertEquals("1.1", result.get(0)
                .get("name")
                .asText());
        }
    }

    @Test
    void testVersionListWithAnInstanceIdReadsTheVersionsThroughTheInstance() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of("GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]")))) {

            command(ciDirectory()).versionList(42L, 7L, 5, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals("limit=5", stub.lastRequest("GET", INSTANCE_VERSIONS_PATH)
                .orElseThrow()
                .query());
            assertEquals("1.1", printedJson().get(0)
                .get("name")
                .asText());
            assertEquals(0, countRequests(stub, "GET", VERSIONS_PATH));
            assertEquals(0, countRequests(stub, "GET", APP_PATH));
        }
    }

    @Test
    void testDeployOfAnInstanceHostedInAnotherWorkspaceReadsTheInstanceAndItsVersions() throws Exception {
        String forbidden = "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}";
        String hostedInstance =
            "{\"id\":7,\"appId\":42,\"workspaceId\":2,\"environment\":\"PRODUCTION\",\"name\":\"Partner\","
                + "\"status\":\"ACTIVE\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(403, forbidden),
                "GET " + INSTANCE_PATH, RoutedStubApi.response(200, hostedInstance),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))))) {

            command(ciDirectory()).deploymentDeploy(
                "1.1", null, null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals("name=1.1", stub.lastRequest("GET", INSTANCE_VERSIONS_PATH)
                .orElseThrow()
                .query());
            assertEquals(70, OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", DEPLOY_PATH)
                    .orElseThrow()
                    .body())
                .get("appVersionId")
                .asLong());
            assertEquals(0, countRequests(stub, "GET", VERSIONS_PATH));
        }
    }

    @Test
    void testDeployOfAnInstanceOfAnotherAppIsRefusedWhenTheAppCannotBeRead() throws Exception {
        String forbidden = "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}";
        String otherAppInstance =
            "{\"id\":7,\"appId\":43,\"workspaceId\":2,\"environment\":\"PRODUCTION\",\"name\":\"Partner\","
                + "\"status\":\"ACTIVE\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(403, forbidden),
                "GET " + INSTANCE_PATH, RoutedStubApi.response(200, otherAppInstance)))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("App 42 has no instance 7"), cliException.getMessage());
            assertEquals(0, countRequests(stub, "GET", INSTANCE_VERSIONS_PATH));
            assertEquals(0, countRequests(stub, "POST", DEPLOY_PATH));
        }
    }

    @Test
    void testVersionListWithoutAppJsonOrAppIdSaysWhatToDo() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).versionList(null, null, null, null, stub.host(), "btc_x", "PRODUCTION",
                    "json"));

            assertTrue(cliException.getMessage()
                .contains("--app-id"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testDeployWorksInProduction(String environment) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))))) {

            Path ciDirectory = ciDirectory();

            command(ciDirectory).deploymentDeploy(
                "1.1", null, null, false, null, 42L, null, null, stub.host(), "btc_x", environment, "json");

            RoutedStubApi.Request versionRequest = stub.lastRequest("GET", INSTANCE_VERSIONS_PATH)
                .orElseThrow();
            RoutedStubApi.Request deployRequest = stub.lastRequest("POST", DEPLOY_PATH)
                .orElseThrow();
            JsonNode result = printedJson();

            assertEquals("name=1.1", versionRequest.query());
            assertEquals(70, OBJECT_MAPPER.readTree(deployRequest.body())
                .get("appVersionId")
                .asLong());
            assertEquals(90, result.get("id")
                .asLong());
            assertEquals("QUEUED", result.get("status")
                .asText());
            assertTrue(result.has("url"), result.toString());
            assertTrue(stub.lastRequest("GET", DEPLOYMENT_PATH)
                .isEmpty());
            assertTrue(stub.requests()
                .stream()
                .allMatch(recorded -> environment.equals(recorded.environment())));
            assertTrue(filesIn(tempDir).isEmpty(), filesIn(tempDir).toString());
        }
    }

    @Test
    void testDeployWithWaitPollsTheDeploymentUntilItIsReady() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.startWithSequences(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))),
            Map.of(
                "GET " + DEPLOYMENT_PATH,
                List.of(
                    RoutedStubApi.response(200, providerDeployment("BUILDING")),
                    RoutedStubApi.response(200, providerDeployment("READY", "https://my-app.vercel.app", null)))))) {

            command(ciDirectory()).deploymentDeploy(
                "1.1", null, null, true, 30, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode result = printedJson();

            assertEquals("READY", result.get("status")
                .asText());
            assertEquals("https://my-app.vercel.app", result.get("url")
                .asText());
            assertEquals(2, countRequests(stub, "GET", DEPLOYMENT_PATH));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "FAILED", "CANCELED"
    })
    void testDeployWithWaitThatEndsFailedOrCanceledExitsNonZero(String status) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "GET " + DEPLOYMENT_PATH,
                RoutedStubApi.response(200, providerDeployment(status, null, "The build failed"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, true, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("90"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains(status), cliException.getMessage());
            assertEquals(status, printedJson().get("status")
                .asText());
        }
    }

    @Test
    void testDeployWithoutWaitThatTheProviderCouldNotStartExitsNonZero() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH,
                RoutedStubApi.response(
                    201, providerDeployment("FAILED", null, "The hosting provider could not start the deployment"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("could not start"), cliException.getMessage());
        }
    }

    @Test
    void testDeployWithWaitTimesOutWithANonZeroExit() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "GET " + DEPLOYMENT_PATH, RoutedStubApi.response(200, providerDeployment("BUILDING"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, true, 1, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("BUILDING"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("1 second"), cliException.getMessage());
            assertTrue(countRequests(stub, "GET", DEPLOYMENT_PATH) >= 1);
        }
    }

    @Test
    void testDeployWithWaitKeepsPollingThroughATransientGatewayError() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.startWithSequences(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))),
            Map.of(
                "GET " + DEPLOYMENT_PATH,
                List.of(
                    RoutedStubApi.response(502, "<html>Bad Gateway</html>"),
                    RoutedStubApi.response(200, providerDeployment("READY", "https://my-app.vercel.app", null)))))) {

            command(ciDirectory()).deploymentDeploy(
                "1.1", null, null, true, 30, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals("READY", printedJson().get("status")
                .asText());
            assertEquals(2, countRequests(stub, "GET", DEPLOYMENT_PATH));
            assertTrue(stderr.toString(StandardCharsets.UTF_8)
                .contains("Deployment 90 is QUEUED"), stderr.toString(StandardCharsets.UTF_8));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {
        408, 429
    })
    void testDeployWithWaitKeepsPollingThroughATimeoutOrRateLimitAnswer(int statusCode) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.startWithSequences(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))),
            Map.of(
                "GET " + DEPLOYMENT_PATH,
                List.of(
                    RoutedStubApi.response(statusCode, "slow down"),
                    RoutedStubApi.response(200, providerDeployment("READY", "https://my-app.vercel.app", null)))))) {

            command(ciDirectory()).deploymentDeploy(
                "1.1", null, null, true, 30, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals("READY", printedJson().get("status")
                .asText());
            assertEquals(2, countRequests(stub, "GET", DEPLOYMENT_PATH));
        }
    }

    @Test
    void testDeployWithWaitStopsAtAForbiddenPollAndReportsTheDeploymentId() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "GET " + DEPLOYMENT_PATH,
                RoutedStubApi.response(403, "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, true, 30, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(2, cliException.exitCode());
            assertEquals(1, countRequests(stub, "GET", DEPLOYMENT_PATH));
            assertTrue(cliException.getMessage()
                .contains("keeps running on the server"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("deployment cancel --deployment-id 90"), cliException.getMessage());
            assertFalse(cliException.getMessage()
                .contains(".."), cliException.getMessage());
            assertEquals(90, printedJson().get("id")
                .asLong());
            assertEquals("QUEUED", printedJson().get("status")
                .asText());
        }
    }

    @Test
    void testDeployWithWaitHonorsATimeoutShorterThanThePollInterval() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "GET " + DEPLOYMENT_PATH, RoutedStubApi.response(200, providerDeployment("BUILDING"))))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, true, 1, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertEquals(List.of(Duration.ofSeconds(1)), sleeps);
            assertEquals(2, countRequests(stub, "GET", DEPLOYMENT_PATH));
            assertTrue(cliException.getMessage()
                .contains("BUILDING"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("deployment cancel --deployment-id 90"), cliException.getMessage());
            assertEquals("BUILDING", printedJson().get("status")
                .asText());
        }
    }

    @ParameterizedTest(name = "--timeout {0}")
    @ValueSource(ints = {
        0, -5
    })
    void testDeployRefusesATimeoutBelowOneSecondBeforeAnyRequest(int timeout) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(Map.of())) {
            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, true, timeout, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--timeout"), cliException.getMessage());
            assertTrue(stub.requests()
                .isEmpty());
        }
    }

    @Test
    void testDeployOfAnUnknownVersionWithoutACommitOrRefFailsBeforeDeploying() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[]")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "2.0", " ", null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(3, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("--commit"), cliException.getMessage());
            assertEquals(0, countRequests(stub, "POST", VERSIONS_PATH));
            assertEquals(0, countRequests(stub, "POST", DEPLOY_PATH));
        }
    }

    @Test
    void testDeployOfAnUnknownVersionWithACommitOrRefCreatesItFirst() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[]"),
                "POST " + VERSIONS_PATH, RoutedStubApi.response(201, VERSION),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))))) {

            command(ciDirectory()).deploymentDeploy(
                "1.1", "abc123", "main", false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            JsonNode versionBody = OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", VERSIONS_PATH)
                    .orElseThrow()
                    .body());

            assertEquals("1.1", versionBody.get("name")
                .asText());
            assertEquals("abc123", versionBody.get("gitCommitSha")
                .asText());
            assertEquals("main", versionBody.get("gitRef")
                .asText());
            assertEquals(70, OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", DEPLOY_PATH)
                    .orElseThrow()
                    .body())
                .get("appVersionId")
                .asLong());
        }
    }

    @Test
    void testDeployOfAnExistingVersionWithAnotherCommitIsRefusedBeforeDeploying() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", "def456", null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("abc123"), cliException.getMessage());
            assertEquals(0, countRequests(stub, "POST", DEPLOY_PATH));
        }
    }

    @Test
    void testDeployConflictShowsTheServersCode() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH,
                RoutedStubApi.response(
                    409,
                    "{\"code\":\"APP_DEPLOYMENT_IN_PROGRESS\",\"message\":\"Deployment 89 is in progress\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("APP_DEPLOYMENT_IN_PROGRESS"), cliException.getMessage());
        }
    }

    @Test
    void testDeployForbiddenNamesTheEnvironmentAndTheDeployRole() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "STAGING")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + DEPLOY_PATH,
                RoutedStubApi.response(403, "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentDeploy(
                    "1.1", null, null, false, null, 42L, 7L, null, stub.host(), "btc_x", "STAGING", "json"));

            assertEquals(2, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("role in STAGING"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("APP_DEPLOY"), cliException.getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testRollbackWorksInProduction(String environment) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "POST " + ROLLBACK_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED"))))) {

            command(ciDirectory()).deploymentRollback(
                "1.1", false, null, 42L, 7L, null, stub.host(), "btc_x", environment, "json");

            assertEquals(70, OBJECT_MAPPER.readTree(
                stub.lastRequest("POST", ROLLBACK_PATH)
                    .orElseThrow()
                    .body())
                .get("appVersionId")
                .asLong());
            assertEquals("QUEUED", printedJson().get("status")
                .asText());
            assertEquals(0, countRequests(stub, "POST", VERSIONS_PATH));
        }
    }

    @Test
    void testRollbackToAnUnknownVersionFailsWithoutCreatingIt() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + INSTANCE_VERSIONS_PATH, RoutedStubApi.response(200, "[]")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentRollback(
                    "0.9", false, null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(3, cliException.exitCode());
            assertEquals(0, countRequests(stub, "POST", VERSIONS_PATH));
            assertEquals(0, countRequests(stub, "POST", ROLLBACK_PATH));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testCancelWithADeploymentIdWorksInProduction(String environment) throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "POST " + CANCEL_PATH, RoutedStubApi.response(200, providerDeployment("CANCELED"))))) {

            command(ciDirectory()).deploymentCancel(
                90L, 42L, 7L, null, stub.host(), "btc_x", environment, "json");

            assertEquals("CANCELED", printedJson().get("status")
                .asText());
            assertEquals(0, countRequests(stub, "GET", DEPLOYMENTS_PATH));
        }
    }

    @Test
    void testCancelWithoutAnIdCancelsTheOneDeploymentInProgress() throws Exception {
        String deployments = "[" + providerDeployment("BUILDING") + ","
            + DEPLOYMENT.replace("\"id\":90", "\"id\":89") + "]";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, deployments),
                "POST " + CANCEL_PATH, RoutedStubApi.response(200, providerDeployment("CANCELED"))))) {

            command(ciDirectory()).deploymentCancel(
                null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals(1, countRequests(stub, "POST", CANCEL_PATH));
            assertEquals("CANCELED", printedJson().get("status")
                .asText());
        }
    }

    @Test
    void testCancelWithoutAnIdAndNothingInProgressFails() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[" + DEPLOYMENT + "]")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentCancel(
                    null, 42L, 7L, null, stub.host(), "btc_x", "PRODUCTION", "json"));

            assertEquals(3, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("in progress"), cliException.getMessage());
        }
    }

    @Test
    void testCommandsAreRegisteredWithTheShell() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.startWithSequences(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "POST " + VERSIONS_PATH, RoutedStubApi.response(201, VERSION),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[]"),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(201, DEPLOYMENT),
                "POST " + DEPLOY_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "POST " + ROLLBACK_PATH, RoutedStubApi.response(201, providerDeployment("QUEUED")),
                "GET " + DEPLOYMENT_PATH, RoutedStubApi.response(200, providerDeployment("READY")),
                "POST " + CANCEL_PATH, RoutedStubApi.response(200, providerDeployment("CANCELED"))),
            Map.of(
                "GET " + VERSIONS_PATH, List.of(RoutedStubApi.response(200, "[]")),
                "GET " + INSTANCE_VERSIONS_PATH, List.of(RoutedStubApi.response(200, "[" + VERSION + "]"))))) {

            String[] connection = {
                "--app-id", "42", "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION"
            };

            assertEquals(0, execute(connection, "automation", "app", "version", "list", "--limit", "5"));
            assertEquals(0, execute(connection, "automation", "app", "version", "create", "--name", "1.1"));
            assertEquals(
                0,
                execute(connection, "automation", "app", "deployment", "list", "--instance-id", "7", "--limit", "5"));
            assertEquals(
                0,
                execute(
                    connection, "automation", "app", "deployment", "record", "--version", "1.1", "--instance-id",
                    "7", "--url", "https://my-app.example", "--commit", "abc123"));

            assertEquals(
                0,
                execute(
                    connection, "automation", "app", "deployment", "deploy", "--version", "1.1", "--instance-id", "7",
                    "--wait", "--timeout", "30"));
            assertEquals(
                0,
                execute(
                    connection, "automation", "app", "deployment", "rollback", "--version", "1.1", "--instance-id",
                    "7"));
            assertEquals(
                0,
                execute(
                    connection, "automation", "app", "deployment", "cancel", "--deployment-id", "90",
                    "--instance-id", "7"));

            assertEquals(1, countRequests(stub, "POST", VERSIONS_PATH));
            assertEquals(1, countRequests(stub, "POST", DEPLOYMENTS_PATH));
            assertEquals(1, countRequests(stub, "POST", DEPLOY_PATH));
            assertEquals(1, countRequests(stub, "GET", DEPLOYMENT_PATH));
            assertEquals(1, countRequests(stub, "POST", ROLLBACK_PATH));
            assertEquals(1, countRequests(stub, "POST", CANCEL_PATH));
            assertEquals(1, countRequests(stub, "GET", VERSIONS_PATH));
            assertEquals(2, countRequests(stub, "GET", INSTANCE_VERSIONS_PATH));
            assertEquals(1, countRequests(stub, "GET", DEPLOYMENTS_PATH));
            assertTrue(stub.requests()
                .stream()
                .anyMatch(request -> VERSIONS_PATH.equals(request.path()) && "limit=5".equals(request.query())));
            assertEquals("limit=5", stub.lastRequest("GET", DEPLOYMENTS_PATH)
                .orElseThrow()
                .query());
        }
    }

    @Test
    void testListsSendTheGivenLimit() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[" + DEPLOYMENT + "]")))) {

            Path ciDirectory = ciDirectory();

            command(ciDirectory).deploymentList(42L, 7L, 7, null, stub.host(), "btc_x", "PRODUCTION", "json");
            command(ciDirectory).versionList(42L, null, 200, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertEquals("limit=7", stub.lastRequest("GET", DEPLOYMENTS_PATH)
                .orElseThrow()
                .query());
            assertEquals("limit=200", stub.lastRequest("GET", VERSIONS_PATH)
                .orElseThrow()
                .query());
        }
    }

    @Test
    void testListsWithoutALimitLeaveTheDefaultToTheServer() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, "PRODUCTION")),
                "GET " + VERSIONS_PATH, RoutedStubApi.response(200, "[" + VERSION + "]"),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(200, "[" + DEPLOYMENT + "]")))) {

            Path ciDirectory = ciDirectory();

            command(ciDirectory).deploymentList(42L, 7L, null, null, stub.host(), "btc_x", "PRODUCTION", "json");
            command(ciDirectory).versionList(42L, null, null, null, stub.host(), "btc_x", "PRODUCTION", "json");

            assertNull(stub.lastRequest("GET", DEPLOYMENTS_PATH)
                .orElseThrow()
                .query());
            assertNull(stub.lastRequest("GET", VERSIONS_PATH)
                .orElseThrow()
                .query());
        }
    }

    @Test
    void testListWithALimitTheServerRefusesShowsItsError() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + VERSIONS_PATH,
                RoutedStubApi.response(
                    400,
                    "{\"code\":\"APP_LIST_LIMIT_INVALID\",\"message\":\"The limit must be from 1 to 200\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).versionList(42L, null, 201, null, stub.host(), "btc_x", "PRODUCTION",
                    "json"));

            assertEquals(1, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("APP_LIST_LIMIT_INVALID"), cliException.getMessage());
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testEveryCommandNamesTheProfilesEnvironmentOn401(String environment) throws Exception {
        String unauthorized = "{\"code\":\"UNAUTHORIZED\",\"message\":\"Bad key\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "GET " + VERSIONS_PATH, RoutedStubApi.response(401, unauthorized),
                "POST " + VERSIONS_PATH, RoutedStubApi.response(401, unauthorized),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(401, unauthorized),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(401, unauthorized)))) {

            for (CliException cliException : failuresOfEveryCommand(stub, environment)) {
                assertEquals(2, cliException.exitCode());
                assertTrue(cliException.getMessage()
                    .contains("created in " + environment), cliException.getMessage());
                assertTrue(cliException.getMessage()
                    .contains("--environment " + environment), cliException.getMessage());
                assertFalse(cliException.getMessage()
                    .contains("DEVELOPMENT"), cliException.getMessage());
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "STAGING", "PRODUCTION"
    })
    void testEveryCommandNamesTheEnvironmentAndThePermissionOn403(String environment) throws Exception {
        String forbidden = "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}";

        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH, RoutedStubApi.response(200, String.format(APP, environment)),
                "GET " + VERSIONS_PATH, RoutedStubApi.response(403, forbidden),
                "POST " + VERSIONS_PATH, RoutedStubApi.response(403, forbidden),
                "GET " + DEPLOYMENTS_PATH, RoutedStubApi.response(403, forbidden),
                "POST " + DEPLOYMENTS_PATH, RoutedStubApi.response(403, forbidden)))) {

            List<CliException> cliExceptions = failuresOfEveryCommand(stub, environment);
            List<String> requiredPermissions = List.of("APP_VIEW", "APP_DEPLOY", "APP_VIEW", "APP_DEPLOY");

            for (int index = 0; index < cliExceptions.size(); index++) {
                String message = cliExceptions.get(index)
                    .getMessage();

                assertEquals(2, cliExceptions.get(index)
                    .exitCode());
                assertTrue(message.contains("role in " + environment), message);
                assertTrue(message.contains("needs the " + requiredPermissions.get(index) + " permission"), message);
                assertFalse(message.contains("DEVELOPMENT"), message);
            }
        }
    }

    @Test
    void testReadingTheAppToFindTheInstanceNamesTheEnvironmentAndAppViewOn403() throws Exception {
        try (RoutedStubApi stub = RoutedStubApi.start(
            Map.of(
                "GET " + APP_PATH,
                RoutedStubApi.response(403, "{\"code\":\"ACCESS_DENIED\",\"message\":\"Access Denied\"}")))) {

            CliException cliException = assertThrows(
                CliException.class,
                () -> command(ciDirectory()).deploymentList(
                    42L, null, null, null, stub.host(), "btc_x", "STAGING", "json"));

            assertEquals(2, cliException.exitCode());
            assertTrue(cliException.getMessage()
                .contains("role in STAGING"), cliException.getMessage());
            assertTrue(cliException.getMessage()
                .contains("APP_VIEW"), cliException.getMessage());
        }
    }

    private List<CliException> failuresOfEveryCommand(RoutedStubApi stub, String environment) throws IOException {
        Path ciDirectory = ciDirectory();

        return List.of(
            assertThrows(
                CliException.class,
                () -> command(ciDirectory).versionList(42L, null, null, null, stub.host(), "btc_x", environment,
                    "json")),
            assertThrows(
                CliException.class,
                () -> command(ciDirectory).versionCreate(
                    "1.1", null, null, null, 42L, null, stub.host(), "btc_x", environment, "json")),
            assertThrows(
                CliException.class,
                () -> command(ciDirectory).deploymentList(
                    42L, 7L, null, null, stub.host(), "btc_x", environment, "json")),
            assertThrows(
                CliException.class,
                () -> command(ciDirectory).deploymentRecord(
                    "1.1", null, null, 42L, 7L, null, stub.host(), "btc_x", environment, "json")));
    }

    private static int execute(String[] connection, String... arguments) {
        return CliApplication.execute(
            Stream.concat(Stream.of(arguments), Stream.of(connection))
                .toArray(String[]::new));
    }

    private static long countRequests(RoutedStubApi stub, String method, String path) {
        return stub.requests()
            .stream()
            .filter(request -> method.equals(request.method()) && path.equals(request.path()))
            .count();
    }
}
