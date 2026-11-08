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

package com.bytechef.cli.command.embedded;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "Deliberately nonexistent paths used only to exercise the missing-file guard; one literal "
        + "lives in a lambda body, which SpotBugs attributes to a synthetic method the test method's own "
        + "annotation does not cover.")
class EmbeddedCodeWorkflowCommandTest {

    private static final String DEPLOY_PATH = "/api/embedded/v1/automation-project-code-workflows/deploy";
    private static final String LIST_PATH = "/api/embedded/v1/automation-project-code-workflows";
    private static final String SECRET_TOKEN = "btc_secret_token_value";

    @TempDir
    Path tempDir;

    @Test
    void testDeployRejectsAMissingFile() {
        EmbeddedCodeWorkflowCommand command = new EmbeddedCodeWorkflowCommand();

        command.setConfigPath(Path.of("/nonexistent/config"));
        command.setEnvironmentVariables(Map.of());

        CliException exception = assertThrows(
            CliException.class,
            () -> command.codeWorkflowDeploy(
                "/nonexistent/project.js", false, "default", "http://localhost:8080", "token", "PRODUCTION"));

        assertEquals(1, exception.exitCode());
    }

    @Test
    void testDeployPostsTheProjectFileAndPrintsTheWarnings() throws Exception {
        Path projectFile = projectFile();

        try (StubApi stub = StubApi.start(200, "{\"warnings\":[\"Workflow orders has no trigger\"]}")) {
            CapturedRun capturedRun = capture(() -> CliApplication.execute(
                "embedded", "code-workflow", "deploy", "--file", projectFile.toString(), "--host", stub.host(),
                "--token", SECRET_TOKEN, "--environment", "STAGING"));

            assertEquals(0, capturedRun.exitCode());
            assertEquals(DEPLOY_PATH, stub.lastPath());
            assertEquals("Bearer " + SECRET_TOKEN, stub.lastAuthorization());
            assertEquals("STAGING", stub.lastEnvironment());
            assertTrue(stub.lastBody()
                .contains("filename=\"project.js\""), stub.lastBody());
            assertTrue(capturedRun.output()
                .contains("Project deployed."), capturedRun.output());
            assertTrue(capturedRun.output()
                .contains("WARNING: Workflow orders has no trigger"), capturedRun.output());
        }
    }

    @Test
    void testDeploySendsTheReleaseFlag() throws Exception {
        Path projectFile = projectFile();

        try (StubApi stub = StubApi.start(200, "{\"warnings\":[]}")) {
            int code = CliApplication.execute(
                "embedded", "code-workflow", "deploy", "--file", projectFile.toString(), "--release",
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(stub.lastBody()
                .contains("name=\"release\""), stub.lastBody());
            assertTrue(stub.lastBody()
                .contains("true"), stub.lastBody());
        }
    }

    @Test
    void testDeployUnauthorizedReturnsExitCode2WithoutLeakingTheToken() throws Exception {
        Path projectFile = projectFile();

        try (StubApi stub = StubApi.start(401, "{\"message\":\"Unauthorized\"}")) {
            CapturedRun capturedRun = capture(() -> CliApplication.execute(
                "embedded", "code-workflow", "deploy", "--file", projectFile.toString(), "--host", stub.host(),
                "--token", SECRET_TOKEN, "--environment", "PRODUCTION"));

            assertEquals(2, capturedRun.exitCode());
            assertTrue(capturedRun.output()
                .contains("Authentication failed (HTTP 401)."), capturedRun.output());
            assertFalse(capturedRun.output()
                .contains(SECRET_TOKEN), capturedRun.output());
        }
    }

    @Test
    void testListHitsTheCarvedOutSurfaceNotTheEmbeddedConnectedUserSurface() throws Exception {
        try (StubApi stub = StubApi.start(200, "[{\"name\":\"Orders\",\"kind\":\"REFERENCE\"}]")) {
            CapturedRun capturedRun = capture(() -> CliApplication.execute(
                "embedded", "code-workflow", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION"));

            assertEquals(0, capturedRun.exitCode());
            assertEquals(LIST_PATH, stub.lastPath());
            assertEquals("Bearer btc_x", stub.lastAuthorization());
            assertEquals("PRODUCTION", stub.lastEnvironment());
            assertTrue(capturedRun.output()
                .contains("Orders"), capturedRun.output());
        }
    }

    @Test
    void testListForbiddenReturnsExitCode2WithoutLeakingTheToken() throws Exception {
        try (StubApi stub = StubApi.start(403, "{\"message\":\"Forbidden\"}")) {
            CapturedRun capturedRun = capture(() -> CliApplication.execute(
                "embedded", "code-workflow", "list", "--host", stub.host(), "--token", SECRET_TOKEN, "--environment",
                "PRODUCTION"));

            assertEquals(2, capturedRun.exitCode());
            assertFalse(capturedRun.output()
                .contains(SECRET_TOKEN), capturedRun.output());
        }
    }

    @Test
    void testListNotFoundReturnsExitCode3() throws Exception {
        try (StubApi stub = StubApi.start(404, "{}")) {
            int code = CliApplication.execute(
                "embedded", "code-workflow", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION");

            assertEquals(3, code);
        }
    }

    @Test
    void testListServerErrorReturnsExitCode1() throws Exception {
        try (StubApi stub = StubApi.start(500, "{}")) {
            int code = CliApplication.execute(
                "embedded", "code-workflow", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION");

            assertEquals(1, code);
        }
    }

    private Path projectFile() throws IOException {
        Path projectFile = tempDir.resolve("project.js");

        Files.writeString(projectFile, "export default {name: 'orders'};");

        return projectFile;
    }

    private static CapturedRun capture(IntSupplier run) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        PrintStream capturingPrintStream = new PrintStream(byteArrayOutputStream, true, StandardCharsets.UTF_8);

        System.setOut(capturingPrintStream);
        System.setErr(capturingPrintStream);

        try {
            int exitCode = run.getAsInt();

            return new CapturedRun(exitCode, byteArrayOutputStream.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private record CapturedRun(int exitCode, String output) {
    }
}
