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
