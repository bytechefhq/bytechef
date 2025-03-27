/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.execution.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationService;
import com.bytechef.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.embedded.execution.facade.dto.ToolDTO;
import com.bytechef.embedded.execution.util.ConnectionIdHelper;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class ToolFacadeImpl implements ToolFacade {

    private static final Map<String, String> COMPONENT_NAME_MAP = new ConcurrentHashMap<>();

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
        Environment environment, List<String> categoryNames, List<String> componentNames,
        List<String> clusterElementNames) {

        String externalId = SecurityUtils.getCurrentUserLogin()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(environment, externalId);

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
        String toolName, Map<String, Object> inputParameters, Environment environment, @Nullable Long instanceId) {

        Map.Entry<String, String> componentClusterElementNames = getComponentClusterElementNames(toolName);

        ClusterElementDefinition clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
            componentClusterElementNames.getKey(), componentClusterElementNames.getValue());

        String componentName = clusterElementDefinition.getComponentName();

        Long connectionId = connectionIdHelper.getConnectionId(componentName, environment, instanceId);

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

        String toolName = sb.toString();

        COMPONENT_NAME_MAP.putIfAbsent(toolName, componentName);

        return toolName;
    }

    private static Map.Entry<String, String> getComponentClusterElementNames(String toolName) {
        String lowerCase = toolName.toLowerCase();

        String[] parts = lowerCase.split("_");

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

        return Map.entry(COMPONENT_NAME_MAP.get(toolName), clusterElementName.toString());
    }
}
