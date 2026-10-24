import {TooltipProvider} from '@/components/ui/tooltip';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {fireEvent, render, screen} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import DataSyncDetailHeader from './DataSyncDetailHeader';

const {deleteDataSyncMutate, deleteDataSyncOptions, navigateMock, projectWorkflowsState, publishProjectMutate} =
    vi.hoisted(() => ({
        deleteDataSyncMutate: vi.fn(),
        deleteDataSyncOptions: {value: {onSuccess: () => {}}},
        navigateMock: vi.fn(),
        projectWorkflowsState: {value: [] as {projectWorkflowId: number}[]},
        publishProjectMutate: vi.fn(),
    }));

vi.mock('@/pages/automation/data-syncs/components/DataSyncDialog', () => ({default: () => null}));
vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: () => null,
}));
vi.mock('@/pages/automation/project/components/ProjectVersionHistorySheet', () => ({
    default: ({projectVersions, title}: {projectVersions: {version: number}[]; title: string}) => (
        <div data-testid="version-history-sheet">
            {title}: {projectVersions.map((projectVersion) => `V${projectVersion.version}`).join(', ')}
        </div>
    ),
}));
vi.mock('@/pages/automation/project/components/project-header/components/PublishPopover', () => ({
    default: ({
        onPublishProjectSubmit,
        title,
    }: {
        onPublishProjectSubmit: ({description, onSuccess}: {description?: string; onSuccess: () => void}) => void;
        title: string;
    }) => (
        <button onClick={() => onPublishProjectSubmit({description: 'First release', onSuccess: () => {}})}>
            {title}
        </button>
    ),
}));

const mockSettingsMenu = vi.fn();

vi.mock('@/pages/automation/project/components/project-header/components/settings-menu/SettingsMenu', () => ({
    default: (props: {
        firstTab?: {
            ariaLabel: string;
            content: (onCloseDropdownMenu: () => void) => ReactNode;
            label: string;
            value: string;
        };
        project: {id?: number};
    }) => {
        mockSettingsMenu(props);

        return <div data-testid="settings-menu" />;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: vi.fn((selector) => selector({currentEnvironmentId: 123})),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useDataSyncVersionsQuery: () => ({
        data: {
            dataSyncVersions: [
                {description: 'First release', publishedDate: '2026-09-01T00:00:00Z', status: 'PUBLISHED', version: 1},
                {description: null, publishedDate: null, status: 'DRAFT', version: 2},
            ],
        },
    }),
    useDeleteDataSyncMutation: (options: {onSuccess: () => void}) => {
        deleteDataSyncOptions.value = options;

        return {isPending: false, mutate: deleteDataSyncMutate};
    },
}));

vi.mock('@/shared/mutations/automation/projects.mutations', () => ({
    usePublishProjectMutation: () => ({isPending: false, mutate: publishProjectMutate}),
}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectWorkflowsQuery: () => ({data: projectWorkflowsState.value}),
}));

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');

    return {...actual, useNavigate: () => navigateMock};
});

const wrap = (ui: ReactNode) => {
    const queryClient = new QueryClient({defaultOptions: {mutations: {retry: false}, queries: {retry: false}}});

    return render(
        <QueryClientProvider client={queryClient}>
            <MemoryRouter>
                <TooltipProvider>{ui}</TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );
};

const renderHeader = (lastPublishedVersion: number, project?: {id: number; name: string; workspaceId: number}) =>
    wrap(
        <DataSyncDetailHeader
            id="10"
            lastPublishedVersion={lastPublishedVersion}
            project={project}
            projectId="7"
            title="CRM to DB"
        />
    );

const openDataSyncTab = () => {
    const {firstTab} = mockSettingsMenu.mock.calls.at(-1)![0] as {
        firstTab: {content: (onCloseDropdownMenu: () => void) => ReactNode};
    };

    wrap(firstTab.content(vi.fn()));
};

describe('DataSyncDetailHeader', () => {
    beforeEach(() => {
        deleteDataSyncMutate.mockClear();
        navigateMock.mockClear();
        projectWorkflowsState.value = [];
    });

    it('does not render a data-sync-only version badge', () => {
        renderHeader(3);

        expect(screen.queryByText('DRAFT')).not.toBeInTheDocument();
        expect(screen.queryByText('V4')).not.toBeInTheDocument();
    });

    it('shows a settings menu with a Data Sync tab once the project has loaded', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        expect(screen.getByTestId('settings-menu')).toBeInTheDocument();
        expect(mockSettingsMenu).toHaveBeenCalledWith(
            expect.objectContaining({
                firstTab: expect.objectContaining({
                    ariaLabel: 'Data Sync tab',
                    label: 'Data Sync',
                    value: 'dataSync',
                }),
                project: {id: 7, name: 'Support Project', workspaceId: 1},
            })
        );
    });

    it('does not render the settings menu before the project has loaded', () => {
        renderHeader(0);

        expect(screen.queryByTestId('settings-menu')).not.toBeInTheDocument();
    });

    it('no longer renders its own data sync menu', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        expect(screen.queryByLabelText('Data Sync menu')).not.toBeInTheDocument();
    });

    it('renders no Run now control', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        expect(screen.queryByRole('button', {name: /run now/i})).not.toBeInTheDocument();
    });

    it('offers Edit and Delete in the settings menu Data Sync tab', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        openDataSyncTab();

        expect(screen.getByRole('button', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Delete'})).toBeInTheDocument();
    });

    it('wires the settings menu Data Sync tab to delete the data sync, guarded by a confirmation dialog', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        openDataSyncTab();

        fireEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(deleteDataSyncMutate).not.toHaveBeenCalled();
        expect(screen.getByText(/This will permanently delete the data sync CRM to DB/)).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Confirm Data Sync Deletion'}));

        expect(deleteDataSyncMutate).toHaveBeenCalledWith({id: '10'});
    });

    it('navigates to the project first workflow after the deleted sync page is gone', () => {
        projectWorkflowsState.value = [{projectWorkflowId: 41}, {projectWorkflowId: 42}];

        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        deleteDataSyncOptions.value.onSuccess();

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects/7/project-workflows/41');
    });

    it('navigates to the projects list when the project has no workflow left', () => {
        projectWorkflowsState.value = [];

        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        deleteDataSyncOptions.value.onSuccess();

        expect(navigateMock).toHaveBeenCalledWith('/automation/projects');
    });

    it('opens the data sync history sheet from the Data Sync tab, filled from the sync versions', () => {
        renderHeader(0, {id: 7, name: 'Support Project', workspaceId: 1});

        openDataSyncTab();

        expect(screen.queryByTestId('version-history-sheet')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Data Sync History'}));

        expect(screen.getByTestId('version-history-sheet')).toHaveTextContent('Data Sync History: V1, V2');
    });

    it('publishes the data sync project through the project publish', () => {
        renderHeader(0);

        fireEvent.click(screen.getByRole('button', {name: 'Publish Project'}));

        expect(publishProjectMutate).toHaveBeenCalledWith(
            {id: 7, publishProjectRequest: {description: 'First release'}},
            expect.objectContaining({onSuccess: expect.any(Function)})
        );
    });

    it('enables Deploy only once the project has a published version', () => {
        renderHeader(0);

        expect(screen.getByRole('button', {name: 'Deploy'})).toBeDisabled();

        renderHeader(2);

        expect(screen.getAllByRole('button', {name: 'Deploy'}).at(-1)).toBeEnabled();
    });
});
