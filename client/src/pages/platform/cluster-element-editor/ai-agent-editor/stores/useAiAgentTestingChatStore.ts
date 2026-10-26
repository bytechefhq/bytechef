import {
    ToolExecutionEventI,
    addToolExecutionToLastAssistantMessage as addToolExecutionHelper,
    appendToLastAssistantMessage as appendHelper,
    setLastAssistantMessageContent as setContentHelper,
    setLastAssistantMessageError as setErrorHelper,
} from '@/shared/util/assistant-message-utils';
import {generateRandomId} from '@/shared/util/random-utils';
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
    truncateMessagesFrom: (index: number) => void;
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
                set((state) => ({
                    messages: setErrorHelper(state.messages, errorMessage),
                })),
            resetMessages: () => set({messages: []}),
            truncateMessagesFrom: (index: number) =>
                set((state) => ({
                    messages: state.messages.slice(0, Math.max(0, index)),
                })),
        }),
        {
            name: 'bytechef.ai-agent-testing-chat',
        }
    )
);

export default useAiAgentTestingChatStore;
