import {ThreadMessageLike} from '@assistant-ui/react';
import {beforeEach, describe, expect, it} from 'vitest';

import {MODE, aiHubStore} from '../useAiHubStore';

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

        expect(messages[0]).toEqual(priorAssistant);
        expect(messages[1]).toEqual(userMessage);
        expect(messages[2]).toEqual(artifactCard);
        expect(messages[3]).toEqual({content: 'Resumed reply', role: 'assistant'});
    });
});

describe('aiHubStore', () => {
    beforeEach(() => {
        aiHubStore.setState({messages: [], messagesLoading: false, mode: MODE.BUILD});
    });

    it('defaults to BUILD mode and switches mode', () => {
        expect(aiHubStore.getState().mode).toBe(MODE.BUILD);

        aiHubStore.getState().setMode(MODE.ASK);

        expect(aiHubStore.getState().mode).toBe(MODE.ASK);
    });

    it('generates a fresh chat id', () => {
        const previousChatId = aiHubStore.getState().chatId;

        aiHubStore.getState().generateChatId();

        expect(aiHubStore.getState().chatId).toBeTruthy();
        expect(aiHubStore.getState().chatId).not.toBe(previousChatId);
    });

    it('editUserMessage replaces the user message and truncates everything after it', () => {
        aiHubStore.setState({
            messages: [
                {content: 'first', role: 'user'},
                {content: 'reply', role: 'assistant'},
                {content: 'second', role: 'user'},
            ],
        });

        aiHubStore.getState().editUserMessage(0, 'edited');

        expect(aiHubStore.getState().messages).toEqual([{content: 'edited', role: 'user'}]);
    });

    it('editUserMessage ignores an index that does not point at a user message', () => {
        const messages: ThreadMessageLike[] = [
            {content: 'first', role: 'user'},
            {content: 'reply', role: 'assistant'},
        ];

        aiHubStore.setState({messages});

        aiHubStore.getState().editUserMessage(1, 'edited');
        aiHubStore.getState().editUserMessage(5, 'edited');

        expect(aiHubStore.getState().messages).toBe(messages);
    });

    it('resetMessages clears messages and the loading flag', () => {
        aiHubStore.setState({messages: [{content: 'hi', role: 'user'}], messagesLoading: true});

        aiHubStore.getState().resetMessages();

        expect(aiHubStore.getState().messages).toEqual([]);
        expect(aiHubStore.getState().messagesLoading).toBe(false);
    });
});
