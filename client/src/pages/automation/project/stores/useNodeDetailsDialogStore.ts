/* eslint-disable sort-keys */
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
    nodeDetailsDialogOpen: boolean;
    setNodeDetailsDialogOpen: (nodeDetailsDialogOpen: boolean) => void;

    currentNode: Node;
    setCurrentNode: (currentNode: Node) => void;

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    focusedInput: any;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    setFocusedInput: (focusedInput: any) => void;
}

interface ConnectionNoteState {
    showConnectionNote: boolean;
    setShowConnectionNote: (showConnectionNote: boolean) => void;
}

export const useNodeDetailsDialogStore = create<NodeDetailsState>()((set) => ({
    nodeDetailsDialogOpen: false,
    setNodeDetailsDialogOpen: (nodeDetailsDialogOpen) =>
        set((state) => ({...state, nodeDetailsDialogOpen})),

    currentNode: {name: '', type: 'component', version: 1},
    setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),

    focusedInput: null,
    setFocusedInput: (focusedInput) =>
        set((state) => ({...state, focusedInput})),
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
