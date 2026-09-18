import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {render} from '@testing-library/react';
import {EdgeProps, Position} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import PlaceholderEdge from './PlaceholderEdge';

const renderEdgePath = (data?: Record<string, unknown>, sourceX = 100) => {
    const {container} = render(
        <svg>
            <PlaceholderEdge
                {...({
                    data,
                    id: 'trigger_1=>final',
                    sourcePosition: Position.Bottom,
                    sourceX,
                    sourceY: 50,
                    targetPosition: Position.Top,
                    targetX: 300,
                    targetY: 250,
                } as unknown as EdgeProps)}
            />
        </svg>
    );

    return container.querySelector('path')!.getAttribute('d')!;
};

describe('PlaceholderEdge', () => {
    it('draws a curve for a regular placeholder edge', () => {
        expect(renderEdgePath()).toContain('C');
    });

    it('draws a stepped path through the shared bus for a trigger fan-in edge', () => {
        const edgePath = renderEdgePath({triggerFanIn: true});

        expect(edgePath).not.toContain('C');
        expect(edgePath).toContain('Q');
    });

    it('keeps a far-off outer trigger on the same bus as a nearby one', () => {
        const busY = 50 + TRIGGER_FAN_IN_BUS_OFFSET;

        const passesThroughBus = (edgePath: string) =>
            [...edgePath.matchAll(/(-?\d+(?:\.\d+)?)[ ,](-?\d+(?:\.\d+)?)/g)].some(
                (match) => Number(match[2]) === busY
            );

        expect(passesThroughBus(renderEdgePath({triggerFanIn: true}, 280))).toBe(true);
        expect(passesThroughBus(renderEdgePath({triggerFanIn: true}, -600))).toBe(true);
    });
});
