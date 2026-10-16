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
            integrationInstanceConfigurations: [{id: 4, integrationId: 2}],
            integrations: [{componentName: 'affinity', id: 2, name: 'Affinity Sync'}],
            startDate,
            status: 'FAILED',
            workflowId: 'workflow-uuid',
            workflows: [{id: 'workflow-uuid', label: 'workflow1'}],
        });

        expect(findValue(filters, 'Status')).toBe('FAILED');
        expect(findValue(filters, 'Start date')).toBe(startDate.toLocaleDateString());
        expect(findValue(filters, 'End date')).toBe(endDate.toLocaleDateString());
        expect(findValue(filters, 'Integration')).toBe('Affinity Sync');
        expect(findValue(filters, 'Instance Configuration')).toBe('Affinity Sync');
        expect(findValue(filters, 'Workflow')).toBe('workflow1');
        expect(findValue(filters, 'Connected User')).toBeUndefined();
    });

    it('uses the fetched instance configuration and falls back to ids', () => {
        const filters = getWorkflowExecutionsFilters({
            automations: 0,
            integrationId: 2,
            integrationInstanceConfiguration: {id: 4, integration: {componentName: 'hubspot', name: 'HubSpot Sync'}},
            integrationInstanceConfigurationId: 4,
            workflowId: 'workflow-uuid',
        });

        expect(findValue(filters, 'Integration')).toBe('2');
        expect(findValue(filters, 'Instance Configuration')).toBe('HubSpot Sync');
        expect(findValue(filters, 'Workflow')).toBe('workflow-uuid');
    });

    it('resolves the connected user of integration executions', () => {
        const filters = getWorkflowExecutionsFilters({
            automations: 0,
            connectedUserId: 7,
            connectedUsers: [{externalId: 'user-7', id: 7}],
        });

        expect(findValue(filters, 'Connected User')).toBe('User user-7');
    });

    it('falls back to the connected user id when the connected users are not loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 0, connectedUserId: 7});

        expect(findValue(filters, 'Connected User')).toBe('7');
    });

    it('falls back to the instance configuration id when nothing is loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 0, integrationInstanceConfigurationId: 4});

        expect(findValue(filters, 'Instance Configuration')).toBe('4');
    });

    it('resolves the connected user and the workflow for automations and ignores integration filters', () => {
        const filters = getWorkflowExecutionsFilters({
            automations: 1,
            connectedUserId: 5,
            connectedUserProjects: [
                {connectedUser: {externalId: '1234', id: '5'}},
                {connectedUser: {externalId: '5678', id: '9'}},
            ],
            integrationId: 2,
            workflowId: 'workflow-uuid',
            workflows: [{id: 'workflow-uuid', label: 'Sync contacts'}],
        });

        expect(findValue(filters, 'Type')).toBe('Automations');
        expect(findValue(filters, 'Connected User')).toBe('User 1234');
        expect(findValue(filters, 'Integration')).toBeUndefined();
        expect(findValue(filters, 'Workflow')).toBe('Sync contacts');
    });

    it('falls back to the workflow id for automations when the workflow is not loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 1, workflowId: 'workflow-uuid'});

        expect(findValue(filters, 'Workflow')).toBe('workflow-uuid');
    });

    it('falls back to the connected user id when the connected user is not loaded', () => {
        const filters = getWorkflowExecutionsFilters({automations: 1, connectedUserId: 5});

        expect(findValue(filters, 'Connected User')).toBe('5');
    });
});
