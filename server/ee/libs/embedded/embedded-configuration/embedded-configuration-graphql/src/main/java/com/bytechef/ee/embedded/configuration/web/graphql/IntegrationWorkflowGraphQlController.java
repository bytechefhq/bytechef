/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.IntegrationWorkflowFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class IntegrationWorkflowGraphQlController {

    private final IntegrationWorkflowFacade integrationWorkflowFacade;
    private final IntegrationWorkflowService integrationWorkflowService;

    @SuppressFBWarnings("EI")
    public IntegrationWorkflowGraphQlController(
        IntegrationWorkflowFacade integrationWorkflowFacade,
        IntegrationWorkflowService integrationWorkflowService) {

        this.integrationWorkflowFacade = integrationWorkflowFacade;
        this.integrationWorkflowService = integrationWorkflowService;
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> integrationWorkflows() {
        return integrationWorkflowFacade.getIntegrationWorkflows();
    }

    @QueryMapping
    List<IntegrationWorkflowDTO> integrationWorkflowsByIntegrationId(@Argument long integrationId) {
        return integrationWorkflowFacade.getIntegrationWorkflows(integrationId);
    }

    @SchemaMapping(typeName = "IntegrationWorkflow")
    List<String> workflowTaskComponentNames(IntegrationWorkflowDTO integrationWorkflowDTO) {
        return new ArrayList<>(
            integrationWorkflowDTO.getTasks()
                .stream()
                .map(workflowTask -> workflowTask.getType()
                    .split("/")[0])
                .collect(Collectors.toSet()));
    }

    @SchemaMapping(typeName = "IntegrationWorkflow")
    List<String> workflowTriggerComponentNames(IntegrationWorkflowDTO integrationWorkflowDTO) {
        List<String> componentNames = integrationWorkflowDTO.getTriggers()
            .stream()
            .map(workflowTrigger -> workflowTrigger.type()
                .split("/")[0])
            .toList();

        return componentNames.isEmpty() ? List.of("manual") : componentNames;
    }

    @MutationMapping
    public IntegrationWorkflowDTO updateIntegrationWorkflowPermissionExpression(
        @Argument long integrationWorkflowId, @Argument String permissionExpression) {

        integrationWorkflowService.updatePermissionExpression(integrationWorkflowId, permissionExpression);

        // Return the DTO (not the domain object) so the IntegrationWorkflow type's @SchemaMapping field resolvers,
        // which declare IntegrationWorkflowDTO as their source, can resolve the selected sub-fields.
        return integrationWorkflowFacade.getIntegrationWorkflow(integrationWorkflowId);
    }

    @SchemaMapping(typeName = "IntegrationWorkflow")
    String permissionExpression(IntegrationWorkflowDTO integrationWorkflowDTO) {
        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getIntegrationWorkflow(
            integrationWorkflowDTO.getIntegrationWorkflowId());

        return integrationWorkflow.getPermissionExpression();
    }
}
