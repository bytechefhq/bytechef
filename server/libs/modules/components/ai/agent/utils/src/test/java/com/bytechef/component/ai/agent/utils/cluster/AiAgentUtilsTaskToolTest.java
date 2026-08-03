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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsTaskToolTest {

    /**
     * Constructing {@code ClaudeSubagentType} is what pulls the library's own tool set — including ShellTools and
     * FileSystemTools — into every subagent, regardless of what the builder attached. Those capabilities remain
     * available as standalone cluster elements a builder may deliberately wire up; what must not come back is a task
     * tool that grants them without being asked.
     */
    @Test
    void testTaskToolNeverConstructsLibraryDefinedSubagents() throws IOException {
        String source = Files.readString(
            Path.of("src/main/java/com/bytechef/component/ai/agent/utils/cluster/AiAgentUtilsTaskTool.java"));

        assertThat(source).doesNotContain("ClaudeSubagentType");
        assertThat(source).doesNotContain("ShellTools");
        assertThat(source).doesNotContain("FileSystemTools");
    }
}
