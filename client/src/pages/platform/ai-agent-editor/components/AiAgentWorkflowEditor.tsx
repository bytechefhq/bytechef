import {Controls, ReactFlow, ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {useShallow} from 'zustand/react/shallow';

import PlaceholderNode from '../../workflow-editor/nodes/PlaceholderNode';
import WorkflowNode from '../../workflow-editor/nodes/WorkflowNode';
import LabeledAiAgentEdge from '../edges/LabeledAiAgentEdge';
import useAiAgentLayout from '../hooks/useAiAgentLayout';
import AiAgentToolsGhostNode from '../nodes/AiAgentToolsGhostNode';
import useAiAgentDataStore from '../stores/useAiAgentDataStore';

const AiAgentWorkflowEditor = () => {
    const {edges, nodes, onEdgesChange, onNodesChange} = useAiAgentDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
        }))
    );

    const aiAgentEdgeTypes = {
        labeledAiAgentEdge: LabeledAiAgentEdge,
    };

    const aiAgentNodeTypes = {
        aiAgentToolsGhostNode: AiAgentToolsGhostNode,
        placeholder: PlaceholderNode,
        workflow: WorkflowNode,
    };

    useAiAgentLayout();

    return (
        <div className="size-full rounded-lg bg-surface-popover-canvas">
            <ReactFlowProvider>
                <ReactFlow
                    edgeTypes={aiAgentEdgeTypes}
                    edges={edges}
                    maxZoom={1}
                    minZoom={0.6}
                    nodeTypes={aiAgentNodeTypes}
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

export default AiAgentWorkflowEditor;
