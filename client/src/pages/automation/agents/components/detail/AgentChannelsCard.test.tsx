import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentChannelsCard from './AgentChannelsCard';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {addAgentChannelMutate, deleteAgentChannelMutate, getWorkspaceConnectionsQuery, updateAgentChannelMutate} =
    vi.hoisted(() => ({
        addAgentChannelMutate: vi.fn(),
        deleteAgentChannelMutate: vi.fn(),
        getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
        updateAgentChannelMutate: vi.fn(),
    }));

// Editing a channel opens the shared component config dialog, which reaches ConnectionDialog and its graphql
// exports — unloadable here, since this file mocks the graphql module wholesale. The stub keeps the one thing
// these tests care about: that submitting a cleared connection reaches the card's handler.
vi.mock('@/shared/components/component-config/ComponentConfigDialog', () => ({
    default: ({
        onSubmit,
        picker,
    }: {
        onSubmit: (values: {connectionId: null; parameters: object}) => void;
        picker?: ReactNode;
    }) => (
        <div>
            {picker}

            <button onClick={() => onSubmit({connectionId: null, parameters: {}})} type="button">
                Save
            </button>
        </div>
    ),
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
    useAddAiAgentChannelMutation: () => ({isPending: false, mutate: addAgentChannelMutate}),
    useDeleteAiAgentChannelMutation: () => ({isPending: false, mutate: deleteAgentChannelMutate}),
    useUpdateAiAgentChannelMutation: () => ({isPending: false, mutate: updateAgentChannelMutate}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

const chatChannel = {channelType: 'chat', connectionId: null, id: 'chat-1', parameters: {}, position: 0};
const workflowCallChannel = {
    channelType: 'workflowCall',
    connectionId: null,
    id: 'workflow-call-1',
    parameters: {},
    position: 1,
};
const telegramChannel = {
    channelType: 'telegram',
    connectionId: null,
    id: 'telegram-1',
    parameters: {},
    position: 2,
};

beforeEach(() => {
    addAgentChannelMutate.mockReset();
    deleteAgentChannelMutate.mockReset();
    updateAgentChannelMutate.mockReset();
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
});

describe('AgentChannelsCard', () => {
    it('renders the pinned chat row without a delete affordance and hides workflowCall', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        const chatRow = screen.getByRole('listitem', {name: 'Chat'});

        expect(within(chatRow).queryByRole('button', {name: /delete/i})).not.toBeInTheDocument();

        // workflowCall stays a real channel on the agent -- the generator always emits it -- but it carries no
        // configuration, so the permanently inert row is not rendered.
        expect(screen.queryByRole('listitem', {name: 'Workflow Call'})).not.toBeInTheDocument();
    });

    it('excludes chat, workflowCall and schedule from the add-channel dialog', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        await user.click(screen.getByRole('button', {name: 'Add channel'}));
        await user.click(await screen.findByLabelText('Channel'));

        expect(await screen.findByRole('option', {name: 'Slack'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Telegram'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Rocket.Chat'})).toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Chat'})).not.toBeInTheDocument();
        expect(screen.queryByRole('option', {name: 'Workflow Call'})).not.toBeInTheDocument();

        // schedule is still an agent_channel row of type 'schedule'; it is only presented in its own
        // Schedules section (AgentScheduleCard) instead of as a messaging channel.
        expect(screen.queryByRole('option', {name: 'Schedule'})).not.toBeInTheDocument();

        // whatsapp is de-scoped from v1: the component's declared trigger output schema doesn't match the real
        // Cloud API webhook shape (arrays vs single objects) — see AgentChannelsCard.tsx's ADDABLE_CHANNEL_TYPES
        // comment and docs/agents/agents.md.
        expect(screen.queryByRole('option', {name: 'WhatsApp'})).not.toBeInTheDocument();
    });

    it('fires the delete mutation for a telegram row', async () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel, telegramChannel]} />);

        const telegramRow = screen.getByRole('listitem', {name: 'Telegram'});

        await userEvent.click(within(telegramRow).getByRole('button', {name: /delete/i}));

        expect(deleteAgentChannelMutate).toHaveBeenCalledWith({id: 'telegram-1'});
    });

    it('clears a wired connection via delete-then-add rather than a no-op update', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        // updateAiAgentChannel treats connectionId: null as "leave unchanged" server-side, so the clear
        // path must go through delete+add instead — simulate the delete resolving so the chained add
        // fires synchronously and is observable in this test.
        deleteAgentChannelMutate.mockImplementation((_variables, options) => {
            options?.onSuccess?.();
        });

        getWorkspaceConnectionsQuery.mockReturnValue({data: [{id: 'conn-1', name: 'My Telegram Bot'}]});

        wrap(
            <AgentChannelsCard
                agentId="agent-1"
                channels={[chatChannel, workflowCallChannel, {...telegramChannel, connectionId: 'conn-1'}]}
            />
        );

        // The connection is edited in the channel dialog now, not an inline select on the row.
        const telegramRow = screen.getByRole('listitem', {name: 'Telegram'});

        await user.click(within(telegramRow).getByRole('button', {name: 'Edit Telegram channel'}));

        await user.click(await screen.findByRole('button', {name: 'Save'}));

        expect(deleteAgentChannelMutate).toHaveBeenCalledWith(
            {id: 'telegram-1'},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(addAgentChannelMutate).toHaveBeenCalledWith({
            input: {agentId: 'agent-1', channelType: 'telegram', connectionId: null, parameters: {}},
        });
    });
});
