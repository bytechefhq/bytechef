import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useDeleteProjectVersionMutation, useRestoreProjectVersionMutation} from '../projects.mutations';

const {deleteProjectVersionMock, restoreProjectVersionMock} = vi.hoisted(() => ({
    deleteProjectVersionMock: vi.fn(),
    restoreProjectVersionMock: vi.fn(),
}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    ProjectApi: class {
        deleteProjectVersion = deleteProjectVersionMock;
        restoreProjectVersion = restoreProjectVersionMock;
    },
}));

describe('projects mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('sends a version delete through to the project API', async () => {
        const onSuccess = vi.fn();

        deleteProjectVersionMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useDeleteProjectVersionMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({id: 7, projectVersion: 2});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(deleteProjectVersionMock).toHaveBeenCalledWith({id: 7, projectVersion: 2});
    });

    it('sends a version restore through to the project API', async () => {
        const onSuccess = vi.fn();

        restoreProjectVersionMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useRestoreProjectVersionMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({id: 7, projectVersion: 1});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());

        expect(restoreProjectVersionMock).toHaveBeenCalledWith({id: 7, projectVersion: 1});
    });
});
