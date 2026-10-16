interface IntegrationInstanceConfigurationOptionI {
    id?: number;
    integration?: {componentName?: string; name?: string};
    integrationId?: number;
}

interface IntegrationOptionI {
    componentName?: string;
    id?: number;
    name?: string;
}

interface WorkflowExecutionsFiltersParamsI {
    automations: number;
    connectedUserId?: number;
    connectedUserProjects?: Array<{connectedUser: {externalId: string; id: number | string}}>;
    connectedUsers?: Array<{externalId: string; id?: number}>;
    endDate?: Date;
    integrationId?: number;
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationOptionI;
    integrationInstanceConfigurationId?: number;
    integrationInstanceConfigurations?: IntegrationInstanceConfigurationOptionI[];
    integrations?: IntegrationOptionI[];
    startDate?: Date;
    status?: string;
    workflowId?: string;
    workflows?: Array<{id?: string; label?: string}>;
}

export const getIntegrationInstanceConfigurationIntegrationName = (
    integrationInstanceConfiguration?: IntegrationInstanceConfigurationOptionI,
    integrations?: IntegrationOptionI[]
) =>
    integrations?.find((integration) => integration.id === integrationInstanceConfiguration?.integrationId)?.name ||
    integrationInstanceConfiguration?.integration?.name;

export const getWorkflowExecutionsFilters = ({
    automations,
    connectedUserId,
    connectedUserProjects,
    connectedUsers,
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
                integrations?.find((integration) => integration.id === integrationId)?.name || String(integrationId);
        }

        if (integrationInstanceConfigurationId) {
            const selectedIntegrationInstanceConfiguration =
                integrationInstanceConfigurations?.find(
                    (configuration) => configuration.id === integrationInstanceConfigurationId
                ) || integrationInstanceConfiguration;

            instanceConfigurationLabel =
                getIntegrationInstanceConfigurationIntegrationName(
                    selectedIntegrationInstanceConfiguration,
                    integrations
                ) || String(integrationInstanceConfigurationId);
        }

        if (connectedUserId) {
            const connectedUser = connectedUsers?.find((connectedUser) => connectedUser.id === connectedUserId);

            connectedUserLabel = connectedUser ? `User ${connectedUser.externalId}` : String(connectedUserId);
        }
    }

    if (workflowId) {
        workflowLabel = workflows?.find((workflow) => workflow.id === workflowId)?.label || workflowId;
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
