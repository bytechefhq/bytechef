import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useRestartJobMutation, useStopJobMutation} from '../jobs.mutations';

const {restartJobMock, stopJobMock} = vi.hoisted(() => ({
    restartJobMock: vi.fn(),
    stopJobMock: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/workflow/execution/apis/JobApi', () => ({
    JobApi: class {
        restartJob = restartJobMock;
        stopJob = stopJobMock;
    },
}));

describe('jobs mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('sends a restart through to the job API and reports success', async () => {
        const onSuccess = vi.fn();

        restartJobMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useRestartJobMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate(9);

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(restartJobMock).toHaveBeenCalledWith({id: 9});
        expect(stopJobMock).not.toHaveBeenCalled();
    });

    it('reports a restart failure to the caller', async () => {
        const onError = vi.fn();

        restartJobMock.mockRejectedValue(new Error('cannot restart'));

        const {result} = renderHook(() => useRestartJobMutation({onError}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate(9);

        await waitFor(() => expect(onError).toHaveBeenCalled());
    });

    it('sends a stop through to the job API', async () => {
        stopJobMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useStopJobMutation({}), {wrapper: createTestQueryClientWrapper()});

        result.current.mutate(5);

        await waitFor(() => expect(stopJobMock).toHaveBeenCalledWith({id: 5}));

        expect(restartJobMock).not.toHaveBeenCalled();
    });
});
