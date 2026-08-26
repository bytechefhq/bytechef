import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowEditorStore from '@/pages/platform/workflow-editor/stores/useWorkflowEditorStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {
    COPILOT_PANEL_WIDTH,
    DATA_PILL_PANEL_WIDTH,
    FINAL_PLACEHOLDER_NODE_ID,
    NODE_DETAILS_PANEL_WIDTH,
    PROJECT_LEFT_SIDEBAR_WIDTH,
    WORKFLOW_NODES_SIDEBAR_WIDTH,
} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {Node, NodeChange, XYPosition, useNodesInitialized, useReactFlow} from '@xyflow/react';
import {DragEventHandler, useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import GraphStartEdge from '../edges/GraphStartEdge';
import GraphTransitionEdge from '../edges/GraphTransitionEdge';
import LabeledBranchCaseEdge from '../edges/LabeledBranchCaseEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import RoundedSmoothStepEdge from '../edges/RoundedSmoothStepEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useGraphConnections from '../hooks/useGraphConnections';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import useStickyNotes from '../hooks/useStickyNotes';
import AiAgentNode from '../nodes/AiAgentNode';
import GraphFrameNode from '../nodes/GraphFrameNode';
import GraphStartNode from '../nodes/GraphStartNode';
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
import clearAllNodePositions from '../utils/clearAllNodePositions';
import {collectAllDescendantNodes, collectChainSuccessorNodes} from '../utils/collectDescendantNodes';
import {
    DraggingPlaceholderStateType,
    buildDraggingPlaceholderState,
    computePlaceholderDragPosition,
} from '../utils/dragTrailingPlaceholder';
import {registerAutoPlacedGraphPositions, takeAutoPlacedGraphPositions} from '../utils/graph/autoPlacedGraphPositions';
import {toGraphContentPosition} from '../utils/graph/graphConnections';
import {GRAPH_FRAME_ID_ATTRIBUTE, getGraphFrameId, getGraphIdFromFrameNodeId} from '../utils/graph/graphFrameGeometry';
import {resizeGraphFrameForMembers} from '../utils/graph/graphFrameResize';
import {buildGraphMemberDragStopPositions, filterToSharedParent} from '../utils/graph/graphMemberDrag';
import {findSelectedGraphTransition, isCanvasDeleteKeyTarget} from '../utils/graph/graphTransitionDeleteKey';
import {removeTransition} from '../utils/graph/graphTransitionMutations';
import {saveGraphTransitions} from '../utils/graph/saveGraphParameters';
import {containsNodePosition} from '../utils/postDagreConstraints';
import resolveTargetTriggerName from '../utils/resolveTargetTriggerName';
import saveWorkflowNodesPosition from '../utils/saveWorkflowNodesPosition';
import {STICKY_NOTE_NODE_TYPE, compensateStickyNotePosition, updateStickyNote} from '../utils/stickyNoteUtils';
import {isWorkflowMutating} from '../utils/workflowMutationGuard';

interface UseWorkflowEditorCanvasParamsI {
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    fitViewOnWorkflowChange?: boolean;
    leftSidebarOpen?: boolean;
    readOnlyWorkflow?: Workflow;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const useWorkflowEditorCanvas = ({
    componentDefinitions,
    customCanvasWidth,
    fitViewOnWorkflowChange,
    leftSidebarOpen,
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
    const {layoutDirection, setCurrentWorkflowUuid} = useLayoutDirectionStore(
        useShallow((state) => ({
            layoutDirection: state.layoutDirection,
            setCurrentWorkflowUuid: state.setCurrentWorkflowUuid,
        }))
    );
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const resetWorkflowLayout = useWorkflowEditorStore((state) => state.resetWorkflowLayout);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);

    const {fitView, getInternalNode, screenToFlowPosition, setViewport} = useReactFlow();

    // True once React Flow has measured every node's dimensions — fitView is a no-op before this, so we
    // gate the embedded fit-to-view on it (see the fitViewOnWorkflowChange effect below).
    const nodesInitialized = useNodesInitialized();

    const {invalidateWorkflowQueries: editorInvalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const {handleAddStickyNote} = useStickyNotes({readOnly: !!readOnlyWorkflow});

    const [
        handleDropOnPlaceholderNode,
        handleDropOnWorkflowEdge,
        handleDropOnTriggerNode,
        handleDropOnTriggerPlaceholder,
        handleDropOnGraphFrame,
    ] = useHandleDrop({
        taskDispatcherDefinitions,
    });

    const draggingDispatcherIdRef = useRef<string | null>(null);
    const dispatcherDragStartRef = useRef<XYPosition | null>(null);
    const childDragStartRef = useRef<Map<string, XYPosition>>(new Map());
    const draggingPlaceholderRef = useRef<DraggingPlaceholderStateType | null>(null);
    const resetPendingRef = useRef(false);

    let canvasWidth = window.innerWidth - 120;

    if (copilotPanelOpen) {
        canvasWidth -= COPILOT_PANEL_WIDTH;
    }

    if (dataPillPanelOpen) {
        canvasWidth -= DATA_PILL_PANEL_WIDTH;
    }

    if (leftSidebarOpen) {
        canvasWidth -= PROJECT_LEFT_SIDEBAR_WIDTH;
    }

    if (rightSidebarOpen) {
        canvasWidth -= WORKFLOW_NODES_SIDEBAR_WIDTH;
    }

    if (workflowNodeDetailsPanelOpen || workflowTestChatPanelOpen) {
        canvasWidth -= NODE_DETAILS_PANEL_WIDTH;
    }

    const canvasHeight = window.innerHeight - 60;

    // Runs here, ahead of the effect that publishes the auto-placed positions it returns, because
    // a later declaration would put that ref in the effect's dependency array — evaluated during
    // render — before it exists. Hook order IS effect order, so it sits as late as that allows.
    const {autoPlacedGraphPositionsRef} = useLayout({
        canvasHeight,
        canvasWidth: customCanvasWidth || canvasWidth,
        componentDefinitions,
        copilotPanelOpen,
        leftSidebarOpen,
        readOnlyWorkflow: readOnlyWorkflow ? workflow : undefined,
        taskDispatcherDefinitions,
    });

    const {handleConnect, handleConnectEnd, handleReconnect, isValidConnection} = useGraphConnections({
        updateWorkflowMutation,
    });

    /**
     * Deletes the selected graph transition on Backspace/Delete.
     *
     * React Flow's own delete key stays off (`deleteKeyCode={null}` in `WorkflowEditor`), so this is
     * the only delete-by-key path on the canvas and it is deliberately narrow: a transition is one
     * row in `parameters.transitions`, and dropping it drops nothing else. `WorkflowEditor`
     * registers this on the document, which is why the focused element is checked: the key belongs
     * to whatever is layered over the canvas — a contenteditable condition, the editor's own To
     * picker, a dialog opened while an edge stayed selected — before it belongs to the canvas.
     */
    const handleTransitionDeleteKeyDown = useCallback(
        (event: KeyboardEvent) => {
            if (readOnlyWorkflow || !updateWorkflowMutation) {
                return;
            }

            if (event.key !== 'Backspace' && event.key !== 'Delete') {
                return;
            }

            if (!isCanvasDeleteKeyTarget(document.activeElement)) {
                return;
            }

            const selectedTransition = findSelectedGraphTransition(useWorkflowDataStore.getState().edges);

            if (!selectedTransition) {
                return;
            }

            event.preventDefault();

            saveGraphTransitions(
                selectedTransition.graphId,
                (transitions) => removeTransition(transitions, selectedTransition.index),
                updateWorkflowMutation
            );
        },
        [readOnlyWorkflow, updateWorkflowMutation]
    );

    const nodeTypes = useMemo(
        () => ({
            clusterRoot: AiAgentNode,
            graphFrame: GraphFrameNode,
            graphStart: GraphStartNode,
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
            graphStart: GraphStartEdge,
            graphTransition: GraphTransitionEdge,
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

        event.preventDefault();

        event.dataTransfer.dropEffect = 'move';
    }, []);

    const onDrop: DragEventHandler = useCallback((event) => {
        const droppedNodeData = event.dataTransfer.getData('application/reactflow');

        let droppedNodeType = '';
        let droppedNodeName;

        if (droppedNodeData.includes('--')) {
            droppedNodeName = droppedNodeData.split('--')[0];
            droppedNodeType = droppedNodeData.split('--')[1];
        } else {
            droppedNodeName = droppedNodeData;
        }

        let droppedNode = componentDefinitions.find((node) => node.name === droppedNodeName) as
            ClickedDefinitionType | undefined;

        if (!droppedNode) {
            const taskDispatcherNode = taskDispatcherDefinitions.find((node) => node.name === droppedNodeName);

            if (taskDispatcherNode) {
                droppedNode = {...taskDispatcherNode, taskDispatcher: true} as ClickedDefinitionType;
            }
        }

        if (!droppedNode) {
            return;
        }

        if (droppedNodeType === 'trigger') {
            droppedNode = {
                ...droppedNode,
                trigger: true,
            };

            const targetChildNode = (event.target as HTMLElement).closest('.react-flow__node > div') as HTMLElement;

            const targetNodeType = targetChildNode?.dataset.nodetype;

            const targetNodeElement =
                event.target instanceof HTMLElement
                    ? targetChildNode?.parentNode
                    : (event.target as SVGElement).closest('.react-flow__node');

            if (targetNodeType === 'trigger' && targetNodeElement instanceof HTMLElement) {
                const targetNodeId = targetNodeElement.dataset.id;

                if (!targetNodeId) {
                    return;
                }

                const targetNode = useWorkflowDataStore.getState().nodes.find((node) => node.id === targetNodeId);

                if (targetNode) {
                    const targetNodeName = resolveTargetTriggerName(targetNode.data as NodeDataType);

                    if (targetNodeName) {
                        handleDropOnTriggerNode(droppedNode, targetNodeName);
                    }
                }

                return;
            }

            // Any trigger dropped in the trigger zone that is not on an existing
            // trigger node (including the "+" add-trigger slot) appends a new trigger.
            handleDropOnTriggerPlaceholder(droppedNode);
        } else {
            const getClosestEdgeElement = (element: HTMLElement | null): HTMLElement | null => {
                let current: HTMLElement | null = element;

                while (current) {
                    if (
                        current.tagName === 'DIV' &&
                        current.id &&
                        current.id.match(/^.+=>.+$/) &&
                        !current.id.endsWith('-button')
                    ) {
                        return current;
                    }

                    current = current.parentElement;
                }

                return null;
            };

            const isTargetNode = event.target instanceof HTMLElement;
            const isTargetEdge = event.target instanceof SVGElement;

            if (isTargetNode) {
                // A frame's own element is the only drop target inside the box: React Flow renders
                // members as siblings in the viewport rather than nested in their parent, so a drop
                // landing on a member never reaches here through the frame.
                const graphFrameElement = (event.target as HTMLElement).closest(`[${GRAPH_FRAME_ID_ATTRIBUTE}]`);

                const graphFrameId = graphFrameElement?.getAttribute(GRAPH_FRAME_ID_ATTRIBUTE);

                // A frame element with no node behind it is mid-teardown; falling through leaves
                // the drop to the normal handling below rather than silently discarding it.
                const graphFrameNode = graphFrameId ? getInternalNode(getGraphFrameId(graphFrameId)) : undefined;

                if (graphFrameId && graphFrameNode) {
                    handleDropOnGraphFrame(
                        graphFrameId,
                        toGraphContentPosition(
                            screenToFlowPosition({x: event.clientX, y: event.clientY}),
                            graphFrameNode.internals.positionAbsolute
                        ),
                        droppedNode
                    );

                    return;
                }

                const targetNodeElement = (event.target as HTMLElement).closest('.react-flow__node') as HTMLElement;

                if (targetNodeElement && targetNodeElement?.dataset.nodetype !== 'trigger') {
                    const targetNodeId = targetNodeElement.dataset.id!;

                    const {nodes} = useWorkflowDataStore.getState();

                    const targetNode = nodes.find((node) => node.id === targetNodeId);

                    if (targetNode && targetNode.type === 'placeholder') {
                        if (targetNode?.position.x === 0 && targetNode?.position.y === 0) {
                            return;
                        }

                        handleDropOnPlaceholderNode(targetNode, droppedNode);

                        return;
                    }
                }

                const edgeElement = getClosestEdgeElement(event.target as HTMLElement);

                if (edgeElement) {
                    const {edges} = useWorkflowDataStore.getState();

                    const targetEdge = edges.find((edge) => edge.id === edgeElement.id);

                    if (targetEdge) {
                        handleDropOnWorkflowEdge(targetEdge, droppedNode);

                        return;
                    }
                }
            } else if (isTargetEdge) {
                const closestDiv = (event.target as SVGElement).closest('div');
                const edgeElement = closestDiv instanceof HTMLElement ? getClosestEdgeElement(closestDiv) : null;

                if (!edgeElement) {
                    return;
                }

                const {edges} = useWorkflowDataStore.getState();

                const targetEdge = edges.find((edge) => edge.id === edgeElement.id);

                if (targetEdge) {
                    handleDropOnWorkflowEdge(targetEdge, droppedNode);

                    return;
                }
            }
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

                // A graph's members are descendants of its dispatcher but are parented to the
                // frame node, so React Flow already carries them when the frame moves. Only nodes
                // sharing the dragged node's parent are in its coordinate space and may take the
                // drag delta — which still includes a dragged member's own subtree.
                childDragStartRef.current = filterToSharedParent(
                    new Map([...descendants, ...chainSuccessors]),
                    currentNodes,
                    node.parentId
                );
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

                const graphId = getGraphIdFromFrameNodeId(
                    nodesBeforeChanges.find((node) => node.id === change.id)?.parentId
                );

                if (graphId) {
                    resizingGraphIds.add(graphId);
                }
            }

            // The frame's authoritative size comes from `layoutGraphFrames`, which only re-runs
            // once the drop has changed the definition. Recomputing it here is what makes the box
            // grow and shrink under the cursor.
            //
            // Only the frame directly parenting a changed node is resized. Dragging a member of a
            // graph nested inside another therefore grows the inner frame live but leaves the
            // enclosing one at its last laid-out size until the drop, which relayouts both. The
            // gap is transient and self-healing, so the frame chain is deliberately not walked.
            const resizeChangedGraphFrames = (candidateNodes: Node[]): Node[] => {
                let resizedNodes = candidateNodes;

                for (const graphId of resizingGraphIds) {
                    resizedNodes = resizeGraphFrameForMembers(resizedNodes, graphId);
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

                setNodes(
                    resizeChangedGraphFrames(
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
                    )
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

                if (resizingGraphIds.size > 0) {
                    const nodesAfterChanges = useWorkflowDataStore.getState().nodes;
                    const resizedNodes = resizeChangedGraphFrames(nodesAfterChanges);

                    if (resizedNodes !== nodesAfterChanges) {
                        setNodes(resizedNodes);
                    }
                }
            }
        },
        [onNodesChange, setNodes]
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

            // A frame child's position is frame-relative, so the cross-axis compensation below —
            // which exists for the outer flow's saved positions — must not touch it either way.
            if (draggedGraphId) {
                // Only a DIRECT member has a position in the graph model. A member's subtree is
                // also parented to the frame but is placed by the layout relative to its member,
                // and unlike the member it carries neither `draggable` nor an extent, so it moves
                // only on an unlocked canvas. Persisting a frame-relative coordinate for one would
                // write a `nodePosition` nothing reads, and would start being read the moment that
                // task left the frame — as an absolute pin, off by the header band.
                const isGraphMember = (draggedNode.data as NodeDataType).graphData?.graphId === draggedGraphId;

                if (isGraphMember && updateWorkflowMutation) {
                    saveWorkflowNodesPosition({
                        draggedNodeId: draggedNode.id,
                        nodePositions: buildGraphMemberDragStopPositions({
                            // Taken, not read: the same channel every other graph interaction
                            // flushes through, so a drop cannot leave entries an add or a connect
                            // would then write a second time.
                            autoPlacedPositions: takeAutoPlacedGraphPositions(draggedGraphId),
                            draggedNodeId: draggedNode.id,
                            draggedNodePosition: draggedNode.position,
                        }),
                        updateWorkflowMutation,
                    });
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

    // Publishes the pre-pass's pending auto-placed positions for the interactions that persist a
    // graph from outside React — a task appended deep inside `saveWorkflowDefinition` cannot reach
    // a hook's ref. A read-only canvas never persists anything, so it stays off the channel rather
    // than taking it over from the editable one.
    useEffect(() => {
        if (readOnlyWorkflow) {
            return;
        }

        return registerAutoPlacedGraphPositions(autoPlacedGraphPositionsRef);
    }, [autoPlacedGraphPositionsRef, readOnlyWorkflow]);

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

    const workflowUuid = workflow.workflowUuid;

    useEffect(() => {
        if (workflowUuid) {
            setCurrentWorkflowUuid(workflowUuid);
        }

        if (fitViewOnWorkflowChange) {
            return;
        }

        setViewport(
            {
                x: 0,
                y: 0,
                zoom: 1,
            },
            {
                duration: 500,
            }
        );
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowUuid]);

    useEffect(() => {
        if (!fitViewOnWorkflowChange || !nodesInitialized) {
            return;
        }

        fitView({maxZoom: 1, minZoom: 0.2, padding: 0.2});
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
