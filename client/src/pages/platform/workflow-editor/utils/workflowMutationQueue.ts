let mutationQueue: Promise<unknown> = Promise.resolve();

export function enqueueWorkflowMutation(execute: () => Promise<unknown>): void {
    mutationQueue = mutationQueue.then(execute, execute).catch((error) => {
        console.error('Workflow mutation execution failed:', error);
    });
}
