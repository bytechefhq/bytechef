import {
    Background,
    BackgroundVariant,
    ControlButton,
    Controls,
    Node,
    OnNodesChange,
    ReactFlow,
    ReactFlowProvider,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {CANVAS_BACKGROUND_COLOR, DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {BrushCleaningIcon} from 'lucide-react';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import PlaceholderNode from '../../workflow-editor/nodes/PlaceholderNode';
import WorkflowNode from '../../workflow-editor/nodes/WorkflowNode';
import {useWorkflowEditor} from '../../workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import clearAllClusterElementPositions from '../../workflow-editor/utils/clearAllClusterElementPositions';
import saveClusterElementNodesPosition from '../../workflow-editor/utils/saveClusterElementNodesPosition';
import LabeledClusterElementsEdge from '../edges/LabeledClusterElementsEdge';
import useClusterElementsLayout from '../hooks/useClusterElementsLayout';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const ClusterElementsWorkflowEditor = () => {
    const {edges, nodes, onEdgesChange, onNodesChange, setDraggingNodeId, setIsNodeDragging} =
        useClusterElementsDataStore(
            useShallow((state) => ({
                edges: state.edges,
                nodes: state.nodes,
                onEdgesChange: state.onEdgesChange,
                onNodesChange: state.onNodesChange,
                setDraggingNodeId: state.setDraggingNodeId,
                setIsNodeDragging: state.setIsNodeDragging,
            }))
        );
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const previousNodePositionsRef = useRef<Record<string, {x: number; y: number}>>({});

    const currentPositions = useMemo(() => {
        const positions = nodes.reduce<Record<string, {x: number; y: number}>>((nodesPositions, node) => {
            nodesPositions[node.id] = {x: node.position.x, y: node.position.y};

            return nodesPositions;
        }, {});

        return positions;
    }, [nodes]);

    const handleNodesChange: OnNodesChange<Node> = (changes) => {
        onNodesChange(changes);

        changes.forEach((change) => {
            if (change.type === 'position') {
                setIsNodeDragging(change.dragging ?? false);

                if (change.dragging) {
                    setDraggingNodeId(change.id);
                } else {
                    setDraggingNodeId(null);
                }
            }
        });

        changes.forEach((change) => {
            if (change.type === 'position' && change.dragging === false) {
                const changedPositions: Record<string, {x: number; y: number}> = {};
                const previousPositions = previousNodePositionsRef.current;

                Object.entries(currentPositions).forEach(([nodeId, currentPosition]) => {
                    const previousPosition = previousPositions[nodeId];

                    const nodeAdded = !previousPosition && currentPosition;
                    const nodePositionChanged =
                        previousPosition?.x !== currentPosition.x || previousPosition?.y !== currentPosition.y;

                    if (nodeAdded || nodePositionChanged) {
                        changedPositions[nodeId] = currentPosition;
                    }
                });

                if (Object.keys(changedPositions).length > 0 && !updateWorkflowMutation.isPending) {
                    setTimeout(() => {
                        if (!updateWorkflowMutation.isPending) {
                            saveClusterElementNodesPosition({
                                invalidateWorkflowQueries,
                                movedClusterElementId: change.id,
                                updateWorkflowMutation,
                                workflow,
                            });
                        }
                    }, 100);
                }

                previousNodePositionsRef.current = {...currentPositions};
            }
        });
    };

    const clusterElementsEdgeTypes = {
        labeledClusterElementsEdge: LabeledClusterElementsEdge,
    };

    const clusterElementsNodeTypes = {
        placeholder: PlaceholderNode,
        workflow: WorkflowNode,
    };

    const resetPendingRef = useRef(false);

    useEffect(() => {
        if (!updateWorkflowMutation.isPending) {
            resetPendingRef.current = false;
        }
    }, [updateWorkflowMutation.isPending]);

    const handleResetLayout = useCallback(() => {
        if (resetPendingRef.current || updateWorkflowMutation.isPending) {
            return;
        }

        resetPendingRef.current = true;

        clearAllClusterElementPositions({
            invalidateWorkflowQueries,
            updateWorkflowMutation,
        });
    }, [invalidateWorkflowQueries, updateWorkflowMutation]);

    useClusterElementsLayout();

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
