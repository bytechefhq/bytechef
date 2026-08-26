import {GRAPH_START_EDGE_TYPE} from '@/shared/constants';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {Connection, Edge, FinalConnectionState, Node, XYPosition, useReactFlow} from '@xyflow/react';
import {useCallback} from 'react';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import {takeAutoPlacedGraphPositions} from '../utils/graph/autoPlacedGraphPositions';
import {
    GraphConnectionType,
    buildGraphConnectionParameters,
    deriveGraphPendingConnection,
    isValidGraphConnection,
    resolveGraphConnection,
} from '../utils/graph/graphConnections';
import {GRAPH_FRAME_ID_ATTRIBUTE, getGraphFrameId} from '../utils/graph/graphFrameGeometry';
import {saveGraphParameters} from '../utils/graph/saveGraphParameters';

interface UseGraphConnectionsPropsI {
    updateWorkflowMutation?: UpdateWorkflowMutationType;
}

function buildNodesById(): Map<string, Node> {
    return new Map(useWorkflowDataStore.getState().nodes.map((node) => [node.id, node]));
}

/** The client coordinates a pointer or touch release happened at. */
function getEventClientPosition(event: MouseEvent | TouchEvent): XYPosition | undefined {
    if ('changedTouches' in event) {
        const touch = event.changedTouches[0];

        return touch ? {x: touch.clientX, y: touch.clientY} : undefined;
    }

    return {x: event.clientX, y: event.clientY};
}

/** The graph whose frame element sits under the given client point, if any. */
function hitTestGraphFrame(clientPosition: XYPosition): string | undefined {
    const element = document.elementFromPoint(clientPosition.x, clientPosition.y);

    return element?.closest(`[${GRAPH_FRAME_ID_ATTRIBUTE}]`)?.getAttribute(GRAPH_FRAME_ID_ATTRIBUTE) ?? undefined;
}

/**
 * The canvas's connection lifecycle for `graph/v1` frames: drawing a transition between two
 * members, re-pointing the Start edge, and releasing a transition over empty frame space to add the
 * node it should lead to.
 *
 * Graph member handles are the canvas's only connectable ones — `nodesConnectable` is off globally
 * and every other node's handles are painted purely as chain anchors — so these handlers can treat
 * any connection that reaches them as a graph edit, and reject the ones that are not.
 */
const useGraphConnections = ({updateWorkflowMutation}: UseGraphConnectionsPropsI) => {
    const {getInternalNode, screenToFlowPosition} = useReactFlow();

    const isValidConnection = useCallback(
        (connection: Connection | Edge) => isValidGraphConnection(connection, buildNodesById()),
        []
    );

    const saveGraphConnection = useCallback(
        (graphConnection: GraphConnectionType) => {
            if (!updateWorkflowMutation) {
                return;
            }

            const {graphId} = graphConnection;

            // A connect is a first interaction with the graph, so it also persists whatever the
            // layout pre-pass had to place itself — otherwise those siblings stay unplaced and the
            // next pass re-invents their spots around the transition just drawn.
            const autoPlacedPositions = takeAutoPlacedGraphPositions(graphId);

            saveGraphParameters(
                graphId,
                (parameters) => buildGraphConnectionParameters({autoPlacedPositions, graphConnection, parameters}),
                updateWorkflowMutation
            );
        },
        [updateWorkflowMutation]
    );

    const handleConnect = useCallback(
        (connection: Connection) => {
            const graphConnection = resolveGraphConnection(connection, buildNodesById());

            if (graphConnection) {
                saveGraphConnection(graphConnection);
            }
        },
        [saveGraphConnection]
    );

    const handleConnectEnd = useCallback(
        (event: MouseEvent | TouchEvent, connectionState: FinalConnectionState) => {
            if (!updateWorkflowMutation) {
                return;
            }

            const fromNode = connectionState.fromNode;
            const fromGraphId = (fromNode?.data as NodeDataType | undefined)?.graphData?.graphId;

            if (!fromNode || !fromGraphId) {
                return;
            }

            const clientPosition = getEventClientPosition(event);

            if (!clientPosition) {
                return;
            }

            const frameNode = getInternalNode(getGraphFrameId(fromGraphId));

            if (!frameNode) {
                return;
            }

            const pendingConnection = deriveGraphPendingConnection({
                flowPosition: screenToFlowPosition(clientPosition),
                frameAbsolutePosition: frameNode.internals.positionAbsolute,
                fromGraphId,
                fromHandleType: connectionState.fromHandle?.type,
                fromNodeName: fromNode.id,
                hitFrameGraphId: hitTestGraphFrame(clientPosition),
                isValid: !!connectionState.isValid,
            });

            if (pendingConnection) {
                useWorkflowEditorStore.getState().setGraphPendingConnection(pendingConnection);
            }
        },
        [getInternalNode, screenToFlowPosition, updateWorkflowMutation]
    );

    const handleReconnect = useCallback(
        (oldEdge: Edge, newConnection: Connection) => {
            // Only the Start edge is re-pointable on the canvas. A transition's `to` is edited in
            // its own popover, where the condition that selects it is edited alongside.
            if (oldEdge.type !== GRAPH_START_EDGE_TYPE) {
                return;
            }

            const graphConnection = resolveGraphConnection(newConnection, buildNodesById());

            // The edge only opts its target end into reconnecting, so this holds today; the guard
            // is what keeps a dragged pill END from being saved as a member-to-member transition
            // if that ever changes.
            if (graphConnection?.kind !== 'start') {
                return;
            }

            saveGraphConnection(graphConnection);
        },
        [saveGraphConnection]
    );

    return {handleConnect, handleConnectEnd, handleReconnect, isValidConnection};
};

export default useGraphConnections;
