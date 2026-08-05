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

        // Placed a full node-width past the rightmost trigger (300) so it clears the label.
        expect(slot.position.x).toBeGreaterThan(300 + 240);

        // Vertically centered on the 72px icon box: triggerY (50) + (72 - 48) / 2 = 62.
        expect(slot.position.y).toBe(62);
    });

    it('is a no-op when there is no placeholder', () => {
        const nodes = [triggerNode('trigger_1', 100)];

        expect(() => positionTriggerPlaceholder(nodes, 'TB')).not.toThrow();
    });
});
