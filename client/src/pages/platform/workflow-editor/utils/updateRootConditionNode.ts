import {Workflow, WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';

import {WorkflowTaskDataType} from '../stores/useWorkflowDataStore';
import getParentConditionTask from './getParentConditionTask';

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
    let currentTaskNode = updatedParentConditionNodeData;

    let currentTaskNodeConditionData = updatedParentConditionNodeData.conditionData;

    while (currentTaskNodeConditionData) {
        const parentConditionTask = getParentConditionTask(tasks, currentTaskNodeConditionData.conditionId);

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

        let currentTask = workflowTasks?.find((task) => task.name === currentTaskNode.componentName);

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

        currentTaskNode = {
            ...parentConditionTaskNode,
            componentName: (parentConditionTaskNode.data as NodeDataType).componentName,
            name: parentConditionTaskNode.id,
            type: parentConditionTaskNode.type || 'workflow',
            version: Number((parentConditionTaskNode.data as NodeDataType)?.type?.split('/v')[1]),
            workflowNodeName: parentConditionTaskNode.id,
        };

        currentTaskNodeConditionData = (parentConditionTaskNode.data as NodeDataType).conditionData;
    }

    return currentTaskNode;
}
