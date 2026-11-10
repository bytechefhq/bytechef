import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import {forEachNestedTaskGroup} from './taskTraversalUtils';

interface VisitTasksProps {
    availableTaskNamesByTaskName: Map<string, Set<string>>;
    inheritedTaskNames: Set<string>;
    repeated: boolean;
    tasks: Array<WorkflowTask>;
}

function isLoopTask(task: WorkflowTask): boolean {
    return !!task.type?.startsWith('loop/');
}

function visitTasks({availableTaskNamesByTaskName, inheritedTaskNames, repeated, tasks}: VisitTasksProps): void {
    tasks.forEach((currentTask, index) => {
        const availableTaskNames = new Set(inheritedTaskNames);

        const candidateTasks = repeated ? tasks : tasks.slice(0, index);

        for (const candidateTask of candidateTasks) {
            availableTaskNames.add(candidateTask.name);
        }

        if (!availableTaskNamesByTaskName.has(currentTask.name)) {
            availableTaskNamesByTaskName.set(currentTask.name, availableTaskNames);
        }

        if (!currentTask.parameters) {
            return;
        }

        const nestedInheritedTaskNames = new Set([...availableTaskNames, currentTask.name]);
        const nestedTasksRepeated = isLoopTask(currentTask);

        forEachNestedTaskGroup(currentTask.parameters as Record<string, unknown>, (nestedTasks) =>
            visitTasks({
                availableTaskNamesByTaskName,
                inheritedTaskNames: nestedInheritedTaskNames,
                repeated: nestedTasksRepeated,
                tasks: nestedTasks,
            })
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

    const topLevelTasks = tasks.filter((currentTask) => !nestedTaskNames.has(currentTask.name));

    visitTasks({
        availableTaskNamesByTaskName,
        inheritedTaskNames: new Set(),
        repeated: false,
        tasks: topLevelTasks,
    });

    return availableTaskNamesByTaskName;
}
