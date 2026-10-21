import ProjectDeploymentsSheet from '@/pages/automation/project/components/ProjectDeploymentsSheet';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
    projectDeployments: [] as ProjectDeployment[],
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetWorkspaceProjectDeploymentsQuery: () => ({data: hoisted.projectDeployments, isLoading: false}),
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
                        ...hoisted.projectDeployments,
                        {enabled: true, environmentId: 1, id: 3, name: 'Orders Staging', projectVersion: 2},
                    ];

                    onSuccess(3, 1);
                }}
            >
                Save deployment
            </button>
        </>
    ),
}));

vi.mock('@/shared/components/EnvironmentSelect', () => ({
    default: ({environmentId, onChange}: {environmentId: number; onChange: (environmentId: number) => void}) => (
        <div>
            <span>{`Selected environment ${environmentId}`}</span>

            <button onClick={() => onChange(1)}>Staging</button>

            <button onClick={() => onChange(2)}>Production</button>
        </div>
    ),
}));

const onSheetOpenChange = vi.fn();

const project = {id: 7, lastProjectVersion: 3, lastPublishedDate: new Date(), name: 'Orders'} as Project;

const renderProjectDeploymentsSheet = () =>
    render(<ProjectDeploymentsSheet onSheetOpenChange={onSheetOpenChange} project={project} sheetOpen />);

describe('ProjectDeploymentsSheet', () => {
    beforeEach(() => {
        hoisted.navigate.mockReset();
        hoisted.projectDeployments = [
            {enabled: true, environmentId: 0, id: 1, name: 'Orders Dev', projectVersion: 3},
            {enabled: false, environmentId: 2, id: 2, name: 'Orders Prod', projectVersion: 2},
        ];

        onSheetOpenChange.mockReset();

        useWorkspaceStore.setState({currentWorkspaceId: 1});
        environmentStore.setState({currentEnvironmentId: 0});
    });

    it('shows the deployments of the current environment by default', () => {
        renderProjectDeploymentsSheet();

        expect(screen.getByText('Project Deployments')).toBeInTheDocument();
        expect(screen.getByText('Selected environment 0')).toBeInTheDocument();
        expect(screen.getByText('Orders Dev')).toBeInTheDocument();
        expect(screen.queryByText('Orders Prod')).not.toBeInTheDocument();
        expect(screen.getByText('Workflow Executions Sheet')).toBeInTheDocument();
    });

    it('switches the listed deployments without changing the current environment', () => {
        renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Production'));

        expect(screen.queryByText('Orders Dev')).not.toBeInTheDocument();
        expect(screen.getByText('Orders Prod')).toBeInTheDocument();
        expect(environmentStore.getState().currentEnvironmentId).toBe(0);

        fireEvent.click(screen.getByText('Staging'));

        expect(screen.getByText('No deployments in STAGING')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Deploy'})).toBeInTheDocument();
        expect(screen.getByText('Deploy dialog for environment 1')).toBeInTheDocument();
    });

    it('opens the deployments page in the selected environment', () => {
        renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Production'));
        fireEvent.click(screen.getByText('Open in Deployments'));

        expect(environmentStore.getState().currentEnvironmentId).toBe(2);
        expect(onSheetOpenChange).toHaveBeenCalledWith(false);
        expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments?projectId=7');
    });

    it('opens on the given environment instead of the current one', () => {
        render(
            <ProjectDeploymentsSheet
                environmentId={2}
                expandedProjectDeploymentId={2}
                onSheetOpenChange={onSheetOpenChange}
                project={project}
                sheetOpen
            />
        );

        expect(screen.getByText('Selected environment 2')).toBeInTheDocument();
        expect(screen.getByText('Orders Prod expanded')).toBeInTheDocument();
        expect(screen.queryByText('Orders Dev')).not.toBeInTheDocument();
    });

    it('expands the deployment created from the empty state', () => {
        const {rerender} = renderProjectDeploymentsSheet();

        fireEvent.click(screen.getByText('Staging'));
        fireEvent.click(screen.getByText('Save deployment'));

        rerender(<ProjectDeploymentsSheet onSheetOpenChange={onSheetOpenChange} project={project} sheetOpen />);

        expect(screen.getByText('Selected environment 1')).toBeInTheDocument();
        expect(screen.getByText('Orders Staging expanded')).toBeInTheDocument();
    });

    it('asks to publish an unpublished project instead of offering to deploy it', () => {
        render(
            <ProjectDeploymentsSheet
                onSheetOpenChange={onSheetOpenChange}
                project={{id: 7, name: 'Orders'} as Project}
                sheetOpen
            />
        );

        fireEvent.click(screen.getByText('Staging'));

        expect(screen.getByText('Publish the project to deploy it')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
    });
});
