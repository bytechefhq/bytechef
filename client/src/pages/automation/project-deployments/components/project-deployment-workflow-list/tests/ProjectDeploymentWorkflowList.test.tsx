import {ProjectDeploymentWorkflow} from '@/shared/middleware/automation/configuration';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowList from '../ProjectDeploymentWorkflowList';

const {listItemPropsMock} = vi.hoisted(() => ({listItemPropsMock: vi.fn()}));

vi.mock('@/shared/queries/automation/projectWorkflows.queries', () => ({
    useGetProjectVersionWorkflowsQuery: () => ({
        data: [
            {id: 'workflow1', label: 'Enabled workflow'},
            {id: 'workflow2', label: 'Disabled workflow'},
        ],
        isLoading: false,
    }),
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItem',
    () => ({
        default: (props: {projectName?: string; workflow: {label: string}}) => {
            listItemPropsMock(props);

            return <li>{props.workflow.label}</li>;
        },
    })
);

const projectDeploymentWorkflows = [
    {enabled: true, workflowId: 'workflow1'},
    {enabled: false, workflowId: 'workflow2'},
] as ProjectDeploymentWorkflow[];

describe('ProjectDeploymentWorkflowList', () => {
    beforeEach(() => {
        listItemPropsMock.mockReset();
    });

    it('passes the project name, version and deployment to enabled and disabled workflow items', async () => {
        const user = userEvent.setup();

        render(
            <ProjectDeploymentWorkflowList
                componentDefinitions={[]}
                environmentId={1}
                projectDeploymentEnabled
                projectDeploymentId={3}
                projectDeploymentWorkflows={projectDeploymentWorkflows}
                projectId={7}
                projectName="Sales"
                projectVersion={2}
                taskDispatcherDefinitions={[]}
            />
        );

        expect(screen.getByText('Enabled workflow')).toBeInTheDocument();

        await user.click(screen.getByText('Disabled Workflows'));

        expect(await screen.findByText('Disabled workflow')).toBeInTheDocument();

        const renderedWorkflowIds = new Set(
            listItemPropsMock.mock.calls.map(([props]) => {
                expect(props).toEqual(
                    expect.objectContaining({projectDeploymentId: 3, projectName: 'Sales', projectVersion: 2})
                );

                return props.workflow.id;
            })
        );

        expect(renderedWorkflowIds).toEqual(new Set(['workflow1', 'workflow2']));
    });
});
