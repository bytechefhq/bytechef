import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import getParentConditionTask from './getParentConditionTask';

interface InsertConditionSubtasksProps {
    conditionId: string;
    tasks: Array<WorkflowTask>;
    newTask: WorkflowTask;
    placeholderId: string;
}

export default function insertNewConditionSubtask({
    conditionId,
    newTask,
    placeholderId,
    tasks,
}: InsertConditionSubtasksProps): Array<WorkflowTask> {
    let conditionTask = tasks.find((task) => task.name === conditionId);

    if (!conditionTask) {
        conditionTask = getParentConditionTask(tasks, conditionId);
    }

    if (!conditionTask) {
        return tasks;
    }

    const conditionCase = placeholderId?.split('-')[1] === 'left' ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE;

    if (!conditionTask.parameters) {
        conditionTask.parameters = {
            caseFalse: [],
            caseTrue: [],
        };
    }

    const caseTasks: Array<WorkflowTask> =
        conditionCase === CONDITION_CASE_TRUE ? conditionTask.parameters.caseTrue : conditionTask.parameters.caseFalse;

    const taskIndex = parseInt(placeholderId.split('-').pop() || '-1');

    if (taskIndex === undefined || taskIndex === -1 || typeof taskIndex !== 'number') {
        caseTasks.push(newTask);
    } else {
        caseTasks.splice(taskIndex, 0, newTask);

        conditionTask.parameters = {
            ...conditionTask.parameters,
            [conditionCase]: caseTasks,
        };
    }

    return tasks.map((task) => (task.name === conditionId ? conditionTask : task));
}
