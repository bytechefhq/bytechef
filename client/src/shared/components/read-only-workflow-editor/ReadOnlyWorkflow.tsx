import {Controls, Edge, MiniMap, Node, ReactFlow} from '@xyflow/react';

import ReadOnlyEdge from './ReadOnlyEdge';
import ReadOnlyNode from './ReadOnlyNode';

const nodeTypes = {readonly: ReadOnlyNode};
const edgeTypes = {readonly: ReadOnlyEdge};

interface ReadOnlyWorkflowProps {
    edges: Edge[];
    nodes: Node[];
}

const ReadOnlyWorkflow = ({edges, nodes}: ReadOnlyWorkflowProps) => (
    <ReactFlow
        defaultEdges={edges}
        defaultNodes={nodes}
        deleteKeyCode={null}
        edgeTypes={edgeTypes}
        edges={edges}
        fitView
        fitViewOptions={{
            maxZoom: 1,
        }}
        maxZoom={1.5}
        minZoom={0.6}
        nodeTypes={nodeTypes}
        nodes={nodes}
        nodesConnectable={false}
        nodesDraggable={false}
        panOnDrag
        panOnScroll
        proOptions={{hideAttribution: true}}
        zoomOnDoubleClick={false}
        zoomOnScroll={false}
    >
        <MiniMap />

        <Controls />
    </ReactFlow>
);

export default ReadOnlyWorkflow;
