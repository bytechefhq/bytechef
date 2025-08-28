import {Workflow} from '@/shared/middleware/platform/configuration';
import {ClusterElementItemType, UpdateWorkflowMutationType} from '@/shared/types';

import useClusterElementsDataStore from '../../cluster-element-editor/stores/useClusterElementsDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import saveWorkflowDefinition from './saveWorkflowDefinition';
import updateClusterElementsPositions from './updateClusterElementsPositions';

interface SaveClusterElementNodesPositionProps {
    invalidateWorkflowQueries: () => void;
    movedClusterElementId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
    workflow: Workflow;
}

export default function saveClusterElementNodesPosition({
    invalidateWorkflowQueries,
    movedClusterElementId,
    updateWorkflowMutation,
    workflow,
}: SaveClusterElementNodesPositionProps) {
    const {nodes: clusterElementNodes, setIsPositionSaving} = useClusterElementsDataStore.getState();
    const {rootClusterElementNodeData, setRootClusterElementNodeData} = useWorkflowEditorStore.getState();

    // Position saving is in progress
    setIsPositionSaving(true);

    if (!workflow.definition || !rootClusterElementNodeData) {
        setIsPositionSaving(false);

        return;
    }

    const workflowDefinitionTasks = JSON.parse(workflow.definition).tasks;

    const mainClusterRootTask = workflowDefinitionTasks.find(
        (task: {name: string}) => task.name === rootClusterElementNodeData?.workflowNodeName
    );

    if (!mainClusterRootTask || !mainClusterRootTask.clusterElements) {
        setIsPositionSaving(false);

        return;
    }

    const clusterElements: Record<string, ClusterElementItemType | ClusterElementItemType[]> =
        mainClusterRootTask.clusterElements;

    const nodePositions = clusterElementNodes.reduce<Record<string, {x: number; y: number}>>((accumulator, node) => {
        accumulator[node.id] = {
            x: node.position.x,
            y: node.position.y,
        };

        return accumulator;
    }, {});

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
        nodeData: updatedNodeData,
        updateWorkflowMutation,
    }).finally(() => {
        setIsPositionSaving(false);
    });
}
