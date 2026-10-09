import {TooltipProvider} from '@/components/ui/tooltip';
import {fireEvent, render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
}));

vi.mock('../WorkflowExecutionSheetContent', () => ({
    default: ({headerActions}: {headerActions?: React.ReactNode}) => (
        <div data-testid="sheet-content">{headerActions}</div>
    ),
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

    it('hides and shows the workflow panel with the toggle in the execution panel', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, job: {id: '5', taskExecutions: []}, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.getByTestId('workflow-panel')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Hide workflow'}));

        expect(screen.queryByTestId('workflow-panel')).not.toBeInTheDocument();
        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();

        fireEvent.click(screen.getByRole('button', {name: 'Show workflow'}));

        expect(screen.getByTestId('workflow-panel')).toBeInTheDocument();
    });

    it('leaves the panel out until there is a job or a trigger execution to show', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />, {wrapper: TooltipProvider});

        expect(screen.queryByTestId('sheet-content')).not.toBeInTheDocument();
    });
});
