import {TooltipProvider} from '@/components/ui/tooltip';
import {DataSyncElementKind, DataSyncTriggerType} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import ProjectDataSyncListItem from './ProjectDataSyncListItem';

const hoisted = vi.hoisted(() => ({
    mockDataSyncsLeftSidebarDropdownMenu: vi.fn(() => <button aria-label="data sync menu">menu</button>),
}));

vi.mock('@/pages/automation/data-syncs/components/DataSyncsLeftSidebarDropdownMenu', () => ({
    default: hoisted.mockDataSyncsLeftSidebarDropdownMenu,
}));

vi.mock('react-inlinesvg', () => ({
    default: ({src}: {src: string}) => <span data-src={src} data-testid="component-icon" />,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({
        data: [
            {icon: 'csv.svg', name: 'csvFile', title: 'CSV File'},
            {icon: 'pg.svg', name: 'postgresql', title: 'PostgreSQL'},
        ],
    }),
}));

const scheduledDataSync = {
    elements: [
        {componentName: 'csvFile', kind: DataSyncElementKind.Source},
        {componentName: 'postgresql', kind: DataSyncElementKind.Destination},
    ],
    id: 'ds-1',
    lastPublishedVersion: 0,
    projectId: '1',
    title: 'CRM to DB',
    triggerParameters: {frequencyKind: 'DAILY', timeOfDay: '09:00'},
    triggerType: DataSyncTriggerType.Schedule,
};

const manualDataSync = {
    elements: [],
    id: 'ds-2',
    lastPublishedVersion: 3,
    projectId: '1',
    title: 'Plain Sync',
    triggerParameters: {},
    triggerType: DataSyncTriggerType.Manual,
};

const renderProjectDataSyncListItem = (dataSync: typeof scheduledDataSync | typeof manualDataSync) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <MemoryRouter>
                    <ProjectDataSyncListItem dataSync={dataSync} />
                </MemoryRouter>
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('ProjectDataSyncListItem', () => {
    it('renders the data sync title as a link into its project-scoped page', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        expect(screen.getByText('CRM to DB').closest('a')).toHaveAttribute(
            'href',
            '/automation/projects/1/data-syncs/ds-1'
        );
    });

    it('names the source and destination', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        expect(screen.getByText('CSV File')).toBeInTheDocument();
        expect(screen.getByText('PostgreSQL')).toBeInTheDocument();
    });

    it('shows the component icon belonging to each endpoint', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        const icons = screen.getAllByTestId('component-icon');

        expect(icons.map((icon) => icon.getAttribute('data-src'))).toEqual(['csv.svg', 'pg.svg']);
    });

    it('falls back to placeholder text with no elements', () => {
        renderProjectDataSyncListItem(manualDataSync);

        expect(screen.getByText('No source')).toBeInTheDocument();
        expect(screen.getByText('No destination')).toBeInTheDocument();
    });

    it('renders the trigger summary via describeTrigger', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        expect(screen.getByText('Daily at 09:00')).toBeInTheDocument();
    });

    it('renders Manual for a manually triggered sync', () => {
        renderProjectDataSyncListItem(manualDataSync);

        expect(screen.getByText('Manual')).toBeInTheDocument();
    });

    it('renders the published version badge', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        expect(screen.getByText('DRAFT')).toBeInTheDocument();
        expect(screen.getByText('V1')).toBeInTheDocument();
    });

    it('renders PUBLISHED with the actual version once published', () => {
        renderProjectDataSyncListItem(manualDataSync);

        expect(screen.getByText('PUBLISHED')).toBeInTheDocument();
        expect(screen.getByText('V3')).toBeInTheDocument();
    });

    it('renders the row menu', () => {
        renderProjectDataSyncListItem(scheduledDataSync);

        expect(screen.getByRole('button', {name: 'data sync menu'})).toBeInTheDocument();
        expect(hoisted.mockDataSyncsLeftSidebarDropdownMenu).toHaveBeenCalledWith(
            expect.objectContaining({current: false, dataSync: scheduledDataSync}),
            undefined
        );
    });
});
