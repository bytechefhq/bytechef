import {NODE_HEIGHT} from '@/shared/constants';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {buildElkGraph, getElkLayoutElements, getFrameId} from './elkLayoutUtils';

import type {ElkNode} from 'elkjs/lib/elk-api';

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

const edge = (source: string, target: string): Edge => ({id: `${source}=>${target}`, source, target});

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

        expect(firstGap).toBe(NODE_HEIGHT + 50);
        expect(secondGap).toBe(NODE_HEIGHT + 50);
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
        expect(firstGap).toBe(170);
        expect(secondGap).toBe(170);
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

        expect(branchGap).toBe(NODE_HEIGHT + 50);
        expect(rootGap).toBe(NODE_HEIGHT + 50);
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

        expect(innermostGap).toBe(NODE_HEIGHT + 50);
        expect(rootGap).toBe(NODE_HEIGHT + 50);
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

        expect(branchGap).toBe(170);
        expect(rootGap).toBe(170);
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

        // rendered centers: placeholder 28 wide, condition anchor 72 wide
        const placeholderCenter = positionOf(result.nodes, 'condition_1-condition-left-placeholder-0').x + 14;
        const nestedConditionCenter = positionOf(result.nodes, 'condition_2').x + 36;

        expect(placeholderCenter).toBeLessThan(nestedConditionCenter);
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
        const leftPlaceholderCenter = positionOf(result.nodes, 'condition_1-condition-left-placeholder-0').x + 14;
        const rightPlaceholderCenter = positionOf(result.nodes, 'condition_1-condition-right-placeholder-0').x + 14;

        expect(Math.abs((leftPlaceholderCenter + rightPlaceholderCenter) / 2 - conditionCenter)).toBeLessThanOrEqual(1);

        // The top ghost bar sits exactly the uniform inter-rank gap (ELK_SPACING, 50px) below
        // the condition's rank footprint. The bar is pinned to the top edge of its own reserved
        // footprint, so the reserved label/button space falls below the bar toward the branches.
        const conditionRankBottom = positionOf(result.nodes, 'condition_1').y + NODE_HEIGHT;
        const topGhostBarY = positionOf(result.nodes, 'condition_1-condition-top-ghost').y;

        expect(topGhostBarY - conditionRankBottom).toBe(50);
    });
});
