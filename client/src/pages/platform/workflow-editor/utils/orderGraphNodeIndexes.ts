import {GraphNodeType} from '@/shared/types';

import extractNextTargets from './extractNextTargets';

/**
 * Produces the VISUAL lane order for a `graph/v1` dispatcher as a permutation of declaration
 * indexes, so a chain of `next` expressions renders as adjacent left-to-right hops instead of
 * back-arcs over declaration-ordered lanes.
 *
 * Only statically resolvable targets constrain the order (`extractNextTargets`'s `targets`);
 * dangling literals, fully dynamic expressions, and self-loops do not — they stay badge/overlay
 * only, exactly as they are today. A node touched by no static transition in either direction
 * ranks after every connected node, so a graph with no transitions at all yields the identity
 * permutation and renders exactly as before.
 *
 * Ordering is a stable Kahn topological sort. Among nodes that are ready (in-degree zero),
 * `startNode` wins if it is one of them, otherwise the lowest declaration index does. When a cycle
 * leaves nothing ready, the lowest-declaration-index remaining node is emitted and its incoming
 * transitions become back-arcs — one deterministic rule, so the rendering never depends on
 * traversal accidents.
 *
 * `startNode` leads even when it has no static transition of its own (e.g. a router whose `next`
 * is fully dynamic, with nothing statically pointing at it either) — it is seeded into the queue
 * so it is immediately ready, rather than falling through to the untouched-node tail where it
 * would render at the far end of the canvas despite being the graph's entry point. That seeding
 * only happens when at least one OTHER node has a static transition; a graph with no statically
 * resolvable transitions anywhere still yields the plain identity permutation regardless of
 * `startNode`.
 *
 * This is presentation only: callers must keep using declaration indexes for ids.
 */
export default function orderGraphNodeIndexes(nodes: Array<GraphNodeType>, startNode?: string): number[] {
    if (!Array.isArray(nodes) || nodes.length === 0) {
        return [];
    }

    const declaredNodeNames = nodes.map((node) => node.name);
    const nodeIndexByName = new Map(declaredNodeNames.map((name, index) => [name, index]));

    const outgoingIndexes: Array<Set<number>> = nodes.map(() => new Set<number>());
    const incomingCounts: number[] = nodes.map(() => 0);
    const hasStaticTransition: boolean[] = nodes.map(() => false);

    nodes.forEach((node, sourceIndex) => {
        const {targets} = extractNextTargets(node.next, declaredNodeNames);

        targets.forEach((target) => {
            const targetIndex = nodeIndexByName.get(target);

            // A self-loop constrains nothing, and a duplicate target would double-count in-degree
            if (targetIndex === undefined || targetIndex === sourceIndex) {
                return;
            }

            if (outgoingIndexes[sourceIndex].has(targetIndex)) {
                return;
            }

            outgoingIndexes[sourceIndex].add(targetIndex);

            incomingCounts[targetIndex] += 1;
            hasStaticTransition[sourceIndex] = true;
            hasStaticTransition[targetIndex] = true;
        });
    });

    const startNodeIndex = startNode === undefined ? undefined : nodeIndexByName.get(startNode);

    const remainingIndexes = new Set<number>();

    nodes.forEach((_, index) => {
        if (hasStaticTransition[index]) {
            remainingIndexes.add(index);
        }
    });

    // A startNode whose `next` is fully dynamic (a router with no literal branches), and that
    // nothing statically points at either, has `hasStaticTransition === false` and would
    // otherwise never enter the queue below — it would never be "ready" and would land last
    // among the trailing no-transition group, even though it is the graph's entry point. Seed it
    // in explicitly so the ready-node tie-break a few lines down (which already prefers
    // startNodeIndex) can let it lead. Gated on the queue being non-empty: when NO node in the
    // graph has a static transition at all, seeding would place startNodeIndex alone via the
    // topological loop and the identity permutation would be broken for every other node — a
    // graph with no statically resolvable transitions must render in declaration order
    // regardless of which node is configured as startNode.
    if (startNodeIndex !== undefined && remainingIndexes.size > 0) {
        remainingIndexes.add(startNodeIndex);
    }

    const orderedIndexes: number[] = [];

    while (remainingIndexes.size > 0) {
        const readyIndexes = Array.from(remainingIndexes).filter((index) => incomingCounts[index] === 0);

        let nextIndex: number;

        if (readyIndexes.length > 0) {
            nextIndex =
                startNodeIndex !== undefined && readyIndexes.includes(startNodeIndex)
                    ? startNodeIndex
                    : Math.min(...readyIndexes);
        } else {
            nextIndex = Math.min(...Array.from(remainingIndexes));
        }

        orderedIndexes.push(nextIndex);

        remainingIndexes.delete(nextIndex);

        outgoingIndexes[nextIndex].forEach((targetIndex) => {
            if (remainingIndexes.has(targetIndex)) {
                incomingCounts[targetIndex] -= 1;
            }
        });
    }

    // startNodeIndex may have been seeded above despite `hasStaticTransition[startNodeIndex]`
    // being false, so it can already be in `orderedIndexes` — skip already-placed indexes here or
    // it would be emitted twice.
    const placedIndexes = new Set(orderedIndexes);

    nodes.forEach((_, index) => {
        if (!hasStaticTransition[index] && !placedIndexes.has(index)) {
            orderedIndexes.push(index);
        }
    });

    return orderedIndexes;
}
