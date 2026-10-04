import {useIntegrationsLeftSidebar} from '@/ee/pages/embedded/integration/components/integrations-sidebar/hooks/useIntegrationsLeftSidebar';
import {Workflow} from '@/ee/shared/middleware/embedded/configuration';
import {renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const NOW = new Date('2026-10-04T12:00:00Z');

const renderUseIntegrationsLeftSidebar = () => renderHook(() => useIntegrationsLeftSidebar()).result.current;

describe('useIntegrationsLeftSidebar', () => {
    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(NOW);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('calculateTimeDifference', () => {
        const minutesAgo = (minutes: number) => new Date(NOW.getTime() - minutes * 60 * 1000).toISOString();

        it('reads "on <date>" for workflows edited more than a week ago', () => {
            const {calculateTimeDifference} = renderUseIntegrationsLeftSidebar();

            const lastModifiedDate = minutesAgo(10 * 24 * 60);

            expect(calculateTimeDifference(lastModifiedDate)).toBe(
                `on ${new Date(lastModifiedDate).toLocaleDateString()}`
            );
        });

        it('counts days, hours and minutes for recent edits', () => {
            const {calculateTimeDifference} = renderUseIntegrationsLeftSidebar();

            expect(calculateTimeDifference(minutesAgo(3 * 24 * 60))).toBe('3 days ago');
            expect(calculateTimeDifference(minutesAgo(24 * 60))).toBe('1 day ago');
            expect(calculateTimeDifference(minutesAgo(5 * 60))).toBe('5 hours ago');
            expect(calculateTimeDifference(minutesAgo(1))).toBe('1 minute ago');
            expect(calculateTimeDifference(NOW.toISOString())).toBe('just now');
        });

        it('reads "Unknown" without a date', () => {
            const {calculateTimeDifference} = renderUseIntegrationsLeftSidebar();

            expect(calculateTimeDifference()).toBe('Unknown');
        });
    });

    describe('getFilteredWorkflows', () => {
        const workflows: Workflow[] = [
            {
                createdDate: new Date('2026-01-01'),
                label: 'Beta',
                lastModifiedDate: new Date('2026-03-01'),
            },
            {
                createdDate: new Date('2026-02-01'),
                label: 'Alpha',
                lastModifiedDate: new Date('2026-02-15'),
            },
        ];

        it('returns an empty list without workflows', () => {
            const {getFilteredWorkflows} = renderUseIntegrationsLeftSidebar();

            expect(getFilteredWorkflows(undefined, 'last-edited', '')).toEqual([]);
        });

        it('sorts by each option', () => {
            const {getFilteredWorkflows} = renderUseIntegrationsLeftSidebar();

            const labels = (sortBy: string) =>
                getFilteredWorkflows(workflows, sortBy, '').map((workflow) => workflow.label);

            expect(labels('alphabetical')).toEqual(['Alpha', 'Beta']);
            expect(labels('reverse-alphabetical')).toEqual(['Beta', 'Alpha']);
            expect(labels('date-created')).toEqual(['Alpha', 'Beta']);
            expect(labels('last-edited')).toEqual(['Beta', 'Alpha']);
            expect(labels('unknown')).toEqual(['Beta', 'Alpha']);
        });

        it('filters by label, ignoring case', () => {
            const {getFilteredWorkflows} = renderUseIntegrationsLeftSidebar();

            expect(getFilteredWorkflows(workflows, 'alphabetical', 'ALP').map((workflow) => workflow.label)).toEqual([
                'Alpha',
            ]);
        });
    });

    describe('getWorkflowsIntegrationId', () => {
        it('maps each integration workflow to its integration', () => {
            const {getWorkflowsIntegrationId} = renderUseIntegrationsLeftSidebar();

            const findIntegrationIdByWorkflow = getWorkflowsIntegrationId([
                {componentName: 'gmail', id: 1, integrationWorkflowIds: [11, 12], multipleInstances: false},
                {componentName: 'slack', id: 2, integrationWorkflowIds: [21], multipleInstances: false},
            ]);

            expect(findIntegrationIdByWorkflow({integrationWorkflowId: 12})).toBe(1);
            expect(findIntegrationIdByWorkflow({integrationWorkflowId: 21})).toBe(2);
            expect(findIntegrationIdByWorkflow({integrationWorkflowId: 99})).toBe(0);
        });
    });
});
