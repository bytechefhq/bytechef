import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ClusterElementDefinitionApi} from '@/shared/middleware/platform/configuration';
import {ClusterElementDefinitionKeys} from '@/shared/queries/platform/clusterElementDefinitions.queries';
import {useGetComponentDefinitionQuery} from '@/shared/queries/platform/componentDefinitions.queries';
import {useGetWorkflowTestConfigurationConnectionsQuery} from '@/shared/queries/platform/workflowTestConfigurations.queries';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {NodeDataType} from '@/shared/types';
import {useQueryClient} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';
import {useShallow} from 'zustand/shallow';

export interface ModelItemI {
    componentName: string;
    icon?: string;
    label: string;
    name: string;
    operationName: string;
    title: string;
    type: string;
}

interface UseAiAgentModelSelectFieldI {
    handleConfigureModel: (model: ModelItemI) => Promise<void>;
    isConnectionMissing: boolean;
    model: ModelItemI | null;
    rootWorkflowNodeName?: string;
}

export default function useAiAgentModelSelectField(): UseAiAgentModelSelectFieldI {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const rootClusterElementNodeData = useWorkflowEditorStore((state) => state.rootClusterElementNodeData);
    const componentDefinitions = useWorkflowDataStore((state) => state.componentDefinitions);
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {setActiveTab, setCurrentComponent, setCurrentNode, setWorkflowNodeDetailsPanelOpen} =
        useWorkflowNodeDetailsPanelStore(
            useShallow((state) => ({
                setActiveTab: state.setActiveTab,
                setCurrentComponent: state.setCurrentComponent,
                setCurrentNode: state.setCurrentNode,
                setWorkflowNodeDetailsPanelOpen: state.setWorkflowNodeDetailsPanelOpen,
            }))
        );

    const queryClient = useQueryClient();

    const model = useMemo<ModelItemI | null>(() => {
        const clusterElements = rootClusterElementNodeData?.clusterElements;

        if (!clusterElements || Array.isArray(clusterElements)) {
            return null;
        }

        const modelValue = clusterElements['model'];

        if (!modelValue) {
            return null;
        }

        const modelElement = (Array.isArray(modelValue) ? modelValue[0] : modelValue) as unknown as NodeDataType;
        const typeSegments = modelElement.type?.split('/') || [];
        const componentName = modelElement.componentName || typeSegments[0] || '';
        const operationName = modelElement.operationName || typeSegments[2] || '';
        const definitionsMap = new Map(componentDefinitions.map((definition) => [definition.name, definition]));
        const componentDefinition = definitionsMap.get(componentName);

        return {
            componentName,
            icon: componentDefinition?.icon,
            label: modelElement.label || modelElement.workflowNodeName || '',
            name: modelElement.workflowNodeName || '',
            operationName,
            title: componentDefinition?.title || componentName,
            type: modelElement.type || '',
        };
    }, [rootClusterElementNodeData?.clusterElements, componentDefinitions]);

    const modelComponentVersion = useMemo(() => {
        if (!model?.type) {
            return 1;
        }

        return Number(model.type.split('/')[1]?.replace(/^v/, '')) || 1;
    }, [model?.type]);

    const {data: modelComponentDefinition} = useGetComponentDefinitionQuery(
        {componentName: model?.componentName || '', componentVersion: modelComponentVersion},
        !!model?.componentName
    );

    const {data: testConnections} = useGetWorkflowTestConfigurationConnectionsQuery(
        {
            environmentId: currentEnvironmentId,
            workflowId: workflow.id!,
            workflowNodeName: rootClusterElementNodeData?.workflowNodeName || '',
        },
        !!workflow.id && !!rootClusterElementNodeData?.workflowNodeName
    );

    const isConnectionMissing =
        !!model &&
        !!modelComponentDefinition?.connection &&
        !testConnections?.some((connection) => connection.workflowConnectionKey === model.name);

    const handleConfigureModel = useCallback(
        async (modelItem: ModelItemI) => {
            const typeSegments = modelItem.type.split('/');
            const clusterElementName = typeSegments[2];
            const componentVersion = parseInt(typeSegments[1]?.replace(/^v/, '')) || 1;

            try {
                await queryClient.fetchQuery({
                    queryFn: () =>
                        new ClusterElementDefinitionApi().getComponentClusterElementDefinition({
                            clusterElementName,
                            componentName: modelItem.componentName,
                            componentVersion: componentVersion,
                        }),
                    queryKey: ClusterElementDefinitionKeys.clusterElementDefinition({
                        clusterElementName,
                        componentName: modelItem.componentName,
                        componentVersion: componentVersion,
                    }),
                });
            } catch (error) {
                console.warn('Failed to pre-populate cluster element definition cache:', error);
            }

            const modelNodeData: NodeDataType = {
                clusterElementName,
                clusterElementType: 'model',
                componentName: modelItem.componentName,
                label: modelItem.label,
                name: modelItem.name,
                operationName: modelItem.operationName,
                parentClusterRootId: rootClusterElementNodeData?.name,
                type: modelItem.type,
                version: componentVersion,
                workflowNodeName: modelItem.name,
            };

            setActiveTab('description');
            setCurrentNode({...modelNodeData, description: ''});
            setWorkflowNodeDetailsPanelOpen(true);

            setCurrentComponent((previousCurrentComponent) => ({
                ...modelNodeData,
                displayConditions: previousCurrentComponent?.displayConditions,
                workflowNodeName: modelItem.name,
            }));
        },
        [
            queryClient,
            rootClusterElementNodeData?.name,
            setActiveTab,
            setCurrentComponent,
            setCurrentNode,
            setWorkflowNodeDetailsPanelOpen,
        ]
    );

    return {
        handleConfigureModel,
        isConnectionMissing,
        model,
        rootWorkflowNodeName: rootClusterElementNodeData?.workflowNodeName,
    };
}
