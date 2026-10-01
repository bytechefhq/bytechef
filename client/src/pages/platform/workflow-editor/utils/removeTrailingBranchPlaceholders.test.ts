import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createParallelEdges from './createParallelEdges';
import createParallelNode from './createParallelNode';
import {getElkLayoutElements} from './elkLayoutUtils';
import {getLayoutElements} from './layoutUtils';
import removeTrailingBranchPlaceholders from './removeTrailingBranchPlaceholders';

const PLACEHOLDER_ID = 'parallel_1-parallel-placeholder-0';
const TOP_GHOST_ID = 'parallel_1-parallel-top-ghost';
const BOTTOM_GHOST_ID = 'parallel_1-parallel-bottom-ghost';
const LEFT_GHOST_ID = 'parallel_1-taskDispatcher-left-ghost';

function buildReadOnlyParallel(laneCount: number): {edges: Edge[]; nodes: Node[]} {
    const tasks = Array.from({length: laneCount}, (_, index) => ({
        name: `accelo_${index + 1}`,
        type: 'accelo/v1/createCompany',
    }));

    const parallelNode: Node = {
        data: {componentName: 'parallel', parameters: {tasks}, taskDispatcher: true, taskDispatcherId: 'parallel_1'},
        id: 'parallel_1',
        position: {x: 0, y: 0},
        type: 'readonly',
    };

    const nodes = createParallelNode({
        allNodes: [parallelNode],
        options: {createLeftGhost: laneCount <= 1},
        parallelId: 'parallel_1',
    }).map((node) => (node.type === 'placeholder' ? {...node, type: 'readonlyPlaceholder'} : node));

    const laneNodes: Node[] = tasks.map((task, index) => ({
        data: {
            componentName: 'accelo',
            label: 'Accelo',
            name: task.name,
            operationName: 'createCompany',
            parallelData: {index, parallelId: 'parallel_1'},
            workflowNodeName: task.name,
        },
        id: task.name,
        position: {x: 0, y: 0},
        type: 'readonly',
    }));

    return {edges: createParallelEdges(parallelNode), nodes: [...nodes, ...laneNodes]};
}

describe('removeTrailingBranchPlaceholders', () => {
    it('drops the "+" and both of its edges when the dispatcher has lanes', () => {
        const {edges, nodes} = buildReadOnlyParallel(2);

        const result = removeTrailingBranchPlaceholders(nodes, edges);

        expect(result.nodes.some((node) => node.id === PLACEHOLDER_ID)).toBe(false);
        expect(result.edges.some((edge) => edge.source === PLACEHOLDER_ID || edge.target === PLACEHOLDER_ID)).toBe(
            false
        );
        expect(result.edges.find((edge) => edge.target === 'accelo_1')!.sourceHandle).toBe(`${TOP_GHOST_ID}-left`);
        expect(result.edges.find((edge) => edge.target === 'accelo_2')!.sourceHandle).toBe(`${TOP_GHOST_ID}-right`);
    });

    it('keeps a single lane on the bar right end, opposite the left rail, so the frame stays a box', () => {
        const {edges, nodes} = buildReadOnlyParallel(1);

        const result = removeTrailingBranchPlaceholders(nodes, edges);

        expect(result.nodes.some((node) => node.id === PLACEHOLDER_ID)).toBe(false);
        expect(result.edges.find((edge) => edge.target === 'accelo_1')!.sourceHandle).toBe(`${TOP_GHOST_ID}-right`);
        expect(result.edges.find((edge) => edge.source === 'accelo_1')!.targetHandle).toBe(`${BOTTOM_GHOST_ID}-right`);
        expect(result.edges.find((edge) => edge.target === LEFT_GHOST_ID)!.sourceHandle).toBe(`${TOP_GHOST_ID}-left`);
    });

    it('keeps the "+" of an empty dispatcher, which is all that closes its frame', () => {
        const {edges, nodes} = buildReadOnlyParallel(0);

        const result = removeTrailingBranchPlaceholders(nodes, edges);

        expect(result.nodes).toBe(nodes);
        expect(result.edges).toBe(edges);
    });

    describe.each([
        ['dagre', getLayoutElements],
        ['elk', getElkLayoutElements],
    ] as const)('laid out by %s', (_, layoutFunction) => {
        it('keeps a single lane right of the dispatcher, opposite the left rail', async () => {
            const readOnlyParallel = buildReadOnlyParallel(1);
            const {edges, nodes} = removeTrailingBranchPlaceholders(readOnlyParallel.nodes, readOnlyParallel.edges);

            const result = await layoutFunction({canvasWidth: 1200, direction: 'TB', edges, nodes});

            const positionOf = (nodeId: string) => result.nodes.find((node) => node.id === nodeId)!.position;

            expect(positionOf('accelo_1').x).toBeGreaterThan(positionOf('parallel_1').x);
            expect(positionOf(LEFT_GHOST_ID).x).toBeLessThan(positionOf('parallel_1').x);
        });

        it.each([2, 3])('centres the dispatcher over %i lanes', async (laneCount) => {
            const readOnlyParallel = buildReadOnlyParallel(laneCount);
            const {edges, nodes} = removeTrailingBranchPlaceholders(readOnlyParallel.nodes, readOnlyParallel.edges);

            const result = await layoutFunction({canvasWidth: 1200, direction: 'TB', edges, nodes});

            const positionOf = (nodeId: string) => result.nodes.find((node) => node.id === nodeId)!.position;

            const laneXs = Array.from({length: laneCount}, (_, index) => positionOf(`accelo_${index + 1}`).x);

            expect(Math.abs(positionOf('parallel_1').x - (Math.min(...laneXs) + Math.max(...laneXs)) / 2)).toBeLessThan(
                1
            );
        });
    });
});
