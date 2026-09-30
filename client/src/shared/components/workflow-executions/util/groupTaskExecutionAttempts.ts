import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';

export interface TaskExecutionAttemptsI<T extends TaskExecution = TaskExecution> {
    latestTaskExecution: T;
    previousTaskExecutions: T[];
}

export const groupTaskExecutionAttempts = <T extends TaskExecution>(
    taskExecutions: T[]
): TaskExecutionAttemptsI<T>[] => {
    const taskExecutionAttemptsByKey = new Map<string, TaskExecutionAttemptsI<T>>();

    taskExecutions.forEach((taskExecution, index) => {
        const key = taskExecution.workflowTask?.name ?? `index-${index}`;
        const taskExecutionAttempts = taskExecutionAttemptsByKey.get(key);

        if (taskExecutionAttempts) {
            taskExecutionAttempts.previousTaskExecutions.push(taskExecutionAttempts.latestTaskExecution);
            taskExecutionAttempts.latestTaskExecution = taskExecution;
        } else {
            taskExecutionAttemptsByKey.set(key, {latestTaskExecution: taskExecution, previousTaskExecutions: []});
        }
    });

    return Array.from(taskExecutionAttemptsByKey.values());
};
