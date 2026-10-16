import {Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, UpdateWorkflowMutationType} from '@/shared/types';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {getTask} from './getTask';
import {removeClusterElementPosition} from './removeClusterElementPosition';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import updateClusterElementsPositions from './updateClusterElementsPositions';

interface SaveClusterElementNodesPositionProps {
    clickedNodeName?: string;
    clusterRootName?: string;
    movedClusterElementId?: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export default function saveClusterElementNodesPosition({
    clickedNodeName,
    clusterRootName,
    movedClusterElementId,
    updateWorkflowMutation,
    workflow,
}: SaveClusterElementNodesPositionProps) {
    const {nodes: clusterElementNodes, setIsPositionSaving} = useClusterElementsDataStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();

    const mainClusterRootName = clusterRootName ?? rootClusterElementNodeData?.workflowNodeName;

    if (!workflow.definition || !mainClusterRootName) {
        console.error('Workflow definition or cluster root not found');

        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = getTask({
        tasks: workflowDefinitionTasks,
        workflowNodeName: mainClusterRootName,
    });

    if (!mainClusterRootTask || !mainClusterRootTask.clusterElements) {
        console.error('Main cluster root task or cluster elements not found');

        return;
    }

    const storedRootClusterElementNodeData =
        rootClusterElementNodeData?.workflowNodeName === mainClusterRootName ? rootClusterElementNodeData : undefined;

    const mainClusterRootComponentName =
        mainClusterRootTask.type?.split('/')[0] || storedRootClusterElementNodeData?.componentName;

    if (!mainClusterRootComponentName) {
        console.error('Main cluster root component not found');

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

        if (storedRootClusterElementNodeData) {
            setRootClusterElementNodeData({
                ...storedRootClusterElementNodeData,
                clusterElements: updatedClusterElements,
            });
        }

        // Save updated data but reset the position saving flag even when there are errors
        saveWorkflowDefinition({
            nodeData: {
                ...updatedNodeData,
                componentName: mainClusterRootComponentName,
                workflowNodeName: mainClusterRootName,
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

        if (storedRootClusterElementNodeData) {
            setRootClusterElementNodeData({
                ...storedRootClusterElementNodeData,
                clusterElements: clusterElementPositionRemovalResult,
            });
        }

        saveWorkflowDefinition({
            nodeData: {
                ...updatedNodeData,
                componentName: mainClusterRootComponentName,
                workflowNodeName: mainClusterRootName,
            },
            updateWorkflowMutation,
        });
    }
}
