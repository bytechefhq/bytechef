import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AiHubAiAgentViewer from '../AiHubAiAgentViewer';

vi.mock('@/pages/automation/agents/AgentDetailContent', () => ({
    default: ({agentId}: {agentId: string}) => <div data-agent-id={agentId} data-testid="agent-detail-content" />,
}));

const wrap = (ui: React.ReactNode) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe('AiHubAiAgentViewer', () => {
    it('renders the agent name and Open in full view link with the correct href', () => {
        wrap(<AiHubAiAgentViewer aiAgentId="agent-42" name="Support Agent" />);

        expect(screen.getByText('Support Agent')).toBeInTheDocument();

        const link = screen.getByRole('link', {name: /open in full view/i});

        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/automation/agents/agent-42');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('renders AgentDetailContent with the correct agentId', () => {
        wrap(<AiHubAiAgentViewer aiAgentId="agent-42" name="Support Agent" />);

        const content = screen.getByTestId('agent-detail-content');

        expect(content).toBeInTheDocument();
        expect(content).toHaveAttribute('data-agent-id', 'agent-42');
    });
});
