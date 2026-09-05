import useDisabledTaskNames from '../hooks/useDisabledTaskNames';
import useTakenEmptyConditionCases from '../hooks/useTakenEmptyConditionCases';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints, isConditionPlaceholderNode} from './getExecutedEdgeStatus';

/**
 * Resolves the executed-path status for an edge by its `source=>target` id. Shared by every edge
 * component so plumbing edges (dispatcher ghosts, labeled case edges) color the same way the main
 * workflow edges do after a test run.
 */
export default function useExecutedEdgeStatus(edgeId: string): 'COMPLETED' | 'FAILED' | undefined {
    const edges = useWorkflowDataStore((state) => state.edges);
    const nodes = useWorkflowDataStore((state) => state.nodes);

    const workflowTestNodeStates = useWorkflowTestNodeStates();
    const disabledTaskNames = useDisabledTaskNames();

    const sourceNodeId = edgeId.split('=>')[0];
    const targetNodeId = edgeId.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const takenEmptyConditionCases = useTakenEmptyConditionCases(
        isConditionPlaceholderNode(sourceNode) || isConditionPlaceholderNode(targetNode)
    );

    const executedEdgeEndpoints = bypassDisabledEdgeEndpoints(sourceNode, targetNode, {
        disabledTaskNames,
        edges,
        nodes,
    });

    return getExecutedEdgeStatus(
        executedEdgeEndpoints.sourceNode,
        executedEdgeEndpoints.targetNode,
        workflowTestNodeStates,
        takenEmptyConditionCases
    );
}
