import {Edge, Node} from '@xyflow/react';

const TOP_GHOST_PATTERN = /-(parallel|forkJoin)-top-ghost$/;

const PLACEHOLDER_NODE_TYPES = ['placeholder', 'readonlyPlaceholder'];

export default function removeTrailingBranchPlaceholders(nodes: Node[], edges: Edge[]): {edges: Edge[]; nodes: Node[]} {
    const nodesById = new Map(nodes.map((node) => [node.id, node]));

    const removedNodeIds = new Set<string>();
    const singleLaneGhostIds = new Map<string, string>();

    edges.forEach((edge) => {
        if (!TOP_GHOST_PATTERN.test(edge.source) || edge.sourceHandle !== `${edge.source}-right`) {
            return;
        }

        const placeholderNode = nodesById.get(edge.target);

        if (!placeholderNode || !PLACEHOLDER_NODE_TYPES.includes(placeholderNode.type ?? '')) {
            return;
        }

        const laneEdges = edges.filter(
            (laneEdge) =>
                laneEdge.source === edge.source &&
                laneEdge.target !== placeholderNode.id &&
                nodesById.get(laneEdge.target)?.type !== 'taskDispatcherLeftGhostNode'
        );

        if (laneEdges.length === 0) {
            return;
        }

        removedNodeIds.add(placeholderNode.id);

        if (laneEdges.length === 1) {
            singleLaneGhostIds.set(edge.source, edge.source.replace(/-top-ghost$/, '-bottom-ghost'));
        }
    });

    if (removedNodeIds.size === 0) {
        return {edges, nodes};
    }

    const singleLaneBottomGhostIds = new Set(singleLaneGhostIds.values());

    return {
        edges: edges
            .filter((edge) => !removedNodeIds.has(edge.source) && !removedNodeIds.has(edge.target))
            .map((edge) => {
                if (singleLaneGhostIds.has(edge.source)) {
                    return {...edge, sourceHandle: `${edge.source}-bottom`};
                }

                if (singleLaneBottomGhostIds.has(edge.target) && edge.targetHandle) {
                    return {...edge, targetHandle: `${edge.target}-top`};
                }

                return edge;
            }),
        nodes: nodes.filter((node) => !removedNodeIds.has(node.id)),
    };
}
