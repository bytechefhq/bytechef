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

    it('supports loop dispatchers, including loops nested in conditions', () => {
        const nodes = [
            taskNode('task1'),
            dispatcherNode('condition_1', 'condition'),
            dispatcherNode('loop_1', 'loop'),
            dispatcherNode('loop_2', 'loop'),
        ];

        expect(isElkLayoutSupported(nodes)).toBe(true);
    });

    it('supports childless dispatchers as plain nodes', () => {
        for (const componentName of ['loopBreak', 'subflow', 'terminate']) {
            expect(
                isElkLayoutSupported([dispatcherNode('loop_1', 'loop'), dispatcherNode('leaf_1', componentName)])
            ).toBe(true);
        }
    });

    it('supports branch dispatchers', () => {
        expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('branch_1', 'branch')])).toBe(true);
    });

    it('supports parallel and fork-join dispatchers', () => {
        for (const componentName of ['fork-join', 'parallel']) {
            expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('dispatcher_1', componentName)])).toBe(true);
        }
    });

    it('supports each, map, and on-error dispatchers', () => {
        for (const componentName of ['each', 'map', 'on-error']) {
            expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('dispatcher_1', componentName)])).toBe(true);
        }
    });

    it('supports cluster roots (AI agent, data stream, approval)', () => {
        const clusterRootNode: Node = {
            data: {clusterRoot: true, componentName: 'aiAgent', workflowNodeName: 'aiAgent_1'},
            id: 'aiAgent_1',
            position: {x: 0, y: 0},
            type: 'clusterRoot',
        };

        expect(isElkLayoutSupported([taskNode('task1'), clusterRootNode])).toBe(true);
    });

    it('rejects an unknown future dispatcher', () => {
        expect(isElkLayoutSupported([taskNode('task1'), dispatcherNode('mystery_1', 'mystery-dispatcher')])).toBe(
            false
        );
    });

    it('supports an empty node list', () => {
        expect(isElkLayoutSupported([])).toBe(true);
    });
});
