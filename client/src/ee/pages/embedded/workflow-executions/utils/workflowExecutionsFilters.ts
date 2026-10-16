interface IntegrationInstanceConfigurationOptionI {
    id?: number;
    integration?: {componentName?: string};
}

interface WorkflowExecutionsFiltersParamsI {
    automations: number;
    connectedUserId?: number;
    connectedUserProjects?: Array<{connectedUser: {externalId: string; id: number | string}}>;
    endDate?: Date;
    integrationId?: number;
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationOptionI;
    integrationInstanceConfigurationId?: number;
    integrationInstanceConfigurations?: IntegrationInstanceConfigurationOptionI[];
    integrations?: Array<{componentName?: string; id?: number}>;
    startDate?: Date;
    status?: string;
    workflowId?: string;
    workflows?: Array<{id?: string; label?: string}>;
}

export const getWorkflowExecutionsFilters = ({
    automations,
    connectedUserId,
    connectedUserProjects,
    endDate,
    integrationId,
    integrationInstanceConfiguration,
    integrationInstanceConfigurationId,
    integrationInstanceConfigurations,
    integrations,
    startDate,
    status,
    workflowId,
    workflows,
}: WorkflowExecutionsFiltersParamsI): Array<{label: string; value?: string}> => {
    let connectedUserLabel: string | undefined;
    let instanceConfigurationLabel: string | undefined;
    let integrationLabel: string | undefined;
    let workflowLabel: string | undefined;

    if (automations) {
        if (connectedUserId) {
            const connectedUserProject = connectedUserProjects?.find(
                (connectedUserProject) => +connectedUserProject.connectedUser.id === connectedUserId
            );

            connectedUserLabel = connectedUserProject
                ? `User ${connectedUserProject.connectedUser.externalId}`
                : String(connectedUserId);
        }
    } else {
        if (integrationId) {
            integrationLabel =
                integrations?.find((integration) => integration.id === integrationId)?.componentName ||
                String(integrationId);
        }

        if (integrationInstanceConfigurationId) {
            const selectedIntegrationInstanceConfiguration =
                integrationInstanceConfigurations?.find(
                    (configuration) => configuration.id === integrationInstanceConfigurationId
                ) || integrationInstanceConfiguration;

            instanceConfigurationLabel =
                selectedIntegrationInstanceConfiguration?.integration?.componentName ||
                String(integrationInstanceConfigurationId);
        }

        if (workflowId) {
            workflowLabel = workflows?.find((workflow) => workflow.id === workflowId)?.label || workflowId;
        }
    }

    return [
        {label: 'Type', value: automations ? 'Automations' : 'Integrations'},
        {label: 'Status', value: status},
        {label: 'Start date', value: startDate?.toLocaleDateString()},
        {label: 'End date', value: endDate?.toLocaleDateString()},
        {label: 'Integration', value: integrationLabel},
        {label: 'Instance Configuration', value: instanceConfigurationLabel},
        {label: 'Connected User', value: connectedUserLabel},
        {label: 'Workflow', value: workflowLabel},
    ];
};
