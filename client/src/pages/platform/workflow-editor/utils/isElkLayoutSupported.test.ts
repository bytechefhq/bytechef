import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import isElkLayoutSupported from './isElkLayoutSupported';

const taskNode = (id: string): Node => ({
    data: {componentName: 'mailchimp', workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const dispatcherNode = (id: string, componentName: string): Node => ({
    data: {componentName, taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

describe('isElkLayoutSupported', () => {
    it('supports plain task chains', () => {
        expect(isElkLayoutSupported([taskNode('task1'), taskNode('task2')])).toBe(true);
    });

    it('supports condition dispatchers, including nested ones', () => {
        const nodes = [
            taskNode('task1'),
            dispatcherNode('condition_1', 'condition'),
            dispatcherNode('condition_2', 'condition'),
            taskNode('task2'),
        ];

        expect(isElkLayoutSupported(nodes)).toBe(true);
    });

    it('rejects any non-condition dispatcher', () => {
        for (const componentName of ['branch', 'each', 'forkJoin', 'loop', 'map', 'parallel']) {
            expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('dispatcher_1', componentName)])).toBe(
                false
            );
        }
    });

    it('rejects AI agent cluster roots', () => {
        const clusterRootNode: Node = {
            data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
            id: 'aiAgent_1',
            position: {x: 0, y: 0},
            type: 'clusterRoot',
        };

        expect(isElkLayoutSupported([taskNode('task1'), clusterRootNode])).toBe(false);
    });

    it('supports an empty node list', () => {
        expect(isElkLayoutSupported([])).toBe(true);
    });
});
