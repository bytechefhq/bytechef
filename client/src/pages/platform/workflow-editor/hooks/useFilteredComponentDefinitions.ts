import {ComponentDefinitionBasic} from '@/shared/middleware/platform/configuration';
import {
    ComponentDefinitionWithActionsProps,
    useGetComponentDefinitionsWithActionsQuery,
} from '@/shared/queries/platform/componentDefinitionsGraphQL.queries';
import {Dispatch, SetStateAction, useMemo, useState} from 'react';
import {useDebounce} from 'use-debounce';

type UseFilteredComponentDefinitionsReturnType = {
    componentsWithActions: Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>;
    filter: string;
    isSearchFetching: boolean;
    setFilter: Dispatch<SetStateAction<string>>;
    trimmedFilter: string;
};

export const useFilteredComponentDefinitions = (
    componentDefinitions: Array<ComponentDefinitionBasic | ComponentDefinitionWithActionsProps>
): UseFilteredComponentDefinitionsReturnType => {
    const [filter, setFilter] = useState('');

    const [debouncedFilter] = useDebounce(filter, 300);

    const trimmedFilter = debouncedFilter.trim();

    const {data: searchedComponentDefinitions, isFetching: isSearchFetching} =
        useGetComponentDefinitionsWithActionsQuery(trimmedFilter);

    const componentsWithActions = useMemo(() => {
        if (trimmedFilter && searchedComponentDefinitions) {
            return searchedComponentDefinitions;
        }

        return componentDefinitions;
    }, [trimmedFilter, searchedComponentDefinitions, componentDefinitions]);

    return {componentsWithActions, filter, isSearchFetching, setFilter, trimmedFilter};
};
