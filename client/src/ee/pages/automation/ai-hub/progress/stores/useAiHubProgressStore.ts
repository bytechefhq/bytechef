import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface StoreStateI {
    clearProgress: () => void;
    progressText: string | null;
    setProgress: (subagentName: string, text: string) => void;
}

export const aiHubProgressStore = create<StoreStateI>()(
    devtools((set) => ({
        clearProgress: () => set({progressText: null}),
        progressText: null,
        setProgress: (subagentName, text) => set({progressText: `${subagentName}: ${text}`}),
    }))
);

export const useAiHubProgressStore = aiHubProgressStore;
