import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

import useOpenCopilot from './useOpenCopilot';

describe('useOpenCopilot', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({copilotPanelOpen: false});
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {workflowId: 'w1'}, source: Source.WORKFLOW_EDITOR},
            conversationStack: [],
            globalPanelConversationToken: null,
            messages: [{content: 'previous', role: 'user'}],
        });
    });

    it('should save the current conversation, start a fresh one for the source and open the panel', () => {
        const previousConversationId = useCopilotStore.getState().conversationId;

        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({parameters: {mcpServerId: 7}, source: Source.MCP_SERVER});
        });

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(1);
        expect(state.globalPanelConversationToken).toBe(state.conversationStack[0]?.token);
        expect(state.messages).toEqual([]);
        expect(state.conversationId).not.toBe(previousConversationId);
        expect(state.context).toEqual({mode: MODE.ASK, parameters: {mcpServerId: 7}, source: Source.MCP_SERVER});
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });

    it('should default the parameters to an empty object', () => {
        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.SKILLS});
        });

        expect(useCopilotStore.getState().context).toEqual({mode: MODE.ASK, parameters: {}, source: Source.SKILLS});
    });

    it('should not save another conversation when the panel is already open', () => {
        useCopilotPanelStore.setState({copilotPanelOpen: true});
        useCopilotStore.setState({globalPanelConversationToken: 'existing-token'});

        const {result} = renderHook(() => useOpenCopilot());

        act(() => {
            result.current({source: Source.MCP_SERVER});
        });

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(0);
        expect(state.globalPanelConversationToken).toBe('existing-token');
        expect(state.messages).toEqual([]);
        expect(state.context.source).toBe(Source.MCP_SERVER);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });
});
