/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.execution.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.ee.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service("com.bytechef.ee.embedded.execution.facade.ToolFacade")
@ConditionalOnEEVersion
public class ToolFacadeImpl implements ToolFacade {

    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionIdHelper connectionIdHelper;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public ToolFacadeImpl(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        ConnectionIdHelper connectionIdHelper,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService) {

        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectionIdHelper = connectionIdHelper;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
    }

    @Override
    public List<ToolDTO> getTools() {
        return clusterElementDefinitionService.getClusterElementDefinitions(ToolFunction.TOOLS)
            .stream()
            .map(clusterElementDefinition -> new ToolDTO(
                getToolName(
                    clusterElementDefinition.getComponentName(), clusterElementDefinition.getName()),
                clusterElementDefinition.getDescription(),
                JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties())))
            .toList();
    }

    @Override
    public Map<String, List<ToolDTO>> getTools(
        String externalUserId, List<String> categoryNames, List<String> componentNames,
        List<String> clusterElementNames, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        List<Long> integrationInstanceConfigurationIds = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUser.getId(), environment)
            .stream()
            .map(IntegrationInstance::getIntegrationInstanceConfigurationId)
            .toList();

        List<Long> integrationIds = integrationInstanceConfigurationService
            .getIntegrationInstanceConfigurations(integrationInstanceConfigurationIds)
            .stream()
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .toList();

        return integrationService.getIntegrations(integrationIds)
            .stream()
            .map(Integration::getComponentName)
            .filter(componentName -> filterByComponentNames(componentNames, componentName))
            .map(componentName -> componentDefinitionService.getComponentDefinition(componentName, null))
            .filter(componentDefinition -> filterByCategoryNames(categoryNames, componentDefinition))
            .flatMap(componentDefinition -> CollectionUtils.stream(
                clusterElementDefinitionService.getClusterElementDefinitions(
                    componentDefinition.getName(), componentDefinition.getVersion(), ToolFunction.TOOLS)))
            .filter(clusterElementDefinition -> filterByClusterElementNames(
                clusterElementNames, clusterElementDefinition))
            .collect(
                Collectors.groupingBy(
                    ClusterElementDefinition::getComponentName,
                    Collectors.mapping(
                        clusterElementDefinition -> new ToolDTO(
                            getToolName(
                                clusterElementDefinition.getComponentName(), clusterElementDefinition.getName()),
                            clusterElementDefinition.getDescription(),
                            JsonSchemaGeneratorUtils.generateInputSchema(clusterElementDefinition.getProperties())),
                        Collectors.toList())));
    }

    @Override
    public Object executeTool(
        String externalUserId, String toolName, Map<String, Object> inputParameters, @Nullable Long instanceId,
        Environment environment) {

        ComponentClusterElementNameResult result = getComponentClusterElementNames(toolName);

        ClusterElementDefinition clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
            result.componentName(), result.clusterElementName());

        String componentName = clusterElementDefinition.getComponentName();

        Long connectionId = connectionIdHelper.getConnectionId(externalUserId, componentName, instanceId, environment);

        return clusterElementDefinitionFacade.executeTool(
            componentName, clusterElementDefinition.getComponentVersion(), clusterElementDefinition.getName(),
            inputParameters, connectionId);
    }

    private static boolean filterByCategoryNames(List<String> categoryNames, ComponentDefinition componentDefinition) {
        if (categoryNames.isEmpty()) {
            return true;
        } else {
            List<String> componentCategoryNames = componentDefinition.getComponentCategories()
                .stream()
                .map(ComponentCategory::getName)
                .toList();

            return componentCategoryNames.stream()
                .anyMatch(categoryNames::contains);
        }
    }

    private static boolean filterByComponentNames(List<String> componentNames, String componentName) {
        if (componentNames.isEmpty()) {
            return true;
        } else {
            return componentNames.contains(componentName);
        }
    }

    private static boolean filterByClusterElementNames(
        List<String> clusterElementNames, ClusterElementDefinition clusterElementDefinition) {

        if (clusterElementNames.isEmpty()) {
            return true;
        } else {
            return clusterElementNames.contains(clusterElementDefinition.getName());
        }
    }

    private static String getToolName(String componentName, String clusterElementName) {
        StringBuilder sb = new StringBuilder();

        sb.append(componentName.toUpperCase());
        sb.append("_");

        sb.append(Character.toUpperCase(clusterElementName.charAt(0)));

        for (int i = 1; i < clusterElementName.length(); i++) {
            char c = clusterElementName.charAt(i);

            if (Character.isUpperCase(c)) {
                sb.append('_')
                    .append(c);
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }

        return sb.toString();
    }

    private static ComponentClusterElementNameResult getComponentClusterElementNames(String toolName) {
        String lowerCase = toolName.toLowerCase();

        String[] parts = lowerCase.split("_");

        String componentName = parts[0];
        StringBuilder clusterElementName = new StringBuilder();

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];

            if (!part.isEmpty()) {
                if (clusterElementName.isEmpty()) {
                    clusterElementName.append(part);
                } else {
                    clusterElementName.append(Character.toUpperCase(part.charAt(0)));
                    clusterElementName.append(part.substring(1));
                }
            }
        }

        return new ComponentClusterElementNameResult(componentName, clusterElementName.toString());
    }

    private record ComponentClusterElementNameResult(String componentName, String clusterElementName) {
    }
}
