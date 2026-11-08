import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {describe, expect, it, vi} from 'vitest';

import CallAiAgentDetailLink from './CallAiAgentDetailLink';

const {useAiAgentsQuery} = vi.hoisted(() => ({
    useAiAgentsQuery: vi.fn(),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: vi.fn((selector) => selector({currentWorkspaceId: 7})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentsQuery,
}));

vi.mock('@/pages/platform/workflow-editor/components/properties/components/CallAiAgentDetailDialog', () => ({
    default: ({agentId, agentTitle}: {agentId: string; agentTitle: string}) => (
        <div data-testid="agent-detail-dialog">
            {agentTitle}:{agentId}
        </div>
    ),
}));

const agents = [
    {id: '11', title: 'Triage Agent', uuid: 'uuid-alpha'},
    {id: '22', title: 'Billing Agent', uuid: 'uuid-beta'},
];

describe('CallAiAgentDetailLink', () => {
    it('should link to the agent whose uuid the property holds', () => {
        useAiAgentsQuery.mockReturnValue({data: {aiAgents: agents}});

        render(<CallAiAgentDetailLink agentUuid="uuid-beta" />);

        expect(screen.getByRole('button', {name: /view agent details/i})).toBeInTheDocument();
    });

    it('should render nothing when no agent is selected', () => {
        useAiAgentsQuery.mockReturnValue({data: {aiAgents: agents}});

        render(<CallAiAgentDetailLink agentUuid={undefined} />);

        expect(screen.queryByRole('button', {name: /view agent details/i})).not.toBeInTheDocument();
    });

    it('should render nothing when the uuid matches no agent, as after the agent is deleted', () => {
        useAiAgentsQuery.mockReturnValue({data: {aiAgents: agents}});

        render(<CallAiAgentDetailLink agentUuid="uuid-gone" />);

        expect(screen.queryByRole('button', {name: /view agent details/i})).not.toBeInTheDocument();
    });

    it('should open the dialog on the resolved agent id, not the uuid', async () => {
        useAiAgentsQuery.mockReturnValue({data: {aiAgents: agents}});

        render(<CallAiAgentDetailLink agentUuid="uuid-beta" />);

        expect(screen.queryByTestId('agent-detail-dialog')).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: /view agent details/i}));

        expect(screen.getByTestId('agent-detail-dialog')).toHaveTextContent('Billing Agent:22');
    });
});
