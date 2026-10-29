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
 * Pins that the hub agents take the shared Copilot picker list (spec D14): ASK gets the read view (connection list,
 * property-option lookups, questions), BUILD gets all eight pickers, and the hub's own kind-keyed
 * {@code lookupPropertyOptions} / {@code selectPropertyOption} pair from {@code 0_732} is not reintroduced.
 *
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
                "lookupClusterElementPropertyOptions", "askUserQuestion")
            .doesNotContain(
                "selectConnection", "selectPropertyOption", "selectTriggerPropertyOption",
                "selectClusterElementPropertyOption", "createConnection", "lookupPropertyOptions");
    }

    @Test
    void testBuildRegistersEveryPicker() throws AGUIException {
        AiHubSpringAIAgent buildAgent = AiHubAgentFixtures.withBuildWorkflow()
            .buildAgent();

        assertThat(toolNames(buildAgent))
            .contains(
                "listConnectionsForComponent", "selectConnection", "lookupActionPropertyOptions",
                "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions", "selectPropertyOption",
                "selectTriggerPropertyOption", "selectClusterElementPropertyOption", "askUserQuestion",
                "createConnection")
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
