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
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.agent.common.workspace.Workspace;

/**
 * @author Ivica Cardic
 */
public final class AgentWorkspace implements Workspace {

    public static final String SANDBOX_ROOT = "/workspace";

    private final @Nullable Pattern hostRootPattern;
    private final String modelRoot;
    private final Path root;

    private AgentWorkspace(Path root, String modelRoot) {
        this.root = root.toAbsolutePath()
            .normalize();
        this.modelRoot = modelRoot;
        this.hostRootPattern = modelRoot.equals(this.root.toString()) ? null : compileHostRootPattern(this.root);
    }

    public static AgentWorkspace sandboxed(Path root) {
        return new AgentWorkspace(root, SANDBOX_ROOT);
    }

    public AgentWorkspace withHostPaths() {
        return new AgentWorkspace(root, root.toString());
    }

    public String displayText(String text) {
        if (hostRootPattern == null) {
            return text;
        }

        Matcher matcher = hostRootPattern.matcher(text);

        return matcher.replaceAll(Matcher.quoteReplacement(modelRoot));
    }

    @Override
    public String display(String hostPath) {
        return displayText(hostPath);
    }

    public String modelRoot() {
        return modelRoot;
    }

    @Override
    public Path root() {
        return root;
    }

    public String toHostPath(String modelPath) {
        String path = modelPath.strip();

        if (path.equals(modelRoot)) {
            return root.toString();
        }

        if (path.startsWith(modelRoot + "/")) {
            return root.resolve(path.substring(modelRoot.length() + 1))
                .toString();
        }

        if (Path.of(path)
            .isAbsolute()) {

            return path;
        }

        return root.resolve(path)
            .toString();
    }

    public String toHostPattern(String modelPattern) {
        String pattern = modelPattern.strip();

        if (pattern.equals(modelRoot) || pattern.startsWith(modelRoot + "/")) {
            return toHostPath(pattern);
        }

        return modelPattern;
    }

    private static Pattern compileHostRootPattern(Path root) {
        Set<String> hostRoots = new LinkedHashSet<>();

        hostRoots.add(root.toString());

        try {
            hostRoots.add(root.toRealPath()
                .toString());
        } catch (IOException ioException) {
            hostRoots.add(root.toString());
        }

        String alternatives = String.join(
            "|", hostRoots.stream()
                .sorted((first, second) -> second.length() - first.length())
                .map(Pattern::quote)
                .toList());

        return Pattern.compile("(?:" + alternatives + ")(?![A-Za-z0-9_.-])");
    }
}
