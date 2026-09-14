import {getWorkflowExecutionsFilters} from '@/pages/automation/workflow-executions/utils/workflowExecutionsFilters';
import {describe, expect, it} from 'vitest';

const findValue = (filters: Array<{label: string; value?: string}>, label: string) =>
    filters.find((filter) => filter.label === label)?.value;

describe('getWorkflowExecutionsFilters', () => {
    it('returns no values when nothing is filtered', () => {
        const filters = getWorkflowExecutionsFilters({});

        expect(filters.map((filter) => filter.label)).toEqual([
            'Status',
            'Start date',
            'End date',
            'Project',
            'Deployment',
            'Workflow',
        ]);
        expect(filters.every((filter) => filter.value === undefined)).toBe(true);
    });

    it('resolves names for the selected project, deployment and workflow', () => {
        const startDate = new Date(2026, 8, 1);
        const endDate = new Date(2026, 8, 9);

        const filters = getWorkflowExecutionsFilters({
            endDate,
            projectDeploymentId: 5,
            projectDeployments: [{id: 5, name: 'AI Agent', projectVersion: 1}],
            projectId: 3,
            projects: [{id: 3, name: 'AI Agent'}],
            startDate,
            status: 'COMPLETED',
            workflowId: 'workflow-uuid',
            workflows: [{id: 'workflow-uuid', label: 'workflow1'}],
        });

        expect(findValue(filters, 'Status')).toBe('COMPLETED');
        expect(findValue(filters, 'Start date')).toBe(startDate.toLocaleDateString());
        expect(findValue(filters, 'End date')).toBe(endDate.toLocaleDateString());
        expect(findValue(filters, 'Project')).toBe('AI Agent');
        expect(findValue(filters, 'Deployment')).toBe('AI Agent V1');
        expect(findValue(filters, 'Workflow')).toBe('workflow1');
    });

    it('falls back to ids while the option lists are not loaded', () => {
        const filters = getWorkflowExecutionsFilters({
            projectDeploymentId: 5,
            projectId: 3,
            workflowId: 'workflow-uuid',
        });

        expect(findValue(filters, 'Project')).toBe('3');
        expect(findValue(filters, 'Deployment')).toBe('5');
        expect(findValue(filters, 'Workflow')).toBe('workflow-uuid');
    });
});
