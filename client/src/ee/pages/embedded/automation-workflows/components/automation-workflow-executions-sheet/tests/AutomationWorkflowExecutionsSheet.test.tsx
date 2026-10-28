import {TooltipProvider} from '@/components/ui/tooltip';
import useAutomationWorkflowExecutionsSheetStore from '@/ee/pages/embedded/automation-workflows/stores/useAutomationWorkflowExecutionsSheetStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen} from '@/shared/util/test-utils';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import AutomationWorkflowExecutionsSheet from '../AutomationWorkflowExecutionsSheet';

const hoisted = vi.hoisted(() => ({
    executionsQueryMock: vi.fn(),
    refetchMock: vi.fn(),
    workflowQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetEmbeddedWorkflowExecutionsQuery: hoisted.executionsQueryMock,
}));

vi.mock('@/shared/queries/automation/workflows.queries', () => ({
    useGetWorkflowQuery: hoisted.workflowQueryMock,
}));

vi.mock('@/ee/pages/embedded/workflow-executions/components/AutomationWorkflowExecutionsTable', () => ({
    default: ({data}: {data: WorkflowExecution[]}) => (
        <div>{`Executions table ${data.map((workflowExecution) => workflowExecution.id).join(',')}`}</div>
    ),
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionSheet', () => ({
    default: () => <div>Execution details sheet</div>,
}));

vi.mock(
    '@/pages/automation/project-deployments/components/project-deployment-workflow-executions-sheet/ProjectDeploymentWorkflowPanel',
    () => ({
        default: ({workflow}: {workflow: Workflow}) => <div>{`Workflow panel ${workflow.id}`}</div>,
    })
);

const mockExecutionsQueryResult = (content: object[], totalPages = 1) =>
    hoisted.executionsQueryMock.mockReturnValue({
        data: {content, size: 20, totalElements: 20 * totalPages, totalPages},
        error: null,
        isFetching: false,
        isLoading: false,
        refetch: hoisted.refetchMock,
    });

const execution = {
    id: 5,
    job: {id: 5, status: 'COMPLETED'},
    project: {id: 1, name: 'Mail Project'},
    projectDeployment: {id: 2, name: '__EMBEDDED__user-1__DEVELOPMENT'},
    workflow: {id: 'draft-wf-1', label: 'Mailer'},
};

const renderSheet = () =>
    render(
        <TooltipProvider>
            <AutomationWorkflowExecutionsSheet />
        </TooltipProvider>
    );

const openSheet = () =>
    act(() =>
        useAutomationWorkflowExecutionsSheetStore.getState().openAutomationWorkflowExecutionsSheet({
            projectName: 'Mail Project',
            workflow: {label: 'Mailer', workflowId: 'draft-wf-1', workflowUuid: 'wf-1'},
        })
    );

describe('AutomationWorkflowExecutionsSheet', () => {
    beforeEach(() => {
        hoisted.executionsQueryMock.mockReset();
        hoisted.refetchMock.mockReset();
        hoisted.workflowQueryMock.mockReset();

        hoisted.workflowQueryMock.mockReturnValue({
            data: {id: 'draft-wf-1', label: 'Mailer'},
            error: null,
            isLoading: false,
        });

        environmentStore.setState({currentEnvironmentId: 1});

        useWorkspaceStore.setState({currentWorkspaceId: 4});

        useAutomationWorkflowExecutionsSheetStore.setState({
            automationWorkflowExecutionsSheetOpen: false,
            projectName: undefined,
            workflow: undefined,
        });

        useWorkflowExecutionSheetStore.setState({workflowExecutionId: 0, workflowExecutionSheetOpen: false});
    });

    it('renders nothing while closed', () => {
        mockExecutionsQueryResult([]);

        renderSheet();

        expect(screen.queryByText('Mailer')).not.toBeInTheDocument();
        expect(hoisted.executionsQueryMock).not.toHaveBeenCalled();
    });

    it("shows the template workflow's executions in the current environment by default", () => {
        mockExecutionsQueryResult([execution]);

        renderSheet();

        openSheet();

        expect(screen.getByText('Mail Project /')).toBeInTheDocument();
        expect(screen.getByText('Mailer', {selector: 'strong'})).toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Executions'})).toHaveAttribute('aria-selected', 'true');
        expect(hoisted.executionsQueryMock).toHaveBeenLastCalledWith(
            {environmentId: 1, id: 4, pageNumber: 0, workflowId: 'wf-1'},
            true
        );
        expect(screen.getByText('Executions table 5')).toBeInTheDocument();
        expect(screen.getByText('Execution details sheet')).toBeInTheDocument();
    });

    it('shows an empty state when the workflow has not run yet', () => {
        mockExecutionsQueryResult([]);

        renderSheet();

        openSheet();

        expect(screen.getByText('No Executions')).toBeInTheDocument();
    });

    it('pages through the executions', async () => {
        mockExecutionsQueryResult([execution], 3);

        renderSheet();

        openSheet();

        await userEvent.click(screen.getByRole('link', {name: 'Go to next page'}));

        expect(hoisted.executionsQueryMock).toHaveBeenLastCalledWith(expect.objectContaining({pageNumber: 1}), true);
    });

    it('refreshes the executions', async () => {
        mockExecutionsQueryResult([execution]);

        renderSheet();

        openSheet();

        await userEvent.click(screen.getByRole('button', {name: 'Refresh workflow executions'}));

        expect(hoisted.refetchMock).toHaveBeenCalled();
    });

    it('shows the read-only template workflow on the Workflow tab', async () => {
        mockExecutionsQueryResult([execution]);

        renderSheet();

        openSheet();

        expect(hoisted.workflowQueryMock).not.toHaveBeenCalled();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow'}));

        expect(hoisted.workflowQueryMock).toHaveBeenLastCalledWith('draft-wf-1');
        expect(screen.getByText('Workflow panel draft-wf-1')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Refresh workflow executions'})).not.toBeInTheDocument();
    });

    it('closes the execution details together with the sheet', async () => {
        mockExecutionsQueryResult([execution]);

        renderSheet();

        openSheet();

        act(() => useWorkflowExecutionSheetStore.getState().setWorkflowExecutionSheetOpen(true));

        await userEvent.click(screen.getByRole('button', {name: 'Close'}));

        expect(useAutomationWorkflowExecutionsSheetStore.getState().automationWorkflowExecutionsSheetOpen).toBe(false);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);
    });
});
