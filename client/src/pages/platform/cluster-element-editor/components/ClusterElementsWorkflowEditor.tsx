import {
    Background,
    BackgroundVariant,
    Controls,
    Node,
    OnNodesChange,
    ReactFlow,
    ReactFlowProvider,
} from '@xyflow/react';

import '@xyflow/react/dist/style.css';
import {DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {useMemo, useRef} from 'react';
import {useShallow} from 'zustand/react/shallow';

import PlaceholderNode from '../../workflow-editor/nodes/PlaceholderNode';
import WorkflowNode from '../../workflow-editor/nodes/WorkflowNode';
import {useWorkflowEditor} from '../../workflow-editor/providers/workflowEditorProvider';
import useWorkflowDataStore from '../../workflow-editor/stores/useWorkflowDataStore';
import saveClusterElementNodesPosition from '../../workflow-editor/utils/saveClusterElementNodesPosition';
import LabeledClusterElementsEdge from '../edges/LabeledClusterElementsEdge';
import useClusterElementsLayout from '../hooks/useClusterElementsLayout';
import useClusterElementsDataStore from '../stores/useClusterElementsDataStore';

const ClusterElementsWorkflowEditor = () => {
    const {edges, nodes, onEdgesChange, onNodesChange, setDraggingNodeId, setIsDragging} = useClusterElementsDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onEdgesChange: state.onEdgesChange,
            onNodesChange: state.onNodesChange,
            setDraggingNodeId: state.setDraggingNodeId,
            setIsDragging: state.setIsDragging,
        }))
    );
    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();
    const {workflow} = useWorkflowDataStore();

    const previousNodePositionsRef = useRef<Record<string, {x: number; y: number}>>({});

    const currentPositions = useMemo(() => {
        const positions: Record<string, {x: number; y: number}> = {};

        nodes.forEach((node) => {
            positions[node.id] = {x: node.position.x, y: node.position.y};
        });

        return positions;
    }, [nodes]);

    const handleNodesChange: OnNodesChange<Node> = (changes) => {
        onNodesChange(changes);

        // Track dragging state and node being dragged currently
        changes.forEach((change) => {
            if (change.type === 'position') {
                setIsDragging(change.dragging ?? false);

                if (change.dragging) {
                    setDraggingNodeId(change.id);
                } else {
                    setDraggingNodeId(null);
                }
            }
        });

        // Find the new position of the node that was dragged
        changes.forEach((change) => {
            if (change.type === 'position' && change.dragging === false) {
                // Get the positions of the node that was dragged
                const changedPositions: Record<string, {x: number; y: number}> = {};
                const previousPositions = previousNodePositionsRef.current;

                // Check if the node that was dragged has actually changed position
                Object.entries(currentPositions).forEach(([nodeId, currentPos]) => {
                    const previousPosition = previousPositions[nodeId];

                    // If position of the node that was dragged changed or node is newly added then record the position for that node
                    if (
                        !previousPosition ||
                        previousPosition.x !== currentPos.x ||
                        previousPosition.y !== currentPos.y
                    ) {
                        changedPositions[nodeId] = currentPos;
                    }
                });

                if (Object.keys(changedPositions).length > 0) {
                    // Prevent race conditions with layout
                    setTimeout(() => {
                        // Save the nodes new positions
                        saveClusterElementNodesPosition({
                            invalidateWorkflowQueries,
                            movedClusterElementId: change.id,
                            updateWorkflowMutation,
                            workflow,
                        });
                    }, 100);
                }

                // Update the previous positions reference for future comparison
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
                    onNodesChange={handleNodesChange}
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
