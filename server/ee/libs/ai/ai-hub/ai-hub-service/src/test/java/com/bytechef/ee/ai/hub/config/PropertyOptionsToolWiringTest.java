/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.agui.core.exception.AGUIException;
import com.bytechef.ee.ai.hub.agent.AiHubSpringAIAgent;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyOptionsToolWiringTest {

    @Test
    void testAskRegistersTheReadPickers() throws AGUIException {
        AiHubSpringAIAgent askAgent = AiHubAgentFixtures.withBuildWorkflow()
            .askAgent();

        assertThat(toolNames(askAgent))
            .contains(
                "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions",
                "askUserQuestion")
            .doesNotContain(
                "selectConnection", "selectPropertyOption", "selectTriggerPropertyOption", "createConnection",
                "lookupPropertyOptions");
    }

    @Test
    void testBuildRegistersEveryPicker() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(toolNames(buildAgent))
            .contains(
                "listConnectionsForComponent", "selectConnection", "lookupActionPropertyOptions",
                "lookupTriggerPropertyOptions", "selectPropertyOption", "selectTriggerPropertyOption",
                "askUserQuestion", "createConnection")
            .doesNotContain("lookupPropertyOptions");
    }

    private static List<String> toolNames(AiHubSpringAIAgent agent) {
        return agent.pinnedToolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
