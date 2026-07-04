// Ported from client/src/pages/platform/workflow-editor/utils/layoutUtils.tsx (getLayoutElements,
// plus the getRenderedMainAxisSize helper). Positions the full dispatcher graph with dagre and
// then runs the post-dagre constraint pipeline.
//
// Adaptations:
//   - Store-free; constants/types/helpers imported locally.
//   - Cluster-element-specific sizing (calculateNodeWidth / getHandlePosition from the
//     cluster-element editor) is dropped: clusterRoot nodes are sized like normal
//     NODE_WIDTH x NODE_HEIGHT nodes. The marketing renderer never authors cluster elements,
//     so the `node.data.clusterElements` branches stay but are inert.
//   - Uses the async import('@dagrejs/dagre') pattern; direction defaults to 'TB'.
//   - calculateNodeHeight is imported from nodeFactory.

import dagre from '@dagrejs/dagre';
import {Edge, Node} from '@xyflow/react';

import {LayoutDirectionType, NODE_WIDTH, PLACEHOLDER_NODE_HEIGHT} from './constants';
import {getCrossAxis, getCrossAxisNodeSize} from './directionUtils';
import {calculateNodeHeight} from './nodeFactory';
import {
    adjustBottomGhostForMovedChildren,
    alignBranchCaseChildren,
    alignChainNodesCrossAxis,
    alignDispatcherGhostsCrossAxis,
    alignTrailingPlaceholder,
    applySavedPositions,
    centerDispatcherChildrenOnMainAxis,
    centerDispatcherPlaceholdersOnMainAxis,
    centerLRSmallNodes,
    centerNodesAfterBottomGhost,
    constrainBranchGhostsCrossAxis,
    constrainConditionGhostsCrossAxis,
    constrainLeftGhostPositions,
    constrainOnErrorGhostsCrossAxis,
    positionConditionCasePlaceholders,
    positionOnErrorCasePlaceholders,
    pullSimpleConditionChildrenInward,
    pullSimpleOnErrorChildrenInward,
    separateOverlappingConditionChildren,
    separateOverlappingOnErrorChildren,
    shiftConditionBranchContent,
    shiftOnErrorBranchContent,
} from './postDagreConstraints';

type GetLayoutElementsPropsType = {
    canvasHeight?: number;
    canvasWidth: number;
    direction?: LayoutDirectionType;
    edges: Edge[];
    nodes: Node[];
    savedPositionCrossAxisShift?: number;
};

/**
 * Returns the approximate rendered main-axis size (width in LR mode) for a node.
 * Dagre reports center coordinates; subtracting half this value converts to the
 * top-left position that ReactFlow expects.
 */
function getRenderedMainAxisSize(node: Node, direction: LayoutDirectionType): number {
    if (direction !== 'LR') {
        return 0;
    }

    if (node.type === 'taskDispatcherLeftGhostNode') {
        return 16;
    }

    if (node.type === 'taskDispatcherTopGhostNode' || node.type === 'taskDispatcherBottomGhostNode') {
        return 2;
    }

    if (node.type === 'clusterRoot') {
        const hasClusterElements =
            node.data.clusterElements &&
            Object.entries(node.data.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        return hasClusterElements ? 240 : 72;
    }

    return 72;
}

export const getLayoutElements = async ({
    canvasHeight,
    canvasWidth,
    direction = 'TB',
    edges,
    nodes,
    savedPositionCrossAxisShift = 0,
}: GetLayoutElementsPropsType): Promise<{edges: Edge[]; nodes: Node[]}> => {
    const dagreGraph = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

    const effectiveDirection = direction;

    dagreGraph.setGraph({
        nodesep: 50,
        rankdir: effectiveDirection,
    });

    nodes.forEach((node) => {
        let height = calculateNodeHeight(node);
        let width = NODE_WIDTH;

        const isGhostNode =
            node.type === 'taskDispatcherTopGhostNode' ||
            node.type === 'taskDispatcherBottomGhostNode' ||
            node.type === 'taskDispatcherLeftGhostNode';

        if (effectiveDirection === 'LR') {
            if (node.type === 'taskDispatcherTopGhostNode') {
                width = 0;
            } else if (isGhostNode) {
                width = PLACEHOLDER_NODE_HEIGHT;
            } else if (node.type === 'placeholder') {
                width = height;
            } else if (node.type === 'clusterRoot') {
                const nodeHasClusterElements =
                    node.data.clusterElements &&
                    Object.entries(node.data.clusterElements).some(
                        ([, value]) =>
                            value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                    );

                width = nodeHasClusterElements ? 292 : 120;
            } else {
                width = 120;
            }

            height = NODE_WIDTH;
        }

        dagreGraph.setNode(node.id, {height, width});
    });

    edges.forEach((edge) => {
        if (edge.target.includes('bottom-ghost')) {
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 2});
        } else if (edge.target.includes('top-ghost')) {
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 1});
        } else {
            const sourceNode = nodes.find((node) => node.id === edge.source);

            const hasValidClusterElements =
                sourceNode?.data.clusterElements &&
                Object.entries(sourceNode.data.clusterElements).some(
                    ([, value]) =>
                        value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                );

            let edgeLength = 1;

            if (hasValidClusterElements && effectiveDirection !== 'LR') {
                edgeLength = 2;
            }

            // Edges from top ghosts to content children need extra space
            // so the edge add-button (+) has room between the case label and the node.
            if (sourceNode?.type === 'taskDispatcherTopGhostNode') {
                edgeLength = 2;
            }

            dagreGraph.setEdge(edge.source, edge.target, {minlen: edgeLength});
        }
    });

    dagre.layout(dagreGraph, {disableOptimalOrderHeuristic: true});

    const crossAxis = getCrossAxis(direction);
    const crossAxisSize = getCrossAxisNodeSize(direction);

    const canvasCrossDimension = direction === 'LR' && canvasHeight ? canvasHeight : canvasWidth;

    const triggerCrossHalf = direction === 'LR' ? NODE_WIDTH / 2 : 72 / 2;

    const canvasCenteringOffset = canvasCrossDimension / 2 - dagreGraph.node(nodes[0].id)[crossAxis] - triggerCrossHalf;

    const allNodes = nodes.map((node) => {
        const dagreNode = dagreGraph.node(node.id);
        let crossAxisPosition = dagreNode[crossAxis] + canvasCenteringOffset;

        const hasValidClusterElements =
            node.data.clusterElements &&
            Object.entries(node.data.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        if (hasValidClusterElements && node.data.clusterRoot && direction === 'TB') {
            crossAxisPosition -= 85;
        }

        const mainAxis = direction === 'TB' ? 'y' : 'x';
        const mainAxisPosition = dagreNode[mainAxis] - getRenderedMainAxisSize(node, direction) / 2;

        if (hasValidClusterElements && node.data.clusterRoot && direction === 'LR') {
            crossAxisPosition -= 23;
        }

        return {
            ...node,
            position: {
                [crossAxis]: crossAxisPosition,
                [mainAxis]: mainAxisPosition,
            } as {x: number; y: number},
        };
    });

    // Post-dagre constraint pipeline
    const nodesep = 50;
    const conditionCaseOffset = (crossAxisSize + nodesep) / 2;

    constrainConditionGhostsCrossAxis(allNodes, crossAxis);
    constrainOnErrorGhostsCrossAxis(allNodes, crossAxis);
    constrainBranchGhostsCrossAxis(allNodes, crossAxis);
    alignBranchCaseChildren(allNodes, edges, crossAxis, crossAxisSize);
    centerNodesAfterBottomGhost(allNodes, edges, {crossAxis, crossAxisSize, direction});
    alignDispatcherGhostsCrossAxis(allNodes, crossAxis);
    separateOverlappingConditionChildren(allNodes, edges, crossAxis);
    separateOverlappingOnErrorChildren(allNodes, edges, crossAxis);
    pullSimpleConditionChildrenInward(allNodes, edges, {conditionCaseOffset, crossAxis});
    pullSimpleOnErrorChildrenInward(allNodes, edges, {conditionCaseOffset, crossAxis});
    positionConditionCasePlaceholders(allNodes, {conditionCaseOffset, crossAxis});
    positionOnErrorCasePlaceholders(allNodes, {conditionCaseOffset, crossAxis});
    shiftConditionBranchContent(allNodes, {crossAxis, nodesep});
    shiftOnErrorBranchContent(allNodes, {crossAxis, nodesep});
    constrainLeftGhostPositions(allNodes, {conditionCaseOffset, crossAxis, direction});

    if (direction === 'LR') {
        centerLRSmallNodes(allNodes, crossAxis);
    }

    const mainAxis = direction === 'TB' ? 'y' : 'x';

    const savedDispatcherDeltas = applySavedPositions(allNodes, crossAxis, savedPositionCrossAxisShift);

    adjustBottomGhostForMovedChildren(allNodes, edges, mainAxis, direction, savedDispatcherDeltas);

    const chainDeltas = alignChainNodesCrossAxis(allNodes, edges, crossAxis, direction, savedDispatcherDeltas);
    const allDispatcherDeltas = new Map([...savedDispatcherDeltas, ...chainDeltas]);

    alignTrailingPlaceholder(allNodes, edges, crossAxis, direction, allDispatcherDeltas);

    centerDispatcherPlaceholdersOnMainAxis(allNodes, edges, mainAxis);

    if (direction === 'TB') {
        centerDispatcherChildrenOnMainAxis(allNodes, edges, mainAxis);
    }

    const sourceEdgeMap = new Map<string, Edge[]>();

    // Sort edges to prioritize task connections over ghost connections
    const sortedEdges = [...edges].sort((firstEdge, secondEdge) => {
        const isFirstEdgeToAuxiliaryNode =
            firstEdge.target.includes('ghost') || firstEdge.target.includes('placeholder');

        const isSecondEdgeToAuxiliaryNode =
            secondEdge.target.includes('ghost') || secondEdge.target.includes('placeholder');

        if (isFirstEdgeToAuxiliaryNode && !isSecondEdgeToAuxiliaryNode) {
            return 1;
        }

        if (!isFirstEdgeToAuxiliaryNode && isSecondEdgeToAuxiliaryNode) {
            return -1;
        }

        return 0;
    });

    // Group edges by source
    sortedEdges.forEach((edge) => {
        if (!sourceEdgeMap.has(edge.source)) {
            sourceEdgeMap.set(edge.source, []);
        }

        sourceEdgeMap.get(edge.source)?.push(edge);
    });

    const filteredEdges: Edge[] = [];

    // Filter edges so that only one edge is kept for each source node
    sourceEdgeMap.forEach((sourceEdges, source) => {
        const sourceNode = allNodes.find((node) => node.id === source);

        if (sourceEdges.length === 0 || !sourceNode) {
            return;
        }

        const multipleEdgesAllowed = [
            {
                condition: sourceNode.type === 'taskDispatcherTopGhostNode',
            },
            {
                condition: sourceNode.type === 'taskDispatcherBottomGhostNode',
            },
            {
                condition: sourceNode.data.clusterRoot,
            },
            {
                condition: sourceNode.data.componentName === 'branch',
            },
            {
                condition: sourceNode.data.componentName === 'fork-join',
            },
        ];

        if (multipleEdgesAllowed.some(({condition}) => condition)) {
            filteredEdges.push(...sourceEdges);
        } else {
            filteredEdges.push(sourceEdges[0]);
        }
    });

    let resultEdges = filteredEdges.reduce(
        (uniqueEdges: {edges: Edge[]; map: Map<string, boolean>}, edge: Edge) => {
            const {source, target} = edge;

            const targetHandle = edge.targetHandle ? `-${edge.targetHandle}` : '';
            const sourceHandle = edge.sourceHandle ? `-${edge.sourceHandle}` : '';

            const edgeKey = `${source}=>${target}${targetHandle}${sourceHandle}`;

            if (!uniqueEdges.map.has(edgeKey)) {
                uniqueEdges.map.set(edgeKey, true);

                uniqueEdges.edges.push(edge);
            }

            return uniqueEdges;
        },
        {edges: [], map: new Map<string, boolean>()}
    ).edges;

    // Remove edges that reference non-existent nodes
    const nodeIds = new Set(allNodes.map((node) => node.id));

    resultEdges = resultEdges.filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target));

    return {edges: resultEdges, nodes: allNodes};
};
