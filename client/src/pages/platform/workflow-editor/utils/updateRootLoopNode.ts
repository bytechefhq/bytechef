import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

interface UpdateRootLoopNodeProps {
    loopId: string;
    nodeIndex: number;
    tasks: Array<WorkflowTask>;
    updatedParentLoopNodeData: NodeDataType;
    updatedParentLoopTask: WorkflowTask;
    nodes: Array<Node>;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function updateRootLoopNode({
    nodeIndex,
    nodes,
    tasks,
    updatedParentLoopNodeData,
    updatedParentLoopTask,
    workflow,
}: UpdateRootLoopNodeProps): NodeDataType {
    let currentTaskNode = updatedParentLoopNodeData;

    let currentTaskNodeLoopData = updatedParentLoopNodeData.loopData;

    while (currentTaskNodeLoopData) {
        const parentLoopTask = getParentTaskDispatcherTask({taskDispatcherId: currentTaskNodeLoopData.loopId, tasks});

        if (!parentLoopTask) {
            break;
        }

        const parentLoopTaskNode = nodes.find((node) => node.id === parentLoopTask.name);

        if (!parentLoopTaskNode) {
            break;
        }

        const parentLoopIterateeTasks: Array<WorkflowTask> =
            (parentLoopTaskNode.data as NodeDataType)?.parameters?.iteratee || [];

        const workflowTasks = workflow.tasks;

        let currentTask = workflowTasks?.find((task) => task.name === currentTaskNode.workflowNodeName);

        if (!currentTask) {
            currentTask = updatedParentLoopTask;
        }

        const currentTaskIndex = parentLoopIterateeTasks.findIndex((task) => task.name === currentTask.name);

        if (currentTaskIndex > -1) {
            parentLoopIterateeTasks[currentTaskIndex] = currentTask;
        } else {
            parentLoopIterateeTasks[nodeIndex] = currentTask;
        }

        parentLoopTaskNode.data.parameters = {
            ...(parentLoopTaskNode.data as NodeDataType).parameters,
            iteratee: parentLoopIterateeTasks,
        };

        currentTaskNode = {
            ...parentLoopTaskNode,
            componentName: (parentLoopTaskNode.data as NodeDataType).componentName,
            name: parentLoopTaskNode.id,
            type: parentLoopTaskNode.type || 'workflow',
            version: Number((parentLoopTaskNode.data as NodeDataType)?.type?.split('/v')[1]),
            workflowNodeName: parentLoopTaskNode.id,
        };

        currentTaskNodeLoopData = (parentLoopTaskNode.data as NodeDataType).loopData;
    }

    return currentTaskNode;
}
