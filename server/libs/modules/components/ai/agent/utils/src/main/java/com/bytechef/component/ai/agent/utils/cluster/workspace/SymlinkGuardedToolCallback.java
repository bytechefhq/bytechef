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

import com.bytechef.commons.util.JsonUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @author Ivica Cardic
 */
public class SymlinkGuardedToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String pathArgumentName;
    private final Path workspaceRoot;

    public SymlinkGuardedToolCallback(ToolCallback delegate, String pathArgumentName, Path workspaceRoot) {
        this.delegate = delegate;
        this.pathArgumentName = pathArgumentName;
        this.workspaceRoot = workspaceRoot.toAbsolutePath()
            .normalize();
    }

    @Override
    public String call(String toolInput) {
        return findEscapingSymlink(toolInput)
            .map(SymlinkGuardedToolCallback::toAccessDeniedMessage)
            .orElseGet(() -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return findEscapingSymlink(toolInput)
            .map(SymlinkGuardedToolCallback::toAccessDeniedMessage)
            .orElseGet(() -> delegate.call(toolInput, toolContext));
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    private Optional<Path> findEscapingSymlink(String toolInput) {
        Path searchPath = resolveSearchPath(toolInput);

        if (!searchPath.startsWith(workspaceRoot) ||
            !Files.isDirectory(searchPath, LinkOption.NOFOLLOW_LINKS) ||
            !Files.isDirectory(workspaceRoot)) {

            return Optional.empty();
        }

        Path realWorkspaceRoot = toRealPath(workspaceRoot);

        if (realWorkspaceRoot == null) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.walk(searchPath)) {
            return paths.filter(Files::isSymbolicLink)
                .filter(symlink -> isOutside(symlink, realWorkspaceRoot))
                .findFirst();
        } catch (IOException | UncheckedIOException exception) {
            return Optional.of(searchPath);
        }
    }

    private Path resolveSearchPath(String toolInput) {
        Map<String, ?> arguments = JsonUtils.readMap(toolInput);

        if (!(arguments.get(pathArgumentName) instanceof String path) || path.isBlank()) {
            return workspaceRoot;
        }

        return workspaceRoot.resolve(path.strip())
            .toAbsolutePath()
            .normalize();
    }

    private static boolean isOutside(Path symlink, Path realWorkspaceRoot) {
        Path realTarget = toRealPath(symlink);

        return realTarget == null || !realTarget.startsWith(realWorkspaceRoot);
    }

    private static @Nullable Path toRealPath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException ioException) {
            return null;
        }
    }

    private static String toAccessDeniedMessage(Path symlink) {
        return "Error: Access denied. The search path contains a symbolic link that points outside the allowed " +
            "directories: " + symlink;
    }
}
