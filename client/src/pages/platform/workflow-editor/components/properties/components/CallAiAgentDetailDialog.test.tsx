import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import CallAiAgentDetailDialog from './CallAiAgentDetailDialog';

const {invalidateAgentQueries, invalidateQueries, mockUseAiAgentQuery} = vi.hoisted(() => ({
    invalidateAgentQueries: vi.fn(),
    invalidateQueries: vi.fn(),
    mockUseAiAgentQuery: vi.fn(),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries}),
}));

vi.mock('@/pages/automation/agents/utils/invalidateAgentQueries', () => ({
    default: invalidateAgentQueries,
}));

vi.mock('@/pages/automation/agents/AgentDetailContent', () => ({
    default: ({agentId}: {agentId: string}) => <div data-testid="agent-detail-content">{agentId}</div>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useAiAgentQuery: mockUseAiAgentQuery,
}));

const renderDialog = (onOpenChange = vi.fn()) =>
    render(
        <MemoryRouter>
            <CallAiAgentDetailDialog agentId="22" agentTitle="Billing Agent" onOpenChange={onOpenChange} open />
        </MemoryRouter>
    );

describe('CallAiAgentDetailDialog', () => {
    beforeEach(() => {
        mockUseAiAgentQuery.mockReturnValue({data: {aiAgent: {id: '22', projectId: '7'}}});
    });

    it('should mount the editable agent builder on the given agent id', () => {
        renderDialog();

        expect(screen.getByTestId('agent-detail-content')).toHaveTextContent('22');
        expect(screen.getByText('Billing Agent')).toBeInTheDocument();
    });

    it('should link to the routed agent page', () => {
        renderDialog();

        expect(screen.getByRole('link', {name: /open in full view/i})).toHaveAttribute(
            'href',
            '/automation/projects/7/agents/22'
        );
    });

    it('should render no link until the agent loads', () => {
        mockUseAiAgentQuery.mockReturnValue({data: undefined});

        renderDialog();

        expect(screen.queryByRole('link', {name: /open in full view/i})).not.toBeInTheDocument();
    });

    it('should invalidate both the node options and the agent queries on close', async () => {
        invalidateQueries.mockClear();
        invalidateAgentQueries.mockClear();

        const onOpenChange = vi.fn();

        renderDialog(onOpenChange);

        await userEventEscape();

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['workflowNodeOptions']});
        expect(invalidateAgentQueries).toHaveBeenCalled();
        expect(onOpenChange).toHaveBeenCalledWith(false);
    });
});

async function userEventEscape() {
    const {default: userEvent} = await import('@testing-library/user-event');

    await userEvent.keyboard('{Escape}');
}
