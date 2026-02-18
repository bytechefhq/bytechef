import {EDGE_STYLES} from '@/shared/constants';
import {BranchCaseType, NodeDataType} from '@/shared/types';
import {Node} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import createBranchEdges from '../utils/createBranchEdges';

function createBranchNode(
    id: string,
    defaultTasks: Array<{name: string; type?: string}> = [],
    cases: BranchCaseType[] = []
): Node {
    return {
        data: {
            componentName: 'branch',
            parameters: {
                cases,
                default: defaultTasks,
            },
        } as unknown as NodeDataType,
        id,
        position: {x: 0, y: 0},
        type: 'workflow',
    };
}

describe('createBranchEdges', () => {
    describe('isMiddleCase data propagation', () => {
        it('should add isMiddleCase data to middle case edges when case count is odd', () => {
            // 3 cases (default + 2 custom) => left=1, middle=1, right=1
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: []},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Find edges for the middle case (default is first, case_0 is middle for 3 cases)
            const middleCaseEdges = edges.filter(
                (edge) => edge.data && (edge.data as Record<string, unknown>).isMiddleCase === true
            );

            expect(middleCaseEdges.length).toBeGreaterThan(0);
        });

        it('should not add isMiddleCase data to left/right case edges', () => {
            // 3 cases => left=1, middle=1, right=1
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: []},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Left and right edges should NOT have isMiddleCase
            const nonMiddleCaseEdges = edges.filter(
                (edge) =>
                    edge.sourceHandle?.endsWith('-left') ||
                    edge.sourceHandle?.endsWith('-right') ||
                    edge.targetHandle?.endsWith('-left') ||
                    edge.targetHandle?.endsWith('-right')
            );

            nonMiddleCaseEdges.forEach((edge) => {
                expect(edge.data).toBeUndefined();
            });
        });

        it('should not have middle case when case count is even', () => {
            // 2 cases (default + 1 custom) => left=1, middle=null, right=1
            const branchNode = createBranchNode('branch_1', [], [{key: 'case_0', tasks: []}]);

            const edges = createBranchEdges(branchNode);

            const middleCaseEdges = edges.filter(
                (edge) => edge.data && (edge.data as Record<string, unknown>).isMiddleCase === true
            );

            expect(middleCaseEdges.length).toBe(0);
        });

        it('should use smoothstep edge type for middle case placeholder-to-bottom-ghost edge', () => {
            // 3 cases with empty middle case — placeholder edges should always use smoothstep
            // to avoid rendering a redundant "+" button (the placeholder node itself is the add button)
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: []},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Find the placeholder-to-bottom-ghost edge for the middle case
            const middlePlaceholderToBottomEdge = edges.find(
                (edge) =>
                    edge.data &&
                    (edge.data as Record<string, unknown>).isMiddleCase === true &&
                    edge.target === 'branch_1-branch-bottom-ghost' &&
                    edge.source.includes('placeholder')
            );

            expect(middlePlaceholderToBottomEdge).toBeDefined();
            expect(middlePlaceholderToBottomEdge!.type).toBe('smoothstep');
        });

        it('should use smoothstep edge type for non-middle case placeholder-to-bottom-ghost edges', () => {
            // 3 cases with empty left/right cases
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: []},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Find placeholder-to-bottom-ghost edges for left/right cases
            const nonMiddlePlaceholderToBottomEdges = edges.filter(
                (edge) =>
                    edge.target === 'branch_1-branch-bottom-ghost' &&
                    edge.source.includes('placeholder') &&
                    (!edge.data || !(edge.data as Record<string, unknown>).isMiddleCase)
            );

            nonMiddlePlaceholderToBottomEdges.forEach((edge) => {
                expect(edge.type).toBe('smoothstep');
            });
        });

        it('should propagate isMiddleCase to edges for middle case with tasks', () => {
            const branchNode = createBranchNode(
                'branch_1',
                [{name: 'defaultTask_1'}],
                [
                    {key: 'case_0', tasks: [{name: 'caseTask_1', type: 'caseTask/v1'}]},
                    {key: 'case_1', tasks: [{name: 'caseTask_2', type: 'caseTask/v1'}]},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Middle case task edges should have isMiddleCase
            const middleEdgeFromTopGhost = edges.find(
                (edge) =>
                    edge.data &&
                    (edge.data as Record<string, unknown>).isMiddleCase === true &&
                    edge.source === 'branch_1-branch-top-ghost'
            );

            expect(middleEdgeFromTopGhost).toBeDefined();

            const middleEdgeToBottomGhost = edges.find(
                (edge) =>
                    edge.data &&
                    (edge.data as Record<string, unknown>).isMiddleCase === true &&
                    edge.target === 'branch_1-branch-bottom-ghost'
            );

            expect(middleEdgeToBottomGhost).toBeDefined();
        });

        it('should propagate isMiddleCase when last task is a nested task dispatcher', () => {
            // 3 cases: default=left, case_0=middle, case_1=right
            // Put a loop task dispatcher in the middle case (case_0)
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: [{name: 'loop_1', type: 'loop/v1'}]},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // The edge from nested bottom ghost to branch bottom ghost should have isMiddleCase
            const nestedGhostEdge = edges.find(
                (edge) => edge.source === 'loop_1-loop-bottom-ghost' && edge.target === 'branch_1-branch-bottom-ghost'
            );

            expect(nestedGhostEdge).toBeDefined();
            expect(nestedGhostEdge!.data).toEqual({isMiddleCase: true});
        });
    });

    describe('targetHandle for nested task dispatcher in bottom ghost edges', () => {
        it('should use -top handle when nested task dispatcher is in the middle case', () => {
            // 3 cases: default=left, case_0=middle (with condition), case_1=right
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: [{name: 'condition_3', type: 'condition/v1'}]},
                    {key: 'case_1', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            const nestedGhostEdge = edges.find(
                (edge) =>
                    edge.source === 'condition_3-condition-bottom-ghost' &&
                    edge.target === 'branch_1-branch-bottom-ghost'
            );

            expect(nestedGhostEdge).toBeDefined();
            expect(nestedGhostEdge!.targetHandle).toBe('branch_1-branch-bottom-ghost-top');
        });

        it('should use -left handle when nested task dispatcher is in a left case', () => {
            // 2 cases (even): default=left, case_0=right
            const branchNode = createBranchNode(
                'branch_1',
                [{name: 'loop_1', type: 'loop/v1'}],
                [{key: 'case_0', tasks: []}]
            );

            const edges = createBranchEdges(branchNode);

            const nestedGhostEdge = edges.find(
                (edge) => edge.source === 'loop_1-loop-bottom-ghost' && edge.target === 'branch_1-branch-bottom-ghost'
            );

            expect(nestedGhostEdge).toBeDefined();
            expect(nestedGhostEdge!.targetHandle).toBe('branch_1-branch-bottom-ghost-left');
        });

        it('should use -right handle when nested task dispatcher is in a right case', () => {
            // 2 cases (even): default=left, case_0=right (with branch)
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [{key: 'case_0', tasks: [{name: 'branch_2', type: 'branch/v1'}]}]
            );

            const edges = createBranchEdges(branchNode);

            const nestedGhostEdge = edges.find(
                (edge) =>
                    edge.source === 'branch_2-branch-bottom-ghost' && edge.target === 'branch_1-branch-bottom-ghost'
            );

            expect(nestedGhostEdge).toBeDefined();
            expect(nestedGhostEdge!.targetHandle).toBe('branch_1-branch-bottom-ghost-right');
        });
    });

    describe('base structure', () => {
        it('should create edge from branch node to top ghost', () => {
            const branchNode = createBranchNode('branch_1', [], [{key: 'case_0', tasks: []}]);

            const edges = createBranchEdges(branchNode);

            const branchToTopGhost = edges.find(
                (edge) => edge.source === 'branch_1' && edge.target === 'branch_1-branch-top-ghost'
            );

            expect(branchToTopGhost).toBeDefined();
            expect(branchToTopGhost!.type).toBe('smoothstep');
            expect(branchToTopGhost!.style).toEqual(EDGE_STYLES);
        });

        it('should create placeholder edges for empty cases', () => {
            const branchNode = createBranchNode('branch_1', [], [{key: 'case_0', tasks: []}]);

            const edges = createBranchEdges(branchNode);

            // Should have edges to/from placeholders for both cases (default and case_0)
            const placeholderEdges = edges.filter(
                (edge) => edge.source.includes('placeholder') || edge.target.includes('placeholder')
            );

            expect(placeholderEdges.length).toBeGreaterThan(0);
        });

        it('should create edges connecting tasks within a case', () => {
            const branchNode = createBranchNode('branch_1', [{name: 'task_1'}, {name: 'task_2'}, {name: 'task_3'}]);

            const edges = createBranchEdges(branchNode);

            const taskToTaskEdge = edges.find((edge) => edge.source === 'task_1' && edge.target === 'task_2');

            expect(taskToTaskEdge).toBeDefined();
            expect(taskToTaskEdge!.type).toBe('workflow');

            const taskToTaskEdge2 = edges.find((edge) => edge.source === 'task_2' && edge.target === 'task_3');

            expect(taskToTaskEdge2).toBeDefined();
        });
    });

    describe('case distribution', () => {
        it('should distribute 1 case as left=0, middle=1, right=0', () => {
            // Only default case exists, but code adds case_0 for empty branch
            const branchNode = createBranchNode('branch_1');

            const edges = createBranchEdges(branchNode);

            // Should have edges (at least base structure + case edges)
            expect(edges.length).toBeGreaterThan(0);
        });

        it('should distribute 5 cases evenly with a middle case', () => {
            // 5 cases: left=2, middle=1, right=2
            const branchNode = createBranchNode(
                'branch_1',
                [],
                [
                    {key: 'case_0', tasks: []},
                    {key: 'case_1', tasks: []},
                    {key: 'case_2', tasks: []},
                    {key: 'case_3', tasks: []},
                ]
            );

            const edges = createBranchEdges(branchNode);

            // Should have middle case edges
            const middleCaseEdges = edges.filter(
                (edge) => edge.data && (edge.data as Record<string, unknown>).isMiddleCase === true
            );

            expect(middleCaseEdges.length).toBeGreaterThan(0);

            // Should have left and right case edges
            const leftEdges = edges.filter((edge) => edge.sourceHandle?.endsWith('-left'));
            const rightEdges = edges.filter((edge) => edge.sourceHandle?.endsWith('-right'));

            expect(leftEdges.length).toBeGreaterThan(0);
            expect(rightEdges.length).toBeGreaterThan(0);
        });
    });
});
