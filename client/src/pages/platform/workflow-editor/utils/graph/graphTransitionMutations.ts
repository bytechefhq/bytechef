import {GraphTransitionType} from '@/shared/types';

export function isDynamicTransitionTarget(to: string | null | undefined): boolean {
    const trimmedTarget = to?.trim();

    return !!trimmedTarget && (trimmedTarget.startsWith('=') || trimmedTarget.includes('${'));
}

const FORMULA_PREFIX = '=';

export function isUnconditional(transition: GraphTransitionType): boolean {
    const trimmedCondition = transition.condition?.trim() ?? '';

    return trimmedCondition === '' || trimmedCondition === FORMULA_PREFIX;
}

interface AddTransitionProps {
    from: string;
    to: string;
    transitions: GraphTransitionType[];
}

export function addTransition({from, to, transitions}: AddTransitionProps): GraphTransitionType[] {
    const pairAlreadyExists = transitions.some((transition) => transition.from === from && transition.to === to);

    if (pairAlreadyExists) {
        return transitions;
    }

    return [...transitions, {from, to}];
}

export function isSameTransition(
    firstTransition: GraphTransitionType | undefined,
    secondTransition: GraphTransitionType | undefined
): boolean {
    if (!firstTransition || !secondTransition) {
        return false;
    }

    return (
        firstTransition.from === secondTransition.from &&
        firstTransition.to === secondTransition.to &&
        (firstTransition.condition || '') === (secondTransition.condition || '')
    );
}

interface RemoveTransitionProps {
    expectedTransition: GraphTransitionType;
    index: number;
    transitions: GraphTransitionType[];
}

export function removeTransition({
    expectedTransition,
    index,
    transitions,
}: RemoveTransitionProps): GraphTransitionType[] {
    if (!isSameTransition(transitions[index], expectedTransition)) {
        return transitions;
    }

    return transitions.filter((_transition, transitionIndex) => transitionIndex !== index);
}

interface UpdateTransitionProps {
    expectedTransition: GraphTransitionType;
    index: number;
    patch: Partial<GraphTransitionType>;
    transitions: GraphTransitionType[];
}

export function updateTransition({
    expectedTransition,
    index,
    patch,
    transitions,
}: UpdateTransitionProps): GraphTransitionType[] {
    if (!isSameTransition(transitions[index], expectedTransition)) {
        return transitions;
    }

    return transitions.map((transition, transitionIndex) =>
        transitionIndex === index ? {...transition, ...patch} : transition
    );
}

export type GraphTransitionFieldType = 'condition' | 'to';

interface UpdateTransitionFieldProps {
    expectedTransition: GraphTransitionType;
    field: GraphTransitionFieldType;
    index: number;
    knownFieldValues: Array<string | undefined>;
    transitions: GraphTransitionType[];
    value: string | undefined;
}

export function updateTransitionField({
    expectedTransition,
    field,
    index,
    knownFieldValues,
    transitions,
    value,
}: UpdateTransitionFieldProps): GraphTransitionType[] {
    const currentTransition = transitions[index];

    if (!currentTransition) {
        return transitions;
    }

    const otherFieldsMatch = isSameTransition(
        {...currentTransition, [field]: expectedTransition[field]},
        expectedTransition
    );

    const currentFieldValue = currentTransition[field] || '';

    const fieldValueKnown = [expectedTransition[field], ...knownFieldValues].some(
        (knownFieldValue) => (knownFieldValue || '') === currentFieldValue
    );

    if (!otherFieldsMatch || !fieldValueKnown || currentFieldValue === (value || '')) {
        return transitions;
    }

    return transitions.map((transition, transitionIndex) =>
        transitionIndex === index ? {...transition, [field]: value} : transition
    );
}

interface MoveTransitionProps {
    direction: -1 | 1;
    expectedTransition: GraphTransitionType;
    index: number;
    transitions: GraphTransitionType[];
}

export function moveTransition({
    direction,
    expectedTransition,
    index,
    transitions,
}: MoveTransitionProps): GraphTransitionType[] {
    const movedTransition = transitions[index];

    if (!isSameTransition(movedTransition, expectedTransition)) {
        return transitions;
    }

    const movedUnconditional = isUnconditional(movedTransition);

    const groupIndexes = transitions.reduce<number[]>((indexes, transition, transitionIndex) => {
        if (transition.from === movedTransition.from && isUnconditional(transition) === movedUnconditional) {
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

        const unconditionalCount = (unconditionalCountByNodeName.get(transition.from) ?? 0) + 1;

        unconditionalCountByNodeName.set(transition.from, unconditionalCount);
    }

    return nodeNameOrder.filter((nodeName) => (unconditionalCountByNodeName.get(nodeName) ?? 0) > 1);
}
