import useWorkflowEditorStore, {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

/**
 * Resolves the owning dispatcher's test-run status for a structural ghost node (top/bottom bar, loop left
 * rail) so the dispatcher's executed state colors its plumbing bars the same way it colors its edges.
 */
export default function useTaskDispatcherGhostStatus(data?: unknown): WorkflowTestNodeStateI['status'] | undefined {
    const taskDispatcherId = (data as {taskDispatcherId?: string} | undefined)?.taskDispatcherId;

    return useWorkflowEditorStore((state) =>
        taskDispatcherId ? state.workflowTestNodeStates[taskDispatcherId]?.status : undefined
    );
}
