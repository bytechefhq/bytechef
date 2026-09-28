import {NodeDataType} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createBranchEdges from '../utils/createBranchEdges';
import createConditionEdges from '../utils/createConditionEdges';
import createEachEdges from '../utils/createEachEdges';
import createForkJoinEdges from '../utils/createForkJoinEdges';
import createLoopEdges from '../utils/createLoopEdges';
import createMapEdges from '../utils/createMapEdges';
import createOnErrorEdges from '../utils/createOnErrorEdges';
import createParallelEdges from '../utils/createParallelEdges';

function dispatcherNode(id: string, componentName: string, parameters: Record<string, unknown>): Node {
    return {
        data: {componentName, parameters, taskDispatcher: true} as unknown as NodeDataType,
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

function subtask(name: string, type: string) {
    return {name, parameters: {}, type};
}

const nestedForkJoin = subtask('forkJoin_1', 'fork-join/v1');
const nestedLastForkJoin = subtask('forkJoin_2', 'fork-join/v1');
const action = subtask('action_1', 'test/v1/action');

const allNodes = [
    dispatcherNode('forkJoin_1', 'fork-join', {branches: []}),
    dispatcherNode('forkJoin_2', 'fork-join', {branches: []}),
];

function edgeSources(edges: Edge[]): string[] {
    return edges.map((edge) => edge.source);
}

function edgeIds(edges: Edge[]): string[] {
    return edges.map((edge) => edge.id);
}

describe('edges of a camelCased fork-join nested in another dispatcher', () => {
    it('branch leaves a nested fork-join from its bottom ghost', () => {
        const edges = createBranchEdges(
            dispatcherNode('branch_1', 'branch', {
                cases: [{key: 'case_0', tasks: [nestedLastForkJoin]}],
                default: [nestedForkJoin, action],
            })
        );

        expect(edgeIds(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost=>action_1');
        expect(edgeSources(edges)).toContain('forkJoin_2-forkJoin-bottom-ghost');
    });

    it('condition leaves a nested fork-join from its bottom ghost', () => {
        const edges = createConditionEdges(
            dispatcherNode('condition_1', 'condition', {
                caseFalse: [nestedLastForkJoin],
                caseTrue: [nestedForkJoin, action],
            }),
            allNodes
        );

        expect(edgeIds(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost=>action_1');
        expect(edgeSources(edges)).toContain('forkJoin_2-forkJoin-bottom-ghost');
    });

    it('condition wires a childless dispatcher straight to the next subtask', () => {
        const edges = createConditionEdges(
            dispatcherNode('condition_1', 'condition', {
                caseFalse: [],
                caseTrue: [subtask('subflow_1', 'subflow/v1'), action],
            }),
            allNodes
        );

        expect(edgeIds(edges)).toContain('subflow_1=>action_1');
    });

    it('on-error leaves a nested fork-join from its bottom ghost', () => {
        const edges = createOnErrorEdges(
            dispatcherNode('onError_1', 'on-error', {
                'main-branch': [nestedForkJoin, action],
                'on-error-branch': [nestedLastForkJoin],
            }),
            allNodes
        );

        expect(edgeIds(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost=>action_1');
        expect(edgeSources(edges)).toContain('forkJoin_2-forkJoin-bottom-ghost');
    });

    it('fork-join leaves a nested fork-join from its bottom ghost', () => {
        const edges = createForkJoinEdges(
            dispatcherNode('forkJoin_3', 'fork-join', {branches: [[nestedForkJoin, action], [nestedLastForkJoin]]})
        );

        expect(edgeIds(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost=>action_1');
        expect(edgeSources(edges)).toContain('forkJoin_2-forkJoin-bottom-ghost');
    });

    it('each leaves a nested fork-join from its bottom ghost', () => {
        const edges = createEachEdges(dispatcherNode('each_1', 'each', {iteratee: nestedForkJoin}));

        expect(edgeSources(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost');
    });

    it('parallel leaves a nested fork-join from its bottom ghost', () => {
        const edges = createParallelEdges(dispatcherNode('parallel_1', 'parallel', {tasks: [nestedForkJoin]}));

        expect(edgeSources(edges)).toContain('forkJoin_1-forkJoin-bottom-ghost');
    });

    it('loop does not wire a nested fork-join straight to the next subtask', () => {
        const edges = createLoopEdges(dispatcherNode('loop_1', 'loop', {iteratee: [nestedForkJoin, action]}));

        expect(edgeIds(edges)).not.toContain('forkJoin_1=>action_1');
    });

    it('map does not wire a nested fork-join straight to the next subtask', () => {
        const edges = createMapEdges(dispatcherNode('map_1', 'map', {iteratee: [nestedForkJoin, action]}));

        expect(edgeIds(edges)).not.toContain('forkJoin_1=>action_1');
    });
});
