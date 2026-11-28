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

package com.bytechef.ai.copilot.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ai.copilot.util.Source.WorkflowOwner;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class SourceTest {

    @Test
    void testFromIdResolvesTheLowerCaseWireId() {
        assertThat(Source.fromId("workflow_editor_embedded")).contains(Source.WORKFLOW_EDITOR_EMBEDDED);
        assertThat(Source.fromId("WORKFLOW_EDITOR")).isEmpty();
        assertThat(Source.fromId("no_such_source")).isEmpty();
    }

    @Test
    void testAgentIdCombinesTheSourceAndMode() {
        assertThat(Source.WORKFLOW_CODE_EDITOR.agentId(Mode.ASK)).isEqualTo("workflow_code_editor_ask");
        assertThat(Source.WORKFLOW_EDITOR_EMBEDDED.agentId(Mode.BUILD)).isEqualTo("workflow_editor_embedded_build");
    }

    @Test
    void testBuildOnlySourcesAlwaysResolveToBuild() {
        assertThat(Source.CONVERTER.resolveMode(Mode.ASK)).isEqualTo(Mode.BUILD);
        assertThat(Source.MCP_SERVER.resolveMode(null)).isEqualTo(Mode.BUILD);
        assertThat(Source.MCP_SERVER.agentId(Mode.ASK)).isEqualTo("mcp_server_build");
    }

    @Test
    void testResolveModeRequiresAModeForNonBuildOnlySources() {
        assertThat(Source.WORKFLOW_EDITOR.resolveMode(Mode.ASK)).isEqualTo(Mode.ASK);

        assertThatThrownBy(() -> Source.WORKFLOW_EDITOR.resolveMode(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testEmbeddedSourcesAreOwnedByTheIntegrationDomain() {
        assertThat(Source.values())
            .filteredOn(source -> source.name()
                .endsWith("_EMBEDDED"))
            .isNotEmpty()
            .allMatch(source -> source.workflowOwner() == WorkflowOwner.INTEGRATION);

        assertThat(Source.values())
            .filteredOn(source -> !source.name()
                .endsWith("_EMBEDDED"))
            .allMatch(source -> source.workflowOwner() == WorkflowOwner.PROJECT);
    }
}
