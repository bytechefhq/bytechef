import {WorkflowTask} from '@/shared/middleware/platform/configuration';
import {NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createConditionEdges, {getConditionBranchSide, hasTaskInConditionBranches} from '../utils/createConditionEdges';

function makeConditionNode(conditionId: string, caseTrue?: WorkflowTask[], caseFalse?: WorkflowTask[]): Node {
    return {
        data: {
            componentName: 'condition',
            parameters: {
                ...(caseFalse !== undefined ? {caseFalse} : {}),
                ...(caseTrue !== undefined ? {caseTrue} : {}),
            },
        } as NodeDataType,
        id: conditionId,
        position: {x: 0, y: 0},
    };
}

function makeTaskNode(taskId: string): Node {
    return {
        data: {componentName: taskId.split('_')[0]} as NodeDataType,
        id: taskId,
        position: {x: 0, y: 0},
    };
}

/**
 * Returns the indices of the first edge targeting each branch's first node,
 * measured from the top-ghost source edges only. A lower index means dagre
 * will place the target further to the left.
 */
function findBranchStartIndices(
    edges: {source: string; target: string}[],
    conditionId: string,
    leftTargetId: string,
    rightTargetId: string
): {leftIndex: number; rightIndex: number} {
    const topGhostId = `${conditionId}-condition-top-ghost`;
    const topGhostOutEdges = edges.filter((edge) => edge.source === topGhostId);

    const leftIndex = topGhostOutEdges.findIndex((edge) => edge.target === leftTargetId);
    const rightIndex = topGhostOutEdges.findIndex((edge) => edge.target === rightTargetId);

    return {leftIndex, rightIndex};
}

describe('createConditionEdges', () => {
    describe('edge ordering for dagre left/right placement', () => {
        it('should insert left-branch content edges before right-branch placeholder edges', () => {
            const conditionId = 'condition_1';
            const trueTask = {name: 'mistral_1', type: 'mistral/v1'};

            const conditionNode = makeConditionNode(conditionId, [trueTask], undefined);
            const allNodes = [conditionNode, makeTaskNode('mistral_1')];

            const edges = createConditionEdges(conditionNode, allNodes);

            const rightPlaceholderId = `${conditionId}-condition-right-placeholder-0`;
            const {leftIndex, rightIndex} = findBranchStartIndices(edges, conditionId, 'mistral_1', rightPlaceholderId);

            expect(leftIndex).toBeGreaterThanOrEqual(0);
            expect(rightIndex).toBeGreaterThanOrEqual(0);
            expect(leftIndex).toBeLessThan(rightIndex);
        });

        it('should insert left-branch placeholder edges before right-branch content edges', () => {
            const conditionId = 'condition_1';
            const falseTask = {name: 'logger_1', type: 'logger/v1'};

            const conditionNode = makeConditionNode(conditionId, undefined, [falseTask]);
            const allNodes = [conditionNode, makeTaskNode('logger_1')];

            const edges = createConditionEdges(conditionNode, allNodes);

            const leftPlaceholderId = `${conditionId}-condition-left-placeholder-0`;
            const {leftIndex, rightIndex} = findBranchStartIndices(edges, conditionId, leftPlaceholderId, 'logger_1');

            expect(leftIndex).toBeGreaterThanOrEqual(0);
            expect(rightIndex).toBeGreaterThanOrEqual(0);
            expect(leftIndex).toBeLessThan(rightIndex);
        });

        it('should insert left-branch content edges before right-branch content edges', () => {
            const conditionId = 'condition_1';
            const trueTask = {name: 'logger_1', type: 'logger/v1'};
            const falseTask = {name: 'terminate_1', type: 'terminate/v1'};

            const conditionNode = makeConditionNode(conditionId, [trueTask], [falseTask]);
            const allNodes = [conditionNode, makeTaskNode('logger_1'), makeTaskNode('terminate_1')];

            const edges = createConditionEdges(conditionNode, allNodes);

            const {leftIndex, rightIndex} = findBranchStartIndices(edges, conditionId, 'logger_1', 'terminate_1');

            expect(leftIndex).toBeGreaterThanOrEqual(0);
            expect(rightIndex).toBeGreaterThanOrEqual(0);
            expect(leftIndex).toBeLessThan(rightIndex);
        });

        it('should insert left placeholder before right placeholder when both branches are empty', () => {
            const conditionId = 'condition_1';

            const conditionNode = makeConditionNode(conditionId, undefined, undefined);
            const allNodes = [conditionNode];

            const edges = createConditionEdges(conditionNode, allNodes);

            const leftPlaceholderId = `${conditionId}-condition-left-placeholder-0`;
            const rightPlaceholderId = `${conditionId}-condition-right-placeholder-0`;
            const {leftIndex, rightIndex} = findBranchStartIndices(
                edges,
                conditionId,
                leftPlaceholderId,
                rightPlaceholderId
            );

            expect(leftIndex).toBeGreaterThanOrEqual(0);
            expect(rightIndex).toBeGreaterThanOrEqual(0);
            expect(leftIndex).toBeLessThan(rightIndex);
        });
    });

    describe('edge structure', () => {
        it('should always start with condition-to-top-ghost edge', () => {
            const conditionId = 'condition_1';
            const conditionNode = makeConditionNode(conditionId, undefined, undefined);

            const edges = createConditionEdges(conditionNode, [conditionNode]);

            expect(edges[0]).toMatchObject({
                source: conditionId,
                target: `${conditionId}-condition-top-ghost`,
            });
        });

        it('should create placeholder edges for empty branches', () => {
            const conditionId = 'condition_1';
            const conditionNode = makeConditionNode(conditionId, undefined, undefined);

            const edges = createConditionEdges(conditionNode, [conditionNode]);

            const leftPlaceholderId = `${conditionId}-condition-left-placeholder-0`;
            const rightPlaceholderId = `${conditionId}-condition-right-placeholder-0`;
            const bottomGhostId = `${conditionId}-condition-bottom-ghost`;

            expect(edges).toContainEqual(expect.objectContaining({source: leftPlaceholderId, target: bottomGhostId}));
            expect(edges).toContainEqual(expect.objectContaining({source: rightPlaceholderId, target: bottomGhostId}));
        });

        it('should create content edges for populated branches', () => {
            const conditionId = 'condition_1';
            const trueTask = {name: 'task_1', type: 'task/v1'};
            const falseTask = {name: 'task_2', type: 'task/v1'};

            const conditionNode = makeConditionNode(conditionId, [trueTask], [falseTask]);
            const allNodes = [conditionNode, makeTaskNode('task_1'), makeTaskNode('task_2')];

            const edges = createConditionEdges(conditionNode, allNodes);

            const topGhostId = `${conditionId}-condition-top-ghost`;
            const bottomGhostId = `${conditionId}-condition-bottom-ghost`;

            expect(edges).toContainEqual(
                expect.objectContaining({
                    source: topGhostId,
                    sourceHandle: `${topGhostId}-left`,
                    target: 'task_1',
                })
            );
            expect(edges).toContainEqual(
                expect.objectContaining({
                    source: topGhostId,
                    sourceHandle: `${topGhostId}-right`,
                    target: 'task_2',
                })
            );
            expect(edges).toContainEqual(expect.objectContaining({source: 'task_1', target: bottomGhostId}));
            expect(edges).toContainEqual(expect.objectContaining({source: 'task_2', target: bottomGhostId}));
        });

        it('should create sequential edges for multi-task branches', () => {
            const conditionId = 'condition_1';
            const trueTasks = [
                {name: 'task_1', type: 'task/v1'},
                {name: 'task_2', type: 'task/v1'},
                {name: 'task_3', type: 'task/v1'},
            ];

            const conditionNode = makeConditionNode(conditionId, trueTasks, undefined);
            const allNodes = [conditionNode, makeTaskNode('task_1'), makeTaskNode('task_2'), makeTaskNode('task_3')];

            const edges = createConditionEdges(conditionNode, allNodes);

            expect(edges).toContainEqual(expect.objectContaining({source: 'task_1', target: 'task_2'}));
            expect(edges).toContainEqual(expect.objectContaining({source: 'task_2', target: 'task_3'}));
        });
    });
    describe('hasTaskInConditionBranches', () => {
        it('should handle non-array caseTrue parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: [{name: 'task1'}, {name: 'task2'}],
                        caseTrue: 'not-an-array', // This should not cause an error
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'task1', tasks);
            }).not.toThrow();

            // Should return true for task1 (in caseFalse array)
            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);

            // Should return false for non-existent task
            expect(hasTaskInConditionBranches('test-condition', 'non-existent', tasks)).toBe(false);
        });

        it('should handle non-array caseFalse parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: {not: 'an-array'}, // This should not cause an error
                        caseTrue: [{name: 'task1'}, {name: 'task2'}],
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'task1', tasks);
            }).not.toThrow();

            // Should return true for task1 (in caseTrue array)
            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);
        });

        it('should handle both non-array parameters without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: {not: 'an-array'},
                        caseTrue: 'not-an-array',
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                hasTaskInConditionBranches('test-condition', 'any-task', tasks);
            }).not.toThrow();

            // Should return false since no arrays contain tasks
            expect(hasTaskInConditionBranches('test-condition', 'any-task', tasks)).toBe(false);
        });

        it('should work correctly with valid array parameters', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'test-condition',
                    parameters: {
                        caseFalse: [{name: 'task3'}, {name: 'task4'}],
                        caseTrue: [{name: 'task1'}, {name: 'task2'}],
                    },
                    type: 'condition/v1',
                },
            ];

            expect(hasTaskInConditionBranches('test-condition', 'task1', tasks)).toBe(true);
            expect(hasTaskInConditionBranches('test-condition', 'task3', tasks)).toBe(true);
            expect(hasTaskInConditionBranches('test-condition', 'non-existent', tasks)).toBe(false);
        });
    });

    describe('getConditionBranchSide', () => {
        it('should handle non-array caseTrue parameter without throwing error', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'parent-condition',
                    parameters: {
                        caseFalse: [{name: 'task1'}],
                        caseTrue: 'not-an-array', // This should not cause an error
                    },
                    type: 'condition/v1',
                },
            ];

            // This should not throw an error
            expect(() => {
                getConditionBranchSide('child-condition', tasks, 'parent-condition');
            }).not.toThrow();

            // Should return 'right' since caseTrue is not an array
            expect(getConditionBranchSide('child-condition', tasks, 'parent-condition')).toBe('right');
        });

        it('should work correctly with valid array parameters', () => {
            const tasks: WorkflowTask[] = [
                {
                    name: 'parent-condition',
                    parameters: {
                        caseFalse: [{name: 'other-task'}],
                        caseTrue: [{name: 'child-condition'}],
                    },
                    type: 'condition/v1',
                },
            ];

            // Should return 'left' since child-condition is in caseTrue
            expect(getConditionBranchSide('child-condition', tasks, 'parent-condition')).toBe('left');

            // Should return 'right' since other-task is not in caseTrue
            expect(getConditionBranchSide('other-task', tasks, 'parent-condition')).toBe('right');
        });

        it('should return right when parent condition is not found', () => {
            const tasks: WorkflowTask[] = [];

            expect(getConditionBranchSide('child-condition', tasks, 'non-existent-parent')).toBe('right');
        });
    });
});
