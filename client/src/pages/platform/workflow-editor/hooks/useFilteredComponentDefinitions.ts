import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionWithActionsProps,
    useGetComponentDefinitionsWithActionsQuery,
} from '@/shared/queries/platform/componentDefinitionsGraphQL.queries';
import {useMemo, useState} from 'react';
import {useDebounce} from 'use-debounce';

interface UseFilteredComponentDefinitionsProps {
    componentsWithActions: Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>;
    filter: string;
    isSearchLoading: boolean;
    setFilter: React.Dispatch<React.SetStateAction<string>>;
    trimmedFilter: string;
}

export const useFilteredComponentDefinitions = (
    componentDefinitions: Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>
): UseFilteredComponentDefinitionsProps => {
    const [filter, setFilter] = useState('');
    const [debouncedFilter] = useDebounce(filter, 300);
    const trimmedFilter = debouncedFilter.trim();

    const {data: searchedComponentDefinitions, isLoading: isSearchLoading} =
        useGetComponentDefinitionsWithActionsQuery(trimmedFilter);

    const componentsWithActions = useMemo(() => {
        if (trimmedFilter && searchedComponentDefinitions && !isSearchLoading) {
            return searchedComponentDefinitions;
        }
        return componentDefinitions;
    }, [trimmedFilter, searchedComponentDefinitions, isSearchLoading, componentDefinitions]);

    return {componentsWithActions, filter, isSearchLoading, setFilter, trimmedFilter};
};
