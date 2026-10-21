import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {useWorkspaceUsersQuery} from '@/shared/middleware/graphql';
import {useCallback, useMemo} from 'react';

/**
 * Resolves an app's ownerUserId to a display name from the workspace members. One query per page; a failed query
 * or a user who is no longer a member falls back to "User <id>".
 */
export default function useAppOwnerNames() {
    const currentWorkspaceId = useWorkspaceStore((state) => state.currentWorkspaceId);

    const {data} = useWorkspaceUsersQuery({workspaceId: String(currentWorkspaceId)});

    const ownerNamesByUserId = useMemo(() => {
        const ownerNames = new Map<string, string>();

        for (const workspaceUser of data?.workspaceUsers ?? []) {
            const user = workspaceUser.user;

            if (!user) {
                continue;
            }

            const fullName = [user.firstName, user.lastName].filter(Boolean).join(' ');

            ownerNames.set(workspaceUser.userId, fullName || user.email);
        }

        return ownerNames;
    }, [data?.workspaceUsers]);

    return useCallback(
        (ownerUserId: string) => ownerNamesByUserId.get(ownerUserId) || `User ${ownerUserId}`,
        [ownerNamesByUserId]
    );
}
