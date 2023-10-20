import {DragEventHandler, useEffect, useMemo, useState} from 'react';
import ReactFlow, {
    ReactFlowProvider,
    Controls,
    MiniMap,
    Edge,
    Node,
    useReactFlow,
    useStore,
} from 'reactflow';
import PlaceholderEdge from './edges/PlaceholderEdge';
import WorkflowEdge from './edges/WorkflowEdge';
import PlaceholderNode from './nodes/PlaceholderNode';
import WorkflowNode from './nodes/WorkflowNode';
import useHandleDrop from './hooks/useHandleDrop';
import useLayout from './hooks/useLayout';
import {PlayIcon} from '@heroicons/react/24/outline';
import RightSlideOver from './components/NodeDetailsDialog';
import {
    ComponentDefinitionModel,
    TaskDispatcherDefinitionModel,
} from 'middleware/definition-registry';

import 'reactflow/dist/base.css';
import './WorkflowEditor.css';

type WorkflowProps = {
    components: ComponentDefinitionModel[];
    flowControls: TaskDispatcherDefinitionModel[];
};

const Workflow = ({components, flowControls}: WorkflowProps): JSX.Element => {
    const [viewportWidth, setViewportWidth] = useState(0);

    const defaultEdges: Edge[] = [
        {
            id: '1=>2',
            source: '1',
            target: '2',
            type: 'placeholder',
        },
    ];

    const defaultNodes: Node[] = [
        {
            id: '1',
            data: {
                icon: <PlayIcon className="h-8 w-8 text-gray-700" />,
                label: 'Manual Trigger',
                name: 'trigger',
                type: 'trigger',
            },
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            id: '2',
            data: {label: '+'},
            position: {x: 0, y: 150},
            type: 'placeholder',
        },
    ];

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
        width: store.width,
        height: store.height,
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

        setViewport({
            x: width / 2,
            y: 50,
            zoom: 1,
        });
    }, [setViewport, width]);

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
        <>
            <ReactFlowProvider>
                <Workflow components={components} flowControls={flowControls} />
            </ReactFlowProvider>

            <RightSlideOver />
        </>
    );
}

export default WorkflowEditor;
