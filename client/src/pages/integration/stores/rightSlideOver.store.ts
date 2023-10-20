import {create} from 'zustand';

interface Node {
    label: string;
}

interface IntegrationState {
    rightSlideOverOpen: boolean;
    setRightSlideOverOpen: (rightSlideOverOpen: boolean) => void;

    currentNode: Node;
    setCurrentNode: (currentNode: Node) => void;
}

export const useRightSlideOverStore = create<IntegrationState>()((set) => ({
    rightSlideOverOpen: false,
    setRightSlideOverOpen: (rightSlideOverOpen) =>
        set((state) => ({...state, rightSlideOverOpen})),

    currentNode: {label: ''},
    setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),
}));

export default useRightSlideOverStore;
