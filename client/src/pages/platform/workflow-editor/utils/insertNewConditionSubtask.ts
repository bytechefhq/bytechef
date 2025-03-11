import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import getParentConditionTask from './getParentConditionTask';

interface InsertConditionSubtasksProps {
    conditionId: string;
    newTask: WorkflowTask;
    placeholderId?: string;
    taskDispatcherContext?: Record<string, unknown>;
    tasks: Array<WorkflowTask>;
}

export default function insertNewConditionSubtask({
    conditionId,
    newTask,
    placeholderId,
    taskDispatcherContext,
    tasks,
}: InsertConditionSubtasksProps): Array<WorkflowTask> {
    let taskIndex: number | undefined;
    let conditionCase: string | undefined;

    let conditionTask = tasks.find((task) => task.name === conditionId);

    if (!conditionTask) {
        conditionTask = getParentConditionTask(tasks, conditionId as string);
    }

    if (!conditionTask) {
        return tasks;
    }

    if (!conditionTask.parameters) {
        conditionTask.parameters = {
            caseFalse: [],
            caseTrue: [],
        };
    }

    if (placeholderId) {
        taskIndex = parseInt(placeholderId.split('-').pop() || '-1');
        conditionCase = placeholderId?.split('-')[1] === 'left' ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE;
    }

    if (taskDispatcherContext) {
        taskIndex = taskDispatcherContext.index as number;
        conditionCase = taskDispatcherContext.conditionCase as string;
    }

    const caseTasks: Array<WorkflowTask> =
        conditionCase === CONDITION_CASE_TRUE ? conditionTask.parameters.caseTrue : conditionTask.parameters.caseFalse;

    if (taskIndex === undefined || taskIndex === -1 || typeof taskIndex !== 'number') {
        caseTasks.push(newTask);
    } else {
        caseTasks.splice(taskIndex, 0, newTask);

        conditionTask.parameters = {
            ...conditionTask.parameters,
            [conditionCase as string]: caseTasks,
        };
    }

    return tasks.map((task) => (task.name === conditionId ? conditionTask : task));
}
