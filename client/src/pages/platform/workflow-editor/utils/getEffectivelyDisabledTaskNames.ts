import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

import {isTaskBranchList, isWorkflowTask} from './flattenDefinitionTasks';

export function getEffectivelyDisabledTaskNames(tasks: Array<WorkflowTask>): Set<string> {
    const disabledTaskNames = new Set<string>();

    collectEffectivelyDisabledTaskNames(tasks, false, disabledTaskNames);

    return disabledTaskNames;
}

function collectEffectivelyDisabledTaskNames(
    tasks: Array<WorkflowTask>,
    ancestorDisabled: boolean,
    disabledTaskNames: Set<string>
): void {
    for (const task of tasks) {
        const taskDisabled = ancestorDisabled || !!task.disabled;

        if (taskDisabled) {
            disabledTaskNames.add(task.name);
        }

        for (const hookTasks of [task.pre, task.post, task.finalize]) {
            if (Array.isArray(hookTasks)) {
                collectEffectivelyDisabledTaskNames(hookTasks, taskDisabled, disabledTaskNames);
            }
        }

        if (task.parameters) {
            collectNestedDisabledTaskNames(task.parameters, taskDisabled, disabledTaskNames);
        }
    }
}

function getNestedTaskLists(value: unknown): Array<Array<WorkflowTask>> {
    if (isWorkflowTask(value)) {
        return [[value]];
    }

    if (!Array.isArray(value) || value.length === 0) {
        return [];
    }

    const firstItem = value[0];

    if (isWorkflowTask(firstItem)) {
        return [value as Array<WorkflowTask>];
    }

    if (firstItem && typeof firstItem === 'object' && 'tasks' in firstItem) {
        return (value as Array<BranchCaseType>)
            .filter((caseItem) => Array.isArray(caseItem.tasks))
            .map((caseItem) => caseItem.tasks as Array<WorkflowTask>);
    }

    if (isTaskBranchList(value)) {
        return value as Array<Array<WorkflowTask>>;
    }

    return [];
}

function collectNestedDisabledTaskNames(
    parameters: Record<string, unknown>,
    ancestorDisabled: boolean,
    disabledTaskNames: Set<string>
): void {
    for (const value of Object.values(parameters)) {
        for (const nestedTasks of getNestedTaskLists(value)) {
            collectEffectivelyDisabledTaskNames(nestedTasks, ancestorDisabled, disabledTaskNames);
        }
    }
}
