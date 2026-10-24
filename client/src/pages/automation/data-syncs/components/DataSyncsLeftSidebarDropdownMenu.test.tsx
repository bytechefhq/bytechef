import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import DataSyncsLeftSidebarDropdownMenu from './DataSyncsLeftSidebarDropdownMenu';

vi.mock('@/pages/automation/data-syncs/components/DataSyncDialog', () => ({default: () => null}));

const mockDeleteDataSyncMutate = vi.fn();

vi.mock('@/shared/middleware/graphql', () => ({
    useDeleteDataSyncMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string}) => {
            mockDeleteDataSyncMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => mockNavigate,
}));

const dataSync = {description: null, id: 'sync-1', title: 'CRM to DB'};

const renderMenu = (current = false) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <DataSyncsLeftSidebarDropdownMenu current={current} dataSync={dataSync} />
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const openMenuAndClickDelete = async () => {
    await userEvent.click(screen.getByRole('button', {name: 'CRM to DB menu'}));
    await userEvent.click(screen.getByRole('menuitem', {name: 'Delete'}));
};

describe('DataSyncsLeftSidebarDropdownMenu', () => {
    beforeEach(() => {
        mockDeleteDataSyncMutate.mockReset();
        mockNavigate.mockReset();
    });

    it('opens the confirmation dialog without deleting the data sync', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        expect(screen.getByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(screen.getByText(/permanently delete the data sync CRM to DB/)).toBeInTheDocument();
        expect(mockDeleteDataSyncMutate).not.toHaveBeenCalled();
    });

    it('deletes the data sync once the confirmation dialog is confirmed', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Data Sync Deletion'}));

        expect(mockDeleteDataSyncMutate).toHaveBeenCalledWith({id: 'sync-1'});
    });

    it('leaves the deleted data sync for the projects data syncs surface when it is the open one', async () => {
        renderMenu(true);

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Data Sync Deletion'}));

        expect(mockNavigate).toHaveBeenCalledWith('/automation/projects?dataSyncs=all');
    });

    it('stays put when the deleted data sync is another row', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Data Sync Deletion'}));

        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('does not delete the data sync when the confirmation dialog is cancelled', async () => {
        renderMenu();

        await openMenuAndClickDelete();

        fireEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(mockDeleteDataSyncMutate).not.toHaveBeenCalled();
        expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument();
    });
});
