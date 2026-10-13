import {
    ComponentConnection,
    IntegrationInstanceConfiguration,
    IntegrationInstanceConfigurationWorkflow,
    Workflow,
} from '@/ee/shared/middleware/embedded/configuration';
import {useGetIntegrationInstanceConfigurationQuery} from '@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries';
import {useGetIntegrationVersionWorkflowsQuery} from '@/ee/shared/queries/embedded/integrationWorkflows.queries';
import {WorkflowConfigurationInput} from '@/shared/middleware/graphql';
import {useCallback, useEffect, useMemo, useRef} from 'react';
import {useForm} from 'react-hook-form';

const getConfigurableComponentConnections = (workflow: Workflow, componentName?: string): ComponentConnection[] =>
    (workflow.tasks ?? [])
        .flatMap((task) => task.connections ?? [])
        .concat((workflow.triggers ?? []).flatMap((trigger) => trigger.connections ?? []))
        .filter((connection) => connection.componentName !== componentName);

interface UseMcpIntegrationWorkflowsFormProps {
    eligibleWorkflowIds?: string[];
    integrationInstanceConfigurationId?: string;
}

export default function useMcpIntegrationWorkflowsForm({
    eligibleWorkflowIds,
    integrationInstanceConfigurationId,
}: UseMcpIntegrationWorkflowsFormProps) {
    const initializedKeyRef = useRef('');

    const form = useForm<IntegrationInstanceConfiguration>({
        defaultValues: {integrationInstanceConfigurationWorkflows: []},
    });

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

    const componentName = integrationInstanceConfiguration?.integration?.componentName;

    const workflows = useMemo(() => {
        if (!integrationVersionWorkflows || !eligibleWorkflowIds) {
            return undefined;
        }

        return integrationVersionWorkflows.filter((workflow) => eligibleWorkflowIds.includes(workflow.id!));
    }, [eligibleWorkflowIds, integrationVersionWorkflows]);

    const getWorkflowConfigurations = useCallback(
        (selectedWorkflowIds: string[]): WorkflowConfigurationInput[] => {
            const formWorkflows = form.getValues('integrationInstanceConfigurationWorkflows') ?? [];

            return selectedWorkflowIds.flatMap((workflowId) => {
                const workflowIndex = workflows?.findIndex((workflow) => workflow.id === workflowId) ?? -1;

                if (workflowIndex < 0) {
                    return [];
                }

                const formWorkflow = formWorkflows[workflowIndex];

                const componentConnections = getConfigurableComponentConnections(
                    workflows![workflowIndex],
                    componentName
                );

                return [
                    {
                        connections: componentConnections.flatMap((componentConnection, connectionIndex) => {
                            const connectionId = formWorkflow?.connections?.[connectionIndex]?.connectionId;

                            if (connectionId == null || Number.isNaN(Number(connectionId))) {
                                return [];
                            }

                            return [
                                {
                                    connectionId: String(connectionId),
                                    workflowConnectionKey: componentConnection.key,
                                    workflowNodeName: componentConnection.workflowNodeName,
                                },
                            ];
                        }),
                        inputs: formWorkflow?.inputs ?? {},
                        workflowId,
                    },
                ];
            });
        },
        [componentName, form, workflows]
    );

    useEffect(() => {
        if (!workflows || !integrationInstanceConfiguration) {
            return;
        }

        const initializationKey = `${integrationInstanceConfiguration.id}-${workflows
            .map((workflow) => workflow.id)
            .join(',')}`;

        if (initializedKeyRef.current === initializationKey) {
            return;
        }

        initializedKeyRef.current = initializationKey;

        const integrationInstanceConfigurationWorkflows: IntegrationInstanceConfigurationWorkflow[] = workflows.map(
            (workflow) => {
                const existingWorkflow =
                    integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows?.find(
                        (integrationInstanceConfigurationWorkflow) =>
                            integrationInstanceConfigurationWorkflow.workflowId === workflow.id
                    );

                return {
                    connections: getConfigurableComponentConnections(workflow, componentName).map(
                        (componentConnection) =>
                            existingWorkflow?.connections?.find(
                                (connection) =>
                                    connection.workflowNodeName === componentConnection.workflowNodeName &&
                                    connection.workflowConnectionKey === componentConnection.key
                            ) ?? {
                                workflowConnectionKey: componentConnection.key,
                                workflowNodeName: componentConnection.workflowNodeName,
                            }
                    ),
                    inputs: existingWorkflow?.inputs ?? {},
                    workflowId: workflow.id!,
                };
            }
        );

        form.reset({integrationInstanceConfigurationWorkflows});
    }, [componentName, form, integrationInstanceConfiguration, workflows]);

    return {componentName, form, getWorkflowConfigurations, workflows};
}
