import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {CANVAS_BACKGROUND_COLOR} from '@/shared/constants';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    Workflow,
} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType} from '@/shared/types';
import {Background, BackgroundVariant, Controls, ReactFlow, useReactFlow} from '@xyflow/react';
import {DragEventHandler, useCallback, useEffect, useMemo} from 'react';
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

type ConditionalWorkflowEditorPropsType =
    | {
          readOnlyWorkflow?: Workflow;
          parentId?: never;
          parentType?: never;
      }
    | {
          readOnlyWorkflow?: never;
      };

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

    const {edges, nodes, onEdgesChange, onNodesChange} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
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

    useLayout({
        canvasWidth: customCanvasWidth || canvasWidth,
        componentDefinitions,
        readOnlyWorkflow: readOnlyWorkflow ? workflow : undefined,
        taskDispatcherDefinitions,
    });

    useEffect(() => {
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
                nodesDraggable={false}
                onDragOver={onDragOver}
                onDrop={onDrop}
                onEdgesChange={onEdgesChange}
                onNodesChange={onNodesChange}
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
                />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
