import {Integration} from '@/ee/shared/middleware/embedded/configuration';

export function isIntegrationPublished(integration: Integration): boolean {
    return !!(integration.lastPublishedDate && integration.lastIntegrationVersion);
}

export function isIntegrationConfigurable(integration: Integration): boolean {
    return isIntegrationPublished(integration) || !integration.integrationWorkflowIds?.length;
}
