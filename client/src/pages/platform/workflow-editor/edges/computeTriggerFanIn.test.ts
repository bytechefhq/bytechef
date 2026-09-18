import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {describe, expect, it} from 'vitest';

import {getTriggerFanInBusCenter, getTriggerFanInButtonPosition} from './computeTriggerFanIn';

describe('getTriggerFanInBusCenter', () => {
    it('returns no bus center for an edge that is not a trigger fan-in', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: false, sourceX: 0, sourceY: 0, targetX: 0, targetY: 200})
        ).toEqual({});
    });

    it('pins a horizontal bus below the trigger row for a mostly vertical edge', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourceX: 100, sourceY: 50, targetX: 160, targetY: 250})
        ).toEqual({centerY: 50 + TRIGGER_FAN_IN_BUS_OFFSET});
    });

    it('pins a vertical bus beside the trigger column for a mostly horizontal edge', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourceX: 50, sourceY: 100, targetX: 250, targetY: 160})
        ).toEqual({centerX: 50 + TRIGGER_FAN_IN_BUS_OFFSET});
    });

    it('treats an edge with equal horizontal and vertical spans as vertical', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourceX: 0, sourceY: 0, targetX: 100, targetY: 100})
        ).toEqual({centerY: TRIGGER_FAN_IN_BUS_OFFSET});
    });
});

describe('getTriggerFanInButtonPosition', () => {
    it('centers the button between a horizontal bus and the target, on the target column', () => {
        expect(getTriggerFanInButtonPosition({busCenter: {centerY: 90}, targetX: 160, targetY: 250})).toEqual({
            x: 160,
            y: 170,
        });
    });

    it('centers the button between a vertical bus and the target, on the target row', () => {
        expect(getTriggerFanInButtonPosition({busCenter: {centerX: 90}, targetX: 250, targetY: 160})).toEqual({
            x: 170,
            y: 160,
        });
    });

    it('falls back to the target point when there is no bus', () => {
        expect(getTriggerFanInButtonPosition({busCenter: {}, targetX: 250, targetY: 160})).toEqual({
            x: 250,
            y: 160,
        });
    });

    it('places the button on the bus that the edge path is drawn with', () => {
        const coordinates = {sourceX: 100, sourceY: 50, targetX: 160, targetY: 250};

        const busCenter = getTriggerFanInBusCenter({isTriggerFanIn: true, ...coordinates});

        const buttonPosition = getTriggerFanInButtonPosition({
            busCenter,
            targetX: coordinates.targetX,
            targetY: coordinates.targetY,
        });

        expect(buttonPosition.x).toBe(coordinates.targetX);
        expect(buttonPosition.y).toBeGreaterThan(busCenter.centerY!);
        expect(buttonPosition.y).toBeLessThan(coordinates.targetY);
    });
});
