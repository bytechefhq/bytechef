import {useComponentDefinitionSearchQuery} from '@/shared/middleware/graphql';
import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {useMemo} from 'react';

export interface ComponentDefinitionWithActionsProps extends ComponentDefinitionBasic {
    actions?: Array<{
        name?: string;
        title?: string;
        description?: string;
    }>;
    triggers?: Array<{
        name?: string;
        title?: string;
        description?: string;
    }>;
    clusterElements?: Array<{
        type?: {
            name?: string;
        };
    }>;
}

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

    const transformedData = useMemo<ComponentDefinitionWithActionsProps[] | null>(() => {
        if (!result.data?.componentDefinitionSearch) {
            return null;
        }

        return result.data.componentDefinitionSearch.map((component) => ({
            ...component,
            clusterElementsCount: {},
        })) as ComponentDefinitionWithActionsProps[];
    }, [result.data]);

    return {
        ...result,
        data: transformedData,
    };
};
