import {describe, expect, it} from 'vitest';

import getTestRunTriggerName from './getTestRunTriggerName';

describe('getTestRunTriggerName', () => {
    it('returns the selected trigger when it is one of the workflow triggers', () => {
        expect(
            getTestRunTriggerName(
                {
                    triggers: [
                        {name: 'trigger_1', type: 'webhook/v1/webhook'},
                        {name: 'trigger_2', type: 'webhook/v1/webhook'},
                    ],
                },
                'trigger_2'
            )
        ).toBe('trigger_2');
    });

    it('falls back to the first trigger when nothing is selected', () => {
        expect(
            getTestRunTriggerName({
                triggers: [
                    {name: 'webhook_1', type: 'webhook/v1/webhook'},
                    {name: 'trigger_2', type: 'webhook/v1/webhook'},
                ],
            })
        ).toBe('webhook_1');
    });

    it('falls back to the first trigger when the selected node is not a workflow trigger', () => {
        expect(
            getTestRunTriggerName(
                {
                    triggers: [
                        {name: 'trigger_1', type: 'webhook/v1/webhook'},
                        {name: 'trigger_2', type: 'webhook/v1/webhook'},
                    ],
                },
                'task_1'
            )
        ).toBe('trigger_1');
    });

    it('falls back to trigger_1 when the workflow has no triggers', () => {
        expect(getTestRunTriggerName({})).toBe('trigger_1');
    });

    it('uses the selected trigger when it is enabled', () => {
        expect(
            getTestRunTriggerName(
                {
                    triggers: [
                        {disabled: false, name: 'trigger_1', type: 'webhook/v1/webhook'},
                        {disabled: false, name: 'trigger_2', type: 'webhook/v1/webhook'},
                    ],
                },
                'trigger_2'
            )
        ).toBe('trigger_2');
    });

    it('skips a selected disabled trigger and uses the first enabled one', () => {
        expect(
            getTestRunTriggerName(
                {
                    triggers: [
                        {disabled: false, name: 'trigger_1', type: 'webhook/v1/webhook'},
                        {disabled: true, name: 'trigger_2', type: 'webhook/v1/webhook'},
                    ],
                },
                'trigger_2'
            )
        ).toBe('trigger_1');
    });

    it('skips a disabled first trigger when nothing is selected', () => {
        expect(
            getTestRunTriggerName({
                triggers: [
                    {disabled: true, name: 'trigger_1', type: 'webhook/v1/webhook'},
                    {disabled: false, name: 'trigger_2', type: 'webhook/v1/webhook'},
                ],
            })
        ).toBe('trigger_2');
    });

    it('treats a disabled manual trigger as enabled', () => {
        expect(
            getTestRunTriggerName({
                triggers: [
                    {disabled: true, name: 'trigger_1', type: 'manual/v1/manual'},
                    {disabled: false, name: 'trigger_2', type: 'webhook/v1/webhook'},
                ],
            })
        ).toBe('trigger_1');
    });

    it('falls back to the first trigger when every trigger is disabled', () => {
        expect(
            getTestRunTriggerName({
                triggers: [
                    {disabled: true, name: 'trigger_1', type: 'webhook/v1/webhook'},
                    {disabled: true, name: 'trigger_2', type: 'webhook/v1/webhook'},
                ],
            })
        ).toBe('trigger_1');
    });
});
