import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    ReactFlow,
    ReactFlowProvider,
    type Viewport,
    useReactFlow,
    useStore,
    useUpdateNodeInternals,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {CANVAS_BACKGROUND_COLOR, DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {BrushCleaningIcon} from 'lucide-react';
import {useCallback} from 'react';
import {useShallow} from 'zustand/react/shallow';

import useClusterElementsWorkflowEditor from '../hooks/useClusterElementsWorkflowEditor';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const SETTLE_FRAMES = 22;

export const RemeasureClusterNodes = () => {
    const updateNodeInternals = useUpdateNodeInternals();
    const {getNodes} = useReactFlow();
    const nodeCount = useStore((state) => state.nodes.length);

    useEffect(() => {
        const remeasure = () => {
            for (const node of getNodes()) {
                updateNodeInternals(node.id);
            }
        };

        remeasure();

        let frame = 0;
        let rafId = 0;

        const tick = () => {
            remeasure();

            frame += 1;

            if (frame < SETTLE_FRAMES) {
                rafId = requestAnimationFrame(tick);
            }
        };

        rafId = requestAnimationFrame(tick);

        return () => cancelAnimationFrame(rafId);
    }, [nodeCount, getNodes, updateNodeInternals]);

    return null;
};

const ClusterElementsWorkflowEditor = () => {
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

    return (
        <div className="size-full">
            <ReactFlowProvider>
                <RemeasureClusterNodes />

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
