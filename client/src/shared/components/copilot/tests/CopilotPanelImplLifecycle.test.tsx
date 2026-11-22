import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {readLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import CopilotPanel, {CopilotPanelContent} from '@/shared/components/copilot/CopilotPanelImpl';
import useCopilotPanelStore from '@/shared/components/copilot/stores/useCopilotPanelStore';
import {MODE, Source, useCopilotStore} from '@/shared/components/copilot/stores/useCopilotStore';
import {AI_PROVIDERS_SETTINGS_PATH} from '@/shared/navigation/settingsPaths';
import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter, Route, Routes, useNavigate} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    defaultModel: null as {model: string; provider: string} | null,
    isPending: false,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useAiDefaultModelQuery: () => ({data: {aiDefaultModel: hoisted.defaultModel}, isPending: hoisted.isPending}),
}));

vi.mock('@/shared/components/copilot/runtime-providers/CopilotRuntimeProvider', () => ({
    CopilotRuntimeProvider: ({children}: {children: ReactNode}) => <>{children}</>,
}));

vi.mock('@/components/assistant-ui/thread', () => ({
    Thread: ({
        composerActions,
        composerPlaceholder,
        leadingComposerActions,
    }: {
        composerActions?: ReactNode;
        composerPlaceholder?: string;
        leadingComposerActions?: ReactNode;
    }) => (
        <div data-testid="thread">
            <span>{composerPlaceholder}</span>

            {leadingComposerActions}

            {composerActions}
        </div>
    ),
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: ({
        onChange,
        selectedModel,
        selectedProvider,
    }: {
        onChange: (provider: string, model: string) => void;
        selectedModel: string | null;
        selectedProvider: string | null;
    }) => (
        <button onClick={() => onChange('ai.provider.openAi', 'gpt-test')}>
            {`model ${selectedProvider ?? 'none'}/${selectedModel ?? 'none'}`}
        </button>
    ),
}));

const NavigateButton = ({to}: {to: string}) => {
    const navigate = useNavigate();

    return <button onClick={() => navigate(to)}>{`go ${to}`}</button>;
};

const renderInRouter = (ui: ReactNode, initialPath = '/workflow') =>
    render(
        <MemoryRouter initialEntries={[initialPath]}>
            <TooltipProvider>
                <Routes>
                    <Route
                        element={
                            <>
                                {ui}

                                <NavigateButton to="/other" />
                            </>
                        }
                        path="*"
                    />

                    <Route element={<div>AI providers settings</div>} path={AI_PROVIDERS_SETTINGS_PATH} />
                </Routes>
            </TooltipProvider>
        </MemoryRouter>
    );

describe('CopilotPanelImpl', () => {
    beforeEach(() => {
        hoisted.defaultModel = {model: 'claude-test', provider: 'ai.provider.anthropic'};
        hoisted.isPending = false;

        localStorage.clear();

        useWorkspaceStore.setState({currentWorkspaceId: 1049});

        useCopilotStore.setState({
            composerPlaceholder: 'Ask the Copilot',
            context: {mode: MODE.ASK, parameters: {}, source: Source.WORKFLOW_EDITOR},
            conversationId: 'conversation-1',
            conversationStack: [],
            globalPanelConversationToken: null,
            messages: [{content: 'first turn', role: 'user'}],
            selectedLlmModel: undefined,
            selectedLlmProvider: undefined,
        });

        useCopilotPanelStore.setState({compactHeader: false, copilotPanelOpen: true});
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('sends the user to the AI providers settings when no provider is enabled', async () => {
        const user = userEvent.setup();

        hoisted.defaultModel = null;

        renderInRouter(<CopilotPanelContent />);

        expect(screen.getByText('No AI providers enabled')).toBeInTheDocument();
        expect(screen.queryByTestId('thread')).not.toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Go to AI Providers'}));

        expect(screen.getByText('AI providers settings')).toBeInTheDocument();
    });

    it('renders the thread with the composer placeholder once a provider is enabled', () => {
        renderInRouter(<CopilotPanelContent />);

        expect(screen.getByTestId('thread')).toBeInTheDocument();
        expect(screen.getByText('Ask the Copilot')).toBeInTheDocument();
        expect(screen.queryByText('No AI providers enabled')).not.toBeInTheDocument();
    });

    it('switches the global panel between Ask and Build mode', async () => {
        const user = userEvent.setup();

        renderInRouter(<CopilotPanelContent />);

        await user.click(screen.getByRole('switch', {name: 'Build mode off (Ask)'}));

        expect(useCopilotStore.getState().context?.mode).toBe(MODE.BUILD);
    });

    it('hides the mode switch on a surface that owns its own mode', () => {
        renderInRouter(<CopilotPanelContent source={Source.DATA_TABLE} />);

        expect(screen.queryByRole('switch')).not.toBeInTheDocument();
    });

    it('seeds the model picker from the last used model and remembers a new pick', async () => {
        const user = userEvent.setup();

        localStorage.setItem(
            'bytechef.modelPicker.lastUsed.1049',
            JSON.stringify({model: 'gpt-previous', provider: 'ai.provider.openAi'})
        );

        renderInRouter(<CopilotPanelContent />);

        await user.click(screen.getByRole('button', {name: 'model ai.provider.openAi/gpt-previous'}));

        expect(readLastUsedModel(1049)).toEqual({model: 'gpt-test', provider: 'ai.provider.openAi'});
        expect(useCopilotStore.getState().selectedLlmModel).toBe('gpt-test');
        expect(useCopilotStore.getState().selectedLlmProvider).toBe('ai.provider.openAi');
    });

    it('cleans the messages and starts a new conversation', async () => {
        const user = userEvent.setup();

        renderInRouter(<CopilotPanelContent />);

        const buttons = screen.getAllByRole('button');

        await user.click(buttons.find((button) => button.getAttribute('aria-label') == null && !button.textContent)!);

        expect(useCopilotStore.getState().messages).toHaveLength(0);
        expect(useCopilotStore.getState().conversationId).not.toBe('conversation-1');
    });

    it('resets the conversation and closes the global panel when the route changes', async () => {
        const user = userEvent.setup();

        renderInRouter(<CopilotPanelContent />);

        await user.click(screen.getByRole('button', {name: 'go /other'}));

        expect(useCopilotStore.getState().messages).toHaveLength(0);
        expect(useCopilotStore.getState().conversationId).not.toBe('conversation-1');
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(false);
    });

    it('keeps a panel that has its own onClose open when the route changes', async () => {
        const user = userEvent.setup();

        renderInRouter(<CopilotPanelContent onClose={vi.fn()} />);

        await user.click(screen.getByRole('button', {name: 'go /other'}));

        expect(useCopilotStore.getState().messages).toHaveLength(0);
        expect(useCopilotPanelStore.getState().copilotPanelOpen).toBe(true);
    });

    it('slides an overlay panel in when it opens and unmounts it after closing', () => {
        vi.useFakeTimers({toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame']});

        const {rerender} = render(
            <MemoryRouter>
                <TooltipProvider>
                    <CopilotPanel className="fixed right-0" open={false} />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(screen.queryByRole('heading', {name: 'AI Copilot'})).not.toBeInTheDocument();

        rerender(
            <MemoryRouter>
                <TooltipProvider>
                    <CopilotPanel className="fixed right-0" open />
                </TooltipProvider>
            </MemoryRouter>
        );

        const panel = () => screen.getByRole('heading', {name: 'AI Copilot'}).closest('.fixed');

        expect(panel()).toHaveClass('translate-x-full');

        act(() => {
            vi.advanceTimersToNextFrame();
            vi.advanceTimersToNextFrame();
        });

        expect(panel()).toHaveClass('translate-x-0');

        rerender(
            <MemoryRouter>
                <TooltipProvider>
                    <CopilotPanel className="fixed right-0" open={false} />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(panel()).toHaveClass('translate-x-full');

        act(() => {
            vi.advanceTimersByTime(300);
        });

        expect(screen.queryByRole('heading', {name: 'AI Copilot'})).not.toBeInTheDocument();
    });

    it('widens an inline panel when it opens', () => {
        vi.useFakeTimers({toFake: ['setTimeout', 'clearTimeout', 'requestAnimationFrame', 'cancelAnimationFrame']});

        const {container} = render(
            <MemoryRouter>
                <TooltipProvider>
                    <CopilotPanel open />
                </TooltipProvider>
            </MemoryRouter>
        );

        expect(container.firstElementChild).toHaveClass('w-[450px]');
        expect(screen.getByRole('heading', {name: 'AI Copilot'})).toBeInTheDocument();
    });
});
