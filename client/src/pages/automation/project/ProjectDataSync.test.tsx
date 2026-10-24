import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectsLeftSidebarStore from '@/pages/automation/project/stores/useProjectsLeftSidebarStore';
import {ProjectStatus} from '@/shared/middleware/automation/configuration';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import ProjectDataSync from './ProjectDataSync';

const {mockProjectItemSelect, mockProjectsLeftSidebar} = vi.hoisted(() => ({
    mockProjectItemSelect: vi.fn(),
    mockProjectsLeftSidebar: vi.fn(),
}));

vi.mock('react-router-dom', () => ({
    useNavigate: () => vi.fn(),
    useParams: () => ({dataSyncId: '10', projectId: '7'}),
}));

vi.mock('@/pages/automation/project/components/projects-sidebar/ProjectsLeftSidebar', () => ({
    default: (props: {currentDataSyncId?: string; projectId: number}) => {
        mockProjectsLeftSidebar(props);

        return <aside data-testid="projects-sidebar">{props.projectId}</aside>;
    },
}));

vi.mock('@/pages/automation/data-syncs/components/wizard/DataSyncWizard', () => ({
    default: ({dataSync}: {dataSync: {id: string}}) => <div data-testid="data-sync-wizard">{dataSync.id}</div>,
}));

vi.mock('@/pages/automation/data-syncs/components/detail/DataSyncDetailHeader', () => ({
    default: ({leading}: {leading?: ReactNode}) => <header>{leading}</header>,
}));

vi.mock('@/pages/automation/project/components/project-header/components/ProjectItemSelect', () => ({
    default: (props: {currentDataSyncId: string; currentLabel: string; projectId: number}) => {
        mockProjectItemSelect(props);

        return <span data-testid="project-item-select">{props.currentLabel}</span>;
    },
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetProjectQuery: () => ({
        data: {id: 7, lastProjectVersion: 3, lastStatus: ProjectStatus.Draft, name: 'Support Project', workspaceId: 1},
    }),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/shared/layout/LeftSidebarButton', () => ({
    default: ({onLeftSidebarOpenClick}: {onLeftSidebarOpenClick: () => void}) => (
        <button aria-label="Toggle project sidebar" onClick={onLeftSidebarOpenClick} />
    ),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDataSyncQuery: () => ({
        data: {
            dataSync: {
                elements: [],
                id: '10',
                lastPublishedVersion: 0,
                projectId: '7',
                title: 'CRM to DB',
            },
        },
    }),
}));

const renderProjectDataSync = () => render(<ProjectDataSync />, {wrapper: TooltipProvider});

describe('ProjectDataSync', () => {
    it('renders the data sync wizard inside the project layout', () => {
        renderProjectDataSync();

        expect(screen.getByTestId('projects-sidebar')).toHaveTextContent('7');
        expect(screen.getByTestId('data-sync-wizard')).toHaveTextContent('10');
    });

    it('opens the project sidebar on the browsed data sync, in a 355px rail', () => {
        renderProjectDataSync();

        expect(mockProjectsLeftSidebar).toHaveBeenCalledWith(
            expect.objectContaining({currentDataSyncId: '10', currentWorkflowId: '', projectId: 7})
        );
        expect(screen.getByTestId('projects-sidebar').parentElement).toHaveClass('w-[355px]');
    });

    it('gives the wizard the full width below the header', () => {
        renderProjectDataSync();

        expect(screen.getByTestId('data-sync-wizard').parentElement).toHaveClass('flex-1');
    });

    it('renders an item switcher for the header title, scoped to the data sync and its project', () => {
        renderProjectDataSync();

        expect(screen.getByTestId('project-item-select')).toHaveTextContent('CRM to DB');
        expect(mockProjectItemSelect).toHaveBeenCalledWith(
            expect.objectContaining({currentDataSyncId: '10', currentLabel: 'CRM to DB', projectId: 7})
        );
    });

    it('shows the project name and project version, and no data-sync-only version badge', () => {
        renderProjectDataSync();

        expect(screen.getByText('Support Project')).toBeInTheDocument();
        expect(screen.getByText('V3')).toBeInTheDocument();

        expect(screen.getAllByText('DRAFT')).toHaveLength(1);
    });

    it('puts the sidebar toggle and the project breadcrumb before the data sync switcher', () => {
        useProjectsLeftSidebarStore.setState({projectLeftSidebarOpen: false});

        renderProjectDataSync();

        expect(screen.getByText('Support Project')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Toggle project sidebar'}));

        expect(useProjectsLeftSidebarStore.getState().projectLeftSidebarOpen).toBe(true);
    });
});
