import {Collapsible} from '@/components/ui/collapsible';
import {TooltipProvider} from '@/components/ui/tooltip';
import ProjectListItem from '@/pages/automation/projects/components/project-list/ProjectListItem';
import {ProjectListItemTabType} from '@/pages/automation/projects/components/project-list/ProjectListItemTabs';
import {Project} from '@/shared/middleware/automation/configuration';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {fireEvent, render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, expect, it, vi} from 'vitest';

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

const project = {
    developmentDeploymentCount: 2,
    id: 7,
    lastProjectVersion: 2,
    lastPublishedDate: new Date(),
    name: 'Orders',
    projectWorkflowIds: [1],
} as Project;

const onTabChange = vi.fn();

const renderProjectListItem = (activeTab?: ProjectListItemTabType, projectOverrides: Partial<Project> = {}) =>
    render(
        <MemoryRouter>
            <TooltipProvider>
                <Collapsible open={!!activeTab}>
                    <ProjectListItem
                        activeTab={activeTab}
                        deploymentsContent={<div>Deployments content</div>}
                        onTabChange={onTabChange}
                        project={{...project, ...projectOverrides}}
                        remainingTags={[]}
                        workflowsContent={<div>Workflows content</div>}
                    />
                </Collapsible>
            </TooltipProvider>
        </MemoryRouter>
    );

beforeEach(() => {
    onTabChange.mockReset();
});

it('summarizes workflows and deployments and hides the tabs while collapsed', () => {
    renderProjectListItem();

    expect(screen.getByText('1 workflow')).toBeInTheDocument();
    expect(screen.getByText('2 deployments')).toBeInTheDocument();
    expect(screen.queryByRole('tab')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Project Deployments')).not.toBeInTheDocument();
});

it('marks the project name with the folder icon', () => {
    const {container} = renderProjectListItem();

    expect(container.querySelector('svg.lucide-folder')).toBeInTheDocument();
});

it('shows deployments without a number when the server did not send a count', () => {
    renderProjectListItem(undefined, {developmentDeploymentCount: undefined});

    expect(screen.getByText('deployments')).toBeInTheDocument();
    expect(screen.queryByText('0 deployments')).not.toBeInTheDocument();
});

it('shows the tabs and the active tab content when expanded', () => {
    renderProjectListItem('deployments');

    expect(screen.getByRole('tab', {name: 'Deployments'})).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByText('Deployments content')).toBeInTheDocument();
});

it('switches tabs', async () => {
    renderProjectListItem('workflows');

    await userEvent.click(screen.getByRole('tab', {name: 'Deployments'}));

    expect(onTabChange).toHaveBeenCalledWith('deployments');
});

it('opens the workflows tab when a collapsed row is clicked', () => {
    renderProjectListItem();

    fireEvent.click(screen.getByLabelText('Orders_container'));

    expect(onTabChange).toHaveBeenCalledWith('workflows');
});

it('collapses an open row when it is clicked', () => {
    renderProjectListItem('workflows');

    fireEvent.click(screen.getByLabelText('Orders_container'));

    expect(onTabChange).toHaveBeenCalledWith(undefined);
});

it('offers New Workflow on the workflows tab', () => {
    renderProjectListItem('workflows');

    expect(screen.getByLabelText('Create Workflow')).toHaveTextContent('New Workflow');
    expect(screen.queryByLabelText('Create Deployment')).not.toBeInTheDocument();
});

it('offers New Deployment on the deployments tab and opens the saved deployment', () => {
    renderProjectListItem('deployments');

    expect(screen.getByLabelText('Create Deployment')).toBeEnabled();

    fireEvent.click(screen.getByText('Save deployment false'));

    expect(onTabChange).toHaveBeenCalledWith('deployments', 42);
});

it('hides New Workflow while the project has no workflows', () => {
    renderProjectListItem('workflows', {projectWorkflowIds: []});

    expect(screen.queryByLabelText('Create Workflow')).not.toBeInTheDocument();
});

it('hides New Deployment while the project has no deployments', () => {
    renderProjectListItem('deployments', {developmentDeploymentCount: 0});

    expect(screen.queryByLabelText('Create Deployment')).not.toBeInTheDocument();
});

it('hides New Deployment until the project is published', () => {
    renderProjectListItem('deployments', {lastPublishedDate: undefined});

    expect(screen.queryByLabelText('Create Deployment')).not.toBeInTheDocument();
});

const openNewDeploymentFromMenu = async () => {
    await userEvent.click(screen.getByLabelText('More Project Actions'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Deployment'}));
};

it('removes the Deploy button from the project row', () => {
    renderProjectListItem();

    expect(screen.queryByRole('button', {name: 'Deploy'})).not.toBeInTheDocument();
});

it('groups the project, workflow and deployment actions in the menu', async () => {
    renderProjectListItem();

    await userEvent.click(screen.getByLabelText('More Project Actions'));

    expect(screen.getByRole('group', {name: 'Project Actions'})).toHaveTextContent('EditPublish');
    expect(screen.getByRole('group', {name: 'Workflow Actions'})).toHaveTextContent(
        'New WorkflowImport WorkflowView Workflows'
    );
    expect(screen.getByRole('group', {name: 'Deployment Actions'})).toHaveTextContent('New Deployment');
});

it('opens the workflow dialog from the New Workflow menu item', async () => {
    renderProjectListItem();

    await userEvent.click(screen.getByLabelText('More Project Actions'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'New Workflow'}));

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
});

it('opens the file picker from the Import Workflow menu item', async () => {
    renderProjectListItem();

    const fileInput = screen.getByTestId('7-importWorkflowHiddenInput') as HTMLInputElement;
    const clickSpy = vi.spyOn(fileInput, 'click');

    await userEvent.click(screen.getByLabelText('More Project Actions'));

    await userEvent.click(screen.getByRole('menuitem', {name: 'Import Workflow'}));

    expect(clickSpy).toHaveBeenCalled();
});

it('opens the deployments tab on a saved development deployment', async () => {
    renderProjectListItem();

    await openNewDeploymentFromMenu();

    fireEvent.click(screen.getByText('Save deployment false'));

    expect(onTabChange).toHaveBeenCalledWith('deployments', 42);
});

it('opens the deployments page in production after deploying to production', async () => {
    environmentStore.setState({currentEnvironmentId: 0});
    hoisted.navigate.mockReset();

    renderProjectListItem();

    await openNewDeploymentFromMenu();

    fireEvent.click(screen.getByText('Save production deployment false'));

    expect(environmentStore.getState().currentEnvironmentId).toBe(2);
    expect(hoisted.navigate).toHaveBeenCalledWith('/automation/deployments?projectId=7', {
        state: {projectDeploymentId: 43},
    });
    expect(onTabChange).not.toHaveBeenCalledWith('deployments', 43);
});
