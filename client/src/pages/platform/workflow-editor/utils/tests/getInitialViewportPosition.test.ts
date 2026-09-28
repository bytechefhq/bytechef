import {CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET, CANVAS_TOP_OFFSET} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import getInitialViewportPosition from '../getInitialViewportPosition';

describe('getInitialViewportPosition', () => {
    it('keeps a top-to-bottom workflow at the overlay panels offset', () => {
        expect(getInitialViewportPosition({layoutDirection: 'TB', offsetX: 0})).toEqual({x: 0, y: CANVAS_TOP_OFFSET});
        expect(getInitialViewportPosition({layoutDirection: 'TB', offsetX: -200})).toEqual({
            x: -200,
            y: CANVAS_TOP_OFFSET,
        });
    });

    it('insets a left-to-right workflow from the canvas left edge', () => {
        expect(getInitialViewportPosition({layoutDirection: 'LR', offsetX: 0})).toEqual({
            x: CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: CANVAS_TOP_OFFSET,
        });
    });

    it('adds the inset on top of the overlay panels offset', () => {
        expect(getInitialViewportPosition({layoutDirection: 'LR', offsetX: -200})).toEqual({
            x: -200 + CANVAS_LEFT_TO_RIGHT_LEFT_OFFSET,
            y: CANVAS_TOP_OFFSET,
        });
    });
});
