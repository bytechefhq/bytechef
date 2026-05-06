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
        let failedChild = null;

        execution.iterations.forEach((iteration, index) => {
            const iterationId = `${execution.id}-iteration-${index}`;

            iteration.forEach(
                (iterationTask) =>
                    (failedChild = getDeepestFailedExecution({
                        currentPath: [...path, iterationId],
                        execution: iterationTask,
                        isTriggerExecution,
                    }))
            );
        });

        return failedChild;
    }

    if ('children' in execution && execution.children && execution.children.length > 0) {
        let failedChild = null;

        execution.children.forEach(
            (child) =>
                (failedChild = getDeepestFailedExecution({
                    currentPath: path,
                    execution: child,
                    isTriggerExecution,
                }))
        );

        return failedChild;
    }

    if (execution.error) {
        return {execution, path};
    }

    return null;
}
