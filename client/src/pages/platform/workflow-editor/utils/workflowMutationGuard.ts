/**
 * Centralized guard preventing concurrent workflow update mutations.
 *
 * Multiple UI paths (drag-and-drop, property edits, node deletion, position
 * resets) can trigger a PUT /workflows/{id} request. The server uses optimistic
 * locking (version column), so two concurrent updates cause
 * OptimisticLockingFailureException because the second request sends a stale
 * version.
 *
 * The guard is scoped per workflow ID so that an in-flight mutation on one
 * workflow does not block saves for unrelated workflows.
 *
 * Every workflow save utility must call `isWorkflowMutating(workflowId)` before
 * calling `mutation.mutate()`, and wrap the call with
 * `setWorkflowMutating(workflowId, true/false)`.
 */

const mutatingWorkflows = new Set<string>();

export function isWorkflowMutating(workflowId?: string): boolean {
    if (!workflowId) {
        return mutatingWorkflows.size > 0;
    }

    return mutatingWorkflows.has(workflowId);
}

export function setWorkflowMutating(workflowId: string, value: boolean): void {
    if (value) {
        mutatingWorkflows.add(workflowId);
    } else {
        mutatingWorkflows.delete(workflowId);
    }
}

/**
 * Clears all mutation flags. Useful for cleanup when a workflow editor unmounts
 * to prevent stale flags from blocking future saves.
 */
export function clearAllWorkflowMutations(): void {
    mutatingWorkflows.clear();
}
