import {Job, TriggerExecution} from '@/shared/middleware/platform/workflow/execution';
import {render} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {ExecutionStatusType, getExecutionStatusIcon, getWorkflowStatusType} from '../util/workflowExecution-utils';

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

describe('workflowExecution-utils', () => {
    describe('getExecutionStatusIcon', () => {
        it('should return check icon with success class for COMPLETED status', () => {
            const {container} = render(getExecutionStatusIcon('COMPLETED'));

            const icon = container.querySelector('.text-success');

            expect(icon).toBeInTheDocument();
        });

        it('should return spinning loader icon with primary class for STARTED status', () => {
            const {container} = render(getExecutionStatusIcon('STARTED'));

            const icon = container.querySelector('.animate-spin.text-primary');

            expect(icon).toBeInTheDocument();
        });

        it('should return spinning loader icon with primary class for CREATED status', () => {
            const {container} = render(getExecutionStatusIcon('CREATED'));

            const icon = container.querySelector('.animate-spin.text-primary');

            expect(icon).toBeInTheDocument();
        });

        it('should return alert icon with destructive class for FAILED status', () => {
            const {container} = render(getExecutionStatusIcon('FAILED'));

            const icon = container.querySelector('.text-destructive');

            expect(icon).toBeInTheDocument();
        });

        it('should return alert icon with destructive class for CANCELLED status', () => {
            const {container} = render(getExecutionStatusIcon('CANCELLED'));

            const icon = container.querySelector('.text-destructive');

            expect(icon).toBeInTheDocument();
        });

        it('should return alert icon with destructive class for unknown status', () => {
            const {container} = render(getExecutionStatusIcon('UNKNOWN' as ExecutionStatusType));

            const icon = container.querySelector('.text-destructive');

            expect(icon).toBeInTheDocument();
        });
    });

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

        describe('polling behavior (isWorkflowRunning)', () => {
            function isWorkflowRunning(job: Job | undefined, triggerExecution?: TriggerExecution): boolean {
                if (!job) {
                    return false;
                }

                return getWorkflowStatusType(job, triggerExecution) === 'running';
            }

            it('should enable polling when job is STARTED', () => {
                const job = createJob({status: 'STARTED'});

                expect(isWorkflowRunning(job)).toBe(true);
            });

            it('should enable polling when job is CREATED', () => {
                const job = createJob({status: 'CREATED'});

                expect(isWorkflowRunning(job)).toBe(true);
            });

            it('should enable polling when job is STARTED and trigger is STARTED', () => {
                const job = createJob({status: 'STARTED'});
                const trigger = createTriggerExecution({status: 'STARTED'});

                expect(isWorkflowRunning(job, trigger)).toBe(true);
            });

            it('should disable polling when job is undefined', () => {
                expect(isWorkflowRunning(undefined)).toBe(false);
            });

            it('should disable polling when job is COMPLETED', () => {
                const job = createJob({status: 'COMPLETED'});

                expect(isWorkflowRunning(job)).toBe(false);
            });

            it('should disable polling when job is FAILED', () => {
                const job = createJob({status: 'FAILED'});

                expect(isWorkflowRunning(job)).toBe(false);
            });

            it('should disable polling when job is STOPPED', () => {
                const job = createJob({status: 'STOPPED'});

                expect(isWorkflowRunning(job)).toBe(false);
            });

            it('should disable polling when job is COMPLETED and trigger is COMPLETED', () => {
                const job = createJob({status: 'COMPLETED'});
                const trigger = createTriggerExecution({status: 'COMPLETED'});

                expect(isWorkflowRunning(job, trigger)).toBe(false);
            });

            it('should disable polling when trigger is FAILED even if job is STARTED', () => {
                const job = createJob({status: 'STARTED'});
                const trigger = createTriggerExecution({status: 'FAILED'});

                expect(isWorkflowRunning(job, trigger)).toBe(false);
            });

            it('should disable polling when trigger is CANCELLED even if job is STARTED', () => {
                const job = createJob({status: 'STARTED'});
                const trigger = createTriggerExecution({status: 'CANCELLED'});

                expect(isWorkflowRunning(job, trigger)).toBe(false);
            });
        });
    });
});
