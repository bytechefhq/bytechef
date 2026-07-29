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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AutomationExecutionCommandTest {

    @Test
    void testExecutionListSendsAuthAndEnvironment() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "automation", "execution", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "STAGING", "--workspace-id", "1");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/workflow-executions"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("workspaceId=1"));
            assertEquals("Bearer btc_x", stub.lastAuthorization());
            assertEquals("STAGING", stub.lastEnvironment());
        }
    }

    @Test
    void testExecutionListForwardsDateAndDeploymentFilters() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"content\":[],\"totalElements\":0}")) {
            int code = CliApplication.execute(
                "automation", "execution", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION", "--workspace-id", "1", "--project-deployment-id", "8", "--start-date",
                "2026-01-01T00:00:00Z", "--end-date", "2026-02-01T00:00:00Z");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .contains("projectDeploymentId=8"),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("startDate="),
                stub.lastPath());
            assertTrue(
                stub.lastPath()
                    .contains("endDate="),
                stub.lastPath());
        }
    }

    @Test
    void testExecutionGetHitsByIdPath() throws Exception {
        try (StubApi stub = StubApi.start(200, "{\"id\":7,\"status\":\"COMPLETED\"}")) {
            int code = CliApplication.execute(
                "automation", "execution", "get", "--id", "7", "--host", stub.host(), "--token", "btc_x",
                "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/workflow-executions/7"),
                stub.lastPath());
        }
    }

    @Test
    void testUnauthorizedReturnsExitCode2() throws Exception {
        try (StubApi stub = StubApi.start(401, "")) {
            int code = CliApplication.execute(
                "automation", "execution", "get", "--id", "7", "--host", stub.host(), "--token", "bad",
                "--environment", "PRODUCTION");

            assertEquals(2, code);
        }
    }

    @Test
    void testExecutionListTableOutput() throws Exception {
        String body = "{\"content\":[{\"id\":7,\"status\":\"COMPLETED\"}],\"totalElements\":1}";

        try (StubApi stub = StubApi.start(200, body)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream original = System.out;

            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));

            try {
                CliApplication.execute(
                    "automation", "execution", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                    "PRODUCTION", "--workspace-id", "1", "--output", "table");
            } finally {
                System.setOut(original);
            }

            String rendered = out.toString(StandardCharsets.UTF_8);

            assertTrue(rendered.contains("COMPLETED"), rendered);
            assertTrue(rendered.contains("ID"), "table header expected");
        }
    }
}
