import {EDGE_STYLES} from '@/shared/constants';
import {Node} from '@xyflow/react';

const defaultLabeledEdgeStyle = {
    style: EDGE_STYLES,
    type: 'labeledClusterElementsEdge',
};

export function createEdgeForPlaceholderNode(nodeId: string, elementType: string) {
    return {
        ...defaultLabeledEdgeStyle,
        id: `${nodeId}=>${nodeId}-${elementType}-placeholder-0`,
        source: nodeId,
        sourceHandle: `${elementType}-handle`,
        style: EDGE_STYLES,
        target: `${nodeId}-${elementType}-placeholder-0`,
    };
}

export function createEdgeForMultipleClusterElementNode(nodeId: string, targetNode: Node) {
    return {
        id: `${nodeId}=>${nodeId}-${targetNode.id}`,
        source: nodeId,
        sourceHandle: `${targetNode.data.clusterElementType}-handle`,
        style: EDGE_STYLES,
        target: targetNode.id,
        type: 'default',
    };
}

export function createEdgeForClusterElementNode(nodeId: string, targetNode: Node) {
    return {
        ...defaultLabeledEdgeStyle,
        id: `${nodeId}=>${nodeId}-${targetNode.id}`,
        source: nodeId,
        sourceHandle: `${targetNode.data.clusterElementType}-handle`,
        target: targetNode.id,
    };
}
