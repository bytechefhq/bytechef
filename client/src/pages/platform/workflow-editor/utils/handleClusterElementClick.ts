import {
    ClusterElementDefinition,
    ClusterElementDefinitionBasic,
    ComponentDefinition,
} from '@/shared/middleware/platform/configuration';
import {PropertyAllType, StructureParentType, UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import {initializeClusterElementsObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedName from './getFormattedName';
import getParametersWithDefaultValues from './getParametersWithDefaultValues';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleClusterElementClickProps {
    clickedClusterElementDefinition: ClusterElementDefinition;
    data: ClusterElementDefinitionBasic;
    parentId: number;
    parentType: StructureParentType;
    queryClient: QueryClient;
    rootClusterElementDefinition: ComponentDefinition;
    setPopoverOpen: (open: boolean) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export default function handleClusterElementClick({
    clickedClusterElementDefinition,
    data,
    parentId,
    parentType,
    queryClient,
    rootClusterElementDefinition,
    setPopoverOpen,
    updateWorkflowMutation,
}: HandleClusterElementClickProps) {
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();
    const {workflow} = useWorkflowDataStore.getState();
    const {currentNode, setCurrentNode} = useWorkflowNodeDetailsPanelStore.getState();

    if (!workflow.definition) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const currentParentTask = workflowDefinitionTasks.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );

    if (!currentParentTask) {
        console.error('Parent task not found:', rootClusterElementNodeData?.workflowNodeName);

        return;
    }

    const clusterElementType = data.name;

    const clusterElementTypeDefinition = rootClusterElementDefinition.clusterElementTypes?.find(
        (clusterElementDefinition) => clusterElementDefinition.name === data.type
    );

    const isMultipleElements = !!clusterElementTypeDefinition?.multipleElements;

    const clusterElements = initializeClusterElementsObject(
        rootClusterElementDefinition,
        currentParentTask.clusterElements
    );

    if (isMultipleElements) {
        clusterElements[clusterElementType] = [
            ...(Array.isArray(clusterElements[clusterElementType]) ? clusterElements[clusterElementType] : []),
            {
                label: data.componentName,
                name: getFormattedName(data.componentName),
                parameters:
                    getParametersWithDefaultValues({
                        properties: clickedClusterElementDefinition?.properties as Array<PropertyAllType>,
                    }) || {},
                type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
            },
        ];
    } else {
        clusterElements[clusterElementType] = {
            label: data.title,
            name: getFormattedName(data.componentName),
            parameters:
                getParametersWithDefaultValues({
                    properties: clickedClusterElementDefinition?.properties as Array<PropertyAllType>,
                }) || {},
            type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
        };
    }

    const updatedNodeData = {
        ...currentParentTask,
        clusterElements,
    };

    setRootClusterElementNodeData({
        ...rootClusterElementNodeData,
        clusterElements,
    } as typeof rootClusterElementNodeData);

    if (currentNode?.rootClusterElement) {
        setCurrentNode({
            ...currentNode,
            clusterElements,
        });
    }

    saveWorkflowDefinition({
        nodeData: updatedNodeData,
        onSuccess: () => {
            handleComponentAddedSuccess({
                nodeData: updatedNodeData,
                queryClient,
                workflow,
            });
        },
        parentId,
        parentType,
        queryClient,
        updateWorkflowMutation,
    });

    setPopoverOpen(false);
}
