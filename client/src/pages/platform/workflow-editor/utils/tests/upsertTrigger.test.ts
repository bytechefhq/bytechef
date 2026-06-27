import {WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import upsertTrigger from '../upsertTrigger';

describe('upsertTrigger', () => {
    it('appends when the trigger name is new', () => {
        const existing = [{name: 'trigger_1', type: 'webhook/v1/onReceive'}] as WorkflowTrigger[];
        const next = {name: 'trigger_2', type: 'schedule/v1/onInterval'} as WorkflowTrigger;

        const result = upsertTrigger(existing, next);

        expect(result.map((trigger) => trigger.name)).toEqual(['trigger_1', 'trigger_2']);
    });

    it('replaces in place when the name already exists, preserving existing metadata', () => {
        const existing = [
            {metadata: {ui: {nodePosition: {x: 10, y: 20}}}, name: 'trigger_1', type: 'webhook/v1/onReceive'},
            {name: 'trigger_2', type: 'schedule/v1/onInterval'},
        ] as WorkflowTrigger[];
        const next = {name: 'trigger_1', type: 'manual/v1/manual'} as WorkflowTrigger;

        const result = upsertTrigger(existing, next);

        expect(result).toHaveLength(2);
        expect(result[0].type).toBe('manual/v1/manual');
        expect(result[0].metadata).toEqual({ui: {nodePosition: {x: 10, y: 20}}});
        expect(result[1].name).toBe('trigger_2');
    });
});
