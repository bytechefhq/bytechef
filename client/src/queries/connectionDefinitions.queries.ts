import {useQuery} from '@tanstack/react-query';

import {
    ConnectionDefinitionModel,
    ConnectionDefinitionsApi,
    GetComponentConnectionDefinitionRequest,
    GetComponentConnectionDefinitionsRequest,
} from '../middleware/core/workflow/configuration';

export const ConnectDefinitionKeys = {
    connectionDefinition: ['connectionDefinition'],
    connectionDefinitionDetails: (
        request?: GetComponentConnectionDefinitionRequest
    ) => [...ConnectDefinitionKeys.connectionDefinition, request],
    connectionDefinitions: (
        request: GetComponentConnectionDefinitionsRequest
    ) => ['connectionDefinitions', request],
};

export const useGetConnectionDefinitionQuery = (
    request?: GetComponentConnectionDefinitionRequest
) =>
    useQuery<ConnectionDefinitionModel, Error>(
        ConnectDefinitionKeys.connectionDefinitionDetails(request),
        () =>
            new ConnectionDefinitionsApi().getComponentConnectionDefinition(
                request!
            ),
        {
            enabled: !!request?.componentName,
        }
    );

export const useGetConnectionDefinitionsQuery = (
    request: GetComponentConnectionDefinitionsRequest,
    enabledCondition?: boolean
) =>
    useQuery<ConnectionDefinitionModel[], Error>(
        ConnectDefinitionKeys.connectionDefinitions(request),
        () =>
            new ConnectionDefinitionsApi().getComponentConnectionDefinitions(
                request
            ),
        {enabled: false || enabledCondition}
    );
