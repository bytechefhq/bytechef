import {render} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import {RemeasureClusterNodes} from './ClusterElementsWorkflowEditor';

// Edges from a cluster-root node to its child/placeholder rendered non-vertical because ReactFlow
// caches handle bounds while the hosting dialog's 300ms open transitions are still resizing/laying
// out the pane. RemeasureClusterNodes re-measures every node immediately, on the next frame, and once
// those transitions have settled, so the final cached bounds match the rendered handles.

const updateNodeInternalsMock = vi.fn();

vi.mock('@xyflow/react', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@xyflow/react')>()),
    useReactFlow: () => ({getNodes: () => [{id: 'approval_1'}, {id: 'approval_1-approvalChannels-placeholder-0'}]}),
    useStore: (selector: (state: unknown) => unknown) => selector({nodes: [{id: 'approval_1'}, {id: 'placeholder'}]}),
    useUpdateNodeInternals: () => updateNodeInternalsMock,
}));

describe('RemeasureClusterNodes', () => {
    beforeEach(() => {
        updateNodeInternalsMock.mockClear();
        vi.useFakeTimers();
    });

    afterEach(() => {
        vi.runOnlyPendingTimers();
        vi.useRealTimers();
    });

    it('re-measures every node immediately on mount', () => {
        render(<RemeasureClusterNodes />);

        expect(updateNodeInternalsMock).toHaveBeenCalledWith('approval_1');
        expect(updateNodeInternalsMock).toHaveBeenCalledWith('approval_1-approvalChannels-placeholder-0');
    });

    it('re-measures again after the dialog open transitions settle', () => {
        render(<RemeasureClusterNodes />);

        const callsAfterMount = updateNodeInternalsMock.mock.calls.length;

        // Advance past the 350ms settle window (the dialog's 300ms open transitions).
        vi.advanceTimersByTime(400);

        expect(updateNodeInternalsMock.mock.calls.length).toBeGreaterThan(callsAfterMount);
        expect(updateNodeInternalsMock).toHaveBeenLastCalledWith('approval_1-approvalChannels-placeholder-0');
    });
});
