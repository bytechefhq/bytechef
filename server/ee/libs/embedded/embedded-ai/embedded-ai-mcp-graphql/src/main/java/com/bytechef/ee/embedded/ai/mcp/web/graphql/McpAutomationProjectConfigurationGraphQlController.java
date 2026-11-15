/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.web.graphql;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationWorkflowConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationProjectConfigurationFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnCoordinator
class McpAutomationProjectConfigurationGraphQlController {

    private final McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade;

    @SuppressFBWarnings("EI")
    McpAutomationProjectConfigurationGraphQlController(
        McpAutomationProjectConfigurationFacade mcpAutomationProjectConfigurationFacade) {

        this.mcpAutomationProjectConfigurationFacade = mcpAutomationProjectConfigurationFacade;
    }

    @MutationMapping
    McpAutomationProjectConfigurationDTO createMcpAutomationProjectConfiguration(
        @Argument CreateMcpAutomationProjectConfigurationInput input) {

        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            mcpAutomationProjectConfigurationFacade.createMcpAutomationProjectConfiguration(
                input.mcpServerId(), input.automationProjectConfigurationId(), input.workflowUuids(),
                toWorkflowConfigurations(input.workflowConfigurations()));

        return mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(
            mcpAutomationProjectConfiguration.getId());
    }

    @MutationMapping
    boolean deleteMcpAutomationProjectConfiguration(@Argument long id) {
        mcpAutomationProjectConfigurationFacade.deleteMcpAutomationProjectConfiguration(id);

        return true;
    }

    @MutationMapping
    McpAutomationProjectConfigurationDTO updateMcpAutomationProjectConfiguration(
        @Argument long id, @Argument UpdateMcpAutomationProjectConfigurationInput input) {

        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfiguration(
                id, input.workflowUuids(), toWorkflowConfigurations(input.workflowConfigurations()));

        return mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(
            mcpAutomationProjectConfiguration.getId());
    }

    @MutationMapping
    McpAutomationProjectConfigurationWorkflowDTO updateMcpAutomationProjectConfigurationWorkflowEnabled(
        @Argument long id, @Argument boolean enabled) {

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowEnabled(
                id, enabled);

        return getWorkflow(mcpAutomationProjectConfigurationWorkflow);
    }

    @MutationMapping
    McpAutomationProjectConfigurationWorkflowDTO updateMcpAutomationProjectConfigurationWorkflowParameters(
        @Argument long id, @Argument Map<String, Object> parameters) {

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            mcpAutomationProjectConfigurationFacade.updateMcpAutomationProjectConfigurationWorkflowParameters(
                id, parameters);

        return getWorkflow(mcpAutomationProjectConfigurationWorkflow);
    }

    @QueryMapping
    List<McpAutomationProjectConfigurationDTO> mcpAutomationProjectConfigurationsByServerId(
        @Argument long mcpServerId) {

        return mcpAutomationProjectConfigurationFacade.getMcpServerMcpAutomationProjectConfigurations(mcpServerId);
    }

    @QueryMapping
    List<String> mcpAutomationProjectConfigurationToolWorkflowUuids(@Argument long automationProjectConfigurationId) {
        return mcpAutomationProjectConfigurationFacade.getToolWorkflowUuids(automationProjectConfigurationId);
    }

    @QueryMapping
    List<Property> mcpAutomationProjectConfigurationWorkflowProperties(
        @Argument long mcpAutomationProjectConfigurationWorkflowId) {

        Optional<Workflow> workflowOptional =
            mcpAutomationProjectConfigurationFacade.fetchMcpAutomationProjectConfigurationWorkflowPinnedWorkflow(
                mcpAutomationProjectConfigurationWorkflowId);

        if (workflowOptional.isEmpty()) {
            return List.of();
        }

        return McpToolWorkflowProperties.getToolProperties(workflowOptional.get());
    }

    private McpAutomationProjectConfigurationWorkflowDTO getWorkflow(
        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow) {

        McpAutomationProjectConfigurationDTO mcpAutomationProjectConfigurationDTO =
            mcpAutomationProjectConfigurationFacade.getMcpAutomationProjectConfiguration(
                mcpAutomationProjectConfigurationWorkflow.getMcpAutomationProjectConfigurationId());

        for (McpAutomationProjectConfigurationWorkflowDTO workflowDTO : mcpAutomationProjectConfigurationDTO
            .workflows()) {

            if (workflowDTO.id() == mcpAutomationProjectConfigurationWorkflow.getId()) {
                return workflowDTO;
            }
        }

        throw new IllegalStateException(
            "McpAutomationProjectConfigurationWorkflow not found: " +
                mcpAutomationProjectConfigurationWorkflow.getId());
    }

    private static List<McpAutomationWorkflowConfigurationDTO> toWorkflowConfigurations(
        List<McpAutomationWorkflowConfigurationInput> workflowConfigurationInputs) {

        if (workflowConfigurationInputs == null) {
            return List.of();
        }

        return workflowConfigurationInputs.stream()
            .map(
                workflowConfigurationInput -> new McpAutomationWorkflowConfigurationDTO(
                    workflowConfigurationInput.workflowUuid(),
                    workflowConfigurationInput.parameters() == null
                        ? Map.of() : workflowConfigurationInput.parameters()))
            .toList();
    }

    @SuppressFBWarnings("EI")
    record CreateMcpAutomationProjectConfigurationInput(
        long mcpServerId, long automationProjectConfigurationId, List<String> workflowUuids,
        List<McpAutomationWorkflowConfigurationInput> workflowConfigurations) {
    }

    @SuppressFBWarnings("EI")
    record McpAutomationWorkflowConfigurationInput(String workflowUuid, Map<String, Object> parameters) {
    }

    @SuppressFBWarnings("EI")
    record UpdateMcpAutomationProjectConfigurationInput(
        List<String> workflowUuids, List<McpAutomationWorkflowConfigurationInput> workflowConfigurations) {
    }
}
