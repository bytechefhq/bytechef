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

package com.bytechef.component.ai.agent.utils.cluster.workspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public record AgentWorkspaceQuota(Path root, long maxBytes) {

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
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .mapToLong(AgentWorkspaceQuota::getSize)
                .sum();
        } catch (IOException | UncheckedIOException exception) {
            return 0;
        }
    }

    public String getFullMessage(long usedBytes, long requiredBytes) {
        return "Error: the agent workspace is full: %s of %s used, this write needs %s. Delete files you no longer need."
            .formatted(formatSize(usedBytes), formatSize(maxBytes), formatSize(requiredBytes));
    }

    public String getOverLimitMessage(long usedBytes) {
        return ("The agent workspace is over its limit (%s of %s). Until files are deleted, only commands that " +
            "start with rm, rmdir, truncate or find ... -delete are run.")
                .formatted(formatSize(usedBytes), formatSize(maxBytes));
    }

    public String getRefusedMessage(long usedBytes) {
        return ("Error: the agent workspace is over its limit (%s of %s), so the command was not run. Delete files " +
            "you no longer need with a command that starts with rm, rmdir, truncate or find ... -delete, then try " +
            "again.")
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

    private static long getSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
            return 0;
        }
    }
}
