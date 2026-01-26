import {useComponentDefinitionSearchQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

export const useGetComponentDefinitionsWithActionsQuery = (searchQuery?: string) => {
    const trimmedQuery = searchQuery?.trim();
    const hasSearchQuery = Boolean(trimmedQuery && trimmedQuery.length > 0);

    const result = useComponentDefinitionSearchQuery(
        {query: trimmedQuery || ''},
        {
            enabled: hasSearchQuery,
            gcTime: 30 * 60 * 1000,
            staleTime: 10 * 60 * 1000,
        }
    );

    const transformedData = useMemo(() => {
        if (!result.data?.componentDefinitionSearch) return null;

        return result.data.componentDefinitionSearch.map((component) => ({
            ...component,
            clusterElementsCount: {},
        }));
    }, [result.data]);

    return {
        ...result,
        data: transformedData,
    };
};
