import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {render} from '@testing-library/react';
import {EdgeProps, Position} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import RoundedSmoothStepEdge from './RoundedSmoothStepEdge';

interface EdgeGeometryI {
    sourcePosition: Position;
    sourceX: number;
    sourceY: number;
    targetPosition: Position;
    targetX: number;
    targetY: number;
}

const getPathPoints = (edgePath: string): Array<{x: number; y: number}> =>
    [...edgePath.matchAll(/(-?\d+(?:\.\d+)?)[ ,](-?\d+(?:\.\d+)?)/g)].map((match) => ({
        x: Number(match[1]),
        y: Number(match[2]),
    }));

const renderFanInEdgePath = (geometry: EdgeGeometryI) => {
    const {container} = render(
        <svg>
            <RoundedSmoothStepEdge
                {...({
                    data: {triggerFanIn: true},
                    id: 'trigger=>task_1',
                    target: 'task_1',
                    ...geometry,
                } as unknown as EdgeProps)}
            />
        </svg>
    );

    return container.querySelector('path')!.getAttribute('d')!;
};

describe('RoundedSmoothStepEdge', () => {
    it('runs an outer and an inner trigger edge along the same horizontal bus in TB', () => {
        const target = {targetPosition: Position.Top, targetX: 500, targetY: 250};

        const innerEdgePath = renderFanInEdgePath({
            sourcePosition: Position.Bottom,
            sourceX: 440,
            sourceY: 50,
            ...target,
        });
        const outerEdgePath = renderFanInEdgePath({
            sourcePosition: Position.Bottom,
            sourceX: 0,
            sourceY: 50,
            ...target,
        });

        const busY = 50 + TRIGGER_FAN_IN_BUS_OFFSET;

        expect(getPathPoints(innerEdgePath).some((point) => point.y === busY)).toBe(true);
        expect(getPathPoints(outerEdgePath).some((point) => point.y === busY)).toBe(true);
    });

    it('runs an outer and an inner trigger edge along the same vertical bus in LR', () => {
        const target = {targetPosition: Position.Left, targetX: 250, targetY: 500};

        const innerEdgePath = renderFanInEdgePath({
            sourcePosition: Position.Right,
            sourceX: 50,
            sourceY: 440,
            ...target,
        });
        const outerEdgePath = renderFanInEdgePath({
            sourcePosition: Position.Right,
            sourceX: 50,
            sourceY: 0,
            ...target,
        });

        const busX = 50 + TRIGGER_FAN_IN_BUS_OFFSET;

        expect(getPathPoints(innerEdgePath).some((point) => point.x === busX)).toBe(true);
        expect(getPathPoints(outerEdgePath).some((point) => point.x === busX)).toBe(true);
    });
});
