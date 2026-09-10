import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {
    useDeleteAiProviderMutation,
    useEnableAiProviderMutation,
    useUpdateAiProviderMutation,
} from '../aiProvider.mutations';

const {deleteAiProviderMock, enableAiProviderMock, updateAiProviderMock} = vi.hoisted(() => ({
    deleteAiProviderMock: vi.fn(),
    enableAiProviderMock: vi.fn(),
    updateAiProviderMock: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    AiProviderApi: class {
        deleteAiProvider = deleteAiProviderMock;
        enableAiProvider = enableAiProviderMock;
        updateAiProvider = updateAiProviderMock;
    },
}));

describe('aiProvider mutations', () => {
    afterEach(() => {
        resetAll();
    });

    it('sends an update through to the AI provider API', async () => {
        updateAiProviderMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useUpdateAiProviderMutation(), {wrapper: createTestQueryClientWrapper()});

        const request = {environment: 1, id: 3, updateAiProviderRequest: {apiKey: 'sk-test'}};

        result.current.mutate(request);

        await waitFor(() => expect(updateAiProviderMock).toHaveBeenCalledWith(request));
    });

    it('reports an update failure to the caller', async () => {
        const onError = vi.fn();

        updateAiProviderMock.mockRejectedValue(new Error('nope'));

        const {result} = renderHook(() => useUpdateAiProviderMutation({onError}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({environment: 1, id: 3, updateAiProviderRequest: {apiKey: 'sk-test'}});

        await waitFor(() => expect(onError).toHaveBeenCalled());
    });

    it('sends a delete through to the AI provider API', async () => {
        deleteAiProviderMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useDeleteAiProviderMutation(), {wrapper: createTestQueryClientWrapper()});

        result.current.mutate({environment: 1, id: 3});

        await waitFor(() => expect(deleteAiProviderMock).toHaveBeenCalledWith({environment: 1, id: 3}));
    });

    it('sends an enable through to the AI provider API', async () => {
        enableAiProviderMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useEnableAiProviderMutation(), {wrapper: createTestQueryClientWrapper()});

        result.current.mutate({enable: true, environment: 1, id: 3});

        await waitFor(() => expect(enableAiProviderMock).toHaveBeenCalledWith({enable: true, environment: 1, id: 3}));
    });

    it('hands the enable result to onSuccess', async () => {
        const onSuccess = vi.fn();

        enableAiProviderMock.mockResolvedValue(undefined);

        const {result} = renderHook(() => useEnableAiProviderMutation({onSuccess}), {
            wrapper: createTestQueryClientWrapper(),
        });

        result.current.mutate({enable: false, environment: 1, id: 3});

        await waitFor(() => expect(onSuccess).toHaveBeenCalled());
    });
});
