export type GraphTransitionEdgeKindType = 'back' | 'forward' | 'self';

export interface GraphTransitionEdgePathParamsI {
    // Shared y every arc in one graph routes through, so arcs travel in a single clear band
    // instead of bowing relative to whichever endpoint happens to sit higher. Lane entry points
    // are NOT level — a lane holding a three-task chain starts well above a single-task lane, and
    // an empty lane's placeholder starts below both — so without a shared band, arcs swoop
    // between unlevel anchors and cut through node labels. Omitted (dagre, unit tests) falls back
    // to the endpoint-relative bow.
    bandY?: number;
    kind: GraphTransitionEdgeKindType;
    offset: number;
    sourceX: number;
    sourceY: number;
    targetX: number;
    targetY: number;
}

export interface GraphTransitionEdgePathResultI {
    labelX: number;
    labelY: number;
    path: string;
    // A point just after the arc leaves its source, for a label naming where this transition
    // goes. Anchored at the start rather than the midpoint so that when several arcs cross, each
    // name stays attached to the lane it departs from.
    startLabelX: number;
    startLabelY: number;
}

// How far along the curve the start label sits. Kept low on purpose: at this t the x is ~94%
// source, so a label sits essentially above its own lane. Further along, x blends toward the
// target — which drags a long back arc's label across the container and stacks it on top of a
// neighbouring arc's. Anchoring at the foot makes label separation equal to LANE separation,
// so collisions are impossible rather than merely unlikely.
const START_LABEL_T = 0.15;

function cubicPointAt(t: number, start: number, controlOne: number, controlTwo: number, end: number): number {
    const inverse = 1 - t;

    return (
        inverse * inverse * inverse * start +
        3 * inverse * inverse * t * controlOne +
        3 * inverse * t * t * controlTwo +
        t * t * t * end
    );
}

// A self loop's endpoints coincide, so the lobe's proportions come entirely from these two. The
// curve reaches ~0.65x the control width to each side but ~0.75x the control height, so equal
// numbers render as a tall sliver — the width leads the height to keep the lobe read as a loop.
const SELF_LOOP_BASE_WIDTH = 42;
const SELF_LOOP_WIDTH_STEP = 13;
const SELF_LOOP_HEIGHT = 52;
// Keeps a self loop's label above the lobe rather than inside it
const SELF_LOOP_LABEL_CLEARANCE = 12;
const FORWARD_EDGE_BASE_BOW = 36;
// Half of BACK_EDGE_BOW_STEP, matching the ratio between FORWARD_EDGE_BASE_BOW and
// BACK_EDGE_BASE_BOW, so stacked forward arcs fan out at the same proportional rate back arcs do.
const FORWARD_EDGE_BOW_STEP = 16;
const BACK_EDGE_BASE_BOW = 72;
const BACK_EDGE_BOW_STEP = 32;
// Separation between arcs stacked within the shared band
const BAND_ROUTE_STEP = 18;

/**
 * Computes the SVG path (and a label anchor point) for one `graphTransition` overlay edge —
 * pure geometry, so it is unit-testable independently of ReactFlow's rendering pipeline.
 *
 * Bows OUTWARD along the axis perpendicular to the source/target separation, so the curve
 * routes around the lane frame instead of cutting straight through the lanes in between: in a
 * TB layout (lanes spread left-to-right) that perpendicular axis is vertical, so the arc rises
 * above the frame; in an LR layout (lanes stacked top-to-bottom) it is horizontal, so the arc
 * bows out past the frame's side edge — literally "around the frame side" for that orientation.
 * The dominant-spread test (`|dx| >= |dy|`) infers this from the actual handle coordinates
 * ReactFlow hands back, so the function needs no direction prop of its own.
 *
 * `offset` ranks this edge among same-kind transitions sharing similar geometry (assigned by
 * the caller, `createGraphEdges`, as a running per-kind counter) so parallel back-edges, parallel
 * forward-edges (or stacked self-loops on adjacent lanes) bow progressively further out instead of
 * overlapping. Topological lane ordering (see `orderGraphNodeIndexes`) makes `forward` the common
 * case — most arcs in a well-ordered graph flow left to right — so leaving it unstacked would
 * reintroduce the "ambiguous arrowheads" overlap defect stage 1 was not supposed to worsen.
 */
export default function computeGraphTransitionEdgePath({
    bandY,
    kind,
    offset,
    sourceX,
    sourceY,
    targetX,
    targetY,
}: GraphTransitionEdgePathParamsI): GraphTransitionEdgePathResultI {
    if (kind === 'self') {
        const loopWidth = SELF_LOOP_BASE_WIDTH + offset * SELF_LOOP_WIDTH_STEP;
        const loopTopY = sourceY - SELF_LOOP_HEIGHT;

        const path = [
            `M ${sourceX},${sourceY}`,
            `C ${sourceX - loopWidth},${loopTopY}`,
            `${sourceX + loopWidth},${loopTopY}`,
            `${targetX},${targetY}`,
        ].join(' ');

        return {
            labelX: sourceX,
            labelY: loopTopY,
            path,
            // Clear of the lobe, not inside it. A self loop encloses a small area and a point on
            // the curve near its start sits within that enclosure, so the badge would cover the
            // very loop it names.
            startLabelX: sourceX,
            startLabelY: loopTopY - SELF_LOOP_LABEL_CLEARANCE,
        };
    }

    const deltaX = targetX - sourceX;
    const deltaY = targetY - sourceY;
    const isHorizontalSpread = Math.abs(deltaX) >= Math.abs(deltaY);

    const bow =
        kind === 'back'
            ? BACK_EDGE_BASE_BOW + offset * BACK_EDGE_BOW_STEP
            : FORWARD_EDGE_BASE_BOW + offset * FORWARD_EDGE_BOW_STEP;

    // Route through the shared band: rise out of the source, cross at a single height, drop into
    // the target. Control points sit ON the band at each endpoint's x, so the curve leaves and
    // arrives vertically — every arrowhead meets its node from directly above, whatever height
    // that node sits at. `offset` lifts stacked arcs clear of one another within the band.
    //
    // A horizontal band only makes sense when the lanes spread horizontally (TB). In LR the lanes
    // stack vertically, so every entry shares roughly one x — banding there collapses the curve
    // into a straight vertical line drawn through the nodes. LR keeps the side bow below.
    if (bandY !== undefined && isHorizontalSpread) {
        const routedY = bandY - offset * BAND_ROUTE_STEP;

        const path = `M ${sourceX},${sourceY} C ${sourceX},${routedY} ${targetX},${routedY} ${targetX},${targetY}`;

        return {
            labelX: (sourceX + targetX) / 2,
            // Cubic Bezier midpoint: (P0 + 3*P1 + 3*P2 + P3) / 8
            labelY: (sourceY + targetY + 6 * routedY) / 8,
            path,
            startLabelX: cubicPointAt(START_LABEL_T, sourceX, sourceX, targetX, targetX),
            startLabelY: cubicPointAt(START_LABEL_T, sourceY, routedY, routedY, targetY),
        };
    }

    if (isHorizontalSpread) {
        const bowY = Math.min(sourceY, targetY) - bow;
        const controlOneX = sourceX + deltaX / 3;
        const controlTwoX = sourceX + (deltaX * 2) / 3;

        const path = `M ${sourceX},${sourceY} C ${controlOneX},${bowY} ${controlTwoX},${bowY} ${targetX},${targetY}`;

        return {
            labelX: (sourceX + targetX) / 2,
            labelY: bowY,
            path,
            startLabelX: cubicPointAt(START_LABEL_T, sourceX, controlOneX, controlTwoX, targetX),
            startLabelY: cubicPointAt(START_LABEL_T, sourceY, bowY, bowY, targetY),
        };
    }

    const bowX = Math.max(sourceX, targetX) + bow;
    const controlOneY = sourceY + deltaY / 3;
    const controlTwoY = sourceY + (deltaY * 2) / 3;

    const path = `M ${sourceX},${sourceY} C ${bowX},${controlOneY} ${bowX},${controlTwoY} ${targetX},${targetY}`;

    return {
        labelX: bowX,
        labelY: (sourceY + targetY) / 2,
        path,
        // Pinned OUT at the bow rather than on the curve near the source. With lanes stacked
        // vertically, a point near the source shares the lane's x — which is where the node's own
        // label text lives, so the transition name would sit on top of it. The bow is the one
        // part of an LR arc that is clear of every node.
        startLabelX: bowX,
        startLabelY: cubicPointAt(START_LABEL_T, sourceY, controlOneY, controlTwoY, targetY),
    };
}
