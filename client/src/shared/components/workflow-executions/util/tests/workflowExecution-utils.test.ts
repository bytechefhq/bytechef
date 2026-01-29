import {getWorkflowStatusType} from '@/shared/components/workflow-executions/util/workflowExecution-utils';
import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {describe, expect, it} from 'vitest';

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
        batch: false,
        maxRetries: 0,
        priority: 1,
        retryAttempts: 0,
        retryDelay: 0,
        retryDelayFactor: 0,
        startDate: new Date('2024-01-01T10:00:00'),
        status: 'COMPLETED',
        ...overrides,
    } as TriggerExecution;
}

describe('getWorkflowStatusType', () => {
    describe('completed status', () => {
        it('should return completed when job is COMPLETED and no trigger execution', () => {
            const job = createJob({status: 'COMPLETED'});

            expect(getWorkflowStatusType(job)).toBe('completed');
        });

        it('should return completed when job is COMPLETED and trigger is COMPLETED', () => {
            const job = createJob({status: 'COMPLETED'});
            const trigger = createTriggerExecution({status: 'COMPLETED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('completed');
        });
    });

    describe('running status', () => {
        it('should return running when job is STARTED and no trigger execution', () => {
            const job = createJob({status: 'STARTED'});

            expect(getWorkflowStatusType(job)).toBe('running');
        });

        it('should return running when job is CREATED and no trigger execution', () => {
            const job = createJob({status: 'CREATED'});

            expect(getWorkflowStatusType(job)).toBe('running');
        });

        it('should return running when job is STARTED and trigger is STARTED', () => {
            const job = createJob({status: 'STARTED'});
            const trigger = createTriggerExecution({status: 'STARTED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('running');
        });

        it('should return running when job is STARTED and trigger is CREATED', () => {
            const job = createJob({status: 'STARTED'});
            const trigger = createTriggerExecution({status: 'CREATED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('running');
        });

        it('should return running when job is CREATED and trigger is STARTED', () => {
            const job = createJob({status: 'CREATED'});
            const trigger = createTriggerExecution({status: 'STARTED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('running');
        });

        it('should return running when job is CREATED and trigger is CREATED', () => {
            const job = createJob({status: 'CREATED'});
            const trigger = createTriggerExecution({status: 'CREATED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('running');
        });
    });

    describe('failed status', () => {
        it('should return failed when job is FAILED and no trigger execution', () => {
            const job = createJob({status: 'FAILED'});

            expect(getWorkflowStatusType(job)).toBe('failed');
        });

        it('should return failed when job is STOPPED and no trigger execution', () => {
            const job = createJob({status: 'STOPPED'});

            expect(getWorkflowStatusType(job)).toBe('failed');
        });

        it('should return failed when job is COMPLETED but trigger is FAILED', () => {
            const job = createJob({status: 'COMPLETED'});
            const trigger = createTriggerExecution({status: 'FAILED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('failed');
        });

        it('should return failed when job is COMPLETED but trigger is CANCELLED', () => {
            const job = createJob({status: 'COMPLETED'});
            const trigger = createTriggerExecution({status: 'CANCELLED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('failed');
        });

        it('should return failed when job is STARTED but trigger is FAILED', () => {
            const job = createJob({status: 'STARTED'});
            const trigger = createTriggerExecution({status: 'FAILED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('failed');
        });

        it('should return failed when job is FAILED and trigger is COMPLETED', () => {
            const job = createJob({status: 'FAILED'});
            const trigger = createTriggerExecution({status: 'COMPLETED'});

            expect(getWorkflowStatusType(job, trigger)).toBe('failed');
        });
    });
});
