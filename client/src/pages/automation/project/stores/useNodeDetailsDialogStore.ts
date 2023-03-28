import {ReactNode} from 'react';
import {create} from 'zustand';

interface Node {
    name: string;
    version: number;
    type: 'component' | 'flowControl';
    icon?: ReactNode;
    label?: string;
}

interface NodeDetailsState {
    nodeDetailsOpen: boolean;
    setNodeDetailsOpen: (nodeDetailsOpen: boolean) => void;

    currentNode: Node;
    setCurrentNode: (currentNode: Node) => void;
}

export const useNodeDetailsStore = create<NodeDetailsState>()((set) => ({
    nodeDetailsOpen: false,
    setNodeDetailsOpen: (nodeDetailsOpen) =>
        set((state) => ({...state, nodeDetailsOpen})),

    currentNode: {name: '', type: 'component', version: 1},
    setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),
}));

export default useNodeDetailsStore;
