import {ModeType, useModeTypeStore} from '@/pages/home/stores/useModeTypeStore';

/* eslint-disable sort-keys */
import {
    ComponentDefinition,
    ComponentDefinitionApi,
    ComponentDefinitionBasic,
    GetComponentDefinitionRequest,
    GetComponentDefinitionsModeTypeEnum,
    GetDataStreamComponentDefinitionsRequest,
} from '@/shared/middleware/platform/configuration';
import {useQuery} from '@tanstack/react-query';

export interface GetComponentDefinitionsRequestI {
    actionDefinitions?: boolean;
    connectionDefinitions?: boolean;
    triggerDefinitions?: boolean;
    include?: Array<string>;
}

export const ComponentDefinitionKeys = {
    componentDefinition: (request: GetComponentDefinitionRequest) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request.componentName,
        request.componentVersion,
    ],
    componentDefinitions: ['componentDefinitions'] as const,
    filteredComponentDefinitions: (request?: GetComponentDefinitionsRequestI) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request,
    ],
    filteredDataStreamComponentDefinitions: (request?: GetDataStreamComponentDefinitionsRequest) => [
        ...ComponentDefinitionKeys.componentDefinitions,
        request?.componentType,
    ],
};

export const useGetComponentDefinitionQuery = (request: GetComponentDefinitionRequest, enabled?: boolean) =>
    useQuery<ComponentDefinition, Error>({
        queryKey: ComponentDefinitionKeys.componentDefinition(request),
        queryFn: () => new ComponentDefinitionApi().getComponentDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });

export const useGetComponentDefinitionsQuery = (request: GetComponentDefinitionsRequestI, enabled?: boolean) => {
    const {currentType} = useModeTypeStore();

    return useQuery<ComponentDefinitionBasic[], Error>({
        queryKey: ComponentDefinitionKeys.filteredComponentDefinitions(request),
        queryFn: () =>
            new ComponentDefinitionApi().getComponentDefinitions({
                ...request,
                modeType:
                    currentType === ModeType.AUTOMATION
                        ? GetComponentDefinitionsModeTypeEnum.Automation
                        : GetComponentDefinitionsModeTypeEnum.Embedded,
            }),
        enabled: enabled === undefined ? true : enabled,
    });
};

export const useGetDataStreamComponentDefinitions = (
    request: GetDataStreamComponentDefinitionsRequest,
    enabled?: boolean
) =>
    useQuery<ComponentDefinitionBasic[], Error>({
        queryKey: ComponentDefinitionKeys.filteredDataStreamComponentDefinitions(request),
        queryFn: () => new ComponentDefinitionApi().getDataStreamComponentDefinitions(request),
        enabled: enabled === undefined ? true : enabled,
    });
