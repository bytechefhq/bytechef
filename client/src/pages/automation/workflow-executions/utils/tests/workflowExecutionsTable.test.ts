import {WorkflowExecution} from '@/shared/middleware/automation/workflow/execution';
import {describe, expect, it} from 'vitest';

import {getProjectVersion} from '../workflowExecutionsTable';

describe('getProjectVersion', () => {
    it('uses the project version the execution ran, not the version the deployment is on now', () => {
        const execution = {
            projectDeployment: {projectVersion: 4},
            projectVersion: 2,
        } as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(2);
    });

    it('falls back to the deployment version when the execution carries none', () => {
        const execution = {projectDeployment: {projectVersion: 3}} as unknown as WorkflowExecution;

        expect(getProjectVersion(execution)).toBe(3);
    });

    it('returns undefined when no version is known', () => {
        expect(getProjectVersion({} as WorkflowExecution)).toBeUndefined();
    });
});
