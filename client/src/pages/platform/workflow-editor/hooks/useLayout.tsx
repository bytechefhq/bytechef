import {CONDITION_CASE_FALSE, CONDITION_CASE_TRUE} from '@/shared/constants';
import defaultNodes from '@/shared/defaultNodes';
import {WorkflowTask} from '@/shared/middleware/automation/configuration';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {getRandomId} from '@/shared/util/random-utils';
import Dagre from '@dagrejs/dagre';
import {ComponentIcon} from 'lucide-react';
import {useEffect} from 'react';
import InlineSVG from 'react-inlinesvg';
import {Edge, Node, ReactFlowState, useReactFlow, useStore} from 'reactflow';

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

const nodeCountSelector = (state: ReactFlowState) => state.nodeInternals.size;

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

export default function useLayout({
    componentDefinitions,
    taskDispatcherDefinitions,
}: {
    componentDefinitions: Array<ComponentDefinitionBasic>;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasic>;
}) {
    const nodeCount = useStore(nodeCountSelector);

    const {fitView, getEdges, getNodes, setEdges, setNodes} = useReactFlow();

    const {
        workflow: {tasks, triggers},
    } = useWorkflowDataStore();

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

    const allNodes: Array<Node> = [];

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
            caseTrueTaskNames = taskNode.data.parameters.caseTrue.map((task: WorkflowTask) => task.name);
            caseFalseTaskNames = taskNode.data.parameters.caseFalse.map((task: WorkflowTask) => task.name);

            conditionChildTasks[taskNode.id] = {
                caseFalse: caseFalseTaskNames,
                caseTrue: caseTrueTaskNames,
            };
        }

        const isConditionChildTask = Object.values(conditionChildTasks).some(
            (conditionCases) =>
                conditionCases.caseTrue.includes(taskNode.id) || conditionCases.caseFalse.includes(taskNode.id)
        );

        // Handle placeholder nodes above and below the task node inside the Condition
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
                data: {conditionId, label: '+'},
                id: belowPlaceholderNodeId,
                position: {x: 0, y: 0},
                type: 'placeholder',
            };

            const conditionChildTaskNode = {
                ...taskNode,
                data: {...taskNode.data, conditionData: {conditionCase, conditionId, index}},
            };

            allNodes.splice(sourcePlaceholderIndex + 1, 0, conditionChildTaskNode, belowPlaceholderNode);

            return;
        }

        allNodes.push(taskNode);

        // Create left, right, and bottom placeholder nodes when the task node is a Condition
        if (taskNode.data.componentName === 'condition') {
            const leftPlaceholderNode: Node = {
                data: {conditionId: taskNode.id, label: '+'},
                id: `${taskNode.id}-left-placeholder-0`,
                position: {x: 0, y: 0},
                type: 'placeholder',
            };

            const rightPlaceholderNode: Node = {
                data: {conditionId: taskNode.id, label: '+'},
                id: `${taskNode.id}-right-placeholder-0`,
                position: {x: 0, y: 0},
                type: 'placeholder',
            };

            const bottomPlaceholderNode: Node = {
                data: {label: '+'},
                id: `${taskNode.id}-bottom-placeholder`,
                position: {x: 0, y: 0},
                type: 'placeholder',
            };

            allNodes.push(leftPlaceholderNode, rightPlaceholderNode, bottomPlaceholderNode);
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
        if (taskNode.id.includes('placeholder') && !taskNode.id.includes('bottom')) {
            const parentConditionTaskId = taskNode.id.split('-')[0];

            taskEdges.push({
                id: `${taskNode.id}=>${parentConditionTaskId}-bottom-placeholder`,
                source: taskNode.id,
                target: `${parentConditionTaskId}-bottom-placeholder`,
                type: 'smoothstep',
            });

            return;
        }

        // Create edges for the Condition child node
        if (taskNode.data.conditionData && !taskNode.id.includes('placeholder')) {
            const {conditionCase, conditionId, index} = taskNode.data.conditionData;

            const sourcePlaceholderId = `${conditionId}-${
                conditionCase === CONDITION_CASE_TRUE ? 'left' : 'right'
            }-placeholder-${index}`;

            const targetPlaceholderId = getNextPlaceholderId(sourcePlaceholderId);

            const edgeFromSourceNodeToTaskNode = {
                id: `${sourcePlaceholderId}=>${taskNode.id}`,
                source: sourcePlaceholderId,
                target: taskNode.id,
                type: 'smoothstep',
            };

            const edgeFromTaskNodeToTargetNode = {
                id: `${taskNode.id}=>${targetPlaceholderId}`,
                source: taskNode.id,
                target: targetPlaceholderId,
                type: 'smoothstep',
            };

            taskEdges.pop();

            taskEdges.push(edgeFromSourceNodeToTaskNode, edgeFromTaskNodeToTargetNode);

            return;
        }

        if (nextNode) {
            taskEdges.push({
                id: `${taskNode.id}=>${nextNode.id}`,
                source: taskNode.id,
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

        let nodes: Node[] = getNodes();
        let edges: Edge[] = getEdges();

        if (triggerAndTaskNodes.length) {
            nodes = taskNodes?.length ? [...triggerAndTaskNodes] : [triggerNode, finalPlaceholderNode];
        }

        edges = taskEdges;

        nodes.forEach((node) => {
            let height = NODE_HEIGHT;

            if (node.id.includes('placeholder')) {
                if (node.id.includes('0')) {
                    height = PLACEHOLDER_NODE_HEIGHT * 2;
                } else {
                    height = NODE_HEIGHT;
                }

                if (node.id.includes('bottom')) {
                    height = PLACEHOLDER_NODE_HEIGHT;
                }
            }

            dagreGraph.setNode(node.id, {height, width: NODE_WIDTH});
        });

        edges.forEach((edge) => {
            dagreGraph.setEdge(edge.source, edge.target);
        });

        Dagre.layout(dagreGraph);

        nodes = nodes.map((node) => {
            const position = dagreGraph.node(node.id);

            return {...node, position};
        });

        setNodes(nodes);
        setEdges(edges);

        window.requestAnimationFrame(() => {
            fitView();
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [finalPlaceholderNode, nodeCount, setEdges, setNodes, taskEdges, taskNodes]);
}
