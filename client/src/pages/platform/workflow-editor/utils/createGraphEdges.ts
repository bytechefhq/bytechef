import {EDGE_STYLES, GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {Edge, MarkerType, Node} from '@xyflow/react';

import {getGraphFrameId} from './graph/graphFrameGeometry';
import {
    GRAPH_START_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX,
    GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX,
    GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX,
} from './graph/graphHandleSuffixes';
import {isDynamicTransitionTarget} from './graph/graphTransitionMutations';

const GRAPH_TRANSITION_EDGE_Z_INDEX = 5;

export default function createGraphEdges(graphNode: Node): Edge[] {
    const nodeData: NodeDataType = graphNode.data as NodeDataType;

    const graphId = graphNode.id;
    const frameId = getGraphFrameId(graphId);
    const startId = `${graphId}-graph-start`;

    const nodes: Array<WorkflowTask> = nodeData.parameters?.nodes ?? [];
    const transitions: Array<GraphTransitionType> = nodeData.parameters?.transitions ?? [];

    const memberNames = new Set(nodes.map((node) => node.name));

    const edges: Edge[] = [
        {
            id: `${graphId}=>${frameId}`,
            source: graphId,
            style: EDGE_STYLES,
            target: frameId,
            targetHandle: `${frameId}-top`,
            type: 'smoothstep',
        },
    ];

    const declaredStartNode = nodeData.parameters?.startNode?.trim();
    const startNodeName = declaredStartNode || nodes[0]?.name;

    if (startNodeName) {
        edges.push({
            id: `${startId}=>${startNodeName}`,
            reconnectable: 'target',
            source: startId,
            sourceHandle: `${startId}${GRAPH_START_SOURCE_HANDLE_SUFFIX}`,
            target: startNodeName,
            targetHandle: `${startNodeName}${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`,
            type: GRAPH_START_EDGE_TYPE,
        });
    }

    transitions.forEach((transition, index) => {
        const dynamic = isDynamicTransitionTarget(transition.to);

        const dangling = !memberNames.has(transition.from) || (!dynamic && !memberNames.has(transition.to));

        edges.push({
            data: {
                condition: transition.condition,
                dangling,
                dynamic,
                graphId,
                index,
                to: transition.to,
            },
            id: `${graphId}-transition-${index}`,
            markerEnd: {type: MarkerType.ArrowClosed},
            source: transition.from,
            sourceHandle: `${transition.from}${GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX}`,
            target: dynamic ? transition.from : transition.to,
            targetHandle: dynamic
                ? `${transition.from}${GRAPH_TRANSITION_DYNAMIC_HANDLE_SUFFIX}`
                : `${transition.to}${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`,
            type: GRAPH_TRANSITION_EDGE_TYPE,
            zIndex: GRAPH_TRANSITION_EDGE_Z_INDEX,
        });
    });

    return edges;
}
