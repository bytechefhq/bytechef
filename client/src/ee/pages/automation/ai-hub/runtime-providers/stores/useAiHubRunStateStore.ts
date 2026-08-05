import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

/**
 * Per-task run state for AI Hub, keyed by AG-UI thread id. Split out of AiHubRuntimeProvider's
 * component state: the provider renders one focused task at a time, but the resume/attach feature
 * lets multiple tasks stream concurrently. A single `isAgentRunning` boolean let a turn streaming
 * in one task leak its "running" state into another task's composer on a task switch. Keying every
 * signal by thread id makes each task's run state independent.
 */
interface AiHubRunStateI {
    /** threadId -> the AG-UI agent run for that task is currently active. */
    runningByTask: Record<string, boolean>;
    /**
     * threadId -> count of in-flight runChatWorkflow sub-streams for that task. The composer must
     * stay disabled until every fire-and-forget workflow SSE settles, not just the agent run.
     */
    inflightStreamCountByTask: Record<string, number>;
    /**
     * threadId -> the runId of that task's current turn. Sent with the cancelAiHubRun mutation so
     * the server can tombstone the exact run even when the Stop click races the run's registration.
     * Entries are never removed per-task (only wiped wholesale by reset) — a few bytes per defunct
     * task id is a negligible leak, matching the taskLlmSelections decision in useAiHubTasksStore.
     */
    runIdByTask: Record<string, string>;
    adjustInflightStreamCount: (threadId: string | undefined, delta: number) => void;
    reset: () => void;
    setTaskRunId: (threadId: string | undefined, runId: string) => void;
    setTaskRunning: (threadId: string | undefined, running: boolean) => void;
}

export const aiHubRunStateStore = create<AiHubRunStateI>()(
    devtools((set) => ({
        adjustInflightStreamCount: (threadId, delta) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                const next = Math.max(0, (state.inflightStreamCountByTask[threadId] ?? 0) + delta);

                return {
                    inflightStreamCountByTask: {...state.inflightStreamCountByTask, [threadId]: next},
                };
            }),
        inflightStreamCountByTask: {},
        reset: () => set({inflightStreamCountByTask: {}, runIdByTask: {}, runningByTask: {}}),
        runIdByTask: {},
        runningByTask: {},
        setTaskRunId: (threadId, runId) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                return {runIdByTask: {...state.runIdByTask, [threadId]: runId}};
            }),
        setTaskRunning: (threadId, running) =>
            set((state) => {
                if (threadId == null) {
                    return state;
                }

                return {runningByTask: {...state.runningByTask, [threadId]: running}};
            }),
    }))
);

export const useAiHubRunStateStore = aiHubRunStateStore;

/**
 * True when the given task has an active agent run OR at least one in-flight workflow sub-stream.
 * Standalone pure function so it can be unit-tested and reused as a Zustand selector.
 */
export const isTaskRunning = (
    state: Pick<AiHubRunStateI, 'inflightStreamCountByTask' | 'runningByTask'>,
    threadId: string | undefined
): boolean => {
    if (threadId == null) {
        return false;
    }

    return (state.runningByTask[threadId] ?? false) || (state.inflightStreamCountByTask[threadId] ?? 0) > 0;
};
