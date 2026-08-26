import {LayoutDirectionType} from '@/shared/constants';
import {GraphTransitionType} from '@/shared/types';

import {loadElk} from '../elkLayoutUtils';
import {isDynamicTransitionTarget} from './graphTransitionMutations';

import type {ElkExtendedEdge, ElkNode} from 'elkjs/lib/elk-api';

export const GRAPH_FRAME_PADDING = 24;
export const GRAPH_FRAME_HEADER_HEIGHT = 40;
export const GRAPH_FRAME_MIN_WIDTH = 320;
export const GRAPH_FRAME_MIN_HEIGHT = 200;
export const GRAPH_START_SIZE = {height: 32, width: 72};
export const GRAPH_MEMBER_NOMINAL_SIZE = {height: 100, width: 240};

// The box a plain task member PAINTS. Its node element is far wider — the label sits beside the box
// and is measured with it — but the label overflows into empty canvas and nothing has to be spaced
// around it, so placement uses this and only sizing uses the measured width.
export const GRAPH_MEMBER_BOX_WIDTH = 72;

// Gap between two members auto-placed side by side, before any room their labels need on top.
export const GRAPH_MEMBER_SPACING = 40;

// Clear space kept beyond the longest label, past which the frame's border is drawn. The frame
// mirrors its left inset to centre the boxes, so this lands on BOTH sides — the left as plain
// whitespace, the right as whatever the labels do not use. A bare frame padding here reads as the
// labels being crowded against the border, because the left has the same measurement all to itself.
export const GRAPH_LABEL_CLEARANCE = 72;

// Space kept clear between the frame's leading edge and the member block, so the lanes a doubling
// back transition is routed through have somewhere to stack. Wide enough for a few of them; a graph
// with more loops than fit will still crowd them, which is a fair trade against the space a gutter
// sized for the worst case would cost every graph that has none.
export const GRAPH_LOOP_GUTTER = 72;

// The attribute `GraphFrameNode` paints its graph id in. It is what the canvas hit-tests to trace a
// pointer release — a transition dropped on empty space, a component dropped into the box — back to
// the graph it happened over, so the component and the hit tests must read it from here.
export const GRAPH_FRAME_ID_ATTRIBUTE = 'data-graph-frame-id';

// Suffix every frame node's id carries. `getGraphFrameId` below is the only place it is applied.
export const GRAPH_FRAME_ID_SUFFIX = '-graph-frame';

// Gap right of the rightmost top-row member when finding a free spot for a newly-added member.
const FREE_SPOT_GAP = 60;

// Gap right of the start marker when the frame has no members yet.
const FREE_SPOT_START_GAP = 40;

/** The id of the frame node holding `graphId`'s members. */
export function getGraphFrameId(graphId: string): string {
    return `${graphId}${GRAPH_FRAME_ID_SUFFIX}`;
}

/** The graph a frame node id belongs to — undefined for any id that is not a frame's. */
export function getGraphIdFromFrameNodeId(nodeId: string | undefined): string | undefined {
    if (!nodeId || !nodeId.endsWith(GRAPH_FRAME_ID_SUFFIX)) {
        return undefined;
    }

    return nodeId.slice(0, -GRAPH_FRAME_ID_SUFFIX.length);
}

export interface GraphMemberBoxI {
    height: number;
    name: string;
    /**
     * How wide the member's own BOX is painted, when that differs from `width`.
     *
     * A plain task paints a fixed icon box and hangs its label off the right of it, and the label is
     * measured as part of the node element — so `width` is the box plus a label, while everything a
     * reader sees as "the member" ends at the box. Frame sizing needs both: wide enough that the
     * label is not cut off by the border, centred on what is painted. Absent means the two are the
     * same, which is true of any member that renders as a real box.
     */
    paintedWidth?: number;
    width: number;
    x: number;
    y: number;
}

/** The member's painted box width — `width` for anything that paints its whole measured width. */
export function getGraphMemberPaintedWidth(memberBox: GraphMemberBoxI): number {
    return memberBox.paintedWidth ?? memberBox.width;
}

// Stands in for the Start pill among the pinned boxes handed to `autoPlaceGraphMembers`. It is not
// a member and never becomes one, so the name only has to be one no task could carry — task names
// are identifiers, so the angle brackets are enough.
const GRAPH_START_PINNED_BOX_NAME = '<graph-start>';

/**
 * The Start pill's box in the frame's CONTENT coordinates, as `createGraphNode` pins it. Auto-place
 * callers pass it in so their results are laid out clear of the pill rather than on top of it.
 */
export function getGraphStartPinnedBox(): GraphMemberBoxI {
    return {
        height: GRAPH_START_SIZE.height,
        name: GRAPH_START_PINNED_BOX_NAME,
        width: GRAPH_START_SIZE.width,
        x: GRAPH_FRAME_PADDING,
        y: 0,
    };
}

/**
 * Member coordinates are stored relative to the frame's CONTENT origin, which sits
 * `GRAPH_FRAME_HEADER_HEIGHT` below the frame node's own (0, 0) — the header occupies that band.
 * These two helpers are the single place that offset is applied/removed; callers should never
 * open-code it.
 */
export function toFrameChildPosition(position: {x: number; y: number}): {x: number; y: number} {
    return {x: position.x, y: position.y + GRAPH_FRAME_HEADER_HEIGHT};
}

export function fromFrameChildPosition(position: {x: number; y: number}): {x: number; y: number} {
    return {x: position.x, y: position.y - GRAPH_FRAME_HEADER_HEIGHT};
}

/**
 * The frame's auto-size: wide enough for every member's labels, tall enough for the union of their
 * boxes, honouring the frame's configured minimums. Height also accounts for the header band sitting
 * above the content origin.
 *
 * Width mirrors the leftmost member's own inset rather than adding a fixed pad, so the member block
 * ends up horizontally CENTRED in the box: whatever gap sits left of it sits right of it too. The
 * mirrored gap is never narrower than the loop gutter, so the two sides stay equal even when a member
 * has been dragged right up against the leading edge.
 *
 * The mirror is taken on the PAINTED boxes, and on nothing else. A plain task's label hangs off the
 * right of its box and is measured with it, so mirroring the label's right edge pushed the whole
 * visible block left of centre by however long the longest label happened to be — which is what made
 * an auto-arranged graph sit well left of the chain edge entering the frame at its centre.
 *
 * There is deliberately no second term widening the frame to fit the labels, tempting as one is.
 * Centring means the right gap EQUALS the left inset, so any term that can exceed the mirror breaks
 * it by construction — and one sized off the labels would, because this runs on nodes React Flow has
 * not measured yet and falls back to the nominal size, reporting a label's worth of width for members
 * whose labels are short. Room for the labels is reserved once, on the left, by
 * `autoPlaceGraphMembers`; mirroring that inset is what puts the same room back on the right. A
 * painted width is the same number whichever source measured it, so the two halves cannot drift.
 *
 * Doing it in the sizing keeps every member exactly where it was stored — the alternative, shifting
 * the block to centre it, would put a second offset between stored and rendered coordinates alongside
 * the header band, and that offset is deliberately the only one.
 */
export function computeGraphFrameSize(memberBoxes: GraphMemberBoxI[]): {height: number; width: number} {
    if (memberBoxes.length === 0) {
        return {height: GRAPH_FRAME_MIN_HEIGHT, width: GRAPH_FRAME_MIN_WIDTH};
    }

    const paintedLeft = Math.min(...memberBoxes.map((memberBox) => memberBox.x));
    const paintedRight = Math.max(
        ...memberBoxes.map((memberBox) => memberBox.x + getGraphMemberPaintedWidth(memberBox))
    );
    const maxBottom = Math.max(...memberBoxes.map((memberBox) => memberBox.y + memberBox.height));

    return {
        height: Math.max(GRAPH_FRAME_MIN_HEIGHT, GRAPH_FRAME_HEADER_HEIGHT + maxBottom + GRAPH_FRAME_PADDING),
        width: Math.max(GRAPH_FRAME_MIN_WIDTH, paintedRight + Math.max(paintedLeft, GRAPH_LOOP_GUTTER)),
    };
}

/**
 * A spot for a newly-added member: right of the rightmost box on the top row (the row holding the
 * smallest `y` among existing members), or — when the frame has no members yet — right of where
 * the Start marker sits.
 */
export function findFreeSpot(memberBoxes: GraphMemberBoxI[]): {x: number; y: number} {
    if (memberBoxes.length === 0) {
        return {x: GRAPH_START_SIZE.width + FREE_SPOT_START_GAP, y: 0};
    }

    const topRowY = Math.min(...memberBoxes.map((memberBox) => memberBox.y));
    const topRowBoxes = memberBoxes.filter((memberBox) => memberBox.y === topRowY);
    const rightmostEdge = Math.max(...topRowBoxes.map((memberBox) => memberBox.x + memberBox.width));

    return {x: rightmostEdge + FREE_SPOT_GAP, y: topRowY};
}

/**
 * Auto-places unplaced members with ELK's layered algorithm, using only the static (non-dynamic,
 * known-target) transitions as edges, then offsets every result clear of the already-positioned
 * ("pinned") members so the two groups never overlap.
 *
 * Lays out along the canvas's own flow axis — downwards in TB, rightwards in LR — because that is
 * the axis a transition enters and leaves a member on. Arranging across the flow instead puts every
 * transition at right angles to the row it just built, which reads as a tangle rather than a chain.
 *
 * The block is then inset from the frame's leading edge by enough that `computeGraphFrameSize`
 * mirrors THAT inset rather than falling back to one of its other terms — which is what actually
 * centres the result in the frame. Two things can otherwise win that Math.max and leave the block
 * sitting left of centre: the room the labels need on the right (they hang off the boxes and are
 * measured with them, so the inset has to match the longest overhang), and the frame's minimum
 * width (a small graph has to be pushed to the middle of a box wider than it needs). The inset
 * covers both, so the painted block's centre line is the frame's, directly under the Start pill —
 * which `alignStartPillWithEntryMember` puts on the entry member's own centre.
 *
 * The entry member is pinned to the first layer. A graph's transitions routinely form a cycle, and
 * layered layout has no notion of which node the RUN starts at — it breaks the cycle wherever its
 * heuristic prefers, which regularly lands the last member on top with the whole graph reading
 * backwards. The graph already knows its entry point, so it says so.
 */
export async function autoPlaceGraphMembers(
    memberSizes: Array<{height: number; labelOverhang?: number; name: string; width: number}>,
    transitions: GraphTransitionType[],
    pinned: GraphMemberBoxI[],
    direction: LayoutDirectionType = 'TB',
    startNodeName?: string
): Promise<Record<string, {x: number; y: number}>> {
    const isHorizontalFlow = direction === 'LR';
    const memberNames = new Set(memberSizes.map((memberSize) => memberSize.name));

    const elkEdges: ElkExtendedEdge[] = transitions
        .filter(
            (transition) =>
                memberNames.has(transition.from) &&
                !isDynamicTransitionTarget(transition.to) &&
                memberNames.has(transition.to)
        )
        .map((transition, transitionIndex) => ({
            id: `${transition.from}->${transition.to}-${transitionIndex}`,
            sources: [transition.from],
            targets: [transition.to],
        }));

    // Members are laid out at the width they PAINT, so their boxes line up in a column instead of
    // zig-zagging by however long each label happens to be. What a label needs is room to its right,
    // which is a question of SPACING rather than of width: siblings share a row in TB, and a row
    // spaced by the boxes alone puts the next member's box underneath its neighbour's label. So the
    // longest overhang is added to the gap between members instead — leaving the boxes aligned and
    // the labels clear.
    //
    // TB only. In LR a sibling sits below rather than beside, and a regular node's label is
    // positioned absolutely there, contributing nothing to measure and needing nothing spaced.
    const maxLabelOverhang = isHorizontalFlow
        ? 0
        : Math.max(0, ...memberSizes.map((memberSize) => memberSize.labelOverhang ?? 0));

    const elkGraph: ElkNode = {
        children: memberSizes.map((memberSize) => ({
            height: memberSize.height,
            id: memberSize.name,
            ...(memberSize.name === startNodeName
                ? {layoutOptions: {'elk.layered.layering.layerConstraint': 'FIRST'}}
                : {}),
            width: memberSize.width,
        })),
        edges: elkEdges,
        id: 'graph',
        layoutOptions: {
            'elk.algorithm': 'layered',
            'elk.direction': isHorizontalFlow ? 'RIGHT' : 'DOWN',
            'elk.layered.spacing.nodeNodeBetweenLayers': '80',
            'elk.spacing.nodeNode': `${GRAPH_MEMBER_SPACING + maxLabelOverhang}`,
        },
    };

    const elk = await loadElk();
    const layoutedGraph = await elk.layout(elkGraph);

    // The pinned group is cleared along the FLOW axis, so the auto-placed block starts after it
    // rather than beside it — in TB that is below the Start pill, in LR to its right.
    const pinnedEnd =
        pinned.length === 0
            ? 0
            : Math.max(
                  ...pinned.map((memberBox) =>
                      isHorizontalFlow ? memberBox.x + memberBox.width : memberBox.y + memberBox.height
                  )
              );
    const flowOffset = pinnedEnd + GRAPH_FRAME_PADDING;

    const laidOutChildren = layoutedGraph.children ?? [];

    const crossStartOf = (child: ElkNode) => (isHorizontalFlow ? (child.y ?? 0) : (child.x ?? 0));
    const crossEndOf = (child: ElkNode) =>
        crossStartOf(child) + (isHorizontalFlow ? (child.height ?? 0) : (child.width ?? 0));

    // ELK pads its own graph, so the block is normalised to start at zero and inset deliberately
    // rather than inheriting whatever padding the run happened to produce.
    const blockStart = laidOutChildren.length === 0 ? 0 : Math.min(...laidOutChildren.map(crossStartOf));
    const blockEnd = laidOutChildren.length === 0 ? 0 : Math.max(...laidOutChildren.map(crossEndOf));

    const crossInset = getCrossAxisInset(laidOutChildren, memberSizes, isHorizontalFlow, blockStart, blockEnd);

    const positionsByName: Record<string, {x: number; y: number}> = {};

    for (const layoutedChild of laidOutChildren) {
        const crossPosition = crossStartOf(layoutedChild) - blockStart + crossInset;

        positionsByName[layoutedChild.id] = {
            x: isHorizontalFlow ? (layoutedChild.x ?? 0) + flowOffset : crossPosition,
            y: isHorizontalFlow ? crossPosition : (layoutedChild.y ?? 0) + flowOffset,
        };
    }

    return positionsByName;
}

/**
 * How far the laid-out block is inset from the frame's leading edge on the CROSS axis.
 *
 * In TB that is the horizontal inset `computeGraphFrameSize` mirrors, so it has to be at least large
 * enough that the mirror term wins the frame's width — otherwise the frame is sized by the labels or
 * by its own minimum and the block ends up left of centre. Both are covered:
 *
 * - `labelOverhang + clearance`, so the room the longest label needs beyond its own box on the right
 *   is matched on the left, with clear space beyond it rather than the border cutting close.
 * - `(minimum width - block width) / 2`, so a block narrower than the frame's minimum is pushed to
 *   the middle of it rather than parked against the gutter.
 *
 * In LR the cross axis is vertical, where none of this applies: labels are positioned absolutely and
 * measure nothing, and the frame's height is not mirrored. The gutter is the whole answer there.
 */
function getCrossAxisInset(
    laidOutChildren: ElkNode[],
    memberSizes: Array<{height: number; labelOverhang?: number; name: string; width: number}>,
    isHorizontalFlow: boolean,
    blockStart: number,
    blockEnd: number
): number {
    if (isHorizontalFlow || laidOutChildren.length === 0) {
        return GRAPH_LOOP_GUTTER;
    }

    const labelOverhangByName = new Map(
        memberSizes.map((memberSize) => [memberSize.name, memberSize.labelOverhang ?? 0])
    );

    const labelEnd = Math.max(
        ...laidOutChildren.map(
            (child) => (child.x ?? 0) + (child.width ?? 0) + (labelOverhangByName.get(child.id) ?? 0)
        )
    );

    return Math.max(
        GRAPH_LOOP_GUTTER,
        Math.max(0, labelEnd - blockEnd) + GRAPH_LABEL_CLEARANCE,
        (GRAPH_FRAME_MIN_WIDTH - (blockEnd - blockStart)) / 2
    );
}
