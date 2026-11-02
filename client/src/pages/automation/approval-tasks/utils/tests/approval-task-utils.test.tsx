import {ApprovalTaskStatus} from '@/shared/middleware/graphql';
import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {
    getPriorityColor,
    getStatusIcon,
    getStatusLabel,
    isApprovalTaskOverdue,
    toClientStatus,
    toServerStatus,
} from '../approval-task-utils';

import type {ApprovalTaskI} from '../../types/types';

const approvalTask = (overrides: Partial<ApprovalTaskI>): ApprovalTaskI => ({
    assignee: '',
    attachments: [],
    comments: [],
    createdAt: '2026-10-01T10:00:00Z',
    dependencies: [],
    description: '',
    priority: 'medium',
    status: 'open',
    title: 'Approve invoice',
    ...overrides,
});

describe('getPriorityColor', () => {
    it.each([
        ['high', 'bg-surface-destructive-secondary text-content-destructive border-stroke-destructive-secondary'],
        ['medium', 'bg-surface-warning-secondary text-content-warning-primary border-stroke-warning-secondary'],
        ['low', 'bg-surface-success-secondary text-content-success-primary border-stroke-success-secondary'],
        ['unknown', 'bg-surface-neutral-secondary text-content-neutral-primary border-stroke-neutral-secondary'],
    ])('colours %s priority', (priority, expectedClasses) => {
        expect(getPriorityColor(priority)).toBe(expectedClasses);
    });
});

describe('getStatusIcon', () => {
    it.each([
        ['completed', 'text-content-success-primary'],
        ['expired', 'text-content-neutral-tertiary'],
        ['in-progress', 'text-content-brand-primary'],
        ['open', 'text-content-neutral-tertiary'],
    ])('colours the %s icon', (status, expectedClass) => {
        const {container} = render(<>{getStatusIcon(status)}</>);

        expect(container.querySelector('svg')).toHaveClass('size-4', expectedClass);
    });
});

describe('approval-task-utils', () => {
    describe('isApprovalTaskOverdue', () => {
        it('flags an open task past its due date', () => {
            expect(isApprovalTaskOverdue(approvalTask({dueDate: '2000-01-01T00:00:00Z'}))).toBe(true);
        });

        it('does not flag a task due in the future or without a due date', () => {
            expect(isApprovalTaskOverdue(approvalTask({dueDate: '2999-01-01T00:00:00Z'}))).toBe(false);
            expect(isApprovalTaskOverdue(approvalTask({}))).toBe(false);
        });

        it('never flags a completed or expired task', () => {
            expect(isApprovalTaskOverdue(approvalTask({dueDate: '2000-01-01T00:00:00Z', status: 'completed'}))).toBe(
                false
            );
            expect(isApprovalTaskOverdue(approvalTask({dueDate: '2000-01-01T00:00:00Z', status: 'expired'}))).toBe(
                false
            );
        });
    });

    describe('expired status', () => {
        it('maps the expired status in both directions', () => {
            expect(toClientStatus(ApprovalTaskStatus.Expired)).toBe('expired');
            expect(toServerStatus('expired')).toBe(ApprovalTaskStatus.Expired);
        });

        it('labels the expired status', () => {
            expect(getStatusLabel('expired')).toBe('Expired');
        });
    });
});
