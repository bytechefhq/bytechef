/* eslint-disable sort-keys */
import {
    type ToolApprovalResolutionI,
    stampToolApprovalResolution,
    supersedePendingToolApprovals,
} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {generateMessageId} from '@/ee/pages/automation/ai-hub/stores/messageId';
import {generateRandomId} from '@/shared/util/random-utils';
import {ThreadMessageLike} from '@assistant-ui/react';
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    ASK = 'ASK',
    BUILD = 'BUILD',
}

interface AiHubStateI {
    /** The AG-UI thread id of the focused chat. */
    chatId: string | undefined;
    generateChatId: () => void;

    mode: MODE;
    setMode: (mode: MODE) => void;

    /** The focused chat's thread. Every message carries an id; a message added without one gets a fresh one. */
    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    /** Replaces the user message at `index` with `content` under `messageId` and drops every message after it. */
    editUserMessage: (index: number, content: string, messageId: string) => void;
    markToolApprovalResolved: (approvalId: number, resolution: ToolApprovalResolutionI) => void;
    /** Stamps every approval still awaiting a decision as superseded, as the server does when it accepts a turn. */
    markPendingToolApprovalsSuperseded: () => void;
    resetMessages: () => void;

    /**
     * Counts the transcripts the thread has shown. A chat switch or reset starts a new one, so a turn subscriber built
     * for an earlier transcript stops writing even when the user comes back to its chat.
     */
    transcriptGeneration: number;

    /** True while a chat switch is fetching that chat's history, so the thread shows a loading placeholder. */
    messagesLoading: boolean;
    setMessagesLoading: (loading: boolean) => void;
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
                messages: [...state.messages, message.id == null ? {...message, id: generateMessageId()} : message],
            })),
        appendToLastAssistantMessage: (text: string) =>
            set((state) => {
                const messages = [...state.messages];

                for (let index = messages.length - 1; index >= 0; index--) {
                    const message = messages[index] as ThreadMessageLike;

                    if (message.role === 'user') {
                        break;
                    }

                    if (message.role === 'assistant' && typeof message.content === 'string') {
                        messages[index] = {...message, content: text} as ThreadMessageLike;

                        return {...state, messages};
                    }
                }

                messages.push({content: text, id: generateMessageId(), role: 'assistant'} as ThreadMessageLike);

                return {...state, messages};
            }),
        editUserMessage: (index, content, messageId) =>
            set((state) => {
                if (index < 0 || index >= state.messages.length) {
                    return state;
                }

                const target = state.messages[index];

                if (target?.role !== 'user') {
                    return state;
                }

                const truncated = state.messages.slice(0, index);

                truncated.push({...target, content, id: messageId} as ThreadMessageLike);

                return {...state, messages: truncated};
            }),
        markToolApprovalResolved: (approvalId, resolution) =>
            set((state) => ({
                ...state,
                messages: stampToolApprovalResolution(state.messages, approvalId, resolution),
            })),
        markPendingToolApprovalsSuperseded: () =>
            set((state) => ({
                ...state,
                messages: supersedePendingToolApprovals(state.messages),
            })),
        resetMessages: () =>
            set((state) => ({
                messages: [],
                messagesLoading: false,
                transcriptGeneration: state.transcriptGeneration + 1,
            })),

        transcriptGeneration: 0,

        messagesLoading: false,
        setMessagesLoading: (messagesLoading) => set({messagesLoading}),
    }))
);

export const useAiHubStore = aiHubStore;
