import {LayoutDirectionType} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {getElkLayoutElements} from '../elkLayoutUtils';
import {getDagreNodeSize, getLayoutElements} from '../layoutUtils';

const TRIGGER_BOX_SIZE = 72;

const clusterFrame = {clusterRootId: 'aiAgent_1', contentOrigin: {x: 32, y: 40}, height: 320, width: 640};

const nodes: Node[] = [
    {data: {trigger: true}, id: 'trigger_1', position: {x: 0, y: 0}, type: 'workflow'},
    {
        data: {clusterElements: {model: {name: 'openAi_1'}}, clusterFrame, clusterRoot: true},
        id: 'aiAgent_1',
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    },
    {data: {}, id: 'logger_1', position: {x: 0, y: 0}, type: 'workflow'},
];

const edges: Edge[] = [
    {id: 'trigger_1=>aiAgent_1', source: 'trigger_1', target: 'aiAgent_1', type: 'workflow'},
    {id: 'aiAgent_1=>logger_1', source: 'aiAgent_1', target: 'logger_1', type: 'workflow'},
];

const layoutEngines = [
    ['dagre', getLayoutElements],
    ['elk', getElkLayoutElements],
] as const;

const directions: LayoutDirectionType[] = ['TB', 'LR'];

function positionOf(layoutNodes: Node[], nodeId: string) {
    return layoutNodes.find((node) => node.id === nodeId)!.position;
}

describe('getDagreNodeSize for a cluster frame', () => {
    it.each(directions)('reserves the frame box in %s', (direction) => {
        expect(getDagreNodeSize(nodes[1], direction)).toEqual({height: 320, width: 640});
    });
});

describe.each(layoutEngines)('%s layout of a cluster frame', (_engineName, layoutElements) => {
    it.each(directions)('centers the frame on the trunk in %s', async (direction) => {
        const {nodes: layoutNodes} = await layoutElements({
            canvasHeight: 800,
            canvasWidth: 1200,
            direction,
            edges,
            nodes,
        });

        const triggerPosition = positionOf(layoutNodes, 'trigger_1');
        const framePosition = positionOf(layoutNodes, 'aiAgent_1');

        if (direction === 'TB') {
            expect(framePosition.x + clusterFrame.width / 2).toBeCloseTo(triggerPosition.x + TRIGGER_BOX_SIZE / 2, 0);
        } else {
            expect(framePosition.y + clusterFrame.height / 2).toBeCloseTo(triggerPosition.y + TRIGGER_BOX_SIZE / 2, 0);
        }
    });

    it.each(directions)('places the next task past the far edge of the frame in %s', async (direction) => {
        const {nodes: layoutNodes} = await layoutElements({
            canvasHeight: 800,
            canvasWidth: 1200,
            direction,
            edges,
            nodes,
        });

        const framePosition = positionOf(layoutNodes, 'aiAgent_1');
        const nextTaskPosition = positionOf(layoutNodes, 'logger_1');

        if (direction === 'TB') {
            expect(nextTaskPosition.y).toBeGreaterThan(framePosition.y + clusterFrame.height);
        } else {
            expect(nextTaskPosition.x).toBeGreaterThan(framePosition.x + clusterFrame.width);
        }
    });
});
