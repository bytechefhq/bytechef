import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {executionQueryMock} = vi.hoisted(() => ({executionQueryMock: vi.fn()}));

vi.mock('@/shared/queries/automation/workflowExecutions.queries', () => ({
    useGetProjectWorkflowExecutionQuery: executionQueryMock,
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
});
