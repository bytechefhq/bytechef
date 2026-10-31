import {FINAL_PLACEHOLDER_NODE_ID, TRIGGER_PLACEHOLDER_NODE_ID} from '@/shared/constants';
import {WorkflowTask, WorkflowTrigger} from '@/shared/middleware/platform/configuration';
import {describe, expect, it} from 'vitest';

import buildWorkflowGraph from './buildWorkflowGraph';

const TRIGGER: WorkflowTrigger = {name: 'trigger_1', type: 'manual/v1/manual'};

function buildTask(name: string, type = 'httpClient/v1/get', parameters: Record<string, unknown> = {}): WorkflowTask {
    return {name, parameters, type};
}

function build(tasks: WorkflowTask[] | undefined, triggers: WorkflowTrigger[] | undefined = [TRIGGER]) {
    return buildWorkflowGraph({
        canvasWidth: 800,
        componentDefinitions: [],
        taskDispatcherDefinitions: [],
        tasks,
        triggers,
    });
}

describe('buildWorkflowGraph', () => {
    it('builds trigger, task and placeholder nodes in order for a linear workflow', () => {
        const {nodes} = build([buildTask('t1'), buildTask('t2')]);

        expect(nodes.map((node) => node.id)).toEqual([
            'trigger_1',
            TRIGGER_PLACEHOLDER_NODE_ID,
            't1',
            't2',
            FINAL_PLACEHOLDER_NODE_ID,
        ]);
    });

    it('creates an edge between consecutive tasks and a fan-in edge from the trigger', () => {
        const {edges} = build([buildTask('t1'), buildTask('t2')]);

        expect(edges.map((edge) => edge.id)).toEqual([`trigger_1=>t1`, `t1=>t2`, `t2=>${FINAL_PLACEHOLDER_NODE_ID}`]);
    });

    it('builds condition child nodes with ghost nodes', () => {
        const {nodes} = build([buildTask('condition_1', 'condition/v1', {caseFalse: [], caseTrue: []})]);

        const nodeIds = nodes.map((node) => node.id);

        expect(nodeIds).toContain('condition_1');
        expect(nodeIds).toContain('condition_1-condition-bottom-ghost');
        expect(nodeIds[nodeIds.length - 1]).toBe(FINAL_PLACEHOLDER_NODE_ID);
    });

    it('returns only placeholders when there are no tasks', () => {
        const {edges, nodes} = build([]);

        expect(nodes.map((node) => node.id)).toEqual([
            'trigger_1',
            TRIGGER_PLACEHOLDER_NODE_ID,
            FINAL_PLACEHOLDER_NODE_ID,
        ]);
        expect(edges.map((edge) => edge.id)).toEqual([`trigger_1=>${FINAL_PLACEHOLDER_NODE_ID}`]);
    });

    it('returns fresh arrays on every call', () => {
        const tasks = [buildTask('t1')];

        const first = build(tasks);
        const second = build(tasks);

        expect(first.nodes).not.toBe(second.nodes);
        expect(first.edges).not.toBe(second.edges);
    });
});
