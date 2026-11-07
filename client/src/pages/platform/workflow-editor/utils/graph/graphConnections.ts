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

    const transitionAlreadyExists = transitions.some(
        (transition) => transition.from === connection.source && transition.to === connection.target
    );

    if (transitionAlreadyExists) {
        return undefined;
    }

    return {from: connection.source, graphId: targetGraphId, kind: 'transition', to: connection.target};
}

export function isValidGraphConnection(connection: Connection | Edge, nodesById: Map<string, Node>): boolean {
    return !!resolveGraphConnection(connection, nodesById);
}

function getGraphTransitions(graphId: string, nodesById: Map<string, Node>): GraphTransitionType[] {
    const graphNodeData = nodesById.get(graphId)?.data as NodeDataType | undefined;

    return (graphNodeData?.parameters?.transitions ?? []) as GraphTransitionType[];
}

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

interface BuildGraphConnectionParametersProps {
    autoPlacedPositions?: Record<string, XYPosition>;
    graphConnection: GraphConnectionType;
    parameters: Record<string, unknown>;
}

export function buildGraphConnectionParameters({
    autoPlacedPositions,
    graphConnection,
    parameters,
}: BuildGraphConnectionParametersProps): Record<string, unknown> {
    const positionedParameters = withGraphMemberPositions(parameters, autoPlacedPositions);

    if (graphConnection.kind === 'start') {
        return {...positionedParameters, startNode: graphConnection.to};
    }

    const transitions = (positionedParameters.transitions ?? []) as GraphTransitionType[];

    const updatedTransitions = addTransition({from: graphConnection.from, to: graphConnection.to, transitions});

    return {
        ...positionedParameters,
        transitions: updatedTransitions,
    };
}

interface DeriveGraphPendingConnectionProps {
    frameAbsolutePosition: XYPosition;
    flowPosition: XYPosition;
    fromGraphId: string | undefined;
    fromHandleType: 'source' | 'target' | undefined;
    fromNodeName: string | undefined;
    hitFrameGraphId: string | undefined;
    isValid: boolean;
}

export function deriveGraphPendingConnection({
    flowPosition,
    frameAbsolutePosition,
    fromGraphId,
    fromHandleType,
    fromNodeName,
    hitFrameGraphId,
    isValid,
}: DeriveGraphPendingConnectionProps): GraphPendingConnectionType | undefined {
    if (isValid || !fromGraphId || !fromNodeName || fromHandleType !== 'source' || hitFrameGraphId !== fromGraphId) {
        return undefined;
    }

    const dropPosition = toGraphContentPosition({flowPosition, frameAbsolutePosition});

    return {
        dropPosition,
        from: fromNodeName,
        graphId: fromGraphId,
    };
}

interface ToGraphContentPositionProps {
    flowPosition: XYPosition;
    frameAbsolutePosition: XYPosition;
}

export function toGraphContentPosition({flowPosition, frameAbsolutePosition}: ToGraphContentPositionProps): XYPosition {
    const frameRelativePosition = {
        x: flowPosition.x - frameAbsolutePosition.x,
        y: flowPosition.y - frameAbsolutePosition.y,
    };

    const contentPosition = fromFrameChildPosition(frameRelativePosition);

    return {x: Math.max(0, contentPosition.x), y: Math.max(0, contentPosition.y)};
}
