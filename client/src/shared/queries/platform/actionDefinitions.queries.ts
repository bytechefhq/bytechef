import {
    ActionDefinitionApi,
    ActionDefinitionModel,
    GetComponentActionDefinitionRequest,
} from '@/shared/middleware/platform/configuration';

/* eslint-disable sort-keys */
import {useQuery} from '@tanstack/react-query';

export const ActionDefinitionKeys = {
    actionDefinition: (request: GetComponentActionDefinitionRequest) => [
        ...ActionDefinitionKeys.actionDefinitions,
        request.componentName,
        request.componentVersion,
        request.actionName,
    ],
    actionDefinitions: ['actionDefinitions'] as const,
};

export const useGetComponentActionDefinitionQuery = (request: GetComponentActionDefinitionRequest, enabled?: boolean) =>
    useQuery<ActionDefinitionModel, Error>({
        queryKey: ActionDefinitionKeys.actionDefinition(request),
        queryFn: () => new ActionDefinitionApi().getComponentActionDefinition(request),
        enabled: enabled === undefined ? true : enabled,
    });
