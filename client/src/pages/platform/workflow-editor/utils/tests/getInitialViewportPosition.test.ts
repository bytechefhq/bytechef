import {describe, expect, it} from 'vitest';

import getInitialViewportPosition from '../getInitialViewportPosition';

const LEFT_INSET = 80;
const TOP_INSET = 120;

describe('getInitialViewportPosition', () => {
    it('should put the topmost node the inset below the top edge and keep the layout x for a top-to-bottom graph', () => {
        expect(
            getInitialViewportPosition({
                canvasHeight: 1000,
                layoutDirection: 'TB',
                leftInset: LEFT_INSET,
                nodesBounds: {height: 200, width: 400, x: 300, y: 50},
                offsetX: 16,
                topInset: TOP_INSET,
            })
        ).toEqual({x: 16, y: 70});
    });

    it('should put the leftmost node the inset from the left edge and centre a left-to-right graph vertically', () => {
        const position = getInitialViewportPosition({
            canvasHeight: 1000,
            layoutDirection: 'LR',
            leftInset: LEFT_INSET,
            nodesBounds: {height: 400, width: 1200, x: 30, y: 50},
            offsetX: 16,
            topInset: TOP_INSET,
        });

        expect(30 + position.x).toBe(96);
        expect(250 + position.y).toBe(500);
    });

    it('should fall back to the top placement for a left-to-right graph taller than the canvas', () => {
        expect(
            getInitialViewportPosition({
                canvasHeight: 600,
                layoutDirection: 'LR',
                leftInset: LEFT_INSET,
                nodesBounds: {height: 900, width: 1200, x: 0, y: 0},
                offsetX: 0,
                topInset: TOP_INSET,
            })
        ).toEqual({x: LEFT_INSET, y: TOP_INSET});
    });

    it('should fall back to the top placement while the canvas has not been measured', () => {
        expect(
            getInitialViewportPosition({
                canvasHeight: 0,
                layoutDirection: 'LR',
                leftInset: LEFT_INSET,
                nodesBounds: {height: 400, width: 1200, x: 0, y: 0},
                offsetX: 0,
                topInset: TOP_INSET,
            })
        ).toEqual({x: LEFT_INSET, y: TOP_INSET});
    });
});
