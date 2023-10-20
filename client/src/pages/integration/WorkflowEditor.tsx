import {useMemo} from 'react';
import ReactFlow, {
    ReactFlowProvider,
    Controls,
    MiniMap,
    Edge,
    Node,
} from 'reactflow';
import 'reactflow/dist/base.css';
import ContextualMenu from './nodes/ContextualMenu';
import PlaceholderEdge from './edges/PlaceholderEdge';
import WorkflowEdge from './edges/WorkflowEdge';
import PlaceholderNode from './nodes/PlaceholderNode';
import WorkflowNode from './nodes/WorkflowNode';

import './workflowEditor.css';
import useLayout from './hooks/useLayout';

const Workflow = (): JSX.Element => {
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
            data: {label: 'Manual Trigger'},
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
            contextualMenu: ContextualMenu,
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

    useLayout();

    return (
        <div className="flex h-full flex-1 flex-col bg-white">
            <ReactFlow
                defaultNodes={defaultNodes}
                defaultEdges={defaultEdges}
                edgeTypes={edgeTypes}
                fitView
                fitViewOptions={{padding: 0.95}}
                minZoom={0.2}
                nodeTypes={nodeTypes}
                nodesDraggable={false}
                nodesConnectable={false}
                zoomOnDoubleClick={false}
            >
                <MiniMap />

                <Controls />
            </ReactFlow>
        </div>
    );
};

function WorkflowEditor() {
    return (
        <ReactFlowProvider>
            <Workflow />
        </ReactFlowProvider>
    );
}

export default WorkflowEditor;
