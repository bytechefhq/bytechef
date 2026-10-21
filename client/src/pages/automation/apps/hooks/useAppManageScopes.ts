import {APP_MANAGE_SCOPE} from '@/pages/automation/apps/constants';
import useAppWorkspaceScopes, {AppWorkspaceScopeTargetI} from '@/pages/automation/apps/hooks/useAppWorkspaceScopes';

export default function useAppManageScopes(targets: AppWorkspaceScopeTargetI[]) {
    return useAppWorkspaceScopes(targets, APP_MANAGE_SCOPE);
}
