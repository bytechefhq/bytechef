import {GraphTransitionType} from '@/shared/types';

/**
 * A transition's `to` is dynamic (LLM/expression-routed) rather than a literal node name when it
 * starts with `=` or contains a `${...}` datapill reference.
 *
 * An absent target answers false rather than throwing. `to` is required in the definition, but the
 * editor writes it through the shared property editor, which holds it empty between a field being
 * cleared and the next value being committed — and this predicate runs on every layout pass, so a
 * transition passing through that state would otherwise take the whole canvas down with it.
 */
export function isDynamicTransitionTarget(to: string | null | undefined): boolean {
    return !!to && (to.startsWith('=') || to.includes('${'));
}

/**
 * A transition is unconditional when its `condition` is absent or blank. Per node, declared
 * order among outgoing transitions is conditional priority, and the runtime takes the first
 * declared unconditional transition when more than one exists (see findNodesWithDuplicateDefault).
 */
export function isUnconditional(transition: GraphTransitionType): boolean {
    return !transition.condition || transition.condition.trim() === '';
}

/**
 * Appends a `{from, to}` transition. No-op (returns the SAME array instance) when that exact
 * from/to pair already exists, so callers can skip a save on a redundant connect.
 */
export function addTransition(transitions: GraphTransitionType[], from: string, to: string): GraphTransitionType[] {
    const pairAlreadyExists = transitions.some((transition) => transition.from === from && transition.to === to);

    if (pairAlreadyExists) {
        return transitions;
    }

    return [...transitions, {from, to}];
}

export function removeTransition(transitions: GraphTransitionType[], index: number): GraphTransitionType[] {
    return transitions.filter((_transition, transitionIndex) => transitionIndex !== index);
}

export function updateTransition(
    transitions: GraphTransitionType[],
    index: number,
    patch: Partial<GraphTransitionType>
): GraphTransitionType[] {
    return transitions.map((transition, transitionIndex) =>
        transitionIndex === index ? {...transition, ...patch} : transition
    );
}

/**
 * Reorders a transition within its `from` group only (declared order among a node's outgoing
 * transitions is conditional priority). No-op (returns the SAME array instance) when the index
 * is out of range or the move would fall outside the group's bounds.
 */
export function moveTransition(
    transitions: GraphTransitionType[],
    index: number,
    direction: -1 | 1
): GraphTransitionType[] {
    const movedTransition = transitions[index];

    if (!movedTransition) {
        return transitions;
    }

    const groupIndexes = transitions.reduce<number[]>((indexes, transition, transitionIndex) => {
        if (transition.from === movedTransition.from) {
            indexes.push(transitionIndex);
        }

        return indexes;
    }, []);

    const positionInGroup = groupIndexes.indexOf(index);
    const targetPositionInGroup = positionInGroup + direction;

    if (targetPositionInGroup < 0 || targetPositionInGroup >= groupIndexes.length) {
        return transitions;
    }

    const targetIndex = groupIndexes[targetPositionInGroup];
    const reorderedTransitions = [...transitions];

    reorderedTransitions[index] = transitions[targetIndex];
    reorderedTransitions[targetIndex] = transitions[index];

    return reorderedTransitions;
}

/**
 * Drops every transition touching `nodeName`, in either direction — used when a member node is
 * deleted so it leaves no dangling references.
 */
export function removeTransitionsForNode(transitions: GraphTransitionType[], nodeName: string): GraphTransitionType[] {
    return transitions.filter((transition) => transition.from !== nodeName && transition.to !== nodeName);
}

export function transitionsFrom(
    transitions: GraphTransitionType[],
    nodeName: string
): Array<{index: number; transition: GraphTransitionType}> {
    return transitions.reduce<Array<{index: number; transition: GraphTransitionType}>>(
        (matches, transition, transitionIndex) => {
            if (transition.from === nodeName) {
                matches.push({index: transitionIndex, transition});
            }

            return matches;
        },
        []
    );
}

/**
 * Returns the names of nodes with more than one unconditional outgoing transition — a warning
 * case, since the runtime takes only the first declared unconditional transition.
 */
export function findNodesWithDuplicateDefault(transitions: GraphTransitionType[]): string[] {
    const unconditionalCountByNodeName = new Map<string, number>();
    const nodeNameOrder: string[] = [];

    for (const transition of transitions) {
        if (!isUnconditional(transition)) {
            continue;
        }

        if (!unconditionalCountByNodeName.has(transition.from)) {
            nodeNameOrder.push(transition.from);
        }

        unconditionalCountByNodeName.set(transition.from, (unconditionalCountByNodeName.get(transition.from) ?? 0) + 1);
    }

    return nodeNameOrder.filter((nodeName) => (unconditionalCountByNodeName.get(nodeName) ?? 0) > 1);
}
