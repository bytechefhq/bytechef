import {describe, expect, it} from 'vitest';

import getTestRunTriggerName from './getTestRunTriggerName';

describe('getTestRunTriggerName', () => {
    it('returns the selected trigger when it is one of the workflow triggers', () => {
        expect(getTestRunTriggerName({triggers: [{name: 'trigger_1'}, {name: 'trigger_2'}]}, 'trigger_2')).toBe(
            'trigger_2'
        );
    });

    it('falls back to the first trigger when nothing is selected', () => {
        expect(getTestRunTriggerName({triggers: [{name: 'webhook_1'}, {name: 'trigger_2'}]})).toBe('webhook_1');
    });

    it('falls back to the first trigger when the selected node is not a workflow trigger', () => {
        expect(getTestRunTriggerName({triggers: [{name: 'trigger_1'}, {name: 'trigger_2'}]}, 'task_1')).toBe(
            'trigger_1'
        );
    });

    it('falls back to trigger_1 when the workflow has no triggers', () => {
        expect(getTestRunTriggerName({})).toBe('trigger_1');
    });
});
