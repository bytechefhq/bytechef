import {TooltipProvider} from '@/components/ui/tooltip';
import {act, fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock, sheetContentPropsMock, taskExecutionQueryMock} = vi.hoisted(() => ({
    executionQueryMock: vi.fn(),
    sheetContentPropsMock: vi.fn(),
    taskExecutionQueryMock: vi.fn(),
}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
    useGetWorkflowExecutionTaskExecutionQuery: taskExecutionQueryMock,
}));

vi.mock('@tanstack/react-query', () => ({
    useQueryClient: () => ({invalidateQueries: vi.fn()}),
}));

vi.mock('../WorkflowExecutionSheetContent', () => ({
    default: (props: {headerActions?: React.ReactNode}) => {
        sheetContentPropsMock(props);

        return <div data-testid="sheet-content">{props.headerActions}</div>;
    },
}));

vi.mock('../WorkflowExecutionSheetWorkflowPanel', () => ({
    default: () => <div data-testid="workflow-panel" />,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: []}),
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    WorkflowReadOnlyProvider: ({children}: {children: React.ReactNode}) => children,
}));

const WorkflowExecutionDetail = (await import('../WorkflowExecutionDetail')).default;

describe('WorkflowExecutionDetail', () => {
    beforeEach(() => {
        executionQueryMock.mockReset();
        sheetContentPropsMock.mockReset();
        taskExecutionQueryMock.mockReset();
        taskExecutionQueryMock.mockReturnValue({data: undefined, isError: false});
    });

    it('shows a loading state while the execution is loading', () => {
        executionQueryMock.mockReturnValue({data: undefined, isLoading: true});

        render(<WorkflowExecutionDetail workflowExecutionId={501} />, {wrapper: TooltipProvider});

        expect(screen.getByTestId('workflow-execution-detail-loading')).toBeInTheDocument();
    });

    it('shows a retry control when the execution fails to load', () => {
        const refetch = vi.fn();

        executionQueryMock.mockReturnValue({data: undefined, isError: true, isLoading: false, refetch});

        render(<WorkflowExecutionDetail workflowExecutionId={501} />, {wrapper: TooltipProvider});

        expect(screen.getByTestId('workflow-execution-detail-error')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Retry'}));

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('offers a way back to the parent when a subflow fails to load', () => {
        const refetch = vi.fn();

        executionQueryMock.mockImplementation(({id}: {id: number}) =>
            id === 501
                ? {data: {id: 501, job: {id: '501', label: 'Parent', status: 'COMPLETED'}}, isLoading: false}
                : {data: undefined, isError: true, isLoading: false, refetch}
        );

        render(<WorkflowExecutionDetail workflowExecutionId={501} />, {wrapper: TooltipProvider});

        const {handleSeeExecutions} = sheetContentPropsMock.mock.calls.at(-1)![0];

        act(() => handleSeeExecutions({id: '900', label: 'Child'}));

        expect(screen.getByTestId('workflow-execution-detail-error')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Back to parent'}));

        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('shows no way back when the root execution fails to load', () => {
        executionQueryMock.mockReturnValue({data: undefined, isError: true, isLoading: false, refetch: vi.fn()});

        render(<WorkflowExecutionDetail workflowExecutionId={501} />, {wrapper: TooltipProvider});

        expect(screen.queryByRole('button', {name: 'Back to parent'})).not.toBeInTheDocument();
    });

    it('shows the execution panel for a trigger-only row, which has no job', () => {
        executionQueryMock.mockReturnValue({
            data: {
                id: 77,
                triggerExecution: {
                    error: {message: 'Signature check failed'},
                    id: '77',
                    workflowTrigger: {name: 'trigger_1'},
                },
                workflow: {label: 'Order intake'},
            },
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={77} />, {wrapper: TooltipProvider});

        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('shows the execution panel for a job-backed row', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, job: {id: '5', taskExecutions: []}, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('starts with the workflow panel hidden and shows or hides it with the toggle', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, job: {id: '5', taskExecutions: []}, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.queryByTestId('workflow-panel')).not.toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Show workflow'}));

        expect(screen.getByTestId('workflow-panel')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Hide workflow'}));

        expect(screen.queryByTestId('workflow-panel')).not.toBeInTheDocument();
        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('leaves the panel out until there is a job or a trigger execution to show', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.queryByTestId('sheet-content')).not.toBeInTheDocument();
    });

    it('passes the lazy options, the focused task and the task execution count to the panel', () => {
        const focusedTaskExecution = {error: {message: 'Order rejected'}, id: '9', jobId: '5', status: 'FAILED'};

        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: '1'}, {taskExecutionId: '9'}],
                id: 5,
                job: {id: '5', status: 'FAILED', taskExecutions: [{childCount: 1, id: '1', jobId: '5'}]},
                taskExecutionCount: 112464,
                workflow: {label: 'Order intake'},
            },
            isLoading: false,
        });
        taskExecutionQueryMock.mockReturnValue({data: focusedTaskExecution, isError: false});

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(sheetContentPropsMock).toHaveBeenLastCalledWith(
            expect.objectContaining({
                focusedExecution: {execution: focusedTaskExecution, path: ['1', '9']},
                lazy: {focusPath: expect.any(Array), refetchInterval: false, workflowExecutionId: 5},
                selectedItem: focusedTaskExecution,
                taskExecutionCount: 112464,
            })
        );
    });

    it('reports a failed focused task load to the panel and retries it', () => {
        const refetch = vi.fn().mockResolvedValue({data: undefined});

        executionQueryMock.mockReturnValue({
            data: {
                focusPath: [{taskExecutionId: '1'}, {taskExecutionId: '9'}],
                id: 5,
                job: {id: '5', status: 'FAILED', taskExecutions: [{childCount: 1, id: '1', jobId: '5'}]},
            },
            isLoading: false,
        });
        taskExecutionQueryMock.mockReturnValue({data: undefined, isError: true, refetch});

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        const props = sheetContentPropsMock.mock.lastCall![0];

        expect(props.focusedTaskExecutionError).toBe(true);

        props.handleFocusedTaskExecutionRetry();

        expect(refetch).toHaveBeenCalledTimes(1);
    });

    it('keeps showing a loaded execution when a refetch fails and passes a live updates retry to the panel', () => {
        const refetch = vi.fn();

        executionQueryMock.mockReturnValue({
            data: {focusPath: [], id: 5, job: {id: '5', status: 'STARTED', taskExecutions: []}},
            isError: true,
            isLoading: false,
            refetch,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.queryByTestId('workflow-execution-detail-error')).not.toBeInTheDocument();
        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();

        const props = sheetContentPropsMock.mock.lastCall![0];

        expect(props.liveUpdatesError).toBe(true);

        props.handleLiveUpdatesRetry();

        expect(refetch).toHaveBeenCalledTimes(1);
    });
});
