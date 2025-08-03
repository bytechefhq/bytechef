package com.bytechef.platform.ai.mcp;

import com.bytechef.component.definition.ai.agent.ToolFunction;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.util.JsonSchemaGeneratorUtils;
import com.bytechef.platform.constant.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Matija Petanjek
 */
@Service("automationToolFacade")
public class ToolFacadeImpl implements ToolFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ClusterElementDefinitionFacade clusterElementDefinitionFacade;

    @SuppressFBWarnings("EI")
    public ToolFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.clusterElementDefinitionFacade = clusterElementDefinitionFacade;
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
    public Object executeTool(
        String toolName, Map<String, Object> inputParameters, Environment environment) {

        ComponentClusterElementNameResult result = getComponentClusterElementNames(toolName);

        ClusterElementDefinition clusterElementDefinition = clusterElementDefinitionService.getClusterElementDefinition(
            result.componentName(), result.clusterElementName());

        String componentName = clusterElementDefinition.getComponentName();

//        Long connectionId = connectionIdHelper.getConnectionId(
//            externalUserId, componentName, instanceId, environment);

        return clusterElementDefinitionFacade.executeTool(
            componentName, clusterElementDefinition.getComponentVersion(), clusterElementDefinition.getName(),
            inputParameters, null);
    }

    private String getToolName(String componentName, String clusterElementName) {
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
