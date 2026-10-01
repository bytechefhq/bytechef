import {
    AI_AGENT_CHANNEL_DEFINITIONS,
    aiAgentChannelDefinitionsQueryResult,
} from '@/pages/automation/agents/hooks/aiAgentChannelDefinitions.fixture';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentChannelsCard from './AgentChannelsCard';

beforeAll(() => {
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {
    addAgentChannelMutate,
    channelDefinitionsQuery,
    deleteAgentChannelMutate,
    getWorkspaceConnectionsQuery,
    updateAgentChannelMutate,
} = vi.hoisted(() => ({
    addAgentChannelMutate: vi.fn(),
    channelDefinitionsQuery: vi.fn(),
    deleteAgentChannelMutate: vi.fn(),
    getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
    updateAgentChannelMutate: vi.fn(),
}));

vi.mock('@/shared/components/component-config/ComponentConfigDialog', () => ({
    default: ({
        onSubmit,
        picker,
        target,
    }: {
        onSubmit: (values: {connectionId: null; parameters: object}) => void;
        picker?: ReactNode;
        target?: {clusterElementName: string; componentName: string; componentVersion: number} | null;
    }) => (
        <div>
            {picker}

            {target && (
                <span data-testid="component-config-target">
                    {`${target.componentName}/v${target.componentVersion}/${target.clusterElementName}`}
                </span>
            )}

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
    useAiAgentChannelDefinitionsQuery: () => channelDefinitionsQuery(),
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
const slackChannel = {channelType: 'slack', connectionId: 'conn-1', id: 'slack-1', parameters: {}, position: 3};
const whatsappChannel = {channelType: 'whatsapp', connectionId: null, id: 'whatsapp-1', parameters: {}, position: 4};

beforeEach(() => {
    addAgentChannelMutate.mockReset();
    deleteAgentChannelMutate.mockReset();
    updateAgentChannelMutate.mockReset();
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
    channelDefinitionsQuery.mockReset().mockReturnValue(aiAgentChannelDefinitionsQueryResult());
});

describe('AgentChannelsCard', () => {
    it('renders the pinned chat row without a delete affordance and hides workflowCall', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        const chatRow = screen.getByRole('listitem', {name: 'Chat'});

        expect(within(chatRow).queryByRole('button', {name: /delete/i})).not.toBeInTheDocument();

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

        expect(screen.queryByRole('option', {name: 'Schedule'})).not.toBeInTheDocument();

        expect(screen.getByRole('option', {name: 'Twilio (WhatsApp)'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Infobip (WhatsApp)'})).toBeInTheDocument();

        expect(screen.queryByRole('option', {name: 'WhatsApp'})).not.toBeInTheDocument();
    });

    it('renders an existing whatsapp channel and leaves it configurable', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, whatsappChannel]} />);

        const whatsappRow = screen.getByRole('listitem', {name: 'WhatsApp'});

        expect(within(whatsappRow).getByRole('button', {name: 'Edit WhatsApp channel'})).toBeInTheDocument();
        expect(within(whatsappRow).getByRole('button', {name: 'Delete WhatsApp trigger'})).toBeInTheDocument();
    });

    it("opens a channel's own trigger, as the registry pairs it", async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, slackChannel]} />);

        await user.click(screen.getByRole('button', {name: 'Edit Slack channel'}));

        expect(await screen.findByTestId('component-config-target')).toHaveTextContent('slack/v1/newMessage');
    });

    it('offers no configure affordance on a channel with neither a connection nor unpinned properties', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        const chatRow = screen.getByRole('listitem', {name: 'Chat'});

        expect(within(chatRow).queryByRole('button', {name: /edit/i})).not.toBeInTheDocument();
    });

    it('offers the configure affordance on a channel that has properties but no connection', () => {
        channelDefinitionsQuery.mockReturnValue(
            aiAgentChannelDefinitionsQueryResult(
                AI_AGENT_CHANNEL_DEFINITIONS.map((definition) =>
                    definition.channelType === 'chat' ? {...definition, propertiesConfigurable: true} : definition
                )
            )
        );

        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        const chatRow = screen.getByRole('listitem', {name: 'Chat'});

        expect(within(chatRow).getByRole('button', {name: 'Edit Chat channel'})).toBeInTheDocument();
    });

    it('fires the delete mutation for a telegram row', async () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel, telegramChannel]} />);

        const telegramRow = screen.getByRole('listitem', {name: 'Telegram'});

        await userEvent.click(within(telegramRow).getByRole('button', {name: /delete/i}));

        expect(deleteAgentChannelMutate).toHaveBeenCalledWith({id: 'telegram-1'});
    });

    it('clears a wired connection via delete-then-add rather than a no-op update', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

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
