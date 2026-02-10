import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CLUSTER_ELEMENT_PLACEHOLDER_WIDTH,
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
    DIRECTION,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
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

import {calculateNodeWidth} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {getConditionBranchSide} from './createConditionEdges';
import {getForkJoinBranchSide} from './createForkJoinEdges';
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
    canvasWidth: number;
    edges: Edge[];
    isClusterElementsCanvas?: boolean;
    nodes: Node[];
}

export const getLayoutedElements = async ({
    canvasWidth,
    edges,
    isClusterElementsCanvas,
    nodes,
}: GetLayoutedElementsProps) => {
    const dagreModule = await loadDagre();

    const dagreGraph = new dagreModule.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

    const horizontalGap = isClusterElementsCanvas ? CLUSTER_ELEMENT_NODE_WIDTH * 2 : 50;

    dagreGraph.setGraph({
        nodesep: horizontalGap,
        rankdir: DIRECTION,
    });

    nodes.forEach((node) => {
        let height = NODE_HEIGHT;
        let width = NODE_WIDTH;

        if (isClusterElementsCanvas) {
            if (node.id.includes('placeholder')) {
                width = CLUSTER_ELEMENT_PLACEHOLDER_WIDTH;
            } else if (node.data.isNestedClusterRoot || node.data.clusterRoot) {
                width = calculateNodeWidth(node.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH;
            } else {
                width = CLUSTER_ELEMENT_NODE_WIDTH;
            }
        } else {
            height = calculateNodeHeight(node);
        }

        dagreGraph.setNode(node.id, {height, width});
    });

    edges.forEach((edge) => {
        if (edge.target.includes('bottom-ghost')) {
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 2});
        } else {
            const sourceNode = nodes.find((node) => node.id === edge.source);

            const hasValidClusterElements =
                sourceNode?.data.clusterElements &&
                Object.entries(sourceNode.data.clusterElements).some(
                    ([, value]) =>
                        value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                );

            let edgeLength = 1;

            if (isClusterElementsCanvas) {
                edgeLength = 1.5;
            } else if (hasValidClusterElements) {
                edgeLength = 2;
            }

            dagreGraph.setEdge(edge.source, edge.target, {minlen: edgeLength});
        }
    });

    dagreModule.layout(dagreGraph, {disableOptimalOrderHeuristic: true});

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const containsNodePosition = (metadata: any): metadata is {ui: {nodePosition: {x: number; y: number}}} =>
        metadata?.ui?.nodePosition !== undefined;

    const calculateClusterElementsCenteringOffsets = (
        graphWidth: number,
        canvasWidth: number,
        mainRootDagreNode: {x: number; width: number; y: number}
    ) => {
        const canvasCenterX = canvasWidth / DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM / 2;
        const graphCenterX = graphWidth / 2;
        const mainRootCenterX = mainRootDagreNode.x + mainRootDagreNode.width / 2;

        const rootOffsetX = graphCenterX - mainRootCenterX;
        const canvasOffsetX = canvasCenterX - graphCenterX;

        return {canvasOffsetX, rootOffsetX};
    };

    if (isClusterElementsCanvas) {
        const mainRootNode = nodes.find((node) => node.data.clusterElements && !node.parentId);

        if (!mainRootNode) {
            console.error('Main root node not found');

            return {edges, nodes};
        }

        const placeholderNodes = nodes.filter((node) => node.type === 'placeholder');
        const clusterElementWorkflowNodes = nodes.filter((node) => node.type !== 'placeholder');

        if (clusterElementWorkflowNodes.length === 0) {
            console.error('Cluster element workflow nodes not found');

            return {edges, nodes: placeholderNodes};
        }

        const graphWidth = dagreGraph.graph().width || 0;
        const mainRootDagreNode = dagreGraph.node(mainRootNode.id);

        const offsets = calculateClusterElementsCenteringOffsets(graphWidth, canvasWidth, mainRootDagreNode);

        const positionedClusterElementNodes = clusterElementWorkflowNodes.map((clusterElementNode) => {
            if (clusterElementNode.id === mainRootNode.id) {
                const defaultPosition = {
                    x: mainRootDagreNode.x + offsets.rootOffsetX + offsets.canvasOffsetX,
                    y: NODE_HEIGHT,
                };

                return {
                    ...clusterElementNode,
                    position: defaultPosition,
                };
            }

            const childDagreNode = dagreGraph.node(clusterElementNode.id);

            if (clusterElementNode.parentId === mainRootNode.id) {
                const childNodeLeftEdge = childDagreNode.x - childDagreNode.width / 2;
                let mainRootLeftEdge = mainRootDagreNode.x - mainRootDagreNode.width / 2;

                if (mainRootNode.data.clusterElements && Object.keys(mainRootNode.data.clusterElements).length > 1) {
                    mainRootLeftEdge += offsets.rootOffsetX;
                }

                const topLevelChildRelativeX = childNodeLeftEdge - mainRootLeftEdge;
                const topLevelChildRelativeY = childDagreNode.y - mainRootDagreNode.y;

                const defaultPosition = {
                    x: topLevelChildRelativeX,
                    y: topLevelChildRelativeY,
                };

                return {
                    ...clusterElementNode,
                    position: containsNodePosition(clusterElementNode.data.metadata)
                        ? clusterElementNode.data.metadata.ui.nodePosition
                        : defaultPosition,
                };
            }

            const parentRootNode = nodes.find((node) => node.id === clusterElementNode.parentId);
            if (!parentRootNode) {
                console.error('Parent root node not found');

                return clusterElementNode;
            }

            const parentRootDagreNode = dagreGraph.node(parentRootNode.id);

            const childNodeLeftEdge = childDagreNode.x - childDagreNode.width / 2;
            const parentNestedRootLeftEdge = parentRootDagreNode.x - parentRootDagreNode.width / 2;

            const nestedChildRelativeX = childNodeLeftEdge - parentNestedRootLeftEdge;
            const nestedChildRelativeY = childDagreNode.y - parentRootDagreNode.y;

            const defaultPosition = {
                x: nestedChildRelativeX,
                y: nestedChildRelativeY,
            };

            return {
                ...clusterElementNode,
                position: containsNodePosition(clusterElementNode.data.metadata)
                    ? clusterElementNode.data.metadata.ui.nodePosition
                    : defaultPosition,
            };
        });

        const positionedNodes = [...positionedClusterElementNodes, ...placeholderNodes];

        return {
            edges,
            nodes: positionedNodes,
        };
    }

    const allNodes = nodes.map((node) => {
        let positionX = dagreGraph.node(node.id).x + (canvasWidth / 2 - dagreGraph.node(nodes[0].id).x - 72 / 2);

        const hasValidClusterElements =
            node.data.clusterElements &&
            Object.entries(node.data.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        if (hasValidClusterElements && node.data.clusterRoot) {
            positionX -= 85;
        }

        return {
            ...node,
            position: {
                x: positionX,
                y: dagreGraph.node(node.id).y,
            },
        };
    });

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

export const createDefaultNodes = (canvasWidth: number): Node[] => [
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
        position: {x: canvasWidth / 2 - 36, y: 50},
        type: 'workflow',
    },
    {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: {x: canvasWidth / 2 - 36, y: 150},
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
