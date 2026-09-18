import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {Position} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import {getTriggerFanInBusCenter, getTriggerFanInButtonPosition} from './computeTriggerFanIn';

describe('getTriggerFanInBusCenter', () => {
    it('returns no bus center for an edge that is not a trigger fan-in', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: false, sourcePosition: Position.Bottom, sourceX: 0, sourceY: 0})
        ).toEqual({});
    });

    it('pins a horizontal bus below the trigger row when triggers connect from the bottom', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourcePosition: Position.Bottom, sourceX: 100, sourceY: 50})
        ).toEqual({centerY: 50 + TRIGGER_FAN_IN_BUS_OFFSET});
    });

    it('pins a vertical bus beside the trigger column when triggers connect from the right', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourcePosition: Position.Right, sourceX: 50, sourceY: 100})
        ).toEqual({centerX: 50 + TRIGGER_FAN_IN_BUS_OFFSET});
    });

    it('puts a far-off outer trigger on the same bus as an inner one', () => {
        const innerTriggerBus = getTriggerFanInBusCenter({
            isTriggerFanIn: true,
            sourcePosition: Position.Bottom,
            sourceX: 480,
            sourceY: 50,
        });
        const outerTriggerBus = getTriggerFanInBusCenter({
            isTriggerFanIn: true,
            sourcePosition: Position.Bottom,
            sourceX: 0,
            sourceY: 50,
        });

        expect(outerTriggerBus).toEqual(innerTriggerBus);
    });

    it('mirrors the bus for top and left handles', () => {
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourcePosition: Position.Top, sourceX: 0, sourceY: 100})
        ).toEqual({centerY: 100 - TRIGGER_FAN_IN_BUS_OFFSET});
        expect(
            getTriggerFanInBusCenter({isTriggerFanIn: true, sourcePosition: Position.Left, sourceX: 100, sourceY: 0})
        ).toEqual({centerX: 100 - TRIGGER_FAN_IN_BUS_OFFSET});
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
        const busCenter = getTriggerFanInBusCenter({
            isTriggerFanIn: true,
            sourcePosition: Position.Bottom,
            sourceX: 100,
            sourceY: 50,
        });

        const buttonPosition = getTriggerFanInButtonPosition({busCenter, targetX: 160, targetY: 250});

        expect(buttonPosition.x).toBe(160);
        expect(buttonPosition.y).toBeGreaterThan(busCenter.centerY!);
        expect(buttonPosition.y).toBeLessThan(250);
    });
});
