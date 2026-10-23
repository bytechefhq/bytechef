import useCeEdition from '@/shared/edition/useCeEdition';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';

export interface EmbeddedMembershipI {
    loading: boolean;
    member: boolean;
}

export const useIsEmbeddedMember = (): EmbeddedMembershipI => {
    const unionScopeState = usePermissionStore((state) => state.embeddedScopeStates[UNION_ENVIRONMENT_ID]);
    const ceEdition = useCeEdition();
    const tenantAdmin = useIsTenantAdmin();

    if (ceEdition || tenantAdmin) {
        return {loading: false, member: true};
    }

    return {
        loading: unionScopeState === undefined || unionScopeState.status === 'loading',
        member: unionScopeState?.status === 'loaded' && unionScopeState.scopes.length > 0,
    };
};
