import {Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, UpdateWorkflowMutationType} from '@/shared/types';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {removeClusterElementPosition} from './removeClusterElementPosition';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import {getTaskDispatcherTask} from './taskDispatcherConfig';
import updateClusterElementsPositions from './updateClusterElementsPositions';

interface SaveClusterElementNodesPositionProps {
    clickedNodeName?: string;
    invalidateWorkflowQueries: () => void;
    movedClusterElementId?: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export default function saveClusterElementNodesPosition({
    clickedNodeName,
    invalidateWorkflowQueries,
    movedClusterElementId,
    updateWorkflowMutation,
    workflow,
}: SaveClusterElementNodesPositionProps) {
    const {nodes: clusterElementNodes, setIsPositionSaving} = useClusterElementsDataStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();

    if (!workflow.definition || !rootClusterElementNodeData) {
        console.error('Workflow definition or root cluster element node data not found');

        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = getTaskDispatcherTask({
        taskDispatcherId: rootClusterElementNodeData.workflowNodeName,
        tasks: workflowDefinitionTasks,
    });

    if (!mainClusterRootTask || !mainClusterRootTask.clusterElements) {
        console.error('Main cluster root task or cluster elements not found');

        return;
    }

    if (movedClusterElementId) {
        setIsPositionSaving(true);

        const clusterElements: Record<string, ClusterElementItemType | ClusterElementItemType[]> =
            mainClusterRootTask.clusterElements;

        const nodePositions = clusterElementNodes.reduce<Record<string, {x: number; y: number}>>(
            (accumulator, node) => {
                accumulator[node.id] = {
                    x: node.position.x,
                    y: node.position.y,
                };

                return accumulator;
            },
            {}
        );

        const updatedClusterElements = updateClusterElementsPositions({
            clusterElements,
            movedClusterElementId,
            nodePositions,
        });

        const updatedNodeData = {
            ...mainClusterRootTask,
            clusterElements: updatedClusterElements,
        };

        setRootClusterElementNodeData({
            ...rootClusterElementNodeData,
            clusterElements: updatedClusterElements,
        } as typeof rootClusterElementNodeData);

        // Save updated data but reset the position saving flag even when there are errors
        saveWorkflowDefinition({
            invalidateWorkflowQueries,
            nodeData: {
                ...updatedNodeData,
                componentName: rootClusterElementNodeData.componentName,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            },
            updateWorkflowMutation,
        })
            .catch((error) => {
                console.error('Error saving cluster element nodes position', error);
            })
            .finally(() => {
                setIsPositionSaving(false);
            });
    } else if (clickedNodeName) {
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
            nodeData: {
                ...updatedNodeData,
                componentName: rootClusterElementNodeData.componentName,
                workflowNodeName: rootClusterElementNodeData.workflowNodeName,
            },
            updateWorkflowMutation,
        });
    }
}
