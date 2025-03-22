import {
    DIRECTION,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import dagre from '@dagrejs/dagre';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {findParentConditionId, isConditionNested} from './conditionHelpers';
import {getConditionBranchSide} from './createConditionEdges';
import {findParentLoopId, isLoopNested} from './loopHelpers';

export const calculateNodeHeight = (node: Node, nodes: Node[], index: number) => {
    const loopGhostNode = (node.data.taskDispatcherId as string)?.includes('loop') && node.type?.includes('GhostNode');

    const conditionPlaceholderNode = node.id.includes('condition') && node.id.includes('placeholder');

    let height = NODE_HEIGHT;

    if (conditionPlaceholderNode) {
        height = PLACEHOLDER_NODE_HEIGHT * 2;

        if (node.id.includes('placeholder-0')) {
            const otherConditionCaseNodes = filterConditionCaseNodes(nodes, node);

            if (otherConditionCaseNodes.length) {
                height = 0;
            }
        } else {
            height = PLACEHOLDER_NODE_HEIGHT;
        }

        if (node.id.includes('bottom')) {
            height = PLACEHOLDER_NODE_HEIGHT;
        }
    } else if (loopGhostNode) {
        height = NODE_HEIGHT;
    } else if (!node.data.conditionData && !node.data.loopData) {
        height = NODE_HEIGHT * 1.2;
    }

    if (index === nodes.length - 1) {
        height = 20;
    }

    return height;
};

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
        type: 'workflow',
    };
};

export const filterConditionCaseNodes = (nodes: Node[], node: Node) =>
    nodes.filter((nodeItem) => {
        if (nodeItem.id === node.id) {
            return false;
        }

        if (!nodeItem.data.conditionId || !node.data.conditionCase) {
            return false;
        }

        return (
            nodeItem.data.conditionId === node.data.conditionId &&
            nodeItem.data.conditionCase === node.data.conditionCase &&
            nodeItem.id !== node.id
        );
    });

export const getLayoutedElements = (nodes: Node[], edges: Edge[], canvasWidth: number) => {
    const dagreGraph = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

    dagreGraph.setGraph({rankdir: DIRECTION});

    nodes.forEach((node, index) => {
        const height = calculateNodeHeight(node, nodes, index);

        dagreGraph.setNode(node.id, {height, width: NODE_WIDTH});
    });

    edges.forEach((edge) => {
        dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    const allNodes = nodes.map((node) => ({
        ...node,
        position: {
            x: dagreGraph.node(node.id).x + (canvasWidth / 2 - dagreGraph.node(nodes[0].id).x - 72 / 2),
            y: dagreGraph.node(node.id).y,
        },
    }));

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

        const isSourceTaskDispatcherTopGhostNode = sourceNode.type === 'taskDispatcherTopGhostNode';

        if (isSourceTaskDispatcherTopGhostNode) {
            filteredEdges.push(...sourceEdges);
        } else {
            filteredEdges.push(sourceEdges[0]);
        }
    });

    edges = filteredEdges.reduce(
        (uniqueEdges: {edges: Edge[]; map: Map<string, boolean>}, edge: Edge) => {
            const edgeKey = `${edge.source}=>${edge.target}`;

            if (!uniqueEdges.map.has(edgeKey)) {
                uniqueEdges.map.set(edgeKey, true);
                uniqueEdges.edges.push(edge);
            }

            return uniqueEdges;
        },
        {edges: [], map: new Map<string, boolean>()}
    ).edges;

    return {edges, nodes: allNodes};
};

export const createEdgeFromTaskDispatcherBottomGhostNode = (
    node: Node,
    allNodes: Node[] = [],
    tasks: WorkflowTask[] = [],
    index: number = 0
): Edge | null => {
    const nodeData = node.data as NodeDataType;
    const {taskDispatcherId} = nodeData;

    if (!taskDispatcherId) {
        return null;
    }

    if (taskDispatcherId.includes('condition')) {
        const isNestedCondition = tasks?.length > 0 && isConditionNested(taskDispatcherId, tasks);

        // Special handling for nested conditions - connect to parent's bottom ghost
        if (isNestedCondition) {
            const parentConditionId = findParentConditionId(taskDispatcherId, tasks);

            if (!parentConditionId) {
                return null;
            }

            const parentConditionBottomGhostId = `${parentConditionId}-condition-bottom-ghost`;
            const parentConditionBottomGhost = allNodes.find((node) => node.id === parentConditionBottomGhostId);

            if (!parentConditionBottomGhost) {
                return null;
            }

            const branchSide = getConditionBranchSide(taskDispatcherId, tasks, parentConditionId);

            // Create edge to parent's bottom ghost
            return {
                id: `${node.id}=>${parentConditionBottomGhostId}`,
                source: node.id,
                style: EDGE_STYLES,
                target: parentConditionBottomGhostId,
                targetHandle: `${parentConditionBottomGhostId}-${branchSide}`,
                type: 'workflow',
            };
        }
    } else if (taskDispatcherId.includes('loop')) {
        const isNestedLoop = tasks?.length > 0 && isLoopNested(taskDispatcherId, tasks);

        if (isNestedLoop) {
            const parentLoopId = findParentLoopId(taskDispatcherId, tasks);

            if (!parentLoopId) {
                return null;
            }

            const parentLoopBottomGhostId = `${parentLoopId}-loop-bottom-ghost`;
            const parentLoopBottomGhost = allNodes.find((node) => node.id === parentLoopBottomGhostId);

            if (!parentLoopBottomGhost) {
                return null;
            }

            return {
                id: `${node.id}=>${parentLoopBottomGhostId}`,
                source: node.id,
                style: EDGE_STYLES,
                target: parentLoopBottomGhostId,
                targetHandle: `${parentLoopBottomGhostId}-right`,
                type: 'workflow',
            };
        }
    }

    const subsequentNodes = allNodes.slice(index + 1);

    const nextTaskNodeOutsideTaskDispatcher = subsequentNodes.find((subsequentNode) => {
        if (subsequentNode.type !== 'workflow') {
            return false;
        }

        const subsequentNodeData = subsequentNode.data as NodeDataType;

        if (subsequentNodeData.conditionData && subsequentNodeData.conditionData.conditionId === taskDispatcherId) {
            return false;
        } else if (subsequentNodeData.loopData && subsequentNodeData.loopData.loopId === taskDispatcherId) {
            return false;
        }

        for (const task of tasks || []) {
            if (task.type?.startsWith('condition') && task.parameters) {
                const conditionSubtasks = [...(task.parameters.caseTrue || []), ...(task.parameters.caseFalse || [])];

                if (conditionSubtasks.some((subtask) => subtask.name === subsequentNode.id)) {
                    return false;
                }
            } else if (task.type?.startsWith('loop') && task.parameters?.iteratee) {
                const loopSubtasks: WorkflowTask[] = task.parameters.iteratee;

                if (loopSubtasks.some((subtask) => subtask.name === subsequentNode.id)) {
                    return false;
                }
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
