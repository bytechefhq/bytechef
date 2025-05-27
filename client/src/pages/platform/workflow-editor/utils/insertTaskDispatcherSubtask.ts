import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {TaskDispatcherContextType} from '@/shared/types';

import getRecursivelyUpdatedTasks from './getRecursivelyUpdatedTasks';
import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

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

    const {extractContextFromPlaceholder, getSubtasks, getTask, initializeParameters, updateTaskParameters} = config;

    let targetTaskDispatcher = tasks.find((task) => task.name === taskDispatcherId);

    if (!targetTaskDispatcher) {
        targetTaskDispatcher = getTask({taskDispatcherId, tasks});
    }

    if (!targetTaskDispatcher) {
        return tasks;
    }

    if (!targetTaskDispatcher.parameters) {
        targetTaskDispatcher.parameters = initializeParameters();
    }

    let context: TaskDispatcherContextType = {...taskDispatcherContext};

    if (placeholderId && context.index === 0) {
        if (componentName === 'parallel') {
            context = {
                ...context,
                index: targetTaskDispatcher.parameters?.tasks?.length,
            };
        } else if (componentName === 'each') {
            context = {
                ...context,
                index: 0,
            };
        } else {
            const placeholderContext = extractContextFromPlaceholder(placeholderId);

            context = {...context, ...placeholderContext};
        }
    }

    if (componentName === 'each') {
        const updatedTaskDispatcherTask = {
            ...targetTaskDispatcher,
            parameters: {
                ...targetTaskDispatcher.parameters,
                iteratee: newTask,
            },
        };

        return getRecursivelyUpdatedTasks(tasks, updatedTaskDispatcherTask);
    }

    const subtasks = getSubtasks({context, task: targetTaskDispatcher});

    let updatedSubtasks: Array<WorkflowTask>;

    if (context.index === undefined || context.index === -1 || typeof context.index !== 'number') {
        updatedSubtasks = [...subtasks, newTask];
    } else {
        updatedSubtasks = [...subtasks];

        updatedSubtasks.splice(context.index, 0, newTask);
    }

    const updatedTaskDispatcherTask = updateTaskParameters({context, task: targetTaskDispatcher, updatedSubtasks});

    return getRecursivelyUpdatedTasks(tasks, updatedTaskDispatcherTask);
}
