import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType} from '@/shared/types';
import {useMemo} from 'react';

const APPROVAL_GATE = 'approvalGate';

export interface ToolItemI {
    componentName: string;
    componentVersion: number;
    icon?: string;
    label: string;
    name: string;
    operationName: string;
    title: string;
    type: string;
}

export interface ToolGroupI {
    channelLabels: string[];
    label: string;
    name: string;
    tools: ToolItemI[];
}

interface UseAiAgentToolsI {
    configuredConnectionKeys: Set<string>;
    rootWorkflowNodeName?: string;
    toolGroups: ToolGroupI[];
    tools: ToolItemI[];
}

type ToolElementType = NodeDataType & {
    clusterElements?: Record<string, unknown>;
    name?: string;
};

type ComponentDefinitionMapType = Map<string, {icon?: string; title?: string}>;

function mapToolElement(toolElement: ToolElementType, definitionsMap: ComponentDefinitionMapType): ToolItemI {
    // Tool elements can appear in either shape: NodeDataType (workflowNodeName)
    // when synced in-memory, or ClusterElementItemType (name) when loaded from
    // the workflow definition. Fall through both so the first load after an
    // Add also produces a non-empty identifier.
    const typeSegments = toolElement.type?.split('/') || [];
    const componentName = toolElement.componentName || typeSegments[0] || '';
    const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;
    const operationName = toolElement.operationName || typeSegments[2] || '';
    const componentDefinition = definitionsMap.get(componentName);
    const toolName = toolElement.workflowNodeName || toolElement.name || '';

    return {
        componentName,
        componentVersion,
        icon: componentDefinition?.icon,
        label: toolElement.label || toolName,
        name: toolName,
        operationName,
        title: componentDefinition?.title || componentName,
        type: toolElement.type || '',
    };
}

export default function useAiAgentTools(): UseAiAgentToolsI {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const {toolGroups, tools} = useMemo<{toolGroups: ToolGroupI[]; tools: ToolItemI[]}>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return {toolGroups: [], tools: []};
        }

        const toolElements = clusterElements['tools'];

        if (!Array.isArray(toolElements)) {
            return {toolGroups: [], tools: []};
        }

        const definitionsMap: ComponentDefinitionMapType = new Map(
            componentDefinitions.map((definition) => [definition.name, definition])
        );

        const groupedTools: ToolGroupI[] = [];
        const ungatedTools: ToolItemI[] = [];

        toolElements.forEach((toolElement) => {
            const element = toolElement as unknown as ToolElementType;

            const operationName = element.operationName || element.type?.split('/')[2] || '';

            if (operationName !== APPROVAL_GATE) {
                ungatedTools.push(mapToolElement(element, definitionsMap));

                return;
            }

            const gateClusterElements = element.clusterElements || {};
            const gatedToolElements = gateClusterElements['tools'];
            const channelElements = gateClusterElements['approvalChannels'];
            const gateParameters = element.parameters as {name?: string} | undefined;
            const gateName = element.workflowNodeName || element.name || '';

            groupedTools.push({
                channelLabels: Array.isArray(channelElements)
                    ? channelElements.map(
                          (channelElement) => (channelElement as {type?: string}).type?.split('/')[0] || ''
                      )
                    : [],
                label: gateParameters?.name || gateName,
                name: gateName,
                tools: Array.isArray(gatedToolElements)
                    ? gatedToolElements.map((gatedToolElement) =>
                          mapToolElement(gatedToolElement as ToolElementType, definitionsMap)
                      )
                    : [],
            });
        });

        return {toolGroups: groupedTools, tools: ungatedTools};
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const {data: testConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const configuredConnectionKeys = useMemo(
        () =>
            new Set(
                testConnections
                    ?.map((connection) => connection.workflowConnectionKey)
                    .filter((key): key is string => key !== undefined)
            ),
        [testConnections]
    );

    return {
        configuredConnectionKeys,
        rootWorkflowNodeName: rootClusterElementNodeData?.workflowNodeName,
        toolGroups,
        tools,
    };
}
