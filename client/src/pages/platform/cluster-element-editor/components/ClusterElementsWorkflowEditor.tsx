import {Controls, ReactFlow, ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {useShallow} from 'zustand/react/shallow';

import PlaceholderNode from '../../workflow-editor/nodes/PlaceholderNode';
import WorkflowNode from '../../workflow-editor/nodes/WorkflowNode';
import LabeledClusterElementsEdge from '../edges/LabeledClusterElementsEdge';
import useClusterElementsLayout from '../hooks/useClusterElementsLayout';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const ClusterElementsWorkflowEditor = () => {
    const {edges, nodes, onEdgesChange, onNodesChange} = useClusterElementsDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
        }))
    );

    const clusterElementsEdgeTypes = {
        labeledClusterElementsEdge: LabeledClusterElementsEdge,
    };

    const clusterElementsNodeTypes = {
        placeholder: PlaceholderNode,
        workflow: WorkflowNode,
    };

    useClusterElementsLayout();

    return (
        <div className="size-full rounded-lg bg-surface-popover-canvas">
            <ReactFlowProvider>
                <ReactFlow
                    edgeTypes={clusterElementsEdgeTypes}
                    edges={edges}
                    maxZoom={1}
                    minZoom={0.6}
                    nodeTypes={clusterElementsNodeTypes}
                    nodes={nodes}
                    nodesConnectable={false}
                    nodesDraggable
                    onEdgesChange={onEdgesChange}
                    onNodesChange={onNodesChange}
                    panOnDrag
                    panOnScroll
                    proOptions={{hideAttribution: true}}
                    zoomOnDoubleClick={false}
                    zoomOnScroll={false}
                >
                    <Controls
                        className="m-2 rounded-md border border-stroke-neutral-secondary bg-background"
                        fitViewOptions={{duration: 500, minZoom: 0.2}}
                        showInteractive={false}
                    />
                </ReactFlow>
            </ReactFlowProvider>
        </div>
    );
};

export default ClusterElementsWorkflowEditor;
