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
    TRIGGER_PLACEHOLDER_NODE_ID,
    WORKFLOW_NODES_SIDEBAR_WIDTH,
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
import {useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';
import {useStoreWithEqualityFn} from 'zustand/traditional';

import useDataPillPanelStore from '../stores/useDataPillPanelStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import useRightSidebarStore from '../stores/useRightSidebarStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '../stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '../stores/useWorkflowTestChatStore';
import animateNodePositions from '../utils/animateNodePositions';
import createBranchEdges from '../utils/createBranchEdges';
import createBranchNode from '../utils/createBranchNode';
import createConditionEdges, {hasTaskInConditionBranches} from '../utils/createConditionEdges';
import createConditionNode from '../utils/createConditionNode';
import createEachEdges from '../utils/createEachEdges';
import createEachNode from '../utils/createEachNode';
import createForkJoinEdges from '../utils/createForkJoinEdges';
import createForkJoinNode from '../utils/createForkJoinNode';
import createGraphEdges from '../utils/createGraphEdges';
import createGraphNode from '../utils/createGraphNode';
import createLoopEdges from '../utils/createLoopEdges';
import createLoopNode from '../utils/createLoopNode';
import createMapEdges from '../utils/createMapEdges';
import createMapNode from '../utils/createMapNode';
import createOnErrorEdges, {hasTaskInOnErrorBranches} from '../utils/createOnErrorEdges';
import createOnErrorNode from '../utils/createOnErrorNode';
import createParallelEdges from '../utils/createParallelEdges';
import createParallelNode from '../utils/createParallelNode';
import {getElkLayoutElements} from '../utils/elkLayoutUtils';
import extractDefinitionPositions from '../utils/extractDefinitionPositions';
import {getEffectivelyDisabledTaskNames} from '../utils/getEffectivelyDisabledTaskNames';
import {isElkLayoutActive} from '../utils/isElkLayoutSupported';
import {createLayoutRetryState, onLayoutFailure, onLayoutSuccess} from '../utils/layoutRetryController';
import {
    buildTriggerNodes,
    collectTaskDispatcherData,
    convertTaskToNode,
    createEdgeFromTaskDispatcherBottomGhostNode,
    getLayoutElements,
    getTaskAncestry,
} from '../utils/layoutUtils';
import {containsNodePosition} from '../utils/postDagreConstraints';
import {buildStickyNoteNodes} from '../utils/stickyNoteUtils';
import {forEachNestedTaskGroup} from '../utils/taskTraversalUtils';

/**
 * Builds a string key that changes only when the task graph structure changes
 * (task names, types, nested task counts) but NOT when parameter values change.
 * This prevents unnecessary dagre layout recalculations on every property save.
 */
/**
 * Rewrites edges for read-only rendering: all surviving edges become plain
 * `smoothstep` connectors. `graphTransition` overlay edges are dropped
 * entirely rather than rewritten — they anchor to hidden `-graph-transition-
 * source`/`-graph-transition-target` handles that only `WorkflowNode` and
 * `PlaceholderNode` expose (see createGraphTransitionEdges); read-only
 * rendering swaps those node types for `ReadOnlyNode`/`ReadOnlyPlaceholderNode`,
 * which carry no such handles, so keeping the edges (under any type) would
 * reference connection points that do not exist on the read-only nodes.
 */
export function toReadOnlyLayoutEdges(edges: Edge[]): Edge[] {
    return edges
        .filter((edge) => edge.type !== 'graphTransition')
        .map((edge) => ({
            ...edge,
            type: 'smoothstep',
        }));
}

/**
 * Collects every graph node `next` expression found under any `nodes` array in the given value. The
 * `next` expressions ARE structural for graph dispatchers — they define the rendered transition
 * overlay edges — so the fingerprint must change when one is edited, or the canvas keeps showing the
 * transitions from the last full layout until a page reload.
 */
function collectGraphNextExpressions(value: unknown, sink: string[]): void {
    if (Array.isArray(value)) {
        value.forEach((item) => collectGraphNextExpressions(item, sink));

        return;
    }

    if (!value || typeof value !== 'object') {
        return;
    }

    const record = value as Record<string, unknown>;

    if (Array.isArray(record.nodes)) {
        for (const graphNode of record.nodes as Array<{next?: unknown}>) {
            if (graphNode && typeof graphNode === 'object') {
                sink.push(typeof graphNode.next === 'string' ? graphNode.next : '');
            }
        }
    }

    Object.values(record).forEach((nested) => collectGraphNextExpressions(nested, sink));
}

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

                const graphNextExpressions: string[] = [];

                collectGraphNextExpressions(task.parameters, graphNextExpressions);

                if (graphNextExpressions.length > 0) {
                    parts.push(`next:${graphNextExpressions.join(';')}`);
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
    const layoutEngine = useLayoutEngineStore((state) => state.layoutEngine);

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

    // The read-only canvas (execution detail) has no live store-backed workflow to derive this
    // from at render time the way WorkflowNode.tsx's useDisabledTaskNames does, so it's computed
    // once here -- from the same `tasks` this hook already resolved for the executed workflow
    // version -- and stamped onto each read-only node's data below.
    const disabledTaskNames = useMemo(() => getEffectivelyDisabledTaskNames(tasks ?? []), [tasks]);

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
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const layoutResetCounter = useWorkflowDataStore((state) => state.layoutResetCounter);

    // Bumped to re-run the layout effect after a silent bail (node drag in
    // progress) or a failed layout computation — without it either path left
    // the canvas stale until an unrelated dependency changed.
    const [layoutRetryNonce, setLayoutRetryNonce] = useState(0);

    const cancelAnimationRef = useRef<(() => void) | null>(null);
    const layoutRetryStateRef = useRef(createLayoutRetryState());
    const isInitialLayoutRef = useRef(true);
    const initialCanvasCrossDimRef = useRef<number | undefined>(undefined);
    const initialDirectionRef = useRef<LayoutDirectionType | undefined>(undefined);
    const canvasWidthRef = useRef(canvasWidth);
    const canvasHeightRef = useRef(canvasHeight);
    const previousCopilotPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousDataPillPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousNodeDetailsPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousTestChatPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousLeftSidebarOpenRef = useRef<boolean | undefined>(undefined);
    const previousRightSidebarOpenRef = useRef<boolean | undefined>(undefined);

    canvasWidthRef.current = canvasWidth;
    canvasHeightRef.current = canvasHeight;

    const {placeholderNode: triggerPlaceholderNode, triggerNodes} = useMemo(
        () => buildTriggerNodes(triggers, componentDefinitions, canvasWidth),
        [canvasWidth, componentDefinitions, triggers]
    );

    let allNodes: Array<Node> = [];

    if (tasks) {
        const branchChildTasks = {};
        const conditionChildTasks = {};
        const eachChildTasks = {};
        const forkJoinChildTasks = {};
        const graphChildTasks = {};
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
                graphChildTasks,
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
                taskNode = convertTaskToNode(task, taskDefinition, false);
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
                graphChildTasks,
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
            } else if (componentName === 'graph') {
                allNodes = createGraphNode({
                    allNodes: [...allNodes, taskNode],
                    graphId: taskNode.id,
                    isNested,
                });
            } else {
                allNodes.push(taskNode);
            }
        });
    }

    const finalPlaceholderNode: Node = {
        data: {label: '+'},
        id: FINAL_PLACEHOLDER_NODE_ID,
        position: {x: 0, y: 0},
        type: 'placeholder',
    };

    const firstDownstreamNodeId = allNodes[0]?.id ?? FINAL_PLACEHOLDER_NODE_ID;

    // `allNodes` is fully assembled by this point (the loop above only ever pushes/splices
    // into it), so this mirrors exactly the engine choice `layoutFunction` makes further down
    // from the same two inputs — ELK-only per the phase-3 spec: dagre keeps the phase-2 badges
    // as its sole transition visualization, graphTransition overlay edges are never emitted on
    // that path. Shared `isElkLayoutActive` keeps this gate and the `layoutFunction` gate below
    // from ever diverging — `allNodes` here and `layoutNodes` there are different arrays, but
    // the predicate only inspects each node's taskDispatcher/componentName, so it is equivalent
    // over either one.
    const usesElkGraphTransitionOverlay = isElkLayoutActive(layoutEngine, allNodes);

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
        const isGraphNode = nodeData.componentName === 'graph';
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

        // Create initial edges for the Graph node
        if (isGraphNode) {
            const graphEdges = createGraphEdges(node, {
                orderLanesByVisualPosition: usesElkGraphTransitionOverlay,
            });

            taskEdges.push(...graphEdges);

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
                    orderLanesByVisualPosition: usesElkGraphTransitionOverlay,
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

    if (!allNodes.some((node) => node.id === FINAL_PLACEHOLDER_NODE_ID)) {
        allNodes.push(finalPlaceholderNode);
    }

    // Fan every trigger directly into the first downstream node (no extra rank, so
    // the trigger row stays close and the connectors form a clean horizontal bus).
    // Only the middle trigger's edge is a `workflow` edge so a SINGLE "+" add-button
    // appears on the (near-)vertical center leg; the rest are plain smoothstep
    // connectors with no button.
    // The fan-in treatment (extra rank span, pinned bus, single centered "+") only
    // applies with 2+ triggers. A single trigger is just a normal edge into the
    // first task, identical to a task-to-task connection.
    const isFanIn = triggerNodes.length > 1;
    const middleTriggerIndex = Math.floor(triggerNodes.length / 2);

    // When the triggers feed the final placeholder (no tasks yet), that placeholder node
    // is already the "+" add affordance. Typing the middle edge `workflow` would stack a
    // redundant mid-edge "+" add-button right above the placeholder's own "+", crowding
    // the short connector. Fall back to a plain placeholder edge in that case so a single
    // "+" shows, matching the pre-fan-in, task-less layout.
    const targetIsFinalPlaceholder = firstDownstreamNodeId === FINAL_PLACEHOLDER_NODE_ID;

    const triggerFanInEdges: Array<Edge> = triggerNodes.map((triggerNode, triggerIndex) => {
        const isMiddleEdge = triggerIndex === middleTriggerIndex;

        let type: string = 'smoothstep';

        if (isMiddleEdge) {
            type = targetIsFinalPlaceholder ? 'placeholder' : 'workflow';
        }

        return {
            // triggerFanIn marks fan-in edges: they span extra dagre ranks and the
            // middle `workflow` edge carries the single centered "+". Omitted for a
            // single trigger so it renders as a plain edge.
            data: isFanIn ? {triggerFanIn: true} : undefined,
            id: `${triggerNode.id}=>${firstDownstreamNodeId}`,
            source: triggerNode.id,
            style: EDGE_STYLES,
            target: firstDownstreamNodeId,
            type,
        };
    });

    allNodes = [...triggerNodes, triggerPlaceholderNode, ...allNodes];

    taskEdges.unshift(...triggerFanInEdges);

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
            previousTestChatPanelOpenRef.current = workflowTestChatPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;
            previousRightSidebarOpenRef.current = rightSidebarOpen;

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
            previousTestChatPanelOpenRef.current !== undefined &&
            previousTestChatPanelOpenRef.current !== workflowTestChatPanelOpen
        ) {
            widthDelta += workflowTestChatPanelOpen ? NODE_DETAILS_PANEL_WIDTH : -NODE_DETAILS_PANEL_WIDTH;
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

        if (
            previousRightSidebarOpenRef.current !== undefined &&
            previousRightSidebarOpenRef.current !== rightSidebarOpen
        ) {
            widthDelta += rightSidebarOpen ? WORKFLOW_NODES_SIDEBAR_WIDTH : -WORKFLOW_NODES_SIDEBAR_WIDTH;
        }

        if (widthDelta === 0) {
            previousCopilotPanelOpenRef.current = copilotPanelOpen;
            previousDataPillPanelOpenRef.current = dataPillPanelOpen;
            previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
            previousTestChatPanelOpenRef.current = workflowTestChatPanelOpen;
            previousLeftSidebarOpenRef.current = leftSidebarOpen;
            previousRightSidebarOpenRef.current = rightSidebarOpen;

            return;
        }

        previousCopilotPanelOpenRef.current = copilotPanelOpen;
        previousDataPillPanelOpenRef.current = dataPillPanelOpen;
        previousNodeDetailsPanelOpenRef.current = workflowNodeDetailsPanelOpen;
        previousTestChatPanelOpenRef.current = workflowTestChatPanelOpen;
        previousLeftSidebarOpenRef.current = leftSidebarOpen;
        previousRightSidebarOpenRef.current = rightSidebarOpen;

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
    }, [
        copilotPanelOpen,
        dataPillPanelOpen,
        leftSidebarOpen,
        rightSidebarOpen,
        workflowNodeDetailsPanelOpen,
        workflowTestChatPanelOpen,
    ]);

    useEffect(() => {
        if (useWorkflowDataStore.getState().isNodeDragging) {
            if (process.env.NODE_ENV === 'development') {
                console.warn('useLayout: deferring relayout until node drag ends');
            }

            // Re-run once the drag ends; bailing without a re-trigger would
            // leave the canvas laid out for the previous direction/engine.
            const unsubscribe = useWorkflowDataStore.subscribe((state) => {
                if (!state.isNodeDragging) {
                    unsubscribe();

                    setLayoutRetryNonce((nonce) => nonce + 1);
                }
            });

            return () => unsubscribe();
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
                    const nodeData = node.data as NodeDataType;

                    return {
                        ...node,
                        data: {
                            ...nodeData,
                            clusterElements: undefined,
                            clusterRoot: undefined,
                            isEffectivelyDisabled:
                                Boolean(nodeData.disabled) || disabledTaskNames.has(nodeData.workflowNodeName),
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

            layoutNodes = layoutNodes.filter((node) => node.id !== TRIGGER_PLACEHOLDER_NODE_ID);

            layoutNodes.pop();

            edges = toReadOnlyLayoutEdges(taskEdges);

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

        // Sticky notes are decorative free-floating annotations: excluded from dagre
        // (they must not affect task layout) and appended after layout with their
        // stored positions, following the same cross-axis-shift contract as pinned tasks.
        // Built lazily so the async layout resolution below picks up notes added
        // while the dagre worker was running.
        const buildCurrentStickyNoteNodes = () =>
            buildStickyNoteNodes({
                crossAxis: layoutDirection === 'TB' ? 'x' : 'y',
                crossAxisShift: savedPositionCrossAxisShift,
                definition: readOnlyWorkflow
                    ? readOnlyWorkflow.definition
                    : useWorkflowDataStore.getState().workflow.definition,
                readOnly: !!readOnlyWorkflow,
            });

        const stickyNoteNodes = buildCurrentStickyNoteNodes();

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
        const newNodeIds = new Set([...layoutNodes, ...stickyNoteNodes].map((node) => node.id));
        const prunedNodes = frozenNodes.filter((node) => newNodeIds.has(node.id));

        if (prunedNodes.length < frozenNodes.length) {
            setNodes(prunedNodes);

            const currentEdges = useWorkflowDataStore.getState().edges;

            setEdges(currentEdges.filter((edge) => newNodeIds.has(edge.source) && newNodeIds.has(edge.target)));
        }

        const layoutFunction = isElkLayoutActive(layoutEngine, layoutNodes) ? getElkLayoutElements : getLayoutElements;

        layoutFunction({
            canvasHeight: canvasHeightRef.current,
            canvasWidth: canvasWidthRef.current,
            direction: layoutDirection,
            edges,
            nodes: layoutNodes,
            savedPositionCrossAxisShift,
        })
            .then((elements) => {
                if (isCancelled) {
                    return;
                }

                // Geometry-coupled renderers (LR ring-bar handle flip) key on the engine
                // that actually ran — ELK silently falls back to dagre on unsupported
                // shapes and layout errors, so the selection alone is not authoritative.
                useLayoutEngineStore.getState().setLastAppliedLayoutEngine(elements.engine);

                const targetNodes: Node[] = [...elements.nodes, ...buildCurrentStickyNoteNodes()];

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

                        // Each tween frame re-renders the whole canvas, so big
                        // workflows manage only a few frames per second — scale the
                        // duration with node count so the motion still reads there
                        // while small canvases stay snappy
                        const animationDuration = Math.min(300 + targetNodes.length * 3, 800);

                        cancelAnimationRef.current = animateNodePositions(frozenNodes, targetNodes, setNodes, {
                            duration: animationDuration,
                        });
                    }
                }

                // Cleared only after the nodes are applied without throwing, so a
                // throw from the body above (not just a rejected layout promise)
                // still re-arms the retry guard rather than looping forever.
                onLayoutSuccess(layoutRetryStateRef.current);
            })
            .catch((error) => {
                if (isCancelled) {
                    return;
                }

                // A rejected layout (or a throw applying its result) otherwise
                // leaves the canvas silently frozen at the previous
                // direction/engine — surface it and retry at most once.
                console.error('Workflow layout failed', error);

                if (onLayoutFailure(layoutRetryStateRef.current)) {
                    setLayoutRetryNonce((nonce) => nonce + 1);
                }
            });

        return () => {
            isCancelled = true;

            if (cancelAnimationRef.current) {
                cancelAnimationRef.current();
            }
        };

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [layoutDirection, layoutEngine, layoutResetCounter, layoutRetryNonce, tasks, triggers, isWorkflowLoaded]);

    useEffect(() => {
        if (canvasWidth > 0 && !isWorkflowLoaded && !readOnlyWorkflow) {
            initializeWithCanvasWidth(canvasWidth);
        }
    }, [canvasWidth, initializeWithCanvasWidth, isWorkflowLoaded, readOnlyWorkflow]);
}
