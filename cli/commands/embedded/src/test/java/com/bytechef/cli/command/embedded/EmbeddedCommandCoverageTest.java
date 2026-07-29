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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Path coverage for every embedded command: each is driven end-to-end against a stub and its request path asserted.
 *
 * @author Ivica Cardic
 */
class EmbeddedCommandCoverageTest {

    private static final String PREFIX = "/api/embedded/v1";

    @Test
    void testIntegrationGet() throws Exception {
        assertPath("/user/integrations/3", 200, "{}", "embedded", "integration", "get", "--id", "3");
    }

    @Test
    void testIntegrationInstanceCreate() throws Exception {
        assertPath(
            "/user/integrations/3/instances", 200, "5", "embedded", "integration-instance", "create", "--id", "3",
            "--data", "{}");
    }

    @Test
    void testIntegrationInstanceDelete() throws Exception {
        assertPath(
            "/user/integration-instances/9", 204, "", "embedded", "integration-instance", "delete", "--id", "9");
    }

    @Test
    void testIntegrationInstanceWorkflowEnable() throws Exception {
        assertPath(
            "/user/integration-instances/9/workflows/wf-1/enable", 204, "", "embedded", "integration-instance",
            "workflow-enable", "--id", "9", "--workflow-uuid", "wf-1");
    }

    @Test
    void testIntegrationInstanceWorkflowDisable() throws Exception {
        assertPath(
            "/user/integration-instances/9/workflows/wf-1/enable", 204, "", "embedded", "integration-instance",
            "workflow-disable", "--id", "9", "--workflow-uuid", "wf-1");
    }

    @Test
    void testIntegrationInstanceWorkflowUpdate() throws Exception {
        assertPath(
            "/user/integration-instances/9/workflows/wf-1", 204, "", "embedded", "integration-instance",
            "workflow-update", "--id", "9", "--workflow-uuid", "wf-1", "--data", "{}");
    }

    @Test
    void testIntegrationInstanceInputOptions() throws Exception {
        assertPath(
            "/user/integration-instances/9/component-input-options", 200, "[]", "embedded", "integration-instance",
            "input-options", "--id", "9", "--data", "{}");
    }

    @Test
    void testProjectList() throws Exception {
        assertPath("/user/automation/projects", 200, "[]", "embedded", "project", "list");
    }

    @Test
    void testWorkflowGet() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1", 200, "{}", "embedded", "workflow", "get", "--workflow-uuid", "wf-1");
    }

    @Test
    void testWorkflowCreate() throws Exception {
        assertPath(
            "/user/automation/workflows", 200, "\"wf-1\"", "embedded", "workflow", "create", "--data", "{}");
    }

    @Test
    void testWorkflowUpdate() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1", 204, "", "embedded", "workflow", "update", "--workflow-uuid", "wf-1",
            "--data", "{}");
    }

    @Test
    void testWorkflowGenerate() throws Exception {
        assertPath(
            "/user/automation/workflows/generate", 200, "\"wf-1\"", "embedded", "workflow", "generate", "--data",
            "{\"prompt\":\"x\"}");
    }

    @Test
    void testWorkflowUpdateFromPrompt() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1/generate", 200, "\"wf-1\"", "embedded", "workflow", "update-from-prompt",
            "--workflow-uuid", "wf-1", "--data", "{\"prompt\":\"x\"}");
    }

    @Test
    void testWorkflowCopyTemplate() throws Exception {
        assertPath(
            "/user/automation/workflow-templates/wf-1/copy", 200, "\"wf-2\"", "embedded", "workflow", "copy-template",
            "--workflow-uuid", "wf-1");
    }

    @Test
    void testWorkflowPublish() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1/publish", 204, "", "embedded", "workflow", "publish", "--workflow-uuid",
            "wf-1");
    }

    @Test
    void testWorkflowDelete() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1", 204, "", "embedded", "workflow", "delete", "--workflow-uuid", "wf-1");
    }

    @Test
    void testWorkflowSetConnection() throws Exception {
        assertPath(
            "/user/automation/workflows/wf-1/workflow-nodes/node1/connections/conn1", 204, "", "embedded", "workflow",
            "set-connection", "--workflow-uuid", "wf-1", "--node-name", "node1", "--connection-key", "conn1", "--data",
            "{}");
    }

    @Test
    void testUserUpdate() throws Exception {
        assertPath("/user", 204, "", "embedded", "user", "update", "--data", "{}");
    }

    @Test
    void testActionExecute() throws Exception {
        assertPath(
            "/user/components/slack/versions/1/actions/sendMessage", 200, "{}", "embedded", "action", "execute",
            "--component-name", "slack", "--component-version", "1", "--action-name", "sendMessage", "--data", "{}");
    }

    @Test
    void testToolList() throws Exception {
        assertPath("/user/tools", 200, "{}", "embedded", "tool", "list");
    }

    @Test
    void testToolInvocationList() throws Exception {
        assertPath(
            "/user/tool-invocations", 200, "{\"content\":[],\"totalElements\":0}", "embedded", "tool-invocation",
            "list");
    }

    private void assertPath(String expectedPathSuffix, int status, String body, String... command) throws Exception {
        try (StubApi stub = StubApi.start(status, body)) {
            List<String> args = new ArrayList<>(List.of(command));

            args.add("--external-user-id");
            args.add("user");
            args.add("--host");
            args.add(stub.host());
            args.add("--token");
            args.add("btc_x");
            args.add("--environment");
            args.add("PRODUCTION");

            int code = CliApplication.execute(args.toArray(new String[0]));

            assertEquals(0, code, "exit code for " + String.join(" ", command));
            assertTrue(
                stub.lastPath()
                    .startsWith(PREFIX + expectedPathSuffix),
                "expected path " + PREFIX + expectedPathSuffix + " but was " + stub.lastPath());
        }
    }
}
