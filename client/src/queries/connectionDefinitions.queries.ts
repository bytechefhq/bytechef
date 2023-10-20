import {useQuery} from '@tanstack/react-query';

import {
    ComponentDefinitionsApi,
    ConnectionDefinitionModel,
    GetComponentConnectionDefinitionRequest,
} from '../middleware/definition-registry';

export const ConnectDefinitionKeys = {
    connectionDefinition: ['connectionDefinition'] as const,
    connectionDefinitionDetails: (
        request?: GetComponentConnectionDefinitionRequest
    ) => [...ConnectDefinitionKeys.connectionDefinition, request],
};

export const useGetConnectionDefinitionQuery = (
    request?: GetComponentConnectionDefinitionRequest
) =>
    useQuery<ConnectionDefinitionModel, Error>(
        ConnectDefinitionKeys.connectionDefinitionDetails(request),
        () =>
            new ComponentDefinitionsApi().getComponentConnectionDefinition(
                request!
            ),
        {
            enabled: !!request?.componentName,
        }
    );
