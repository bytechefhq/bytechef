/* eslint-disable sort-keys */
import {
    type ToolApprovalResolutionI,
    stampToolApprovalResolution,
} from '@/ee/pages/automation/ai-hub/chats/pendingToolApproval';
import {getServerRowIndex, withServerRowIndex} from '@/ee/pages/automation/ai-hub/stores/serverRowIndex';
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

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    editUserMessage: (index: number, content: string) => void;
    markToolApprovalResolved: (approvalId: number, resolution: ToolApprovalResolutionI) => void;
    resetMessages: () => void;

    /** The server transcript row index the next persisted user or assistant message will take. */
    nextServerRowIndex: number;
    setNextServerRowIndex: (nextServerRowIndex: number) => void;
    /** Marks the message at `index` with the next server row index, unless it is already marked. */
    tagServerRow: (index: number) => void;

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
                messages: [...state.messages, message],
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
        markToolApprovalResolved: (approvalId, resolution) =>
            set((state) => ({
                ...state,
                messages: stampToolApprovalResolution(state.messages, approvalId, resolution),
            })),
        resetMessages: () => set({messages: [], messagesLoading: false, nextServerRowIndex: 0}),

        nextServerRowIndex: 0,
        setNextServerRowIndex: (nextServerRowIndex) => set({nextServerRowIndex}),
        tagServerRow: (index) =>
            set((state) => {
                const message = state.messages[index];

                if (message == null || getServerRowIndex(message) != null) {
                    return state;
                }

                const messages = [...state.messages];

                messages[index] = withServerRowIndex(message, state.nextServerRowIndex);

                return {...state, messages, nextServerRowIndex: state.nextServerRowIndex + 1};
            }),

        messagesLoading: false,
        setMessagesLoading: (messagesLoading) => set({messagesLoading}),
    }))
);

export const useAiHubStore = aiHubStore;
