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

package com.bytechef.platform.ai.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public record AgentWorkspaceQuota(Path root, long maxBytes) {

    private static final Pattern CLEANUP_COMMAND_PATTERN = Pattern.compile(
        "^\\s*(rm|rmdir)\\s.*|^\\s*truncate\\s+(.*\\s)?(-s\\s*0|--size(=|\\s+)0)(\\s.*)?$|" +
            "^\\s*find\\s.*\\s-delete(\\s.*)?$");
    private static final Pattern FIND_ACTION_PATTERN = Pattern.compile(
        "\\s-(exec|execdir|ok|okdir|fls|fprint|fprint0|fprintf)(\\s|$)");
    private static final Pattern SHELL_CONTROL_PATTERN = Pattern.compile("[;&|<>`\\n\\r]|\\$\\(");
    private static final String[] UNITS = {
        "bytes", "KB", "MB", "GB", "TB"
    };

    public boolean isLimited() {
        return maxBytes > 0;
    }

    public boolean isExceeded() {
        return isLimited() && getUsedBytes() > maxBytes;
    }

    public long getUsedBytes() {
        return getDirectorySize(root);
    }

    public static long getDirectorySize(Path directory) {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .mapToLong(AgentWorkspaceQuota::getSize)
                .sum();
        } catch (IOException | UncheckedIOException exception) {
            return 0;
        }
    }

    public static boolean isCleanupCommand(@Nullable String command) {
        if (command == null || containsMatch(SHELL_CONTROL_PATTERN, command) ||
            containsMatch(FIND_ACTION_PATTERN, command)) {

            return false;
        }

        Matcher matcher = CLEANUP_COMMAND_PATTERN.matcher(command);

        return matcher.matches();
    }

    public @Nullable String checkWrite(long requiredBytes) {
        if (!isLimited() || requiredBytes <= 0) {
            return null;
        }

        long usedBytes = getUsedBytes();

        if (usedBytes + requiredBytes <= maxBytes) {
            return null;
        }

        return getFullMessage(usedBytes, requiredBytes);
    }

    public long getEditRequiredBytes(
        @Nullable Path file, @Nullable String oldString, @Nullable String newString, boolean replaceAll) {

        long growth = getByteCount(newString) - getByteCount(oldString);

        if (growth <= 0) {
            return 0;
        }

        if (replaceAll) {
            return growth * countOccurrences(file, oldString);
        }

        return growth;
    }

    public long getWriteRequiredBytes(@Nullable Path file, @Nullable String content) {
        return getByteCount(content) - getFileSize(file);
    }

    public String getFullMessage(long usedBytes, long requiredBytes) {
        return "Error: the agent workspace is full: %s of %s used, this write needs %s. Delete files you no longer need."
            .formatted(formatSize(usedBytes), formatSize(maxBytes), formatSize(requiredBytes));
    }

    public String getOverLimitMessage(long usedBytes) {
        return ("The agent workspace is over its limit (%s of %s). Until files are deleted, only single " +
            "commands that start with rm, rmdir, truncate -s 0 or find ... -delete are run.")
                .formatted(formatSize(usedBytes), formatSize(maxBytes));
    }

    public String getRefusedMessage(long usedBytes) {
        return ("Error: the agent workspace is over its limit (%s of %s), so the command was not run. Delete files " +
            "you no longer need with a single command that starts with rm, rmdir, truncate -s 0 or find ... -delete, " +
            "then try again.")
                .formatted(formatSize(usedBytes), formatSize(maxBytes));
    }

    static String formatSize(long bytes) {
        double size = bytes;
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return bytes + " " + UNITS[0];
        }

        return String.format(Locale.ROOT, "%.1f %s", size, UNITS[unitIndex]);
    }

    private static boolean containsMatch(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);

        return matcher.find();
    }

    private static long countOccurrences(@Nullable Path file, @Nullable String oldString) {
        if (file == null || oldString == null || oldString.isEmpty()) {
            return 1;
        }

        try {
            String fileContent = Files.readString(file, StandardCharsets.UTF_8);

            long count = 0;
            int index = fileContent.indexOf(oldString);

            while (index >= 0) {
                count++;

                index = fileContent.indexOf(oldString, index + oldString.length());
            }

            return Math.max(count, 1);
        } catch (IOException | RuntimeException exception) {
            return 1;
        }
    }

    private static long getByteCount(@Nullable String text) {
        if (text == null) {
            return 0;
        }

        return text.getBytes(StandardCharsets.UTF_8).length;
    }

    private static long getFileSize(@Nullable Path file) {
        if (file == null || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        try {
            return Files.size(file);
        } catch (IOException ioException) {
            return 0;
        }
    }

    private static long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
            return 0;
        }
    }
}
