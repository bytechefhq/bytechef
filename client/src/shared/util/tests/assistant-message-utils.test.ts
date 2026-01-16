import {ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {appendToLastAssistantMessage, setLastAssistantMessageContent} from '../assistant-message-utils';

describe('assistant-message-utils', () => {
    describe('appendToLastAssistantMessage', () => {
        it('appends content to the last assistant message', () => {
            const messages: ThreadMessageLike[] = [
                {content: 'user message', role: 'user'},
                {content: 'assistant ', role: 'assistant'},
            ];

            const result = appendToLastAssistantMessage(messages, 'message');

            expect(result).toHaveLength(2);
            expect(result[1]).toEqual({content: 'assistant message', role: 'assistant'});
        });

        it('creates a new assistant message if none exists', () => {
            const messages: ThreadMessageLike[] = [{content: 'user message', role: 'user'}];

            const result = appendToLastAssistantMessage(messages, 'new assistant message');

            expect(result).toHaveLength(2);
            expect(result[1]).toEqual({content: 'new assistant message', role: 'assistant'});
        });

        it('appends to the last assistant message when multiple exist', () => {
            const messages: ThreadMessageLike[] = [
                {content: 'first assistant', role: 'assistant'},
                {content: 'user message', role: 'user'},
                {content: 'second assistant', role: 'assistant'},
            ];

            const result = appendToLastAssistantMessage(messages, ' update');

            expect(result).toHaveLength(3);
            expect(result[0]).toEqual({content: 'first assistant', role: 'assistant'});
            expect(result[2]).toEqual({content: 'second assistant update', role: 'assistant'});
        });

        it('handles non-string content by converting to empty string first', () => {
            const messages: ThreadMessageLike[] = [{content: 123 as unknown as string, role: 'assistant'}];

            const result = appendToLastAssistantMessage(messages, ' text');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: ' text', role: 'assistant'});
        });

        it('handles non-string delta by converting to string', () => {
            const messages: ThreadMessageLike[] = [{content: 'text ', role: 'assistant'}];

            const result = appendToLastAssistantMessage(messages, 456 as unknown as string);

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: 'text 456', role: 'assistant'});
        });

        it('does not mutate the original messages array', () => {
            const messages: ThreadMessageLike[] = [{content: 'original', role: 'assistant'}];
            const originalCopy = [...messages];

            appendToLastAssistantMessage(messages, ' appended');

            expect(messages).toEqual(originalCopy);
        });

        it('works with empty messages array', () => {
            const messages: ThreadMessageLike[] = [];

            const result = appendToLastAssistantMessage(messages, 'first message');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: 'first message', role: 'assistant'});
        });

        it('handles empty string content', () => {
            const messages: ThreadMessageLike[] = [{content: '', role: 'assistant'}];

            const result = appendToLastAssistantMessage(messages, 'new content');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: 'new content', role: 'assistant'});
        });

        it('handles empty string delta', () => {
            const messages: ThreadMessageLike[] = [{content: 'existing', role: 'assistant'}];

            const result = appendToLastAssistantMessage(messages, '');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: 'existing', role: 'assistant'});
        });
    });

    describe('setLastAssistantMessageContent', () => {
        it('sets content on the last assistant message', () => {
            const messages: ThreadMessageLike[] = [
                {content: 'user message', role: 'user'},
                {content: 'old content', role: 'assistant'},
            ];

            const result = setLastAssistantMessageContent(messages, 'new content');

            expect(result).toHaveLength(2);
            expect(result[1]).toEqual({content: 'new content', role: 'assistant'});
        });

        it('creates a new assistant message if none exists', () => {
            const messages: ThreadMessageLike[] = [{content: 'user message', role: 'user'}];

            const result = setLastAssistantMessageContent(messages, 'assistant content');

            expect(result).toHaveLength(2);
            expect(result[1]).toEqual({content: 'assistant content', role: 'assistant'});
        });

        it('sets content on the last assistant message when multiple exist', () => {
            const messages: ThreadMessageLike[] = [
                {content: 'first assistant', role: 'assistant'},
                {content: 'user message', role: 'user'},
                {content: 'second assistant', role: 'assistant'},
            ];

            const result = setLastAssistantMessageContent(messages, 'updated content');

            expect(result).toHaveLength(3);
            expect(result[0]).toEqual({content: 'first assistant', role: 'assistant'});
            expect(result[2]).toEqual({content: 'updated content', role: 'assistant'});
        });

        it('does not mutate the original messages array', () => {
            const messages: ThreadMessageLike[] = [{content: 'original', role: 'assistant'}];
            const originalCopy = [...messages];

            setLastAssistantMessageContent(messages, 'new content');

            expect(messages).toEqual(originalCopy);
        });

        it('works with empty messages array', () => {
            const messages: ThreadMessageLike[] = [];

            const result = setLastAssistantMessageContent(messages, 'first message');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: 'first message', role: 'assistant'});
        });

        it('handles empty string content', () => {
            const messages: ThreadMessageLike[] = [{content: 'existing', role: 'assistant'}];

            const result = setLastAssistantMessageContent(messages, '');

            expect(result).toHaveLength(1);
            expect(result[0]).toEqual({content: '', role: 'assistant'});
        });

        it('preserves other message properties', () => {
            const messages: ThreadMessageLike[] = [
                {
                    attachments: [
                        {
                            content: [],
                            contentType: 'text/plain',
                            id: 'file-1',
                            name: 'file.txt',
                            status: {type: 'complete' as const},
                            type: 'file' as const,
                        },
                    ],
                    content: 'old content',
                    createdAt: new Date('2024-01-01'),
                    id: 'msg-123',
                    role: 'assistant',
                },
            ];

            const result = setLastAssistantMessageContent(messages, 'new content');

            expect(result).toHaveLength(1);
            expect(result[0]).toMatchObject({
                attachments: [
                    {
                        content: [],
                        contentType: 'text/plain',
                        id: 'file-1',
                        name: 'file.txt',
                        status: {type: 'complete'},
                        type: 'file',
                    },
                ],
                content: 'new content',
                createdAt: new Date('2024-01-01'),
                id: 'msg-123',
                role: 'assistant',
            });
        });
    });
});
