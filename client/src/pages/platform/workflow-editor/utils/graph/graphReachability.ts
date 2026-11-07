import {GraphTransitionType} from '@/shared/types';

import {isDynamicTransitionTarget} from './graphTransitionMutations';

export function findGraphMembersPrecedingMember(
    memberName: string,
    transitions: GraphTransitionType[],
    memberNames: string[]
): Set<string> {
    if (transitions.some((transition) => isDynamicTransitionTarget(transition.to))) {
        return new Set(memberNames);
    }

    const declaredMemberNames = new Set(memberNames);
    const precedingMemberNames = new Set<string>();
    const pendingMemberNames = [memberName];

    while (pendingMemberNames.length > 0) {
        const currentMemberName = pendingMemberNames.pop()!;

        for (const transition of transitions) {
            if (
                transition.to !== currentMemberName ||
                !declaredMemberNames.has(transition.from) ||
                precedingMemberNames.has(transition.from)
            ) {
                continue;
            }

            precedingMemberNames.add(transition.from);
            pendingMemberNames.push(transition.from);
        }
    }

    return precedingMemberNames;
}
