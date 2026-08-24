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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "Deliberately nonexistent paths used only to exercise the missing-file guard; one literal "
        + "lives in a lambda body, which SpotBugs attributes to a synthetic method the test method's own "
        + "annotation does not cover.")
class EmbeddedCodeWorkflowCommandTest {

    @Test
    void testDeployRejectsAMissingFile() {
        EmbeddedCodeWorkflowCommand command = new EmbeddedCodeWorkflowCommand();

        command.setConfigPath(Path.of("/nonexistent/config"));
        command.setEnvironmentVariables(Map.of());

        CliException exception = assertThrows(
            CliException.class,
            () -> command.codeWorkflowDeploy(
                "/nonexistent/project.js", null, "default", "http://localhost:8080", "token", "PRODUCTION"));

        assertEquals(1, exception.exitCode());
    }

    @Test
    void testListHitsTheCarvedOutSurfaceNotTheEmbeddedConnectedUserSurface() throws Exception {
        try (StubApi stub = StubApi.start(200, "[]")) {
            int code = CliApplication.execute(new String[] {
                "embedded", "code-workflow", "list", "--host", stub.host(), "--token", "btc_x", "--environment",
                "PRODUCTION"
            });

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/embedded/v1/automation-project-code-workflows"),
                "expected path /api/embedded/v1/automation-project-code-workflows but was " + stub.lastPath());
        }
    }
}
