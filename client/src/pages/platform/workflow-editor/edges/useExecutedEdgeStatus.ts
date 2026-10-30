import useDisabledTaskNames from '../hooks/useDisabledTaskNames';
import useTakenEmptyConditionCases from '../hooks/useTakenEmptyConditionCases';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints, isConditionPlaceholderNode} from './getExecutedEdgeStatus';

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
