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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class EmbeddedCommandTest {

    @Test
    void testIntegrationListSendsAuthAndPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "[]")) {
            int code = CliApplication.execute(
                "embedded", "integration", "list", "--external-user-id", "user-1", "--host", stub.host(), "--token",
                "btc_x", "--environment", "STAGING");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/integrations"),
                stub.lastPath());
            assertEquals("Bearer btc_x", stub.lastAuthorization());
            assertEquals("STAGING", stub.lastEnvironment());
        }
    }

    @Test
    void testWorkflowEnableHitsEnablePath() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "embedded", "workflow", "enable", "--external-user-id", "user-1", "--workflow-uuid", "wf-9", "--host",
                stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/automation/workflows/wf-9/enable"),
                stub.lastPath());
        }
    }

    @Test
    void testExecutionListHitsExecutionsPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "embedded", "execution", "list", "--external-user-id", "user-1", "--host", stub.host(), "--token",
                "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/workflow-executions"),
                stub.lastPath());
        }
    }

    @Test
    void testToolExecuteSendsBody() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"result\":\"ok\"}")) {
            int code = CliApplication.execute(
                "embedded", "tool", "execute", "--external-user-id", "user-1", "--data", "{\"name\":\"my_tool\"}",
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/tools"),
                stub.lastPath());
        }
    }

    @Test
    void testConnectionListHitsTemplatedExternalUserIdPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "[]")) {
            int code = CliApplication.execute(
                "embedded", "connection", "list", "--external-user-id", "user-1", "--component-name", "slack",
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/components/slack/connections"),
                stub.lastPath());
        }
    }

    @Test
    void testUnauthorizedReturnsExitCode2() throws Exception {
        try (StubApi stub = StubApi.start(401, "")) {
            int code = CliApplication.execute(
                "embedded", "integration", "get", "--external-user-id", "user-1", "--id", "5", "--host", stub.host(),
                "--token", "bad", "--environment", "PRODUCTION");

            assertEquals(2, code);
        }
    }

    @Test
    void testIntegrationListTableOutput() throws Exception {
        String body = "[{\"id\":1,\"name\":\"Slack\"},{\"id\":2,\"name\":\"GitHub\"}]";

        try (StubApi stub = StubApi.start(200, body)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            java.io.PrintStream original = System.out;

            System.setOut(new java.io.PrintStream(out, true, java.nio.charset.StandardCharsets.UTF_8));

            try {
                CliApplication.execute(
                    "embedded", "integration", "list", "--external-user-id", "user-1", "--output", "table", "--host",
                    stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");
            } finally {
                System.setOut(original);
            }

            String rendered = out.toString(java.nio.charset.StandardCharsets.UTF_8);

            assertTrue(rendered.contains("NAME"), rendered);
            assertTrue(rendered.contains("Slack"));
            assertTrue(rendered.contains("GitHub"));
        }
    }

    @Test
    void testInvalidDataJsonReturnsExitCode1() {
        int code = CliApplication.execute(
            "embedded", "tool", "execute", "--external-user-id", "user-1", "--data", "not-json", "--host",
            "https://h", "--token", "btc_x", "--environment", "PRODUCTION");

        assertEquals(1, code);
    }

    @Test
    void testDataFromFile(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) throws Exception {
        java.nio.file.Path dataFile = tempDir.resolve("tool.json");

        java.nio.file.Files.writeString(dataFile, "{\"name\":\"my_tool\"}");

        try (StubApi stub = StubApi.start(200, "{\"result\":\"ok\"}")) {
            int code = CliApplication.execute(
                "embedded", "tool", "execute", "--external-user-id", "user-1", "--data", "@" + dataFile, "--host",
                stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/user-1/tools"),
                stub.lastPath());
        }
    }

    @Test
    void testExecutionListForwardsFilters() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "embedded", "execution", "list", "--external-user-id", "user-1", "--status", "COMPLETED",
                "--start-date", "2026-01-01T00:00:00Z", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .contains("status=COMPLETED"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("startDate="),
                stub.lastPath());
        }
    }
}
