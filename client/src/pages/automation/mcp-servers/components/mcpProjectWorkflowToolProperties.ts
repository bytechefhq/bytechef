import {
    ToolEligibleWorkflowPropertiesDocument,
    ToolEligibleWorkflowPropertiesQuery,
    ToolEligibleWorkflowPropertiesQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {PropertyAllType} from '@/shared/types';
import {QueryClient} from '@tanstack/react-query';

export function toToolEligibleWorkflowProperties(
    propertiesData: ToolEligibleWorkflowPropertiesQuery | undefined
): PropertyAllType[] {
    return (propertiesData?.toolEligibleWorkflowProperties ?? []).filter(Boolean) as unknown as PropertyAllType[];
}

export async function fetchToolEligibleWorkflowProperties(
    queryClient: QueryClient,
    variables: ToolEligibleWorkflowPropertiesQueryVariables
): Promise<PropertyAllType[]> {
    const propertiesData = await queryClient.fetchQuery({
        queryFn: fetcher<ToolEligibleWorkflowPropertiesQuery, ToolEligibleWorkflowPropertiesQueryVariables>(
            ToolEligibleWorkflowPropertiesDocument,
            variables
        ),
        queryKey: ['toolEligibleWorkflowProperties', variables],
    });

    return toToolEligibleWorkflowProperties(propertiesData);
}
