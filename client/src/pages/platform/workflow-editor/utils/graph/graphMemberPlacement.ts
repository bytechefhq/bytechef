import {LayoutDirectionType} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node, XYPosition} from '@xyflow/react';

import {
    GRAPH_MEMBER_BOX_WIDTH,
    GRAPH_MEMBER_NOMINAL_SIZE,
    GraphMemberBoxI,
    findFreeSpot,
    fromFrameChildPosition,
    getGraphFrameId,
    getGraphMemberPaintedWidth,
} from './graphFrameGeometry';
import {findGraphMemberOwner, findTopLevelGraphMemberName} from './layoutGraphFrames';

export interface GraphMemberCanvasStateI {
    positions: Record<string, XYPosition>;
    sizes: Record<string, {height: number; paintedWidth?: number; width: number}>;
}

function getMemberSize(node: Node): {height: number; width: number} {
    const nodeData = node.data as NodeDataType;
    const preComputedBox = nodeData.clusterFrame ?? nodeData.graphFrame;

    if (preComputedBox) {
        return {height: preComputedBox.height, width: preComputedBox.width};
    }

    return {
        height: node.measured?.height ?? node.height ?? GRAPH_MEMBER_NOMINAL_SIZE.height,
        width: node.measured?.width ?? node.width ?? GRAPH_MEMBER_NOMINAL_SIZE.width,
    };
}

export function readGraphMemberCanvasState(graphId: string, nodes: Node[]): GraphMemberCanvasStateI {
    const positions: Record<string, XYPosition> = {};
    const sizes: GraphMemberCanvasStateI['sizes'] = {};

    for (const memberBox of collectGraphMemberBoxes(graphId, nodes)) {
        positions[memberBox.name] = {x: memberBox.x, y: memberBox.y};
        sizes[memberBox.name] = {
            height: memberBox.height,
            paintedWidth: memberBox.paintedWidth,
            width: memberBox.width,
        };
    }

    return {positions, sizes};
}

export function collectGraphMemberBoxes(graphId: string, nodes: Node[]): GraphMemberBoxI[] {
    const frameId = getGraphFrameId(graphId);

    const nodeEntries = nodes.map((node): [string, Node] => [node.id, node]);

    const nodesById = new Map(nodeEntries);

    const nodesByMemberName = new Map<string, Node[]>();

    for (const node of nodes) {
        if (node.parentId !== frameId || findGraphMemberOwner(node, nodesById) !== graphId) {
            continue;
        }

        const memberName = findTopLevelGraphMemberName({graphId, node, nodesById});

        if (!memberName) {
            continue;
        }

        const memberNodes = nodesByMemberName.get(memberName);

        if (memberNodes) {
            memberNodes.push(node);
        } else {
            nodesByMemberName.set(memberName, [node]);
        }
    }

    return [...nodesByMemberName.entries()].map(([memberName, memberNodes]) => buildMemberBox(memberName, memberNodes));
}

function buildMemberBox(memberName: string, memberNodes: Node[]): GraphMemberBoxI {
    const ownsSubtree = memberNodes.length > 1;

    const nodeBoxes = memberNodes.map((node) => {
        const contentPosition = fromFrameChildPosition(node.position);
        const size = getMemberSize(node);

        return {height: size.height, width: size.width, x: contentPosition.x, y: contentPosition.y};
    });

    const leftEdges = nodeBoxes.map((nodeBox) => nodeBox.x);
    const topEdges = nodeBoxes.map((nodeBox) => nodeBox.y);
    const rightEdges = nodeBoxes.map((nodeBox) => nodeBox.x + nodeBox.width);
    const bottomEdges = nodeBoxes.map((nodeBox) => nodeBox.y + nodeBox.height);

    const leftEdge = Math.min(...leftEdges);
    const topEdge = Math.min(...topEdges);
    const width = Math.max(...rightEdges) - leftEdge;
    const height = Math.max(...bottomEdges) - topEdge;

    const memberNode = memberNodes.find((node) => node.id === memberName);

    const paintedWidth = getGraphMemberPlacementWidth({measuredWidth: width, node: memberNode, ownsSubtree});

    return {
        height,
        name: memberName,
        paintedWidth,
        width,
        x: leftEdge,
        y: topEdge,
    };
}

interface GetGraphMemberPlacementWidthProps {
    measuredWidth: number;
    node: Node | undefined;
    ownsSubtree: boolean;
}

export function getGraphMemberPlacementWidth({
    measuredWidth,
    node,
    ownsSubtree,
}: GetGraphMemberPlacementWidthProps): number {
    const nodeData = node?.data as NodeDataType | undefined;

    if (!nodeData || ownsSubtree || nodeData.clusterRoot) {
        return measuredWidth;
    }

    return Math.min(measuredWidth, GRAPH_MEMBER_BOX_WIDTH);
}

export function collectGraphMemberSizes(
    graphId: string,
    nodes: Node[]
): Array<{height: number; labelOverhang: number; name: string; width: number}> {
    return collectGraphMemberBoxes(graphId, nodes).map((memberBox) => {
        const paintedWidth = getGraphMemberPaintedWidth(memberBox);

        return {
            height: memberBox.height,
            labelOverhang: memberBox.width - paintedWidth,
            name: memberBox.name,
            width: paintedWidth,
        };
    });
}

export function withMemberNodePosition(member: WorkflowTask, position: XYPosition): WorkflowTask {
    return {...member, metadata: {...member.metadata, ui: {...member.metadata?.ui, nodePosition: position}}};
}

interface PlaceGraphMembersProps {
    autoPlacedPositions?: Record<string, XYPosition>;
    canvasState: GraphMemberCanvasStateI;
    direction?: LayoutDirectionType;
    dropPosition?: XYPosition;
    previousMembers: WorkflowTask[];
    updatedMembers: WorkflowTask[];
}

export function placeGraphMembers({
    autoPlacedPositions,
    canvasState,
    direction,
    dropPosition,
    previousMembers,
    updatedMembers,
}: PlaceGraphMembersProps): {addedMemberName: string | undefined; members: WorkflowTask[]} {
    const previousMemberNameList = previousMembers.map((member) => member.name);

    const previousMemberNames = new Set(previousMemberNameList);

    const addedMemberName = updatedMembers.find((member) => !previousMemberNames.has(member.name))?.name;

    const resolvePosition = (member: WorkflowTask): XYPosition | undefined => {
        if (member.name === addedMemberName) {
            if (dropPosition) {
                return dropPosition;
            }

            const occupiedBoxes = buildOccupiedBoxes(previousMembers, canvasState);

            return findFreeSpot(occupiedBoxes, direction);
        }

        if (member.metadata?.ui?.nodePosition) {
            return undefined;
        }

        return autoPlacedPositions?.[member.name];
    };

    return {
        addedMemberName,
        members: updatedMembers.map((member) => {
            const position = resolvePosition(member);

            return position ? withMemberNodePosition(member, position) : member;
        }),
    };
}

function buildOccupiedBoxes(members: WorkflowTask[], canvasState: GraphMemberCanvasStateI): GraphMemberBoxI[] {
    return members.reduce<GraphMemberBoxI[]>((boxes, member) => {
        const position = member.metadata?.ui?.nodePosition ?? canvasState.positions[member.name];

        if (position) {
            boxes.push({
                ...(canvasState.sizes[member.name] ?? GRAPH_MEMBER_NOMINAL_SIZE),
                name: member.name,
                x: position.x,
                y: position.y,
            });
        }

        return boxes;
    }, []);
}
