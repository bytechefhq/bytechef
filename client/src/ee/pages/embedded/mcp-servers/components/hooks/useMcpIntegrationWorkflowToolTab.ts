import {useToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {toToolEligibleIntegrationWorkflowProperties} from '../mcpIntegrationWorkflowToolProperties';

interface UseMcpIntegrationWorkflowToolTabParamsI {
    integrationInstanceConfigurationId: string;
    workflowId: string;
}

export default function useMcpIntegrationWorkflowToolTab({
    integrationInstanceConfigurationId,
    workflowId,
}: UseMcpIntegrationWorkflowToolTabParamsI) {
    const {data: propertiesData, isLoading} = useToolEligibleIntegrationInstanceConfigurationWorkflowPropertiesQuery({
        integrationInstanceConfigurationId,
        workflowId,
    });

    const properties = useMemo(() => toToolEligibleIntegrationWorkflowProperties(propertiesData), [propertiesData]);

    return {isLoading, properties};
}
