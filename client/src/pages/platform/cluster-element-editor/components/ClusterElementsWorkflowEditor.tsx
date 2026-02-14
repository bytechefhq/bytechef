import {Background, BackgroundVariant, ControlButton, Controls, ReactFlow, ReactFlowProvider} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {CANVAS_BACKGROUND_COLOR, DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {BrushCleaningIcon} from 'lucide-react';

import useClusterElementsWorkflowEditor from '../hooks/useClusterElementsWorkflowEditor';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const ClusterElementsWorkflowEditor = () => {
    const onEdgesChange = useClusterElementsDataStore((state) => state.onEdgesChange);

    const {clusterElementsEdgeTypes, clusterElementsNodeTypes, edges, handleNodesChange, handleResetLayout, nodes} =
        useClusterElementsWorkflowEditor();

    return (
        <div className="size-full rounded-lg bg-surface-popover-canvas">
            <ReactFlowProvider>
                <ReactFlow
                    defaultViewport={{x: 0, y: 0, zoom: DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM}}
                    edgeTypes={clusterElementsEdgeTypes}
                    edges={edges}
                    maxZoom={1}
                    minZoom={0.001}
                    nodeTypes={clusterElementsNodeTypes}
                    nodes={nodes}
                    nodesConnectable={false}
                    nodesDraggable
                    onEdgesChange={onEdgesChange}
                    onNodesChange={handleNodesChange}
                    panOnDrag
                    panOnScroll
                    proOptions={{hideAttribution: true}}
                    zoomOnDoubleClick={false}
                    zoomOnScroll={false}
                >
                    <Background color={CANVAS_BACKGROUND_COLOR} size={2} variant={BackgroundVariant.Dots} />

                    <Controls
                        className="m-2 rounded-md border border-stroke-neutral-secondary bg-background"
                        showInteractive={false}
                    >
                        <ControlButton onClick={handleResetLayout} title="Reset layout">
                            <BrushCleaningIcon className="size-3" />
                        </ControlButton>
                    </Controls>
                </ReactFlow>
            </ReactFlowProvider>
        </div>
    );
};

export default ClusterElementsWorkflowEditor;
