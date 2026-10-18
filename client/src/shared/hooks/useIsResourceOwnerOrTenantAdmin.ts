import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

export function useIsResourceOwnerOrTenantAdmin(createdBy?: string | null): boolean {
    const login = useAuthenticationStore((state) => state.account?.login);
    const isTenantAdmin = useIsTenantAdmin();

    return isTenantAdmin || (login != null && createdBy != null && login === createdBy);
}
