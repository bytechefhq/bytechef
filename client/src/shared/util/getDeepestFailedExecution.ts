import {TaskExecution, TriggerExecution} from '../middleware/platform/workflow/test';

interface GetDeepestFailedExecutionProps {
    currentPath: string[];
    execution: TaskExecution | TriggerExecution;
    isTriggerExecution?: boolean;
}
export default function getDeepestFailedExecution({
    currentPath,
    execution,
    isTriggerExecution = false,
}: GetDeepestFailedExecutionProps): {execution: TaskExecution | TriggerExecution; path: string[]} | null {
    const path = execution.id ? [...currentPath, execution.id] : currentPath;

    if (isTriggerExecution && execution.error) {
        return {execution, path};
    }

    if ('iterations' in execution && execution.iterations && execution.iterations.length > 0) {
        for (const [iterationIndex, iteration] of execution.iterations.entries()) {
            const iterationId = `${execution.id}-iteration-${iterationIndex}`;

            const iterationPath = [...path, iterationId];

            for (const iterationTask of iteration) {
                const failedIterationTask = getDeepestFailedExecution({
                    currentPath: iterationPath,
                    execution: iterationTask,
                    isTriggerExecution,
                });

                if (failedIterationTask) {
                    return failedIterationTask;
                }
            }
        }

        return null;
    }

    if ('children' in execution && execution.children && execution.children.length > 0) {
        for (const child of execution.children) {
            const failedChildExecution = getDeepestFailedExecution({
                currentPath: path,
                execution: child,
                isTriggerExecution,
            });

            if (failedChildExecution) {
                return failedChildExecution;
            }
        }

        return null;
    }

    if (execution.error) {
        return {execution, path};
    }

    return null;
}
