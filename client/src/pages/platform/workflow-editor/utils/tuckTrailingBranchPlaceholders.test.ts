import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createForkJoinEdges from './createForkJoinEdges';
import createForkJoinNode from './createForkJoinNode';
import createParallelEdges from './createParallelEdges';
import createParallelNode from './createParallelNode';
import {getElkLayoutElements} from './elkLayoutUtils';
import {getLayoutElements} from './layoutUtils';

const NODE_ANCHOR_HALF = 36;

const PLACEHOLDER_BOX_HALF = 36;

type DispatcherKindType = 'fork-join' | 'parallel';

const DISPATCHER_ID: Record<DispatcherKindType, string> = {
    'fork-join': 'fork-join_1',
    parallel: 'parallel_1',
};

function buildWorkflow(dispatcherKind: DispatcherKindType, laneCount: number): {edges: Edge[]; nodes: Node[]} {
    const dispatcherId = DISPATCHER_ID[dispatcherKind];

    const laneTasks = Array.from({length: laneCount}, (_, index) => ({
        name: `accelo_${index + 1}`,
        type: 'accelo/v1/createCompany',
    }));

    const triggerNode: Node = {
        data: {componentName: 'manual', label: 'Manual', name: 'trigger_1', trigger: true},
        id: 'trigger_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const dispatcherNode: Node = {
        data: {
            componentName: dispatcherKind,
            label: dispatcherKind,
            name: dispatcherId,
            parameters:
                dispatcherKind === 'parallel'
                    ? {tasks: laneTasks}
                    : {branches: laneTasks.map((laneTask) => [laneTask])},
            taskDispatcher: true,
            taskDispatcherId: dispatcherId,
            workflowNodeName: dispatcherId,
        },
        id: dispatcherId,
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const laneNodes: Node[] = laneTasks.map((laneTask, index) => ({
        data: {
            componentName: 'accelo',
            label: 'Accelo',
            name: laneTask.name,
            operationName: 'createCompany',
            workflowNodeName: laneTask.name,
            ...(dispatcherKind === 'parallel'
                ? {parallelData: {index, parallelId: dispatcherId}}
                : {forkJoinData: {branchIndex: index, forkJoinId: dispatcherId, index: 0}}),
        },
        id: laneTask.name,
        position: {x: 0, y: 0},
        type: 'workflow',
    }));

    const dispatcherNodes =
        dispatcherKind === 'parallel'
            ? createParallelNode({allNodes: [dispatcherNode], parallelId: dispatcherId})
            : createForkJoinNode({allNodes: [dispatcherNode], forkJoinId: dispatcherId});

    const dispatcherEdges =
        dispatcherKind === 'parallel' ? createParallelEdges(dispatcherNode) : createForkJoinEdges(dispatcherNode);

    return {
        edges: [
            {id: `trigger_1=>${dispatcherId}`, source: 'trigger_1', target: dispatcherId, type: 'workflow'},
            ...dispatcherEdges,
        ],
        nodes: [triggerNode, ...dispatcherNodes, ...laneNodes],
    };
}

const cases = (['dagre', 'elk'] as const).flatMap((engine) =>
    (['parallel', 'fork-join'] as const).map((dispatcherKind) => [engine, dispatcherKind] as const)
);

describe.each(cases)('trailing add-a-branch placeholder (%s, %s)', (engine, dispatcherKind) => {
    const layoutFunction = engine === 'elk' ? getElkLayoutElements : getLayoutElements;
    const dispatcherId = DISPATCHER_ID[dispatcherKind];

    it('centres the dispatcher between a single lane and the "+", the two sides of its frame', async () => {
        const {edges, nodes} = buildWorkflow(dispatcherKind, 1);

        const result = await layoutFunction({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const positionOf = (nodeId: string) => result.nodes.find((node) => node.id === nodeId)!.position;

        const laneAnchor = positionOf('accelo_1').x + NODE_ANCHOR_HALF;
        const placeholderCenter =
            result.nodes.find(
                (node) => node.type === 'placeholder' && (node.data as {taskDispatcherId?: string}).taskDispatcherId
            )!.position.x + PLACEHOLDER_BOX_HALF;

        expect(edges.find((edge) => edge.target === 'accelo_1')!.sourceHandle).toMatch(/-left$/);
        expect(laneAnchor).toBeLessThan(positionOf(dispatcherId).x);
        expect(
            Math.abs(positionOf(dispatcherId).x + NODE_ANCHOR_HALF - (laneAnchor + placeholderCenter) / 2)
        ).toBeLessThan(1);
    });

    it.each([2, 3, 4])('centres the dispatcher on %i real lanes, ignoring the "+" column', async (laneCount) => {
        const {edges, nodes} = buildWorkflow(dispatcherKind, laneCount);

        const result = await layoutFunction({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const positionOf = (nodeId: string) => result.nodes.find((node) => node.id === nodeId)!.position;

        const laneAnchors = Array.from(
            {length: laneCount},
            (_, index) => positionOf(`accelo_${index + 1}`).x + NODE_ANCHOR_HALF
        );

        const lanesCenter = (Math.min(...laneAnchors) + Math.max(...laneAnchors)) / 2;

        expect(Math.abs(positionOf(dispatcherId).x + NODE_ANCHOR_HALF - lanesCenter)).toBeLessThan(1);

        laneAnchors.slice(1).forEach((laneAnchor, index) => {
            expect(laneAnchor - laneAnchors[index]).toBeGreaterThanOrEqual(240);
        });
    });

    it('pulls the "+" in against the last lane instead of reserving a full task column', async () => {
        const {edges, nodes} = buildWorkflow(dispatcherKind, 2);

        const result = await layoutFunction({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const lastLaneX = result.nodes.find((node) => node.id === 'accelo_2')!.position.x;
        const placeholderX = result.nodes.find(
            (node) => node.type === 'placeholder' && (node.data as {taskDispatcherId?: string}).taskDispatcherId
        )!.position.x;

        expect(placeholderX).toBeGreaterThan(lastLaneX + 72);
        expect(placeholderX - lastLaneX).toBeLessThan(240);
    });
});
