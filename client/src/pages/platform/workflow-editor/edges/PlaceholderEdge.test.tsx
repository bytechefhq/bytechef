import {render} from '@testing-library/react';
import {EdgeProps, Position} from '@xyflow/react';
import {describe, expect, it} from 'vitest';

import PlaceholderEdge from './PlaceholderEdge';

const renderEdgePath = (data?: Record<string, unknown>) => {
    const {container} = render(
        <svg>
            <PlaceholderEdge
                {...({
                    data,
                    id: 'trigger_1=>final',
                    sourcePosition: Position.Bottom,
                    sourceX: 100,
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
});
