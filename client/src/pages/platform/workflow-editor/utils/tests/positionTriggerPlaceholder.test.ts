import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {positionTriggerPlaceholder} from '../layoutUtils';

const triggerNode = (id: string, x: number): Node => ({data: {trigger: true}, id, position: {x, y: 50}}) as Node;

describe('positionTriggerPlaceholder', () => {
    it('places the slot to the right of the rightmost trigger in TB', () => {
        const nodes = [
            triggerNode('trigger_1', 100),
            triggerNode('trigger_2', 300),
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'TB');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        expect(slot.position.x).toBeGreaterThan(300 + 240);

        expect(slot.position.y).toBe(72);
    });

    it('places the slot below the lowest trigger in LR', () => {
        const nodes = [
            {data: {trigger: true}, id: 'trigger_1', position: {x: 50, y: 100}} as Node,
            {data: {trigger: true}, id: 'trigger_2', position: {x: 50, y: 300}} as Node,
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'LR');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        expect(slot.position.x).toBe(50);

        expect(slot.position.y).toBeGreaterThan(300);
    });

    it('is a no-op when there are no triggers', () => {
        const slot = {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 7, y: 9}} as Node;

        positionTriggerPlaceholder([slot], 'TB');

        expect(slot.position).toEqual({x: 7, y: 9});
    });

    it('is a no-op when there is no placeholder', () => {
        const nodes = [triggerNode('trigger_1', 100)];

        expect(() => positionTriggerPlaceholder(nodes, 'TB')).not.toThrow();
    });
});
