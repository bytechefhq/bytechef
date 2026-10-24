import {type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import getWorkflowTaskCount from '../getWorkflowTaskCount';

const createNode = (id: string, type: string, data: Record<string, unknown> = {}): Node => ({
    data: {name: id, ...data},
    id,
    position: {x: 0, y: 0},
    type,
});

describe('getWorkflowTaskCount', () => {
    it('counts tasks but not the trigger', () => {
        const nodes = [
            createNode('trigger_1', 'workflow', {trigger: true}),
            createNode('var_1', 'workflow'),
            createNode('var_2', 'workflow'),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(2);
    });

    it('counts a cluster root once and none of its cluster elements', () => {
        const nodes = [
            createNode('trigger_1', 'workflow', {trigger: true}),
            createNode('aiAgent_1', 'clusterRoot', {clusterRoot: true}),
            createNode('openAi_1', 'workflow', {clusterElementType: 'model'}),
            createNode('checkForViolations_1', 'workflow', {clusterElementType: 'guardrails'}),
            createNode('keywords_1', 'workflow', {clusterElementType: 'checkForViolations'}),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(1);
    });

    it('does not count a nested cluster root such as a sub-agent', () => {
        const nodes = [
            createNode('aiAgent_1', 'clusterRoot', {clusterRoot: true}),
            createNode('subagent_1', 'clusterRoot', {clusterRoot: true, isNestedClusterRoot: true}),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(1);
    });

    it('counts the tasks inside a task dispatcher but not the dispatcher', () => {
        const nodes = [
            createNode('condition_1', 'workflow', {taskDispatcher: true}),
            createNode('var_1', 'workflow'),
            createNode('var_2', 'workflow'),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(2);
    });

    it('counts a cluster root the same on the read-only canvas', () => {
        const nodes = [
            createNode('trigger_1', 'readonly', {trigger: true}),
            createNode('aiAgent_1', 'readonly', {clusterRoot: true}),
            createNode('aiAgent_2', 'readonly', {clusterRoot: true}),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(2);
    });

    it('ignores placeholders, ghost nodes and sticky notes', () => {
        const nodes = [
            createNode('var_1', 'workflow'),
            createNode('placeholder-1', 'placeholder'),
            createNode('ghost-1', 'taskDispatcherLeftGhostNode'),
            createNode('note-1', 'stickyNote'),
        ];

        expect(getWorkflowTaskCount(nodes)).toBe(1);
    });
});
