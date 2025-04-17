import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface UpdateRootLoopNodeProps {
    updatedParentNodeData: NodeDataType;
}

export default function getUpdatedRootLoopNodeData({updatedParentNodeData}: UpdateRootLoopNodeProps): NodeDataType {
    const {nodes, workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        console.error('No workflow definition found');

        return updatedParentNodeData;
    }

    const nodesMap = new Map(nodes.map((node) => [node.id, node]));

    const definitionTasks = JSON.parse(workflow.definition).tasks;
    const workflowTasks = workflow.tasks;

    let currentTaskNodeData = updatedParentNodeData;
    let currentTaskNodeLoopData = updatedParentNodeData.loopData;

    while (currentTaskNodeLoopData) {
        const parentLoopTask = TASK_DISPATCHER_CONFIG.loop.getTask({
            taskDispatcherId: currentTaskNodeLoopData.loopId,
            tasks: definitionTasks,
        });

        if (!parentLoopTask) {
            console.error('No parent loop task found for loopId: ', currentTaskNodeLoopData.loopId);

            break;
        }

        const parentLoopTaskNode = nodesMap.get(parentLoopTask.name);

        if (!parentLoopTaskNode) {
            console.error('No parent loop task node found for task: ', parentLoopTask.name);

            break;
        }

        const parentLoopSubtasks: Array<WorkflowTask> = TASK_DISPATCHER_CONFIG.loop.getSubtasks({
            node: parentLoopTaskNode,
        });

        const currentTask = workflowTasks?.find((task) => task.name === currentTaskNodeData.workflowNodeName);

        if (!currentTask) {
            console.error('No current task found for node: ', currentTaskNodeData.workflowNodeName);

            break;
        }

        const currentTaskIndex = parentLoopSubtasks.findIndex((task) => task.name === currentTask.name) ?? 0;

        parentLoopSubtasks[currentTaskIndex] = currentTask;

        parentLoopTaskNode.data.parameters = {
            ...(parentLoopTaskNode.data as NodeDataType).parameters,
            iteratee: parentLoopSubtasks,
        };

        currentTaskNodeData = parentLoopTaskNode.data as NodeDataType;

        currentTaskNodeLoopData = currentTaskNodeData.loopData;
    }

    return currentTaskNodeData;
}
