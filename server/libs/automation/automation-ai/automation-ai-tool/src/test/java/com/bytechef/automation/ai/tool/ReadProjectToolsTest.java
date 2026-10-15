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

package com.bytechef.automation.ai.tool;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class ReadProjectToolsTest {

    private final ProjectTools projectTools = mock(ProjectTools.class);
    private final ReadProjectTools readProjectTools = new ReadProjectTools(projectTools);
    private final ToolContext toolContext = new ToolContext(Map.of("key", "value"));

    @Test
    void testEveryToolPassesTheToolContextThrough() {
        readProjectTools.listProjects(1L, toolContext);
        readProjectTools.getProject(7L, toolContext);
        readProjectTools.searchProjects("alpha", 1L, toolContext);
        readProjectTools.getProjectStatus(7L, toolContext);

        verify(projectTools).listProjects(1L, toolContext);
        verify(projectTools).getProject(7L, toolContext);
        verify(projectTools).searchProjects("alpha", 1L, toolContext);
        verify(projectTools).getProjectStatus(7L, toolContext);
    }
}
