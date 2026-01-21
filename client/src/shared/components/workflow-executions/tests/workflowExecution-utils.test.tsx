import {render} from '@/shared/util/test-utils';
import {describe, expect, it} from 'vitest';

import {ExecutionStatusType, getExecutionStatusIcon, getWorkflowStatusType} from '../util/workflowExecution-utils';

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
        it('should return completed when job and trigger are both completed', () => {
            const job = {status: 'COMPLETED'} as Parameters<typeof getWorkflowStatusType>[0];
            const trigger = {status: 'COMPLETED'} as Parameters<typeof getWorkflowStatusType>[1];

            expect(getWorkflowStatusType(job, trigger)).toBe('completed');
        });

        it('should return completed when job is completed and no trigger', () => {
            const job = {status: 'COMPLETED'} as Parameters<typeof getWorkflowStatusType>[0];

            expect(getWorkflowStatusType(job)).toBe('completed');
        });

        it('should return running when job is started', () => {
            const job = {status: 'STARTED'} as Parameters<typeof getWorkflowStatusType>[0];

            expect(getWorkflowStatusType(job)).toBe('running');
        });

        it('should return running when job is created', () => {
            const job = {status: 'CREATED'} as Parameters<typeof getWorkflowStatusType>[0];

            expect(getWorkflowStatusType(job)).toBe('running');
        });

        it('should return failed when job is failed', () => {
            const job = {status: 'FAILED'} as Parameters<typeof getWorkflowStatusType>[0];

            expect(getWorkflowStatusType(job)).toBe('failed');
        });

        it('should return failed when job is completed but trigger is failed', () => {
            const job = {status: 'COMPLETED'} as Parameters<typeof getWorkflowStatusType>[0];
            const trigger = {status: 'FAILED'} as Parameters<typeof getWorkflowStatusType>[1];

            expect(getWorkflowStatusType(job, trigger)).toBe('failed');
        });
    });
});
