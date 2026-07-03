import {create} from 'zustand';

/**
 * Per-session store of which askUserQuestion prompts the user has already answered. Keyed by a content
 * fingerprint of the question + options (see fingerprintQuestions in AskUserQuestionMessage.tsx) since
 * the server doesn't issue correlation ids for the chat-memory answer-flow path — the fingerprint is a stable
 * proxy for "the same question instance."
 *
 * State lives in memory only — page reloads start fresh. This is intentional: a refreshed thread re-reads
 * chat memory from the server, and any already-answered questions there are already in the transcript as
 * system messages, so re-disabling the buttons would be a courtesy only. Persisting across reloads via
 * localStorage was considered but adds the risk of stale entries pinning state for questions that no longer
 * exist (e.g. after the user deletes a thread).
 */
interface AiHubAskedQuestionsStateI {
    answers: Record<string, string>;
    getAnswer: (fingerprint: string) => string | undefined;
    hasAnswered: (fingerprint: string) => boolean;
    markAnswered: (fingerprint: string, answer: string) => void;
    reset: () => void;
}

export const aiChatAskedQuestionsStore = create<AiHubAskedQuestionsStateI>((set, get) => ({
    answers: {},
    getAnswer: (fingerprint) => get().answers[fingerprint],
    hasAnswered: (fingerprint) => get().answers[fingerprint] !== undefined,
    markAnswered: (fingerprint, answer) =>
        set((state) => ({
            answers: {...state.answers, [fingerprint]: answer},
        })),
    reset: () => set({answers: {}}),
}));

export const useAiChatAskedQuestionsStore = aiChatAskedQuestionsStore;
