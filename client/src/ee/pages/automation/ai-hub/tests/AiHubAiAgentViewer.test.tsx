import {fireEvent, render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import AiHubAiAgentViewer from '../AiHubAiAgentViewer';

vi.mock('@/pages/automation/agents/AgentDetailContent', () => ({
    default: ({agentId}: {agentId: string}) => <div data-agent-id={agentId} data-testid="agent-detail-content" />,
}));

vi.mock('@/pages/automation/agents/components/detail/AgentTestChatPanel', () => ({
    default: ({onClose, workflowId}: {onClose?: () => void; workflowId: string}) => (
        <div data-testid="agent-test-chat-panel" data-workflow-id={workflowId}>
            <button aria-label="Close test panel" onClick={onClose} />
        </div>
    ),
}));

const {mockUseAiAgentQuery} = vi.hoisted(() => ({
    mockUseAiAgentQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentQuery: mockUseAiAgentQuery,
}));

const wrap = (ui: React.ReactNode) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe('AiHubAiAgentViewer', () => {
    it('renders the agent name and Open in full view link with the correct href once the agent loads', () => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {id: '42', projectId: '7'}}});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        expect(screen.getByText('Support Agent')).toBeInTheDocument();

        const link = screen.getByRole('link', {name: /open in full view/i});

        expect(link).toBeInTheDocument();
        expect(link).toHaveAttribute('href', '/automation/projects/7/agents/42');
        expect(link).toHaveAttribute('target', '_blank');
    });

    it('renders no link until the agent loads', () => {
        mockUseAiAgentQuery.mockReturnValue({data: undefined});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        expect(screen.queryByRole('link', {name: /open in full view/i})).not.toBeInTheDocument();
    });

    it('renders AgentDetailContent with the correct agentId', () => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {id: '42', projectId: '7'}}});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        const content = screen.getByTestId('agent-detail-content');

        expect(content).toBeInTheDocument();
        expect(content).toHaveAttribute('data-agent-id', '42');
    });

    it('opens the test chat on the draft workflow over the detail content when Test is clicked', () => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {draftWorkflowId: 'wf-1', id: '42', projectId: '7'}}});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        const content = screen.getByTestId('agent-detail-content');

        fireEvent.click(screen.getByRole('button', {name: 'Test Agent'}));

        expect(screen.getByTestId('agent-test-chat-panel')).toHaveAttribute('data-workflow-id', 'wf-1');
        expect(screen.getByTestId('agent-detail-content')).toBe(content);

        fireEvent.click(screen.getByRole('button', {name: 'Hide Test Agent panel'}));

        expect(screen.queryByTestId('agent-test-chat-panel')).not.toBeInTheDocument();
        expect(screen.getByTestId('agent-detail-content')).toBe(content);
    });

    it('closes the test chat from its own close button', () => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {draftWorkflowId: 'wf-1', id: '42', projectId: '7'}}});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        fireEvent.click(screen.getByRole('button', {name: 'Test Agent'}));
        fireEvent.click(screen.getByRole('button', {name: 'Close test panel'}));

        expect(screen.queryByTestId('agent-test-chat-panel')).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Test Agent'})).toBeInTheDocument();
    });

    it('hides the Test button while the agent has no draft workflow', () => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {id: '42', projectId: '7'}}});

        wrap(<AiHubAiAgentViewer aiAgentId="42" name="Support Agent" />);

        expect(screen.queryByRole('button', {name: 'Test Agent'})).not.toBeInTheDocument();
    });
});
