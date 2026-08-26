import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentModelCard from './AgentModelCard';

const {addAgentElementMutate, deleteAgentElementMutate, updateAgentElementMutate} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
    updateAgentElementMutate: vi.fn(),
}));

// The summary row resolves its label from provider-definition and catalog queries of its own, and the dialog
// drags in the whole shared component-config surface. These tests are about this card's mutation
// choreography, so both are stubbed down to what the card actually consumes: a way in, and a save that
// reports a complete configuration.
vi.mock('@/pages/automation/agents/components/detail/AgentModelSummary', () => ({
    default: ({onClick}: {onClick: () => void}) => (
        <button onClick={onClick} type="button">
            Configure model
        </button>
    ),
}));

vi.mock('@/pages/automation/agents/components/detail/AgentModelDialog', () => ({
    default: ({
        onSubmit,
    }: {
        onSubmit: (values: {
            connectionId: string | null;
            model: string;
            parameters: Record<string, unknown>;
            provider: string;
        }) => void;
    }) => (
        <div>
            <button
                onClick={() => onSubmit({connectionId: null, model: 'gpt-4o', parameters: {}, provider: 'openAi'})}
                type="button"
            >
                save-openai-without-connection
            </button>

            <button
                onClick={() =>
                    onSubmit({connectionId: null, model: 'claude-opus-4-1', parameters: {}, provider: 'anthropic'})
                }
                type="button"
            >
                save-anthropic
            </button>
        </div>
    ),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
    useUpdateAiAgentElementMutation: () => ({isPending: false, mutate: updateAgentElementMutate}),
}));

const OPEN_AI_ELEMENT = {
    connectionId: 'conn-1',
    id: 'model-element-1',
    kind: 'MODEL',
    parameters: {model: 'gpt-4o', provider: 'openAi'},
    position: 0,
    referenceId: null,
};

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    // main.tsx mounts a TooltipProvider at the app root; the connection label's tooltip needs one here too.
    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>{ui}</TooltipProvider>
        </QueryClientProvider>
    );
};

const openDialog = async (user: ReturnType<typeof userEvent.setup>) =>
    user.click(screen.getByRole('button', {name: 'Configure model'}));

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    updateAgentElementMutate.mockReset();
});

describe('AgentModelCard', () => {
    // The dialog only reports on Save, and never without both halves, so a submit is always a complete
    // configuration — there is no half-configured state for the card to defer around.
    it('creates the MODEL element from a dialog save carrying both provider and model', async () => {
        const user = userEvent.setup({pointerEventsCheck: 0});

        wrap(<AgentModelCard agentId="agent-1" elements={[]} />);

        await openDialog(user);
        await user.click(await screen.findByRole('button', {name: 'save-openai-without-connection'}));

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

        wrap(<AgentModelCard agentId="agent-1" elements={[OPEN_AI_ELEMENT]} />);

        await openDialog(user);
        await user.click(await screen.findByRole('button', {name: 'save-anthropic'}));

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

        wrap(<AgentModelCard agentId="agent-1" elements={[OPEN_AI_ELEMENT]} />);

        await openDialog(user);
        await user.click(await screen.findByRole('button', {name: 'save-openai-without-connection'}));

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
