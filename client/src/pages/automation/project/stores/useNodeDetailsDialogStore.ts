import {ReactNode} from 'react';
import {create} from 'zustand';
import {devtools, persist} from 'zustand/middleware';

interface Node {
    name: string;
    version: number;
    type: 'component' | 'flowControl';
    icon?: ReactNode;
    originNodeName?: string;
    label?: string;
}

interface NodeDetailsState {
    nodeDetailsOpen: boolean;
    setNodeDetailsOpen: (nodeDetailsOpen: boolean) => void;

    currentNode: Node;
    setCurrentNode: (currentNode: Node) => void;
}
interface ConnectionNoteState {
    showConnectionNote: boolean;
    setShowConnectionNote: (showConnectionNote: boolean) => void;
}

export const useNodeDetailsDialogStore = create<NodeDetailsState>()((set) => ({
    nodeDetailsOpen: false,
    setNodeDetailsOpen: (nodeDetailsOpen) =>
        set((state) => ({...state, nodeDetailsOpen})),

    currentNode: {name: '', type: 'component', version: 1},
    setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),
}));

export const useConnectionNoteStore = create<ConnectionNoteState>()(
    devtools(
        persist(
            (set) => ({
                showConnectionNote: true,
                setShowConnectionNote: (connectionNoteStatus) =>
                    set(() => ({
                        showConnectionNote: connectionNoteStatus,
                    })),
            }),
            {
                name: 'show-connection-note',
            }
        )
    )
);
