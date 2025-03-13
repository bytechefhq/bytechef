import useDataPillPanelStore from '@/pages/platform/workflow-editor/stores/useDataPillPanelStore';
import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import useWorkflowTestChatStore from '@/pages/platform/workflow-editor/stores/useWorkflowTestChatStore';
import {useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {MINIMAP_MASK_COLOR, MINIMAP_NODE_COLOR} from '@/shared/constants';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType} from '@/shared/types';
import {Controls, MiniMap, ReactFlow, useReactFlow} from '@xyflow/react';
import {DragEventHandler, useCallback, useEffect, useMemo} from 'react';
import {twMerge} from 'tailwind-merge';
import {useShallow} from 'zustand/react/shallow';

import ConditionEdge from '../edges/ConditionEdge';
import LoopDecorativeEdge from '../edges/LoopDecorativeEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import LoopLeftGhostNode from '../nodes/LoopLeftGhostNode';
import PlaceholderNode from '../nodes/PlaceholderNode';
import TaskDispatcherBottomGhostNode from '../nodes/TaskDispatcherBottomGhostNode';
import WorkflowNode from '../nodes/WorkflowNode';

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasic[];
    projectLeftSidebarOpen?: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const WorkflowEditor = ({
    componentDefinitions,
    projectLeftSidebarOpen,
    taskDispatcherDefinitions,
}: WorkflowEditorProps) => {
    const {workflow} = useWorkflowDataStore();

    const {edges, nodes, onEdgesChange, onNodesChange} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
        }))
    );
    const {copilotPanelOpen} = useCopilotStore();
    const {dataPillPanelOpen} = useDataPillPanelStore();
    const {rightSidebarOpen} = useRightSidebarStore();
    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {workflowTestChatPanelOpen} = useWorkflowTestChatStore();

    const {setViewport} = useReactFlow();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode] = useHandleDrop();

    const nodeTypes = useMemo(
        () => ({
            loopLeftGhostNode: LoopLeftGhostNode,
            placeholder: PlaceholderNode,
            taskDispatcherBottomGhostNode: TaskDispatcherBottomGhostNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            condition: ConditionEdge,
            loopDecorative: LoopDecorativeEdge,
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
            if (event.target instanceof HTMLElement) {
                const targetNodeElement = event.target.closest('.react-flow__node') as HTMLElement;

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
            } else if (event.target instanceof SVGElement) {
                const targetEdgeElement = event.target.closest('.react-flow__edge') as HTMLElement;

                if (
                    !targetEdgeElement ||
                    (targetEdgeElement.parentNode as HTMLElement).dataset?.nodetype === 'trigger'
                ) {
                    return;
                }

                const {edges} = useWorkflowDataStore.getState();

                const targetEdge = edges.find((edge) => edge.id === targetEdgeElement.id);

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

    useLayout({canvasWidth, componentDefinitions, taskDispatcherDefinitions});

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
                minZoom={0.6}
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
                <MiniMap
                    className={twMerge('mb-2 mr-16', rightSidebarOpen && 'absolute right-minimap-placement')}
                    maskColor={MINIMAP_MASK_COLOR}
                    nodeBorderRadius={24}
                    nodeColor={MINIMAP_NODE_COLOR}
                />

                <Controls
                    className="m-2 rounded-md border border-stroke-neutral-secondary bg-background"
                    fitViewOptions={{duration: 500, minZoom: 0.2}}
                    showInteractive={false}
                />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
