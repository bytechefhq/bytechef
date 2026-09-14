import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {ProjectDeploymentWorkflow, Workflow} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowListItem from '../ProjectDeploymentWorkflowListItem';

vi.mock('@/shared/mutations/automation/projectDeploymentWorkflows.mutations', () => ({
    useEnableProjectDeploymentWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

const {rowPropsMock} = vi.hoisted(() => ({rowPropsMock: vi.fn()}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: (props: object) => {
        rowPropsMock(props);

        return null;
    },
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-list/ProjectDeploymentWorkflowListItemDropdownMenu',
    () => ({default: () => null})
);

vi.mock('@/pages/automation/project-deployments/components/ProjectDeploymentEditWorkflowDialog', () => ({
    default: () => null,
}));

const workflow = {id: 'workflow1', label: 'workflow1', triggers: []} as unknown as Workflow;

const renderListItem = (projectName?: string) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <TooltipProvider>
                    <ProjectDeploymentWorkflowListItem
                        environmentId={1}
                        projectDeploymentEnabled
                        projectDeploymentId={3}
                        projectDeploymentWorkflow={
                            {enabled: true, workflowId: 'workflow1'} as ProjectDeploymentWorkflow
                        }
                        projectName={projectName}
                        projectVersion={2}
                        workflow={workflow}
                        workflowComponentDefinitions={{}}
                        workflowTaskDispatcherDefinitions={{}}
                    />
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('ProjectDeploymentWorkflowListItem', () => {
    beforeEach(() => {
        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentId: undefined,
            projectDeploymentWorkflowSheetOpen: false,
            projectName: undefined,
            projectVersion: undefined,
            workflow: undefined,
        });
    });

    it('opens the executions sheet for its deployment, project, version and workflow when clicked', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow');

        await user.click(screen.getByText('workflow1'));

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(state.projectDeploymentId).toBe(3);
        expect(state.projectName).toBe('Subflow');
        expect(state.projectVersion).toBe(2);
        expect(state.workflow).toBe(workflow);
    });

    it('closes an execution detail left open elsewhere so the sheet starts on the executions list', async () => {
        const user = userEvent.setup();

        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 9,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: true,
        });

        renderListItem('Subflow');

        await user.click(screen.getByText('workflow1'));

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(true);
    });

    it('shows the trigger and components like the projects page', () => {
        rowPropsMock.mockClear();

        renderListItem('Subflow');

        expect(rowPropsMock).toHaveBeenLastCalledWith(
            expect.objectContaining({
                className: 'hidden sm:flex',
                workflow,
                workflowComponentDefinitions: {},
                workflowTaskDispatcherDefinitions: {},
            })
        );
    });

    it('aligns the workflow name tooltip to the start of the name', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow');

        await user.hover(screen.getByText('workflow1'));

        await waitFor(() =>
            expect(document.querySelector('[data-slot="tooltip-content"]')).toHaveAttribute('data-align', 'start')
        );
    });

    it('does not open the executions sheet when the enable switch is toggled', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow');

        await user.click(screen.getByRole('switch'));

        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
    });
});
