import {CollapsibleTrigger} from '@/components/ui/collapsible';
import {useProjectDeploymentsEnabledStore} from '@/pages/automation/project-deployments/stores/useProjectDeploymentsEnabledStore';
import {Project, ProjectDeployment, Tag} from '@/shared/middleware/automation/configuration';
import {render, screen, userEvent} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentList from './ProjectDeploymentList';

vi.mock('./ProjectDeploymentListItem', () => ({
    default: ({projectDeployment, remainingTags}: {projectDeployment: ProjectDeployment; remainingTags?: Tag[]}) => (
        <div data-testid={`item-${projectDeployment.id}`}>
            <CollapsibleTrigger>{`Toggle ${projectDeployment.name}`}</CollapsibleTrigger>

            <span>{`Remaining tags ${projectDeployment.id}: ${remainingTags?.map((tag) => tag.name).join(',')}`}</span>
        </div>
    ),
}));

vi.mock('../project-deployment-workflow-list/ProjectDeploymentWorkflowList', () => ({
    default: ({
        projectDeploymentEnabled,
        projectDeploymentId,
    }: {
        projectDeploymentEnabled: boolean;
        projectDeploymentId: number;
    }) => <div>{`Workflows of ${projectDeploymentId} enabled=${projectDeploymentEnabled}`}</div>,
}));

const project: Project = {id: 7, name: 'Order alerts', workspaceId: 1};

const tags: Tag[] = [
    {id: 1, name: 'billing'},
    {id: 2, name: 'sales'},
];

const createProjectDeployment = (id: number, overrides: Partial<ProjectDeployment> = {}): ProjectDeployment => ({
    enabled: true,
    environmentId: 0,
    id,
    name: `Deployment ${id}`,
    projectDeploymentWorkflows: [{id: id * 10, workflowId: `workflow-${id}`}],
    projectId: project.id,
    projectVersion: 1,
    tags: [],
    ...overrides,
});

const projectDeployments = [createProjectDeployment(1), createProjectDeployment(2)];

const renderList = (props: Partial<Parameters<typeof ProjectDeploymentList>[0]> = {}) =>
    render(<ProjectDeploymentList project={project} projectDeployments={projectDeployments} tags={tags} {...props} />);

describe('ProjectDeploymentList', () => {
    beforeEach(() => {
        useProjectDeploymentsEnabledStore.setState({projectDeploymentMap: new Map<number, boolean>()});
    });

    it('should render every deployment collapsed by default', () => {
        renderList();

        expect(screen.getByTestId('item-1')).toBeInTheDocument();
        expect(screen.getByTestId('item-2')).toBeInTheDocument();
        expect(screen.queryByText(/Workflows of/)).not.toBeInTheDocument();
    });

    it('should expand only the newly created deployment', () => {
        renderList({newlyCreatedDeploymentId: 2});

        expect(screen.getByText('Workflows of 2 enabled=true')).toBeInTheDocument();
        expect(screen.queryByText(/Workflows of 1/)).not.toBeInTheDocument();
    });

    it('should expand a deployment created after the first render and keep the ones already open', async () => {
        const {rerender} = renderList();

        await userEvent.click(screen.getByText('Toggle Deployment 1'));

        rerender(
            <ProjectDeploymentList
                newlyCreatedDeploymentId={2}
                project={project}
                projectDeployments={projectDeployments}
                tags={tags}
            />
        );

        expect(screen.getByText('Workflows of 1 enabled=true')).toBeInTheDocument();
        expect(screen.getByText('Workflows of 2 enabled=true')).toBeInTheDocument();
    });

    it('should toggle a deployment open and closed through its trigger', async () => {
        renderList();

        await userEvent.click(screen.getByText('Toggle Deployment 1'));

        expect(screen.getByText('Workflows of 1 enabled=true')).toBeInTheDocument();

        await userEvent.click(screen.getByText('Toggle Deployment 1'));

        expect(screen.queryByText(/Workflows of 1/)).not.toBeInTheDocument();
    });

    it('should prefer the enabled state toggled in the store over the fetched one', () => {
        useProjectDeploymentsEnabledStore.setState({projectDeploymentMap: new Map([[2, false]])});

        renderList({newlyCreatedDeploymentId: 2});

        expect(screen.getByText('Workflows of 2 enabled=false')).toBeInTheDocument();
    });

    it('should offer only the tags the deployment does not already have', () => {
        renderList({
            projectDeployments: [createProjectDeployment(1, {tags: [{id: 1, name: 'billing'}]})],
        });

        expect(screen.getByText('Remaining tags 1: sales')).toBeInTheDocument();
    });

    it('should render no workflow list for a deployment without workflows', () => {
        renderList({
            newlyCreatedDeploymentId: 1,
            projectDeployments: [createProjectDeployment(1, {projectDeploymentWorkflows: []})],
        });

        expect(screen.getByTestId('item-1')).toBeInTheDocument();
        expect(screen.queryByText(/Workflows of/)).not.toBeInTheDocument();
    });

    it('should render nothing when the project has no id', () => {
        renderList({project: {name: 'Unsaved', workspaceId: 1}});

        expect(screen.queryByTestId('item-1')).not.toBeInTheDocument();
    });
});
