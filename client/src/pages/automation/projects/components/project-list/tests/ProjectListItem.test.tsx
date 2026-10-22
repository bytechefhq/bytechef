import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    navigate: vi.fn(),
}));

vi.mock('react-router-dom', async (importOriginal) => ({
    ...(await importOriginal<typeof import('react-router-dom')>()),
    useNavigate: () => hoisted.navigate,
}));

vi.mock('@/pages/automation/project-deployments/components/project-deployment-dialog/ProjectDeploymentDialog', () => ({
    default: ({
        onSuccess,
        redirectOnSubmit,
        triggerNode,
    }: {
        onSuccess: (deploymentId: number, environmentId?: number) => void;
        redirectOnSubmit?: boolean;
        triggerNode: React.ReactNode;
    }) => (
        <>
            {triggerNode}

            <button onClick={() => onSuccess(42, 0)}>{`Save deployment ${redirectOnSubmit}`}</button>

            <button onClick={() => onSuccess(43, 2)}>{`Save production deployment ${redirectOnSubmit}`}</button>
        </>
    ),
}));

vi.mock('@/pages/automation/project/components/ProjectDeploymentsSheet', () => ({
    default: ({
        expandedProjectDeploymentId,
        onSheetOpenChange,
    }: {
        expandedProjectDeploymentId?: number;
        onSheetOpenChange: (open: boolean) => void;
    }) => (
        <div>
            <span>{`Deployments sheet ${expandedProjectDeploymentId ?? 'none'}`}</span>

            <button onClick={() => onSheetOpenChange(false)}>Close sheet</button>
        </div>
    ),
}));

const project = {
    id: 7,
    lastProjectVersion: 2,
    lastPublishedDate: new Date(),
    name: 'Orders',
    projectWorkflowIds: [1],
} as Project;

const renderProjectListItem = () =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Collapsible>
                    <ProjectListItem project={project} remainingTags={[]} />
                </Collapsible>
            </TooltipProvider>
        </MemoryRouter>
    );

it('opens the project deployments sheet from the deployments button', () => {
    renderProjectListItem();

    fireEvent.click(screen.getByLabelText('Project Deployments'));

    expect(screen.getByText('Deployments sheet none')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Close sheet'));

    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});

it('opens the project deployments sheet on a saved development deployment', () => {
    renderProjectListItem();

    fireEvent.click(screen.getByText('Save deployment false'));

    expect(screen.getByText('Deployments sheet 42')).toBeInTheDocument();
});

it('opens the deployments page in production after deploying to production', () => {
    environmentStore.setState({currentEnvironmentId: 0});
    hoisted.navigate.mockReset();

    renderProjectListItem();

    fireEvent.click(screen.getByText('Save production deployment false'));

    expect(environmentStore.getState().currentEnvironmentId).toBe(2);
    expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments?projectId=7', {
        state: {projectDeploymentId: 43},
    });
    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});
