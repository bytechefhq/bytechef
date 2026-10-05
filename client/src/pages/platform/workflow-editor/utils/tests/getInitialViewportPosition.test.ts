import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, CANVAS_TOP_OFFSET} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import getInitialViewportPosition from '../getInitialViewportPosition';

describe('getInitialViewportPosition', () => {
    it('keeps a top-to-bottom workflow at the overlay panels offset', () => {
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 700, layoutDirection: 'TB', offsetX: 0})
        ).toEqual({x: 0, y: CANVAS_TOP_OFFSET});
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 700, layoutDirection: 'TB', offsetX: -200})
        ).toEqual({
            x: -200,
            y: CANVAS_TOP_OFFSET,
        });
    });

    it('insets a left-to-right workflow from the canvas left edge', () => {
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 650, layoutDirection: 'LR', offsetX: 0})
        ).toEqual({
            x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: 0,
        });
    });

    it('adds the inset on top of the overlay panels offset', () => {
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 650, layoutDirection: 'LR', offsetX: -200})
        ).toEqual({
            x: -200 + CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: 0,
        });
    });

    it('moves the left-to-right row onto the measured flow center', () => {
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 700, layoutDirection: 'LR', offsetX: 0})
        ).toEqual({
            x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: 25,
        });
    });

    it('trusts the layout centering before the flow is measured', () => {
        expect(
            getInitialViewportPosition({canvasHeight: 650, flowHeight: 0, layoutDirection: 'LR', offsetX: 0})
        ).toEqual({
            x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: 0,
        });
    });
});
