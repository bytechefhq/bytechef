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

package com.bytechef.platform.ai.agent.memory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Tools for managing persistent memory entries. Forked from {@code org.springaicommunity.agent.tools.AutoMemoryTools}
 * (commit 5548e80) and re-backed by a {@link MemoryResourceResolver} (content read/write) plus an
 * {@link AutoMemoryDirectoryOps} SPI (list/delete/rename/exists), so memory can live outside the filesystem. The
 * LLM-facing tool surface mirrors the Claude memory-tool spec: view, create, str_replace, insert, delete, rename.
 */
public class AutoMemoryTools {

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
        - Use an empty path, "/" or "MEMORY.md" to read the always-loaded index of all memory entries.
          Consult it before reading or writing any memory.
        - Otherwise path names a single memory entry; its contents are returned with line numbers.
        - Optionally supply a line range 'start,end' to page through large entries.
        """)
    public String memoryView(
        @ToolParam(description = "Memory entry path, or empty/'/'/'MEMORY.md' for the index.") String path,
        @ToolParam(description = "Optional line range 'start,end' (e.g. '1,50') when viewing an entry.",
            required = false) String viewRange,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return directoryOps.list(path == null ? "" : path, toolContext);
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: Path does not exist: " + path;
            }

            String content = read(path, toolContext);

            return formatFileView(path, content, viewRange);
        } catch (IOException exception) {
            return "Error reading path: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryCreate", description = """
        Create a new memory entry.

        Usage:
        - The entry must NOT already exist; use MemoryStrReplace to update an existing entry.
        - Provide the full entry content, including the YAML frontmatter block followed by the body.
        - After creating an entry, the index (MEMORY.md) updates automatically — you do not edit it by hand.
        - Always check the index (MemoryView on MEMORY.md) first to avoid duplicates.
        """)
    public String memoryCreate(
        @ToolParam(description = "Path for the new entry (e.g. 'user_profile.md').") String path,
        @ToolParam(description = "Full entry content: YAML frontmatter block then the body.") String fileText,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be created directly.";
            }

            if (directoryOps.exists(path, toolContext)) {
                return "Error: File already exists: " + path + ". Use MemoryStrReplace to modify existing files.";
            }

            String text = fileText != null ? fileText : "";

            write(path, text, toolContext);

            return "Successfully created file: " + path + " (" + text.length() + " bytes)";
        } catch (IOException exception) {
            return "Error creating file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryStrReplace", description = """
        Replace an exact string in an existing memory entry.

        Usage:
        - old_str must match exactly (including whitespace and newlines) and must appear exactly once.
        - If old_str appears more than once the edit is rejected — include more surrounding context.
        - new_str can be empty to delete the matched text.
        """)
    public String memoryStrReplace(
        @ToolParam(description = "Path of the entry to edit.") String path,
        @ToolParam(description = "Exact text to find; must appear exactly once.") String oldStr,
        @ToolParam(description = "Replacement text; empty to delete the matched text.") String newStr,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: File does not exist: " + path;
            }

            String content = read(path, toolContext);
            int occurrences = countOccurrences(content, oldStr);

            if (occurrences == 0) {
                return "Error: old_str not found in file: " + path;
            }

            if (occurrences > 1) {
                return String.format(
                    "Error: old_str appears %d times in the file. Provide more surrounding context to make it unique.",
                    occurrences);
            }

            String replacement = newStr != null ? newStr : "";
            String updated = replaceFirst(content, oldStr, replacement);

            write(path, updated, toolContext);

            if (!StringUtils.hasText(replacement)) {
                return String.format("Successfully deleted matched text from %s.", path);
            }

            return String.format(
                "Successfully edited %s. Here's a snippet of the result:%n%s", path,
                generateEditSnippet(updated, replacement));
        } catch (IOException exception) {
            return "Error editing file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryInsert", description = """
        Insert text at a specific line number in an existing memory entry.

        Usage:
        - insert_line is the line number AFTER which the new text is inserted (0 inserts at the beginning).
        - Lines are 1-indexed. Providing insert_line equal to the total line count appends to the end.
        """)
    public String memoryInsert(
        @ToolParam(description = "Path of the entry to modify.") String path,
        @ToolParam(description = "Line number after which to insert (0 = before first line).") Integer insertLine,
        @ToolParam(description = "Text to insert.") String insertText,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index is maintained automatically and cannot be edited directly.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: File does not exist: " + path;
            }

            if (insertLine == null || insertLine < 0) {
                return "Error: insert_line must be a non-negative integer";
            }

            String content = read(path, toolContext);
            boolean trailingNewline = content.endsWith("\n");

            List<String> lines = new ArrayList<>(List.of(content.isEmpty() ? new String[0] : content.split("\n", -1)));

            if (trailingNewline && !lines.isEmpty()) {
                lines.remove(lines.size() - 1);
            }

            if (insertLine > lines.size()) {
                return String.format("Error: insert_line %d exceeds file length of %d lines", insertLine, lines.size());
            }

            lines.add(insertLine, insertText != null ? insertText : "");

            String updated = String.join("\n", lines) + (trailingNewline ? "\n" : "");

            write(path, updated, toolContext);

            return "Successfully inserted text at line " + insertLine + " in: " + path;
        } catch (IOException exception) {
            return "Error inserting into file: " + exception.getMessage();
        } catch (RuntimeException exception) {
            return "Error: " + exception.getMessage();
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
        @ToolParam(description = "Path of the entry to delete.") String path,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(path)) {
                return "Error: The index cannot be deleted.";
            }

            if (!directoryOps.exists(path, toolContext)) {
                return "Error: Path does not exist: " + path;
            }

            directoryOps.delete(path, toolContext);

            return "Successfully deleted file: " + path;
        } catch (RuntimeException exception) {
            return "Error deleting path: " + exception.getMessage();
        }
    }

    // @formatter:off
    @Tool(name = "MemoryRename", description = """
        Rename a memory entry.

        Usage:
        - The source entry must exist; the destination must NOT already exist.
        - The index (MEMORY.md) updates automatically after the rename.
        """)
    public String memoryRename(
        @ToolParam(description = "Current path of the entry.") String oldPath,
        @ToolParam(description = "New path for the entry.") String newPath,
        ToolContext toolContext) { // @formatter:on

        try {
            if (isIndexOrRoot(oldPath) || isIndexOrRoot(newPath)) {
                return "Error: The index cannot be renamed.";
            }

            if (!directoryOps.exists(oldPath, toolContext)) {
                return "Error: Source path does not exist: " + oldPath;
            }

            if (directoryOps.exists(newPath, toolContext)) {
                return "Error: Destination path already exists: " + newPath;
            }

            directoryOps.rename(oldPath, newPath, toolContext);

            return String.format("Successfully renamed '%s' to '%s'", oldPath, newPath);
        } catch (RuntimeException exception) {
            return "Error renaming path: " + exception.getMessage();
        }
    }

    private String read(String path, ToolContext toolContext) throws IOException {
        try (InputStream inputStream = resourceResolver.resolve(path, toolContext)
            .getInputStream()) {

            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        }
    }

    private void write(String path, String content, ToolContext toolContext) throws IOException {
        WritableResource resource = resourceResolver.resolve(path, toolContext);

        try (OutputStream outputStream = resource.getOutputStream()) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static boolean isIndexOrRoot(String path) {
        return !StringUtils.hasText(path) || path.equals("/") || path.equals("MEMORY.md");
    }

    private static String formatFileView(String path, String content, String viewRange) {
        String[] allLines = content.split("\n", -1);
        int totalLines = allLines.length;

        int startLine = 1;
        int endLine = totalLines;

        if (StringUtils.hasText(viewRange)) {
            String[] parts = viewRange.split(",");

            if (parts.length != 2) {
                return "Error: view_range must be 'start,end' (e.g. '1,50')";
            }

            try {
                startLine = Math.max(1, Integer.parseInt(parts[0].trim()));
                endLine = Math.min(totalLines, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException exception) {
                return "Error: view_range must be 'start,end' integers (e.g. '1,50')";
            }
        }

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(String.format("File: %s%nLines %d-%d of %d%n%n", path, startLine, endLine, totalLines));

        for (int index = startLine - 1; index < endLine; index++) {
            stringBuilder.append(String.format("%6d\t%s%n", index + 1, allLines[index]));
        }

        return stringBuilder.toString();
    }

    private static int countOccurrences(String text, String substring) {
        if (!StringUtils.hasLength(substring)) {
            return 0;
        }

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

        for (int i = 0; i < lines.length; i++) {
            if (newLines.length > 0 && lines[i].contains(newLines[0])) {
                boolean matches = true;

                for (int j = 1; j < newLines.length && i + j < lines.length; j++) {
                    if (!lines[i + j].contains(newLines[j])) {
                        matches = false;

                        break;
                    }
                }

                if (matches) {
                    editStartLine = i;
                    editEndLine = i + newLines.length - 1;

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

        for (int i = startLine; i <= endLine; i++) {
            snippet.append(String.format("%6d→%s", i + 1, lines[i]));

            if (i < endLine) {
                snippet.append("\n");
            }
        }

        return snippet.toString();
    }
}
