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
});
