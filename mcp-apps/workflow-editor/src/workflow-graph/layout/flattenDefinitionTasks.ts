// Ported from client/src/pages/platform/workflow-editor/utils/flattenDefinitionTasks.ts.
// Adaptation: local WorkflowTaskType / BranchCaseType instead of @/shared types.
//
// The website receives the NESTED workflow definition (dispatcher children live inside
// `parameters.caseTrue` / `iteratee` / `cases[].tasks` / `branches[][]`). The layout
// orchestration (mirroring the in-app editor) expects the SERVER-flattened task list, where
// each task appears as a top-level entry followed by its nested subtasks. This recreates that
// flattening so child tasks become nodes.

import {BranchCaseType, WorkflowTaskType} from '../types';

export function flattenDefinitionTasks(tasks: Array<WorkflowTaskType>): Array<WorkflowTaskType> {
    const result: Array<WorkflowTaskType> = [];

    for (const task of tasks) {
        result.push(task);

        if (task.parameters) {
            result.push(...extractNestedTasks(task.parameters));
        }
    }

    return result;
}

function extractNestedTasks(parameters: Record<string, unknown>): Array<WorkflowTaskType> {
    const result: Array<WorkflowTaskType> = [];

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
            result.push(...flattenDefinitionTasks(value as Array<WorkflowTaskType>));

            continue;
        }

        // List of objects with 'tasks' key (e.g., branch.cases)
        if (firstItem && typeof firstItem === 'object' && 'tasks' in firstItem) {
            for (const caseItem of value as Array<BranchCaseType>) {
                if (Array.isArray(caseItem.tasks)) {
                    result.push(...flattenDefinitionTasks(caseItem.tasks as Array<WorkflowTaskType>));
                }
            }

            continue;
        }

        // List of lists (e.g., fork-join.branches)
        if (Array.isArray(firstItem) && firstItem.length > 0 && isWorkflowTask(firstItem[0])) {
            for (const branch of value as Array<Array<WorkflowTaskType>>) {
                result.push(...flattenDefinitionTasks(branch));
            }
        }
    }

    return result;
}

function isWorkflowTask(value: unknown): value is WorkflowTaskType {
    if (value === null || typeof value !== 'object' || !('name' in value) || !('type' in value)) {
        return false;
    }

    const {name, type} = value as {name: unknown; type: unknown};

    return typeof name === 'string' && typeof type === 'string' && type.includes('/');
}
