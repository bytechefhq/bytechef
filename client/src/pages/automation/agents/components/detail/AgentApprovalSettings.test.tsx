import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentApprovalSettings from './AgentApprovalSettings';

beforeAll(() => {
    // Radix Select/DropdownMenu rely on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
    Element.prototype.scrollIntoView = vi.fn();
});

const {
    addAgentElementMutate,
    deleteAgentElementMutate,
    getComponentDefinitionsQuery,
    getWorkspaceConnectionsQuery,
    mutationPendingState,
    updateAgentElementMutate,
} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
    getComponentDefinitionsQuery: vi.fn().mockReturnValue({
        data: [
            {name: 'chat', version: 1},
            {name: 'slack', version: 1},
        ],
    }),
    getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
    mutationPendingState: {isAddPending: false},
    updateAgentElementMutate: vi.fn(),
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

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: getComponentDefinitionsQuery,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: mutationPendingState.isAddPending, mutate: addAgentElementMutate}),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
    useUpdateAiAgentElementMutation: () => ({isPending: false, mutate: updateAgentElementMutate}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

// The delivery channels are shared by both HITL mechanisms and only render when at least one is on, so the
// channel-facing tests below carry one enabling element rather than an empty list.
const approvalToolElement = {
    connectionId: null,
    id: 'approval-tool-1',
    kind: 'APPROVAL_TOOL',
    parameters: {},
    position: 0,
    referenceId: null,
};

const approvalGateElement = {
    connectionId: null,
    id: 'approval-gate-1',
    kind: 'APPROVAL_GATE',
    parameters: {},
    position: 0,
    referenceId: null,
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    updateAgentElementMutate.mockReset();
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
    getComponentDefinitionsQuery.mockReset().mockReturnValue({
        data: [
            {name: 'chat', version: 1},
            {name: 'slack', version: 1},
        ],
    });
    mutationPendingState.isAddPending = false;
});

// The agent's own channels, which is where approvals are delivered now: chat carries one, workflowCall cannot.
const approvalChannelsFixture = [
    {channelType: 'chat', connectionId: null, id: 'chat-1', parameters: {}, position: 0},
    {channelType: 'slack', connectionId: 'conn-1', id: 'slack-1', parameters: {}, position: 1},
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
] as any;

describe('AgentApprovalSettings', () => {
    // Approvals are delivered over the agent's OWN channels, so there is nothing to add here — the section
    // names what the agent already has instead of offering a second, separately configured list.
    it('names the agent channels approvals are delivered over', () => {
        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                channels={approvalChannelsFixture}
                elements={[approvalToolElement]}
            />
        );

        expect(screen.getByText(/delivered to the agent's channels: Chat, Slack/)).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add channel'})).not.toBeInTheDocument();
    });

    // schedule and workflowCall cannot carry an approval — one has nobody to ask, the other's caller is another
    // workflow — so an agent with only those falls back to chat at run time, which this says plainly.
    it('warns when the agent has no channel that can carry an approval', () => {
        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                channels={[{channelType: 'schedule', id: 'schedule-1', parameters: {}, position: 0}] as any}
                elements={[approvalToolElement]}
            />
        );

        expect(screen.getByText(/no channel that can carry an approval/)).toBeInTheDocument();
    });

    it('adds an APPROVAL_TOOL element when the request-approval switch is turned on', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentApprovalSettings agentId="agent-1" channels={approvalChannelsFixture} elements={[]} />);

        await user.click(screen.getByRole('switch', {name: 'Agent may request approval'}));

        expect(addAgentElementMutate).toHaveBeenCalledWith({input: {agentId: 'agent-1', kind: 'APPROVAL_TOOL'}});
    });

    it('deletes the APPROVAL_TOOL element when the request-approval switch is turned off', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                channels={approvalChannelsFixture}
                elements={[approvalToolElement]}
            />
        );

        await user.click(screen.getByRole('switch', {name: 'Agent may request approval'}));

        expect(deleteAgentElementMutate).toHaveBeenCalledWith({id: 'approval-tool-1'});
    });

    it('disables the request-approval switch while a mutation is pending', () => {
        mutationPendingState.isAddPending = true;

        wrap(<AgentApprovalSettings agentId="agent-1" channels={approvalChannelsFixture} elements={[]} />);

        expect(screen.getByRole('switch', {name: 'Agent may request approval'})).toBeDisabled();
    });

    // The APPROVAL_GATE row is the master switch AiAgentWorkflowGenerator.buildToolSequence reads: with no row,
    // a tool's own requiresApproval flag does not gate it. Adding and removing the row IS the toggle, which is
    // also why the toggle needs no default declared anywhere — absence is off.
    it('adds an APPROVAL_GATE element when the tool-approval switch is turned on', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentApprovalSettings agentId="agent-1" channels={approvalChannelsFixture} elements={[]} />);

        await user.click(screen.getByRole('switch', {name: 'Tool approval'}));

        expect(addAgentElementMutate).toHaveBeenCalledWith({input: {agentId: 'agent-1', kind: 'APPROVAL_GATE'}});
    });

    it('deletes the APPROVAL_GATE element when the tool-approval switch is turned off', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                channels={approvalChannelsFixture}
                elements={[approvalGateElement]}
            />
        );

        await user.click(screen.getByRole('switch', {name: 'Tool approval'}));

        expect(deleteAgentElementMutate).toHaveBeenCalledWith({id: 'approval-gate-1'});
    });

    it('shows the expiry input only once the tool-approval switch is on', async () => {
        const {unmount} = wrap(
            <AgentApprovalSettings agentId="agent-1" channels={approvalChannelsFixture} elements={[]} />
        );

        expect(screen.queryByLabelText('Approval expires after (days)')).not.toBeInTheDocument();

        unmount();

        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                channels={approvalChannelsFixture}
                elements={[approvalGateElement]}
            />
        );

        expect(screen.getByLabelText('Approval expires after (days)')).toBeInTheDocument();
    });

    // Both toggles are independent, but they deliver over the same channels — so the channel list belongs to
    // neither alone and is hidden only when both are off.
    it('hides the approval channels block when both switches are off', () => {
        wrap(<AgentApprovalSettings agentId="agent-1" channels={approvalChannelsFixture} elements={[]} />);

        expect(screen.queryByRole('button', {name: 'Add channel'})).not.toBeInTheDocument();
        expect(screen.queryByRole('listitem', {name: 'Chat'})).not.toBeInTheDocument();
    });

    it('names the delivery channels when only the tool-approval switch is on', () => {
        wrap(
            <AgentApprovalSettings
                agentId="agent-1"
                channels={approvalChannelsFixture}
                elements={[approvalGateElement]}
            />
        );

        expect(screen.getByText(/delivered to the agent's channels/)).toBeInTheDocument();
    });
});
