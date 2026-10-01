import {describe, expect, it} from 'vitest';

import computeAddBranchChipPosition from './computeAddBranchChipPosition';

describe('computeAddBranchChipPosition', () => {
    it('should place the chip at the right end of the horizontal bar run in TB', () => {
        expect(
            computeAddBranchChipPosition({
                layoutDirection: 'TB',
                sourceX: 400,
                sourceY: 200,
                targetX: 700,
                targetY: 300,
            })
        ).toEqual({x: 664, y: 200});
    });

    it('should fall back to the run midpoint when the TB run is too short', () => {
        expect(
            computeAddBranchChipPosition({
                layoutDirection: 'TB',
                sourceX: 400,
                sourceY: 200,
                targetX: 440,
                targetY: 300,
            })
        ).toEqual({x: 420, y: 200});
    });

    it('should place the chip at the far end of the vertical bar run in LR', () => {
        expect(
            computeAddBranchChipPosition({
                layoutDirection: 'LR',
                sourceX: 200,
                sourceY: 400,
                targetX: 300,
                targetY: 700,
            })
        ).toEqual({x: 200, y: 664});
    });
});
