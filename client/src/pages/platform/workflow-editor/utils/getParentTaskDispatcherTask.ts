import {TASK_DISPATCHER_SUBTASK_COLLECTIONS} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';

const ALL_COLLECTION_NAMES = Object.values(TASK_DISPATCHER_SUBTASK_COLLECTIONS).flat();

type GetParentTaskDispatcherTaskType = {
    taskDispatcherId: string;
    tasks: Array<WorkflowTask>;
};

export default function getParentTaskDispatcherTask({
    taskDispatcherId,
    tasks,
}: GetParentTaskDispatcherTaskType): WorkflowTask | undefined {
    for (const task of tasks) {
        if (task.name === taskDispatcherId) {
            return task;
        }

        if (task.parameters) {
            for (const collectionName of ALL_COLLECTION_NAMES) {
                const subtasks = task.parameters[collectionName];

                if (Array.isArray(subtasks) && subtasks.length > 0) {
                    const foundTask = getParentTaskDispatcherTask({taskDispatcherId, tasks: subtasks});

                    if (foundTask) {
                        return foundTask;
                    }
                }
            }
        }
    }

    return undefined;
}
