/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationFacade;
import com.bytechef.ee.embedded.configuration.public_.web.rest.converter.CaseInsensitiveEnumPropertyEditorSupport;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpIntegrationInstanceToolModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.McpToolModel;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.IntegrationApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
public class IntegrationApiController implements IntegrationApi {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ConversionService conversionService;
    private final ConnectedUserIntegrationFacade connectedUserIntegrationFacade;
    private final EnvironmentService environmentService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpComponentService mcpComponentService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public IntegrationApiController(
        ClusterElementDefinitionService clusterElementDefinitionService, ConversionService conversionService,
        ConnectedUserIntegrationFacade connectedUserIntegrationFacade, EnvironmentService environmentService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService, McpComponentService mcpComponentService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpServerService mcpServerService, McpToolService mcpToolService, WorkflowService workflowService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.conversionService = conversionService;
        this.connectedUserIntegrationFacade = connectedUserIntegrationFacade;
        this.environmentService = environmentService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpComponentService = mcpComponentService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.workflowService = workflowService;
    }

    @CrossOrigin
    @Override
    public ResponseEntity<IntegrationModel> getFrontendIntegration(Long id, EnvironmentModel xEnvironment) {
        String externalId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO =
            connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalId, id, true, getEnvironment(xEnvironment));

        IntegrationModel integrationModel = conversionService.convert(
            connectedUserIntegrationDTO, IntegrationModel.class);

        populateMcpData(connectedUserIntegrationDTO, integrationModel);
        filterDisabledWorkflows(connectedUserIntegrationDTO, integrationModel);

        return ResponseEntity.ok(integrationModel);
    }

    @CrossOrigin
    @Override
    public ResponseEntity<List<IntegrationBasicModel>> getFrontendIntegrations(EnvironmentModel xEnvironment) {
        String externalId = SecurityUtils.fetchCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        return ResponseEntity.ok(
            connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalId, true, getEnvironment(xEnvironment))
                .stream()
                .map(integrationDTO -> conversionService.convert(integrationDTO, IntegrationBasicModel.class))
                .toList());
    }

    @Override
    public ResponseEntity<IntegrationModel> getIntegration(
        String externalUserId, Long id, EnvironmentModel xEnvironment) {

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO =
            connectedUserIntegrationFacade.getConnectedUserIntegration(
                externalUserId, id, true, getEnvironment(xEnvironment));

        IntegrationModel integrationModel = conversionService.convert(
            connectedUserIntegrationDTO, IntegrationModel.class);

        populateMcpData(connectedUserIntegrationDTO, integrationModel);
        filterDisabledWorkflows(connectedUserIntegrationDTO, integrationModel);

        return ResponseEntity.ok(integrationModel);
    }

    @Override
    public ResponseEntity<List<IntegrationBasicModel>> getIntegrations(
        String externalUserId, EnvironmentModel xEnvironment) {

        return ResponseEntity.ok(
            connectedUserIntegrationFacade
                .getConnectedUserIntegrations(externalUserId, true, getEnvironment(xEnvironment))
                .stream()
                .map(integrationInstanceConfigurationDTO -> conversionService.convert(
                    integrationInstanceConfigurationDTO, IntegrationBasicModel.class))
                .toList());
    }

    @InitBinder
    public void initBinder(WebDataBinder dataBinder) {
        dataBinder.registerCustomEditor(EnvironmentModel.class, new CaseInsensitiveEnumPropertyEditorSupport());
    }

    private void filterDisabledWorkflows(
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO, IntegrationModel integrationModel) {

        if (integrationModel == null || integrationModel.getWorkflows() == null) {
            return;
        }

        if (connectedUserIntegrationDTO.integrationInstanceConfiguration() == null ||
            connectedUserIntegrationDTO.integrationInstanceConfiguration()
                .integrationInstanceConfigurationWorkflows() == null) {

            return;
        }

        Set<String> mcpWorkflowUuids = integrationModel.getMcpWorkflows() == null
            ? Set.of()
            : integrationModel.getMcpWorkflows()
                .stream()
                .map(IntegrationWorkflowModel::getWorkflowUuid)
                .collect(Collectors.toSet());

        Set<String> enabledWorkflowUuids = connectedUserIntegrationDTO.integrationInstanceConfiguration()
            .integrationInstanceConfigurationWorkflows()
            .stream()
            .filter(IntegrationInstanceConfigurationWorkflowDTO::enabled)
            .map(IntegrationInstanceConfigurationWorkflowDTO::workflowUuid)
            .filter(workflowUuid -> !mcpWorkflowUuids.contains(workflowUuid))
            .collect(Collectors.toSet());

        integrationModel.setWorkflows(
            integrationModel.getWorkflows()
                .stream()
                .filter(workflowModel -> enabledWorkflowUuids.contains(workflowModel.getWorkflowUuid()))
                .toList());
    }

    private Environment getEnvironment(EnvironmentModel xEnvironment) {
        return environmentService.getEnvironment(xEnvironment == null ? null : xEnvironment.name());
    }

    private void populateMcpData(
        ConnectedUserIntegrationDTO connectedUserIntegrationDTO, IntegrationModel integrationModel) {

        if (integrationModel == null) {
            return;
        }

        String componentName =
            connectedUserIntegrationDTO.integrationInstanceConfiguration()
                .integration()
                .componentName();

        List<McpToolModel> mcpToolModels = mcpComponentService.getMcpComponentsByComponentName(componentName)
            .stream()
            .filter(mcpComponent -> isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId()))
            .flatMap(mcpComponent -> mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .stream()
                .map(mcpTool -> {
                    McpToolModel mcpToolModel = conversionService.convert(mcpTool, McpToolModel.class);

                    mcpToolModel.setName(mcpTool.getName());

                    ClusterElementDefinition clusterElementDefinition =
                        clusterElementDefinitionService.getClusterElementDefinition(
                            mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

                    mcpToolModel.setDescription(clusterElementDefinition.getDescription());

                    return mcpToolModel;
                }))
            .toList();

        integrationModel.setMcpTools(mcpToolModels);

        long integrationId = connectedUserIntegrationDTO.integrationInstanceConfiguration()
            .integrationId();

        List<IntegrationWorkflowModel> mcpWorkflowModels =
            mcpIntegrationInstanceConfigurationService
                .getMcpIntegrationInstanceConfigurationsByIntegrationId(integrationId)
                .stream()
                .filter(mcpIntegrationInstanceConfiguration -> isEmbeddedMcpServerEnabled(
                    mcpIntegrationInstanceConfiguration.getMcpServerId()))
                .map(McpIntegrationInstanceConfiguration::getId)
                .flatMap(mcpIntegrationInstanceConfigurationId -> mcpIntegrationInstanceConfigurationWorkflowService
                    .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                        mcpIntegrationInstanceConfigurationId)
                    .stream())
                .map(mcpIntegrationInstanceConfigurationWorkflow -> {
                    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                            mcpIntegrationInstanceConfigurationWorkflow
                                .getIntegrationInstanceConfigurationWorkflowId());

                    String workflowId = integrationInstanceConfigurationWorkflow.getWorkflowId();

                    Workflow workflow = workflowService.getWorkflow(workflowId);

                    IntegrationWorkflow integrationWorkflow =
                        integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

                    List<InputModel> inputModels = workflow.getInputs()
                        .stream()
                        .map(input -> new InputModel()
                            .label(input.label())
                            .name(input.name())
                            .required(input.required())
                            .type(InputTypeModel.fromValue(input.type())))
                        .toList();

                    return new IntegrationWorkflowModel()
                        .description(workflow.getDescription())
                        .inputs(inputModels)
                        .label(workflow.getLabel())
                        .workflowUuid(integrationWorkflow.getUuidAsString());
                })
                .filter(model -> model.getWorkflowUuid() != null)
                .toList();

        integrationModel.setMcpWorkflows(mcpWorkflowModels);

        populateMcpInstanceData(integrationModel);
    }

    private void populateMcpInstanceData(IntegrationModel integrationModel) {
        if (integrationModel.getIntegrationInstances() == null) {
            return;
        }

        for (IntegrationInstanceModel integrationInstanceModel : integrationModel.getIntegrationInstances()) {
            Long integrationInstanceId = integrationInstanceModel.getId();

            if (integrationInstanceId == null) {
                continue;
            }

            List<McpIntegrationInstanceToolModel> mcpInstanceToolModels =
                mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId)
                    .stream()
                    .filter(mcpInstanceTool -> {
                        McpTool mcpTool = mcpToolService.fetchMcpTool(mcpInstanceTool.getMcpToolId())
                            .orElse(null);

                        if (mcpTool == null) {
                            return false;
                        }

                        McpComponent mcpComponent =
                            mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

                        return isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId());
                    })
                    .map(mcpTool -> conversionService.convert(mcpTool, McpIntegrationInstanceToolModel.class))
                    .toList();

            integrationInstanceModel.setMcpTools(mcpInstanceToolModels);

            List<IntegrationInstanceWorkflowModel> mcpInstanceWorkflowModels =
                integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId)
                    .stream()
                    .map(integrationInstanceWorkflow -> {
                        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                            mcpIntegrationInstanceConfigurationWorkflowService
                                .fetchMcpIntegrationInstanceConfigurationWorkflowByIntegrationInstanceConfigurationWorkflowId(
                                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
                                .orElse(null);

                        if (mcpIntegrationInstanceConfigurationWorkflow == null) {
                            return null;
                        }

                        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
                            mcpIntegrationInstanceConfigurationService
                                .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationWorkflow
                                    .getMcpIntegrationInstanceConfigurationId())
                                .orElse(null);

                        if (mcpIntegrationInstanceConfiguration == null
                            || !isEmbeddedMcpServerEnabled(mcpIntegrationInstanceConfiguration.getMcpServerId())) {
                            return null;
                        }

                        IntegrationInstanceWorkflowModel model = conversionService.convert(
                            integrationInstanceWorkflow, IntegrationInstanceWorkflowModel.class);

                        IntegrationInstanceConfigurationWorkflow configurationWorkflow =
                            integrationInstanceConfigurationWorkflowService
                                .getIntegrationInstanceConfigurationWorkflow(
                                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                        IntegrationWorkflow integrationWorkflow =
                            integrationWorkflowService.getWorkflowIntegrationWorkflow(
                                configurationWorkflow.getWorkflowId());

                        model.setWorkflowUuid(integrationWorkflow.getUuidAsString());

                        return model;
                    })
                    .filter(model -> model != null)
                    .toList();

            integrationInstanceModel.setMcpWorkflows(mcpInstanceWorkflowModels);
        }
    }

    private boolean isEmbeddedMcpServerEnabled(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        return mcpServer.getType() == PlatformType.EMBEDDED && mcpServer.isEnabled();
    }
}
