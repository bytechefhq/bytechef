import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {describe, expect, it} from 'vitest';

import {getProjectVersion} from '../workflowExecutionsTable';

describe('getProjectVersion', () => {
    it('uses the project version the job ran with', () => {
        const execution = {
            job: {metadata: {projectVersion: 2}},
            projectDeployment: {projectVersion: 3},
        } as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(2);
    });

    it('reads a project version stored as a string in the job metadata as a number', () => {
        const execution = {job: {metadata: {projectVersion: '4'}}} as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(4);
    });

    it('falls back to the deployment version when the job has no project version metadata', () => {
        const execution = {
            job: {metadata: {}},
            projectDeployment: {projectVersion: 3},
        } as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(3);
    });

    it('falls back to the deployment version for a trigger-only execution without a job', () => {
        const execution = {projectDeployment: {projectVersion: 3}} as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(3);
    });

    it('returns undefined when no version is known', () => {
        expect(getProjectVersion({} as WorkflowExecution)).toBeUndefined();
    });
});
