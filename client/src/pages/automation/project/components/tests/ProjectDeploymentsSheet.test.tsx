import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectDeploymentsSheet from '@/pages/automation/project/components/ProjectDeploymentsSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    error: null as Error | null,
    grantedScopes: ['DEPLOYMENT_CREATE'] as string[],
    navigate: vi.fn(),
    projectDeployments: [] as ProjectDeployment[] | undefined,
    refetch: vi.fn(),
    savedEnvironmentId: 0,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (_workspaceId: number | undefined, scope: string) => hoisted.grantedScopes.includes(scope),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({
        data: hoisted.projectDeployments,
        error: hoisted.error,
        isFetching: false,
        isLoading: false,
        refetch: hoisted.refetch,
    }),
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/automation/projectDeploymentTags.queries', () => ({
    useGetProjectDeploymentTagsQuery: () => ({data: []}),
}));

vi.mock('@/shared/queries/platform/taskDispatcherDefinitions.queries', () => ({
    useGetTaskDispatcherDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-list/ProjectDeploymentList', () => ({
    default: ({
        newlyCreatedDeploymentId,
        projectDeployments,
    }: {
        newlyCreatedDeploymentId?: number;
        projectDeployments: ProjectDeployment[];
    }) => (
        <ul>
            {projectDeployments.map((projectDeployment) => (
                <li key={projectDeployment.id}>
                    {projectDeployment.id === newlyCreatedDeploymentId
                        ? `${projectDeployment.name} expanded`
                        : projectDeployment.name}
                </li>
            ))}
        </ul>
    ),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowExecutionsSheet',
    () => ({
        default: () => <div>Workflow Executions Sheet</div>,
    })
);

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({
        onSuccess,
        projectDeployment,
        triggerNode,
    }: {
        onSuccess: (deploymentId: number, environmentId?: number) => void;
        projectDeployment: ProjectDeployment;
        triggerNode: React.ReactNode;
    }) => (
        <>
            {triggerNode}

            <span>{`Deploy dialog for environment ${projectDeployment.environmentId}`}</span>

            <button
                onClick={() => {
                    hoisted.projectDeployments = [
                        ...(hoisted.projectDeployments ?? []),
                        {
                            enabled: true,
                            environmentId: hoisted.savedEnvironmentId,
                            id: 3,
                            name: 'Orders New',
                            projectVersion: 2,
                        },
                    ];

                    onSuccess(3, hoisted.savedEnvironmentId);
                }}
            >
                Save deployment
            </button>
        </>
    ),
}));

const onSheetOpenChange = vi.fn();

const project = {id: 7, lastProjectVersion: 3, lastPublishedDate: new Date(), name: 'Orders'} as Project;

const renderProjectDeploymentsSheet = () =>
    render(
        <TooltipProvider>
            <ProjectDeploymentsSheet onSheetOpenChange={onSheetOpenChange} project={project} sheetOpen />
        </TooltipProvider>
    );

describe('ProjectDeploymentsSheet', () => {
    beforeEach(() => {
        hoisted.error = null;
        hoisted.grantedScopes = ['DEPLOYMENT_CREATE'];
        hoisted.navigate.mockReset();
        hoisted.refetch.mockReset();
        hoisted.projectDeployments = [
            {enabled: true, environmentId: 0, id: 1, name: 'Orders Dev', projectVersion: 3},
            {enabled: false, environmentId: 2, id: 2, name: 'Orders Prod', projectVersion: 2},
        ];
        hoisted.savedEnvironmentId = 0;

        onSheetOpenChange.mockReset();

        useWorkspaceStore.setState({currentWorkspaceId: 1});
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('shows only the deployments of the current environment', () => {
        renderProjectDeploymentsSheet();

        expect(screen.getByText('Project Deployments')).toBeInTheDocument();
        expect(screen.getByText('Orders Dev')).toBeInTheDocument();
        expect(screen.queryByText('Orders Prod')).not.toBeInTheDocument();
        expect(screen.queryByText('DEVELOPMENT')).not.toBeInTheDocument();
        expect(screen.getByText('Workflow Executions Sheet')).toBeInTheDocument();
    });

    it('expands the given deployment', () => {
        render(
            <TooltipProvider>
                <ProjectDeploymentsSheet
                    expandedProjectDeploymentId={1}
                    onSheetOpenChange={onSheetOpenChange}
                    project={project}
                    sheetOpen
                />
            </TooltipProvider>
        );

        expect(screen.getByText('Orders Dev expanded')).toBeInTheDocument();
    });

    it('opens the deployments page without changing the current environment', () => {
        renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Open in Deployments'));

        expect(environmentStore.getState().currentEnvironmentId).toBe(0);
        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments?projectId=7');
    });

    it('offers to deploy into the current environment when it has no deployments', () => {
        hoisted.projectDeployments = [
            {enabled: false, environmentId: 2, id: 2, name: 'Orders Prod', projectVersion: 2},
        ];

        renderProjectDeploymentsSheet();

        expect(screen.getByText('No deployments in DEVELOPMENT')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
        expect(screen.getByText('Deploy dialog for environment 0')).toBeInTheDocument();
    });

    it('hides the deploy action in the empty state without DEPLOYMENT_CREATE', () => {
        hoisted.grantedScopes = [];
        hoisted.projectDeployments = [];

        renderProjectDeploymentsSheet();

        expect(screen.getByText('No deployments in DEVELOPMENT')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
        expect(screen.queryByText('Deploy dialog for environment 0')).not.toBeInTheDocument();
    });

    it('expands a development deployment created from the empty state', () => {
        hoisted.projectDeployments = [];

        const {rerender} = renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Save deployment'));

        rerender(
            <TooltipProvider>
                <ProjectDeploymentsSheet onSheetOpenChange={onSheetOpenChange} project={project} sheetOpen />
            </TooltipProvider>
        );

        expect(screen.getByText('Orders New expanded')).toBeInTheDocument();
        expect(hoisted.navigate).not.toHaveBeenCalled();
    });

    it('opens the deployments page in the environment of a staging deployment created from the empty state', () => {
        hoisted.projectDeployments = [];
        hoisted.savedEnvironmentId = 1;

        renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Save deployment'));

        expect(environmentStore.getState().currentEnvironmentId).toBe(1);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments?projectId=7', {
            state: {projectDeploymentId: 3},
        });
    });

    it('asks to publish an unpublished project instead of offering to deploy it', () => {
        hoisted.projectDeployments = [];

        render(
            <TooltipProvider>
                <ProjectDeploymentsSheet
                    onSheetOpenChange={onSheetOpenChange}
                    project={{id: 7, name: 'Orders'} as Project}
                    sheetOpen
                />
            </TooltipProvider>
        );

        expect(screen.getByText('Publish the project to deploy it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    });

    it('shows an error with a retry instead of offering to deploy when the deployments fail to load', () => {
        hoisted.error = new Error('Request failed');
        hoisted.projectDeployments = undefined;

        renderProjectDeploymentsSheet();

        expect(screen.getByText('Failed to load deployments')).toBeInTheDocument();
        expect(screen.queryByText('No deployments in DEVELOPMENT')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(hoisted.refetch).toHaveBeenCalledTimes(1);
    });

    it('keeps showing the loaded deployments when a refresh fails', () => {
        hoisted.error = new Error('Request failed');

        renderProjectDeploymentsSheet();

        expect(screen.getByText('Orders Dev')).toBeInTheDocument();
        expect(screen.queryByText('Failed to load deployments')).not.toBeInTheDocument();
    });

    it('refreshes the deployments from the header', () => {
        renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByLabelText('Refresh project deployments'));

        expect(hoisted.refetch).toHaveBeenCalledTimes(1);
    });
});
