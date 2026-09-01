import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {renderHook, waitFor} from '@testing-library/react';
import {ReactNode} from 'react';
import {describe, expect, it, vi} from 'vitest';

import {ProjectCategoryKeys, useGetProjectCategoriesQuery} from '../projectCategories.queries';

const {getProjectCategoriesMock} = vi.hoisted(() => ({getProjectCategoriesMock: vi.fn()}));

vi.mock('@/shared/middleware/automation/configuration', () => ({
    CategoryApi: class {
        getProjectCategories = getProjectCategoriesMock;
    },
}));

const wrapper = ({children}: {children: ReactNode}) => (
    <QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}}})}>
        {children}
    </QueryClientProvider>
);

describe('projectCategories.queries', () => {
    it('keys the cache by workspace, so two workspaces cannot share an entry', () => {
        expect(ProjectCategoryKeys.projectCategories(1)).toEqual(['projectCategories', 1]);
        expect(ProjectCategoryKeys.projectCategories(1)).not.toEqual(ProjectCategoryKeys.projectCategories(2));
    });

    it('asks for the categories of the given workspace', async () => {
        getProjectCategoriesMock.mockResolvedValue([{id: 10, name: 'Records'}]);

        const {result} = renderHook(() => useGetProjectCategoriesQuery(7), {wrapper});

        await waitFor(() => expect(result.current.isSuccess).toBe(true));

        expect(getProjectCategoriesMock).toHaveBeenCalledWith({id: 7});
        expect(result.current.data).toEqual([{id: 10, name: 'Records'}]);
    });
});
