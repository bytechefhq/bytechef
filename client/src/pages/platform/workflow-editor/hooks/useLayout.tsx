import {
    COPILOT_PANEL_WIDTH,
    DATA_PILL_PANEL_WIDTH,
    EDGE_STYLES,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    NODE_DETAILS_PANEL_WIDTH,
    ON_ERROR_WIRE_KEY_ERROR_BRANCH,
    ON_ERROR_WIRE_KEY_MAIN_BRANCH,
    PROJECT_LEFT_SIDEBAR_WIDTH,
    TASK_DISPATCHER_NAMES,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {ComponentIcon} from 'lucide-react';
import {useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';
import {useStoreWithEqualityFn} from 'zustand/traditional';

import useDataPillPanelStore from '../stores/useDataPillPanelStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
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
import createOnErrorEdges, {hasTaskInOnErrorBranches} from '../utils/createOnErrorEdges';
import createOnErrorNode from '../utils/createOnErrorNode';
import createParallelEdges from '../utils/createParallelEdges';
import createParallelNode from '../utils/createParallelNode';
import extractDefinitionPositions from '../utils/extractDefinitionPositions';
import {
    collectTaskDispatcherData,
    convertTaskToNode,
    createDefaultNodes,
    createEdgeFromTaskDispatcherBottomGhostNode,
    getLayoutElements,
    getTaskAncestry,
} from '../utils/layoutUtils';
import {containsNodePosition} from '../utils/postDagreConstraints';
import {forEachNestedTaskGroup} from '../utils/taskTraversalUtils';

/**
 * Builds a string key that changes only when the task graph structure changes
 * (task names, types, nested task counts) but NOT when parameter values change.
 * This prevents unnecessary dagre layout recalculations on every property save.
 */
export function getTasksStructuralFingerprint(tasks: WorkflowTask[]): string {
    return tasks
        .map((task) => {
            const hasFilledClusterElements =
                task.clusterElements &&
                Object.values(task.clusterElements).some(
                    (value) => value !== null && value !== undefined && !(Array.isArray(value) && value.length === 0)
                );

            const parts = [task.name, task.type, task.clusterRoot ? 'cr' : '', hasFilledClusterElements ? 'ce' : ''];

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
    copilotPanelOpen?: boolean;
    direction?: LayoutDirectionType;
    leftSidebarOpen?: boolean;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: Array<TaskDispatcherDefinitionBasic>;
}

export default function useLayout({
    canvasHeight,
    canvasWidth,
    componentDefinitions,
    copilotPanelOpen,
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

    const {initializeWithCanvasWidth, setEdges, setNodes, setSavedPositionCrossAxisShift} = useWorkflowDataStore(
        useShallow((state) => ({
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
    const previousCopilotPanelOpenRef = useRef<boolean | undefined>(undefined);
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
        const onErrorChildTasks = {};
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
                onErrorChildTasks,
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
                onErrorChildTasks,
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
            } else if (componentName === 'on-error') {
                const errorBranch = parameters?.[ON_ERROR_WIRE_KEY_ERROR_BRANCH];
                const mainBranch = parameters?.[ON_ERROR_WIRE_KEY_MAIN_BRANCH];
                const hasErrorBranchTasks = Array.isArray(errorBranch) && errorBranch.length > 0;
                const hasMainBranchTasks = Array.isArray(mainBranch) && mainBranch.length > 0;

                allNodes = createOnErrorNode({
                    allNodes: [...allNodes, taskNode],
                    isNested,
                    onErrorId: taskNode.id,
                    options: {
                        createLeftPlaceholder: !hasMainBranchTasks,
                        createRightPlaceholder: !hasErrorBranchTasks,
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
        const isOnErrorNode = nodeData.componentName === 'on-error';

        const isConditionPlaceholderNode = nodeData.conditionId && node.type === 'placeholder';
        const isBranchPlaceholderNode = nodeData.branchId && node.type === 'placeholder';
        const isOnErrorPlaceholderNode = nodeData.onErrorId && node.type === 'placeholder';

        const isConditionChildTask = nodeData.conditionData;
        const isOnErrorChildTask = nodeData.onErrorData;

        const nextNode = allNodes[index + 1];

        // Create initial edges for the Condition node
        if (isConditionNode) {
            const conditionEdges = createConditionEdges(node, allNodes);

            taskEdges.push(...conditionEdges);

            return;
        }

        // Create initial edges for the on-error node
        if (isOnErrorNode) {
            const onErrorEdges = createOnErrorEdges(node, allNodes);

            taskEdges.push(...onErrorEdges);

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

            if (isOnErrorChildTask || node.id.includes('-onError-')) {
                const onErrorDispatcherId = isOnErrorChildTask ? nodeData.onErrorData?.onErrorId : nodeData.onErrorId;

                if (onErrorDispatcherId) {
                    const isNextNodeInSameOnError =
                        nextNodeData.onErrorData?.onErrorId === onErrorDispatcherId ||
                        hasTaskInOnErrorBranches(onErrorDispatcherId, nextNode.id, tasks);

                    const isOwnBottomGhost =
                        isNextNodeTaskDispatcherBottomNode &&
                        nextNode.id === `${onErrorDispatcherId}-onError-bottom-ghost`;

                    const isInDifferentOnErrorBranches =
                        nodeData.onErrorData &&
                        nextNodeData.onErrorData &&
                        nodeData.onErrorData.onErrorCase !== nextNodeData.onErrorData.onErrorCase;

                    if (isInDifferentOnErrorBranches || (!isNextNodeInSameOnError && !isOwnBottomGhost)) {
                        return;
                    }
                }
            }

            if (isConditionPlaceholderNode || isBranchPlaceholderNode || isOnErrorPlaceholderNode) {
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
        // Skip shift updates while the cluster elements canvas covers the main
        // graph — the canvasWidth change from the dialog's panel is transient
        // and would produce incorrect shifts for saved-position nodes.
        const {clusterElementsCanvasOpen} = useWorkflowEditorStore.getState();

        if (clusterElementsCanvasOpen) {
            return;
        }

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
            previousCopilotPanelOpenRef.current = copilotPanelOpen;
            previousDataPillPanelOpenRef.current = dataPillPanelOpen;
            previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;

            return;
        }

        // Skip layout reset when the cluster elements canvas is open or
        // closing — the dialog covers the graph so shifting is unnecessary
        // and causes a visible jump during the dialog transition.
        const {clusterElementsCanvasOpen} = useWorkflowEditorStore.getState();

        if (clusterElementsCanvasOpen) {
            return;
        }

        let widthDelta = 0;

        if (
            previousCopilotPanelOpenRef.current !== undefined &&
            previousCopilotPanelOpenRef.current !== copilotPanelOpen
        ) {
            widthDelta += copilotPanelOpen ? COPILOT_PANEL_WIDTH : -COPILOT_PANEL_WIDTH;
        }

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
            previousCopilotPanelOpenRef.current = copilotPanelOpen;
            previousDataPillPanelOpenRef.current = dataPillPanelOpen;
            previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;

            return;
        }

        previousCopilotPanelOpenRef.current = copilotPanelOpen;
        previousDataPillPanelOpenRef.current = dataPillPanelOpen;
        previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
        previousLeftSidebarOpenRef.current = leftSidebarOpen;

        if (cancelAnimationRef.current) {
            cancelAnimationRef.current();
            cancelAnimationRef.current = null;
        }

        // Shift all existing nodes horizontally by half the width delta rather
        // than triggering a full dagre recalculation. A full layout recomputation
        // would re-run alignChainNodesCrossAxis which aligns non-saved nodes to
        // saved predecessors — an alignment that may not have existed before the
        // panel toggle, causing mixed (saved + dagre) layouts to visibly
        // rearrange instead of uniformly shifting.
        // Side panels always change horizontal width, so the shift is always on x
        // regardless of layout direction.
        const shift = -widthDelta / 2;
        const {nodes: currentNodes, setNodes: updateNodes} = useWorkflowDataStore.getState();

        const shiftedNodes = currentNodes.map((node) => ({
            ...node,
            position: {
                ...node.position,
                x: node.position.x + shift,
            },
        }));

        cancelAnimationRef.current = animateNodePositions(currentNodes, shiftedNodes, updateNodes);
    }, [copilotPanelOpen, dataPillPanelOpen, leftSidebarOpen, workflowNodeDetailsPanelOpen]);

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

        // Sync position metadata from the latest workflow definition into layout
        // nodes. storeTasks uses fingerprint equality that ignores position metadata,
        // so allNodes may have stale/missing positions when the layout is triggered
        // by panel toggles or position resets. The definition is the source of truth.
        if (!readOnlyWorkflow) {
            const currentDefinition = useWorkflowDataStore.getState().workflow.definition;

            if (currentDefinition) {
                const definitionPositions = extractDefinitionPositions(currentDefinition);

                layoutNodes = layoutNodes.map((node) => {
                    const nodeData = node.data as NodeDataType;
                    const definitionPosition = definitionPositions.get(node.id);
                    const hasNodePosition = containsNodePosition(nodeData.metadata);

                    if (definitionPosition) {
                        const currentNodePosition = hasNodePosition ? nodeData.metadata!.ui!.nodePosition : undefined;

                        if (
                            !currentNodePosition ||
                            currentNodePosition.x !== definitionPosition.x ||
                            currentNodePosition.y !== definitionPosition.y
                        ) {
                            return {
                                ...node,
                                data: {
                                    ...nodeData,
                                    metadata: {
                                        ...nodeData.metadata,
                                        ui: {
                                            ...nodeData.metadata?.ui,
                                            nodePosition: definitionPosition,
                                        },
                                    },
                                },
                            };
                        }
                    } else if (hasNodePosition) {
                        // Node has position but definition doesn't — position was removed
                        return {
                            ...node,
                            data: {
                                ...nodeData,
                                metadata: {
                                    ...nodeData.metadata,
                                    ui: {
                                        ...nodeData.metadata?.ui,
                                        nodePosition: undefined,
                                    },
                                },
                            },
                        };
                    }

                    return node;
                });
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

        // Immediately remove nodes that are no longer part of the layout so that
        // deleted task dispatcher children disappear at the same time as the parent,
        // rather than lingering until the async layout calculation resolves.
        const newNodeIds = new Set(layoutNodes.map((node) => node.id));
        const prunedNodes = frozenNodes.filter((node) => newNodeIds.has(node.id));

        if (prunedNodes.length < frozenNodes.length) {
            setNodes(prunedNodes);

            const currentEdges = useWorkflowDataStore.getState().edges;

            setEdges(currentEdges.filter((edge) => newNodeIds.has(edge.source) && newNodeIds.has(edge.target)));
        }

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

            const targetNodes: Node[] = elements.nodes;

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
        if (canvasWidth > 0 && !isWorkflowLoaded && !readOnlyWorkflow) {
            initializeWithCanvasWidth(canvasWidth);
        }
    }, [canvasWidth, initializeWithCanvasWidth, isWorkflowLoaded, readOnlyWorkflow]);
}
