import {
    CLUSTER_ELEMENT_NODE_WIDTH,
    CLUSTER_ROOT_NODE_WIDTH,
    GRAPH_FRAME_NODE_TYPE,
    GRAPH_START_EDGE_TYPE,
    GRAPH_START_NODE_TYPE,
    GRAPH_TRANSITION_EDGE_TYPE,
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

import {calculateNodeWidth} from '../../cluster-element-editor/utils/clusterElementsUtils';
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

    // These three pin the graph-route BACKSTOP, which is reached only by direct callers — the
    // editor's own path never routes a frame's transitions through this function (see the
    // function's own comment). A graph member (see createGraphEdges) legitimately carries TWO
    // outgoing edges: the structural wiring of its own subtree, and a `graphTransition` route to
    // another member sharing the same source. The "one edge per source" collapse must not treat
    // those as a competing structural fan-out and keep only one.
    it('should keep a graphTransition edge alongside its source member structural edge', () => {
        const nodes: Node[] = [makeWorkflowNode('taskA'), makeWorkflowNode('taskB'), makeWorkflowNode('taskC')];

        const edges: Edge[] = [
            {id: 'taskA=>taskB', source: 'taskA', target: 'taskB', type: 'workflow'},
            {
                id: 'graph_1-transition-0-1',
                source: 'taskA',
                sourceHandle: 'taskA-graph-transition-source',
                target: 'taskC',
                targetHandle: 'taskC-graph-transition-target',
                type: GRAPH_TRANSITION_EDGE_TYPE,
            },
        ];

        const result = filterAndDedupeLayoutEdges(nodes, edges);

        const structuralEdge = result.find((edge) => edge.type === 'workflow');
        const transitionEdge = result.find((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE);

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
            type: GRAPH_TRANSITION_EDGE_TYPE,
        };

        const result = filterAndDedupeLayoutEdges(nodes, [duplicateTransitionEdge, {...duplicateTransitionEdge}]);

        expect(result.filter((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toHaveLength(1);
    });

    it('should still drop a graphTransition edge that references a node no longer in the layout', () => {
        const nodes: Node[] = [makeWorkflowNode('taskA')];

        const edges: Edge[] = [
            {
                id: 'graph_1-transition-0-1',
                source: 'taskA',
                target: 'deletedTask',
                type: GRAPH_TRANSITION_EDGE_TYPE,
            },
        ];

        expect(filterAndDedupeLayoutEdges(nodes, edges)).toEqual([]);
    });
});

describe('getLayoutElements (dagre) with graphTransition edges', () => {
    // `createGraphEdges` emits a `graphTransition` edge per declared transition on EVERY engine,
    // and `layoutGraphFrames` strips them before the outer layout runs — so dagre should never
    // see one from `useLayout`. These pin the backstop filter that keeps it safe when something
    // hands them over anyway: a direct caller, or getElkLayoutElements's error-fallback branch,
    // which re-invokes getLayoutElements with the SAME edges array ELK was given. Mirrors the
    // ELK-side pin in elkLayoutUtils.test.ts.
    const makeWorkflowNode = (id: string): Node => ({
        data: {componentName: 'mailchimp', workflowNodeName: id},
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    });

    const nodes: Node[] = [makeWorkflowNode('n0'), makeWorkflowNode('n1')];

    const cyclicTransitionEdges: Edge[] = [
        {id: 'graph_1-transition-0-1', source: 'n0', target: 'n1', type: GRAPH_TRANSITION_EDGE_TYPE},
        {id: 'graph_1-transition-1-0', source: 'n1', target: 'n0', type: GRAPH_TRANSITION_EDGE_TYPE},
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

        expect(result.edges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(false);
    });
});

describe('getLayoutElements (dagre) with graph edges', () => {
    // A graph dispatcher's members live free-form inside its `graphFrame`, and every route
    // between them is a `graphTransition` edge built straight off `parameters.transitions` — no
    // lane columns, no per-member ghost-bar handles. The surrounding chain still addresses the
    // container only through its top/bottom ghost bars. `layoutGraphFrames` strips a frame's own
    // routes before the outer layout runs; this pins dagre's backstop filter for direct callers.
    const members = [
        {name: 'task_0', type: 'task/v1'},
        {name: 'task_1', type: 'task/v1'},
    ];

    const graphDispatcherNode: Node = {
        data: {
            componentName: 'graph',
            parameters: {nodes: members, startNode: 'task_0', transitions: [{from: 'task_0', to: 'task_1'}]},
            taskDispatcher: true,
        },
        id: 'graph_1',
        position: {x: 0, y: 0},
        type: 'workflow',
    };

    const graphNodes: Node[] = [
        graphDispatcherNode,
        {
            data: {graphFrame: {graphId: 'graph_1', height: 200, width: 320}, taskDispatcherId: 'graph_1'},
            id: 'graph_1-graph-frame',
            position: {x: 0, y: 0},
            type: GRAPH_FRAME_NODE_TYPE,
        },
        {
            data: {graphStart: {graphId: 'graph_1'}, taskDispatcherId: 'graph_1'},
            id: 'graph_1-graph-start',
            position: {x: 0, y: 0},
            type: GRAPH_START_NODE_TYPE,
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
    ];

    const graphEdges = createGraphEdges(graphDispatcherNode);

    it('drops graphTransition edges and keeps the container chain end to end via getLayoutElements', async () => {
        expect(graphEdges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(true);

        const result = await getLayoutElements({
            canvasWidth: 1600,
            direction: 'TB',
            edges: graphEdges,
            nodes: graphNodes,
        });

        const graphToFrame = result.edges.find(
            (edge) => edge.source === 'graph_1' && edge.target === 'graph_1-graph-frame'
        );

        expect(graphToFrame?.targetHandle).toBe('graph_1-graph-frame-top');
        expect(result.edges.some((edge) => edge.type === GRAPH_TRANSITION_EDGE_TYPE)).toBe(false);
        expect(result.edges.some((edge) => edge.type === GRAPH_START_EDGE_TYPE)).toBe(false);
    });

    it('lays the frame out inside the container chain at its own size', async () => {
        // `data.graphFrame` is what the layout pre-pass writes and what GraphFrameNode paints, so
        // it has to be dagre's footprint AND its placement box — a frame positioned like an
        // ordinary node would paint half its height into the next rank.
        const framedNodes = graphNodes.map((node) =>
            node.id === 'graph_1-graph-frame'
                ? {
                      ...node,
                      data: {...node.data, graphFrame: {graphId: 'graph_1', height: 400, width: 560}},
                      height: 400,
                      width: 560,
                  }
                : node
        );

        const result = await getLayoutElements({
            canvasWidth: 1600,
            direction: 'TB',
            edges: graphEdges,
            nodes: framedNodes,
        });

        const positionOf = (id: string) => result.nodes.find((node) => node.id === id)!.position;

        const framePosition = positionOf('graph_1-graph-frame');

        // Centred on the dispatcher's chain axis (a node's handles sit 36px past its position).
        expect(Math.abs(framePosition.x + 560 / 2 - (positionOf('graph_1').x + 36))).toBeLessThanOrEqual(1);

        // The whole box sits between the two ghost bars rather than overrunning them.
        expect(framePosition.y).toBeGreaterThan(positionOf('graph_1').y);
    });

    it('places the frame the same way with the axes swapped in LR', async () => {
        const framedNodes = graphNodes.map((node) =>
            node.id === 'graph_1-graph-frame'
                ? {
                      ...node,
                      data: {...node.data, graphFrame: {graphId: 'graph_1', height: 400, width: 560}},
                      height: 400,
                      width: 560,
                  }
                : node
        );

        const result = await getLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1600,
            direction: 'LR',
            edges: graphEdges,
            nodes: framedNodes,
        });

        const positionOf = (id: string) => result.nodes.find((node) => node.id === id)!.position;

        const framePosition = positionOf('graph_1-graph-frame');

        expect(Math.abs(framePosition.y + 400 / 2 - (positionOf('graph_1').y + 36))).toBeLessThanOrEqual(1);

        expect(framePosition.x).toBeGreaterThan(positionOf('graph_1').x);
    });
});

describe('getClusterElementsLayoutElements cross-subtree overlap', () => {
    const canvasWidth = 1200;
    const childBaseY = 160 + PLACEHOLDER_NODE_HEIGHT + NODE_HEIGHT / 4;

    /**
     * Two nested cluster roots side by side (a Task Tool and an Approval Gate), each with a child of its own.
     * The children have different parents, so the per-parent overlap pass never compares them and they used to
     * render on top of each other.
     */
    function buildTwoNestedRootsWithChildren(): Node[] {
        const nestedRoot = (nodeId: string): Node => ({
            data: {
                clusterElementType: 'tools',
                clusterElementTypeIndex: 0,
                clusterElementTypesCount: 2,
                isNestedClusterRoot: true,
                metadata: {},
                parentClusterRootElementsTypeCount: 1,
            },
            id: nodeId,
            parentId: 'root',
            position: {x: 0, y: 0},
            type: 'workflow',
        });

        const grandChild = (nodeId: string, parentId: string): Node => ({
            data: {
                clusterElementType: 'tools',
                clusterElementTypeIndex: 0,
                isNestedClusterRoot: false,
                metadata: {},
                parentClusterRootElementsTypeCount: 2,
            },
            id: nodeId,
            parentId,
            position: {x: 0, y: 0},
            type: 'workflow',
        });

        return [
            {
                data: {clusterElementTypesCount: 1, clusterElements: {tools: []}},
                id: 'root',
                position: {x: 0, y: 0},
                type: 'clusterRoot',
            },
            nestedRoot('taskTool'),
            nestedRoot('approvalGateTool'),
            grandChild('subagentTool', 'taskTool'),
            grandChild('gatedTool', 'approvalGateTool'),
        ];
    }

    it('separates children that belong to different nested roots', () => {
        const result = getClusterElementsLayoutElements({
            canvasHeight: 800,
            canvasWidth,
            edges: [],
            nodes: buildTwoNestedRootsWithChildren(),
        });

        const absoluteX = (nodeId: string): number => {
            let node = result.nodes.find((candidate) => candidate.id === nodeId);
            let x = 0;

            while (node) {
                x += node.position.x;

                node = node.parentId ? result.nodes.find((candidate) => candidate.id === node!.parentId) : undefined;
            }

            return x;
        };

        const subagentX = absoluteX('subagentTool');
        const gatedToolX = absoluteX('gatedTool');

        const leftX = Math.min(subagentX, gatedToolX);
        const rightX = Math.max(subagentX, gatedToolX);

        expect(rightX - leftX).toBeGreaterThanOrEqual(CLUSTER_ELEMENT_NODE_WIDTH);
    });

    it('keeps both grandchildren on the same row', () => {
        const result = getClusterElementsLayoutElements({
            canvasHeight: 800,
            canvasWidth,
            edges: [],
            nodes: buildTwoNestedRootsWithChildren(),
        });

        const subagent = result.nodes.find((node) => node.id === 'subagentTool');
        const gatedTool = result.nodes.find((node) => node.id === 'gatedTool');

        expect(subagent!.position.y).toBe(childBaseY);
        expect(gatedTool!.position.y).toBe(childBaseY);
    });
});

describe('getClusterElementsLayoutElements sibling spacing after a cross-subtree shift', () => {
    /**
     * The shape reported from a real AI Agent: a chat-memory subtree whose own child is a nested cluster root,
     * next to a RAG subtree with several nested cluster roots of its own. Clearing the chat-memory grandchild
     * shifts the RAG subtree's first child on its own, which used to leave it exactly stacked on the sibling
     * behind it because the cross-subtree sweep skipped pairs sharing a parent.
     */
    function buildAgentWithNeighbouringSubtrees(): Node[] {
        const element = (
            id: string,
            parentId: string,
            clusterElementType: string,
            clusterElementTypeIndex: number,
            parentClusterRootElementsTypeCount: number,
            ownTypes?: number
        ): Node => ({
            data: {
                clusterElementType,
                clusterElementTypeIndex,
                ...(ownTypes ? {clusterElementTypesCount: ownTypes} : {}),
                isNestedClusterRoot: !!ownTypes,
                metadata: {},
                parentClusterRootElementsTypeCount,
            },
            id,
            parentId,
            position: {x: 0, y: 0},
            type: 'workflow',
        });

        return [
            {
                data: {clusterElementTypesCount: 5, clusterElements: {chatMemory: {}, rag: []}},
                id: 'aiAgent_2',
                position: {x: 0, y: 0},
                type: 'workflow',
            },
            element('anthropic_5', 'aiAgent_2', 'model', 0, 5),
            element('vectorStoreChatMemory_2', 'aiAgent_2', 'chatMemory', 1, 5, 1),
            element('pinecone_3', 'vectorStoreChatMemory_2', 'vectorStore', 0, 1, 1),
            element('modularRag_2', 'aiAgent_2', 'rag', 2, 5, 5),
            element('documentJoiner_2', 'modularRag_2', 'documentJoiner', 0, 5),
            element('vectorStoreDocumentRetriever_2', 'modularRag_2', 'documentRetriever', 1, 5, 1),
            element('queryAugmenter_2', 'modularRag_2', 'queryAugmenter', 2, 5, 1),
            element('queryExpander_2', 'modularRag_2', 'queryExpander', 3, 5, 1),
        ];
    }

    function layOutAgent() {
        const result = getClusterElementsLayoutElements({
            canvasHeight: 900,
            canvasWidth: 1900,
            edges: [],
            nodes: buildAgentWithNeighbouringSubtrees(),
        });

        const absolutePoint = (nodeId: string): {x: number; y: number} => {
            let node = result.nodes.find((candidate) => candidate.id === nodeId);
            let x = 0;
            let y = 0;

            while (node) {
                x += node.position.x;
                y += node.position.y;

                node = node.parentId ? result.nodes.find((candidate) => candidate.id === node!.parentId) : undefined;
            }

            return {x, y};
        };

        const nodeWidth = (nodeId: string): number => {
            const node = result.nodes.find((candidate) => candidate.id === nodeId)!;

            return node.data.clusterElementTypesCount
                ? calculateNodeWidth(node.data.clusterElementTypesCount as number) || ROOT_CLUSTER_WIDTH
                : CLUSTER_ELEMENT_NODE_WIDTH;
        };

        return {absolutePoint, nodeWidth, result};
    }

    it('does not stack two children of the same cluster root on top of each other', () => {
        const {absolutePoint, nodeWidth} = layOutAgent();

        const joiner = {id: 'documentJoiner_2', x: absolutePoint('documentJoiner_2').x};
        const retriever = {id: 'vectorStoreDocumentRetriever_2', x: absolutePoint('vectorStoreDocumentRetriever_2').x};

        const [left, right] = joiner.x <= retriever.x ? [joiner, retriever] : [retriever, joiner];

        expect(right.x).toBeGreaterThanOrEqual(left.x + nodeWidth(left.id));
    });

    it('leaves no overlapping pair anywhere on the canvas', () => {
        const {absolutePoint, nodeWidth, result} = layOutAgent();

        const rows = new Map<number, Array<{id: string; x: number}>>();

        for (const node of result.nodes) {
            const {x, y} = absolutePoint(node.id);
            const row = Math.round(y);

            if (!rows.has(row)) {
                rows.set(row, []);
            }

            rows.get(row)!.push({id: node.id, x});
        }

        const overlaps: string[] = [];

        for (const rowEntries of rows.values()) {
            const sorted = [...rowEntries].sort((entryA, entryB) => entryA.x - entryB.x);

            for (let index = 1; index < sorted.length; index++) {
                const previous = sorted[index - 1];

                if (sorted[index].x < previous.x + nodeWidth(previous.id)) {
                    overlaps.push(`${previous.id} / ${sorted[index].id}`);
                }
            }
        }

        expect(overlaps).toEqual([]);
    });
});
