import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {collectAllDescendantNodes, collectChainSuccessorNodes, isChildNodeOfDispatcher} from './collectDescendantNodes';

function makeNode(
    id: string,
    nestingData?: Record<string, unknown>,
    options?: {taskDispatcher?: boolean; taskDispatcherId?: string}
): Node {
    return {
        data: {
            componentName: id.split('_')[0],
            name: id,
            taskDispatcher: options?.taskDispatcher ?? false,
            taskDispatcherId: options?.taskDispatcherId,
            type: `${id.split('_')[0]}/v1`,
            ...nestingData,
        },
        id,
        position: {x: 100, y: 200},
        type: 'workflow',
    };
}

function makeGhostNode(dispatcherId: string, ghostType: string): Node {
    return {
        data: {
            conditionId: dispatcherId,
            taskDispatcherId: dispatcherId,
        },
        id: `${dispatcherId}-condition-${ghostType}`,
        position: {x: 0, y: 0},
        type: ghostType.includes('placeholder')
            ? 'placeholder'
            : `taskDispatcher${ghostType.charAt(0).toUpperCase() + ghostType.slice(1)}Node`,
    };
}

describe('isChildNodeOfDispatcher', () => {
    it('should match node with conditionData.conditionId', () => {
        const node = makeNode('aiAgent_1', {
            conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
        });

        expect(isChildNodeOfDispatcher(node, 'condition_1')).toBe(true);
    });

    it('should match node with loopData.loopId', () => {
        const node = makeNode('task_1', {
            loopData: {index: 0, loopId: 'loop_1'},
        });

        expect(isChildNodeOfDispatcher(node, 'loop_1')).toBe(true);
    });

    it('should match node with eachData.eachId', () => {
        const node = makeNode('task_1', {
            eachData: {eachId: 'each_1'},
        });

        expect(isChildNodeOfDispatcher(node, 'each_1')).toBe(true);
    });

    it('should match node with mapData.mapId', () => {
        const node = makeNode('task_1', {
            mapData: {index: 0, mapId: 'map_1'},
        });

        expect(isChildNodeOfDispatcher(node, 'map_1')).toBe(true);
    });

    it('should match node with branchData.branchId', () => {
        const node = makeNode('task_1', {
            branchData: {branchId: 'branch_1', caseKey: 'case1'},
        });

        expect(isChildNodeOfDispatcher(node, 'branch_1')).toBe(true);
    });

    it('should match node with parallelData.parallelId', () => {
        const node = makeNode('task_1', {
            parallelData: {parallelId: 'parallel_1'},
        });

        expect(isChildNodeOfDispatcher(node, 'parallel_1')).toBe(true);
    });

    it('should match node with forkJoinData.forkJoinId', () => {
        const node = makeNode('task_1', {
            forkJoinData: {forkJoinId: 'forkJoin_1'},
        });

        expect(isChildNodeOfDispatcher(node, 'forkJoin_1')).toBe(true);
    });

    it('should match ghost node with taskDispatcherId', () => {
        const ghost = makeGhostNode('condition_1', 'top-ghost');

        expect(isChildNodeOfDispatcher(ghost, 'condition_1')).toBe(true);
    });

    it('should not match the dispatcher node itself', () => {
        const node = makeNode('condition_1', undefined, {
            taskDispatcher: true,
            taskDispatcherId: 'condition_1',
        });

        expect(isChildNodeOfDispatcher(node, 'condition_1')).toBe(false);
    });

    it('should not match node belonging to a different dispatcher', () => {
        const node = makeNode('task_1', {
            conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_2', index: 0},
        });

        expect(isChildNodeOfDispatcher(node, 'condition_1')).toBe(false);
    });

    it('should not match node with no nesting data', () => {
        const node = makeNode('task_1');

        expect(isChildNodeOfDispatcher(node, 'condition_1')).toBe(false);
    });
});

describe('collectAllDescendantNodes', () => {
    it('should collect direct children of a condition dispatcher', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode('aiAgent_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            makeNode('logger_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 1},
            }),
            makeGhostNode('condition_1', 'top-ghost'),
            makeGhostNode('condition_1', 'bottom-ghost'),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.has('aiAgent_1')).toBe(true);
        expect(descendants.has('logger_1')).toBe(true);
        expect(descendants.has('condition_1-condition-top-ghost')).toBe(true);
        expect(descendants.has('condition_1-condition-bottom-ghost')).toBe(true);
        expect(descendants.has('condition_1')).toBe(false);
    });

    it('should collect children from both caseTrue and caseFalse', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode('task_true', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            makeNode('task_false', {
                conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_1', index: 0},
            }),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.has('task_true')).toBe(true);
        expect(descendants.has('task_false')).toBe(true);
    });

    it('should recursively collect nested dispatcher children', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode(
                'loop_1',
                {conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0}},
                {
                    taskDispatcher: true,
                    taskDispatcherId: 'loop_1',
                }
            ),
            makeNode('inner_task', {
                loopData: {index: 0, loopId: 'loop_1'},
            }),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.has('loop_1')).toBe(true);
        expect(descendants.has('inner_task')).toBe(true);
    });

    it('should recursively collect deeply nested condition children', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode(
                'condition_2',
                {
                    conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
                },
                {taskDispatcher: true, taskDispatcherId: 'condition_2'}
            ),
            makeNode('aiAgent_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_2', index: 0},
            }),
            makeNode('logger_1', {
                conditionData: {conditionCase: 'caseFalse', conditionId: 'condition_2', index: 0},
            }),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.has('condition_2')).toBe(true);
        expect(descendants.has('aiAgent_1')).toBe(true);
        expect(descendants.has('logger_1')).toBe(true);
    });

    it('should collect each dispatcher children', () => {
        const nodes: Node[] = [
            makeNode('each_1', undefined, {taskDispatcher: true, taskDispatcherId: 'each_1'}),
            makeNode('child_1', {
                eachData: {eachId: 'each_1'},
            }),
        ];

        const descendants = collectAllDescendantNodes('each_1', nodes);

        expect(descendants.has('child_1')).toBe(true);
    });

    it('should not collect nodes from unrelated dispatchers', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode('task_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            makeNode('condition_2', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_2'}),
            makeNode('task_2', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_2', index: 0},
            }),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.has('task_1')).toBe(true);
        expect(descendants.has('task_2')).toBe(false);
        expect(descendants.has('condition_2')).toBe(false);
    });

    it('should return empty map when dispatcher has no children', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeNode('task_1'),
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.size).toBe(0);
    });

    it('should preserve start positions from node.position', () => {
        const childNode = makeNode('task_1', {
            conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
        });

        childNode.position = {x: 350, y: 450};

        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            childNode,
        ];

        const descendants = collectAllDescendantNodes('condition_1', nodes);

        expect(descendants.get('task_1')).toEqual({x: 350, y: 450});
    });

    it('should collect parallel dispatcher children', () => {
        const nodes: Node[] = [
            makeNode('parallel_1', undefined, {taskDispatcher: true, taskDispatcherId: 'parallel_1'}),
            makeNode('task_1', {parallelData: {parallelId: 'parallel_1'}}),
            makeNode('task_2', {parallelData: {parallelId: 'parallel_1'}}),
        ];

        const descendants = collectAllDescendantNodes('parallel_1', nodes);

        expect(descendants.has('task_1')).toBe(true);
        expect(descendants.has('task_2')).toBe(true);
    });

    it('should collect branch dispatcher children including default case', () => {
        const nodes: Node[] = [
            makeNode('branch_1', undefined, {taskDispatcher: true, taskDispatcherId: 'branch_1'}),
            makeNode('case_task', {branchData: {branchId: 'branch_1', caseKey: 'case1'}}),
            makeNode('default_task', {branchData: {branchId: 'branch_1', caseKey: 'default'}}),
        ];

        const descendants = collectAllDescendantNodes('branch_1', nodes);

        expect(descendants.has('case_task')).toBe(true);
        expect(descendants.has('default_task')).toBe(true);
    });

    it('should collect three levels deep even when taskDispatcherId is overwritten to parent ID', () => {
        // In production, buildGenericNodeData overwrites taskDispatcherId
        // to the parent dispatcher's ID. The recursion must use node.id
        // instead of nodeData.taskDispatcherId to descend correctly.
        const nodes: Node[] = [
            makeNode('each_1', undefined, {taskDispatcher: true, taskDispatcherId: 'each_1'}),
            makeNode('fork-join_1', {eachData: {eachId: 'each_1'}}, {taskDispatcher: true, taskDispatcherId: 'each_1'}),
            makeGhostNode('fork-join_1', 'top-ghost'),
            makeNode(
                'condition_2',
                {forkJoinData: {branchIndex: 0, forkJoinId: 'fork-join_1', index: 0}},
                {taskDispatcher: true, taskDispatcherId: 'fork-join_1'}
            ),
            makeGhostNode('condition_2', 'top-ghost'),
            makeGhostNode('condition_2', 'bottom-ghost'),
            makeNode('inner_task', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_2', index: 0},
            }),
        ];

        const descendants = collectAllDescendantNodes('each_1', nodes);

        expect(descendants.has('fork-join_1')).toBe(true);
        expect(descendants.has('condition_2')).toBe(true);
        expect(descendants.has('condition_2-condition-top-ghost')).toBe(true);
        expect(descendants.has('condition_2-condition-bottom-ghost')).toBe(true);
        expect(descendants.has('inner_task')).toBe(true);
    });
});

function makeEdge(source: string, target: string): Edge {
    return {
        id: `${source}=>${target}`,
        source,
        target,
        type: 'workflow',
    };
}

describe('collectChainSuccessorNodes', () => {
    it('should collect nodes following the bottom ghost in the main flow', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'top-ghost'),
            makeGhostNode('condition_1', 'bottom-ghost'),
            makeNode('acumbamail_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            makeNode('aiAgent_1'),
            makeNode('logger_1'),
        ];

        const edges: Edge[] = [
            makeEdge('condition_1', 'condition_1-condition-top-ghost'),
            makeEdge('condition_1-condition-top-ghost', 'acumbamail_1'),
            makeEdge('acumbamail_1', 'condition_1-condition-bottom-ghost'),
            makeEdge('condition_1-condition-bottom-ghost', 'aiAgent_1'),
            makeEdge('aiAgent_1', 'logger_1'),
            makeEdge('logger_1', 'mmm1ubf269tdnz993fl'),
        ];

        const descendantIds = new Set([
            'condition_1-condition-top-ghost',
            'condition_1-condition-bottom-ghost',
            'acumbamail_1',
        ]);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.has('aiAgent_1')).toBe(true);
        expect(successors.has('logger_1')).toBe(true);
    });

    it('should not include the final placeholder node', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'bottom-ghost'),
            makeNode('task_1'),
            {data: {label: '+'}, id: 'mmm1ubf269tdnz993fl', position: {x: 0, y: 0}, type: 'placeholder'},
        ];

        const edges: Edge[] = [
            makeEdge('condition_1-condition-bottom-ghost', 'task_1'),
            makeEdge('task_1', 'mmm1ubf269tdnz993fl'),
        ];

        const descendantIds = new Set(['condition_1-condition-bottom-ghost']);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.has('task_1')).toBe(true);
        expect(successors.has('mmm1ubf269tdnz993fl')).toBe(false);
    });

    it('should return empty map when bottom ghost has no chain successors', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'bottom-ghost'),
        ];

        const edges: Edge[] = [makeEdge('condition_1-condition-bottom-ghost', 'mmm1ubf269tdnz993fl')];

        const descendantIds = new Set(['condition_1-condition-bottom-ghost']);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.size).toBe(0);
    });

    it('should skip nodes that are already collected as descendants', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'bottom-ghost'),
            makeNode('task_1', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_1', index: 0},
            }),
            makeNode('task_2'),
        ];

        const edges: Edge[] = [makeEdge('condition_1-condition-bottom-ghost', 'task_1'), makeEdge('task_1', 'task_2')];

        const descendantIds = new Set(['condition_1-condition-bottom-ghost', 'task_1']);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.has('task_1')).toBe(false);
        expect(successors.has('task_2')).toBe(true);
    });

    it('should recursively collect all descendants of a successor task dispatcher', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'bottom-ghost'),
            makeNode('condition_2', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_2'}),
            makeGhostNode('condition_2', 'top-ghost'),
            makeGhostNode('condition_2', 'bottom-ghost'),
            makeNode('inner_task', {
                conditionData: {conditionCase: 'caseTrue', conditionId: 'condition_2', index: 0},
            }),
        ];

        const edges: Edge[] = [makeEdge('condition_1-condition-bottom-ghost', 'condition_2')];

        const descendantIds = new Set(['condition_1-condition-bottom-ghost']);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.has('condition_2')).toBe(true);
        expect(successors.has('condition_2-condition-top-ghost')).toBe(true);
        expect(successors.has('condition_2-condition-bottom-ghost')).toBe(true);
        expect(successors.has('inner_task')).toBe(true);
    });

    it('should continue the chain past a successor dispatcher via its bottom ghost', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
            makeGhostNode('condition_1', 'bottom-ghost'),
            makeNode('condition_2', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_2'}),
            makeGhostNode('condition_2', 'top-ghost'),
            makeGhostNode('condition_2', 'bottom-ghost'),
            makeNode('loop_1', undefined, {taskDispatcher: true, taskDispatcherId: 'loop_1'}),
            makeNode('task_after_loop'),
        ];

        const edges: Edge[] = [
            makeEdge('condition_1-condition-bottom-ghost', 'condition_2'),
            makeEdge('condition_2', 'condition_2-condition-top-ghost'),
            makeEdge('condition_2-condition-bottom-ghost', 'loop_1'),
            makeEdge('loop_1', 'task_after_loop'),
            makeEdge('task_after_loop', 'mmm1ubf269tdnz993fl'),
        ];

        const descendantIds = new Set(['condition_1-condition-bottom-ghost']);

        const successors = collectChainSuccessorNodes('condition_1', nodes, edges, descendantIds);

        expect(successors.has('condition_2')).toBe(true);
        expect(successors.has('condition_2-condition-top-ghost')).toBe(true);
        expect(successors.has('condition_2-condition-bottom-ghost')).toBe(true);
        expect(successors.has('loop_1')).toBe(true);
        expect(successors.has('task_after_loop')).toBe(true);
    });

    it('should return empty map when no bottom ghost exists', () => {
        const nodes: Node[] = [
            makeNode('condition_1', undefined, {taskDispatcher: true, taskDispatcherId: 'condition_1'}),
        ];

        const descendantIds = new Set<string>();

        const successors = collectChainSuccessorNodes('condition_1', nodes, [], descendantIds);

        expect(successors.size).toBe(0);
    });
});
