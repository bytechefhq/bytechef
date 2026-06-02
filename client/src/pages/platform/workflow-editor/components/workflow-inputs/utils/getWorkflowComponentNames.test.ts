import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import getWorkflowComponentNames from './getWorkflowComponentNames';

describe('getWorkflowComponentNames', () => {
    it('returns the union of task and trigger component names, de-duplicated and sorted', () => {
        const workflow = {
            workflowTaskComponentNames: ['slack', 'googleSheets', 'slack'],
            workflowTriggerComponentNames: ['googleSheets', 'webhook'],
        } as Workflow;

        expect(getWorkflowComponentNames(workflow)).toEqual(['googleSheets', 'slack', 'webhook']);
    });

    it('returns an empty array when no components are present', () => {
        expect(getWorkflowComponentNames({} as Workflow)).toEqual([]);
    });
});
