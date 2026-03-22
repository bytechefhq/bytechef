import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

/**
 * Iterates over all nested subtask groups within a task dispatcher's parameters,
 * normalizing the various storage formats (array subtasks, branch cases, fork-join branches,
 * single-object iteratee) into a uniform callback interface.
 *
 * Each callback invocation receives one group of subtasks and the parameter key it came from.
 * For 'cases', the callback fires once per case; for 'branches', once per branch.
 */
export function forEachNestedTaskGroup(
    parameters: Record<string, unknown>,
    callback: (tasks: WorkflowTask[], key: string) => void
): void {
    for (const key of ['caseTrue', 'caseFalse', 'default', 'main-branch', 'on-error-branch', 'tasks']) {
        const value = parameters[key];

        if (Array.isArray(value) && value.length > 0 && value[0]?.name) {
            callback(value as WorkflowTask[], key);
        }
    }

    const iteratee = parameters.iteratee;

    if (Array.isArray(iteratee) && iteratee.length > 0 && iteratee[0]?.name) {
        callback(iteratee as WorkflowTask[], 'iteratee');
    } else if (
        iteratee &&
        typeof iteratee === 'object' &&
        !Array.isArray(iteratee) &&
        (iteratee as WorkflowTask).name
    ) {
        callback([iteratee as WorkflowTask], 'iteratee');
    }

    if (Array.isArray(parameters.cases)) {
        for (const caseItem of parameters.cases as BranchCaseType[]) {
            if (Array.isArray(caseItem.tasks)) {
                callback(caseItem.tasks as WorkflowTask[], 'cases');
            }
        }
    }

    if (Array.isArray(parameters.branches)) {
        for (const branch of parameters.branches as WorkflowTask[][]) {
            if (Array.isArray(branch)) {
                callback(branch, 'branches');
            }
        }
    }
}
