import useCeEdition from '@/shared/edition/useCeEdition';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

export type WorkspaceScopeType = 'API_KEY_CREATE' | 'API_KEY_DELETE' | 'API_KEY_VIEW';

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
