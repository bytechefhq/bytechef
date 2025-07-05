import {
    CONDITION_CASE_FALSE,
    CONDITION_CASE_TRUE,
    DIRECTION,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    ROOT_CLUSTER_ELEMENT_NAMES,
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
    ClusterElementsType,
    ConditionChildTasksType,
    EachChildTasksType,
    LoopChildTasksType,
    NodeDataType,
    ParallelChildTasksType,
} from '@/shared/types';
import dagre from '@dagrejs/dagre';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import InlineSVG from 'react-inlinesvg';

import {getConditionBranchSide} from './createConditionEdges';
import {TASK_DISPATCHER_CONFIG, getParentTaskDispatcherTask} from './taskDispatcherConfig';

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

    const isRootClusterElement = ROOT_CLUSTER_ELEMENT_NAMES.includes(componentName);

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
            rootClusterElement: isRootClusterElement,
            taskDispatcher: isTaskDispatcher,
            taskDispatcherId: isTaskDispatcher ? task.name : undefined,
            trigger: index === 0,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: componentName === 'aiAgent' ? 'aiAgentNode' : 'workflow',
    };
};

interface GetLayoutedElementsProps {
    canvasWidth: number;
    edges: Edge[];
    isClusterElementsCanvas?: boolean;
    nodes: Node[];
}

export const getLayoutedElements = ({canvasWidth, edges, isClusterElementsCanvas, nodes}: GetLayoutedElementsProps) => {
    const dagreGraph = new dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

    dagreGraph.setGraph({rankdir: DIRECTION});

    nodes.forEach((node) => {
        let height = NODE_HEIGHT;
        let width = NODE_WIDTH;

        if (isClusterElementsCanvas) {
            width = 72;

            if (node.id.includes('placeholder')) {
                width = 15;
            }

            if (node.data.rootClusterElement) {
                width = ROOT_CLUSTER_WIDTH;
            }
        } else {
            height = calculateNodeHeight(node);
        }

        dagreGraph.setNode(node.id, {height, width});
    });

    edges.forEach((edge) => {
        if (edge.target.includes('bottom-ghost')) {
            dagreGraph.setEdge(edge.source, edge.target, {minlen: 2});
        } else if (ROOT_CLUSTER_ELEMENT_NAMES.includes(edge.source.split('_')[0])) {
            const sourceNode = nodes.find((node) => node.id === edge.source);

            const hasValidClusterElements =
                sourceNode?.data.clusterElements &&
                Object.entries(sourceNode.data.clusterElements).some(
                    ([, value]) =>
                        value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                );

            if (hasValidClusterElements) {
                dagreGraph.setEdge(edge.source, edge.target, {minlen: 2});
            } else {
                dagreGraph.setEdge(edge.source, edge.target);
            }
        } else {
            dagreGraph.setEdge(edge.source, edge.target);
        }
    });

    dagre.layout(dagreGraph);

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    function containsNodePosition(metadata: any): metadata is {ui: {nodePosition: {x: number; y: number}}} {
        return metadata?.ui?.nodePosition !== undefined;
    }

    if (isClusterElementsCanvas) {
        const rootNode = nodes.find((node) => node.data.clusterElements);

        const hasValidClusterElements =
            rootNode &&
            Object.entries(rootNode.data.clusterElements as ClusterElementsType).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        const nodesWithPositions = hasValidClusterElements
            ? nodes.filter(
                  (node) => containsNodePosition(node.data.metadata) || (node.position.x !== 0 && node.position.y !== 0)
              )
            : [];

        const nodesToLayout = hasValidClusterElements
            ? nodes.filter(
                  (node) => !containsNodePosition(node.data.metadata) && node.position.x === 0 && node.position.y === 0
              )
            : nodes;

        let centeringOffsetX = 0;

        if (nodesToLayout.length > 0) {
            const dagrePositions = nodesToLayout.map((node) => dagreGraph.node(node.id).x);

            const minX = Math.min(...dagrePositions);
            const maxX = Math.max(...dagrePositions);

            const graphWidth = maxX - minX + NODE_WIDTH;

            const graphCenter = minX + graphWidth / 2;

            centeringOffsetX = canvasWidth / 2 - graphCenter;
        }

        let positionedNodes;

        if (hasValidClusterElements && nodesWithPositions.length > 0) {
            positionedNodes = [
                ...nodesWithPositions.map((node) => ({
                    ...node,
                    position: containsNodePosition(node.data.metadata)
                        ? node.data.metadata.ui.nodePosition
                        : node.position,
                })),
                ...nodesToLayout.map((node) => ({
                    ...node,
                    position: {
                        x: dagreGraph.node(node.id).x + centeringOffsetX,
                        y: dagreGraph.node(node.id).y + NODE_HEIGHT,
                    },
                })),
            ];
        } else {
            positionedNodes = nodesToLayout.map((node) => ({
                ...node,
                position: {
                    x: dagreGraph.node(node.id).x + centeringOffsetX,
                    y: dagreGraph.node(node.id).y + NODE_HEIGHT,
                },
            }));
        }

        return {edges, nodes: positionedNodes};
    }

    const allNodes = nodes.map((node) => {
        let positionX = dagreGraph.node(node.id).x + (canvasWidth / 2 - dagreGraph.node(nodes[0].id).x - 72 / 2);

        const hasValidClusterElements =
            node.data.clusterElements &&
            Object.entries(node.data.clusterElements).some(
                ([, value]) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
            );

        if (hasValidClusterElements && node.type === 'aiAgentNode') {
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
                condition: sourceNode.data.rootClusterElement,
            },
            {
                condition: sourceNode.data.componentName === 'branch',
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

        if (componentName === 'parallel') {
            return null;
        }

        let parentSubtasks: WorkflowTask[] = [];

        if (componentName === 'condition') {
            parentSubtasks = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG].getSubtasks({
                context: {
                    conditionCase:
                        ((taskDispatcherNode?.data as NodeDataType).conditionData?.conditionCase as
                            | 'caseTrue'
                            | 'caseFalse') || CONDITION_CASE_TRUE,
                    taskDispatcherId: parentTaskDispatcher.name,
                },
                task: parentTaskDispatcher,
            });
        } else {
            parentSubtasks = TASK_DISPATCHER_CONFIG[componentName as keyof typeof TASK_DISPATCHER_CONFIG].getSubtasks({
                task: parentTaskDispatcher,
            });
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

            if (subtasks.some((subtask) => subtask.name === subsequentNode.id)) {
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
 * Collects nested tasks for all task dispatchers in the workflow
 */
export function collectTaskDispatcherData(
    task: WorkflowTask,
    branchChildTasks: BranchChildTasksType,
    conditionChildTasks: ConditionChildTasksType,
    eachChildTasks: EachChildTasksType,
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
    }
}

/**
 * Detects if a task is nested inside a task dispatcher and returns relevant nesting data
 */
interface GetTaskAncestryProps {
    branchChildTasks: BranchChildTasksType;
    conditionChildTasks: ConditionChildTasksType;
    eachChildTasks: EachChildTasksType;
    loopChildTasks: LoopChildTasksType;
    parallelChildTasks: ParallelChildTasksType;
    taskName: string;
}

export function getTaskAncestry({
    branchChildTasks,
    conditionChildTasks,
    eachChildTasks,
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

    return {isNested, nestingData};
}
