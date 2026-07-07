import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    ReactFlow,
    ReactFlowProvider,
    type Viewport,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {CANVAS_BACKGROUND_COLOR, DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {BrushCleaningIcon} from 'lucide-react';
import {useCallback, useEffect, useState} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsWorkflowEditor from '../hooks/useClusterElementsWorkflowEditor';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const SETTLE_DELAY = 350;

const ClusterElementsWorkflowEditor = () => {
    const [canvasReady, setCanvasReady] = useState(false);

    const {onEdgesChange, setCanvasZoom} = useClusterElementsDataStore(
        useShallow((state) => ({
            onEdgesChange: state.onEdgesChange,
            setCanvasZoom: state.setCanvasZoom,
        }))
    );

    const {clusterElementsEdgeTypes, clusterElementsNodeTypes, edges, handleNodesChange, handleResetLayout, nodes} =
        useClusterElementsWorkflowEditor();

    const handleViewportChange = useCallback(
        (viewport: Viewport) => {
            setCanvasZoom(viewport.zoom);
        },
        [setCanvasZoom]
    );

    // Mount ReactFlow only after the hosting dialog's open transition settles. ReactFlow measures
    // handle positions once on mount; mounting it into a not-yet-settled layout caches wrong bounds
    // and renders edges non-vertical until a later remeasure corrects them (a visible snap on reopen).
    useEffect(() => {
        const timeoutId = setTimeout(() => setCanvasReady(true), SETTLE_DELAY);

        return () => clearTimeout(timeoutId);
    }, []);

    if (!canvasReady) {
        return <div className="size-full" />;
    }

    return (
        <div className="size-full">
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
                    onViewportChange={handleViewportChange}
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
