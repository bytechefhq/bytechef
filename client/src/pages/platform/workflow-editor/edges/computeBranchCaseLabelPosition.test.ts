import {describe, expect, it} from 'vitest';

import computeBranchCaseLabelPosition from './computeBranchCaseLabelPosition';

describe('computeBranchCaseLabelPosition', () => {
    const defaultCoords = {sourceX: 100, sourceY: 200, targetX: 300, targetY: 400};

    describe('LR layout', () => {
        it('should anchor above the row line, 44px past the split bar', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                layoutDirection: 'LR',
            });

            expect(result).toEqual({anchor: 'above', x: 144, y: 388});
        });

        it('should lift the chip above the icon band when the row shares the dispatcher axis', () => {
            const result = computeBranchCaseLabelPosition({
                layoutDirection: 'LR',
                sourceX: 100,
                sourceY: 200,
                targetX: 300,
                targetY: 210,
            });

            expect(result).toEqual({anchor: 'above', x: 144, y: 144});
        });

        it('should drop below the line for the row under the dispatcher label text', () => {
            const result = computeBranchCaseLabelPosition({
                layoutDirection: 'LR',
                sourceX: 100,
                sourceY: 200,
                targetX: 300,
                targetY: 325,
            });

            expect(result).toEqual({anchor: 'below', x: 144, y: 337});
        });

        it('should stay above the line for the row just above the axis', () => {
            const result = computeBranchCaseLabelPosition({
                layoutDirection: 'LR',
                sourceX: 100,
                sourceY: 200,
                targetX: 300,
                targetY: 75,
            });

            expect(result).toEqual({anchor: 'above', x: 144, y: 63});
        });
    });

    describe('TB layout', () => {
        it('should position at (targetX, sourceY + offset)', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                layoutDirection: 'TB',
            });

            expect(result).toEqual({anchor: 'center', x: 300, y: 210});
        });
    });
});
