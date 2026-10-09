import {TooltipProvider} from '@/components/ui/tooltip';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CopilotPanel from './CopilotPanel';

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({data: {aiDefaultModel: null}, isPending: false}),
}));

vi.mock('@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider', () => ({
    CopilotRuntimeProvider: () => null,
}));

const getCloseButton = () => {
    const heading = screen.getByRole('heading', {name: 'AI Copilot'});
    const header = heading.parentElement?.parentElement as HTMLElement;
    const headerButtons = within(header).getAllByRole('button');

    return headerButtons[headerButtons.length - 1] as HTMLElement;
};

const renderCopilotPanel = (onClose?: () => void) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <CopilotPanel onClose={onClose} open />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('CopilotPanel', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({copilotPanelOpen: true});
        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {mcpServerId: 1}, source: Source.MCP_SERVER},
            conversationStack: [],
            globalPanelConversationToken: null,
            messages: [],
        });
    });

    it('should delegate closing to onClose without touching the global panel state', async () => {
        const onClose = vi.fn();

        renderCopilotPanel(onClose);

        await userEvent.click(getCloseButton());

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
        expect(useCopilotStore.getState().context.source).toBe(Source.MCP_SERVER);
    });

    it('should reset the context to the workflow editor when no conversation was saved', async () => {
        renderCopilotPanel();

        await userEvent.click(getCloseButton());

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('should restore the saved conversation and clear the global token when closing', async () => {
        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {workflowId: 'w1'}, source: Source.WORKFLOW_EDITOR},
            messages: [{content: 'saved', role: 'user'}],
        });

        const token = useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {}, source: Source.MCP_SERVER},
            globalPanelConversationToken: token,
            messages: [],
        });

        renderCopilotPanel();

        await userEvent.click(getCloseButton());

        const state = useCopilotStore.getState();

        expect(state.context).toEqual({
            mode: MODE.BUILD,
            parameters: {workflowId: 'w1'},
            source: Source.WORKFLOW_EDITOR,
        });
        expect(state.messages).toEqual([{content: 'saved', role: 'user'}]);
        expect(state.conversationStack).toHaveLength(0);
        expect(state.globalPanelConversationToken).toBeNull();
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });
});
