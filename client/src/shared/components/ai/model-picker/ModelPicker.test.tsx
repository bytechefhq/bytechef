import {render, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ModelPicker from './ModelPicker';

const {navigateMock, useAiProviderCatalogQueryMock} = vi.hoisted(() => ({
    navigateMock: vi.fn(),
    useAiProviderCatalogQueryMock: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiProviderCatalogQuery: useAiProviderCatalogQueryMock,
}));

const catalog = [
    {
        enabled: true,
        icon: '<svg>openai</svg>',
        key: 'ai.provider.openAi',
        models: [{label: 'GPT-4o', name: 'gpt-4o'}],
        name: 'Open AI',
        supportsModelById: false,
    },
    {
        enabled: false,
        icon: '<svg>anthropic</svg>',
        key: 'ai.provider.anthropic',
        models: [],
        name: 'Anthropic',
        supportsModelById: true,
    },
];

describe('ModelPicker', () => {
    beforeEach(() => {
        windowResizeObserver();
        navigateMock.mockReset();
        useAiProviderCatalogQueryMock.mockReturnValue({data: {aiProviderCatalog: catalog}});
        localStorage.clear();
    });

    it('lists active and inactive providers', async () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        expect(screen.getByText('Open AI')).toBeInTheDocument();
        expect(screen.getByText('Anthropic')).toBeInTheDocument();
    });

    it('shows the configured default model in the trigger when nothing is selected', () => {
        render(
            <ModelPicker
                defaultModel="gpt-4o"
                defaultProvider="ai.provider.openAi"
                environment={1}
                onChange={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
            />
        );

        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.queryByText('Select model')).not.toBeInTheDocument();
    });

    it('falls back to "Select model" when no default and no selection', () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        expect(screen.getByText('Select model')).toBeInTheDocument();
    });

    it('fires the catalog query on the Development environment (id 0)', () => {
        // Environments are 0-based (DEVELOPMENT=0); the query must fire for id 0, not only for ids > 0.
        render(<ModelPicker environment={0} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        expect(useAiProviderCatalogQueryMock).toHaveBeenCalledWith(
            {environment: '0'},
            expect.objectContaining({enabled: true})
        );
    });

    it('shows exact selected provider+model in the trigger, not a default label', () => {
        render(
            <ModelPicker
                environment={1}
                onChange={vi.fn()}
                selectedModel="gpt-4o"
                selectedProvider="ai.provider.openAi"
            />
        );

        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.queryByText('Workspace default')).not.toBeInTheDocument();
    });

    it('shows the workspace-default sentinel and clears selection to (null, null) on click', async () => {
        const onChange = vi.fn();

        render(
            <ModelPicker
                environment={1}
                onChange={onChange}
                selectedModel={null}
                selectedProvider={null}
                workspaceDefaultLabel="Workspace default"
            />
        );

        expect(screen.getByLabelText('Select LLM provider and model')).toHaveTextContent('Workspace default');

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        await userEvent.click(screen.getByText('Use workspace default'));

        expect(onChange).toHaveBeenCalledWith(null, null);
    });

    it('renders the Personal agents section when personalAgents and onSelectPersonalAgent are passed', async () => {
        render(
            <ModelPicker
                environment={1}
                onChange={vi.fn()}
                onSelectPersonalAgent={vi.fn()}
                personalAgents={[{id: 1, name: 'A', title: 'Agent A'}]}
                selectedModel={null}
                selectedProvider={null}
            />
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        expect(screen.getByText('Personal agents')).toBeInTheDocument();
    });

    it('navigates to AI Providers settings for an inactive provider', async () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));
        await userEvent.click(screen.getByText('Anthropic'));
        await userEvent.click(screen.getByText('Configure credentials'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai-providers');
    });
});
