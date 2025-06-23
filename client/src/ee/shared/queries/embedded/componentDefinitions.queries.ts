import {ComponentDefinitionApi, ComponentDefinitionBasic} from '@/ee/shared/middleware/embedded/configuration';
import {
    ComponentDefinitionKeys,
    GetComponentDefinitionsRequestI,
} from '@/shared/queries/platform/componentDefinitions.queries';
import {useQuery} from '@tanstack/react-query';

export const useGetComponentDefinitionsQuery = (request: GetComponentDefinitionsRequestI, enabled?: boolean) => {
    return useQuery<ComponentDefinitionBasic[], Error>({
        enabled: enabled === undefined ? true : enabled,
        queryFn: () =>
            new ComponentDefinitionApi().getComponentDefinitions({
                ...request,
            }),
        queryKey: ComponentDefinitionKeys.filteredComponentDefinitions(request),
    });
};
