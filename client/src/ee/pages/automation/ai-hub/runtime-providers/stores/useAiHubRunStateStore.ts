import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * Per-chat run state for AI Hub, keyed by AG-UI thread id, so a turn streaming in one chat never shows another
 * chat's composer as running after a chat switch.
 */
interface AiHubRunStateI {
    reset: () => void;
    /** threadId -> the runId of that chat's current turn, sent with the cancelAiHubRun mutation. */
    runIdByChat: Record<string, string>;
    /** threadId -> the AG-UI agent run for that chat is currently active. */
    runningByChat: Record<string, boolean>;
    setChatRunId: (threadId: string | undefined, runId: string) => void;
    setChatRunning: (threadId: string | undefined, running: boolean) => void;
}

export const aiHubRunStateStore = create<AiHubRunStateI>()(
    devtools((set) => ({
        reset: () => set({runIdByChat: {}, runningByChat: {}}),
        runIdByChat: {},
        runningByChat: {},
        setChatRunId: (threadId, runId) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                return {runIdByChat: {...state.runIdByChat, [threadId]: runId}};
            }),
        setChatRunning: (threadId, running) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                return {runningByChat: {...state.runningByChat, [threadId]: running}};
            }),
    }))
);

export const useAiHubRunStateStore = aiHubRunStateStore;

export const isChatRunning = (state: Pick<AiHubRunStateI, 'runningByChat'>, threadId: string | undefined): boolean => {
    if (threadId == null) {
        return false;
    }

    return state.runningByChat[threadId] ?? false;
};
