import {create} from 'zustand';

/**
 * Per-session store of which askUserQuestion prompts the user has already answered, keyed by a content
 * fingerprint of the question + options (see fingerprintQuestions in AskUserQuestionMessage.tsx) since the
 * server issues no correlation ids for the answer flow. In-memory only — a refreshed thread re-reads answered
 * questions from the chat-memory transcript, so persistence would add stale-entry risk for no real gain.
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
