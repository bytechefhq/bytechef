import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpProjectWorkflowListItem from './McpProjectWorkflowListItem';
import {McpProjectWorkflowItemType} from './hooks/useMcpProjectList';

const hoisted = vi.hoisted(() => ({
    invalidateQueries: vi.fn(),
    openProjectWorkflow: vi.fn(),
    updateMutate: vi.fn(),
    workflow: undefined as
        {id: string; label: string; workflowTaskComponentNames: string[]; workflowUuid: string} | undefined,
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => true,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteMcpProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useUpdateMcpProjectWorkflowMutation: (options?: {onSuccess?: () => void}) => ({
        isPending: false,
        mutate: (variables: {id: string; input: {enabled: boolean}}) => {
            hoisted.updateMutate(variables);

            options?.onSuccess?.();
        },
    }),
}));

vi.mock('@tanstack/react-query', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@tanstack/react-query')>()),
    useQueryClient: () => ({invalidateQueries: hoisted.invalidateQueries}),
}));

vi.mock('@/shared/queries/automation/projectDeployments.queries', () => ({
    useGetProjectDeploymentQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: () => ({data: hoisted.workflow}),
}));

vi.mock('@/pages/automation/project-deployments/hooks/useOpenInProject', () => ({
    default: () => ({openProject: vi.fn(), openProjectWorkflow: hoisted.openProjectWorkflow}),
}));

vi.mock('@/shared/hooks/useWorkflowComponentDefinitions', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/hooks/useWorkflowComponentDefinitions')>()),
    default: () => ({
        workflowComponentDefinitions: {slack: {icon: 'slack.svg', name: 'slack', title: 'Slack'}},
        workflowTaskDispatcherDefinitions: {},
    }),
}));

vi.mock('@/shared/components/workflow/WorkflowTriggerAndComponentsRow', () => ({
    default: ({filteredComponentNames}: {filteredComponentNames?: string[]}) => (
        <div data-testid="trigger-and-components-row">{filteredComponentNames?.join(',')}</div>
    ),
}));

const renderListItem = (mcpProjectWorkflow: McpProjectWorkflowItemType, projectProps = {}) =>
    render(
        <TooltipProvider>
            <McpProjectWorkflowListItem mcpProjectWorkflow={mcpProjectWorkflow} {...projectProps} />
        </TooltipProvider>
    );

const Wrapper = ({children}: {children: ReactNode}) => <TooltipProvider>{children}</TooltipProvider>;

vi.mock('@/shared/contexts/McpActivePopoverContext', () => ({
    useCloseActivePopoverOnUnmount: vi.fn(),
    useMcpActivePopover: () => ({activePopoverId: null, closePopover: vi.fn(), openPopover: vi.fn()}),
}));

const createMcpProjectWorkflow = (enabled: boolean) =>
    ({
        enabled,
        id: '9',
        mcpProjectId: 3,
        projectDeploymentWorkflowId: 4,
        workflow: {id: 'workflow-a', label: 'Lead intake'},
    }) as unknown as McpProjectWorkflowItemType;

describe('McpProjectWorkflowListItem', () => {
    beforeEach(() => {
        hoisted.invalidateQueries.mockReset();
        hoisted.openProjectWorkflow.mockReset();
        hoisted.updateMutate.mockReset();
        hoisted.workflow = undefined;

        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentId: undefined,
            projectDeploymentWorkflowSheetOpen: false,
            projectId: undefined,
            projectName: undefined,
            projectVersion: undefined,
            workflow: undefined,
        });
    });

    it('disables the workflow tool from its toggle', async () => {
        const user = userEvent.setup();

        render(<McpProjectWorkflowListItem mcpProjectWorkflow={createMcpProjectWorkflow(true)} />, {
            wrapper: Wrapper,
        });

        const enabledSwitch = screen.getByRole('switch', {name: 'Enable Lead intake'});

        expect(enabledSwitch).toBeChecked();

        await user.click(enabledSwitch);

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledWith({id: '9', input: {enabled: false}}));

        expect(hoisted.invalidateQueries).toHaveBeenCalledWith({queryKey: ['mcpProjectsByServerId']});
    });

    it('shows a disabled workflow tool as switched off', () => {
        render(<McpProjectWorkflowListItem mcpProjectWorkflow={createMcpProjectWorkflow(false)} />, {
            wrapper: Wrapper,
        });

        expect(screen.getByRole('switch', {name: 'Enable Lead intake'})).not.toBeChecked();
    });

    it('offers configure before the toggle and edit and delete in the actions menu', async () => {
        const user = userEvent.setup();

        render(<McpProjectWorkflowListItem mcpProjectWorkflow={createMcpProjectWorkflow(true)} />, {
            wrapper: Wrapper,
        });

        const configureButton = screen.getByRole('button', {name: 'Configure Lead intake'});

        expect(
            configureButton.compareDocumentPosition(screen.getByRole('switch', {name: 'Enable Lead intake'})) &
                Node.DOCUMENT_POSITION_FOLLOWING
        ).toBeTruthy();

        await user.click(screen.getByRole('button', {name: 'Lead intake actions'}));

        expect(await screen.findByRole('menuitem', {name: 'Edit'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Configure'})).not.toBeInTheDocument();

        await user.click(screen.getByRole('menuitem', {name: 'Delete'}));

        expect(await screen.findByText('Are you absolutely sure?')).toBeInTheDocument();
    });

    it("opens the workflow's executions in the deployment sheet when the workflow is clicked", async () => {
        const user = userEvent.setup();

        hoisted.workflow = {
            id: 'workflow-a',
            label: 'Lead intake',
            workflowTaskComponentNames: ['slack'],
            workflowUuid: 'uuid-a',
        };

        renderListItem(createMcpProjectWorkflow(true), {
            projectDeploymentId: 31,
            projectId: 5,
            projectName: 'Sales project',
            projectVersion: 2,
        });

        await user.click(screen.getByRole('button', {name: 'Lead intake'}));

        const state = useProjectDeploymentWorkflowSheetStore.getState();

        expect(state.projectDeploymentWorkflowSheetOpen).toBe(true);
        expect(state.projectDeploymentId).toBe(31);
        expect(state.projectId).toBe(5);
        expect(state.projectName).toBe('Sales project');
        expect(state.projectVersion).toBe(2);
        expect(state.workflow).toEqual(hoisted.workflow);
    });

    it('shows the trigger and components of the workflow and opens it in its project', async () => {
        const user = userEvent.setup();

        hoisted.workflow = {
            id: 'workflow-a',
            label: 'Lead intake',
            workflowTaskComponentNames: ['slack'],
            workflowUuid: 'uuid-a',
        };

        renderListItem(createMcpProjectWorkflow(true), {projectDeploymentId: 31, projectId: 5});

        expect(screen.getByTestId('trigger-and-components-row')).toHaveTextContent('slack');

        await user.click(screen.getByRole('button', {name: 'Open in project'}));

        expect(hoisted.openProjectWorkflow).toHaveBeenCalledWith(5, 'uuid-a');
    });

    it('keeps the workflow name inert until the workflow and its deployment are known', () => {
        renderListItem(createMcpProjectWorkflow(true));

        expect(screen.getByRole('button', {name: 'Lead intake'})).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'Open in project'})).not.toBeInTheDocument();
        expect(screen.queryByTestId('trigger-and-components-row')).not.toBeInTheDocument();
    });
});
