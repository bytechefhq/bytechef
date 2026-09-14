import {describe, expect, it} from 'vitest';

import {getAxisCenteredViewport} from './axisCenteredViewportUtils';

describe('getAxisCenteredViewport', () => {
    it('puts the axis, not the middle of the bounds, in the middle of the flow', () => {
        const viewport = getAxisCenteredViewport({
            axisX: 100,
            bounds: {height: 200, width: 400, x: 0, y: 0},
            flowHeight: 1000,
            flowWidth: 1000,
            maxZoom: 1,
            minZoom: 0.1,
            padding: 0,
        });

        expect(viewport).toEqual({x: 400, y: 400, zoom: 1});
        expect(viewport.x + 100 * viewport.zoom).toBe(500);
    });

    it('zooms out so the side farthest from the axis still fits', () => {
        const viewport = getAxisCenteredViewport({
            axisX: 100,
            bounds: {height: 200, width: 400, x: 0, y: 0},
            flowHeight: 1000,
            flowWidth: 300,
            maxZoom: 1,
            minZoom: 0.1,
            padding: 0,
        });

        expect(viewport.zoom).toBe(0.5);
        expect(viewport.x + 100 * viewport.zoom).toBe(150);
        expect(viewport.x + 400 * viewport.zoom).toBeLessThanOrEqual(300);
    });

    it('applies padding the way fitView does when the axis is the middle of the bounds', () => {
        const padding = 0.15;

        const viewport = getAxisCenteredViewport({
            axisX: 200,
            bounds: {height: 100, width: 400, x: 0, y: 0},
            flowHeight: 1000,
            flowWidth: 1000,
            maxZoom: 10,
            minZoom: 0.1,
            padding,
        });

        expect(viewport.zoom).toBeCloseTo(1000 / (400 * (1 + padding)));
        expect(viewport.x + 200 * viewport.zoom).toBeCloseTo(500);
    });

    it('clamps the zoom to the given limits', () => {
        const viewport = getAxisCenteredViewport({
            axisX: 0,
            bounds: {height: 10000, width: 20, x: -10, y: 0},
            flowHeight: 100,
            flowWidth: 100,
            maxZoom: 1,
            minZoom: 0.1,
            padding: 0.15,
        });

        expect(viewport.zoom).toBe(0.1);
        expect(viewport.x).toBe(50);
    });
});
