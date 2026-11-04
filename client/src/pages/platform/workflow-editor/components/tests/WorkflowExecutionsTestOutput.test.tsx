import {Job, TaskExecution, TriggerExecution, WorkflowTestExecution} from '@/shared/middleware/platform/workflow/test';
import {render, screen} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import WorkflowExecutionsTestOutput from '../WorkflowExecutionsTestOutput';

const {workflowExecutionsHookMock} = vi.hoisted(() => ({
    workflowExecutionsHookMock: vi.fn(),
}));

vi.mock('../properties/hooks/useWorkflowExecutions', () => ({
    default: () => workflowExecutionsHookMock(),
}));

vi.mock('@/shared/components/workflow-executions/WorkflowExecutionsTabsPanel', () => ({default: () => null}));

const idleWorkflowExecutions = {
    activeTab: 'output',
    deepestFailedExecution: undefined,
    dialogOpen: false,
    handleBreadcrumbNavigate: vi.fn(),
    handleExecutionClick: vi.fn(),
    handleSeeExecutions: vi.fn(),
    isTriggerExecution: false,
    job: undefined,
    jobFailedWithNoExecutions: false,
    jobFailureError: undefined,
    rootJob: undefined,
    selectedExecution: undefined,
    setActiveTab: vi.fn(),
    setDialogOpen: vi.fn(),
    subflowStack: [],
    taskExecutions: [],
    triggerExecution: undefined,
};

const SELECTED_ROW_CLASS_NAME = 'bg-surface-neutral-secondary';

describe('WorkflowExecutionsTestOutput', () => {
    beforeEach(() => {
        workflowExecutionsHookMock.mockReturnValue(idleWorkflowExecutions);
    });

    it('lays out the task list and details panes while a run waits for its first progress snapshot', () => {
        render(<WorkflowExecutionsTestOutput workflowIsRunning />);

        expect(screen.queryByText('Workflow is running...')).not.toBeInTheDocument();
        expect(screen.getByTestId('workflow-test-output-pending')).toBeInTheDocument();
    });

    it('draws placeholders in the details pane while a run waits for its first progress snapshot', () => {
        render(<WorkflowExecutionsTestOutput workflowIsRunning />);

        const detailsPane = screen.getByTestId('workflow-test-output-pending-details');

        expect(detailsPane.querySelectorAll('[data-slot="skeleton"]').length).toBeGreaterThan(0);
    });

    it('draws placeholders for the status row while a run waits for its first progress snapshot', () => {
        render(<WorkflowExecutionsTestOutput workflowIsRunning />);

        const statusRow = screen.getByTestId('workflow-test-output-pending-header');

        expect(statusRow.querySelectorAll('[data-slot="skeleton"]').length).toBe(4);
        expect(screen.queryByText('Test Output')).not.toBeInTheDocument();
    });

    it('labels the panel Test Output when no run has started', () => {
        render(<WorkflowExecutionsTestOutput workflowIsRunning={false} />);

        expect(screen.getByText('Test Output')).toBeInTheDocument();
        expect(screen.queryByTestId('workflow-test-output-pending-header')).not.toBeInTheDocument();
    });

    describe('a trigger execution and a task execution with the same id', () => {
        const triggerExecution = {
            id: '34',
            startDate: new Date('2026-11-05T10:00:00Z'),
            status: 'COMPLETED',
            workflowTrigger: {label: 'Webhook', name: 'trigger_1'},
        } as unknown as TriggerExecution;

        const taskExecution = {
            id: '34',
            jobId: '34',
            priority: 0,
            startDate: new Date('2026-11-05T10:00:01Z'),
            status: 'COMPLETED',
            title: 'Var',
            workflowTask: {label: 'Var', name: 'var_1'},
        } as unknown as TaskExecution;

        const job = {id: '34', priority: 0, status: 'COMPLETED', taskExecutions: [taskExecution]} as unknown as Job;

        const renderSelected = (selectedExecution: TaskExecution | TriggerExecution, isTriggerExecution: boolean) => {
            workflowExecutionsHookMock.mockReturnValue({
                ...idleWorkflowExecutions,
                isTriggerExecution,
                job,
                rootJob: job,
                selectedExecution,
                taskExecutions: [taskExecution],
                triggerExecution,
            });

            render(
                <WorkflowExecutionsTestOutput
                    workflowIsRunning={false}
                    workflowTestExecution={{job, triggerExecution} as WorkflowTestExecution}
                />
            );

            return screen
                .getAllByRole('button')
                .filter((button) => button.className.includes(SELECTED_ROW_CLASS_NAME))
                .map((button) => button.textContent);
        };

        it('highlights only the trigger row when the trigger is selected', () => {
            const selectedRowTexts = renderSelected(triggerExecution, true);

            expect(selectedRowTexts).toHaveLength(1);
            expect(selectedRowTexts[0]).toContain('Webhook');
        });

        it('highlights only the task row when the task is selected', () => {
            const selectedRowTexts = renderSelected(taskExecution, false);

            expect(selectedRowTexts).toHaveLength(1);
            expect(selectedRowTexts[0]).toContain('Var');
        });
    });
});
