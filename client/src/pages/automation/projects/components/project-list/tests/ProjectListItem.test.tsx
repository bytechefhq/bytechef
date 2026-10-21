import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {Project} from '@/shared/middleware/automation/configuration';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import {MemoryRouter} from 'react-router-dom';
import {expect, it, vi} from 'vitest';

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

            <button onClick={() => onSuccess(42, 2)}>{`Save deployment ${redirectOnSubmit}`}</button>
        </>
    ),
}));

vi.mock('@/pages/automation/project/components/ProjectDeploymentsSheet', () => ({
    default: ({
        environmentId,
        expandedProjectDeploymentId,
        onSheetOpenChange,
    }: {
        environmentId?: number;
        expandedProjectDeploymentId?: number;
        onSheetOpenChange: (open: boolean) => void;
    }) => (
        <div>
            <span>{`Deployments sheet ${environmentId ?? 'current'} ${expandedProjectDeploymentId ?? 'none'}`}</span>

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

    expect(screen.getByText('Deployments sheet current none')).toBeInTheDocument();

    fireEvent.click(screen.getByText('Close sheet'));

    expect(screen.queryByText(/Deployments sheet/)).not.toBeInTheDocument();
});

it('opens the project deployments sheet on the saved deployment instead of redirecting', () => {
    renderProjectListItem();

    fireEvent.click(screen.getByText('Save deployment false'));

    expect(screen.getByText('Deployments sheet 2 42')).toBeInTheDocument();
});
