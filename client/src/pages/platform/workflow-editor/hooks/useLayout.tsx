import {EDGE_STYLES, FINAL_PLACEHOLDER_NODE_ID, TASK_DISPATCHER_NAMES} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {WIDTHS} from '@/shared/theme/constants';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useEffect, useMemo} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import createBranchEdges from '../utils/createBranchEdges';
import createBranchNode from '../utils/createBranchNode';
import createConditionEdges, {hasTaskInConditionBranches} from '../utils/createConditionEdges';
import createConditionNode from '../utils/createConditionNode';
import createEachEdges from '../utils/createEachEdges';
import createEachNode from '../utils/createEachNode';
import createForkJoinEdges from '../utils/createForkJoinEdges';
import createForkJoinNode from '../utils/createForkJoinNode';
import createLoopEdges from '../utils/createLoopEdges';
import createLoopNode from '../utils/createLoopNode';
import createParallelEdges from '../utils/createParallelEdges';
import createParallelNode from '../utils/createParallelNode';
import {
    collectTaskDispatcherData,
    convertTaskToNode,
    createDefaultNodes,
    createEdgeFromTaskDispatcherBottomGhostNode,
    getLayoutedElements,
    getTaskAncestry,
} from '../utils/layoutUtils';

interface UseLayoutProps {
    canvasWidth: number;
    componentDefinitions: Array<ComponentDefinitionBasic>;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasic>;
}

export default function useLayout({
    canvasWidth,
    componentDefinitions,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: UseLayoutProps) {
    let workflow = useWorkflowDataStore((state) => state.workflow);

    if (!workflow.tasks && readOnlyWorkflow) {
        workflow = {...workflow, ...readOnlyWorkflow};
    }

    const {tasks, triggers} = workflow;

    const {initializeWithCanvasWidth, setEdges, setNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            initializeWithCanvasWidth: state.initializeWithCanvasWidth,
            setEdges: state.setEdges,
            setNodes: state.setNodes,
        }))
    );

    const triggerComponentName = useMemo(() => triggers?.[0]?.type.split('/')[0], [triggers]);

    const triggerDefinition = useMemo(
        () => componentDefinitions.find((definition) => definition.name === triggerComponentName),
        [componentDefinitions, triggerComponentName]
    );

    const triggerNode = useMemo(() => {
        if (triggerDefinition && triggers?.[0]) {
            return convertTaskToNode(triggers[0], triggerDefinition, 0);
        }

        return createDefaultNodes(canvasWidth)[0];
    }, [triggerDefinition, triggers, canvasWidth]);

    let allNodes: Array<Node> = [triggerNode];

    if (tasks) {
        const branchChildTasks = {};
        const conditionChildTasks = {};
        const eachChildTasks = {};
        const forkJoinChildTasks = {};
        const loopChildTasks = {};
        const parallelChildTasks = {};

        // First pass: collect all task dispatcher data and save it in the corresponding objects
        tasks.forEach((task) => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks
            );
        });

        tasks.forEach((task) => {
            const {name, parameters, type} = task;

            const componentName = type.split('/')[0];
            const isTaskDispatcher = TASK_DISPATCHER_NAMES.includes(componentName);

            let taskNode: Node;

            const taskDefinition = [...componentDefinitions, ...taskDispatcherDefinitions].find(
                (definition) => definition.name === componentName
            );

            // Convert task to node
            if (taskDefinition) {
                taskNode = convertTaskToNode(task, taskDefinition, 1);
            } else {
                taskNode = {
                    data: {
                        ...task,
                        componentName,
                        icon: <ComponentIcon className="size-9 flex-none text-gray-900" />,
                        operationName: type.split('/')[2],
                        taskDispatcher: isTaskDispatcher,
                        taskDispatcherId: isTaskDispatcher ? name : undefined,
                    },
                    id: name,
                    position: {x: 0, y: 0},
                    type: 'workflow',
                };
            }

            const {isNested, nestingData: detectedNestingData} = getTaskAncestry({
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                parallelChildTasks,
                taskName: name,
            });

            if (isNested) {
                taskNode.data = {
                    ...taskNode.data,
                    ...detectedNestingData,
                };
            }

            // Create auxiliary nodes for task dispatchers
            if (componentName === 'condition') {
                const hasTrueBranchTasks = parameters?.caseTrue?.length > 0;
                const hasFalseBranchTasks = parameters?.caseFalse?.length > 0;

                allNodes = createConditionNode({
                    allNodes: [...allNodes, taskNode],
                    conditionId: taskNode.id,
                    isNested,
                    options: {
                        createLeftPlaceholder: !hasTrueBranchTasks,
                        createRightPlaceholder: !hasFalseBranchTasks,
                    },
                });
            } else if (componentName === 'loop') {
                const hasSubtasks = parameters?.iteratee?.length > 0;

                allNodes = createLoopNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    loopId: taskNode.id,
                    options: {
                        createPlaceholder: !hasSubtasks,
                    },
                });
            } else if (componentName === 'branch') {
                const hasDefaultSubtasks = parameters?.default?.length > 0;

                const casesWithoutTasks = (parameters?.cases as BranchCaseType[])?.filter(
                    (taskCase) => taskCase.tasks?.length === 0
                );

                const emptyCaseKeys = casesWithoutTasks?.map((taskCase) => taskCase.key);

                allNodes = createBranchNode({
                    allNodes: [...allNodes, taskNode],
                    branchId: taskNode.id,
                    isNested,
                    options: {
                        createDefaultPlaceholder: !hasDefaultSubtasks,
                        emptyCaseKeys,
                    },
                });
            } else if (componentName === 'parallel') {
                const hasSubtasks = parameters?.tasks?.length > 0;

                allNodes = createParallelNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    options: {
                        createLeftGhost: !hasSubtasks,
                    },
                    parallelId: taskNode.id,
                });
            } else if (componentName === 'each') {
                const hasSubtasks = parameters?.iteratee?.name;

                allNodes = createEachNode({
                    allNodes: [...allNodes, taskNode],
                    eachId: taskNode.id,
                    isNested,
                    options: {
                        createPlaceholder: !hasSubtasks,
                    },
                });
            } else if (componentName === 'fork-join') {
                const hasSubtasks = parameters?.branches?.length > 0;

                allNodes = createForkJoinNode({
                    allNodes: [...allNodes, taskNode],
                    forkJoinId: taskNode.id,
                    isNested,
                    options: {
                        createLeftGhost: !hasSubtasks,
                    },
                });
            } else {
                allNodes.push(taskNode);
            }
        });
    }

    const finalPlaceholderNode: Node = useMemo(() => {
        return {
            data: {label: '+'},
            id: FINAL_PLACEHOLDER_NODE_ID,
            position: {x: 0, y: 0},
            type: 'placeholder',
        };
    }, []);

    const taskEdges: Array<Edge> = [];

    // Create edges based on nodes
    allNodes.forEach((node, index) => {
        const nodeData: NodeDataType = node.data as NodeDataType;

        const isBranchNode = nodeData.componentName === 'branch';
        const isConditionNode = nodeData.componentName === 'condition';
        const isEachNode = nodeData.componentName === 'each';
        const isLoopNode = nodeData.componentName === 'loop';
        const isParallellNode = nodeData.componentName === 'parallel';
        const isForkJoinNode = nodeData.componentName === 'fork-join';

        const isConditionPlaceholderNode = nodeData.conditionId && node.type === 'placeholder';
        const isBranchPlaceholderNode = nodeData.branchId && node.type === 'placeholder';

        const isConditionChildTask = nodeData.conditionData;

        const nextNode = allNodes[index + 1];

        // Create initial edges for the Condition node
        if (isConditionNode) {
            const conditionEdges = createConditionEdges(node, allNodes);

            taskEdges.push(...conditionEdges);

            return;
        }

        // Create initial edges for the Loop node
        if (isLoopNode) {
            const loopEdges = createLoopEdges(node);

            taskEdges.push(...loopEdges);

            return;
        }

        // Create initial edges for the Branch node
        if (isBranchNode) {
            const branchEdges = createBranchEdges(node);

            taskEdges.push(...branchEdges);

            return;
        }

        // Create initial edges for the Parallel node
        if (isParallellNode) {
            const parallelEdges = createParallelEdges(node);

            taskEdges.push(...parallelEdges);

            return;
        }

        // Create initial edges for the Each node
        if (isEachNode) {
            const eachEdges = createEachEdges(node);

            taskEdges.push(...eachEdges);

            return;
        }

        // Create initial edges for the Fork-Join node
        if (isForkJoinNode) {
            const forkJoinEdges = createForkJoinEdges(node);

            taskEdges.push(...forkJoinEdges);

            return;
        }

        if (nextNode && tasks) {
            const isNextNodeTaskDispatcherBottomNode = nextNode.type === 'taskDispatcherBottomGhostNode';

            const nextNodeData: NodeDataType = nextNode.data as NodeDataType;

            const isTaskDispatcherBottomGhostNode = node.type === 'taskDispatcherBottomGhostNode';

            if (isTaskDispatcherBottomGhostNode) {
                const edgeFromTaskDispatcherBottomGhost = createEdgeFromTaskDispatcherBottomGhostNode({
                    allNodes,
                    index,
                    node,
                    tasks,
                });

                if (edgeFromTaskDispatcherBottomGhost) {
                    taskEdges.push(edgeFromTaskDispatcherBottomGhost);

                    if (
                        edgeFromTaskDispatcherBottomGhost.target === FINAL_PLACEHOLDER_NODE_ID &&
                        !allNodes.some((node) => node.id === FINAL_PLACEHOLDER_NODE_ID)
                    ) {
                        allNodes.push(finalPlaceholderNode);
                    }
                }

                return;
            }

            if (isConditionChildTask || node.id.includes('condition-')) {
                const conditionId = isConditionChildTask ? nodeData.conditionData?.conditionId : node.id.split('-')[0];

                if (conditionId) {
                    const isNextNodeInSameCondition =
                        nextNodeData.conditionData?.conditionId === conditionId ||
                        hasTaskInConditionBranches(conditionId, nextNode.id, tasks);

                    const isOwnBottomGhost =
                        isNextNodeTaskDispatcherBottomNode && nextNode.id === `${conditionId}-condition-bottom-ghost`;

                    const isInDifferentBranches =
                        nodeData.conditionData &&
                        nextNodeData.conditionData &&
                        nodeData.conditionData.conditionCase !== nextNodeData.conditionData.conditionCase;

                    if (isInDifferentBranches || (!isNextNodeInSameCondition && !isOwnBottomGhost)) {
                        return;
                    }
                }
            }

            if (isConditionPlaceholderNode || isBranchPlaceholderNode) {
                return;
            }

            let edgeToNextNode: Edge = {
                id: `${node.id}=>${nextNode.id}`,
                source: node.id,
                style: EDGE_STYLES,
                target: nextNode.id,
                type: node.id.includes('placeholder') ? 'smoothstep' : 'workflow',
            };

            if (isNextNodeTaskDispatcherBottomNode) {
                edgeToNextNode = {
                    ...edgeToNextNode,
                    targetHandle: `${nextNode.id}-bottom-ghost-right`,
                };
            }

            if (!taskEdges.find((edge) => edge.source === node.id)) {
                taskEdges.push(edgeToNextNode);
            }
        } else {
            allNodes.push(finalPlaceholderNode);

            taskEdges.push({
                id: `${node.id}=>${FINAL_PLACEHOLDER_NODE_ID}`,
                source: node.id,
                target: FINAL_PLACEHOLDER_NODE_ID,
                type: 'placeholder',
            });
        }
    });

    useEffect(() => {
        if (!workflow) {
            return;
        }

        let layoutNodes = allNodes;
        let edges: Edge[] = taskEdges;

        if (readOnlyWorkflow) {
            layoutNodes = allNodes.map((node) => {
                if (node.type === 'workflow') {
                    return {
                        ...node,
                        type: 'readonly',
                    };
                }

                if (node.type === 'placeholder') {
                    return {
                        ...node,
                        type: 'readonlyPlaceholder',
                    };
                }

                return node;
            });

            layoutNodes.pop();

            edges = taskEdges.map((edge) => ({
                ...edge,
                type: 'smoothstep',
            }));

            const lastEdge = edges[edges.length - 1];

            if (lastEdge && lastEdge.target === FINAL_PLACEHOLDER_NODE_ID) {
                edges.pop();
            }
        }

        getLayoutedElements({canvasWidth, edges, nodes: layoutNodes}).then((elements) => {
            if (readOnlyWorkflow) {
                const SHEET_WIDTH = WIDTHS.WORKFLOW_READ_ONLY_SHEET_WIDTH;

                const centeringOffsetX = (canvasWidth - SHEET_WIDTH) / 2;

                const centeredNodes = elements.nodes.map((node) => ({
                    ...node,
                    position: {
                        x: node.position.x - centeringOffsetX,
                        y: node.position.y,
                    },
                }));

                setNodes(centeredNodes);
            } else {
                setNodes(elements.nodes);
            }

            setEdges(elements.edges);
        });

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canvasWidth, tasks, triggers, workflow]);

    useEffect(() => {
        if (canvasWidth > 0) {
            initializeWithCanvasWidth(canvasWidth);
        }
    }, [canvasWidth, initializeWithCanvasWidth]);
}
