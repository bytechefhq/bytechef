import {FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node, XYPosition} from '@xyflow/react';

/**
 * Checks whether a node is a direct child of a given task dispatcher.
 * Matches against all supported nesting data properties.
 */
export function isChildNodeOfDispatcher(node: Node, dispatcherId: string): boolean {
    const nodeData = node.data as NodeDataType;

    return (
        node.id !== dispatcherId &&
        (nodeData.taskDispatcherId === dispatcherId ||
            nodeData.conditionData?.conditionId === dispatcherId ||
            nodeData.loopData?.loopId === dispatcherId ||
            nodeData.mapData?.mapId === dispatcherId ||
            nodeData.branchData?.branchId === dispatcherId ||
            nodeData.eachData?.eachId === dispatcherId ||
            nodeData.parallelData?.parallelId === dispatcherId ||
            nodeData.forkJoinData?.forkJoinId === dispatcherId ||
            nodeData.onErrorData?.onErrorId === dispatcherId)
    );
}

/**
 * Recursively collects all descendant nodes of a task dispatcher,
 * including children of nested dispatchers.
 */
export function collectAllDescendantNodes(dispatcherId: string, allNodes: Node[]): Map<string, XYPosition> {
    const collected = new Set<string>();
    const startPositions = new Map<string, XYPosition>();

    const collect = (currentDispatcherId: string) => {
        allNodes.forEach((node) => {
            if (collected.has(node.id)) {
                return;
            }

            if (isChildNodeOfDispatcher(node, currentDispatcherId)) {
                collected.add(node.id);
                startPositions.set(node.id, {...node.position});

                const nodeData = node.data as NodeDataType;

                // Use node.id (the dispatcher's own ID) instead of
                // nodeData.taskDispatcherId, which may be overwritten to the
                // parent dispatcher's ID by buildGenericNodeData.
                if (nodeData.taskDispatcher) {
                    collect(node.id);
                }
            }
        });
    };

    collect(dispatcherId);

    return startPositions;
}

/**
 * Collects chain successor nodes — nodes that follow the dispatcher's
 * bottom ghost in the main workflow flow. These are top-level nodes
 * connected via edges from the bottom ghost, continuing until the
 * final placeholder is reached.
 */
export function collectChainSuccessorNodes(
    dispatcherId: string,
    allNodes: Node[],
    allEdges: Edge[],
    descendantIds: Set<string>
): Map<string, XYPosition> {
    const successorPositions = new Map<string, XYPosition>();
    const nodeMap = new Map(allNodes.map((node) => [node.id, node]));

    // Build edge lookup: source → target IDs
    const edgesBySource = new Map<string, string[]>();

    for (const edge of allEdges) {
        if (!edgesBySource.has(edge.source)) {
            edgesBySource.set(edge.source, []);
        }

        edgesBySource.get(edge.source)!.push(edge.target);
    }

    // Find the bottom ghost node for this dispatcher
    const bottomGhostId = [...descendantIds].find((nodeId) => nodeId.includes('bottom-ghost'));

    if (!bottomGhostId) {
        return successorPositions;
    }

    // Walk the chain from the bottom ghost's outgoing edges
    const visited = new Set<string>();
    const queue = edgesBySource.get(bottomGhostId) || [];

    while (queue.length > 0) {
        const targetId = queue.shift()!;

        if (visited.has(targetId) || targetId === FINAL_PLACEHOLDER_NODE_ID || !nodeMap.has(targetId)) {
            continue;
        }

        // Stop at placeholder nodes (final trailing placeholder)
        const targetNodeType = nodeMap.get(targetId)?.type;

        if (targetNodeType === 'placeholder' && !nodeMap.get(targetId)?.data?.taskDispatcherId) {
            continue;
        }

        visited.add(targetId);

        // Skip nodes that are already collected as descendants,
        // but still follow their outgoing edges to find successors beyond them
        if (descendantIds.has(targetId)) {
            const nextTargets = edgesBySource.get(targetId) || [];

            for (const nextTarget of nextTargets) {
                if (!visited.has(nextTarget)) {
                    queue.push(nextTarget);
                }
            }

            continue;
        }

        const targetNode = nodeMap.get(targetId);

        if (!targetNode) {
            continue;
        }

        successorPositions.set(targetId, {...targetNode.position});

        // If this successor is itself a task dispatcher, recursively collect
        // all its descendants (ghosts, children, and nested dispatchers).
        const targetNodeData = targetNode.data as NodeDataType;

        if (targetNodeData.taskDispatcher) {
            const nestedDescendants = collectAllDescendantNodes(targetId, allNodes);

            nestedDescendants.forEach((position, nodeId) => {
                if (!visited.has(nodeId) && !descendantIds.has(nodeId) && !successorPositions.has(nodeId)) {
                    successorPositions.set(nodeId, position);
                    visited.add(nodeId);
                }
            });

            // Continue the chain from the dispatcher's bottom ghost since
            // descendants were added to visited and the normal edge walk
            // from targetId would stop at the top ghost.
            const nestedBottomGhostId = [...nestedDescendants.keys()].find(
                (nodeId) => nodeId.startsWith(`${targetId}-`) && nodeId.includes('bottom-ghost')
            );

            if (nestedBottomGhostId) {
                const bottomGhostTargets = edgesBySource.get(nestedBottomGhostId) || [];

                for (const nextTarget of bottomGhostTargets) {
                    if (!visited.has(nextTarget)) {
                        queue.push(nextTarget);
                    }
                }
            }
        }

        // Continue following the chain from the current node
        const nextTargets = edgesBySource.get(targetId) || [];

        for (const nextTarget of nextTargets) {
            if (!visited.has(nextTarget)) {
                queue.push(nextTarget);
            }
        }
    }

    return successorPositions;
}
