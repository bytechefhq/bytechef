import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphPendingConnectionType, GraphTransitionType, NodeDataType} from '@/shared/types';
import {Connection, Edge, Node, XYPosition} from '@xyflow/react';

import {fromFrameChildPosition} from './graphFrameGeometry';
import {
    GRAPH_START_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX,
} from './graphHandleSuffixes';
import {withMemberNodePosition} from './graphMemberPlacement';
import {addTransition} from './graphTransitionMutations';

export type GraphConnectionType =
    {graphId: string; kind: 'start'; to: string} | {from: string; graphId: string; kind: 'transition'; to: string};

/**
 * Classifies a React Flow connection as one of the two edits a graph frame accepts, or as nothing
 * at all. Both endpoints must belong to the SAME graph, and both must be addressed through the
 * transition handles — the members carry the ordinary chain handles too, and a task dispatcher
 * member carries its own.
 *
 * A self-loop is deliberately allowed: a node may transition back to itself, and the budget is
 * what bounds the resulting cycle.
 *
 * Returns undefined for a `from`/`to` pair the graph already declares. The runtime takes the first
 * matching transition, so a duplicate pair would add a row that can never fire.
 */
export function resolveGraphConnection(
    connection: Connection | Edge,
    nodesById: Map<string, Node>
): GraphConnectionType | undefined {
    const sourceNode = nodesById.get(connection.source);
    const targetNode = nodesById.get(connection.target);

    if (!sourceNode || !targetNode) {
        return undefined;
    }

    const targetGraphId = (targetNode.data as NodeDataType).graphData?.graphId;

    if (!targetGraphId || !connection.targetHandle?.endsWith(GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX)) {
        return undefined;
    }

    const sourceData = sourceNode.data as NodeDataType;
    const startGraphId = sourceData.graphStart?.graphId;

    if (startGraphId) {
        if (startGraphId !== targetGraphId || !connection.sourceHandle?.endsWith(GRAPH_START_SOURCE_HANDLE_SUFFIX)) {
            return undefined;
        }

        return {graphId: targetGraphId, kind: 'start', to: connection.target};
    }

    const sourceGraphId = sourceData.graphData?.graphId;

    if (sourceGraphId !== targetGraphId || !connection.sourceHandle?.endsWith(GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX)) {
        return undefined;
    }

    const transitions = getGraphTransitions(targetGraphId, nodesById);

    if (
        transitions.some((transition) => transition.from === connection.source && transition.to === connection.target)
    ) {
        return undefined;
    }

    return {from: connection.source, graphId: targetGraphId, kind: 'transition', to: connection.target};
}

/** React Flow's `isValidConnection` predicate — see `resolveGraphConnection` for the rules. */
export function isValidGraphConnection(connection: Connection | Edge, nodesById: Map<string, Node>): boolean {
    return !!resolveGraphConnection(connection, nodesById);
}

function getGraphTransitions(graphId: string, nodesById: Map<string, Node>): GraphTransitionType[] {
    return ((nodesById.get(graphId)?.data as NodeDataType | undefined)?.parameters?.transitions ??
        []) as GraphTransitionType[];
}

/**
 * Stamps `metadata.ui.nodePosition` on each named member. Used to flush the positions the layout
 * pre-pass had to invent for members that carried none, so a graph does not reshuffle itself the
 * next time it is laid out.
 */
export function withGraphMemberPositions(
    parameters: Record<string, unknown>,
    positions: Record<string, XYPosition> | undefined
): Record<string, unknown> {
    if (!positions || Object.keys(positions).length === 0) {
        return parameters;
    }

    const members = (parameters.nodes ?? []) as WorkflowTask[];

    return {
        ...parameters,
        nodes: members.map((member) => {
            const position = positions[member.name];

            return position ? withMemberNodePosition(member, position) : member;
        }),
    };
}

interface BuildGraphConnectionParametersPropsI {
    /** The graph's pending auto-placed member positions, flushed alongside the connection. */
    autoPlacedPositions?: Record<string, XYPosition>;
    graphConnection: GraphConnectionType;
    parameters: Record<string, unknown>;
}

/** The `parameters` a resolved connection writes back onto its `graph/v1` task. */
export function buildGraphConnectionParameters({
    autoPlacedPositions,
    graphConnection,
    parameters,
}: BuildGraphConnectionParametersPropsI): Record<string, unknown> {
    const positionedParameters = withGraphMemberPositions(parameters, autoPlacedPositions);

    if (graphConnection.kind === 'start') {
        return {...positionedParameters, startNode: graphConnection.to};
    }

    const transitions = (positionedParameters.transitions ?? []) as GraphTransitionType[];

    return {
        ...positionedParameters,
        transitions: addTransition(transitions, graphConnection.from, graphConnection.to),
    };
}

interface DeriveGraphPendingConnectionPropsI {
    /** The frame node's own origin in absolute flow coordinates. */
    frameAbsolutePosition: XYPosition;
    /** Where the connection was released, in absolute flow coordinates. */
    flowPosition: XYPosition;
    /** The graph the drag started in — undefined when it did not start on a member. */
    fromGraphId: string | undefined;
    /** Which end of a transition the drag began at. */
    fromHandleType: 'source' | 'target' | undefined;
    fromNodeName: string | undefined;
    /** `data-graph-frame-id` of the frame under the pointer, undefined when there is none. */
    hitFrameGraphId: string | undefined;
    isValid: boolean;
}

/**
 * The pending connection a release over empty frame space raises, which is what makes the frame
 * open the component popover and the new task land where the pointer was.
 *
 * A release that landed on a handle is already an `onConnect`, and a release outside the drag's own
 * frame is a plain cancel — a member only ever transitions within its own graph, so dropping into
 * a different frame must not silently move the edit there.
 *
 * Only a drag that began at a SOURCE handle raises one. `from` is the member the transition leaves,
 * so a backwards drag out of a target handle would author the edge pointing the opposite way from
 * the one the user drew. `GraphTransitionHandles` already refuses to start a drag at a target
 * handle; this is the second lock, because the consequence is a silently inverted route rather
 * than a visible failure.
 */
export function deriveGraphPendingConnection({
    flowPosition,
    frameAbsolutePosition,
    fromGraphId,
    fromHandleType,
    fromNodeName,
    hitFrameGraphId,
    isValid,
}: DeriveGraphPendingConnectionPropsI): GraphPendingConnectionType | undefined {
    if (isValid || !fromGraphId || !fromNodeName || fromHandleType !== 'source' || hitFrameGraphId !== fromGraphId) {
        return undefined;
    }

    return {
        dropPosition: toGraphContentPosition(flowPosition, frameAbsolutePosition),
        from: fromNodeName,
        graphId: fromGraphId,
    };
}

/**
 * A point on the canvas expressed in a frame's CONTENT coordinates — what a member's stored
 * `nodePosition` is in. Clamped non-negative, so a release in the header band or left of the frame
 * still lands on a spot the member can occupy.
 */
export function toGraphContentPosition(flowPosition: XYPosition, frameAbsolutePosition: XYPosition): XYPosition {
    const contentPosition = fromFrameChildPosition({
        x: flowPosition.x - frameAbsolutePosition.x,
        y: flowPosition.y - frameAbsolutePosition.y,
    });

    return {x: Math.max(0, contentPosition.x), y: Math.max(0, contentPosition.y)};
}
