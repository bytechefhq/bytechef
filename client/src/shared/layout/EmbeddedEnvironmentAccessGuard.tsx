import EmptyList from '@/components/EmptyList';
import {toEnvironmentName} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {useIsEmbeddedMember} from '@/shared/hooks/useIsEmbeddedMember';
import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {ShieldOffIcon} from 'lucide-react';
import {Outlet, useLocation} from 'react-router-dom';

const UNGUARDED_PATH_PREFIXES = ['/embedded/account'];

const EmbeddedEnvironmentAccessGuard = () => {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const embeddedScopeState = usePermissionStore((state) => state.embeddedScopeStates[currentEnvironmentId]);
    const unionScopeState = usePermissionStore((state) => state.embeddedScopeStates[UNION_ENVIRONMENT_ID]);

    const eeEdition = useEeEdition();
    const {loading: membershipLoading, member} = useIsEmbeddedMember();
    const location = useLocation();
    const tenantAdmin = useIsTenantAdmin();

    const unguardedPath = UNGUARDED_PATH_PREFIXES.some(
        (pathPrefix) => location.pathname === pathPrefix || location.pathname.startsWith(`${pathPrefix}/`)
    );

    if (!eeEdition || tenantAdmin || unguardedPath) {
        return <Outlet />;
    }

    if (membershipLoading) {
        return null;
    }

    if (unionScopeState?.status === 'error') {
        return <Outlet />;
    }

    if (!member) {
        return (
            <div className="flex size-full items-center justify-center p-8">
                <EmptyList
                    icon={<ShieldOffIcon className="size-12 text-content-neutral-secondary" />}
                    message="You are not an embedded member. Ask an embedded admin or a tenant admin to add you."
                    title="No access to Embedded"
                />
            </div>
        );
    }

    if (embeddedScopeState?.status !== 'loaded' && embeddedScopeState?.status !== 'error') {
        return null;
    }

    if (embeddedScopeState.status === 'error' || embeddedScopeState.scopes.length > 0) {
        return <Outlet />;
    }

    return (
        <div className="flex size-full items-center justify-center p-8">
            <EmptyList
                icon={<ShieldOffIcon className="size-12 text-content-neutral-secondary" />}
                message="You have no role in this environment. Switch to an environment you have access to, or ask an embedded admin for access."
                title={`No access to ${toEnvironmentName(currentEnvironmentId)}`}
            />
        </div>
    );
};

export default EmbeddedEnvironmentAccessGuard;
