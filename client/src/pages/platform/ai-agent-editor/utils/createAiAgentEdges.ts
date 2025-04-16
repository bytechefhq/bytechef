import {EDGE_STYLES} from '@/shared/constants';
import {Node} from '@xyflow/react';

const edgeFromAiAgentStyle = {
    style: EDGE_STYLES,
    type: 'smoothstep',
};

export function createEdgeForPlaceholderNode(node: Node, type: string) {
    return {
        ...edgeFromAiAgentStyle,
        id: `${node.id}=>${node.id}-${type}-placeholder-0`,
        source: node.id,
        target: `${node.id}-${type}-placeholder-0`,
    };
}

export function createEdgeForClusterElementNode(node: Node, targetNode: Node) {
    return {
        ...edgeFromAiAgentStyle,
        id: `${node.id}=>${node.id}-${targetNode.id}`,
        source: node.id,
        target: targetNode.id,
    };
}

export function createEdgeForNextToolNode(node: Node, nextToolNodeId: string) {
    return {
        id: `${node.id}=>${nextToolNodeId}`,
        source: node.id,
        style: EDGE_STYLES,
        target: `${nextToolNodeId}`,
        type: 'smoothstep',
    };
}

export function createEdgeForFinalToolPlaceholderNode(node: Node, finalToolPlaceholderNode: Node) {
    return {
        id: `${node.id}=>${finalToolPlaceholderNode.id}`,
        source: node.id,
        style: EDGE_STYLES,
        target: `${finalToolPlaceholderNode.id}`,
        type: 'smoothstep',
    };
}
