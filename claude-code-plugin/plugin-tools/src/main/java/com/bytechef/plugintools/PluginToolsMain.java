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

package com.bytechef.plugintools;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point for the plugin-tools generators, dispatched by {@code args[0]}.
 *
 * @author Ivica Cardic
 */
public final class PluginToolsMain {

    private static final String MCP_INSTRUCTIONS_COMMAND = "mcp-instructions";

    private PluginToolsMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            throw new IllegalArgumentException("Missing command; expected '" + MCP_INSTRUCTIONS_COMMAND + "'");
        }

        String command = args[0];

        if (MCP_INSTRUCTIONS_COMMAND.equals(command)) {
            if (args.length != 3) {
                throw new IllegalArgumentException(
                    "Usage: " + MCP_INSTRUCTIONS_COMMAND + " <skillsDirectory> <outputFile>");
            }

            McpInstructionFragmentGenerator.generate(Path.of(args[1]), Path.of(args[2]));

            return;
        }

        throw new IllegalArgumentException("Unknown command '" + command + "'");
    }
}
