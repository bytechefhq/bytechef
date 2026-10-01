import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {userEvent} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectAgentCreationActions from './ProjectAgentCreationActions';

const mockAgentDialog = vi.fn();

vi.mock('@/pages/automation/agents/components/AgentDialog', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: (props: any) => {
        mockAgentDialog(props);

        return <div role="dialog">AgentDialog</div>;
    },
}));

const mockImportAgentMutate = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useImportAiAgentMutation: (options: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: unknown) => {
            mockImportAgentMutate(variables);
            options.onSuccess?.();
        },
    }),
}));

vi.mock('sonner', () => ({toast: {error: vi.fn(), success: vi.fn()}}));

const renderWithProviders = (ui: ReactElement) => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <MemoryRouter>{ui}</MemoryRouter>
            </TooltipProvider>
        </QueryClientProvider>
    );
};

const project = {id: 2, workspaceId: 10} as Project;

describe('ProjectAgentCreationActions', () => {
    it('renders the full-size Create Agent button for the empty-state placement', () => {
        renderWithProviders(<ProjectAgentCreationActions placement="emptyState" project={project} />);

        expect(screen.getByRole('button', {name: 'Create Agent'})).toHaveTextContent('Create Agent');
    });

    it('renders the compact + Agent button for the tab-row placement', () => {
        renderWithProviders(<ProjectAgentCreationActions placement="tabRow" project={project} />);

        expect(screen.getByRole('button', {name: 'Create Agent'})).toHaveTextContent('Agent');
    });

    it('opens AgentDialog locked to this project when Create Agent is clicked', async () => {
        renderWithProviders(<ProjectAgentCreationActions placement="emptyState" project={project} />);

        fireEvent.click(screen.getByRole('button', {name: 'Create Agent'}));

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

        const lastCall = mockAgentDialog.mock.calls[mockAgentDialog.mock.calls.length - 1][0];

        expect(lastCall.projectId).toBe(2);
    });

    it('imports the selected file locked to this project and workspace', async () => {
        renderWithProviders(<ProjectAgentCreationActions placement="emptyState" project={project} />);

        await userEvent.click(screen.getByRole('button', {name: 'More Agent Creation Actions'}));
        await userEvent.click(await screen.findByText('Import Agent'));

        const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement;

        expect(fileInput).toBeTruthy();

        const file = new File(['{"title":"Imported"}'], 'agent.json', {type: 'application/json'});

        fireEvent.change(fileInput, {target: {files: [file]}});

        await waitFor(() =>
            expect(mockImportAgentMutate).toHaveBeenCalledWith(
                expect.objectContaining({projectId: '2', workspaceId: '10'})
            )
        );
    });
});
