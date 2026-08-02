import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CLUSTER_ROOT_NODE_WIDTH,
    NODE_HEIGHT,
    NODE_WIDTH,
    PLACEHOLDER_NODE_HEIGHT,
    ROOT_CLUSTER_WIDTH,
} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    GraphChildTasksType,
    LoopChildTasksType,
    MapChildTasksType,
    OnErrorChildTasksType,
    ParallelChildTasksType,
} from '@/shared/types';
import {Edge, Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createGraphEdges from './createGraphEdges';
import {
    CLUSTER_ELEMENT_GAP,
    CLUSTER_ELEMENT_LABEL_PADDING,
    CLUSTER_ELEMENT_OVERLAP_PADDING,
    CLUSTER_ROOT_GAP,
    calculateNodeHeight,
    collectTaskDispatcherData,
    filterAndDedupeLayoutEdges,
    getClusterElementsLayoutElements,
    getDagreNodeSize,
    getLayoutElements,
    realignGraphLaneHandlesToDeclarationOrder,
} from './layoutUtils';

// Type for test tasks with potentially malformed parameters
type TestTaskType = Omit<WorkflowTask, 'parameters'> & {
    parameters?: {
        caseFalse?: unknown;
        caseTrue?: unknown;
        branches?: unknown;
        iteratee?: unknown;
        tasks?: unknown;
    };
};

describe('calculateNodeHeight', () => {
    it('should return NODE_HEIGHT for regular workflow nodes', () => {
        const node: Node = {data: {}, id: 'node_1', position: {x: 0, y: 0}, type: 'workflow'};

        expect(calculateNodeHeight(node)).toBe(NODE_HEIGHT);
    });

    it('should return NODE_HEIGHT for cluster root nodes', () => {
        const node: Node = {data: {clusterRoot: true}, id: 'aiAgent_1', position: {x: 0, y: 0}, type: 'clusterRoot'};

        expect(calculateNodeHeight(node)).toBe(NODE_HEIGHT);
    });

    it('should return 0 for top ghost nodes', () => {
        const node: Node = {
            data: {taskDispatcherId: 'each_1'},
            id: 'each_1-each-top-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        };

        expect(calculateNodeHeight(node)).toBe(0);
    });

    it('should return 0 for bottom ghost nodes', () => {
        const node: Node = {
            data: {taskDispatcherId: 'each_1'},
            id: 'each_1-each-bottom-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        };

        expect(calculateNodeHeight(node)).toBe(0);
    });

    it('should return PLACEHOLDER_NODE_HEIGHT for left ghost nodes', () => {
        const node: Node = {
            data: {taskDispatcherId: 'each_1'},
            id: 'each_1-taskDispatcher-left-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherLeftGhostNode',
        };

        expect(calculateNodeHeight(node)).toBe(PLACEHOLDER_NODE_HEIGHT);
    });

    it('should return PLACEHOLDER_NODE_HEIGHT for placeholder nodes', () => {
        const node: Node = {data: {}, id: 'placeholder_1', position: {x: 0, y: 0}, type: 'placeholder'};

        expect(calculateNodeHeight(node)).toBe(PLACEHOLDER_NODE_HEIGHT);
    });
});

describe('getDagreNodeSize', () => {
    const regularNode: Node = {data: {}, id: 'logger_1', position: {x: 0, y: 0}, type: 'workflow'};

    const clusterRootWithElements: Node = {
        data: {clusterElements: {channel: {name: 'slack'}}, clusterRoot: true},
        id: 'approval_1',
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    };

    const clusterRootWithoutElements: Node = {
        data: {clusterElements: {channel: null}, clusterRoot: true},
        id: 'approval_2',
        position: {x: 0, y: 0},
        type: 'clusterRoot',
    };

    it('should reserve NODE_WIDTH for a regular node in TB', () => {
        expect(getDagreNodeSize(regularNode, 'TB').width).toBe(NODE_WIDTH);
    });

    it('should reserve the cluster-root width for a configured cluster root in TB', () => {
        expect(getDagreNodeSize(clusterRootWithElements, 'TB').width).toBe(CLUSTER_ROOT_NODE_WIDTH);
        expect(CLUSTER_ROOT_NODE_WIDTH).toBeGreaterThan(NODE_WIDTH);
    });

    it('should reserve NODE_WIDTH for an unconfigured cluster root in TB', () => {
        expect(getDagreNodeSize(clusterRootWithoutElements, 'TB').width).toBe(NODE_WIDTH);
    });

    it('should preserve the LR cluster-root width reservation (292)', () => {
        expect(getDagreNodeSize(clusterRootWithElements, 'LR').width).toBe(292);
    });
});

describe('collectTaskDispatcherData', () => {
    it('should handle non-array caseFalse parameter without throwing error', () => {
        const task: TestTaskType = {
            name: 'test-condition',
            parameters: {
                caseFalse: 'not-an-array', // This should not cause an error
                caseTrue: [{name: 'task1'}, {name: 'task2'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                graphChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseFalse since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual([]);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task1', 'task2']);
    });

    it('should handle non-array caseTrue parameter without throwing error', () => {
        const task: TestTaskType = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: {not: 'an-array'}, // This should not cause an error
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // This should not throw an error
        expect(() => {
            collectTaskDispatcherData(
                task,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                graphChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty array for caseTrue since it's not an array
        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual([]);
    });

    it('should handle valid array parameters correctly', () => {
        const task: WorkflowTask = {
            name: 'test-condition',
            parameters: {
                caseFalse: [{name: 'task1'}, {name: 'task2'}],
                caseTrue: [{name: 'task3'}, {name: 'task4'}],
            },
            type: 'condition/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            graphChildTasks,
            loopChildTasks,
            mapChildTasks,
            onErrorChildTasks,
            parallelChildTasks
        );

        expect(conditionChildTasks['test-condition'].caseFalse).toEqual(['task1', 'task2']);
        expect(conditionChildTasks['test-condition'].caseTrue).toEqual(['task3', 'task4']);

        // Additional assertions to ensure other collections are not affected
        expect(branchChildTasks).toEqual({});
        expect(eachChildTasks).toEqual({});
        expect(forkJoinChildTasks).toEqual({});
        expect(loopChildTasks).toEqual({});
        expect(mapChildTasks).toEqual({});
        expect(onErrorChildTasks).toEqual({});
        expect(parallelChildTasks).toEqual({});
    });

    it('should handle valid array parameters for fork-join correctly', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join',
            parameters: {
                branches: [
                    [{name: 'task1'}, {name: 'task2'}], // First branch
                    [{name: 'task3'}, {name: 'task4'}], // Second branch
                ],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            graphChildTasks,
            loopChildTasks,
            mapChildTasks,
            onErrorChildTasks,
            parallelChildTasks
        );

        // Should preserve the array-of-arrays structure (each branch is separate)
        expect(forkJoinChildTasks['test-fork-join'].branches).toEqual([
            ['task1', 'task2'],
            ['task3', 'task4'],
        ]);
    });

    it('should handle fork-join with empty branches', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join-empty',
            parameters: {
                branches: [],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            graphChildTasks,
            loopChildTasks,
            mapChildTasks,
            onErrorChildTasks,
            parallelChildTasks
        );

        expect(forkJoinChildTasks['test-fork-join-empty'].branches).toEqual([]);
    });

    it('should handle fork-join with single empty branch', () => {
        const task: WorkflowTask = {
            name: 'test-fork-join-single-empty',
            parameters: {
                branches: [[]],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            graphChildTasks,
            loopChildTasks,
            mapChildTasks,
            onErrorChildTasks,
            parallelChildTasks
        );

        expect(forkJoinChildTasks['test-fork-join-single-empty'].branches).toEqual([[]]);
    });

    it('should handle fork-join with malformed branch data', () => {
        const task: TestTaskType = {
            name: 'test-fork-join-malformed',
            parameters: {
                branches: [
                    [{name: 'task1'}, {name: 'task2'}], // Valid branch
                    'not-an-array', // Malformed branch
                    [{name: 'task3'}], // Valid branch
                ],
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        collectTaskDispatcherData(
            task,
            branchChildTasks,
            conditionChildTasks,
            eachChildTasks,
            forkJoinChildTasks,
            graphChildTasks,
            loopChildTasks,
            mapChildTasks,
            onErrorChildTasks,
            parallelChildTasks
        );

        // Should handle malformed branches gracefully
        expect(forkJoinChildTasks['test-fork-join-malformed'].branches).toEqual([['task1', 'task2'], [], ['task3']]);
    });

    it('should handle non-array parameters for other task types', () => {
        const loopTask: TestTaskType = {
            name: 'test-loop',
            parameters: {
                iteratee: 'not-an-array', // This should not cause an error
            },
            type: 'loop/v1',
        };

        const parallelTask: TestTaskType = {
            name: 'test-parallel',
            parameters: {
                tasks: {not: 'an-array'}, // This should not cause an error
            },
            type: 'parallel/v1',
        };

        const forkJoinTask: TestTaskType = {
            name: 'test-fork-join',
            parameters: {
                branches: 'not-an-array', // This should not cause an error
            },
            type: 'fork-join/v1',
        };

        const branchChildTasks: BranchChildTasksType = {};
        const conditionChildTasks: ConditionChildTasksType = {};
        const eachChildTasks: EachChildTasksType = {};
        const forkJoinChildTasks: ForkJoinChildTasksType = {};
        const graphChildTasks: GraphChildTasksType = {};
        const loopChildTasks: LoopChildTasksType = {};
        const mapChildTasks: MapChildTasksType = {};
        const onErrorChildTasks: OnErrorChildTasksType = {};
        const parallelChildTasks: ParallelChildTasksType = {};

        // These should not throw errors
        expect(() => {
            collectTaskDispatcherData(
                loopTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                graphChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        expect(() => {
            collectTaskDispatcherData(
                parallelTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                graphChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        expect(() => {
            collectTaskDispatcherData(
                forkJoinTask,
                branchChildTasks,
                conditionChildTasks,
                eachChildTasks,
                forkJoinChildTasks,
                graphChildTasks,
                loopChildTasks,
                mapChildTasks,
                onErrorChildTasks,
                parallelChildTasks
            );
        }).not.toThrow();

        // Should create empty arrays for non-array parameters
        expect(loopChildTasks['test-loop'].iteratee).toEqual([]);
        expect(parallelChildTasks['test-parallel'].tasks).toEqual([]);
        expect(forkJoinChildTasks['test-fork-join'].branches).toEqual([]);
    });
});

describe('cluster element spacing', () => {
    it('should produce a horizontal gap that exceeds the overlap resolution threshold', () => {
        const horizontalGap = CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_ELEMENT_GAP;
        const overlapMinDistance =
            CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_ELEMENT_LABEL_PADDING * 2 + CLUSTER_ELEMENT_OVERLAP_PADDING;

        expect(horizontalGap).toBeGreaterThan(overlapMinDistance);
    });

    it('should have a gap of 142px center-to-center between cluster elements', () => {
        const horizontalGap = CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_ELEMENT_GAP;

        expect(horizontalGap).toBe(142);
    });
});

describe('cluster root spacing', () => {
    it('should produce a uniform gap that exceeds the overlap resolution threshold', () => {
        const clusterRootHorizontalGap = ROOT_CLUSTER_WIDTH + CLUSTER_ROOT_GAP;
        const overlapMinDistance = ROOT_CLUSTER_WIDTH + CLUSTER_ELEMENT_OVERLAP_PADDING;

        expect(clusterRootHorizontalGap).toBeGreaterThan(overlapMinDistance);
    });

    it('should have a gap of 320px center-to-center between cluster root children', () => {
        const clusterRootHorizontalGap = ROOT_CLUSTER_WIDTH + CLUSTER_ROOT_GAP;

        expect(clusterRootHorizontalGap).toBe(320);
    });

    it('should use CLUSTER_ROOT_GAP as overlap resolution minimum between cluster roots', () => {
        expect(CLUSTER_ROOT_GAP).toBeGreaterThanOrEqual(CLUSTER_ELEMENT_OVERLAP_PADDING);
    });
});

describe('getClusterElementsLayoutElements new node positioning with moved siblings', () => {
    const canvasWidth = 1200;
    const horizontalGap = CLUSTER_ELEMENT_NODE_WIDTH + CLUSTER_ELEMENT_GAP;
    const childBaseY = 160 + PLACEHOLDER_NODE_HEIGHT + NODE_HEIGHT / 4;

    function makeRootNode(): Node {
        return {
            data: {
                clusterElementTypesCount: 1,
                clusterElements: {typeA: []},
            },
            id: 'root',
            position: {x: 0, y: 0},
            type: 'clusterRoot',
        };
    }

    function makeChildNode(nodeId: string, overrides: {metadata?: Record<string, unknown>} = {}): Node {
        return {
            data: {
                clusterElementType: 'typeA',
                clusterElementTypeIndex: 0,
                isNestedClusterRoot: false,
                metadata: overrides.metadata || {},
                parentClusterRootElementsTypeCount: 1,
            },
            id: nodeId,
            parentId: 'root',
            position: {x: 0, y: 0},
            type: 'workflow',
        };
    }

    it('should place a new node next to the actual rightmost sibling, not the default position', () => {
        // Sibling at index 0 has been manually moved far to the left (saved position)
        // Sibling at index 1 has been manually moved far to the left (saved position)
        // New node (index 2) should be placed relative to the actual rightmost sibling
        const movedX = 50;

        const nodes: Node[] = [
            makeRootNode(),
            makeChildNode('child-0', {metadata: {ui: {nodePosition: {x: movedX, y: childBaseY}}}}),
            makeChildNode('child-1', {metadata: {ui: {nodePosition: {x: movedX + horizontalGap, y: childBaseY}}}}),
            makeChildNode('child-2'), // New node, no saved position
        ];

        const result = getClusterElementsLayoutElements({canvasHeight: 800, canvasWidth, edges: [], nodes});
        const newNode = result.nodes.find((node) => node.id === 'child-2');
        const secondSibling = result.nodes.find((node) => node.id === 'child-1');

        expect(newNode).toBeDefined();
        expect(secondSibling).toBeDefined();

        // New node should be placed one horizontalGap to the right of the rightmost sibling
        expect(newNode!.position.x).toBe(secondSibling!.position.x + horizontalGap);
    });

    it('should place a new node next to a sibling moved to the left', () => {
        // Only one existing sibling, moved far to the left
        const movedX = -200;

        const nodes: Node[] = [
            makeRootNode(),
            makeChildNode('child-0', {metadata: {ui: {nodePosition: {x: movedX, y: childBaseY}}}}),
            makeChildNode('child-1'), // New node
        ];

        const result = getClusterElementsLayoutElements({canvasHeight: 800, canvasWidth, edges: [], nodes});
        const newNode = result.nodes.find((node) => node.id === 'child-1');

        expect(newNode).toBeDefined();

        // Should be placed relative to the moved sibling, not the default position
        expect(newNode!.position.x).toBe(movedX + horizontalGap);
    });

    it('should use the default position when no siblings have saved positions', () => {
        const nodes: Node[] = [makeRootNode(), makeChildNode('child-0'), makeChildNode('child-1')];

        const result = getClusterElementsLayoutElements({canvasHeight: 800, canvasWidth, edges: [], nodes});
        const firstNode = result.nodes.find((node) => node.id === 'child-0');
        const secondNode = result.nodes.find((node) => node.id === 'child-1');

        expect(firstNode).toBeDefined();
        expect(secondNode).toBeDefined();

        // Second node should be one gap away from the first
        expect(secondNode!.position.x).toBe(firstNode!.position.x + horizontalGap);
    });
});

describe('filterAndDedupeLayoutEdges', () => {
    const makeWorkflowNode = (id: string): Node => ({
        data: {componentName: 'mailchimp'},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    // A graph lane's first task (see createGraphTransitionEdges) legitimately carries
    // TWO outgoing edges: its real structural successor edge, and a `graphTransition`
    // overlay edge sharing the same source. The "one edge per source" collapse below
    // must not treat these as a competing structural fan-out — regression-pinned since
    // that collapse previously dropped the overlay edge outright whenever a source
    // already had a real successor (P3-T3).
    it('should keep a graphTransition edge alongside its source lane task real structural edge', () => {
        const nodes: Node[] = [makeWorkflowNode('taskA'), makeWorkflowNode('taskB'), makeWorkflowNode('taskC')];

        const edges: Edge[] = [
            {id: 'taskA=>taskB', source: 'taskA', target: 'taskB', type: 'workflow'},
            {
                id: 'graph_1-transition-0-1',
                source: 'taskA',
                sourceHandle: 'taskA-graph-transition-source',
                target: 'taskC',
                targetHandle: 'taskC-graph-transition-target',
                type: 'graphTransition',
            },
        ];

        const result = filterAndDedupeLayoutEdges(nodes, edges);

        const structuralEdge = result.find((edge) => edge.type === 'workflow');
        const transitionEdge = result.find((edge) => edge.type === 'graphTransition');

        expect(structuralEdge).toBeDefined();
        expect(structuralEdge!.target).toBe('taskB');
        expect(transitionEdge).toBeDefined();
        expect(transitionEdge!.target).toBe('taskC');
    });

    it('should still dedupe an exact-duplicate graphTransition edge by endpoint+handle key', () => {
        const nodes: Node[] = [makeWorkflowNode('taskA'), makeWorkflowNode('taskC')];

        const duplicateTransitionEdge: Edge = {
            id: 'graph_1-transition-0-1',
            source: 'taskA',
            sourceHandle: 'taskA-graph-transition-source',
            target: 'taskC',
            targetHandle: 'taskC-graph-transition-target',
            type: 'graphTransition',
        };

        const result = filterAndDedupeLayoutEdges(nodes, [duplicateTransitionEdge, {...duplicateTransitionEdge}]);

        expect(result.filter((edge) => edge.type === 'graphTransition')).toHaveLength(1);
    });

    it('should still drop a graphTransition edge that references a node no longer in the layout', () => {
        const nodes: Node[] = [makeWorkflowNode('taskA')];

        const edges: Edge[] = [
            {
                id: 'graph_1-transition-0-1',
                source: 'taskA',
                target: 'deletedTask',
                type: 'graphTransition',
            },
        ];

        expect(filterAndDedupeLayoutEdges(nodes, edges)).toEqual([]);
    });
});

describe('getLayoutElements (dagre) with graphTransition edges', () => {
    // `usesElkGraphTransitionOverlay` (useLayout.tsx) never creates `graphTransition`
    // edges while dagre is the active engine, BUT getElkLayoutElements's error-fallback
    // branch re-invokes getLayoutElements with the SAME edges array ELK was given —
    // which may include them. Pinning that dagre stays cycle-safe and geometry-stable
    // even on that fallback path (P3-T3), mirroring the ELK-side pin in
    // elkLayoutUtils.test.ts.
    const makeWorkflowNode = (id: string): Node => ({
        data: {componentName: 'mailchimp', workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const nodes: Node[] = [makeWorkflowNode('n0'), makeWorkflowNode('n1')];

    const cyclicTransitionEdges: Edge[] = [
        {id: 'graph_1-transition-0-1', source: 'n0', target: 'n1', type: 'graphTransition'},
        {id: 'graph_1-transition-1-0', source: 'n1', target: 'n0', type: 'graphTransition'},
    ];

    it('does not throw and lays out identically with a cyclic pair of graphTransition edges present', async () => {
        const withoutTransitions = await getLayoutElements({canvasWidth: 1000, direction: 'TB', edges: [], nodes});

        const withTransitions = await getLayoutElements({
            canvasWidth: 1000,
            direction: 'TB',
            edges: cyclicTransitionEdges,
            nodes,
        });

        expect(withTransitions.nodes.find((node) => node.id === 'n0')?.position).toEqual(
            withoutTransitions.nodes.find((node) => node.id === 'n0')?.position
        );
        expect(withTransitions.nodes.find((node) => node.id === 'n1')?.position).toEqual(
            withoutTransitions.nodes.find((node) => node.id === 'n1')?.position
        );
    });

    it('never returns a graphTransition edge (dagre keeps phase-2 badges as its sole transition visualization)', async () => {
        const result = await getLayoutElements({
            canvasWidth: 1000,
            direction: 'TB',
            edges: cyclicTransitionEdges,
            nodes,
        });

        expect(result.edges.some((edge) => edge.type === 'graphTransition')).toBe(false);
    });
});

describe('getLayoutElements (dagre) with visual-lane-order graph edges', () => {
    // getElkLayoutElements's error-fallback branch re-invokes getLayoutElements with the SAME
    // edges array ELK was given — for a `graph/v1` dispatcher those edges were built with
    // `orderLanesByVisualPosition: true`, so their shared top/bottom-ghost handles follow the
    // VISUAL lane permutation while dagre always lays lanes out in DECLARATION order. Without a
    // fix the fallback renders a lane's edges leaving from a handle that no longer matches its
    // physical column (the boxy-staircase regression). Pinning that dagre corrects this on the
    // fallback path, mirroring the graphTransition pin immediately above.
    //
    // Declaration order is node_0..node_3, but the transitions form the chain node_0 -> node_3 ->
    // node_2 -> node_1 (with node_1 -> node_0 closing the cycle), so the VISUAL order is node_0,
    // node_3, node_2, node_1 (matching the fixture in createGraphEdges.test.ts).
    const cyclicFourNodeChain = [
        {name: 'node_0', next: "'node_3'", tasks: [{name: 'task_0', type: 'task/v1'}]},
        {name: 'node_1', next: "'node_0'", tasks: [{name: 'task_1', type: 'task/v1'}]},
        {name: 'node_2', next: "'node_1'", tasks: [{name: 'task_2', type: 'task/v1'}]},
        {name: 'node_3', next: "'node_2'", tasks: [{name: 'task_3', type: 'task/v1'}]},
    ];

    const graphDispatcherNode: Node = {
        data: {componentName: 'graph', parameters: {nodes: cyclicFourNodeChain}, taskDispatcher: true},
        id: 'graph_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const graphNodes: Node[] = [
        graphDispatcherNode,
        {
            data: {taskDispatcherId: 'graph_1'},
            id: 'graph_1-graph-top-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherTopGhostNode',
        },
        {
            data: {taskDispatcherId: 'graph_1'},
            id: 'graph_1-graph-bottom-ghost',
            position: {x: 0, y: 0},
            type: 'taskDispatcherBottomGhostNode',
        },
        {
            data: {componentName: 'mailchimp', workflowNodeName: 'task_0'},
            id: 'task_0',
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            data: {componentName: 'mailchimp', workflowNodeName: 'task_1'},
            id: 'task_1',
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            data: {componentName: 'mailchimp', workflowNodeName: 'task_2'},
            id: 'task_2',
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {
            data: {componentName: 'mailchimp', workflowNodeName: 'task_3'},
            id: 'task_3',
            position: {x: 0, y: 0},
            type: 'workflow',
        },
        {data: {}, id: 'graph_1-graph-node-4-placeholder-0', position: {x: 0, y: 0}, type: 'placeholder'},
    ];

    // Simulates the edges ELK was actually given — visual lane order, transition edges included —
    // which is exactly what getElkLayoutElements's fallback branch would hand to dagre verbatim.
    const visualOrderEdges = createGraphEdges(graphDispatcherNode, {orderLanesByVisualPosition: true});

    describe('realignGraphLaneHandlesToDeclarationOrder', () => {
        it('rewrites a graph lane handle tainted by visual ordering back to its declaration-order side', () => {
            const realignedEdges = realignGraphLaneHandlesToDeclarationOrder(graphNodes, visualOrderEdges);

            // task_1 (declared index 1) renders as the RIGHTMOST lane under visual order, but
            // must stay in the LEFT group under declaration order (left=[0,1], middle=2, right=[3]).
            const topGhostToTask1 = realignedEdges.find(
                (edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === 'task_1'
            );
            const task1ToBottomGhost = realignedEdges.find(
                (edge) => edge.source === 'task_1' && edge.target === 'graph_1-graph-bottom-ghost'
            );

            expect(topGhostToTask1?.sourceHandle).toBe('graph_1-graph-top-ghost-left');
            expect(task1ToBottomGhost?.targetHandle).toBe('graph_1-graph-bottom-ghost-left');
        });

        it('does not itself drop graphTransition overlay edges — that is the separate graphTransition filter in getLayoutElements, applied before this function runs', () => {
            const realignedEdges = realignGraphLaneHandlesToDeclarationOrder(graphNodes, visualOrderEdges);

            expect(realignedEdges.some((edge) => edge.type === 'graphTransition')).toBe(true);
        });

        it('returns edges unchanged when no graph dispatcher node is present', () => {
            const plainNodes: Node[] = [
                {data: {componentName: 'mailchimp'}, id: 'n0', position: {x: 0, y: 0}, type: 'workflow'},
            ];
            const plainEdges: Edge[] = [{id: 'n0=>n1', source: 'n0', target: 'n1', type: 'workflow'}];

            expect(realignGraphLaneHandlesToDeclarationOrder(plainNodes, plainEdges)).toBe(plainEdges);
        });
    });

    it('renders declaration-order lane handles end to end when dagre is handed visual-order edges (the ELK fallback shape)', async () => {
        const result = await getLayoutElements({
            canvasWidth: 1600,
            direction: 'TB',
            edges: visualOrderEdges,
            nodes: graphNodes,
        });

        const topGhostToTask1 = result.edges.find(
            (edge) => edge.source === 'graph_1-graph-top-ghost' && edge.target === 'task_1'
        );

        expect(topGhostToTask1?.sourceHandle).toBe('graph_1-graph-top-ghost-left');
        expect(result.edges.some((edge) => edge.type === 'graphTransition')).toBe(false);
    });
});
