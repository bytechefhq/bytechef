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
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Node, NodeChange, XYPosition, useReactFlow} from '@xyflow/react';
import {DragEventHandler, useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import LabeledBranchCaseEdge from '../edges/LabeledBranchCaseEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import RoundedSmoothStepEdge from '../edges/RoundedSmoothStepEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import useOverlayPanelsViewport from '../hooks/useOverlayPanelsViewport';
import useStickyNotes from '../hooks/useStickyNotes';
import AiAgentNode from '../nodes/AiAgentNode';
import PlaceholderNode from '../nodes/PlaceholderNode';
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
import {CANVAS_DRAG_DATA_TYPE, getCanvasDragKind} from '../utils/canvasDragData';
import clearAllNodePositions from '../utils/clearAllNodePositions';
import {collectAllDescendantNodes, collectChainSuccessorNodes} from '../utils/collectDescendantNodes';
import {
    DraggingPlaceholderStateType,
    buildDraggingPlaceholderState,
    computePlaceholderDragPosition,
} from '../utils/dragTrailingPlaceholder';
import getInitialViewportPosition from '../utils/getInitialViewportPosition';
import {extractLayoutDirection} from '../utils/layoutDirectionDefinitionUtils';
import {containsNodePosition} from '../utils/postDagreConstraints';
import resolveCanvasDropTarget from '../utils/resolveCanvasDropTarget';
import resolveTargetTriggerName from '../utils/resolveTargetTriggerName';
import saveWorkflowNodesPosition from '../utils/saveWorkflowNodesPosition';
import {STICKY_NOTE_NODE_TYPE, compensateStickyNotePosition, updateStickyNote} from '../utils/stickyNoteUtils';
import {isWorkflowMutating} from '../utils/workflowMutationGuard';

interface UseWorkflowEditorCanvasParamsI {
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    fitViewOnLoad?: boolean;
    leftSidebarOpen?: boolean;
    readOnlyLayoutDirection?: LayoutDirectionType;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const useWorkflowEditorCanvas = ({
    componentDefinitions,
    customCanvasWidth,
    fitViewOnLoad,
    leftSidebarOpen,
    readOnlyLayoutDirection,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: UseWorkflowEditorCanvasParamsI) => {
    let workflow = useWorkflowDataStore((state) => state.workflow);

    if (!workflow.tasks && readOnlyWorkflow) {
        workflow = {...workflow, ...readOnlyWorkflow};
    }

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

    const {setViewport} = useReactFlow();

    const {invalidateWorkflowQueries: editorInvalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const {handleAddStickyNote} = useStickyNotes({readOnly: !!readOnlyWorkflow});

    const [
        handleDropOnPlaceholderNode,
        handleDropOnWorkflowEdge,
        handleDropOnTriggerNode,
        handleDropOnTriggerPlaceholder,
    ] = useHandleDrop({
        taskDispatcherDefinitions,
    });

    const draggingDispatcherIdRef = useRef<string | null>(null);
    const dispatcherDragStartRef = useRef<XYPosition | null>(null);
    const childDragStartRef = useRef<Map<string, XYPosition>>(new Map());
    const draggingPlaceholderRef = useRef<DraggingPlaceholderStateType | null>(null);
    const resetPendingRef = useRef(false);

    const nodeTypes = useMemo(
        () => ({
            clusterRoot: AiAgentNode,
            placeholder: PlaceholderNode,
            readonly: ReadOnlyNode,
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
            labeledBranchCase: LabeledBranchCaseEdge,
            placeholder: PlaceholderEdge,
            smoothstep: RoundedSmoothStepEdge,
            workflow: WorkflowEdge,
        }),
        []
    );

    const onDragOver: DragEventHandler = useCallback((event) => {
        if (event.target instanceof HTMLButtonElement && event.target.dataset.nodeType === 'workflow') {
            return;
        }

        const dragKind = getCanvasDragKind(event.dataTransfer);

        if (dragKind) {
            const {edges, nodes} = useWorkflowDataStore.getState();

            if (!resolveCanvasDropTarget({dragKind, edges, nodes, target: event.target})) {
                event.dataTransfer.dropEffect = 'none';

                return;
            }
        }

        event.preventDefault();

        event.dataTransfer.dropEffect = 'move';
    }, []);

    const onDrop: DragEventHandler = useCallback((event) => {
        const droppedNodeData = event.dataTransfer.getData(CANVAS_DRAG_DATA_TYPE);

        let droppedNodeType = '';
        let droppedNodeName;

        if (droppedNodeData.includes('--')) {
            droppedNodeName = droppedNodeData.split('--')[0];
            droppedNodeType = droppedNodeData.split('--')[1];
        } else {
            droppedNodeName = droppedNodeData;
        }

        let droppedNode = componentDefinitions.find((node) => node.name === droppedNodeName) as
            | ClickedDefinitionType
            | undefined;

        if (!droppedNode) {
            const taskDispatcherNode = taskDispatcherDefinitions.find((node) => node.name === droppedNodeName);

            if (taskDispatcherNode) {
                droppedNode = {...taskDispatcherNode, taskDispatcher: true} as ClickedDefinitionType;
            }
        }

        if (!droppedNode) {
            return;
        }

        const isTriggerDrop = droppedNodeType === 'trigger';

        if (isTriggerDrop) {
            droppedNode = {
                ...droppedNode,
                trigger: true,
            };
        }

        const {edges, nodes} = useWorkflowDataStore.getState();

        const dropTarget = resolveCanvasDropTarget({
            dragKind: isTriggerDrop ? 'trigger' : 'task',
            edges,
            nodes,
            target: event.target,
        });

        if (!dropTarget) {
            return;
        }

        if (dropTarget.type === 'triggerPlaceholder') {
            handleDropOnTriggerPlaceholder(droppedNode);
        } else if (dropTarget.type === 'trigger') {
            const targetTriggerName = resolveTargetTriggerName(dropTarget.node.data as NodeDataType);

            if (targetTriggerName) {
                handleDropOnTriggerNode(droppedNode, targetTriggerName);
            }
        } else if (dropTarget.type === 'placeholder') {
            handleDropOnPlaceholderNode(dropTarget.node, droppedNode);
        } else {
            handleDropOnWorkflowEdge(dropTarget.edge, droppedNode);
        }
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

                childDragStartRef.current = new Map([...descendants, ...chainSuccessors]);
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

                setNodes(
                    currentNodes.map((node) => {
                        const newPosition = childPositions!.get(node.id);

                        if (newPosition) {
                            return {...node, position: newPosition};
                        }

                        if (placeholderPosition && node.id === placeholderPosition.id) {
                            return {...node, position: placeholderPosition.position};
                        }

                        return node;
                    })
                );
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
            }
        },
        [onNodesChange, setNodes]
    );

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

            draggingDispatcherIdRef.current = null;
            dispatcherDragStartRef.current = null;
            childDragStartRef.current = new Map();
            draggingPlaceholderRef.current = null;
        },
        [layoutDirection, setIsNodeDragging, updateWorkflowMutation]
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

    useLayout({
        canvasHeight,
        canvasWidth: customCanvasWidth || canvasWidth,
        componentDefinitions,
        copilotPanelOpen,
        leftSidebarOpen,
        readOnlyWorkflow: readOnlyWorkflow ? workflow : undefined,
        taskDispatcherDefinitions,
    });

    const {getViewportOffsetX} = useOverlayPanelsViewport({enabled: !readOnlyWorkflow});

    const workflowUuid = workflow.workflowUuid;

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

        if (fitViewOnLoad) {
            return;
        }

        const {x, y} = getInitialViewportPosition({
            layoutDirection: useLayoutDirectionStore.getState().layoutDirection,
            offsetX: getViewportOffsetX(),
        });

        setViewport(
            {
                x,
                y,
                zoom: 1,
            },
            {
                duration: 500,
            }
        );
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowUuid]);

    return {
        edgeTypes,
        handleAddStickyNote,
        handleNodeDragStart,
        handleNodeDragStop,
        handleNodesChange,
        handleResetLayout,
        nodeTypes,
        onDragOver,
        onDrop,
    };
};

export default useWorkflowEditorCanvas;
