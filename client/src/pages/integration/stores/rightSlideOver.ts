import {create} from 'zustand';

interface Node {
    label: string;
}

interface RightSlideOverState {
    rightSlideOverOpen: boolean;
    setRightSlideOverOpen: (rightSlideOverOpen: boolean) => void;

    currentNode: Node;
    setCurrentNode: (currentNode: Node) => void;
}

export const useRightSlideOverStore = create<RightSlideOverState>()((set) => ({
    rightSlideOverOpen: false,
    setRightSlideOverOpen: (rightSlideOverOpen) =>
        set((state) => ({...state, rightSlideOverOpen})),

    currentNode: {label: ''},
    setCurrentNode: (currentNode) => set((state) => ({...state, currentNode})),
}));

export default useRightSlideOverStore;
