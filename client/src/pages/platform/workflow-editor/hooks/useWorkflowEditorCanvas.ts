import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    COPILOT_PANEL_WIDTH,
    FINAL_PLACEHOLDER_NODE_ID,
    LayoutDirectionType,
    PROJECT_LEFT_SIDEBAR_WIDTH,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {NodeDataType, UpdateWorkflowMutationType} from '@/shared/types';
import {Node, NodeChange, XYPosition, useNodesInitialized, useReactFlow} from '@xyflow/react';
import {DragEventHandler, useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import ClusterElementEdge from '../../cluster-element-editor/edges/ClusterElementEdge';
import LabeledClusterElementsEdge from '../../cluster-element-editor/edges/LabeledClusterElementsEdge';
import GraphStartEdge from '../edges/GraphStartEdge';
import GraphTransitionEdge from '../edges/GraphTransitionEdge';
import LabeledBranchCaseEdge from '../edges/LabeledBranchCaseEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import RoundedSmoothStepEdge from '../edges/RoundedSmoothStepEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useCanvasHeightViewport from '../hooks/useCanvasHeightViewport';
import useGraphConnections from '../hooks/useGraphConnections';
import useHandleDrop from '../hooks/useHandleDrop';
import useInitialViewport from '../hooks/useInitialViewport';
import useLayout from '../hooks/useLayout';
import useOverlayPanelsViewport from '../hooks/useOverlayPanelsViewport';
import useStickyNotes from '../hooks/useStickyNotes';
import ClusterRootNode from '../nodes/ClusterRootNode';
import GraphFrameNode from '../nodes/GraphFrameNode';
import GraphStartNode from '../nodes/GraphStartNode';
import PlaceholderNode from '../nodes/PlaceholderNode';
import ReadOnlyClusterElementNode from '../nodes/ReadOnlyClusterElementNode';
import ReadOnlyNode from '../nodes/ReadOnlyNode';
import ReadOnlyPlaceholderNode from '../nodes/ReadOnlyPlaceholderNode';
import StickyNoteNode from '../nodes/StickyNoteNode';
import TaskDispatcherBottomGhostNode from '../nodes/TaskDispatcherBottomGhostNode';
import TaskDispatcherLeftGhostNode from '../nodes/TaskDispatcherLeftGhostNode';
import TaskDispatcherTopGhostNode from '../nodes/TaskDispatcherTopGhostNode';
import TriggerPlaceholderNode from '../nodes/TriggerPlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import {handleCanvasDragOver, handleCanvasDrop} from '../utils/canvasDrop';
import clearAllNodePositions from '../utils/clearAllNodePositions';
import {fromClusterFrameChildPosition} from '../utils/clusterFrame/clusterFrameGeometry';
import {collectAllDescendantNodes, collectChainSuccessorNodes} from '../utils/collectDescendantNodes';
import {
    DraggingPlaceholderStateType,
    buildDraggingPlaceholderState,
    computePlaceholderDragPosition,
} from '../utils/dragTrailingPlaceholder';
import {getTask} from '../utils/getTask';
import {registerAutoPlacedGraphPositions, takeAutoPlacedGraphPositions} from '../utils/graph/autoPlacedGraphPositions';
import {toGraphContentPosition} from '../utils/graph/graphConnections';
import {GRAPH_FRAME_ID_ATTRIBUTE, getGraphFrameId, getGraphIdFromFrameNodeId} from '../utils/graph/graphFrameGeometry';
import {resizeGraphFrameForMembers} from '../utils/graph/graphFrameResize';
import {
    buildGraphMemberDragStopPositions,
    filterToSharedParent,
    getGraphMemberGroupInset,
} from '../utils/graph/graphMemberDrag';
import {findSelectedGraphTransition, isCanvasDeleteKeyTarget} from '../utils/graph/graphTransitionDeleteKey';
import {removeTransition} from '../utils/graph/graphTransitionMutations';
import {saveGraphTransitions} from '../utils/graph/saveGraphParameters';
import {extractLayoutDirection} from '../utils/layoutDirectionDefinitionUtils';
import {containsNodePosition} from '../utils/postDagreConstraints';
import {resolveClusterRootId} from '../utils/resolveClusterRootId';
import saveWorkflowDefinition from '../utils/saveWorkflowDefinition';
import saveWorkflowNodesPosition from '../utils/saveWorkflowNodesPosition';
import {STICKY_NOTE_NODE_TYPE, compensateStickyNotePosition, updateStickyNote} from '../utils/stickyNoteUtils';
import updateClusterElementsPositions from '../utils/updateClusterElementsPositions';
import {isWorkflowMutating} from '../utils/workflowMutationGuard';

interface UseWorkflowEditorCanvasParamsI {
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    fitViewOnLoad?: boolean;
    fitViewOnWorkflowChange?: boolean;
    leftSidebarOpen?: boolean;
    readOnlyLayoutDirection?: LayoutDirectionType;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const GRAPH_FRAME_ID_SELECTOR = `[${GRAPH_FRAME_ID_ATTRIBUTE}]`;

const getGraphFrameElementId = (target: EventTarget | null) =>
    target instanceof HTMLElement
        ? (target.closest(GRAPH_FRAME_ID_SELECTOR)?.getAttribute(GRAPH_FRAME_ID_ATTRIBUTE) ?? undefined)
        : undefined;

const isGraphFrameDropTarget = (target: EventTarget | null) => !!getGraphFrameElementId(target);

interface SaveDraggedClusterElementPositionProps {
    draggedNode: Node;
    parentClusterRootId: string;
    updateWorkflowMutation: UpdateWorkflowMutationType;
}

function saveDraggedClusterElementPosition({
    draggedNode,
    parentClusterRootId,
    updateWorkflowMutation,
}: SaveDraggedClusterElementPositionProps) {
    const {nodes: currentNodes, workflow: currentWorkflow} = useWorkflowDataStore.getState();

    const workflowDefinitionTasks = currentWorkflow.definition
        ? (JSON.parse(currentWorkflow.definition).tasks ?? [])
        : [];

    const topLevelClusterRootId = resolveClusterRootId(draggedNode.data as NodeDataType) ?? parentClusterRootId;

    const clusterRootTask = getTask({
        tasks: workflowDefinitionTasks,
        workflowNodeName: topLevelClusterRootId,
    });

    if (!clusterRootTask?.clusterElements) {
        return;
    }

    const clusterFrameRootNode = currentNodes.find((node) => node.id === topLevelClusterRootId);

    const clusterFrame = (clusterFrameRootNode?.data as NodeDataType | undefined)?.clusterFrame;

    const draggedNodePosition =
        draggedNode.parentId === topLevelClusterRootId
            ? fromClusterFrameChildPosition(draggedNode.position, clusterFrame?.contentOrigin)
            : draggedNode.position;

    const updatedClusterElements = updateClusterElementsPositions({
        clusterElements: clusterRootTask.clusterElements,
        movedClusterElementId: draggedNode.id,
        nodePositions: {
            [draggedNode.id]: draggedNodePosition,
        },
    });

    void saveWorkflowDefinition({
        nodeData: {
            ...clusterRootTask,
            clusterElements: updatedClusterElements,
            componentName: clusterRootTask.type.split('/')[0],
            workflowNodeName: topLevelClusterRootId,
        },
        updateWorkflowMutation,
    });
}

const useWorkflowEditorCanvas = ({
    componentDefinitions,
    customCanvasWidth,
    fitViewOnLoad,
    fitViewOnWorkflowChange,
    leftSidebarOpen,
    readOnlyLayoutDirection,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: UseWorkflowEditorCanvasParamsI) => {
    const storeWorkflow = useWorkflowDataStore((state) => state.workflow);

    const workflow = readOnlyWorkflow || storeWorkflow;

    const workflowId = workflow.id!;
    const {incrementLayoutResetCounter, onNodesChange, setIsNodeDragging, setNodes} = useWorkflowDataStore(
        useShallow((state) => ({
            incrementLayoutResetCounter: state.incrementLayoutResetCounter,
            onNodesChange: state.onNodesChange,
            setIsNodeDragging: state.setIsNodeDragging,
            setNodes: state.setNodes,
        }))
    );
    const {applyStoredLayoutDirection, layoutDirection, resetLayoutDirection, setCurrentWorkflowUuid} =
        useLayoutDirectionStore(
            useShallow((state) => ({
                applyStoredLayoutDirection: state.applyStoredLayoutDirection,
                layoutDirection: state.layoutDirection,
                resetLayoutDirection: state.resetLayoutDirection,
                setCurrentWorkflowUuid: state.setCurrentWorkflowUuid,
            }))
        );
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const resetWorkflowLayout = useWorkflowEditorStore((state) => state.resetWorkflowLayout);

    const {fitView, getInternalNode, screenToFlowPosition} = useReactFlow();

    const nodesInitialized = useNodesInitialized();

    const {invalidateWorkflowQueries: editorInvalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const {handleAddStickyNote} = useStickyNotes({readOnly: !!readOnlyWorkflow});

    const [
        handleDropOnPlaceholderNode,
        handleDropOnWorkflowEdge,
        handleDropOnTriggerNode,
        handleDropOnTriggerPlaceholder,
        handleDropOnGraphFrame,
        handleDropOnClusterElementPlaceholder,
    ] = useHandleDrop({
        taskDispatcherDefinitions,
    });

    const draggingDispatcherIdRef = useRef<string | null>(null);
    const dispatcherDragStartRef = useRef<XYPosition | null>(null);
    const childDragStartRef = useRef<Map<string, XYPosition>>(new Map());
    const draggingPlaceholderRef = useRef<DraggingPlaceholderStateType | null>(null);
    const resetPendingRef = useRef(false);

    const {handleConnect, handleConnectEnd, handleReconnect, isValidConnection} = useGraphConnections({
        updateWorkflowMutation,
    });

    const handleTransitionDeleteKeyDown = useCallback(
        (event: KeyboardEvent) => {
            if (readOnlyWorkflow || !updateWorkflowMutation) {
                return;
            }

            if (event.repeat || (event.key !== 'Backspace' && event.key !== 'Delete')) {
                return;
            }

            if (!isCanvasDeleteKeyTarget(document.activeElement)) {
                return;
            }

            const {edges} = useWorkflowDataStore.getState();

            const selectedTransition = findSelectedGraphTransition(edges);

            if (!selectedTransition) {
                return;
            }

            event.preventDefault();

            saveGraphTransitions({
                graphId: selectedTransition.graphId,
                mutate: (transitions) =>
                    removeTransition({
                        expectedTransition: selectedTransition.transition,
                        index: selectedTransition.index,
                        transitions,
                    }),
                updateWorkflowMutation,
            });
        },
        [readOnlyWorkflow, updateWorkflowMutation]
    );

    const nodeTypes = useMemo(
        () => ({
            clusterRoot: ClusterRootNode,
            graphFrame: GraphFrameNode,
            graphStart: GraphStartNode,
            placeholder: PlaceholderNode,
            readonly: ReadOnlyNode,
            readonlyClusterElement: ReadOnlyClusterElementNode,
            readonlyPlaceholder: ReadOnlyPlaceholderNode,
            stickyNote: StickyNoteNode,
            taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
            taskDispatcherLeftGhostNode: TaskDispatcherLeftGhostNode,
            taskDispatcherTopGhostNode: TaskDispatcherTopGhostNode,
            triggerPlaceholder: TriggerPlaceholderNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            clusterElement: ClusterElementEdge,
            graphStart: GraphStartEdge,
            graphTransition: GraphTransitionEdge,
            labeledBranchCase: LabeledBranchCaseEdge,
            labeledClusterElementsEdge: LabeledClusterElementsEdge,
            placeholder: PlaceholderEdge,
            smoothstep: RoundedSmoothStepEdge,
            workflow: WorkflowEdge,
        }),
        []
    );

    const onDragOver: DragEventHandler = useCallback((event) => {
        const {edges, nodes} = useWorkflowDataStore.getState();

        handleCanvasDragOver({edges, event, isAdditionalDropTarget: isGraphFrameDropTarget, nodes});
    }, []);

    const onDrop: DragEventHandler = useCallback((event) => {
        const {edges, nodes} = useWorkflowDataStore.getState();

        handleCanvasDrop({
            componentDefinitions,
            edges,
            event,
            handlers: {
                onDropOnClusterElementPlaceholder: (targetNode, droppedNode) =>
                    void handleDropOnClusterElementPlaceholder(targetNode, droppedNode),
                onDropOnGraphFrame: (droppedNode) => {
                    const graphId = getGraphFrameElementId(event.target);

                    const graphFrameNodeId = graphId ? getGraphFrameId(graphId) : undefined;

                    const graphFrameNode = graphFrameNodeId ? getInternalNode(graphFrameNodeId) : undefined;

                    if (!graphId || !graphFrameNode) {
                        return false;
                    }

                    const screenPosition = {x: event.clientX, y: event.clientY};

                    const flowPosition = screenToFlowPosition(screenPosition);

                    const dropPosition = toGraphContentPosition({
                        flowPosition,
                        frameAbsolutePosition: graphFrameNode.internals.positionAbsolute,
                    });

                    handleDropOnGraphFrame({dropPosition, droppedNode, graphId});

                    return true;
                },
                onDropOnPlaceholderNode: handleDropOnPlaceholderNode,
                onDropOnTriggerNode: handleDropOnTriggerNode,
                onDropOnTriggerPlaceholder: handleDropOnTriggerPlaceholder,
                onDropOnWorkflowEdge: handleDropOnWorkflowEdge,
            },
            nodes,
            taskDispatcherDefinitions,
        });
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleNodeDragStart = useCallback(
        (_event: MouseEvent | TouchEvent, node: Node) => {
            setIsNodeDragging(true);

            if (node.type === STICKY_NOTE_NODE_TYPE) {
                return;
            }

            const nodeData = node.data as NodeDataType;
            const {edges: currentEdges, nodes: currentNodes} = useWorkflowDataStore.getState();

            if (nodeData.taskDispatcher) {
                draggingDispatcherIdRef.current = node.id;
                dispatcherDragStartRef.current = {...node.position};

                const descendants = collectAllDescendantNodes(node.id, currentNodes);

                // Also collect chain successor nodes (tasks that follow the
                // dispatcher's bottom ghost in the main flow)
                const chainSuccessors = collectChainSuccessorNodes(
                    node.id,
                    currentNodes,
                    currentEdges,
                    new Set(descendants.keys())
                );

                const dragStartPositions = new Map([...descendants, ...chainSuccessors]);

                childDragStartRef.current = filterToSharedParent({
                    nodes: currentNodes,
                    parentId: node.parentId,
                    startPositions: dragStartPositions,
                });
            }

            draggingPlaceholderRef.current = buildDraggingPlaceholderState(
                node,
                !!nodeData.taskDispatcher,
                FINAL_PLACEHOLDER_NODE_ID,
                currentEdges,
                currentNodes,
                childDragStartRef.current
            );
        },
        [setIsNodeDragging]
    );

    const handleNodesChange = useCallback(
        (changes: NodeChange<Node>[]) => {
            const nodesBeforeChanges = useWorkflowDataStore.getState().nodes;

            const resizingGraphIds = new Set<string>();

            for (const change of changes) {
                if (change.type !== 'position') {
                    continue;
                }

                const changedNode = nodesBeforeChanges.find((node) => node.id === change.id);

                const graphId = getGraphIdFromFrameNodeId(changedNode?.parentId);

                if (graphId) {
                    resizingGraphIds.add(graphId);
                }
            }

            const resizeChangedGraphFrames = (candidateNodes: Node[]): Node[] => {
                let resizedNodes = candidateNodes;

                for (const graphId of resizingGraphIds) {
                    resizedNodes = resizeGraphFrameForMembers({
                        direction: layoutDirection,
                        graphId,
                        nodes: resizedNodes,
                    });
                }

                return resizedNodes;
            };

            let childPositions: Map<string, {x: number; y: number}> | null = null;

            if (draggingDispatcherIdRef.current && dispatcherDragStartRef.current) {
                const dispatcherChange = changes.find(
                    (change) =>
                        change.type === 'position' && change.id === draggingDispatcherIdRef.current && change.position
                );

                if (dispatcherChange && dispatcherChange.type === 'position' && dispatcherChange.position) {
                    const delta = {
                        x: dispatcherChange.position.x - dispatcherDragStartRef.current.x,
                        y: dispatcherChange.position.y - dispatcherDragStartRef.current.y,
                    };

                    childPositions = new Map();

                    childDragStartRef.current.forEach((startPosition, childId) => {
                        childPositions!.set(childId, {
                            x: startPosition.x + delta.x,
                            y: startPosition.y + delta.y,
                        });
                    });
                }
            }

            let placeholderPosition: {id: string; position: {x: number; y: number}} | null = null;

            if (draggingPlaceholderRef.current) {
                // The tracked node may be the dragged node itself (in changes)
                // or a descendant (in childPositions).
                let trackedNodePosition = childPositions?.get(draggingPlaceholderRef.current.nodeId);

                if (!trackedNodePosition) {
                    const trackedChange = changes.find(
                        (change) =>
                            change.type === 'position' &&
                            change.id === draggingPlaceholderRef.current!.nodeId &&
                            change.position
                    );

                    if (trackedChange && trackedChange.type === 'position') {
                        trackedNodePosition = trackedChange.position;
                    }
                }

                if (trackedNodePosition) {
                    placeholderPosition = {
                        id: FINAL_PLACEHOLDER_NODE_ID,
                        position: computePlaceholderDragPosition(draggingPlaceholderRef.current, trackedNodePosition),
                    };
                }
            }

            // Apply the dragged node's change via onNodesChange (React Flow's
            // native drag), then directly set new node objects for descendants
            // so React Flow detects reference changes and recalculates edges.
            if (childPositions) {
                onNodesChange(changes);

                const currentNodes = useWorkflowDataStore.getState().nodes;

                const repositionedNodes = currentNodes.map((node) => {
                    const newPosition = childPositions!.get(node.id);

                    if (newPosition) {
                        return {...node, position: newPosition};
                    }

                    if (placeholderPosition && node.id === placeholderPosition.id) {
                        return {...node, position: placeholderPosition.position};
                    }

                    return node;
                });

                const resizedRepositionedNodes = resizeChangedGraphFrames(repositionedNodes);

                setNodes(resizedRepositionedNodes);
            } else {
                const allChanges: NodeChange<Node>[] = [...changes];

                if (placeholderPosition) {
                    allChanges.push({
                        id: placeholderPosition.id,
                        position: placeholderPosition.position,
                        type: 'position',
                    });
                }

                onNodesChange(allChanges);

                if (resizingGraphIds.size > 0) {
                    const nodesAfterChanges = useWorkflowDataStore.getState().nodes;
                    const resizedNodes = resizeChangedGraphFrames(nodesAfterChanges);

                    if (resizedNodes !== nodesAfterChanges) {
                        setNodes(resizedNodes);
                    }
                }
            }
        },
        [layoutDirection, onNodesChange, setNodes]
    );

    const resetDragTracking = useCallback(() => {
        draggingDispatcherIdRef.current = null;
        dispatcherDragStartRef.current = null;
        childDragStartRef.current = new Map();
        draggingPlaceholderRef.current = null;
    }, []);

    const handleNodeDragStop = useCallback(
        (_event: MouseEvent | TouchEvent, draggedNode: Node) => {
            setIsNodeDragging(false);

            if (draggedNode.type === STICKY_NOTE_NODE_TYPE) {
                if (updateWorkflowMutation) {
                    updateStickyNote({
                        id: draggedNode.id,
                        patch: {position: compensateStickyNotePosition(draggedNode.position)},
                        updateWorkflowMutation,
                    });
                }

                return;
            }

            const draggedGraphId = getGraphIdFromFrameNodeId(draggedNode.parentId);

            if (draggedGraphId) {
                const isGraphMember = (draggedNode.data as NodeDataType).graphData?.graphId === draggedGraphId;

                if (isGraphMember && updateWorkflowMutation) {
                    const autoPlacedPositions = takeAutoPlacedGraphPositions(draggedGraphId);

                    const memberGroupInset = getGraphMemberGroupInset({
                        graphId: draggedGraphId,
                        memberId: draggedNode.id,
                        nodes: useWorkflowDataStore.getState().nodes,
                    });

                    const nodePositions = buildGraphMemberDragStopPositions({
                        autoPlacedPositions,
                        draggedNodeId: draggedNode.id,
                        draggedNodePosition: draggedNode.position,
                        memberGroupInset,
                    });

                    saveWorkflowNodesPosition({
                        draggedNodeId: draggedNode.id,
                        nodePositions,
                        updateWorkflowMutation,
                    });
                }

                resetDragTracking();

                return;
            }

            const parentClusterRootId = (draggedNode.data as NodeDataType).parentClusterRootId;

            if (parentClusterRootId) {
                if (updateWorkflowMutation) {
                    saveDraggedClusterElementPosition({draggedNode, parentClusterRootId, updateWorkflowMutation});
                }

                resetDragTracking();

                return;
            }

            if (updateWorkflowMutation) {
                // Pre-compensate positions for the current cross-axis shift so that
                // when useLayout re-runs and applySavedPositions adds the shift back,
                // nodes end up at the correct screen position.
                const crossAxisShift = useWorkflowDataStore.getState().savedPositionCrossAxisShift;
                const crossAxis = layoutDirection === 'TB' ? 'x' : 'y';

                const compensatePosition = (position: {x: number; y: number}) => ({
                    ...position,
                    [crossAxis]: position[crossAxis] - crossAxisShift,
                });

                const nodePositions: Record<string, {x: number; y: number}> = {};

                nodePositions[draggedNode.id] = compensatePosition(draggedNode.position);

                let clearPositionNodeIds: Set<string> | undefined;

                if (draggingDispatcherIdRef.current && dispatcherDragStartRef.current) {
                    const {nodes: currentNodes} = useWorkflowDataStore.getState();

                    const incrementalDelta = {
                        x: draggedNode.position.x - dispatcherDragStartRef.current.x,
                        y: draggedNode.position.y - dispatcherDragStartRef.current.y,
                    };

                    clearPositionNodeIds = new Set<string>();

                    childDragStartRef.current.forEach((startPosition, childId) => {
                        const childNode = currentNodes.find((node) => node.id === childId);

                        if (!childNode) {
                            return;
                        }

                        const childData = childNode.data as NodeDataType;

                        if (containsNodePosition(childData?.metadata)) {
                            // Child has a saved position — shift it by the dispatcher's drag delta
                            // so it preserves its relative offset from the dispatcher
                            nodePositions[childId] = compensatePosition({
                                x: startPosition.x + incrementalDelta.x,
                                y: startPosition.y + incrementalDelta.y,
                            });
                        } else {
                            clearPositionNodeIds!.add(childId);
                        }
                    });
                }

                saveWorkflowNodesPosition({
                    clearPositionNodeIds,
                    draggedNodeId: draggedNode.id,
                    nodePositions,
                    updateWorkflowMutation,
                });
            }

            resetDragTracking();
        },
        [layoutDirection, resetDragTracking, setIsNodeDragging, updateWorkflowMutation]
    );

    let canvasWidth = window.innerWidth - 120;

    if (copilotPanelOpen) {
        canvasWidth -= COPILOT_PANEL_WIDTH;
    }

    if (leftSidebarOpen) {
        canvasWidth -= PROJECT_LEFT_SIDEBAR_WIDTH;
    }

    const canvasHeight = window.innerHeight - 60;

    useEffect(() => {
        if (!updateWorkflowMutation?.isPending && !isWorkflowMutating(workflowId)) {
            resetPendingRef.current = false;
        }
    }, [updateWorkflowMutation?.isPending, workflowId]);

    const handleResetLayout = useCallback(() => {
        if (!updateWorkflowMutation || resetPendingRef.current || isWorkflowMutating(workflowId)) {
            return;
        }

        resetPendingRef.current = true;

        clearAllNodePositions({
            incrementLayoutResetCounter,
            invalidateWorkflowQueries: editorInvalidateWorkflowQueries,
            updateWorkflowMutation,
        });
    }, [editorInvalidateWorkflowQueries, incrementLayoutResetCounter, updateWorkflowMutation, workflowId]);

    useEffect(() => {
        if (!resetWorkflowLayout) {
            return;
        }

        if (updateWorkflowMutation?.isPending || isWorkflowMutating(workflowId)) {
            return;
        }

        handleResetLayout();
        useWorkflowEditorStore.getState().setResetWorkflowLayout(false);
    }, [resetWorkflowLayout, updateWorkflowMutation?.isPending, handleResetLayout, workflowId]);

    const {autoPlacedGraphPositionsRef} = useLayout({
        canvasHeight,
        canvasWidth: customCanvasWidth || canvasWidth,
        componentDefinitions,
        copilotPanelOpen,
        leftSidebarOpen,
        readOnlyWorkflow: readOnlyWorkflow ? workflow : undefined,
        taskDispatcherDefinitions,
    });

    const {getViewportOffsetX} = useOverlayPanelsViewport({enabled: !readOnlyWorkflow});

    useCanvasHeightViewport({enabled: !readOnlyWorkflow});

    const workflowUuid = readOnlyWorkflow ? undefined : storeWorkflow.workflowUuid;

    useEffect(() => {
        if (readOnlyWorkflow) {
            return;
        }

        return registerAutoPlacedGraphPositions(autoPlacedGraphPositionsRef);
    }, [autoPlacedGraphPositionsRef, readOnlyWorkflow]);

    useEffect(() => {
        if (!readOnlyWorkflow) {
            return;
        }

        resetLayoutDirection(readOnlyLayoutDirection ?? extractLayoutDirection(readOnlyWorkflow.definition));

        return () => applyStoredLayoutDirection();
    }, [applyStoredLayoutDirection, readOnlyLayoutDirection, readOnlyWorkflow, resetLayoutDirection]);

    useEffect(() => {
        if (workflowUuid && !readOnlyWorkflow) {
            setCurrentWorkflowUuid(workflowUuid, extractLayoutDirection(workflow.definition));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowUuid]);

    useInitialViewport({
        canvasHeight,
        enabled: !fitViewOnLoad && !fitViewOnWorkflowChange,
        getViewportOffsetX,
        workflowUuid,
    });

    useEffect(() => {
        if (!fitViewOnWorkflowChange || !nodesInitialized) {
            return;
        }

        fitView({maxZoom: 1, minZoom: 0.2, padding: {bottom: 0.2, left: 0.2, right: 0.2, top: '72px'}});
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [fitViewOnWorkflowChange, nodesInitialized, workflowUuid, customCanvasWidth]);

    return {
        edgeTypes,
        handleAddStickyNote,
        handleConnect,
        handleConnectEnd,
        handleNodeDragStart,
        handleNodeDragStop,
        handleNodesChange,
        handleReconnect,
        handleResetLayout,
        handleTransitionDeleteKeyDown,
        isValidConnection,
        nodeTypes,
        onDragOver,
        onDrop,
    };
};

export default useWorkflowEditorCanvas;
