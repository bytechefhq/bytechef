import {ThreadMessageLike} from '@assistant-ui/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {aiHubStore} from '../useAiHubStore';

describe('aiHubStore.appendToLastAssistantMessage', () => {
    beforeEach(() => {
        aiHubStore.setState({messages: []});
    });

    it('creates a new trailing assistant message when the last message is the user turn', () => {
        aiHubStore.setState({messages: [{content: 'continue', role: 'user'}]});

        aiHubStore.getState().appendToLastAssistantMessage('Streaming reply');

        const {messages} = aiHubStore.getState();

        expect(messages).toHaveLength(2);
        expect(messages[1]).toEqual({content: 'Streaming reply', role: 'assistant'});
    });

    it('extends the trailing string assistant of the current turn', () => {
        aiHubStore.setState({
            messages: [
                {content: 'continue', role: 'user'},
                {content: 'Strea', role: 'assistant'},
            ],
        });

        aiHubStore.getState().appendToLastAssistantMessage('Streaming reply');

        const {messages} = aiHubStore.getState();

        expect(messages).toHaveLength(2);
        expect(messages[1]).toEqual({content: 'Streaming reply', role: 'assistant'});
    });

    it('does NOT overwrite a prior-turn assistant when the trailing message is an artifact-link card', () => {
        const priorAssistant: ThreadMessageLike = {content: 'Earlier answer', role: 'assistant'};
        const userMessage: ThreadMessageLike = {content: 'continue', role: 'user'};
        const artifactCard: ThreadMessageLike = {
            content: [{args: {}, toolCallId: 'a1', toolName: 'openWorkflowTab', type: 'tool-call'}],
            role: 'assistant',
        };

        aiHubStore.setState({messages: [priorAssistant, userMessage, artifactCard]});

        aiHubStore.getState().appendToLastAssistantMessage('Resumed reply');

        const {messages} = aiHubStore.getState();

        // The prior-turn assistant is untouched; the resumed reply is appended AFTER the user message + card.
        expect(messages[0]).toEqual(priorAssistant);
        expect(messages[1]).toEqual(userMessage);
        expect(messages[2]).toEqual(artifactCard);
        expect(messages[3]).toEqual({content: 'Resumed reply', role: 'assistant'});
    });
});
