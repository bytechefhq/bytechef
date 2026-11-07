import {
    GRAPH_FRAME_NODE_TYPE,
    GRAPH_START_EDGE_TYPE,
    GRAPH_TRANSITION_EDGE_TYPE,
    LayoutDirectionType,
} from '@/shared/constants';
import {GraphTransitionType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

import {getOwningDispatcherId} from '../elkLayoutUtils';
import {GetLayoutElementsProps, LayoutElementsResultI, getDagreNodeSize} from '../layoutUtils';
import {containsNodePosition} from '../postDagreConstraints';
import {
    GRAPH_FRAME_PADDING,
    GRAPH_START_SIZE,
    GraphMemberBoxI,
    autoPlaceGraphMembers,
    computeGraphFrameSize,
    fromFrameChildPosition,
    getGraphFrameId,
    getGraphStartPinnedBox,
    separateOverlappingGraphMembers,
    toFrameChildPosition,
} from './graphFrameGeometry';
import {getGraphMemberPlacementWidth} from './graphMemberPlacement';

export interface LayoutGraphFramesResultI {
    autoPlaced: Record<string, Record<string, {x: number; y: number}>>;
    memberEdges: Edge[];
    memberNodes: Node[];
    outerEdges: Edge[];
    outerNodes: Node[];
}

type LayoutFunctionType = (props: GetLayoutElementsProps) => Promise<LayoutElementsResultI>;

type MemberGroupType = {
    boundingBox: {height: number; minX: number; minY: number; width: number};
    contentPosition?: {x: number; y: number};
    name: string;
    nodes: Node[];
    positionsById: Map<string, {x: number; y: number}>;
};

const GRAPH_CONTENT_ORIGIN = toFrameChildPosition({x: 0, y: 0});

const GRAPH_MEMBER_EXTENT: [[number, number], [number, number]] = [
    [GRAPH_CONTENT_ORIGIN.x, GRAPH_CONTENT_ORIGIN.y],
    [Infinity, Infinity],
];

const MAX_OWNER_WALK_DEPTH = 64;

export function findGraphMemberOwner(node: Node, nodesById: Map<string, Node>): string | undefined {
    let current: Node | undefined = node;

    for (let depth = 0; current && depth < MAX_OWNER_WALK_DEPTH; depth++) {
        const currentData = current.data as NodeDataType;

        if (currentData.graphData?.graphId) {
            return currentData.graphData.graphId;
        }

        const owningDispatcherId = getOwningDispatcherId(current);

        current = owningDispatcherId ? nodesById.get(owningDispatcherId) : undefined;
    }

    return undefined;
}

interface FindTopLevelGraphMemberNameProps {
    graphId: string;
    node: Node;
    nodesById: Map<string, Node>;
}

export function findTopLevelGraphMemberName({
    graphId,
    node,
    nodesById,
}: FindTopLevelGraphMemberNameProps): string | undefined {
    let current: Node | undefined = node;

    for (let depth = 0; current && depth < MAX_OWNER_WALK_DEPTH; depth++) {
        if ((current.data as NodeDataType).graphData?.graphId === graphId) {
            return current.id;
        }

        const owningDispatcherId = getOwningDispatcherId(current);

        current = owningDispatcherId ? nodesById.get(owningDispatcherId) : undefined;
    }

    return undefined;
}

function getGraphNestingDepth(graphId: string, nodesById: Map<string, Node>): number {
    let current = nodesById.get(graphId);
    let depth = 0;

    while (current && depth < MAX_OWNER_WALK_DEPTH) {
        const owningGraphId = findGraphMemberOwner(current, nodesById);

        if (!owningGraphId) {
            return depth;
        }

        current = nodesById.get(owningGraphId);
        depth += 1;
    }

    return depth;
}

function toNodesById(nodes: Node[]): Map<string, Node> {
    const nodeEntries = nodes.map((node): [string, Node] => [node.id, node]);

    return new Map(nodeEntries);
}

function findGroupMemberNode(group: MemberGroupType): Node | undefined {
    return group.nodes.find((groupNode) => groupNode.id === group.name);
}

function getGroupPlacementSize(group: MemberGroupType, measuredSize: number): number {
    const memberNode = findGroupMemberNode(group);
    const ownsSubtree = group.nodes.length > 1;

    return getGraphMemberPlacementWidth({measuredWidth: measuredSize, node: memberNode, ownsSubtree});
}

function toPinnedMemberBox(group: MemberGroupType, direction: LayoutDirectionType): GraphMemberBoxI {
    return {
        height: getMemberPaintedHeight(group, direction),
        name: group.name,
        paintedWidth: getGroupPlacementSize(group, group.boundingBox.width),
        width: group.boundingBox.width,
        x: group.contentPosition!.x,
        y: group.contentPosition!.y,
    };
}

interface LayoutGraphFramesProps {
    direction: LayoutDirectionType;
    edges: Edge[];
    layoutFunction: LayoutFunctionType;
    nodes: Node[];
}

export async function layoutGraphFrames({
    direction,
    edges,
    layoutFunction,
    nodes,
}: LayoutGraphFramesProps): Promise<LayoutGraphFramesResultI> {
    const frameNodes = nodes.filter((node) => node.type === GRAPH_FRAME_NODE_TYPE);

    if (frameNodes.length === 0) {
        return {autoPlaced: {}, memberEdges: [], memberNodes: [], outerEdges: edges, outerNodes: nodes};
    }

    const initialNodesById = toNodesById(nodes);

    const orderedGraphIds = frameNodes
        .map((frameNode) => (frameNode.data as NodeDataType).graphFrame?.graphId)
        .filter((graphId): graphId is string => !!graphId)
        .sort((firstGraphId, secondGraphId) => {
            const firstNestingDepth = getGraphNestingDepth(firstGraphId, initialNodesById);
            const secondNestingDepth = getGraphNestingDepth(secondGraphId, initialNodesById);

            return secondNestingDepth - firstNestingDepth;
        });

    const autoPlaced: LayoutGraphFramesResultI['autoPlaced'] = {};

    let outerNodes = [...nodes];
    let outerEdges = [...edges];
    let memberNodes: Node[] = [];
    let memberEdges: Edge[] = [];

    for (const graphId of orderedGraphIds) {
        const frameId = getGraphFrameId(graphId);
        const frameIndex = outerNodes.findIndex((node) => node.id === frameId);
        const dispatcherNode = outerNodes.find((node) => node.id === graphId);

        if (frameIndex === -1 || !dispatcherNode) {
            continue;
        }

        const workingNodesById = toNodesById(outerNodes);

        const frameChromeNodes = outerNodes.filter((node) => node.parentId === frameId);

        const groups = groupOwnedNodesByMember({
            dispatcherNode,
            graphId,
            workingNodes: outerNodes,
            workingNodesById,
        });

        await layOutMemberGroups({direction, frameId, groups, layoutFunction, workingEdges: outerEdges});

        const autoPlacedPositions = await resolveMemberPositions({direction, dispatcherNode, groups});

        const unseparatedMemberBoxes = groups.map((group) => toPinnedMemberBox(group, direction));

        const memberBoxes: GraphMemberBoxI[] = separateOverlappingGraphMembers(unseparatedMemberBoxes, direction);

        const settledPositionEntries = memberBoxes.map(
            (memberBox) => [memberBox.name, {x: memberBox.x, y: memberBox.y}] as const
        );

        const settledPositionsByName = new Map(settledPositionEntries);

        for (const group of groups) {
            group.contentPosition = settledPositionsByName.get(group.name) ?? group.contentPosition;
        }

        const autoPlacedNames = Object.keys(autoPlacedPositions);

        if (autoPlacedNames.length > 0) {
            const autoPlacedEntries = autoPlacedNames.map((memberName) => [
                memberName,
                settledPositionsByName.get(memberName) ?? autoPlacedPositions[memberName],
            ]);

            autoPlaced[graphId] = Object.fromEntries(autoPlacedEntries);
        }

        const frameSize = computeGraphFrameSize(memberBoxes, direction);

        outerNodes[frameIndex] = withFrameSize({frameNode: outerNodes[frameIndex], graphId, size: frameSize});

        const alignedFrameChromeNodes = alignStartPillWithEntryMember({
            direction,
            dispatcherNode,
            frameChromeNodes,
            frameSize,
            graphId,
            groups,
        });

        const framedNodes = [...alignedFrameChromeNodes, ...emitFrameChildren(groups, frameId)];
        const framedNodeIdList = framedNodes.map((node) => node.id);
        const framedNodeIds = new Set(framedNodeIdList);

        const {framedEdges, remainingEdges} = partitionFrameEdges({framedNodeIds, groups, workingEdges: outerEdges});

        memberNodes = [...framedNodes, ...memberNodes];
        memberEdges = [...framedEdges, ...memberEdges];

        outerNodes = outerNodes.filter((node) => !framedNodeIds.has(node.id));
        outerEdges = remainingEdges;
    }

    return {autoPlaced, memberEdges, memberNodes, outerEdges, outerNodes};
}

interface AlignStartPillWithEntryMemberProps {
    direction: LayoutDirectionType;
    dispatcherNode: Node;
    frameChromeNodes: Node[];
    frameSize: {height: number; width: number};
    graphId: string;
    groups: MemberGroupType[];
}

function alignStartPillWithEntryMember({
    direction,
    dispatcherNode,
    frameChromeNodes,
    frameSize,
    graphId,
    groups,
}: AlignStartPillWithEntryMemberProps): Node[] {
    const startNodeId = `${graphId}-graph-start`;
    const declaredStartNode = (dispatcherNode.data as NodeDataType).parameters?.startNode?.trim();
    const entryName = declaredStartNode || groups[0]?.name;
    const entryGroup = groups.find((group) => group.name === entryName);
    const entryNode = entryGroup?.nodes.find((groupNode) => groupNode.id === entryName);

    const isHorizontalFlow = direction === 'LR';

    let centredContentPosition: {x: number; y: number};

    if (!entryGroup?.contentPosition) {
        const frameCentredContentPosition = fromFrameChildPosition({
            x: (frameSize.width - GRAPH_START_SIZE.width) / 2,
            y: (frameSize.height - GRAPH_START_SIZE.height) / 2,
        });

        centredContentPosition = isHorizontalFlow
            ? {x: GRAPH_FRAME_PADDING, y: Math.max(0, frameCentredContentPosition.y)}
            : {x: Math.max(0, frameCentredContentPosition.x), y: 0};
    } else if (isHorizontalFlow) {
        const entryCentre = entryGroup.contentPosition.y + getMemberPaintedHeight(entryGroup, direction) / 2;
        const entryCentredY = entryCentre - GRAPH_START_SIZE.height / 2;

        centredContentPosition = {x: GRAPH_FRAME_PADDING, y: Math.max(0, entryCentredY)};
    } else {
        const entryPlacementWidth = getGraphMemberPlacementWidth({
            measuredWidth: entryGroup.boundingBox.width,
            node: entryNode,
            ownsSubtree: entryGroup.nodes.length > 1,
        });

        const entryCentre = entryGroup.contentPosition.x + entryPlacementWidth / 2;
        const entryCentredX = entryCentre - GRAPH_START_SIZE.width / 2;

        centredContentPosition = {x: Math.max(0, entryCentredX), y: 0};
    }

    const centredPosition = toFrameChildPosition(centredContentPosition);

    return frameChromeNodes.map((chromeNode) =>
        chromeNode.id === startNodeId ? {...chromeNode, position: centredPosition} : chromeNode
    );
}

function getMemberPaintedHeight(group: MemberGroupType, direction: LayoutDirectionType): number {
    if (direction !== 'LR') {
        return group.boundingBox.height;
    }

    return getGroupPlacementSize(group, group.boundingBox.height);
}

interface WithFrameSizeProps {
    frameNode: Node;
    graphId: string;
    size: {height: number; width: number};
}

function withFrameSize({frameNode, graphId, size}: WithFrameSizeProps): Node {
    return {
        ...frameNode,
        data: {...frameNode.data, graphFrame: {graphId, height: size.height, width: size.width}},
        height: size.height,
        width: size.width,
    };
}

interface GroupOwnedNodesByMemberProps {
    dispatcherNode: Node;
    graphId: string;
    workingNodes: Node[];
    workingNodesById: Map<string, Node>;
}

function groupOwnedNodesByMember({
    dispatcherNode,
    graphId,
    workingNodes,
    workingNodesById,
}: GroupOwnedNodesByMemberProps): MemberGroupType[] {
    const nodesByMemberName = new Map<string, Node[]>();

    for (const node of workingNodes) {
        if (findGraphMemberOwner(node, workingNodesById) !== graphId) {
            continue;
        }

        const memberName = findTopLevelGraphMemberName({graphId, node, nodesById: workingNodesById});

        if (!memberName) {
            continue;
        }

        const groupNodes = nodesByMemberName.get(memberName) ?? [];

        if (node.id === memberName) {
            groupNodes.unshift(node);
        } else {
            groupNodes.push(node);
        }

        nodesByMemberName.set(memberName, groupNodes);
    }

    const declaredMembers: Array<{name: string}> = (dispatcherNode.data as NodeDataType).parameters?.nodes ?? [];

    const declaredMemberNames = declaredMembers.map((declaredMember) => declaredMember.name);

    const presentDeclaredMemberNames = declaredMemberNames.filter((memberName) => nodesByMemberName.has(memberName));

    const undeclaredMemberNames = [...nodesByMemberName.keys()].filter(
        (memberName) => !declaredMemberNames.includes(memberName)
    );

    const orderedMemberNames = [...presentDeclaredMemberNames, ...undeclaredMemberNames];

    return orderedMemberNames.map((memberName) => ({
        boundingBox: {height: 0, minX: 0, minY: 0, width: 0},
        name: memberName,
        nodes: nodesByMemberName.get(memberName)!,
        positionsById: new Map(),
    }));
}

interface LayOutMemberGroupsProps {
    direction: LayoutDirectionType;
    frameId: string;
    groups: MemberGroupType[];
    layoutFunction: LayoutFunctionType;
    workingEdges: Edge[];
}

async function layOutMemberGroups({
    direction,
    frameId,
    groups,
    layoutFunction,
    workingEdges,
}: LayOutMemberGroupsProps): Promise<void> {
    for (const group of groups) {
        const groupNodeIdList = group.nodes.map((node) => node.id);
        const groupNodeIds = new Set(groupNodeIdList);

        const groupEdges = workingEdges.filter(
            (edge) => groupNodeIds.has(edge.source) && groupNodeIds.has(edge.target)
        );

        const framedGroupNodes = group.nodes.map((node) => ({...node, parentId: frameId}));

        const laidOut = await layoutFunction({
            canvasWidth: 0,
            direction,
            edges: groupEdges,
            nodes: framedGroupNodes,
            savedPositionCrossAxisShift: 0,
        });

        const laidOutById = toNodesById(laidOut.nodes);

        let minX = Infinity;
        let minY = Infinity;
        let maxX = -Infinity;
        let maxY = -Infinity;

        for (const groupNode of group.nodes) {
            const position = (laidOutById.get(groupNode.id) ?? groupNode).position;
            const size = getRenderedMemberSize(groupNode, direction);

            group.positionsById.set(groupNode.id, position);

            const rightEdge = position.x + size.width;
            const bottomEdge = position.y + size.height;

            minX = Math.min(minX, position.x);
            minY = Math.min(minY, position.y);
            maxX = Math.max(maxX, rightEdge);
            maxY = Math.max(maxY, bottomEdge);
        }

        group.boundingBox = {height: maxY - minY, minX, minY, width: maxX - minX};
    }
}

function getRenderedMemberSize(node: Node, direction: LayoutDirectionType): {height: number; width: number} {
    const measured = node.measured;

    if (measured?.height != null && measured?.width != null) {
        return {height: measured.height, width: measured.width};
    }

    return getDagreNodeSize(node, direction);
}

interface ResolveMemberPositionsProps {
    direction: LayoutDirectionType;
    dispatcherNode: Node;
    groups: MemberGroupType[];
}

async function resolveMemberPositions({
    direction,
    dispatcherNode,
    groups,
}: ResolveMemberPositionsProps): Promise<Record<string, {x: number; y: number}>> {
    const unplacedGroups: MemberGroupType[] = [];

    for (const group of groups) {
        const memberNode = findGroupMemberNode(group);

        const memberData = memberNode?.data as NodeDataType | undefined;

        if (memberData && containsNodePosition(memberData.metadata)) {
            group.contentPosition = memberData.metadata.ui.nodePosition;
        } else {
            unplacedGroups.push(group);
        }
    }

    if (unplacedGroups.length === 0) {
        return {};
    }

    const dispatcherParameters = (dispatcherNode.data as NodeDataType).parameters;

    const transitions: GraphTransitionType[] = dispatcherParameters?.transitions ?? [];

    const memberSizes = unplacedGroups.map((group) => {
        const placementWidth = getGroupPlacementSize(group, group.boundingBox.width);

        return {
            height: getMemberPaintedHeight(group, direction),
            labelOverhang: group.boundingBox.width - placementWidth,
            name: group.name,
            width: placementWidth,
        };
    });

    const placedMemberBoxes = groups
        .filter((group) => !!group.contentPosition)
        .map((group) => toPinnedMemberBox(group, direction));

    const graphStartPinnedBox = getGraphStartPinnedBox();

    const pinnedBoxes = [graphStartPinnedBox, ...placedMemberBoxes];

    const startNodeName = dispatcherParameters?.startNode || groups[0]?.name;

    const autoPlacedPositions = await autoPlaceGraphMembers({
        direction,
        memberSizes,
        pinnedBoxes,
        startNodeName,
        transitions,
    });

    for (const group of unplacedGroups) {
        group.contentPosition = autoPlacedPositions[group.name] ?? {x: 0, y: 0};
    }

    const autoPlacedEntries = unplacedGroups.map((group) => [group.name, group.contentPosition!]);

    return Object.fromEntries(autoPlacedEntries);
}

function emitFrameChildren(groups: MemberGroupType[], frameId: string): Node[] {
    return groups.flatMap((group) => {
        const framePosition = toFrameChildPosition(group.contentPosition!);

        return group.nodes.map((groupNode) => {
            const position = group.positionsById.get(groupNode.id) ?? groupNode.position;

            const framedNode: Node = {
                ...groupNode,
                parentId: frameId,
                position: {
                    x: framePosition.x + position.x - group.boundingBox.minX,
                    y: framePosition.y + position.y - group.boundingBox.minY,
                },
            };

            if (groupNode.id === group.name) {
                framedNode.draggable = true;
                framedNode.extent = GRAPH_MEMBER_EXTENT;
            }

            return framedNode;
        });
    });
}

interface PartitionFrameEdgesProps {
    framedNodeIds: Set<string>;
    groups: MemberGroupType[];
    workingEdges: Edge[];
}

function partitionFrameEdges({framedNodeIds, groups, workingEdges}: PartitionFrameEdgesProps): {
    framedEdges: Edge[];
    remainingEdges: Edge[];
} {
    const memberNameByNodeId = new Map<string, string>();

    for (const group of groups) {
        for (const groupNode of group.nodes) {
            memberNameByNodeId.set(groupNode.id, group.name);
        }
    }

    const framedEdges: Edge[] = [];
    const remainingEdges: Edge[] = [];

    for (const edge of workingEdges) {
        const sourceFramed = framedNodeIds.has(edge.source);
        const targetFramed = framedNodeIds.has(edge.target);

        if (!sourceFramed && !targetFramed) {
            remainingEdges.push(edge);

            continue;
        }

        if (!sourceFramed || !targetFramed) {
            continue;
        }

        if (edge.type === GRAPH_TRANSITION_EDGE_TYPE || edge.type === GRAPH_START_EDGE_TYPE) {
            framedEdges.push(edge);

            continue;
        }

        const sourceMemberName = memberNameByNodeId.get(edge.source);
        const targetMemberName = memberNameByNodeId.get(edge.target);

        if (sourceMemberName && sourceMemberName === targetMemberName) {
            framedEdges.push(edge);
        }
    }

    return {framedEdges, remainingEdges};
}
