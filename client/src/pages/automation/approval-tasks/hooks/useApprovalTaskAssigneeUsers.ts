import {useIsTenantAdmin} from '@/shared/hooks/useIsTenantAdmin';
import {useUsersQuery} from '@/shared/middleware/graphql';
import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useMemo} from 'react';

import type {UserWithDisplayInfoI} from '../utils/approval-task-utils';

export function useApprovalTaskAssigneeUsers(): Array<UserWithDisplayInfoI | null> | undefined {
    const account = useAuthenticationStore((state) => state.account);

    const isTenantAdmin = useIsTenantAdmin();

    const {data: usersData} = useUsersQuery(undefined, {enabled: isTenantAdmin});

    return useMemo(() => {
        if (isTenantAdmin) {
            return usersData?.users?.content;
        }

        return account ? [account] : undefined;
    }, [account, isTenantAdmin, usersData]);
}
