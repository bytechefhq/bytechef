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

package com.bytechef.platform.component.runner.external;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.platform.component.runner.TaskRunnerRequest;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
final class TaskRunnerOutputs {

    private static final String OUTPUT_JSON = "output.json";

    private TaskRunnerOutputs() {
    }

    @Nullable
    static Object readOutputJson(TaskRunnerWorkingDirectory workingDirectory) throws IOException {
        Path outputPath = workingDirectory.getOutputPath();

        if (!isRealDirectory(outputPath)) {
            return null;
        }

        Path outputJsonPath = outputPath.resolve(OUTPUT_JSON);

        if (!Files.isRegularFile(outputJsonPath, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }

        String json;

        try (InputStream inputStream = Files.newInputStream(outputJsonPath, LinkOption.NOFOLLOW_LINKS)) {
            json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        if (json.isBlank()) {
            return null;
        }

        return JsonUtils.read(json);
    }

    static Map<String, FileEntry> collectOutputFiles(
        TaskRunnerRequest request, TaskRunnerWorkingDirectory workingDirectory) throws IOException {

        List<String> patterns = request.outputFilePatterns();

        if (patterns.isEmpty()) {
            return Map.of();
        }

        List<PathMatcher> matchers = patterns.stream()
            .map(pattern -> FileSystems.getDefault()
                .getPathMatcher("glob:" + pattern))
            .toList();

        Path outputPath = workingDirectory.getOutputPath();

        if (!isRealDirectory(outputPath)) {
            return Map.of();
        }

        Map<String, FileEntry> outputFiles = new LinkedHashMap<>();

        try (Stream<Path> paths = Files.walk(outputPath)) {
            List<Path> candidates = paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .sorted()
                .toList();

            for (Path candidate : candidates) {
                Path relative = outputPath.relativize(candidate);

                if (OUTPUT_JSON.equals(relative.toString())) {
                    continue;
                }

                boolean matched = matchers.stream()
                    .anyMatch(matcher -> matcher.matches(relative));

                if (!matched) {
                    continue;
                }

                String relativeFileName = relative.toString();

                try (InputStream inputStream = Files.newInputStream(candidate, LinkOption.NOFOLLOW_LINKS)) {
                    FileEntry fileEntry = request.actionContext()
                        .file(file -> file.storeContent(relativeFileName, inputStream));

                    outputFiles.put(relativeFileName, fileEntry);
                }
            }
        }

        return outputFiles;
    }

    private static boolean isRealDirectory(Path path) {
        return Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
    }
}
