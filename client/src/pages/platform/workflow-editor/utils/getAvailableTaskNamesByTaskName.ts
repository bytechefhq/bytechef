import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import {forEachNestedTaskGroup} from './taskTraversalUtils';

function visitTasks(
    tasks: Array<WorkflowTask>,
    inheritedTaskNames: Set<string>,
    availableTaskNamesByTaskName: Map<string, Set<string>>
): void {
    tasks.forEach((currentTask, index) => {
        const availableTaskNames = new Set(inheritedTaskNames);

        for (const precedingTask of tasks.slice(0, index)) {
            availableTaskNames.add(precedingTask.name);
        }

        if (!availableTaskNamesByTaskName.has(currentTask.name)) {
            availableTaskNamesByTaskName.set(currentTask.name, availableTaskNames);
        }

        if (!currentTask.parameters) {
            return;
        }

        const nestedInheritedTaskNames = new Set([...availableTaskNames, currentTask.name]);

        forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) =>
            visitTasks(nestedTasks, nestedInheritedTaskNames, availableTaskNamesByTaskName)
        );
    });
}

/**
 * The tasks whose output each task can read, by task name — the same rule the previous-node-outputs query and the
 * data pill panel apply, so a reference this rules out is one the editor already draws as an unavailable pill.
 *
 * A task sees the siblings declared before it in its own list and, at every enclosing level, the dispatcher that
 * holds it plus that dispatcher's earlier siblings. It does not see a sibling branch (fork-join branches, the
 * opposite condition case, other branch cases) nor the tasks nested inside an earlier dispatcher, whose outputs stay
 * in that dispatcher.
 *
 * The server lists nested tasks both inside their dispatchers and flattened at the top level, so the top-level
 * entries that are also nested somewhere are left out of the walk rather than read as top-level siblings.
 */
export default function getAvailableTaskNamesByTaskName(tasks: Array<WorkflowTask>): Map<string, Set<string>> {
    const nestedTaskNames = new Set<string>();

    for (const currentTask of tasks) {
        if (currentTask.parameters) {
            forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) => {
                for (const nestedTask of nestedTasks) {
                    nestedTaskNames.add(nestedTask.name);
                }
            });
        }
    }

    const availableTaskNamesByTaskName = new Map<string, Set<string>>();

    visitTasks(
        tasks.filter((currentTask) => !nestedTaskNames.has(currentTask.name)),
        new Set(),
        availableTaskNamesByTaskName
    );

    return availableTaskNamesByTaskName;
}
