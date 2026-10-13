import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aProjectWorkflowEditDialog from '../A2aProjectWorkflowEditDialog';
import {A2aProjectItemType} from '../hooks/useA2aProjectList';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    projectDeployment: undefined as unknown,
    workflow: undefined as unknown,
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: () => ({data: hoisted.projectDeployment}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: hoisted.workflow}),
}));

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: ({
        onClose,
        projectDeploymentWorkflow,
        workflow,
    }: {
        onClose: () => void;
        projectDeploymentWorkflow: {id: number};
        workflow: {label: string};
    }) => (
        <div>
            <span>{`Editing ${workflow.label} in deployment workflow ${projectDeploymentWorkflow.id}`}</span>

            <button onClick={onClose} type="button">
                Close
            </button>
        </div>
    ),
}));

const a2aProject = {
    id: '11',
    lastModifiedDate: null,
    projectDeploymentId: '31',
    projectId: '5',
    projectVersion: 2,
    workflowIds: ['workflow-a', 'workflow-b'],
} as A2aProjectItemType;

describe('A2aProjectWorkflowEditDialog', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.projectDeployment = {
            id: 31,
            projectDeploymentWorkflows: [
                {id: 41, workflowId: 'workflow-a'},
                {id: 42, workflowId: 'workflow-b'},
            ],
        };
        hoisted.workflow = {id: 'workflow-b', label: 'Follow up'};
    });

    it('edits the deployment workflow of the chosen workflow only', () => {
        render(<A2aProjectWorkflowEditDialog a2aProject={a2aProject} onClose={vi.fn()} workflowId="workflow-b" />);

        expect(screen.getByText('Editing Follow up in deployment workflow 42')).toBeInTheDocument();
    });

    it('renders nothing until the deployment and the workflow are loaded', () => {
        hoisted.projectDeployment = undefined;

        const {container} = render(
            <A2aProjectWorkflowEditDialog a2aProject={a2aProject} onClose={vi.fn()} workflowId="workflow-b" />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('renders nothing when the workflow is not part of the deployment', () => {
        const {container} = render(
            <A2aProjectWorkflowEditDialog a2aProject={a2aProject} onClose={vi.fn()} workflowId="workflow-c" />
        );

        expect(container).toBeEmptyDOMElement();
    });

    it('refreshes the A2A workflows and closes', async () => {
        const onClose = vi.fn();

        render(<A2aProjectWorkflowEditDialog a2aProject={a2aProject} onClose={onClose} workflowId="workflow-b" />);

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['a2aProjectWorkflowsByA2aProjectId']});
        expect(onClose).toHaveBeenCalledTimes(1);
    });
});
