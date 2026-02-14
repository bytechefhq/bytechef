import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {CANVAS_BACKGROUND_COLOR, FINAL_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType, NodeDataType} from '@/shared/types';
import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    Node,
    NodeChange,
    ReactFlow,
    XYPosition,
    useReactFlow,
} from '@xyflow/react';
import {ArrowDownIcon, ArrowRightIcon, BrushCleaningIcon} from 'lucide-react';
import {DragEventHandler, useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import LabeledBranchCaseEdge from '../edges/LabeledBranchCaseEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import AiAgentNode from '../nodes/AiAgentNode';
import PlaceholderNode from '../nodes/PlaceholderNode';
import ReadOnlyNode from '../nodes/ReadOnlyNode';
import ReadOnlyPlaceholderNode from '../nodes/ReadOnlyPlaceholderNode';
import TaskDispatcherBottomGhostNode from '../nodes/TaskDispatcherBottomGhostNode';
import TaskDispatcherLeftGhostNode from '../nodes/TaskDispatcherLeftGhostNode';
import TaskDispatcherTopGhostNode from '../nodes/TaskDispatcherTopGhostNode';
import WorkflowNode from '../nodes/WorkflowNode';
import {useWorkflowEditor} from '../providers/workflowEditorProvider';
import useLayoutDirectionStore from '../stores/useLayoutDirectionStore';
import clearAllNodePositions from '../utils/clearAllNodePositions';
import {
    DraggingPlaceholderStateType,
    buildDraggingPlaceholderState,
    computePlaceholderDragPosition,
} from '../utils/dragTrailingPlaceholder';
import {containsNodePosition} from '../utils/postDagreConstraints';
import saveWorkflowNodesPosition from '../utils/saveWorkflowNodesPosition';
import {isWorkflowMutating} from '../utils/workflowMutationGuard';

type ConditionalWorkflowEditorPropsType =
    | {
          readOnlyWorkflow?: Workflow;
          parentId?: never;
          parentType?: never;
      }
    | {
          readOnlyWorkflow?: never;
      };

/**
 * Node types that are synthetic layout constructs (ghost nodes, placeholders) with no
 * corresponding WorkflowTask in the definition. Dragging these nodes should not trigger
 * a position save mutation since they are recreated on every layout pass.
 */
export const NON_PERSISTED_NODE_TYPES = new Set([
    'placeholder',
    'taskDispatcherBottomGhostNode',
    'taskDispatcherLeftGhostNode',
    'taskDispatcherTopGhostNode',
]);

type WorkflowEditorPropsType = {
    componentDefinitions: ComponentDefinitionBasic[];
    customCanvasWidth?: number;
    invalidateWorkflowQueries: () => void;
    projectLeftSidebarOpen?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
};

const WorkflowEditor = ({
    componentDefinitions,
    customCanvasWidth,
    invalidateWorkflowQueries,
    projectLeftSidebarOpen,
    readOnlyWorkflow,
    taskDispatcherDefinitions,
}: WorkflowEditorPropsType & ConditionalWorkflowEditorPropsType) => {
    let workflow = useWorkflowDataStore((state) => state.workflow);

    if (!workflow.tasks && readOnlyWorkflow) {
        workflow = {...workflow, ...readOnlyWorkflow};
    }

    const {edges, incrementLayoutResetCounter, nodes, onEdgesChange, onNodesChange, setIsNodeDragging} =
        useWorkflowDataStore(
            useShallow((state) => ({
                edges: state.edges,
                incrementLayoutResetCounter: state.incrementLayoutResetCounter,
                nodes: state.nodes,
                onEdgesChange: state.onEdgesChange,
                onNodesChange: state.onNodesChange,
                setIsNodeDragging: state.setIsNodeDragging,
            }))
        );
    const {layoutDirection, setLayoutDirection, setWorkflowId} = useLayoutDirectionStore(
        useShallow((state) => ({
            layoutDirection: state.layoutDirection,
            setLayoutDirection: state.setLayoutDirection,
            setWorkflowId: state.setWorkflowId,
        }))
    );
    const copilotPanelOpen = useCopilotPanelStore((state) => state.copilotPanelOpen);
    const dataPillPanelOpen = useDataPillPanelStore((state) => state.dataPillPanelOpen);
    const rightSidebarOpen = useRightSidebarStore((state) => state.rightSidebarOpen);
    const workflowNodeDetailsPanelOpen = useWorkflowNodeDetailsPanelStore(
        (state) => state.workflowNodeDetailsPanelOpen
    );
    const workflowTestChatPanelOpen = useWorkflowTestChatStore((state) => state.workflowTestChatPanelOpen);

    const {setViewport} = useReactFlow();

    const {invalidateWorkflowQueries: editorInvalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode] = useHandleDrop({
        invalidateWorkflowQueries,
        taskDispatcherDefinitions,
    });

    const nodeTypes = useMemo(
        () => ({
            aiAgentNode: AiAgentNode,
            placeholder: PlaceholderNode,
            readonly: ReadOnlyNode,
            readonlyPlaceholder: ReadOnlyPlaceholderNode,
            taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
            taskDispatcherLeftGhostNode: TaskDispatcherLeftGhostNode,
            taskDispatcherTopGhostNode: TaskDispatcherTopGhostNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            labeledBranchCase: LabeledBranchCaseEdge,
            placeholder: PlaceholderEdge,
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

                const targetNode = nodes.find((node) => node.id === targetNodeId);

                if (targetNode) {
                    handleDropOnTriggerNode(droppedNode);
                }

                return;
            }
        } else {
            const isTargetNode = event.target instanceof HTMLElement;
            const isTargetEdge = event.target instanceof SVGElement;

            if (isTargetNode) {
                const targetNodeElement = (event.target as HTMLElement).closest('.react-flow__node') as HTMLElement;

                if (!targetNodeElement || targetNodeElement?.dataset.nodetype === 'trigger') {
                    return;
                }

                const targetNodeId = targetNodeElement.dataset.id!;

                const {nodes} = useWorkflowDataStore.getState();

                const targetNode = nodes.find((node) => node.id === targetNodeId);

                if (targetNode && targetNode.type === 'placeholder') {
                    if (targetNode?.position.x === 0 && targetNode?.position.y === 0) {
                        return;
                    }

                    handleDropOnPlaceholderNode(targetNode, droppedNode);
                }
            } else if (isTargetEdge) {
                const getClosestEdgeElement = (element: HTMLElement): HTMLElement | null => {
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

                const edgeElement = getClosestEdgeElement(event.target as HTMLElement);

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

    const draggingDispatcherIdRef = useRef<string | null>(null);
    const dispatcherDragStartRef = useRef<XYPosition | null>(null);
    const childDragStartRef = useRef<Map<string, XYPosition>>(new Map());
    const draggingPlaceholderRef = useRef<DraggingPlaceholderStateType | null>(null);

    const isChildNodeOfDispatcher = useCallback((node: Node, dispatcherId: string) => {
        const nodeData = node.data as NodeDataType;

        return (
            node.id !== dispatcherId &&
            (nodeData.taskDispatcherId === dispatcherId ||
                nodeData.conditionData?.conditionId === dispatcherId ||
                nodeData.loopData?.loopId === dispatcherId ||
                nodeData.branchData?.branchId === dispatcherId ||
                nodeData.eachData?.eachId === dispatcherId ||
                nodeData.parallelData?.parallelId === dispatcherId ||
                nodeData.forkJoinData?.forkJoinId === dispatcherId)
        );
    }, []);

    const collectAllDescendantNodes = useCallback(
        (dispatcherId: string, allNodes: Node[]): Map<string, XYPosition> => {
            const collected = new Set<string>();
            const startPositions = new Map<string, XYPosition>();

            const collect = (currentDispatcherId: string) => {
                allNodes.forEach((node) => {
                    if (collected.has(node.id)) {
                        return;
                    }

                    if (isChildNodeOfDispatcher(node, currentDispatcherId)) {
                        collected.add(node.id);
                        startPositions.set(node.id, {...node.position});

                        const nodeData = node.data as NodeDataType;

                        if (nodeData.taskDispatcher && nodeData.taskDispatcherId) {
                            collect(nodeData.taskDispatcherId);
                        }
                    }
                });
            };

            collect(dispatcherId);

            return startPositions;
        },
        [isChildNodeOfDispatcher]
    );

    const handleNodeDragStart = useCallback(
        (_event: React.MouseEvent, node: Node) => {
            setIsNodeDragging(true);

            const nodeData = node.data as NodeDataType;
            const {edges: currentEdges, nodes: currentNodes} = useWorkflowDataStore.getState();

            if (nodeData.taskDispatcher) {
                draggingDispatcherIdRef.current = node.id;
                dispatcherDragStartRef.current = {...node.position};
                childDragStartRef.current = collectAllDescendantNodes(node.id, currentNodes);
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
        [collectAllDescendantNodes, setIsNodeDragging]
    );

    const handleNodesChange = useCallback(
        (changes: NodeChange<Node>[]) => {
            const allChanges: NodeChange<Node>[] = [...changes];

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

                    childDragStartRef.current.forEach((startPosition, childId) => {
                        allChanges.push({
                            id: childId,
                            position: {
                                x: startPosition.x + delta.x,
                                y: startPosition.y + delta.y,
                            },
                            type: 'position',
                        });
                    });
                }
            }

            if (draggingPlaceholderRef.current) {
                const trackedChange = allChanges.find(
                    (change) =>
                        change.type === 'position' &&
                        change.id === draggingPlaceholderRef.current!.nodeId &&
                        change.position
                );

                if (trackedChange && trackedChange.type === 'position' && trackedChange.position) {
                    allChanges.push({
                        id: FINAL_PLACEHOLDER_NODE_ID,
                        position: computePlaceholderDragPosition(
                            draggingPlaceholderRef.current,
                            trackedChange.position
                        ),
                        type: 'position',
                    });
                }
            }

            onNodesChange(allChanges);
        },
        [onNodesChange]
    );

    const handleNodeDragStop = useCallback(
        (_event: React.MouseEvent, draggedNode: Node) => {
            setIsNodeDragging(false);

            if (!NON_PERSISTED_NODE_TYPES.has(draggedNode.type!) && !isWorkflowMutating(workflow.id!)) {
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
                    invalidateWorkflowQueries: editorInvalidateWorkflowQueries,
                    nodePositions,
                    updateWorkflowMutation,
                });
            }

            draggingDispatcherIdRef.current = null;
            dispatcherDragStartRef.current = null;
            childDragStartRef.current = new Map();
            draggingPlaceholderRef.current = null;
        },
        [editorInvalidateWorkflowQueries, layoutDirection, setIsNodeDragging, updateWorkflowMutation, workflow.id]
    );

    let canvasWidth = window.innerWidth - 120;

    if (copilotPanelOpen) {
        canvasWidth -= 450;
    }

    if (dataPillPanelOpen) {
        canvasWidth -= 400;
    }

    if (projectLeftSidebarOpen) {
        canvasWidth -= 384;
    }

    if (rightSidebarOpen) {
        canvasWidth -= 384;
    }

    if (workflowNodeDetailsPanelOpen || workflowTestChatPanelOpen) {
        canvasWidth -= 460;
    }

    const canvasHeight = window.innerHeight - 60;

    const resetPendingRef = useRef(false);

    useEffect(() => {
        if (!updateWorkflowMutation.isPending && !isWorkflowMutating(workflow.id!)) {
            resetPendingRef.current = false;
        }
    }, [updateWorkflowMutation.isPending, workflow.id]);

    const handleResetLayout = useCallback(() => {
        if (resetPendingRef.current || isWorkflowMutating(workflow.id!)) {
            return;
        }

        resetPendingRef.current = true;

        clearAllNodePositions({
            invalidateWorkflowQueries: editorInvalidateWorkflowQueries,
            updateWorkflowMutation,
        });

        incrementLayoutResetCounter();
    }, [editorInvalidateWorkflowQueries, incrementLayoutResetCounter, updateWorkflowMutation, workflow.id]);

    useLayout({
        canvasHeight,
        canvasWidth: customCanvasWidth || canvasWidth,
        componentDefinitions,
        readOnlyWorkflow: readOnlyWorkflow ? workflow : undefined,
        taskDispatcherDefinitions,
    });

    useEffect(() => {
        if (workflow.id) {
            setWorkflowId(String(workflow.id));
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
    }, [workflow.id]);

    return (
        <div className="flex h-full flex-1 flex-col rounded-lg bg-background">
            <ReactFlow
                edgeTypes={edgeTypes}
                edges={edges}
                maxZoom={1.5}
                minZoom={0.001}
                nodeTypes={nodeTypes}
                nodes={nodes}
                nodesConnectable={false}
                nodesDraggable
                onDragOver={onDragOver}
                onDrop={onDrop}
                onEdgesChange={onEdgesChange}
                onNodeDragStart={handleNodeDragStart}
                onNodeDragStop={handleNodeDragStop}
                onNodesChange={handleNodesChange}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                <Controls
                    className="m-2 mb-3 rounded-md border border-stroke-neutral-secondary bg-background"
                    fitViewOptions={{duration: 500, minZoom: 0.2}}
                    showInteractive={false}
                >
                    <ControlButton
                        onClick={() => setLayoutDirection(layoutDirection === 'TB' ? 'LR' : 'TB')}
                        title={layoutDirection === 'TB' ? 'Switch to horizontal layout' : 'Switch to vertical layout'}
                    >
                        {layoutDirection === 'TB' ? (
                            <ArrowRightIcon className="size-3" />
                        ) : (
                            <ArrowDownIcon className="size-3" />
                        )}
                    </ControlButton>

                    <ControlButton onClick={handleResetLayout} title="Reset layout">
                        <BrushCleaningIcon className="size-3" />
                    </ControlButton>
                </Controls>
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
