import {GraphTransitionType} from '@/shared/types';

import {isDynamicTransitionTarget} from './graphTransitionMutations';

/**
 * The members that can have run before `memberName` is entered, found by walking `transitions`
 * BACKWARDS from it.
 *
 * A graph is free-form: declaration order says nothing about run order, so "everything declared
 * earlier" — which is what the previous-node-outputs query returns — offers a member the output of a
 * sibling branch that cannot have run on the way to it. The transitions ARE the run order, so they
 * are what decides this.
 *
 * A member on a cycle reaches itself, and is deliberately included: on the second lap round a loop
 * its own earlier output really has been produced.
 *
 * Undefined means the question cannot be answered rather than that nothing precedes: a transition
 * whose target is an expression may land on any member, so no member can be ruled out. Callers
 * filter nothing in that case rather than hiding an output that may genuinely be there.
 */
export function findGraphMembersPrecedingMember(
    memberName: string,
    transitions: GraphTransitionType[]
): Set<string> | undefined {
    if (transitions.some((transition) => isDynamicTransitionTarget(transition.to))) {
        return undefined;
    }

    const precedingMemberNames = new Set<string>();
    const pendingMemberNames = [memberName];

    while (pendingMemberNames.length > 0) {
        const currentMemberName = pendingMemberNames.pop()!;

        for (const transition of transitions) {
            if (transition.to !== currentMemberName || precedingMemberNames.has(transition.from)) {
                continue;
            }

            precedingMemberNames.add(transition.from);
            pendingMemberNames.push(transition.from);
        }
    }

    return precedingMemberNames;
}
