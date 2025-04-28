import {EDGE_STYLES} from '@/shared/constants';
import {Node} from '@xyflow/react';

const defaultLabeledEdgeStyle = {
    style: EDGE_STYLES,
    type: 'labeledAiAgentEdge',
};

export function createEdgeForPlaceholderNode(nodeId: string, type: string) {
    return {
        ...defaultLabeledEdgeStyle,
        id: `${nodeId}=>${nodeId}-${type}-placeholder-0`,
        source: nodeId,
        sourceHandle: `${type}-handle`,
        target: `${nodeId}-${type}-placeholder-0`,
    };
}

export function createEdgeForToolsGhostNode(nodeId: string) {
    return {
        ...defaultLabeledEdgeStyle,
        id: `${nodeId}=>${nodeId}-tools-ghost`,
        source: nodeId,
        sourceHandle: 'tools-handle',
        target: `${nodeId}-tools-ghost`,
    };
}

export function createEdgeForToolsPlaceholderNode(currentNodeId: string, aiAgentId: string) {
    return {
        id: `${currentNodeId}=>${aiAgentId}-tools-placeholder-0`,
        source: currentNodeId,
        style: EDGE_STYLES,
        target: `${aiAgentId}-tools-placeholder-0`,
        type: 'default',
    };
}

export function createEdgeForToolNode(aiAgentId: string, currentNodeId: string, toolNodeId: string) {
    return {
        id: `${currentNodeId}=>${aiAgentId}-${toolNodeId}`,
        source: currentNodeId,
        style: EDGE_STYLES,
        target: toolNodeId,
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
