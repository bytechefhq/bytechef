import {useGetComponentDefinitionQuery} from '@/queries/componentDefinitions.queries';
import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from 'middleware/hermes/configuration';
import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import ReactFlow, {Controls, MiniMap, useReactFlow, useStore} from 'reactflow';

import PlaceholderEdge from '../edges/PlaceholderEdge';
import WorkflowEdge from '../edges/WorkflowEdge';
import defaultEdges from '../edges/defaultEdges';
import useHandleDrop from '../hooks/useHandleDrop';
import useLayout from '../hooks/useLayout';
import PlaceholderNode from '../nodes/PlaceholderNode';
import WorkflowNode from '../nodes/WorkflowNode';
import defaultNodes from '../nodes/defaultNodes';
import {useNodeDetailsPanelStore} from '../stores/useNodeDetailsPanelStore';
import useWorkflowDataStore from '../stores/useWorkflowDataStore';

export type WorkflowEditorProps = {
    componentDefinitions: ComponentDefinitionBasicModel[];
    taskDispatcherDefinitions: TaskDispatcherDefinitionBasicModel[];
};

const WorkflowEditor = ({
    componentDefinitions,
    taskDispatcherDefinitions,
}: WorkflowEditorProps) => {
    const [nodeNames, setNodeNames] = useState<Array<string>>([]);
    const [viewportWidth, setViewportWidth] = useState(0);

    const {nodeDetailsPanelOpen} = useNodeDetailsPanelStore();

    const nodeTypes = useMemo(
        () => ({
            placeholder: PlaceholderNode,
            workflow: WorkflowNode,
        }),
        []
    );

    const edgeTypes = useMemo(
        () => ({
            placeholder: PlaceholderEdge,
            workflow: WorkflowEdge,
        }),
        []
    );

    const {data: workflowComponent} = useGetComponentDefinitionQuery(
        {
            componentName: nodeNames[nodeNames.length - 1],
        },
        !!nodeNames.length
    );

    const {getEdge, getNode, getNodes, setViewport} = useReactFlow();

    const nodes = getNodes();

    useEffect(() => {
        const workflowNodes = nodes.filter((node) => node.data.originNodeName);

        setNodeNames(workflowNodes.map((node) => node.data.originNodeName));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [nodes.length]);

    const {componentActions, setComponentActions} = useWorkflowDataStore();

    useEffect(() => {
        if (workflowComponent?.actions) {
            let workflowAlias = `${workflowComponent.name}-1`;
            let index = 2;

            while (
                componentActions.some(
                    (action) => action.componentName === workflowAlias
                )
            ) {
                workflowAlias = `${workflowComponent.name}-${index}`;

                index++;
            }

            setComponentActions([
                ...componentActions,
                {
                    actionName: workflowComponent.actions[0].name,
                    componentName: workflowComponent.name,
                    workflowAlias,
                },
            ]);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [workflowComponent?.name]);

    const {width} = useStore((store) => ({
        height: store.height,
        width: store.width,
    }));

    const [handleDropOnPlaceholderNode, handleDropOnWorkflowEdge] =
        useHandleDrop();

    const onDrop: DragEventHandler = (event) => {
        const droppedNodeName = event.dataTransfer.getData(
            'application/reactflow'
        );

        const droppedNode = [
            ...componentDefinitions,
            ...taskDispatcherDefinitions,
        ].find((node) => node.name === droppedNodeName);

        if (!droppedNode) {
            return;
        }

        if (event.target instanceof HTMLElement) {
            const targetNodeElement = event.target.closest(
                '.react-flow__node'
            ) as HTMLElement;

            if (targetNodeElement) {
                const targetNodeId = targetNodeElement.dataset.id!;

                const targetNode = getNode(targetNodeId);

                if (targetNode) {
                    handleDropOnPlaceholderNode(targetNode, droppedNode);
                }
            }
        } else if (event.target instanceof SVGElement) {
            const targetEdgeElement = event.target.closest('.react-flow__edge');

            if (targetEdgeElement) {
                const targetEdge = getEdge(targetEdgeElement.id);

                if (targetEdge) {
                    handleDropOnWorkflowEdge(targetEdge, droppedNode);
                }
            }
        }
    };

    useEffect(() => {
        setViewportWidth(width);

        const adaptedViewportWidth = nodeDetailsPanelOpen
            ? width / 2 - window.innerWidth / 6
            : width / 2;

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [nodeDetailsPanelOpen, setViewport, width]);

    useLayout();

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
