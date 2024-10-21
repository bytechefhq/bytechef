import {WorkflowTask} from '@/shared/middleware/automation/configuration';
import {NodeDataType} from '@/shared/types';

import getNextPlaceholderId from './getNextPlaceholderId';

interface AddConditionChildNodeProps {
    tasks: Array<WorkflowTask>;
    nodeData: NodeDataType;
    newTask: WorkflowTask;
    nodeIndex?: number;
}

export default function getTasksWithConditionChildNode({
    newTask,
    nodeData,
    nodeIndex,
    tasks,
}: AddConditionChildNodeProps) {
    const sourceNodeId = nodeData.metadata?.ui?.condition;

    if (nodeIndex === undefined || nodeIndex === -1 || typeof nodeIndex !== 'number') {
        tasks.push(newTask);
    }

    const subsequentTasks = tasks.slice(nodeIndex);

    const updatedSubsequentTasks = subsequentTasks.map((sequentialTask) => {
        let sequentialTaskCondition = sequentialTask.metadata?.ui?.condition;

        if (sequentialTaskCondition) {
            const taskConditionSide = sequentialTaskCondition.includes('left')
                ? 'left'
                : sequentialTaskCondition.includes('right')
                  ? 'right'
                  : null;

            const nodeConditionSide = sourceNodeId?.includes('left')
                ? 'left'
                : sourceNodeId?.includes('right')
                  ? 'right'
                  : null;

            if (taskConditionSide && nodeConditionSide && taskConditionSide === nodeConditionSide) {
                sequentialTaskCondition = getNextPlaceholderId(sequentialTaskCondition);
            }
        }

        if (sequentialTask.metadata?.ui?.condition) {
            return {
                ...sequentialTask,
                metadata: {
                    ...sequentialTask.metadata,
                    ui: {
                        ...sequentialTask.metadata?.ui,
                        condition: sequentialTaskCondition,
                    },
                },
            };
        }

        return sequentialTask;
    });

    tasks = [...tasks.slice(0, nodeIndex), ...updatedSubsequentTasks];

    tasks.splice(nodeIndex!, 0, newTask);

    return tasks;
}
