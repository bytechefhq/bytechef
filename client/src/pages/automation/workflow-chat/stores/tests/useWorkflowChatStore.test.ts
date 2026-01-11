import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {useWorkflowChatStore} from '../useWorkflowChatStore';

describe('useWorkflowChatStore', () => {
    beforeEach(() => {
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.reset();
        });
    });

    it('initializes with empty messages and a conversation ID', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        expect(result.current.messages).toEqual([]);
        expect(result.current.conversationId).toBeDefined();
        expect(typeof result.current.conversationId).toBe('string');
        expect(result.current.conversationId.length).toBe(32);
    });

    it('generates unique conversation IDs', () => {
        const {result: result1} = renderHook(() => useWorkflowChatStore());
        const id1 = result1.current.conversationId;

        act(() => {
            result1.current.reset();
        });

        const id2 = result1.current.conversationId;

        expect(id1).not.toBe(id2);
    });

    it('adds a message to the messages array', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'Hello', role: 'user'});
        });

        expect(result.current.messages).toHaveLength(1);
        expect(result.current.messages[0]).toEqual({content: 'Hello', role: 'user'});
    });

    it('adds multiple messages in order', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

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
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setMessage({content: 'Assistant ', role: 'assistant'});
            result.current.appendToLastAssistantMessage('response');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'Assistant response', role: 'assistant'});
    });

    it('creates a new assistant message if none exists when appending', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.appendToLastAssistantMessage('New assistant message');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'New assistant message', role: 'assistant'});
    });

    it('appends to the last assistant message when multiple exist', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

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
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setMessage({content: 'Old content', role: 'assistant'});
            result.current.setLastAssistantMessageContent('New content');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'New content', role: 'assistant'});
    });

    it('creates a new assistant message if none exists when setting content', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'User message', role: 'user'});
            result.current.setLastAssistantMessageContent('Assistant content');
        });

        expect(result.current.messages).toHaveLength(2);
        expect(result.current.messages[1]).toEqual({content: 'Assistant content', role: 'assistant'});
    });

    it('resets messages to empty array', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

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

    it('resets conversation ID and messages', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        const originalId = result.current.conversationId;

        act(() => {
            result.current.setMessage({content: 'Message 1', role: 'user'});
            result.current.setMessage({content: 'Message 2', role: 'assistant'});
        });

        act(() => {
            result.current.reset();
        });

        expect(result.current.messages).toEqual([]);
        expect(result.current.conversationId).not.toBe(originalId);
        expect(result.current.conversationId.length).toBe(32);
    });

    it('preserves message attachments when setting message', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

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
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'Assistant message', role: 'assistant'});
            result.current.appendToLastAssistantMessage('');
        });

        expect(result.current.messages[0]).toEqual({content: 'Assistant message', role: 'assistant'});
    });

    it('handles setting empty string content', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

        act(() => {
            result.current.setMessage({content: 'Old content', role: 'assistant'});
            result.current.setLastAssistantMessageContent('');
        });

        expect(result.current.messages[0]).toEqual({content: '', role: 'assistant'});
    });

    it('maintains store state across multiple operations', () => {
        const {result} = renderHook(() => useWorkflowChatStore());

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
        const {result} = renderHook(() => useWorkflowChatStore());

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
});
