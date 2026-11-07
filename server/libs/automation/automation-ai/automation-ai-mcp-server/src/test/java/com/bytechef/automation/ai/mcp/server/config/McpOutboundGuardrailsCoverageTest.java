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

package com.bytechef.automation.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class McpOutboundGuardrailsCoverageTest {

    private static final Path CONFIGURATION_SOURCE = Path.of(
        "src/main/java/com/bytechef/automation/ai/mcp/server/config/AutomationMcpServerConfiguration.java");

    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/|//[^\\r\\n]*", Pattern.DOTALL);

    private static final String SPECIFICATION_CALL = "toAsyncToolSpecification";

    private static final String GUARD_CALL = "guard(";

    @Test
    void testEveryToolSpecificationIsBuiltFromAGuardedCallback() throws IOException {
        assertTrue(
            Files.isRegularFile(CONFIGURATION_SOURCE),
            "Configuration source not found, working directory is wrong: " + CONFIGURATION_SOURCE);

        String source = COMMENT_PATTERN.matcher(Files.readString(CONFIGURATION_SOURCE))
            .replaceAll("");

        List<String> violations = new ArrayList<>();

        int siteCount = 0;

        for (String statement : source.split(";")) {
            if (!statement.contains(SPECIFICATION_CALL)) {
                continue;
            }

            siteCount++;

            if (!statement.contains(GUARD_CALL)) {
                violations.add(statement.strip());
            }
        }

        assertEquals(
            3, siteCount,
            "Expected 3 tool-specification sites in this configuration; the scan or the file has changed. Verify the "
                + "new site is guarded, then update this count deliberately.");

        if (!violations.isEmpty()) {
            fail(
                "Found " + violations.size() + " of " + siteCount + " tool-specification site(s) built from an "
                    + "unguarded ToolCallback - wrap the callback in guard(...) so its results are redacted before "
                    + "they reach the calling agent:\n" + String.join("\n---\n", violations));
        }
    }
}
