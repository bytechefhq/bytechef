import {ThreadMessageLike} from '@assistant-ui/react';
import {afterEach, describe, expect, it} from 'vitest';

import useAiAgentTestingChatStore from './useAiAgentTestingChatStore';

function resetStore() {
    useAiAgentTestingChatStore.getState().resetMessages();
}

describe('useAiAgentTestingChatStore', () => {
    afterEach(resetStore);

    describe('setMessage', () => {
        it('should append a user message', () => {
            const {setMessage} = useAiAgentTestingChatStore.getState();
            const message: ThreadMessageLike = {content: 'hello', role: 'user'};

            setMessage(message);

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(1);
            expect(messages[0]).toEqual(message);
        });

        it('should append multiple messages in order', () => {
            const {setMessage} = useAiAgentTestingChatStore.getState();

            setMessage({content: 'hello', role: 'user'});
            setMessage({content: 'hi', role: 'assistant'});

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(2);
            expect(messages[0]?.role).toBe('user');
            expect(messages[1]?.role).toBe('assistant');
        });
    });

    describe('appendToLastAssistantMessage', () => {
        it('should append delta to existing assistant message', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'Hello', role: 'assistant'});
            store.appendToLastAssistantMessage(' world');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(1);
            expect((messages[0] as {content: string}).content).toBe('Hello world');
        });

        it('should create new assistant message when none exists', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'user msg', role: 'user'});
            store.appendToLastAssistantMessage('streamed');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(2);
            expect(messages[1]?.role).toBe('assistant');
            expect((messages[1] as {content: string}).content).toBe('streamed');
        });
    });

    describe('setLastAssistantMessageContent', () => {
        it('should replace content of the last assistant message', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'partial', role: 'assistant'});
            store.setLastAssistantMessageContent('complete response');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(1);
            expect((messages[0] as {content: string}).content).toBe('complete response');
        });
    });

    describe('setLastAssistantMessageError', () => {
        it('should set error status on the last assistant message', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'user question', role: 'user'});
            store.setMessage({content: 'partial answer', role: 'assistant'});
            store.setLastAssistantMessageError('Connection timed out');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(2);

            const assistantMessage = messages[1] as {content: string; role: string; status: object};

            expect(assistantMessage.role).toBe('assistant');
            expect(assistantMessage.content).toBe('partial answer');
            expect(assistantMessage.status).toEqual({
                error: 'Connection timed out',
                reason: 'error',
                type: 'incomplete',
            });
        });

        it('should find the last assistant message when multiple exist', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'first response', role: 'assistant'});
            store.setMessage({content: 'follow-up', role: 'user'});
            store.setMessage({content: 'second response', role: 'assistant'});
            store.setLastAssistantMessageError('Server error');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(3);

            const firstAssistant = messages[0] as {status?: object};
            const secondAssistant = messages[2] as {status?: object};

            expect(firstAssistant.status).toBeUndefined();
            expect(secondAssistant.status).toEqual({
                error: 'Server error',
                reason: 'error',
                type: 'incomplete',
            });
        });

        it('should create new assistant message with error when none exists', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'user question', role: 'user'});
            store.setLastAssistantMessageError('Failed to connect');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(2);

            const errorMessage = messages[1] as {content: string; role: string; status: object};

            expect(errorMessage.role).toBe('assistant');
            expect(errorMessage.content).toBe('');
            expect(errorMessage.status).toEqual({
                error: 'Failed to connect',
                reason: 'error',
                type: 'incomplete',
            });
        });

        it('should create assistant message with error when messages are empty', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setLastAssistantMessageError('No messages yet');

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(1);

            const errorMessage = messages[0] as {content: string; role: string; status: object};

            expect(errorMessage.role).toBe('assistant');
            expect(errorMessage.content).toBe('');
            expect(errorMessage.status).toEqual({
                error: 'No messages yet',
                reason: 'error',
                type: 'incomplete',
            });
        });

        it('should not mutate original messages array', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'response', role: 'assistant'});

            const messagesBefore = useAiAgentTestingChatStore.getState().messages;

            store.setLastAssistantMessageError('error');

            const messagesAfter = useAiAgentTestingChatStore.getState().messages;

            expect(messagesBefore).not.toBe(messagesAfter);
        });
    });

    describe('resetMessages', () => {
        it('should clear all messages', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.setMessage({content: 'hello', role: 'user'});
            store.setMessage({content: 'hi', role: 'assistant'});
            store.resetMessages();

            const {messages} = useAiAgentTestingChatStore.getState();

            expect(messages).toHaveLength(0);
        });
    });

    describe('generateConversationId', () => {
        it('should generate a conversation id string', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.generateConversationId();

            const {conversationId} = useAiAgentTestingChatStore.getState();

            expect(typeof conversationId).toBe('string');
            expect(conversationId!.length).toBe(32);
        });

        it('should generate different ids on subsequent calls', () => {
            const store = useAiAgentTestingChatStore.getState();

            store.generateConversationId();

            const firstId = useAiAgentTestingChatStore.getState().conversationId;

            store.generateConversationId();

            const secondId = useAiAgentTestingChatStore.getState().conversationId;

            expect(firstId).not.toBe(secondId);
        });
    });
});
