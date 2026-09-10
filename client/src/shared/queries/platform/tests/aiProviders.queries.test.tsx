import {createTestQueryClientWrapper, renderHook, resetAll, waitFor} from '@/shared/util/test-utils';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {AiProviderKeys, useGetAiProvidersQuery} from '../aiProviders.queries';

const {getAiProvidersMock} = vi.hoisted(() => ({getAiProvidersMock: vi.fn()}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    AiProviderApi: class {
        getAiProviders = getAiProvidersMock;
    },
}));

describe('aiProviders queries', () => {
    afterEach(() => {
        resetAll();
    });

    it('keys the cache by environment so two environments never share a list', () => {
        expect(AiProviderKeys.aiProviders(1)).toEqual(['aiProviders', 1]);
        expect(AiProviderKeys.aiProviders(2)).not.toEqual(AiProviderKeys.aiProviders(1));
    });

    it('fetches the providers for the given environment', async () => {
        getAiProvidersMock.mockResolvedValue([{id: 1, name: 'openai'}]);

        const {result} = renderHook(() => useGetAiProvidersQuery(4), {wrapper: createTestQueryClientWrapper()});

        await waitFor(() => expect(result.current.data).toEqual([{id: 1, name: 'openai'}]));

        expect(getAiProvidersMock).toHaveBeenCalledWith({environment: 4});
    });
});
