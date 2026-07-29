import {describe, expect, it} from 'vitest';

import computeExitEdgeJogCenter from './computeExitEdgeJogCenter';

function makeParams(overrides: Record<string, unknown> = {}) {
    return {
        correctedSourceX: 3383,
        correctedSourceY: 2492,
        correctedTargetX: 2787,
        correctedTargetY: 2984,
        isHorizontal: false,
        isTriggerFanIn: false,
        targetNodeType: 'taskDispatcherBottomGhostNode',
        ...overrides,
    };
}

describe('computeExitEdgeJogCenter', () => {
    it('pins the bend 16px above the bottom bar for a long trailing edge in TB mode', () => {
        // The default smoothstep corner sits at the path midpoint — mid-frame
        // for a long trailing edge — so its horizontal leg crossed sibling
        // case content on the way to the bar
        expect(computeExitEdgeJogCenter(makeParams())).toEqual({centerY: 2984 - 16});
    });

    it('pins the bend 16px before the bar on the main axis in LR mode', () => {
        const result = computeExitEdgeJogCenter(
            makeParams({
                correctedSourceX: 2492,
                correctedSourceY: 3383,
                correctedTargetX: 2984,
                correctedTargetY: 2787,
                isHorizontal: true,
            })
        );

        expect(result).toEqual({centerX: 2984 - 16});
    });

    it('also jogs a nested-to-enclosing bar merge edge (bottom ghost source and target)', () => {
        // Merge stubs drop only one layer gap (68px) but still deserve the
        // near-bar bend so the run stays in the strip between the two bars
        const result = computeExitEdgeJogCenter(
            makeParams({
                correctedSourceX: 2939,
                correctedSourceY: 2916,
                correctedTargetX: 2787,
                correctedTargetY: 2984,
            })
        );

        expect(result).toEqual({centerY: 2984 - 16});
    });

    it('leaves a short stub untouched — the default corner already sits beside the bar', () => {
        const result = computeExitEdgeJogCenter(
            makeParams({
                correctedSourceY: 2984 - 40,
            })
        );

        expect(result).toEqual({});
    });

    it('ignores edges that do not terminate on a bottom bar', () => {
        expect(computeExitEdgeJogCenter(makeParams({targetNodeType: 'workflow'}))).toEqual({});
        expect(computeExitEdgeJogCenter(makeParams({targetNodeType: undefined}))).toEqual({});
    });

    it('ignores trigger fan-in edges, which pin their own bus center', () => {
        expect(computeExitEdgeJogCenter(makeParams({isTriggerFanIn: true}))).toEqual({});
    });
});
