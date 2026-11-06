import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectDeploymentsPanel from '@/pages/automation/project/components/ProjectDeploymentsPanel';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen, within} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    error: null as Error | null,
    isLoading: false,
    navigate: vi.fn(),
    projectDeployments: [] as ProjectDeployment[] | undefined,
    refetch: vi.fn(),
    savedEnvironmentId: 0,
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({
        data: hoisted.projectDeployments,
        error: hoisted.error,
        isFetching: false,
        isLoading: hoisted.isLoading,
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
        expandAll,
        newlyCreatedDeploymentId,
        projectDeployments,
    }: {
        expandAll?: boolean;
        newlyCreatedDeploymentId?: number;
        projectDeployments: ProjectDeployment[];
    }) => (
        <ul data-expand-all={String(!!expandAll)} data-testid="project-deployment-list">
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

const onProjectDeploymentSaved = vi.fn();

const project = {id: 7, lastProjectVersion: 3, lastPublishedDate: new Date(), name: 'Orders'} as Project;

const renderProjectDeploymentsPanel = (
    expandedProjectDeploymentId?: number,
    renderedProject: Project = project,
    expandAll = false
) =>
    render(
        <TooltipProvider>
            <ProjectDeploymentsPanel
                enabled
                expandAll={expandAll}
                expandedProjectDeploymentId={expandedProjectDeploymentId}
                onProjectDeploymentSaved={onProjectDeploymentSaved}
                project={renderedProject}
            />
        </TooltipProvider>
    );

describe('ProjectDeploymentsPanel', () => {
    beforeEach(() => {
        hoisted.error = null;
        hoisted.isLoading = false;
        hoisted.navigate.mockReset();
        hoisted.refetch.mockReset();
        hoisted.projectDeployments = [
            {enabled: true, environmentId: 0, id: 1, name: 'Orders Dev', projectVersion: 3},
            {enabled: false, environmentId: 2, id: 2, name: 'Orders Prod', projectVersion: 2},
        ];
        hoisted.savedEnvironmentId = 0;

        onProjectDeploymentSaved.mockReset();

        useWorkspaceStore.setState({currentWorkspaceId: 1});
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('shows a single skeleton card however many deployments are known', () => {
        hoisted.isLoading = true;
        hoisted.projectDeployments = undefined;

        renderProjectDeploymentsPanel(undefined, {...project, developmentDeploymentCount: 2} as Project);

        expect(screen.getAllByRole('status', {name: 'Loading deployments'})).toHaveLength(1);
    });

    it('shows a skeleton card while deployments of an environment without a known count load', () => {
        hoisted.isLoading = true;
        hoisted.projectDeployments = undefined;

        environmentStore.setState({currentEnvironmentId: 2});

        renderProjectDeploymentsPanel(undefined, {...project, developmentDeploymentCount: 0} as Project);

        expect(screen.getAllByRole('status', {name: 'Loading deployments'})).toHaveLength(1);
    });

    it('shows a skeleton workflow row with a run button per project workflow when deployments render expanded', () => {
        hoisted.isLoading = true;
        hoisted.projectDeployments = undefined;

        renderProjectDeploymentsPanel(
            undefined,
            {...project, developmentDeploymentCount: 2, projectWorkflowIds: [1, 2]} as Project,
            true
        );

        expect(within(screen.getByRole('list', {name: 'Loading workflows'})).getAllByRole('listitem')).toHaveLength(2);
        expect(screen.getAllByTestId('workflow-run-button-skeleton')).toHaveLength(2);
    });

    it('shows no skeleton workflow rows when deployments render collapsed', () => {
        hoisted.isLoading = true;
        hoisted.projectDeployments = undefined;

        renderProjectDeploymentsPanel(undefined, {
            ...project,
            developmentDeploymentCount: 2,
            projectWorkflowIds: [1, 2],
        } as Project);

        expect(screen.getByRole('status', {name: 'Loading deployments'})).toBeInTheDocument();
        expect(screen.queryByRole('list', {name: 'Loading workflows'})).not.toBeInTheDocument();
    });

    it('shows no skeleton while the deployments of a project without deployments load', () => {
        hoisted.isLoading = true;
        hoisted.projectDeployments = undefined;

        renderProjectDeploymentsPanel(undefined, {...project, developmentDeploymentCount: 0} as Project);

        expect(screen.queryByRole('status', {name: 'Loading deployments'})).not.toBeInTheDocument();
    });

    it('shows only the deployments of the current environment', () => {
        renderProjectDeploymentsPanel();

        expect(screen.getByText('Orders Dev')).toBeInTheDocument();
        expect(screen.queryByText('Orders Prod')).not.toBeInTheDocument();
        expect(screen.getByText('Workflow Executions Sheet')).toBeInTheDocument();
    });

    it('passes expandAll through to the list', () => {
        render(
            <TooltipProvider>
                <ProjectDeploymentsPanel
                    enabled
                    expandAll
                    onProjectDeploymentSaved={onProjectDeploymentSaved}
                    project={project}
                />
            </TooltipProvider>
        );

        expect(screen.getByTestId('project-deployment-list')).toHaveAttribute('data-expand-all', 'true');
    });

    it('expands the given deployment', () => {
        renderProjectDeploymentsPanel(1);

        expect(screen.getByText('Orders Dev expanded')).toBeInTheDocument();
    });

    it('reports a development deployment created from the empty state', () => {
        hoisted.projectDeployments = [];

        renderProjectDeploymentsPanel();

        fireEvent.click(screen.getByText('Save deployment'));

        expect(onProjectDeploymentSaved).toHaveBeenCalledWith(3);
    });

    it('asks to publish an unpublished project', () => {
        hoisted.projectDeployments = [];

        render(
            <TooltipProvider>
                <ProjectDeploymentsPanel
                    enabled
                    onProjectDeploymentSaved={onProjectDeploymentSaved}
                    project={{...project, lastPublishedDate: undefined}}
                />
            </TooltipProvider>
        );

        expect(screen.getByText('Publish the project to deploy it')).toBeInTheDocument();
    });
});
