import {ClusterElementItemType, ClusterElementsType, UpdateWorkflowMutationType} from '@/shared/types';

import {isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {getTask} from './getTask';
import saveWorkflowDefinition from './saveWorkflowDefinition';

interface ClearAllClusterElementPositionsProps {
    invalidateWorkflowQueries: () => void;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

function clearClusterElementPositions(clusterElements: ClusterElementsType): ClusterElementsType {
    const updatedClusterElements: ClusterElementsType = {};

    Object.entries(clusterElements).forEach(([elementKey, elementValue]) => {
        if (Array.isArray(elementValue)) {
            updatedClusterElements[elementKey] = elementValue.map((element) => {
                const updatedElement: ClusterElementItemType = {
                    ...element,
                    metadata: {
                        ...element?.metadata,
                        ui: {
                            ...element?.metadata?.ui,
                            nodePosition: undefined,
                        },
                    },
                };

                if (updatedElement.clusterElements) {
                    updatedElement.clusterElements = clearClusterElementPositions(updatedElement.clusterElements);
                }

                return updatedElement;
            });
        } else if (elementValue && isPlainObject(elementValue)) {
            const updatedElement = {
                ...elementValue,
                metadata: {
                    ...elementValue?.metadata,
                    ui: {
                        ...elementValue?.metadata?.ui,
                        nodePosition: undefined,
                    },
                },
            };

            if (updatedElement.clusterElements) {
                updatedElement.clusterElements = clearClusterElementPositions(updatedElement.clusterElements);
            }

            updatedClusterElements[elementKey] = updatedElement;
        }
    });

    return updatedClusterElements;
}

export default function clearAllClusterElementPositions({
    invalidateWorkflowQueries,
    updateWorkflowMutation,
}: ClearAllClusterElementPositionsProps) {
    const {workflow} = useWorkflowDataStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();

    if (!workflow.definition || !rootClusterElementNodeData) {
        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = rootClusterElementNodeData?.workflowNodeName
        ? getTask({
              tasks: workflowDefinitionTasks,
              workflowNodeName: rootClusterElementNodeData.workflowNodeName,
          })
        : undefined;

    if (!mainClusterRootTask || !mainClusterRootTask.clusterElements) {
        return;
    }

    const clearedClusterElements = clearClusterElementPositions(mainClusterRootTask.clusterElements);

    const updatedNodeData = {
        ...mainClusterRootTask,
        clusterElements: clearedClusterElements,
    };

    setRootClusterElementNodeData({
        ...rootClusterElementNodeData,
        clusterElements: clearedClusterElements,
    } as typeof rootClusterElementNodeData);

    saveWorkflowDefinition({
        invalidateWorkflowQueries,
        nodeData: {
            ...updatedNodeData,
            componentName: rootClusterElementNodeData.componentName,
            workflowNodeName: rootClusterElementNodeData.workflowNodeName,
        },
        updateWorkflowMutation,
    });
}
