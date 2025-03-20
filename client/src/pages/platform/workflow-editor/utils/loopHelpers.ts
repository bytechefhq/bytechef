import {WorkflowTask} from '@/shared/middleware/platform/configuration';

/**
 * Check if a loop is nested inside another loop
 */
export function isLoopNested(loopId: string, tasks: WorkflowTask[]): boolean {
    return tasks.some((task) => {
        if (task.type?.startsWith('loop') && task.parameters && task.name !== loopId) {
            const loopChildTasks: WorkflowTask[] = task.parameters.iteratee || [];

            return loopChildTasks.some((childTask) => childTask.name === loopId);
        }

        return false;
    });
}

/**
 * Finds the immediate parent loop of a nested loop
 */
export function findParentLoopId(loopId: string, tasks: WorkflowTask[]): string | undefined {
    const parentLoop = tasks.find((task) => {
        const {parameters, type} = task;

        if (type?.startsWith('loop') && parameters) {
            const loopChildTasks: WorkflowTask[] = parameters.iteration || [];

            return loopChildTasks.some((childTask) => childTask.name === loopId);
        }

        return false;
    });

    return parentLoop?.name;
}
