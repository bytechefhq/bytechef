import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentSubAgentsCard from './AgentSubAgentsCard';

const {addAgentElementMutate, deleteAgentElementMutate} = vi.hoisted(() => ({
    addAgentElementMutate: vi.fn(),
    deleteAgentElementMutate: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAddAiAgentElementMutation: () => ({isPending: false, mutate: addAgentElementMutate}),
    useAiAgentsQuery: vi.fn(),
    useDeleteAiAgentElementMutation: () => ({isPending: false, mutate: deleteAgentElementMutate}),
}));

const {useAiAgentsQuery} = await import('@/shared/middleware/graphql');

const mockUseAgentsQuery = vi.mocked(useAiAgentsQuery);

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
};

beforeEach(() => {
    addAgentElementMutate.mockReset();
    deleteAgentElementMutate.mockReset();
    mockUseAgentsQuery.mockReset();
});

describe('AgentSubAgentsCard', () => {
    it('warns when a selected sub-agent itself has sub-agents', () => {
        mockUseAgentsQuery.mockReturnValue({
            data: {
                aiAgents: [
                    {
                        description: null,
                        elements: [{id: 'nested-element-1', kind: 'SUB_AGENT'}],
                        id: 'agent-2',
                        lastModifiedDate: null,
                        lastPublishedVersion: 0,
                        name: 'triage-agent',
                        title: 'Triage Agent',
                        unpublishedChanges: false,
                    },
                ],
            },
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        wrap(
            <AgentSubAgentsCard
                agentId="agent-1"
                elements={[
                    {
                        connectionId: null,
                        id: 'element-1',
                        kind: 'SUB_AGENT',
                        parameters: {},
                        position: 0,
                        referenceId: 'agent-2',
                    },
                ]}
            />
        );

        expect(screen.getByText(/won't be callable when it runs as a sub-agent/i)).toBeInTheDocument();
    });

    it('does not warn when the selected sub-agent has no sub-agents of its own', () => {
        mockUseAgentsQuery.mockReturnValue({
            data: {
                aiAgents: [
                    {
                        description: null,
                        elements: [],
                        id: 'agent-2',
                        lastModifiedDate: null,
                        lastPublishedVersion: 0,
                        name: 'triage-agent',
                        title: 'Triage Agent',
                        unpublishedChanges: false,
                    },
                ],
            },
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any);

        wrap(
            <AgentSubAgentsCard
                agentId="agent-1"
                elements={[
                    {
                        connectionId: null,
                        id: 'element-1',
                        kind: 'SUB_AGENT',
                        parameters: {},
                        position: 0,
                        referenceId: 'agent-2',
                    },
                ]}
            />
        );

        expect(screen.queryByText(/won't be callable when it runs as a sub-agent/i)).not.toBeInTheDocument();
    });
});
