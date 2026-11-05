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

const {openProjectWorkflowMock, rowPropsMock} = vi.hoisted(() => ({
    openProjectWorkflowMock: vi.fn(),
    rowPropsMock: vi.fn(),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({openProject: vi.fn(), openProjectWorkflow: openProjectWorkflowMock}),
}));

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

const workflow = {
    id: 'workflow1',
    label: 'workflow1',
    triggers: [],
    workflowUuid: 'workflow-uuid',
} as unknown as Workflow;

interface RenderListItemOverridesI {
    projectDeploymentWorkflow?: ProjectDeploymentWorkflow;
    workflow?: Workflow;
}

const renderListItem = (projectName?: string, projectId?: number, overrides: RenderListItemOverridesI = {}) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <MemoryRouter>
                <TooltipProvider>
                    <ProjectDeploymentWorkflowListItem
                        environmentId={1}
                        projectDeploymentEnabled
                        projectDeploymentId={3}
                        projectDeploymentWorkflow={
                            overrides.projectDeploymentWorkflow ??
                            ({enabled: true, workflowId: 'workflow1'} as ProjectDeploymentWorkflow)
                        }
                        projectId={projectId}
                        projectName={projectName}
                        projectVersion={2}
                        workflow={overrides.workflow ?? workflow}
                        workflowComponentDefinitions={{}}
                        workflowTaskDispatcherDefinitions={{}}
                    />
                </TooltipProvider>
            </MemoryRouter>
        </QueryClientProvider>
    );

describe('ProjectDeploymentWorkflowListItem', () => {
    beforeEach(() => {
        openProjectWorkflowMock.mockReset();

        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentId: undefined,
            projectDeploymentWorkflowSheetOpen: false,
            projectId: undefined,
            projectName: undefined,
            projectVersion: undefined,
            workflow: undefined,
        });
    });

    it('opens the executions sheet for its deployment, project, version and workflow when clicked', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow', 7);

        await user.click(screen.getByText('workflow1'));

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(state.projectDeploymentId).toBe(3);
        expect(state.projectId).toBe(7);
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
        expect(rowPropsMock.mock.lastCall?.[0]).not.toHaveProperty('fitToWidth');
    });

    it('marks the workflow name with the workflow icon', () => {
        const {container} = renderListItem('Subflow');

        expect(container.querySelector('svg.lucide-workflow')).toBeInTheDocument();
    });

    it('keeps the name column one width so every row starts its icons at the same position', () => {
        renderListItem('Subflow');

        const nameColumn = screen.getByText('workflow1').closest('.max-w-80');

        expect(nameColumn).toHaveClass('sm:w-80', 'sm:shrink-0');
        expect(nameColumn?.nextElementSibling).toHaveClass('min-w-0');
    });

    it('aligns the workflow name tooltip to the start of the name', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow');

        await user.hover(screen.getByText('workflow1'));

        await waitFor(() =>
            expect(document.querySelector('[data-slot="tooltip-content"]')).toHaveAttribute('data-align', 'start')
        );
    });

    it('opens the workflow in its project from the icon beside the name without opening the sheet', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow', 7);

        await user.click(screen.getByRole('button', {name: 'Open in project'}));

        expect(openProjectWorkflowMock).toHaveBeenCalledWith(7, 'workflow-uuid');
        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
    });

    it('hides the open in project icon without a project', () => {
        renderListItem('Subflow');

        expect(screen.queryByRole('button', {name: 'Open in project'})).not.toBeInTheDocument();
    });

    it('does not open the executions sheet when the enable switch is toggled', async () => {
        const user = userEvent.setup();

        renderListItem('Subflow');

        await user.click(screen.getByRole('switch'));

        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
    });

    describe('trigger page links', () => {
        const staticWebhookUrl = 'http://localhost:9555/webhooks/workflow-execution-id';

        const formWorkflow = {
            ...workflow,
            triggers: [{name: 'trigger_1', type: 'form/v1/newFormRequest'}],
        } as unknown as Workflow;

        const chatWorkflow = {
            ...workflow,
            triggers: [{name: 'trigger_1', parameters: {mode: 1}, type: 'chat/v1/newChatRequest'}],
        } as unknown as Workflow;

        it('renders the form button as a link to the form page so it can open in a new tab', () => {
            renderListItem(undefined, undefined, {
                projectDeploymentWorkflow: {
                    enabled: true,
                    staticWebhookUrl,
                    workflowId: 'workflow1',
                } as ProjectDeploymentWorkflow,
                workflow: formWorkflow,
            });

            expect(screen.getByRole('link', {name: 'Open form'})).toHaveAttribute(
                'href',
                '/form/1/workflow-execution-id'
            );
        });

        it('renders the hosted chat button as a link to the chat page so it can open in a new tab', () => {
            renderListItem(undefined, undefined, {
                projectDeploymentWorkflow: {
                    enabled: true,
                    staticWebhookUrl,
                    workflowId: 'workflow1',
                } as ProjectDeploymentWorkflow,
                workflow: chatWorkflow,
            });

            expect(screen.getByRole('link', {name: 'Open hosted chat'})).toHaveAttribute(
                'href',
                '/automation/chats/workflow-execution-id'
            );
        });

        it('renders disabled buttons instead of links while the workflow is disabled', () => {
            renderListItem(undefined, undefined, {
                projectDeploymentWorkflow: {
                    enabled: false,
                    staticWebhookUrl,
                    workflowId: 'workflow1',
                } as ProjectDeploymentWorkflow,
                workflow: formWorkflow,
            });

            expect(screen.queryByRole('link', {name: 'Open form'})).not.toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Open form'})).toBeDisabled();
        });
    });
});
