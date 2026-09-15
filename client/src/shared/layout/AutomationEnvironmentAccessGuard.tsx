import EmptyList from '@/components/EmptyList';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {toEnvironmentName} from '@/shared/constants';
import useEeEdition from '@/shared/edition/useEeEdition';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {usePermissionStore} from '@/shared/stores/usePermissionStore';
import {ShieldOffIcon} from 'lucide-react';
import {Outlet, useLocation} from 'react-router-dom';

const UNGUARDED_PATH_PREFIXES = ['/automation/account', '/automation/settings'];

const AutomationEnvironmentAccessGuard = () => {
    const account = useAuthenticationStore((state) => state.account);
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const workspaceScopeState = usePermissionStore((state) =>
        currentWorkspaceId == null ? undefined : state.workspaceScopeStates[currentWorkspaceId]?.[currentEnvironmentId]
    );

    const eeEdition = useEeEdition();
    const location = useLocation();

    const tenantAdmin = authenticated && (account?.authorities?.includes('ROLE_ADMIN') ?? false);

    const unguardedPath = UNGUARDED_PATH_PREFIXES.some(
        (pathPrefix) => location.pathname === pathPrefix || location.pathname.startsWith(`${pathPrefix}/`)
    );

    const guarded = eeEdition && !tenantAdmin && !unguardedPath && currentWorkspaceId != null;

    if (!guarded) {
        return <Outlet />;
    }

    if (workspaceScopeState?.status !== 'loaded' && workspaceScopeState?.status !== 'error') {
        return null;
    }

    if (workspaceScopeState.status === 'error' || workspaceScopeState.scopes.length > 0) {
        return <Outlet />;
    }

    return (
        <div className="flex size-full items-center justify-center p-8">
            <EmptyList
                icon={<ShieldOffIcon className="size-12 text-content-neutral-secondary" />}
                message="You have no role in this environment. Switch to an environment you have access to, or ask a workspace admin for access."
                title={`No access to ${toEnvironmentName(currentEnvironmentId)}`}
            />
        </div>
    );
};

export default AutomationEnvironmentAccessGuard;
