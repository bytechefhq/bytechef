import {AUTHORITIES} from '@/shared/constants';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

export function useIsTenantAdmin(): boolean {
    return useAuthenticationStore(
        (state) => state.authenticated && (state.account?.authorities ?? []).includes(AUTHORITIES.ADMIN)
    );
}
