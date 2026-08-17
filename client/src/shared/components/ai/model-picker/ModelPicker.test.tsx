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

    it('shows the raw model id as secondary text under the model label', async () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));
        await userEvent.click(screen.getByText('Open AI'));

        // The id is what API configurations reference, so a labelled item must keep it visible.
        expect(screen.getByText('GPT-4o')).toBeInTheDocument();
        expect(screen.getByText('gpt-4o')).toBeInTheDocument();
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

    it('renders the Agents cascade rows and fires onSelectAgentChat with the row payload', async () => {
        const onSelectAgentChat = vi.fn();

        render(
            <ModelPicker
                agentChats={[
                    {label: 'Support Agent', projectDeploymentId: '42', workflowExecutionId: 'exec-1'},
                    {label: 'Billing Agent', projectDeploymentId: '43', workflowExecutionId: 'exec-2'},
                ]}
                environment={1}
                onChange={vi.fn()}
                onSelectAgentChat={onSelectAgentChat}
                selectedModel={null}
                selectedProvider={null}
            />
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));
        await userEvent.click(screen.getByText('Agents'));

        expect(await screen.findByText('Support Agent')).toBeInTheDocument();
        expect(screen.getByText('Billing Agent')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Support Agent'));

        expect(onSelectAgentChat).toHaveBeenCalledWith('exec-1', '42', 'Support Agent');
    });

    it('keeps the Agents cascade with an explanatory empty state when there are no agent chats', async () => {
        render(
            <ModelPicker
                agentChats={[]}
                environment={1}
                onChange={vi.fn()}
                onSelectAgentChat={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
            />
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        expect(screen.getByText('Agents')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Agents'));

        expect(await screen.findByText('No agent with an enabled deployment.')).toBeInTheDocument();
    });

    it('renders Agents ahead of Workflows, above the provider list', async () => {
        render(
            <ModelPicker
                agentChats={[{label: 'Support Agent', projectDeploymentId: '42', workflowExecutionId: 'exec-1'}]}
                environment={1}
                onChange={vi.fn()}
                onSelectAgentChat={vi.fn()}
                onSelectWorkflowChat={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
                workflowChats={[{label: 'Project — Flow', projectDeploymentId: '7', workflowExecutionId: 'exec-9'}]}
            />
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        const agents = screen.getByText('Agents');
        const workflows = screen.getByText('Workflows');

        expect(agents).toBeInTheDocument();
        expect(workflows).toBeInTheDocument();
        // Node.compareDocumentPosition: DOCUMENT_POSITION_FOLLOWING (4) means Workflows comes after Agents.
        expect(agents.compareDocumentPosition(workflows) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    });

    it('does not render a Tasks cascade', async () => {
        render(
            <ModelPicker
                agentChats={[]}
                environment={1}
                onChange={vi.fn()}
                onSelectAgentChat={vi.fn()}
                onSelectWorkflowChat={vi.fn()}
                selectedModel={null}
                selectedProvider={null}
                workflowChats={[]}
            />
        );

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));

        expect(screen.queryByText('Tasks')).not.toBeInTheDocument();
    });

    it('navigates to AI Providers settings for an inactive provider', async () => {
        render(<ModelPicker environment={1} onChange={vi.fn()} selectedModel={null} selectedProvider={null} />);

        await userEvent.click(screen.getByLabelText('Select LLM provider and model'));
        await userEvent.click(screen.getByText('Anthropic'));
        await userEvent.click(screen.getByText('Configure credentials'));

        expect(navigateMock).toHaveBeenCalledWith('/automation/settings/ai-providers');
    });
});
