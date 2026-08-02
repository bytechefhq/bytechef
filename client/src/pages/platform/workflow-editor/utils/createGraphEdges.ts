import {
    CHILDLESS_TASK_DISPATCHER_NAMES,
    EDGE_STYLES,
    GRAPH_TRANSITION_EDGE_COLOR,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {GraphNodeType, NodeDataType} from '@/shared/types';
import {Edge, MarkerType, Node} from '@xyflow/react';

import deriveGraphTransitionEdges from './deriveGraphTransitionEdges';
import {nestedBottomGhostIdForDispatcherTask} from './nestedBottomGhostId';
import orderGraphNodeIndexes from './orderGraphNodeIndexes';

// Handle id suffixes for the hidden anchor handles `WorkflowNode`/`PlaceholderNode` expose
// specifically for `graphTransition` overlay edges (see those components) — kept separate from
// the default (id-less) source/target handles the structural lane-chain edges bind to, so the
// overlay never shares a connection point with the underlying structural edge.
const GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX = 'graph-transition-source';
const GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX = 'graph-transition-target';

/**
 * Creates edges for one graph node's EMPTY lane (top ghost -> placeholder -> bottom ghost)
 */
function createEdgesForEmptyGraphNode(
    graphId: string,
    nodeIndex: number,
    handlePosition: 'left' | 'middle' | 'right'
): Edge[] {
    const topGhostId = `${graphId}-graph-top-ghost`;
    const bottomGhostId = `${graphId}-graph-bottom-ghost`;
    const placeholderId = `${graphId}-graph-node-${nodeIndex}-placeholder-0`;

    const topGhostHandlePosition = handlePosition === 'middle' ? 'bottom' : handlePosition;
    const bottomGhostHandlePosition = handlePosition === 'middle' ? 'top' : handlePosition;

    const edgeFromTopGhostToPlaceholder = {
        id: `${topGhostId}=>${placeholderId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: placeholderId,
        // Carries the node name chip (GraphNodeLabel) for this empty lane — mirrors how branch
        // splits its case label between 'labeledBranchCase' (empty case) and a plain 'workflow'
        // edge (non-empty case, handled in WorkflowEdge via graphData.nodeIndex).
        type: 'labeledGraphNode',
    };

    const edgeFromPlaceholderToBottomGhost = {
        id: `${placeholderId}=>${bottomGhostId}`,
        source: placeholderId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost];
}

/**
 * Creates edges for the trailing "add a new node" placeholder — always present, fixed to the
 * right side (mirrors fork-join's add-branch placeholder, which is likewise excluded from the
 * left/middle/right lane distribution).
 */
function createEdgesForTrailingPlaceholder(graphId: string, nodeCount: number): Edge[] {
    const topGhostId = `${graphId}-graph-top-ghost`;
    const bottomGhostId = `${graphId}-graph-bottom-ghost`;
    const placeholderId = `${graphId}-graph-node-${nodeCount}-placeholder-0`;

    // With no graph-node lanes the trailing placeholder is the only child and layout centers it under
    // the ghost bars — anchor to the center handles so its edges drop straight down instead of
    // S-curving out of the side handles.
    const topGhostHandlePosition = nodeCount === 0 ? 'bottom' : 'right';
    const bottomGhostHandlePosition = nodeCount === 0 ? 'top' : 'right';

    const edgeFromTopGhostToPlaceholder = {
        id: `${topGhostId}=>${placeholderId}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: placeholderId,
        type: 'smoothstep',
    };

    const edgeFromPlaceholderToBottomGhost = {
        id: `${placeholderId}=>${bottomGhostId}`,
        source: placeholderId,
        style: EDGE_STYLES,
        target: bottomGhostId,
        targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
        type: 'smoothstep',
    };

    return [edgeFromTopGhostToPlaceholder, edgeFromPlaceholderToBottomGhost];
}

// Helper to create edges for a single graph node's task chain (lane)
function createGraphNodeTaskEdges(
    graphId: string,
    tasks: WorkflowTask[],
    position: 'left' | 'middle' | 'right'
): Edge[] {
    const edges: Edge[] = [];
    const topGhostId = `${graphId}-graph-top-ghost`;
    const bottomGhostId = `${graphId}-graph-bottom-ghost`;

    let topGhostHandlePosition = 'left';
    let bottomGhostHandlePosition = 'left';

    if (position === 'middle') {
        topGhostHandlePosition = 'bottom';
        bottomGhostHandlePosition = 'top';
    } else if (position === 'right') {
        topGhostHandlePosition = 'right';
        bottomGhostHandlePosition = 'right';
    }

    if (!tasks || tasks.length === 0) {
        return edges;
    }

    const edgeFromTopGhostToFirstTask = {
        id: `${topGhostId}=>${tasks[0].name}`,
        source: topGhostId,
        sourceHandle: `${topGhostId}-${topGhostHandlePosition}`,
        style: EDGE_STYLES,
        target: tasks[0].name,
        type: 'workflow',
    };

    edges.push(edgeFromTopGhostToFirstTask);

    tasks.forEach((nodeTask, index) => {
        const sourceTaskId = nodeTask.name;
        const targetTaskId = tasks[index + 1]?.name;

        if (!targetTaskId) {
            return;
        }

        const sourceTaskComponentName = sourceTaskId.split('_')[0];

        if (
            TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName) &&
            !CHILDLESS_TASK_DISPATCHER_NAMES.includes(sourceTaskComponentName)
        ) {
            const nestedBottomGhostId = nestedBottomGhostIdForDispatcherTask(sourceTaskId);

            const edgeFromNestedGhostToNextTask = {
                id: `${nestedBottomGhostId}=>${targetTaskId}`,
                source: nestedBottomGhostId,
                style: EDGE_STYLES,
                target: targetTaskId,
                type: 'workflow',
            };

            edges.push(edgeFromNestedGhostToNextTask);
        } else {
            const edgeBetweenTasks = {
                id: `${sourceTaskId}=>${targetTaskId}`,
                source: sourceTaskId,
                style: EDGE_STYLES,
                target: targetTaskId,
                type: 'workflow',
            };

            edges.push(edgeBetweenTasks);
        }
    });

    const lastTaskId = tasks[tasks.length - 1].name;
    const lastTaskComponentName = lastTaskId.split('_')[0];

    if (
        TASK_DISPATCHER_NAMES.includes(lastTaskComponentName) &&
        !CHILDLESS_TASK_DISPATCHER_NAMES.includes(lastTaskComponentName)
    ) {
        const nestedBottomGhostId = nestedBottomGhostIdForDispatcherTask(lastTaskId);

        const edgeFromNestedGhostToBottomGhost = {
            id: `${nestedBottomGhostId}=>${bottomGhostId}`,
            source: nestedBottomGhostId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'workflow',
        };

        edges.push(edgeFromNestedGhostToBottomGhost);
    } else {
        const edgeFromLastTaskToBottomGhost = {
            id: `${lastTaskId}=>${bottomGhostId}`,
            source: lastTaskId,
            style: EDGE_STYLES,
            target: bottomGhostId,
            targetHandle: `${bottomGhostId}-${bottomGhostHandlePosition}`,
            type: 'workflow',
        };

        edges.push(edgeFromLastTaskToBottomGhost);
    }

    return edges;
}

/**
 * Distributes graph node lane INDEXES into left, middle, and right groups (copied from
 * fork-join's distributeBranches, index-based so callers can look up each lane's tasks). The
 * `+1` mirrors fork-join: it reserves room in the left/middle/right split for the always-present
 * trailing add-node placeholder, which itself is excluded from the distribution and rendered
 * fixed to the right (see `createEdgesForTrailingPlaceholder`).
 *
 * `laneOrder`, when supplied, is the PHYSICAL left-to-right column order the lanes will actually
 * render in (see `orderGraphNodeIndexes`) and overrides the default declaration order used to
 * fill `indexes`. The left/middle/right split still slices the first/middle/last positions of
 * `indexes`, so with `laneOrder` supplied each group ends up holding whichever declared indexes
 * physically sit in that column band — required so the shared top/bottom-ghost handle a group's
 * edges attach to (see `createGraphNodeTaskEdges`) stays on the same side as the columns it
 * actually points at. Without it (dagre, which always lays out lanes in declaration order) the
 * default declaration-order `indexes` keeps the two in sync exactly as before.
 */
function distributeGraphNodeIndexes(
    nodeCount: number,
    laneOrder?: number[]
): {
    leftIndexes: number[];
    middleIndex: number | null;
    rightIndexes: number[];
} {
    const indexes = laneOrder ?? Array.from({length: nodeCount}, (_, index) => index);
    const isEvenCount = (nodeCount + 1) % 2 === 0;

    if (isEvenCount) {
        const halfPoint = (nodeCount + 1) / 2;

        return {
            leftIndexes: indexes.slice(0, halfPoint),
            middleIndex: null,
            rightIndexes: indexes.slice(halfPoint),
        };
    } else {
        const middlePosition = Math.floor((nodeCount + 1) / 2);

        return {
            leftIndexes: indexes.slice(0, middlePosition),
            middleIndex: indexes[middlePosition] ?? null,
            rightIndexes: indexes.slice(middlePosition + 1),
        };
    }
}

/**
 * Resolves the anchor node id for one graph lane's entry point — the SAME node the lane's
 * entry edge (top-ghost -> first task, or top-ghost -> placeholder for an empty lane) already
 * targets, so it exists for both empty and populated lanes without minting any new node. This
 * is also the "lane header" the transition badges (`GraphTransitionBadges`) render on already,
 * making it the natural anchor for the corresponding overlay transition edges.
 */
function graphLaneAnchorNodeId(graphId: string, nodes: Array<GraphNodeType>, nodeIndex: number): string {
    const tasks = nodes[nodeIndex]?.tasks ?? [];

    return tasks.length > 0 ? tasks[0].name : `${graphId}-graph-node-${nodeIndex}-placeholder-0`;
}

/**
 * Derives the `graphTransition` overlay edges for a graph node's declared `next` expressions
 * (`deriveGraphTransitionEdges`) and anchors each one to its source/target lane's entry point.
 * Per the phase-3 plan's architecture decision these are purely a paint-time overlay: they are
 * never fed into layout and never walked by any chain/containment traversal (Task 3 audits and
 * pins that isolation) — this function's only job is producing ReactFlow `Edge` objects for the
 * `graphTransition` edge type to render.
 *
 * `offset` is a running per-kind counter (reset per graph node) so parallel back-edges (or
 * stacked self-loops) bow progressively further out instead of overlapping — see
 * `computeGraphTransitionEdgePath`.
 *
 * `visualPositionByIndex`, when supplied, judges each arc's `forward`/`back` kind against where
 * its lanes actually sit on screen rather than declaration order (see
 * `deriveGraphTransitionEdges`) — safe to thread unconditionally here since transition edges are
 * already ELK-only (`orderLanesByVisualPosition`, see `CreateGraphEdgesOptionsI`), so dagre never
 * calls this function at all.
 */
function createGraphTransitionEdges(
    graphId: string,
    nodes: Array<GraphNodeType>,
    visualPositionByIndex?: Map<number, number>
): Edge[] {
    const transitions = deriveGraphTransitionEdges(nodes, visualPositionByIndex);

    let backEdgeOffset = 0;
    let selfLoopOffset = 0;

    return transitions.map(({kind, sourceIndex, targetIndex}) => {
        const sourceAnchorId = graphLaneAnchorNodeId(graphId, nodes, sourceIndex);
        const targetAnchorId = graphLaneAnchorNodeId(graphId, nodes, targetIndex);

        // The two kinds separate differently. Forward arcs are mostly adjacent hops occupying
        // disjoint horizontal ranges, so a running counter would fan equal-length arcs out to
        // different heights for no reason; span is what actually distinguishes them, and lifting
        // only the arcs that skip lanes keeps every adjacent hop level. Back arcs sweep the full
        // width whatever their span, so span tells us nothing — but several of them do overlap
        // each other, which is what a counter separates. Self loops share one anchor, so they
        // stack too.
        let offset: number;

        if (kind === 'back') {
            offset = backEdgeOffset++;
        } else if (kind === 'self') {
            offset = selfLoopOffset++;
        } else {
            const sourcePosition = visualPositionByIndex?.get(sourceIndex) ?? sourceIndex;
            const targetPosition = visualPositionByIndex?.get(targetIndex) ?? targetIndex;

            offset = Math.max(0, Math.abs(targetPosition - sourcePosition) - 1);
        }

        return {
            data: {
                graphId,
                kind,
                offset,
                sourceIndex,
                targetIndex,
                targetName: nodes[targetIndex]?.name,
            },
            id: `${graphId}-transition-${sourceIndex}-${targetIndex}`,
            markerEnd: {
                color: GRAPH_TRANSITION_EDGE_COLOR,
                type: MarkerType.ArrowClosed,
            },
            source: sourceAnchorId,
            sourceHandle: `${sourceAnchorId}-${GRAPH_TRANSITION_SOURCE_HANDLE_SUFFIX}`,
            target: targetAnchorId,
            targetHandle: `${targetAnchorId}-${GRAPH_TRANSITION_TARGET_HANDLE_SUFFIX}`,
            type: 'graphTransition',
            zIndex: 5,
        };
    });
}

export interface CreateGraphEdgesOptionsI {
    // ELK-only, driven by isElkLayoutActive at the useLayout.tsx call site. A single flag governs
    // both lane-order-sensitive behaviours together: ELK orders lanes by orderGraphNodeIndexes'
    // visual permutation (see elkLayoutUtils' getMemberCaseRank), not declaration order, so the
    // shared top/bottom-ghost handle a lane's edges attach to (left/middle/right, see
    // distributeGraphNodeIndexes) must follow that same permutation — otherwise a lane whose
    // declaration-order group no longer matches its physical column forces its shared-handle edge
    // into a boxy jog across the frame — and the `graphTransition` overlay arcs (see
    // createGraphTransitionEdges) must be classified forward/back against that same visual order,
    // or the arcs describe an ordering the lanes do not actually have. dagre always lays lanes out
    // in declaration order and never renders transition arcs, so it must leave this off and keep
    // the declaration-order grouping with no transition edges.
    //
    // This used to be two independently settable fields (`includeTransitionEdges` and
    // `orderLanesByVisualPosition`); {includeTransitionEdges: true, orderLanesByVisualPosition:
    // false} was a reachable state that classified arcs against the visual order while lanes
    // still rendered in declaration order — exactly the bug the phase-3 revert fixed. Collapsing
    // to one flag (both are always set from the same isElkLayoutActive value at the only call
    // site) makes that state unreachable instead of merely documenting it.
    orderLanesByVisualPosition?: boolean;
}

/**
 * Creates all edges for a graph node and its per-node lanes
 */
export default function createGraphEdges(graphNode: Node, options: CreateGraphEdgesOptionsI = {}): Edge[] {
    const edges: Edge[] = [];
    const nodeData: NodeDataType = graphNode.data as NodeDataType;
    const graphId = graphNode.id;

    const nodes: Array<GraphNodeType> = nodeData.parameters?.nodes ?? [];

    edges.push({
        id: `${graphId}=>${graphId}-graph-top-ghost`,
        source: graphId,
        style: EDGE_STYLES,
        target: `${graphId}-graph-top-ghost`,
        type: 'smoothstep',
    });

    // Computed once when the lane-order-sensitive flag is on: needed both for handle grouping
    // below and for arc-kind classification (see `createGraphTransitionEdges`). dagre never sets
    // the flag, so it never sees a non-identity order.
    const orderedNodeIndexes = options.orderLanesByVisualPosition
        ? orderGraphNodeIndexes(nodes, nodeData.parameters?.startNode as string | undefined)
        : undefined;

    const visualPositionByIndex = orderedNodeIndexes
        ? new Map(orderedNodeIndexes.map((declaredNodeIndex, visualPosition) => [declaredNodeIndex, visualPosition]))
        : undefined;

    const laneVisualOrder = options.orderLanesByVisualPosition ? orderedNodeIndexes : undefined;

    const {leftIndexes, middleIndex, rightIndexes} = distributeGraphNodeIndexes(nodes.length, laneVisualOrder);

    leftIndexes.forEach((nodeIndex) => {
        const tasks = nodes[nodeIndex]?.tasks || [];

        if (tasks.length === 0) {
            edges.push(...createEdgesForEmptyGraphNode(graphId, nodeIndex, 'left'));
        } else {
            edges.push(...createGraphNodeTaskEdges(graphId, tasks, 'left'));
        }
    });

    if (middleIndex !== null) {
        const tasks = nodes[middleIndex]?.tasks || [];

        if (tasks.length === 0) {
            edges.push(...createEdgesForEmptyGraphNode(graphId, middleIndex, 'middle'));
        } else {
            edges.push(...createGraphNodeTaskEdges(graphId, tasks, 'middle'));
        }
    }

    rightIndexes.forEach((nodeIndex) => {
        const tasks = nodes[nodeIndex]?.tasks || [];

        if (tasks.length === 0) {
            edges.push(...createEdgesForEmptyGraphNode(graphId, nodeIndex, 'right'));
        } else {
            edges.push(...createGraphNodeTaskEdges(graphId, tasks, 'right'));
        }
    });

    edges.push(...createEdgesForTrailingPlaceholder(graphId, nodes.length));

    if (options.orderLanesByVisualPosition) {
        edges.push(...createGraphTransitionEdges(graphId, nodes, visualPositionByIndex));
    }

    return edges;
}

/**
 * Determines the target handle side for a task nested inside a graph node, based on that
 * node's lane position — mirrors `getForkJoinBranchSide`.
 *
 * `orderLanesByVisualPosition` is supplied only on the ELK path (see `orderLanesByVisualPosition`
 * on `createGraphEdges`, threaded from `useLayout.tsx`'s `isElkLayoutActive` check). This function
 * runs on the SHARED edge path (`createEdgeFromTaskDispatcherBottomGhostNode`), which both dagre
 * and ELK call, so when the flag is absent the side must stay declaration-index-based — dagre
 * always lays lanes out in declaration order and must never see its handles move.
 */
export function getGraphNodeSide(
    taskDispatcherId: string,
    tasks: WorkflowTask[],
    parentGraphId: string,
    orderLanesByVisualPosition = false
): 'left' | 'right' | 'bottom' {
    const parentGraphTask = tasks?.find((task) => task.name === parentGraphId);

    if (!parentGraphTask) {
        return 'right';
    }

    const nodes = (parentGraphTask.parameters?.nodes || []) as Array<GraphNodeType>;

    const nodeIndex = nodes.findIndex(
        (graphNode) =>
            Array.isArray(graphNode.tasks) && graphNode.tasks.some((subtask) => subtask.name === taskDispatcherId)
    );

    if (nodeIndex === -1) {
        return 'right';
    }

    const totalNodes = nodes.length;

    if (totalNodes === 1) {
        return 'right';
    }

    let lanePosition = nodeIndex;

    if (orderLanesByVisualPosition) {
        const orderedNodeIndexes = orderGraphNodeIndexes(
            nodes,
            parentGraphTask.parameters?.startNode as string | undefined
        );
        const visualPosition = orderedNodeIndexes.indexOf(nodeIndex);

        lanePosition = visualPosition === -1 ? nodeIndex : visualPosition;
    }

    if (lanePosition === 0) {
        return 'left';
    } else if (lanePosition === totalNodes - 1) {
        return 'right';
    } else {
        return 'bottom';
    }
}
