import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

/**
 * Flattens hierarchical definition tasks into a flat array matching the server's
 * WorkflowTaskUtils.getTasks(tasks, null) output. Each task appears first, followed
 * by its nested subtasks (recursively).
 */
export function flattenDefinitionTasks(tasks: Array<WorkflowTask>): Array<WorkflowTask> {
    const result: Array<WorkflowTask> = [];

    for (const task of tasks) {
        result.push(task);

        if (task.parameters) {
            result.push(...extractNestedTasks(task.parameters));
        }
    }

    return result;
}

function extractNestedTasks(parameters: Record<string, unknown>): Array<WorkflowTask> {
    const result: Array<WorkflowTask> = [];

    for (const value of Object.values(parameters)) {
        if (!value) {
            continue;
        }

        // Single task object (e.g., each.iteratee)
        if (isWorkflowTask(value)) {
            result.push(...flattenDefinitionTasks([value]));

            continue;
        }

        if (!Array.isArray(value) || value.length === 0) {
            continue;
        }

        const firstItem = value[0];

        // List of tasks (e.g., condition.caseTrue, loop.iteratee, parallel.tasks)
        if (isWorkflowTask(firstItem)) {
            result.push(...flattenDefinitionTasks(value as Array<WorkflowTask>));

            continue;
        }

        // List of objects with 'tasks' key (e.g., branch.cases)
        if (firstItem && typeof firstItem === 'object' && 'tasks' in firstItem) {
            for (const caseItem of value as Array<BranchCaseType>) {
                if (Array.isArray(caseItem.tasks)) {
                    result.push(...flattenDefinitionTasks(caseItem.tasks as Array<WorkflowTask>));
                }
            }

            continue;
        }

        // List of lists (e.g., fork-join.branches)
        if (Array.isArray(firstItem) && firstItem.length > 0 && isWorkflowTask(firstItem[0])) {
            for (const branch of value as Array<Array<WorkflowTask>>) {
                result.push(...flattenDefinitionTasks(branch));
            }
        }
    }

    return result;
}

function isWorkflowTask(value: unknown): value is WorkflowTask {
    return value !== null && typeof value === 'object' && 'name' in value && 'type' in value;
}
