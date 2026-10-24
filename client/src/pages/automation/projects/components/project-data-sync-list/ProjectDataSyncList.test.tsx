import {TooltipProvider} from '@/components/ui/tooltip';
import {Project} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {ReactElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectDataSyncList from './ProjectDataSyncList';

vi.mock('@/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu', () => ({
    default: () => null,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/data-syncs/hooks/useDataSyncs', () => ({
    default: () => ({
        dataSyncs: [
            {
                elements: [],
                id: 'ds1',
                lastPublishedVersion: 0,
                projectId: '1',
                title: 'CRM Sync',
                triggerParameters: {},
                triggerType: 'MANUAL',
            },
            {
                elements: [],
                id: 'ds2',
                lastPublishedVersion: 0,
                projectId: '5',
                title: 'Other Project Sync',
                triggerParameters: {},
                triggerType: 'MANUAL',
            },
        ],
    }),
}));

vi.mock('@/pages/automation/projects/components/project-data-sync-list/ProjectDataSyncCreationActions', () => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: ({placement}: any) => <button aria-label="Create Data Sync">Create Data Sync ({placement})</button>,
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

const projectWithDataSync = {id: 1, workspaceId: 10} as Project;
const projectWithoutDataSyncs = {id: 2, workspaceId: 10} as Project;

describe('ProjectDataSyncList', () => {
    it('lists the data syncs without its own create button when the project has data syncs (the tab row has one)', () => {
        renderWithProviders(<ProjectDataSyncList project={projectWithDataSync} />);

        expect(screen.getByText('CRM Sync')).toBeInTheDocument();
        expect(screen.queryByText('Other Project Sync')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Data Sync'})).not.toBeInTheDocument();
    });

    it("links each data sync row to the sync's project-scoped page", () => {
        renderWithProviders(<ProjectDataSyncList project={projectWithDataSync} />);

        expect(screen.getByText('CRM Sync').closest('a')).toHaveAttribute(
            'href',
            '/automation/projects/1/data-syncs/ds1'
        );
    });

    it('shows the create-data-sync action inside the empty state when the project has no data syncs', () => {
        renderWithProviders(<ProjectDataSyncList project={projectWithoutDataSyncs} />);

        expect(screen.getByText('No data syncs in this project')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Data Sync'})).toHaveTextContent('emptyState');
    });
});
