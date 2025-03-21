import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType} from '@/shared/types';

import getParentTaskDispatcherTask from './getParentTaskDispatcherTask';

type UpdateTaskParametersType = {
    context?: TaskDispatcherContextType;
    task: WorkflowTask;
    updatedSubtasks: Array<WorkflowTask>;
};

const TASK_DISPATCHER_CONFIG = {
    condition: {
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');
            const conditionCase = parts[1] === 'left' ? CONDITION_CASE_TRUE : CONDITION_CASE_FALSE;

            return {
                conditionCase,
                index,
            };
        },
        getParentTask: getParentTaskDispatcherTask,
        getSubtasks: (task: WorkflowTask, context: TaskDispatcherContextType): Array<WorkflowTask> => {
            const conditionCase = context.conditionCase || CONDITION_CASE_TRUE;

            return conditionCase === CONDITION_CASE_TRUE
                ? task.parameters?.caseTrue || []
                : task.parameters?.caseFalse || [];
        },
        initializeParameters: () => ({
            caseFalse: [],
            caseTrue: [],
        }),
        updateTaskParameters: ({context, task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => {
            const conditionCase = context?.conditionCase || CONDITION_CASE_TRUE;

            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    [conditionCase]: updatedSubtasks,
                },
            };
        },
    },
    loop: {
        extractContextFromPlaceholder: (placeholderId: string): TaskDispatcherContextType => {
            const parts = placeholderId.split('-');
            const index = parseInt(parts[parts.length - 1] || '-1');

            return {index};
        },
        getParentTask: getParentTaskDispatcherTask,
        getSubtasks: (task: WorkflowTask): Array<WorkflowTask> => task.parameters?.iteratee || [],
        initializeParameters: () => ({
            iteratee: [],
        }),
        updateTaskParameters: ({task, updatedSubtasks}: UpdateTaskParametersType): WorkflowTask => ({
            ...task,
            parameters: {
                ...task.parameters,
                iteratee: updatedSubtasks,
            },
        }),
    },
};

function updateTasksRecursively(tasks: Array<WorkflowTask>, taskToReplace: WorkflowTask): Array<WorkflowTask> {
    return tasks.map((task) => {
        if (task.name === taskToReplace.name) {
            return taskToReplace;
        }

        if (task.parameters?.caseTrue || task.parameters?.caseFalse) {
            const updatedTask = {...task};

            if (task.parameters.caseTrue) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    caseTrue: updateTasksRecursively(task.parameters.caseTrue, taskToReplace),
                };
            }

            if (task.parameters.caseFalse) {
                updatedTask.parameters = {
                    ...updatedTask.parameters,
                    caseFalse: updateTasksRecursively(task.parameters.caseFalse, taskToReplace),
                };
            }

            return updatedTask;
        }

        if (task.parameters?.iteratee) {
            return {
                ...task,
                parameters: {
                    ...task.parameters,
                    iteratee: updateTasksRecursively(task.parameters.iteratee, taskToReplace),
                },
            };
        }

        return task;
    });
}

interface InsertTaskDispatcherSubtaskProps {
    dispatcherId: string;
    newTask: WorkflowTask;
    placeholderId?: string;
    taskDispatcherContext?: TaskDispatcherContextType;
    tasks: Array<WorkflowTask>;
}

export default function insertTaskDispatcherSubtask({
    dispatcherId,
    newTask,
    placeholderId,
    taskDispatcherContext = {},
    tasks,
}: InsertTaskDispatcherSubtaskProps): Array<WorkflowTask> {
    const componentName = dispatcherId.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const config = TASK_DISPATCHER_CONFIG[componentName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${componentName}`);

        return tasks;
    }

    const {extractContextFromPlaceholder, getParentTask, getSubtasks, initializeParameters, updateTaskParameters} =
        config;

    let task = tasks.find((task) => task.name === dispatcherId);

    if (!task) {
        task = getParentTask({taskDispatcherId: dispatcherId, tasks});
    }

    if (!task) {
        return tasks;
    }

    if (!task.parameters) {
        task.parameters = initializeParameters();
    }

    let context = {...taskDispatcherContext};

    if (placeholderId && !context.index) {
        const placeholderContext = extractContextFromPlaceholder(placeholderId);

        context = {...context, ...placeholderContext};
    }

    const subtasks = getSubtasks(task, context);

    let updatedSubtasks: Array<WorkflowTask>;

    if (context.index === undefined || context.index === -1 || typeof context.index !== 'number') {
        updatedSubtasks = [...subtasks, newTask];
    } else {
        updatedSubtasks = [...subtasks];

        updatedSubtasks.splice(context.index, 0, newTask);
    }

    const updatedTask = updateTaskParameters({context, task, updatedSubtasks});

    return updateTasksRecursively(tasks, updatedTask);
}
