import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentToolsCard from './AgentToolsCard';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {
    addAgentElementMutate,
    deleteAgentElementMutate,
    getComponentDefinitionQuery,
    getComponentDefinitionsQuery,
    getRootComponentClusterElementDefinitions,
    getWorkspaceConnectionsQuery,
    updateAgentElementMutate,
} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
    getComponentDefinitionQuery: vi.fn().mockReturnValue({data: undefined}),
    getComponentDefinitionsQuery: vi.fn().mockReturnValue({data: []}),
    getRootComponentClusterElementDefinitions: vi.fn().mockReturnValue({data: []}),
    getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
    updateAgentElementMutate: vi.fn(),
}));

// The dialog is exercised on its own; here it would drag the whole connection stack (ConnectionDialog and
// its graphql exports) into a test that is about the card's rows and mutations. The stub still renders the
// picker and offers a submit, since the add flow's component/tool panes live in that slot.
vi.mock('@/shared/components/component-config/ComponentConfigDialog', () => ({
    default: ({
        onSubmit,
        picker,
    }: {
        onSubmit: (values: {connectionId: null; parameters: object}) => void;
        picker?: ReactNode;
    }) => (
        <div data-testid="component-config-dialog">
            {picker}

            <button onClick={() => onSubmit({connectionId: null, parameters: {}})} type="button">
                Add
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

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: getComponentDefinitionsQuery,
}));

vi.mock('@/shared/queries/platform/clusterElementDefinitions.queries', () => ({
    useGetRootComponentClusterElementDefinitions: getRootComponentClusterElementDefinitions,
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: getComponentDefinitionQuery,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
    useUpdateAiAgentElementMutation: () => ({isPending: false, mutate: updateAgentElementMutate}),
}));

// TooltipProvider mirrors main.tsx, which wraps the whole app in one — Radix's Tooltip throws without it.
const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );
};

const toolElement = {
    connectionId: null,
    id: 'tool-1',
    kind: 'TOOL',
    parameters: {actionName: 'createRefund', componentName: 'stripe', componentVersion: 1, parameters: {amount: 100}},
    position: 0,
    referenceId: null,
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    updateAgentElementMutate.mockReset();
    getComponentDefinitionQuery.mockReset().mockReturnValue({data: undefined});
    getComponentDefinitionsQuery.mockReset().mockReturnValue({data: []});
    getRootComponentClusterElementDefinitions.mockReset().mockReturnValue({data: []});
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
});

const componentDefinitions = [
    {actionsCount: 3, description: 'Payments', icon: null, name: 'stripe', title: 'Stripe', version: 1},
    {actionsCount: 2, description: 'Messaging', icon: null, name: 'slack', title: 'Slack', version: 1},
    // Filtered out of the picker entirely: it exposes no TOOLS cluster element, so it has no tool to offer —
    // having actions is not the same thing, which is exactly what the picker used to get wrong.
    {actionsCount: 4, description: 'Actions but no tools', icon: null, name: 'accelo', title: 'Accelo', version: 1},
];

// What the aiAgent root reports as its available TOOLS cluster elements, each carrying its owning component.
const agentTools = [
    {
        componentName: 'stripe',
        componentVersion: 1,
        description: 'Refunds a charge.',
        name: 'createRefund',
        outputDefined: false,
        title: 'Create Refund',
    },
    {
        componentName: 'stripe',
        componentVersion: 1,
        description: 'Never hand-picked.',
        name: 'skillsTool',
        outputDefined: false,
        title: 'Skills',
    },
    {
        componentName: 'slack',
        componentVersion: 1,
        description: 'Posts a message.',
        name: 'sendMessage',
        outputDefined: false,
        title: 'Send Message',
    },
];

describe('AgentToolsCard', () => {
    const openPicker = async (user: ReturnType<typeof userEvent.setup>) => {
        getComponentDefinitionsQuery.mockReturnValue({data: componentDefinitions});
        getRootComponentClusterElementDefinitions.mockReturnValue({data: agentTools});

        wrap(<AgentToolsCard agentId="agent-1" elements={[]} />);

        await user.click(screen.getByRole('button', {name: 'Add tool'}));
    };

    it('offers only tool-bearing components in the component combobox', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        await openPicker(user);

        await user.click(screen.getByRole('combobox', {name: /component/i}));

        expect(await screen.findByText('Stripe')).toBeInTheDocument();
        expect(screen.getByText('Slack')).toBeInTheDocument();

        // Has actions but declares no TOOLS cluster element, so it offers no tool an agent could use.
        expect(screen.queryByText('Accelo')).not.toBeInTheDocument();
    });

    // skillsTool is emitted by AiAgentWorkflowGenerator whenever the agent has SKILL elements, so offering it
    // here would let a user add a second, conflicting copy of a tool the generator already owns.
    it('shows a chosen component tools, minus skillsTool, and adds the picked one', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        await openPicker(user);

        await user.click(screen.getByRole('combobox', {name: /component/i}));
        await user.click(await screen.findByText('Stripe'));

        await user.click(screen.getByRole('combobox', {name: /tool/i}));

        expect(await screen.findByText('Create Refund')).toBeInTheDocument();
        expect(screen.queryByText('Skills')).not.toBeInTheDocument();

        await user.click(screen.getByText('Create Refund'));
        await user.click(screen.getByRole('button', {name: 'Add'}));

        expect(addAgentElementMutate).toHaveBeenCalledWith({
            input: {
                agentId: 'agent-1',
                connectionId: null,
                kind: 'TOOL',
                parameters: {
                    actionName: 'createRefund',
                    componentName: 'stripe',
                    componentVersion: 1,
                    parameters: {},
                },
            },
        });
    });

    // Switching components must not carry the previous component's tool along: the pair is what identifies the
    // tool, so a stale action name would add a tool the newly picked component does not have. Asserted through
    // the mutation rather than the submit button's state, which belongs to the dialog and is stubbed here.
    it('clears the chosen tool when a different component is picked', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        await openPicker(user);

        await user.click(screen.getByRole('combobox', {name: /component/i}));
        await user.click(await screen.findByText('Stripe'));

        await user.click(screen.getByRole('combobox', {name: /tool/i}));
        await user.click(await screen.findByText('Create Refund'));

        await user.click(screen.getByRole('combobox', {name: /component/i}));
        await user.click(await screen.findByText('Slack'));

        await user.click(screen.getByRole('button', {name: 'Add'}));

        expect(addAgentElementMutate).not.toHaveBeenCalled();
    });

    it('sends the full existing parameters map plus requiresApproval: true when the switch is turned on', async () => {
        const user = userEvent.setup();

        wrap(<AgentToolsCard agentId="agent-1" elements={[toolElement]} />);

        const row = screen.getByRole('listitem', {name: 'stripe / createRefund'});

        await user.click(within(row).getByRole('switch', {name: 'Requires approval'}));

        expect(updateAgentElementMutate).toHaveBeenCalledWith({
            input: {
                id: 'tool-1',
                parameters: {
                    actionName: 'createRefund',
                    componentName: 'stripe',
                    componentVersion: 1,
                    parameters: {amount: 100},
                    requiresApproval: true,
                },
            },
        });
    });

    // The picker already keeps cluster roots out, but a row can still arrive through the agent tool API, the
    // copilot or MCP. Its properties live on its child cluster elements, so the config dialog would open empty.
    it('hides configure on a tool whose component is a cluster root, keeping delete available', () => {
        getComponentDefinitionQuery.mockReturnValue({
            data: {clusterRoot: true, name: 'aiAgent', title: 'AI Agent', version: 1},
        });

        wrap(
            <AgentToolsCard
                agentId="agent-1"
                elements={[
                    {
                        ...toolElement,
                        parameters: {actionName: 'chat', componentName: 'aiAgent', componentVersion: 1},
                    },
                ]}
            />
        );

        const row = screen.getByRole('listitem', {name: 'aiAgent / chat'});

        expect(within(row).queryByRole('button', {name: 'Configure tool'})).not.toBeInTheDocument();
        expect(within(row).getByLabelText('Configure on the workflow canvas')).toBeInTheDocument();
        expect(within(row).getByRole('button', {name: 'Delete tool'})).toBeInTheDocument();
    });

    it('offers configure on a tool whose component is not a cluster root', () => {
        getComponentDefinitionQuery.mockReturnValue({
            data: {clusterRoot: false, name: 'stripe', title: 'Stripe', version: 1},
        });

        wrap(<AgentToolsCard agentId="agent-1" elements={[toolElement]} />);

        const row = screen.getByRole('listitem', {name: 'stripe / createRefund'});

        expect(within(row).getByRole('button', {name: 'Configure tool'})).toBeInTheDocument();
    });

    it('drops requiresApproval from the parameters map when the switch is turned off', async () => {
        const user = userEvent.setup();

        wrap(
            <AgentToolsCard
                agentId="agent-1"
                elements={[{...toolElement, parameters: {...toolElement.parameters, requiresApproval: true}}]}
            />
        );

        const row = screen.getByRole('listitem', {name: 'stripe / createRefund'});

        await user.click(within(row).getByRole('switch', {name: 'Requires approval'}));

        expect(updateAgentElementMutate).toHaveBeenCalledWith({
            input: {
                id: 'tool-1',
                parameters: {
                    actionName: 'createRefund',
                    componentName: 'stripe',
                    componentVersion: 1,
                    parameters: {amount: 100},
                },
            },
        });
    });
});
