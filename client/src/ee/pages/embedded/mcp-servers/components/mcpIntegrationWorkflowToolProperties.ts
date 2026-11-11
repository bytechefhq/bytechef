import {
    ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesDocument,
    ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery,
    ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {PropertyAllType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

export function toToolEligibleIntegrationWorkflowProperties(
    propertiesData: ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery | undefined
): PropertyAllType[] {
    return (propertiesData?.toolEligibleIntegrationInstanceConfigurationWorkflowProperties ?? []).filter(
        Boolean
    ) as unknown as PropertyAllType[];
}

export async function fetchToolEligibleIntegrationWorkflowProperties(
    queryClient: QueryClient,
    variables: ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables
): Promise<PropertyAllType[]> {
    const propertiesData = await queryClient.fetchQuery({
        queryFn: fetcher<
            ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery,
            ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQueryVariables
        >(ToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesDocument, variables),
        queryKey: ['toolEligibleIntegrationInstanceConfigurationWorkflowProperties', variables],
    });

    return toToolEligibleIntegrationWorkflowProperties(propertiesData);
}
