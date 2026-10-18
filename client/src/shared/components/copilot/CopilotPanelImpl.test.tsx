import {TooltipProvider} from '@/components/ui/tooltip';
import {CopilotPanelContent} from '@/shared/components/copilot/CopilotPanelImpl';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter, useNavigate} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({data: {aiDefaultModel: null}, isPending: false}),
}));

const wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter>
        <TooltipProvider>{children}</TooltipProvider>
    </MemoryRouter>
);

const baseContext = {mode: MODE.ASK, parameters: {intent: 'covering'}, source: Source.DATA_TABLE};

describe('CopilotPanelContent closeGlobalPanel', () => {
    beforeEach(() => {
        useCopilotStore.setState({
            composerPlaceholder: 'covering placeholder',
            context: baseContext,
            conversationId: 'covering-conversation',
            conversationStack: [],
            globalPanelConversationToken: null,
            messages: [{content: 'covering turn', role: 'user'}],
        });

        useCopilotPanelStore.setState({copilotPanelOpen: true});
    });

    it('delegates closing to onClose without touching the global panel state', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();

        render(<CopilotPanelContent onClose={onClose} />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        expect(onClose).toHaveBeenCalledTimes(1);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
        expect(useCopilotStore.getState().context).toEqual(baseContext);
    });

    it('pops the stack and restores the covered conversation when a token was pushed', async () => {
        const user = userEvent.setup();

        const token = useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {}, source: Source.PROJECT},
            conversationId: 'global-panel-conversation',
            globalPanelConversationToken: token,
            messages: [{content: 'global panel turn', role: 'user'}],
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        const state = useCopilotStore.getState();

        expect(state.conversationStack).toHaveLength(0);
        expect(state.conversationId).toBe('covering-conversation');
        expect(state.context).toEqual(baseContext);
        expect(state.composerPlaceholder).toBe('covering placeholder');
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('falls back to the hardcoded reset when nothing was pushed for this open, even with an unrelated entry on the stack', async () => {
        const user = userEvent.setup();

        useCopilotStore.getState().saveConversationState();

        useCopilotStore.setState({
            context: {mode: MODE.ASK, parameters: {workflowId: '7'}, source: Source.DATA_TABLE},
            globalPanelConversationToken: null,
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        const state = useCopilotStore.getState();

        expect(state.context).toEqual({mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR});
        expect(state.composerPlaceholder).toBeUndefined();
        expect(state.conversationStack).toHaveLength(1);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('keeps a workflow editor conversation in Build when closing without a pushed token', async () => {
        const user = userEvent.setup();

        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            globalPanelConversationToken: null,
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        const state = useCopilotStore.getState();

        expect(state.context).toEqual({mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR});
        expect(state.messages).toEqual([{content: 'covering turn', role: 'user'}]);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('returns a Skills Build conversation to Ask when closing without a pushed token', async () => {
        const user = userEvent.setup();

        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.SKILLS},
            globalPanelConversationToken: null,
        });

        render(<CopilotPanelContent />, {wrapper});

        await user.click(screen.getByLabelText('Close Copilot panel'));

        expect(useCopilotStore.getState().context).toEqual({
            mode: MODE.ASK,
            parameters: {},
            source: Source.WORKFLOW_EDITOR,
        });
    });

    it('returns to Ask when the user switches to another workflow', async () => {
        const user = userEvent.setup();

        useCopilotStore.setState({
            context: {mode: MODE.BUILD, parameters: {}, source: Source.WORKFLOW_EDITOR},
            globalPanelConversationToken: null,
        });

        const SwitchWorkflowButton = () => {
            const navigate = useNavigate();

            return <button onClick={() => navigate('/projects/1/project-workflows/3')}>Switch workflow</button>;
        };

        render(
            <MemoryRouter initialEntries={['/projects/1/project-workflows/2']}>
                <TooltipProvider>
                    <SwitchWorkflowButton />

                    <CopilotPanelContent />
                </TooltipProvider>
            </MemoryRouter>
        );

        await user.click(screen.getByText('Switch workflow'));

        const state = useCopilotStore.getState();

        expect(state.context.mode).toBe(MODE.ASK);
        expect(state.messages).toEqual([]);
    });
});

describe('CopilotPanelContent header', () => {
    beforeEach(() => {
        useCopilotPanelStore.setState({compactHeader: false, copilotPanelOpen: true});
    });

    const header = () => screen.getByRole('heading', {name: 'AI Copilot'}).parentElement?.parentElement;

    it('keeps the page header padding by default', () => {
        render(<CopilotPanelContent />, {wrapper});

        expect(header()).toHaveClass('p-4');
    });

    it('lines up with a workflow editor toolbar when the page asks for a compact header', () => {
        useCopilotPanelStore.setState({compactHeader: true});

        render(<CopilotPanelContent />, {wrapper});

        expect(header()).toHaveClass('px-4', 'py-2.5');
        expect(header()).not.toHaveClass('p-4');
    });
});
