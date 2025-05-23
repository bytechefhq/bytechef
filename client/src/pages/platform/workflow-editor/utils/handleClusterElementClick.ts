import {ClusterElementDefinitionBasic, ComponentDefinition} from '@/shared/middleware/platform/configuration';
import {UpdateWorkflowMutationType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

import {initializeClusterElementsObject} from '../../ai-agent-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import getFormattedName from './getFormattedName';
import handleComponentAddedSuccess from './handleComponentAddedSuccess';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface HandleClusterElementClickProps {
    data: ClusterElementDefinitionBasic;
    projectId: string;
    queryClient: QueryClient;
    rootClusterElementDefinition: ComponentDefinition;
    setPopoverOpen: (open: boolean) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

export default function handleClusterElementClick({
    data,
    projectId,
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

    const currentParentTask = workflowDefinitionTasks?.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );
    const existingClusterElements = currentParentTask?.clusterElements;

    const currentClusterElementDefinition = rootClusterElementDefinition.clusterElementTypes?.find(
        (clusterElementDefinition) => clusterElementDefinition.name === data.type
    );
    const clusterElementContainsMultipleElements = currentClusterElementDefinition?.multipleElements;

    const clusterElements = initializeClusterElementsObject(rootClusterElementDefinition, existingClusterElements);

    const objectKey = data.name;

    if (objectKey in clusterElements) {
        if (clusterElementContainsMultipleElements) {
            const existingElements = Array.isArray(clusterElements[objectKey]) ? clusterElements[objectKey] : [];

            clusterElements[objectKey] = [
                ...existingElements,
                {
                    label: data.componentName,
                    name: getFormattedName(data.componentName),
                    parameters: {},
                    type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
                },
            ];
        } else {
            clusterElements[objectKey] = {
                ...(clusterElements[objectKey] && !Array.isArray(clusterElements[objectKey])
                    ? clusterElements[objectKey]
                    : {}),
                label: data.title,
                name: getFormattedName(data.componentName),
                parameters: {},
                type: `${data.componentName}/v${data.componentVersion}/${data.name}`,
            };
        }
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
        projectId: +projectId!,
        queryClient,
        updateWorkflowMutation,
    });

    setPopoverOpen(false);
}
