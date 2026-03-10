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
 * When a position save is skipped because a mutation is in flight, the
 * definition is queued so it can be sent once the current mutation settles.
 * Without this, the in-flight mutation's refetch would overwrite the locally
 * synced definition with server data that lacks the skipped positions.
 */

const mutatingWorkflows = new Set<string>();

/**
 * Stores pending definition strings keyed by workflow ID.
 * Set when a position mutation is skipped; consumed in onSettled.
 */
const pendingDefinitions = new Map<string, string>();

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

export function setPendingDefinition(workflowId: string, definition: string): void {
    pendingDefinitions.set(workflowId, definition);
}

export function hasPendingDefinition(workflowId: string): boolean {
    return pendingDefinitions.has(workflowId);
}

export function consumePendingDefinition(workflowId: string): string | undefined {
    const definition = pendingDefinitions.get(workflowId);

    pendingDefinitions.delete(workflowId);

    return definition;
}

/**
 * Clears all mutation flags and pending definitions. Useful for cleanup
 * when a workflow editor unmounts to prevent stale flags from blocking
 * future saves.
 */
export function clearAllWorkflowMutations(): void {
    mutatingWorkflows.clear();
    pendingDefinitions.clear();
}
