import {useQuery} from '@tanstack/react-query';
import {
    ConnectionDefinitionModel,
    ConnectionDefinitionsApi,
} from '../middleware/definition-registry';

interface Request {
    componentName: string;
    componentVersion: number;
}

export const ConnectDefinitionKeys = {
    connectionDefinition: ['connectionDefinition'] as const,
    connectionDefinitionDetails: (request?: Request) => [
        ...ConnectDefinitionKeys.connectionDefinition,
        request,
    ],
};

export const useGetConnectionDefinitionQuery = (request?: Request) =>
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
