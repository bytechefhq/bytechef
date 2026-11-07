import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType} from '@/shared/types';

import {findGraphMembersPrecedingMember} from './graph/graphReachability';
import {forEachNestedTaskGroup} from './taskTraversalUtils';

interface VisitTasksProps {
    availableTaskNamesByTaskName: Map<string, Set<string>>;
    graphTransitions: Array<GraphTransitionType> | undefined;
    inheritedTaskNames: Set<string>;
    repeated: boolean;
    tasks: Array<WorkflowTask>;
}

function isLoopTask(task: WorkflowTask): boolean {
    return !!task.type?.startsWith('loop/');
}

function visitTasks({
    availableTaskNamesByTaskName,
    graphTransitions,
    inheritedTaskNames,
    repeated,
    tasks,
}: VisitTasksProps): void {
    const graphMemberNames = graphTransitions ? tasks.map((memberTask) => memberTask.name) : undefined;

    tasks.forEach((currentTask, index) => {
        const availableTaskNames = new Set(inheritedTaskNames);

        if (graphTransitions && graphMemberNames) {
            const precedingGraphMemberNames = findGraphMembersPrecedingMember(
                currentTask.name,
                graphTransitions,
                graphMemberNames
            );

            for (const precedingGraphMemberName of precedingGraphMemberNames) {
                availableTaskNames.add(precedingGraphMemberName);
            }
        } else {
            const candidateTasks = repeated ? tasks : tasks.slice(0, index);

            for (const candidateTask of candidateTasks) {
                availableTaskNames.add(candidateTask.name);
            }
        }

        if (!availableTaskNamesByTaskName.has(currentTask.name)) {
            availableTaskNamesByTaskName.set(currentTask.name, availableTaskNames);
        }

        if (!currentTask.parameters) {
            return;
        }

        const parameters = currentTask.parameters as Record<string, unknown>;
        const nestedInheritedTaskNames = new Set([...availableTaskNames, currentTask.name]);
        const nestedTasksRepeated = isLoopTask(currentTask);

        forEachNestedTaskGroup(parameters, (nestedTasks, key) => {
            if (key !== 'nodes') {
                visitTasks({
                    availableTaskNamesByTaskName,
                    graphTransitions: undefined,
                    inheritedTaskNames: nestedInheritedTaskNames,
                    repeated: nestedTasksRepeated,
                    tasks: nestedTasks,
                });

                return;
            }

            for (const memberTask of nestedTasks) {
                availableTaskNames.add(memberTask.name);
            }

            const nestedGraphTransitions = Array.isArray(parameters.transitions)
                ? (parameters.transitions as Array<GraphTransitionType>)
                : [];

            visitTasks({
                availableTaskNamesByTaskName,
                graphTransitions: nestedGraphTransitions,
                inheritedTaskNames: nestedInheritedTaskNames,
                repeated: false,
                tasks: nestedTasks,
            });
        });
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

    const rootTasks = tasks.filter((currentTask) => !nestedTaskNames.has(currentTask.name));
    const rootInheritedTaskNames = new Set<string>();

    visitTasks({
        availableTaskNamesByTaskName,
        graphTransitions: undefined,
        inheritedTaskNames: rootInheritedTaskNames,
        repeated: false,
        tasks: rootTasks,
    });

    return availableTaskNamesByTaskName;
}
