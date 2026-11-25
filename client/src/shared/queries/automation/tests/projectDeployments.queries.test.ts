import {ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {describe, expect, it} from 'vitest';

import {getProjectDeploymentsRefetchInterval} from '../projectDeployments.queries';

const startDate = new Date('2026-11-25T10:50:00Z');
const endDate = new Date('2026-11-25T10:51:00Z');

describe('getProjectDeploymentsRefetchInterval', () => {
    it('does not poll without deployments', () => {
        expect(getProjectDeploymentsRefetchInterval(undefined)).toBe(false);
        expect(getProjectDeploymentsRefetchInterval([])).toBe(false);
    });

    it('does not poll when every execution has finished', () => {
        const projectDeployments = [
            {
                lastExecutionDate: endDate,
                lastExecutionStartDate: startDate,
                projectDeploymentWorkflows: [{lastExecutionDate: endDate, lastExecutionStartDate: startDate}],
            },
        ] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments)).toBe(false);
    });

    it('polls while a deployment execution has started but not ended', () => {
        const projectDeployments = [{lastExecutionStartDate: startDate}] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments)).toBe(3000);
    });

    it('polls while a workflow execution has started but not ended', () => {
        const projectDeployments = [
            {
                lastExecutionDate: endDate,
                lastExecutionStartDate: startDate,
                projectDeploymentWorkflows: [{lastExecutionDate: endDate}, {lastExecutionStartDate: startDate}],
            },
        ] as ProjectDeployment[];

        expect(getProjectDeploymentsRefetchInterval(projectDeployments)).toBe(3000);
    });
});
