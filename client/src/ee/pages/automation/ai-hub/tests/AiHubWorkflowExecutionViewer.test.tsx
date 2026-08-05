import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const {detailMock} = vi.hoisted(() => ({detailMock: vi.fn()}));

vi.mock('@/pages/automation/workflow-executions/components/workflow-execution-sheet/WorkflowExecutionDetail', () => ({
    default: detailMock,
}));

const AiHubWorkflowExecutionViewer = (await import('../AiHubWorkflowExecutionViewer')).default;

describe('AiHubWorkflowExecutionViewer', () => {
    beforeEach(() => {
        detailMock.mockReset();
        detailMock.mockImplementation(({workflowExecutionId}: {workflowExecutionId: number}) => (
            <div data-testid="detail">{workflowExecutionId}</div>
        ));
    });

    it('renders the execution detail for the given execution id', () => {
        render(<AiHubWorkflowExecutionViewer workflowExecutionId={501} />);

        expect(screen.getByTestId('detail')).toHaveTextContent('501');
    });
});
