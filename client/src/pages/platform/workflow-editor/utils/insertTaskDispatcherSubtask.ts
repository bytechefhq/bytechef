import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType} from '@/shared/types';

import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

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
    newTask: WorkflowTask;
    placeholderId?: string;
    taskDispatcherContext: TaskDispatcherContextType;
    tasks: Array<WorkflowTask>;
}

export default function insertTaskDispatcherSubtask({
    newTask,
    placeholderId,
    taskDispatcherContext,
    tasks,
}: InsertTaskDispatcherSubtaskProps): Array<WorkflowTask> {
    const taskDispatcherId = taskDispatcherContext.taskDispatcherId;

    const componentName = taskDispatcherId?.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const config = TASK_DISPATCHER_CONFIG[componentName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${componentName}`);

        return tasks;
    }

    const {extractContextFromPlaceholder, getParentTask, getSubtasks, initializeParameters, updateTaskParameters} =
        config;

    let task = tasks.find((task) => task.name === taskDispatcherId);

    if (!task) {
        task = getParentTask({taskDispatcherId, tasks});
    }

    if (!task) {
        return tasks;
    }

    if (!task.parameters) {
        task.parameters = initializeParameters();
    }

    let context: TaskDispatcherContextType = {...taskDispatcherContext};

    if (placeholderId && context.index === 0) {
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
