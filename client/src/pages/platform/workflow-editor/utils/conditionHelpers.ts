import {WorkflowTask} from '@/shared/middleware/platform/configuration';

export function isConditionNested(conditionId: string, tasks: WorkflowTask[]): boolean {
    // Check if this condition appears in any branch of any other condition
    return tasks.some((task) => {
        if (task.type?.includes('condition') && task.parameters && task.name !== conditionId) {
            const allBranchTasks = [...(task.parameters.caseTrue || []), ...(task.parameters.caseFalse || [])];

            return allBranchTasks.some((branchTask) => branchTask.name === conditionId);
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
            const allBranchTasks = [...(parameters.caseTrue || []), ...(parameters.caseFalse || [])];

            return allBranchTasks.some((branchTask) => branchTask.name === conditionId);
        }

        return false;
    });

    return parentCondition?.name;
}
