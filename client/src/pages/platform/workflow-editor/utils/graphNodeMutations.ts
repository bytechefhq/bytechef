import {GraphNodeType} from '@/shared/types';

/**
 * Generates a unique `node_<n>` name for a newly created graph node, skipping any name
 * already present in `reservedNodeNames` (and reserving the returned name for callers
 * that need to generate several names in one pass).
 */
export function getNextAvailableGraphNodeName(reservedNodeNames: Set<string>): string {
    let counter = 0;
    let candidateName = `node_${counter}`;

    while (reservedNodeNames.has(candidateName)) {
        counter += 1;
        candidateName = `node_${counter}`;
    }

    reservedNodeNames.add(candidateName);

    return candidateName;
}

export interface GraphNodeNameValidationResultI {
    error?: string;
    valid: boolean;
}

/**
 * Validates a candidate node name against the graph's other declared node names: non-empty,
 * and unique among every OTHER node (the node currently being renamed is excluded from the
 * uniqueness check, so saving an unchanged name is always valid).
 */
export function validateGraphNodeName(
    nodes: Array<GraphNodeType>,
    nodeIndex: number,
    candidateName: string
): GraphNodeNameValidationResultI {
    const trimmedName = candidateName.trim();

    if (!trimmedName) {
        return {error: 'Node name cannot be empty.', valid: false};
    }

    if (trimmedName === 'vars') {
        return {error: '"vars" is a reserved name.', valid: false};
    }

    const isDuplicate = nodes.some((graphNode, index) => index !== nodeIndex && graphNode.name === trimmedName);

    if (isDuplicate) {
        return {error: `A node named "${trimmedName}" already exists.`, valid: false};
    }

    return {valid: true};
}

/**
 * Renames `nodes[nodeIndex]` in place (returning a new array). Per the phase-2 binding
 * decision, this does NOT rewrite any other node's `next` expression — a `next` literal that
 * referenced the old name becomes a dangling target, surfaced by the transition-badge warning
 * (see `extractNextTargets`) rather than being silently repaired.
 */
export function renameGraphNode(nodes: Array<GraphNodeType>, nodeIndex: number, newName: string): Array<GraphNodeType> {
    if (nodeIndex < 0 || nodeIndex >= nodes.length) {
        return nodes;
    }

    return nodes.map((graphNode, index) => (index === nodeIndex ? {...graphNode, name: newName.trim()} : graphNode));
}

/**
 * Appends a new, uniquely-named empty node (`{name, tasks: []}`) to the graph. An empty node
 * is a legal, addressable lane — it may be a pure router driven only by its `next` expression.
 */
export function addGraphNode(nodes: Array<GraphNodeType>): Array<GraphNodeType> {
    const reservedNodeNames = new Set(nodes.map((graphNode) => graphNode.name));

    const newNodeName = getNextAvailableGraphNodeName(reservedNodeNames);

    return [...nodes, {name: newNodeName, tasks: []}];
}

/**
 * Removes `nodes[nodeIndex]`. Legal regardless of whether the node currently holds tasks —
 * unlike branch (which always keeps at least one case), a graph may shrink to zero nodes.
 * Removing a node shifts every later node's index down by one; that's safe because the whole
 * `nodes` array is written back atomically in one save, so no separate per-lane re-addressing
 * is needed.
 */
export function deleteGraphNode(nodes: Array<GraphNodeType>, nodeIndex: number): Array<GraphNodeType> {
    return nodes.filter((_, index) => index !== nodeIndex);
}
