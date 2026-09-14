import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionsTable from '../WorkflowExecutionsTable';

vi.mock('../WorkflowExecutionsDropdownMenu', () => ({
    default: () => <div data-testid="dropdown" />,
}));

const failedTriggerRow = {
    id: 77,
    project: {name: 'Sales'},
    projectDeployment: {name: 'Production'},
    triggerExecution: {
        endDate: new Date('2026-09-04T10:00:02Z'),
        id: '77',
        startDate: new Date('2026-09-04T10:00:00Z'),
        status: 'FAILED',
    },
    workflow: {label: 'Order intake'},
};

const jobRow = {
    id: 5,
    job: {
        endDate: new Date('2026-09-04T09:00:03Z'),
        id: '5',
        label: 'Order intake',
        startDate: new Date('2026-09-04T09:00:00Z'),
        status: 'COMPLETED',
    },
    project: {name: 'Sales'},
    projectDeployment: {name: 'Production'},
    workflow: {label: 'Order intake'},
};

describe('WorkflowExecutionsTable', () => {
    beforeEach(() => {
        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });
    });

    it('renders a failed trigger execution as a row without a job', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[failedTriggerRow as any]} />);

        expect(screen.getByText('FAILED')).toBeInTheDocument();
        expect(screen.getByText('Order intake')).toBeInTheDocument();
        expect(screen.getByText('Sales')).toBeInTheDocument();
    });

    it('opens the sheet for the trigger execution when its row is clicked', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[failedTriggerRow as any, jobRow as any]} />);

        screen.getByText('FAILED').closest('tr')!.click();

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(77);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('TRIGGER_EXECUTION');
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);

        screen.getByText('COMPLETED').closest('tr')!.click();

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(5);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
    });

    it('shows the project version the job ran with', () => {
        const row = {
            ...jobRow,
            job: {...jobRow.job, metadata: {projectVersion: 2}},
            projectDeployment: {name: 'Production', projectVersion: 3},
        };

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[row as any]} />);

        expect(screen.getByText('V2')).toBeInTheDocument();
        expect(screen.queryByText('V3')).not.toBeInTheDocument();
    });

    it('falls back to the deployment version when the job has no project version metadata', () => {
        const row = {...jobRow, projectDeployment: {name: 'Production', projectVersion: 3}};

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[row as any]} />);

        expect(screen.getByText('V3')).toBeInTheDocument();
    });

    it('pads the table by default and lets callers override the padding', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const {rerender} = render(<WorkflowExecutionsTable workflowExecutions={[jobRow as any]} />);

        expect(screen.getByTestId('workflow-executions-table')).toHaveClass('p-4', 'pt-0');

        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        rerender(<WorkflowExecutionsTable className="p-0" workflowExecutions={[jobRow as any]} />);

        expect(screen.getByTestId('workflow-executions-table')).toHaveClass('p-0');
        expect(screen.getByTestId('workflow-executions-table')).not.toHaveClass('p-4');
        expect(screen.getByTestId('workflow-executions-table')).not.toHaveClass('pt-0');
    });

    it('centers the actions menu under the Actions header', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[jobRow as any]} />);

        expect(screen.getByRole('columnheader', {name: 'Actions'})).toHaveClass('text-center');
        expect(screen.getByTestId('dropdown').closest('td')).toHaveClass('text-center');
        expect(screen.getByRole('columnheader', {name: 'Status'})).not.toHaveClass('text-center');
    });

    it('leaves the version empty when no version is known', () => {
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        render(<WorkflowExecutionsTable workflowExecutions={[jobRow as any]} />);

        expect(screen.queryByText(/^V\d+$/)).not.toBeInTheDocument();
    });
});
