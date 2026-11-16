import {WorkflowTask} from '@/shared/middleware/platform/configuration';

import getAllTasksRecursively from './getAllTasksRecursively';
import {getWorkflowNodeComponentName, getWorkflowTaskComponentName} from './workflowNodeNameUtils';

export default function getTaskDispatcherComponentName(
    taskDispatcherId: string,
    tasks: Array<WorkflowTask> = []
): string {
    const taskDispatcherTask = getAllTasksRecursively(tasks).find((task) => task.name === taskDispatcherId);

    return taskDispatcherTask
        ? getWorkflowTaskComponentName(taskDispatcherTask)
        : getWorkflowNodeComponentName(taskDispatcherId);
}
