import {describe, expect, it} from 'vitest';

import computeBranchCaseLabelPosition from './computeBranchCaseLabelPosition';

describe('computeBranchCaseLabelPosition', () => {
    const defaultCoords = {sourceX: 100, sourceY: 200, targetX: 300, targetY: 400};

    describe('LR layout', () => {
        it('should position at (sourceX, targetY) without edge button', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                layoutDirection: 'LR',
            });

            expect(result).toEqual({x: 100, y: 400});
        });

        it('should offset y by 10px when hasEdgeButton is true', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                hasEdgeButton: true,
                layoutDirection: 'LR',
            });

            expect(result).toEqual({x: 100, y: 410});
        });

        it('should not offset y when hasEdgeButton is false', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                hasEdgeButton: false,
                layoutDirection: 'LR',
            });

            expect(result).toEqual({x: 100, y: 400});
        });
    });

    describe('TB layout', () => {
        it('should position at (targetX, sourceY)', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                layoutDirection: 'TB',
            });

            expect(result).toEqual({x: 300, y: 200});
        });

        it('should ignore hasEdgeButton in TB mode', () => {
            const result = computeBranchCaseLabelPosition({
                ...defaultCoords,
                hasEdgeButton: true,
                layoutDirection: 'TB',
            });

            expect(result).toEqual({x: 300, y: 200});
        });
    });
});
