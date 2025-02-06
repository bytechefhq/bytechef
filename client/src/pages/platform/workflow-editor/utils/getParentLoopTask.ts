import {WorkflowTask} from '@/shared/middleware/platform/configuration';

export default function getParentLoopTask(tasks: Array<WorkflowTask>, loopId: string): WorkflowTask | undefined {
    for (const task of tasks) {
        if (task.name === loopId) {
            return task;
        }

        if (task.parameters?.iteratee) {
            const foundTask = getParentLoopTask(task.parameters.iteratee || [], loopId);

            if (foundTask) {
                return foundTask;
            }
        }
    }

    return undefined;
}
