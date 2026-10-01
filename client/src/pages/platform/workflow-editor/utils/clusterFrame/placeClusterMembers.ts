import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {getClusterElementsLayoutElements} from '../layoutUtils';

interface PlaceClusterMembersProps {
    clusterRootId: string;
    edges: Edge[];
    memberNodes: Node[];
    rootClusterElements: NodeDataType['clusterElements'];
}

export function placeClusterMembers({
    clusterRootId,
    edges,
    memberNodes,
    rootClusterElements,
}: PlaceClusterMembersProps): Node[] {
    if (memberNodes.length === 0) {
        return memberNodes;
    }

    const firstDirectMember = memberNodes.find((memberNode) => memberNode.parentId === clusterRootId);

    const syntheticRootNode: Node = {
        data: {
            clusterElementTypesCount:
                (firstDirectMember?.data as {parentClusterRootElementsTypeCount?: number} | undefined)
                    ?.parentClusterRootElementsTypeCount ?? 1,
            clusterElements: rootClusterElements ?? {},
        },
        id: clusterRootId,
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const placedElements = getClusterElementsLayoutElements({
        canvasHeight: 0,
        canvasWidth: 0,
        currentRootPosition: {x: 0, y: 0},
        edges,
        nodes: [syntheticRootNode, ...memberNodes],
    });

    const placedPositionsById = new Map(
        placedElements.nodes.map((placedNode) => [placedNode.id, placedNode.position] as const)
    );

    return memberNodes.map((memberNode) => {
        const placedPosition = placedPositionsById.get(memberNode.id);

        return placedPosition ? {...memberNode, position: placedPosition} : memberNode;
    });
}
