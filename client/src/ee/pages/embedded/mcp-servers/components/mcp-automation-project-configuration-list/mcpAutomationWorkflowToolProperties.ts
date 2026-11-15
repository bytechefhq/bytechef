import {
    ToolEligibleAutomationProjectConfigurationWorkflowPropertiesDocument,
    ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery,
    ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {PropertyAllType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

export function toToolEligibleAutomationWorkflowProperties(
    propertiesData: ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery | undefined
): PropertyAllType[] {
    return (propertiesData?.toolEligibleAutomationProjectConfigurationWorkflowProperties ?? []).filter(
        Boolean
    ) as unknown as PropertyAllType[];
}

export async function fetchToolEligibleAutomationWorkflowProperties(
    queryClient: QueryClient,
    variables: ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQueryVariables
): Promise<PropertyAllType[]> {
    const propertiesData = await queryClient.fetchQuery({
        queryFn: fetcher<
            ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQuery,
            ToolEligibleAutomationProjectConfigurationWorkflowPropertiesQueryVariables
        >(ToolEligibleAutomationProjectConfigurationWorkflowPropertiesDocument, variables),
        queryKey: ['toolEligibleAutomationProjectConfigurationWorkflowProperties', variables],
    });

    return toToolEligibleAutomationWorkflowProperties(propertiesData);
}
