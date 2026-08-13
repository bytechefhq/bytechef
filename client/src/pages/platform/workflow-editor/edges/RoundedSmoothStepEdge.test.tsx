import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {render} from '@testing-library/react';
import {Position, ReactFlowProvider} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import RoundedSmoothStepEdge from './RoundedSmoothStepEdge';

const {directionStoreState} = vi.hoisted(() => ({
    directionStoreState: {layoutDirection: 'TB'},
}));

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector(directionStoreState),
}));

function renderEdgePath(props: Partial<Parameters<typeof RoundedSmoothStepEdge>[0]> = {}) {
    const {container} = render(
        <ReactFlowProvider>
            <svg>
                <RoundedSmoothStepEdge
                    id="edge-under-test"
                    source="sourceNode"
                    sourcePosition={Position.Bottom}
                    sourceX={300}
                    sourceY={100}
                    target="targetNode"
                    targetPosition={Position.Top}
                    targetX={100}
                    targetY={600}
                    {...props}
                />
            </svg>
        </ReactFlowProvider>
    );

    return container.querySelector('path')?.getAttribute('d') ?? '';
}

describe('RoundedSmoothStepEdge trigger fan-in bus', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
    });

    // Every trigger in a fan-in row must bend on ONE horizontal line. An outer trigger sits far to
    // the side, so its Δx beats its Δy even in TB — orientation read off that gave it a vertical
    // bus, leaving its horizontal leg at the path midpoint instead of on the bus.
    it('pins both sides of a fan-in row to the same bus line', () => {
        const busY = 100 + TRIGGER_FAN_IN_BUS_OFFSET;

        // Δx deliberately beats Δy on both, which is the geometry that used to flip the orientation:
        // a fan-in row is wide and shallow, so its outer triggers always look "horizontal" per-edge.
        const leftPath = renderEdgePath({data: {triggerFanIn: true}, sourceX: 100, targetX: 400, targetY: 300});
        const rightPath = renderEdgePath({data: {triggerFanIn: true}, sourceX: 700, targetX: 400, targetY: 300});

        expect(leftPath).toContain(`,${busY}`);
        expect(rightPath).toContain(`,${busY}`);

        // The midpoint corner the vertical-bus reading produced.
        expect(leftPath).not.toContain(',200');
        expect(rightPath).not.toContain(',200');
    });

    it('pins the bus on the main axis in LR mode', () => {
        directionStoreState.layoutDirection = 'LR';

        const busX = 300 + TRIGGER_FAN_IN_BUS_OFFSET;

        const outerPath = renderEdgePath({
            data: {triggerFanIn: true},
            sourceY: 700,
            targetX: 900,
            targetY: 100,
        });

        expect(outerPath).toContain(`${busX},`);
    });
});

describe('RoundedSmoothStepEdge exit jog', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
    });

    it('bends beside the bottom bar for an edge into a bottom ghost in TB mode', () => {
        // The horizontal leg must run at targetY - 16 (the structurally empty
        // strip beside the bar), not at the path midpoint mid-frame
        const path = renderEdgePath({target: 'condition_1-condition-bottom-ghost'});

        expect(path).toContain('584');
        expect(path).not.toContain('L300 350');
    });

    it('bends beside the bar on the main axis in LR mode', () => {
        directionStoreState.layoutDirection = 'LR';

        const path = renderEdgePath({
            sourcePosition: Position.Right,
            sourceX: 100,
            sourceY: 300,
            target: 'loop_1-loop-bottom-ghost',
            targetPosition: Position.Left,
            targetX: 600,
            targetY: 100,
        });

        expect(path).toContain('584');
    });

    it('keeps the midpoint bend for ordinary targets', () => {
        const path = renderEdgePath({target: 'someTask_1'});

        expect(path).not.toContain('584');
    });
});
