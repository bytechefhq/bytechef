import {render} from '@testing-library/react';
import {describe, expect, it} from 'vitest';

import TaskDispatcherGhostBarHalves from './TaskDispatcherGhostBarHalves';

function renderHalves(isFlippedRingBar: boolean) {
    const {container} = render(
        <TaskDispatcherGhostBarHalves
            isFlippedRingBar={isFlippedRingBar}
            leftStatus={undefined}
            rightStatus="COMPLETED"
        />
    );

    return (half: 'end' | 'start') => container.querySelector(`[data-ghost-bar-half="${half}"]`)?.className ?? '';
}

describe('TaskDispatcherGhostBarHalves', () => {
    it('paints only the half whose side ran', () => {
        const half = renderHalves(false);

        expect(half('start')).toContain('flex-1');
        expect(half('start')).not.toContain('bg-');
        expect(half('end')).toContain('bg-green-500');
    });

    it('swaps the halves on an LR iteration ring bar, following its swapped handles', () => {
        const half = renderHalves(true);

        expect(half('start')).toContain('bg-green-500');
        expect(half('end')).toContain('flex-1');
        expect(half('end')).not.toContain('bg-');
    });
});
