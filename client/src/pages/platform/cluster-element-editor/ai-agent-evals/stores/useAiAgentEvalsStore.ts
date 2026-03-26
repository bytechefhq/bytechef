import {create} from 'zustand';

type EvalsTabType = 'judges' | 'runs' | 'tests';

export const INITIAL_STATE = {
    evalsPanelOpen: false,
    evalsTab: 'tests' as EvalsTabType,
    selectedRunId: null as string | null,
    selectedTestId: null as string | null,
};

interface AiAgentEvalsStoreI {
    evalsPanelOpen: boolean;
    evalsTab: EvalsTabType;
    selectedRunId: string | null;
    selectedTestId: string | null;
    setEvalsPanelOpen: (open: boolean) => void;
    setEvalsTab: (tab: EvalsTabType) => void;
    setSelectedRunId: (id: string | null) => void;
    setSelectedTestId: (id: string | null) => void;
}

export const useAiAgentEvalsStore = create<AiAgentEvalsStoreI>((set) => ({
    ...INITIAL_STATE,
    setEvalsPanelOpen: (open: boolean) => set({evalsPanelOpen: open}),
    setEvalsTab: (tab: EvalsTabType) => set({evalsTab: tab}),
    setSelectedRunId: (id: string | null) => set({selectedRunId: id}),
    setSelectedTestId: (id: string | null) => set({selectedTestId: id}),
}));
