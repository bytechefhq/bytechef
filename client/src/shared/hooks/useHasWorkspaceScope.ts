import useCeEdition from '@/shared/edition/useCeEdition';
import useEditionResolved from '@/shared/edition/useEditionResolved';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

export type WorkspaceScopeType =
    | 'WORKFLOW_VIEW'
    | 'WORKFLOW_CREATE'
    | 'WORKFLOW_EDIT'
    | 'WORKFLOW_DELETE'
    | 'PROJECT_CREATE'
    | 'PROJECT_SETTINGS'
    | 'PROJECT_DELETE'
    | 'PROJECT_PUBLISH'
    | 'PROJECT_PUSH'
    | 'PROJECT_PULL'
    | 'DEPLOYMENT_CREATE'
    | 'DEPLOYMENT_EDIT'
    | 'DEPLOYMENT_DELETE'
    | 'API_PLATFORM_CREATE'
    | 'API_PLATFORM_EDIT'
    | 'API_PLATFORM_DELETE'
    | 'MCP_CREATE'
    | 'MCP_EDIT'
    | 'MCP_DELETE'
    | 'DATA_TABLE_CREATE'
    | 'DATA_TABLE_EDIT'
    | 'DATA_TABLE_DELETE'
    | 'KNOWLEDGE_BASE_CREATE'
    | 'KNOWLEDGE_BASE_EDIT'
    | 'KNOWLEDGE_BASE_DELETE'
    | 'EXECUTION_DETAILS_VIEW'
    | 'CONNECTION_VIEW'
    | 'CONNECTION_EDIT'
    | 'CONNECTION_DELETE'
    | 'API_KEY_CREATE'
    | 'API_KEY_DELETE'
    | 'API_KEY_VIEW'
    | 'WORKSPACE_MANAGE'
    | 'WORKSPACE_MEMBER_MANAGE';

export interface WorkspaceScopeStateI {
    editionUnknown: boolean;
    error: boolean;
    granted: boolean;
    loading: boolean;
}

export const useHasWorkspaceScope = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType,
    environmentId?: number
): boolean => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined
            ? undefined
            : state.workspaceScopeStates[workspaceId]?.[environmentId ?? currentEnvironmentId]
    );
    const ceEdition = useCeEdition();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return true;
    }

    if (workspaceId === undefined) {
        return false;
    }

    return workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope);
};

export const useWorkspaceScopeState = (
    workspaceId: number | undefined,
    scope: WorkspaceScopeType,
    environmentId?: number
): WorkspaceScopeStateI => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const workspaceScopeState = usePermissionStore((state) =>
        workspaceId === undefined
            ? undefined
            : state.workspaceScopeStates[workspaceId]?.[environmentId ?? currentEnvironmentId]
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

    if (workspaceId === undefined) {
        return {editionUnknown: false, error: false, granted: false, loading: false};
    }

    return {
        editionUnknown: false,
        error: workspaceScopeState?.status === 'error',
        granted: workspaceScopeState?.status === 'loaded' && workspaceScopeState.scopes.includes(scope),
        loading: workspaceScopeState?.status === 'loading',
    };
};
