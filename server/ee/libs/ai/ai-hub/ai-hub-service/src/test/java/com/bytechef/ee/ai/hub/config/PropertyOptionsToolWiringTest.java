/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class PropertyOptionsToolWiringTest {

    @Test
    void testAskRegistersTheReadPickers() {
        AiHubModeToolCallbacks askToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .askToolCallbacks();

        assertThat(toolNames(askToolCallbacks))
            .contains(
                "listConnectionsForComponent", "lookupActionPropertyOptions", "lookupTriggerPropertyOptions",
                "lookupClusterElementPropertyOptions", "askUserQuestion")
            .doesNotContain(
                "selectConnection", "selectPropertyOption", "selectTriggerPropertyOption",
                "selectClusterElementPropertyOption", "createConnection", "lookupPropertyOptions");
    }

    @Test
    void testBuildRegistersEveryPicker() {
        AiHubModeToolCallbacks buildToolCallbacks = AiHubAgentFixtures.withBuildWorkflow()
            .buildToolCallbacks();

        assertThat(toolNames(buildToolCallbacks))
            .contains(
                "listConnectionsForComponent", "selectConnection", "lookupActionPropertyOptions",
                "lookupTriggerPropertyOptions", "lookupClusterElementPropertyOptions", "selectPropertyOption",
                "selectTriggerPropertyOption", "selectClusterElementPropertyOption", "askUserQuestion",
                "createConnection")
            .doesNotContain("lookupPropertyOptions");
    }

    private static List<String> toolNames(AiHubModeToolCallbacks modeToolCallbacks) {
        return modeToolCallbacks.toolCallbacks()
            .stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }
}
