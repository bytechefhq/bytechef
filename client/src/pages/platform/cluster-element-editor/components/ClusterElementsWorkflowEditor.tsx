import {Background, BackgroundVariant, Controls, ReactFlow, ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
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
                    defaultViewport={{x: 0, y: 0, zoom: DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM}}
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
                    <Background color="#ccc" size={2} variant={BackgroundVariant.Dots} />

                    <Controls
                        className="m-2 rounded-md border border-stroke-neutral-secondary bg-background"
                        showInteractive={false}
                    />
                </ReactFlow>
            </ReactFlowProvider>
        </div>
    );
};

export default ClusterElementsWorkflowEditor;
