/*
 * Copyright 2025-2026 the original author or authors.
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
 *
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.platform.ai.agent.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Tools for managing persistent memory entries. Forked from {@code org.springaicommunity.agent.tools.AutoMemoryTools}
 * (commit 5548e80) and re-backed by a {@link MemoryResourceResolver} (content read/write) plus an
 * {@link AutoMemoryDirectoryOps} SPI (list/delete/rename/exists), so memory can live outside the filesystem. The tool
 * surface is loosely modeled on the Claude memory-tool spec (view, create, str_replace, insert, delete, rename), split
 * into six tools with camelCase parameters.
 *
 * <p>
 * Failures the model can act on — an {@link IOException} (malformed entry, invalid name, duplicate or missing entry, or
 * an entry another writer changed meanwhile) or an {@link AutoMemoryUnavailableException} — are returned as
 * {@code Error: <message>} and logged at DEBUG. An {@link IllegalArgumentException} that reaches a tool is returned the
 * same way but logged at WARN, because it can also come from a bug below the validation layer; a resource that knows
 * its storage's validation failures (an empty body, an over-long field) reports them as {@link IOException}s instead,
 * so they stay at DEBUG. Any other exception is a bug or an infrastructure failure: it is logged at ERROR and the model
 * is told the operation was not applied and not to retry it, so internal details never reach the conversation.
 * </p>
 *
 * @author Christian Tzolov
 * @author Ivica Cardic
 */
public class AutoMemoryTools {

    private static final Logger log = LoggerFactory.getLogger(AutoMemoryTools.class);

    private static final String FRONTMATTER_DELIMITER = "---";

    private static final String INDEX_FILE_NAME = "MEMORY.md";

    private static final String UNEXPECTED_FAILURE_MESSAGE =
        "Error: the memory operation failed and was not applied. Do not retry it in this conversation.";

    private final MemoryResourceResolver resourceResolver;
    private final AutoMemoryDirectoryOps directoryOps;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AutoMemoryTools(MemoryResourceResolver resourceResolver, AutoMemoryDirectoryOps directoryOps) {
        Assert.notNull(resourceResolver, "resourceResolver must not be null");
        Assert.notNull(directoryOps, "directoryOps must not be null");

        this.resourceResolver = resourceResolver;
        this.directoryOps = directoryOps;
    }

    // @formatter:off
    @Tool(name = "MemoryView", description = """
        View a memory entry, or list the memory index.

        Usage:
        - Use an empty path, "/" or "MEMORY.md" to list the index of all memory entries: a header line with the
          entry count, then one line per entry, most recently updated first, formatted
          "- <name>.md — [TYPE] title — description" (without the description part when there is none). Check it
          before reading or writing any memory.
        - Otherwise path names a single memory entry; its contents, frontmatter included, are returned with line
          numbers.
        - Optionally supply viewRange 'start,end' to page through large entries. An end past the last line is clamped
          to it; a start past the last line is an error.
        """)
    public String memoryView(
        @ToolParam(description = "Memory entry path, or empty/'/'/'MEMORY.md' for the index.") String path,
        @ToolParam(description = "Optional line range 'start,end' (e.g. '1,50') when viewing an entry.",
            required = false) String viewRange) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return directoryOps.list(path == null ? "" : path);
            }

            if (!directoryOps.exists(path)) {
                return "Error: Path does not exist: " + path;
            }

            String content = read(path);

            return formatFileView(path, content, viewRange);
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryView", path, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryView", path, exception);
        }
    }

    // @formatter:off
    @Tool(name = "MemoryCreate", description = """
        Create a new memory entry.

        Usage:
        - path is a flat entry name: lowercase letters, digits, '-' and '_', at most 64 characters, optionally ending
          in '.md' (e.g. 'user_profile.md'). Directories are not supported, and 'memory' is reserved for the index.
        - The entry must NOT already exist; use MemoryStrReplace to update an existing entry.
        - fileText must start with a frontmatter block, followed by the body:
            ---
            title: <one line>
            description: <one line, optional>
            type: USER | FEEDBACK | PROJECT | REFERENCE
            ---
            <body>
          USER: facts about the user (role, preferences). FEEDBACK: corrections or approaches the user confirmed.
          PROJECT: decisions and constraints of the ongoing work. REFERENCE: pointers to external systems.
          A missing title defaults to the entry name and a missing type to PROJECT. A 'name:' line, as MemoryView
          shows, is optional; when present it must equal the entry name.
        - Limits: title at most 255 characters, description at most 1024, body at most 65,536.
        - Keep entries short and factual; the body must not be empty.
        - The index (MEMORY.md) updates automatically — you do not edit it by hand.
        - Check the index (MemoryView with path 'MEMORY.md') first to avoid duplicates.
        """)
    public String memoryCreate(
        @ToolParam(description = "Entry name, e.g. 'user_profile.md'.") String path,
        @ToolParam(description = "Full entry content: the frontmatter block, then the body.") String fileText) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be created directly.";
            }

            if (directoryOps.exists(path)) {
                return "Error: File already exists: " + path + ". Use MemoryStrReplace to modify existing files.";
            }

            String text = fileText != null ? fileText : "";

            write(path, text);

            return "Successfully created file: " + path + " (" + text.length() + " characters)";
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryCreate", path, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryCreate", path, exception);
        }
    }

    // @formatter:off
    @Tool(name = "MemoryStrReplace", description = """
        Replace an exact string in an existing memory entry.

        Usage:
        - oldStr must match exactly (including whitespace and newlines) and must appear exactly once.
        - If oldStr appears more than once the edit is rejected — include more surrounding context.
        - newStr can be empty to delete the matched text.
        - The entry must keep a valid frontmatter block (see MemoryCreate). Removing the description line clears the
          description; removing the title or type line keeps the stored value. Use MemoryRename to change the name.
        - The edit applies to the entry's current content, which may have changed since you last viewed it. If
          another writer changes the entry while this edit is being applied, the edit is rejected: view it again and
          redo the edit.
        """)
    public String memoryStrReplace(
        @ToolParam(description = "Path of the entry to edit.") String path,
        @ToolParam(description = "Exact text to find; must appear exactly once.") String oldStr,
        @ToolParam(description = "Replacement text; empty to delete the matched text.") String newStr) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!StringUtils.hasLength(oldStr)) {
                return "Error: oldStr must not be empty.";
            }

            if (!directoryOps.exists(path)) {
                return "Error: File does not exist: " + path;
            }

            // One resource for the read and the write, so the write can tell whether the entry changed in between.
            WritableResource resource = resourceResolver.resolve(path);

            String content = read(resource);
            int occurrences = countOccurrences(content, oldStr);

            if (occurrences == 0) {
                return "Error: oldStr not found in file: " + path;
            }

            if (occurrences > 1) {
                return String.format(
                    "Error: oldStr appears %d times in the file. Provide more surrounding context to make it unique.",
                    occurrences);
            }

            String replacement = newStr != null ? newStr : "";
            String updated = replaceFirst(content, oldStr, replacement);

            write(resource, updated);

            if (!StringUtils.hasText(replacement)) {
                return String.format("Successfully deleted matched text from %s.", path);
            }

            return String.format(
                "Successfully edited %s. Here's a snippet of the result:%n%s", path,
                generateEditSnippet(updated, replacement));
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryStrReplace", path, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryStrReplace", path, exception);
        }
    }

    // @formatter:off
    @Tool(name = "MemoryInsert", description = """
        Insert text at a specific line number in an existing memory entry.

        Usage:
        - insertLine is the line number AFTER which the new text is inserted (0 inserts before the first line).
        - Lines are 1-indexed and count the frontmatter block, which opens with '---' on line 1 and closes with the
          next '---' line. Insert body text after the closing '---' line. Text inserted before the opening '---' is
          rejected, and so is a '---' line inserted anywhere before the closing one. Inside the frontmatter the only
          field you can add is a 'description:' line when the entry has none; change existing fields with
          MemoryStrReplace.
        - Providing insertLine equal to the total line count appends to the end.
        - insertLine refers to the entry's current content, which may have changed since you last viewed it: view the
          entry right before inserting. If another writer changes the entry while this insert is being applied, the
          insert is rejected: view it again and redo it.
        """)
    public String memoryInsert(
        @ToolParam(description = "Path of the entry to modify.") String path,
        @ToolParam(description = "Line number after which to insert (0 = before first line).") Integer insertLine,
        @ToolParam(description = "Text to insert.") String insertText) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!directoryOps.exists(path)) {
                return "Error: File does not exist: " + path;
            }

            if (insertLine == null || insertLine < 0) {
                return "Error: insertLine must be a non-negative integer";
            }

            WritableResource resource = resourceResolver.resolve(path);

            String content = read(resource);
            boolean trailingNewline = content.endsWith("\n");

            List<String> lines = splitLines(content);

            if (insertLine > lines.size()) {
                return String.format("Error: insertLine %d exceeds file length of %d lines", insertLine, lines.size());
            }

            int frontmatterEnd = findFrontmatterEnd(lines);

            if (insertLine <= frontmatterEnd && containsDelimiterLine(insertText)) {
                return String.format(
                    "Error: a '---' line cannot be inserted before line %d, which closes the frontmatter. Insert body "
                        + "text after line %d.",
                    frontmatterEnd + 1, frontmatterEnd + 1);
            }

            lines.add(insertLine, insertText != null ? insertText : "");

            String updated = String.join("\n", lines) + (trailingNewline ? "\n" : "");

            write(resource, updated);

            return "Successfully inserted text at line " + insertLine + " in: " + path;
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryInsert", path, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryInsert", path, exception);
        }
    }

    // @formatter:off
    @Tool(name = "MemoryDelete", description = """
        Delete a memory entry.

        Usage:
        - This operation is irreversible; use with caution.
        - The index (MEMORY.md) updates automatically after deletion.
        - Use when a memory is confirmed stale, wrong, or superseded.
        """)
    public String memoryDelete(
        @ToolParam(description = "Path of the entry to delete.") String path) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index cannot be deleted.";
            }

            if (!directoryOps.exists(path)) {
                return "Error: Path does not exist: " + path;
            }

            directoryOps.delete(path);

            return "Successfully deleted file: " + path;
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryDelete", path, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryDelete", path, exception);
        }
    }

    // @formatter:off
    @Tool(name = "MemoryRename", description = """
        Rename a memory entry.

        Usage:
        - The source entry must exist; the destination must NOT already exist.
        - newPath follows the same naming rules as the path of MemoryCreate.
        - The index (MEMORY.md) updates automatically after the rename.
        """)
    public String memoryRename(
        @ToolParam(description = "Current path of the entry.") String oldPath,
        @ToolParam(description = "New path for the entry.") String newPath) { // @formatter:on

        try {
            if (isIndexOrRoot(oldPath) || isIndexOrRoot(newPath)) {
                return "Error: The index cannot be renamed.";
            }

            if (!directoryOps.exists(oldPath)) {
                return "Error: Source path does not exist: " + oldPath;
            }

            if (directoryOps.exists(newPath)) {
                return "Error: Destination path already exists: " + newPath;
            }

            directoryOps.rename(oldPath, newPath);

            return String.format("Successfully renamed '%s' to '%s'", oldPath, newPath);
        } catch (IOException | AutoMemoryUnavailableException exception) {
            return expectedFailure(exception);
        } catch (IllegalArgumentException exception) {
            return rejectedInput("MemoryRename", oldPath, exception);
        } catch (RuntimeException exception) {
            return unexpectedFailure("MemoryRename", oldPath, exception);
        }
    }

    private static String expectedFailure(Exception exception) {
        log.debug("Memory tool rejected an operation: {}", exception.getMessage(), exception);

        return toErrorMessage(exception);
    }

    private static String rejectedInput(String toolName, String path, IllegalArgumentException exception) {
        log.warn("Memory tool {} rejected the input for path '{}'", toolName, path, exception);

        return toErrorMessage(exception);
    }

    private static String toErrorMessage(Exception exception) {
        String message = exception.getMessage();

        return "Error: " + (StringUtils.hasText(message) ? message
            : exception.getClass()
                .getSimpleName());
    }

    private static String unexpectedFailure(String toolName, String path, RuntimeException exception) {
        log.error("Memory tool {} failed for path '{}'", toolName, path, exception);

        return UNEXPECTED_FAILURE_MESSAGE;
    }

    private String read(String path) throws IOException {
        return read(resourceResolver.resolve(path));
    }

    private static String read(WritableResource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void write(String path, String content) throws IOException {
        write(resourceResolver.resolve(path), content);
    }

    private static void write(WritableResource resource, String content) throws IOException {
        try (OutputStream outputStream = resource.getOutputStream()) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Whether {@code path} names the index: empty, only slashes, or {@code MEMORY.md} in any case, with surrounding
     * whitespace and leading slashes allowed. Other spellings that resolve to the reserved entry name {@code memory},
     * such as {@code "memory"}, are not the index: the resolver reports them as missing, or rejects creating them.
     */
    private static boolean isIndexOrRoot(String path) {
        if (!StringUtils.hasText(path)) {
            return true;
        }

        String trimmedPath = path.trim();

        while (trimmedPath.startsWith("/")) {
            trimmedPath = trimmedPath.substring(1);
        }

        return trimmedPath.isEmpty() || trimmedPath.equalsIgnoreCase(INDEX_FILE_NAME);
    }

    /**
     * The index of the line that closes the frontmatter block opened on the first line, or {@code -1} when the entry
     * does not open with one. Delimiters are matched ignoring surrounding whitespace, as the frontmatter parser does.
     */
    private static int findFrontmatterEnd(List<String> lines) {
        if (lines.isEmpty() || !isDelimiterLine(lines.getFirst())) {
            return -1;
        }

        for (int index = 1; index < lines.size(); index++) {
            if (isDelimiterLine(lines.get(index))) {
                return index;
            }
        }

        return -1;
    }

    private static boolean containsDelimiterLine(String text) {
        if (text == null) {
            return false;
        }

        for (String line : text.split("\\R", -1)) {
            if (isDelimiterLine(line)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isDelimiterLine(String line) {
        return FRONTMATTER_DELIMITER.equals(line.strip());
    }

    /**
     * The entry's lines as MemoryView numbers them and MemoryInsert counts them: a trailing newline ends the last line
     * rather than starting an empty one.
     */
    private static List<String> splitLines(String content) {
        if (content.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> lines = new ArrayList<>(List.of(content.split("\n", -1)));

        if (content.endsWith("\n")) {
            lines.removeLast();
        }

        return lines;
    }

    private static String formatFileView(String path, String content, String viewRange) {
        List<String> allLines = splitLines(content);
        int totalLines = allLines.size();

        int startLine = 1;
        int endLine = totalLines;

        if (StringUtils.hasText(viewRange)) {
            String[] parts = viewRange.split(",");

            if (parts.length != 2) {
                return "Error: viewRange must be 'start,end' (e.g. '1,50')";
            }

            try {
                startLine = Math.max(1, Integer.parseInt(parts[0].trim()));
                endLine = Math.min(totalLines, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException exception) {
                return "Error: viewRange must be 'start,end' integers (e.g. '1,50')";
            }

            if (startLine > totalLines) {
                return String.format(
                    "Error: viewRange starts at line %d, past the end of the entry (%d lines)", startLine, totalLines);
            }

            if (startLine > endLine) {
                return String.format("Error: viewRange start %d is after its end %d", startLine, endLine);
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("File: %s%nLines %d-%d of %d%n%n", path, startLine, endLine, totalLines));

        for (int index = startLine - 1; index < endLine; index++) {
            stringBuilder.append(String.format("%6d\t%s%n", index + 1, allLines.get(index)));
        }

        return stringBuilder.toString();
    }

    private static int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;

        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }

        return count;
    }

    private static String replaceFirst(String text, String oldStr, String newStr) {
        int index = text.indexOf(oldStr);

        if (index == -1) {
            return text;
        }

        return text.substring(0, index) + newStr + text.substring(index + oldStr.length());
    }

    private static String generateEditSnippet(String fileContent, String newStr) {
        String[] lines = fileContent.split("\n", -1);
        String[] newLines = newStr.split("\n", -1);

        int editStartLine = -1;
        int editEndLine = -1;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            if (newLines.length > 0 && lines[lineIndex].contains(newLines[0])) {
                boolean matches = true;

                for (int offset = 1; offset < newLines.length && lineIndex + offset < lines.length; offset++) {
                    if (!lines[lineIndex + offset].contains(newLines[offset])) {
                        matches = false;

                        break;
                    }
                }

                if (matches) {
                    editStartLine = lineIndex;
                    editEndLine = lineIndex + newLines.length - 1;

                    break;
                }
            }
        }

        if (editStartLine == -1) {
            editStartLine = 0;
            editEndLine = Math.min(10, lines.length - 1);
        }

        int startLine = Math.max(0, editStartLine - 5);
        int endLine = Math.min(lines.length - 1, editEndLine + 5);

        StringBuilder snippet = new StringBuilder();

        for (int lineIndex = startLine; lineIndex <= endLine; lineIndex++) {
            snippet.append(String.format("%6d→%s", lineIndex + 1, lines[lineIndex]));

            if (lineIndex < endLine) {
                snippet.append("\n");
            }
        }

        return snippet.toString();
    }
}
