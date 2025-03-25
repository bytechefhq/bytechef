import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

interface UpdateRootConditionNodeProps {
    conditionCase: string;
    conditionId: string;
    nodeIndex: number;
    tasks: Array<WorkflowTask>;
    updatedParentConditionNodeData: NodeDataType;
    updatedParentConditionTask: WorkflowTask;
    nodes: Array<Node>;
    workflow: Workflow & WorkflowTaskDataType;
}

export default function updateRootConditionNode({
    nodeIndex,
    nodes,
    tasks,
    updatedParentConditionNodeData,
    updatedParentConditionTask,
    workflow,
}: UpdateRootConditionNodeProps): NodeDataType {
    let currentTaskNodeData = updatedParentConditionNodeData;

    let currentTaskNodeConditionData = updatedParentConditionNodeData.conditionData;

    while (currentTaskNodeConditionData) {
        const parentConditionTask = getParentTaskDispatcherTask({
            taskDispatcherId: currentTaskNodeConditionData.conditionId,
            tasks,
        });

        if (!parentConditionTask) {
            break;
        }

        const parentConditionTaskNode = nodes.find((node) => node.id === parentConditionTask.name);

        if (!parentConditionTaskNode) {
            break;
        }

        const currentConditionCase = currentTaskNodeConditionData.conditionCase;

        const parentConditionCaseTasks: Array<WorkflowTask> =
            (parentConditionTaskNode.data as NodeDataType)?.parameters?.[currentConditionCase] || [];

        const workflowTasks = workflow.tasks;

        let currentTask = workflowTasks?.find((task) => task.name === currentTaskNodeData.workflowNodeName);

        if (!currentTask) {
            currentTask = updatedParentConditionTask;
        }

        const currentTaskIndex = parentConditionCaseTasks.findIndex((task) => task.name === currentTask.name);

        if (currentTaskIndex > -1) {
            parentConditionCaseTasks[currentTaskIndex] = currentTask;
        } else {
            parentConditionCaseTasks[nodeIndex] = currentTask;
        }

        parentConditionTaskNode.data.parameters = {
            ...(parentConditionTaskNode.data as NodeDataType).parameters,
            [currentConditionCase]: parentConditionCaseTasks,
        };

        currentTaskNodeData = parentConditionTaskNode.data as NodeDataType;

        currentTaskNodeConditionData = (parentConditionTaskNode.data as NodeDataType).conditionData;
    }

    return currentTaskNodeData;
}
