import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const baseContext = {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR};

describe('copilot conversation stack', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: baseContext,
            conversationId: 'conversation-0',
            conversationStack: [],
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });
    });

    it('restores the most recently saved conversation first', () => {
        const {saveConversationState} = useCopilotStore.getState();

        useCopilotStore.setState({conversationId: 'first', messages: [{content: 'a', role: 'user'}]});
        const firstToken = saveConversationState();

        useCopilotStore.setState({conversationId: 'second', messages: [{content: 'b', role: 'user'}]});
        const secondToken = saveConversationState();

        useCopilotStore.setState({conversationId: 'third', messages: []});

        useCopilotStore.getState().restoreConversationState(secondToken);

        expect(useCopilotStore.getState().conversationId).toBe('second');

        useCopilotStore.getState().restoreConversationState(firstToken);

        expect(useCopilotStore.getState().conversationId).toBe('first');
    });

    it('leaves state untouched when restoring with an empty stack', () => {
        useCopilotStore.setState({conversationId: 'current', messages: [{content: 'keep', role: 'user'}]});

        useCopilotStore.getState().restoreConversationState(null);

        expect(useCopilotStore.getState().conversationId).toBe('current');
        expect(useCopilotStore.getState().messages).toHaveLength(1);
    });

    it('restores every field of the snapshot', () => {
        useCopilotStore.setState({
            composerPlaceholder: 'describe the workflow',
            context: {...baseContext, source: Source.DATA_TABLE},
            conversationId: 'rich',
            messages: [{content: 'x', role: 'user'}],
            selectedLlmModel: 'model-a',
            selectedLlmProvider: 'provider-a',
        });

        const token = useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: baseContext,
            conversationId: 'other',
            messages: [],
            selectedLlmModel: null,
            selectedLlmProvider: null,
        });

        useCopilotStore.getState().restoreConversationState(token);

        const restored = useCopilotStore.getState();

        expect(restored.composerPlaceholder).toBe('describe the workflow');
        expect(restored.context.source).toBe(Source.DATA_TABLE);
        expect(restored.conversationId).toBe('rich');
        expect(restored.messages).toHaveLength(1);
        expect(restored.selectedLlmModel).toBe('model-a');
        expect(restored.selectedLlmProvider).toBe('provider-a');
    });

    it('caps the stack depth and drops the deepest entry', () => {
        const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});

        for (let index = 0; index < 12; index += 1) {
            useCopilotStore.setState({conversationId: `conversation-${index}`});
            useCopilotStore.getState().saveConversationState();
        }

        expect(useCopilotStore.getState().conversationStack).toHaveLength(10);
        expect(useCopilotStore.getState().conversationStack[0]?.conversationId).toBe('conversation-2');
        expect(warnSpy).toHaveBeenCalled();

        warnSpy.mockRestore();
    });

    it('does not carry the stack into a saved snapshot', () => {
        useCopilotStore.getState().saveConversationState();
        useCopilotStore.getState().saveConversationState();

        const [firstEntry] = useCopilotStore.getState().conversationStack;

        expect(firstEntry).not.toHaveProperty('conversationStack');
    });

    describe('token-paired push and pop', () => {
        it('is a no-op and leaves the stack intact when restoring with a null token on a non-empty stack', () => {
            useCopilotStore.setState({conversationId: 'underneath'});
            useCopilotStore.getState().saveConversationState();

            useCopilotStore.setState({conversationId: 'on-top'});

            useCopilotStore.getState().restoreConversationState(null);

            const state = useCopilotStore.getState();

            expect(state.conversationId).toBe('on-top');
            expect(state.conversationStack).toHaveLength(1);
        });

        it('is a no-op when handed undefined, so re-widening the signature cannot restore the escape hatch', () => {
            useCopilotStore.setState({conversationId: 'underneath'});
            useCopilotStore.getState().saveConversationState();

            useCopilotStore.setState({conversationId: 'on-top'});

            useCopilotStore.getState().restoreConversationState(undefined as unknown as string | null);

            const state = useCopilotStore.getState();

            expect(state.conversationId).toBe('on-top');
            expect(state.conversationStack).toHaveLength(1);
        });

        it('is a no-op and leaves the stack intact when the token does not match the top entry', () => {
            useCopilotStore.setState({conversationId: 'underneath'});
            useCopilotStore.getState().saveConversationState();

            useCopilotStore.setState({conversationId: 'on-top'});

            useCopilotStore.getState().restoreConversationState('some-other-surfaces-token');

            const state = useCopilotStore.getState();

            expect(state.conversationId).toBe('on-top');
            expect(state.conversationStack).toHaveLength(1);
        });

        it('pops when the token matches the top entry', () => {
            useCopilotStore.setState({conversationId: 'underneath'});

            const token = useCopilotStore.getState().saveConversationState();

            useCopilotStore.setState({conversationId: 'on-top'});

            useCopilotStore.getState().restoreConversationState(token);

            const state = useCopilotStore.getState();

            expect(state.conversationId).toBe('underneath');
            expect(state.conversationStack).toHaveLength(0);
        });

        it('no-ops on a second restore with the same token once already popped', () => {
            useCopilotStore.setState({conversationId: 'underneath'});

            const token = useCopilotStore.getState().saveConversationState();

            useCopilotStore.setState({conversationId: 'on-top'});
            useCopilotStore.getState().restoreConversationState(token);

            expect(useCopilotStore.getState().conversationId).toBe('underneath');

            useCopilotStore.setState({conversationId: 'unrelated-later-conversation'});
            useCopilotStore.getState().restoreConversationState(token);

            expect(useCopilotStore.getState().conversationId).toBe('unrelated-later-conversation');
        });
    });
});
