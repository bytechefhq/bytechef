/* eslint-disable sort-keys */

import {DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM} from '@/shared/constants';
import {Edge, Node, OnEdgesChange, OnNodesChange, applyEdgeChanges, applyNodeChanges} from '@xyflow/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface ClusterElementsDataStoreI {
    edges: Edge[];
    setEdges: (edges: Edge[]) => void;
    onEdgesChange: OnEdgesChange;

    nodes: Node[];
    setNodes: (nodes: Node[]) => void;
    onNodesChange: OnNodesChange;

    isNodeDragging: boolean;
    setIsNodeDragging: (isNodeDragging: boolean) => void;

    draggingNodeId: string | null;
    setDraggingNodeId: (nodeId: string | null) => void;

    isPositionSaving: boolean;
    setIsPositionSaving: (isPositionSaving: boolean) => void;

    canvasZoom: number;
    setCanvasZoom: (canvasZoom: number) => void;

    nodesLocked: boolean;
    setNodesLocked: (nodesLocked: boolean) => void;

    // Bumped by the Reset layout control. Unlocked canvases skip the automatic layout so hand-placed nodes stay put,
    // so an explicit reset needs a signal of its own to get through.
    layoutResetCounter: number;
    incrementLayoutResetCounter: () => void;

    reset: () => void;
}

const useClusterElementsDataStore = create<ClusterElementsDataStoreI>()(
    devtools(
        (set, get) => ({
            edges: [],
            setEdges: (edges) => {
                set({edges});
            },
            onEdgesChange: (changes) => {
                set({
                    edges: applyEdgeChanges(changes, get().edges),
                });
            },

            nodes: [],
            setNodes: (nodes) => {
                set({nodes});
            },
            onNodesChange: (changes) => {
                set({
                    nodes: applyNodeChanges(changes, get().nodes),
                });
            },

            isNodeDragging: false,
            setIsNodeDragging: (isNodeDragging) => {
                set({isNodeDragging});
            },

            draggingNodeId: null,
            setDraggingNodeId: (draggingNodeId) => {
                set({draggingNodeId});
            },

            isPositionSaving: false,
            setIsPositionSaving: (isPositionSaving) => {
                set({isPositionSaving});
            },

            canvasZoom: DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
            setCanvasZoom: (canvasZoom) => {
                set({canvasZoom});
            },

            nodesLocked: true,
            setNodesLocked: (nodesLocked) => {
                set({nodesLocked});
            },

            layoutResetCounter: 0,
            incrementLayoutResetCounter: () => {
                set((state) => ({layoutResetCounter: state.layoutResetCounter + 1}));
            },

            reset: () => {
                set({
                    canvasZoom: DEFAULT_CLUSTER_ELEMENT_CANVAS_ZOOM,
                    draggingNodeId: null,
                    edges: [],
                    isNodeDragging: false,
                    isPositionSaving: false,
                    layoutResetCounter: 0,
                    nodes: [],
                    nodesLocked: true,
                });
            },
        }),
        {name: 'cluster-elements-data'}
    )
);

export default useClusterElementsDataStore;
