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

interface UseGraphConnectionsProps {
    updateWorkflowMutation?: UpdateWorkflowMutationType;
}

const GRAPH_FRAME_ID_SELECTOR = `[${GRAPH_FRAME_ID_ATTRIBUTE}]`;

function buildNodesById(): Map<string, Node> {
    const {nodes} = useWorkflowDataStore.getState();

    const nodeEntries = nodes.map((node): [string, Node] => [node.id, node]);

    return new Map(nodeEntries);
}

function getEventClientPosition(event: MouseEvent | TouchEvent): XYPosition | undefined {
    if ('changedTouches' in event) {
        const touch = event.changedTouches[0];

        return touch ? {x: touch.clientX, y: touch.clientY} : undefined;
    }

    return {x: event.clientX, y: event.clientY};
}

function hitTestGraphFrame(clientPosition: XYPosition): string | undefined {
    const element = document.elementFromPoint(clientPosition.x, clientPosition.y);

    return element?.closest(GRAPH_FRAME_ID_SELECTOR)?.getAttribute(GRAPH_FRAME_ID_ATTRIBUTE) ?? undefined;
}

const useGraphConnections = ({updateWorkflowMutation}: UseGraphConnectionsProps) => {
    const {getInternalNode, screenToFlowPosition} = useReactFlow();

    const isValidConnection = useCallback((connection: Connection | Edge) => {
        const nodesById = buildNodesById();

        return isValidGraphConnection(connection, nodesById);
    }, []);

    const saveGraphConnection = useCallback(
        (graphConnection: GraphConnectionType) => {
            if (!updateWorkflowMutation) {
                return;
            }

            const {graphId} = graphConnection;

            const autoPlacedPositions = takeAutoPlacedGraphPositions(graphId);

            saveGraphParameters({
                graphId,
                updateWorkflowMutation,
                updater: (parameters) =>
                    buildGraphConnectionParameters({autoPlacedPositions, graphConnection, parameters}),
            });
        },
        [updateWorkflowMutation]
    );

    const handleConnect = useCallback(
        (connection: Connection) => {
            const nodesById = buildNodesById();

            const graphConnection = resolveGraphConnection(connection, nodesById);

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

            const frameNodeId = getGraphFrameId(fromGraphId);

            const frameNode = getInternalNode(frameNodeId);

            if (!frameNode) {
                return;
            }

            const flowPosition = screenToFlowPosition(clientPosition);

            const hitFrameGraphId = hitTestGraphFrame(clientPosition);

            const pendingConnection = deriveGraphPendingConnection({
                flowPosition,
                frameAbsolutePosition: frameNode.internals.positionAbsolute,
                fromGraphId,
                fromHandleType: connectionState.fromHandle?.type,
                fromNodeName: fromNode.id,
                hitFrameGraphId,
                isValid: !!connectionState.isValid,
            });

            if (pendingConnection) {
                const {setGraphPendingConnection} = useWorkflowEditorStore.getState();

                setGraphPendingConnection(pendingConnection);
            }
        },
        [getInternalNode, screenToFlowPosition, updateWorkflowMutation]
    );

    const handleReconnect = useCallback(
        (oldEdge: Edge, newConnection: Connection) => {
            if (oldEdge.type !== GRAPH_START_EDGE_TYPE) {
                return;
            }

            const nodesById = buildNodesById();

            const graphConnection = resolveGraphConnection(newConnection, nodesById);

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
