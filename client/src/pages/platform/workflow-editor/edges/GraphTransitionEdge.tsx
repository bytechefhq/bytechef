import Badge from '@/components/Badge/Badge';
import {
    GRAPH_TRANSITION_EDGE_COLOR,
    GRAPH_TRANSITION_EDGE_DANGLING_COLOR,
    GRAPH_TRANSITION_EDGE_SELECTED_COLOR,
} from '@/shared/constants';
import {BaseEdge, EdgeLabelRenderer, EdgeProps, Position, getBezierPath} from '@xyflow/react';
import {useEffect, useMemo, useState} from 'react';
import {twMerge} from 'tailwind-merge';

import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useExecutedEdgeStatus from './useExecutedEdgeStatus';

/** How far a doubling-back transition bows out to the side, and how fast that grows with its span. */
const BACKWARD_BOW_MIN = 48;
const BACKWARD_BOW_MAX = 220;
const BACKWARD_BOW_RATIO = 0.35;

/** A self transition spans nothing, so its reach is fixed rather than derived from the distance. */
const SELF_BOW = 74;

// Concurrent returns over the same stretch would otherwise be drawn on top of each other, since
// their reach comes from a span they share. Nesting them by rank separates them into concentric
// arcs; the rank is the declaration index taken modulo a few, because that index counts across the
// whole graph and would otherwise push a late transition arbitrarily wide.
const BOW_RANKS = 4;
const BOW_RANK_STEP = 26;

export interface GraphTransitionEdgeDataI {
    condition?: string;
    dangling?: boolean;
    dynamic?: boolean;
    graphId: string;
    index: number;
    /**
     * Suppresses the editing popover. Set by `toReadOnlyLayoutEdges` on every transition it hands to
     * a read-only or execution canvas, where the frame and its routing are shown but not edited.
     */
    readOnly?: boolean;
    to: string;
}

/**
 * Renders one `parameters.transitions[]` entry of a `graph/v1` dispatcher as an edge between two
 * members of its frame. Unlike the structural chain edges around the frame, a transition is a
 * first-class part of the definition: `createGraphEdges` emits exactly one of these per declared
 * transition, keyed by declaration index.
 *
 * Four states change how it paints:
 *
 * - **dynamic** — the transition's `to` is an expression resolved at run time, so it names no
 *   member. The edge returns to its own source (ending on that member's hidden stub anchor) and is
 *   dashed, because there is no target to point at.
 * - **dangling** — an endpoint names no member at all, only reachable through edits made outside
 *   the editor. Styled in the warning color here for completeness, but note that such an edge does
 *   NOT reach the canvas: its `source`/`target` reference a node id that does not exist, so React
 *   Flow drops it before rendering. `GraphTransitionsPanel` is the surface that actually shows a
 *   dangling transition, recomputing the same condition from the graph's members.
 * - **hovered or selected** — reveals a dynamic transition's resolved expression. A static
 *   transition's condition is always shown, since it is what decides whether the edge is taken.
 *   Kept off at rest so a frame full of transitions does not read as a wall of text; the same
 *   "reveal on interaction" affordance `WorkflowEdge` uses for its add-node button.
 * - **executed** — the run routed along this transition, so it takes the green (or, for a visit
 *   that failed, red) of the executed path. This one is not a peer of the others: it OUTRANKS
 *   hover and selection, which stop recolouring the stroke while it holds, because an execution
 *   view must not lose its executed signal to a passing pointer. Selection still announces itself
 *   through the thicker stroke, the revealed label and the popover. Which transitions were taken is
 *   decided from the graph's routing history, not from its members' states — see
 *   `getExecutedGraphTransitionStatus`.
 *
 * Selection additionally opens the editor for this one transition's target, condition and existence.
 * That editor is NOT rendered here: React Flow recreates its edge components whenever the edges array
 * is replaced, which a relayout does after every save, and an editor living inside one lost its
 * caret, its data pill popup and its pending keystrokes each time. This edge publishes only where its
 * label sits; `GraphTransitionEditorLayer` mounts the editor once at canvas level and reads which
 * transition is open from the edges' own selection.
 */
export default function GraphTransitionEdge({
    data,
    id,
    markerEnd,
    selected,
    source,
    sourcePosition,
    sourceX,
    sourceY,
    target,
    targetPosition,
    targetX,
    targetY,
}: EdgeProps) {
    const {condition, dangling, dynamic, graphId, index, to} = (data ?? {}) as Partial<GraphTransitionEdgeDataI>;

    const [isHovered, setIsHovered] = useState(false);

    const setGraphTransitionLabelPosition = useWorkflowEditorStore((state) => state.setGraphTransitionLabelPosition);

    // Keyed on the transition itself, not on the edge id — the id is `<graphId>-transition-<index>`
    // and says nothing about routing. A transition without a graph or a target names nothing the
    // run could have followed, so it asks nothing.
    const executedEdgeStatus = useExecutedEdgeStatus(
        id,
        graphId !== undefined && to !== undefined ? {from: source, graphId, to} : undefined
    );

    // Both a self transition and a dynamic stub end on the member they left, so one predicate
    // covers them: an orthogonal step path has nowhere to turn between two endpoints that share a
    // node, and degenerates into a line drawn through it.
    const returnsToSource = source === target;
    const isHorizontalFlow = sourcePosition === Position.Right;

    const {labelX, labelY, path} = useMemo(() => {
        // A transition that doubles back gets a bezier BOWED to one side rather than React Flow's
        // own. Its control points face away from each other, which between a bottom handle and a
        // top handle above it collapses the curve into a near-straight diagonal drawn across
        // whatever lies between the two. Pushing both controls out to the same side turns the same
        // two endpoints into a curve that reads as a return path. The bow grows with the distance
        // spanned, so a long return arcs wide and a short one stays tight.
        // A self transition is the same figure with no distance to span: it leaves the bottom of a
        // box and returns to the top of the SAME box, so it doubles back by definition and only
        // needs a fixed reach. Sharing one curve with the ordinary return keeps the two reading as
        // the same kind of thing.
        const flowsBackwards = returnsToSource || (isHorizontalFlow ? targetX < sourceX : targetY < sourceY);

        if (flowsBackwards) {
            const span = isHorizontalFlow ? Math.abs(sourceX - targetX) : Math.abs(sourceY - targetY);
            const rankReach = ((index ?? 0) % BOW_RANKS) * BOW_RANK_STEP;

            const bow = returnsToSource
                ? SELF_BOW + rankReach
                : Math.min(BACKWARD_BOW_MAX, BACKWARD_BOW_MIN + span * BACKWARD_BOW_RATIO) + rankReach;

            const path = isHorizontalFlow
                ? `M ${sourceX},${sourceY} C ${sourceX + bow},${sourceY - bow} ${targetX - bow},${targetY - bow} ${targetX},${targetY}`
                : `M ${sourceX},${sourceY} C ${sourceX - bow},${sourceY + bow} ${targetX - bow},${targetY - bow} ${targetX},${targetY}`;

            return {
                labelX: isHorizontalFlow ? (sourceX + targetX) / 2 : Math.min(sourceX, targetX) - bow / 2,
                labelY: isHorizontalFlow ? Math.min(sourceY, targetY) - bow / 2 : (sourceY + targetY) / 2,
                path,
            };
        }

        // Bezier for every transition between two members, including one that doubles back. A
        // frame's members are placed freely, so routing them on orthogonal rails reads as a circuit
        // diagram in which every near-miss looks like a junction — and rails can only be kept apart
        // by a router that sees them all at once, which per-edge routing cannot be. Curves cross
        // each other legibly, which is the property that actually matters here.
        const [bezierPath, bezierLabelX, bezierLabelY] = getBezierPath({
            sourcePosition,
            sourceX,
            sourceY,
            targetPosition,
            targetX,
            targetY,
        });

        return {labelX: bezierLabelX, labelY: bezierLabelY, path: bezierPath};
    }, [index, isHorizontalFlow, returnsToSource, sourcePosition, sourceX, sourceY, targetPosition, targetX, targetY]);

    const isActive = isHovered || !!selected;

    const strokeColor = dangling
        ? GRAPH_TRANSITION_EDGE_DANGLING_COLOR
        : isActive
          ? GRAPH_TRANSITION_EDGE_SELECTED_COLOR
          : GRAPH_TRANSITION_EDGE_COLOR;

    // Where the editor hangs, published rather than rendered here. Selection is what opens it — hover
    // only reveals the label, since a popover appearing under the pointer as it crosses a frame full
    // of transitions would be unusable — but the editor itself lives at canvas level, out of reach of
    // the remounts React Flow puts its edge components through. See `GraphTransitionEditorLayer`,
    // which reads selection off the edges and takes only this position from the store.
    useEffect(() => {
        setGraphTransitionLabelPosition(id, {labelX, labelY});
    }, [id, labelX, labelY, setGraphTransitionLabelPosition]);

    // A condition is shown whenever there is one. Which of a node's outgoing transitions will be
    // taken is decided entirely by these expressions, in declared order, so a graph that hides them
    // until each edge is hovered cannot be read at all — the branching is the thing the picture is
    // supposed to show. An unconditional transition has nothing to say and stays bare.
    //
    // A dynamic transition points at no member, so its dashed lobe alone says nothing about what it
    // is: it carries a small standing badge naming itself, which hover or selection swaps for the
    // expression it resolves at run time.
    let label: string | undefined;

    if (dynamic) {
        label = isActive ? `dynamic: ${to ?? ''}` : 'dynamic';
    } else {
        label = condition;
    }

    return (
        <>
            {/* Wide, invisible hit area — the visible stroke below is intentionally thin, so
                this widens the hover target without affecting the painted edge. */}
            <path
                d={path}
                fill="none"
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}
                stroke="transparent"
                strokeWidth={20}
            />

            <BaseEdge
                /* The executed colors are classes, matching every other edge on the canvas, so the
                   inline stroke stands down for them — an inline style would win over the class. */
                className={twMerge(
                    executedEdgeStatus === 'COMPLETED' && 'stroke-green-500',
                    executedEdgeStatus === 'FAILED' && 'stroke-red-500'
                )}
                id={id}
                markerEnd={markerEnd}
                path={path}
                style={{
                    fill: 'none',
                    ...(executedEdgeStatus ? {} : {stroke: strokeColor}),
                    strokeDasharray: dynamic ? '6 4' : undefined,
                    strokeWidth: isActive ? 2.5 : 2,
                }}
            />

            {label && (
                <EdgeLabelRenderer>
                    <div
                        className="nodrag nopan pointer-events-none absolute z-10"
                        style={{transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`}}
                    >
                        <Badge
                            label={label}
                            styleType={
                                dangling ? 'warning-filled' : isActive ? 'secondary-filled' : 'secondary-outline'
                            }
                        />
                    </div>
                </EdgeLabelRenderer>
            )}
        </>
    );
}
