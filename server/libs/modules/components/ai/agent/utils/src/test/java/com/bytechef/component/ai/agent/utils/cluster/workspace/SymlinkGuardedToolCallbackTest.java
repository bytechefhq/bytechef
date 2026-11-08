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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;

/**
 * @author Ivica Cardic
 */
@DisabledOnOs(OS.WINDOWS)
@ExtendWith(ObjectMapperSetupExtension.class)
class SymlinkGuardedToolCallbackTest {

    @TempDir
    private Path outsideDirectory;

    @TempDir
    private Path workspaceRoot;

    private final ToolCallback delegate = mock(ToolCallback.class);

    @BeforeEach
    void beforeEach() {
        when(delegate.call(anyString())).thenReturn("searched");
    }

    @Test
    void testSearchWithoutSymlinksIsDelegated() throws Exception {
        Files.writeString(Files.createDirectories(workspaceRoot.resolve("docs"))
            .resolve("notes.txt"), "content");

        assertThat(callTool(Map.of("pattern", "content"))).isEqualTo("searched");
    }

    @Test
    void testSearchWithASymlinkInsideTheWorkspaceIsDelegated() throws Exception {
        Path target = Files.writeString(workspaceRoot.resolve("notes.txt"), "content");

        Files.createSymbolicLink(workspaceRoot.resolve("link.txt"), target);

        assertThat(callTool(Map.of("pattern", "content"))).isEqualTo("searched");
    }

    @Test
    void testSearchWithASymlinkToAFileOutsideTheWorkspaceIsDenied() throws Exception {
        Path secret = Files.writeString(outsideDirectory.resolve("secret.txt"), "secret");

        Files.createSymbolicLink(Files.createDirectories(workspaceRoot.resolve("docs"))
            .resolve("link.txt"), secret);

        String result = callTool(Map.of("pattern", "secret", "path", workspaceRoot.toString()));

        assertThat(result).contains("Access denied")
            .contains("docs/link.txt");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testSearchWithASymlinkToADirectoryOutsideTheWorkspaceIsDenied() throws Exception {
        Files.createSymbolicLink(workspaceRoot.resolve("outside"), outsideDirectory);

        assertThat(callTool(Map.of("pattern", "secret"))).contains("Access denied");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testSearchWithADanglingSymlinkIsDenied() throws Exception {
        Files.createSymbolicLink(workspaceRoot.resolve("dangling.txt"), outsideDirectory.resolve("missing.txt"));

        assertThat(callTool(Map.of("pattern", "secret"))).contains("Access denied");

        verify(delegate, never()).call(anyString());
    }

    @Test
    void testSearchOfAFileIsDelegatedSoTheToolValidatesIt() throws Exception {
        Path secret = Files.writeString(outsideDirectory.resolve("secret.txt"), "secret");

        Files.createSymbolicLink(workspaceRoot.resolve("link.txt"), secret);

        assertThat(callTool(Map.of("pattern", "secret", "path", "link.txt"))).isEqualTo("searched");
    }

    @Test
    void testSearchOutsideTheWorkspaceIsDelegatedSoTheToolDeniesIt() {
        assertThat(callTool(Map.of("pattern", "secret", "path", outsideDirectory.toString()))).isEqualTo("searched");
    }

    private String callTool(Map<String, Object> arguments) {
        return new SymlinkGuardedToolCallback(delegate, "path", workspaceRoot).call(JsonUtils.write(arguments));
    }
}
