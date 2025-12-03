import {TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import {TASK_DISPATCHER_CONFIG} from './taskDispatcherConfig';

/**
 * Recursively gets all tasks from a workflow, including tasks nested inside task dispatchers
 */
export default function getAllTasksRecursively(tasks: Array<WorkflowTask>): Array<WorkflowTask> {
    const allTasks: Array<WorkflowTask> = [];

    const extractTasks = (taskList: Array<WorkflowTask>) => {
        taskList.forEach((task) => {
            allTasks.push(task);

            // Check if this is a task dispatcher
            const componentName = task.type?.split('/')[0];

            if (componentName && TASK_DISPATCHER_NAMES.includes(componentName)) {
                const config = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG];

                if (config) {
                    const subtasks = config.getSubtasks({getAllSubtasks: true, task});

                    if (subtasks && subtasks.length > 0) {
                        extractTasks(subtasks);
                    }
                }
            }
        });
    };

    extractTasks(tasks);

    return allTasks;
}
