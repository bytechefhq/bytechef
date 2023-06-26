import {
    ComponentDefinitionBasicModel,
    TaskDispatcherDefinitionBasicModel,
} from 'middleware/core/workflow/configuration';
import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import ReactFlow, {
    Controls,
    MiniMap,
    ReactFlowProvider,
    useReactFlow,
    useStore,
} from 'reactflow';

import NodeDetailsDialog from './components/NodeDetailsDialog';
import PlaceholderEdge from './edges/PlaceholderEdge';
import WorkflowEdge from './edges/WorkflowEdge';
import defaultEdges from './edges/defaultEdges';
import useHandleDrop from './hooks/useHandleDrop';
import useLayout from './hooks/useLayout';
import PlaceholderNode from './nodes/PlaceholderNode';
import WorkflowNode from './nodes/WorkflowNode';
import defaultNodes from './nodes/defaultNodes';

import 'reactflow/dist/base.css';

import './WorkflowEditor.css';
import DataPillPanel from './components/DataPillPanel';
import {useNodeDetailsDialogStore} from './stores/useNodeDetailsDialogStore';

type WorkflowProps = {
    components: ComponentDefinitionBasicModel[];
    flowControls: TaskDispatcherDefinitionBasicModel[];
};

const Workflow = ({components, flowControls}: WorkflowProps): JSX.Element => {
    const [viewportWidth, setViewportWidth] = useState(0);

    const {nodeDetailsOpen} = useNodeDetailsDialogStore();

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

    const {getEdge, getNode, setViewport} = useReactFlow();

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

        const droppedNode = [...components, ...flowControls].find(
            (node) => node.name === droppedNodeName
        );

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

        const adaptedViewportWidth = nodeDetailsOpen
            ? width / 2 - window.innerWidth / 6
            : width / 2;

        setViewport({
            x: adaptedViewportWidth,
            y: 50,
            zoom: 1,
        });
    }, [nodeDetailsOpen, setViewport, width]);

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
                minZoom={0.6}
                maxZoom={1.5}
                nodeTypes={nodeTypes}
                nodesDraggable={false}
                nodesConnectable={false}
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

type WorkflowEditorProps = WorkflowProps;

function WorkflowEditor({components, flowControls}: WorkflowEditorProps) {
    return (
        <ReactFlowProvider>
            <Workflow components={components} flowControls={flowControls} />

            <NodeDetailsDialog />

            <DataPillPanel />
        </ReactFlowProvider>
    );
}

export default WorkflowEditor;
