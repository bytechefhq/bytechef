import {TooltipProvider} from '@/components/ui/tooltip';
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

const {mockArchivedChatsDataRef, mockChatsDataRef, modelPickerPropsRef, unarchiveMutateMock} = vi.hoisted(() => ({
    mockArchivedChatsDataRef: {current: [] as AiHubChatI[]},
    mockChatsDataRef: {current: [] as AiHubChatI[]},
    modelPickerPropsRef: {
        current: undefined as {selectedModel: string | null; selectedProvider: string | null} | undefined,
    },
    unarchiveMutateMock: vi.fn(),
}));

vi.mock('@/ee/pages/automation/ai-hub/messages/AiHubThread', () => ({
    default: () => <div data-testid="ai-hub-thread" />,
}));

vi.mock('@/ee/pages/automation/ai-hub/composer/AiHubChatComposer', () => ({
    default: ({modelPicker}: {modelPicker?: ReactNode}) => <div data-testid="ai-hub-composer">{modelPicker}</div>,
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/AiHubArtifactsCard', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/automation/ai-hub/artifacts/useAiHubArtifactsCard', () => ({
    default: () => ({artifacts: [], visible: false, workspaceId: 1}),
}));

vi.mock('@/shared/components/ai/model-picker/ModelPicker', () => ({
    default: (props: {selectedModel: string | null; selectedProvider: string | null}) => {
        modelPickerPropsRef.current = props;

        return <div data-testid="model-picker" />;
    },
}));

vi.mock('@/ee/pages/automation/ai-hub/chats/hooks/useChats', () => ({
    useAiHubChatsQuery: (_workspaceId: number, _environmentId: number, status: 'ACTIVE' | 'ARCHIVED') => ({
        data: status === 'ARCHIVED' ? mockArchivedChatsDataRef.current : mockChatsDataRef.current,
        isLoading: false,
    }),
    useDeleteAiHubChatMutation: () => ({mutate: vi.fn()}),
    usePatchAiHubChatMutation: () => ({mutate: unarchiveMutateMock}),
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
        lastPreview: null,
        messageCount: 0,
        ownerUserId: 1,
        status: 'ACTIVE',
        threadId: 'thread-1',
        title: null,
        updatedAt: new Date().toISOString(),
        userId: 1,
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
    useAiHubSettingsStore.setState({showToolCalls: false});
});

afterEach(() => {
    mockArchivedChatsDataRef.current = [];
    mockChatsDataRef.current = [];
    unarchiveMutateMock.mockReset();
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

    it('opens the resource panel from the header', async () => {
        const user = userEvent.setup();

        renderPanel();

        await user.click(screen.getByRole('button', {name: 'Open resource panel'}));

        expect(aiHubTabsStore.getState().rightPanelOpen).toBe(true);
    });

    it('hides the open-panel control while the panel is open', () => {
        aiHubTabsStore.setState({rightPanelOpen: true});

        renderPanel();

        expect(screen.queryByRole('button', {name: 'Open resource panel'})).toBeNull();
    });
});

describe('AiHubPanel chat options menu', () => {
    const openMenu = async () => {
        const user = userEvent.setup();

        renderPanel();

        await user.click(screen.getByRole('button', {name: 'Chat options'}));

        return user;
    };

    it('offers rename, archive and delete for a chat the caller owns, and no Share item', async () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Some chat'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        await openMenu();

        expect(await screen.findByRole('menuitem', {name: 'Rename'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Archive'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: /Share/})).toBeNull();
    });

    it('shows an open archived chat with its title and offers unarchive, rename and delete', async () => {
        mockArchivedChatsDataRef.current = [buildChat({id: 43, status: 'ARCHIVED', title: 'Archived chat'})];
        aiHubChatsStore.getState().setCurrentChatId(43);

        const user = await openMenu();

        expect(screen.getByRole('heading', {hidden: true, name: 'Archived chat'})).toBeInTheDocument();
        expect(await screen.findByRole('menuitem', {name: 'Rename'})).toBeInTheDocument();
        expect(screen.getByRole('menuitem', {name: 'Delete'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Archive'})).toBeNull();

        await user.click(screen.getByRole('menuitem', {name: 'Unarchive'}));

        expect(unarchiveMutateMock).toHaveBeenCalledWith({chatId: 43, patch: {status: 'ACTIVE'}, workspaceId: 1});
    });

    it('offers none of rename/archive/delete for a chat the caller does not own', async () => {
        mockChatsDataRef.current = [buildChat({id: 42, isOwner: false, title: 'Other chat'})];
        aiHubChatsStore.getState().setCurrentChatId(42);

        await openMenu();

        expect(await screen.findByRole('menuitem', {name: 'Show tool calls'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Rename'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Delete'})).not.toBeInTheDocument();
    });

    it('offers only the tool-call toggle when no chat is current', async () => {
        await openMenu();

        expect(await screen.findByRole('menuitem', {name: 'Show tool calls'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Rename'})).not.toBeInTheDocument();
    });

    it('toggles the tool-call cards', async () => {
        const user = await openMenu();

        await user.click(await screen.findByRole('menuitem', {name: 'Show tool calls'}));

        expect(useAiHubSettingsStore.getState().showToolCalls).toBe(true);
    });

    it('opens the rename dialog seeded with the title', async () => {
        mockChatsDataRef.current = [buildChat({id: 41, title: 'Some chat'})];
        aiHubChatsStore.getState().setCurrentChatId(41);

        const user = await openMenu();

        await user.click(await screen.findByRole('menuitem', {name: 'Rename'}));

        expect(await screen.findByDisplayValue('Some chat')).toBeInTheDocument();
    });
});
