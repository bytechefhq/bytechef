/* eslint-disable sort-keys */
import {generateRandomId} from '@/shared/util/random-utils';
import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    ASK = 'ASK',
    BUILD = 'BUILD',
}

interface AiHubStateI {
    chatId: string | undefined;
    generateChatId: () => void;

    mode: MODE;
    setMode: (mode: MODE) => void;

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    editUserMessage: (index: number, content: string) => void;
    resetMessages: () => void;

    // True while a chat switch is fetching that chat's history. The thread shows a loading placeholder
    // (not the "What should we get done?" empty state) so switching chats reads as load→content instead of
    // flashing the welcome/empty thread before the messages arrive.
    messagesLoading: boolean;
    setMessagesLoading: (loading: boolean) => void;

    // Voice transcript queue between the voice hook's onTranscript callback and the composer's effect that
    // dispatches into the AG-UI runtime. Carries a monotonic seq so repeated identical transcripts
    // ('yes' / 'yes') aren't coalesced by the composer's dedupe ref.
    pendingVoiceUserMessage: {text: string; seq: number} | null;
    setPendingVoiceUserMessage: (text: string | null) => void;
}

export const aiHubStore = create<AiHubStateI>()(
    devtools((set) => ({
        chatId: generateRandomId(),
        generateChatId: () => {
            set({chatId: generateRandomId()});
        },

        mode: MODE.BUILD,
        setMode: (mode) => set({mode}),

        messages: [],
        addMessage: (message) =>
            set((state) => ({
                ...state,
                messages: [...state.messages, message],
            })),
        appendToLastAssistantMessage: (text: string) =>
            set((state) => {
                const messages = [...state.messages];

                for (let i = messages.length - 1; i >= 0; i--) {
                    const message = messages[i] as ThreadMessageLike;

                    // Stop at the most recent user message. The streaming reply belongs to the current turn, which
                    // begins after that message, so a fresh assistant is pushed at the end rather than reusing one
                    // found before it. Without this boundary a resumed stream — when the trailing message is an
                    // artifact-link card (array content) or the user's message itself after a chat-switch reload —
                    // would walk past both and overwrite a PRIOR turn's assistant, rendering the reply above the
                    // user's message.
                    if (message.role === 'user') {
                        break;
                    }

                    if (message.role === 'assistant' && typeof message.content === 'string') {
                        messages[i] = {...message, content: text} as ThreadMessageLike;

                        return {...state, messages};
                    }
                }

                messages.push({role: 'assistant', content: text} as ThreadMessageLike);

                return {...state, messages};
            }),
        editUserMessage: (index, content) =>
            set((state) => {
                if (index < 0 || index >= state.messages.length) {
                    return state;
                }

                const target = state.messages[index];

                if (target?.role !== 'user') {
                    return state;
                }

                const truncated = state.messages.slice(0, index);

                truncated.push({...target, content} as ThreadMessageLike);

                return {...state, messages: truncated};
            }),
        resetMessages: () => set({messages: [], messagesLoading: false}),

        messagesLoading: false,
        setMessagesLoading: (messagesLoading) => set({messagesLoading}),

        pendingVoiceUserMessage: null,
        setPendingVoiceUserMessage: (text) =>
            set((state) => {
                if (text == null) {
                    return {...state, pendingVoiceUserMessage: null};
                }

                const nextSeq = (state.pendingVoiceUserMessage?.seq ?? 0) + 1;

                return {...state, pendingVoiceUserMessage: {seq: nextSeq, text}};
            }),
    }))
);

export const useAiHubStore = aiHubStore;
