import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import {getAutomationGettingStartedSteps} from '@/pages/automation/getting-started/getAutomationGettingStartedSteps';
import {AutomationJourneyI} from '@/pages/automation/journey/getAutomationJourney';
import {describe, expect, it} from 'vitest';

const run = {
    date: '2026-11-11T10:00:00Z',
    errorMessage: null,
    jobId: '1',
    projectDeploymentEnabled: true,
    projectDeploymentId: '2',
    status: 'COMPLETED',
};

const baseStatus: AutomationGettingStartedStatusType = {
    completed: false,
    deployed: false,
    firstRun: null,
    httpClientAdded: false,
    lastFailedRun: null,
    live: false,
    loggerAdded: false,
    projectCreated: false,
    published: false,
    workflowCreated: false,
};

const publishJourney: AutomationJourneyI = {
    stage: 'publish',
    stepStatuses: {buildWorkflow: 'complete', createProject: 'complete', deploy: 'upcoming', publish: 'current'},
};

const liveJourney: AutomationJourneyI = {
    stage: 'live',
    stepStatuses: {buildWorkflow: 'complete', createProject: 'complete', deploy: 'complete', publish: 'complete'},
};

describe('getAutomationGettingStartedSteps', () => {
    it('marks everything complete once a run exists', () => {
        const result = getAutomationGettingStartedSteps(
            {
                stage: 'createProject',
                stepStatuses: {
                    buildWorkflow: 'upcoming',
                    createProject: 'current',
                    deploy: 'upcoming',
                    publish: 'upcoming',
                },
            },
            {...baseStatus, firstRun: run}
        );

        expect(result.firstRunState).toBe('done');
        expect(Object.values(result.stepStatuses)).toEqual(['complete', 'complete', 'complete', 'complete']);
    });

    it('keeps everything complete when the run was deleted after completion', () => {
        const result = getAutomationGettingStartedSteps(publishJourney, {...baseStatus, completed: true});

        expect(result.firstRunState).toBe('done');
        expect(Object.values(result.stepStatuses)).toEqual(['complete', 'complete', 'complete', 'complete']);
    });

    it('keeps the workflow step current until it is tested, which publishing confirms', () => {
        const result = getAutomationGettingStartedSteps(publishJourney, {
            ...baseStatus,
            httpClientAdded: true,
            loggerAdded: true,
            workflowCreated: true,
        });

        expect(result.stepStatuses).toEqual({
            buildWorkflow: 'current',
            createProject: 'complete',
            deploy: 'upcoming',
            publish: 'upcoming',
        });
    });

    it('completes the workflow step once a version is published', () => {
        const result = getAutomationGettingStartedSteps(
            {
                stage: 'deploy',
                stepStatuses: {
                    buildWorkflow: 'complete',
                    createProject: 'complete',
                    deploy: 'current',
                    publish: 'complete',
                },
            },
            {...baseStatus, httpClientAdded: true, loggerAdded: true, published: true}
        );

        expect(result.stepStatuses.buildWorkflow).toBe('complete');
        expect(result.stepStatuses.publish).toBe('complete');
    });

    it('waits for the first run once a deployment can run', () => {
        const result = getAutomationGettingStartedSteps(liveJourney, baseStatus);

        expect(result.firstRunState).toBe('waiting');
        expect(result.stepStatuses.deploy).toBe('current');
        expect(result.stepStatuses.publish).toBe('complete');
    });

    it('reports failed when only failed runs exist', () => {
        const result = getAutomationGettingStartedSteps(liveJourney, {
            ...baseStatus,
            lastFailedRun: {...run, status: 'FAILED'},
        });

        expect(result.firstRunState).toBe('failed');
    });
});
