import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
}));

vi.mock('../WorkflowExecutionSheetContent', () => ({
    default: () => <div data-testid="sheet-content" />,
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

        render(<WorkflowExecutionDetail workflowExecutionId={501} />);

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

        render(<WorkflowExecutionDetail workflowExecutionId={77} />);

        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('shows the execution panel for a job-backed row', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, job: {id: '5', taskExecutions: []}, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />);

        expect(screen.getByTestId('sheet-content')).toBeInTheDocument();
    });

    it('leaves the panel out until there is a job or a trigger execution to show', () => {
        executionQueryMock.mockReturnValue({
            data: {id: 5, workflow: {label: 'Order intake'}},
            isLoading: false,
        });

        render(<WorkflowExecutionDetail workflowExecutionId={5} />);

        expect(screen.queryByTestId('sheet-content')).not.toBeInTheDocument();
    });
});
