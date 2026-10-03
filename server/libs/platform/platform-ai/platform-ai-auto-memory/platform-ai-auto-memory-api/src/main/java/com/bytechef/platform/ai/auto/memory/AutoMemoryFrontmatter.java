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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Renders an {@link AiAutoMemory} as a frontmatter document and parses such a document back into its fields. The
 * on-the-wire form the LLM sees through the Memory* tools is:
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
 * <p>
 * Parsing is strict about content, lenient about layout: leading whitespace and padded {@code ---} delimiters are
 * accepted, but a document that does not open and close its frontmatter block, a line that is not {@code key: value},
 * an unknown or repeated key or an unknown type is rejected with an {@link IllegalArgumentException} whose message
 * tells the model how to fix the document. Silently falling back would let a single bad edit overwrite a memory's
 * stored metadata.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class AutoMemoryFrontmatter {

    private static final String DELIMITER = "---";

    private static final Pattern LINE_BREAK = Pattern.compile("\\R");

    private static final String EXPECTED_FORMAT =
        "Expected the entry to start with:\n---\ntitle: <one line>\ndescription: <one line, optional>\n"
            + "type: <" + typeNames() + ">\n---\nfollowed by the body.";

    private AutoMemoryFrontmatter() {
    }

    /**
     * The fields a document carries. A frontmatter field the document omits (or leaves blank) is {@code null};
     * {@code content} is the body, possibly empty.
     */
    public record Parsed(
        @Nullable String name, @Nullable String title, @Nullable String description,
        @Nullable AiAutoMemoryType memoryType, String content) {
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
            .append(toSingleLine(title))
            .append("\n");

        if (description != null && !description.isBlank()) {
            stringBuilder.append("description: ")
                .append(toSingleLine(description))
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

    public static Parsed parse(@Nullable String text) {
        String document = text == null ? "" : text;

        List<String> lines = Arrays.asList(document.stripLeading()
            .split("\n", -1));

        if (!isDelimiter(lines.getFirst())) {
            throw new IllegalArgumentException("The entry has no frontmatter block. " + EXPECTED_FORMAT);
        }

        int closingIndex = -1;

        for (int index = 1; index < lines.size(); index++) {
            if (isDelimiter(lines.get(index))) {
                closingIndex = index;

                break;
            }
        }

        if (closingIndex < 0) {
            throw new IllegalArgumentException(
                "The frontmatter block is not closed with a '---' line. " + EXPECTED_FORMAT);
        }

        String name = null;
        String title = null;
        String description = null;
        AiAutoMemoryType memoryType = null;
        Set<String> seenKeys = new HashSet<>();

        for (String line : lines.subList(1, closingIndex)) {
            if (line.isBlank()) {
                continue;
            }

            int separator = line.indexOf(':');

            if (separator < 0) {
                throw new IllegalArgumentException(
                    "Invalid frontmatter line '" + line.strip() + "': expected 'key: value'. " + EXPECTED_FORMAT);
            }

            String key = line.substring(0, separator)
                .trim();
            String value = blankToNull(line.substring(separator + 1));

            if (!seenKeys.add(key)) {
                throw new IllegalArgumentException(
                    "The frontmatter key '" + key + "' appears more than once; keep a single '" + key + ":' line.");
            }

            switch (key) {
                case "name" -> name = value;
                case "title" -> title = value;
                case "description" -> description = value;
                case "type" -> memoryType = value == null ? null : parseType(value);
                default -> throw new IllegalArgumentException(
                    "Unknown frontmatter key '" + key + "'; allowed keys are name, title, description and type.");
            }
        }

        String content = String.join("\n", lines.subList(closingIndex + 1, lines.size()));

        return new Parsed(name, title, description, memoryType, content);
    }

    /**
     * Title and description are single-line, but a value stored before that rule was enforced may still hold a line
     * break; rendered as-is it would end the field early and turn the rest into frontmatter lines of its own, which the
     * next edit would parse back wrongly. Rendered on one line instead, the next edit also repairs the stored value.
     */
    private static String toSingleLine(String value) {
        return LINE_BREAK.matcher(value)
            .replaceAll(" ");
    }

    private static boolean isDelimiter(String line) {
        return line.strip()
            .equals(DELIMITER);
    }

    private static @Nullable String blankToNull(String value) {
        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }

    private static AiAutoMemoryType parseType(String value) {
        try {
            return AiAutoMemoryType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "Unknown memory type '" + value + "'; expected one of " + typeNames() + ".", exception);
        }
    }

    private static String typeNames() {
        return Arrays.stream(AiAutoMemoryType.values())
            .map(Enum::name)
            .collect(Collectors.joining("|"));
    }
}
