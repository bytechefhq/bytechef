import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@testing-library/react';
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
                focusPath: [{taskExecutionId: 1}, {taskExecutionId: 9}],
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
                lazy: {refetchInterval: false, workflowExecutionId: 5},
                selectedItem: focusedTaskExecution,
                taskExecutionCount: 112464,
            })
        );
    });
});
