import {groupTaskExecutionAttempts} from '@/shared/components/workflow-executions/util/groupTaskExecutionAttempts';
import {TaskExecution} from '@/shared/middleware/platform/workflow/execution';

const countNestedTaskExecutions = (taskExecution: TaskExecution): number => {
    if (taskExecution.iterations && taskExecution.iterations.length > 0) {
        return taskExecution.iterations.reduce(
            (count, iterationTaskExecutions) => count + countTaskExecutions(iterationTaskExecutions),
            0
        );
    }

    return countTaskExecutions(taskExecution.children ?? []);
};

export const countTaskExecutions = (taskExecutions: TaskExecution[]): number =>
    groupTaskExecutionAttempts(taskExecutions).reduce(
        (count, {latestTaskExecution}) => count + 1 + countNestedTaskExecutions(latestTaskExecution),
        0
    );
