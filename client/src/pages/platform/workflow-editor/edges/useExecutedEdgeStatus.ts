import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import getExecutedEdgeStatus from './getExecutedEdgeStatus';

/**
 * Resolves the executed-path status for an edge by its `source=>target` id. Shared by every edge
 * component so plumbing edges (dispatcher ghosts, labeled case/graph edges) color the same way the
 * main workflow edges do after a test run.
 */
export default function useExecutedEdgeStatus(edgeId: string): 'COMPLETED' | 'FAILED' | undefined {
    const nodes = useWorkflowDataStore((state) => state.nodes);
    const workflowTestNodeStates = useWorkflowEditorStore((state) => state.workflowTestNodeStates);

    const sourceNodeId = edgeId.split('=>')[0];
    const targetNodeId = edgeId.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    return getExecutedEdgeStatus(sourceNode, targetNode, workflowTestNodeStates);
}
