import {act, renderHook} from '@testing-library/react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => {
    const mockQueryResult = {
        data: null as Array<{actionsCount: number; name: string; triggersCount: number; version: number}> | null,
        isFetching: false,
    };

    return {mockQueryResult, platformType: 'EMBEDDED', queryArgs: [] as unknown[][]};
});

vi.mock('use-debounce', () => ({
    useDebounce: (value: string) => [value],
}));

vi.mock('@/shared/queries/platform/componentDefinitionsGraphQL.queries', () => ({
    useGetComponentDefinitionsWithActionsQuery: (...args: unknown[]) => {
        hoisted.queryArgs.push(args);

        return hoisted.mockQueryResult;
    },
}));

vi.mock('@/pages/platform/workflow-editor/providers/workflowEditorProvider', () => ({
    useWorkflowEditor: () => ({platformType: hoisted.platformType}),
}));

const componentDefinitions = [
    {actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1},
    {actionsCount: 2, name: 'slack', triggersCount: 0, version: 1},
    {actionsCount: 1, name: 'github', triggersCount: 2, version: 1},
];

describe('useFilteredComponentDefinitions', () => {
    beforeEach(() => {
        hoisted.mockQueryResult.data = null;
        hoisted.mockQueryResult.isFetching = false;
        hoisted.queryArgs.length = 0;
    });

    afterEach(() => {
        vi.clearAllMocks();
    });

    it('should return original componentDefinitions when filter is empty', async () => {
        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        expect(result.current.componentsWithActions).toBe(componentDefinitions);
        expect(result.current.filter).toBe('');
        expect(result.current.isSearchFetching).toBe(false);
    });

    it('should return search results when filter is set and data is available', async () => {
        const searchResults = [{actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1}];

        hoisted.mockQueryResult.data = searchResults;
        hoisted.mockQueryResult.isFetching = false;

        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        act(() => {
            result.current.setFilter('gmail');
        });

        expect(result.current.componentsWithActions).toBe(searchResults);
    });

    it('should return isSearchFetching as true when query is in flight', async () => {
        hoisted.mockQueryResult.isFetching = true;

        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        expect(result.current.isSearchFetching).toBe(true);
    });

    it('should return previous search results while fetching new ones (keepPreviousData)', async () => {
        const previousResults = [{actionsCount: 3, name: 'gmail', triggersCount: 1, version: 1}];

        hoisted.mockQueryResult.data = previousResults;
        hoisted.mockQueryResult.isFetching = true;

        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        act(() => {
            result.current.setFilter('gma');
        });

        expect(result.current.componentsWithActions).toBe(previousResults);
        expect(result.current.isSearchFetching).toBe(true);
    });

    it('should return original componentDefinitions when filter is whitespace only', async () => {
        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        act(() => {
            result.current.setFilter('   ');
        });

        expect(result.current.componentsWithActions).toBe(componentDefinitions);
    });

    it('should expose trimmedFilter as debounced and trimmed value', async () => {
        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        act(() => {
            result.current.setFilter('  gmail  ');
        });

        expect(result.current.trimmedFilter).toBe('gmail');
    });

    it('should search with the platform type provided by the workflow editor', async () => {
        const {useFilteredComponentDefinitions} = await import('../useFilteredComponentDefinitions');

        const {result} = renderHook(() => useFilteredComponentDefinitions(componentDefinitions));

        act(() => {
            result.current.setFilter('webhook');
        });

        expect(hoisted.queryArgs.at(-1)).toEqual(['EMBEDDED', 'webhook']);
    });
});
