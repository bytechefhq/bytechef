import {ALIGNED_SIDE_CASE_THRESHOLD} from '@/shared/constants';
import {Position} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import computeEdgeCorrectedCoordinates from './computeEdgeCorrectedCoordinates';

function makeParams(overrides: Record<string, unknown> = {}) {
    return {
        isHorizontal: false,
        isMiddleCaseEdge: false,
        sourcePosition: Position.Bottom,
        sourceX: 400,
        sourceY: 200,
        targetPosition: Position.Top,
        targetX: 400,
        targetY: 500,
        ...overrides,
    };
}

describe('computeEdgeCorrectedCoordinates', () => {
    describe('no corrections (regular edges)', () => {
        it('should pass through coordinates unchanged for a regular TB edge', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    sourceNodeType: 'workflow',
                    targetNodeType: 'workflow',
                })
            );

            expect(result).toEqual({
                correctedSourcePosition: Position.Bottom,
                correctedSourceX: 400,
                correctedSourceY: 200,
                correctedTargetPosition: Position.Top,
                correctedTargetX: 400,
                correctedTargetY: 500,
            });
        });

        it('should pass through coordinates unchanged for a regular LR edge', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'workflow',
                    sourcePosition: Position.Right,
                    targetNodeType: 'workflow',
                    targetPosition: Position.Left,
                })
            );

            expect(result).toEqual({
                correctedSourcePosition: Position.Right,
                correctedSourceX: 400,
                correctedSourceY: 200,
                correctedTargetPosition: Position.Left,
                correctedTargetX: 400,
                correctedTargetY: 500,
            });
        });
    });

    describe('middle-case corrections in TB mode', () => {
        it('should snap sourceX to targetX for middle-case edge from top ghost in TB', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isMiddleCaseEdge: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourceX: 410,
                    targetX: 400,
                })
            );

            expect(result.correctedSourceX).toBe(400);
            expect(result.correctedSourceY).toBe(200);
        });

        it('should snap targetX to sourceX for middle-case edge to bottom ghost in TB', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isMiddleCaseEdge: true,
                    sourceX: 400,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetX: 410,
                })
            );

            expect(result.correctedTargetX).toBe(400);
            expect(result.correctedTargetY).toBe(500);
        });

        it('should not correct sourceX when source is not a top ghost node in TB', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isMiddleCaseEdge: true,
                    sourceNodeType: 'workflow',
                    sourceX: 410,
                    targetX: 400,
                })
            );

            expect(result.correctedSourceX).toBe(410);
        });
    });

    describe('middle-case corrections in LR mode', () => {
        it('should snap sourceY to targetY for middle-case edge from top ghost in LR', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    isMiddleCaseEdge: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Right,
                    sourceY: 310,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourceX).toBe(400);
        });

        it('should snap targetY to sourceY for middle-case edge to bottom ghost in LR', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    isMiddleCaseEdge: true,
                    sourcePosition: Position.Right,
                    sourceY: 300,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetPosition: Position.Left,
                    targetY: 310,
                })
            );

            expect(result.correctedTargetY).toBe(300);
            expect(result.correctedTargetX).toBe(400);
        });
    });

    describe('aligned side-case corrections in LR mode (from top ghost)', () => {
        it('should correct sourceY and sourcePosition when Y diff is within threshold', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceX: 200,
                    sourceY: 307,
                    targetPosition: Position.Left,
                    targetX: 400,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
            expect(result.correctedSourceX).toBe(200);
        });

        it('should correct at exact threshold boundary', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceY: 300 + ALIGNED_SIDE_CASE_THRESHOLD,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
        });

        it('should not correct when Y diff exceeds threshold', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceY: 300 + ALIGNED_SIDE_CASE_THRESHOLD + 1,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300 + ALIGNED_SIDE_CASE_THRESHOLD + 1);
            expect(result.correctedSourcePosition).toBe(Position.Bottom);
        });

        it('should not correct when source is not a top ghost node', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'workflow',
                    sourcePosition: Position.Bottom,
                    sourceY: 307,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(307);
            expect(result.correctedSourcePosition).toBe(Position.Bottom);
        });

        it('should not correct in TB mode even when Y diff is small', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: false,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceY: 307,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(307);
            expect(result.correctedSourcePosition).toBe(Position.Bottom);
        });

        it('should not correct for middle-case edges even when Y diff is small', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    isMiddleCaseEdge: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Right,
                    sourceY: 307,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            // Middle-case has its own correction, not the aligned side-case one
            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
        });

        it('should handle negative Y diff (source above target)', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Top,
                    sourceY: 293,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
        });
    });

    describe('aligned side-case corrections in LR mode (to bottom ghost)', () => {
        it('should correct targetY and targetPosition when Y diff is within threshold', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourcePosition: Position.Right,
                    sourceX: 400,
                    sourceY: 300,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetPosition: Position.Top,
                    targetX: 600,
                    targetY: 307,
                })
            );

            expect(result.correctedTargetY).toBe(300);
            expect(result.correctedTargetPosition).toBe(Position.Left);
            expect(result.correctedTargetX).toBe(600);
        });

        it('should correct at exact threshold boundary for bottom ghost', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceY: 300,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetPosition: Position.Top,
                    targetY: 300 + ALIGNED_SIDE_CASE_THRESHOLD,
                })
            );

            expect(result.correctedTargetY).toBe(300);
            expect(result.correctedTargetPosition).toBe(Position.Left);
        });

        it('should not correct when Y diff exceeds threshold for bottom ghost', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceY: 300,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetPosition: Position.Top,
                    targetY: 300 + ALIGNED_SIDE_CASE_THRESHOLD + 1,
                })
            );

            expect(result.correctedTargetY).toBe(300 + ALIGNED_SIDE_CASE_THRESHOLD + 1);
            expect(result.correctedTargetPosition).toBe(Position.Top);
        });

        it('should not correct when target is not a bottom ghost node', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceY: 300,
                    targetNodeType: 'workflow',
                    targetPosition: Position.Top,
                    targetY: 307,
                })
            );

            expect(result.correctedTargetY).toBe(307);
            expect(result.correctedTargetPosition).toBe(Position.Top);
        });
    });

    describe('combined corrections (both source and target ghost)', () => {
        it('should correct both source and target when both are ghost nodes within threshold', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceX: 200,
                    sourceY: 305,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                    targetPosition: Position.Top,
                    targetX: 600,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
            expect(result.correctedTargetY).toBe(305);
            expect(result.correctedTargetPosition).toBe(Position.Left);
        });
    });

    describe('edge cases', () => {
        it('should handle zero Y difference', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    sourcePosition: Position.Bottom,
                    sourceY: 300,
                    targetPosition: Position.Left,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Right);
        });

        it('should not correct when node types are undefined', () => {
            const result = computeEdgeCorrectedCoordinates(
                makeParams({
                    isHorizontal: true,
                    sourcePosition: Position.Bottom,
                    sourceY: 307,
                    targetPosition: Position.Top,
                    targetY: 300,
                })
            );

            expect(result.correctedSourceY).toBe(307);
            expect(result.correctedTargetY).toBe(300);
            expect(result.correctedSourcePosition).toBe(Position.Bottom);
            expect(result.correctedTargetPosition).toBe(Position.Top);
        });
    });
});
