import {Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, ClusterElementsType, NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import saveWorkflowDefinition from './saveWorkflowDefinition';

export const removeClusterElementPosition = (
    clusterElements: ClusterElementsType,
    clickedNodeName: string
): ClusterElementsType => {
    const updatedClusterElements: ClusterElementsType = {};

    Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            updatedClusterElements[elementKey] = elementValue.map((element) => {
                const positionRemovalRequired = element.name === clickedNodeName;

                const updatedElement: ClusterElementItemType = {
                    ...element,
                    metadata: {
                        ...element?.metadata,
                        ui: {
                            ...element?.metadata?.ui,
                            nodePosition: positionRemovalRequired ? undefined : element?.metadata?.ui?.nodePosition,
                        },
                    },
                };

                // Process nested elements if needed
                if (updatedElement.clusterElements) {
                    updatedElement.clusterElements = removeClusterElementPosition(
                        updatedElement.clusterElements,
                        clickedNodeName
                    );
                }

                return updatedElement;
            });
        } else if (elementValue && isPlainObject(elementValue)) {
            const positionRemovalRequired = elementValue.name === clickedNodeName;

            const updatedElement = {
                ...elementValue,
                metadata: {
                    ...elementValue?.metadata,
                    ui: {
                        ...elementValue?.metadata?.ui,
                        nodePosition: positionRemovalRequired ? undefined : elementValue?.metadata?.ui?.nodePosition,
                    },
                },
            };

            // Process nested elements if needed
            if (updatedElement.clusterElements) {
                updatedElement.clusterElements = removeClusterElementPosition(
                    updatedElement.clusterElements,
                    clickedNodeName
                );
            }

            updatedClusterElements[elementKey] = updatedElement;
        }
    });

    return updatedClusterElements;
};

interface FindAndRemoveSavedClusterElementPositionProps {
    clickedNodeName: string;
    invalidateWorkflowQueries: () => void;
    rootClusterElementNodeData: NodeDataType;
    setRootClusterElementNodeData: (data: NodeDataType) => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export const findAndRemoveSavedClusterElementPosition = ({
    clickedNodeName,
    invalidateWorkflowQueries,
    rootClusterElementNodeData,
    setRootClusterElementNodeData,
    updateWorkflowMutation,
    workflow,
}: FindAndRemoveSavedClusterElementPositionProps) => {
    if (!workflow.definition || !rootClusterElementNodeData) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = workflowDefinitionTasks.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );

    if (!mainClusterRootTask || !mainClusterRootTask.clusterElements) {
        return;
    }

    const clusterElementPositionRemovalResult = removeClusterElementPosition(
        mainClusterRootTask.clusterElements,
        clickedNodeName
    );

    const updatedNodeData = {
        ...mainClusterRootTask,
        clusterElements: clusterElementPositionRemovalResult,
    };

    setRootClusterElementNodeData({
        ...rootClusterElementNodeData,
        clusterElements: clusterElementPositionRemovalResult,
    } as typeof rootClusterElementNodeData);

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: updatedNodeData,
        updateWorkflowMutation,
    });
};
