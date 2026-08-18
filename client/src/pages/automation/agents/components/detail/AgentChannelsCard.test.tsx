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
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
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

// Editing a channel opens the shared component config dialog, which reaches ConnectionDialog and its graphql
// exports — unloadable here, since this file mocks the graphql module wholesale. The stub keeps the one thing
// these tests care about: that submitting a cleared connection reaches the card's handler.
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

            {/* The tuple the dialog looks a definition up by. Rendered so a test can pin it: a wrong trigger
                name yields an EMPTY Properties tab in the real dialog rather than an error. */}

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

// The GENERATED QUERY is mocked, not useAiAgentChannelDefinitions -- so the real hook, including the one
// client-side exclusion it applies to the add menu, runs in these tests.
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

        // Titles come from each channel's own declaration now, which is why these two read as WhatsApp channels
        // rather than as bare component names -- twilio and infobip each also expose SMS and inbound-call
        // operations, so "Twilio" alone would not say which one this is.
        expect(screen.getByRole('option', {name: 'Twilio (WhatsApp)'})).toBeInTheDocument();
        expect(screen.getByRole('option', {name: 'Infobip (WhatsApp)'})).toBeInTheDocument();

        // whatsapp is de-scoped: its declared trigger output schema does not match the real Cloud API webhook
        // payload, so the exclusion lives in useAiAgentChannelDefinitions rather than in this card. Only the add
        // affordance is hidden -- the row test below pins that an existing channel is untouched.
        expect(screen.queryByRole('option', {name: 'WhatsApp'})).not.toBeInTheDocument();
    });

    // The exclusion above hides the "add a new one" affordance and nothing else: an agent that already has a
    // whatsapp channel must keep seeing it and keep being able to configure it.
    it('renders an existing whatsapp channel and leaves it configurable', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, whatsappChannel]} />);

        const whatsappRow = screen.getByRole('listitem', {name: 'WhatsApp'});

        expect(within(whatsappRow).getByRole('button', {name: 'Edit WhatsApp channel'})).toBeInTheDocument();
        expect(within(whatsappRow).getByRole('button', {name: 'Delete WhatsApp trigger'})).toBeInTheDocument();
    });

    // A channel is edited through its trigger's own property tree, and a wrong trigger name renders an empty
    // Properties tab rather than an error. Slack's channel pairs newMessage; the client used to name anyEvent.
    it("opens a channel's own trigger, as the registry pairs it", async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, slackChannel]} />);

        await user.click(screen.getByRole('button', {name: 'Edit Slack channel'}));

        expect(await screen.findByTestId('component-config-target')).toHaveTextContent('slack/v1/newMessage');
    });

    // The Configure affordance follows what there is to configure, which is a connection OR the trigger's own
    // unpinned properties -- not the connection alone. chat is the case that distinguishes the two: it takes no
    // connection, and its trigger's only property (mode) is pinned to hosted by the channel itself, so there is
    // nothing left to set and the row carries no button.
    it('offers no configure affordance on a channel with neither a connection nor unpinned properties', () => {
        wrap(<AgentChannelsCard agentId="agent-1" channels={[chatChannel, workflowCallChannel]} />);

        const chatRow = screen.getByRole('listitem', {name: 'Chat'});

        expect(within(chatRow).queryByRole('button', {name: /edit/i})).not.toBeInTheDocument();
    });

    // ...and the predicate is genuinely reading propertiesConfigurable rather than standing in for
    // connectionRequired: a channel with properties and no connection does get the button.
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
