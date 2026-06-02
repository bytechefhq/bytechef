import {Workflow} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {getBackingWorkflowNodeName} from './getBackingWorkflowNodeName';

const workflow = {
    definition: JSON.stringify({
        tasks: [{name: 'sendMessage_1', type: 'slack/v2/sendMessage'}],
        triggers: [{name: 'newMessage_1', type: 'slack/v2/newMessage'}],
    }),
} as Workflow;

describe('getBackingWorkflowNodeName', () => {
    it('returns the first task node using the component', () => {
        expect(getBackingWorkflowNodeName(workflow, 'slack')).toBe('sendMessage_1');
    });

    it('falls back to a trigger node when no task uses the component', () => {
        const triggerOnly = {
            definition: JSON.stringify({tasks: [], triggers: [{name: 'newMessage_1', type: 'slack/v2/newMessage'}]}),
        } as Workflow;

        expect(getBackingWorkflowNodeName(triggerOnly, 'slack')).toBe('newMessage_1');
    });

    it('returns undefined when no node uses the component', () => {
        expect(getBackingWorkflowNodeName(workflow, 'googleSheets')).toBeUndefined();
    });

    it('returns undefined for a missing component name or definition', () => {
        expect(getBackingWorkflowNodeName(workflow, undefined)).toBeUndefined();
        expect(getBackingWorkflowNodeName({} as Workflow, 'slack')).toBeUndefined();
    });
});
