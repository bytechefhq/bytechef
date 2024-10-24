import defaultEdges from '@/shared/defaultEdges';
import defaultNodes from '@/shared/defaultNodes';
import {
    ComponentDefinitionBasic,
    TaskDispatcherDefinitionBasic,
    WorkflowTask,
    WorkflowTrigger,
} from '@/shared/middleware/platform/configuration';
import {DragEventHandler, useCallback, useEffect, useMemo, useState} from 'react';
import ReactFlow, {Controls, MiniMap, useReactFlow, useStore} from 'reactflow';

import ConditionEdge from '../edges/ConditionEdge';
import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export interface WorkflowEditorProps {
    componentDefinitions: ComponentDefinitionBasic[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasic[];
}

const WorkflowEditor = ({componentDefinitions, taskDispatcherDefinitions}: WorkflowEditorProps) => {
    const [viewportWidth, setViewportWidth] = useState(0);

    const {setComponentActions, setWorkflow, workflow} = useWorkflowDataStore();

    const {componentNames} = workflow;

    const {getEdge, getNode, getNodes, setViewport} = useReactFlow();

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

    const width = useStore((store) => store.width);

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

        const droppedNode = [...componentDefinitions, ...taskDispatcherDefinitions].find(
            (node) => node.name === droppedNodeName
        );

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

    useLayout({componentDefinitions, taskDispatcherDefinitions});

    // Update workflow node names when nodes change
    useEffect(() => {
        const workflowNodes = getNodes();

        if (workflowNodes?.length) {
            const workflowNodeNames = workflowNodes.map((node) => {
                if (node.data.type !== 'placeholder' && node?.data.workflowNodeName) {
                    return node?.data.workflowNodeName;
                }
            });

            setWorkflow({
                ...workflow,
                nodeNames: workflowNodeNames.filter((nodeName) => !!nodeName),
            });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [setWorkflow, componentNames]);

    // Set viewport width and position
    useEffect(() => {
        setViewportWidth(width);

        let adaptedViewportWidth = width / 2.5;

        if (componentNames.includes('condition')) {
            adaptedViewportWidth -= 140;
        }

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [componentNames, setViewport, width]);

    // Update nodeNames and componentActions when workflow definition changes
    useEffect(() => {
        const workflowComponents: Array<WorkflowTrigger & WorkflowTask> = [
            workflow.triggers?.[0] || defaultNodes[0].data,
            ...(workflow?.tasks || []),
        ];

        setWorkflow({
            ...workflow,
            nodeNames: workflowComponents?.map((component) => component.name),
        });

        setComponentActions(
            workflowComponents.map((component) => {
                const componentName = component.type!.split('/')[0];
                const operationName = component.type!.split('/')[2];

                return {
                    componentName,
                    operationName,
                    workflowNodeName: component.name,
                };
            })
        );

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflow.tasks, workflow.triggers]);

    return (
        <div className="flex h-full flex-1 flex-col">
            <ReactFlow
                defaultEdges={defaultEdges}
                defaultNodes={defaultNodes}
                defaultViewport={{
                    x: viewportWidth / 2,
                    y: 50,
                    zoom: 1,
                }}
                deleteKeyCode={null}
                edgeTypes={edgeTypes}
                maxZoom={1.5}
                minZoom={0.6}
                nodeTypes={nodeTypes}
                nodesConnectable={false}
                nodesDraggable={false}
                onDragOver={onDragOver}
                onDrop={onDrop}
                panOnDrag
                panOnScroll
                proOptions={{hideAttribution: true}}
                zoomOnDoubleClick={false}
                zoomOnScroll={false}
            >
                <MiniMap />

                <Controls />
            </ReactFlow>
        </div>
    );
};

export default WorkflowEditor;
