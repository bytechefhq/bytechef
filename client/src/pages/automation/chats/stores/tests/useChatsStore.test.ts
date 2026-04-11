import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useChatsStore} from '../useChatsStore';

describe('useWorkflowChatStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.resetAll();
        });
    });

    it('initializes with empty messages and a conversation ID', () => {
        const {result} = renderHook(() => useChatsStore());

        expect(result.current.messages).toEqual([]);
        expect(result.current.conversationId).toBeDefined();
        expect(typeof result.current.conversationId).toBe('string');
        expect(result.current.conversationId.length).toBe(32);
    });

    it('generates unique conversation IDs', () => {
        const {result: result1} = renderHook(() => useChatsStore());
        const id1 = result1.current.conversationId;

        act(() => {
            result1.current.resetAll();
        });

        const id2 = result1.current.conversationId;

        expect(id1).not.toBe(id2);
    });

    it('adds a message to the messages array', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'Hello', role: 'user'});
        });

        expect(result.current.messages).toHaveLength(1);
        expect(result.current.messages[0]).toEqual({content: 'Hello', role: 'user'});
    });

    it('adds multiple messages in order', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'First', role: 'user'});
            result.current.setMessage({content: 'Second', role: 'assistant'});
            result.current.setMessage({content: 'Third', role: 'user'});
        });

        expect(result.current.messages).toHaveLength(3);
        expect(result.current.messages[0]).toEqual({content: 'First', role: 'user'});
        expect(result.current.messages[1]).toEqual({content: 'Second', role: 'assistant'});
        expect(result.current.messages[2]).toEqual({content: 'Third', role: 'user'});
    });

    it('appends content to the last assistant message', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setMessage({content: 'Assistant ', role: 'assistant'});
            result.current.appendToLastAssistantMessage('response');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'Assistant response', role: 'assistant'});
    });

    it('creates a new assistant message if none exists when appending', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.appendToLastAssistantMessage('New assistant message');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'New assistant message', role: 'assistant'});
    });

    it('appends to the last assistant message when multiple exist', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'First assistant', role: 'assistant'});
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setMessage({content: 'Second assistant', role: 'assistant'});
            result.current.appendToLastAssistantMessage(' update');
        });

        expect(result.current.messages).toHaveLength(3);
        expect(result.current.messages[0]).toEqual({content: 'First assistant', role: 'assistant'});
        expect(result.current.messages[2]).toEqual({content: 'Second assistant update', role: 'assistant'});
    });

    it('sets content on the last assistant message', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setMessage({content: 'Old content', role: 'assistant'});
            result.current.setLastAssistantMessageContent('New content');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'New content', role: 'assistant'});
    });

    it('creates a new assistant message if none exists when setting content', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setLastAssistantMessageContent('Assistant content');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'Assistant content', role: 'assistant'});
    });

    it('resets messages to empty array', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'Message 1', role: 'user'});
            result.current.setMessage({content: 'Message 2', role: 'assistant'});
        });

        expect(result.current.messages).toHaveLength(2);

        act(() => {
            result.current.resetMessages();
        });

        expect(result.current.messages).toEqual([]);
    });

    it('resets all state including conversation cache', () => {
        const {result} = renderHook(() => useChatsStore());

        const originalId = result.current.conversationId;

        act(() => {
            result.current.switchChat('chat-1');
            result.current.setMessage({content: 'Message 1', role: 'user'});
            result.current.switchChat('chat-2');
            result.current.setMessage({content: 'Message 2', role: 'user'});
        });

        act(() => {
            result.current.resetAll();
        });

        expect(result.current.messages).toEqual([]);
        expect(result.current.conversationId).not.toBe(originalId);
        expect(result.current.conversationId.length).toBe(32);
        expect(result.current.activeWorkflowExecutionId).toBeNull();
        expect(result.current.conversationCache).toEqual({});
    });

    it('preserves message attachments when setting message', () => {
        const {result} = renderHook(() => useChatsStore());

        const attachments = [
            {
                content: [],
                contentType: 'text/plain',
                id: 'file-1',
                name: 'file.txt',
                status: {type: 'complete' as const},
                type: 'file' as const,
            },
        ];

        act(() => {
            result.current.setMessage({attachments, content: 'Message with attachment', role: 'user'});
        });

        expect(result.current.messages[0]).toMatchObject({
            attachments,
            content: 'Message with attachment',
            role: 'user',
        });
    });

    it('handles appending empty string', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'Assistant message', role: 'assistant'});
            result.current.appendToLastAssistantMessage('');
        });

        expect(result.current.messages[0]).toEqual({content: 'Assistant message', role: 'assistant'});
    });

    it('handles setting empty string content', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: 'Old content', role: 'assistant'});
            result.current.setLastAssistantMessageContent('');
        });

        expect(result.current.messages[0]).toEqual({content: '', role: 'assistant'});
    });

    it('maintains store state across multiple operations', () => {
        const {result} = renderHook(() => useChatsStore());

        const conversationId = result.current.conversationId;

        act(() => {
            result.current.setMessage({content: 'User 1', role: 'user'});
            result.current.setMessage({content: 'Assistant 1', role: 'assistant'});
            result.current.appendToLastAssistantMessage(' continued');
            result.current.setMessage({content: 'User 2', role: 'user'});
            result.current.setLastAssistantMessageContent('Assistant 1 continued final');
        });

        expect(result.current.conversationId).toBe(conversationId);
        expect(result.current.messages).toHaveLength(3);
        expect(result.current.messages[1]).toEqual({content: 'Assistant 1 continued final', role: 'assistant'});
    });

    it('handles rapid consecutive appends', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.setMessage({content: '', role: 'assistant'});
            result.current.appendToLastAssistantMessage('Hello');
            result.current.appendToLastAssistantMessage(' ');
            result.current.appendToLastAssistantMessage('world');
            result.current.appendToLastAssistantMessage('!');
        });

        expect(result.current.messages).toHaveLength(1);
        expect(result.current.messages[0]).toEqual({content: 'Hello world!', role: 'assistant'});
    });

    it('preserves messages when switching between chats', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('chat-1');
            result.current.setMessage({content: 'Hello from chat 1', role: 'user'});
            result.current.setMessage({content: 'Reply in chat 1', role: 'assistant'});
        });

        act(() => {
            result.current.switchChat('chat-2');
            result.current.setMessage({content: 'Hello from chat 2', role: 'user'});
        });

        expect(result.current.messages).toHaveLength(1);
        expect(result.current.messages[0]).toEqual({content: 'Hello from chat 2', role: 'user'});

        act(() => {
            result.current.switchChat('chat-1');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[0]).toEqual({content: 'Hello from chat 1', role: 'user'});
        expect(result.current.messages[1]).toEqual({content: 'Reply in chat 1', role: 'assistant'});
    });

    it('preserves conversationId when switching back to a cached chat', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('chat-1');
        });

        const chat1ConversationId = result.current.conversationId;

        act(() => {
            result.current.switchChat('chat-2');
        });

        const chat2ConversationId = result.current.conversationId;

        expect(chat1ConversationId).not.toBe(chat2ConversationId);

        act(() => {
            result.current.switchChat('chat-1');
        });

        expect(result.current.conversationId).toBe(chat1ConversationId);
    });

    it('creates a fresh conversation for a new chat', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('new-chat');
        });

        expect(result.current.messages).toEqual([]);
        expect(result.current.conversationId).toBeDefined();
        expect(result.current.conversationId.length).toBe(32);
        expect(result.current.activeWorkflowExecutionId).toBe('new-chat');
    });

    it('sets activeWorkflowExecutionId on switch', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('chat-abc');
        });

        expect(result.current.activeWorkflowExecutionId).toBe('chat-abc');

        act(() => {
            result.current.switchChat('chat-xyz');
        });

        expect(result.current.activeWorkflowExecutionId).toBe('chat-xyz');
    });

    it('blocks switchChat when isRunning is true', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('chat-1');
            result.current.setMessage({content: 'Hello', role: 'user'});
            result.current.setMessage({content: 'Reply', role: 'assistant'});
        });

        const conversationIdBeforeBlock = result.current.conversationId;

        act(() => {
            result.current.setIsRunning(true);
        });

        act(() => {
            result.current.switchChat('chat-2');
        });

        expect(result.current.activeWorkflowExecutionId).toBe('chat-1');
        expect(result.current.conversationId).toBe(conversationIdBeforeBlock);
        expect(result.current.messages).toHaveLength(2);
        expect(result.current.conversationCache).toEqual({});
    });

    it('allows switchChat after isRunning becomes false', () => {
        const {result} = renderHook(() => useChatsStore());

        act(() => {
            result.current.switchChat('chat-1');
            result.current.setMessage({content: 'Hello', role: 'user'});
            result.current.setIsRunning(true);
        });

        act(() => {
            result.current.switchChat('chat-2');
        });

        expect(result.current.activeWorkflowExecutionId).toBe('chat-1');

        act(() => {
            result.current.setIsRunning(false);
            result.current.switchChat('chat-2');
        });

        expect(result.current.activeWorkflowExecutionId).toBe('chat-2');
        expect(result.current.messages).toEqual([]);
    });
});
