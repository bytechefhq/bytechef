import {calculateNodeWidth} from '@/pages/platform/cluster-element-editor/utils/clusterElementsUtils';
import {NODE_HEIGHT} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {
    CLUSTER_FRAME_HEADER_HEIGHT,
    ClusterFrameContentOriginI,
    ClusterMemberBoxI,
    computeClusterFrameContentOrigin,
    computeClusterFrameSize,
    getClusterMemberSize,
    toClusterFrameChildPosition,
} from './clusterFrameGeometry';
import {placeClusterMembers} from './placeClusterMembers';

const CLUSTER_FRAME_MEMBER_EXTENT: [[number, number], [number, number]] = [
    [0, CLUSTER_FRAME_HEADER_HEIGHT],
    [Infinity, Infinity],
];

export interface LayoutClusterFramesResultI {
    memberEdges: Edge[];
    memberNodes: Node[];
    outerEdges: Edge[];
    outerNodes: Node[];
}

function collectContentPositions(memberNodes: Node[], clusterRootId: string): Map<string, {x: number; y: number}> {
    const contentPositions = new Map<string, {x: number; y: number}>();

    for (const memberNode of memberNodes) {
        const parentPosition =
            memberNode.parentId === clusterRootId ? {x: 0, y: 0} : contentPositions.get(memberNode.parentId ?? '');

        contentPositions.set(memberNode.id, {
            x: (parentPosition?.x ?? 0) + memberNode.position.x,
            y: (parentPosition?.y ?? 0) + memberNode.position.y,
        });
    }

    return contentPositions;
}

export function layoutClusterFrames(
    nodes: Node[],
    edges: Edge[],
    elementsByRootId: {edgesByRootId: Record<string, Edge[]>; nodesByRootId: Record<string, Node[]>},
    lockedByRootId: Record<string, boolean>
): LayoutClusterFramesResultI {
    const {edgesByRootId, nodesByRootId} = elementsByRootId;

    const memberNodes: Node[] = [];
    const memberEdges: Edge[] = [];

    const outerNodes = nodes.map((node) => {
        const elementNodes = nodesByRootId[node.id];

        if (!elementNodes?.length) {
            return node;
        }

        const nodeData = node.data as NodeDataType;
        const rootEdges = edgesByRootId[node.id] ?? [];

        const placedElementNodes = placeClusterMembers({
            clusterRootId: node.id,
            edges: rootEdges,
            memberNodes: elementNodes,
            rootClusterElements: nodeData.clusterElements,
        });

        const contentPositions = collectContentPositions(placedElementNodes, node.id);

        const clusterElementTypesCount = new Set(
            placedElementNodes
                .filter((elementNode) => elementNode.parentId === node.id)
                .map((elementNode) => (elementNode.data as {clusterElementType?: string}).clusterElementType)
                .filter((clusterElementType): clusterElementType is string => !!clusterElementType)
        ).size;

        const childBoxes: ClusterMemberBoxI[] = [
            {
                height: NODE_HEIGHT,
                width: calculateNodeWidth(clusterElementTypesCount),
                x: 0,
                y: 0,
            },
            ...placedElementNodes.map((elementNode) => {
                const contentPosition = contentPositions.get(elementNode.id) ?? elementNode.position;

                return {
                    ...getClusterMemberSize(elementNode),
                    x: contentPosition.x,
                    y: contentPosition.y,
                };
            }),
        ];

        const frameSize = computeClusterFrameSize(childBoxes);
        const contentOrigin: ClusterFrameContentOriginI = computeClusterFrameContentOrigin(childBoxes, frameSize.width);

        const isLocked = lockedByRootId[node.id] !== false;

        const positionedElementNodes = placedElementNodes.map((elementNode) => ({
            ...elementNode,
            connectable: false,
            draggable: !isLocked && elementNode.type !== 'placeholder',
            extent: CLUSTER_FRAME_MEMBER_EXTENT,
            position:
                elementNode.parentId === node.id
                    ? toClusterFrameChildPosition(elementNode.position, contentOrigin)
                    : elementNode.position,
        }));

        memberNodes.push(...positionedElementNodes);
        memberEdges.push(...rootEdges);

        return {
            ...node,
            data: {
                ...nodeData,
                clusterFrame: {
                    clusterRootId: node.id,
                    contentOrigin,
                    height: frameSize.height,
                    width: frameSize.width,
                },
            },
            height: frameSize.height,
            width: frameSize.width,
        };
    });

    const memberNodeIds = new Set(memberNodes.map((memberNode) => memberNode.id));

    return {
        memberEdges,
        memberNodes,
        outerEdges: edges.filter((edge) => !memberNodeIds.has(edge.source) && !memberNodeIds.has(edge.target)),
        outerNodes: outerNodes.filter((node) => !memberNodeIds.has(node.id)),
    };
}
