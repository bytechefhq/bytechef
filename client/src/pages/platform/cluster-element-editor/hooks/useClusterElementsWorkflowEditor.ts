import {Node, OnNodesChange} from '@xyflow/react';
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

const useClusterElementsWorkflowEditor = () => {
    const {edges, nodes, onNodesChange, setDraggingNodeId, setIsNodeDragging} = useClusterElementsDataStore(
        useShallow((state) => ({
            edges: state.edges,
            nodes: state.nodes,
            onNodesChange: state.onNodesChange,
            setDraggingNodeId: state.setDraggingNodeId,
            setIsNodeDragging: state.setIsNodeDragging,
        }))
    );
    const workflow = useWorkflowDataStore((state) => state.workflow);

    const {invalidateWorkflowQueries, updateWorkflowMutation} = useWorkflowEditor();

    const previousNodePositionsRef = useRef<Record<string, {x: number; y: number}>>({});

    const currentPositions = useMemo(() => {
        return nodes.reduce<Record<string, {x: number; y: number}>>((nodesPositions, node) => {
            nodesPositions[node.id] = {x: node.position.x, y: node.position.y};

            return nodesPositions;
        }, {});
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

    return {
        clusterElementsEdgeTypes,
        clusterElementsNodeTypes,
        edges,
        handleNodesChange,
        handleResetLayout,
        nodes,
    };
};

export default useClusterElementsWorkflowEditor;
