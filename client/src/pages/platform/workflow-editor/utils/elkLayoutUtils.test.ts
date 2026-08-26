import {GRAPH_TRANSITION_EDGE_TYPE} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {buildElkGraph, getElkLayoutElements, getFrameId} from './elkLayoutUtils';

import type {ElkNode} from 'elkjs/lib/elk-api';

// The distance between consecutive node origins on the main axis: the 72px
// icon anchor box plus the uniform CHAIN_GAP. Identical for every consecutive
// pair, at every nesting depth, in both TB and LR — the engine's core invariant.
const CHAIN_GAP = 80;

// Box-adjacent edges (condition→frame bar, bar→next node): layer gap + one
// 14px anchor slack.
const BOX_GAP = 66;

// The frame's entry bar is pulled toward the condition so the TRUE/FALSE labels
// read as attached to the box. TB pulls 28 (clean 38px corridor); LR pulls 16
// (50px corridor) because its rotated labels live inside the node→bar gap and
// need room. See TB_BAR_LABEL_PULL / LR_BAR_LABEL_PULL in elkLayoutUtils.
const TOP_BOX_GAP = 38;
const LR_TOP_BOX_GAP = 50;
const BAR_TO_CHILD_GAP = 94;
const CHAIN_STEP = 72 + CHAIN_GAP;

const taskNode = (
    id: string,
    conditionParent?: {conditionCase: 'caseTrue' | 'caseFalse'; conditionId: string}
): Node => ({
    data: {
        componentName: 'mailchimp',
        ...(conditionParent
            ? {
                  conditionData: {
                      conditionCase: conditionParent.conditionCase,
                      conditionId: conditionParent.conditionId,
                      index: 0,
                  },
              }
            : {}),
        workflowNodeName: id,
    },
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const conditionNode = (
    id: string,
    conditionParent?: {conditionCase: 'caseTrue' | 'caseFalse'; conditionId: string}
): Node => ({
    data: {
        componentName: 'condition',
        ...(conditionParent
            ? {
                  conditionData: {
                      conditionCase: conditionParent.conditionCase,
                      conditionId: conditionParent.conditionId,
                      index: 0,
                  },
              }
            : {}),
        taskDispatcher: true,
        taskDispatcherId: id,
        workflowNodeName: id,
    },
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const conditionGhostNodes = (conditionId: string): Node[] => [
    {
        data: {conditionId, taskDispatcherId: conditionId},
        id: `${conditionId}-condition-top-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherTopGhostNode',
    },
    {
        data: {conditionId, taskDispatcherId: conditionId},
        id: `${conditionId}-condition-bottom-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherBottomGhostNode',
    },
];

const conditionPlaceholderNode = (conditionId: string, side: 'left' | 'right'): Node => ({
    data: {
        conditionCase: side === 'left' ? 'caseTrue' : 'caseFalse',
        conditionId,
        label: '+',
        taskDispatcherId: conditionId,
    },
    id: `${conditionId}-condition-${side}-placeholder-0`,
    position: {x: 0, y: 0},
    type: 'placeholder',
});

const loopNode = (
    id: string,
    owner?: {conditionCase: 'caseTrue' | 'caseFalse'; conditionId: string} | {loopId: string}
): Node => ({
    data: {
        componentName: 'loop',
        ...(owner && 'conditionId' in owner
            ? {
                  conditionData: {
                      conditionCase: owner.conditionCase,
                      conditionId: owner.conditionId,
                      index: 0,
                  },
              }
            : {}),
        ...(owner && 'loopId' in owner ? {loopData: {index: 0, loopId: owner.loopId}} : {}),
        taskDispatcher: true,
        taskDispatcherId: id,
        workflowNodeName: id,
    },
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

const loopChildTaskNode = (id: string, loopId: string): Node => ({
    data: {componentName: 'mailchimp', loopData: {index: 0, loopId}, workflowNodeName: id},
    id,
    position: {x: 0, y: 0},
    type: 'workflow',
});

// Mirrors createLoopNode: top ghost, loop-back rail ghost, bottom ghost — the
// bottom ghost genuinely has no loopId in its data, only taskDispatcherId
const loopAuxNodes = (loopId: string): Node[] => [
    {
        data: {loopId, taskDispatcherId: loopId},
        id: `${loopId}-loop-top-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherTopGhostNode',
    },
    {
        data: {loopId, taskDispatcherId: loopId},
        id: `${loopId}-taskDispatcher-left-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherLeftGhostNode',
    },
    {
        data: {taskDispatcherId: loopId},
        id: `${loopId}-loop-bottom-ghost`,
        position: {x: 0, y: 0},
        type: 'taskDispatcherBottomGhostNode',
    },
];

const loopPlaceholderNode = (loopId: string): Node => ({
    data: {label: '+', loopId, taskDispatcherId: loopId},
    id: `${loopId}-loop-placeholder-0`,
    position: {x: 0, y: 0},
    type: 'placeholder',
});

const edge = (source: string, target: string): Edge => ({id: `${source}=>${target}`, source, target});

// Base loop wiring per createLoopEdges: loop→top, top→rail→bottom (rail), plus
// top→content→bottom supplied by the caller
const loopStructureEdges = (loopId: string): Edge[] => [
    edge(loopId, `${loopId}-loop-top-ghost`),
    edge(`${loopId}-loop-top-ghost`, `${loopId}-taskDispatcher-left-ghost`),
    edge(`${loopId}-taskDispatcher-left-ghost`, `${loopId}-loop-bottom-ghost`),
];

const childIds = (elkNode: ElkNode | undefined): string[] => (elkNode?.children ?? []).map((child) => child.id).sort();

const findChild = (elkNode: ElkNode, id: string): ElkNode | undefined =>
    (elkNode.children ?? []).find((child) => child.id === id);

const collectScopeEdgeViolations = (elkNode: ElkNode): string[] => {
    const violations: string[] = [];

    const memberIds = new Set((elkNode.children ?? []).map((child) => child.id));

    (elkNode.edges ?? []).forEach((scopeEdge) => {
        [...(scopeEdge.sources ?? []), ...(scopeEdge.targets ?? [])].forEach((endpointId) => {
            if (!memberIds.has(endpointId)) {
                violations.push(`${scopeEdge.id}: ${endpointId} not in scope ${elkNode.id}`);
            }
        });
    });

    (elkNode.children ?? []).forEach((child) => violations.push(...collectScopeEdgeViolations(child)));

    return violations;
};

const singleConditionFixture = () => {
    const nodes: Node[] = [
        taskNode('task1'),
        conditionNode('condition_1'),
        ...conditionGhostNodes('condition_1'),
        taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
        taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        taskNode('task2'),
    ];

    const edges: Edge[] = [
        edge('task1', 'condition_1'),
        edge('condition_1', 'condition_1-condition-top-ghost'),
        edge('condition_1-condition-top-ghost', 'childTrue1'),
        edge('condition_1-condition-top-ghost', 'childFalse1'),
        edge('childTrue1', 'condition_1-condition-bottom-ghost'),
        edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        edge('condition_1-condition-bottom-ghost', 'task2'),
    ];

    return {edges, nodes};
};

describe('buildElkGraph', () => {
    it('lays a linear chain flat in the root scope', () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['task1', 'task2', 'task3']);
        expect(graph.edges).toHaveLength(2);
    });

    it('wraps condition members in a frame and keeps the condition node outside it', () => {
        const {edges, nodes} = singleConditionFixture();

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['condition_1', getFrameId('condition_1'), 'task1', 'task2']);

        const frame = findChild(graph, getFrameId('condition_1'));

        expect(childIds(frame)).toEqual([
            'childFalse1',
            'childTrue1',
            'condition_1-condition-bottom-ghost',
            'condition_1-condition-top-ghost',
        ]);
    });

    it('remaps root edges onto the frame', () => {
        const {edges, nodes} = singleConditionFixture();

        const graph = buildElkGraph(nodes, edges, 'TB');

        const rootEdgePairs = (graph.edges ?? []).map(
            (scopeEdge) => `${scopeEdge.sources[0]}=>${scopeEdge.targets[0]}`
        );

        expect(rootEdgePairs.sort()).toEqual([
            `condition_1=>${getFrameId('condition_1')}`,
            `${getFrameId('condition_1')}=>task2`,
            'task1=>condition_1',
        ]);
    });

    it('produces no cross-hierarchy edges anywhere', () => {
        const {edges, nodes} = singleConditionFixture();

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);
    });

    it('places empty-branch placeholders inside the frame', () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
        ];

        const frame = findChild(buildElkGraph(nodes, edges, 'TB'), getFrameId('condition_1'));

        expect(childIds(frame)).toContain('condition_1-condition-left-placeholder-0');
        expect(childIds(frame)).toContain('condition_1-condition-right-placeholder-0');
    });

    it('nests a condition frame inside its parent frame', () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionNode('condition_2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('innerChild', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'innerChild'),
            edge('innerChild', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const graph = buildElkGraph(nodes, edges, 'TB');

        const outerFrame = findChild(graph, getFrameId('condition_1'));

        expect(childIds(outerFrame)).toEqual([
            'childFalse1',
            'condition_1-condition-bottom-ghost',
            'condition_1-condition-top-ghost',
            'condition_2',
            getFrameId('condition_2'),
        ]);

        const innerFrame = findChild(outerFrame!, getFrameId('condition_2'));

        expect(childIds(innerFrame)).toEqual([
            'condition_2-condition-bottom-ghost',
            'condition_2-condition-top-ghost',
            'innerChild',
        ]);

        expect(collectScopeEdgeViolations(graph)).toEqual([]);
    });

    it('terminates on cyclic condition ownership from malformed state', () => {
        const nodes: Node[] = [
            conditionNode('condition_a', {conditionCase: 'caseTrue', conditionId: 'condition_b'}),
            ...conditionGhostNodes('condition_a'),
            conditionNode('condition_b', {conditionCase: 'caseTrue', conditionId: 'condition_a'}),
            ...conditionGhostNodes('condition_b'),
        ];

        const edges: Edge[] = [edge('condition_a-condition-top-ghost', 'condition_b-condition-top-ghost')];

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(graph.id).toEqual('__root__');
    });

    it('falls back to the root scope when conditionData references a missing condition', () => {
        const nodes: Node[] = [
            taskNode('task1'),
            taskNode('orphanTask', {conditionCase: 'caseTrue', conditionId: 'condition_missing'}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [edge('task1', 'orphanTask'), edge('orphanTask', 'task2')];

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['orphanTask', 'task1', 'task2']);
    });
});

const positionOf = (layoutedNodes: Node[], id: string): {x: number; y: number} => {
    const layoutedNode = layoutedNodes.find((node) => node.id === id);

    if (!layoutedNode) {
        throw new Error(`Node ${id} missing from layout result`);
    }

    return layoutedNode.position;
};

describe('getElkLayoutElements', () => {
    it('spaces a TB chain uniformly (footprint gap = 50)', async () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const firstGap = positionOf(result.nodes, 'task2').y - positionOf(result.nodes, 'task1').y;
        const secondGap = positionOf(result.nodes, 'task3').y - positionOf(result.nodes, 'task2').y;

        expect(firstGap).toBe(CHAIN_STEP);
        expect(secondGap).toBe(CHAIN_STEP);
    });

    it('spaces an LR chain uniformly on the x axis', async () => {
        const nodes = [taskNode('task1'), taskNode('task2'), taskNode('task3')];
        const edges = [edge('task1', 'task2'), edge('task2', 'task3')];

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1000,
            direction: 'LR',
            edges,
            nodes,
        });

        const firstGap = positionOf(result.nodes, 'task2').x - positionOf(result.nodes, 'task1').x;
        const secondGap = positionOf(result.nodes, 'task3').x - positionOf(result.nodes, 'task2').x;

        // LR footprint width is 120 (see getDagreNodeSize) + 50 spacing
        expect(firstGap).toBe(CHAIN_STEP);
        expect(secondGap).toBe(CHAIN_STEP);
    });

    it('gives LR frame entries the same bar-to-child run as TB', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1200,
            direction: 'LR',
            edges,
            nodes,
        });

        const topGhostBarX = positionOf(result.nodes, 'condition_1-condition-top-ghost').x;

        // LR can't pull the bar toward the dispatcher (the rotated TRUE/FALSE
        // labels own that gap), so the interior inset supplies the same 94px
        // entry run instead — room for the edge add-button before the node
        expect(positionOf(result.nodes, 'childTrue1').x - (topGhostBarX + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(positionOf(result.nodes, 'childFalse1').x - (topGhostBarX + 2)).toBe(BAR_TO_CHILD_GAP);
    });

    it('uses the same gap inside a nested condition branch as at the root', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childTrue2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'childTrue2'),
            edge('childTrue2', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-bottom-ghost', 'task2'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const rootGap = positionOf(result.nodes, 'condition_1').y - positionOf(result.nodes, 'task1').y;
        const branchGap = positionOf(result.nodes, 'childTrue2').y - positionOf(result.nodes, 'childTrue1').y;

        expect(branchGap).toBe(CHAIN_STEP);
        expect(rootGap).toBe(CHAIN_STEP);
    });

    it('drops synthetic frame nodes from the result', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        expect(result.nodes.map((node) => node.id)).not.toContain(getFrameId('condition_1'));
        expect(result.nodes).toHaveLength(nodes.length);
    });

    it('centers the condition node and its ghosts on the same cross-axis line', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const conditionPosition = positionOf(result.nodes, 'condition_1');
        const topGhostPosition = positionOf(result.nodes, 'condition_1-condition-top-ghost');
        const bottomGhostPosition = positionOf(result.nodes, 'condition_1-condition-bottom-ghost');

        // rendered widths: condition anchor 72, ghosts 72
        const conditionCenter = conditionPosition.x + 36;
        const topGhostCenter = topGhostPosition.x + 36;
        const bottomGhostCenter = bottomGhostPosition.x + 36;

        expect(Math.abs(topGhostCenter - conditionCenter)).toBeLessThanOrEqual(1);
        expect(Math.abs(bottomGhostCenter - conditionCenter)).toBeLessThanOrEqual(1);
    });

    it('still lays out a node whose conditionData references a missing condition', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            taskNode('orphanTask', {conditionCase: 'caseTrue', conditionId: 'condition_missing'}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [edge('task1', 'orphanTask'), edge('orphanTask', 'task2')];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const orphanPosition = positionOf(result.nodes, 'orphanTask');

        expect(Number.isFinite(orphanPosition.x)).toBe(true);
        expect(Number.isFinite(orphanPosition.y)).toBe(true);
        expect(orphanPosition).not.toEqual({x: 0, y: 0});
    });

    it('uses the same gap three levels deep as at the root (TB)', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionNode('condition_2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            conditionNode('condition_3', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
            ...conditionGhostNodes('condition_3'),
            taskNode('deepChild1', {conditionCase: 'caseTrue', conditionId: 'condition_3'}),
            taskNode('deepChild2', {conditionCase: 'caseTrue', conditionId: 'condition_3'}),
        ];

        const edges: Edge[] = [
            edge('task1', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'condition_3'),
            edge('condition_3', 'condition_3-condition-top-ghost'),
            edge('condition_3-condition-top-ghost', 'deepChild1'),
            edge('deepChild1', 'deepChild2'),
            edge('deepChild2', 'condition_3-condition-bottom-ghost'),
            edge('condition_3-condition-bottom-ghost', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const rootGap = positionOf(result.nodes, 'condition_1').y - positionOf(result.nodes, 'task1').y;
        const innermostGap = positionOf(result.nodes, 'deepChild2').y - positionOf(result.nodes, 'deepChild1').y;

        expect(innermostGap).toBe(CHAIN_STEP);
        expect(rootGap).toBe(CHAIN_STEP);
    });

    it('uses the same gap inside a nested condition branch as at the root (LR)', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childTrue2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'childTrue2'),
            edge('childTrue2', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-bottom-ghost', 'task2'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1000,
            direction: 'LR',
            edges,
            nodes,
        });

        const rootGap = positionOf(result.nodes, 'condition_1').x - positionOf(result.nodes, 'task1').x;
        const branchGap = positionOf(result.nodes, 'childTrue2').x - positionOf(result.nodes, 'childTrue1').x;

        expect(branchGap).toBe(CHAIN_STEP);
        expect(rootGap).toBe(CHAIN_STEP);
    });

    it('honors saved node positions', async () => {
        const nodes = [taskNode('task1'), taskNode('task2')];

        (nodes[1].data as Record<string, unknown>).metadata = {ui: {nodePosition: {x: 400, y: 900}}};

        const result = await getElkLayoutElements({
            canvasWidth: 1000,
            direction: 'TB',
            edges: [edge('task1', 'task2')],
            nodes,
        });

        expect(positionOf(result.nodes, 'task2')).toEqual({x: 400, y: 900});
    });

    it('keeps the caseTrue branch on the TRUE side in TB', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        expect(positionOf(result.nodes, 'childTrue1').x).toBeLessThan(positionOf(result.nodes, 'childFalse1').x);
    });

    it('keeps the caseTrue branch on top in LR', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1000,
            direction: 'LR',
            edges,
            nodes,
        });

        expect(positionOf(result.nodes, 'childTrue1').y).toBeLessThan(positionOf(result.nodes, 'childFalse1').y);
    });

    it('keeps branch sides when the caseFalse branch outweighs the caseTrue branch', async () => {
        // TRUE branch: lone placeholder; FALSE branch: nested condition subtree.
        // Crossing minimization would swap these without model-order forcing.
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionNode('condition_2', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('innerChild', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'innerChild'),
            edge('innerChild', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // rendered centers: both the placeholder DOM box (mx-margins around the +) and the condition anchor are 72 wide
        const placeholderCenter = positionOf(result.nodes, 'condition_1-condition-left-placeholder-0').x + 36;
        const nestedConditionCenter = positionOf(result.nodes, 'condition_2').x + 36;

        expect(placeholderCenter).toBeLessThan(nestedConditionCenter);

        // Even with a wide caseFalse subtree, the condition sits midway between its
        // two branch entry axes — NOT over the frame's bounding-box center, which
        // would drift toward the wider subtree
        const conditionCenter = positionOf(result.nodes, 'condition_1').x + 36;

        expect(Math.abs((placeholderCenter + nestedConditionCenter) / 2 - conditionCenter)).toBeLessThanOrEqual(1);

        // The empty-branch placeholder sits midway between the two ghost bars on
        // the main axis, however deep the sibling branch is
        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;
        const placeholderMainCenter = positionOf(result.nodes, 'condition_1-condition-left-placeholder-0').y + 14;

        expect(Math.abs(placeholderMainCenter - (topGhostBarY + bottomGhostBarY + 2) / 2)).toBeLessThanOrEqual(1);
    });

    it('gives frame exit edges the node-to-node rhythm', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // Leaving a box reads like a node→node edge: bottom bar → next node = CHAIN_GAP
        const bottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;
        const nextTaskTop = positionOf(result.nodes, 'task2').y;

        expect(nextTaskTop - (bottomGhostBarY + 2)).toBe(CHAIN_GAP);
    });

    it('keeps merge stubs between nested and enclosing bottom bars at the box gap', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionNode('condition_2', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('innerChild', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'innerChild'),
            edge('innerChild', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const innerBottomBarY = positionOf(result.nodes, 'condition_2-condition-bottom-ghost').y;
        const outerBottomBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;

        expect(outerBottomBarY - (innerBottomBarY + 2)).toBe(BOX_GAP);
    });

    it('centers a short branch chain between the bars when its sibling is taller', async () => {
        // dagre parity (centerDispatcherChildrenOnMainAxis): the FALSE branch's
        // lone task floats centered in the frame interior instead of hugging the
        // top bar; the tall TRUE chain keeps its designed 94/66 gaps
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childTrue2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childTrue3', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'childTrue2'),
            edge('childTrue2', 'childTrue3'),
            edge('childTrue3', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;

        // Tall TRUE chain keeps the designed asymmetric gaps
        expect(positionOf(result.nodes, 'childTrue1').y - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(bottomGhostBarY - (positionOf(result.nodes, 'childTrue3').y + 72)).toBe(BOX_GAP);

        // Short FALSE chain floats centered in the interior
        const interiorCenter = (topGhostBarY + 2 + bottomGhostBarY) / 2;
        const falseChildCenter = positionOf(result.nodes, 'childFalse1').y + 36;

        expect(Math.abs(falseChildCenter - interiorCenter)).toBeLessThanOrEqual(1);
    });

    it('keeps a short chain at the entry bar when the interior is far taller than the chain', async () => {
        const trueChildren = Array.from({length: 7}, (unused, index) =>
            taskNode(`childTrue${index + 1}`, {conditionCase: 'caseTrue', conditionId: 'condition_1'})
        );

        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            ...trueChildren,
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            ...trueChildren.slice(0, -1).map((childNode, index) => edge(childNode.id, `childTrue${index + 2}`)),
            edge('childTrue7', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        // Beyond CHAIN_CENTERING_MAX_SLACK the short chain is NOT centered —
        // it hugs the entry bar like the tall chain, avoiding a floating
        // island between two huge voids
        expect(positionOf(result.nodes, 'childFalse1').y - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);
    });

    it('pins a trailing placeholder after a condition onto the chain axis', async () => {
        const {edges, nodes} = singleConditionFixture();

        nodes.push({data: {label: '+'}, id: 'final-placeholder', position: {x: 0, y: 0}, type: 'placeholder'});
        edges.push(edge('condition_1-condition-bottom-ghost', 'final-placeholder'));

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const conditionCenter = positionOf(result.nodes, 'condition_1').x + 36;
        const trailingPlaceholderCenter = positionOf(result.nodes, 'final-placeholder').x + 36;

        expect(Math.abs(trailingPlaceholderCenter - conditionCenter)).toBeLessThanOrEqual(1);
    });

    it('centers an empty condition frame on the condition node with a uniform gap', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // The frame box (spanned by the two case placeholders) is centered on the
        // condition's 72px anchor box
        const conditionCenter = positionOf(result.nodes, 'condition_1').x + 36;
        const leftPlaceholderCenter = positionOf(result.nodes, 'condition_1-condition-left-placeholder-0').x + 36;
        const rightPlaceholderCenter = positionOf(result.nodes, 'condition_1-condition-right-placeholder-0').x + 36;

        expect(Math.abs((leftPlaceholderCenter + rightPlaceholderCenter) / 2 - conditionCenter)).toBeLessThanOrEqual(1);

        // The visible edge from the condition icon to the frame's top ghost bar is
        // exactly BOX_GAP (layer gap + one anchor slack)
        const conditionBottom = positionOf(result.nodes, 'condition_1').y + 72;
        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        expect(topGhostBarY - conditionBottom).toBe(TOP_BOX_GAP);
    });

    it('keeps uniform box gaps in a frame with one populated and one empty branch', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('loggerTask', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'loggerTask'),
            edge('loggerTask', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const conditionBottom = positionOf(result.nodes, 'condition_1').y + 72;
        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;
        const loggerTaskTop = positionOf(result.nodes, 'loggerTask').y;
        const bottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;

        expect(topGhostBarY - conditionBottom).toBe(TOP_BOX_GAP);
        expect(loggerTaskTop - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(bottomGhostBarY - (loggerTaskTop + 72)).toBe(BOX_GAP);

        // Condition sits midway between the populated chain and the empty-branch placeholder
        const conditionCenter = positionOf(result.nodes, 'condition_1').x + 36;
        const loggerCenter = positionOf(result.nodes, 'loggerTask').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'condition_1-condition-right-placeholder-0').x + 36;

        expect(Math.abs((loggerCenter + placeholderCenter) / 2 - conditionCenter)).toBeLessThanOrEqual(1);
    });

    it('moves the whole frame with a dispatcher that has a saved position', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('loggerTask', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        (nodes[0].data as Record<string, unknown>).metadata = {ui: {nodePosition: {x: 400, y: 900}}};

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'loggerTask'),
            edge('loggerTask', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // The dispatcher honors its saved position...
        expect(positionOf(result.nodes, 'condition_1')).toEqual({x: 400, y: 900});

        // ...and its ghosts and children shift rigidly with it, keeping the frame's
        // internal geometry (uniform CHAIN_GAPs, ghosts centered on the condition)
        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;
        const loggerTaskTop = positionOf(result.nodes, 'loggerTask').y;
        const bottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;

        expect(topGhostBarY - (900 + 72)).toBe(TOP_BOX_GAP);
        expect(loggerTaskTop - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(bottomGhostBarY - (loggerTaskTop + 72)).toBe(BOX_GAP);

        const topGhostCenter = positionOf(result.nodes, 'condition_1-condition-top-ghost').x + 36;

        expect(Math.abs(topGhostCenter - (400 + 36))).toBeLessThanOrEqual(1);
    });

    it('floats a shallow sibling frame centered while keeping its box gap uniform', async () => {
        // condition_1's TRUE branch holds a shallow nested condition (empty
        // branches) while the FALSE branch holds a deeper one — the shallow
        // subtree floats centered in the outer interior as one rigid unit
        // (dagre parity: centerDispatcherChildrenOnMainAxis), so its
        // condition→box gap must stay the uniform TOP_BOX_GAP throughout
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionNode('condition_4', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_4'),
            conditionPlaceholderNode('condition_4', 'left'),
            conditionPlaceholderNode('condition_4', 'right'),
            conditionNode('condition_2', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('loggerTask', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
            conditionPlaceholderNode('condition_2', 'right'),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_4'),
            edge('condition_4', 'condition_4-condition-top-ghost'),
            edge('condition_4-condition-top-ghost', 'condition_4-condition-left-placeholder-0'),
            edge('condition_4-condition-top-ghost', 'condition_4-condition-right-placeholder-0'),
            edge('condition_4-condition-left-placeholder-0', 'condition_4-condition-bottom-ghost'),
            edge('condition_4-condition-right-placeholder-0', 'condition_4-condition-bottom-ghost'),
            edge('condition_4-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'loggerTask'),
            edge('loggerTask', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-top-ghost', 'condition_2-condition-right-placeholder-0'),
            edge('condition_2-condition-right-placeholder-0', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // The shallow frame's bar keeps the uniform box gap to its condition...
        const shallowConditionBottom = positionOf(result.nodes, 'condition_4').y + 72;
        const shallowTopGhostBarY = positionOf(result.nodes, 'condition_4-condition-top-ghost').y;

        expect(shallowTopGhostBarY - shallowConditionBottom).toBe(TOP_BOX_GAP);

        // ...the deep defining chain keeps the designed entry gap...
        const outerTopGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        expect(positionOf(result.nodes, 'condition_2').y - (outerTopGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);

        // ...and the shallow subtree floats centered in the outer interior
        const outerBottomGhostBarY = positionOf(result.nodes, 'condition_1-condition-bottom-ghost').y;
        const outerInteriorCenter = (outerTopGhostBarY + 2 + outerBottomGhostBarY) / 2;

        const shallowSubtreeStart = positionOf(result.nodes, 'condition_4').y;
        const shallowSubtreeEnd = positionOf(result.nodes, 'condition_4-condition-bottom-ghost').y + 2;
        const shallowSubtreeCenter = (shallowSubtreeStart + shallowSubtreeEnd) / 2;

        expect(Math.abs(shallowSubtreeCenter - outerInteriorCenter)).toBeLessThanOrEqual(1);
    });
});

describe('getElkLayoutElements with loops', () => {
    const populatedLoopFixture = () => {
        const nodes: Node[] = [
            taskNode('task1'),
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
            loopChildTaskNode('loopChild2', 'loop_1'),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'loop_1'),
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loopChild2'),
            edge('loopChild2', 'loop_1-loop-bottom-ghost'),
            edge('loop_1-loop-bottom-ghost', 'task2'),
        ];

        return {edges, nodes};
    };

    it('never synthesizes extra ring nodes — the content chain IS the right side', async () => {
        // Loop grammar: two verticals only. The builders' `-right` handle
        // edges draw the right side through the offset content column, so no
        // synthetic ticks and no handle rewriting are needed.
        const {edges: baseEdges, nodes} = populatedLoopFixture();

        const edges = baseEdges.map((currentEdge) => {
            if (currentEdge.id === 'loop_1-loop-top-ghost=>loopChild1') {
                return {...currentEdge, sourceHandle: 'loop_1-loop-top-ghost-right'};
            }

            if (currentEdge.id === 'loopChild2=>loop_1-loop-bottom-ghost') {
                return {...currentEdge, targetHandle: 'loop_1-loop-bottom-ghost-right'};
            }

            return currentEdge;
        });

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        expect(result.nodes.some((resultNode) => resultNode.id.endsWith('-taskDispatcher-right-rail'))).toBe(false);

        const entryEdge = result.edges.find((resultEdge) => resultEdge.id === 'loop_1-loop-top-ghost=>loopChild1');
        const exitEdge = result.edges.find((resultEdge) => resultEdge.id === 'loopChild2=>loop_1-loop-bottom-ghost');

        expect(entryEdge?.sourceHandle).toBe('loop_1-loop-top-ghost-right');
        expect(exitEdge?.targetHandle).toBe('loop_1-loop-bottom-ghost-right');
    });

    it('keeps empty-ring placeholder edges and condition side handles untouched', async () => {
        // The empty ring's "+" IS the ring's right side, and condition bars have
        // no rail — neither may be pulled onto the spine
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopPlaceholderNode('loop_1'),
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            {
                ...edge('loop_1-loop-top-ghost', 'loop_1-loop-placeholder-0'),
                sourceHandle: 'loop_1-loop-top-ghost-right',
            },
            {
                ...edge('loop_1-loop-placeholder-0', 'loop_1-loop-bottom-ghost'),
                targetHandle: 'loop_1-loop-bottom-ghost-right',
            },
            edge('loop_1-loop-bottom-ghost', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            {
                ...edge('condition_1-condition-top-ghost', 'childTrue1'),
                sourceHandle: 'condition_1-condition-top-ghost-left',
            },
            edge('childTrue1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const placeholderEntryEdge = result.edges.find(
            (resultEdge) => resultEdge.id === 'loop_1-loop-top-ghost=>loop_1-loop-placeholder-0'
        );
        const placeholderExitEdge = result.edges.find(
            (resultEdge) => resultEdge.id === 'loop_1-loop-placeholder-0=>loop_1-loop-bottom-ghost'
        );
        const conditionEntryEdge = result.edges.find(
            (resultEdge) => resultEdge.id === 'condition_1-condition-top-ghost=>childTrue1'
        );

        expect(placeholderEntryEdge?.sourceHandle).toBe('loop_1-loop-top-ghost-right');
        expect(placeholderExitEdge?.targetHandle).toBe('loop_1-loop-bottom-ghost-right');
        expect(conditionEntryEdge?.sourceHandle).toBe('condition_1-condition-top-ghost-left');
    });

    it('wraps loop members in a frame and keeps the loop node outside it', () => {
        const {edges, nodes} = populatedLoopFixture();

        const graph = buildElkGraph(nodes, edges, 'TB');

        expect(childIds(graph)).toEqual(['loop_1', getFrameId('loop_1'), 'task1', 'task2']);

        const frame = findChild(graph, getFrameId('loop_1'));

        // The loop-back rail is a decoration positioned after layout, not an ELK member
        expect(childIds(frame)).toEqual([
            'loopChild1',
            'loopChild2',
            'loop_1-loop-bottom-ghost',
            'loop_1-loop-top-ghost',
        ]);

        expect(collectScopeEdgeViolations(graph)).toEqual([]);
    });

    it('places the loop body on the ring right side with the rail mirrored left', async () => {
        const {edges, nodes} = populatedLoopFixture();

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // Loop grammar: exactly two verticals — the content chain IS the
        // ring's right side (offset off the spine), the rail mirrors it left,
        // so the loop node reads centered in its ring
        const loopCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const childCenter = positionOf(result.nodes, 'loopChild1').x + 36;

        expect(childCenter - loopCenter).toBe(100);

        const railCenter = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x + 1;

        expect(loopCenter - railCenter).toBe(100);

        // Loop boxes get the same top-bar pull as conditions; uniform chain step inside
        const loopBottom = positionOf(result.nodes, 'loop_1').y + 72;
        const topGhostBarY = positionOf(result.nodes, 'loop_1-loop-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'loop_1-loop-bottom-ghost').y;
        const firstChildTop = positionOf(result.nodes, 'loopChild1').y;
        const secondChildTop = positionOf(result.nodes, 'loopChild2').y;

        expect(topGhostBarY - loopBottom).toBe(TOP_BOX_GAP);
        expect(firstChildTop - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(secondChildTop - firstChildTop).toBe(CHAIN_STEP);
        expect(bottomGhostBarY - (secondChildTop + 72)).toBe(BOX_GAP);
    });

    it('centers an empty loop placeholder inside the frame', async () => {
        const nodes: Node[] = [loopNode('loop_1'), ...loopAuxNodes('loop_1'), loopPlaceholderNode('loop_1')];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loop_1-loop-placeholder-0'),
            edge('loop_1-loop-placeholder-0', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // The empty ring renders SQUARE: the "+" sits on the right edge and the
        // rail mirrors it, each half the ring's own bar-to-bar span off the axis
        const topGhostBarY = positionOf(result.nodes, 'loop_1-loop-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'loop_1-loop-bottom-ghost').y;
        const ringHalfWidth = (bottomGhostBarY - topGhostBarY + 2) / 2;

        const loopCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'loop_1-loop-placeholder-0').x + 36;

        expect(placeholderCenter - loopCenter).toBe(ringHalfWidth);

        const railCenter = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x + 1;

        expect(loopCenter - railCenter).toBe(ringHalfWidth);

        const placeholderMainCenter = positionOf(result.nodes, 'loop_1-loop-placeholder-0').y + 14;

        expect(Math.abs(placeholderMainCenter - (topGhostBarY + bottomGhostBarY + 2) / 2)).toBeLessThanOrEqual(1);
    });

    it('renders the empty loop ring square in LR too', async () => {
        const nodes: Node[] = [loopNode('loop_1'), ...loopAuxNodes('loop_1'), loopPlaceholderNode('loop_1')];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loop_1-loop-placeholder-0'),
            edge('loop_1-loop-placeholder-0', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 800,
            canvasWidth: 1000,
            direction: 'LR',
            edges,
            nodes,
        });

        // LR ring span differs from TB (no label pull, wider placeholder
        // footprint) — the square derives from the direction's own span. The
        // content side flips in LR: the "+" rides the TOP edge, rail below.
        const topGhostBarX = positionOf(result.nodes, 'loop_1-loop-top-ghost').x;
        const bottomGhostBarX = positionOf(result.nodes, 'loop_1-loop-bottom-ghost').x;
        const ringHalfWidth = (bottomGhostBarX - topGhostBarX + 2) / 2;

        const loopCenter = positionOf(result.nodes, 'loop_1').y + 36;
        const placeholderCenter = positionOf(result.nodes, 'loop_1-loop-placeholder-0').y + 14;

        expect(loopCenter - placeholderCenter).toBe(ringHalfWidth);

        const railCenter = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').y + 1;

        expect(railCenter - loopCenter).toBe(ringHalfWidth);
    });

    it('keeps a loop on its branch side inside a condition and offsets its body', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            loopNode('loop_1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'loop_1'),
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loop_1-loop-bottom-ghost'),
            edge('loop_1-loop-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // Loop (caseTrue) stays left of the FALSE branch child
        expect(positionOf(result.nodes, 'loop_1').x).toBeLessThan(positionOf(result.nodes, 'childFalse1').x);

        // The loop body sits on the ring's right side even when nested
        const loopCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const loopChildCenter = positionOf(result.nodes, 'loopChild1').x + 36;

        expect(loopChildCenter - loopCenter).toBe(100);

        // Uniform pulled top gap at nesting depth
        const loopBottom = positionOf(result.nodes, 'loop_1').y + 72;
        const loopTopBarY = positionOf(result.nodes, 'loop_1-loop-top-ghost').y;

        expect(loopTopBarY - loopBottom).toBe(TOP_BOX_GAP);
    });

    it('lays out a condition nested inside a loop body', async () => {
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            conditionNode('condition_1', undefined),
            ...conditionGhostNodes('condition_1'),
            conditionPlaceholderNode('condition_1', 'left'),
            conditionPlaceholderNode('condition_1', 'right'),
        ];

        (nodes[4].data as Record<string, unknown>).loopData = {index: 0, loopId: 'loop_1'};

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'condition_1'),
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-left-placeholder-0'),
            edge('condition_1-condition-top-ghost', 'condition_1-condition-right-placeholder-0'),
            edge('condition_1-condition-left-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-right-placeholder-0', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-bottom-ghost', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        // The nested condition sits on the loop ring's right side, and its own
        // frame keeps the condition label pull
        const loopCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const conditionCenter = positionOf(result.nodes, 'condition_1').x + 36;

        expect(conditionCenter - loopCenter).toBe(100);

        const conditionBottom = positionOf(result.nodes, 'condition_1').y + 72;
        const conditionTopBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        expect(conditionTopBarY - conditionBottom).toBe(TOP_BOX_GAP);
    });

    it('keeps uniform gaps in a loop nested inside a loop', async () => {
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopNode('loop_2', {loopId: 'loop_1'}),
            ...loopAuxNodes('loop_2'),
            loopChildTaskNode('innerChild', 'loop_2'),
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loop_2'),
            ...loopStructureEdges('loop_2'),
            edge('loop_2-loop-top-ghost', 'innerChild'),
            edge('innerChild', 'loop_2-loop-bottom-ghost'),
            edge('loop_2-loop-bottom-ghost', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const outerLoopBottom = positionOf(result.nodes, 'loop_1').y + 72;
        const outerTopBarY = positionOf(result.nodes, 'loop_1-loop-top-ghost').y;
        const innerLoopBottom = positionOf(result.nodes, 'loop_2').y + 72;
        const innerTopBarY = positionOf(result.nodes, 'loop_2-loop-top-ghost').y;

        expect(outerTopBarY - outerLoopBottom).toBe(TOP_BOX_GAP);
        expect(innerTopBarY - innerLoopBottom).toBe(TOP_BOX_GAP);

        // Merge stub inner bottom bar → outer bottom bar keeps the box gap
        const innerBottomBarY = positionOf(result.nodes, 'loop_2-loop-bottom-ghost').y;
        const outerBottomBarY = positionOf(result.nodes, 'loop_1-loop-bottom-ghost').y;

        expect(outerBottomBarY - (innerBottomBarY + 2)).toBe(BOX_GAP);

        // Ring staircase: each loop's body column sits on its own ring's right
        // side, and each rail mirrors its own content offset — so the inner
        // ring nests fully right of the outer rail
        const outerCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const innerCenter = positionOf(result.nodes, 'loop_2').x + 36;

        expect(innerCenter - outerCenter).toBe(100);

        const innerRailCenter = positionOf(result.nodes, 'loop_2-taskDispatcher-left-ghost').x + 1;
        const outerRailCenter = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x + 1;

        expect(innerCenter - innerRailCenter).toBe(100);
        expect(outerRailCenter).toBeLessThan(innerRailCenter);
    });

    it('lays out childless dispatchers as plain chain nodes inside a loop', async () => {
        const loopBreakNode: Node = {
            data: {
                componentName: 'loopBreak',
                loopData: {index: 0, loopId: 'loop_1'},
                taskDispatcher: true,
                taskDispatcherId: 'loopBreak_1',
                workflowNodeName: 'loopBreak_1',
            },
            id: 'loopBreak_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
            loopBreakNode,
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loopBreak_1'),
            edge('loopBreak_1', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // loopBreak gets no frame and steps like an ordinary task
        expect(result.nodes.map((node) => node.id)).not.toContain(getFrameId('loopBreak_1'));

        const chainStep = positionOf(result.nodes, 'loopBreak_1').y - positionOf(result.nodes, 'loopChild1').y;

        expect(chainStep).toBe(CHAIN_STEP);
    });
});

describe('getElkLayoutElements with branches', () => {
    const branchNode = (id: string, caseKeys: string[]): Node => ({
        data: {
            componentName: 'branch',
            parameters: {cases: caseKeys.map((caseKey) => ({key: caseKey, tasks: []}))},
            taskDispatcher: true,
            taskDispatcherId: id,
            workflowNodeName: id,
        },
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const branchGhostNodes = (branchId: string): Node[] => [
        {
            data: {branchId, taskDispatcherId: branchId},
            id: `${branchId}-branch-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        {
            data: {branchId, taskDispatcherId: branchId},
            id: `${branchId}-branch-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const branchCasePlaceholderNode = (branchId: string, caseKey: string): Node => ({
        data: {branchId, caseKey, label: '+', taskDispatcherId: branchId},
        id: `${branchId}-branch-${caseKey}-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    });

    const branchChildTaskNode = (id: string, branchId: string, caseKey: string): Node => ({
        data: {branchData: {branchId, caseKey, index: 0}, componentName: 'mailchimp', workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    it('keeps empty case columns at full pitch beside a deep case subtree', async () => {
        // ELK may park sibling case placeholders in DIFFERENT layers (their
        // entry/exit edges span the whole frame), and cross positions of
        // disjoint layers may legally overlap — the placeholder mid-centering
        // pass then collapses them onto one row, overlapping their case chips.
        // The separation pass must restore the full footprint pitch.
        const nodes: Node[] = [
            branchNode('branch_1', ['case_0', 'case_1', 'case_2', 'case_3']),
            ...branchGhostNodes('branch_1'),
            branchCasePlaceholderNode('branch_1', 'default'),
            branchCasePlaceholderNode('branch_1', 'case_0'),
            branchCasePlaceholderNode('branch_1', 'case_1'),
            branchNode('branch_2', ['case_0']),
            ...branchGhostNodes('branch_2'),
            branchCasePlaceholderNode('branch_2', 'default'),
            branchChildTaskNode('subflow_1', 'branch_2', 'case_0'),
            branchCasePlaceholderNode('branch_1', 'case_3'),
        ];

        const branch2Node = nodes.find((candidateNode) => candidateNode.id === 'branch_2');

        branch2Node!.data = {
            ...branch2Node!.data,
            branchData: {branchId: 'branch_1', caseKey: 'case_2', index: 0},
        };

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-default-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_0-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_1-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_2'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_3-placeholder-0'),
            edge('branch_1-branch-default-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_0-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_1-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_3-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_2', 'branch_2-branch-top-ghost'),
            edge('branch_2-branch-top-ghost', 'branch_2-branch-default-placeholder-0'),
            edge('branch_2-branch-top-ghost', 'subflow_1'),
            edge('branch_2-branch-default-placeholder-0', 'branch_2-branch-bottom-ghost'),
            edge('subflow_1', 'branch_2-branch-bottom-ghost'),
            edge('branch_2-branch-bottom-ghost', 'branch_1-branch-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        // Case placeholder columns have 160px cross footprints + 50px gap, so
        // consecutive column centers must sit at least 210 apart
        const placeholderCenters = [
            'branch_1-branch-default-placeholder-0',
            'branch_1-branch-case_0-placeholder-0',
            'branch_1-branch-case_1-placeholder-0',
        ].map((placeholderId) => positionOf(result.nodes, placeholderId).x + 36);

        expect(placeholderCenters[1] - placeholderCenters[0]).toBeGreaterThanOrEqual(209);
        expect(placeholderCenters[2] - placeholderCenters[1]).toBeGreaterThanOrEqual(209);
    });

    it('stretches TB branch entries so case chips and add-buttons both fit', async () => {
        const nodes: Node[] = [
            branchNode('branch_1', ['case_0']),
            ...branchGhostNodes('branch_1'),
            branchChildTaskNode('child1', 'branch_1', 'case_0'),
            branchChildTaskNode('child2', 'branch_1', 'default'),
        ];

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'child1'),
            edge('branch_1-branch-top-ghost', 'child2'),
            edge('child1', 'branch_1-branch-bottom-ghost'),
            edge('child2', 'branch_1-branch-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1200, direction: 'TB', edges, nodes});

        const topGhostBarY = positionOf(result.nodes, 'branch_1-branch-top-ghost').y;

        // Branch entries stack [case chip][add-button] before the node, so they
        // get 26px on top of the standard 94px frame entry
        expect(positionOf(result.nodes, 'child1').y - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP + 26);
        expect(positionOf(result.nodes, 'child2').y - (topGhostBarY + 2)).toBe(BAR_TO_CHILD_GAP + 26);
    });

    // Mirrors the live "condition3" workflow: a wide TRUE branch subtree
    // (5 cases, one holding a nested branch) beside a deep FALSE loop stack
    const deepSiblingFixture = () => {
        const nodes: Node[] = [
            conditionNode('condition_3'),
            ...conditionGhostNodes('condition_3'),
            branchNode('branch_1', ['case_0', 'case_1', 'case_2', 'case_3']),
            ...branchGhostNodes('branch_1'),
            branchCasePlaceholderNode('branch_1', 'default'),
            branchCasePlaceholderNode('branch_1', 'case_0'),
            branchCasePlaceholderNode('branch_1', 'case_1'),
            branchNode('branch_2', ['case_0']),
            ...branchGhostNodes('branch_2'),
            branchCasePlaceholderNode('branch_2', 'default'),
            branchChildTaskNode('subflow_1', 'branch_2', 'case_0'),
            branchCasePlaceholderNode('branch_1', 'case_3'),
            loopNode('loop_3'),
            ...loopAuxNodes('loop_3'),
            loopChildTaskNode('accelo_2', 'loop_3'),
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            conditionNode('condition_5'),
            ...conditionGhostNodes('condition_5'),
            conditionPlaceholderNode('condition_5', 'left'),
            loopNode('loop_2'),
            ...loopAuxNodes('loop_2'),
            loopChildTaskNode('loopChild1', 'loop_2'),
            loopChildTaskNode('loopChild2', 'loop_2'),
            loopChildTaskNode('accelo_1', 'loop_1'),
        ];

        const setData = (nodeId: string, extraData: Record<string, unknown>) => {
            const targetNode = nodes.find((candidateNode) => candidateNode.id === nodeId);

            targetNode!.data = {...targetNode!.data, ...extraData};
        };

        setData('branch_1', {conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_3', index: 0}});
        setData('branch_2', {branchData: {branchId: 'branch_1', caseKey: 'case_2', index: 0}});
        setData('loop_3', {conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_3', index: 0}});
        setData('loop_1', {conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_3', index: 1}});
        setData('condition_5', {loopData: {index: 0, loopId: 'loop_1'}});
        setData('loop_2', {conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_5', index: 0}});

        const edges: Edge[] = [
            edge('condition_3', 'condition_3-condition-top-ghost'),
            edge('condition_3-condition-top-ghost', 'branch_1'),
            edge('condition_3-condition-top-ghost', 'loop_3'),
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-default-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_0-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_1-placeholder-0'),
            edge('branch_1-branch-top-ghost', 'branch_2'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_3-placeholder-0'),
            edge('branch_1-branch-default-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_0-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_1-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-case_3-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_2', 'branch_2-branch-top-ghost'),
            edge('branch_2-branch-top-ghost', 'branch_2-branch-default-placeholder-0'),
            edge('branch_2-branch-top-ghost', 'subflow_1'),
            edge('branch_2-branch-default-placeholder-0', 'branch_2-branch-bottom-ghost'),
            edge('subflow_1', 'branch_2-branch-bottom-ghost'),
            edge('branch_2-branch-bottom-ghost', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-bottom-ghost', 'condition_3-condition-bottom-ghost'),
            ...loopStructureEdges('loop_3'),
            edge('loop_3-loop-top-ghost', 'accelo_2'),
            edge('accelo_2', 'loop_3-loop-bottom-ghost'),
            edge('loop_3-loop-bottom-ghost', 'loop_1'),
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'condition_5'),
            edge('condition_5', 'condition_5-condition-top-ghost'),
            edge('condition_5-condition-top-ghost', 'condition_5-condition-left-placeholder-0'),
            edge('condition_5-condition-left-placeholder-0', 'condition_5-condition-bottom-ghost'),
            edge('condition_5-condition-top-ghost', 'loop_2'),
            ...loopStructureEdges('loop_2'),
            edge('loop_2-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loopChild2'),
            edge('loopChild2', 'loop_2-loop-bottom-ghost'),
            edge('loop_2-loop-bottom-ghost', 'condition_5-condition-bottom-ghost'),
            edge('condition_5-condition-bottom-ghost', 'accelo_1'),
            edge('accelo_1', 'loop_1-loop-bottom-ghost'),
            edge('loop_1-loop-bottom-ghost', 'condition_3-condition-bottom-ghost'),
        ];

        return {edges, nodes};
    };

    it('separates a wide TRUE branch subtree from a deep FALSE loop subtree', async () => {
        // The latent cross overlap between boxes in disjoint ELK layers becomes
        // a real edge crossing once chain centering floats the short subtree
        // into its sibling's band — the deep loop stack's rail and empty TRUE
        // column must never cross the wide branch subtree's rightmost case
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        // Rightmost TRUE-side extent: the case_3 placeholder column footprint
        // and the branch_2 subtree's subflow column (240 footprint)
        const trueRightEdge = Math.max(
            positionOf(result.nodes, 'branch_1-branch-case_3-placeholder-0').x + 36 + 100,
            positionOf(result.nodes, 'subflow_1').x + 36 + 120
        );

        // Leftmost FALSE-side extents: the deep loop's rail and the empty TRUE
        // column of the nested condition
        const falseRailX = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x;
        const falsePlaceholderLeft = positionOf(result.nodes, 'condition_5-condition-left-placeholder-0').x + 36 - 100;

        expect(falseRailX).toBeGreaterThan(trueRightEdge);
        expect(falsePlaceholderLeft).toBeGreaterThan(trueRightEdge);
    });

    it('packs sibling subtrees at the exact gap and re-anchors the parent', async () => {
        // The separation sweep must PULL in ELK's over-spaced raw cross
        // placement (computed for the pre-compaction banded layout), not only
        // push overlaps apart — and after repacking, the parent dispatcher must
        // sit back on its entries' anchor axis
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const trueRightEdge = Math.max(
            positionOf(result.nodes, 'branch_1-branch-case_3-placeholder-0').x + 36 + 100,
            positionOf(result.nodes, 'subflow_1').x + 36 + 120
        );

        const falseLeftEdge = Math.min(
            positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x,
            positionOf(result.nodes, 'condition_5-condition-left-placeholder-0').x + 36 - 100
        );

        // The exact 50px gap is enforced between the closest BAND-overlapping
        // pair of boxes (which now includes the column's axis spine and the
        // loop's opaque rectangle); this measured proxy pair may legitimately
        // sit closer — the pins guard strict separation and the pull-in from
        // the old 300px gulf
        expect(falseLeftEdge - trueRightEdge).toBeGreaterThan(0);
        expect(falseLeftEdge - trueRightEdge).toBeLessThanOrEqual(400);

        // Two entries (even count): the parent centers on their mean
        const trueEntryCenter = positionOf(result.nodes, 'branch_1').x + 36;
        const falseEntryCenter = positionOf(result.nodes, 'loop_3').x + 36;
        const parentCenter = positionOf(result.nodes, 'condition_3').x + 36;

        expect(Math.abs(parentCenter - (trueEntryCenter + falseEntryCenter) / 2)).toBeLessThanOrEqual(1);
    });

    it('packs a case with an asymmetric subtree band-tight against both neighbours', async () => {
        // branch_2's subtree is asymmetric around its axis (200px default
        // column left, 240px subflow column right). Band-aware packing places
        // each neighbour at the exact gap from the boxes it actually shares a
        // band with, so the two pitches differ by at most the subtree's own
        // box asymmetry — never by phantom mirrored space (the old symmetrized
        // envelopes reserved a subtree's full asymmetry as empty canvas, which
        // compounded through nesting levels on production workflows)
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const caseOneCenter = positionOf(result.nodes, 'branch_1-branch-case_1-placeholder-0').x + 36;
        const branchTwoCenter = positionOf(result.nodes, 'branch_2').x + 36;
        const caseThreeCenter = positionOf(result.nodes, 'branch_1-branch-case_3-placeholder-0').x + 36;

        const leftPitch = branchTwoCenter - caseOneCenter;
        const rightPitch = caseThreeCenter - branchTwoCenter;

        // Pitch asymmetry is bounded by the subtree's own box asymmetry PLUS
        // the one-sided label reservation (NODE_LABEL_CROSS_OVERHANG): titles
        // render only to the RIGHT of the icon, so the right neighbour packs
        // against the label edge while the left neighbour packs against the
        // footprint — collision honesty is deliberately chosen over symmetric
        // pitches (a symmetric model let edges slice through label text)
        expect(Math.abs(leftPitch - rightPitch)).toBeLessThanOrEqual(170);

        // Five entries (odd count): the branch dispatcher anchors on the MEDIAN
        // case column (case_1), keeping its middle-case edge straight
        const branchOneCenter = positionOf(result.nodes, 'branch_1').x + 36;

        expect(Math.abs(branchOneCenter - caseOneCenter)).toBeLessThanOrEqual(1);
    });

    it('re-hugs the ring rail after separation moves nested columns', async () => {
        // Repack/re-anchor shift a nested condition's columns WITHOUT the
        // enclosing loop's rail (the rail is not one of that frame's column
        // members), so a hug computed before separation can leave the rail
        // nearly touching the nested box's left edge — the rail must end up
        // hugging the FINAL content positions
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const loopOneCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const truePlaceholderX = positionOf(result.nodes, 'condition_5-condition-left-placeholder-0').x;
        const railX = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x;

        // Content hug = 36px anchor half + 70px RAIL_CONTENT_PADDING off the
        // placeholder center (its x + 36), i.e. its origin x - 70
        expect(railX).toBe(Math.min(loopOneCenter - 100 - 1, truePlaceholderX - 70));
    });

    it('staircases nested ring content columns to the right', async () => {
        // Loop grammar at depth: each ring's content column sits on its own
        // ring's right side, offset from ITS dispatcher's spine — deep stacks
        // staircase rightward exactly like dagre, with no synthetic ring nodes
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        expect(result.nodes.some((resultNode) => resultNode.id.endsWith('-taskDispatcher-right-rail'))).toBe(false);

        // loop_1's body (condition_5, accelo_1) sits on loop_1's ring right side
        const loopOneCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const conditionFiveCenter = positionOf(result.nodes, 'condition_5').x + 36;

        expect(conditionFiveCenter - loopOneCenter).toBe(100);

        // loop_2's body staircases further right off loop_2's own spine
        const loopTwoCenter = positionOf(result.nodes, 'loop_2').x + 36;
        const loopTwoChildCenter = positionOf(result.nodes, 'loopChild1').x + 36;

        expect(loopTwoChildCenter - loopTwoCenter).toBe(100);
    });

    it('compacts sibling columns of unequal depth independently (dagre parity)', async () => {
        // ELK's global layer bands stretch a chain when a deep sibling column
        // shares the scope: frame boxes get parked in balanced middle layers,
        // leaving hundreds of px between a dispatcher and its own box. Every
        // chain must instead stack on the designed footprint rhythm, exactly
        // like dagre's independently compact columns.
        const {edges, nodes} = deepSiblingFixture();

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        // Every frame box hangs TOP_BOX_GAP below its own dispatcher...
        for (const [dispatcherId, barId] of [
            ['branch_1', 'branch_1-branch-top-ghost'],
            ['branch_2', 'branch_2-branch-top-ghost'],
            ['loop_3', 'loop_3-loop-top-ghost'],
            ['loop_1', 'loop_1-loop-top-ghost'],
            ['condition_5', 'condition_5-condition-top-ghost'],
            ['loop_2', 'loop_2-loop-top-ghost'],
        ]) {
            const dispatcherBottom = positionOf(result.nodes, dispatcherId).y + 72;
            const barY = positionOf(result.nodes, barId).y;

            expect(barY - dispatcherBottom).toBe(TOP_BOX_GAP);
        }

        // ...chain steps inside the FALSE stack stay on the designed rhythm...
        const loop3BottomBarY = positionOf(result.nodes, 'loop_3-loop-bottom-ghost').y;

        expect(positionOf(result.nodes, 'loop_1').y - (loop3BottomBarY + 2)).toBe(CHAIN_GAP);

        const loop1TopBarY = positionOf(result.nodes, 'loop_1-loop-top-ghost').y;

        expect(positionOf(result.nodes, 'condition_5').y - (loop1TopBarY + 2)).toBe(BAR_TO_CHILD_GAP);

        // ...and the last child still closes its frame with the standard gap
        const accelo1Bottom = positionOf(result.nodes, 'accelo_1').y + 72;
        const loop1BottomBarY = positionOf(result.nodes, 'loop_1-loop-bottom-ghost').y;

        expect(loop1BottomBarY - accelo1Bottom).toBe(BOX_GAP);
    });

    it('orders branch case columns by the params-derived canonical order, not array order', async () => {
        // Ordering trap per spec: the case_a placeholder is created BEFORE all
        // chain tasks in the flat array, but canonically default < case_a < case_b
        const nodes: Node[] = [
            branchNode('branch_1', ['case_a', 'case_b']),
            ...branchGhostNodes('branch_1'),
            branchCasePlaceholderNode('branch_1', 'case_a'),
            branchChildTaskNode('defaultChild', 'branch_1', 'default'),
            branchChildTaskNode('caseBChild', 'branch_1', 'case_b'),
        ];

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'defaultChild'),
            edge('defaultChild', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'branch_1-branch-case_a-placeholder-0'),
            edge('branch_1-branch-case_a-placeholder-0', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'caseBChild'),
            edge('caseBChild', 'branch_1-branch-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const defaultCenter = positionOf(result.nodes, 'defaultChild').x + 36;
        const caseACenter = positionOf(result.nodes, 'branch_1-branch-case_a-placeholder-0').x + 36;
        const caseBCenter = positionOf(result.nodes, 'caseBChild').x + 36;

        expect(defaultCenter).toBeLessThan(caseACenter);
        expect(caseACenter).toBeLessThan(caseBCenter);

        // Odd case count: the middle column sits exactly on the branch axis.
        // The outer columns are NOT mirror images — the left neighbour packs
        // against defaultChild's one-sided label reservation while the right
        // neighbour packs against the placeholder's symmetric box, so their
        // mean sits up to half the label overhang off the axis
        const branchCenter = positionOf(result.nodes, 'branch_1').x + 36;

        expect(Math.abs(caseACenter - branchCenter)).toBeLessThanOrEqual(1);
        expect(Math.abs((defaultCenter + caseBCenter) / 2 - branchCenter)).toBeLessThanOrEqual(60);

        // Standard box gaps at the branch frame
        const branchBottom = positionOf(result.nodes, 'branch_1').y + 72;
        const topGhostBarY = positionOf(result.nodes, 'branch_1-branch-top-ghost').y;

        expect(topGhostBarY - branchBottom).toBe(TOP_BOX_GAP);
    });

    it('pins the middle case column to the branch axis even with an asymmetric outer case', async () => {
        // dagre parity (alignBranchCaseChildren): the middle case's edges leave
        // the bar's bottom handle and must run straight — a wide outer subtree
        // must not drag the middle column off the axis (mean-centering would)
        const nodes: Node[] = [
            branchNode('branch_1', ['case_a', 'case_b']),
            ...branchGhostNodes('branch_1'),
            {
                data: {
                    branchData: {branchId: 'branch_1', caseKey: 'default', index: 0},
                    componentName: 'condition',
                    taskDispatcher: true,
                    taskDispatcherId: 'condition_9',
                    workflowNodeName: 'condition_9',
                },
                id: 'condition_9',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            ...conditionGhostNodes('condition_9'),
            conditionPlaceholderNode('condition_9', 'left'),
            conditionPlaceholderNode('condition_9', 'right'),
            branchChildTaskNode('caseAChild', 'branch_1', 'case_a'),
            branchChildTaskNode('caseBChild', 'branch_1', 'case_b'),
        ];

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'condition_9'),
            edge('condition_9', 'condition_9-condition-top-ghost'),
            edge('condition_9-condition-top-ghost', 'condition_9-condition-left-placeholder-0'),
            edge('condition_9-condition-top-ghost', 'condition_9-condition-right-placeholder-0'),
            edge('condition_9-condition-left-placeholder-0', 'condition_9-condition-bottom-ghost'),
            edge('condition_9-condition-right-placeholder-0', 'condition_9-condition-bottom-ghost'),
            edge('condition_9-condition-bottom-ghost', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'caseAChild'),
            edge('caseAChild', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'caseBChild'),
            edge('caseBChild', 'branch_1-branch-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        // Middle case (case_a) sits exactly on the branch axis despite the wide
        // default-case condition subtree on the left
        const branchCenter = positionOf(result.nodes, 'branch_1').x + 36;
        const caseACenter = positionOf(result.nodes, 'caseAChild').x + 36;

        expect(Math.abs(caseACenter - branchCenter)).toBeLessThanOrEqual(1);
    });

    it('ranks unknown case keys last', async () => {
        const nodes: Node[] = [
            branchNode('branch_1', ['case_a']),
            ...branchGhostNodes('branch_1'),
            branchChildTaskNode('strayChild', 'branch_1', 'zzz_unknown'),
            branchChildTaskNode('caseAChild', 'branch_1', 'case_a'),
            branchChildTaskNode('defaultChild', 'branch_1', 'default'),
        ];

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'defaultChild'),
            edge('defaultChild', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'caseAChild'),
            edge('caseAChild', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'strayChild'),
            edge('strayChild', 'branch_1-branch-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const defaultCenter = positionOf(result.nodes, 'defaultChild').x;
        const caseACenter = positionOf(result.nodes, 'caseAChild').x;
        const strayCenter = positionOf(result.nodes, 'strayChild').x;

        expect(defaultCenter).toBeLessThan(caseACenter);
        expect(caseACenter).toBeLessThan(strayCenter);
    });

    it('lays out a loop inside a branch case and keeps canonical order', async () => {
        const nodes: Node[] = [
            branchNode('branch_1', ['case_a']),
            ...branchGhostNodes('branch_1'),
            branchChildTaskNode('defaultChild', 'branch_1', 'default'),
            {
                data: {
                    branchData: {branchId: 'branch_1', caseKey: 'case_a', index: 0},
                    componentName: 'loop',
                    taskDispatcher: true,
                    taskDispatcherId: 'loop_1',
                    workflowNodeName: 'loop_1',
                },
                id: 'loop_1',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
        ];

        const edges: Edge[] = [
            edge('branch_1', 'branch_1-branch-top-ghost'),
            edge('branch_1-branch-top-ghost', 'defaultChild'),
            edge('defaultChild', 'branch_1-branch-bottom-ghost'),
            edge('branch_1-branch-top-ghost', 'loop_1'),
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loop_1-loop-bottom-ghost'),
            edge('loop_1-loop-bottom-ghost', 'branch_1-branch-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        // default column left of the case_a loop subtree
        expect(positionOf(result.nodes, 'defaultChild').x).toBeLessThan(positionOf(result.nodes, 'loop_1').x);

        // The nested loop body still sits on its ring's right side
        const loopCenter = positionOf(result.nodes, 'loop_1').x + 36;
        const loopChildCenter = positionOf(result.nodes, 'loopChild1').x + 36;

        expect(loopChildCenter - loopCenter).toBe(100);
    });
});

describe('getElkLayoutElements with parallel and fork-join', () => {
    const parallelNode = (id: string): Node => ({
        data: {componentName: 'parallel', taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    // Mirrors createParallelNode: bottom ghost carries only taskDispatcherId
    const parallelAuxNodes = (parallelId: string, options: {withRail: boolean}): Node[] => [
        {
            data: {parallelId, taskDispatcherId: parallelId},
            id: `${parallelId}-parallel-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        ...(options.withRail
            ? [
                  {
                      data: {parallelId, taskDispatcherId: parallelId},
                      id: `${parallelId}-taskDispatcher-left-ghost`,
                      position: {x: 0, y: 0},
                      type: 'taskDispatcherLeftGhostNode',
                  } as Node,
              ]
            : []),
        {
            data: {label: '+', parallelId, taskDispatcherId: parallelId},
            id: `${parallelId}-parallel-placeholder-0`,
            position: {x: 0, y: 0},
            type: 'placeholder',
        },
        {
            data: {taskDispatcherId: parallelId},
            id: `${parallelId}-parallel-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const parallelChildTaskNode = (id: string, parallelId: string, index: number): Node => ({
        data: {componentName: 'mailchimp', parallelData: {index, parallelId}, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    // Mirrors createForkJoinNode: camelCase 'forkJoin' id segment; placeholders
    // carry a top-level branchIndex
    const forkJoinNode = (id: string): Node => ({
        data: {componentName: 'fork-join', taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const forkJoinAuxNodes = (forkJoinId: string, options: {withRail: boolean}): Node[] => [
        {
            data: {forkJoinId, taskDispatcherId: forkJoinId},
            id: `${forkJoinId}-forkJoin-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        ...(options.withRail
            ? [
                  {
                      data: {forkJoinId, taskDispatcherId: forkJoinId},
                      id: `${forkJoinId}-taskDispatcher-left-ghost`,
                      position: {x: 0, y: 0},
                      type: 'taskDispatcherLeftGhostNode',
                  } as Node,
              ]
            : []),
        {
            data: {taskDispatcherId: forkJoinId},
            id: `${forkJoinId}-forkJoin-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const forkJoinPlaceholderNode = (forkJoinId: string, branchIndex: number): Node => ({
        data: {branchIndex, forkJoinId, label: '+', taskDispatcherId: forkJoinId},
        id: `${forkJoinId}-forkJoin-placeholder-${branchIndex}`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    });

    const forkJoinChildTaskNode = (id: string, forkJoinId: string, branchIndex: number, index: number): Node => ({
        data: {componentName: 'mailchimp', forkJoinData: {branchIndex, forkJoinId, index}, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    it('orders parallel columns by index with the trailing placeholder last', async () => {
        const nodes: Node[] = [
            parallelNode('parallel_1'),
            ...parallelAuxNodes('parallel_1', {withRail: false}),
            parallelChildTaskNode('p1', 'parallel_1', 0),
            parallelChildTaskNode('p2', 'parallel_1', 1),
            parallelChildTaskNode('p3', 'parallel_1', 2),
        ];

        const edges: Edge[] = [
            edge('parallel_1', 'parallel_1-parallel-top-ghost'),
            edge('parallel_1-parallel-top-ghost', 'p1'),
            edge('p1', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-top-ghost', 'p2'),
            edge('p2', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-top-ghost', 'p3'),
            edge('p3', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-top-ghost', 'parallel_1-parallel-placeholder-0'),
            edge('parallel_1-parallel-placeholder-0', 'parallel_1-parallel-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        const p1Center = positionOf(result.nodes, 'p1').x + 36;
        const p2Center = positionOf(result.nodes, 'p2').x + 36;
        const p3Center = positionOf(result.nodes, 'p3').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'parallel_1-parallel-placeholder-0').x + 36;

        expect(p1Center).toBeLessThan(p2Center);
        expect(p2Center).toBeLessThan(p3Center);
        expect(p3Center).toBeLessThan(placeholderCenter);

        // Even column count (3 tasks + placeholder): dispatcher on the mean
        const parallelCenter = positionOf(result.nodes, 'parallel_1').x + 36;
        const columnMean = (p1Center + p2Center + p3Center + placeholderCenter) / 4;

        expect(Math.abs(columnMean - parallelCenter)).toBeLessThanOrEqual(1);

        const parallelBottom = positionOf(result.nodes, 'parallel_1').y + 72;
        const topGhostBarY = positionOf(result.nodes, 'parallel_1-parallel-top-ghost').y;

        expect(topGhostBarY - parallelBottom).toBe(TOP_BOX_GAP);
    });

    it('renders an empty parallel as a square ring', async () => {
        const nodes: Node[] = [parallelNode('parallel_1'), ...parallelAuxNodes('parallel_1', {withRail: true})];

        const edges: Edge[] = [
            edge('parallel_1', 'parallel_1-parallel-top-ghost'),
            edge('parallel_1-parallel-top-ghost', 'parallel_1-taskDispatcher-left-ghost'),
            edge('parallel_1-taskDispatcher-left-ghost', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-top-ghost', 'parallel_1-parallel-placeholder-0'),
            edge('parallel_1-parallel-placeholder-0', 'parallel_1-parallel-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const topGhostBarY = positionOf(result.nodes, 'parallel_1-parallel-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'parallel_1-parallel-bottom-ghost').y;
        const ringHalfWidth = (bottomGhostBarY - topGhostBarY + 2) / 2;

        const parallelCenter = positionOf(result.nodes, 'parallel_1').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'parallel_1-parallel-placeholder-0').x + 36;
        const railCenter = positionOf(result.nodes, 'parallel_1-taskDispatcher-left-ghost').x + 1;

        expect(placeholderCenter - parallelCenter).toBe(ringHalfWidth);
        expect(parallelCenter - railCenter).toBe(ringHalfWidth);
    });

    it('orders fork-join branch columns by branchIndex with chains inside', async () => {
        const nodes: Node[] = [
            forkJoinNode('forkJoin_1'),
            ...forkJoinAuxNodes('forkJoin_1', {withRail: false}),
            forkJoinPlaceholderNode('forkJoin_1', 2),
            forkJoinChildTaskNode('f1', 'forkJoin_1', 0, 0),
            forkJoinChildTaskNode('f2', 'forkJoin_1', 0, 1),
            forkJoinChildTaskNode('g1', 'forkJoin_1', 1, 0),
        ];

        const edges: Edge[] = [
            edge('forkJoin_1', 'forkJoin_1-forkJoin-top-ghost'),
            edge('forkJoin_1-forkJoin-top-ghost', 'f1'),
            edge('f1', 'f2'),
            edge('f2', 'forkJoin_1-forkJoin-bottom-ghost'),
            edge('forkJoin_1-forkJoin-top-ghost', 'g1'),
            edge('g1', 'forkJoin_1-forkJoin-bottom-ghost'),
            edge('forkJoin_1-forkJoin-top-ghost', 'forkJoin_1-forkJoin-placeholder-2'),
            edge('forkJoin_1-forkJoin-placeholder-2', 'forkJoin_1-forkJoin-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        const branch0Center = positionOf(result.nodes, 'f1').x + 36;
        const branch1Center = positionOf(result.nodes, 'g1').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'forkJoin_1-forkJoin-placeholder-2').x + 36;

        expect(branch0Center).toBeLessThan(branch1Center);
        expect(branch1Center).toBeLessThan(placeholderCenter);

        // Odd column count (2 branches + placeholder): median column on the axis
        const forkJoinCenter = positionOf(result.nodes, 'forkJoin_1').x + 36;

        expect(Math.abs(branch1Center - forkJoinCenter)).toBeLessThanOrEqual(1);

        // Chain rhythm inside branch 0
        expect(positionOf(result.nodes, 'f2').y - positionOf(result.nodes, 'f1').y).toBe(CHAIN_STEP);
    });

    it('renders an empty fork-join as a square ring via camelCase ghost ids', async () => {
        const nodes: Node[] = [
            forkJoinNode('forkJoin_1'),
            ...forkJoinAuxNodes('forkJoin_1', {withRail: true}),
            forkJoinPlaceholderNode('forkJoin_1', 0),
        ];

        const edges: Edge[] = [
            edge('forkJoin_1', 'forkJoin_1-forkJoin-top-ghost'),
            edge('forkJoin_1-forkJoin-top-ghost', 'forkJoin_1-taskDispatcher-left-ghost'),
            edge('forkJoin_1-taskDispatcher-left-ghost', 'forkJoin_1-forkJoin-bottom-ghost'),
            edge('forkJoin_1-forkJoin-top-ghost', 'forkJoin_1-forkJoin-placeholder-0'),
            edge('forkJoin_1-forkJoin-placeholder-0', 'forkJoin_1-forkJoin-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const topGhostBarY = positionOf(result.nodes, 'forkJoin_1-forkJoin-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'forkJoin_1-forkJoin-bottom-ghost').y;
        const ringHalfWidth = (bottomGhostBarY - topGhostBarY + 2) / 2;

        const forkJoinCenter = positionOf(result.nodes, 'forkJoin_1').x + 36;
        const placeholderCenter = positionOf(result.nodes, 'forkJoin_1-forkJoin-placeholder-0').x + 36;
        const railCenter = positionOf(result.nodes, 'forkJoin_1-taskDispatcher-left-ghost').x + 1;

        // The 38px pulled top gap proves the camelCase ghost ids resolved
        const forkJoinBottom = positionOf(result.nodes, 'forkJoin_1').y + 72;

        expect(topGhostBarY - forkJoinBottom).toBe(TOP_BOX_GAP);
        expect(placeholderCenter - forkJoinCenter).toBe(ringHalfWidth);
        expect(forkJoinCenter - railCenter).toBe(ringHalfWidth);
    });

    it('lays out a parallel inside a condition branch', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            {
                data: {
                    componentName: 'parallel',
                    conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
                    taskDispatcher: true,
                    taskDispatcherId: 'parallel_1',
                    workflowNodeName: 'parallel_1',
                },
                id: 'parallel_1',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            ...parallelAuxNodes('parallel_1', {withRail: false}),
            parallelChildTaskNode('p1', 'parallel_1', 0),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'parallel_1'),
            edge('parallel_1', 'parallel_1-parallel-top-ghost'),
            edge('parallel_1-parallel-top-ghost', 'p1'),
            edge('p1', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-top-ghost', 'parallel_1-parallel-placeholder-0'),
            edge('parallel_1-parallel-placeholder-0', 'parallel_1-parallel-bottom-ghost'),
            edge('parallel_1-parallel-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        // Parallel (caseTrue) stays left of the FALSE branch child
        expect(positionOf(result.nodes, 'parallel_1').x).toBeLessThan(positionOf(result.nodes, 'childFalse1').x);
    });
});

describe('getElkLayoutElements with graph', () => {
    const graphNode = (id: string): Node => ({
        data: {componentName: 'graph', taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    // What a graph contributes to the OUTER arrays once the layout pre-pass has run: the ghost
    // bars the surrounding chain addresses the container through, and the frame itself as a
    // single leaf node pre-sized from its members. The members, the Start pill and the add-node
    // placeholder are frame children with frame-relative positions, so they never reach the
    // engine at all — see layoutGraphFrames.
    const graphContainerNodes = (graphId: string, size: {height: number; width: number}): Node[] => [
        {
            data: {graphId, taskDispatcherId: graphId},
            id: `${graphId}-graph-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        {
            data: {
                graphFrame: {graphId, height: size.height, width: size.width},
                graphId,
                taskDispatcherId: graphId,
            },
            height: size.height,
            id: `${graphId}-graph-frame`,
            position: {x: 0, y: 0},
            type: 'graphFrame',
            width: size.width,
        },
        {
            data: {taskDispatcherId: graphId},
            id: `${graphId}-graph-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const graphContainerEdges = (graphId: string): Edge[] => [
        edge(graphId, `${graphId}-graph-top-ghost`),
        edge(`${graphId}-graph-top-ghost`, `${graphId}-graph-frame`),
        edge(`${graphId}-graph-frame`, `${graphId}-graph-bottom-ghost`),
    ];

    it('lays the frame out at its own size and centers it under the dispatcher', async () => {
        const nodes: Node[] = [graphNode('graph_1'), ...graphContainerNodes('graph_1', {height: 240, width: 400})];

        const result = await getElkLayoutElements({
            canvasWidth: 2000,
            direction: 'TB',
            edges: graphContainerEdges('graph_1'),
            nodes,
        });

        const framePosition = positionOf(result.nodes, 'graph_1-graph-frame');
        const graphPosition = positionOf(result.nodes, 'graph_1');

        // The frame paints exactly `data.graphFrame`, so its rendered box — not a 72px anchor —
        // is what centers on the chain axis.
        expect(Math.abs(framePosition.x + 400 / 2 - (graphPosition.x + 36))).toBeLessThanOrEqual(1);

        const topGhostBarY = positionOf(result.nodes, 'graph_1-graph-top-ghost').y;
        const bottomGhostBarY = positionOf(result.nodes, 'graph_1-graph-bottom-ghost').y;

        expect(framePosition.y).toBeGreaterThan(topGhostBarY);
        expect(bottomGhostBarY).toBeGreaterThanOrEqual(framePosition.y + 240);

        expect(topGhostBarY - (graphPosition.y + 72)).toBe(TOP_BOX_GAP);
    });

    it('grows the container chain when the frame grows', async () => {
        const layoutWithFrameHeight = async (height: number) =>
            getElkLayoutElements({
                canvasWidth: 2000,
                direction: 'TB',
                edges: graphContainerEdges('graph_1'),
                nodes: [graphNode('graph_1'), ...graphContainerNodes('graph_1', {height, width: 400})],
            });

        const shortResult = await layoutWithFrameHeight(240);
        const tallResult = await layoutWithFrameHeight(640);

        const shortSpan =
            positionOf(shortResult.nodes, 'graph_1-graph-bottom-ghost').y -
            positionOf(shortResult.nodes, 'graph_1-graph-top-ghost').y;
        const tallSpan =
            positionOf(tallResult.nodes, 'graph_1-graph-bottom-ghost').y -
            positionOf(tallResult.nodes, 'graph_1-graph-top-ghost').y;

        expect(tallSpan - shortSpan).toBe(400);
    });

    it('moves the whole graph frame with a dispatcher that has a saved position', async () => {
        const nodes: Node[] = [graphNode('graph_1'), ...graphContainerNodes('graph_1', {height: 240, width: 400})];

        (nodes[0].data as Record<string, unknown>).metadata = {ui: {nodePosition: {x: 400, y: 900}}};

        const result = await getElkLayoutElements({
            canvasWidth: 1000,
            direction: 'TB',
            edges: graphContainerEdges('graph_1'),
            nodes,
        });

        // The dispatcher honors its saved position...
        expect(positionOf(result.nodes, 'graph_1')).toEqual({x: 400, y: 900});

        // ...and its ghosts AND its frame shift rigidly with it, so the box never detaches from
        // the node that owns it (the frame's own children are positioned relative to the frame,
        // so they follow for free).
        const topGhostBarY = positionOf(result.nodes, 'graph_1-graph-top-ghost').y;

        expect(topGhostBarY - (900 + 72)).toBe(TOP_BOX_GAP);
        expect(positionOf(result.nodes, 'graph_1-graph-frame').y).toBeGreaterThan(topGhostBarY);
        expect(Math.abs(positionOf(result.nodes, 'graph_1-graph-frame').x + 200 - (400 + 36))).toBeLessThanOrEqual(1);
    });

    it('lays out a graph inside a condition branch', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            {
                data: {
                    componentName: 'graph',
                    conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
                    taskDispatcher: true,
                    taskDispatcherId: 'graph_1',
                    workflowNodeName: 'graph_1',
                },
                id: 'graph_1',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            ...graphContainerNodes('graph_1', {height: 240, width: 400}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'graph_1'),
            ...graphContainerEdges('graph_1'),
            edge('graph_1-graph-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 2000, direction: 'TB', edges, nodes});

        // Graph (caseTrue) stays left of the FALSE branch child
        expect(positionOf(result.nodes, 'graph_1').x).toBeLessThan(positionOf(result.nodes, 'childFalse1').x);
    });

    // `layoutGraphFrames` strips a frame's own routes before the outer layout runs, so these
    // never reach ELK from `useLayout`. They still must not corrupt it if a caller hands them
    // over directly: a back-and-forth pair of transitions is a genuine 2-node CYCLE, which would
    // wreck the layered ranking of every other node, and either chain walker could follow a
    // transition instead of the real structural continuation.
    describe('graphTransition edge isolation from ELK ranking', () => {
        const structuralNodes: Node[] = [taskNode('n0'), taskNode('n1')];

        const structuralEdges: Edge[] = [edge('n0', 'n1')];

        // A mutual back-and-forth: n0 -> n1 and n1 -> n0. Listed BEFORE the structural edges so
        // a walker that failed to skip them would land on one first.
        const cyclicTransitionEdges: Edge[] = [
            {
                id: 'graph_1-transition-0',
                source: 'n0',
                sourceHandle: 'n0-graph-transition-source',
                target: 'n1',
                targetHandle: 'n1-graph-transition-target',
                type: GRAPH_TRANSITION_EDGE_TYPE,
            },
            {
                id: 'graph_1-transition-1',
                source: 'n1',
                sourceHandle: 'n1-graph-transition-source',
                target: 'n0',
                targetHandle: 'n0-graph-transition-target',
                type: GRAPH_TRANSITION_EDGE_TYPE,
            },
        ];

        it('excludes graphTransition edges from the ELK graph entirely', () => {
            const graph = buildElkGraph(structuralNodes, [...cyclicTransitionEdges, ...structuralEdges], 'TB');

            const allScopeEdgeIds = new Set<string>();

            const collectEdgeIds = (elkNode: ElkNode) => {
                (elkNode.edges ?? []).forEach((scopeEdge) => allScopeEdgeIds.add(scopeEdge.id));
                (elkNode.children ?? []).forEach(collectEdgeIds);
            };

            collectEdgeIds(graph);

            expect([...allScopeEdgeIds]).toEqual(['elk-edge-n0=>n1']);
            expect(collectScopeEdgeViolations(graph)).toEqual([]);
        });

        // The cyclic pair above happens to point at the SAME target as the structural edge, so a
        // walker that dropped its `type !== GRAPH_TRANSITION_EDGE_TYPE` guard would land on the
        // right node anyway and stay green. A SELF-transition does not: it names its own source as
        // the target, so following it stalls the walk. That is the reachable case, not a contrived
        // one — `layOutMemberGroups` filters a group's edges to both-endpoints-in-group, and a
        // self-transition is the one transition shape that survives that filter into the engine.
        it('keeps chain walking on the structural edge when a self-transition sorts before it', async () => {
            const nodes: Node[] = [
                conditionNode('condition_1'),
                ...conditionGhostNodes('condition_1'),
                taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
                taskNode('childTrue2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
                taskNode('childTrue3', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
                taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            ];

            const structuralBranchEdges: Edge[] = [
                edge('condition_1', 'condition_1-condition-top-ghost'),
                edge('condition_1-condition-top-ghost', 'childTrue1'),
                edge('childTrue1', 'childTrue2'),
                edge('childTrue2', 'childTrue3'),
                edge('childTrue3', 'condition_1-condition-bottom-ghost'),
                edge('condition_1-condition-top-ghost', 'childFalse1'),
                edge('childFalse1', 'condition_1-condition-bottom-ghost'),
            ];

            // Sorted BEFORE every structural edge, so `.find()` reaches it first.
            const selfTransitionEdges: Edge[] = [
                {
                    id: 'graph_1-transition-self-false',
                    source: 'childFalse1',
                    sourceHandle: 'childFalse1-graph-transition-source',
                    target: 'childFalse1',
                    targetHandle: 'childFalse1-graph-transition-dynamic',
                    type: GRAPH_TRANSITION_EDGE_TYPE,
                },
                {
                    id: 'graph_1-transition-self-true',
                    source: 'childTrue1',
                    sourceHandle: 'childTrue1-graph-transition-source',
                    target: 'childTrue1',
                    targetHandle: 'childTrue1-graph-transition-dynamic',
                    type: GRAPH_TRANSITION_EDGE_TYPE,
                },
            ];

            const baseline = await getElkLayoutElements({
                canvasWidth: 1200,
                direction: 'TB',
                edges: structuralBranchEdges,
                nodes,
            });

            const withSelfTransitions = await getElkLayoutElements({
                canvasWidth: 1200,
                direction: 'TB',
                edges: [...selfTransitionEdges, ...structuralBranchEdges],
                nodes,
            });

            // collectChainMainNodes (frame centering) and the main-axis restack's own
            // findContinuationEdge both walk from these nodes; either one following the self
            // edge instead of the structural successor moves the geometry.
            for (const id of [
                'condition_1',
                'condition_1-condition-top-ghost',
                'condition_1-condition-bottom-ghost',
                'childTrue1',
                'childTrue2',
                'childTrue3',
                'childFalse1',
            ]) {
                expect(positionOf(withSelfTransitions.nodes, id)).toEqual(positionOf(baseline.nodes, id));
            }

            // And the short branch is still centered in the interior — the centering pass the
            // walkers feed actually ran, rather than both runs being equally broken.
            const topGhostBarY = positionOf(withSelfTransitions.nodes, 'condition_1-condition-top-ghost').y;
            const bottomGhostBarY = positionOf(withSelfTransitions.nodes, 'condition_1-condition-bottom-ghost').y;

            const interiorCenter = (topGhostBarY + 2 + bottomGhostBarY) / 2;
            const falseChildCenter = positionOf(withSelfTransitions.nodes, 'childFalse1').y + 36;

            expect(Math.abs(falseChildCenter - interiorCenter)).toBeLessThanOrEqual(1);
        });

        it('lays out identically with a cyclic pair of graphTransition edges, and does not throw', async () => {
            const withoutTransitions = await getElkLayoutElements({
                canvasWidth: 2000,
                direction: 'TB',
                edges: structuralEdges,
                nodes: structuralNodes,
            });

            // Would hang/throw here if the back-edges leaked into ELK's layered ranking
            // algorithm as real structural edges.
            const withTransitions = await getElkLayoutElements({
                canvasWidth: 2000,
                direction: 'TB',
                edges: [...cyclicTransitionEdges, ...structuralEdges],
                nodes: structuralNodes,
            });

            for (const id of ['n0', 'n1']) {
                expect(positionOf(withTransitions.nodes, id)).toEqual(positionOf(withoutTransitions.nodes, id));
            }

            // The routes still come back out for rendering — ELK is isolated from them, the
            // canvas is not.
            const returnedTransitionIds = withTransitions.edges
                .filter((returnedEdge) => returnedEdge.type === GRAPH_TRANSITION_EDGE_TYPE)
                .map((returnedEdge) => returnedEdge.id)
                .sort();

            expect(returnedTransitionIds).toEqual(['graph_1-transition-0', 'graph_1-transition-1']);
        });
    });
});

describe('getElkLayoutElements with each and map', () => {
    // Mirrors createEachNode/createMapNode: plain 'each'/'map' ghost segments,
    // the generic '-taskDispatcher-left-ghost' rail id, and the loop quirk of
    // a bottom ghost carrying only taskDispatcherId (no eachId/mapId)
    const ringDispatcherNode = (id: string, componentName: string, extraData: Record<string, unknown> = {}): Node => ({
        data: {componentName, taskDispatcher: true, taskDispatcherId: id, workflowNodeName: id, ...extraData},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const ringAuxNodes = (dispatcherId: string, segment: string, ownerKey: string): Node[] => [
        {
            data: {[ownerKey]: dispatcherId, taskDispatcherId: dispatcherId},
            id: `${dispatcherId}-${segment}-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        {
            data: {[ownerKey]: dispatcherId, taskDispatcherId: dispatcherId},
            id: `${dispatcherId}-taskDispatcher-left-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherLeftGhostNode',
        },
        {
            data: {isNestedBottomGhost: false, taskDispatcherId: dispatcherId},
            id: `${dispatcherId}-${segment}-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const ringChildNode = (id: string, extraData: Record<string, unknown>): Node => ({
        data: {componentName: 'mailchimp', workflowNodeName: id, ...extraData},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const ringStructureEdges = (dispatcherId: string, segment: string): Edge[] => [
        edge(dispatcherId, `${dispatcherId}-${segment}-top-ghost`),
        edge(`${dispatcherId}-${segment}-top-ghost`, `${dispatcherId}-taskDispatcher-left-ghost`),
        edge(`${dispatcherId}-taskDispatcher-left-ghost`, `${dispatcherId}-${segment}-bottom-ghost`),
    ];

    it('lays out a populated map chain with the loop ring grammar', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            ringDispatcherNode('map_1', 'map'),
            ...ringAuxNodes('map_1', 'map', 'mapId'),
            ringChildNode('mapChild1', {mapData: {index: 0, mapId: 'map_1'}}),
            ringChildNode('mapChild2', {mapData: {index: 1, mapId: 'map_1'}}),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'map_1'),
            ...ringStructureEdges('map_1', 'map'),
            edge('map_1-map-top-ghost', 'mapChild1'),
            edge('mapChild1', 'mapChild2'),
            edge('mapChild2', 'map_1-map-bottom-ghost'),
            edge('map_1-map-bottom-ghost', 'task2'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // Ring grammar: body on the right edge, rail mirrored left
        const mapCenter = positionOf(result.nodes, 'map_1').x + 36;

        expect(positionOf(result.nodes, 'mapChild1').x + 36 - mapCenter).toBe(100);
        expect(mapCenter - (positionOf(result.nodes, 'map_1-taskDispatcher-left-ghost').x + 1)).toBe(100);

        // The uniform main-axis rhythm at every step
        const mapBottom = positionOf(result.nodes, 'map_1').y + 72;
        const topBarY = positionOf(result.nodes, 'map_1-map-top-ghost').y;
        const bottomBarY = positionOf(result.nodes, 'map_1-map-bottom-ghost').y;

        expect(topBarY - mapBottom).toBe(TOP_BOX_GAP);
        expect(positionOf(result.nodes, 'mapChild1').y - (topBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(positionOf(result.nodes, 'mapChild2').y - positionOf(result.nodes, 'mapChild1').y).toBe(CHAIN_STEP);
        expect(bottomBarY - (positionOf(result.nodes, 'mapChild2').y + 72)).toBe(BOX_GAP);
        expect(positionOf(result.nodes, 'task2').y - (bottomBarY + 2)).toBe(CHAIN_GAP);
    });

    it('lays out a single each iteratee on the ring right side', async () => {
        const nodes: Node[] = [
            ringDispatcherNode('each_1', 'each'),
            ...ringAuxNodes('each_1', 'each', 'eachId'),
            ringChildNode('iterateeChild', {eachData: {eachId: 'each_1', index: 0}}),
        ];

        const edges: Edge[] = [
            ...ringStructureEdges('each_1', 'each'),
            edge('each_1-each-top-ghost', 'iterateeChild'),
            edge('iterateeChild', 'each_1-each-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const eachCenter = positionOf(result.nodes, 'each_1').x + 36;

        expect(positionOf(result.nodes, 'iterateeChild').x + 36 - eachCenter).toBe(100);
        expect(eachCenter - (positionOf(result.nodes, 'each_1-taskDispatcher-left-ghost').x + 1)).toBe(100);

        const topBarY = positionOf(result.nodes, 'each_1-each-top-ghost').y;

        expect(topBarY - (positionOf(result.nodes, 'each_1').y + 72)).toBe(TOP_BOX_GAP);
        expect(positionOf(result.nodes, 'iterateeChild').y - (topBarY + 2)).toBe(BAR_TO_CHILD_GAP);
    });

    it('renders an empty each as the square ring', async () => {
        const nodes: Node[] = [
            ringDispatcherNode('each_1', 'each'),
            ...ringAuxNodes('each_1', 'each', 'eachId'),
            {
                data: {eachId: 'each_1', label: '+', taskDispatcherId: 'each_1'},
                id: 'each_1-each-placeholder-0',
                position: {x: 0, y: 0},
                type: 'placeholder',
            },
        ];

        const edges: Edge[] = [
            ...ringStructureEdges('each_1', 'each'),
            edge('each_1-each-top-ghost', 'each_1-each-placeholder-0'),
            edge('each_1-each-placeholder-0', 'each_1-each-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        const topBarY = positionOf(result.nodes, 'each_1-each-top-ghost').y;
        const bottomBarY = positionOf(result.nodes, 'each_1-each-bottom-ghost').y;
        const ringHalfWidth = (bottomBarY - topBarY + 2) / 2;

        const eachCenter = positionOf(result.nodes, 'each_1').x + 36;

        expect(positionOf(result.nodes, 'each_1-each-placeholder-0').x + 36 - eachCenter).toBe(ringHalfWidth);
        expect(eachCenter - (positionOf(result.nodes, 'each_1-taskDispatcher-left-ghost').x + 1)).toBe(ringHalfWidth);
    });

    it('keeps a map on its branch side inside a condition', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            ringDispatcherNode('map_1', 'map', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            ...ringAuxNodes('map_1', 'map', 'mapId'),
            ringChildNode('mapChild1', {mapData: {index: 0, mapId: 'map_1'}}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'map_1'),
            ...ringStructureEdges('map_1', 'map'),
            edge('map_1-map-top-ghost', 'mapChild1'),
            edge('mapChild1', 'map_1-map-bottom-ghost'),
            edge('map_1-map-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // Map (caseTrue) stays left of the FALSE branch child; body offset
        expect(positionOf(result.nodes, 'map_1').x).toBeLessThan(positionOf(result.nodes, 'childFalse1').x);
        expect(positionOf(result.nodes, 'mapChild1').x - positionOf(result.nodes, 'map_1').x).toBe(100);
    });

    it('walks past the dangling fork-join edge from createEachEdges', async () => {
        // createEachEdges emits a bogus `fork-join_1-fork-join-bottom-ghost`
        // continuation (real ids use the camelCase 'forkJoin' segment); the
        // engine must drop the dangling edge and lay out via the correct one
        const forkJoinAux: Node[] = [
            {
                data: {forkJoinId: 'fork-join_1', taskDispatcherId: 'fork-join_1'},
                id: 'fork-join_1-forkJoin-top-ghost',
                position: {x: 0, y: 0},
                type: 'taskDispatcherTopGhostNode',
            },
            {
                data: {taskDispatcherId: 'fork-join_1'},
                id: 'fork-join_1-forkJoin-bottom-ghost',
                position: {x: 0, y: 0},
                type: 'taskDispatcherBottomGhostNode',
            },
        ];

        const nodes: Node[] = [
            ringDispatcherNode('each_1', 'each'),
            ...ringAuxNodes('each_1', 'each', 'eachId'),
            ringDispatcherNode('fork-join_1', 'fork-join', {eachData: {eachId: 'each_1', index: 0}}),
            ...forkJoinAux,
            ringChildNode('branchChild', {forkJoinData: {branchIndex: 0, forkJoinId: 'fork-join_1', index: 0}}),
        ];

        const edges: Edge[] = [
            ...ringStructureEdges('each_1', 'each'),
            edge('each_1-each-top-ghost', 'fork-join_1'),
            edge('fork-join_1', 'fork-join_1-forkJoin-top-ghost'),
            edge('fork-join_1-forkJoin-top-ghost', 'branchChild'),
            edge('branchChild', 'fork-join_1-forkJoin-bottom-ghost'),
            // The dangling inline edge with the WRONG segment
            edge('fork-join_1-fork-join-bottom-ghost', 'each_1-each-bottom-ghost'),
            // The correct generic continuation
            edge('fork-join_1-forkJoin-bottom-ghost', 'each_1-each-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // The nested fork-join sits on each's ring right side, and its bottom
        // bar merges into each's bottom bar with the standard stub
        expect(positionOf(result.nodes, 'fork-join_1').x - positionOf(result.nodes, 'each_1').x).toBe(100);

        const forkJoinBottomY = positionOf(result.nodes, 'fork-join_1-forkJoin-bottom-ghost').y;
        const eachBottomY = positionOf(result.nodes, 'each_1-each-bottom-ghost').y;

        expect(eachBottomY - (forkJoinBottomY + 2)).toBe(BOX_GAP);

        // The dangling edge is gone from the output
        expect(result.edges.some((resultEdge) => resultEdge.source === 'fork-join_1-fork-join-bottom-ghost')).toBe(
            false
        );
    });
});

describe('getElkLayoutElements with on-error', () => {
    // Mirrors createOnErrorNode: camelCase 'onError' aux segments, side
    // placeholders carrying top-level onErrorCase, bottom ghost WITH onErrorId
    const onErrorNode = (id: string, extraData: Record<string, unknown> = {}): Node => ({
        data: {
            componentName: 'on-error',
            taskDispatcher: true,
            taskDispatcherId: id,
            workflowNodeName: id,
            ...extraData,
        },
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const onErrorGhostNodes = (onErrorId: string): Node[] => [
        {
            data: {onErrorId, taskDispatcherId: onErrorId},
            id: `${onErrorId}-onError-top-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        {
            data: {isNestedBottomGhost: false, onErrorId, taskDispatcherId: onErrorId},
            id: `${onErrorId}-onError-bottom-ghost`,
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
    ];

    const onErrorPlaceholderNode = (onErrorId: string, side: 'left' | 'right'): Node => ({
        data: {
            label: '+',
            onErrorCase: side === 'left' ? 'mainBranch' : 'onErrorBranch',
            onErrorId,
            taskDispatcherId: onErrorId,
        },
        id: `${onErrorId}-onError-${side}-placeholder-0`,
        position: {x: 0, y: 0},
        type: 'placeholder',
    });

    const onErrorChildNode = (id: string, onErrorId: string, onErrorCase: 'mainBranch' | 'onErrorBranch'): Node => ({
        data: {
            componentName: 'mailchimp',
            onErrorData: {index: 0, onErrorCase, onErrorId},
            workflowNodeName: id,
        },
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    it('keeps the TRY branch left of CATCH with uniform gaps', async () => {
        const nodes: Node[] = [
            taskNode('task1'),
            onErrorNode('on-error_1'),
            ...onErrorGhostNodes('on-error_1'),
            onErrorChildNode('tryChild', 'on-error_1', 'mainBranch'),
            onErrorChildNode('catchChild', 'on-error_1', 'onErrorBranch'),
            taskNode('task2'),
        ];

        const edges: Edge[] = [
            edge('task1', 'on-error_1'),
            edge('on-error_1', 'on-error_1-onError-top-ghost'),
            edge('on-error_1-onError-top-ghost', 'tryChild'),
            edge('tryChild', 'on-error_1-onError-bottom-ghost'),
            edge('on-error_1-onError-top-ghost', 'catchChild'),
            edge('catchChild', 'on-error_1-onError-bottom-ghost'),
            edge('on-error_1-onError-bottom-ghost', 'task2'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // mainBranch (TRY) left of onErrorBranch (CATCH)
        expect(positionOf(result.nodes, 'tryChild').x).toBeLessThan(positionOf(result.nodes, 'catchChild').x);

        // Parent centered on the two-entry mean
        const parentCenter = positionOf(result.nodes, 'on-error_1').x + 36;
        const entryMean =
            (positionOf(result.nodes, 'tryChild').x + 36 + positionOf(result.nodes, 'catchChild').x + 36) / 2;

        expect(Math.abs(parentCenter - entryMean)).toBeLessThanOrEqual(1);

        // Uniform rhythm: pulled box gap, entry gap, exit gaps
        const parentBottom = positionOf(result.nodes, 'on-error_1').y + 72;
        const topBarY = positionOf(result.nodes, 'on-error_1-onError-top-ghost').y;
        const bottomBarY = positionOf(result.nodes, 'on-error_1-onError-bottom-ghost').y;

        expect(topBarY - parentBottom).toBe(TOP_BOX_GAP);
        expect(positionOf(result.nodes, 'tryChild').y - (topBarY + 2)).toBe(BAR_TO_CHILD_GAP);
        expect(bottomBarY - (positionOf(result.nodes, 'tryChild').y + 72)).toBe(BOX_GAP);
        expect(positionOf(result.nodes, 'task2').y - (bottomBarY + 2)).toBe(CHAIN_GAP);
    });

    it('centers an empty CATCH side placeholder mid-frame on its own side', async () => {
        const nodes: Node[] = [
            onErrorNode('on-error_1'),
            ...onErrorGhostNodes('on-error_1'),
            onErrorChildNode('tryChild', 'on-error_1', 'mainBranch'),
            onErrorPlaceholderNode('on-error_1', 'right'),
        ];

        const edges: Edge[] = [
            edge('on-error_1', 'on-error_1-onError-top-ghost'),
            edge('on-error_1-onError-top-ghost', 'tryChild'),
            edge('tryChild', 'on-error_1-onError-bottom-ghost'),
            edge('on-error_1-onError-top-ghost', 'on-error_1-onError-right-placeholder-0'),
            edge('on-error_1-onError-right-placeholder-0', 'on-error_1-onError-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // Placeholder right of the TRY chain, mid-frame on the main axis
        expect(positionOf(result.nodes, 'on-error_1-onError-right-placeholder-0').x).toBeGreaterThan(
            positionOf(result.nodes, 'tryChild').x
        );

        const topBarY = positionOf(result.nodes, 'on-error_1-onError-top-ghost').y;
        const bottomBarY = positionOf(result.nodes, 'on-error_1-onError-bottom-ghost').y;
        const placeholderMainCenter = positionOf(result.nodes, 'on-error_1-onError-right-placeholder-0').y + 14;

        expect(Math.abs(placeholderMainCenter - (topBarY + bottomBarY + 2) / 2)).toBeLessThanOrEqual(1);
    });

    it('lays out an on-error inside a loop body on the ring right side', async () => {
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            onErrorNode('on-error_1', {loopData: {index: 0, loopId: 'loop_1'}}),
            ...onErrorGhostNodes('on-error_1'),
            onErrorPlaceholderNode('on-error_1', 'left'),
            onErrorPlaceholderNode('on-error_1', 'right'),
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'on-error_1'),
            edge('on-error_1', 'on-error_1-onError-top-ghost'),
            edge('on-error_1-onError-top-ghost', 'on-error_1-onError-left-placeholder-0'),
            edge('on-error_1-onError-left-placeholder-0', 'on-error_1-onError-bottom-ghost'),
            edge('on-error_1-onError-top-ghost', 'on-error_1-onError-right-placeholder-0'),
            edge('on-error_1-onError-right-placeholder-0', 'on-error_1-onError-bottom-ghost'),
            edge('on-error_1-onError-bottom-ghost', 'loop_1-loop-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // The on-error node sits on the loop ring's right side and keeps its
        // own pulled box gap
        expect(positionOf(result.nodes, 'on-error_1').x - positionOf(result.nodes, 'loop_1').x).toBe(100);

        const onErrorBottom = positionOf(result.nodes, 'on-error_1').y + 72;
        const onErrorTopBarY = positionOf(result.nodes, 'on-error_1-onError-top-ghost').y;

        expect(onErrorTopBarY - onErrorBottom).toBe(TOP_BOX_GAP);
    });
});

describe('getElkLayoutElements with cluster roots', () => {
    // A cluster root is ONE plain chain node on the main canvas (elements
    // render inside its DOM); configured roots have a ~240px button with the
    // chain handles at its center (left 120px)
    const clusterRootNode = (id: string, configured: boolean, extraData: Record<string, unknown> = {}): Node => ({
        data: {
            clusterElements: configured ? {chatMemory: {name: 'memory_1', type: 'x/v1/y'}} : {},
            clusterRoot: true,
            componentName: 'aiAgent',
            workflowNodeName: id,
            ...extraData,
        },
        id,
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    });

    it('keeps the chain handle column straight through a configured root', async () => {
        const nodes: Node[] = [taskNode('task1'), clusterRootNode('aiAgent_1', true), taskNode('task2')];

        const edges: Edge[] = [edge('task1', 'aiAgent_1'), edge('aiAgent_1', 'task2')];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        // Handles: regular nodes at position + 36, configured root at
        // position + 120 — one straight column
        const taskHandleX = positionOf(result.nodes, 'task1').x + 36;

        expect(positionOf(result.nodes, 'aiAgent_1').x + 120).toBe(taskHandleX);
        expect(positionOf(result.nodes, 'task2').x + 36).toBe(taskHandleX);

        // The root's bigger rendered box keeps the 80px node→node rhythm on
        // both sides
        const rootTop = positionOf(result.nodes, 'aiAgent_1').y;
        const rootBottom = rootTop + 100;

        expect(rootTop - (positionOf(result.nodes, 'task1').y + 72)).toBe(CHAIN_GAP);
        expect(positionOf(result.nodes, 'task2').y - rootBottom).toBe(CHAIN_GAP);
    });

    it('treats an unconfigured root exactly like a plain task node', async () => {
        const nodes: Node[] = [taskNode('task1'), clusterRootNode('aiAgent_1', false), taskNode('task2')];

        const edges: Edge[] = [edge('task1', 'aiAgent_1'), edge('aiAgent_1', 'task2')];

        const result = await getElkLayoutElements({canvasWidth: 1000, direction: 'TB', edges, nodes});

        expect(positionOf(result.nodes, 'aiAgent_1').x).toBe(positionOf(result.nodes, 'task1').x);
        expect(positionOf(result.nodes, 'aiAgent_1').y - (positionOf(result.nodes, 'task1').y + 72)).toBe(CHAIN_GAP);
        expect(positionOf(result.nodes, 'task2').y - (positionOf(result.nodes, 'aiAgent_1').y + 72)).toBe(CHAIN_GAP);
    });

    it('keeps a configured root on its branch column inside a condition', async () => {
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            clusterRootNode('aiAgent_1', true, {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'aiAgent_1'),
            edge('aiAgent_1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        expect(collectScopeEdgeViolations(buildElkGraph(nodes, edges, 'TB'))).toEqual([]);

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        // TRUE (root) left of FALSE, frame box gap uniform
        expect(positionOf(result.nodes, 'aiAgent_1').x + 120).toBeLessThan(
            positionOf(result.nodes, 'childFalse1').x + 36
        );

        const topBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        expect(topBarY - (positionOf(result.nodes, 'condition_1').y + 72)).toBe(TOP_BOX_GAP);
        expect(positionOf(result.nodes, 'aiAgent_1').y - (topBarY + 2)).toBe(BAR_TO_CHILD_GAP);
    });
});

describe('getElkLayoutElements ring hug in LR', () => {
    it('keeps the rail the same visual distance from a nested box line in LR as in TB', async () => {
        // The nested condition's visible box line runs through its placeholder
        // CENTERS; placeholder DOMs are 72x28, so an edge-based hug that reads
        // 106px in TB collapses to 84px in LR — the hug must be center-based
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            conditionNode('condition_2', undefined),
            ...conditionGhostNodes('condition_2'),
            conditionPlaceholderNode('condition_2', 'left'),
            taskNode('falseChild', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
        ];

        (nodes[4].data as Record<string, unknown>).loopData = {index: 0, loopId: 'loop_1'};

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'condition_2-condition-left-placeholder-0'),
            edge('condition_2-condition-left-placeholder-0', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-top-ghost', 'falseChild'),
            edge('falseChild', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1400,
            direction: 'LR',
            edges,
            nodes,
        });

        // The ring content flips to the TOP edge in LR, so the rail mirrors
        // to the BOTTOM: it hugs the bottom-most content center at 106px
        // (36 anchor half + 70 content padding), its 2px line centered 1px in
        const falseChildCenterY = positionOf(result.nodes, 'falseChild').y + 36;
        const railLineY = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').y + 1;
        const loopCenterY = positionOf(result.nodes, 'loop_1').y + 36;

        expect(railLineY).toBe(Math.max(loopCenterY + 100, falseChildCenterY + 105));
        expect(railLineY - falseChildCenterY).toBeGreaterThanOrEqual(105);
    });
});

// Mirrors getLabelCrossOverhang: fixtures carry only workflowNodeName, so the
// label-side box edge is icon half (36) + min(200, 16 + 9 * name length)
const labelSideEdge = (iconCenter: number, nodeName: string): number =>
    iconCenter + 36 + Math.min(200, 16 + 9 * nodeName.length);

describe('no-crossing lanes', () => {
    // A frame column owns more than its nodes: its chip, entry drop,
    // connectors and trailing edge all render on the entry axis (a 45px
    // half-width spine spanning the whole frame), node titles extend 200px
    // right of the 72px icon, and a nested frame's rectangle is opaque. These
    // tests pin the packing consequences of that box model — the guarantees
    // behind "no edge crossings at all".

    it('reserves the one-sided label extent between adjacent single-task columns', async () => {
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const trueCenter = positionOf(result.nodes, 'childTrue1').x + 36;
        const falseCenter = positionOf(result.nodes, 'childFalse1').x + 36;

        // Binding pair: the left column's label edge (center + 36 + the
        // per-node estimate, 106 for the 10-char 'childTrue1') against the
        // right column's footprint edge (center − 120) at the exact 50px gap
        // — 142 + 50 + 120 = 312. A centered-footprint model would pack these
        // at 290 and let long labels collide; a flat worst-case reservation
        // packed them at 406 and read too airy beside dagre.
        expect(falseCenter - trueCenter).toBeGreaterThanOrEqual(labelSideEdge(0, 'childTrue1') + 50 + 120 - 1);
        expect(falseCenter - trueCenter).toBeLessThanOrEqual(labelSideEdge(0, 'childTrue1') + 50 + 120 + 1);
    });

    it('keeps a short chain clear of a deep sibling subtree for the FULL frame height', async () => {
        // condition_1: TRUE = chain into a nested condition (wide, deep),
        // FALSE = one task. Band-blind tucking would slide the FALSE task in
        // beside the TRUE entry — and its trailing edge (running on its axis
        // all the way down to condition_1's bottom bar) would slice through
        // the nested condition's case content.
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('childTrue1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            conditionNode('condition_2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            taskNode('nestedTrue', {conditionCase: 'caseTrue', conditionId: 'condition_2'}),
            taskNode('nestedFalse', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'childTrue1'),
            edge('childTrue1', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'nestedTrue'),
            edge('condition_2-condition-top-ghost', 'nestedFalse'),
            edge('nestedTrue', 'condition_2-condition-bottom-ghost'),
            edge('nestedFalse', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const falseAxis = positionOf(result.nodes, 'childFalse1').x + 36;

        // The FALSE column's spine lane [axis ± 45] spans condition_1's whole
        // frame, so EVERY TRUE-side node — including the nested condition's
        // cases living in bands far below the single FALSE task — must clear
        // it by the sibling gap on the label-extended side
        for (const trueSideId of ['childTrue1', 'condition_2', 'nestedTrue', 'nestedFalse']) {
            const trueCenter = positionOf(result.nodes, trueSideId).x + 36;

            if (falseAxis > trueCenter) {
                expect(falseAxis - 45 - labelSideEdge(trueCenter, trueSideId)).toBeGreaterThanOrEqual(49);
            } else {
                expect(trueCenter - 120 - (falseAxis + 45)).toBeGreaterThanOrEqual(49);
            }
        }
    });

    it('treats a nested frame rectangle as opaque even where it has no content', async () => {
        // The nested condition's TRUE case is a bare placeholder — its column
        // band is nearly empty, exactly where band-blind packing would tuck
        // the outer FALSE task INSIDE the nested frame's drawn rectangle
        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            conditionNode('condition_2', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            ...conditionGhostNodes('condition_2'),
            conditionPlaceholderNode('condition_2', 'left'),
            taskNode('nestedFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
            taskNode('nestedFalse2', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
            taskNode('childFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'condition_2-condition-left-placeholder-0'),
            edge('condition_2-condition-left-placeholder-0', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-top-ghost', 'nestedFalse1'),
            edge('nestedFalse1', 'nestedFalse2'),
            edge('nestedFalse2', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'childFalse1'),
            edge('childFalse1', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const outerFalseCenter = positionOf(result.nodes, 'childFalse1').x + 36;

        // Rectangle bounds from the nested frame's own members, using the
        // same label-extended box model the pack reserves
        const placeholderCenter = positionOf(result.nodes, 'condition_2-condition-left-placeholder-0').x + 36;
        const nestedChainCenters = [
            positionOf(result.nodes, 'nestedFalse1').x + 36,
            positionOf(result.nodes, 'nestedFalse2').x + 36,
        ];

        const nestedChainNames = ['nestedFalse1', 'nestedFalse2'];

        const rectangleStart = Math.min(placeholderCenter - 100, ...nestedChainCenters.map((center) => center - 120));
        const rectangleEnd = Math.max(
            placeholderCenter + 100,
            ...nestedChainCenters.map((center, index) => labelSideEdge(center, nestedChainNames[index]!))
        );

        expect(
            outerFalseCenter - 120 > rectangleEnd || labelSideEdge(outerFalseCenter, 'childFalse1') < rectangleStart
        ).toBe(true);
    });
});

describe('ring corridor rhythm', () => {
    it('centers the ring line between a sibling trailing edge and the loop content trailing edge', async () => {
        // Three long parallel verticals: the sibling case's trailing edge, the
        // loop ring, and the loop's leftmost content trailing edge. The ring
        // must read as the MIDDLE line of a near-even rhythm — its content hug
        // (36 + RAIL_CONTENT_PADDING = 106) against the 96px a neighbour packs
        // off the ring line (45px spine + 50px gap + the hairline).
        const conditionInLoop: Node = {
            data: {
                componentName: 'condition',
                loopData: {index: 0, loopId: 'loop_1'},
                taskDispatcher: true,
                taskDispatcherId: 'condition_2',
                workflowNodeName: 'condition_2',
            },
            id: 'condition_2',
            position: {x: 0, y: 0},
            type: 'workflow',
        };

        const nodes: Node[] = [
            conditionNode('condition_1'),
            ...conditionGhostNodes('condition_1'),
            taskNode('sibling1', {conditionCase: 'caseTrue', conditionId: 'condition_1'}),
            taskNode('chainTask1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            loopNode('loop_1', {conditionCase: 'caseFalse', conditionId: 'condition_1'}),
            ...loopAuxNodes('loop_1'),
            conditionInLoop,
            ...conditionGhostNodes('condition_2'),
            conditionPlaceholderNode('condition_2', 'left'),
            taskNode('nestedFalse1', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
            taskNode('nestedFalse2', {conditionCase: 'caseFalse', conditionId: 'condition_2'}),
        ];

        const edges: Edge[] = [
            edge('condition_1', 'condition_1-condition-top-ghost'),
            edge('condition_1-condition-top-ghost', 'sibling1'),
            edge('sibling1', 'condition_1-condition-bottom-ghost'),
            edge('condition_1-condition-top-ghost', 'chainTask1'),
            edge('chainTask1', 'loop_1'),
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'condition_2'),
            edge('condition_2', 'condition_2-condition-top-ghost'),
            edge('condition_2-condition-top-ghost', 'condition_2-condition-left-placeholder-0'),
            edge('condition_2-condition-left-placeholder-0', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-top-ghost', 'nestedFalse1'),
            edge('nestedFalse1', 'nestedFalse2'),
            edge('nestedFalse2', 'condition_2-condition-bottom-ghost'),
            edge('condition_2-condition-bottom-ghost', 'loop_1-loop-bottom-ghost'),
            edge('loop_1-loop-bottom-ghost', 'condition_1-condition-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1600, direction: 'TB', edges, nodes});

        const siblingAxis = positionOf(result.nodes, 'sibling1').x + 36;
        const ringLine = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x + 1;
        const contentLine = positionOf(result.nodes, 'condition_2-condition-left-placeholder-0').x + 36;

        const leftGap = ringLine - siblingAxis;
        const rightGap = contentLine - ringLine;

        // Content-bound hug: exactly 106 off the leftmost content center —
        // the corridor-splitting distance (a ring-bound neighbour packs 96px
        // off the ring line, a footprint-bound one 215px off the content axis,
        // so 106 reads as the middle line in both rhythms)
        expect(rightGap).toBeGreaterThanOrEqual(104);
        expect(rightGap).toBeLessThanOrEqual(107);

        // The sibling can never sit closer than the ring-bound minimum; in
        // this fixture its own entry-band label pitch governs (further out)
        expect(leftGap).toBeGreaterThanOrEqual(95);
    });
});

describe('LR ring content side', () => {
    it('puts a populated ring body on the TOP edge in LR, rail on the bottom', async () => {
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1400,
            direction: 'LR',
            edges,
            nodes,
        });

        const loopCenterY = positionOf(result.nodes, 'loop_1').y + 36;
        const childCenterY = positionOf(result.nodes, 'loopChild1').y + 36;
        const railLineY = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').y + 1;

        // Content column rides the ring's TOP side, mirrored by the rail below
        expect(childCenterY).toBe(loopCenterY - 100);
        expect(railLineY).toBe(loopCenterY + 100);
    });

    it('puts an empty ring "+" placeholder on the TOP edge in LR', async () => {
        const nodes: Node[] = [loopNode('loop_1'), ...loopAuxNodes('loop_1'), loopPlaceholderNode('loop_1')];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loop_1-loop-placeholder-0'),
            edge('loop_1-loop-placeholder-0', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1400,
            direction: 'LR',
            edges,
            nodes,
        });

        const loopCenterY = positionOf(result.nodes, 'loop_1').y + 36;
        const placeholderCenterY = positionOf(result.nodes, 'loop_1-loop-placeholder-0').y + 14;
        const railLineY = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').y + 1;

        // The "+" sits on the ring's TOP edge, the rail mirrors it below —
        // the ring renders square around the dispatcher spine
        expect(placeholderCenterY).toBeLessThan(loopCenterY);
        expect(railLineY).toBeGreaterThan(loopCenterY);
        expect(Math.abs(loopCenterY - placeholderCenterY - (railLineY - loopCenterY))).toBeLessThanOrEqual(2);
    });

    it('keeps the TB ring body on the RIGHT side unchanged', async () => {
        const nodes: Node[] = [
            loopNode('loop_1'),
            ...loopAuxNodes('loop_1'),
            loopChildTaskNode('loopChild1', 'loop_1'),
        ];

        const edges: Edge[] = [
            ...loopStructureEdges('loop_1'),
            edge('loop_1-loop-top-ghost', 'loopChild1'),
            edge('loopChild1', 'loop_1-loop-bottom-ghost'),
        ];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const loopCenterX = positionOf(result.nodes, 'loop_1').x + 36;
        const childCenterX = positionOf(result.nodes, 'loopChild1').x + 36;
        const railLineX = positionOf(result.nodes, 'loop_1-taskDispatcher-left-ghost').x + 1;

        expect(childCenterX).toBe(loopCenterX + 100);
        expect(railLineX).toBe(loopCenterX - 100);
    });
});

describe('LR entry gap', () => {
    it('gives the LR condition a wider node-to-bar corridor than TB for its rotated labels', async () => {
        // LR pulls the bar less (16 vs 28) so the rotated TRUE/FALSE labels that
        // live INSIDE the node->bar corridor get balanced air: gap is 50, not
        // TB's 38. The child side still reads the 94px entry run, restored by
        // LR_FRAME_ENTRY_INSET so the smaller pull costs no add-button room.
        const {edges, nodes} = singleConditionFixture();

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1400,
            direction: 'LR',
            edges,
            nodes,
        });

        const conditionRight = positionOf(result.nodes, 'condition_1').x + 72;
        const topGhostBarX = positionOf(result.nodes, 'condition_1-condition-top-ghost').x;

        expect(topGhostBarX - conditionRight).toBe(LR_TOP_BOX_GAP);

        // Entry run: bar -> first row node keeps the same add-button room as TB
        const trueChildX = positionOf(result.nodes, 'childTrue1').x;

        expect(trueChildX - (topGhostBarX + 2)).toBe(BAR_TO_CHILD_GAP);
    });
});

describe('cluster root chain alignment', () => {
    const measuredClusterRoot = (id: string, height: number): Node =>
        ({
            data: {clusterElements: {model: {name: 'model_1'}}, componentName: 'aiAgent', workflowNodeName: id},
            id,
            measured: {height, width: 292},
            position: {x: 0, y: 0},
            type: 'clusterRoot',
        }) as unknown as Node;

    it('lifts a cluster root so its centre sits on the chain in LR', async () => {
        const nodes: Node[] = [taskNode('before'), measuredClusterRoot('agent', 116), taskNode('after')];

        const edges: Edge[] = [edge('before', 'agent'), edge('agent', 'after')];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1600,
            direction: 'LR',
            edges,
            nodes,
        });

        const neighbourCenter = positionOf(result.nodes, 'before').y + 36;
        const agentCenter = positionOf(result.nodes, 'agent').y + 116 / 2;

        // ELK top-aligns, which only coincides with centring while every node is a 72px icon. A
        // cluster root grows downward, so its centre — and the handles ReactFlow puts there —
        // would otherwise hang below the row its neighbours connect on.
        expect(Math.abs(agentCenter - neighbourCenter)).toBeLessThanOrEqual(1);
    });

    it('centres an unmeasured cluster root on its elements-row growth', async () => {
        const nodes: Node[] = [
            taskNode('before'),
            {
                data: {clusterElements: {model: {name: 'model_1'}}, componentName: 'aiAgent'},
                id: 'agent',
                position: {x: 0, y: 0},
                type: 'clusterRoot',
            } as unknown as Node,
        ];

        const edges: Edge[] = [edge('before', 'agent')];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1600,
            direction: 'LR',
            edges,
            nodes,
        });

        // Nodes rebuilt from the workflow definition carry no measurement, so the shift comes from
        // how much an elements row grows the box (44) — half of it, lifting the centre onto the row
        expect(positionOf(result.nodes, 'agent').y).toBe(positionOf(result.nodes, 'before').y - 22);
    });

    it('leaves a cluster root with no elements alone, since it is already icon-sized', async () => {
        const nodes: Node[] = [
            taskNode('before'),
            {
                data: {componentName: 'aiAgent'},
                id: 'agent',
                position: {x: 0, y: 0},
                type: 'clusterRoot',
            } as unknown as Node,
        ];

        const edges: Edge[] = [edge('before', 'agent')];

        const result = await getElkLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1600,
            direction: 'LR',
            edges,
            nodes,
        });

        expect(positionOf(result.nodes, 'agent').y).toBe(positionOf(result.nodes, 'before').y);
    });
});

describe('trigger row label separation', () => {
    const triggerNode = (id: string, title: string): Node => ({
        data: {componentName: 'schedule', title, trigger: true, workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    it('clears a long first-trigger label before the second trigger', async () => {
        const nodes: Node[] = [
            triggerNode('trigger_1', 'Pokreni Svakog Radnog Dana'),
            triggerNode('trigger_2', 'Pokreni Rucno'),
            taskNode('task1'),
        ];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const firstX = positionOf(result.nodes, 'trigger_1').x;
        const secondX = positionOf(result.nodes, 'trigger_2').x;

        // The 26-char title caps at the 200px overhang past the icon's right
        // edge; the second trigger's icon must clear it plus the 30px air —
        // previously the row kept its tight 160px footprint pitch and the
        // label ran under the neighbouring icon
        expect(secondX).toBeGreaterThanOrEqual(firstX + 72 + 200 + 30 - 1);

        // The spread row re-centers on its pre-repack mean, so the fan-in
        // stays symmetric around the first task instead of trailing sideways
        const taskCenter = positionOf(result.nodes, 'task1').x + 36;

        expect(Math.abs((firstX + secondX) / 2 + 36 - taskCenter)).toBeLessThanOrEqual(1);
    });

    it('leaves room below the trigger row for the fan-in bus and the first edge add button', async () => {
        const nodes: Node[] = [triggerNode('trigger_1', 'Run'), triggerNode('trigger_2', 'Go'), taskNode('task1')];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const triggerBottom =
            Math.max(positionOf(result.nodes, 'trigger_1').y, positionOf(result.nodes, 'trigger_2').y) + 72;

        // Two triggers fan in, so the gap carries TWO segments: the bus is pinned 40px into it
        // and the first edge's leg runs from there to the task at a full 80px node-to-node gap.
        // One gap total left the leg at half length with the add button crammed inside it.
        expect(positionOf(result.nodes, 'task1').y - triggerBottom).toBe(120);
    });

    it('leaves the same room to the right of the trigger column in LR', async () => {
        const nodes: Node[] = [triggerNode('trigger_1', 'Run'), triggerNode('trigger_2', 'Go'), taskNode('task1')];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'LR', edges, nodes});

        const triggerRight =
            Math.max(positionOf(result.nodes, 'trigger_1').x, positionOf(result.nodes, 'trigger_2').x) + 72;

        expect(positionOf(result.nodes, 'task1').x - triggerRight).toBe(120);
    });

    it('uses a single node-to-node gap when one trigger means no fan-in bus', async () => {
        const nodes: Node[] = [triggerNode('trigger_1', 'Run'), taskNode('task1')];

        const edges: Edge[] = [edge('trigger_1', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const triggerBottom = positionOf(result.nodes, 'trigger_1').y + 72;

        // No bus to accommodate, so the first edge is simply a normal edge
        expect(positionOf(result.nodes, 'task1').y - triggerBottom).toBe(80);
    });

    // Label repacking spaces the row by label width, so a long first label leaves an uneven pitch.
    // Centering on the MEAN then put the middle trigger beside the target column rather than on it,
    // and that one edge arrived with a kink while its siblings ran straight into the bus.
    it('puts the middle trigger of an odd row exactly on the fan-in target', async () => {
        const nodes: Node[] = [
            triggerNode('trigger_1', 'Pokreni Svakog Radnog Dana'),
            triggerNode('trigger_2', 'Go'),
            triggerNode('trigger_3', 'Run'),
            taskNode('task1'),
        ];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1'), edge('trigger_3', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        expect(positionOf(result.nodes, 'trigger_2').x).toBeCloseTo(positionOf(result.nodes, 'task1').x, 5);
    });

    it('splits an even row around the fan-in target', async () => {
        const nodes: Node[] = [
            triggerNode('trigger_1', 'Pokreni Svakog Radnog Dana'),
            triggerNode('trigger_2', 'Go'),
            taskNode('task1'),
        ];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const firstX = positionOf(result.nodes, 'trigger_1').x;
        const secondX = positionOf(result.nodes, 'trigger_2').x;

        expect((firstX + secondX) / 2).toBeCloseTo(positionOf(result.nodes, 'task1').x, 5);
    });

    it('keeps the tight trigger pitch when labels fit', async () => {
        const nodes: Node[] = [triggerNode('trigger_1', 'Run'), triggerNode('trigger_2', 'Go'), taskNode('task1')];

        const edges: Edge[] = [edge('trigger_1', 'task1'), edge('trigger_2', 'task1')];

        const result = await getElkLayoutElements({canvasWidth: 1400, direction: 'TB', edges, nodes});

        const firstX = positionOf(result.nodes, 'trigger_1').x;
        const secondX = positionOf(result.nodes, 'trigger_2').x;

        // Short labels ('trigger_1' name is the longest text at 9 chars →
        // 97px estimate) fit inside the tight footprint pitch — the row must
        // not spread to label-reservation distances
        expect(secondX - firstX).toBeGreaterThanOrEqual(190);
        expect(secondX - firstX).toBeLessThanOrEqual(260);
    });
});
