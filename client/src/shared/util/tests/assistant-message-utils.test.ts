import {ThreadMessageLike} from '@assistant-ui/react';
import {describe, expect, it} from 'vitest';

import {
    appendToLastAssistantMessage,
    getResumeFailure,
    getResumeStreamRequest,
    hasLastAssistantMessageText,
    setLastAssistantMessageContent,
} from '../assistant-message-utils';

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

    describe('getResumeFailure', () => {
        it.each([
            [null, true],
            [400, false],
            [404, false],
            [406, false],
            [409, true],
            [410, false],
            [422, false],
            [429, true],
            [500, true],
            [502, true],
            [503, true],
            [504, true],
        ])('for status %s returns retryable=%s', (status, retryable) => {
            expect(getResumeFailure(status).retryable).toBe(retryable);
        });

        it('tells the user the workflow failed for a failed job', () => {
            expect(getResumeFailure(422).message).toBe('The workflow failed, so your answer could not be delivered.');
        });

        it('tells the user the question is gone for an expired or answered question', () => {
            expect(getResumeFailure(410).message).toBe('This question has expired or was already answered.');
        });
    });

    describe('hasLastAssistantMessageText', () => {
        it('returns false when there is no assistant message', () => {
            expect(hasLastAssistantMessageText([{content: 'question', role: 'user'}])).toBe(false);
        });

        it('returns false for a blank string assistant message', () => {
            expect(hasLastAssistantMessageText([{content: '  ', role: 'assistant'}])).toBe(false);
        });

        it('returns true for an assistant message with text', () => {
            expect(
                hasLastAssistantMessageText([
                    {content: 'partial answer', role: 'assistant'},
                    {content: 'follow up', role: 'user'},
                ])
            ).toBe(true);
        });

        it('checks the text parts of a content array', () => {
            expect(hasLastAssistantMessageText([{content: [{text: '', type: 'text'}], role: 'assistant'}])).toBe(false);
            expect(hasLastAssistantMessageText([{content: [{text: 'partial', type: 'text'}], role: 'assistant'}])).toBe(
                true
            );
        });
    });

    describe('getResumeStreamRequest', () => {
        it('posts the answer as JSON to the resume URL', () => {
            const request = getResumeStreamRequest('https://example.com/job/resume/abc', 'Blue');

            expect(request).toEqual({
                init: {
                    body: JSON.stringify({message: 'Blue'}),
                    headers: {'Content-Type': 'application/json'},
                    method: 'POST',
                },
                url: 'https://example.com/job/resume/abc',
            });
        });
    });
});
