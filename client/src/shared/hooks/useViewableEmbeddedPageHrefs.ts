import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';

export const useViewableEmbeddedPageHrefs = (): string[] => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const appEventViewGranted = useHasEmbeddedScope('APP_EVENT_VIEW');
    const connectedUserViewGranted = useHasEmbeddedScope('CONNECTED_USER_VIEW');
    const connectionViewGranted = useHasEmbeddedScope('CONNECTION_VIEW');
    const executionViewGranted = useHasEmbeddedScope('EXECUTION_VIEW');
    const instanceConfigurationViewGranted = useHasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_VIEW');
    const mcpViewGranted = useHasEmbeddedScope('MCP_VIEW');
    const workflowViewGranted = useHasEmbeddedScope('WORKFLOW_VIEW', DEVELOPMENT_ENVIRONMENT);
    const tenantAdmin = useIsTenantAdmin();

    const pages: [string, boolean][] = [
        ['/embedded/integrations', workflowViewGranted && currentEnvironmentId === DEVELOPMENT_ENVIRONMENT],
        ['/embedded/configurations', instanceConfigurationViewGranted],
        ['/embedded/app-events', appEventViewGranted],
        ['/embedded/automation-workflows', workflowViewGranted],
        ['/embedded/automation-configurations', tenantAdmin],
        ['/embedded/mcp-servers', mcpViewGranted],
        ['/embedded/connected-users', connectedUserViewGranted],
        ['/embedded/executions', executionViewGranted],
        ['/embedded/executions/tool-invocations', tenantAdmin],
        ['/embedded/connections', connectionViewGranted],
    ];

    return pages.filter(([, granted]) => granted).map(([href]) => href);
};
