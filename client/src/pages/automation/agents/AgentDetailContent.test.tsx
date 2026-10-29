import {useAgentExternalChangeStore} from '@/pages/automation/agents/stores/useAgentExternalChangeStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen} from '@testing-library/react';
import {ReactNode, useState} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentDetailContent from './AgentDetailContent';

const hoisted = vi.hoisted(() => ({
    agentRef: {current: {channels: [], elements: [], id: 'agent-1', instructions: 'Be terse.', settings: null}},
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentQuery: () => ({data: {aiAgent: hoisted.agentRef.current}, error: null, isLoading: false}),
}));

vi.mock('@/pages/automation/agents/components/detail/AgentInstructionsCard', () => ({
    default: ({
        externalChangeRevision,
        instructions,
    }: {
        externalChangeRevision: number;
        instructions: string | null;
    }) => <p>{`Instructions ${instructions} at revision ${externalChangeRevision}`}</p>,
}));

vi.mock('@/pages/automation/agents/components/detail/AgentModelCard', () => ({
    default: function MockAgentModelCard() {
        const [mountedInstructions] = useState(hoisted.agentRef.current.instructions);

        return <p>{`Model card mounted with ${mountedInstructions}`}</p>;
    },
}));

vi.mock('@/pages/automation/agents/components/detail/AgentChannelsCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentKnowledgeBaseCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentScheduleCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentSettingsCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentSkillsCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentSubAgentsCard', () => ({default: () => null}));
vi.mock('@/pages/automation/agents/components/detail/AgentToolsCard', () => ({default: () => null}));

const wrap = (ui: ReactNode) => (
    <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
        {ui}
    </QueryClientProvider>
);

describe('AgentDetailContent', () => {
    beforeEach(() => {
        hoisted.agentRef.current = {...hoisted.agentRef.current, instructions: 'Be terse.'};

        useAgentExternalChangeStore.setState({revision: 0});
    });

    it('keeps the mounted cards when the agent changes through the cards themselves', () => {
        const {rerender} = render(wrap(<AgentDetailContent agentId="agent-1" />));

        hoisted.agentRef.current = {...hoisted.agentRef.current, instructions: 'Be verbose.'};

        rerender(wrap(<AgentDetailContent agentId="agent-1" />));

        expect(screen.getByText('Model card mounted with Be terse.')).toBeInTheDocument();
    });

    it('remounts the cards with the refetched agent after an external change', () => {
        render(wrap(<AgentDetailContent agentId="agent-1" />));

        hoisted.agentRef.current = {...hoisted.agentRef.current, instructions: 'Be verbose.'};

        act(() => {
            useAgentExternalChangeStore.getState().bumpRevision();
        });

        expect(screen.getByText('Model card mounted with Be verbose.')).toBeInTheDocument();
    });

    it('hands the external change revision to the Instructions card instead of remounting it', () => {
        render(wrap(<AgentDetailContent agentId="agent-1" />));

        hoisted.agentRef.current = {...hoisted.agentRef.current, instructions: 'Be verbose.'};

        act(() => {
            useAgentExternalChangeStore.getState().bumpRevision();
        });

        expect(screen.getByText('Instructions Be verbose. at revision 1')).toBeInTheDocument();
    });
});
