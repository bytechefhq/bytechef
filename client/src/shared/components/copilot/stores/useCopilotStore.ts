import {generateRandomId} from '@/shared/util/random-utils';
import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */

import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export enum MODE {
    ASK = 'ASK',
    BUILD = 'BUILD',
}

export enum Source {
    WORKFLOW_EXECUTION = 'WORKFLOW_EXECUTION',
    WORKFLOW_EDITOR = 'WORKFLOW_EDITOR',
    CODE_EDITOR = 'CODE_EDITOR',
    CLUSTER_ELEMENT = 'CLUSTER_ELEMENT',
    SKILLS = 'SKILLS',
}

export type ContextType = {
    source: Source;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    parameters: Record<string, any>;
    mode: MODE;
    workflowExecutionError?: {
        errorMessage?: string;
        stackTrace?: string[];
        title?: string;
        workflowId?: string;
    };
};

interface CopilotStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    context: ContextType;
    setContext: (context: ContextType | undefined) => void;
    setWorkflowExecutionError: (
        workflowExecutionError:
            | {
                  errorMessage?: string;
                  stackTrace?: string[];
                  title?: string;
                  workflowId?: string;
              }
            | undefined
    ) => void;

    messages: ThreadMessageLike[];
    addMessage: (message: ThreadMessageLike) => void;
    appendToLastAssistantMessage: (text: string) => void;
    editUserMessage: (index: number, content: string) => void;
    resetMessages: () => void;
    selectedLlmProvider: string | null;
    selectedLlmModel: string | null;
    setSelectedLlm: (provider: string | null, model: string | null) => void;

    savedState: {
        conversationId: string | undefined;
        context: ContextType;
        messages: ThreadMessageLike[];
        selectedLlmProvider: string | null;
        selectedLlmModel: string | null;
    } | null;
    saveConversationState: () => void;
    restoreConversationState: () => void;
}

export const useCopilotStore = create<CopilotStateI>()(
    devtools((set) => ({
        conversationId: generateRandomId(),
        generateConversationId: () => {
            // New conversation = reset the picker too. The picker is "per conversation" by design, so a
            // user who picked GPT-4o in one conversation lands on the workspace default in the next one
            // rather than carrying the selection across unrelated threads.
            set({conversationId: generateRandomId(), selectedLlmModel: null, selectedLlmProvider: null});
        },

        context: {
            source: Source.WORKFLOW_EDITOR,
            parameters: {},
            mode: MODE.ASK,
            workflowExecutionError: undefined,
        },
        setContext: (context) =>
            set((state) => {
                return {
                    ...state,
                    context,
                };
            }),
        setWorkflowExecutionError: (error) =>
            set((state) => {
                return {
                    ...state,
                    context: {
                        ...state.context,
                        workflowExecutionError: error,
                    },
                };
            }),

        messages: [],
        addMessage: (message) =>
            set((state) => {
                return {
                    ...state,
                    messages: [...state.messages, message],
                };
            }),
        appendToLastAssistantMessage: (text: string) =>
            set((state) => {
                const messages = [...state.messages];

                // Scan back from the end for this turn's streaming assistant message, stopping at the latest user
                // message so we never reach into a previous turn and overwrite its reply (issue #5348).
                for (let i = messages.length - 1; i >= 0; i--) {
                    const message = messages[i] as ThreadMessageLike;

                    if (message.role === 'user') {
                        break;
                    }

                    if (message.role === 'assistant' && typeof message.content === 'string') {
                        messages[i] = {...message, content: text} as ThreadMessageLike;

                        return {...state, messages};
                    }
                }

                // No assistant message for the current turn yet; create one.
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
        resetMessages: () => set({messages: []}),

        // Per-conversation LLM picker selection. Stored as nullable strings; the runtime provider injects
        // both into the AG-UI state on every chat request when set. Null means "no override — server uses
        // the workspace @Primary ChatModel."
        selectedLlmProvider: null,
        selectedLlmModel: null,
        setSelectedLlm: (provider, model) => set({selectedLlmModel: model, selectedLlmProvider: provider}),

        savedState: null,
        saveConversationState: () =>
            set((state) => ({
                ...state,
                savedState: {
                    conversationId: state.conversationId,
                    context: state.context,
                    messages: state.messages,
                    selectedLlmModel: state.selectedLlmModel,
                    selectedLlmProvider: state.selectedLlmProvider,
                },
            })),
        restoreConversationState: () =>
            set((state) => {
                if (!state.savedState) {
                    return state;
                }

                return {
                    ...state,
                    conversationId: state.savedState.conversationId,
                    context: state.savedState.context,
                    messages: state.savedState.messages,
                    savedState: null,
                    selectedLlmModel: state.savedState.selectedLlmModel,
                    selectedLlmProvider: state.savedState.selectedLlmProvider,
                };
            }),
    }))
);
