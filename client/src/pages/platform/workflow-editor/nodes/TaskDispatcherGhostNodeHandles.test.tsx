import {render} from '@testing-library/react';
import {ReactFlowProvider} from '@xyflow/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import TaskDispatcherBottomGhostNode from './TaskDispatcherBottomGhostNode';
import TaskDispatcherTopGhostNode from './TaskDispatcherTopGhostNode';

const {directionStoreState} = vi.hoisted(() => ({
    directionStoreState: {layoutDirection: 'TB'},
}));

vi.mock('../stores/useLayoutDirectionStore', () => ({
    default: (selector: (state: {layoutDirection: string}) => unknown) => selector(directionStoreState),
}));

function renderGhost(component: 'top' | 'bottom', id: string) {
    const Ghost = component === 'top' ? TaskDispatcherTopGhostNode : TaskDispatcherBottomGhostNode;

    const {container} = render(
        <ReactFlowProvider>
            <Ghost id={id} />
        </ReactFlowProvider>
    );

    return (handleSuffix: string) => container.querySelector(`[data-handleid="${id}${handleSuffix}"]`);
}

describe('ghost bar side handles', () => {
    beforeEach(() => {
        directionStoreState.layoutDirection = 'TB';
    });

    it('keeps TB side handles on the bar ends', () => {
        const handle = renderGhost('top', 'loop_1-loop-top-ghost');

        expect(handle('-left')?.className).toContain('react-flow__handle-left');
        expect(handle('-left')?.className).toContain('left-8');
        expect(handle('-right')?.className).toContain('react-flow__handle-right');
        expect(handle('-right')?.className).toContain('right-8');
    });

    it('swaps the side handle ends on an LR iteration-ring top bar', () => {
        // The ring content flips to the TOP edge in LR: the rail edge leaves
        // via -left from the bar's BOTTOM end and the body enters via -right
        // from its TOP end — without the swap the connectors cut diagonally
        // across the body
        directionStoreState.layoutDirection = 'LR';

        const handle = renderGhost('top', 'loop_1-loop-top-ghost');

        expect(handle('-left')?.className).toContain('react-flow__handle-bottom');
        expect(handle('-left')?.className).toContain('bottom-8');
        expect(handle('-right')?.className).toContain('react-flow__handle-top');
        expect(handle('-right')?.className).toContain('top-8');
    });

    it('swaps the side handle ends on an LR iteration-ring bottom bar', () => {
        directionStoreState.layoutDirection = 'LR';

        const handle = renderGhost('bottom', 'each_2-each-bottom-ghost');

        expect(handle('-left')?.className).toContain('react-flow__handle-bottom');
        expect(handle('-left')?.className).toContain('bottom-8');
        expect(handle('-right')?.className).toContain('react-flow__handle-top');
        expect(handle('-right')?.className).toContain('top-8');
    });

    it('leaves non-ring bars unflipped in LR', () => {
        // Condition (and branch/parallel/fork-join) bars keep the plain LR
        // mapping: their side handles double as case/branch attachments
        directionStoreState.layoutDirection = 'LR';

        const handle = renderGhost('top', 'condition_1-condition-top-ghost');

        expect(handle('-left')?.className).toContain('react-flow__handle-top');
        expect(handle('-left')?.className).toContain('top-8');
        expect(handle('-right')?.className).toContain('react-flow__handle-bottom');
        expect(handle('-right')?.className).toContain('bottom-8');
    });

    it('leaves a parallel bottom bar unflipped in LR', () => {
        directionStoreState.layoutDirection = 'LR';

        const handle = renderGhost('bottom', 'parallel_1-parallel-bottom-ghost');

        expect(handle('-left')?.className).toContain('react-flow__handle-top');
        expect(handle('-left')?.className).toContain('top-8');
        expect(handle('-right')?.className).toContain('react-flow__handle-bottom');
        expect(handle('-right')?.className).toContain('bottom-8');
    });
});
