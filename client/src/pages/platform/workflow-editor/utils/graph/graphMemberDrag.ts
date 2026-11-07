import {Node, XYPosition} from '@xyflow/react';

import {fromFrameChildPosition, getGraphFrameId} from './graphFrameGeometry';
import {findTopLevelGraphMemberName} from './layoutGraphFrames';

interface GetGraphMemberGroupInsetProps {
    graphId: string;
    memberId: string;
    nodes: Node[];
}

export function getGraphMemberGroupInset({graphId, memberId, nodes}: GetGraphMemberGroupInsetProps): XYPosition {
    const frameId = getGraphFrameId(graphId);
    const nodeEntries = nodes.map((node): [string, Node] => [node.id, node]);
    const nodesById = new Map(nodeEntries);
    const memberNode = nodesById.get(memberId);

    if (!memberNode) {
        return {x: 0, y: 0};
    }

    const memberGroupNodes = nodes.filter(
        (node) => node.parentId === frameId && findTopLevelGraphMemberName({graphId, node, nodesById}) === memberId
    );

    const groupLeft = Math.min(memberNode.position.x, ...memberGroupNodes.map((node) => node.position.x));
    const groupTop = Math.min(memberNode.position.y, ...memberGroupNodes.map((node) => node.position.y));

    return {x: memberNode.position.x - groupLeft, y: memberNode.position.y - groupTop};
}

interface BuildGraphMemberDragStopPositionsProps {
    autoPlacedPositions?: Record<string, XYPosition>;
    draggedNodeId: string;
    draggedNodePosition: XYPosition;
    memberGroupInset?: XYPosition;
}

function clampNonNegative(position: XYPosition): XYPosition {
    return {x: Math.max(0, position.x), y: Math.max(0, position.y)};
}

export function buildGraphMemberDragStopPositions({
    autoPlacedPositions,
    draggedNodeId,
    draggedNodePosition,
    memberGroupInset = {x: 0, y: 0},
}: BuildGraphMemberDragStopPositionsProps): Record<string, XYPosition> {
    const draggedContentPosition = fromFrameChildPosition({
        x: draggedNodePosition.x - memberGroupInset.x,
        y: draggedNodePosition.y - memberGroupInset.y,
    });

    return {
        ...autoPlacedPositions,
        [draggedNodeId]: clampNonNegative(draggedContentPosition),
    };
}

interface FilterToSharedParentProps {
    nodes: Node[];
    parentId: string | undefined;
    startPositions: Map<string, XYPosition>;
}

export function filterToSharedParent({
    nodes,
    parentId,
    startPositions,
}: FilterToSharedParentProps): Map<string, XYPosition> {
    const nodeEntries = nodes.map((node): [string, Node] => [node.id, node]);
    const nodesById = new Map(nodeEntries);

    const sharedParentEntries = [...startPositions].filter(([nodeId]) => {
        const node = nodesById.get(nodeId);

        return !!node && node.parentId === parentId;
    });

    return new Map(sharedParentEntries);
}
