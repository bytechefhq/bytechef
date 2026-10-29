import {create} from 'zustand';

interface AgentExternalChangeStateI {
    bumpRevision: () => void;
    revision: number;
}

/**
 * Counts the AI Agent changes made outside the agent's own detail cards (by the Copilot or the AI Hub), so the cards
 * can remount with the refetched agent instead of keeping the values they were mounted with.
 */
export const useAgentExternalChangeStore = create<AgentExternalChangeStateI>()((set) => ({
    bumpRevision: () => set((state) => ({revision: state.revision + 1})),
    revision: 0,
}));
