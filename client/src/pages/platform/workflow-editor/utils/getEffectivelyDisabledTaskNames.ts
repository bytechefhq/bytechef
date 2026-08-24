import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

import {isWorkflowTask} from './flattenDefinitionTasks';

/**
 * Client mirror of the server's disabled-task walk: a task is "effectively disabled" when it
 * carries its own `disabled: true` flag OR sits under an ancestor that does, at any nesting
 * depth. The ancestor state is derived here at call time -- nothing is written back into the
 * task tree, so re-enabling a dispatcher instantly "restores" every descendant.
 *
 * Nesting shapes covered: a task's `parameters` (where task dispatchers nest subtasks) and its
 * `pre`/`post`/`finalize` hook lists -- matching the server's `collectDisabledTaskNames`, which
 * walks the whole task map, and `toggleNodeDisabled`, which toggles into those hook lists. The
 * canvas does not render hook subtasks today, so this costs nothing at render time; it exists so
 * the three walks that must mirror each other actually cover the same shapes.
 *
 * Two heuristics are deliberately NOT aligned with the server and must stay as they are unless
 * changed everywhere at once:
 *
 * - `isWorkflowTask` (from `flattenDefinitionTasks`) is looser than the server's
 *   `isWorkflowTaskMap`: it accepts any `type` string containing `/`, while the server requires
 *   `componentName/vN[/operation]`. Tightening it here alone would desync this walk from
 *   `flattenDefinitionTasks`, which builds `workflow.tasks`.
 * - The array walk classifies a list by inspecting only `value[0]`, inherited from
 *   `flattenDefinitionTasks`. A heterogeneous list whose first element is not task-shaped is
 *   skipped wholesale.
 */
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

function collectNestedDisabledTaskNames(
    parameters: Record<string, unknown>,
    ancestorDisabled: boolean,
    disabledTaskNames: Set<string>
): void {
    for (const value of Object.values(parameters)) {
        if (!value) {
            continue;
        }

        // Single task object (e.g., each.iteratee)
        if (isWorkflowTask(value)) {
            collectEffectivelyDisabledTaskNames([value], ancestorDisabled, disabledTaskNames);

            continue;
        }

        if (!Array.isArray(value) || value.length === 0) {
            continue;
        }

        const firstItem = value[0];

        // List of tasks (e.g., condition.caseTrue, loop.iteratee, parallel.tasks)
        if (isWorkflowTask(firstItem)) {
            collectEffectivelyDisabledTaskNames(value as Array<WorkflowTask>, ancestorDisabled, disabledTaskNames);

            continue;
        }

        // List of objects with 'tasks' key (e.g., branch.cases)
        if (firstItem && typeof firstItem === 'object' && 'tasks' in firstItem) {
            for (const caseItem of value as Array<BranchCaseType>) {
                if (Array.isArray(caseItem.tasks)) {
                    collectEffectivelyDisabledTaskNames(
                        caseItem.tasks as Array<WorkflowTask>,
                        ancestorDisabled,
                        disabledTaskNames
                    );
                }
            }

            continue;
        }

        // List of lists (e.g., fork-join.branches)
        if (Array.isArray(firstItem) && firstItem.length > 0 && isWorkflowTask(firstItem[0])) {
            for (const branch of value as Array<Array<WorkflowTask>>) {
                collectEffectivelyDisabledTaskNames(branch, ancestorDisabled, disabledTaskNames);
            }
        }
    }
}
