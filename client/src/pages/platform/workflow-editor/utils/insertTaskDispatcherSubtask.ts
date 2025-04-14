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

/**
 * Insert a new task into the task dispatcher subtask list.
 */
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

    let targetTaskDispatcher = tasks.find((task) => task.name === taskDispatcherId);

    if (!targetTaskDispatcher) {
        targetTaskDispatcher = getParentTask({taskDispatcherId, tasks});
    }

    if (!targetTaskDispatcher) {
        return tasks;
    }

    if (!targetTaskDispatcher.parameters) {
        targetTaskDispatcher.parameters = initializeParameters();
    }

    let context: TaskDispatcherContextType = {...taskDispatcherContext};

    if (placeholderId && context.index === 0) {
        const placeholderContext = extractContextFromPlaceholder(placeholderId);

        context = {...context, ...placeholderContext};
    }

    const subtasks = getSubtasks({context, task: targetTaskDispatcher});

    let updatedSubtasks: Array<WorkflowTask>;

    if (context.index === undefined || context.index === -1 || typeof context.index !== 'number') {
        updatedSubtasks = [...subtasks, newTask];
    } else {
        updatedSubtasks = [...subtasks];

        updatedSubtasks.splice(context.index, 0, newTask);
    }

    const updatedTaskDispatcher = updateTaskParameters({context, task: targetTaskDispatcher, updatedSubtasks});

    return updateTasksRecursively(tasks, updatedTaskDispatcher);
}
