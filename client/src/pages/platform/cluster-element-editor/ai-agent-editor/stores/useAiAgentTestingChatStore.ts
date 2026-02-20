import {
    ToolExecutionEventI,
    addToolExecutionToLastAssistantMessage as addToolExecutionHelper,
    appendToLastAssistantMessage as appendHelper,
    setLastAssistantMessageContent as setContentHelper,
} from '@/shared/util/assistant-message-utils';
import {ThreadMessageLike} from '@assistant-ui/react';

/* eslint-disable sort-keys */
import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

interface AiAgentTestingChatStateI {
    conversationId: string | undefined;
    generateConversationId: () => void;

    messages: ThreadMessageLike[];
    setMessage: (message: ThreadMessageLike) => void;
    addToolExecution: (toolExecution: ToolExecutionEventI) => void;
    appendToLastAssistantMessage: (delta: string) => void;
    setLastAssistantMessageContent: (content: string) => void;
    setLastAssistantMessageError: (errorMessage: string) => void;
    resetMessages: () => void;
}

function generateRandomId(): string {
    return Array(32)
        .fill(0)
        .map(() => Math.random().toString(36).charAt(2))
        .join('');
}

const useAiAgentTestingChatStore = create<AiAgentTestingChatStateI>()(
    devtools(
        (set) => ({
            conversationId: undefined,
            generateConversationId: () => {
                set({conversationId: generateRandomId()});
            },

            messages: [],
            setMessage: (message) =>
                set((state) => ({
                    messages: [...state.messages, message],
                })),
            addToolExecution: (toolExecution: ToolExecutionEventI) =>
                set((state) => ({
                    messages: addToolExecutionHelper(state.messages, toolExecution),
                })),
            appendToLastAssistantMessage: (delta: string) =>
                set((state) => ({
                    messages: appendHelper(state.messages, delta),
                })),
            setLastAssistantMessageContent: (content: string) =>
                set((state) => ({
                    messages: setContentHelper(state.messages, content),
                })),
            setLastAssistantMessageError: (errorMessage: string) =>
                set((state) => {
                    const updatedMessages = [...state.messages];
                    const errorStatus = {error: errorMessage, reason: 'error' as const, type: 'incomplete' as const};

                    for (let i = updatedMessages.length - 1; i >= 0; i--) {
                        if (updatedMessages[i]?.role === 'assistant') {
                            updatedMessages[i] = {...updatedMessages[i], status: errorStatus};

                            return {messages: updatedMessages};
                        }
                    }

                    return {
                        messages: [
                            ...updatedMessages,
                            {content: '', role: 'assistant', status: errorStatus} as ThreadMessageLike,
                        ],
                    };
                }),
            resetMessages: () => set({messages: []}),
        }),
        {
            name: 'bytechef.ai-agent-testing-chat',
        }
    )
);

export default useAiAgentTestingChatStore;
