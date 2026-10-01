import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AgentsLeftSidebarDropdownMenu from './AgentsLeftSidebarDropdownMenu';

vi.mock('@/pages/automation/agents/components/AgentDialog', () => ({default: () => null}));

const mockDeleteAiAgentMutate = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteAiAgentMutation: () => ({isPending: false, mutate: mockDeleteAiAgentMutate}),
}));

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => mockNavigate,
}));

const agent = {description: null, id: 'agent-1', title: 'Support Bot'};

const renderMenu = (current = false) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <TooltipProvider>
                    <AgentsLeftSidebarDropdownMenu agent={agent} current={current} />
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const openMenuAndClickDelete = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'Support Bot menu'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
};

describe('AgentsLeftSidebarDropdownMenu', () => {
    beforeEach(() => {
        mockDeleteAiAgentMutate.mockReset();
        mockNavigate.mockReset();
    });

    it('opens the confirmation dialog without deleting the agent', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(screen.getByText(/permanently delete the agent Support Bot/)).toBeInTheDocument();
        expect(mockDeleteAiAgentMutate).not.toHaveBeenCalled();
    });

    it('deletes the agent once the confirmation dialog is confirmed', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Agent Deletion'}));

        expect(mockDeleteAiAgentMutate).toHaveBeenCalledWith({id: 'agent-1'});
    });

    it('does not delete the agent when the confirmation dialog is cancelled', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(mockDeleteAiAgentMutate).not.toHaveBeenCalled();
        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });
});
