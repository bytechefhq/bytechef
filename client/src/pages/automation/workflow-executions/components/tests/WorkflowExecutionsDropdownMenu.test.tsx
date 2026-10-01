import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useWorkflowExecutionSheetStore from '../../stores/useWorkflowExecutionSheetStore';
import WorkflowExecutionsDropdownMenu from '../WorkflowExecutionsDropdownMenu';

const {restartJobMutateMock, stopJobMutateMock} = vi.hoisted(() => ({
    restartJobMutateMock: vi.fn(),
    stopJobMutateMock: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/jobs.mutations', () => ({
    useRestartJobMutation: () => ({mutate: restartJobMutateMock}),
    useStopJobMutation: () => ({mutate: stopJobMutateMock}),
}));

const failedTriggerExecution = {
    id: 77,
    triggerExecution: {id: '77', status: 'FAILED'},
} as unknown as WorkflowExecution;

const failedJobExecution = {
    id: 9,
    job: {id: '9', status: 'FAILED'},
} as unknown as WorkflowExecution;

const failedSubflowChildJobExecution = {
    id: 12,
    job: {id: '12', parentTaskExecutionId: 30, status: 'FAILED'},
} as unknown as WorkflowExecution;

const jobExecution = {
    id: 5,
    job: {id: '5', status: 'STARTED'},
} as unknown as WorkflowExecution;

const renderMenu = (execution: WorkflowExecution) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <WorkflowExecutionsDropdownMenu execution={execution} />
        </QueryClientProvider>
    );

describe('WorkflowExecutionsDropdownMenu', () => {
    beforeEach(() => {
        restartJobMutateMock.mockReset();
        stopJobMutateMock.mockReset();

        useWorkflowExecutionSheetStore.setState({
            workflowExecutionId: 0,
            workflowExecutionKind: 'JOB',
            workflowExecutionSheetOpen: false,
        });
    });

    it('opens a trigger-only row as a trigger execution, which has no job to fetch', async () => {
        const user = userEvent.setup();

        renderMenu(failedTriggerExecution);

        await user.click(screen.getByRole('button'));
        await user.click(await screen.findByText('View'));

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(77);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('TRIGGER_EXECUTION');
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionSheetOpen).toBe(true);
    });

    it('opens a job-backed row as a job', async () => {
        const user = userEvent.setup();

        renderMenu(jobExecution);

        await user.click(screen.getByRole('button'));
        await user.click(await screen.findByText('View'));

        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionId).toBe(5);
        expect(useWorkflowExecutionSheetStore.getState().workflowExecutionKind).toBe('JOB');
    });

    it('aligns the menu to the end of the actions button', async () => {
        const user = userEvent.setup();

        renderMenu(jobExecution);

        await user.click(screen.getByRole('button'));

        expect(await screen.findByRole('menu')).toHaveAttribute('data-align', 'end');
    });

    it('restarts a failed job', async () => {
        const user = userEvent.setup();

        renderMenu(failedJobExecution);

        await user.click(screen.getByRole('button'));
        await user.click(await screen.findByText('Restart'));

        expect(restartJobMutateMock).toHaveBeenCalledWith(9);
    });

    it('disables Restart for a job that has not failed', async () => {
        const user = userEvent.setup();

        renderMenu(jobExecution);

        await user.click(screen.getByRole('button'));

        expect(await screen.findByRole('menuitem', {name: /Restart/})).toHaveAttribute('data-disabled');
    });

    it('disables Restart for a trigger-only row', async () => {
        const user = userEvent.setup();

        renderMenu(failedTriggerExecution);

        await user.click(screen.getByRole('button'));

        expect(await screen.findByRole('menuitem', {name: /Restart/})).toHaveAttribute('data-disabled');
    });

    it('disables Restart for a failed subflow child job, which cannot be resumed on its own', async () => {
        const user = userEvent.setup();

        renderMenu(failedSubflowChildJobExecution);

        await user.click(screen.getByRole('button'));

        expect(await screen.findByRole('menuitem', {name: /Restart/})).toHaveAttribute('data-disabled');
    });
});
