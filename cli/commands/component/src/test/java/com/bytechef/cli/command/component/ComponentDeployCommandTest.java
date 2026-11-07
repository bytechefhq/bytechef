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

package com.bytechef.cli.command.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.cli.CliApplication;
import com.bytechef.cli.core.error.CliException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings(
    value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
    justification = "Deliberately nonexistent paths used only to exercise the missing-file guard.")
class ComponentDeployCommandTest {

    @Test
    void testDeployRejectsAMissingFile() {
        ComponentDeployCommand command = new ComponentDeployCommand();

        command.setConfigPath(Path.of("/nonexistent/config"));
        command.setEnvironmentVariables(Map.of());

        CliException exception = assertThrows(
            CliException.class,
            () -> command.componentDeploy(
                "/nonexistent/component.js", "default", "http://localhost:8080", "token", "PRODUCTION"));

        assertEquals(1, exception.exitCode());
    }

    @Test
    void testDeployPostsToThePlatformCustomComponentEndpoint() throws Exception {
        Path componentFile = Files.createTempFile("component", ".js");

        Files.writeString(componentFile, "export default {};");

        try (StubApi stub = StubApi.start(204, "")) {
            int code = CliApplication.execute(new String[] {
                "component", "deploy", "--file", componentFile.toString(), "--host", stub.host(), "--token",
                "btc_x", "--environment", "PRODUCTION"
            });

            assertEquals(0, code);
            assertTrue(
                stub.lastPath()
                    .startsWith("/api/platform/v1/custom-components/deploy"),
                "expected path /api/platform/v1/custom-components/deploy but was " + stub.lastPath());
        }
    }
}
