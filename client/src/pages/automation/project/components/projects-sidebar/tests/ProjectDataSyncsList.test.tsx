import {TooltipProvider} from '@/components/ui/tooltip';
import useWorkflowDataStore from '@/pages/platform/workflow-editor/stores/useWorkflowDataStore';
import {DataSyncElementKind} from '@/shared/middleware/graphql';
import {render, screen} from '@testing-library/react';
import {ComponentProps} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDataSyncsList from '../components/ProjectDataSyncsList';

vi.mock('@/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu', () => ({
    default: ({current, dataSync}: {current: boolean; dataSync: {title: string}}) => (
        <button aria-label={`${dataSync.title} menu`} data-current={current} />
    ),
}));

const mockDataSyncs = [
    {
        description: null,
        elements: [
            {componentName: 'postgresql', kind: DataSyncElementKind.Source},
            {componentName: 'salesforce', kind: DataSyncElementKind.Destination},
        ],
        id: '1',
        lastModifiedDate: '2024-01-15T10:00:00',
        projectId: '7',
        title: 'Contacts Sync',
    },
    {
        description: null,
        elements: [],
        id: '2',
        lastModifiedDate: '2024-01-16T10:00:00',
        projectId: '8',
        title: 'Other Project Sync',
    },
];

vi.mock('@/pages/automation/data-syncs/hooks/useDataSyncs', () => ({
    default: () => ({
        dataSyncs: mockDataSyncs,
        dataSyncsError: undefined,
        dataSyncsIsLoading: false,
    }),
}));

const calculateTimeDifference = vi.fn().mockReturnValue('3 days ago');

const renderList = (props: Partial<ComponentProps<typeof ProjectDataSyncsList>> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <ProjectDataSyncsList
                    calculateTimeDifference={calculateTimeDifference}
                    emptyMessage="No data syncs yet."
                    projectId={0}
                    {...props}
                />
            </TooltipProvider>
        </MemoryRouter>
    );

describe('ProjectDataSyncsList', () => {
    beforeEach(() => {
        useWorkflowDataStore.getState().setComponentDefinitions([
            {icon: 'postgresql-icon.svg', name: 'postgresql', title: 'PostgreSQL'},
            {icon: 'salesforce-icon.svg', name: 'salesforce', title: 'Salesforce'},
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
        ] as any);
    });

    afterEach(() => {
        useWorkflowDataStore.getState().setComponentDefinitions([]);
    });

    it('lists only the data syncs of the given project, linking to the project-scoped route', () => {
        renderList({projectId: 7});

        expect(screen.getByRole('link', {name: /Contacts Sync/})).toHaveAttribute(
            'href',
            '/automation/projects/7/data-syncs/1'
        );
        expect(screen.queryByText('Other Project Sync')).not.toBeInTheDocument();
    });

    it('shows data syncs of every project when projectId is 0 (all projects)', () => {
        renderList({projectId: 0});

        expect(screen.getByText('Contacts Sync')).toBeInTheDocument();
        expect(screen.getByText('Other Project Sync')).toBeInTheDocument();
    });

    it('shows the empty message instead of a list when the project has no data syncs', () => {
        renderList({emptyMessage: 'No data syncs in this project.', projectId: 99});

        expect(screen.getByText('No data syncs in this project.')).toBeInTheDocument();
        expect(screen.queryByRole('list')).not.toBeInTheDocument();
    });

    it('gives each row an Edit/Delete menu that knows whether it is the currently open data sync', () => {
        renderList({currentDataSyncId: '1', projectId: 0});

        expect(screen.getByRole('button', {name: 'Contacts Sync menu'})).toHaveAttribute('data-current', 'true');
        expect(screen.getByRole('button', {name: 'Other Project Sync menu'})).toHaveAttribute('data-current', 'false');
    });

    it('shows the source and destination component icons and no leading type icon', () => {
        renderList({projectId: 7});

        const icons = screen.getAllByLabelText('Workflow component icon');

        expect(icons).toHaveLength(2);
        expect(screen.queryByLabelText(/type icon/i)).not.toBeInTheDocument();
    });

    it('renders no badge row for a data sync with no source or destination yet', () => {
        renderList({projectId: 8});

        expect(screen.queryByLabelText('Workflow component icon')).not.toBeInTheDocument();
    });

    it("renders the Edited date from the data sync's lastModifiedDate using the shared date formatting", () => {
        renderList({projectId: 7});

        expect(calculateTimeDifference).toHaveBeenCalledWith('2024-01-15T10:00:00');
        expect(screen.getByText('Edited')).toBeInTheDocument();
        expect(screen.getByText('3 days ago')).toBeInTheDocument();
    });
});
