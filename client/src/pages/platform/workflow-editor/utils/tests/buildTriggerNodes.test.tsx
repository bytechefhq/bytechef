import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {ComponentDefinitionBasic, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import {buildTriggerNodes} from '../layoutUtils';

const definitions = [
    {icon: '<svg/>', name: 'webhook', title: 'Webhook'},
    {icon: '<svg/>', name: 'schedule', title: 'Schedule'},
] as ComponentDefinitionBasic[];

describe('buildTriggerNodes', () => {
    it('builds one node per trigger plus a placeholder', () => {
        const triggers = [
            {name: 'trigger_1', type: 'webhook/v1/onReceive'},
            {name: 'trigger_2', type: 'schedule/v1/onInterval'},
        ] as WorkflowTrigger[];

        const {placeholderNode, triggerNodes} = buildTriggerNodes(triggers, definitions, 1200);

        expect(triggerNodes.map((node) => node.id)).toEqual(['trigger_1', 'trigger_2']);
        expect(triggerNodes.every((node) => node.data.trigger === true)).toBe(true);
        expect(placeholderNode.id).toBe(TRIGGER_PLACEHOLDER_NODE_ID);
        expect(placeholderNode.type).toBe('triggerPlaceholder');
    });

    it('falls back to the Manual placeholder node when there are no triggers', () => {
        const {triggerNodes} = buildTriggerNodes([], definitions, 1200);

        expect(triggerNodes).toHaveLength(1);
        expect(triggerNodes[0].id).toBe('trigger_1');
        expect(triggerNodes[0].data.componentName).toBe('manual');
    });

    it('uses a fallback node when a trigger component definition is missing', () => {
        const triggers = [{name: 'trigger_1', type: 'unknown/v1/onThing'}] as WorkflowTrigger[];

        const {triggerNodes} = buildTriggerNodes(triggers, definitions, 1200);

        expect(triggerNodes).toHaveLength(1);
        expect(triggerNodes[0].data.trigger).toBe(true);
        expect(triggerNodes[0].data.componentName).toBe('unknown');
    });
});
