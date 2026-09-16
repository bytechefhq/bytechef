import {renderHook} from '@/shared/util/test-utils';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useGetComponentDefinitionsWithActionsQuery} from '../componentDefinitionsGraphQL.queries';

const hoisted = vi.hoisted(() => ({
    queryResult: {data: undefined as unknown, isFetching: false},
    useComponentDefinitionSearchQueryMock: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    PlatformType: {
        Automation: 'AUTOMATION',
        Embedded: 'EMBEDDED',
    },
    useComponentDefinitionSearchQuery: (...args: unknown[]) => {
        hoisted.useComponentDefinitionSearchQueryMock(...args);

        return hoisted.queryResult;
    },
}));

describe('useGetComponentDefinitionsWithActionsQuery', () => {
    beforeEach(() => {
        hoisted.queryResult = {data: undefined, isFetching: false};
        hoisted.useComponentDefinitionSearchQueryMock.mockReset();
    });

    it('searches with the given platform type and trimmed query', async () => {
        const {PlatformType} = await import('@/shared/middleware/graphql');

        renderHook(() => useGetComponentDefinitionsWithActionsQuery(PlatformType.Embedded, '  webhook  '));

        expect(hoisted.useComponentDefinitionSearchQueryMock).toHaveBeenCalledWith(
            {platformType: 'EMBEDDED', query: 'webhook'},
            expect.objectContaining({enabled: true})
        );
    });

    it('disables the search when the query is blank', async () => {
        const {PlatformType} = await import('@/shared/middleware/graphql');

        renderHook(() => useGetComponentDefinitionsWithActionsQuery(PlatformType.Automation, '   '));

        expect(hoisted.useComponentDefinitionSearchQueryMock).toHaveBeenCalledWith(
            {platformType: 'AUTOMATION', query: ''},
            expect.objectContaining({enabled: false})
        );
    });

    it('returns null data when there are no search results', async () => {
        const {PlatformType} = await import('@/shared/middleware/graphql');

        const {result} = renderHook(() => useGetComponentDefinitionsWithActionsQuery(PlatformType.Automation, 'gmail'));

        expect(result.current.data).toBeNull();
    });

    it('returns the component definitions from the search results', async () => {
        const componentDefinitions = [{actions: [{name: 'sendEmail'}], name: 'gmail', version: 1}];

        hoisted.queryResult = {data: {componentDefinitionSearch: componentDefinitions}, isFetching: true};

        const {PlatformType} = await import('@/shared/middleware/graphql');

        const {result} = renderHook(() => useGetComponentDefinitionsWithActionsQuery(PlatformType.Automation, 'gmail'));

        expect(result.current.data).toEqual(componentDefinitions);
        expect(result.current.isFetching).toBe(true);
    });
});
