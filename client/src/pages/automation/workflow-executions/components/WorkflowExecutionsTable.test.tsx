import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it, vi} from 'vitest';

import {formatDateTime} from '../utils/workflowExecutionsTable';
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

    it('shows the duration of a completed job', () => {
        render(<WorkflowExecutionsTable workflowExecutions={workflowExecutions} />);

        expect(screen.getByText('5000ms')).toBeInTheDocument();
    });

    it('shows no duration and no end date for a running job whose trigger already ended', () => {
        const runningExecutions = [
            {
                id: 2,
                job: {
                    id: '2',
                    label: 'Agent Tool Calls',
                    startDate: new Date('2026-10-20T10:00:10Z'),
                    status: 'STARTED',
                },
                project: {name: 'Agent Run Demos'},
                projectDeployment: {name: 'Agent Run Demos', projectVersion: 1},
                triggerExecution: {
                    endDate: new Date('2026-10-20T10:00:02Z'),
                    startDate: new Date('2026-10-20T10:00:01Z'),
                },
            },
        ] as unknown as WorkflowExecution[];

        render(<WorkflowExecutionsTable workflowExecutions={runningExecutions} />);

        expect(screen.queryByText(/ms$/)).not.toBeInTheDocument();
        expect(screen.queryByText(formatDateTime(new Date('2026-10-20T10:00:02Z')))).not.toBeInTheDocument();
    });

    it('takes the duration of an execution without a job from its trigger', () => {
        const triggerOnlyExecutions = [
            {
                id: 3,
                project: {name: 'Load Test'},
                projectDeployment: {name: 'Load Test', projectVersion: 2},
                triggerExecution: {
                    endDate: new Date('2026-10-20T10:00:03Z'),
                    startDate: new Date('2026-10-20T10:00:01Z'),
                },
            },
        ] as unknown as WorkflowExecution[];

        render(<WorkflowExecutionsTable workflowExecutions={triggerOnlyExecutions} />);

        expect(screen.getByText('2000ms')).toBeInTheDocument();
    });
});
