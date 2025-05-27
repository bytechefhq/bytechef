import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

export default function getRecursivelyUpdatedTasks(
    tasks: Array<WorkflowTask>,
    taskToReplace: WorkflowTask
): Array<WorkflowTask> {
    if (!Array.isArray(tasks)) {
        return [tasks];
    }

    return tasks.map((task) => {
        if (task.name === taskToReplace.name) {
            return taskToReplace;
        }

        if (task.parameters?.caseTrue || task.parameters?.caseFalse) {
            const updatedTask = {...task};

            if (task.parameters.caseTrue) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    caseTrue: getRecursivelyUpdatedTasks(task.parameters.caseTrue, taskToReplace),
                };
            }

            if (task.parameters.caseFalse) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    caseFalse: getRecursivelyUpdatedTasks(task.parameters.caseFalse, taskToReplace),
                };
            }

            return updatedTask;
        }

        if (task.parameters?.iteratee) {
            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    iteratee: getRecursivelyUpdatedTasks(task.parameters.iteratee, taskToReplace),
                },
            };
        }

        if (task.parameters?.cases) {
            const updatedTask = {...task};

            if (task.parameters.default) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    default: getRecursivelyUpdatedTasks(task.parameters.default, taskToReplace),
                };
            }

            if (task.parameters.cases) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    cases: (task.parameters.cases as BranchCaseType[]).map((caseItem) => {
                        const updatedCaseItem = {...caseItem};

                        if (caseItem.tasks) {
                            updatedCaseItem.tasks = getRecursivelyUpdatedTasks(caseItem.tasks, taskToReplace);
                        }

                        return updatedCaseItem;
                    }),
                };
            }

            return updatedTask;
        }

        if (task.parameters?.tasks) {
            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    tasks: getRecursivelyUpdatedTasks(task.parameters.tasks, taskToReplace),
                },
            };
        }

        return task;
    });
}
