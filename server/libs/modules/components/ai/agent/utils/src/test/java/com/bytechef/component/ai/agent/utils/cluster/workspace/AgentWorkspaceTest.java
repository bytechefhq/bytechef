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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * @author Ivica Cardic
 */
class AgentWorkspaceTest {

    @TempDir
    private Path basedir;

    @Test
    void testSandboxedWorkspaceShowsTheSandboxRootToTheModel() {
        AgentWorkspace workspace = AgentWorkspace.sandboxed(basedir.resolve("default"));

        assertThat(workspace.modelRoot()).isEqualTo("/workspace");
        assertThat(workspace.root()).isEqualTo(basedir.resolve("default")
            .toAbsolutePath()
            .normalize());
    }

    @Test
    void testToHostPathMapsSandboxPathsIntoTheWorkspace() {
        Path root = basedir.resolve("default");

        AgentWorkspace workspace = AgentWorkspace.sandboxed(root);

        assertThat(workspace.toHostPath("/workspace")).isEqualTo(root.toString());
        assertThat(workspace.toHostPath("/workspace/src/Main.java")).isEqualTo(root.resolve("src/Main.java")
            .toString());
        assertThat(workspace.toHostPath("src/Main.java")).isEqualTo(root.resolve("src/Main.java")
            .toString());
    }

    @Test
    void testToHostPathKeepsOtherAbsolutePathsSoTheConfinementRejectsThem() {
        AgentWorkspace workspace = AgentWorkspace.sandboxed(basedir.resolve("default"));

        assertThat(workspace.toHostPath("/etc/passwd")).isEqualTo("/etc/passwd");
        assertThat(workspace.toHostPath("/workspace2/notes.txt")).isEqualTo("/workspace2/notes.txt");
    }

    @Test
    void testToHostPatternMapsOnlySandboxPatterns() {
        Path root = basedir.resolve("default");

        AgentWorkspace workspace = AgentWorkspace.sandboxed(root);

        assertThat(workspace.toHostPattern("**/*.java")).isEqualTo("**/*.java");
        assertThat(workspace.toHostPattern("/workspace/**/*.java")).isEqualTo(root.resolve("**/*.java")
            .toString());
    }

    @Test
    void testDisplayTextReplacesTheHostRootWithTheSandboxRoot() {
        Path root = basedir.resolve("default");

        AgentWorkspace workspace = AgentWorkspace.sandboxed(root);

        assertThat(workspace.displayText("Created " + root.resolve("notes.txt") + " in " + root))
            .isEqualTo("Created /workspace/notes.txt in /workspace");
    }

    @Test
    void testDisplayTextLeavesSiblingDirectoriesAlone() {
        Path root = basedir.resolve("default");

        AgentWorkspace workspace = AgentWorkspace.sandboxed(root);

        String siblingPath = basedir.resolve("default-copy/notes.txt")
            .toString();

        assertThat(workspace.displayText(siblingPath)).isEqualTo(siblingPath);
    }

    @Test
    void testWithHostPathsShowsHostPathsUnchanged() {
        Path root = basedir.resolve("default");

        AgentWorkspace workspace = AgentWorkspace.sandboxed(root)
            .withHostPaths();

        String text = "cwd " + root.resolve("notes.txt");

        assertThat(workspace.modelRoot()).isEqualTo(workspace.root()
            .toString());
        assertThat(workspace.displayText(text)).isEqualTo(text);
        assertThat(workspace.toHostPath("notes.txt")).isEqualTo(workspace.root()
            .resolve("notes.txt")
            .toString());
    }
}
