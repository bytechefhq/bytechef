import {createMcpToolOptionsLoader} from '@/pages/platform/mcp-servers/hooks/useMcpToolOptionsLoader';
import useToolSeeding from '@/pages/platform/mcp-servers/hooks/useToolSeeding';
import {
    collectHiddenToolPaths,
    collectPropertyDefaults,
    computeAiFillValues,
} from '@/pages/platform/mcp-servers/utils/mcpToolAiFill';
import {
    ClusterElementDefinitionDocument,
    ClusterElementDefinitionQuery,
    ClusterElementDefinitionQueryVariables,
    ComponentOperationType,
    ComponentPropertyDisplayConditionsDocument,
    ComponentPropertyDisplayConditionsQuery,
    ComponentPropertyDisplayConditionsQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {PropertyAllType} from '@/shared/types';
import {QueryClient, useQueryClient} from '@tanstack/react-query';

interface UseMcpToolSeedingParamsI {
    componentName: string;
    componentVersion: number;
    connectionId?: string | null;
    connectionRequired?: boolean;
    newToolNames: string[];
}

interface FetchDisplayConditionsParamsI {
    componentName: string;
    componentVersion: number;
    parameters: Record<string, unknown>;
    queryClient: QueryClient;
    toolName: string;
}

function hasDisplayCondition(properties: PropertyAllType[]): boolean {
    return properties.some((property) => !!property.displayCondition || hasDisplayCondition(property.properties ?? []));
}

async function fetchDisplayConditions({
    componentName,
    componentVersion,
    parameters,
    queryClient,
    toolName,
}: FetchDisplayConditionsParamsI): Promise<Record<string, boolean> | undefined> {
    const variables: ComponentPropertyDisplayConditionsQueryVariables = {
        componentName,
        componentVersion,
        operationName: toolName,
        operationType: ComponentOperationType.ClusterElement,
        parameters,
    };

    try {
        const result = await queryClient.fetchQuery({
            queryFn: fetcher<ComponentPropertyDisplayConditionsQuery, ComponentPropertyDisplayConditionsQueryVariables>(
                ComponentPropertyDisplayConditionsDocument,
                variables
            ),
            queryKey: ['componentPropertyDisplayConditions', variables],
        });

        return result?.componentPropertyDisplayConditions as Record<string, boolean> | undefined;
    } catch {
        return undefined;
    }
}

export default function useMcpToolSeeding({
    componentName,
    componentVersion,
    connectionId,
    connectionRequired,
    newToolNames,
}: UseMcpToolSeedingParamsI) {
    const queryClient = useQueryClient();

    const seedTool = async (toolName: string): Promise<Record<string, string>> => {
        const variables: ClusterElementDefinitionQueryVariables = {
            clusterElementName: toolName,
            componentName,
            componentVersion,
        };

        const definition = await queryClient.fetchQuery({
            queryFn: fetcher<ClusterElementDefinitionQuery, ClusterElementDefinitionQueryVariables>(
                ClusterElementDefinitionDocument,
                variables
            ),
            queryKey: ['clusterElementDefinition', variables],
        });

        const properties = (definition?.clusterElementDefinition?.properties ?? []) as unknown as PropertyAllType[];

        const propertyDefaults = collectPropertyDefaults(properties);

        const hasConditionalProperties = hasDisplayCondition(properties);

        const displayConditions = hasConditionalProperties
            ? await fetchDisplayConditions({
                  componentName,
                  componentVersion,
                  parameters: propertyDefaults,
                  queryClient,
                  toolName,
              })
            : undefined;

        return computeAiFillValues({
            hiddenPaths: collectHiddenToolPaths(properties, displayConditions),
            includeAdvanced: false,
            includeConditional: !hasConditionalProperties || displayConditions !== undefined,
            loadOptions: createMcpToolOptionsLoader({
                componentName,
                componentVersion,
                connectionId,
                connectionRequired,
                queryClient,
                toolName,
            }),
            properties,
            values: propertyDefaults,
        });
    };

    const {seededValuesByKey, waitForSeeding} = useToolSeeding({
        keys: newToolNames,
        seed: seedTool,
        seedKey: `${componentName}:${componentVersion}:${connectionId ?? ''}:${connectionRequired ?? ''}`,
    });

    return {
        aiValuesByTool: seededValuesByKey,
        waitForSeeding,
    };
}
