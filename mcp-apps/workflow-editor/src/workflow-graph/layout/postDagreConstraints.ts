import {Edge, Node} from '@xyflow/react';

import {NodeDataType} from '../types';
import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    LayoutDirectionType,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
} from './constants';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const containsNodePosition = (metadata: any): metadata is {ui: {nodePosition: {x: number; y: number}}} =>
    metadata?.ui?.nodePosition !== undefined;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export const isNodePositioned = (metadata: any): boolean =>
    metadata?.ui?.nodePosition !== undefined || metadata?.ui?.chainAlignedPosition !== undefined;

function getParentDispatcherId(nodeData: NodeDataType): string | undefined {
    return (
        nodeData.conditionData?.conditionId ||
        nodeData.loopData?.loopId ||
        nodeData.mapData?.mapId ||
        nodeData.branchData?.branchId ||
        nodeData.parallelData?.parallelId ||
        nodeData.eachData?.eachId ||
        nodeData.forkJoinData?.forkJoinId ||
        nodeData.onErrorData?.onErrorId ||
        nodeData.taskDispatcherId
    );
}

function bottomGhostIdForDispatcherTask(componentName: string, taskDispatcherId: string): string {
    if (componentName === 'fork-join') {
        return `${taskDispatcherId}-forkJoin-bottom-ghost`;
    }

    if (componentName === 'on-error') {
        return `${taskDispatcherId}-onError-bottom-ghost`;
    }

    return `${taskDispatcherId}-${componentName}-bottom-ghost`;
}

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
            data.mapData?.mapId === dispatcherId ||
            data.branchData?.branchId === dispatcherId ||
            data.eachData?.eachId === dispatcherId ||
            data.parallelData?.parallelId === dispatcherId ||
            data.forkJoinData?.forkJoinId === dispatcherId ||
            data.onErrorData?.onErrorId === dispatcherId;

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

export function constrainOnErrorGhostsCrossAxis(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((ghostNode) => {
        const ghostData = ghostNode.data as NodeDataType;

        if (!ghostData.onErrorId) {
            return;
        }

        const isTopGhost = ghostNode.type === 'taskDispatcherTopGhostNode';
        const isBottomGhost = ghostNode.type === 'taskDispatcherBottomGhostNode';

        if (!isTopGhost && !isBottomGhost) {
            return;
        }

        const onErrorNode = allNodes.find((node) => node.id === ghostData.onErrorId);

        if (!onErrorNode) {
            return;
        }

        ghostNode.position = {
            ...ghostNode.position,
            [crossAxis]: onErrorNode.position[crossAxis],
        };
    });
}

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

        const dispatcherId =
            sourceData.branchId ||
            ((sourceData as Record<string, unknown>).parallelId as string | undefined) ||
            ((sourceData as Record<string, unknown>).forkJoinId as string | undefined);

        if (!dispatcherId) {
            return;
        }

        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || (targetNode.type === 'placeholder' && !isMiddleCase)) {
            return;
        }

        const dispatcherNode = allNodes.find((node) => node.id === dispatcherId);
        const dispatcherCenterCross = dispatcherNode
            ? dispatcherNode.position[crossAxis] + crossAxisSize / 2
            : sourceNode.position[crossAxis] + CLUSTER_ELEMENT_NODE_WIDTH / 2;

        const targetCross = dispatcherCenterCross - crossAxisSize / 2;

        if (isMiddleCase) {
            targetNode.position = {
                ...targetNode.position,
                [crossAxis]: targetCross,
            };

            alignCaseChainNodes(allNodes, edges, targetNode, targetCross, crossAxis);
        }
    });
}

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

export function alignDispatcherGhostsCrossAxis(allNodes: Node[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((ghostNode) => {
        if (ghostNode.type !== 'taskDispatcherTopGhostNode' && ghostNode.type !== 'taskDispatcherBottomGhostNode') {
            return;
        }

        const ghostData = ghostNode.data as NodeDataType;

        if (ghostData.branchId || ghostData.conditionId || ghostData.onErrorId) {
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

export function separateOverlappingConditionChildren(allNodes: Node[], edges: Edge[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((conditionNode) => {
        const conditionData = conditionNode.data as NodeDataType;

        if (conditionData.componentName !== 'condition') {
            return;
        }

        const parentDispatcherId = conditionData.conditionData?.conditionId || conditionData.branchData?.branchId;

        if (!parentDispatcherId) {
            return;
        }

        const parentCondition = allNodes.find((node) => node.id === parentDispatcherId);

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

                    chainNodeId = edges.find((chainEdge) => chainEdge.source === chainBottomGhostId)?.target || '';
                } else {
                    chainNodeId = edges.find((chainEdge) => chainEdge.source === chainNodeId)?.target || '';
                }
            }
        }
    });
}

export function separateOverlappingOnErrorChildren(allNodes: Node[], edges: Edge[], crossAxis: 'x' | 'y'): void {
    allNodes.forEach((onErrorNode) => {
        const onErrorNodeData = onErrorNode.data as NodeDataType;

        if (onErrorNodeData.componentName !== 'on-error') {
            return;
        }

        const parentOnErrorId = onErrorNodeData.onErrorData?.onErrorId;

        if (!parentOnErrorId) {
            return;
        }

        const parentOnError = allNodes.find((node) => node.id === parentOnErrorId);

        if (!parentOnError) {
            return;
        }

        const onErrorId = onErrorNode.id;
        const topGhostId = `${onErrorId}-onError-top-ghost`;
        const onErrorCross = onErrorNode.position[crossAxis];
        const parentCross = parentOnError.position[crossAxis];
        const onErrorToParent = parentCross - onErrorCross;

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
            const onErrorToChild = childCross - onErrorCross;

            const sameDirection = onErrorToParent * onErrorToChild > 0;
            const atOrBeyondParent = Math.abs(onErrorToChild) >= Math.abs(onErrorToParent) - 1;

            if (!sameDirection || !atOrBeyondParent) {
                continue;
            }

            const targetCross = onErrorCross + onErrorToParent * 0.5;
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

            const componentName = childData.componentName as string;
            const bottomGhostId = bottomGhostIdForDispatcherTask(componentName, childData.taskDispatcherId);
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
                    const chainBottomGhostId = bottomGhostIdForDispatcherTask(
                        chainComponentName,
                        chainNodeData.taskDispatcherId
                    );

                    chainNodeId = edges.find((chainEdge) => chainEdge.source === chainBottomGhostId)?.target || '';
                } else {
                    chainNodeId = edges.find((chainEdge) => chainEdge.source === chainNodeId)?.target || '';
                }
            }
        }
    });
}

function getGhostOrPlaceholderMainAxisSize(nodeType: string | undefined, mainAxis: 'x' | 'y'): number {
    const GHOST_RENDERED_THICKNESS = 2;

    if (nodeType === 'taskDispatcherTopGhostNode' || nodeType === 'taskDispatcherBottomGhostNode') {
        return GHOST_RENDERED_THICKNESS;
    }

    return mainAxis === 'x' ? CLUSTER_ELEMENT_NODE_WIDTH : PLACEHOLDER_NODE_HEIGHT;
}

export function centerDispatcherPlaceholdersOnMainAxis(allNodes: Node[], edges: Edge[], mainAxis: 'x' | 'y'): void {
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

function isBranchSimple(firstNodeId: string, bottomGhostId: string, allNodes: Node[], edges: Edge[]): boolean {
    let currentNodeId = firstNodeId;

    while (currentNodeId) {
        const currentNode = allNodes.find((node) => node.id === currentNodeId);

        if (!currentNode) {
            return false;
        }

        if (currentNode.type === 'placeholder') {
            return false;
        }

        const currentData = currentNode.data as NodeDataType;

        if (currentData.taskDispatcher) {
            return false;
        }

        const nextEdge = edges.find((edge) => edge.source === currentNodeId);

        if (!nextEdge) {
            return false;
        }

        if (nextEdge.target === bottomGhostId) {
            return true;
        }

        currentNodeId = nextEdge.target;
    }

    return false;
}

export function pullSimpleConditionChildrenInward(
    allNodes: Node[],
    edges: Edge[],
    options: {
        conditionCaseOffset: number;
        crossAxis: 'x' | 'y';
    }
): void {
    const {conditionCaseOffset, crossAxis} = options;

    const simpleConditions = new Set<string>();

    allNodes.forEach((conditionNode) => {
        const conditionData = conditionNode.data as NodeDataType;

        if (conditionData.componentName !== 'condition') {
            return;
        }

        const conditionId = conditionNode.id;

        const topGhostId = `${conditionId}-condition-top-ghost`;
        const bottomGhostId = `${conditionId}-condition-bottom-ghost`;

        const branchStartEdges = edges.filter(
            (edge) =>
                edge.source === topGhostId &&
                edge.sourceHandle &&
                (edge.sourceHandle.endsWith('-left') || edge.sourceHandle.endsWith('-right'))
        );

        const allBranchesSimple = branchStartEdges.every((startEdge) =>
            isBranchSimple(startEdge.target, bottomGhostId, allNodes, edges)
        );

        if (allBranchesSimple && branchStartEdges.length === 2) {
            simpleConditions.add(conditionId);
        }
    });

    edges.forEach((edge) => {
        if (!edge.sourceHandle) {
            return;
        }

        const isLeftCase = edge.sourceHandle.endsWith('-left');
        const isRightCase = edge.sourceHandle.endsWith('-right');

        if (!isLeftCase && !isRightCase) {
            return;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode || sourceNode.type !== 'taskDispatcherTopGhostNode') {
            return;
        }

        const sourceData = sourceNode.data as NodeDataType;

        if (!sourceData.conditionId || !simpleConditions.has(sourceData.conditionId)) {
            return;
        }

        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || targetNode.type === 'placeholder') {
            return;
        }

        const conditionNode = allNodes.find((node) => node.id === sourceData.conditionId);

        if (!conditionNode) {
            return;
        }

        const conditionCross = conditionNode.position[crossAxis];

        const expectedCross = isLeftCase ? conditionCross - conditionCaseOffset : conditionCross + conditionCaseOffset;
        const currentCross = targetNode.position[crossAxis];

        const isFartherThanExpected = isLeftCase ? currentCross < expectedCross : currentCross > expectedCross;

        if (!isFartherThanExpected) {
            return;
        }

        targetNode.position = {
            ...targetNode.position,
            [crossAxis]: expectedCross,
        };

        alignCaseChainNodes(allNodes, edges, targetNode, expectedCross, crossAxis);
    });
}

export function pullSimpleOnErrorChildrenInward(
    allNodes: Node[],
    edges: Edge[],
    options: {
        conditionCaseOffset: number;
        crossAxis: 'x' | 'y';
    }
): void {
    const {conditionCaseOffset, crossAxis} = options;

    const simpleOnErrors = new Set<string>();

    allNodes.forEach((onErrorNode) => {
        const onErrorNodeData = onErrorNode.data as NodeDataType;

        if (onErrorNodeData.componentName !== 'on-error') {
            return;
        }

        const onErrorId = onErrorNode.id;

        const topGhostId = `${onErrorId}-onError-top-ghost`;
        const bottomGhostId = `${onErrorId}-onError-bottom-ghost`;

        const branchStartEdges = edges.filter(
            (edge) =>
                edge.source === topGhostId &&
                edge.sourceHandle &&
                (edge.sourceHandle.endsWith('-left') || edge.sourceHandle.endsWith('-right'))
        );

        const allBranchesSimple = branchStartEdges.every((startEdge) =>
            isBranchSimple(startEdge.target, bottomGhostId, allNodes, edges)
        );

        if (allBranchesSimple && branchStartEdges.length === 2) {
            simpleOnErrors.add(onErrorId);
        }
    });

    edges.forEach((edge) => {
        if (!edge.sourceHandle) {
            return;
        }

        const isLeftCase = edge.sourceHandle.endsWith('-left');
        const isRightCase = edge.sourceHandle.endsWith('-right');

        if (!isLeftCase && !isRightCase) {
            return;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode || sourceNode.type !== 'taskDispatcherTopGhostNode') {
            return;
        }

        const sourceData = sourceNode.data as NodeDataType;

        if (!sourceData.onErrorId || !simpleOnErrors.has(sourceData.onErrorId)) {
            return;
        }

        const targetNode = allNodes.find((node) => node.id === edge.target);

        if (!targetNode || targetNode.type === 'placeholder') {
            return;
        }

        const onErrorDispatcherNode = allNodes.find((node) => node.id === sourceData.onErrorId);

        if (!onErrorDispatcherNode) {
            return;
        }

        const onErrorCross = onErrorDispatcherNode.position[crossAxis];

        const expectedCross = isLeftCase ? onErrorCross - conditionCaseOffset : onErrorCross + conditionCaseOffset;
        const currentCross = targetNode.position[crossAxis];

        const isFartherThanExpected = isLeftCase ? currentCross < expectedCross : currentCross > expectedCross;

        if (!isFartherThanExpected) {
            return;
        }

        targetNode.position = {
            ...targetNode.position,
            [crossAxis]: expectedCross,
        };

        alignCaseChainNodes(allNodes, edges, targetNode, expectedCross, crossAxis);
    });
}

interface ConditionPlaceholderI {
    conditionCaseOffset: number;
    crossAxis: 'x' | 'y';
}

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

    const nestedFramePadding = conditionCaseOffset / 2;

    let frameChanged = true;

    while (frameChanged) {
        frameChanged = false;

        allNodes.forEach((conditionNode) => {
            const conditionData = conditionNode.data as NodeDataType;

            if (conditionData.componentName !== 'condition') {
                return;
            }

            const conditionId = conditionNode.id;
            const rightPlaceholder = allNodes.find(
                (node) => node.id === `${conditionId}-condition-right-placeholder-0`
            );

            const leftPlaceholder = allNodes.find((node) => node.id === `${conditionId}-condition-left-placeholder-0`);

            if (!rightPlaceholder && !leftPlaceholder) {
                return;
            }

            const descendantIds = new Set<string>();

            collectNestedDispatcherNodes(conditionId, allNodes, descendantIds);

            allNodes.forEach((descendantNode) => {
                if (
                    !descendantIds.has(descendantNode.id) ||
                    descendantNode.type !== 'placeholder' ||
                    descendantNode.id.startsWith(`${conditionId}-condition-`)
                ) {
                    return;
                }

                const descendantCross = descendantNode.position[crossAxis];

                if (rightPlaceholder && descendantCross + nestedFramePadding > rightPlaceholder.position[crossAxis]) {
                    rightPlaceholder.position = {
                        ...rightPlaceholder.position,
                        [crossAxis]: descendantCross + nestedFramePadding,
                    };

                    frameChanged = true;
                }

                if (leftPlaceholder && descendantCross - nestedFramePadding < leftPlaceholder.position[crossAxis]) {
                    leftPlaceholder.position = {
                        ...leftPlaceholder.position,
                        [crossAxis]: descendantCross - nestedFramePadding,
                    };

                    frameChanged = true;
                }
            });
        });
    }
}

interface ShiftConditionBranchContentI {
    crossAxis: 'x' | 'y';
    nodesep: number;
}

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

export function positionOnErrorCasePlaceholders(allNodes: Node[], options: ConditionPlaceholderI): void {
    const {conditionCaseOffset, crossAxis} = options;

    allNodes.forEach((onErrorNode) => {
        const onErrorNodeData = onErrorNode.data as NodeDataType;

        if (onErrorNodeData.componentName !== 'on-error') {
            return;
        }

        const onErrorId = onErrorNode.id;
        const onErrorCross = onErrorNode.position[crossAxis];

        const leftPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-left-placeholder-0`);

        const rightPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-right-placeholder-0`);

        const descendantIds = new Set<string>();

        collectNestedDispatcherNodes(onErrorId, allNodes, descendantIds);

        if (leftPlaceholder) {
            const maxLeftCross = onErrorCross - conditionCaseOffset;

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
            const minRightCross = onErrorCross + conditionCaseOffset;

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

    const nestedFramePadding = conditionCaseOffset / 2;

    let frameChanged = true;

    while (frameChanged) {
        frameChanged = false;

        allNodes.forEach((onErrorNode) => {
            const onErrorNodeData = onErrorNode.data as NodeDataType;

            if (onErrorNodeData.componentName !== 'on-error') {
                return;
            }

            const onErrorId = onErrorNode.id;
            const rightPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-right-placeholder-0`);

            const leftPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-left-placeholder-0`);

            if (!rightPlaceholder && !leftPlaceholder) {
                return;
            }

            const descendantIds = new Set<string>();

            collectNestedDispatcherNodes(onErrorId, allNodes, descendantIds);

            allNodes.forEach((descendantNode) => {
                if (
                    !descendantIds.has(descendantNode.id) ||
                    descendantNode.type !== 'placeholder' ||
                    descendantNode.id.startsWith(`${onErrorId}-onError-`)
                ) {
                    return;
                }

                const descendantCross = descendantNode.position[crossAxis];

                if (rightPlaceholder && descendantCross + nestedFramePadding > rightPlaceholder.position[crossAxis]) {
                    rightPlaceholder.position = {
                        ...rightPlaceholder.position,
                        [crossAxis]: descendantCross + nestedFramePadding,
                    };

                    frameChanged = true;
                }

                if (leftPlaceholder && descendantCross - nestedFramePadding < leftPlaceholder.position[crossAxis]) {
                    leftPlaceholder.position = {
                        ...leftPlaceholder.position,
                        [crossAxis]: descendantCross - nestedFramePadding,
                    };

                    frameChanged = true;
                }
            });
        });
    }
}

interface ShiftOnErrorBranchContentI {
    crossAxis: 'x' | 'y';
    nodesep: number;
}

export function shiftOnErrorBranchContent(allNodes: Node[], options: ShiftOnErrorBranchContentI): void {
    const {crossAxis, nodesep} = options;

    allNodes.forEach((onErrorNode) => {
        const onErrorNodeData = onErrorNode.data as NodeDataType;

        if (onErrorNodeData.componentName !== 'on-error') {
            return;
        }

        const onErrorId = onErrorNode.id;
        const leftPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-left-placeholder-0`);
        const rightPlaceholder = allNodes.find((node) => node.id === `${onErrorId}-onError-right-placeholder-0`);

        const descendantIds = new Set<string>();

        collectNestedDispatcherNodes(onErrorId, allNodes, descendantIds);

        const isInternalDescendant = (nodeId: string) =>
            descendantIds.has(nodeId) && nodeId !== onErrorId && !nodeId.startsWith(`${onErrorId}-onError-`);

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

export function constrainLeftGhostPositions(allNodes: Node[], options: ConstrainLeftGhostI): void {
    const {conditionCaseOffset, crossAxis, direction} = options;

    const LEFT_GHOST_VISUAL_SIZE = 2;
    const PLACEHOLDER_DOM_CROSS_SIZE = 72;
    const handleCenterDifference = PLACEHOLDER_DOM_CROSS_SIZE / 2 - LEFT_GHOST_VISUAL_SIZE / 2;
    const MAX_RING_WIDTH = direction === 'LR' ? conditionCaseOffset : conditionCaseOffset - handleCenterDifference;
    const LEFT_GHOST_PADDING = 20;
    const NESTED_RING_INDENT = 50;
    const lrCenteringOffset = direction === 'LR' ? (72 - LEFT_GHOST_VISUAL_SIZE) / 2 : 0;

    const leftGhostNodes: Node[] = [];
    const descendantCache = new Map<string, Set<string>>();

    allNodes.forEach((node) => {
        if (node.type !== 'taskDispatcherLeftGhostNode') {
            return;
        }

        const dispatcherId = (node.data as NodeDataType).taskDispatcherId;

        if (!dispatcherId) {
            return;
        }

        leftGhostNodes.push(node);

        const descendants = new Set<string>();

        collectNestedDispatcherNodes(dispatcherId, allNodes, descendants);
        descendantCache.set(node.id, descendants);
    });

    leftGhostNodes.sort(
        (ghostA, ghostB) => (descendantCache.get(ghostA.id)?.size || 0) - (descendantCache.get(ghostB.id)?.size || 0)
    );

    for (const leftGhostNode of leftGhostNodes) {
        const taskDispatcherId = (leftGhostNode.data as NodeDataType).taskDispatcherId!;

        const topGhostNode = allNodes.find(
            (node) =>
                node.type === 'taskDispatcherTopGhostNode' &&
                (node.data as NodeDataType).taskDispatcherId === taskDispatcherId
        );

        if (!topGhostNode) {
            continue;
        }

        const cappedCross = topGhostNode.position[crossAxis] - MAX_RING_WIDTH;
        const descendantIds = descendantCache.get(leftGhostNode.id) || new Set();

        let leftmostContentCross = Infinity;
        let leftmostChildGhostCross = Infinity;

        allNodes.forEach((node) => {
            if (!descendantIds.has(node.id) || node.id === leftGhostNode.id) {
                return;
            }

            if (node.type === 'taskDispatcherLeftGhostNode') {
                leftmostChildGhostCross = Math.min(leftmostChildGhostCross, node.position[crossAxis]);
            } else {
                leftmostContentCross = Math.min(leftmostContentCross, node.position[crossAxis]);
            }
        });

        const contentRequired =
            leftmostContentCross === Infinity
                ? Infinity
                : leftmostContentCross - LEFT_GHOST_PADDING - lrCenteringOffset;

        const nestingRequired =
            leftmostChildGhostCross === Infinity
                ? Infinity
                : leftmostChildGhostCross - NESTED_RING_INDENT - lrCenteringOffset;

        const minRequiredCross = Math.min(
            contentRequired === Infinity && nestingRequired === Infinity ? cappedCross : Infinity,
            contentRequired,
            nestingRequired
        );

        leftGhostNode.position = {
            ...leftGhostNode.position,
            [crossAxis]: Math.min(cappedCross, minRequiredCross),
        };
    }
}

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
                x: (rawSavedPosition.x ?? dagrePosition.x) + (crossAxis === 'x' ? crossAxisShift : 0),
                y: (rawSavedPosition.y ?? dagrePosition.y) + (crossAxis === 'y' ? crossAxisShift : 0),
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

                    const parentDispatcherId =
                        nodeData.conditionData?.conditionId ||
                        nodeData.loopData?.loopId ||
                        nodeData.mapData?.mapId ||
                        nodeData.branchData?.branchId ||
                        nodeData.eachData?.eachId ||
                        nodeData.parallelData?.parallelId ||
                        nodeData.forkJoinData?.forkJoinId ||
                        nodeData.onErrorData?.onErrorId;

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

                    if (nodeData.taskDispatcher && nodeData.taskDispatcherId) {
                        dispatcherDeltas.set(allNodes[nodeIndex].id, delta);
                    }
                }
            }
        }
    }

    return dispatcherDeltas;
}

export function adjustBottomGhostForMovedChildren(
    allNodes: Node[],
    edges: Edge[],
    mainAxis: 'x' | 'y',
    direction: LayoutDirectionType,
    savedDispatcherDeltas: Map<string, {x: number; y: number}> = new Map()
): void {
    const propagatedShifts = new Map<string, number>();

    const sortedDeltas = [...savedDispatcherDeltas.entries()].sort(
        (firstEntry, secondEntry) => secondEntry[1][mainAxis] - firstEntry[1][mainAxis]
    );

    for (const [dispatcherId, delta] of sortedDeltas) {
        const dispatcherNode = allNodes.find((node) => node.id === dispatcherId);

        if (!dispatcherNode) {
            continue;
        }

        const dispatcherData = dispatcherNode.data as NodeDataType;

        if (!containsNodePosition(dispatcherData.metadata)) {
            continue;
        }

        const parentDispatcherId =
            dispatcherData.conditionData?.conditionId ||
            dispatcherData.loopData?.loopId ||
            dispatcherData.mapData?.mapId ||
            dispatcherData.branchData?.branchId ||
            dispatcherData.eachData?.eachId ||
            dispatcherData.parallelData?.parallelId ||
            dispatcherData.forkJoinData?.forkJoinId ||
            dispatcherData.onErrorData?.onErrorId;

        const parentMainAxisDelta = parentDispatcherId
            ? (savedDispatcherDeltas.get(parentDispatcherId)?.[mainAxis] ?? 0)
            : 0;

        const incrementalDelta = delta[mainAxis] - parentMainAxisDelta;

        if (incrementalDelta <= 0) {
            continue;
        }

        const bottomGhost = allNodes.find(
            (node) =>
                node.type === 'taskDispatcherBottomGhostNode' &&
                (node.data as NodeDataType).taskDispatcherId === dispatcherId
        );

        if (!bottomGhost) {
            continue;
        }

        const auxiliaryTypes = new Set([
            'taskDispatcherTopGhostNode',
            'taskDispatcherBottomGhostNode',
            'taskDispatcherLeftGhostNode',
            'placeholder',
        ]);

        const shiftedBottomGhosts = new Set<string>();

        const visited = new Set<string>([bottomGhost.id]);
        const queue: Array<{nodeId: string; afterShiftedGhost: boolean}> = [
            {afterShiftedGhost: false, nodeId: bottomGhost.id},
        ];

        while (queue.length > 0) {
            const {afterShiftedGhost: queueAfterShiftedGhost, nodeId: currentId} = queue.shift()!;

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
                    const auxiliaryDispatcherId = targetData.taskDispatcherId;

                    if (auxiliaryDispatcherId && visited.has(auxiliaryDispatcherId)) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }

                    if (!auxiliaryDispatcherId && !afterShiftedGhost) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }
                } else {
                    if (!afterShiftedGhost) {
                        queue.push({afterShiftedGhost, nodeId: targetNode.id});

                        continue;
                    }
                }

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

    return NODE_HEIGHT / 2 + RANKSEP * BOTTOM_GHOST_MINLEN;
}

function getLRDagreWidth(node: Node): number {
    if (node.type === 'clusterRoot') {
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
    if (node.type === 'clusterRoot') {
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

    const predecessorMap = new Map<string, string>();

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
    }

    const anchored = new Set<string>();
    const skipped = new Set<string>();
    const dispatcherDeltas = new Map<string, {x: number; y: number}>();
    const mainAxis = crossAxis === 'x' ? 'y' : 'x';

    const nodeIndexById = new Map<string, number>();

    for (let nodeIndex = 0; nodeIndex < allNodes.length; nodeIndex++) {
        nodeIndexById.set(allNodes[nodeIndex].id, nodeIndex);
    }

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

            const predecessorIndex = nodeIndexById.get(predecessorId);

            if (predecessorIndex === undefined) {
                continue;
            }

            const predecessorNode = allNodes[predecessorIndex];

            const predecessorData = predecessorNode.data as NodeDataType;
            const predecessorHasPosition = isNodePositioned(predecessorData.metadata);

            if (nodeData.taskDispatcher && !predecessorHasPosition) {
                skipped.add(node.id);

                continue;
            }

            const oldPosition = node.position;
            let newPosition: {x: number; y: number};
            let adjustedMainAxis = false;

            const clusterCrossOffset =
                getClusterRootCrossOffset(node, direction) - getClusterRootCrossOffset(predecessorNode, direction);

            const deltaShift =
                deltaShiftNodes.get(node.id) ??
                (crossAxisOnlyNodes.has(node.id) ? dispatcherDeltas.get(predecessorId) : undefined);

            if (deltaShift) {
                newPosition = {
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                    [mainAxis]: oldPosition[mainAxis] + deltaShift[mainAxis],
                } as {x: number; y: number};

                adjustedMainAxis = true;
            } else if (crossAxisOnlyNodes.has(node.id)) {
                newPosition = {
                    ...oldPosition,
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                };
            } else if (!containsNodePosition(predecessorData.metadata)) {
                newPosition = {
                    ...oldPosition,
                    [crossAxis]: predecessorNode.position[crossAxis] + clusterCrossOffset,
                };

                adjustedMainAxis = true;
            } else {
                skipped.add(node.id);

                continue;
            }

            if (adjustedMainAxis) {
                allNodes[nodeIndex] = {
                    ...node,
                    data: {
                        ...nodeData,
                        metadata: {
                            ...nodeData.metadata,
                            ui: {
                                ...nodeData.metadata?.ui,
                                chainAlignedPosition: newPosition,
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
                                    chainAlignedPosition: newPosition,
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

        if (targetData.taskDispatcherId) {
            continue;
        }

        const sourceNode = allNodes.find((node) => node.id === edge.source);

        if (!sourceNode) {
            continue;
        }

        const sourceData = sourceNode.data as NodeDataType;

        if (isNodePositioned(sourceData.metadata)) {
            const gap = computeMainAxisGapToPlaceholder(sourceNode, direction);

            const crossAxisCenteringAdjustment =
                direction === 'LR' ? (CLUSTER_ELEMENT_NODE_WIDTH - PLACEHOLDER_NODE_HEIGHT) / 2 : 0;

            targetNode.position = {
                [crossAxis]: sourceNode.position[crossAxis] + crossAxisCenteringAdjustment,
                [mainAxis]: sourceNode.position[mainAxis] + gap,
            } as {x: number; y: number};

            continue;
        }

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

function getNodeMainAxisSize(node: Node, mainAxis: 'x' | 'y'): number {
    if (node.type === 'taskDispatcherTopGhostNode' || node.type === 'taskDispatcherBottomGhostNode') {
        return 2;
    }

    if (node.type === 'placeholder') {
        return mainAxis === 'y' ? PLACEHOLDER_NODE_HEIGHT : CLUSTER_ELEMENT_NODE_WIDTH;
    }

    return mainAxis === 'y' ? NODE_HEIGHT : NODE_WIDTH;
}

function collectChainMainNodeIds(
    startNodeId: string,
    bottomGhostId: string,
    allNodes: Node[],
    edges: Edge[]
): string[] {
    const nodeIds: string[] = [];
    let currentNodeId = startNodeId;

    while (currentNodeId) {
        if (currentNodeId === bottomGhostId) {
            break;
        }

        const currentNode = allNodes.find((node) => node.id === currentNodeId);

        if (!currentNode || currentNode.type === 'taskDispatcherBottomGhostNode') {
            break;
        }

        nodeIds.push(currentNodeId);

        const currentData = currentNode.data as NodeDataType;

        if (currentData.taskDispatcher && currentData.taskDispatcherId) {
            const componentName = currentData.componentName as string;
            const nestedBottomGhostId = bottomGhostIdForDispatcherTask(componentName, currentData.taskDispatcherId);
            const nestedBottomGhostEdge = edges.find((edge) => edge.source === nestedBottomGhostId);

            currentNodeId = nestedBottomGhostEdge?.target || '';
        } else {
            const nextEdge = edges.find((edge) => edge.source === currentNodeId);

            currentNodeId = nextEdge?.target || '';
        }
    }

    return nodeIds;
}

export function centerDispatcherChildrenOnMainAxis(allNodes: Node[], edges: Edge[], mainAxis: 'x' | 'y'): void {
    const GHOST_SIZE = 2;

    allNodes.forEach((topGhost) => {
        if (topGhost.type !== 'taskDispatcherTopGhostNode') {
            return;
        }

        const bottomGhostId = topGhost.id.replace('-top-ghost', '-bottom-ghost');
        const bottomGhost = allNodes.find((node) => node.id === bottomGhostId);

        if (!bottomGhost) {
            return;
        }

        const availableTop = topGhost.position[mainAxis] + GHOST_SIZE;
        const availableBottom = bottomGhost.position[mainAxis];

        if (availableBottom <= availableTop) {
            return;
        }

        const outgoingEdges = edges.filter((edge) => edge.source === topGhost.id);

        for (const outgoingEdge of outgoingEdges) {
            const firstChild = allNodes.find((node) => node.id === outgoingEdge.target);

            if (!firstChild || firstChild.type === 'placeholder' || firstChild.type === 'taskDispatcherLeftGhostNode') {
                continue;
            }

            const chainMainNodeIds = collectChainMainNodeIds(firstChild.id, bottomGhostId, allNodes, edges);

            if (chainMainNodeIds.length === 0) {
                continue;
            }

            const hasAdjustedPosition = chainMainNodeIds.some((nodeId) => {
                const node = allNodes.find((existingNode) => existingNode.id === nodeId);

                return node && isNodePositioned((node.data as NodeDataType).metadata);
            });

            if (hasAdjustedPosition) {
                continue;
            }

            const allChainNodeIds = new Set(chainMainNodeIds);

            for (const nodeId of chainMainNodeIds) {
                const node = allNodes.find((existingNode) => existingNode.id === nodeId);
                const nodeData = node?.data as NodeDataType;

                if (nodeData?.taskDispatcher && nodeData.taskDispatcherId) {
                    collectNestedDispatcherNodes(nodeData.taskDispatcherId, allNodes, allChainNodeIds);
                }
            }

            let chainMin = Infinity;
            let chainMax = -Infinity;

            for (const nodeId of allChainNodeIds) {
                const node = allNodes.find((existingNode) => existingNode.id === nodeId);

                if (!node) {
                    continue;
                }

                const position = node.position[mainAxis];
                const size = getNodeMainAxisSize(node, mainAxis);

                chainMin = Math.min(chainMin, position);
                chainMax = Math.max(chainMax, position + size);
            }

            if (!isFinite(chainMin)) {
                continue;
            }

            const chainCenter = (chainMin + chainMax) / 2;
            const availableCenter = (availableTop + availableBottom) / 2;
            const shift = availableCenter - chainCenter;

            if (Math.abs(shift) < 1) {
                continue;
            }

            for (const nodeId of allChainNodeIds) {
                const node = allNodes.find((existingNode) => existingNode.id === nodeId);

                if (node) {
                    node.position = {
                        ...node.position,
                        [mainAxis]: node.position[mainAxis] + shift,
                    };
                }
            }
        }
    });
}
