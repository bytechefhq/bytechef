import {
    COPILOT_PANEL_WIDTH,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    PROJECT_LEFT_SIDEBAR_WIDTH,
    TRIGGER_PLACEHOLDER_NODE_ID,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
    WorkflowTask,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {useEffect, useMemo, useRef, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';
import {useStoreWithEqualityFn} from 'zustand/traditional';

import useClusterElementNodes from '../../cluster-element-editor/hooks/useClusterElementNodes';
import {getNestedClusterElements, isPlainObject} from '../../cluster-element-editor/utils/clusterElementsUtils';
import {useWorkflowExecutionOverlay} from '../providers/workflowExecutionOverlayContext';
import useClusterFrameCollapsedStore from '../stores/useClusterFrameCollapsedStore';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import useLayoutEngineStore from '../stores/useLayoutEngineStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';
import useWorkflowEditorStore from '../stores/useWorkflowEditorStore';
import animateNodePositions from '../utils/animateNodePositions';
import buildWorkflowGraph from '../utils/buildWorkflowGraph';
import {layoutClusterFrames} from '../utils/clusterFrame/layoutClusterFrames';
import {getElkLayoutElements} from '../utils/elkLayoutUtils';
import extractDefinitionPositions from '../utils/extractDefinitionPositions';
import {getEffectivelyDisabledTaskNames} from '../utils/getEffectivelyDisabledTaskNames';
import isElkLayoutSupported from '../utils/isElkLayoutSupported';
import {createLayoutRetryState, onLayoutFailure, onLayoutSuccess} from '../utils/layoutRetryController';
import {getLayoutElements} from '../utils/layoutUtils';
import {containsNodePosition} from '../utils/postDagreConstraints';
import {
    getReadOnlyFramedClusterRootIds,
    toReadOnlyClusterFrameMembers,
    toReadOnlyFramedClusterRoot,
} from '../utils/readOnlyClusterFrames';
import removeTrailingBranchPlaceholders from '../utils/removeTrailingBranchPlaceholders';
import shiftNodesHorizontally from '../utils/shiftNodesHorizontally';
import {buildStickyNoteNodes} from '../utils/stickyNoteUtils';
import {forEachNestedTaskGroup} from '../utils/taskTraversalUtils';
import useClusterElementsViewMode from './useClusterElementsViewMode';

function getClusterElementsFingerprint(clusterElements: unknown): string {
    if (!clusterElements || typeof clusterElements !== 'object') {
        return '';
    }

    return Object.entries(clusterElements as Record<string, unknown>)
        .sort(([firstElementKey], [secondElementKey]) => firstElementKey.localeCompare(secondElementKey))
        .map(([elementKey, elementValue]) => {
            const elements = (Array.isArray(elementValue) ? elementValue : [elementValue]).filter(
                (element): element is {name?: string; type?: string; workflowNodeName?: string} =>
                    isPlainObject(element)
            );

            if (elements.length === 0) {
                return '';
            }

            const elementFingerprints = elements.map((element) => {
                const elementName = element.name || element.workflowNodeName;
                const nestedClusterElements = getNestedClusterElements(element);
                const nestedFingerprint = getClusterElementsFingerprint(nestedClusterElements);
                const nestedFingerprintPart = nestedFingerprint ? '{' + nestedFingerprint + '}' : '';
                const nodePosition = (element as {metadata?: {ui?: {nodePosition?: {x: number; y: number}}}}).metadata
                    ?.ui?.nodePosition;
                const positionPart = nodePosition ? `@${nodePosition.x},${nodePosition.y}` : '';

                return `${elementName}:${element.type}${positionPart}${nestedFingerprintPart}`;
            });

            return `${elementKey}[${elementFingerprints.join(';')}]`;
        })
        .filter(Boolean)
        .join(';');
}

/**
 * Builds a string key that changes only when the task graph structure changes
 * (task names, types, nested task counts, cluster element names and types) but NOT when parameter values change.
 * This prevents unnecessary dagre layout recalculations on every property save.
 */
export function getTasksStructuralFingerprint(tasks: WorkflowTask[]): string {
    return tasks
        .map((task) => {
            const parts = [
                task.name,
                task.type,
                task.clusterRoot ? 'cr' : '',
                getClusterElementsFingerprint(task.clusterElements),
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
    const clusterFrameLockedByRootId = useWorkflowEditorStore((state) => state.clusterFrameLockedByRootId);

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

    const disabledTaskNames = useMemo(() => getEffectivelyDisabledTaskNames(tasks ?? []), [tasks]);

    const {clearCanvas, setEdges, setNodes, setSavedPositionCrossAxisShift} = useWorkflowDataStore(
        useShallow((state) => ({
            clearCanvas: state.clearCanvas,
            setEdges: state.setEdges,
            setNodes: state.setNodes,
            setSavedPositionCrossAxisShift: state.setSavedPositionCrossAxisShift,
        }))
    );
    const layoutResetCounter = useWorkflowDataStore((state) => state.layoutResetCounter);

    // Bumped to re-run the layout effect after a silent bail (node drag in
    // progress) or a failed layout computation — without it either path left
    // the canvas stale until an unrelated dependency changed.
    const [layoutRetryNonce, setLayoutRetryNonce] = useState(0);
    const clusterElementsViewMode = useClusterElementsViewMode();

    const workflowId = useWorkflowDataStore((state) => state.workflow.id);
    const collapsedClusterRootIds = useClusterFrameCollapsedStore((state) =>
        workflowId ? state.collapsedByWorkflowId[workflowId] : undefined
    );

    const workflowExecutionOverlay = useWorkflowExecutionOverlay();

    const isReadOnlyWorkflow = !!readOnlyWorkflow;
    const readOnlyWorkflowTasks = readOnlyWorkflow?.tasks;

    const framesReadOnlyClusterRoots = isReadOnlyWorkflow && !!workflowExecutionOverlay;

    const boxModeClusterRootIds = useMemo(() => {
        if (isReadOnlyWorkflow) {
            return getReadOnlyFramedClusterRootIds(readOnlyWorkflowTasks, framesReadOnlyClusterRoots);
        }

        return clusterElementsViewMode === 'box'
            ? (tasks ?? [])
                  .filter((task) => task.clusterRoot && !collapsedClusterRootIds?.[task.name])
                  .map((task) => task.name)
            : [];
    }, [
        clusterElementsViewMode,
        collapsedClusterRootIds,
        framesReadOnlyClusterRoots,
        isReadOnlyWorkflow,
        readOnlyWorkflowTasks,
        tasks,
    ]);

    const {definitionsReady, edgesByRootId, nodesByRootId} = useClusterElementNodes(
        boxModeClusterRootIds,
        framesReadOnlyClusterRoots ? readOnlyWorkflow?.definition : undefined
    );

    const cancelAnimationRef = useRef<(() => void) | null>(null);
    const layoutRetryStateRef = useRef(createLayoutRetryState());
    const isInitialLayoutRef = useRef(true);
    const initialCanvasCrossDimRef = useRef<number | undefined>(undefined);
    const initialDirectionRef = useRef<LayoutDirectionType | undefined>(undefined);
    const canvasWidthRef = useRef(canvasWidth);
    const canvasHeightRef = useRef(canvasHeight);
    const previousCopilotPanelOpenRef = useRef<boolean | undefined>(undefined);
    const previousLeftSidebarOpenRef = useRef<boolean | undefined>(undefined);

    canvasWidthRef.current = canvasWidth;
    canvasHeightRef.current = canvasHeight;

    const {edges: taskEdges, nodes: allNodes} = useMemo(
        () => buildWorkflowGraph({canvasWidth, componentDefinitions, taskDispatcherDefinitions, tasks, triggers}),
        [canvasWidth, componentDefinitions, taskDispatcherDefinitions, tasks, triggers]
    );

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
            previousLeftSidebarOpenRef.current !== undefined &&
            previousLeftSidebarOpenRef.current !== leftSidebarOpen
        ) {
            widthDelta += leftSidebarOpen ? PROJECT_LEFT_SIDEBAR_WIDTH : -PROJECT_LEFT_SIDEBAR_WIDTH;
        }

        previousCopilotPanelOpenRef.current = copilotPanelOpen;
        previousLeftSidebarOpenRef.current = leftSidebarOpen;

        if (widthDelta === 0) {
            return;
        }

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

        const shiftedNodes = shiftNodesHorizontally(currentNodes, shift);

        cancelAnimationRef.current = animateNodePositions(currentNodes, shiftedNodes, updateNodes);
    }, [copilotPanelOpen, leftSidebarOpen]);

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

        if (boxModeClusterRootIds.length > 0 && !definitionsReady) {
            return;
        }

        let isCancelled = false;

        let layoutNodes = allNodes;
        let edges: Edge[] = taskEdges;

        if (readOnlyWorkflow) {
            layoutNodes = allNodes.map((node) => {
                if (boxModeClusterRootIds.includes(node.id) && nodesByRootId[node.id]?.length) {
                    return toReadOnlyFramedClusterRoot(node, disabledTaskNames);
                }

                if (node.type === 'workflow' || node.type === 'clusterRoot') {
                    const nodeData = node.data as NodeDataType;

                    return {
                        ...node,
                        data: {
                            ...nodeData,
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

            edges = taskEdges.map((edge) => ({
                ...edge,
                type: 'smoothstep',
            }));

            const lastEdge = edges[edges.length - 1];
            if (lastEdge && lastEdge.target === FINAL_PLACEHOLDER_NODE_ID) {
                edges.pop();
            }
        }

        const layoutWithoutTrailingBranchPlaceholders = removeTrailingBranchPlaceholders(layoutNodes, edges);

        const keptLayoutNodeIds = new Set(layoutWithoutTrailingBranchPlaceholders.nodes.map((node) => node.id));

        const hiddenTrailingBranchPlaceholderNodes: Node[] = readOnlyWorkflow
            ? []
            : layoutNodes.filter((node) => !keptLayoutNodeIds.has(node.id)).map((node) => ({...node, hidden: true}));

        ({edges, nodes: layoutNodes} = layoutWithoutTrailingBranchPlaceholders);

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
        const newNodeIds = new Set(
            [
                ...layoutNodes,
                ...hiddenTrailingBranchPlaceholderNodes,
                ...stickyNoteNodes,
                ...Object.values(nodesByRootId).flat(),
            ].map((node) => node.id)
        );
        const prunedNodes = frozenNodes.filter((node) => newNodeIds.has(node.id));

        if (prunedNodes.length < frozenNodes.length) {
            setNodes(prunedNodes);

            const currentEdges = useWorkflowDataStore.getState().edges;

            setEdges(currentEdges.filter((edge) => newNodeIds.has(edge.source) && newNodeIds.has(edge.target)));
        }

        const framedClusters = layoutClusterFrames(
            layoutNodes,
            edges,
            {edgesByRootId, nodesByRootId},
            clusterFrameLockedByRootId
        );

        const layoutFunction =
            layoutEngine === 'elk' && isElkLayoutSupported(layoutNodes) ? getElkLayoutElements : getLayoutElements;

        layoutFunction({
            canvasHeight: canvasHeightRef.current,
            canvasWidth: canvasWidthRef.current,
            direction: layoutDirection,
            edges: framedClusters.outerEdges,
            nodes: framedClusters.outerNodes,
            savedPositionCrossAxisShift,
        })
            .then((layoutElements) => {
                if (isCancelled) {
                    return;
                }

                useLayoutEngineStore.getState().setLastAppliedLayoutEngine(layoutElements.engine);

                const clusterMembers = readOnlyWorkflow
                    ? toReadOnlyClusterFrameMembers(framedClusters.memberNodes, framedClusters.memberEdges)
                    : {memberEdges: framedClusters.memberEdges, memberNodes: framedClusters.memberNodes};

                const elements = {
                    ...layoutElements,
                    edges: [...layoutElements.edges, ...clusterMembers.memberEdges],
                    nodes: [...layoutElements.nodes, ...clusterMembers.memberNodes],
                };

                const targetNodes: Node[] = [
                    ...elements.nodes,
                    ...hiddenTrailingBranchPlaceholderNodes,
                    ...buildCurrentStickyNoteNodes(),
                ];

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
    }, [
        layoutDirection,
        layoutEngine,
        layoutResetCounter,
        layoutRetryNonce,
        tasks,
        triggers,
        isWorkflowLoaded,
        boxModeClusterRootIds,
        definitionsReady,
        edgesByRootId,
        nodesByRootId,
        clusterFrameLockedByRootId,
        clusterElementsViewMode,
    ]);

    useEffect(() => {
        if (!isWorkflowLoaded && !readOnlyWorkflow) {
            clearCanvas();
        }
    }, [clearCanvas, isWorkflowLoaded, readOnlyWorkflow]);
}
