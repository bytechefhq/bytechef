import {ThreadMessageLike} from '@assistant-ui/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {MODE, Source, useCopilotStore} from './useCopilotStore';

function resetStore() {
    useCopilotStore.getState().resetMessages();
    useCopilotStore.setState({conversationStack: [], globalPanelConversationToken: null});
}

function contentOf(message: ThreadMessageLike | undefined): string {
    return typeof message?.content === 'string' ? message.content : '';
}

describe('useCopilotStore', () => {
    afterEach(resetStore);

    describe('message ids', () => {
        it('should give an added message without an id a stable id', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'u1', role: 'user'});
            store.addMessage({content: 'u2', role: 'user'});

            const {messages} = useCopilotStore.getState();

            expect(messages[0]?.id).toEqual(expect.any(String));
            expect(messages[1]?.id).toEqual(expect.any(String));
            expect(messages[0]?.id).not.toBe(messages[1]?.id);
        });

        it('should keep the id an added message already has', () => {
            useCopilotStore.getState().addMessage({content: 'u1', id: 'user-1', role: 'user'});

            expect(useCopilotStore.getState().messages[0]?.id).toBe('user-1');
        });

        it('should give a streamed assistant message an id and keep it while streaming', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'u1', role: 'user'});
            store.appendToLastAssistantMessage('a');

            const streamedMessageId = useCopilotStore.getState().messages[1]?.id;

            store.appendToLastAssistantMessage('ab');

            expect(streamedMessageId).toEqual(expect.any(String));
            expect(useCopilotStore.getState().messages[1]?.id).toBe(streamedMessageId);
        });

        it('should keep the id of an edited user message', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'u1', id: 'user-1', role: 'user'});
            store.appendToLastAssistantMessage('a1');
            store.editUserMessage(0, 'u1, edited');

            expect(useCopilotStore.getState().messages).toEqual([{content: 'u1, edited', id: 'user-1', role: 'user'}]);
        });
    });

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

    describe('saveConversationState and restoreConversationState', () => {
        it('should restore the saved conversation when the matching token is passed', () => {
            const store = useCopilotStore.getState();

            store.setContext({mode: MODE.BUILD, parameters: {workflowId: 'w1'}, source: Source.WORKFLOW_EDITOR});
            store.setSelectedLlm('openai', 'gpt-4o');
            store.addMessage({content: 'original', role: 'user'});

            const originalConversationId = useCopilotStore.getState().conversationId;

            const token = store.saveConversationState();

            store.resetMessages();
            store.generateConversationId();
            store.setContext({mode: MODE.ASK, parameters: {}, source: Source.MCP_SERVER});
            store.addMessage({content: 'nested', role: 'user'});

            useCopilotStore.getState().restoreConversationState(token);

            const state = useCopilotStore.getState();

            expect(state.conversationStack).toHaveLength(0);
            expect(state.conversationId).toBe(originalConversationId);
            expect(state.context).toEqual({
                mode: MODE.BUILD,
                parameters: {workflowId: 'w1'},
                source: Source.WORKFLOW_EDITOR,
            });
            expect(state.messages.map(contentOf)).toEqual(['original']);
            expect(state.selectedLlmProvider).toBe('openai');
            expect(state.selectedLlmModel).toBe('gpt-4o');
        });

        it('should return a distinct token for every save', () => {
            const store = useCopilotStore.getState();

            const firstToken = store.saveConversationState();
            const secondToken = store.saveConversationState();

            expect(firstToken).not.toBe(secondToken);
            expect(useCopilotStore.getState().conversationStack.map((snapshot) => snapshot.token)).toEqual([
                firstToken,
                secondToken,
            ]);
        });

        it('should ignore a restore whose token is not on top of the stack', () => {
            const store = useCopilotStore.getState();

            const outerToken = store.saveConversationState();

            store.saveConversationState();
            store.addMessage({content: 'inner', role: 'user'});

            useCopilotStore.getState().restoreConversationState(outerToken);

            const state = useCopilotStore.getState();

            expect(state.conversationStack).toHaveLength(2);
            expect(state.messages.map(contentOf)).toEqual(['inner']);
        });

        it('should ignore a restore with a null token', () => {
            const store = useCopilotStore.getState();

            store.saveConversationState();
            store.addMessage({content: 'current', role: 'user'});

            useCopilotStore.getState().restoreConversationState(null);

            const state = useCopilotStore.getState();

            expect(state.conversationStack).toHaveLength(1);
            expect(state.messages.map(contentOf)).toEqual(['current']);
        });

        it('should ignore a restore when nothing was saved', () => {
            const store = useCopilotStore.getState();

            store.addMessage({content: 'current', role: 'user'});

            useCopilotStore.getState().restoreConversationState('missing');

            expect(useCopilotStore.getState().messages.map(contentOf)).toEqual(['current']);
        });

        it('should drop the oldest snapshot and warn once the stack exceeds its depth', () => {
            const consoleWarnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

            const store = useCopilotStore.getState();

            const tokens = Array.from({length: 11}, () => store.saveConversationState());

            const {conversationStack} = useCopilotStore.getState();

            expect(conversationStack).toHaveLength(10);
            expect(conversationStack[0]?.token).toBe(tokens[1]);
            expect(conversationStack[9]?.token).toBe(tokens[10]);
            expect(consoleWarnSpy).toHaveBeenCalledTimes(1);

            consoleWarnSpy.mockRestore();
        });
    });

    describe('generateConversationId', () => {
        it('should return a new conversation to Ask', () => {
            useCopilotStore.setState({
                context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            });

            useCopilotStore.getState().generateConversationId();

            expect(useCopilotStore.getState().context).toEqual({
                mode: MODE.ASK,
                parameters: {},
                source: Source.WORKFLOW_EDITOR,
            });
        });
    });

    describe('setGlobalPanelConversationToken', () => {
        it('should store and clear the global panel conversation token', () => {
            useCopilotStore.getState().setGlobalPanelConversationToken('token-1');

            expect(useCopilotStore.getState().globalPanelConversationToken).toBe('token-1');

            useCopilotStore.getState().setGlobalPanelConversationToken(null);

            expect(useCopilotStore.getState().globalPanelConversationToken).toBeNull();
        });
    });
});
