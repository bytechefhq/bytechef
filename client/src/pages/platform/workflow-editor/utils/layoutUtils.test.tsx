import {CLUSTER_ELEMENT_NODE_WIDTH, NODE_HEIGHT, PLACEHOLDER_NODE_HEIGHT, ROOT_CLUSTER_WIDTH} from '@/shared/constants';
import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {
    BranchChildTasksType,
    ConditionChildTasksType,
    EachChildTasksType,
    ForkJoinChildTasksType,
    LoopChildTasksType,
    MapChildTasksType,
    OnErrorChildTasksType,
    ParallelChildTasksType,
} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {
    CLUSTER_ELEMENT_GAP,
    CLUSTER_ELEMENT_LABEL_PADDING,
    CLUSTER_ELEMENT_OVERLAP_PADDING,
    CLUSTER_ROOT_GAP,
    calculateNodeHeight,
    collectTaskDispatcherData,
    getClusterElementsLayoutElements,
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
