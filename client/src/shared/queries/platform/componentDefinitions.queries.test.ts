import {
    ComponentDefinitionKeys,
    useGetComponentDefinitionVersionsQuery,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {type ReactNode, createElement} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

const mockGetComponentDefinitionVersions = vi.fn();

vi.mock('@/shared/middleware/platform/configuration', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/platform/configuration')>()),
    ComponentDefinitionApi: class {
        getComponentDefinitionVersions = mockGetComponentDefinitionVersions;
    },
}));

const createWrapper = () => {
    const queryClient = new QueryClient({defaultOptions: {queries: {retry: false}}});

    return ({children}: {children: ReactNode}) => createElement(QueryClientProvider, {client: queryClient}, children);
};

describe('useGetComponentDefinitionVersionsQuery', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should key the versions of a component apart from the component itself', () => {
        expect(ComponentDefinitionKeys.componentDefinitionVersions({componentName: 'slack'})).toEqual([
            'componentDefinitions',
            'slack',
            'versions',
        ]);
    });

    it('should return the versions the component registers', async () => {
        const versions = [
            {name: 'slack', version: 1},
            {name: 'slack', version: 2},
        ];

        mockGetComponentDefinitionVersions.mockResolvedValue(versions);

        const {result} = renderHook(() => useGetComponentDefinitionVersionsQuery({componentName: 'slack'}), {
            wrapper: createWrapper(),
        });

        await waitFor(() => expect(result.current.data).toEqual(versions));

        expect(mockGetComponentDefinitionVersions).toHaveBeenCalledWith({componentName: 'slack'});
    });

    it('should not fetch while it is disabled', () => {
        renderHook(() => useGetComponentDefinitionVersionsQuery({componentName: 'slack'}, false), {
            wrapper: createWrapper(),
        });

        expect(mockGetComponentDefinitionVersions).not.toHaveBeenCalled();
    });
});
