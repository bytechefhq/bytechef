import {LoadPropertyOptionsType, isEmptyToolValue} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import {ClusterElementOptionsDocument, ClusterElementOptionsQuery} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {PropertyAllType} from '@/shared/types';
import {QueryClient, useQueryClient} from '@tanstack/react-query';
import {useMemo} from 'react';

const OPTIONS_STALE_TIME_MS = 60_000;

interface UseMcpToolOptionsLoaderParamsI {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    toolName: string;
}

interface CreateMcpToolOptionsLoaderParamsI extends UseMcpToolOptionsLoaderParamsI {
    queryClient: QueryClient;
}

export function createMcpToolOptionsLoader({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    queryClient,
    toolName,
}: CreateMcpToolOptionsLoaderParamsI): LoadPropertyOptionsType {
    return async (property: PropertyAllType, path: string, values: Record<string, unknown>) => {
        if (connectionRequired && !connectionId) {
            return null;
        }

        const lookupDependsOnPaths =
            (property as unknown as {optionsDataSource?: {optionsLookupDependsOn?: string[]}}).optionsDataSource
                ?.optionsLookupDependsOn ?? [];

        const propertyName = property.name || path;

        const inputParameters = Object.fromEntries(
            Object.entries(values).filter(
                ([, parameterValue]) =>
                    !(
                        typeof parameterValue === 'string' &&
                        (parameterValue.startsWith('=') || parameterValue.includes('${'))
                    )
            )
        );

        const lookupDependsOnValues = lookupDependsOnPaths.map((dependencyPath) => inputParameters[dependencyPath]);

        if (lookupDependsOnValues.some((dependencyValue) => isEmptyToolValue(dependencyValue))) {
            return null;
        }

        const variables = {
            clusterElementName: toolName,
            componentName,
            componentVersion,
            connectionId: connectionId || undefined,
            inputParameters,
            lookupDependsOnPaths,
            propertyName,
        };

        try {
            const result = await queryClient.fetchQuery({
                queryFn: fetcher<ClusterElementOptionsQuery, typeof variables>(
                    ClusterElementOptionsDocument,
                    variables
                ),
                queryKey: [
                    'clusterElementOptions',
                    {
                        clusterElementName: toolName,
                        componentName,
                        componentVersion,
                        connectionId: connectionId || undefined,
                        lookupDependsOnValues: lookupDependsOnPaths.length ? lookupDependsOnValues : undefined,
                        propertyName,
                    },
                ],
                staleTime: OPTIONS_STALE_TIME_MS,
            });

            const options = result?.clusterElementOptions;

            return options && options.length > 0 ? options : null;
        } catch {
            return null;
        }
    };
}

export default function useMcpToolOptionsLoader({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    toolName,
}: UseMcpToolOptionsLoaderParamsI): LoadPropertyOptionsType {
    const queryClient = useQueryClient();

    return useMemo(
        () =>
            createMcpToolOptionsLoader({
                componentName,
                componentVersion,
                connectionId,
                connectionRequired,
                queryClient,
                toolName,
            }),
        [componentName, componentVersion, connectionId, connectionRequired, queryClient, toolName]
    );
}
