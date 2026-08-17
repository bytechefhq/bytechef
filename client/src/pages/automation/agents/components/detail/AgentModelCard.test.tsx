import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeAll, beforeEach, describe, expect, it, vi} from 'vitest';

import AgentModelCard from './AgentModelCard';

beforeAll(() => {
    // Radix Select relies on pointer-capture APIs that jsdom does not implement.
    Element.prototype.hasPointerCapture = vi.fn(() => false);
    Element.prototype.setPointerCapture = vi.fn();
    Element.prototype.releasePointerCapture = vi.fn();
});

const {addAgentElementMutate, deleteAgentElementMutate, getWorkspaceConnectionsQuery, updateAgentElementMutate} =
    vi.hoisted(() => ({
        addAgentElementMutate: vi.fn(),
        deleteAgentElementMutate: vi.fn(),
        getWorkspaceConnectionsQuery: vi.fn().mockReturnValue({data: []}),
        updateAgentElementMutate: vi.fn(),
    }));

// AgentModelPicker resolves providers and models from cluster-element definition queries of its own; these
// tests are about this card's mutation choreography, so it is stubbed down to the one thing the card
// consumes — the (provider, model) pair it reports.
// Same reason as AgentToolsCard's test: the dialog drags in ConnectionDialog and graphql exports this
// module mock does not provide, and the whole file fails to load without this. The stub exposes a submit
// button so the card's own save path -- which still owns the replace-vs-update rules -- stays testable.
vi.mock('@/shared/components/component-config/ComponentConfigDialog', () => ({
    default: ({onSubmit}: {onSubmit: (values: {connectionId: string | null; parameters: object}) => void}) => (
        <button onClick={() => onSubmit({connectionId: null, parameters: {}})} type="button">
            submit-without-connection
        </button>
    ),
}));

vi.mock('@/pages/automation/agents/components/detail/AgentModelPicker', () => ({
    default: ({onChange}: {onChange: (selection: {model: string; provider: string}) => void}) => (
        <div>
            <button onClick={() => onChange({model: 'gpt-4o', provider: 'openAi'})} type="button">
                Pick OpenAI gpt-4o
            </button>

            <button onClick={() => onChange({model: 'claude-opus-4-1', provider: 'anthropic'})} type="button">
                Pick Anthropic claude-opus-4-1
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
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
    useUpdateAiAgentElementMutation: () => ({isPending: false, mutate: updateAgentElementMutate}),
}));

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    // main.tsx mounts a TooltipProvider at the app root; the connection label's tooltip needs one here too.
    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    updateAgentElementMutate.mockReset();
    getWorkspaceConnectionsQuery.mockReset().mockReturnValue({data: []});
});

describe('AgentModelCard', () => {
    // The picker always reports provider and model together, so a single pick is a complete
    // configuration — unlike the old provider-Select + model-Input pair, which had to defer the create
    // until both halves existed.
    it('creates the MODEL element from a single pick carrying both provider and model', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentModelCard agentId="agent-1" elements={[]} />);

        await user.click(screen.getByRole('button', {name: 'Pick OpenAI gpt-4o'}));

        expect(addAgentElementMutate).toHaveBeenCalledTimes(1);
        expect(addAgentElementMutate).toHaveBeenCalledWith(
            {
                input: {
                    agentId: 'agent-1',
                    connectionId: null,
                    kind: 'MODEL',
                    parameters: {model: 'gpt-4o', parameters: {}, provider: 'openAi'},
                },
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('replaces the MODEL element via delete-then-add on a provider switch instead of updating it', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        deleteAgentElementMutate.mockImplementation((_variables, options) => {
            options?.onSuccess?.();
        });

        wrap(
            <AgentModelCard
                agentId="agent-1"
                elements={[
                    {
                        connectionId: 'conn-1',
                        id: 'model-element-1',
                        kind: 'MODEL',
                        parameters: {model: 'gpt-4o', provider: 'openAi'},
                        position: 0,
                        referenceId: null,
                    },
                ]}
            />
        );

        await user.click(screen.getByRole('button', {name: 'Pick Anthropic claude-opus-4-1'}));

        // A provider switch must never update in place: an update leaves a null connectionId
        // untouched server-side (AiAgentFacadeImpl treats it as "leave unchanged"), which would let the
        // old provider's connection stay wired to the new provider's model row.
        expect(updateAgentElementMutate).not.toHaveBeenCalled();
        expect(deleteAgentElementMutate).toHaveBeenCalledWith(
            {id: 'model-element-1'},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(addAgentElementMutate).toHaveBeenCalledWith(
            {
                input: {
                    agentId: 'agent-1',
                    connectionId: null,
                    kind: 'MODEL',
                    parameters: {model: 'claude-opus-4-1', parameters: {}, provider: 'anthropic'},
                },
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('clears a wired connection via delete-then-add rather than a no-op update', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        deleteAgentElementMutate.mockImplementation((_variables, options) => {
            options?.onSuccess?.();
        });

        getWorkspaceConnectionsQuery.mockReturnValue({data: [{id: 'conn-1', name: 'My OpenAI Key'}]});

        wrap(
            <AgentModelCard
                agentId="agent-1"
                elements={[
                    {
                        connectionId: 'conn-1',
                        id: 'model-element-1',
                        kind: 'MODEL',
                        parameters: {model: 'gpt-4o', provider: 'openAi'},
                        position: 0,
                        referenceId: null,
                    },
                ]}
            />
        );

        // The connection is cleared through the config dialog now, not an inline dropdown; the stub above
        // submits with connectionId null.
        await user.click(screen.getByRole('button', {name: 'Configure model'}));
        await user.click(await screen.findByRole('button', {name: 'submit-without-connection'}));

        expect(deleteAgentElementMutate).toHaveBeenCalledWith(
            {id: 'model-element-1'},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
        expect(addAgentElementMutate).toHaveBeenCalledWith(
            {
                input: {
                    agentId: 'agent-1',
                    connectionId: null,
                    kind: 'MODEL',
                    parameters: {model: 'gpt-4o', parameters: {}, provider: 'openAi'},
                },
            },
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });
});
