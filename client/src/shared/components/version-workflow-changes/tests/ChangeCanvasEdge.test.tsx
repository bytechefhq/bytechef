import useLayoutDirectionStore from '@/pages/platform/workflow-editor/stores/useLayoutDirectionStore';
import {render, screen} from '@/shared/util/test-utils';
import {EdgeProps, Position, getSmoothStepPath} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ChangeCanvasEdge from '../ChangeCanvasEdge';

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    BaseEdge: ({className, id, path}: {className: string; id: string; path: string}) => (
        <div className={className} data-path={path} data-testid={id} />
    ),
}));

const EDGE_PROPS = {
    id: 'edge_1',
    source: 'task_1',
    sourcePosition: Position.Bottom,
    sourceX: 0,
    sourceY: 0,
    target: 'task_2',
    targetPosition: Position.Top,
    targetX: 100,
    targetY: 200,
} as EdgeProps;

const getPath = () => screen.getByTestId('edge_1').getAttribute('data-path');

const smoothStepPath = (center: {centerX?: number; centerY?: number} = {}) =>
    getSmoothStepPath({
        borderRadius: 10,
        ...center,
        sourcePosition: Position.Bottom,
        sourceX: 0,
        sourceY: 0,
        targetPosition: Position.Top,
        targetX: 100,
        targetY: 200,
    })[0];

describe('ChangeCanvasEdge', () => {
    beforeEach(() => {
        useLayoutDirectionStore.setState({layoutDirection: 'TB'});
    });

    it('draws a plain smooth step edge between two tasks', () => {
        render(<ChangeCanvasEdge {...EDGE_PROPS} />);

        expect(getPath()).toBe(smoothStepPath());
        expect(screen.getByTestId('edge_1')).toHaveClass('stroke-stroke-neutral-tertiary');
    });

    it('bends an edge into a bottom ghost just before the bar', () => {
        render(<ChangeCanvasEdge {...EDGE_PROPS} target="condition_1-bottom-ghost" />);

        expect(getPath()).toBe(smoothStepPath({centerY: 184}));
        expect(getPath()).not.toBe(smoothStepPath());
    });

    it('routes a trigger fan-in edge through its bus instead of the bottom ghost jog', () => {
        render(<ChangeCanvasEdge {...EDGE_PROPS} data={{triggerFanIn: true}} target="condition_1-bottom-ghost" />);

        expect(getPath()).not.toBe(smoothStepPath({centerY: 184}));
        expect(getPath()).not.toBe(smoothStepPath());
    });
});
