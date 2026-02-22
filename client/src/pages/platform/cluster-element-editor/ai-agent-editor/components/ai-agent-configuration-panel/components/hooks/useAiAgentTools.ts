import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType} from '@/shared/types';
import {useMemo} from 'react';

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

interface UseAiAgentToolsI {
    configuredConnectionKeys: Set<string>;
    rootWorkflowNodeName?: string;
    tools: ToolItemI[];
}

export default function useAiAgentTools(): UseAiAgentToolsI {
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
    const workflow = useWorkflowDataStore((state) => state.workflow);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const tools = useMemo<ToolItemI[]>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return [];
        }

        const toolElements = clusterElements['tools'];

        if (!Array.isArray(toolElements)) {
            return [];
        }

        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));

        return toolElements.map((toolElement) => {
            // Tool elements from rootClusterElementNodeData use NodeDataType shape
            // (with workflowNodeName) rather than ClusterElementItemType shape (with name)
            const tool = toolElement as unknown as NodeDataType;
            const typeSegments = tool.type?.split('/') || [];
            const componentName = tool.componentName || typeSegments[0] || '';
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;
            const operationName = tool.operationName || typeSegments[2] || '';
            const componentDefinition = definitionsMap.get(componentName);
            const toolName = tool.workflowNodeName || '';

            return {
                componentName,
                componentVersion,
                icon: componentDefinition?.icon,
                label: tool.label || toolName,
                name: toolName,
                operationName,
                title: componentDefinition?.title || componentName,
                type: tool.type || '',
            };
        });
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
        tools,
    };
}
