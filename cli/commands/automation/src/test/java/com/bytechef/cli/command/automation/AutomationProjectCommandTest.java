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
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AutomationProjectCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testProjectPullHitsGitPullPath() throws Exception {
        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "project", "pull", "--id", "5", "--host", stub.host(), "--token", "btc_x",
                "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/projects/5/git/pull"),
                stub.lastPath());
        }
    }

    @Test
    void testProjectDeployUploadsFile() throws Exception {
        Path projectFile = tempDir.resolve("project.zip");

        Files.writeString(projectFile, "dummy");

        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(
                "automation", "project", "deploy", "--workspace-id", "1", "--project-file", projectFile.toString(),
                "--host", stub.host(), "--token", "btc_x", "--environment", "PRODUCTION");

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/automation/v1/projects/deploy"),
                stub.lastPath());
        }
    }

    @Test
    void testProjectDeployMissingFileReturnsExitCode1() {
        int code = CliApplication.execute(
            "automation", "project", "deploy", "--workspace-id", "1", "--project-file", "/no/such/file.zip", "--host",
            "https://h", "--token", "btc_x", "--environment", "PRODUCTION");

        assertEquals(1, code);
    }
}
