/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.dto;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;

/**
 * What moving connected users from one configuration to another would change, shown before the move is confirmed.
 *
 * @param workflows      the workflows of the target's version that need new required inputs or connections, with the
 *                       number of the moved users affected
 * @param skipped        the requested connected users the move would leave in place, with the reason
 * @param mcpServerCount the number of distinct MCP servers that expose the source or the target configuration
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public record ConfigurationMoveImpactDTO(
    List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> workflows,
    List<ConfigurationMoveResultDTO.Skipped> skipped, int mcpServerCount) {

    public ConfigurationMoveImpactDTO(
        List<AutomationWorkflowProjectConfigurationVersionImpactDTO.Workflow> workflows,
        List<ConfigurationMoveResultDTO.Skipped> skipped) {

        this(workflows, skipped, 0);
    }
}
