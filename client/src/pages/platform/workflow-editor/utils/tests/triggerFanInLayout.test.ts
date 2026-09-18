import {FINAL_PLACEHOLDER_NODE_ID, NODE_WIDTH, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    buildTriggerFanInEdges,
    getDagreNodeSize,
    getLabelCrossOverhang,
    getLayoutElements,
    getTriggerRowDagreWidth,
} from '../layoutUtils';

const triggerNode = (id: string): Node => ({data: {trigger: true}, id, position: {x: 0, y: 0}, type: 'workflow'});

const labelledTriggerNode = (id: string, label: string): Node => ({
    data: {label, operationName: 'newRecord', trigger: true, workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const taskNode = (id: string): Node => ({data: {}, id, position: {x: 0, y: 0}, type: 'workflow'});

describe('buildTriggerFanInEdges', () => {
    it('connects a single trigger with a plain workflow edge', () => {
        const edges = buildTriggerFanInEdges([triggerNode('trigger_1')], 'task_1');

        expect(edges).toEqual([
            expect.objectContaining({
                data: undefined,
                id: 'trigger_1=>task_1',
                source: 'trigger_1',
                target: 'task_1',
                type: 'workflow',
            }),
        ]);
    });

    it('fans every trigger into the first task with the add button only on the middle edge', () => {
        const edges = buildTriggerFanInEdges(
            [triggerNode('trigger_1'), triggerNode('trigger_2'), triggerNode('trigger_3')],
            'task_1'
        );

        expect(edges.map((edge) => edge.type)).toEqual(['smoothstep', 'workflow', 'smoothstep']);
        expect(edges.every((edge) => edge.target === 'task_1')).toBe(true);
        expect(edges.every((edge) => (edge.data as {triggerFanIn?: boolean})?.triggerFanIn === true)).toBe(true);
    });

    it('draws every trigger edge as a placeholder edge when the triggers feed the final placeholder', () => {
        const edges = buildTriggerFanInEdges(
            [triggerNode('trigger_1'), triggerNode('trigger_2'), triggerNode('trigger_3')],
            FINAL_PLACEHOLDER_NODE_ID
        );

        expect(edges.map((edge) => edge.type)).toEqual(['placeholder', 'placeholder', 'placeholder']);
        expect(edges.every((edge) => (edge.data as {triggerFanIn?: boolean})?.triggerFanIn === true)).toBe(true);
    });

    it('keeps a single trigger feeding the final placeholder as a plain placeholder edge', () => {
        const [edge] = buildTriggerFanInEdges([triggerNode('trigger_1')], FINAL_PLACEHOLDER_NODE_ID);

        expect(edge.type).toBe('placeholder');
        expect(edge.data).toBeUndefined();
    });
});

describe('getDagreNodeSize for triggers', () => {
    it('reserves a narrower cross-axis footprint for a trigger than for a task in TB', () => {
        expect(getDagreNodeSize(triggerNode('trigger_1'), 'TB').width).toBeLessThan(NODE_WIDTH);
    });

    it('reserves a narrower cross-axis footprint for a trigger than for a task in LR', () => {
        expect(getDagreNodeSize(triggerNode('trigger_1'), 'LR').height).toBeLessThan(NODE_WIDTH);
    });

    it('uses the shared trigger row width in TB only', () => {
        expect(getDagreNodeSize(triggerNode('trigger_1'), 'TB', 300).width).toBe(300);
        expect(getDagreNodeSize(triggerNode('trigger_1'), 'LR', 300).height).toBe(
            getDagreNodeSize(triggerNode('trigger_1'), 'LR').height
        );
    });

    it('does not treat the add-trigger slot as a trigger', () => {
        const slot: Node = {data: {trigger: true}, id: TRIGGER_PLACEHOLDER_NODE_ID, position: {x: 0, y: 0}};

        expect(getDagreNodeSize(slot, 'TB').width).toBe(NODE_WIDTH);
    });
});

describe('getLayoutElements with multiple triggers', () => {
    it('lays the triggers out on one row above the first task and centers the row on the canvas', async () => {
        const triggers = [triggerNode('trigger_1'), triggerNode('trigger_2')];
        const nodes = [...triggers, taskNode('task_1'), {...taskNode(FINAL_PLACEHOLDER_NODE_ID), type: 'placeholder'}];

        const edges = [
            ...buildTriggerFanInEdges(triggers, 'task_1'),
            {id: `task_1=>${FINAL_PLACEHOLDER_NODE_ID}`, source: 'task_1', target: FINAL_PLACEHOLDER_NODE_ID},
        ];

        const canvasWidth = 1200;

        const {nodes: layoutNodes} = await getLayoutElements({canvasWidth, direction: 'TB', edges, nodes});

        const position = (id: string) => layoutNodes.find((node) => node.id === id)!.position;

        expect(position('trigger_1').y).toBe(position('trigger_2').y);
        expect(position('trigger_1').x).not.toBe(position('trigger_2').x);
        expect(position('task_1').y).toBeGreaterThan(position('trigger_1').y);

        const triggerRowCenter = (position('trigger_1').x + position('trigger_2').x) / 2;

        expect(Math.abs(triggerRowCenter + 72 / 2 - canvasWidth / 2)).toBeLessThan(1);
    });
});

describe('getTriggerRowDagreWidth', () => {
    it('keeps the default width when every label is short', () => {
        expect(getTriggerRowDagreWidth([triggerNode('trigger_1'), triggerNode('trigger_2')])).toBe(
            getDagreNodeSize(triggerNode('trigger_1'), 'TB').width
        );
    });

    it('sizes the whole row by its longest label', () => {
        const shortTrigger = labelledTriggerNode('trigger_1', 'Manual');
        const longTrigger = labelledTriggerNode('trigger_2', 'A trigger with a much longer label');

        expect(getTriggerRowDagreWidth([shortTrigger, longTrigger])).toBe(
            getTriggerRowDagreWidth([longTrigger, shortTrigger])
        );
        expect(getTriggerRowDagreWidth([shortTrigger, longTrigger])).toBeGreaterThan(
            getTriggerRowDagreWidth([shortTrigger])
        );
    });
});

describe('getLayoutElements trigger row spacing', () => {
    it('leaves room for the node menu button between a label and the next trigger in TB', async () => {
        const triggers = [
            labelledTriggerNode('trigger_1', 'Agile CRM'),
            labelledTriggerNode('trigger_2', 'Airtable'),
            labelledTriggerNode('trigger_3', 'Manual'),
        ];
        const nodes = [...triggers, taskNode('task_1')];

        const {nodes: layoutNodes} = await getLayoutElements({
            canvasWidth: 1200,
            direction: 'TB',
            edges: buildTriggerFanInEdges(triggers, 'task_1'),
            nodes,
        });

        const laidOutTriggers = layoutNodes
            .filter((node) => node.id.startsWith('trigger_'))
            .sort((first, second) => first.position.x - second.position.x);

        for (let index = 1; index < laidOutTriggers.length; index++) {
            const previousTrigger = laidOutTriggers[index - 1];
            const previousLabelEnd = previousTrigger.position.x + 72 + getLabelCrossOverhang(previousTrigger);

            expect(laidOutTriggers[index].position.x - previousLabelEnd).toBeGreaterThanOrEqual(96);
        }
    });
});

describe('getLabelCrossOverhang', () => {
    it('grows with the longest label line', () => {
        const shortLabel: Node = {data: {operationName: 'get', title: 'Chat'}, id: 'trigger_1', position: {x: 0, y: 0}};
        const longLabel: Node = {
            data: {operationName: 'newIncomingWebhookRequest', title: 'Chat'},
            id: 'trigger_2',
            position: {x: 0, y: 0},
        };

        expect(getLabelCrossOverhang(longLabel)).toBeGreaterThan(getLabelCrossOverhang(shortLabel));
    });

    it('caps the overhang for very long labels', () => {
        const veryLongLabel: Node = {data: {title: 'x'.repeat(120)}, id: 'trigger_1', position: {x: 0, y: 0}};

        expect(getLabelCrossOverhang(veryLongLabel)).toBe(200);
    });
});
