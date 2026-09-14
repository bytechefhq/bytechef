import {getWorkflowExecutionsFilters} from '@/ee/pages/embedded/workflow-executions/utils/workflowExecutionsFilters';
import {describe, expect, it} from 'vitest';

const findValue = (filters: Array<{label: string; value?: string}>, label: string) =>
    filters.find((filter) => filter.label === label)?.value;

describe('getWorkflowExecutionsFilters', () => {
    it('only sets the type when nothing else is filtered', () => {
        const filters = getWorkflowExecutionsFilters({automations: 0});

        expect(findValue(filters, 'Type')).toBe('Integrations');
        expect(filters.filter((filter) => filter.value !== undefined)).toHaveLength(1);
    });

    it('resolves integration filters by name', () => {
        const startDate = new Date(2026, 8, 1);
        const endDate = new Date(2026, 8, 9);

        const filters = getWorkflowExecutionsFilters({
            automations: 0,
            endDate,
            integrationId: 2,
            integrationInstanceConfigurationId: 4,
            integrationInstanceConfigurations: [{id: 4, integration: {componentName: 'affinity'}}],
            integrations: [{componentName: 'affinity', id: 2}],
            startDate,
            status: 'FAILED',
            workflowId: 'workflow-uuid',
            workflows: [{id: 'workflow-uuid', label: 'workflow1'}],
        });

        expect(findValue(filters, 'Status')).toBe('FAILED');
        expect(findValue(filters, 'Start date')).toBe(startDate.toLocaleDateString());
        expect(findValue(filters, 'End date')).toBe(endDate.toLocaleDateString());
        expect(findValue(filters, 'Integration')).toBe('affinity');
        expect(findValue(filters, 'Instance Configuration')).toBe('affinity');
        expect(findValue(filters, 'Workflow')).toBe('workflow1');
        expect(findValue(filters, 'Connected User')).toBeUndefined();
    });

    it('uses the fetched instance configuration and falls back to ids', () => {
        const filters = getWorkflowExecutionsFilters({
            automations: 0,
            integrationId: 2,
            integrationInstanceConfiguration: {id: 4, integration: {componentName: 'hubspot'}},
            integrationInstanceConfigurationId: 4,
            workflowId: 'workflow-uuid',
        });

        expect(findValue(filters, 'Integration')).toBe('2');
        expect(findValue(filters, 'Instance Configuration')).toBe('hubspot');
        expect(findValue(filters, 'Workflow')).toBe('workflow-uuid');
    });

    it('falls back to the instance configuration id when nothing is loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 0, integrationInstanceConfigurationId: 4});

        expect(findValue(filters, 'Instance Configuration')).toBe('4');
    });

    it('resolves the connected user for automations and ignores integration filters', () => {
        const filters = getWorkflowExecutionsFilters({
            automations: 1,
            connectedUserProjects: [{connectedUser: {externalId: '1234'}, projectId: '9'}],
            integrationId: 2,
            projectId: 9,
            workflowId: 'workflow-uuid',
        });

        expect(findValue(filters, 'Type')).toBe('Automations');
        expect(findValue(filters, 'Connected User')).toBe('User 1234');
        expect(findValue(filters, 'Integration')).toBeUndefined();
        expect(findValue(filters, 'Workflow')).toBeUndefined();
    });

    it('falls back to the project id when the connected user is not loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 1, projectId: 9});

        expect(findValue(filters, 'Connected User')).toBe('9');
    });
});
