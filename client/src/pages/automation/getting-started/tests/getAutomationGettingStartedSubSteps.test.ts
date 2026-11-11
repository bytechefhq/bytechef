import {AutomationGettingStartedStatusType} from '@/pages/automation/getting-started/automationGettingStartedStatus';
import {
    getDeploySubSteps,
    getProjectSubSteps,
    getPublishSubSteps,
    getWorkflowSubSteps,
} from '@/pages/automation/getting-started/getAutomationGettingStartedSubSteps';
import {AutomationJourneyI, AutomationJourneyStageType} from '@/pages/automation/journey/getAutomationJourney';
import {describe, expect, it} from 'vitest';

const GUIDE_HREF = '/automation/projects/3/project-workflows/9';

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

const journeyAt = (stage: AutomationJourneyStageType): AutomationJourneyI => ({
    stage,
    stepStatuses: {buildWorkflow: 'current', createProject: 'complete', deploy: 'upcoming', publish: 'upcoming'},
});

const doneFlags = (subSteps: {done: boolean}[]) => subSteps.map((subStep) => subStep.done);

describe('getAutomationGettingStartedSubSteps', () => {
    it('ticks recipe sub-steps from the status flags', () => {
        const subSteps = getWorkflowSubSteps(
            journeyAt('buildWorkflow'),
            {...baseStatus, httpClientAdded: true},
            '/p',
            'dialog'
        );

        expect(doneFlags(subSteps)).toEqual([false, true, false, false]);
    });

    it('ticks test once a version is published', () => {
        expect(doneFlags(getWorkflowSubSteps(journeyAt('publish'), baseStatus, '/p', 'dialog'))).toEqual([
            true,
            false,
            false,
            false,
        ]);
        expect(doneFlags(getWorkflowSubSteps(journeyAt('deploy'), baseStatus, '/p', 'dialog'))[2]).toBe(true);
        expect(doneFlags(getPublishSubSteps(journeyAt('deploy'), '/p'))).toEqual([true]);
        expect(doneFlags(getProjectSubSteps(journeyAt('createProject')))).toEqual([false]);
    });

    it('names Development in the deploy sub-step', () => {
        const subSteps = getDeploySubSteps(journeyAt('enable'), baseStatus, GUIDE_HREF);

        expect(subSteps[0].label).toBe('Deploy it in Development');
        expect(doneFlags(subSteps)).toEqual([true, false, false]);
        expect(doneFlags(getDeploySubSteps(journeyAt('live'), baseStatus, GUIDE_HREF))).toEqual([true, true, false]);
    });

    it('links every deploy sub-step to the guide workflow', () => {
        const subSteps = getDeploySubSteps(journeyAt('deploy'), baseStatus, GUIDE_HREF);

        expect(subSteps.map((subStep) => subStep.href)).toEqual([GUIDE_HREF, GUIDE_HREF, GUIDE_HREF]);
        expect(subSteps.map((subStep) => subStep.hint)).toEqual([
            'In the editor, Getting started card → Deploy',
            'In the editor, Getting started card → Enable',
            'In the editor, Getting started card → Run now',
        ]);
    });

    it('disables the create workflow sub-step until a project exists', () => {
        const withoutProject = getWorkflowSubSteps(journeyAt('buildWorkflow'), baseStatus, GUIDE_HREF, 'none');
        const withProject = getWorkflowSubSteps(journeyAt('buildWorkflow'), baseStatus, GUIDE_HREF, 'dialog');

        expect(withoutProject[0].disabled).toBe(true);
        expect(withoutProject[0].hint).toBe('Create a project first');
        expect(withProject[0].disabled).toBe(false);
        expect(withProject[0].hint).toBe('Opens the Create Workflow dialog');
    });

    it('links the editor sub-steps to the guide workflow', () => {
        const subSteps = getWorkflowSubSteps(journeyAt('buildWorkflow'), baseStatus, GUIDE_HREF, 'dialog');

        expect(subSteps.slice(1).map((subStep) => subStep.href)).toEqual([GUIDE_HREF, GUIDE_HREF, GUIDE_HREF]);
        expect(getPublishSubSteps(journeyAt('deploy'), GUIDE_HREF)[0].href).toBe(GUIDE_HREF);
    });

    it('ticks every sub-step once a run exists', () => {
        const status: AutomationGettingStartedStatusType = {
            ...baseStatus,
            firstRun: {
                date: '2026-11-11T10:00:00Z',
                errorMessage: null,
                jobId: '1',
                projectDeploymentEnabled: true,
                projectDeploymentId: '2',
                status: 'COMPLETED',
            },
        };
        const journey = journeyAt('createProject');

        expect(
            [
                ...getProjectSubSteps(journey, status),
                ...getWorkflowSubSteps(journey, status, '/p', 'dialog'),
                ...getPublishSubSteps(journey, '/p', status),
                ...getDeploySubSteps(journey, status, GUIDE_HREF),
            ].every((subStep) => subStep.done)
        ).toBe(true);
    });

    it('uses the spec copy verbatim', () => {
        const journey = journeyAt('createProject');

        const subSteps = [
            ...getProjectSubSteps(journey),
            ...getWorkflowSubSteps(journey, baseStatus, '/p', 'dialog'),
            ...getPublishSubSteps(journey, '/p'),
            ...getDeploySubSteps(journey, baseStatus, GUIDE_HREF),
        ].map((subStep) => [subStep.label, subStep.hint]);

        expect(subSteps).toMatchInlineSnapshot(`
          [
            [
              "Create a project",
              "Opens the Create Project dialog",
            ],
            [
              "Create a workflow",
              "Opens the Create Workflow dialog",
            ],
            [
              "Fetch a quote",
              "Click + below the trigger → HTTP Client → GET, URI https://dummyjson.com/quotes/random",
            ],
            [
              "Test it",
              "Open the HTTP Client step's Output tab → Test Action",
            ],
            [
              "Log it",
              "Click + below HTTP Client → Helpers tab → Logger → Info, click into Text, pick quote under HTTP Client in the Data Pill Panel, then click Test",
            ],
            [
              "Publish the project",
              "In the editor, Getting started card → Publish",
            ],
            [
              "Deploy it in Development",
              "In the editor, Getting started card → Deploy",
            ],
            [
              "Enable the workflow and the deployment",
              "In the editor, Getting started card → Enable",
            ],
            [
              "See it run",
              "In the editor, Getting started card → Run now",
            ],
          ]
        `);
    });
});
