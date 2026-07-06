import {ThreadMessageLike} from '@assistant-ui/react';
import {afterEach, describe, expect, it} from 'vitest';

import {useCopilotStore} from './useCopilotStore';

function resetStore() {
    useCopilotStore.getState().resetMessages();
}

function contentOf(message: ThreadMessageLike | undefined): string {
    return typeof message?.content === 'string' ? message.content : '';
}

describe('useCopilotStore', () => {
    afterEach(resetStore);

    describe('appendToLastAssistantMessage', () => {
        it('should create a new assistant message when the turn has none yet', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'u1', role: 'user'});
            store.appendToLastAssistantMessage('a1');

            const {messages} = useCopilotStore.getState();

            expect(messages).toHaveLength(2);
            expect(messages[1]?.role).toBe('assistant');
            expect(contentOf(messages[1])).toBe('a1');
        });

        it('should replace the current turn assistant message while streaming', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'u1', role: 'user'});
            store.appendToLastAssistantMessage('a');
            store.appendToLastAssistantMessage('ab');
            store.appendToLastAssistantMessage('abc');

            const {messages} = useCopilotStore.getState();

            expect(messages).toHaveLength(2);
            expect(contentOf(messages[1])).toBe('abc');
        });

        it('should keep messages in back-and-forth order across multiple turns (#5348)', () => {
            const store = useCopilotStore.getState();

            // Turn 1
            store.addMessage({content: 'u1', role: 'user'});
            store.appendToLastAssistantMessage('a1');

            // Turn 2
            store.addMessage({content: 'u2', role: 'user'});
            store.appendToLastAssistantMessage('a2');

            // Turn 3
            store.addMessage({content: 'u3', role: 'user'});
            store.appendToLastAssistantMessage('a3');

            const {messages} = useCopilotStore.getState();

            expect(messages.map((message) => message.role)).toEqual([
                'user',
                'assistant',
                'user',
                'assistant',
                'user',
                'assistant',
            ]);
            expect(messages.map(contentOf)).toEqual(['u1', 'a1', 'u2', 'a2', 'u3', 'a3']);
        });

        it('should not overwrite a previous turn assistant message that carries array content', () => {
            const store = useCopilotStore.getState();

            // Turn 1 produced a tool-result style assistant message (array content).
            store.addMessage({content: 'u1', role: 'user'});
            store.addMessage({content: [{data: {message: 'done'}, type: 'data-run-error'}], role: 'assistant'});

            // Turn 2 streams plain text — it must land in a fresh assistant message, not the array one.
            store.addMessage({content: 'u2', role: 'user'});
            store.appendToLastAssistantMessage('a2');

            const {messages} = useCopilotStore.getState();

            expect(messages).toHaveLength(4);
            expect(messages.map((message) => message.role)).toEqual(['user', 'assistant', 'user', 'assistant']);
            expect(contentOf(messages[3])).toBe('a2');
        });
    });
});
