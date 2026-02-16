import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    LayoutDirectionType,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
} from '@/shared/constants';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const containsNodePosition = (metadata: any): metadata is {ui: {nodePosition: {x: number; y: number}}} =>
    metadata?.ui?.nodePosition !== undefined;

/**
 * Returns the parent dispatcher ID for a node that is a child of a dispatcher.
 * Ghost/placeholder nodes use `taskDispatcherId` directly, while workflow child
 * nodes use dispatcher-specific data properties (conditionData, loopData, etc.).
 */
function getParentDispatcherId(nodeData: NodeDataType): string | undefined {
    // Use nesting-data properties first to find the parent dispatcher.
    // taskDispatcherId is only a fallback for ghost/placeholder nodes that
    // lack nesting data. For task dispatchers, taskDispatcherId is the
    // node's OWN ID — not the parent's — so it must come last.
    return (
        nodeData.conditionData?.conditionId ||
        nodeData.loopData?.loopId ||
        nodeData.branchData?.branchId ||
        nodeData.parallelData?.parallelId ||
        nodeData.eachData?.eachId ||
        nodeData.forkJoinData?.forkJoinId ||
        nodeData.taskDispatcherId
    );
}

/**
 * Returns the cross-axis centering offset for cluster-root nodes (AI Agent
 * with configured cluster elements). These nodes are wider than regular
 * task nodes, so dagre-to-ReactFlow conversion applies a centering offset
 * (-85 for TB, -23 for LR). This offset must be preserved when chain-
 * aligning nodes of different widths.
 */
function getClusterRootCrossOffset(node: Node, direction: LayoutDirectionType): number {
    const nodeData = node.data as NodeDataType;
    const hasClusterElements =
        nodeData.clusterElements &&
        Object.entries(nodeData.clusterElements).some(
            ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
        );

    if (!hasClusterElements || !nodeData.clusterRoot) {
        return 0;
    }

    return direction === 'TB' ? -85 : -23;
}

/**
 * Recursively collects node IDs belonging to a nested task dispatcher
 * (ghost nodes, placeholders, children, and any further nested dispatchers).
 */
function collectNestedDispatcherNodes(dispatcherId: string, allNodes: Node[], collected: Set<string>): void {
    const newNodeIds: string[] = [];

    allNodes.forEach((node) => {
        if (collected.has(node.id)) {
            return;
        }

        const data = node.data as NodeDataType;

        const belongsToDispatcher =
            data.taskDispatcherId === dispatcherId ||
            data.conditionData?.conditionId === dispatcherId ||
            data.loopData?.loopId === dispatcherId ||
            data.branchData?.branchId === dispatcherId ||
            data.eachData?.eachId === dispatcherId ||
            data.parallelData?.parallelId === dispatcherId ||
            data.forkJoinData?.forkJoinId === dispatcherId;

        if (belongsToDispatcher) {
            collected.add(node.id);
            newNodeIds.push(node.id);
        }
    });

    newNodeIds.forEach((nodeId) => {
        const node = allNodes.find((existingNode) => existingNode.id === nodeId);

        if (!node) {
            return;
        }

        const data = node.data as NodeDataType;

        if (data.taskDispatcher && data.taskDispatcherId && data.taskDispatcherId !== dispatcherId) {
            collectNestedDispatcherNodes(data.taskDispatcherId, allNodes, collected);
        }
    });
}

/**
 * Constrains condition ghost nodes (top and bottom) so their cross-axis
 * aligns with the condition node, preventing the condition frame from
 * growing with child width.
 */
export function constrainConditionGhostsCrossAxis(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((ghostNode) => {
        const ghostData = ghostNode.data as NodeDataType;

        if (!ghostData.conditionId) {
            return;
        }

        const isTopGhost = ghostNode.type === 'taskDispatcherTopGhostNode';
        const isBottomGhost = ghostNode.type === 'taskDispatcherBottomGhostNode';

        if (!isTopGhost && !isBottomGhost) {
            return;
        }

        const conditionNode = allNodes.find((node) => node.id === ghostData.conditionId);

        if (!conditionNode) {
            return;
        }

        ghostNode.position = {
            ...ghostNode.position,
            [crossAxis]: conditionNode.position[crossAxis],
        };
    });
}

/**
 * Constrains branch ghost nodes (top and bottom) so their cross-axis
 * aligns with the branch node.
 */
export function constrainBranchGhostsCrossAxis(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((ghostNode) => {
        const ghostData = ghostNode.data as NodeDataType;

        if (!ghostData.branchId) {
            return;
        }

        const isTopGhost = ghostNode.type === 'taskDispatcherTopGhostNode';
        const isBottomGhost = ghostNode.type === 'taskDispatcherBottomGhostNode';

        if (!isTopGhost && !isBottomGhost) {
            return;
        }

        const branchNode = allNodes.find((node) => node.id === ghostData.branchId);

        if (!branchNode) {
            return;
        }

        ghostNode.position = {
            ...ghostNode.position,
            [crossAxis]: branchNode.position[crossAxis],
        };
    });
}

/**
 * Aligns branch case children with the branch center so edges are straight.
 *
 * Middle-case children (sourceHandle ending in '-bottom') are always aligned.
 * Side-case children (sourceHandle ending in '-left' or '-right') are only
 * aligned when dagre placed them close to the branch center, preventing
 * near-horizontal edges that look misaligned without disturbing intentionally
 * fanned-out outer cases.
 */
export function alignBranchCaseChildren(
    allNodes: Node[],
    edges: Edge[],
    crossAxis: 'x' | 'y',
    crossAxisSize: number
): void {
    edges.forEach((edge) => {
        if (!edge.sourceHandle) {
            return;
        }

        const isMiddleCase = edge.sourceHandle.endsWith('-bottom');
        const isSideCase = edge.sourceHandle.endsWith('-left') || edge.sourceHandle.endsWith('-right');

        if (!isMiddleCase && !isSideCase) {
            return;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode || sourceNode.type !== 'taskDispatcherTopGhostNode') {
            return;
        }

        const sourceData = sourceNode.data as NodeDataType;

        if (!sourceData.branchId) {
            return;
        }

        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || targetNode.type === 'placeholder') {
            return;
        }

        const branchNode = allNodes.find((node) => node.id === sourceData.branchId);
        const branchCenterCross = branchNode
            ? branchNode.position[crossAxis] + crossAxisSize / 2
            : sourceNode.position[crossAxis] + CLUSTER_ELEMENT_NODE_WIDTH / 2;

        const targetCross = branchCenterCross - crossAxisSize / 2;

        if (isMiddleCase) {
            targetNode.position = {
                ...targetNode.position,
                [crossAxis]: targetCross,
            };

            // Walk the chain from the first child and align subsequent nodes
            alignCaseChainNodes(allNodes, edges, targetNode, targetCross, crossAxis);
        } else {
            const distance = Math.abs(targetNode.position[crossAxis] - targetCross);

            if (distance <= CLUSTER_ELEMENT_NODE_WIDTH) {
                targetNode.position = {
                    ...targetNode.position,
                    [crossAxis]: targetCross,
                };

                alignCaseChainNodes(allNodes, edges, targetNode, targetCross, crossAxis);
            }
        }
    });
}

/**
 * Walks a chain from a starting node and aligns each subsequent workflow node
 * to the same cross-axis position. For task dispatcher nodes, shifts all
 * descendants by the same delta and jumps to the bottom ghost's target.
 */
function alignCaseChainNodes(
    allNodes: Node[],
    edges: Edge[],
    startNode: Node,
    targetCross: number,
    crossAxis: 'x' | 'y'
): void {
    let currentNodeId = startNode.id;
    let currentData = startNode.data as NodeDataType;

    while (currentNodeId) {
        let nextNodeId = '';

        if (currentData?.taskDispatcher && currentData.taskDispatcherId) {
            const componentName = currentData.componentName as string;
            const bottomGhostId = `${currentData.taskDispatcherId}-${componentName}-bottom-ghost`;
            const bottomGhostEdge = edges.find((bottomEdge) => bottomEdge.source === bottomGhostId);

            nextNodeId = bottomGhostEdge?.target || '';
        } else {
            const nextEdge = edges.find((nextEdge) => nextEdge.source === currentNodeId);

            nextNodeId = nextEdge?.target || '';
        }

        if (!nextNodeId) {
            break;
        }

        const nextNode = allNodes.find((node) => node.id === nextNodeId);

        if (!nextNode || nextNode.type === 'taskDispatcherBottomGhostNode') {
            break;
        }

        const nextData = nextNode.data as NodeDataType;

        if (containsNodePosition(nextData.metadata)) {
            currentNodeId = nextNodeId;
            currentData = nextData;

            continue;
        }

        const shiftCross = targetCross - nextNode.position[crossAxis];

        if (Math.abs(shiftCross) > 0.5) {
            if (nextData.taskDispatcher && nextData.taskDispatcherId) {
                nextNode.position = {...nextNode.position, [crossAxis]: targetCross};

                const descendantIds = new Set<string>();

                collectNestedDispatcherNodes(nextData.taskDispatcherId, allNodes, descendantIds);

                allNodes.forEach((descendantNode) => {
                    if (descendantIds.has(descendantNode.id) && descendantNode.id !== nextNodeId) {
                        descendantNode.position = {
                            ...descendantNode.position,
                            [crossAxis]: descendantNode.position[crossAxis] + shiftCross,
                        };
                    }
                });
            } else {
                nextNode.position = {...nextNode.position, [crossAxis]: targetCross};
            }
        }

        currentNodeId = nextNodeId;
        currentData = nextData;
    }
}

/**
 * Aligns loop/each/parallel/fork-join ghost nodes (top and bottom) to their dispatcher node.
 * Skips condition and branch ghosts which are handled by earlier constraints.
 */
export function alignDispatcherGhostsCrossAxis(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((ghostNode) => {
        if (ghostNode.type !== 'taskDispatcherTopGhostNode' && ghostNode.type !== 'taskDispatcherBottomGhostNode') {
            return;
        }

        const ghostData = ghostNode.data as NodeDataType;

        // Skip condition and branch ghosts (already handled by earlier constraints)
        if (ghostData.conditionId || ghostData.branchId) {
            return;
        }

        const dispatcherId = ghostData.taskDispatcherId;

        if (!dispatcherId) {
            return;
        }

        const dispatcherNode = allNodes.find((node) => node.id === dispatcherId);

        if (!dispatcherNode) {
            return;
        }

        ghostNode.position = {...ghostNode.position, [crossAxis]: dispatcherNode.position[crossAxis]};
    });
}

/**
 * Separates overlapping frames for nested condition task-dispatcher children.
 *
 * When a condition inside another condition's branch has a task-dispatcher
 * child, the child often lands at the same cross-axis level as the grandparent
 * condition (because conditionCaseOffset is symmetric and cancels out across
 * nesting levels). This creates overlapping frame boundaries and merged edge
 * paths.
 *
 * This function detects such overlaps and pulls the child (plus all its
 * descendants) closer to the parent condition, placing it at the midpoint
 * between the parent and grandparent conditions.
 */
export function separateOverlappingConditionChildren(
    allNodes: Node[],
    edges: Edge[],
    crossAxis: 'x' | 'y'
): void {
    allNodes.forEach((conditionNode) => {
        const conditionData = conditionNode.data as NodeDataType;

        if (conditionData.componentName !== 'condition') {
            return;
        }

        // Only process conditions nested inside another condition
        const parentConditionId = conditionData.conditionData?.conditionId;

        if (!parentConditionId) {
            return;
        }

        const parentCondition = allNodes.find((node) => node.id === parentConditionId);

        if (!parentCondition) {
            return;
        }

        const conditionId = conditionNode.id;
        const topGhostId = `${conditionId}-condition-top-ghost`;
        const conditionCross = conditionNode.position[crossAxis];
        const parentCross = parentCondition.position[crossAxis];
        const conditionToParent = parentCross - conditionCross;

        for (const edge of edges) {
            if (edge.source !== topGhostId || !edge.sourceHandle) {
                continue;
            }

            const childNode = allNodes.find((node) => node.id === edge.target);

            if (!childNode) {
                continue;
            }

            const childData = childNode.data as NodeDataType;

            if (!childData.taskDispatcher || !childData.taskDispatcherId) {
                continue;
            }

            const childCross = childNode.position[crossAxis];
            const conditionToChild = childCross - conditionCross;

            // Check if the child is on the same side as the grandparent
            // and at or beyond its cross-axis level
            const sameDirection = conditionToParent * conditionToChild > 0;
            const atOrBeyondParent = Math.abs(conditionToChild) >= Math.abs(conditionToParent) - 1;

            if (!sameDirection || !atOrBeyondParent) {
                continue;
            }

            const targetCross = conditionCross + conditionToParent * 0.5;
            const shiftDelta = targetCross - childCross;

            childNode.position = {
                ...childNode.position,
                [crossAxis]: targetCross,
            };

            const descendantIds = new Set<string>();

            collectNestedDispatcherNodes(childData.taskDispatcherId, allNodes, descendantIds);

            allNodes.forEach((descendantNode) => {
                if (descendantIds.has(descendantNode.id) && descendantNode.id !== childNode.id) {
                    descendantNode.position = {
                        ...descendantNode.position,
                        [crossAxis]: descendantNode.position[crossAxis] + shiftDelta,
                    };
                }
            });

            // Also shift nodes that follow this dispatcher in the condition branch
            // chain. These nodes (e.g., loop_2 after loop_1) are not descendants of
            // the shifted dispatcher but still need the same cross-axis adjustment.
            const componentName = childData.componentName as string;
            const bottomGhostId = `${childData.taskDispatcherId}-${componentName}-bottom-ghost`;
            let chainNodeId = edges.find((chainEdge) => chainEdge.source === bottomGhostId)?.target || '';

            while (chainNodeId) {
                const chainNode = allNodes.find((node) => node.id === chainNodeId);

                if (!chainNode || chainNode.type === 'taskDispatcherBottomGhostNode') {
                    break;
                }

                chainNode.position = {
                    ...chainNode.position,
                    [crossAxis]: chainNode.position[crossAxis] + shiftDelta,
                };

                const chainNodeData = chainNode.data as NodeDataType;

                if (chainNodeData.taskDispatcher && chainNodeData.taskDispatcherId) {
                    const chainDescendantIds = new Set<string>();

                    collectNestedDispatcherNodes(chainNodeData.taskDispatcherId, allNodes, chainDescendantIds);

                    allNodes.forEach((descendantNode) => {
                        if (chainDescendantIds.has(descendantNode.id) && descendantNode.id !== chainNodeId) {
                            descendantNode.position = {
                                ...descendantNode.position,
                                [crossAxis]: descendantNode.position[crossAxis] + shiftDelta,
                            };
                        }
                    });

                    const chainComponentName = chainNodeData.componentName as string;
                    const chainBottomGhostId = `${chainNodeData.taskDispatcherId}-${chainComponentName}-bottom-ghost`;

                    chainNodeId =
                        edges.find((chainEdge) => chainEdge.source === chainBottomGhostId)?.target || '';
                } else {
                    chainNodeId = edges.find((chainEdge) => chainEdge.source === chainNodeId)?.target || '';
                }
            }
        }
    });
}

/**
 * Returns the rendered main-axis size for ghost and placeholder nodes.
 *
 * In LR mode the main axis is x (width): ghosts render at 2px, placeholders at 72px.
 * In TB mode the main axis is y (height): ghosts render at 2px, placeholders at 28px.
 */
function getGhostOrPlaceholderMainAxisSize(nodeType: string | undefined, mainAxis: 'x' | 'y'): number {
    const GHOST_RENDERED_THICKNESS = 2;

    if (nodeType === 'taskDispatcherTopGhostNode' || nodeType === 'taskDispatcherBottomGhostNode') {
        return GHOST_RENDERED_THICKNESS;
    }

    return mainAxis === 'x' ? CLUSTER_ELEMENT_NODE_WIDTH : PLACEHOLDER_NODE_HEIGHT;
}

/**
 * Centers task-dispatcher placeholder nodes on the main axis between their
 * source ghost and target ghost.
 *
 * Dagre places placeholders at arbitrary main-axis positions (often near the
 * bottom-ghost). This function repositions each placeholder so its visual
 * center aligns with the midpoint between the visual centers of its source
 * and target ghosts. This accounts for size differences between ghosts (2px)
 * and placeholders (72px in LR, 28px in TB) to produce equal-length edges.
 *
 * Works uniformly for all dispatcher types (condition, loop, branch, each,
 * fork-join, parallel) because they all follow the same edge pattern:
 *   sourceGhost → placeholder → targetGhost
 */
export function centerDispatcherPlaceholdersOnMainAxis(
    allNodes: Node[],
    edges: Edge[],
    mainAxis: 'x' | 'y'
): void {
    allNodes.forEach((node) => {
        if (node.type !== 'placeholder') {
            return;
        }

        const nodeData = node.data as NodeDataType;

        if (!nodeData.taskDispatcherId) {
            return;
        }

        const incomingEdge = edges.find((edge) => edge.target === node.id);
        const outgoingEdge = edges.find((edge) => edge.source === node.id);

        if (!incomingEdge || !outgoingEdge) {
            return;
        }

        const sourceGhost = allNodes.find((ghostNode) => ghostNode.id === incomingEdge.source);
        const targetGhost = allNodes.find((ghostNode) => ghostNode.id === outgoingEdge.target);

        if (!sourceGhost || !targetGhost) {
            return;
        }

        const sourceGhostSize = getGhostOrPlaceholderMainAxisSize(sourceGhost.type, mainAxis);
        const targetGhostSize = getGhostOrPlaceholderMainAxisSize(targetGhost.type, mainAxis);
        const placeholderSize = getGhostOrPlaceholderMainAxisSize(node.type, mainAxis);

        const sourceGhostCenter = sourceGhost.position[mainAxis] + sourceGhostSize / 2;
        const targetGhostCenter = targetGhost.position[mainAxis] + targetGhostSize / 2;
        const midpointOfCenters = (sourceGhostCenter + targetGhostCenter) / 2;

        node.position = {
            ...node.position,
            [mainAxis]: midpointOfCenters - placeholderSize / 2,
        };
    });
}

interface CenterNodesAfterBottomGhostI {
    crossAxis: 'x' | 'y';
    crossAxisSize: number;
    direction: LayoutDirectionType;
}

/**
 * Centers nodes after a task dispatcher bottom ghost under the dispatcher node,
 * shifting entire task dispatcher subtrees and skipping their internals.
 */
export function centerNodesAfterBottomGhost(
    allNodes: Node[],
    edges: Edge[],
    options: CenterNodesAfterBottomGhostI
): void {
    const {crossAxis, crossAxisSize, direction} = options;

    edges.forEach((edge) => {
        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode || sourceNode.type !== 'taskDispatcherBottomGhostNode') {
            return;
        }

        const sourceData = sourceNode.data as NodeDataType;
        const dispatcherId = sourceData.taskDispatcherId;

        if (!dispatcherId) {
            return;
        }

        const dispatcherNode = allNodes.find((node) => node.id === dispatcherId);

        if (!dispatcherNode) {
            return;
        }

        // Skip centering when the dispatcher has a saved custom position
        const dispatcherData = dispatcherNode.data as NodeDataType;

        if (containsNodePosition(dispatcherData.metadata)) {
            return;
        }

        const dispatcherCenterCross = dispatcherNode.position[crossAxis] + crossAxisSize / 2;
        const targetCross = dispatcherCenterCross - crossAxisSize / 2;

        let currentNodeId = edge.target;

        while (currentNodeId) {
            const currentNode = allNodes.find((node) => node.id === currentNodeId);

            if (!currentNode || currentNode.type === 'taskDispatcherBottomGhostNode') {
                break;
            }

            const currentData = currentNode.data as NodeDataType;

            // Skip nodes with saved positions
            if (containsNodePosition(currentData.metadata)) {
                if (currentData.taskDispatcher && currentData.taskDispatcherId) {
                    const componentName = currentData.componentName as string;
                    const bottomGhostId = `${currentData.taskDispatcherId}-${componentName}-bottom-ghost`;
                    const bottomGhostEdge = edges.find((bottomEdge) => bottomEdge.source === bottomGhostId);

                    currentNodeId = bottomGhostEdge?.target || '';
                } else {
                    const nextEdge = edges.find((nextEdge) => nextEdge.source === currentNodeId);

                    currentNodeId = nextEdge?.target || '';
                }

                continue;
            }

            const nodeTargetCross = targetCross + getClusterRootCrossOffset(currentNode, direction);

            if (currentData.taskDispatcher && currentData.taskDispatcherId) {
                const shiftCross = nodeTargetCross - currentNode.position[crossAxis];

                if (Math.abs(shiftCross) > 0.5) {
                    currentNode.position = {...currentNode.position, [crossAxis]: nodeTargetCross};

                    const descendantIds = new Set<string>();

                    collectNestedDispatcherNodes(currentData.taskDispatcherId, allNodes, descendantIds);

                    allNodes.forEach((descendantNode) => {
                        if (descendantIds.has(descendantNode.id) && descendantNode.id !== currentNodeId) {
                            descendantNode.position = {
                                ...descendantNode.position,
                                [crossAxis]: descendantNode.position[crossAxis] + shiftCross,
                            };
                        }
                    });
                }

                const componentName = currentData.componentName as string;
                const bottomGhostId = `${currentData.taskDispatcherId}-${componentName}-bottom-ghost`;
                const bottomGhostEdge = edges.find((bottomEdge) => bottomEdge.source === bottomGhostId);

                currentNodeId = bottomGhostEdge?.target || '';
            } else {
                currentNode.position = {...currentNode.position, [crossAxis]: nodeTargetCross};

                const nextEdge = edges.find((nextEdge) => nextEdge.source === currentNodeId);

                currentNodeId = nextEdge?.target || '';
            }
        }
    });
}

interface ConditionPlaceholderI {
    conditionCaseOffset: number;
    crossAxis: 'x' | 'y';
}

/**
 * Positions condition case placeholders at least +/- conditionCaseOffset from center,
 * expanding further if descendants extend beyond that offset.
 */
export function positionConditionCasePlaceholders(allNodes: Node[], options: ConditionPlaceholderI): void {
    const {conditionCaseOffset, crossAxis} = options;

    allNodes.forEach((conditionNode) => {
        const conditionData = conditionNode.data as NodeDataType;

        if (conditionData.componentName !== 'condition') {
            return;
        }

        const conditionId = conditionNode.id;
        const conditionCross = conditionNode.position[crossAxis];

        const leftPlaceholder = allNodes.find((node) => node.id === `${conditionId}-condition-left-placeholder-0`);

        const rightPlaceholder = allNodes.find((node) => node.id === `${conditionId}-condition-right-placeholder-0`);

        const descendantIds = new Set<string>();

        collectNestedDispatcherNodes(conditionId, allNodes, descendantIds);

        if (leftPlaceholder) {
            const maxLeftCross = conditionCross - conditionCaseOffset;

            let leftmostDescendantCross = Infinity;

            allNodes.forEach((descendantNode) => {
                if (
                    descendantIds.has(descendantNode.id) &&
                    descendantNode.type !== 'taskDispatcherLeftGhostNode' &&
                    descendantNode.type !== 'placeholder'
                ) {
                    leftmostDescendantCross = Math.min(leftmostDescendantCross, descendantNode.position[crossAxis]);
                }
            });

            const neededLeftCross =
                leftmostDescendantCross !== Infinity ? leftmostDescendantCross - conditionCaseOffset : maxLeftCross;

            leftPlaceholder.position = {
                ...leftPlaceholder.position,
                [crossAxis]: Math.min(maxLeftCross, neededLeftCross),
            };
        }

        if (rightPlaceholder) {
            const minRightCross = conditionCross + conditionCaseOffset;

            let rightmostDescendantCross = -Infinity;

            allNodes.forEach((descendantNode) => {
                if (
                    descendantIds.has(descendantNode.id) &&
                    descendantNode.type !== 'taskDispatcherLeftGhostNode' &&
                    descendantNode.type !== 'placeholder'
                ) {
                    rightmostDescendantCross = Math.max(rightmostDescendantCross, descendantNode.position[crossAxis]);
                }
            });

            const neededRightCross =
                rightmostDescendantCross !== -Infinity ? rightmostDescendantCross + conditionCaseOffset : minRightCross;

            rightPlaceholder.position = {
                ...rightPlaceholder.position,
                [crossAxis]: Math.max(minRightCross, neededRightCross),
            };
        }
    });
}

interface ShiftConditionBranchContentI {
    crossAxis: 'x' | 'y';
    nodesep: number;
}

/**
 * Shifts condition branch content so nested dispatcher nodes stay within the condition frame.
 */
export function shiftConditionBranchContent(allNodes: Node[], options: ShiftConditionBranchContentI): void {
    const {crossAxis, nodesep} = options;

    allNodes.forEach((conditionNode) => {
        const conditionData = conditionNode.data as NodeDataType;

        if (conditionData.componentName !== 'condition') {
            return;
        }

        const conditionId = conditionNode.id;
        const leftPlaceholder = allNodes.find((node) => node.id === `${conditionId}-condition-left-placeholder-0`);
        const rightPlaceholder = allNodes.find((node) => node.id === `${conditionId}-condition-right-placeholder-0`);

        const descendantIds = new Set<string>();

        collectNestedDispatcherNodes(conditionId, allNodes, descendantIds);

        const isInternalDescendant = (nodeId: string) =>
            descendantIds.has(nodeId) && nodeId !== conditionId && !nodeId.startsWith(`${conditionId}-condition-`);

        if (leftPlaceholder) {
            const leftBound = leftPlaceholder.position[crossAxis];

            let minDescendantCross = Infinity;

            allNodes.forEach((descendantNode) => {
                if (isInternalDescendant(descendantNode.id)) {
                    minDescendantCross = Math.min(minDescendantCross, descendantNode.position[crossAxis]);
                }
            });

            if (minDescendantCross < leftBound) {
                const shift = leftBound - minDescendantCross + nodesep / 2;

                allNodes.forEach((descendantNode) => {
                    if (isInternalDescendant(descendantNode.id)) {
                        descendantNode.position = {
                            ...descendantNode.position,
                            [crossAxis]: descendantNode.position[crossAxis] + shift,
                        };
                    }
                });
            }
        }

        if (rightPlaceholder) {
            const rightBound = rightPlaceholder.position[crossAxis];

            let maxDescendantCross = -Infinity;

            allNodes.forEach((descendantNode) => {
                if (isInternalDescendant(descendantNode.id)) {
                    maxDescendantCross = Math.max(maxDescendantCross, descendantNode.position[crossAxis]);
                }
            });

            if (maxDescendantCross > rightBound) {
                const shift = rightBound - maxDescendantCross - nodesep / 2;

                allNodes.forEach((descendantNode) => {
                    if (isInternalDescendant(descendantNode.id)) {
                        descendantNode.position = {
                            ...descendantNode.position,
                            [crossAxis]: descendantNode.position[crossAxis] + shift,
                        };
                    }
                });
            }
        }
    });
}

interface ConstrainLeftGhostI {
    conditionCaseOffset: number;
    crossAxis: 'x' | 'y';
    direction: LayoutDirectionType;
}

/**
 * Constrains left-ghost positions so loop/each rings don't grow with child width,
 * but ensures the left ghost always encompasses all descendant nodes.
 */
export function constrainLeftGhostPositions(allNodes: Node[], options: ConstrainLeftGhostI): void {
    const {conditionCaseOffset, crossAxis, direction} = options;

    const LEFT_GHOST_VISUAL_SIZE = 2;
    const PLACEHOLDER_DOM_CROSS_SIZE = 72;
    const handleCenterDifference = PLACEHOLDER_DOM_CROSS_SIZE / 2 - LEFT_GHOST_VISUAL_SIZE / 2;
    const MAX_RING_WIDTH = direction === 'LR' ? conditionCaseOffset : conditionCaseOffset - handleCenterDifference;
    const LEFT_GHOST_PADDING = 20;
    const lrCenteringOffset = direction === 'LR' ? (72 - LEFT_GHOST_VISUAL_SIZE) / 2 : 0;

    allNodes.forEach((leftGhostNode) => {
        if (leftGhostNode.type !== 'taskDispatcherLeftGhostNode') {
            return;
        }

        const taskDispatcherId = (leftGhostNode.data as NodeDataType).taskDispatcherId;

        if (!taskDispatcherId) {
            return;
        }

        const topGhostNode = allNodes.find(
            (node) =>
                node.type === 'taskDispatcherTopGhostNode' &&
                (node.data as NodeDataType).taskDispatcherId === taskDispatcherId
        );

        if (!topGhostNode) {
            return;
        }

        const cappedCross = topGhostNode.position[crossAxis] - MAX_RING_WIDTH;

        const descendantIds = new Set<string>();

        collectNestedDispatcherNodes(taskDispatcherId, allNodes, descendantIds);

        let leftmostDescendantCross = Infinity;

        allNodes.forEach((node) => {
            if (descendantIds.has(node.id) && node.type !== 'taskDispatcherLeftGhostNode') {
                leftmostDescendantCross = Math.min(leftmostDescendantCross, node.position[crossAxis]);
            }
        });

        const minRequiredCross =
            leftmostDescendantCross === Infinity
                ? cappedCross
                : leftmostDescendantCross - LEFT_GHOST_PADDING - lrCenteringOffset;

        leftGhostNode.position = {
            ...leftGhostNode.position,
            [crossAxis]: Math.min(cappedCross, minRequiredCross),
        };
    });
}

/**
 * In LR mode, dagre allocates NODE_WIDTH (240px) cross-axis height for every
 * node but visual sizes differ (72px for workflow/ghost, 28px for placeholder,
 * 2px for left-ghost). This function centers each node within its dagre
 * allocation so all visual centers align at dagre_center + NODE_WIDTH/2,
 * preserving dagre's symmetric layout.
 */
export function centerLRSmallNodes(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    const LEFT_GHOST_VISUAL_SIZE = 2;

    allNodes.forEach((node) => {
        let visualCrossSize: number;

        if (node.type === 'taskDispatcherLeftGhostNode') {
            visualCrossSize = LEFT_GHOST_VISUAL_SIZE;
        } else if (node.type === 'placeholder') {
            visualCrossSize = PLACEHOLDER_NODE_HEIGHT;
        } else {
            visualCrossSize = CLUSTER_ELEMENT_NODE_WIDTH;
        }

        const offset = (NODE_WIDTH - visualCrossSize) / 2;

        if (offset > 0) {
            node.position = {
                ...node.position,
                [crossAxis]: node.position[crossAxis] + offset,
            };
        }
    });
}

/**
 * Overrides dagre positions with saved metadata positions and offsets ghost/placeholder
 * nodes of dispatchers with saved positions so frames stay attached.
 *
 * When `crossAxisShift` is non-zero, saved positions are shifted on the cross-axis
 * to compensate for canvas centering changes (e.g., when a side panel opens/closes).
 */
export function applySavedPositions(
    allNodes: Node[],
    crossAxis: 'x' | 'y' = 'x',
    crossAxisShift: number = 0
): Map<string, {x: number; y: number}> {
    const dispatcherDeltas = new Map<string, {x: number; y: number}>();

    for (let nodeIndex = 0; nodeIndex < allNodes.length; nodeIndex++) {
        const nodeData = allNodes[nodeIndex].data as NodeDataType;

        if (containsNodePosition(nodeData.metadata)) {
            const dagrePosition = allNodes[nodeIndex].position;
            const rawSavedPosition = nodeData.metadata.ui.nodePosition;
            const savedPosition = {
                x: rawSavedPosition.x + (crossAxis === 'x' ? crossAxisShift : 0),
                y: rawSavedPosition.y + (crossAxis === 'y' ? crossAxisShift : 0),
            };

            allNodes[nodeIndex] = {
                ...allNodes[nodeIndex],
                position: savedPosition,
            };

            if (nodeData.taskDispatcher) {
                dispatcherDeltas.set(allNodes[nodeIndex].id, {
                    x: savedPosition.x - dagrePosition.x,
                    y: savedPosition.y - dagrePosition.y,
                });
            }
        }
    }

    if (dispatcherDeltas.size > 0) {
        const ghostAndPlaceholderTypes = new Set([
            'taskDispatcherTopGhostNode',
            'taskDispatcherBottomGhostNode',
            'taskDispatcherLeftGhostNode',
            'placeholder',
        ]);

        // Iteratively shift ghosts/placeholders and child workflow nodes.
        // When a shifted child is itself a task dispatcher, add it to
        // dispatcherDeltas so its own ghosts/placeholders and children
        // are shifted in the next iteration (handles arbitrary nesting).
        const shifted = new Set<string>();
        let changed = true;

        while (changed) {
            changed = false;

            for (let nodeIndex = 0; nodeIndex < allNodes.length; nodeIndex++) {
                if (shifted.has(allNodes[nodeIndex].id)) {
                    continue;
                }

                const nodeData = allNodes[nodeIndex].data as NodeDataType;

                if (ghostAndPlaceholderTypes.has(allNodes[nodeIndex].type!) && nodeData.taskDispatcherId) {
                    const delta = dispatcherDeltas.get(nodeData.taskDispatcherId);

                    if (delta) {
                        allNodes[nodeIndex] = {
                            ...allNodes[nodeIndex],
                            position: {
                                x: allNodes[nodeIndex].position.x + delta.x,
                                y: allNodes[nodeIndex].position.y + delta.y,
                            },
                        };

                        shifted.add(allNodes[nodeIndex].id);
                        changed = true;
                    }
                } else if (!ghostAndPlaceholderTypes.has(allNodes[nodeIndex].type!)) {
                    if (containsNodePosition(nodeData.metadata)) {
                        continue;
                    }

                    // Use nesting-data properties (NOT taskDispatcherId) to find the
                    // parent dispatcher. taskDispatcherId is the node's OWN ID when
                    // the node is itself a task dispatcher, not the parent's ID.
                    const parentDispatcherId =
                        nodeData.conditionData?.conditionId ||
                        nodeData.loopData?.loopId ||
                        nodeData.branchData?.branchId ||
                        nodeData.eachData?.eachId ||
                        nodeData.parallelData?.parallelId ||
                        nodeData.forkJoinData?.forkJoinId;

                    if (!parentDispatcherId) {
                        continue;
                    }

                    const delta = dispatcherDeltas.get(parentDispatcherId);

                    if (!delta) {
                        continue;
                    }

                    allNodes[nodeIndex] = {
                        ...allNodes[nodeIndex],
                        position: {
                            x: allNodes[nodeIndex].position.x + delta.x,
                            y: allNodes[nodeIndex].position.y + delta.y,
                        },
                    };

                    shifted.add(allNodes[nodeIndex].id);
                    changed = true;

                    // Propagate delta to nested dispatcher's descendants
                    if (nodeData.taskDispatcher && nodeData.taskDispatcherId) {
                        dispatcherDeltas.set(allNodes[nodeIndex].id, delta);
                    }
                }
            }
        }
    }

    return dispatcherDeltas;
}

/**
 * After saved positions are applied, nodes downstream of a child dispatcher's
 * bottom ghost may remain at their dagre positions while the bottom ghost was
 * shifted. This happens because applySavedPositions only shifts nodes whose
 * parent dispatcher ID matches the shifted dispatcher, but sibling nodes in
 * the parent frame (after the child's bottom ghost) reference the PARENT
 * dispatcher, not the child.
 *
 * This function:
 * 1. Propagates the incremental main-axis shift from each saved-position
 *    dispatcher's bottom ghost to downstream sibling nodes.
 * 2. As a safety net, checks each bottom ghost against its incoming-edge
 *    sources and pushes it forward if any source extends beyond it.
 */
export function adjustBottomGhostForMovedChildren(
    allNodes: Node[],
    edges: Edge[],
    mainAxis: 'x' | 'y',
    direction: LayoutDirectionType,
    savedDispatcherDeltas: Map<string, {x: number; y: number}> = new Map()
): void {
    // Step 1: Propagate incremental main-axis shift from each saved-position
    // dispatcher's bottom ghost to downstream sibling nodes.
    //
    // When a child dispatcher (e.g. condition_4 inside condition_1) has a saved
    // position, applySavedPositions shifts it and its own ghosts/children. But
    // nodes after its bottom ghost in the parent frame (with conditionData
    // pointing to the parent) aren't shifted. This step fixes that.
    const propagatedShifts = new Map<string, number>();

    // Process dispatchers with larger deltas first so the max shift wins
    // at convergence points (e.g. parent's bottom ghost reached from
    // multiple child dispatchers).
    const sortedDeltas = [...savedDispatcherDeltas.entries()].sort(
        (firstEntry, secondEntry) => secondEntry[1][mainAxis] - firstEntry[1][mainAxis]
    );

    for (const [dispatcherId, delta] of sortedDeltas) {
        // Only process dispatchers with their own saved position (not inherited)
        const dispatcherNode = allNodes.find((node) => node.id === dispatcherId);

        if (!dispatcherNode) {
            continue;
        }

        const dispatcherData = dispatcherNode.data as NodeDataType;

        if (!containsNodePosition(dispatcherData.metadata)) {
            continue;
        }

        // Find the parent dispatcher's delta (if any) so we compute the
        // INCREMENTAL shift: childDelta - parentDelta. This avoids double-
        // shifting when both parent and child dispatchers have saved positions.
        const parentDispatcherId =
            dispatcherData.conditionData?.conditionId ||
            dispatcherData.loopData?.loopId ||
            dispatcherData.branchData?.branchId ||
            dispatcherData.eachData?.eachId ||
            dispatcherData.parallelData?.parallelId ||
            dispatcherData.forkJoinData?.forkJoinId;

        const parentMainAxisDelta = parentDispatcherId
            ? (savedDispatcherDeltas.get(parentDispatcherId)?.[mainAxis] ?? 0)
            : 0;

        const incrementalDelta = delta[mainAxis] - parentMainAxisDelta;

        // Only propagate forward shifts (positive main-axis delta)
        if (incrementalDelta <= 0) {
            continue;
        }

        // Find the bottom ghost for this dispatcher
        const bottomGhost = allNodes.find(
            (node) =>
                node.type === 'taskDispatcherBottomGhostNode' &&
                (node.data as NodeDataType).taskDispatcherId === dispatcherId
        );

        if (!bottomGhost) {
            continue;
        }

        // BFS from bottom ghost, shifting auxiliary nodes (ghosts, placeholders)
        // and workflow nodes that come after a DIFFERENT dispatcher's bottom ghost.
        //
        // Workflow nodes directly downstream of the STARTING bottom ghost are
        // resolved by the same dispatcher (condition_4) and will be shifted by
        // alignChainNodesCrossAxis via deltaShiftNodes — skipping them here
        // avoids double-shifting. But once we cross into a different dispatcher's
        // bottom ghost (e.g. condition_1-condition-bottom-ghost), the downstream
        // workflow nodes (e.g. accelo_5) are resolved by that different dispatcher
        // and WON'T be in deltaShiftNodes — so we must shift them here.
        const auxiliaryTypes = new Set([
            'taskDispatcherTopGhostNode',
            'taskDispatcherBottomGhostNode',
            'taskDispatcherLeftGhostNode',
            'placeholder',
        ]);

        // Track bottom ghosts shifted by this BFS (belonging to a different
        // dispatcher). Workflow nodes reached from these are safe to shift.
        const shiftedBottomGhosts = new Set<string>();

        const visited = new Set<string>([bottomGhost.id]);
        const queue: Array<{nodeId: string; afterShiftedGhost: boolean}> = [
            {afterShiftedGhost: false, nodeId: bottomGhost.id},
        ];

        while (queue.length > 0) {
            const {afterShiftedGhost: queueAfterShiftedGhost, nodeId: currentId} = queue.shift()!;

            // If the current node itself was shifted as a bottom ghost,
            // all its downstream nodes are "after a shifted ghost".
            const afterShiftedGhost = queueAfterShiftedGhost || shiftedBottomGhosts.has(currentId);

            for (const edge of edges) {
                if (edge.source !== currentId || visited.has(edge.target)) {
                    continue;
                }

                visited.add(edge.target);

                const targetNode = allNodes.find((node) => node.id === edge.target);

                if (!targetNode) {
                    continue;
                }

                const targetData = targetNode.data as NodeDataType;

                if (containsNodePosition(targetData.metadata)) {
                    queue.push({afterShiftedGhost, nodeId: targetNode.id});

                    continue;
                }

                const isAuxiliary = auxiliaryTypes.has(targetNode.type!);

                if (isAuxiliary) {
                    // Skip cluster elements of downstream dispatchers — they move
                    // with their parent dispatcher node, not independently.
                    const auxiliaryDispatcherId = targetData.taskDispatcherId;

                    if (auxiliaryDispatcherId && visited.has(auxiliaryDispatcherId)) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }

                    // Auxiliary nodes without a dispatcher (e.g. trailing
                    // placeholder) follow the same rule as workflow nodes:
                    // only shift if after a shifted bottom ghost boundary.
                    if (!auxiliaryDispatcherId && !afterShiftedGhost) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }
                } else {
                    // Workflow node: only shift if we've crossed a shifted bottom
                    // ghost boundary (resolved by a different dispatcher, so
                    // alignChainNodesCrossAxis won't handle the main-axis shift).
                    if (!afterShiftedGhost) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }
                }

                // Apply the larger shift at convergence points
                const existingShift = propagatedShifts.get(targetNode.id) ?? 0;

                if (incrementalDelta > existingShift) {
                    const additionalShift = incrementalDelta - existingShift;

                    targetNode.position = {
                        ...targetNode.position,
                        [mainAxis]: targetNode.position[mainAxis] + additionalShift,
                    } as {x: number; y: number};

                    propagatedShifts.set(targetNode.id, incrementalDelta);

                    if (isAuxiliary && targetNode.type === 'taskDispatcherBottomGhostNode') {
                        shiftedBottomGhosts.add(targetNode.id);
                    }
                }

                queue.push({afterShiftedGhost, nodeId: targetNode.id});
            }
        }
    }

    // Step 2: Safety net — check each bottom ghost against incoming-edge
    // sources and push forward if any source extends beyond it.
    for (let ghostIndex = 0; ghostIndex < allNodes.length; ghostIndex++) {
        if (allNodes[ghostIndex].type !== 'taskDispatcherBottomGhostNode') {
            continue;
        }

        const ghostMainPos = allNodes[ghostIndex].position[mainAxis];
        let neededPosition = -Infinity;

        const incomingEdges = edges.filter((edge) => edge.target === allNodes[ghostIndex].id);

        for (const edge of incomingEdges) {
            const sourceNode = allNodes.find((node) => node.id === edge.source);

            if (!sourceNode) {
                continue;
            }

            const gapFromSource = computeMinGapToBottomGhost(sourceNode, direction);
            const required = sourceNode.position[mainAxis] + gapFromSource;

            if (required > neededPosition) {
                neededPosition = required;
            }
        }

        if (neededPosition <= ghostMainPos) {
            continue;
        }

        const cascadeDelta = neededPosition - ghostMainPos;

        allNodes[ghostIndex].position = {
            ...allNodes[ghostIndex].position,
            [mainAxis]: neededPosition,
        } as {x: number; y: number};

        // Cascade the push to all downstream nodes (BFS via outgoing edges),
        // skipping nodes that have their own saved position.
        const visited = new Set<string>([allNodes[ghostIndex].id]);
        const queue = [allNodes[ghostIndex].id];

        while (queue.length > 0) {
            const currentId = queue.shift()!;

            for (const edge of edges) {
                if (edge.source !== currentId || visited.has(edge.target)) {
                    continue;
                }

                visited.add(edge.target);

                const targetNode = allNodes.find((node) => node.id === edge.target);

                if (!targetNode) {
                    continue;
                }

                const targetData = targetNode.data as NodeDataType;

                if (containsNodePosition(targetData.metadata)) {
                    continue;
                }

                targetNode.position = {
                    ...targetNode.position,
                    [mainAxis]: targetNode.position[mainAxis] + cascadeDelta,
                } as {x: number; y: number};

                queue.push(edge.target);
            }
        }
    }
}

/**
 * Computes the minimum main-axis gap from a source node to a bottom ghost,
 * accounting for the minlen=2 that dagre uses for edges targeting bottom ghosts.
 *
 * Formula mirrors computeMainAxisGap but uses 2 × RANKSEP (for minlen=2)
 * and the bottom ghost's dagre size of 0 with rendered size of 2.
 */
function computeMinGapToBottomGhost(sourceNode: Node, direction: LayoutDirectionType): number {
    const RANKSEP = 50;
    const BOTTOM_GHOST_MINLEN = 2;
    const BOTTOM_GHOST_RENDERED_HALF = 1;

    if (direction === 'LR') {
        let sourceDagreWidth: number;
        let sourceRendered: number;

        if (
            sourceNode.type === 'placeholder' ||
            sourceNode.type === 'taskDispatcherTopGhostNode' ||
            sourceNode.type === 'taskDispatcherLeftGhostNode'
        ) {
            sourceDagreWidth = sourceNode.type === 'taskDispatcherLeftGhostNode' ? 16 : PLACEHOLDER_NODE_HEIGHT;
            sourceRendered =
                sourceNode.type === 'taskDispatcherLeftGhostNode'
                    ? 16
                    : sourceNode.type === 'taskDispatcherTopGhostNode'
                      ? 2
                      : CLUSTER_ELEMENT_NODE_WIDTH;
        } else {
            sourceDagreWidth = getLRDagreWidth(sourceNode);
            sourceRendered = getLRRenderedWidth(sourceNode);
        }

        return sourceRendered / 2 + sourceDagreWidth / 2 + RANKSEP * BOTTOM_GHOST_MINLEN - BOTTOM_GHOST_RENDERED_HALF;
    }

    // TB mode: positions are dagre centers (getRenderedMainAxisSize returns 0).
    return NODE_HEIGHT / 2 + RANKSEP * BOTTOM_GHOST_MINLEN;
}

/**
 * Computes the React Flow main-axis position gap between two consecutive
 * workflow nodes, matching what dagre would produce for adjacent ranks.
 *
 * In LR mode the main axis is x; in TB mode the main axis is y.
 * The gap accounts for dagre-allocated sizes (which differ from rendered sizes)
 * and the default ranksep of 50.
 */
function computeMainAxisGap(predecessorNode: Node, currentNode: Node, direction: LayoutDirectionType): number {
    const RANKSEP = 50;

    if (direction === 'LR') {
        const predecessorDagreWidth = getLRDagreWidth(predecessorNode);
        const predecessorRendered = getLRRenderedWidth(predecessorNode);
        const currentDagreWidth = getLRDagreWidth(currentNode);
        const currentRendered = getLRRenderedWidth(currentNode);

        return (
            predecessorRendered / 2 + predecessorDagreWidth / 2 + RANKSEP + currentDagreWidth / 2 - currentRendered / 2
        );
    }

    // TB mode: getRenderedMainAxisSize returns 0, so RF position = dagre center.
    // Gap = half dagre heights + ranksep.
    return getTBDagreHeight(predecessorNode) / 2 + RANKSEP + getTBDagreHeight(currentNode) / 2;
}

const AI_AGENT_NODE_HEIGHT = 150;

function getTBDagreHeight(node: Node): number {
    if (node.type === 'aiAgentNode') {
        return AI_AGENT_NODE_HEIGHT;
    }

    return NODE_HEIGHT;
}

function getLRDagreWidth(node: Node): number {
    if (node.type === 'aiAgentNode') {
        const nodeData = node.data as NodeDataType;
        const hasClusterElements =
            nodeData.clusterElements &&
            Object.entries(nodeData.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        return hasClusterElements ? 292 : 120;
    }

    return 120;
}

function getLRRenderedWidth(node: Node): number {
    if (node.type === 'aiAgentNode') {
        const nodeData = node.data as NodeDataType;
        const hasClusterElements =
            nodeData.clusterElements &&
            Object.entries(nodeData.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        return hasClusterElements ? 240 : CLUSTER_ELEMENT_NODE_WIDTH;
    }

    return CLUSTER_ELEMENT_NODE_WIDTH;
}

/**
 * Aligns consecutive workflow nodes in a chain to the same cross-axis level,
 * matching the inline "add node" behavior in both LR and TB modes.
 *
 * Cross-axis alignment (y in LR, x in TB) always happens for chain nodes.
 * Main-axis adjustment only happens when the predecessor has a saved or
 * previously-adjusted position, to compensate for the offset between
 * dagre's rank position and the saved position.
 *
 * Task dispatcher nodes (condition, loop, branch) are skipped when the
 * predecessor has no saved position, since dagre positions them to
 * accommodate their branch structure.
 */
export function alignChainNodesCrossAxis(
    allNodes: Node[],
    edges: Edge[],
    crossAxis: 'x' | 'y',
    direction: LayoutDirectionType,
    savedDispatcherDeltas: Map<string, {x: number; y: number}> = new Map()
): Map<string, {x: number; y: number}> {
    const AUXILIARY_TYPES = new Set([
        'taskDispatcherTopGhostNode',
        'taskDispatcherBottomGhostNode',
        'taskDispatcherLeftGhostNode',
        'placeholder',
    ]);

    // Build predecessor map: workflow node → its workflow-node predecessor
    // For direct workflow-to-workflow edges, use the source directly.
    // For edges from ghost nodes, resolve to the parent dispatcher so that
    // nodes inherit its cross-axis position:
    // - Bottom-ghosts: always resolve (nodes following a dispatcher)
    // - Top-ghosts: resolve only for single-body dispatchers (loop/each),
    //   NOT for multi-branch dispatchers (condition/branch) where branches
    //   fan out at different cross-axis levels
    const predecessorMap = new Map<string, string>();

    // Nodes resolved via top-ghost should only get cross-axis alignment,
    // because dagre correctly handles main-axis spacing through the ghost node.
    // Bottom-ghost resolved nodes are shifted by the dispatcher's saved-dagre delta
    // (both axes) since applySavedPositions shifts ghosts but not subsequent nodes.
    const crossAxisOnlyNodes = new Set<string>();
    const deltaShiftNodes = new Map<string, {x: number; y: number}>();

    for (const edge of edges) {
        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || AUXILIARY_TYPES.has(targetNode.type!)) {
            continue;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode) {
            continue;
        }

        const sourceData = sourceNode.data as NodeDataType;

        if (!AUXILIARY_TYPES.has(sourceNode.type!)) {
            predecessorMap.set(edge.target, edge.source);
        } else if (sourceNode.type === 'taskDispatcherBottomGhostNode') {
            // Resolve bottom-ghost to its parent dispatcher.
            // If the dispatcher was moved (saved position), shift the target by the same delta.
            if (sourceData.taskDispatcherId && !predecessorMap.has(edge.target)) {
                predecessorMap.set(edge.target, sourceData.taskDispatcherId);

                const delta = savedDispatcherDeltas.get(sourceData.taskDispatcherId);

                if (delta) {
                    deltaShiftNodes.set(edge.target, delta);
                } else {
                    crossAxisOnlyNodes.add(edge.target);
                }
            }
        }
        // Top-ghost resolution is intentionally omitted: children inside
        // single-body dispatchers (loop/each) have dagre-assigned cross-axis
        // offsets that must be preserved. When the dispatcher is chain-aligned,
        // the descendants block delta-shifts children correctly. When the
        // dispatcher has a saved position, applySavedPositions handles it.
    }

    // "anchored" = has a saved position or was aligned to an anchored predecessor.
    // Only anchored nodes propagate alignment to their successors.
    // Without any saved positions in a chain, nothing gets aligned (dagre is trusted).
    const anchored = new Set<string>();
    const skipped = new Set<string>();
    const dispatcherDeltas = new Map<string, {x: number; y: number}>();
    const mainAxis = crossAxis === 'x' ? 'y' : 'x';

    // Seed anchored set with nodes that have saved positions
    for (const node of allNodes) {
        if (containsNodePosition((node.data as NodeDataType).metadata)) {
            anchored.add(node.id);
        }
    }

    let changed = true;

    while (changed) {
        changed = false;

        for (let nodeIndex = 0; nodeIndex < allNodes.length; nodeIndex++) {
            const node = allNodes[nodeIndex];
            const nodeData = node.data as NodeDataType;

            if (anchored.has(node.id) || skipped.has(node.id) || AUXILIARY_TYPES.has(node.type!)) {
                continue;
            }

            const predecessorId = predecessorMap.get(node.id);

            if (!predecessorId || !anchored.has(predecessorId)) {
                continue;
            }

            const predecessorNode = allNodes.find((searchNode) => searchNode.id === predecessorId);

            if (!predecessorNode) {
                continue;
            }

            const predecessorData = predecessorNode.data as NodeDataType;
            const predecessorHasPosition = containsNodePosition(predecessorData.metadata);

            // Skip task dispatchers unless predecessor has a saved/adjusted position,
            // because dagre positions dispatchers to accommodate their branch structure.
            // Don't mark as anchored — dagre position is trusted, no alignment cascades through.
            if (nodeData.taskDispatcher && !predecessorHasPosition) {
                skipped.add(node.id);

                continue;
            }

            const oldPosition = node.position;
            let newPosition: {x: number; y: number};
            let adjustedMainAxis = false;

            // Cluster-root nodes (AI Agent with cluster elements) have a centering
            // offset applied during dagre conversion. When aligning different-width
            // nodes, preserve each node's own centering offset.
            const clusterCrossOffset =
                getClusterRootCrossOffset(node, direction) - getClusterRootCrossOffset(predecessorNode, direction);

            // Check pre-computed delta from savedDispatcherDeltas, then fall back
            // to chain-computed dispatcherDeltas for bottom-ghost-resolved nodes
            // whose predecessor dispatcher was chain-aligned (not originally saved).
            const deltaShift =
                deltaShiftNodes.get(node.id) ??
                (crossAxisOnlyNodes.has(node.id) ? dispatcherDeltas.get(predecessorId) : undefined);

            if (deltaShift) {
                // Bottom-ghost resolved node with dispatcher delta:
                // Cross-axis: align to predecessor (dispatcher's saved position)
                // Main-axis: shift by delta (preserves dagre gap through dispatcher subtree)
                newPosition = {
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                    [mainAxis]: oldPosition[mainAxis] + deltaShift[mainAxis],
                } as {x: number; y: number};

                adjustedMainAxis = true;
            } else if (predecessorHasPosition && !crossAxisOnlyNodes.has(node.id)) {
                // Predecessor has saved/adjusted position: adjust both axes
                const mainAxisGap = computeMainAxisGap(predecessorNode, node, direction);

                newPosition = {
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                    [mainAxis]: predecessorNode.position[mainAxis] + mainAxisGap,
                } as {x: number; y: number};

                adjustedMainAxis = true;
            } else {
                // Predecessor at dagre position or top-ghost resolved: only align cross-axis
                newPosition = {
                    ...oldPosition,
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                };
            }

            if (adjustedMainAxis) {
                // Set nodePosition metadata for pin button display and cascading
                allNodes[nodeIndex] = {
                    ...node,
                    data: {
                        ...nodeData,
                        metadata: {
                            ...nodeData.metadata,
                            ui: {
                                ...nodeData.metadata?.ui,
                                nodePosition: newPosition,
                            },
                        },
                    },
                    position: newPosition,
                };
            } else {
                allNodes[nodeIndex] = {
                    ...node,
                    position: newPosition,
                };
            }

            if (nodeData.taskDispatcher) {
                dispatcherDeltas.set(node.id, {
                    x: newPosition.x - oldPosition.x,
                    y: newPosition.y - oldPosition.y,
                });
            }

            anchored.add(node.id);
            changed = true;
        }
    }

    // Shift dispatcher descendants when a dispatcher was aligned
    if (dispatcherDeltas.size > 0) {
        const shifted = new Set<string>();

        let shiftChanged = true;

        while (shiftChanged) {
            shiftChanged = false;

            for (let nodeIndex = 0; nodeIndex < allNodes.length; nodeIndex++) {
                const node = allNodes[nodeIndex];
                const nodeData = node.data as NodeDataType;

                const parentDispatcherId = getParentDispatcherId(nodeData);

                if (!parentDispatcherId || shifted.has(node.id) || anchored.has(node.id)) {
                    continue;
                }

                // Skip the dispatcher node itself — it was already aligned above
                if (dispatcherDeltas.has(node.id)) {
                    continue;
                }

                const delta = dispatcherDeltas.get(parentDispatcherId);

                if (!delta) {
                    continue;
                }

                if (containsNodePosition(nodeData.metadata) && !anchored.has(node.id)) {
                    continue;
                }

                const newPosition = {
                    x: node.position.x + delta.x,
                    y: node.position.y + delta.y,
                };

                if (AUXILIARY_TYPES.has(node.type!)) {
                    allNodes[nodeIndex] = {
                        ...node,
                        position: newPosition,
                    };
                } else {
                    allNodes[nodeIndex] = {
                        ...node,
                        data: {
                            ...nodeData,
                            metadata: {
                                ...nodeData.metadata,
                                ui: {
                                    ...nodeData.metadata?.ui,
                                    nodePosition: newPosition,
                                },
                            },
                        },
                        position: newPosition,
                    };
                }

                shifted.add(node.id);

                if (nodeData.taskDispatcher) {
                    dispatcherDeltas.set(node.id, delta);
                    shiftChanged = true;
                }
            }
        }
    }

    return dispatcherDeltas;
}

/**
 * Computes the React Flow main-axis position gap between a workflow node
 * and the trailing placeholder, matching what dagre would produce.
 *
 * In LR mode, the placeholder's dagre width is NODE_HEIGHT (swapped axes)
 * and its rendered width follows the default (CLUSTER_ELEMENT_NODE_WIDTH).
 * In TB mode, both use NODE_HEIGHT for dagre allocation with 0 rendered offset.
 */
function computeMainAxisGapToPlaceholder(predecessorNode: Node, direction: LayoutDirectionType): number {
    const RANKSEP = 50;

    if (direction === 'LR') {
        const predecessorDagreWidth = getLRDagreWidth(predecessorNode);
        const predecessorRendered = getLRRenderedWidth(predecessorNode);
        const placeholderDagreWidth = NODE_HEIGHT;
        const placeholderRendered = CLUSTER_ELEMENT_NODE_WIDTH;

        return (
            predecessorRendered / 2 +
            predecessorDagreWidth / 2 +
            RANKSEP +
            placeholderDagreWidth / 2 -
            placeholderRendered / 2
        );
    }

    return NODE_HEIGHT / 2 + RANKSEP + NODE_HEIGHT / 2;
}

/**
 * Aligns the trailing placeholder (the final "+" node at the end of the workflow)
 * to follow its predecessor when that predecessor was repositioned by a saved
 * position or chain alignment.
 *
 * The trailing placeholder is distinguished from in-frame placeholders by not
 * having a taskDispatcherId. When the predecessor has a saved or adjusted position,
 * the placeholder is repositioned on both axes (cross-axis alignment + main-axis gap).
 * When connected via a bottom-ghost from a saved dispatcher, it is shifted by the
 * dispatcher's saved-dagre delta.
 */
export function alignTrailingPlaceholder(
    allNodes: Node[],
    edges: Edge[],
    crossAxis: 'x' | 'y',
    direction: LayoutDirectionType,
    savedDispatcherDeltas: Map<string, {x: number; y: number}> = new Map()
): void {
    const mainAxis = crossAxis === 'x' ? 'y' : 'x';

    for (const edge of edges) {
        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || targetNode.type !== 'placeholder') {
            continue;
        }

        const targetData = targetNode.data as NodeDataType;

        // Only handle trailing placeholder (no taskDispatcherId)
        if (targetData.taskDispatcherId) {
            continue;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode) {
            continue;
        }

        const sourceData = sourceNode.data as NodeDataType;

        // Case 1: source is a workflow node with saved/adjusted position
        if (containsNodePosition(sourceData.metadata)) {
            const gap = computeMainAxisGapToPlaceholder(sourceNode, direction);

            // In LR mode, centerLRSmallNodes applied different cross-axis centering
            // offsets: +84 for workflow nodes (72px) vs +106 for placeholders (28px).
            // Compensate so visual centers align on the cross-axis.
            const crossAxisCenteringAdjustment =
                direction === 'LR' ? (CLUSTER_ELEMENT_NODE_WIDTH - PLACEHOLDER_NODE_HEIGHT) / 2 : 0;

            targetNode.position = {
                [crossAxis]: sourceNode.position[crossAxis] + crossAxisCenteringAdjustment,
                [mainAxis]: sourceNode.position[mainAxis] + gap,
            } as {x: number; y: number};

            continue;
        }

        // Case 2: source is a bottom-ghost from a saved dispatcher
        if (sourceNode.type === 'taskDispatcherBottomGhostNode' && sourceData.taskDispatcherId) {
            const delta = savedDispatcherDeltas.get(sourceData.taskDispatcherId);

            if (delta) {
                targetNode.position = {
                    x: targetNode.position.x + delta.x,
                    y: targetNode.position.y + delta.y,
                };
            }
        }
    }
}
