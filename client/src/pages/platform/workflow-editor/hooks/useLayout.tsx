import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE, EDGE_STYLES} from '@/shared/constants';
import defaultNodes from '@/shared/defaultNodes';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {getRandomId} from '@/shared/util/random-utils';
import Dagre from '@dagrejs/dagre';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useEffect} from 'react';
import InlineSVG from 'react-inlinesvg';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import getNextPlaceholderId from '../utils/getNextPlaceholderId';

const NODE_WIDTH = 240;
const NODE_HEIGHT = 100;
const PLACEHOLDER_NODE_HEIGHT = 28;
const DIRECTION = 'TB';
const FINAL_PLACEHOLDER_NODE_ID = getRandomId();

const TASK_DISPATCHER_NAMES = [
    'branch',
    'condition',
    'each',
    'fork-join',
    'loop',
    'loop-break',
    'map',
    'parallel',
    'subflow',
];

const convertTaskToNode = (
    task: WorkflowTask,
    taskDefinition: ComponentDefinitionBasic | TaskDispatcherDefinitionBasic,
    index: number
): Node => {
    const componentName = task.type.split('/')[0];

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
            taskDispatcher: TASK_DISPATCHER_NAMES.includes(componentName),
            trigger: index === 0,
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
};

const createConditionNode = ({
    allNodes,
    belowPlaceholderNode,
    sourcePlaceholderIndex,
    taskNode,
}: {
    allNodes: Array<Node>;
    belowPlaceholderNode?: Node;
    sourcePlaceholderIndex?: number;
    taskNode: Node;
}) => {
    const leftPlaceholderNode: Node = {
        data: {conditionCase: CONDITION_CASE_TRUE, conditionId: taskNode.id, label: '+'},
        id: `${taskNode.id}-left-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const rightPlaceholderNode: Node = {
        data: {conditionCase: CONDITION_CASE_FALSE, conditionId: taskNode.id, label: '+'},
        id: `${taskNode.id}-right-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    if (taskNode.data.conditionData && belowPlaceholderNode && sourcePlaceholderIndex) {
        allNodes.splice(sourcePlaceholderIndex + 1, 0, leftPlaceholderNode, rightPlaceholderNode, belowPlaceholderNode);

        return allNodes;
    }

    const bottomPlaceholderNode: Node = {
        data: {label: '+'},
        id: `${taskNode.id}-bottom-placeholder`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const insertIndex = allNodes.findIndex((node) => node.id === taskNode.id) + 1;

    allNodes.splice(insertIndex, 0, leftPlaceholderNode, rightPlaceholderNode, bottomPlaceholderNode);

    return allNodes;
};

const filterConditionCaseNodes = (nodes: Node[], node: Node) => {
    return nodes.filter((nodeItem) => {
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
};

export default function useLayout({
    canvasWidth,
    componentDefinitions,
    taskDispatcherDefinitions,
}: {
    componentDefinitions: Array<ComponentDefinitionBasic>;
    canvasWidth: number;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasic>;
}) {
    const {
        workflow: {tasks, triggers},
    } = useWorkflowDataStore();

    const {nodes, setEdges, setNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            nodes: state.nodes,
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const triggerComponentName = triggers?.[0]?.type.split('/')[0];

    const triggerDefinition = componentDefinitions.find((definition) => definition.name === triggerComponentName);

    const triggerNode =
        triggerDefinition && triggers?.[0] ? convertTaskToNode(triggers[0], triggerDefinition, 0) : defaultNodes[0];

    let taskNodes: Array<Node> = [];

    if (tasks) {
        taskNodes = tasks?.map((task, index) => {
            const componentName = task.type.split('/')[0];

            const combinedDefinitions = [...componentDefinitions, ...taskDispatcherDefinitions];

            const taskDefinition = combinedDefinitions.find((definition) => definition.name === componentName);

            if (taskDefinition) {
                return convertTaskToNode(task, taskDefinition, index + 1);
            } else {
                return {
                    data: {
                        ...task,
                        componentName,
                        icon: <ComponentIcon className="size-9 flex-none text-gray-900" />,
                        operationName: task.type.split('/')[2],
                        taskDispatcher: TASK_DISPATCHER_NAMES.includes(componentName),
                        trigger: index === 0,
                    },
                    id: task.name,
                    position: {x: 0, y: 0},
                    type: 'workflow',
                };
            }
        });
    }

    let allNodes: Array<Node> = [];

    const conditionChildTasks: {
        [key: string]: {
            caseTrue: string[];
            caseFalse: string[];
        };
    } = {};

    let caseTrueTaskNames;
    let caseFalseTaskNames;

    // Prepare auxiliary nodes
    taskNodes.forEach((taskNode) => {
        if (taskNode.data.componentName === 'condition') {
            caseTrueTaskNames = (taskNode.data as NodeDataType)?.parameters?.caseTrue.map(
                (task: WorkflowTask) => task.name
            );
            caseFalseTaskNames = (taskNode.data as NodeDataType)?.parameters?.caseFalse.map(
                (task: WorkflowTask) => task.name
            );

            conditionChildTasks[taskNode.id] = {
                caseFalse: caseFalseTaskNames,
                caseTrue: caseTrueTaskNames,
            };
        }

        const isConditionChildTask = Object.values(conditionChildTasks).some(
            (conditionCases) =>
                conditionCases.caseTrue.includes(taskNode.id) || conditionCases.caseFalse.includes(taskNode.id)
        );

        // Handle Condition child placeholder nodes
        if (isConditionChildTask) {
            const conditionId = Object.keys(conditionChildTasks).find(
                (key) =>
                    conditionChildTasks[key].caseTrue.includes(taskNode.id) ||
                    conditionChildTasks[key].caseFalse.includes(taskNode.id)
            );

            if (!conditionId) {
                return;
            }

            const conditionCase = conditionChildTasks[conditionId].caseTrue.includes(taskNode.id)
                ? CONDITION_CASE_TRUE
                : CONDITION_CASE_FALSE;

            const index = conditionChildTasks[conditionId][conditionCase].indexOf(taskNode.id);

            const sourcePlaceholderIndex = allNodes.findIndex(
                (node) =>
                    node.id ===
                    `${conditionId}-${conditionCase === CONDITION_CASE_TRUE ? 'left' : 'right'}-placeholder-${index}`
            );

            if (sourcePlaceholderIndex === -1) {
                return;
            }

            const sourcePlaceholderNode = allNodes[sourcePlaceholderIndex];

            const belowPlaceholderNodeId = getNextPlaceholderId(sourcePlaceholderNode.id);

            const belowPlaceholderNode = {
                data: {conditionCase, conditionId, label: '+'},
                id: belowPlaceholderNodeId,
                position: {x: 0, y: 0},
                type: 'placeholder',
            };

            const conditionChildTaskNode = {
                ...taskNode,
                data: {...taskNode.data, conditionData: {conditionCase, conditionId, index}},
            };

            if ((conditionChildTaskNode.data as NodeDataType).componentName === 'condition') {
                allNodes = createConditionNode({
                    allNodes,
                    belowPlaceholderNode,
                    sourcePlaceholderIndex,
                    taskNode: conditionChildTaskNode,
                });

                allNodes.splice(sourcePlaceholderIndex + 1, 0, conditionChildTaskNode);

                return;
            }

            allNodes.splice(sourcePlaceholderIndex + 1, 0, conditionChildTaskNode, belowPlaceholderNode);

            return;
        }

        allNodes.push(taskNode);

        // Create left, right, and bottom placeholder nodes when the task node is a Condition
        if (taskNode.data.componentName === 'condition') {
            allNodes = createConditionNode({allNodes, taskNode});
        }

        const currentTaskNode = allNodes.find((node) => node.id === taskNode.id);

        if (currentTaskNode) {
            Object.assign(taskNode, currentTaskNode);
        }
    });

    taskNodes = allNodes;

    const triggerAndTaskNodes: Array<Node> = [triggerNode, ...(taskNodes?.length ? taskNodes : [])];

    const finalPlaceholderNode: Node = {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const taskEdges: Array<Edge> = [];

    // Create edges based on nodes
    triggerAndTaskNodes.forEach((taskNode, index) => {
        const nextNode = triggerAndTaskNodes[index + 1];

        const parentConditionId = Object.keys(conditionChildTasks).find((key) => {
            const conditionCases = conditionChildTasks[key];

            return (
                conditionCases.caseTrue.includes((taskNode.data as NodeDataType)?.conditionId ?? '') ||
                conditionCases.caseFalse.includes((taskNode.data as NodeDataType)?.conditionId ?? '')
            );
        });

        // Create initial edges for the Condition node
        if (taskNode.data.componentName === 'condition') {
            const leftPlaceholderEdge = {
                id: `${taskNode.id}=>${taskNode.id}-left-placeholder-0`,
                source: taskNode.id,
                target: `${taskNode.id}-left-placeholder-0`,
                type: 'condition',
            };

            const rightPlaceholderEdge = {
                id: `${taskNode.id}=>${taskNode.id}-right-placeholder-0`,
                source: taskNode.id,
                target: `${taskNode.id}-right-placeholder-0`,
                type: 'condition',
            };

            taskEdges.push(leftPlaceholderEdge, rightPlaceholderEdge);

            return;
        }

        // Create the bottom Condition edge
        if (
            taskNode.id.includes('placeholder') &&
            !taskNode.id.includes('bottom') &&
            taskNode.data.conditionData &&
            !taskNode.id.includes('condition')
        ) {
            const parentConditionTaskId = taskNode.id.split('-')[0];

            taskEdges.push({
                id: `${taskNode.id}=>${parentConditionTaskId}-bottom-placeholder`,
                source: taskNode.id,
                style: EDGE_STYLES,
                target: `${parentConditionTaskId}-bottom-placeholder`,
                type: 'smoothstep',
            });

            return;
        }

        // Create the edge for the Condition child placeholder node
        if (taskNode.id.includes('placeholder') && !taskNode.id.includes('bottom') && parentConditionId) {
            const nextPlaceholderNode = triggerAndTaskNodes
                .slice(index + 1)
                .find((node) => node.id.includes('placeholder') && node.data.conditionId === parentConditionId);

            if (!nextPlaceholderNode) {
                return;
            }

            const placeholderNodeConditionCase = taskNode.id.includes('left')
                ? CONDITION_CASE_TRUE
                : CONDITION_CASE_FALSE;

            const nextTaskNode = triggerAndTaskNodes
                .slice(index + 1)
                .find(
                    (node) =>
                        !node.id.includes('placeholder') &&
                        (node.data as NodeDataType).conditionData?.conditionCase === placeholderNodeConditionCase
                );

            if (
                nextTaskNode &&
                (nextTaskNode.data as NodeDataType).conditionData?.conditionId === taskNode.data.conditionId
            ) {
                taskEdges.push({
                    id: `${taskNode.id}=>${nextTaskNode.id}`,
                    source: taskNode.id,
                    style: EDGE_STYLES,
                    target: nextTaskNode.id,
                    type: 'smoothstep',
                });

                return;
            }

            const edgeExists = taskEdges.some(
                (edge) => edge.source === taskNode.id && edge.target === nextPlaceholderNode.id
            );

            if (!edgeExists) {
                taskEdges.push({
                    id: `${taskNode.id}=>${nextPlaceholderNode.id}`,
                    source: taskNode.id,
                    style: EDGE_STYLES,
                    target: nextPlaceholderNode.id,
                    type: 'smoothstep',
                });
            }

            return;
        }

        // Create edges for the Condition child node
        if (taskNode.data.conditionData && !taskNode.id.includes('placeholder')) {
            const {conditionCase, conditionId, index} = (taskNode.data as NodeDataType).conditionData!;

            const sourcePlaceholderId = `${conditionId}-${
                conditionCase === CONDITION_CASE_TRUE ? 'left' : 'right'
            }-placeholder-${index}`;

            const targetPlaceholderId = getNextPlaceholderId(sourcePlaceholderId);

            const edgeFromSourceNodeToTaskNode = {
                id: `${sourcePlaceholderId}=>${taskNode.id}`,
                source: sourcePlaceholderId,
                style: EDGE_STYLES,
                target: taskNode.id,
                type: 'smoothstep',
            };

            const edgeFromTaskNodeToTargetNode = {
                id: `${taskNode.id}=>${targetPlaceholderId}`,
                source: taskNode.id,
                style: EDGE_STYLES,
                target: targetPlaceholderId,
                type: 'smoothstep',
            };

            taskEdges.pop();

            taskEdges.push(edgeFromSourceNodeToTaskNode, edgeFromTaskNodeToTargetNode);

            return;
        }

        if (nextNode) {
            const nextSideNode = triggerAndTaskNodes.find((node) => node.id === getNextPlaceholderId(taskNode.id));

            if (!nextSideNode) {
                taskEdges.push({
                    id: `${taskNode.id}=>${taskNode.data.conditionId}-bottom-placeholder`,
                    source: taskNode.id,
                    style: EDGE_STYLES,
                    target: `${taskNode.data.conditionId}-bottom-placeholder`,
                    type: 'smoothstep',
                });
            }

            if (taskNode.data.conditionId && taskNode.data.conditionId === nextNode.data.conditionId) {
                const placeholderIndex = parseInt(taskNode.id.split('-').pop() || '0', 10);
                const nextNodePlaceholderIndex = parseInt(nextNode.id.split('-').pop() || '0', 10);

                if (placeholderIndex + 1 !== nextNodePlaceholderIndex) {
                    return;
                }
            }

            taskEdges.push({
                id: `${taskNode.id}=>${nextNode.id}`,
                source: taskNode.id,
                style: EDGE_STYLES,
                target: nextNode.id,
                type: taskNode.id.includes('placeholder') ? 'smoothstep' : 'workflow',
            });
        } else {
            triggerAndTaskNodes.push(finalPlaceholderNode);

            taskEdges.push({
                id: `${taskNode.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
                source: taskNode.id,
                target: FINAL_PLACEHOLDER_NODE_ID,
                type: 'placeholder',
            });
        }
    });

    useEffect(() => {
        const dagreGraph = new Dagre.graphlib.Graph().setDefaultEdgeLabel(() => ({}));

        dagreGraph.setGraph({rankdir: DIRECTION});

        let layoutNodes: Node[] = nodes;
        let edges: Edge[] = taskEdges;

        if (triggerAndTaskNodes.length) {
            layoutNodes = taskNodes?.length ? [...triggerAndTaskNodes] : [triggerNode, finalPlaceholderNode];
        }

        layoutNodes.forEach((node, index) => {
            let height = NODE_HEIGHT;

            if (node.id.includes('placeholder')) {
                height = PLACEHOLDER_NODE_HEIGHT * 2;

                if (node.id.includes('placeholder-0')) {
                    const hasOtherConditionCaseNodes = filterConditionCaseNodes(layoutNodes, node);

                    if (hasOtherConditionCaseNodes.length) {
                        height = 0;
                    } else {
                        height = PLACEHOLDER_NODE_HEIGHT * 2;
                    }
                } else {
                    height = PLACEHOLDER_NODE_HEIGHT;
                }

                if (node.id.includes('bottom')) {
                    height = PLACEHOLDER_NODE_HEIGHT;
                }
            } else if (!node.data.conditionData) {
                height = NODE_HEIGHT * 1.2;
            }

            if (index === layoutNodes.length - 1) {
                height = 20;

                const penultimateNode = layoutNodes[layoutNodes.length - 2];

                if (penultimateNode.id.includes('bottom-placeholder')) {
                    height = 90;
                }
            }

            dagreGraph.setNode(node.id, {height, width: NODE_WIDTH});
        });

        edges.forEach((edge) => {
            dagreGraph.setEdge(edge.source, edge.target);
        });

        Dagre.layout(dagreGraph);

        layoutNodes = layoutNodes.map((node) => {
            let positionY = dagreGraph.node(node.id).y;

            if (node.id.includes('placeholder-0') && !node.id.includes('bottom')) {
                const hasOtherConditionCaseNodes = filterConditionCaseNodes(layoutNodes, node);

                if (hasOtherConditionCaseNodes.length) {
                    positionY += 35;
                }
            } else if (node.id.includes('bottom-placeholder')) {
                positionY += 35;
            }

            return {
                ...node,
                position: {
                    x: dagreGraph.node(node.id).x + (canvasWidth / 2 - dagreGraph.node(layoutNodes[0].id).x - 72 / 2),
                    y: positionY,
                },
            };
        });

        edges = edges.reduce(
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

        setNodes(layoutNodes);
        setEdges(edges);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, tasks, triggers]);
}
