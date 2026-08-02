import {GraphNodeType} from '@/shared/types';

import extractNextTargets from './extractNextTargets';

export type GraphTransitionEdgeKindType = 'back' | 'forward' | 'self';

export interface GraphTransitionEdgeI {
    kind: GraphTransitionEdgeKindType;
    sourceIndex: number;
    targetIndex: number;
}

/**
 * Derives the transition edge model for a `graph/v1` container from its declared nodes' `next`
 * expressions — one entry per (source node, statically-resolvable target), pure and
 * side-effect-free so it can be unit-tested independently of any rendering code (the phase-3
 * overlay-edge renderer built on top of this, see the plan's architecture decision, consumes
 * this array directly).
 *
 * Only `extractNextTargets`'s `targets` (result-position literals that resolve to a currently
 * declared node) produce an edge. `dangling` literals and fully `dynamic` expressions with no
 * literal targets produce none — both remain badge-only (`GraphTransitionBadges`), since neither
 * has a resolvable node to anchor a rendered edge to.
 *
 * `kind` is derived purely from position comparison between source and target lanes: `'self'`
 * when a node targets itself, `'forward'` when the target lane comes after the source lane in
 * visual lane position, `'back'` otherwise (a cycle — the target lane comes before or the graph
 * loops back around). When `visualPositionByIndex` is omitted, visual position falls back to
 * declaration order.
 */
export default function deriveGraphTransitionEdges(
    nodes: Array<GraphNodeType>,
    visualPositionByIndex?: Map<number, number>
): Array<GraphTransitionEdgeI> {
    const declaredNodeNames = nodes.map((node) => node.name);
    const nodeIndexByName = new Map(nodes.map((node, index) => [node.name, index]));

    const edges: Array<GraphTransitionEdgeI> = [];

    nodes.forEach((node, sourceIndex) => {
        const {targets} = extractNextTargets(node.next, declaredNodeNames);

        targets.forEach((target) => {
            const targetIndex = nodeIndexByName.get(target);

            if (targetIndex === undefined) {
                return;
            }

            // sourceIndex/targetIndex stay DECLARATION indexes (ids and lane lookups depend on
            // them); only the arc's kind is judged against where the lanes actually sit.
            const sourcePosition = visualPositionByIndex?.get(sourceIndex) ?? sourceIndex;
            const targetPosition = visualPositionByIndex?.get(targetIndex) ?? targetIndex;

            edges.push({
                kind: deriveTransitionKind(sourcePosition, targetPosition),
                sourceIndex,
                targetIndex,
            });
        });
    });

    return edges;
}

function deriveTransitionKind(sourceIndex: number, targetIndex: number): GraphTransitionEdgeKindType {
    if (sourceIndex === targetIndex) {
        return 'self';
    }

    return targetIndex > sourceIndex ? 'forward' : 'back';
}
