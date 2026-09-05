import {type Edge, type Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {toConditionCaseKey} from './collectTakenEmptyConditionCases';
import getExecutedEdgeStatus, {bypassDisabledEdgeEndpoints, resolveTestStateNodeName} from './getExecutedEdgeStatus';

const taskNode = (workflowNodeName: string): Node => ({
    data: {workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const ghostNode = (taskDispatcherId: string, suffix: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-${suffix}`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherTopGhostNode',
});

const bottomGhostNode = (taskDispatcherId: string, suffix: string): Node => ({
    data: {taskDispatcherId},
    id: `${taskDispatcherId}-${suffix}`,
    position: {x: 0, y: 0},
    type: 'taskDispatcherBottomGhostNode',
});

const disabledTaskNode = (workflowNodeName: string): Node => ({
    data: {disabled: true, workflowNodeName},
    id: workflowNodeName,
    position: {x: 0, y: 0},
});

const chainEdge = (source: string, target: string): Edge => ({id: `${source}=>${target}`, source, target});

describe('resolveTestStateNodeName', () => {
    it('prefers workflowNodeName, falls back to taskDispatcherId, then id', () => {
        expect(resolveTestStateNodeName(taskNode('logger_1'))).toBe('logger_1');
        expect(resolveTestStateNodeName(ghostNode('condition_1', 'condition-top-ghost'))).toBe('condition_1');
        expect(resolveTestStateNodeName({data: {}, id: 'raw-id', position: {x: 0, y: 0}})).toBe('raw-id');
        expect(resolveTestStateNodeName(undefined)).toBeUndefined();
    });
});

describe('getExecutedEdgeStatus', () => {
    it('marks an edge COMPLETED when both endpoints completed', () => {
        const states = {logger_1: {status: 'COMPLETED' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('logger_1'), states)).toBe('COMPLETED');
    });

    it('marks the incoming edge of a failed node COMPLETED, since the data reached it', () => {
        const states = {http_1: {status: 'FAILED' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('http_1'), states)).toBe('COMPLETED');
    });

    it('never colors the edges into or out of a "+" placeholder', () => {
        const placeholderNode: Node = {
            data: {taskDispatcherId: 'parallel_1'},
            id: 'parallel_1-parallel-placeholder-0',
            position: {x: 0, y: 0},
            type: 'placeholder',
        };

        const states = {parallel_1: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(ghostNode('parallel_1', 'parallel-top-ghost'), placeholderNode, states)
        ).toBeUndefined();

        expect(
            getExecutedEdgeStatus(placeholderNode, ghostNode('parallel_1', 'parallel-bottom-ghost'), states)
        ).toBeUndefined();
    });

    it('colors the edges around a condition placeholder only when the run took that empty case', () => {
        const conditionPlaceholderNode = (conditionCase: string, side: string): Node => ({
            data: {conditionCase, conditionId: 'condition_1', taskDispatcherId: 'condition_1'},
            id: `condition_1-condition-${side}-placeholder-0`,
            position: {x: 0, y: 0},
            type: 'placeholder',
        });

        const states = {condition_1: {status: 'COMPLETED' as const}};
        const takenEmptyConditionCases = new Set([toConditionCaseKey('condition_1', 'caseFalse')]);

        const topGhost = ghostNode('condition_1', 'condition-top-ghost');
        const bottomGhost = bottomGhostNode('condition_1', 'condition-bottom-ghost');
        const takenPlaceholder = conditionPlaceholderNode('caseFalse', 'right');
        const untakenPlaceholder = conditionPlaceholderNode('caseTrue', 'left');

        expect(getExecutedEdgeStatus(topGhost, takenPlaceholder, states, takenEmptyConditionCases)).toBe('COMPLETED');
        expect(getExecutedEdgeStatus(takenPlaceholder, bottomGhost, states, takenEmptyConditionCases)).toBe(
            'COMPLETED'
        );
        expect(getExecutedEdgeStatus(topGhost, untakenPlaceholder, states, takenEmptyConditionCases)).toBeUndefined();
        expect(
            getExecutedEdgeStatus(untakenPlaceholder, bottomGhost, states, takenEmptyConditionCases)
        ).toBeUndefined();
    });

    it('leaves the stem into a condition with two empty cases neutral, since the taken side is unknown', () => {
        const conditionNode = (caseTrue: unknown[], caseFalse: unknown[]): Node => ({
            data: {parameters: {caseFalse, caseTrue}, workflowNodeName: 'condition_2'},
            id: 'condition_2',
            position: {x: 0, y: 0},
        });

        const states = {condition_2: {status: 'COMPLETED' as const}};
        const topGhost = ghostNode('condition_2', 'condition-top-ghost');

        expect(getExecutedEdgeStatus(conditionNode([], []), topGhost, states)).toBeUndefined();
        expect(getExecutedEdgeStatus(conditionNode([{name: 'logger_1'}], []), topGhost, states)).toBe('COMPLETED');
    });

    it('colors dispatcher plumbing through ghost nodes', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}, logger_3: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(ghostNode('condition_1', 'condition-top-ghost'), taskNode('logger_3'), states)
        ).toBe('COMPLETED');
    });

    it('colors plumbing out of a dispatcher that failed through a nested child', () => {
        // A condition whose branch child failed is itself reported FAILED, so every edge leaving it --
        // its own edge into the top ghost, and the ghost's edge into the child -- has a FAILED source.
        const states = {condition_1: {status: 'FAILED' as const}, condition_2: {status: 'FAILED' as const}};

        expect(
            getExecutedEdgeStatus(taskNode('condition_1'), ghostNode('condition_1', 'condition-top-ghost'), states)
        ).toBe('COMPLETED');

        expect(
            getExecutedEdgeStatus(ghostNode('condition_1', 'condition-top-ghost'), taskNode('condition_2'), states)
        ).toBe('COMPLETED');
    });

    it('keeps the sibling branch of a failed dispatcher gray', () => {
        const states = {condition_1: {status: 'FAILED' as const}, condition_2: {status: 'FAILED' as const}};

        expect(
            getExecutedEdgeStatus(ghostNode('condition_1', 'condition-top-ghost'), taskNode('logger_2'), states)
        ).toBeUndefined();
    });

    it('colors a completed child of a dispatcher that failed later in the same branch', () => {
        const states = {condition_1: {status: 'FAILED' as const}, logger_5: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(ghostNode('condition_1', 'condition-top-ghost'), taskNode('logger_5'), states)
        ).toBe('COMPLETED');
    });

    it('keeps the untaken branch gray: child without state into an executed ghost', () => {
        const states = {condition_1: {status: 'COMPLETED' as const}};

        expect(
            getExecutedEdgeStatus(taskNode('logger_2'), ghostNode('condition_1', 'condition-bottom-ghost'), states)
        ).toBeUndefined();
    });

    it('colors a completed branch into the join green even when a sibling branch failed the dispatcher', () => {
        // A parallel whose other branch failed is itself reported FAILED, and its bottom ghost borrows
        // that status. The branch that finished still joined cleanly, so its edge must not turn red.
        const states = {dataStorage_2: {status: 'COMPLETED' as const}, parallel_1: {status: 'FAILED' as const}};

        expect(
            getExecutedEdgeStatus(
                taskNode('dataStorage_2'),
                bottomGhostNode('parallel_1', 'parallel-bottom-ghost'),
                states
            )
        ).toBe('COMPLETED');
    });

    it('keeps the edge from a failed last child into the join neutral', () => {
        // The failure stopped the branch, so nothing flowed on into the join -- the same as the gray edge
        // after a failed node anywhere else on the canvas.
        const states = {http_1: {status: 'FAILED' as const}, parallel_1: {status: 'FAILED' as const}};

        expect(
            getExecutedEdgeStatus(taskNode('http_1'), bottomGhostNode('parallel_1', 'parallel-bottom-ghost'), states)
        ).toBeUndefined();
    });

    it('keeps the edge from a failed nested dispatcher into the outer join neutral', () => {
        const states = {condition_2: {status: 'FAILED' as const}, parallel_1: {status: 'FAILED' as const}};

        expect(
            getExecutedEdgeStatus(
                bottomGhostNode('condition_2', 'condition-bottom-ghost'),
                bottomGhostNode('parallel_1', 'parallel-bottom-ghost'),
                states
            )
        ).toBeUndefined();
    });

    it('is neutral while the target is still running or has no state', () => {
        const states = {logger_1: {status: 'RUNNING' as const}, var_1: {status: 'COMPLETED' as const}};

        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('logger_1'), states)).toBeUndefined();
        expect(getExecutedEdgeStatus(taskNode('var_1'), taskNode('missing'), states)).toBeUndefined();
    });
});

describe('bypassDisabledEdgeEndpoints', () => {
    // firecrawl_7 -> openAi_2 (disabled) -> anthropic_1, the left branch of a parallel.
    const nodes = [
        ghostNode('parallel_1', 'parallel-top-ghost'),
        disabledTaskNode('firecrawl_4'),
        taskNode('firecrawl_7'),
        disabledTaskNode('openAi_2'),
        taskNode('anthropic_1'),
    ];

    const edges = [
        chainEdge('parallel_1-parallel-top-ghost', 'firecrawl_4'),
        chainEdge('firecrawl_4', 'firecrawl_7'),
        chainEdge('firecrawl_7', 'openAi_2'),
        chainEdge('openAi_2', 'anthropic_1'),
    ];

    const states = {
        anthropic_1: {status: 'FAILED' as const},
        firecrawl_7: {status: 'COMPLETED' as const},
        parallel_1: {status: 'FAILED' as const},
    };

    const statusOf = (source: string, target: string, disabledTaskNames = new Set<string>()) => {
        const endpoints = bypassDisabledEdgeEndpoints(
            nodes.find((node) => node.id === source),
            nodes.find((node) => node.id === target),
            {disabledTaskNames, edges, nodes}
        );

        return getExecutedEdgeStatus(endpoints.sourceNode, endpoints.targetNode, states);
    };

    it('carries the executed trail across a disabled node up to the failed node', () => {
        expect(statusOf('firecrawl_7', 'openAi_2')).toBe('COMPLETED');
        expect(statusOf('openAi_2', 'anthropic_1')).toBe('COMPLETED');
    });

    it('carries the executed path from a ghost across a disabled first child', () => {
        expect(statusOf('parallel_1-parallel-top-ghost', 'firecrawl_4')).toBe('COMPLETED');
        expect(statusOf('firecrawl_4', 'firecrawl_7')).toBe('COMPLETED');
    });

    it('treats a task under a disabled ancestor as disabled', () => {
        const ancestorNodes = [taskNode('firecrawl_7'), taskNode('openAi_2'), taskNode('anthropic_1')];

        const endpoints = bypassDisabledEdgeEndpoints(ancestorNodes[0], ancestorNodes[1], {
            disabledTaskNames: new Set(['openAi_2']),
            edges,
            nodes: ancestorNodes,
        });

        expect(endpoints.targetNode?.id).toBe('anthropic_1');
    });

    it('stops at a disabled node that fans out, since no single path runs through it', () => {
        const fanOutEdges = [...edges, chainEdge('openAi_2', 'firecrawl_4')];

        const endpoints = bypassDisabledEdgeEndpoints(nodes[2], nodes[3], {
            disabledTaskNames: new Set(),
            edges: fanOutEdges,
            nodes,
        });

        expect(endpoints.targetNode?.id).toBe('openAi_2');
        expect(getExecutedEdgeStatus(endpoints.sourceNode, endpoints.targetNode, states)).toBeUndefined();
    });

    it('keeps an untaken branch neutral across a disabled node', () => {
        const endpoints = bypassDisabledEdgeEndpoints(nodes[0], nodes[1], {disabledTaskNames: new Set(), edges, nodes});

        expect(
            getExecutedEdgeStatus(endpoints.sourceNode, endpoints.targetNode, {
                parallel_1: {status: 'COMPLETED' as const},
            })
        ).toBeUndefined();
    });

    it('returns enabled endpoints untouched', () => {
        const endpoints = bypassDisabledEdgeEndpoints(nodes[2], nodes[4], {disabledTaskNames: new Set(), edges, nodes});

        expect(endpoints).toEqual({sourceNode: nodes[2], targetNode: nodes[4]});
    });
});
