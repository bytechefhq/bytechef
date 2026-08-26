import {useMemo} from 'react';

import useWorkflowTestNodeStates from '../hooks/useWorkflowTestNodeStates';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import collectGraphNodeExecutions from './collectGraphNodeExecutions';
import getExecutedEdgeStatus from './getExecutedEdgeStatus';

/** The transition a `graphTransition` edge draws, and the graph whose routing history decides it. */
export interface GraphTransitionEdgeIdentityI {
    from: string;
    graphId: string;
    to: string;
}

/**
 * Resolves the executed-path status for an edge by its `source=>target` id. Shared by every edge
 * component so plumbing edges (dispatcher ghosts, labeled case/graph edges) color the same way the
 * main workflow edges do after a test run.
 *
 * A `graphTransition` edge passes its transition instead: its id is `<graphId>-transition-<index>`,
 * not a `source=>target` pair, and — more importantly — a transition is on the executed path only if
 * the run actually routed along it, which the two endpoints' node states cannot say. The graph's
 * routing history is read from the run's task execution tree and handed to the pure rule.
 */
export default function useExecutedEdgeStatus(
    edgeId: string,
    graphTransition?: GraphTransitionEdgeIdentityI
): 'COMPLETED' | 'FAILED' | undefined {
    const nodes = useWorkflowDataStore((state) => state.nodes);
    // The selector returns a constant `undefined` for the edge types that will never ask for a
    // graph's routing history — every other edge on the canvas. They still subscribe to the store
    // (for `workflowTestNodeStates` below), but this slice can no longer re-render them.
    const workflowTestExecution = useWorkflowEditorStore((state) =>
        graphTransition ? state.workflowTestExecution : undefined
    );
    const workflowTestNodeStates = useWorkflowTestNodeStates();

    const sourceNodeId = edgeId.split('=>')[0];
    const targetNodeId = edgeId.split('=>')[1];

    const sourceNode = nodes.find((node) => node.id === sourceNodeId);
    const targetNode = nodes.find((node) => node.id === targetNodeId);

    // Read out as fields so the memo below can be keyed on them rather than on the transition
    // object: an edge component rebuilds that literal on every render, so depending on it directly
    // would never let the memo hit.
    const transitionFrom = graphTransition?.from;
    const transitionGraphId = graphTransition?.graphId;
    const transitionTo = graphTransition?.to;

    // Memoized because it walks the whole execution tree and the rule then sorts what it returns,
    // while a streaming test run pushes a store update per node-state event — every transition edge
    // on the canvas would otherwise redo both on each of them.
    const graphTransitionExecution = useMemo(
        () =>
            transitionFrom !== undefined && transitionGraphId !== undefined && transitionTo !== undefined
                ? {
                      from: transitionFrom,
                      nodeExecutions: collectGraphNodeExecutions(workflowTestExecution, transitionGraphId),
                      to: transitionTo,
                  }
                : undefined,
        [transitionFrom, transitionGraphId, transitionTo, workflowTestExecution]
    );

    return getExecutedEdgeStatus(sourceNode, targetNode, workflowTestNodeStates, graphTransitionExecution);
}
