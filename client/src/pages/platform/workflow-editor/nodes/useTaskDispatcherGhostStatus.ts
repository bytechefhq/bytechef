import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import {WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

export default function useTaskDispatcherGhostStatus(data?: unknown): WorkflowTestNodeStateI['status'] | undefined {
    const taskDispatcherId = (data as {taskDispatcherId?: string} | undefined)?.taskDispatcherId;

    const workflowTestNodeStates = useWorkflowTestNodeStates();

    return taskDispatcherId ? workflowTestNodeStates[taskDispatcherId]?.status : undefined;
}
