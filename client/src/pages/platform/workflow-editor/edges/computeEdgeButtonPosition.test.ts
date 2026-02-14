import {describe, expect, it} from 'vitest';

import computeEdgeButtonPosition from './computeEdgeButtonPosition';

const EDGE_CENTER = {x: 500, y: 500};

function makeParams(overrides: Record<string, unknown> = {}) {
    return {
        correctedSourceX: 100,
        correctedSourceY: 100,
        correctedTargetX: 100,
        correctedTargetY: 300,
        edgeCenterX: EDGE_CENTER.x,
        edgeCenterY: EDGE_CENTER.y,
        isHorizontal: false,
        ...overrides,
    };
}

describe('computeEdgeButtonPosition', () => {
    describe('main-axis edges', () => {
        it('should return edge center for a vertical main-axis edge in TB mode', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceY: 100,
                    correctedTargetY: 300,
                })
            );

            expect(result).toEqual(EDGE_CENTER);
        });

        it('should return edge center for a horizontal main-axis edge in LR mode', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 100,
                    correctedTargetX: 400,
                    isHorizontal: true,
                })
            );

            expect(result).toEqual(EDGE_CENTER);
        });
    });

    describe('bottom ghost source edges', () => {
        it('should return edge center for bottom ghost source in TB mode (straight down)', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 540,
                    correctedSourceY: 1080,
                    correctedTargetX: 540,
                    correctedTargetY: 1200,
                    sourceNodeType: 'taskDispatcherBottomGhostNode',
                })
            );

            expect(result).toEqual(EDGE_CENTER);
        });

        it('should return edge center for bottom ghost source in TB mode when target is far to the side', () => {
            // This is the bug scenario: loop bottom ghost at x=540, condition_1 dragged to x=1958
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 540,
                    correctedSourceY: 1080,
                    correctedTargetX: 1958,
                    correctedTargetY: 644,
                    edgeCenterX: 1207,
                    edgeCenterY: 861,
                    sourceNodeType: 'taskDispatcherBottomGhostNode',
                })
            );

            expect(result).toEqual({x: 1207, y: 861});
        });

        it('should return edge center for bottom ghost source in LR mode', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 800,
                    correctedSourceY: 300,
                    correctedTargetX: 1500,
                    correctedTargetY: 100,
                    isHorizontal: true,
                    sourceNodeType: 'taskDispatcherBottomGhostNode',
                })
            );

            expect(result).toEqual(EDGE_CENTER);
        });
    });

    describe('top ghost source edges (branch)', () => {
        it('should use custom positioning for branch top ghost in TB mode', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 600,
                    correctedSourceY: 200,
                    correctedTargetX: 400,
                    correctedTargetY: 400,
                    sourceNodeTaskDispatcherId: 'branch_1',
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    targetNodeType: 'workflow',
                })
            );

            // TB mode: posX = correctedTargetX, posY = midpoint + 15 (branch top ghost to workflow)
            expect(result.x).toBe(400);
            expect(result.y).toBe(200 + (400 - 200) * 0.5 + 15);
        });

        it('should use custom positioning for branch top ghost in LR mode', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 200,
                    correctedSourceY: 600,
                    correctedTargetX: 400,
                    correctedTargetY: 400,
                    isHorizontal: true,
                    sourceNodeTaskDispatcherId: 'branch_1',
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                    targetNodeType: 'workflow',
                })
            );

            // LR mode: posX = midpoint + 15 (horizontal offset), posY = correctedTargetY (on the edge)
            expect(result.x).toBe(200 + (400 - 200) * 0.5 + 15);
            expect(result.y).toBe(400);
        });
    });

    describe('top ghost source edges (non-branch)', () => {
        it('should return edge center for non-branch top ghost main-axis edge', () => {
            // Loop/condition top ghost → child node, main-axis edge
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 500,
                    correctedSourceY: 200,
                    correctedTargetX: 500,
                    correctedTargetY: 400,
                    sourceNodeTaskDispatcherId: 'loop_1',
                    sourceNodeType: 'taskDispatcherTopGhostNode',
                })
            );

            // Main-axis (Y diff > X diff) and NOT branch → edge center
            expect(result).toEqual(EDGE_CENTER);
        });
    });

    describe('target is bottom ghost', () => {
        it('should position at source X in TB mode when target is bottom ghost', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 700,
                    correctedSourceY: 800,
                    correctedTargetX: 500,
                    correctedTargetY: 900,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                })
            );

            // TB off-axis: posX = correctedSourceX, posY = midpoint
            expect(result.x).toBe(700);
            expect(result.y).toBe(800 + (900 - 800) * 0.5);
        });

        it('should position at source Y in LR mode when target is bottom ghost', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 600,
                    correctedSourceY: 400,
                    correctedTargetX: 700,
                    correctedTargetY: 300,
                    isHorizontal: true,
                    targetNodeType: 'taskDispatcherBottomGhostNode',
                })
            );

            // LR off-axis: posX = midpoint, posY = correctedSourceY
            expect(result.x).toBe(600 + (700 - 600) * 0.5);
            expect(result.y).toBe(400);
        });
    });

    describe('task dispatcher source edges', () => {
        it('should position at target X in TB mode for task dispatcher source', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 500,
                    correctedSourceY: 200,
                    correctedTargetX: 700,
                    correctedTargetY: 300,
                    sourceNodeComponentName: 'condition',
                })
            );

            // TB off-axis: posX = correctedTargetX, posY = midpoint
            expect(result.x).toBe(700);
            expect(result.y).toBe(200 + (300 - 200) * 0.5);
        });

        it('should position at target Y in LR mode for task dispatcher source', () => {
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 200,
                    correctedSourceY: 500,
                    correctedTargetX: 300,
                    correctedTargetY: 700,
                    isHorizontal: true,
                    sourceNodeComponentName: 'loop',
                })
            );

            // LR off-axis: posX = midpoint, posY = correctedTargetY
            expect(result.x).toBe(200 + (300 - 200) * 0.5);
            expect(result.y).toBe(700);
        });
    });

    describe('fallback to edge center', () => {
        it('should fall back to edge center when no special case matches in TB mode', () => {
            // Off-axis edge with no special node types
            const result = computeEdgeButtonPosition(
                makeParams({
                    correctedSourceX: 100,
                    correctedSourceY: 200,
                    correctedTargetX: 400,
                    correctedTargetY: 300,
                    sourceNodeType: 'workflow',
                    targetNodeType: 'workflow',
                })
            );

            // TB off-axis with no matching type: posX = edgeCenterX, posY = midpoint
            expect(result.x).toBe(EDGE_CENTER.x);
            expect(result.y).toBe(200 + (300 - 200) * 0.5);
        });
    });
});
