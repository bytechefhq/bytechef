import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    ROOT_CLUSTER_WIDTH,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {
    BranchCaseType,
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    LoopChildTasksType,
    NodeDataType,
    ParallelChildTasksType,
} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon, PlayIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {calculateNodeWidth, getHandlePosition} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {getConditionBranchSide} from './createConditionEdges';
import {getForkJoinBranchSide} from './createForkJoinEdges';
import {getCrossAxis, getCrossAxisNodeSize} from './directionUtils';
import {
    adjustBottomGhostForMovedChildren,
    alignBranchCaseChildren,
    alignChainNodesCrossAxis,
    alignDispatcherGhostsCrossAxis,
    alignTrailingPlaceholder,
    applySavedPositions,
    centerLRSmallNodes,
    centerNodesAfterBottomGhost,
    constrainBranchGhostsCrossAxis,
    constrainConditionGhostsCrossAxis,
    constrainLeftGhostPositions,
    containsNodePosition,
    positionConditionCasePlaceholders,
    shiftConditionBranchContent,
} from './postDagreConstraints';
import {TASK_DISPATCHER_CONFIG, getParentTaskDispatcherTask} from './taskDispatcherConfig';

let dagre: typeof import('@dagrejs/dagre') | null = null;

const loadDagre = async () => {
    if (!dagre) {
        dagre = await import('@dagrejs/dagre');
    }

    return dagre;
};

export const calculateNodeHeight = (node: Node) => {
    const isTopGhostNode = node.type === 'taskDispatcherTopGhostNode';
    const isBottomGhostNode = node.type === 'taskDispatcherBottomGhostNode';
    const isLeftGhostNode = node.type === 'taskDispatcherLeftGhostNode';
    const isPlaceholderNode = node.type === 'placeholder';
    const isAiAgentNode = node.type === 'aiAgentNode';
    const isGhostNode = isTopGhostNode || isBottomGhostNode || isLeftGhostNode;

    let height = NODE_HEIGHT;
    const aiAgentNodeHeight = 150;

    if (isPlaceholderNode || isGhostNode) {
        height = PLACEHOLDER_NODE_HEIGHT;

        if (isBottomGhostNode) {
            height = 0;
        }
    }

    if (isAiAgentNode) {
        height = aiAgentNodeHeight;
    }

    return height;
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

    if (node.type === 'aiAgentNode') {
        const hasClusterElements =
            node.data.clusterElements &&
            Object.entries(node.data.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        return hasClusterElements ? 240 : 72;
    }

    return 72;
}

export const convertTaskToNode = (
    task: WorkflowTask,
    taskDefinition: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic,
    index: number
): Node => {
    const componentName = task.type.split('/')[0];

    const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(componentName);

    return {
        data: {
            ...task,
            componentName,
            icon: (
                <InlineSVG
                    className="size-9"
                    loader={<ComponentIcon className="size-9 flex-none text-gray-900" />}
                    src={taskDefinition.icon!}
                />
            ),
            operationName: task.type.split('/')[2],
            taskDispatcher: isTaskDispatcher,
            taskDispatcherId: isTaskDispatcher ? task.name : undefined,
            trigger: index === 0,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: task.clusterRoot ? 'aiAgentNode' : 'workflow',
    };
};

interface GetLayoutedElementsProps {
    canvasHeight?: number;
    canvasWidth: number;
    direction?: LayoutDirectionType;
    edges: Edge[];
    isClusterElementsCanvas?: boolean;
    nodes: Node[];
    savedPositionCrossAxisShift?: number;
}

export const getLayoutedElements = async ({
    canvasHeight,
    canvasWidth,
    direction = 'TB',
    edges,
    isClusterElementsCanvas,
    nodes,
    savedPositionCrossAxisShift = 0,
}: GetLayoutedElementsProps) => {
    if (isClusterElementsCanvas) {
        const mainRootNode = nodes.find((node) => node.data.clusterElements && !node.parentId);

        if (!mainRootNode) {
            console.error('Main root node not found');

            return {edges, nodes};
        }

        const placeholderNodes = nodes.filter((node) => node.type === 'placeholder');
        const workflowNodes = nodes.filter((node) => node.type !== 'placeholder');

        if (workflowNodes.length === 0) {
            console.error('Cluster element workflow nodes not found');

            return {edges, nodes: placeholderNodes};
        }

        const mainRootTypesCount = (mainRootNode.data.clusterElementTypesCount as number) || 1;
        const mainRootWidth = calculateNodeWidth(mainRootTypesCount) || ROOT_CLUSTER_WIDTH;
        const canvasCenterX = canvasWidth / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM / 2;

        const positionedNodes: Node[] = [];

        positionedNodes.push({
            ...mainRootNode,
            position: {
                x: canvasCenterX - mainRootWidth / 2,
                y: NODE_HEIGHT,
            },
        });

        const placeholderY = 140;
        const childBaseY = placeholderY + PLACEHOLDER_NODE_HEIGHT + NODE_HEIGHT / 4;
        const horizontalGap = CLUSTER_ELEMENT_NODE_WIDTH + 80;

        const overlapPadding = 20;

        // Labels rendered below node circles extend ~40-60px beyond NODE_HEIGHT.
        // Include this overhang in extent calculations so stacked cluster roots
        // don't overlap labels from sibling subtrees.
        const labelOverhang = 40;

        // Returns the maximum Y extent of the subtree (relative to the parent)
        const positionChildrenOfParent = (parentId: string): number => {
            const children = workflowNodes.filter((node) => node.parentId === parentId && node.id !== parentId);

            if (children.length === 0) {
                return NODE_HEIGHT + labelOverhang;
            }

            const parentTypesCount = (children[0].data.parentClusterRootElementsTypeCount as number) || 1;
            const parentWidth = calculateNodeWidth(parentTypesCount) || ROOT_CLUSTER_WIDTH;
            const parentCenterX = parentWidth / 2;

            const typeGroups = new Map<string, Node[]>();

            for (const child of children) {
                const childType = child.data.clusterElementType as string;

                if (!typeGroups.has(childType)) {
                    typeGroups.set(childType, []);
                }

                typeGroups.get(childType)!.push(child);
            }

            let maxExtentY = NODE_HEIGHT;

            // First pass: position regular (non-cluster-root) children
            for (const [, typeChildren] of typeGroups) {
                const typeIndex = (typeChildren[0].data.clusterElementTypeIndex as number) || 0;

                const handleX = getHandlePosition({
                    handlesCount: parentTypesCount,
                    index: typeIndex,
                    nodeWidth: parentWidth,
                });

                const isRightSide = handleX >= parentCenterX;

                const regularChildren = typeChildren.filter((child) => !child.data.isNestedClusterRoot);

                for (let childIndex = 0; childIndex < regularChildren.length; childIndex++) {
                    const child = regularChildren[childIndex];

                    if (containsNodePosition(child.data.metadata)) {
                        positionedNodes.push({...child, position: child.data.metadata.ui.nodePosition});
                    } else {
                        const firstChildX = handleX - CLUSTER_ELEMENT_NODE_WIDTH / 2;
                        let childX: number;

                        if (childIndex === 0) {
                            childX = firstChildX;
                        } else if (isRightSide) {
                            childX = firstChildX + childIndex * horizontalGap;
                        } else {
                            childX = firstChildX - childIndex * horizontalGap;
                        }

                        positionedNodes.push({...child, position: {x: childX, y: childBaseY}});
                    }

                    const childExtent = positionChildrenOfParent(child.id);

                    maxExtentY = Math.max(maxExtentY, childBaseY + childExtent);
                }
            }

            // Second pass: position cluster root children below all regular children
            const hasRegularChildren = [...typeGroups.values()].some((typeChildren) =>
                typeChildren.some((child) => !child.data.isNestedClusterRoot)
            );

            let clusterRootY = hasRegularChildren
                ? Math.max(childBaseY + NODE_HEIGHT * 1.5, maxExtentY + overlapPadding)
                : childBaseY;

            for (const [, typeChildren] of typeGroups) {
                const typeIndex = (typeChildren[0].data.clusterElementTypeIndex as number) || 0;

                const handleX = getHandlePosition({
                    handlesCount: parentTypesCount,
                    index: typeIndex,
                    nodeWidth: parentWidth,
                });

                const isRightSide = handleX >= parentCenterX;

                const clusterRootChildren = typeChildren.filter((child) => child.data.isNestedClusterRoot);
                const multipleClusterRootChildren = clusterRootChildren.filter(
                    (child) => child.data.multipleClusterElementsNode
                );
                const singleClusterRootChildren = clusterRootChildren.filter(
                    (child) => !child.data.multipleClusterElementsNode
                );

                // Position multiple-instance cluster root children horizontally
                for (let childIndex = 0; childIndex < multipleClusterRootChildren.length; childIndex++) {
                    const child = multipleClusterRootChildren[childIndex];
                    const childTypesCount = (child.data.clusterElementTypesCount as number) || 1;
                    const childWidth = calculateNodeWidth(childTypesCount) || ROOT_CLUSTER_WIDTH;
                    const clusterRootHorizontalGap = childWidth + 80;

                    if (containsNodePosition(child.data.metadata)) {
                        positionedNodes.push({...child, position: child.data.metadata.ui.nodePosition});
                    } else {
                        const firstChildX = handleX - childWidth / 2;
                        let childX: number;

                        if (childIndex === 0) {
                            childX = firstChildX;
                        } else if (isRightSide) {
                            childX = firstChildX + childIndex * clusterRootHorizontalGap;
                        } else {
                            childX = firstChildX - childIndex * clusterRootHorizontalGap;
                        }

                        positionedNodes.push({...child, position: {x: childX, y: clusterRootY}});
                    }

                    const childExtent = positionChildrenOfParent(child.id);

                    maxExtentY = Math.max(maxExtentY, clusterRootY + childExtent);
                }

                // Position single-instance cluster root children vertically
                let singleClusterRootY = clusterRootY;

                for (const child of singleClusterRootChildren) {
                    const childTypesCount = (child.data.clusterElementTypesCount as number) || 1;
                    const childWidth = calculateNodeWidth(childTypesCount) || ROOT_CLUSTER_WIDTH;

                    if (containsNodePosition(child.data.metadata)) {
                        positionedNodes.push({...child, position: child.data.metadata.ui.nodePosition});
                    } else {
                        const childX = handleX - childWidth / 2;

                        positionedNodes.push({...child, position: {x: childX, y: singleClusterRootY}});
                    }

                    const childExtent = positionChildrenOfParent(child.id);

                    singleClusterRootY = Math.max(
                        singleClusterRootY + childBaseY,
                        singleClusterRootY + childExtent + overlapPadding
                    );

                    maxExtentY = Math.max(maxExtentY, singleClusterRootY);
                }

                clusterRootY = Math.max(clusterRootY, singleClusterRootY);
            }

            return maxExtentY;
        };

        positionChildrenOfParent(mainRootNode.id);

        // Resolve horizontal overlaps among sibling nodes
        const siblingGroups = new Map<string, Node[]>();

        for (const node of positionedNodes) {
            const parentId = node.parentId || '';

            if (!siblingGroups.has(parentId)) {
                siblingGroups.set(parentId, []);
            }

            siblingGroups.get(parentId)!.push(node);
        }

        for (const [, siblings] of siblingGroups) {
            if (siblings.length < 2) {
                continue;
            }

            siblings.sort((nodeA, nodeB) => nodeA.position.x - nodeB.position.x);

            for (let i = 0; i < siblings.length; i++) {
                const nodeA = siblings[i];
                const isClusterRootA = !!nodeA.data.clusterElementTypesCount;
                const widthA = isClusterRootA
                    ? calculateNodeWidth(nodeA.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH
                    : CLUSTER_ELEMENT_NODE_WIDTH;

                for (let j = i + 1; j < siblings.length; j++) {
                    const nodeB = siblings[j];

                    if (containsNodePosition(nodeB.data.metadata)) {
                        continue;
                    }

                    const isClusterRootB = !!nodeB.data.clusterElementTypesCount;

                    // Small circle nodes (72px) have labels that extend ~40px
                    // beyond each side; account for this to prevent label overlap
                    const labelPaddingA = isClusterRootA ? 0 : 40;
                    const labelPaddingB = isClusterRootB ? 0 : 40;

                    const verticalOverlap = Math.abs(nodeA.position.y - nodeB.position.y) < NODE_HEIGHT + labelOverhang;
                    const minX = nodeA.position.x + widthA + labelPaddingA + labelPaddingB + overlapPadding;

                    if (verticalOverlap && nodeB.position.x < minX) {
                        nodeB.position = {...nodeB.position, x: minX};
                    }
                }
            }
        }

        return {
            edges,
            nodes: [...positionedNodes, ...placeholderNodes],
        };
    }

    const dagreModule = await loadDagre();

    const dagreGraph = new dagreModule.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

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
            if (isGhostNode || node.type === 'placeholder') {
                width = height;
            } else if (node.type === 'aiAgentNode') {
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

            dagreGraph.setEdge(edge.source, edge.target, {minlen: edgeLength});
        }
    });

    dagreModule.layout(dagreGraph, {disableOptimalOrderHeuristic: true});

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
    constrainBranchGhostsCrossAxis(allNodes, crossAxis);
    alignBranchCaseChildren(allNodes, edges, crossAxis, crossAxisSize);
    centerNodesAfterBottomGhost(allNodes, edges, {crossAxis, crossAxisSize, direction});
    alignDispatcherGhostsCrossAxis(allNodes, crossAxis);
    positionConditionCasePlaceholders(allNodes, {conditionCaseOffset, crossAxis});
    shiftConditionBranchContent(allNodes, {crossAxis, nodesep});
    constrainLeftGhostPositions(allNodes, {conditionCaseOffset, crossAxis, direction});

    if (direction === 'LR') {
        centerLRSmallNodes(allNodes, crossAxis);
    }

    const savedDispatcherDeltas = applySavedPositions(allNodes, crossAxis, savedPositionCrossAxisShift);

    const mainAxis = direction === 'TB' ? 'y' : 'x';

    adjustBottomGhostForMovedChildren(allNodes, edges, mainAxis, direction, savedDispatcherDeltas);

    const chainDeltas = alignChainNodesCrossAxis(allNodes, edges, crossAxis, direction, savedDispatcherDeltas);
    const allDispatcherDeltas = new Map([...savedDispatcherDeltas, ...chainDeltas]);

    alignTrailingPlaceholder(allNodes, edges, crossAxis, direction, allDispatcherDeltas);

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

    edges = filteredEdges.reduce(
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

    edges = edges.filter((edge) => nodeIds.has(edge.source) && nodeIds.has(edge.target));

    return {edges, nodes: allNodes};
};

interface CreateEdgeFromTaskDispatcherBottomGhostNodeProps {
    allNodes?: Node[];
    index?: number;
    node: Node;
    tasks?: WorkflowTask[];
}

export const createEdgeFromTaskDispatcherBottomGhostNode = ({
    allNodes = [],
    index = 0,
    node,
    tasks = [],
}: CreateEdgeFromTaskDispatcherBottomGhostNodeProps): Edge | null => {
    const nodeData = node.data as NodeDataType;

    const {taskDispatcherId} = nodeData;

    if (!taskDispatcherId) {
        return null;
    }

    let componentName;

    // Connect to the parent task dispatcher if this is a nested task dispatcher
    if (node.data.isNestedBottomGhost) {
        const parentTaskDispatcher = getParentTaskDispatcherTask(taskDispatcherId, tasks);

        if (!parentTaskDispatcher) {
            return null;
        }

        const taskDispatcherNode = allNodes.find((node) => node.id === taskDispatcherId);

        componentName = parentTaskDispatcher.type.split('/')[0];

        let parentSubtasks: WorkflowTask[] = [];

        switch (componentName) {
            case 'branch': {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        caseKey: (taskDispatcherNode?.data as NodeDataType)?.branchData?.caseKey,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            }
            case 'parallel':
                return null;
            case 'condition':
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    context: {
                        conditionCase:
                            ((taskDispatcherNode?.data as NodeDataType).conditionData?.conditionCase as
                                | 'caseTrue'
                                | 'caseFalse') || CONDITION_CASE_TRUE,
                        taskDispatcherId: parentTaskDispatcher.name,
                    },
                    task: parentTaskDispatcher,
                });

                break;
            case 'fork-join': {
                const branches = parentTaskDispatcher.parameters?.branches || [];

                const branchIndex = branches.findIndex(
                    (branch: WorkflowTask[]) =>
                        Array.isArray(branch) && branch.some((subtask) => subtask.name === taskDispatcherId)
                );

                parentSubtasks = branchIndex !== -1 ? branches[branchIndex] || [] : [];

                break;
            }
            default: {
                parentSubtasks = TASK_DISPATCHER_CONFIG[
                    componentName as keyof typeof TASK_DISPATCHER_CONFIG
                ].getSubtasks({
                    task: parentTaskDispatcher,
                });

                break;
            }
        }

        const currentSubtaskIndex = parentSubtasks.findIndex((subtask) => subtask.name === taskDispatcherId);

        const nextSubtask = parentSubtasks[currentSubtaskIndex + 1];

        if (nextSubtask) {
            const edgeFromNestedBottomGhostToNextSubtask = {
                id: `${node.id}=>${nextSubtask.name}`,
                source: node.id,
                style: EDGE_STYLES,
                target: nextSubtask.name,
                type: 'workflow',
            };

            return edgeFromNestedBottomGhostToNextSubtask;
        }

        const parentTaskDispatcherBottomGhostId = `${parentTaskDispatcher.name}-${componentName}-bottom-ghost`;
        const parentTaskDispatcherBottomGhost = allNodes.find((node) => node.id === parentTaskDispatcherBottomGhostId);

        if (!parentTaskDispatcherBottomGhost) {
            return null;
        }

        let targetHandle = `${parentTaskDispatcherBottomGhostId}-right`;

        if (componentName === 'condition') {
            const branchSide = getConditionBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'fork-join') {
            const branchSide = getForkJoinBranchSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${branchSide}`;
        } else if (componentName === 'branch') {
            const branchSide = getBranchCaseSide(taskDispatcherId, tasks, parentTaskDispatcher.name);

            const handlePosition = branchSide === 'middle' ? 'top' : branchSide;

            targetHandle = `${parentTaskDispatcherBottomGhostId}-${handlePosition}`;
        }

        return {
            id: `${node.id}=>${parentTaskDispatcherBottomGhostId}`,
            source: node.id,
            style: EDGE_STYLES,
            target: parentTaskDispatcherBottomGhostId,
            targetHandle,
            type: 'workflow',
        };
    }

    const subsequentNodes = allNodes.slice(index + 1);

    const nextTaskNodeOutsideTaskDispatcher = subsequentNodes.find((subsequentNode) => {
        if (subsequentNode.type !== 'workflow' && subsequentNode.type !== 'aiAgentNode') {
            return false;
        }

        const subsequentNodeData = subsequentNode.data as NodeDataType;

        if (subsequentNodeData.conditionData && subsequentNodeData.conditionData.conditionId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.loopData && subsequentNodeData.loopData.loopId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.branchData && subsequentNodeData.branchData.branchId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.parallelData && subsequentNodeData.parallelData.parallelId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.eachData && subsequentNodeData.eachData.eachId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.forkJoinData && subsequentNodeData.forkJoinData.forkJoinId === taskDispatcherId) {
            return false;
        }

        for (const task of tasks || []) {
            const componentName = task.type?.split('/')[0];

            if (!TASK_DISPATCHER_NAMES.includes(componentName)) {
                continue;
            }

            const subtasks = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG].getSubtasks({
                getAllSubtasks: true,
                task,
            });

            if (Array.isArray(subtasks) && subtasks.some((subtask) => subtask.name === subsequentNode.id)) {
                return false;
            }
        }

        return true;
    });

    if (nextTaskNodeOutsideTaskDispatcher) {
        return {
            id: `${node.id}=>${nextTaskNodeOutsideTaskDispatcher.id}`,
            source: node.id,
            style: EDGE_STYLES,
            target: nextTaskNodeOutsideTaskDispatcher.id,
            type: 'workflow',
        };
    }

    return {
        id: `${node.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: node.id,
        style: EDGE_STYLES,
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    };
};

/**
 * Determines the target handle side for a branch case based on case position
 */
export function getBranchCaseSide(
    taskDispatcherId: string,
    tasks: WorkflowTask[],
    parentBranchId: string
): 'left' | 'middle' | 'right' {
    const parentBranchTask = tasks?.find((task) => task.name === parentBranchId);

    if (!parentBranchTask) {
        return 'right';
    }

    const defaultCase = {
        key: 'default',
        tasks: parentBranchTask.parameters?.default || [],
    };

    const customCases = (parentBranchTask.parameters?.cases || []).map((caseItem: BranchCaseType) => ({
        key: caseItem.key,
        tasks: caseItem.tasks || [],
    }));

    const allCases = [defaultCase, ...customCases];

    const caseIndex = allCases.findIndex((caseItem) =>
        caseItem.tasks.some((task: WorkflowTask) => task.name === taskDispatcherId)
    );

    if (caseIndex === -1) {
        return 'right';
    }

    const isEvenCount = allCases.length % 2 === 0;

    if (isEvenCount) {
        const halfPoint = allCases.length / 2;

        if (caseIndex < halfPoint) {
            return 'left';
        } else {
            return 'right';
        }
    } else {
        const middleIndex = Math.floor(allCases.length / 2);

        if (caseIndex < middleIndex) {
            return 'left';
        } else if (caseIndex === middleIndex) {
            return 'middle';
        } else {
            return 'right';
        }
    }
}

/**
 * Collects nested tasks for all task dispatchers in the workflow
 */
export function collectTaskDispatcherData(
    task: WorkflowTask,
    branchChildTasks: BranchChildTasksType,
    conditionChildTasks: ConditionChildTasksType,
    eachChildTasks: EachChildTasksType,
    forkJoinChildTasks: ForkJoinChildTasksType,
    loopChildTasks: LoopChildTasksType,
    parallelChildTasks: ParallelChildTasksType
): void {
    const {name, parameters, type} = task;
    const componentName = type.split('/')[0];

    if (!TASK_DISPATCHER_NAMES.includes(componentName)) {
        return;
    }

    if (componentName === 'condition' && parameters) {
        conditionChildTasks[name] = {
            caseFalse: Array.isArray(parameters.caseFalse)
                ? parameters.caseFalse.map((caseFalseSubtask: WorkflowTask) => caseFalseSubtask.name)
                : [],
            caseTrue: Array.isArray(parameters.caseTrue)
                ? parameters.caseTrue.map((caseTrueSubtask: WorkflowTask) => caseTrueSubtask.name)
                : [],
        };
    } else if (componentName === 'loop' && parameters?.iteratee) {
        loopChildTasks[name] = {
            iteratee: Array.isArray(parameters.iteratee)
                ? parameters.iteratee.map((iteratee: WorkflowTask) => iteratee.name)
                : [],
        };
    } else if (componentName === 'branch' && parameters) {
        branchChildTasks[name] = {
            cases: Array.isArray(parameters.cases)
                ? parameters.cases.reduce((acc: {[key: string]: string[]}, caseItem: BranchCaseType) => {
                      const caseKey = caseItem.key;

                      const taskNames = Array.isArray(caseItem.tasks)
                          ? caseItem.tasks.map((task: WorkflowTask) => task.name)
                          : [];

                      acc[caseKey] = taskNames;

                      return acc;
                  }, {})
                : {},
            default: Array.isArray(parameters.default)
                ? parameters.default.map((defaultSubtask: WorkflowTask) => defaultSubtask.name)
                : [],
        };
    } else if (componentName === 'parallel' && parameters?.tasks) {
        parallelChildTasks[name] = {
            tasks: Array.isArray(parameters.tasks) ? parameters.tasks.map((task: WorkflowTask) => task.name) : [],
        };
    } else if (componentName === 'each' && parameters?.iteratee) {
        eachChildTasks[name] = {
            iteratee: parameters.iteratee.name,
        };
    } else if (componentName === 'fork-join') {
        forkJoinChildTasks[name] = {
            branches: Array.isArray(parameters?.branches)
                ? parameters.branches.map((branch: WorkflowTask[]) =>
                      Array.isArray(branch) ? branch.map((task: WorkflowTask) => task.name) : []
                  )
                : [],
        };
    }
}

/**
 * Detects if a task is nested inside a task dispatcher and returns relevant nesting data
 */
interface GetTaskAncestryProps {
    branchChildTasks: BranchChildTasksType;
    conditionChildTasks: ConditionChildTasksType;
    eachChildTasks: EachChildTasksType;
    forkJoinChildTasks: ForkJoinChildTasksType;
    loopChildTasks: LoopChildTasksType;
    parallelChildTasks: ParallelChildTasksType;
    taskName: string;
}

export function getTaskAncestry({
    branchChildTasks,
    conditionChildTasks,
    eachChildTasks,
    forkJoinChildTasks,
    loopChildTasks,
    parallelChildTasks,
    taskName,
}: GetTaskAncestryProps): {nestingData: Record<string, unknown>; isNested: boolean} {
    let isNested = false;
    let nestingData = {};

    for (const [conditionId, conditionCases] of Object.entries(conditionChildTasks)) {
        const conditionCasesList = [
            {taskNames: conditionCases.caseTrue, value: CONDITION_CASE_TRUE},
            {taskNames: conditionCases.caseFalse, value: CONDITION_CASE_FALSE},
        ];

        const matchingCase = conditionCasesList.find((conditionCase) => conditionCase.taskNames.includes(taskName));

        if (matchingCase) {
            nestingData = {
                conditionData: {
                    conditionCase: matchingCase.value,
                    conditionId,
                    index: matchingCase.taskNames.indexOf(taskName),
                },
            };

            isNested = true;

            break;
        }
    }

    if (!isNested) {
        for (const [loopId, loopData] of Object.entries(loopChildTasks)) {
            if (loopData.iteratee.includes(taskName)) {
                nestingData = {
                    loopData: {
                        index: loopData.iteratee.indexOf(taskName),
                        loopId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [branchId, branchData] of Object.entries(branchChildTasks)) {
            if (branchData.default.includes(taskName)) {
                nestingData = {
                    branchData: {
                        branchId,
                        caseKey: 'default',
                        index: branchData.default.indexOf(taskName),
                    },
                };

                isNested = true;

                break;
            }

            for (const [caseKey, caseTasks] of Object.entries(branchData.cases)) {
                if (caseTasks.includes(taskName)) {
                    nestingData = {
                        branchData: {
                            branchId,
                            caseKey,
                            index: caseTasks.indexOf(taskName),
                        },
                    };

                    isNested = true;

                    break;
                }
            }

            if (isNested) {
                break;
            }
        }
    }

    if (!isNested) {
        for (const [parallelId, parallelData] of Object.entries(parallelChildTasks)) {
            if (parallelData.tasks.includes(taskName)) {
                nestingData = {
                    parallelData: {
                        index: parallelData.tasks.indexOf(taskName),
                        parallelId,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [eachId, eachData] of Object.entries(eachChildTasks)) {
            if (eachData.iteratee === taskName) {
                nestingData = {
                    eachData: {
                        eachId,
                        index: 0,
                    },
                };

                isNested = true;

                break;
            }
        }
    }

    if (!isNested) {
        for (const [forkJoinId, forkJoinData] of Object.entries(forkJoinChildTasks)) {
            const forkJoinSubtaskNameBranches = forkJoinData.branches;

            forkJoinSubtaskNameBranches.forEach((branch, branchIndex) => {
                if (isNested) {
                    return;
                }

                const taskIndex = branch.indexOf(taskName);

                if (taskIndex !== -1) {
                    nestingData = {
                        forkJoinData: {
                            branchIndex,
                            forkJoinId,
                            index: taskIndex,
                        },
                    };

                    isNested = true;
                }
            });

            if (isNested) {
                break;
            }
        }
    }

    return {isNested, nestingData};
}

export const createDefaultNodes = (canvasWidth: number, direction: LayoutDirectionType = 'TB'): Node[] => [
    {
        data: {
            componentName: 'manual',
            icon: <PlayIcon className="size-9 text-gray-700" />,
            id: 'manual',
            label: 'Manual',
            name: 'manual',
            operationName: 'manual',
            trigger: true,
            type: 'manual/v1/manual',
            workflowNodeName: 'trigger_1',
        },
        id: 'trigger_1',
        position: direction === 'LR' ? {x: 50, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 50},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: direction === 'LR' ? {x: 150, y: canvasWidth / 2 - 36} : {x: canvasWidth / 2 - 36, y: 150},
        type: 'placeholder',
    },
];

export const createDefaultEdges = (): Edge[] => [
    {
        id: `trigger_1=>${FINAL_PLACEHOLDER_NODE_ID}`,
        source: 'trigger_1',
        target: FINAL_PLACEHOLDER_NODE_ID,
        type: 'placeholder',
    },
];
