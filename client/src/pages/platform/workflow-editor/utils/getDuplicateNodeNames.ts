import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';

import {forEachNestedTaskGroup} from './taskTraversalUtils';

function collectTaskNames(tasks: Array<WorkflowTask>, names: Array<string>): void {
    for (const task of tasks) {
        if (task?.name) {
            names.push(task.name);
        }

        if (task?.parameters) {
            forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (nestedTasks) =>
                collectTaskNames(nestedTasks, names)
            );
        }
    }
}

/**
 * Collects node names (triggers + tasks, including tasks nested inside condition,
 * loop, branch, parallel, each, fork-join and on-error dispatchers) that occur
 * more than once.
 *
 * Node names are the ids of the nodes rendered in the workflow editor, so a
 * duplicate name produces two nodes with the same id. That collision makes the
 * graph layout resolve edges against the wrong node and can render a broken
 * graph, so the editor surfaces it to the user instead of silently mislaying it.
 */
export default function getDuplicateNodeNames(
    tasks: Array<WorkflowTask> = [],
    triggers: Array<WorkflowTrigger> = []
): Array<string> {
    const names: Array<string> = [];

    for (const trigger of triggers) {
        if (trigger?.name) {
            names.push(trigger.name);
        }
    }

    collectTaskNames(tasks, names);

    const seenNames = new Set<string>();
    const duplicateNames = new Set<string>();

    for (const name of names) {
        if (seenNames.has(name)) {
            duplicateNames.add(name);
        } else {
            seenNames.add(name);
        }
    }

    return [...duplicateNames];
}
