import {TooltipProvider} from '@/components/Tooltip/Tooltip';
import AiHubPanel from '@/ee/pages/automation/ai-hub/AiHubPanel';
import {AiHubChatI} from '@/ee/pages/automation/ai-hub/chats/api/chats.api';
import {aiHubChatsStore} from '@/ee/pages/automation/ai-hub/chats/stores/useAiHubChatsStore';
import useAiHubSettingsStore from '@/ee/pages/automation/ai-hub/stores/useAiHubSettingsStore';
import {aiHubTabsStore} from '@/ee/pages/automation/ai-hub/stores/useAiHubTabsStore';
import {writeLastUsedModel} from '@/shared/components/ai/model-picker/lastUsedModel';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const {
    artifactsCardStateRef,
    launcherSelectMock,
    mockArchivedChatsDataRef,
    mockChatsDataRef,
    modelPickerPropsRef,
    threadPropsRef,
} = vi.hoisted(() => ({
    artifactsCardStateRef: {current: {available: false, visible: false}},
    launcherSelectMock: vi.fn(),
    mockArchivedChatsDataRef: {current: [] as AiHubChatI[]},
    mockChatsDataRef: {current: [] as AiHubChatI[]},
    modelPickerPropsRef: {
        current: undefined as
            | {
                  onSelectWorkflowChat?: unknown;
                  selectedModel: string | null;
                  selectedProvider: string | null;
                  workflowChats?: unknown;
              }
            | undefined,
    },
    threadPropsRef: {current: undefined as {showSuggestions?: boolean} | undefined},
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubThread', () => ({
    default: (props: {showSuggestions?: boolean}) => {
        threadPropsRef.current = props;

        return <div data-testid="ai-hub-thread" />;
    },
}));

vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: ({modelPicker}: {modelPicker?: ReactNode}) => <div data-testid="ai-hub-composer">{modelPicker}</div>,
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/useAiHubArtifactsCard', () => ({
    default: () => ({artifacts: [], ...artifactsCardStateRef.current, workspaceId: 1}),
}));

vi.mock('@/ee/pages/automation/ai-hub/hooks/useAiHubChatLaunchers', () => ({
    default: () => ({
        onSelectWorkflowChat: launcherSelectMock,
        workflowChats: [{label: 'Acme — Support', projectDeploymentId: '5', workflowExecutionId: 'exec-1'}],
    }),
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: (props: {
        onSelectWorkflowChat?: unknown;
        selectedModel: string | null;
        selectedProvider: string | null;
        workflowChats?: unknown;
    }) => {
        modelPickerPropsRef.current = props;

        return <div data-testid="model-picker" />;
    },
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: (_workspaceId: number, _environmentId: number, status: 'ACTIVE' | 'ARCHIVED') => ({
        data: status === 'ARCHIVED' ? mockArchivedChatsDataRef.current : mockChatsDataRef.current,
        isLoading: false,
    }),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiDefaultModelQuery: () => ({data: undefined}),
    useAiProviderCatalogQuery: () => ({
        data: {
            aiProviderCatalog: [
                {
                    enabled: true,
                    icon: null,
                    key: 'ai.provider.anthropic',
                    models: [{label: 'Claude Sonnet 4.6', name: 'claude-sonnet-4-6'}],
                    name: 'Anthropic',
                    supportsModelById: false,
                },
            ],
        },
    }),
    useCancelAiHubRunMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 1}),
}));

function buildChat(overrides: Partial<AiHubChatI> = {}): AiHubChatI {
    return {
        autoTitled: true,
        createdAt: new Date().toISOString(),
        environmentId: 1,
        id: 1,
        isOwner: true,
        kind: 'STANDARD',
        lastPreview: null,
        messageCount: 0,
        ownerUserId: 1,
        projectDeploymentId: null,
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: null,
        updatedAt: new Date().toISOString(),
        userId: 1,
        workflowExecutionId: null,
        workspaceId: 1,
        ...overrides,
    };
}

const renderPanel = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <AiHubPanel />
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    aiHubTabsStore.setState({rightPanelOpen: false});
});

afterEach(() => {
    mockArchivedChatsDataRef.current = [];
    mockChatsDataRef.current = [];
    artifactsCardStateRef.current = {available: false, visible: false};
    useAiHubSettingsStore.setState({artifactsCardHidden: false});
    aiHubChatsStore.getState().reset();
});

describe('AiHubPanel', () => {
    it('renders the New Chat fallback title, the thread and the composer', () => {
        const {container} = renderPanel();

        expect(screen.getByText('New Chat')).toBeInTheDocument();
        expect(screen.getByTestId('ai-hub-thread')).toBeInTheDocument();
        expect(screen.getByTestId('ai-hub-composer')).toBeInTheDocument();
        expect(container.querySelector('.size-full')).not.toBeNull();
    });

    it('shows the default instead of a remembered model the catalog no longer offers', () => {
        localStorage.clear();

        writeLastUsedModel(1, 'ai.provider.anthropic', 'claude-nonexistent-9');

        aiHubChatsStore.getState().setCurrentChatId(41);

        renderPanel();

        expect(modelPickerPropsRef.current).toMatchObject({selectedModel: null, selectedProvider: null});

        localStorage.clear();
    });

    it("shows the chat's explicit pick as is", () => {
        aiHubChatsStore.getState().setCurrentChatId(41);
        aiHubChatsStore.getState().setChatLlmSelection(41, 'ai.provider.openAi', 'gpt-5');

        renderPanel();

        expect(modelPickerPropsRef.current).toMatchObject({
            selectedModel: 'gpt-5',
            selectedProvider: 'ai.provider.openAi',
        });
    });

    it("renders the chat's title and its model picker", () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Quarterly report'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        renderPanel();

        expect(screen.getByRole('heading', {name: 'Quarterly report'})).toBeInTheDocument();
        expect(screen.getByTestId('model-picker')).toBeInTheDocument();
    });

    it('passes the workflow chat launchers to the model picker', () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Some chat'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        renderPanel();

        expect(modelPickerPropsRef.current).toMatchObject({
            onSelectWorkflowChat: launcherSelectMock,
            workflowChats: [{label: 'Acme — Support', projectDeploymentId: '5', workflowExecutionId: 'exec-1'}],
        });
    });

    it('hides the model picker and the suggestions in a workflow chat and shows the workflow badge', () => {
        mockChatsDataRef.current = [buildChat({id: 41, kind: 'WORKFLOW_CHAT', title: 'Acme — Support'})];
        aiHubChatsStore.getState().setCurrentChatId(41);
        modelPickerPropsRef.current = undefined;

        renderPanel();

        expect(screen.getByTestId('ai-hub-composer')).toBeInTheDocument();
        expect(screen.queryByTestId('model-picker')).toBeNull();
        expect(screen.getByText('Workflow chat')).toBeInTheDocument();
        expect(threadPropsRef.current?.showSuggestions).toBe(false);
    });

    it('shows no workflow badge and keeps the suggestions in a standard chat', () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Some chat'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        renderPanel();

        expect(screen.queryByText('Workflow chat')).toBeNull();
        expect(threadPropsRef.current?.showSuggestions).toBe(true);
    });

    it('opens the resource panel from the header', async () => {
        const user = userEvent.setup();

        renderPanel();

        await user.click(screen.getByRole('button', {name: 'Open resource panel'}));

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(true);
    });

    it('shows the plain right-panel icon on the open-panel control', () => {
        renderPanel();

        const openButton = screen.getByRole('button', {name: 'Open resource panel'});

        expect(openButton.querySelector('svg.lucide-panel-right')).not.toBeNull();
        expect(openButton.querySelector('svg.lucide-panel-right-open')).toBeNull();
    });

    it('hides the open-panel control while the panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        renderPanel();

        expect(screen.queryByRole('button', {name: 'Open resource panel'})).toBeNull();
    });
});

describe('AiHubPanel header', () => {
    it('has no chat options menu for a chat the caller owns', () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Some chat'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        renderPanel();

        expect(screen.getByRole('heading', {name: 'Some chat'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Chat options'})).toBeNull();
    });

    it('shows an open archived chat with its title', () => {
        mockArchivedChatsDataRef.current = [buildChat({id: 43, status: 'ARCHIVED', title: 'Archived chat'})];
        aiHubChatsStore.getState().setCurrentChatId(43);

        renderPanel();

        expect(screen.getByRole('heading', {name: 'Archived chat'})).toBeInTheDocument();
    });
});

describe('AiHubPanel artifacts toggle', () => {
    it('has no artifacts toggle while the chat has no artifacts', () => {
        renderPanel();

        expect(screen.queryByRole('button', {name: /artifacts/})).toBeNull();
    });

    it('hides the visible artifacts card', async () => {
        artifactsCardStateRef.current = {available: true, visible: true};

        const user = userEvent.setup();

        renderPanel();

        const toggle = screen.getByRole('button', {name: 'Hide artifacts'});

        expect(toggle).toHaveAttribute('aria-pressed', 'true');

        await user.click(toggle);

        expect(useAiHubSettingsStore.getState().artifactsCardHidden).toBe(true);
    });

    it('shows the hidden artifacts card', async () => {
        artifactsCardStateRef.current = {available: true, visible: false};
        useAiHubSettingsStore.setState({artifactsCardHidden: true});

        const user = userEvent.setup();

        renderPanel();

        const toggle = screen.getByRole('button', {name: 'Show artifacts'});

        expect(toggle).toHaveAttribute('aria-pressed', 'false');

        await user.click(toggle);

        expect(useAiHubSettingsStore.getState().artifactsCardHidden).toBe(false);
    });
});
