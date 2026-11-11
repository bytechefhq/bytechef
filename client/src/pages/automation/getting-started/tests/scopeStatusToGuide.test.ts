import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import scopeStatusToGuide from '@/pages/automation/getting-started/scopeStatusToGuide';
import {describe, expect, it} from 'vitest';

const failedRun = {
    date: '2026-11-11T10:00:00Z',
    errorMessage: 'Connection refused',
    jobId: '1',
    projectDeploymentEnabled: true,
    projectDeploymentId: '2',
    status: 'FAILED',
};

const tenantStatus: AutomationGettingStartedStatusType = {
    completed: false,
    deployed: true,
    firstRun: null,
    httpClientAdded: true,
    lastFailedRun: failedRun,
    live: true,
    loggerAdded: true,
    projectCreated: true,
    published: true,
    workflowCreated: true,
};

describe('scopeStatusToGuide', () => {
    it('keeps the tenant-wide progress once the guide workflow exists', () => {
        expect(scopeStatusToGuide({hasGuideProject: true, hasGuideWorkflow: true, status: tenantStatus})).toBe(
            tenantStatus
        );
    });

    it('clears every signal while the guide has no project', () => {
        expect(scopeStatusToGuide({hasGuideProject: false, hasGuideWorkflow: false, status: tenantStatus})).toEqual({
            ...tenantStatus,
            deployed: false,
            httpClientAdded: false,
            lastFailedRun: null,
            live: false,
            loggerAdded: false,
            projectCreated: false,
            published: false,
            workflowCreated: false,
        });
    });

    it('keeps only the project signal while the guide project has no guide workflow', () => {
        const scopedStatus = scopeStatusToGuide({hasGuideProject: true, hasGuideWorkflow: false, status: tenantStatus});

        expect(scopedStatus?.projectCreated).toBe(true);
        expect(scopedStatus?.workflowCreated).toBe(false);
        expect(scopedStatus?.published).toBe(false);
    });

    it('keeps a finished guide as it is', () => {
        const completedStatus = {...tenantStatus, completed: true};

        expect(scopeStatusToGuide({hasGuideProject: false, hasGuideWorkflow: false, status: completedStatus})).toBe(
            completedStatus
        );
    });

    it('returns undefined while the status is loading', () => {
        expect(scopeStatusToGuide({hasGuideProject: false, hasGuideWorkflow: false})).toBeUndefined();
    });
});
