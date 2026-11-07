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

describe('upsertTrigger cluster elements', () => {
    const clusterElements = {
        tools: [{name: 'httpClient_1', parameters: {}, type: 'httpClient/v1/get'}],
        voiceAgent: {name: 'voiceAgent_1', parameters: {}, type: 'deepgram/v1/voiceAgent'},
    };

    it('keeps the existing cluster elements when the replacement carries none', () => {
        const existing = [{clusterElements, name: 'trigger_1', type: 'browser/v1/voiceSession'}] as WorkflowTrigger[];
        const next = {name: 'trigger_1', parameters: {}, type: 'browser/v1/voiceSession'} as WorkflowTrigger;

        expect(upsertTrigger(existing, next)[0].clusterElements).toEqual(clusterElements);
    });

    it('takes the replacement cluster elements when it carries them', () => {
        const existing = [{clusterElements, name: 'trigger_1', type: 'browser/v1/voiceSession'}] as WorkflowTrigger[];
        const next = {
            clusterElements: {tools: []},
            name: 'trigger_1',
            type: 'browser/v1/voiceSession',
        } as WorkflowTrigger;

        expect(upsertTrigger(existing, next)[0].clusterElements).toEqual({tools: []});
    });
});

describe('upsertTrigger cluster elements across a type change', () => {
    const clusterElements = {
        tools: [{name: 'httpClient_1', parameters: {}, type: 'httpClient/v1/get'}],
        voiceAgent: {name: 'voiceAgent_1', parameters: {}, type: 'deepgram/v1/voiceAgent'},
    };

    it('drops the existing cluster elements when the replacement is a different trigger type', () => {
        const existing = [{clusterElements, name: 'trigger_1', type: 'browser/v1/voiceSession'}] as WorkflowTrigger[];
        const next = {name: 'trigger_1', parameters: {}, type: 'manual/v1/manual'} as WorkflowTrigger;

        expect(upsertTrigger(existing, next)[0].clusterElements).toBeUndefined();
    });

    it('keeps the existing cluster elements when the same trigger type is saved without them', () => {
        const existing = [{clusterElements, name: 'trigger_1', type: 'browser/v1/voiceSession'}] as WorkflowTrigger[];
        const next = {label: 'Voice', name: 'trigger_1', type: 'browser/v1/voiceSession'} as WorkflowTrigger;

        expect(upsertTrigger(existing, next)[0].clusterElements).toEqual(clusterElements);
    });
});
