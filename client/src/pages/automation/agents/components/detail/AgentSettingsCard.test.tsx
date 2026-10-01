import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentSettingsCard from './AgentSettingsCard';

beforeAll(() => {
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
                reasoningEffort: 'MEDIUM',
                streamResponse: true,
                thinking: false,
            },
        });
    });

    it('renders stream response on by default and off when the stored setting says so', () => {
        const {unmount} = wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.getByRole('switch', {name: 'Stream response'})).toBeChecked();

        unmount();

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={{streamResponse: false}} />);

        expect(screen.getByRole('switch', {name: 'Stream response'})).not.toBeChecked();
    });

    it('sends builtInTools alongside streamResponse when stream response is toggled off', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await user.click(screen.getByRole('switch', {name: 'Stream response'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: true,
                    skillManagement: true,
                    webSearch: false,
                    webSearchProvider: 'BRAVE',
                },
                reasoningEffort: 'MEDIUM',
                streamResponse: false,
                thinking: false,
            },
        });
    });

    it('preserves an explicit streamResponse when a built-in tool is toggled', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={{streamResponse: false}} />);

        await user.click(screen.getByRole('switch', {name: 'Auto memory'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: false,
                    skillManagement: true,
                    webSearch: false,
                    webSearchProvider: 'BRAVE',
                },
                reasoningEffort: 'MEDIUM',
                streamResponse: false,
                thinking: false,
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
                reasoningEffort: 'MEDIUM',
                streamResponse: true,
                thinking: false,
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
                reasoningEffort: 'MEDIUM',
                streamResponse: true,
                thinking: false,
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

    it('leaves the settings-map switches enabled while the settings mutation is in flight', () => {
        mutationPendingState.isSettingsPending = true;

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.getByRole('switch', {name: 'Ask user question'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Auto memory'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Skill management'})).toBeEnabled();
        expect(screen.getByRole('switch', {name: 'Web search'})).toBeEnabled();
    });

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

    it('renders thinking off by default and keeps the reasoning effort select hidden', () => {
        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        expect(screen.getByRole('switch', {name: 'Thinking'})).not.toBeChecked();
        expect(screen.queryByLabelText('Reasoning effort')).not.toBeInTheDocument();
    });

    it('sends thinking and reveals the reasoning effort select when thinking is turned on', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await user.click(screen.getByRole('switch', {name: 'Thinking'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith({
            id: 'agent-1',
            settings: {
                builtInTools: {
                    askUserQuestion: true,
                    autoMemory: true,
                    skillManagement: true,
                    webSearch: false,
                    webSearchProvider: 'BRAVE',
                },
                reasoningEffort: 'MEDIUM',
                streamResponse: true,
                thinking: true,
            },
        });
        expect(await screen.findByLabelText('Reasoning effort')).toBeInTheDocument();
    });

    it('sends the chosen reasoning effort', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={{thinking: true}} />);

        await user.click(screen.getByLabelText('Reasoning effort'));
        await user.click(await screen.findByRole('option', {name: 'High'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith(
            expect.objectContaining({settings: expect.objectContaining({reasoningEffort: 'HIGH', thinking: true})})
        );
    });

    it('falls back to medium effort for an unrecognised stored value', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[]}
                settings={{reasoningEffort: 'ludicrous', thinking: true}}
            />
        );

        expect(screen.getByLabelText('Reasoning effort')).toHaveTextContent('Medium');
    });

    it('warns when thinking is on for a model provider that has none', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[
                    {
                        connectionId: null,
                        id: 'element-1',
                        kind: 'MODEL',
                        parameters: {model: 'gpt-4', provider: 'openRouter'},
                        position: 0,
                        referenceId: null,
                    },
                ]}
                settings={{thinking: true}}
            />
        );

        expect(screen.getByText(/openRouter has no extended reasoning/)).toBeInTheDocument();
    });

    it('does not warn when thinking is on for a provider that supports it', () => {
        wrap(
            <AgentSettingsCard
                agentId="agent-1"
                channels={[]}
                elements={[
                    {
                        connectionId: null,
                        id: 'element-1',
                        kind: 'MODEL',
                        parameters: {model: 'claude-sonnet-5', provider: 'anthropic'},
                        position: 0,
                        referenceId: null,
                    },
                ]}
                settings={{thinking: true}}
            />
        );

        expect(screen.queryByText(/has no extended reasoning/)).not.toBeInTheDocument();
    });

    it('omits maxToolCalls from the payload while it is unset', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await user.click(screen.getByRole('switch', {name: 'Auto memory'}));

        const [call] = updateAiAgentSettingsMutate.mock.calls.at(-1) as [{settings: Record<string, unknown>}];

        expect(call.settings).not.toHaveProperty('maxToolCalls');
    });

    it('commits max tool calls on blur, and clears it back to the default when emptied', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={{maxToolCalls: 30}} />);

        const input = screen.getByLabelText('Max tool calls');

        expect(input).toHaveValue(30);

        await user.clear(input);
        await user.type(input, '80');
        await user.tab();

        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith(
            expect.objectContaining({settings: expect.objectContaining({maxToolCalls: 80})})
        );

        await user.clear(input);
        await user.tab();

        const [call] = updateAiAgentSettingsMutate.mock.calls.at(-1) as [{settings: Record<string, unknown>}];

        expect(call.settings).not.toHaveProperty('maxToolCalls');
    });

    it('does not commit while max tool calls is being typed', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={null} />);

        await user.type(screen.getByLabelText('Max tool calls'), '150');

        expect(updateAiAgentSettingsMutate).not.toHaveBeenCalled();
    });

    it('preserves an explicit maxToolCalls when a built-in tool is toggled', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentSettingsCard agentId="agent-1" channels={[]} elements={[]} settings={{maxToolCalls: 30}} />);

        await user.click(screen.getByRole('switch', {name: 'Auto memory'}));

        expect(updateAiAgentSettingsMutate).toHaveBeenLastCalledWith(
            expect.objectContaining({settings: expect.objectContaining({maxToolCalls: 30})})
        );
    });
});
