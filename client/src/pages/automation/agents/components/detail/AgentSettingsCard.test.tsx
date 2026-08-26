import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentSettingsCard from './AgentSettingsCard';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {
    addAgentElementMutate,
    deleteAgentElementMutate,
    getWorkspaceConnectionsQuery,
    mutationPendingState,
    updateAiAgentSettingsMutate,
} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
    getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
    mutationPendingState: {isSettingsPending: false},
    updateAiAgentSettingsMutate: vi.fn(),
}));

// Stubbed rather than rendered: the HITL switches are element-backed, so they pull in three more graphql
// mutation hooks and a component-definitions query that this file's wholesale graphql mock does not provide.
// They have their own test file — these tests are about the settings-map toggles.
vi.mock('@/pages/automation/agents/components/detail/AgentApprovalSettings', () => ({
    default: () => null,
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

vi.mock('@/shared/queries/automation/connections.queries', () => ({
    useGetWorkspaceConnectionsQuery: getWorkspaceConnectionsQuery,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
    useUpdateAiAgentSettingsMutation: () => ({
        isPending: mutationPendingState.isSettingsPending,
        mutate: updateAiAgentSettingsMutate,
    }),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    updateAiAgentSettingsMutate.mockReset();
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
    mutationPendingState.isSettingsPending = false;
});

describe('AgentSettingsCard', () => {
    it('renders the documented defaults when settings is empty (webSearch off, the rest on)', () => {
        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.getByRole('switch', {name: 'Ask user question'})).toBeChecked();
        expect(screen.getByRole('switch', {name: 'Auto memory'})).toBeChecked();
        expect(screen.getByRole('switch', {name: 'Skill management'})).toBeChecked();
        expect(screen.getByRole('switch', {name: 'Web search'})).not.toBeChecked();
        expect(screen.queryByLabelText('Brave connection')).not.toBeInTheDocument();
    });

    it('honors explicit settings values over the defaults', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[]}
                settings={{builtInTools: {askUserQuestion: false, webSearch: true, webSearchConnectionId: 42}}}
            />
        );

        expect(screen.getByRole('switch', {name: 'Ask user question'})).not.toBeChecked();
        expect(screen.getByRole('switch', {name: 'Web search'})).toBeChecked();
        expect(screen.getByLabelText('Brave connection')).toBeInTheDocument();
    });

    it('sends the complete builtInTools map when a switch is toggled off', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await user.click(screen.getByRole('switch', {name: 'Skill management'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: true,
                    skillManagement: false,
                    webSearch: false,
                    webSearchProvider: 'BRAVE',
                },
            },
        });
    });

    it('reveals a Brave connection select when web search is turned on, and drops the id when turned off', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.queryByLabelText('Brave connection')).not.toBeInTheDocument();

        await user.click(screen.getByRole('switch', {name: 'Web search'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: true,
                    skillManagement: true,
                    webSearch: true,
                    webSearchProvider: 'BRAVE',
                },
            },
        });
        expect(await screen.findByLabelText('Brave connection')).toBeInTheDocument();
    });

    it("queries the selected provider's own component for connections and clears a stale id on switching", async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[]}
                settings={{builtInTools: {webSearch: true, webSearchConnectionId: 42}}}
            />
        );

        expect(getWorkspaceConnectionsQuery).toHaveBeenLastCalledWith(
            {componentName: 'brave', environmentId: 123, id: 7},
            true
        );

        await user.click(screen.getByLabelText('Search provider'));
        await user.click(await screen.findByRole('option', {name: 'Firecrawl'}));

        // The Brave connection id must not survive: it names a connection of the wrong component.
        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: true,
                    skillManagement: true,
                    webSearch: true,
                    webSearchProvider: 'FIRECRAWL',
                },
            },
        });
        expect(await screen.findByLabelText('Firecrawl connection')).toBeInTheDocument();
        expect(getWorkspaceConnectionsQuery).toHaveBeenLastCalledWith(
            {componentName: 'firecrawl', environmentId: 123, id: 7},
            true
        );
    });

    it('offers no connection for the native provider and disables the connections query', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[]}
                settings={{builtInTools: {webSearch: true, webSearchProvider: 'NATIVE'}}}
            />
        );

        expect(screen.queryByLabelText('Brave connection')).not.toBeInTheDocument();
        expect(screen.queryByLabelText('Firecrawl connection')).not.toBeInTheDocument();
        expect(screen.getByText('The model searches the web itself. No connection needed.')).toBeInTheDocument();
        expect(getWorkspaceConnectionsQuery).toHaveBeenLastCalledWith(
            {componentName: '', environmentId: 123, id: 7},
            false
        );
    });

    // Publishing is what actually rejects this (AiAgentErrorType.NATIVE_WEB_SEARCH_UNSUPPORTED); the panel says so
    // first, so the choice is not silently accepted here and refused three clicks later.
    it('warns when the native provider is picked against a model provider that has no built-in search', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[{id: '1', kind: 'MODEL', parameters: {model: 'gpt-4o', provider: 'openai'}}] as never}
                settings={{builtInTools: {webSearch: true, webSearchProvider: 'NATIVE'}}}
            />
        );

        expect(screen.getByText(/openai has no built-in web search/)).toBeInTheDocument();
    });

    // The settings-map switches are optimistic — `commit` writes local state before firing the mutation — so
    // disabling them mid-flight buys nothing and greys out the whole list on every single flip, which is what
    // made one toggle look like it re-rendered all of them.
    it('leaves the settings-map switches enabled while the settings mutation is in flight', () => {
        mutationPendingState.isSettingsPending = true;

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.getByRole('switch', {name: 'Ask user question'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Auto memory'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Skill management'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Web search'})).toBeEnabled();
    });

    // Chat memory shares this toggle list with the built-in tools but is a CHAT_MEMORY element row, so it
    // must drive the element mutations and leave the settings mutation untouched.
    it('fires the add-element mutation with kind CHAT_MEMORY when chat memory is switched on', async () => {
        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await userEvent.click(screen.getByRole('switch', {name: 'Chat memory'}));

        expect(addAgentElementMutate).toHaveBeenCalledWith({input: {agentId: 'agent-1', kind: 'CHAT_MEMORY'}});
        expect(updateAiAgentSettingsMutate).not.toHaveBeenCalled();
    });

    it('fires the delete-element mutation when chat memory is switched off', async () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[
                    {
                        connectionId: null,
                        id: 'element-1',
                        kind: 'CHAT_MEMORY',
                        parameters: {},
                        position: 0,
                        referenceId: null,
                    },
                ]}
                settings={null}
            />
        );

        await userEvent.click(screen.getByRole('switch', {name: 'Chat memory'}));

        expect(deleteAgentElementMutate).toHaveBeenCalledWith({id: 'element-1'});
        expect(updateAiAgentSettingsMutate).not.toHaveBeenCalled();
    });
});
