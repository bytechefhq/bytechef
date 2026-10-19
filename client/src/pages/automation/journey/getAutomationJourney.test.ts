import {Project, ProjectDeployment} from '@/shared/middleware/automation/configuration';
import {describe, expect, it} from 'vitest';

import {getAutomationJourney} from './getAutomationJourney';

const draftProject: Project = {id: 1, name: 'Lead sync', projectWorkflowIds: [], workspaceId: 1};

const draftProjectWithWorkflow: Project = {id: 2, name: 'Invoice digest', projectWorkflowIds: [21, 22], workspaceId: 1};

const otherDraftProjectWithWorkflow: Project = {id: 3, name: 'Ticket triage', projectWorkflowIds: [31], workspaceId: 1};

const publishedProject: Project = {
    id: 4,
    lastPublishedDate: new Date('2026-10-01T10:00:00Z'),
    name: 'Order alerts',
    projectWorkflowIds: [41],
    workspaceId: 1,
};

const disabledDeployment: ProjectDeployment = {enabled: false, id: 7, name: 'Order alerts', projectId: 4};

const enabledDeployment: ProjectDeployment = {enabled: true, id: 8, name: 'Order alerts', projectId: 4};

describe('getAutomationJourney stage', () => {
    it.each([
        {deployments: [], projects: [], stage: 'createProject', title: 'no projects'},
        {deployments: [], projects: [draftProject], stage: 'buildWorkflow', title: 'projects without workflows'},
        {
            deployments: [],
            projects: [draftProject, draftProjectWithWorkflow],
            stage: 'publish',
            title: 'a workflow but nothing published',
        },
        {deployments: [], projects: [publishedProject], stage: 'deploy', title: 'published but not deployed'},
        {
            deployments: [disabledDeployment],
            projects: [publishedProject],
            stage: 'enable',
            title: 'deployed but disabled',
        },
        {
            deployments: [disabledDeployment, enabledDeployment],
            projects: [publishedProject],
            stage: 'live',
            title: 'one deployment enabled',
        },
    ] as const)('is $stage with $title', ({deployments, projects, stage}) => {
        expect(getAutomationJourney([...projects], [...deployments]).stage).toBe(stage);
    });

    it('counts a published project as past the workflow step even when its current draft has no workflows', () => {
        const publishedWithEmptyDraft: Project = {...publishedProject, projectWorkflowIds: []};

        expect(getAutomationJourney([publishedWithEmptyDraft], []).stage).toBe('deploy');
    });
});

describe('getAutomationJourney step statuses', () => {
    it('marks earlier steps complete, the stage step current and later steps upcoming', () => {
        expect(getAutomationJourney([draftProject, draftProjectWithWorkflow], []).stepStatuses).toEqual({
            buildWorkflow: 'complete',
            createProject: 'complete',
            deploy: 'upcoming',
            publish: 'current',
        });
    });

    it('keeps the deploy step current while the deployment is disabled', () => {
        expect(getAutomationJourney([publishedProject], [disabledDeployment]).stepStatuses).toEqual({
            buildWorkflow: 'complete',
            createProject: 'complete',
            deploy: 'current',
            publish: 'complete',
        });
    });

    it('marks every step complete once live', () => {
        expect(getAutomationJourney([publishedProject], [enabledDeployment]).stepStatuses).toEqual({
            buildWorkflow: 'complete',
            createProject: 'complete',
            deploy: 'complete',
            publish: 'complete',
        });
    });
});

describe('getAutomationJourney next project', () => {
    it('names the only project that can be published', () => {
        expect(getAutomationJourney([draftProject, draftProjectWithWorkflow], []).nextProject).toBe(
            draftProjectWithWorkflow
        );
    });

    it('names no project when more than one could be published', () => {
        expect(
            getAutomationJourney([draftProjectWithWorkflow, otherDraftProjectWithWorkflow], []).nextProject
        ).toBeUndefined();
    });

    it('names no project outside the publish stage', () => {
        expect(getAutomationJourney([publishedProject, draftProjectWithWorkflow], []).nextProject).toBeUndefined();
    });
});
