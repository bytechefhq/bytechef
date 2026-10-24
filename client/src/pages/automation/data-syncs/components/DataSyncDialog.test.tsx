import DataSyncDialog from '@/pages/automation/data-syncs/components/DataSyncDialog';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {ComponentProps} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {createDataSyncMock, navigateMock, updateDataSyncMock} = vi.hoisted(() => ({
    createDataSyncMock: vi.fn(),
    navigateMock: vi.fn(),
    updateDataSyncMock: vi.fn(),
}));

vi.mock('react-router-dom', async () => ({
    ...(await vi.importActual<typeof import('react-router-dom')>('react-router-dom')),
    useNavigate: () => navigateMock,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useCreateDataSyncMutation: () => ({mutate: createDataSyncMock}),
    useUpdateDataSyncMutation: () => ({mutate: updateDataSyncMock}),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => ({
    useWorkspaceStore: (selector: (state: {currentWorkspaceId: number}) => unknown) =>
        selector({currentWorkspaceId: 1}),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    ProjectKeys: {
        filteredProjects: (filters: {id: number}) => ['projects', filters.id, filters],
        projects: ['projects'],
    },
    useGetWorkspaceProjectsQuery: () => ({data: [{id: 5, name: 'Existing Project'}]}),
}));

const renderDialog = (props: Partial<ComponentProps<typeof DataSyncDialog>> = {}) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <DataSyncDialog onOpenChange={vi.fn()} open={true} {...props} />
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('DataSyncDialog', () => {
    beforeEach(() => {
        createDataSyncMock.mockReset();
        navigateMock.mockReset();
        updateDataSyncMock.mockReset();
    });

    it('offers the project picker when creating without a locked project', () => {
        renderDialog();

        expect(screen.getByText('Project')).toBeInTheDocument();
        expect(screen.getByRole('combobox')).toBeInTheDocument();
    });

    it('does not offer a project picker when opened with a locked project', () => {
        renderDialog({projectId: 7});

        expect(screen.queryByLabelText('Project')).not.toBeInTheDocument();
    });

    it('does not offer a project picker when editing an existing data sync', () => {
        renderDialog({dataSync: {id: '1', title: 'CRM to DB'}});

        expect(screen.queryByLabelText('Project')).not.toBeInTheDocument();
    });

    it('creates the data sync in a new project by default and lands on the project-scoped route', async () => {
        createDataSyncMock.mockImplementation((_variables, options) =>
            options?.onSuccess?.({createDataSync: {id: '99', projectId: '7'}})
        );

        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter data sync title'), {target: {value: 'CRM to DB'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createDataSyncMock).toHaveBeenCalled());

        expect(createDataSyncMock.mock.calls[0][0].input.projectId).toBeUndefined();
        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/data-syncs/99');
    });

    it('sends the locked project id when opened from inside a project', async () => {
        renderDialog({projectId: 7});

        fireEvent.change(screen.getByPlaceholderText('Enter data sync title'), {target: {value: 'CRM to DB'}});
        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createDataSyncMock).toHaveBeenCalled());

        expect(createDataSyncMock.mock.calls[0][0].input.projectId).toBe('7');
    });

    it('sends the picked existing project id instead of creating a new project', async () => {
        renderDialog();

        fireEvent.change(screen.getByPlaceholderText('Enter data sync title'), {target: {value: 'CRM to DB'}});

        fireEvent.click(screen.getByRole('combobox'));
        fireEvent.click(screen.getByRole('option', {name: 'Existing Project'}));

        fireEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(createDataSyncMock).toHaveBeenCalled());

        expect(createDataSyncMock.mock.calls[0][0].input.projectId).toBe('5');
    });
});
