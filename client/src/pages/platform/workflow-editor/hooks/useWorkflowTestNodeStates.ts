import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore, {type WorkflowTestNodeStateI} from '../stores/useWorkflowEditorStore';

const EMPTY_WORKFLOW_TEST_NODE_STATES: Record<string, WorkflowTestNodeStateI> = {};

/**
 * Per-node status of the last test run, but only when that run belongs to the workflow currently open.
 * The states outlive a single workflow and are keyed by node name alone, so without this guard a
 * workflow opened after a run inherits the colors of every node whose name it happens to share.
 */
export default function useWorkflowTestNodeStates(): Record<string, WorkflowTestNodeStateI> {
    const workflowId = useWorkflowDataStore((state) => state.workflow.id);
    const workflowTestNodeStates = useWorkflowEditorStore((state) => state.workflowTestNodeStates);
    const workflowTestNodeStatesWorkflowId = useWorkflowEditorStore((state) => state.workflowTestNodeStatesWorkflowId);

    if (workflowTestNodeStatesWorkflowId === undefined || workflowTestNodeStatesWorkflowId !== workflowId) {
        return EMPTY_WORKFLOW_TEST_NODE_STATES;
    }

    return workflowTestNodeStates;
}
