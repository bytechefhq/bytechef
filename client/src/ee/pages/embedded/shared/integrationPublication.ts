import {Integration} from '@/ee/shared/middleware/embedded/configuration';

export function isIntegrationPublished(integration: Integration): boolean {
    return !!(integration.lastPublishedDate && integration.lastIntegrationVersion);
}

export function isIntegrationConfigurable(integration: Integration): boolean {
    return isIntegrationPublished(integration) || !integration.integrationWorkflowIds?.length;
}

export function isIntegrationConnectionOnly(integration: Integration): boolean {
    return (
        !integration.codeWorkflow &&
        !integration.integrationWorkflowIds?.length &&
        (!integration.lastPublishedDate || !integration.unpublishedChanges)
    );
}

export function canPublishIntegration(integration: Integration): boolean {
    return (
        !!integration.integrationWorkflowIds?.length ||
        (isIntegrationPublished(integration) && !!integration.unpublishedChanges)
    );
}
