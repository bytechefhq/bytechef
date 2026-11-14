import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {useMemo} from 'react';

interface UseMcpIntegrationWorkflowsFormProps {
    eligibleWorkflowIds?: string[];
    integrationInstanceConfigurationId?: string;
}

export default function useMcpIntegrationWorkflowsForm({
    eligibleWorkflowIds,
    integrationInstanceConfigurationId,
}: UseMcpIntegrationWorkflowsFormProps) {
    const {data: integrationInstanceConfiguration} = useGetIntegrationInstanceConfigurationQuery(
        Number(integrationInstanceConfigurationId),
        !!integrationInstanceConfigurationId
    );

    const {data: integrationVersionWorkflows} = useGetIntegrationVersionWorkflowsQuery(
        integrationInstanceConfiguration?.integrationId ?? 0,
        integrationInstanceConfiguration?.integrationVersion ?? 0,
        true,
        !!integrationInstanceConfiguration?.integrationId && !!integrationInstanceConfiguration?.integrationVersion
    );

    const workflows = useMemo(() => {
        if (!integrationVersionWorkflows || !eligibleWorkflowIds) {
            return undefined;
        }

        return integrationVersionWorkflows.filter((workflow) => eligibleWorkflowIds.includes(workflow.id!));
    }, [eligibleWorkflowIds, integrationVersionWorkflows]);

    const enabledWorkflowIds = useMemo(
        () =>
            (integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows ?? []).flatMap(
                (integrationInstanceConfigurationWorkflow) =>
                    integrationInstanceConfigurationWorkflow.enabled &&
                    integrationInstanceConfigurationWorkflow.workflowId
                        ? [integrationInstanceConfigurationWorkflow.workflowId]
                        : []
            ),
        [integrationInstanceConfiguration?.integrationInstanceConfigurationWorkflows]
    );

    return {enabledWorkflowIds, workflows};
}
