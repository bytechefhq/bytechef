import {ApprovalTaskStatus} from '@/shared/middleware/graphql';
import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import {
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

        it('renders a muted icon for the expired status', () => {
            const {container} = render(<>{getStatusIcon('expired')}</>);

            expect(container.querySelector('svg')).toHaveClass('text-gray-400');
        });
    });
});
