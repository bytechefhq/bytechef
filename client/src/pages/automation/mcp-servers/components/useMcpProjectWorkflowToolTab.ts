import {useToolEligibleWorkflowPropertiesQuery} from '@/shared/middleware/graphql';
import {useMemo} from 'react';

import {toToolEligibleWorkflowProperties} from './mcpProjectWorkflowToolProperties';

interface UseMcpProjectWorkflowToolTabParamsI {
    projectId: number;
    workflowId: string;
}

export default function useMcpProjectWorkflowToolTab({projectId, workflowId}: UseMcpProjectWorkflowToolTabParamsI) {
    const {data: propertiesData, isLoading} = useToolEligibleWorkflowPropertiesQuery({
        projectId: String(projectId),
        workflowId,
    });

    const properties = useMemo(() => toToolEligibleWorkflowProperties(propertiesData), [propertiesData]);

    return {isLoading, properties};
}
