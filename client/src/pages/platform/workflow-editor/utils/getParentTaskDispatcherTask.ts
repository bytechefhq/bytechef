import {WorkflowTask} from '@/shared/middleware/platform/configuration';

const TASK_DISPATCHER_CONFIG = {
    condition: {
        collections: ['caseTrue', 'caseFalse'],
    },
    loop: {
        collections: ['iteratee'],
    },
};

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
    }

    const componentName = taskDispatcherId.split('_')[0] as keyof typeof TASK_DISPATCHER_CONFIG;

    const config = TASK_DISPATCHER_CONFIG[componentName];

    if (!config) {
        console.error(`Unknown task dispatcher type: ${componentName}`);

        return undefined;
    }

    for (const task of tasks) {
        if (!task.parameters) {
            continue;
        }

        for (const collectionName of config.collections) {
            const subtasks = task.parameters[collectionName];

            if (Array.isArray(subtasks) && subtasks.length > 0) {
                const foundTask = getParentTaskDispatcherTask({taskDispatcherId, tasks: subtasks});

                if (foundTask) {
                    return foundTask;
                }
            }
        }
    }

    return undefined;
}
