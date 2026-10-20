import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsTable from './WorkflowExecutionsTable';

vi.mock('../hooks/useWorkflowExecutionsTable', () => ({
    useWorkflowExecutionsTable: () => ({
        expandedJobIds: new Set<string>(),
        handleRowClick: vi.fn(),
        handleToggleExpand: vi.fn(),
    }),
}));

vi.mock('./WorkflowExecutionsDropdownMenu', () => ({
    default: () => <button>execution menu</button>,
}));

const workflowExecutions = [
    {
        id: 1,
        job: {
            endDate: new Date('2026-10-20T10:00:05Z'),
            id: '1',
            label: 'Heavy orders load test',
            startDate: new Date('2026-10-20T10:00:00Z'),
            status: 'COMPLETED',
        },
        project: {name: 'Load Test'},
        projectDeployment: {name: 'Load Test', projectVersion: 2},
    },
] as unknown as WorkflowExecution[];

describe('WorkflowExecutionsTable', () => {
    it('pins the Actions column to the right edge of the scrolling table', () => {
        render(<WorkflowExecutionsTable workflowExecutions={workflowExecutions} />);

        const actionsHeader = screen.getByText('Actions').closest('th');
        const actionsCell = screen.getByText('execution menu').closest('td');

        expect(actionsHeader).toHaveClass('sticky', 'right-0');
        expect(actionsCell).toHaveClass('sticky', 'right-0');
    });

    it('leaves the other columns unpinned', () => {
        render(<WorkflowExecutionsTable workflowExecutions={workflowExecutions} />);

        const workflowHeader = screen.getByText('Workflow').closest('th');

        expect(workflowHeader).not.toHaveClass('sticky');
    });
});
