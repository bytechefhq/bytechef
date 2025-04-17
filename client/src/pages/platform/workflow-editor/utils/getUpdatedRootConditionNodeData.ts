import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

interface UpdateRootConditionNodeProps {
    updatedParentNodeData: NodeDataType;
}

export default function getUpdatedRootConditionNodeData({
    updatedParentNodeData,
}: UpdateRootConditionNodeProps): NodeDataType {
    const {nodes, workflow} = useWorkflowDataStore.getState();

    if (!workflow.definition) {
        console.error('No workflow definition found');

        return updatedParentNodeData;
    }

    const nodesMap = new Map(nodes.map((node) => [node.id, node]));

    const definitionTasks = JSON.parse(workflow.definition).tasks;
    const workflowTasks = workflow.tasks;

    let currentTaskNodeData = updatedParentNodeData;
    let currentTaskNodeConditionData = updatedParentNodeData.conditionData;

    while (currentTaskNodeConditionData) {
        const parentConditionTask = TASK_DISPATCHER_CONFIG.condition.getTask({
            taskDispatcherId: currentTaskNodeConditionData.conditionId,
            tasks: definitionTasks,
        });

        if (!parentConditionTask) {
            console.error('No parent condition task found for conditionId: ', currentTaskNodeConditionData.conditionId);

            break;
        }

        const parentConditionTaskNode = nodesMap.get(parentConditionTask.name);

        if (!parentConditionTaskNode) {
            console.error('No parent condition task node found for task: ', parentConditionTask.name);

            break;
        }

        const currentConditionCase = currentTaskNodeConditionData.conditionCase as 'caseTrue' | 'caseFalse';

        const parentSubtasks: Array<WorkflowTask> = TASK_DISPATCHER_CONFIG.condition.getSubtasks({
            context: {
                conditionCase: currentConditionCase,
                taskDispatcherId: currentTaskNodeConditionData.conditionId,
            },
            node: parentConditionTaskNode,
        });

        const currentTask = workflowTasks?.find((task) => task.name === currentTaskNodeData.workflowNodeName);

        if (!currentTask) {
            console.error('No current task found for node: ', currentTaskNodeData.workflowNodeName);

            break;
        }

        const currentTaskIndex = parentSubtasks.findIndex((task) => task.name === currentTask.name) ?? 0;

        parentSubtasks[currentTaskIndex] = currentTask;

        parentConditionTaskNode.data.parameters = {
            ...(parentConditionTaskNode.data as NodeDataType).parameters,
            [currentConditionCase]: parentSubtasks,
        };

        currentTaskNodeData = parentConditionTaskNode.data as NodeDataType;

        currentTaskNodeConditionData = (parentConditionTaskNode.data as NodeDataType).conditionData;
    }

    return currentTaskNodeData;
}
