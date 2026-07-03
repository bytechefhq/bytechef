import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export interface AiHubRetryableErrorI {
    errorMessage: string;
    lastUserMessage: string;
    toolName: string;
}

interface StoreStateI {
    currentError: AiHubRetryableErrorI | undefined;
    clearError: () => void;
    setError: (error: AiHubRetryableErrorI) => void;
}

export const aiChatRetryableErrorStore = create<StoreStateI>()(
    devtools((set) => ({
        clearError: () => set({currentError: undefined}),
        currentError: undefined,
        setError: (error) => set({currentError: error}),
    }))
);

export const useAiChatRetryableErrorStore = aiChatRetryableErrorStore;
