import {toast} from 'sonner';

export const WORKFLOW_MUTATION_TIMEOUT_MS = 30_000;

let mutationQueue: Promise<unknown> = Promise.resolve();

function executeWithTimeout(execute: () => Promise<unknown>): Promise<unknown> {
    let timeoutId: ReturnType<typeof setTimeout> | undefined;

    const timeout = new Promise<void>((resolve) => {
        timeoutId = setTimeout(() => {
            toast.error('A change is taking too long to save', {
                description: 'It may not have been saved. Check your connection and refresh the page if it is missing.',
            });

            resolve();
        }, WORKFLOW_MUTATION_TIMEOUT_MS);
    });

    return Promise.race([execute(), timeout]).finally(() => clearTimeout(timeoutId));
}

export function enqueueWorkflowMutation(execute: () => Promise<unknown>): void {
    const executeQueued = () => executeWithTimeout(execute);

    mutationQueue = mutationQueue.then(executeQueued, executeQueued).catch((error) => {
        console.error('Workflow mutation execution failed:', error);
    });
}
