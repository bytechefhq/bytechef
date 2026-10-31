import {WorkflowChangeType} from '@/shared/middleware/graphql';
import {describe, expect, it} from 'vitest';

import {
    formatVersionLabel,
    formatVersionWorkflowChangesSummary,
    getWorkflowChangeTypeBadgeStyleType,
    getWorkflowChangeTypeLabel,
} from '../versionWorkflowChangesUtils';

const summary = (changeType: WorkflowChangeType) => ({changeCount: 1, changeType, workflowUuid: 'uuid'});

describe('versionWorkflowChangesUtils', () => {
    it('formats the empty summary', () => {
        expect(formatVersionWorkflowChangesSummary([])).toBe('No workflow changes.');
    });

    it('formats singular and plural summaries', () => {
        expect(formatVersionWorkflowChangesSummary([summary(WorkflowChangeType.Added)])).toBe(
            '1 workflow changed (1 added)'
        );
        expect(
            formatVersionWorkflowChangesSummary([
                summary(WorkflowChangeType.Added),
                summary(WorkflowChangeType.Modified),
                summary(WorkflowChangeType.Modified),
            ])
        ).toBe('3 workflows changed (1 added, 2 modified)');
    });

    it('omits zero counts and orders added, modified, removed', () => {
        expect(
            formatVersionWorkflowChangesSummary([
                summary(WorkflowChangeType.Removed),
                summary(WorkflowChangeType.Added),
            ])
        ).toBe('2 workflows changed (1 added, 1 removed)');
    });

    it('formats version labels with draft', () => {
        expect(formatVersionLabel(3, false)).toBe('V3');
        expect(formatVersionLabel(null, true)).toBe('Draft');
    });

    it('maps change types to badge styles and labels', () => {
        expect(getWorkflowChangeTypeBadgeStyleType(WorkflowChangeType.Added)).toBe('success-outline');
        expect(getWorkflowChangeTypeBadgeStyleType(WorkflowChangeType.Modified)).toBe('warning-outline');
        expect(getWorkflowChangeTypeBadgeStyleType(WorkflowChangeType.Removed)).toBe('destructive-outline');
        expect(getWorkflowChangeTypeBadgeStyleType(WorkflowChangeType.Unchanged)).toBe('secondary-outline');
        expect(getWorkflowChangeTypeLabel(WorkflowChangeType.Unchanged)).toBe('Unchanged');
    });
});
