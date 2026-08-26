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
    GRAPH_FRAME_HEADER_HEIGHT,
    GRAPH_FRAME_PADDING,
    GRAPH_START_SIZE,
    GraphMemberBoxI,
    autoPlaceGraphMembers,
    computeGraphFrameSize,
    getGraphFrameId,
    getGraphStartPinnedBox,
    toFrameChildPosition,
} from './graphFrameGeometry';
import {getGraphMemberPlacementWidth} from './graphMemberPlacement';

export interface LayoutGraphFramesResultI {
    /** Members that had no saved position: graphId -> member name -> content-origin position. */
    autoPlaced: Record<string, Record<string, {x: number; y: number}>>;
    /** Edges living entirely inside a frame, to append after the outer layout returns. */
    memberEdges: Edge[];
    /** Frame children (`parentId` set, frame-relative positions) in parent-before-child order. */
    memberNodes: Node[];
    /** The outer arrays with every frame child removed and every frame node sized. */
    outerEdges: Edge[];
    outerNodes: Node[];
}

type LayoutFunctionType = (props: GetLayoutElementsProps) => Promise<LayoutElementsResultI>;

type MemberGroupType = {
    /** Bounding box of the laid-out group, used to normalise it onto the member's own position. */
    boundingBox: {height: number; minX: number; minY: number; width: number};
    /** Where the member sits in the frame's CONTENT coordinates — stored, or auto-placed. */
    contentPosition?: {x: number; y: number};
    name: string;
    nodes: Node[];
    /** Position of the laid-out group's nodes, keyed by node id, before normalisation. */
    positionsById: Map<string, {x: number; y: number}>;
};

/**
 * Members are clamped out of the header band but otherwise free to move anywhere inside the frame,
 * which grows to fit them.
 */
const GRAPH_MEMBER_EXTENT: [[number, number], [number, number]] = [
    [0, GRAPH_FRAME_HEADER_HEIGHT],
    [Infinity, Infinity],
];

// Guards the owner walks below against a cycle in malformed nesting data.
const MAX_OWNER_WALK_DEPTH = 64;

/**
 * The graph whose frame this node belongs inside — either because it IS a member task
 * (`graphData.graphId`) or because it sits somewhere in a member's own subtree. Returns undefined
 * for root-scope nodes and for a graph's own chrome (its dispatcher node and ghost bars), which
 * stay OUTSIDE the frame: the walk starts at the node itself and only ever reports the graph an
 * ancestor is a member of.
 *
 * Takes the canvas as a map rather than an array because callers classify many nodes against the
 * same canvas — the drag-time frame resize does it per frame child, per mouse move — and build the
 * map once rather than paying for one per node.
 */
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

/**
 * Which of `graphId`'s own members this node belongs to — the node itself when it IS the member,
 * the top-level member otherwise. A dispatcher member's whole subtree therefore answers with the
 * dispatcher's own name, which is what lets a member be measured as the box it actually renders as
 * rather than as its own node box.
 *
 * The walk `findGraphMemberOwner` performs, asking a different question of it: that one reports
 * WHICH graph owns the node and stops at the innermost one, this one keeps walking until it reaches
 * a member of the graph it was ASKED about. The two disagree on a node inside a nested graph — this
 * one names the outer member holding the inner graph, that one names the inner graph — so a caller
 * that means "directly owned by this graph" must pair them, as `collectGraphMemberBoxes` does.
 */
export function findTopLevelGraphMemberName(
    node: Node,
    nodesById: Map<string, Node>,
    graphId: string
): string | undefined {
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

/**
 * How deeply a graph is nested inside other graphs. Frames are processed deepest-first so an inner
 * frame is already sized by the time the outer frame measures the member group holding it.
 */
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

/**
 * Lays every `graph/v1` dispatcher's members out inside its own frame and hands the caller the
 * outer arrays with each frame reduced to a single sized leaf node.
 *
 * Per frame, in post-order over nesting:
 *
 * 1. Owned nodes (member tasks plus everything in their subtrees) are grouped by the top-level
 *    member they belong to, and each group is laid out on its own with the active engine — a plain
 *    member is one node, a dispatcher member yields its subtree's bounding box, and a nested graph
 *    contributes the frame this pass already sized.
 * 2. Each member sits at its stored `metadata.ui.nodePosition`, or at a spot `autoPlaceGraphMembers`
 *    picks for it (reported back through `autoPlaced` so the caller can persist it lazily).
 * 3. The frame is sized to the union of the member boxes and handed to the outer engine as an
 *    ordinary node, so the surrounding flow reflows around it identically under either engine.
 *
 * Member positions are frame-relative, so nothing the outer layout does can disturb them.
 */
export async function layoutGraphFrames(
    nodes: Node[],
    edges: Edge[],
    direction: LayoutDirectionType,
    layoutFunction: LayoutFunctionType
): Promise<LayoutGraphFramesResultI> {
    const frameNodes = nodes.filter((node) => node.type === GRAPH_FRAME_NODE_TYPE);

    if (frameNodes.length === 0) {
        return {autoPlaced: {}, memberEdges: [], memberNodes: [], outerEdges: edges, outerNodes: nodes};
    }

    const initialNodesById = new Map(nodes.map((node) => [node.id, node]));

    const orderedGraphIds = frameNodes
        .map((frameNode) => (frameNode.data as NodeDataType).graphFrame?.graphId)
        // A frame node whose `graphFrame.graphId` is missing is skipped: it cannot be tied back to
        // a dispatcher, so there is no members list to lay out and no `parameters.transitions` to
        // route. `createGraphNode` always sets it, so this only fires on a node hand-built without
        // it (a test fixture, or a future creator that forgets) — the frame then renders at the
        // size it already carries and its members stay in the outer flow, which is visible rather
        // than silent.
        .filter((graphId): graphId is string => !!graphId)
        .sort(
            (firstGraphId, secondGraphId) =>
                getGraphNestingDepth(secondGraphId, initialNodesById) -
                getGraphNestingDepth(firstGraphId, initialNodesById)
        );

    const autoPlaced: LayoutGraphFramesResultI['autoPlaced'] = {};

    let outerNodes = [...nodes];
    let outerEdges = [...edges];
    let memberNodes: Node[] = [];
    let memberEdges: Edge[] = [];

    for (const graphId of orderedGraphIds) {
        const frameId = getGraphFrameId(graphId);
        const frameIndex = outerNodes.findIndex((node) => node.id === frameId);
        const dispatcherNode = outerNodes.find((node) => node.id === graphId);

        // Both halves mean the graph is mid-deletion: `useLayout` rebuilds the node array from the
        // task list, so a graph removed from the definition loses its dispatcher node while a
        // stale frame may still be in the array for one pass (or vice versa). Skipping leaves the
        // orphan in the outer arrays for the engine to place, which is what happens to every other
        // orphaned auxiliary node, and the next relayout drops it.
        if (frameIndex === -1 || !dispatcherNode) {
            continue;
        }

        const workingNodesById = new Map(outerNodes.map((node) => [node.id, node]));

        // The Start pill and the add-node placeholder are frame chrome `createGraphNode` already
        // parented and positioned; they only need taking out of the outer arrays.
        const frameChromeNodes = outerNodes.filter((node) => node.parentId === frameId);

        const groups = groupOwnedNodesByMember(graphId, outerNodes, workingNodesById, dispatcherNode);

        await layOutMemberGroups(groups, frameId, outerEdges, direction, layoutFunction);

        const autoPlacedPositions = await resolveMemberPositions(groups, dispatcherNode, direction);

        if (Object.keys(autoPlacedPositions).length > 0) {
            autoPlaced[graphId] = autoPlacedPositions;
        }

        const memberBoxes: GraphMemberBoxI[] = groups.map((group) => ({
            height: group.boundingBox.height,
            name: group.name,
            paintedWidth: getGraphMemberPlacementWidth(
                group.nodes.find((groupNode) => groupNode.id === group.name),
                group.boundingBox.width,
                group.nodes.length > 1
            ),
            width: group.boundingBox.width,
            x: group.contentPosition!.x,
            y: group.contentPosition!.y,
        }));

        const frameSize = computeGraphFrameSize(memberBoxes);

        outerNodes[frameIndex] = withFrameSize(outerNodes[frameIndex], graphId, frameSize);

        const framedNodes = [
            ...alignStartPillWithEntryMember(frameChromeNodes, graphId, frameSize, direction, groups, dispatcherNode),
            ...emitFrameChildren(groups, frameId),
        ];
        const framedNodeIds = new Set(framedNodes.map((node) => node.id));

        const {framedEdges, remainingEdges} = partitionFrameEdges(outerEdges, framedNodeIds, groups);

        // Deepest-first processing produces the innermost frame's children first, but React Flow
        // needs every parent to precede its children — so each frame's batch goes in front.
        memberNodes = [...framedNodes, ...memberNodes];
        memberEdges = [...framedEdges, ...memberEdges];

        outerNodes = outerNodes.filter((node) => !framedNodeIds.has(node.id));
        outerEdges = remainingEdges;
    }

    return {autoPlaced, memberEdges, memberNodes, outerEdges, outerNodes};
}

/**
 * Puts the Start pill on the entry member's own cross-axis centre, so the edge marking where the
 * graph is entered runs straight into it.
 *
 * This is also where it reads as centred, now that auto-arrange puts the entry member on the block's
 * centre line: the two requirements — pill centred, start edge straight — only conflict while the
 * entry member sits off to one side. Aligning the pill to the frame instead would satisfy neither
 * once a member has been dragged, because the frame is sized around the LABELS while the member is
 * placed by its painted box, and those two centres are not the same point.
 *
 * The member's PAINTED box is what it lines up with for that reason. Falls back to the frame's own
 * centre when the entry member has no laid-out position, which is the best guess available.
 */
function alignStartPillWithEntryMember(
    frameChromeNodes: Node[],
    graphId: string,
    frameSize: {height: number; width: number},
    direction: LayoutDirectionType,
    groups: MemberGroupType[],
    dispatcherNode: Node
): Node[] {
    const startNodeId = `${graphId}-graph-start`;
    const declaredStartNode = (dispatcherNode.data as NodeDataType).parameters?.startNode?.trim();
    const entryName = declaredStartNode || groups[0]?.name;
    const entryGroup = groups.find((group) => group.name === entryName);
    const entryNode = entryGroup?.nodes.find((groupNode) => groupNode.id === entryName);

    const isHorizontalFlow = direction === 'LR';

    let centredPosition: {x: number; y: number};

    if (isHorizontalFlow) {
        // The painted box, not the group's bounding height: in LR the engine reserves NODE_WIDTH
        // across the flow for every task, so centring on that drops the pill a long way below the
        // box it is meant to point at.
        const entryCentre = entryGroup?.contentPosition
            ? entryGroup.contentPosition.y +
              getGraphMemberPlacementWidth(entryNode, entryGroup.boundingBox.height, entryGroup.nodes.length > 1) / 2
            : (frameSize.height - GRAPH_FRAME_HEADER_HEIGHT) / 2;

        centredPosition = {
            x: GRAPH_FRAME_PADDING,
            y: GRAPH_FRAME_HEADER_HEIGHT + Math.max(0, entryCentre - GRAPH_START_SIZE.height / 2),
        };
    } else {
        const entryCentre = entryGroup?.contentPosition
            ? entryGroup.contentPosition.x +
              getGraphMemberPlacementWidth(entryNode, entryGroup.boundingBox.width, entryGroup.nodes.length > 1) / 2
            : frameSize.width / 2;

        centredPosition = {
            x: Math.max(0, entryCentre - GRAPH_START_SIZE.width / 2),
            y: GRAPH_FRAME_HEADER_HEIGHT,
        };
    }

    return frameChromeNodes.map((chromeNode) =>
        chromeNode.id === startNodeId ? {...chromeNode, position: centredPosition} : chromeNode
    );
}

function withFrameSize(frameNode: Node, graphId: string, size: {height: number; width: number}): Node {
    return {
        ...frameNode,
        data: {...frameNode.data, graphFrame: {graphId, height: size.height, width: size.width}},
        height: size.height,
        width: size.width,
    };
}

/**
 * Buckets every node the frame owns under the top-level member it belongs to, in the order the
 * members are declared — auto-placement is order-sensitive, so array order must not decide it.
 */
function groupOwnedNodesByMember(
    graphId: string,
    workingNodes: Node[],
    workingNodesById: Map<string, Node>,
    dispatcherNode: Node
): MemberGroupType[] {
    const nodesByMemberName = new Map<string, Node[]>();

    for (const node of workingNodes) {
        if (findGraphMemberOwner(node, workingNodesById) !== graphId) {
            continue;
        }

        const memberName = findTopLevelGraphMemberName(node, workingNodesById, graphId);

        if (!memberName) {
            continue;
        }

        const groupNodes = nodesByMemberName.get(memberName) ?? [];

        // The member itself anchors its group, so it leads regardless of array order.
        if (node.id === memberName) {
            groupNodes.unshift(node);
        } else {
            groupNodes.push(node);
        }

        nodesByMemberName.set(memberName, groupNodes);
    }

    const declaredMemberNames = ((dispatcherNode.data as NodeDataType).parameters?.nodes ?? []).map(
        (declaredMember: {name: string}) => declaredMember.name
    );

    const orderedMemberNames = [
        ...declaredMemberNames.filter((memberName: string) => nodesByMemberName.has(memberName)),
        ...[...nodesByMemberName.keys()].filter((memberName) => !declaredMemberNames.includes(memberName)),
    ];

    return orderedMemberNames.map((memberName) => ({
        boundingBox: {height: 0, minX: 0, minY: 0, width: 0},
        name: memberName,
        nodes: nodesByMemberName.get(memberName)!,
        positionsById: new Map(),
    }));
}

/**
 * Lays each group out on its own and records its bounding box. The nodes are handed to the engine
 * already carrying their `parentId`, which is what makes `applySavedPositions` leave them alone: a
 * frame child's stored position is FRAME-relative and would fling the group if read as an absolute
 * canvas coordinate.
 */
async function layOutMemberGroups(
    groups: MemberGroupType[],
    frameId: string,
    workingEdges: Edge[],
    direction: LayoutDirectionType,
    layoutFunction: LayoutFunctionType
): Promise<void> {
    for (const group of groups) {
        const groupNodeIds = new Set(group.nodes.map((node) => node.id));

        const laidOut = await layoutFunction({
            canvasWidth: 0,
            direction,
            edges: workingEdges.filter((edge) => groupNodeIds.has(edge.source) && groupNodeIds.has(edge.target)),
            nodes: group.nodes.map((node) => ({...node, parentId: frameId})),
            savedPositionCrossAxisShift: 0,
        });

        const laidOutById = new Map(laidOut.nodes.map((node) => [node.id, node]));

        let minX = Infinity;
        let minY = Infinity;
        let maxX = -Infinity;
        let maxY = -Infinity;

        for (const groupNode of group.nodes) {
            const position = (laidOutById.get(groupNode.id) ?? groupNode).position;
            const size = getRenderedMemberSize(groupNode, direction);

            group.positionsById.set(groupNode.id, position);

            minX = Math.min(minX, position.x);
            minY = Math.min(minY, position.y);
            maxX = Math.max(maxX, position.x + size.width);
            maxY = Math.max(maxY, position.y + size.height);
        }

        group.boundingBox = {height: maxY - minY, minX, minY, width: maxX - minX};
    }
}

/**
 * How large a member's node actually renders.
 *
 * `getDagreNodeSize` answers with the engine's stand-in footprint — `NODE_WIDTH` for any regular
 * task — which is what the outer chain needs to reserve a lane, but it is far wider than the box a
 * member paints. Sizing the frame from it leaves a phantom margin on the side the widest member
 * sits, which is visible now that the frame mirrors its members' inset to centre them. React Flow's
 * own measurement is the honest number; the stand-in is the fallback for the first layout, before
 * anything has been measured.
 */
function getRenderedMemberSize(node: Node, direction: LayoutDirectionType): {height: number; width: number} {
    const measured = node.measured;

    if (measured?.height != null && measured?.width != null) {
        return {height: measured.height, width: measured.width};
    }

    return getDagreNodeSize(node, direction);
}

/**
 * Pins each member to its stored position, then asks `autoPlaceGraphMembers` for the rest and
 * reports those back so the caller can persist them on the user's first interaction.
 */
async function resolveMemberPositions(
    groups: MemberGroupType[],
    dispatcherNode: Node,
    direction: LayoutDirectionType
): Promise<Record<string, {x: number; y: number}>> {
    const unplacedGroups: MemberGroupType[] = [];

    for (const group of groups) {
        const memberNode = group.nodes.find((groupNode) => groupNode.id === group.name);

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

    const transitions: GraphTransitionType[] = (dispatcherNode.data as NodeDataType).parameters?.transitions ?? [];

    // The Start pill is pinned at the content origin by `createGraphNode` and is not a member, so
    // it never appears in `groups` — but `autoPlaceGraphMembers` offsets its results below the
    // pinned boxes it is given, and with no pinned MEMBERS that offset is just the frame padding.
    // On every freshly-created graph (no member has a stored position yet) that puts the first
    // auto-placed member 4px under the pill, horizontally overlapping it — clear only because ELK
    // happens to pad its own graph by 12. Passing the pill in as a pinned box turns that accident
    // into a deliberate full-padding gap.
    const autoPlacedPositions = await autoPlaceGraphMembers(
        unplacedGroups.map((group) => {
            const placementWidth = getGraphMemberPlacementWidth(
                group.nodes.find((groupNode) => groupNode.id === group.name),
                group.boundingBox.width,
                group.nodes.length > 1
            );

            return {
                height: group.boundingBox.height,
                // What the member measures beyond the box it paints — its label, for a plain task.
                // Auto-place mirrors this on the left so the frame's centre lands on the block.
                labelOverhang: group.boundingBox.width - placementWidth,
                name: group.name,
                width: placementWidth,
            };
        }),
        transitions,
        [
            getGraphStartPinnedBox(),
            ...groups
                .filter((group) => !!group.contentPosition)
                .map((group) => ({
                    height: group.boundingBox.height,
                    name: group.name,
                    paintedWidth: getGraphMemberPlacementWidth(
                        group.nodes.find((groupNode) => groupNode.id === group.name),
                        group.boundingBox.width,
                        group.nodes.length > 1
                    ),
                    width: group.boundingBox.width,
                    x: group.contentPosition!.x,
                    y: group.contentPosition!.y,
                })),
        ],
        direction,
        (dispatcherNode.data as NodeDataType).parameters?.startNode || groups[0]?.name
    );

    for (const group of unplacedGroups) {
        // ELK returns a position for every child it was given, so the fallback only fires if the
        // auto-place layout failed outright. Every member that missed out then stacks at the
        // content origin — overlapping, but present, editable and draggable apart, which beats
        // dropping them out of the frame. They are still reported as auto-placed, so the first
        // interaction persists the pile and the user's own drags replace it.
        group.contentPosition = autoPlacedPositions[group.name] ?? {x: 0, y: 0};
    }

    return Object.fromEntries(unplacedGroups.map((group) => [group.name, group.contentPosition!]));
}

/**
 * Translates each laid-out group so its bounding box starts at the member's own frame position,
 * and parents the whole group to the frame. Only the member node itself is draggable — its subtree
 * rides along through the existing collect-descendants path.
 */
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

/**
 * Splits the edge list three ways: edges the frame keeps (the transition/start routes plus each
 * group's own internal wiring), edges that stay outside, and edges that would cross the frame
 * boundary — which are dropped, because a member is reachable only through a transition.
 */
function partitionFrameEdges(
    workingEdges: Edge[],
    framedNodeIds: Set<string>,
    groups: MemberGroupType[]
): {framedEdges: Edge[]; remainingEdges: Edge[]} {
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

        // Anything else is only legitimate WITHIN one member's own subtree — an edge between two
        // members is a transition, and there is no structural chain across a free-form frame.
        if (sourceMemberName && sourceMemberName === memberNameByNodeId.get(edge.target)) {
            framedEdges.push(edge);
        }
    }

    return {framedEdges, remainingEdges};
}
