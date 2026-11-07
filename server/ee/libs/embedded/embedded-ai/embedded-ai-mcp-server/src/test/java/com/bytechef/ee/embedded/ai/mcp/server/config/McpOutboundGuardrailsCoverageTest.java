/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

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
 * @version ee
 * @author Ivica Cardic
 */
class McpOutboundGuardrailsCoverageTest {

    private static final Path CONFIGURATION_SOURCE = Path.of(
        "src/main/java/com/bytechef/ee/embedded/ai/mcp/server/config/EmbeddedMcpServerConfiguration.java");

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
            2, siteCount,
            "Expected 2 tool-specification sites in this configuration; the scan or the file has changed. Verify the "
                + "new site is guarded, then update this count deliberately.");

        if (!violations.isEmpty()) {
            fail(
                "Found " + violations.size() + " of " + siteCount + " tool-specification site(s) built from an "
                    + "unguarded ToolCallback - wrap the callback in guard(...) so its results are redacted before "
                    + "they reach the calling agent:\n" + String.join("\n---\n", violations));
        }
    }
}
