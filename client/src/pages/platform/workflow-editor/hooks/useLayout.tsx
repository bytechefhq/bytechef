import {
    DATA_PILL_PANEL_WIDTH,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    NODE_DETAILS_PANEL_WIDTH,
    PROJECT_LEFT_SIDEBAR_WIDTH,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {WIDTHS} from '@/shared/theme/constants';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';
import {useStoreWithEqualityFn} from 'zustand/traditional';

import useDataPillPanelStore from '../stores/useDataPillPanelStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import animateNodePositions from '../utils/animateNodePositions';
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
import createMapEdges from '../utils/createMapEdges';
import createMapNode from '../utils/createMapNode';
import createParallelEdges from '../utils/createParallelEdges';
import createParallelNode from '../utils/createParallelNode';
import {
    collectTaskDispatcherData,
    convertTaskToNode,
    createDefaultNodes,
    createEdgeFromTaskDispatcherBottomGhostNode,
    getLayoutElements,
    getTaskAncestry,
} from '../utils/layoutUtils';
import {forEachNestedTaskGroup} from '../utils/taskTraversalUtils';

/**
 * Builds a string key that changes only when the task graph structure changes
 * (task names, types, nested task counts) but NOT when parameter values change.
 * This prevents unnecessary dagre layout recalculations on every property save.
 */
function countSavedPositions(task: WorkflowTask): number {
    let count = task.metadata?.ui?.nodePosition ? 1 : 0;

    if (task.parameters) {
        forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (subtasks) => {
            for (const subtask of subtasks) {
                count += countSavedPositions(subtask);
            }
        });
    }

    return count;
}

export function getTasksStructuralFingerprint(tasks: WorkflowTask[]): string {
    return tasks
        .map((task) => {
            const hasFilledClusterElements =
                task.clusterElements &&
                Object.values(task.clusterElements).some(
                    (value) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                );

            const parts = [
                task.name,
                task.type,
                task.clusterRoot ? 'cr' : '',
                hasFilledClusterElements ? 'ce' : '',
                `p${countSavedPositions(task)}`,
            ];

            if (task.parameters) {
                const keyCounts = new Map<string, number[]>();

                forEachNestedTaskGroup(task.parameters as Record<string, unknown>, (subtasks, key) => {
                    if (!keyCounts.has(key)) {
                        keyCounts.set(key, []);
                    }

                    keyCounts.get(key)!.push(subtasks.length);
                });

                for (const [key, counts] of keyCounts) {
                    parts.push(counts.length === 1 ? `${key}${counts[0]}` : `${key}${counts.join('-')}`);
                }
            }

            return parts.join('|');
        })
        .join(',');
}

interface UseLayoutProps {
    canvasHeight?: number;
    canvasWidth: number;
    componentDefinitions: Array<ComponentDefinitionBasic>;
    direction?: LayoutDirectionType;
    leftSidebarOpen?: boolean;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasic>;
}

export default function useLayout({
    canvasHeight,
    canvasWidth,
    componentDefinitions,
    direction: directionProp,
    leftSidebarOpen,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: UseLayoutProps) {
    const storeDirection = useLayoutDirectionStore((state) => state.layoutDirection);
    const layoutDirection = directionProp || storeDirection;

    // Selective subscriptions with structural equality — prevents re-renders on parameter-only
    // changes (typing). Only re-renders when task graph structure changes (add/delete node).
    const storeTasks = useStoreWithEqualityFn(
        useWorkflowDataStore,
        (state) => state.workflow.tasks,
        (previousTasks, nextTasks) => {
            if (previousTasks === nextTasks) {
                return true;
            }

            if (!previousTasks || !nextTasks) {
                return false;
            }

            return getTasksStructuralFingerprint(previousTasks) === getTasksStructuralFingerprint(nextTasks);
        }
    );

    const storeTriggers = useStoreWithEqualityFn(
        useWorkflowDataStore,
        (state) => state.workflow.triggers,
        (previousTriggers, nextTriggers) => {
            if (previousTriggers === nextTriggers) {
                return true;
            }

            if (!previousTriggers || !nextTriggers) {
                return false;
            }

            if (previousTriggers.length !== nextTriggers.length) {
                return false;
            }

            return previousTriggers.every(
                (trigger, index) =>
                    trigger.name === nextTriggers[index].name && trigger.type === nextTriggers[index].type
            );
        }
    );

    const isWorkflowLoaded = useWorkflowDataStore((state) => state.isWorkflowLoaded);

    const tasks = storeTasks || readOnlyWorkflow?.tasks;
    const triggers = storeTriggers || readOnlyWorkflow?.triggers;

    const {incrementLayoutResetCounter, initializeWithCanvasWidth, setEdges, setNodes, setSavedPositionCrossAxisShift} =
        useWorkflowDataStore(
            useShallow((state) => ({
                incrementLayoutResetCounter: state.incrementLayoutResetCounter,
                initializeWithCanvasWidth: state.initializeWithCanvasWidth,
                setEdges: state.setEdges,
                setNodes: state.setNodes,
                setSavedPositionCrossAxisShift: state.setSavedPositionCrossAxisShift,
            }))
        );
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );
    const layoutResetCounter = useWorkflowDataStore((state) => state.layoutResetCounter);

    const cancelAnimationRef = useRef<(() => void) | null>(null);
    const isInitialLayoutRef = useRef(true);
    const initialCanvasCrossDimRef = useRef<number | undefined>(undefined);
    const initialDirectionRef = useRef<LayoutDirectionType | undefined>(undefined);
    const canvasWidthRef = useRef(canvasWidth);
    const canvasHeightRef = useRef(canvasHeight);
    const previousDataPillPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousNodeDetailsPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousLeftSidebarOpenRef = useRef<boolean | undefined>(undefined);

    canvasWidthRef.current = canvasWidth;
    canvasHeightRef.current = canvasHeight;

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
        const mapChildTasks = {};
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
                mapChildTasks,
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
                    type: task.clusterRoot ? 'clusterRoot' : 'workflow',
                };
            }

            const {isNested, nestingData: detectedNestingData} = getTaskAncestry({
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                loopChildTasks,
                mapChildTasks,
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
            } else if (componentName === 'map') {
                const hasSubtasks = parameters?.iteratee?.length > 0;

                allNodes = createMapNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    mapId: taskNode.id,
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
        const isMapNode = nodeData.componentName === 'map';
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

        // Create initial edges for the Map node
        if (isMapNode) {
            const mapEdges = createMapEdges(node);

            taskEdges.push(...mapEdges);

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
                    const isDuplicate = taskEdges.some((edge) => edge.id === edgeFromTaskDispatcherBottomGhost.id);

                    if (!isDuplicate) {
                        taskEdges.push(edgeFromTaskDispatcherBottomGhost);

                        if (
                            edgeFromTaskDispatcherBottomGhost.target === FINAL_PLACEHOLDER_NODE_ID &&
                            !allNodes.some((node) => node.id === FINAL_PLACEHOLDER_NODE_ID)
                        ) {
                            allNodes.push(finalPlaceholderNode);
                        }
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
        const canvasCrossDimension = layoutDirection === 'LR' && canvasHeight ? canvasHeight : canvasWidth;

        if (initialCanvasCrossDimRef.current === undefined || initialDirectionRef.current !== layoutDirection) {
            initialCanvasCrossDimRef.current = canvasCrossDimension;
            initialDirectionRef.current = layoutDirection;
            setSavedPositionCrossAxisShift(0);
        } else {
            setSavedPositionCrossAxisShift((canvasCrossDimension - initialCanvasCrossDimRef.current) / 2);
        }
    }, [canvasWidth, canvasHeight, layoutDirection, setSavedPositionCrossAxisShift]);

    useEffect(() => {
        if (!useWorkflowDataStore.getState().isWorkflowLoaded) {
            previousDataPillPanelOpenRef.current = dataPillPanelOpen;
            previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;

            return;
        }

        let widthDelta = 0;

        if (
            previousNodeDetailsPanelOpenRef.current !== undefined &&
            previousNodeDetailsPanelOpenRef.current !== workflowNodeDetailsPanelOpen
        ) {
            widthDelta += workflowNodeDetailsPanelOpen ? NODE_DETAILS_PANEL_WIDTH : -NODE_DETAILS_PANEL_WIDTH;
        }

        if (
            previousDataPillPanelOpenRef.current !== undefined &&
            previousDataPillPanelOpenRef.current !== dataPillPanelOpen
        ) {
            widthDelta += dataPillPanelOpen ? DATA_PILL_PANEL_WIDTH : -DATA_PILL_PANEL_WIDTH;
        }

        if (
            previousLeftSidebarOpenRef.current !== undefined &&
            previousLeftSidebarOpenRef.current !== leftSidebarOpen
        ) {
            widthDelta += leftSidebarOpen ? PROJECT_LEFT_SIDEBAR_WIDTH : -PROJECT_LEFT_SIDEBAR_WIDTH;
        }

        if (widthDelta === 0) {
            previousDataPillPanelOpenRef.current = dataPillPanelOpen;
            previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;

            return;
        }

        previousDataPillPanelOpenRef.current = dataPillPanelOpen;
        previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
        previousLeftSidebarOpenRef.current = leftSidebarOpen;

        if (cancelAnimationRef.current) {
            cancelAnimationRef.current();
            cancelAnimationRef.current = null;
        }

        // Trigger a full layout recomputation rather than shifting nodes directly.
        // The dagre worker already accounts for canvas dimensions via savedPositionCrossAxisShift.
        // Direct shifting caused visible jumps in LR mode when the panel open coincided
        // with a structural graph change (e.g. adding a node).
        incrementLayoutResetCounter();
    }, [
        dataPillPanelOpen,
        incrementLayoutResetCounter,
        layoutDirection,
        leftSidebarOpen,
        workflowNodeDetailsPanelOpen,
    ]);

    useEffect(() => {
        if (useWorkflowDataStore.getState().isNodeDragging) {
            return;
        }

        if (!isWorkflowLoaded && !readOnlyWorkflow) {
            return;
        }

        let isCancelled = false;

        let layoutNodes = allNodes;
        let edges: Edge[] = taskEdges;

        if (readOnlyWorkflow) {
            layoutNodes = allNodes.map((node) => {
                if (node.type === 'workflow' || node.type === 'clusterRoot') {
                    return {
                        ...node,
                        data: {
                            ...node.data,
                            clusterElements: undefined,
                            clusterRoot: undefined,
                        },
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

        const savedPositionCrossAxisShift = useWorkflowDataStore.getState().savedPositionCrossAxisShift;

        // Cancel any in-flight animation immediately so competing effects
        // (e.g. panel-shift animation) don't visibly move nodes while the worker runs
        if (cancelAnimationRef.current) {
            cancelAnimationRef.current();
            cancelAnimationRef.current = null;
        }

        // Snapshot positions right after cancelling — these are where nodes visually are right now
        const frozenNodes = useWorkflowDataStore.getState().nodes;

        getLayoutElements({
            canvasHeight: canvasHeightRef.current,
            canvasWidth: canvasWidthRef.current,
            direction: layoutDirection,
            edges,
            nodes: layoutNodes,
            savedPositionCrossAxisShift,
        }).then((elements) => {
            if (isCancelled) {
                return;
            }

            let targetNodes: Node[];

            if (readOnlyWorkflow) {
                const SHEET_WIDTH = WIDTHS.WORKFLOW_READ_ONLY_SHEET_WIDTH;

                const centeringOffsetX = (canvasWidthRef.current - SHEET_WIDTH) / 2;

                targetNodes = elements.nodes.map((node) => ({
                    ...node,
                    position: {
                        x: node.position.x - centeringOffsetX,
                        y: node.position.y,
                    },
                }));
            } else {
                targetNodes = elements.nodes;
            }

            if (isInitialLayoutRef.current || readOnlyWorkflow) {
                setNodes(targetNodes);
                setEdges(elements.edges);
                isInitialLayoutRef.current = false;
            } else {
                const structureChanged = frozenNodes.length !== targetNodes.length;

                if (structureChanged) {
                    // Snap immediately when nodes are added or removed — animating
                    // a large centering shift looks like a visual jump
                    setNodes(targetNodes);
                    setEdges(elements.edges);
                } else {
                    const previousPositionMap = new Map(frozenNodes.map((node) => [node.id, node.position]));

                    // Place surviving nodes at their frozen positions so they can animate to targets
                    const nodesWithCurrentPositions = targetNodes.map((targetNode) => {
                        const previousPosition = previousPositionMap.get(targetNode.id);

                        return previousPosition ? {...targetNode, position: previousPosition} : targetNode;
                    });

                    // Set final nodes (at frozen positions) and final edges immediately
                    setNodes(nodesWithCurrentPositions);
                    setEdges(elements.edges);

                    cancelAnimationRef.current = animateNodePositions(frozenNodes, targetNodes, setNodes);
                }
            }
        });

        return () => {
            isCancelled = true;

            if (cancelAnimationRef.current) {
                cancelAnimationRef.current();
            }
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [layoutDirection, layoutResetCounter, tasks, triggers, isWorkflowLoaded]);

    useEffect(() => {
        if (canvasWidth > 0 && !isWorkflowLoaded) {
            initializeWithCanvasWidth(canvasWidth);
        }
    }, [canvasWidth, initializeWithCanvasWidth, isWorkflowLoaded]);
}
