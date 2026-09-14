import {TooltipProvider} from '@/components/ui/tooltip';
import useProjectDeploymentWorkflowSheetStore from '@/pages/automation/project-deployments/stores/useProjectDeploymentWorkflowSheetStore';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import useWorkflowExecutionSheetStore from '@/pages/automation/workflow-executions/stores/useWorkflowExecutionSheetStore';
import {Workflow} from '@/shared/middleware/automation/configuration';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {act, render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {ComponentProps} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProjectDeploymentWorkflowExecutionsSheet from '../ProjectDeploymentWorkflowExecutionsSheet';

const {detailQueryMock, queryMock, refetchMock, sheetContentPropsMock} = vi.hoisted(() => ({
    detailQueryMock: vi.fn((_request: {id: number}, enabled: boolean) => ({
        data: enabled ? {id: 5, job: {id: '5', metadata: {projectVersion: 2}}} : undefined,
    })),
    queryMock: vi.fn(),
    refetchMock: vi.fn(),
    sheetContentPropsMock: vi.fn(),
}));

vi.mock('@/components/ui/sheet', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/components/ui/sheet')>();

    return {
        ...actual,
        SheetContent: (props: ComponentProps<typeof actual.SheetContent>) => {
            sheetContentPropsMock(props);

            return <actual.SheetContent {...props} />;
        },
    };
});

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: detailQueryMock,
    useGetWorkspaceProjectWorkflowExecutionsQuery: queryMock,
}));

vi.mock('@/shared/mutations/platform/jobs.mutations', () => ({
    useStopJobMutation: () => ({mutate: vi.fn()}),
}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail', () => ({
    default: ({workflowExecutionId}: {workflowExecutionId: number}) => (
        <div>{`Execution detail ${workflowExecutionId}`}</div>
    ),
}));

vi.mock('../ProjectDeploymentWorkflowPanel', () => ({
    default: ({workflow}: {workflow: Workflow}) => <div>{`Workflow panel ${workflow.id}`}</div>,
}));

const mockQueryResult = (content: object[], totalPages = 1) =>
    queryMock.mockReturnValue({
        data: {content, size: 20, totalElements: content.length, totalPages},
        error: null,
        isFetching: false,
        isLoading: false,
        refetch: refetchMock,
    });

const renderSheet = () =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <ProjectDeploymentWorkflowExecutionsSheet />
            </TooltipProvider>
        </QueryClientProvider>
    );

const openSheet = () =>
    useProjectDeploymentWorkflowSheetStore.getState().openProjectDeploymentWorkflowSheet({
        projectDeploymentId: 3,
        projectName: 'Subflow',
        projectVersion: 3,
        workflow: {id: 'workflow1', label: 'workflow1'} as Workflow,
    });

const jobExecution = {
    id: 5,
    job: {
        endDate: '2026-09-14T10:00:01.250Z',
        id: '5',
        startDate: '2026-09-14T10:00:00.000Z',
        status: 'COMPLETED',
    },
};

describe('ProjectDeploymentWorkflowExecutionsSheet', () => {
    beforeEach(() => {
        detailQueryMock.mockClear();
        queryMock.mockReset();
        refetchMock.mockReset();

        useProjectDeploymentWorkflowSheetStore.setState({
            projectDeploymentId: undefined,
            projectDeploymentWorkflowSheetOpen: false,
            projectName: undefined,
            workflow: undefined,
        });

        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });

        useWorkspaceStore.setState({currentWorkspaceId: 1});
    });

    it('renders nothing while closed', () => {
        mockQueryResult([]);

        renderSheet();

        expect(screen.queryByRole('tab')).not.toBeInTheDocument();
        expect(queryMock).not.toHaveBeenCalled();
    });

    it('shows one header with project, workflow and tabs and fetches only that workflow in the deployment', () => {
        mockQueryResult([]);

        openSheet();

        renderSheet();

        expect(document.querySelectorAll('header')).toHaveLength(1);
        expect(document.querySelector('header')).toHaveTextContent('Subflow /workflow1');
        expect(screen.getByRole('tab', {name: 'Executions'})).toHaveAttribute('data-state', 'active');
        expect(screen.getByRole('tab', {name: 'Workflow'})).toHaveAttribute('data-state', 'inactive');
        expect(screen.getByText('No Executions')).toBeInTheDocument();
        expect(queryMock).toHaveBeenCalledWith(
            {id: 1, pageNumber: 0, projectDeploymentId: 3, workflowId: 'workflow1'},
            true
        );
    });

    it('shows the deployment version in the list header and the executed version in the execution detail', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        expect(document.querySelector('header')).toHaveTextContent('Subflow /workflow1/ V3');

        await user.click(screen.getByText('COMPLETED'));

        expect(screen.getByText('Execution detail 5')).toBeInTheDocument();
        expect(document.querySelector('header')).toHaveTextContent('Subflow /workflow1/ V2');
    });

    it('switches to the read-only workflow tab and back', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        expect(screen.queryByText('Workflow panel workflow1')).not.toBeInTheDocument();

        await user.click(screen.getByRole('tab', {name: 'Workflow'}));

        expect(screen.getByText('Workflow panel workflow1')).toBeInTheDocument();
        expect(screen.getByText('COMPLETED').closest('.hidden')).not.toBeNull();
        expect(screen.queryByLabelText('Refresh workflow executions')).not.toBeInTheDocument();

        await user.click(screen.getByRole('tab', {name: 'Executions'}));

        expect(screen.queryByText('Workflow panel workflow1')).not.toBeInTheDocument();
        expect(screen.getByText('COMPLETED').closest('.hidden')).toBeNull();
    });

    it('opens a clicked job execution as a job', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        expect(screen.getByText('1250ms')).toBeInTheDocument();

        await user.click(screen.getByText('COMPLETED'));

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(5);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
    });

    it('shows the execution detail in place and goes back to the list with the back arrow', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        expect(screen.queryByLabelText('Back to executions')).not.toBeInTheDocument();

        await user.click(screen.getByText('COMPLETED'));

        expect(screen.getByText('Execution detail 5')).toBeInTheDocument();
        expect(screen.getByText('COMPLETED').closest('.hidden')).not.toBeNull();
        expect(screen.queryByLabelText('Refresh workflow executions')).not.toBeInTheDocument();
        expect(screen.queryByRole('tab')).not.toBeInTheDocument();

        await user.click(screen.getByLabelText('Back to executions'));

        expect(screen.queryByText('Execution detail 5')).not.toBeInTheDocument();
        expect(screen.getByRole('tab', {name: 'Executions'})).toHaveAttribute('data-state', 'active');
        expect(screen.getByText('COMPLETED').closest('.hidden')).toBeNull();
        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(true);
    });

    it('closes the execution detail together with the sheet so reopening shows the list', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        await user.click(screen.getByText('COMPLETED'));

        expect(screen.getByText('Execution detail 5')).toBeInTheDocument();

        await user.keyboard('{Escape}');

        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(false);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(false);

        openSheet();

        expect(await screen.findByRole('tab', {name: 'Executions'})).toBeInTheDocument();
        expect(screen.queryByText('Execution detail 5')).not.toBeInTheDocument();
    });

    it('prevents closing when clicking or focusing outside the sheet', () => {
        mockQueryResult([jobExecution]);

        openSheet();

        sheetContentPropsMock.mockClear();

        renderSheet();

        const {onFocusOutside, onPointerDownOutside} = sheetContentPropsMock.mock.lastCall![0];

        const pointerDownOutsideEvent = {preventDefault: vi.fn()};
        const focusOutsideEvent = {preventDefault: vi.fn()};

        expect(onPointerDownOutside).toBeTypeOf('function');
        expect(onFocusOutside).toBeTypeOf('function');

        onPointerDownOutside(pointerDownOutsideEvent);
        onFocusOutside(focusOutsideEvent);

        expect(pointerDownOutsideEvent.preventDefault).toHaveBeenCalledTimes(1);
        expect(focusOutsideEvent.preventDefault).toHaveBeenCalledTimes(1);
        expect(useProjectDeploymentWorkflowSheetStore.getState().projectDeploymentWorkflowSheetOpen).toBe(true);
    });

    it('renders the executions table and pagination edge to edge without padding around them', () => {
        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        const island = screen.getByTestId('workflow-executions-island');

        expect(island).toHaveClass('rounded-b-md', 'bg-surface-neutral-primary');
        expect(island.parentElement?.className).not.toMatch(/(^|\s)p-\d/);
        expect(island).toContainElement(screen.getByText('COMPLETED'));
        expect(island).toContainElement(screen.getByText(/Showing 1 to 1 of 1 results/));
        expect(screen.getByTestId('workflow-executions-table')).toHaveClass('p-0');
        expect(island.querySelector('.pt-4')).toBeNull();
    });

    it('refetches the executions when the refresh button is clicked', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        await user.click(screen.getByLabelText('Refresh workflow executions'));

        expect(refetchMock).toHaveBeenCalledTimes(1);
    });

    it('fetches the opened execution only while its detail is shown', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution]);

        openSheet();

        renderSheet();

        expect(detailQueryMock).toHaveBeenLastCalledWith({id: 0}, false, undefined, 'JOB');

        await user.click(screen.getByText('COMPLETED'));

        expect(detailQueryMock).toHaveBeenLastCalledWith({id: 5}, true, undefined, 'JOB');

        await user.click(screen.getByLabelText('Back to executions'));

        expect(detailQueryMock).toHaveBeenLastCalledWith({id: 5}, false, undefined, 'JOB');
    });

    it('starts on the executions tab and the first page when another workflow is opened', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution], 3);

        openSheet();

        renderSheet();

        await user.click(screen.getByText('2'));

        expect(queryMock).toHaveBeenLastCalledWith(expect.objectContaining({pageNumber: 1}), true);

        await user.click(screen.getByRole('tab', {name: 'Workflow'}));

        expect(screen.getByText('Workflow panel workflow1')).toBeInTheDocument();

        act(() => {
            useProjectDeploymentWorkflowSheetStore.getState().openProjectDeploymentWorkflowSheet({
                projectDeploymentId: 3,
                projectName: 'Subflow',
                workflow: {id: 'workflow2', label: 'workflow2'} as Workflow,
            });
        });

        expect(screen.getByRole('tab', {name: 'Executions'})).toHaveAttribute('data-state', 'active');
        expect(screen.queryByText('Workflow panel workflow1')).not.toBeInTheDocument();
        expect(document.querySelector('header')).toHaveTextContent('Subflow /workflow2');
        expect(queryMock).toHaveBeenLastCalledWith(
            {id: 1, pageNumber: 0, projectDeploymentId: 3, workflowId: 'workflow2'},
            true
        );
    });

    it('opens a trigger-only execution as a trigger execution', async () => {
        const user = userEvent.setup();

        mockQueryResult([{id: 77, triggerExecution: {id: 77, status: 'FAILED'}}]);

        openSheet();

        renderSheet();

        await user.click(screen.getByText('FAILED'));

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(77);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('TRIGGER_EXECUTION');
    });

    it('pages through executions', async () => {
        const user = userEvent.setup();

        mockQueryResult([jobExecution], 3);

        openSheet();

        renderSheet();

        await user.click(screen.getByText('2'));

        expect(queryMock).toHaveBeenLastCalledWith(expect.objectContaining({pageNumber: 1}), true);
    });
});
