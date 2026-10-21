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

package com.bytechef.cli.command.automation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes the app's ByteChef values into an env file without touching its other lines, and tells whether git ignores the
 * file. The new content goes to an owner-only temporary file in the same directory that is then renamed over the env
 * file, so no reader ever sees a half-written file and the key is never readable by group or other.
 *
 * @author Ivica Cardic
 */
final class EnvFile {

    static final String BYTECHEF_APP_KEY = "BYTECHEF_APP_KEY";
    static final String BYTECHEF_URL = "BYTECHEF_URL";

    private static final Pattern ASSIGNMENT_PATTERN =
        Pattern.compile("^(\\s*(?:export\\s+)?)([A-Za-z_][A-Za-z0-9_]*)\\s*=");
    private static final Pattern LINE_TERMINATOR_PATTERN = Pattern.compile("\\r\\n|\\n|\\r");
    private static final long GIT_TIMEOUT_SECONDS = 10;
    private static final Set<PosixFilePermission> OWNER_ONLY = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    private EnvFile() {
    }

    enum GitIgnoreStatus {

        IGNORED, NOT_IGNORED, UNKNOWN
    }

    static List<String> merge(List<String> lines, Map<String, String> values) {
        List<String> mergedLines = new ArrayList<>();
        Set<String> replacedKeys = new HashSet<>();

        for (String line : lines) {
            Matcher matcher = ASSIGNMENT_PATTERN.matcher(line);

            if (matcher.find() && values.containsKey(matcher.group(2))) {
                String key = matcher.group(2);

                mergedLines.add(matcher.group(1) + key + "=" + values.get(key));
                replacedKeys.add(key);
            } else {
                mergedLines.add(line);
            }
        }

        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!replacedKeys.contains(entry.getKey())) {
                mergedLines.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        return mergedLines;
    }

    static void writeBytechefValues(Path envFile, String bytechefUrl, String appKey) throws IOException {
        Map<String, String> values = new LinkedHashMap<>();

        values.put(BYTECHEF_URL, requireSingleLineValue(BYTECHEF_URL, bytechefUrl));
        values.put(BYTECHEF_APP_KEY, requireSingleLineValue(BYTECHEF_APP_KEY, appKey));

        Path absoluteEnvFile = envFile.toAbsolutePath();
        Path directory = absoluteEnvFile.getParent();
        Path fileName = absoluteEnvFile.getFileName();

        if (directory == null || fileName == null) {
            throw new IOException("Not a file path: " + envFile);
        }

        if (Files.isSymbolicLink(absoluteEnvFile)) {
            throw new IOException(
                "Refusing to write through the symbolic link " + absoluteEnvFile +
                    ". Point --env-file at the real file or replace the link with a regular file.");
        }

        String originalContent = Files.isRegularFile(absoluteEnvFile)
            ? Files.readString(absoluteEnvFile, StandardCharsets.UTF_8) : "";
        String content = mergeContent(originalContent, values);
        Path temporaryFile = createOwnerOnlyTemporaryFile(directory, fileName.toString());

        try {
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            Files.move(
                temporaryFile, absoluteEnvFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    @SuppressFBWarnings(
        value = "COMMAND_INJECTION",
        justification = "Runs the fixed git executable with the env file path as a separate argument, never a shell.")
    static GitIgnoreStatus gitIgnoreStatus(Path envFile) {
        Path absoluteEnvFile = envFile.toAbsolutePath();
        Path directory = absoluteEnvFile.getParent();

        if (directory == null) {
            return GitIgnoreStatus.UNKNOWN;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            "git", "-C", directory.toString(), "check-ignore", "-q", "--", absoluteEnvFile.toString());

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        try {
            Process process = processBuilder.start();

            if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();

                return GitIgnoreStatus.UNKNOWN;
            }

            return switch (process.exitValue()) {
                case 0 -> GitIgnoreStatus.IGNORED;
                case 1 -> GitIgnoreStatus.NOT_IGNORED;
                default -> GitIgnoreStatus.UNKNOWN;
            };
        } catch (IOException exception) {
            return GitIgnoreStatus.UNKNOWN;
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();

            return GitIgnoreStatus.UNKNOWN;
        }
    }

    private static String requireSingleLineValue(String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(key + " must not be blank");
        }

        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(key + " must not contain control characters");
            }
        }

        return value;
    }

    private static String mergeContent(String content, Map<String, String> values) {
        List<String> lines = new ArrayList<>();
        List<String> terminators = new ArrayList<>();
        Matcher terminatorMatcher = LINE_TERMINATOR_PATTERN.matcher(content);
        int lineStart = 0;

        while (terminatorMatcher.find()) {
            lines.add(content.substring(lineStart, terminatorMatcher.start()));
            terminators.add(terminatorMatcher.group());

            lineStart = terminatorMatcher.end();
        }

        if (lineStart < content.length()) {
            lines.add(content.substring(lineStart));
            terminators.add("");
        }

        String newline = content.contains("\r\n") ? "\r\n" : "\n";
        int originalLineCount = lines.size();
        boolean endsWithNewline = originalLineCount == 0 || !terminators.get(originalLineCount - 1)
            .isEmpty();
        List<String> mergedLines = merge(lines, values);
        StringBuilder mergedContent = new StringBuilder();

        for (int index = 0; index < mergedLines.size(); index++) {
            mergedContent.append(mergedLines.get(index));

            boolean last = index == mergedLines.size() - 1;
            String terminator = index < originalLineCount ? terminators.get(index) : newline;

            if (!last) {
                mergedContent.append(terminator.isEmpty() ? newline : terminator);
            } else if (index < originalLineCount || endsWithNewline) {
                mergedContent.append(index < originalLineCount ? terminator : newline);
            }
        }

        return mergedContent.toString();
    }

    @SuppressFBWarnings(
        value = "PATH_TRAVERSAL_IN", justification = "The env file path is supplied by the CLI user on purpose.")
    private static Path createOwnerOnlyTemporaryFile(Path directory, String fileName) throws IOException {
        String prefix = "." + fileName + ".";
        FileSystem fileSystem = directory.getFileSystem();

        if (fileSystem.supportedFileAttributeViews()
            .contains("posix")) {

            return Files.createTempFile(directory, prefix, ".tmp", PosixFilePermissions.asFileAttribute(OWNER_ONLY));
        }

        return Files.createTempFile(directory, prefix, ".tmp");
    }
}
