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

package com.bytechef.platform.ai.auto.memory;

import org.jspecify.annotations.Nullable;

/**
 * Renders an {@link com.bytechef.platform.ai.auto.memory.AiAutoMemory} as a frontmatter document and parses such a
 * document back into its fields. The on-the-wire form the LLM sees through the Memory* tools is:
 *
 * <pre>
 * ---
 * name: &lt;slug&gt;
 * title: &lt;title&gt;
 * description: &lt;description&gt;   (omitted when blank)
 * type: &lt;USER|FEEDBACK|PROJECT|REFERENCE&gt;
 * ---
 * &lt;body&gt;
 * </pre>
 *
 * @author Ivica Cardic
 */
public final class AutoMemoryFrontmatter {

    private static final String DELIMITER = "---";

    private AutoMemoryFrontmatter() {
    }

    public record Parsed(
        @Nullable String title, @Nullable String description, @Nullable AiAutoMemoryType memoryType, String content) {
    }

    public static String render(
        String name, String title, @Nullable String description, AiAutoMemoryType memoryType, String content) {

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(DELIMITER)
            .append("\n")
            .append("name: ")
            .append(name)
            .append("\n")
            .append("title: ")
            .append(title)
            .append("\n");

        if (description != null && !description.isBlank()) {
            stringBuilder.append("description: ")
                .append(description)
                .append("\n");
        }

        stringBuilder.append("type: ")
            .append(memoryType.name())
            .append("\n")
            .append(DELIMITER)
            .append("\n")
            .append(content);

        return stringBuilder.toString();
    }

    public static Parsed parse(String text) {
        String normalized = text == null ? "" : text;

        if (!normalized.stripLeading()
            .startsWith(DELIMITER)) {

            return new Parsed(null, null, null, normalized);
        }

        String afterFirst = normalized.stripLeading()
            .substring(DELIMITER.length());

        int closingIndex = afterFirst.indexOf("\n" + DELIMITER);

        if (closingIndex < 0) {
            return new Parsed(null, null, null, normalized);
        }

        String frontmatter = afterFirst.substring(0, closingIndex);
        String afterClosing = afterFirst.substring(closingIndex + ("\n" + DELIMITER).length());
        String content = afterClosing.startsWith("\n") ? afterClosing.substring(1) : afterClosing.stripLeading();

        String title = null;
        String description = null;
        AiAutoMemoryType memoryType = null;

        for (String line : frontmatter.split("\n")) {
            int separator = line.indexOf(':');

            if (separator < 0) {
                continue;
            }

            String key = line.substring(0, separator)
                .trim();
            String value = line.substring(separator + 1)
                .trim();

            switch (key) {
                case "title" -> title = value;
                case "description" -> description = value;
                case "type" -> memoryType = parseType(value);
                default -> {
                    // "name" is authoritative from the path, not the frontmatter; ignore other keys.
                }
            }
        }

        return new Parsed(title, description, memoryType, content);
    }

    @Nullable
    private static AiAutoMemoryType parseType(String value) {
        try {
            return AiAutoMemoryType.valueOf(value.trim()
                .toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
