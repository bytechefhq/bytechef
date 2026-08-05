import {TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {positionTriggerPlaceholder} from '../layoutUtils';

const triggerNode = (id: string, x: number, operationName = 'get'): Node =>
    ({data: {operationName, title: 'Chat', trigger: true}, id, position: {x, y: 50}}) as Node;

describe('positionTriggerPlaceholder', () => {
    it('places the slot past the rightmost trigger’s label in TB', () => {
        const nodes = [
            triggerNode('trigger_1', 100),
            triggerNode('trigger_2', 300, 'newChatRequest'),
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'TB');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        // Past where the trigger's own label reaches, not a constant footprint: the 72px icon plus
        // the overhang estimated from its longest label line ("newChatRequest", 14 chars ->
        // 16 + 14 * 9 = 142). 300 + 72 + 142 + 40 = 554.
        expect(slot.position.x).toBe(554);

        // Vertically centered on the 72px icon box: triggerY (50) + (72 - 48) / 2 = 62.
        expect(slot.position.y).toBe(62);
    });

    it('follows a longer label further out in TB', () => {
        const shortLabelNodes = [
            triggerNode('trigger_1', 100),
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];
        const longLabelNodes = [
            triggerNode('trigger_1', 100, 'newIncomingWebhookRequest'),
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(shortLabelNodes, 'TB');
        positionTriggerPlaceholder(longLabelNodes, 'TB');

        const shortLabelSlot = shortLabelNodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;
        const longLabelSlot = longLabelNodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        // A constant footprint placed both at the same x, which is what ran the longer label into
        // the slot.
        expect(longLabelSlot.position.x).toBeGreaterThan(shortLabelSlot.position.x);
    });

    it('places the slot below the lowest trigger in LR, centred on the icon box', () => {
        const nodes = [
            {data: {trigger: true}, id: 'trigger_1', position: {x: 100, y: 50}} as Node,
            {data: {trigger: true}, id: 'trigger_2', position: {x: 100, y: 260}} as Node,
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'LR');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        // One gap past what the LOWEST trigger renders — 72px icon box plus its 64px label block
        // — so the connector stays the same short length it is in TB: 260 + 72 + 64 + 40 = 436.
        // Matching the wider interval the triggers sit apart stretched it to roughly double.
        expect(slot.position.y).toBe(436);

        // Centred on the 72px icon box, allowing for the slot box's own 8px inset:
        // 100 + (72 - 48) / 2 - 8 = 104.
        expect(slot.position.x).toBe(104);
    });

    it('places the slot one gap past a lone trigger', () => {
        const nodes = [
            {data: {trigger: true}, id: 'trigger_1', position: {x: 100, y: 50}} as Node,
            {data: {label: '+'}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}} as Node,
        ];

        positionTriggerPlaceholder(nodes, 'LR');

        const slot = nodes.find((node) => node.id === TRIGGER_PLACEHOLDER_NODE_ID)!;

        // Same one-gap placement with a single trigger: 50 + 72 + 64 + 40 = 226.
        expect(slot.position.y).toBe(226);
    });

    it('is a no-op when there is no placeholder', () => {
        const nodes = [triggerNode('trigger_1', 100)];

        expect(() => positionTriggerPlaceholder(nodes, 'TB')).not.toThrow();
    });
});
