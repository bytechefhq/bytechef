import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {useMemo} from 'react';

import useDisabledTaskNames from '../hooks/useDisabledTaskNames';
import useTakenEmptyConditionCases from '../hooks/useTakenEmptyConditionCases';
import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import collectGraphNodeExecutions from './collectGraphNodeExecutions';
import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints, isConditionPlaceholderNode} from './getExecutedEdgeStatus';

export interface GraphTransitionEdgeIdentityI {
    dynamic?: boolean;
    from: string;
    graphId: string;
    to: string;
}

export default function useExecutedEdgeStatus(
    edgeId: string,
    graphTransition?: GraphTransitionEdgeIdentityI
): 'COMPLETED' | 'FAILED' | undefined {
    const edges = useWorkflowDataStore((state) => state.edges);
    const nodes = useWorkflowDataStore((state) => state.nodes);
    const testTaskExecutions = useWorkflowEditorStore((state) =>
        graphTransition ? state.workflowTestExecution?.job?.taskExecutions : undefined
    );
    const workflowExecutionOverlay = useWorkflowExecutionOverlay();
    const workflowTestNodeStates = useWorkflowTestNodeStates();
    const disabledTaskNames = useDisabledTaskNames();

    const sourceNodeId = edgeId.split('=>')[0];
    const targetNodeId = edgeId.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    const takenEmptyConditionCases = useTakenEmptyConditionCases(
        isConditionPlaceholderNode(sourceNode) || isConditionPlaceholderNode(targetNode)
    );

    const taskExecutions = workflowExecutionOverlay ? workflowExecutionOverlay.taskExecutions : testTaskExecutions;

    const transitionDynamic = graphTransition?.dynamic;
    const transitionFrom = graphTransition?.from;
    const transitionGraphId = graphTransition?.graphId;
    const transitionTo = graphTransition?.to;

    const staticTargetNames = useMemo(() => {
        if (!transitionDynamic || transitionFrom === undefined || transitionGraphId === undefined) {
            return undefined;
        }

        return edges.reduce<string[]>((targetNames, edge) => {
            const edgeData = edge.data as {dynamic?: boolean; graphId?: string; to?: string} | undefined;

            if (
                edge.type === GRAPH_TRANSITION_EDGE_TYPE &&
                edge.source === transitionFrom &&
                edgeData?.graphId === transitionGraphId &&
                !edgeData.dynamic &&
                edgeData.to
            ) {
                targetNames.push(edgeData.to);
            }

            return targetNames;
        }, []);
    }, [edges, transitionDynamic, transitionFrom, transitionGraphId]);

    const graphTransitionExecution = useMemo(
        () =>
            transitionFrom !== undefined && transitionGraphId !== undefined && transitionTo !== undefined
                ? {
                      dynamic: transitionDynamic,
                      from: transitionFrom,
                      nodeExecutionRuns: collectGraphNodeExecutions(taskExecutions, transitionGraphId),
                      staticTargetNames,
                      to: transitionTo,
                  }
                : undefined,
        [staticTargetNames, taskExecutions, transitionDynamic, transitionFrom, transitionGraphId, transitionTo]
    );

    const executedEdgeEndpoints = bypassDisabledEdgeEndpoints(sourceNode, targetNode, {
        disabledTaskNames,
        edges,
        nodes,
    });

    return getExecutedEdgeStatus({
        graphTransition: graphTransitionExecution,
        sourceNode: executedEdgeEndpoints.sourceNode,
        takenEmptyConditionCases,
        targetNode: executedEdgeEndpoints.targetNode,
        workflowTestNodeStates,
    });
}
