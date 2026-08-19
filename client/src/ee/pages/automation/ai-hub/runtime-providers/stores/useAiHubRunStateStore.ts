import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * Per-chat run state for AI Hub, keyed by AG-UI thread id. Split out of AiHubRuntimeProvider's
 * component state: the provider renders one focused chat at a time, but the resume/attach feature
 * lets multiple chats stream concurrently. A single `isAgentRunning` boolean let a turn streaming
 * in one chat leak its "running" state into another chat's composer on a chat switch. Keying every
 * signal by thread id makes each chat's run state independent.
 */
interface AiHubRunStateI {
    /** threadId -> the AG-UI agent run for that chat is currently active. */
    runningByChat: Record<string, boolean>;
    /**
     * threadId -> count of in-flight runChatWorkflow sub-streams for that chat. The composer must
     * stay disabled until every fire-and-forget workflow SSE settles, not just the agent run.
     */
    inflightStreamCountByChat: Record<string, number>;
    /**
     * threadId -> the runId of that chat's current turn. Sent with the cancelAiHubRun mutation so
     * the server can tombstone the exact run even when the Stop click races the run's registration.
     * Entries are never removed per-chat (only wiped wholesale by reset) — a few bytes per defunct
     * chat id is a negligible leak, matching the chatLlmSelections decision in useAiHubChatsStore.
     */
    runIdByChat: Record<string, string>;
    adjustInflightStreamCount: (threadId: string | undefined, delta: number) => void;
    reset: () => void;
    setChatRunId: (threadId: string | undefined, runId: string) => void;
    setChatRunning: (threadId: string | undefined, running: boolean) => void;
}

export const aiHubRunStateStore = create<AiHubRunStateI>()(
    devtools((set) => ({
        adjustInflightStreamCount: (threadId, delta) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                const next = Math.max(0, (state.inflightStreamCountByChat[threadId] ?? 0) + delta);

                return {
                    inflightStreamCountByChat: {...state.inflightStreamCountByChat, [threadId]: next},
                };
            }),
        inflightStreamCountByChat: {},
        reset: () => set({inflightStreamCountByChat: {}, runIdByChat: {}, runningByChat: {}}),
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

/**
 * True when the given chat has an active agent run OR at least one in-flight workflow sub-stream.
 * Standalone pure function so it can be unit-tested and reused as a Zustand selector.
 */
export const isChatRunning = (
    state: Pick<AiHubRunStateI, 'inflightStreamCountByChat' | 'runningByChat'>,
    threadId: string | undefined
): boolean => {
    if (threadId == null) {
        return false;
    }

    return (state.runningByChat[threadId] ?? false) || (state.inflightStreamCountByChat[threadId] ?? 0) > 0;
};
