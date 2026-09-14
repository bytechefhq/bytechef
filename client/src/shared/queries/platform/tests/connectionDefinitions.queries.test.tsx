import {useGetConnectionDefinitionQuery} from '@/shared/queries/platform/connectionDefinitions.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    getComponentConnectionDefinition: vi.fn(),
}));

vi.mock('@/shared/middleware/platform/configuration', () => ({
    ConnectionDefinitionApi: class {
        getComponentConnectionDefinition = hoisted.getComponentConnectionDefinition;
    },
}));

const createWrapper = (queryClient: QueryClient) => {
    const Wrapper = ({children}: {children: ReactNode}) => (
        <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );

    return Wrapper;
};

describe('useGetConnectionDefinitionQuery', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        hoisted.getComponentConnectionDefinition.mockReset();

        queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});
    });

    it('does not fetch or cache an error when componentName is missing', async () => {
        const {result} = renderHook(
            () => useGetConnectionDefinitionQuery({componentName: undefined as unknown as string}),
            {wrapper: createWrapper(queryClient)}
        );

        await new Promise((resolve) => setTimeout(resolve, 10));

        expect(hoisted.getComponentConnectionDefinition).not.toHaveBeenCalled();
        expect(result.current.fetchStatus).toBe('idle');
        expect(result.current.error).toBeNull();
        expect(queryClient.getQueryState(['connectionDefinitions', undefined, undefined])?.error ?? null).toBeNull();
    });

    it('fetches when componentName is provided', async () => {
        hoisted.getComponentConnectionDefinition.mockResolvedValue({componentName: 'affinity', version: 1});

        const {result} = renderHook(() => useGetConnectionDefinitionQuery({componentName: 'affinity'}), {
            wrapper: createWrapper(queryClient),
        });

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(hoisted.getComponentConnectionDefinition).toHaveBeenCalledWith({componentName: 'affinity'});
    });

    it('stays disabled when the caller disables it', async () => {
        renderHook(() => useGetConnectionDefinitionQuery({componentName: 'affinity'}, false), {
            wrapper: createWrapper(queryClient),
        });

        await new Promise((resolve) => setTimeout(resolve, 10));

        expect(hoisted.getComponentConnectionDefinition).not.toHaveBeenCalled();
    });
});
