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
    /** Where each member currently sits, in the frame's CONTENT coordinates. */
    positions: Record<string, XYPosition>;
    /** How large each member currently renders — a dispatcher member over its whole subtree. */
    sizes: Record<string, {height: number; width: number}>;
}

function getMemberSize(node: Node): {height: number; width: number} {
    // `measured` is what React Flow reports once the node is on screen; `width`/`height` cover the
    // nodes the layout sizes itself, and the nominal size is the last resort for one with neither.
    return {
        height: node.measured?.height ?? node.height ?? GRAPH_MEMBER_NOMINAL_SIZE.height,
        width: node.measured?.width ?? node.width ?? GRAPH_MEMBER_NOMINAL_SIZE.width,
    };
}

/**
 * Where the graph's members currently RENDER, in the coordinates the model stores.
 *
 * Measured through `collectGraphMemberBoxes`, so a member is the box it actually renders as rather
 * than its own node box — a dispatcher member covers its whole subtree, and the free-spot search
 * places a newcomer clear of THAT instead of laying it on top of the member's children. Auto-arrange
 * takes the same measurement from the same collector; there is deliberately no second sizing rule.
 *
 * Read for geometry only — the free-spot search needs to know what a newcomer must avoid, and a
 * member is just as much in the way whether or not its position was ever persisted. Persistence
 * goes through the auto-placed channel instead, which is the layout's own report of what it had
 * to invent.
 */
export function readGraphMemberCanvasState(graphId: string, nodes: Node[]): GraphMemberCanvasStateI {
    const positions: Record<string, XYPosition> = {};
    const sizes: Record<string, {height: number; width: number}> = {};

    for (const memberBox of collectGraphMemberBoxes(graphId, nodes)) {
        positions[memberBox.name] = {x: memberBox.x, y: memberBox.y};
        sizes[memberBox.name] = {height: memberBox.height, width: memberBox.width};
    }

    return {positions, sizes};
}

/**
 * The box each of the graph's members currently occupies inside its frame, in the frame's CONTENT
 * coordinates — the member node UNIONED with everything in its own subtree.
 *
 * A dispatcher member is one node on the canvas but renders as a whole subtree of siblings parented
 * to the same frame, and its own node box says nothing about how tall that subtree is. Measuring
 * per member rather than per node is what `layoutGraphFrames` does when it lays each member group
 * out and records its bounding box; this is the same measurement taken off the live canvas, and the
 * single place the drag-time frame resize, auto-arrange and the add-a-member free-spot search all
 * take it.
 */
export function collectGraphMemberBoxes(graphId: string, nodes: Node[]): GraphMemberBoxI[] {
    const frameId = getGraphFrameId(graphId);

    // Built once: this runs per frame child on every position change React Flow emits during a drag.
    const nodesById = new Map(nodes.map((node) => [node.id, node]));

    const nodesByMemberName = new Map<string, Node[]>();

    for (const node of nodes) {
        if (node.parentId !== frameId || findGraphMemberOwner(node, nodesById) !== graphId) {
            continue;
        }

        const memberName = findTopLevelGraphMemberName(node, nodesById, graphId);

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

/**
 * One member's box: the union of every node in its group, plus the width its own box PAINTS.
 *
 * The painted width is what the frame centres on, and the two differ only for a plain task, whose
 * label is measured as part of its node element — a member owning a subtree stands for all of it, so
 * for those the box really is as wide as it measures. Owning a subtree is a fact about the group,
 * which is why this is computed here rather than per node.
 */
function buildMemberBox(memberName: string, memberNodes: Node[]): GraphMemberBoxI {
    const ownsSubtree = memberNodes.length > 1;

    const nodeBoxes = memberNodes.map((node) => {
        const contentPosition = fromFrameChildPosition(node.position);
        const size = getMemberSize(node);

        return {height: size.height, width: size.width, x: contentPosition.x, y: contentPosition.y};
    });

    const x = Math.min(...nodeBoxes.map((nodeBox) => nodeBox.x));
    const y = Math.min(...nodeBoxes.map((nodeBox) => nodeBox.y));
    const width = Math.max(...nodeBoxes.map((nodeBox) => nodeBox.x + nodeBox.width)) - x;
    const height = Math.max(...nodeBoxes.map((nodeBox) => nodeBox.y + nodeBox.height)) - y;

    const memberNode = memberNodes.find((node) => node.id === memberName);

    return {
        height,
        name: memberName,
        paintedWidth: getGraphMemberPlacementWidth(memberNode, width, ownsSubtree),
        width,
        x,
        y,
    };
}

/**
 * How wide a member is for PLACEMENT purposes.
 *
 * A plain task paints a fixed icon box with its label beside it, and the label is measured as part
 * of the node element — so laying members out at their measured width spaces them by their longest
 * label and leaves the boxes themselves zig-zagging across each row, which is what makes the
 * forward edges cross. The label overflows into empty canvas and nothing needs spacing around it,
 * so placement uses the painted box.
 *
 * A member that renders as a real box keeps its measured width, because for those the box IS that
 * wide: a cluster root paints one, and a member owning a subtree stands for all of it. Whether it
 * owns a subtree is the caller's to say — it is a fact about the member's GROUP, which a single
 * box has no way to report.
 */
export function getGraphMemberPlacementWidth(
    node: Node | undefined,
    measuredWidth: number,
    ownsSubtree: boolean
): number {
    const nodeData = node?.data as NodeDataType | undefined;

    if (!nodeData || ownsSubtree || nodeData.clusterRoot) {
        return measuredWidth;
    }

    return Math.min(measuredWidth, GRAPH_MEMBER_BOX_WIDTH);
}

/**
 * The member sizes `autoPlaceGraphMembers` lays out, in declaration order on the canvas.
 *
 * Each member is measured over the whole subtree it owns, so auto-arrange leaves room for what a
 * dispatcher member actually renders as instead of laying its neighbour on top of its children.
 *
 * Inherited from the shared collector: a member only counts when its node is parented to the frame.
 * That holds on the live canvas — `layoutGraphFrames` parents every frame child before React Flow
 * ever renders it — and where it somehow does not, `handleAutoArrange`'s empty-sizes guard turns
 * this into a no-op rather than a layout computed from half the members.
 */
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

/** The single place `metadata.ui.nodePosition` is written, so no other `ui` field can be dropped. */
export function withMemberNodePosition(member: WorkflowTask, position: XYPosition): WorkflowTask {
    return {...member, metadata: {...member.metadata, ui: {...member.metadata?.ui, nodePosition: position}}};
}

interface PlaceGraphMembersPropsI {
    /** The graph's pending auto-placed positions, flushed alongside the insertion. */
    autoPlacedPositions?: Record<string, XYPosition>;
    /** Where the members currently sit and how large they render, for the free-spot search. */
    canvasState: GraphMemberCanvasStateI;
    /** Content-origin position a newly added member was dropped at, when there was one. */
    dropPosition?: XYPosition;
    previousMembers: WorkflowTask[];
    updatedMembers: WorkflowTask[];
}

/**
 * Gives an added graph member a concrete position, and flushes the positions the layout pre-pass
 * had to invent for its siblings.
 *
 * A new member goes exactly where it was dropped, or — added from the frame header, where there is
 * no drop point — at the free spot beside the existing ones. Adding a node is a first interaction
 * with the graph, so the pending auto-placed siblings are persisted in the same write and the graph
 * does not rearrange itself around the newcomer on the next layout.
 */
export function placeGraphMembers({
    autoPlacedPositions,
    canvasState,
    dropPosition,
    previousMembers,
    updatedMembers,
}: PlaceGraphMembersPropsI): {addedMemberName: string | undefined; members: WorkflowTask[]} {
    const previousMemberNames = new Set(previousMembers.map((member) => member.name));

    const addedMemberName = updatedMembers.find((member) => !previousMemberNames.has(member.name))?.name;

    const resolvePosition = (member: WorkflowTask): XYPosition | undefined => {
        if (member.name === addedMemberName) {
            return dropPosition ?? findFreeSpot(buildOccupiedBoxes(previousMembers, canvasState));
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

/**
 * The boxes already occupied inside the frame, so a new member can be placed clear of them.
 *
 * Falls back to where a member currently RENDERS when it carries no stored position — that is a
 * geometry question, not a persistence one, so it deliberately does not go through the auto-placed
 * channel: a box left out here would let the newcomer land on top of a member that is plainly
 * on screen.
 */
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
