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

                if (nodeData.taskDispatcher && nodeData.taskDispatcherId) {
                    collect(nodeData.taskDispatcherId);
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

        // If this successor is itself a task dispatcher, also collect its descendants
        const targetNodeData = targetNode.data as NodeDataType;

        if (targetNodeData.taskDispatcher && targetNodeData.taskDispatcherId) {
            allNodes.forEach((node) => {
                if (
                    !visited.has(node.id) &&
                    !descendantIds.has(node.id) &&
                    !successorPositions.has(node.id) &&
                    isChildNodeOfDispatcher(node, targetNodeData.taskDispatcherId!)
                ) {
                    successorPositions.set(node.id, {...node.position});
                    visited.add(node.id);
                }
            });
        }

        // Continue following the chain
        const nextTargets = edgesBySource.get(targetId) || [];

        for (const nextTarget of nextTargets) {
            if (!visited.has(nextTarget)) {
                queue.push(nextTarget);
            }
        }
    }

    return successorPositions;
}
