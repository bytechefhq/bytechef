import {DIRECTION, NODE_HEIGHT, NODE_WIDTH, PLACEHOLDER_NODE_HEIGHT, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import dagre from '@dagrejs/dagre';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

export const calculateNodeHeight = (node: Node, nodes: Node[], index: number) => {
    const loopPlaceholderNode =
        (node.data.taskDispatcherId as string)?.includes('loop') && node.id.includes('placeholder');

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
    } else if (loopPlaceholderNode) {
        const otherLoopPlaceholderNodes = getOtherLoopPlaceholderNodes(nodes, node);

        height = PLACEHOLDER_NODE_HEIGHT * 2;

        if (otherLoopPlaceholderNodes.length) {
            height = 0;
        }
    } else if (loopGhostNode) {
        height = 0;
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

export const getOtherLoopPlaceholderNodes = (nodes: Node[], node: Node) =>
    nodes.filter((nodeItem) => {
        if (nodeItem.id === node.id) {
            return false;
        }

        if (!nodeItem.data.loopId) {
            return false;
        }

        return nodeItem.data.loopId === node.data.loopId && nodeItem.id !== node.id;
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

    sourceEdgeMap.forEach((sourceEdges, source) => {
        if (sourceEdges.length === 0) {
            return;
        }

        const isConditionTaskNode =
            source.includes('condition') && !source.includes('ghost') && !source.includes('placeholder');

        if (isConditionTaskNode) {
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
