import {TooltipProvider} from '@/components/ui/tooltip';
import AiHubHomePanel from '@/ee/pages/automation/ai-hub/AiHubHomePanel';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import {writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {catalogQueryRef, defaultModelQueryRef, modelPickerPropsRef, navigateMock} = vi.hoisted(() => ({
    catalogQueryRef: {current: {data: undefined} as {data: {aiProviderCatalog: unknown[]} | undefined}},
    defaultModelQueryRef: {
        current: {data: undefined, isPending: false} as {
            data: {aiDefaultModel: {model: string; provider: string} | null} | undefined;
            isPending: boolean;
        },
    },
    modelPickerPropsRef: {
        current: undefined as {selectedModel: string | null; selectedProvider: string | null} | undefined,
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

vi.mock('@/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers', () => ({
    default: () => ({onSelectWorkflowChat: vi.fn(), workflowChats: []}),
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubSuggestionChips', () => ({
    default: () => <div data-testid="suggestion-chips" />,
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: (props: {selectedModel: string | null; selectedProvider: string | null}) => {
        modelPickerPropsRef.current = props;

        return <div data-testid="model-picker" />;
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: () => defaultModelQueryRef.current,
    useAiProviderCatalogQuery: () => catalogQueryRef.current,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

const ANTHROPIC_CATALOG = [
    {
        enabled: true,
        icon: null,
        key: 'ai.provider.anthropic',
        models: [{label: 'Claude Sonnet 4.6', name: 'claude-sonnet-4-6'}],
        name: 'Anthropic',
        supportsModelById: false,
    },
];

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

        localStorage.clear();

        aiHubChatsStore.getState().reset();

        catalogQueryRef.current = {data: {aiProviderCatalog: ANTHROPIC_CATALOG}};
        modelPickerPropsRef.current = undefined;

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

    it('offers the same Open AI Providers action as the Copilot when no provider is enabled', async () => {
        defaultModelQueryRef.current = {data: {aiDefaultModel: null}, isPending: false};

        const user = userEvent.setup();

        renderHome();

        expect(screen.getByText('No AI providers enabled')).toBeInTheDocument();
        expect(screen.queryByTestId('ai-hub-composer')).toBeNull();

        await user.click(screen.getByRole('button', {name: 'Open AI Providers'}));

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai-providers');
    });

    describe('model picker', () => {
        it('shows a remembered model the catalog offers', () => {
            writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

            renderHome();

            expect(modelPickerPropsRef.current).toMatchObject({
                selectedModel: 'claude-sonnet-4-6',
                selectedProvider: 'ai.provider.anthropic',
            });
        });

        it('shows the default instead of a remembered model the catalog no longer offers', () => {
            writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-nonexistent-9');

            renderHome();

            expect(modelPickerPropsRef.current).toMatchObject({selectedModel: null, selectedProvider: null});
        });

        it('shows the default until the catalog has loaded', () => {
            catalogQueryRef.current = {data: undefined};

            writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-sonnet-4-6');

            renderHome();

            expect(modelPickerPropsRef.current).toMatchObject({selectedModel: null, selectedProvider: null});
        });

        it('shows an explicit draft pick as is', () => {
            aiHubChatsStore.getState().setDraftLlmSelection('ai.provider.openAi', 'gpt-5');

            renderHome();

            expect(modelPickerPropsRef.current).toMatchObject({
                selectedModel: 'gpt-5',
                selectedProvider: 'ai.provider.openAi',
            });
        });
    });
});
