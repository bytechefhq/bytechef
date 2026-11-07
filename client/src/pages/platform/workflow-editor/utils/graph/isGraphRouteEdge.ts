import {GRAPH_START_EDGE_TYPE, GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge} from '@xyflow/react';

export default function isGraphRouteEdge(edge: Pick<Edge, 'type'>): boolean {
    return edge.type === GRAPH_TRANSITION_EDGE_TYPE || edge.type === GRAPH_START_EDGE_TYPE;
}
