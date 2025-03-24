import {WorkflowTask} from '@/shared/middleware/platform/configuration';

export function isConditionNested(conditionId: string, tasks: WorkflowTask[]): boolean {
    return tasks.some((task) => {
        if (task.type?.startsWith('condition/') && task.parameters && task.name !== conditionId) {
            const subtasks = [...task.parameters.caseTrue, ...task.parameters.caseFalse];

            return subtasks.some((subtask) => subtask.name === conditionId);
        } else if (task.type?.startsWith('loop/') && task.parameters && task.name !== conditionId) {
            const subtasks: WorkflowTask[] = task.parameters.iteratee;

            return subtasks?.some((childTask) => childTask.name === conditionId);
        }

        return false;
    });
}

/**
 * Finds the immediate parent condition of a nested condition
 */
export function findParentConditionId(conditionId: string, tasks: WorkflowTask[]): string | undefined {
    const parentCondition = tasks.find((task) => {
        const {parameters, type} = task;

        if (type?.includes('condition/') && parameters) {
            const subtasks = [...parameters.caseTrue, ...parameters.caseFalse];

            return subtasks?.some((subtask) => subtask.name === conditionId);
        }

        return false;
    });

    return parentCondition?.name;
}
