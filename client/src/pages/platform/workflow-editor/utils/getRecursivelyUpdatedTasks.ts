import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {BranchCaseType} from '@/shared/types';

function updateTaskParameter(task: WorkflowTask, parameterKey: string, parameterValue: unknown): WorkflowTask {
    return {
        ...task,
        parameters: {
            ...task.parameters,
            [parameterKey]: parameterValue,
        },
    };
}

function updateTaskArray(task: WorkflowTask, parameterKey: string, taskToReplace: WorkflowTask): WorkflowTask {
    const tasks = task.parameters?.[parameterKey] as WorkflowTask[];

    if (!tasks) {
        return task;
    }

    return updateTaskParameter(task, parameterKey, getRecursivelyUpdatedTasks(tasks, taskToReplace));
}

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
            let updatedTask = {...task};

            if (task.parameters.caseTrue) {
                updatedTask = updateTaskArray(updatedTask, 'caseTrue', taskToReplace);
            }

            if (task.parameters.caseFalse) {
                updatedTask = updateTaskArray(updatedTask, 'caseFalse', taskToReplace);
            }

            return updatedTask;
        }

        if (task.parameters?.iteratee) {
            if (Array.isArray(task.parameters.iteratee)) {
                return updateTaskArray(task, 'iteratee', taskToReplace);
            } else {
                return updateTaskParameter(
                    task,
                    'iteratee',
                    getRecursivelyUpdatedTasks([task.parameters.iteratee], taskToReplace)[0]
                );
            }
        }

        if (task.parameters?.cases) {
            let updatedTask = {...task};

            if (task.parameters.default) {
                updatedTask = updateTaskArray(updatedTask, 'default', taskToReplace);
            }

            if (task.parameters.cases) {
                const updatedCases = (task.parameters.cases as BranchCaseType[]).map((caseItem) => ({
                    ...caseItem,
                    tasks: caseItem.tasks ? getRecursivelyUpdatedTasks(caseItem.tasks, taskToReplace) : caseItem.tasks,
                }));

                updatedTask = updateTaskParameter(updatedTask, 'cases', updatedCases);
            }

            return updatedTask;
        }

        if (task.parameters?.tasks) {
            return updateTaskArray(task, 'tasks', taskToReplace);
        }

        if (task.parameters?.branches) {
            const updatedBranches = ((task.parameters?.branches as WorkflowTask[][]) || []).map((branch) =>
                getRecursivelyUpdatedTasks(branch, taskToReplace)
            );

            return updateTaskParameter(task, 'branches', updatedBranches);
        }

        return task;
    });
}
