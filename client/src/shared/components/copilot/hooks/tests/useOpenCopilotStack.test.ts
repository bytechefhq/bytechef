import useOpenCopilot from '@/shared/components/copilot/hooks/useOpenCopilot';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {renderHook} from '@testing-library/react';
import {act} from 'react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useOpenCopilot conversation stack', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: undefined,
            context: {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'existing',
            conversationStack: [],
            messages: [{content: 'earlier turn', role: 'user'}],
        });
    });

    it('pushes the current conversation before installing the new context', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(1);
        expect(state.conversationStack[0]?.conversationId).toBe('existing');
        expect(state.conversationStack[0]?.messages).toHaveLength(1);
    });

    it('starts the new surface with no messages and a fresh conversation id', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.PROJECT});
        });

        const state = useCopilotStore.getState();

        expect(state.messages).toHaveLength(0);
        expect(state.conversationId).not.toBe('existing');
        expect(state.context.source).toBe(Source.PROJECT);
    });
});
