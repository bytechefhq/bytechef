import {useEffect, useMemo, useState} from 'react';
import ReactFlow, {
    ReactFlowProvider,
    Controls,
    MiniMap,
    Edge,
    Node,
    useReactFlow,
    useStore,
} from 'reactflow';
import 'reactflow/dist/base.css';
import PlaceholderEdge from './edges/PlaceholderEdge';
import WorkflowEdge from './edges/WorkflowEdge';
import PlaceholderNode from './nodes/PlaceholderNode';
import WorkflowNode from './nodes/WorkflowNode';

import './WorkflowEditor.css';
import useLayout from './hooks/useLayout';
import {PlayIcon} from '@heroicons/react/24/outline';
import RightSlideOver from './components/NodeDetailsDialog';

const Workflow = (): JSX.Element => {
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
                label: 'Manual Trigger',
                name: 'trigger',
                icon: <PlayIcon className="h-8 w-8 text-gray-700" />,
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

    const {setViewport} = useReactFlow();

    const {width} = useStore((store) => ({
        width: store.width,
        height: store.height,
    }));

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
                defaultNodes={defaultNodes}
                defaultEdges={defaultEdges}
                defaultViewport={{
                    x: viewportWidth / 2,
                    y: 50,
                    zoom: 1,
                }}
                edgeTypes={edgeTypes}
                minZoom={0.6}
                maxZoom={1.5}
                nodeTypes={nodeTypes}
                nodesDraggable={false}
                nodesConnectable={false}
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

function WorkflowEditor() {
    return (
        <>
            <ReactFlowProvider>
                <Workflow />
            </ReactFlowProvider>

            <RightSlideOver />
        </>
    );
}

export default WorkflowEditor;
