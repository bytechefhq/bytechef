/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.ai.hub.task.AiHubTaskArtifactKind;
import org.junit.jupiter.api.Test;

/**
 * Pins the {@link AiHubPersonalAgentResourceKind#toArtifactKind()} mapping. The task-spawn copy path translates each
 * agent resource kind into the corresponding {@link AiHubTaskArtifactKind} reference kind; a wrong mapping would record
 * the resource under the wrong artifact kind and surface it incorrectly in the right-panel artifact list.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubPersonalAgentResourceKindTest {

    @Test
    void testToArtifactKindMapsEveryKind() {
        assertThat(AiHubPersonalAgentResourceKind.values()).hasSize(8);
        assertThat(AiHubPersonalAgentResourceKind.WORKFLOW.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.WORKFLOW_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.FILE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.FILE_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.DATA_TABLE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.DATA_TABLE_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.KNOWLEDGE_BASE.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.KB_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.MCP_SERVER.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.MCP_SERVER_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.API_COLLECTION.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.API_COLLECTION_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.WORKFLOW_EXECUTION.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.WORKFLOW_EXECUTION_REFERENCED);
        assertThat(AiHubPersonalAgentResourceKind.TASK.toArtifactKind())
            .isEqualTo(AiHubTaskArtifactKind.TASK_REFERENCED);
    }
}
