import {TRIGGER_FAN_IN_BUS_OFFSET} from '@/shared/constants';
import {render} from '@testing-library/react';
import {EdgeProps, Position, ReactFlowProvider} from '@xyflow/react';
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

describe('RoundedSmoothStepEdge trigger fan-in', () => {
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
