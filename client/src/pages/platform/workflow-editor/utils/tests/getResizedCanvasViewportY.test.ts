import {describe, expect, it} from 'vitest';

import getResizedCanvasViewportY from '../getResizedCanvasViewportY';

const TOP_INSET = 120;

describe('getResizedCanvasViewportY', () => {
    it('should move the graph up by half the height the canvas lost', () => {
        expect(
            getResizedCanvasViewportY({
                canvasHeight: 800,
                nodesBounds: {height: 400, width: 1200, x: 0, y: 0},
                previousCanvasHeight: 1200,
                topInset: TOP_INSET,
                viewportY: 400,
                zoom: 1,
            })
        ).toBe(200);
    });

    it('should move the graph back down by half the height the canvas regained', () => {
        expect(
            getResizedCanvasViewportY({
                canvasHeight: 1200,
                nodesBounds: {height: 400, width: 1200, x: 0, y: 0},
                previousCanvasHeight: 800,
                topInset: TOP_INSET,
                viewportY: 200,
                zoom: 1,
            })
        ).toBe(400);
    });

    it('should stop moving up once the top node reaches the inset', () => {
        expect(
            getResizedCanvasViewportY({
                canvasHeight: 400,
                nodesBounds: {height: 700, width: 1200, x: 0, y: 0},
                previousCanvasHeight: 1200,
                topInset: TOP_INSET,
                viewportY: 250,
                zoom: 1,
            })
        ).toBe(TOP_INSET);
    });

    it('should measure the inset in screen pixels at the current zoom', () => {
        expect(
            getResizedCanvasViewportY({
                canvasHeight: 400,
                nodesBounds: {height: 700, width: 1200, x: 0, y: 100},
                previousCanvasHeight: 1200,
                topInset: TOP_INSET,
                viewportY: 250,
                zoom: 0.5,
            })
        ).toBe(70);
    });

    it('should not move a graph whose top already sits above the inset any further up', () => {
        expect(
            getResizedCanvasViewportY({
                canvasHeight: 800,
                nodesBounds: {height: 700, width: 1200, x: 0, y: 0},
                previousCanvasHeight: 1200,
                topInset: TOP_INSET,
                viewportY: 50,
                zoom: 1,
            })
        ).toBe(50);
    });
});
