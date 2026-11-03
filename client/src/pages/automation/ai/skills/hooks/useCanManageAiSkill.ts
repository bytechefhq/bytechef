import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

export default function useCanManageAiSkill(createdBy?: string | null): boolean {
    const login = useAuthenticationStore((state) => state.account?.login);

    const isTenantAdmin = useIsTenantAdmin();

    return isTenantAdmin || (!!login && login === createdBy);
}
