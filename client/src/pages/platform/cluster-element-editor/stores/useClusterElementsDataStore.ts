/* eslint-disable sort-keys */

import {Edge, Node, OnEdgesChange, OnNodesChange, applyEdgeChanges, applyNodeChanges} from '@xyflow/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

import defaultAiAgentEdges from '../edges/defaultAiAgentEdges';
import defaultAiAgentNodes from '../nodes/defaultAiAgentNodes';

interface ClusterElementsDataStoreI {
    edges: Edge[];
    setEdges: (edges: Edge[]) => void;
    onEdgesChange: OnEdgesChange;

    nodes: Node[];
    setNodes: (nodes: Node[]) => void;
    onNodesChange: OnNodesChange;
}

const useClusterElementsDataStore = create<ClusterElementsDataStoreI>()(
    devtools(
        (set, get) => ({
            edges: defaultAiAgentEdges,
            setEdges: (edges) => {
                set({edges});
            },
            onEdgesChange: (changes) => {
                set({
                    edges: applyEdgeChanges(changes, get().edges),
                });
            },

            nodes: defaultAiAgentNodes,
            setNodes: (nodes) => {
                set({nodes});
            },
            onNodesChange: (changes) => {
                set({
                    nodes: applyNodeChanges(changes, get().nodes),
                });
            },
        }),
        {name: 'cluster-elements-data'}
    )
);

export default useClusterElementsDataStore;
