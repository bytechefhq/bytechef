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

package com.bytechef.ai.mcp.server.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class CliCommandInventoryTest {

    @Test
    void testCommandOptionsReadsTheLongNamesOfEachCommandMethod() {
        String source = """
            class SampleCommand {

                @Command(name = "sample one", description = "First (with parentheses).")
                public void one(
                    @Option(longName = "alpha", required = true) String alpha,
                    @Option(longName = "beta-gamma", defaultValue = "json") String betaGamma) {

                    helper("not-an-option");
                }

                @Command(
                    name = "sample two",
                    description = "Second.")
                public void two(
                    @Option(
                        longName = "delta", shortName = 'd', description = "uses (parentheses) and \\"quotes\\"",
                        defaultValue = "x") String delta)
                    throws Exception {

                    @Option(longName = "outside-of-the-signature")
                    String ignored = null;
                }

                @Command(name = "sample three")
                public void three() {
                }
            }
            """;

        assertEquals(
            Map.of(
                "sample one", Set.of("alpha", "beta-gamma"), "sample two", Set.of("delta"), "sample three",
                Set.of()),
            CliCommandInventory.commandOptions(source));
    }

    @Test
    void testCommandOptionsOfTheRealCliIncludeTheSharedConnectionOptions() throws IOException {
        Map<String, Set<String>> commandOptions = CliCommandInventory.commandOptions(
            CliCommandInventory.repositoryRoot());

        assertTrue(commandOptions.get("automation app create")
            .containsAll(Set.of("name", "profile", "host", "token", "environment", "workspace-id", "output")));
        assertEquals(CliCommandInventory.commandNames(CliCommandInventory.repositoryRoot()), commandOptions.keySet());
    }
}
