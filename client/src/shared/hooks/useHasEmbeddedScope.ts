import useCeEdition from '@/shared/edition/useCeEdition';
import useEditionResolved from '@/shared/edition/useEditionResolved';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

export const UNION_ENVIRONMENT_ID = -1;

export type EmbeddedScopeType =
    | 'API_KEY_CREATE'
    | 'API_KEY_DELETE'
    | 'API_KEY_VIEW'
    | 'APP_EVENT_CREATE'
    | 'APP_EVENT_DELETE'
    | 'APP_EVENT_EDIT'
    | 'APP_EVENT_VIEW'
    | 'CONNECTED_USER_DELETE'
    | 'CONNECTED_USER_MANAGE'
    | 'CONNECTED_USER_VIEW'
    | 'CONNECTION_CREATE'
    | 'CONNECTION_DELETE'
    | 'CONNECTION_EDIT'
    | 'CONNECTION_VIEW'
    | 'EMBEDDED_MEMBER_MANAGE'
    | 'EMBEDDED_SETTINGS_MANAGE'
    | 'EMBEDDED_VIEW'
    | 'EXECUTION_DELETE'
    | 'EXECUTION_DETAILS_VIEW'
    | 'EXECUTION_VIEW'
    | 'INTEGRATION_CREATE'
    | 'INTEGRATION_DELETE'
    | 'INTEGRATION_INSTANCE_CONFIGURATION_CREATE'
    | 'INTEGRATION_INSTANCE_CONFIGURATION_DELETE'
    | 'INTEGRATION_INSTANCE_CONFIGURATION_EDIT'
    | 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW'
    | 'INTEGRATION_PUBLISH'
    | 'INTEGRATION_SETTINGS'
    | 'MCP_CREATE'
    | 'MCP_DELETE'
    | 'MCP_EDIT'
    | 'MCP_VIEW'
    | 'WORKFLOW_CREATE'
    | 'WORKFLOW_DELETE'
    | 'WORKFLOW_EDIT'
    | 'WORKFLOW_VIEW';

export interface EmbeddedScopeStateI {
    editionUnknown: boolean;
    error: boolean;
    granted: boolean;
    loading: boolean;
}

export const useHasEmbeddedScope = (scope: EmbeddedScopeType, environmentId?: number): boolean => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const embeddedScopeState = usePermissionStore(
        (state) => state.embeddedScopeStates[environmentId ?? currentEnvironmentId]
    );
    const ceEdition = useCeEdition();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return true;
    }

    return embeddedScopeState?.status === 'loaded' && embeddedScopeState.scopes.includes(scope);
};

export const useEmbeddedScopeState = (scope: EmbeddedScopeType, environmentId?: number): EmbeddedScopeStateI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const embeddedScopeState = usePermissionStore(
        (state) => state.embeddedScopeStates[environmentId ?? currentEnvironmentId]
    );
    const ceEdition = useCeEdition();
    const editionResolved = useEditionResolved();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return {editionUnknown: false, error: false, granted: true, loading: false};
    }

    if (!editionResolved) {
        return {editionUnknown: true, error: false, granted: false, loading: false};
    }

    return {
        editionUnknown: false,
        error: embeddedScopeState?.status === 'error',
        granted: embeddedScopeState?.status === 'loaded' && embeddedScopeState.scopes.includes(scope),
        loading: embeddedScopeState?.status === 'loading',
    };
};
