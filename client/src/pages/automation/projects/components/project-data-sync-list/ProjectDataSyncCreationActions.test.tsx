import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectDataSyncCreationActions from './ProjectDataSyncCreationActions';

const mockDataSyncDialog = vi.fn();

vi.mock('@/pages/automation/data-syncs/components/DataSyncDialog', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: (props: any) => {
        mockDataSyncDialog(props);

        return <div role="dialog">DataSyncDialog</div>;
    },
}));

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

describe('ProjectDataSyncCreationActions', () => {
    it('renders the full-size Create Data Sync button for the empty-state placement', () => {
        renderWithProviders(<ProjectDataSyncCreationActions placement="emptyState" project={project} />);

        expect(screen.getByRole('button', {name: 'Create Data Sync'})).toHaveTextContent('Create Data Sync');
    });

    it('renders the compact New Data Sync button for the tab-row placement, with no import dropdown', () => {
        renderWithProviders(<ProjectDataSyncCreationActions placement="tabRow" project={project} />);

        expect(screen.getByRole('button', {name: 'New Data Sync'})).toHaveTextContent('New Data Sync');
        expect(screen.queryByRole('button', {name: /More.*Actions/i})).not.toBeInTheDocument();
    });

    it('opens DataSyncDialog locked to this project when the button is clicked', async () => {
        renderWithProviders(<ProjectDataSyncCreationActions placement="emptyState" project={project} />);

        fireEvent.click(screen.getByRole('button', {name: 'Create Data Sync'}));

        await waitFor(() => expect(screen.getByRole('dialog')).toBeInTheDocument());

        const lastCall = mockDataSyncDialog.mock.calls[mockDataSyncDialog.mock.calls.length - 1][0];

        expect(lastCall.projectId).toBe(2);
    });
});
