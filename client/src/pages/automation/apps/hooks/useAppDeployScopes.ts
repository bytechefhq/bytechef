import {APP_DEPLOY_SCOPE} from '@/pages/automation/apps/constants';
import useAppWorkspaceScopes, {AppWorkspaceScopeTargetI} from '@/pages/automation/apps/hooks/useAppWorkspaceScopes';

export default function useAppDeployScopes(targets: AppWorkspaceScopeTargetI[]) {
    return useAppWorkspaceScopes(targets, APP_DEPLOY_SCOPE);
}
