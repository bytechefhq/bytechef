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
        target: `${nodeId}-${elementType}-placeholder-0`,
    };
}

export function createEdgeForMultipleElementsGhostNode(nodeId: string, elementType: string) {
    return {
        ...defaultLabeledEdgeStyle,
        id: `${nodeId}=>${nodeId}-${elementType}-ghost`,
        source: nodeId,
        sourceHandle: `${elementType}-handle`,
        target: `${nodeId}-${elementType}-ghost`,
    };
}

export function createEdgeForMultipleElementsPlaceholderNode(
    currentNodeId: string,
    elementType: string,
    rootNodeId: string
) {
    return {
        id: `${currentNodeId}=>${rootNodeId}-${elementType}-placeholder-0`,
        source: currentNodeId,
        style: EDGE_STYLES,
        target: `${rootNodeId}-${elementType}-placeholder-0`,
        type: 'default',
    };
}

export function createEdgeMultipleElementsNode(
    rootNodeId: string,
    currentNodeId: string,
    multipleElementNodesId: string
) {
    return {
        id: `${currentNodeId}=>${rootNodeId}-${multipleElementNodesId}`,
        source: currentNodeId,
        style: EDGE_STYLES,
        target: multipleElementNodesId,
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
