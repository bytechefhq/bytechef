import {create} from 'zustand';

interface AgentExternalChangeStateI {
    bumpRevision: () => void;
    revision: number;
}

export const useAgentExternalChangeStore = create<AgentExternalChangeStateI>()((set) => ({
    bumpRevision: () => set((state) => ({revision: state.revision + 1})),
    revision: 0,
}));
