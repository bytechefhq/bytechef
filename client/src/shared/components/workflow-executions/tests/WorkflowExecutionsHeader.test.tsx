import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {render, screen} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import WorkflowExecutionsHeader from '../WorkflowExecutionsHeader';

function createJob(overrides: Partial<Job> = {}): Job {
    return {
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        ...overrides,
    } as Job;
}

function createTriggerExecution(overrides: Partial<TriggerExecution> = {}): TriggerExecution {
    return {
        priority: 1,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        ...overrides,
    } as TriggerExecution;
}

describe('WorkflowExecutionsHeader', () => {
    describe('badge display', () => {
        it('should display DONE badge when workflow is completed', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('DONE')).toBeInTheDocument();
        });

        it('should display Running badge when workflow is running', () => {
            const job = createJob({
                endDate: undefined,
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'STARTED',
                taskExecutions: [{} as never],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('Running')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when workflow has failed', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'FAILED',
                taskExecutions: [{} as never],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });

        it('should display Running badge when job is CREATED (not yet started)', () => {
            const job = createJob({
                endDate: undefined,
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'CREATED',
                taskExecutions: [],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('Running')).toBeInTheDocument();
        });
    });

    describe('badge display with triggerExecution', () => {
        it('should display DONE badge when both job and trigger are completed', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'COMPLETED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('DONE')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when job is completed but trigger failed', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'FAILED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when job is completed but trigger is still running', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'STARTED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });

        it('should display Running badge when both job and trigger are running', () => {
            const job = createJob({
                endDate: undefined,
                status: 'STARTED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'STARTED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Running')).toBeInTheDocument();
        });

        it('should display Running badge when job is running and trigger is CREATED', () => {
            const job = createJob({
                endDate: undefined,
                status: 'STARTED',
                taskExecutions: [],
            });
            const triggerExecution = createTriggerExecution({status: 'CREATED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Running')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when job is running but trigger failed', () => {
            const job = createJob({
                endDate: undefined,
                status: 'STARTED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'FAILED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when job failed regardless of trigger status', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'FAILED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'COMPLETED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });

        it('should display Workflow failed badge when trigger is cancelled', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });
            const triggerExecution = createTriggerExecution({status: 'CANCELLED'});

            render(<WorkflowExecutionsHeader job={job} triggerExecution={triggerExecution} />);

            expect(screen.getByText('Workflow failed')).toBeInTheDocument();
        });
    });

    describe('execution details', () => {
        it('should display duration when start and end dates are provided', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
                taskExecutions: [],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('Duration: 5000ms')).toBeInTheDocument();
        });

        it('should display task count with singular form for one task', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
                taskExecutions: [{} as never],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('1 task executed')).toBeInTheDocument();
        });

        it('should display task count with plural form for multiple tasks', () => {
            const job = createJob({
                endDate: new Date('2024-01-01T10:00:05'),
                startDate: new Date('2024-01-01T10:00:00'),
                status: 'COMPLETED',
                taskExecutions: [{} as never, {} as never, {} as never],
            });

            render(<WorkflowExecutionsHeader job={job} />);

            expect(screen.getByText('3 tasks executed')).toBeInTheDocument();
        });
    });
});
