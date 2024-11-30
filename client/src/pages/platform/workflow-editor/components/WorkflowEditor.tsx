import useRightSidebarStore from '@/pages/platform/workflow-editor/stores/useRightSidebarStore';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import useWorkflowNodeDetailsPanelStore from '@/pages/platform/workflow-editor/stores/useWorkflowNodeDetailsPanelStore';
import {ComponentDefinitionBasic, TaskDispatcherDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {ClickedDefinitionType} from '@/shared/types';
import {DragEventHandler, useCallback, useEffect, useMemo} from 'react';
import ReactFlow, {Controls, MiniMap, useReactFlow} from 'reactflow';
import {useShallow} from 'zustand/react/shallow';

import ConditionEdge from '../edges/ConditionEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasic[];
    leftSidebarOpen: boolean;
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const WorkflowEditor = ({componentDefinitions, leftSidebarOpen, taskDispatcherDefinitions}: WorkflowEditorProps) => {
    const {edges, nodes, onEdgesChange, onNodesChange, workflow} = useWorkflowDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
            workflow: state.workflow,
        }))
    );
    const {workflowNodeDetailsPanelOpen} = useWorkflowNodeDetailsPanelStore();
    const {rightSidebarOpen} = useRightSidebarStore();

    const {getEdge, getNode, setViewport} = useReactFlow();

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge, handleDropOnTriggerNode] = useHandleDrop();

    const nodeTypes = useMemo(
        () => ({
            placeholder: PlaceholderNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            condition: ConditionEdge,
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

    const onDrop: DragEventHandler = (event) => {
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
                droppedNode = {
                    ...taskDispatcherNode,
                    taskDispatcher: true,
                } as ClickedDefinitionType;
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

                const targetNode = getNode(targetNodeId);

                if (targetNode) {
                    handleDropOnTriggerNode(droppedNode);
                }

                return;
            }
        } else {
            if (event.target instanceof HTMLElement) {
                const targetNodeElement = event.target.closest('.react-flow__node') as HTMLElement;

                if (!targetNodeElement) {
                    return;
                }

                if (targetNodeElement.dataset.nodetype === 'trigger') {
                    return;
                }

                const targetNodeId = targetNodeElement.dataset.id!;

                const targetNode = getNode(targetNodeId);

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

                const targetEdge = getEdge(targetEdgeElement.id);

                if (targetEdge) {
                    handleDropOnWorkflowEdge(targetEdge, droppedNode);

                    return;
                }
            }
        }
    };

    let canvasWidth = window.innerWidth - 120;

    if (leftSidebarOpen) {
        canvasWidth -= 384;
    }
    if (rightSidebarOpen) {
        canvasWidth -= 384;
    }
    if (workflowNodeDetailsPanelOpen) {
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
        <div className="flex h-full flex-1 flex-col">
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
                <MiniMap />

                <Controls fitViewOptions={{duration: 500, minZoom: 0.2}} showInteractive={false} />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
