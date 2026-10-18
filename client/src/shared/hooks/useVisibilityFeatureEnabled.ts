import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {EditionType, useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';

export type VisibilityFeatureType =
    | {enabled: true; isAdmin: boolean; workspaceId: number}
    | {enabled: false; isAdmin: boolean; workspaceId: number | undefined};

export const useIsVisibilityEditionEnabled = (): boolean =>
    useApplicationInfoStore((state) => state.application?.edition === EditionType.EE);

export const useVisibilityFeatureEnabled = (): VisibilityFeatureType => {
    const workspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);
    const isEE = useIsVisibilityEditionEnabled();
    const isAdmin = useAuthenticationStore((state) => state.account?.authorities?.includes('ROLE_ADMIN') ?? false);

    if (isEE && workspaceId !== undefined) {
        return {enabled: true, isAdmin, workspaceId};
    }

    return {enabled: false, isAdmin, workspaceId};
};
