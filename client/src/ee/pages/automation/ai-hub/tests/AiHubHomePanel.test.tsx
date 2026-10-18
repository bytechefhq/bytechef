import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {defaultModelQueryRef, navigateMock} = vi.hoisted(() => ({
    defaultModelQueryRef: {
        current: {data: undefined, isPending: false} as {
            data: {aiDefaultModel: {model: string; provider: string} | null} | undefined;
            isPending: boolean;
        },
    },
    navigateMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal<typeof import('react-router-dom')>();

    return {...actual, useNavigate: () => navigateMock};
});

vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: ({modelPicker}: {modelPicker?: ReactNode}) => <div data-testid="ai-hub-composer">{modelPicker}</div>,
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips', () => ({
    default: () => <div data-testid="suggestion-chips" />,
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: () => <div data-testid="model-picker" />,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: () => defaultModelQueryRef.current,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const renderHome = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AiHubHomePanel />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('AiHubHomePanel', () => {
    beforeEach(() => {
        navigateMock.mockReset();

        defaultModelQueryRef.current = {
            data: {aiDefaultModel: {model: 'claude', provider: 'anthropic'}},
            isPending: false,
        };
    });

    it('renders the welcome copy, the composer with its model picker, and the suggestions', () => {
        renderHome();

        expect(screen.getByRole('heading', {name: 'What should we get done?'})).toBeInTheDocument();
        expect(screen.getByTestId('ai-hub-composer')).toBeInTheDocument();
        expect(screen.getByTestId('model-picker')).toBeInTheDocument();
        expect(screen.getByTestId('suggestion-chips')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Hide chats'})).toBeInTheDocument();
    });

    it('points to the AI provider settings when no provider is enabled', async () => {
        defaultModelQueryRef.current = {data: {aiDefaultModel: null}, isPending: false};

        const user = userEvent.setup();

        renderHome();

        expect(screen.getByText('No AI providers enabled')).toBeInTheDocument();
        expect(screen.queryByTestId('ai-hub-composer')).toBeNull();

        await user.click(screen.getByRole('button', {name: 'Go to AI Providers'}));

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai-providers');
    });
});
