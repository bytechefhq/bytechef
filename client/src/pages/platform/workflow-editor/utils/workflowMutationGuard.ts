/**
 * Centralized guard preventing concurrent workflow update mutations.
 *
 * Multiple UI paths (drag-and-drop, property edits, node deletion, position
 * resets) can trigger a PUT /workflows/{id} request. The server uses optimistic
 * locking (version column), so two concurrent updates cause
 * OptimisticLockingFailureException because the second request sends a stale
 * version.
 *
 * Every workflow save utility must call `isWorkflowMutating()` before calling
 * `mutation.mutate()`, and wrap the call with `setWorkflowMutating(true/false)`.
 */

let mutating = false;

export function isWorkflowMutating(): boolean {
    return mutating;
}

export function setWorkflowMutating(value: boolean): void {
    mutating = value;
}
