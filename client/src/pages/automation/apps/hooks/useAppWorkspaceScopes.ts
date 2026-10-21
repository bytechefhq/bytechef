import {APP_ENVIRONMENT_ENUMS} from '@/pages/automation/apps/constants';
import {
    MyWorkspaceScopesDocument,
    MyWorkspaceScopesQuery,
    MyWorkspaceScopesQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {useQueries} from '@tanstack/react-query';
import {useCallback, useMemo} from 'react';

export interface AppWorkspaceScopeTargetI {
    environmentId: string;
    workspaceId: string;
}

const toTargetKey = (workspaceId: string, environmentId: string) => `${workspaceId}:${environmentId}`;

export default function useAppWorkspaceScopes(targets: AppWorkspaceScopeTargetI[], scope: string) {
    const uniqueTargets = useMemo(() => {
        const targetsByKey = new Map<string, AppWorkspaceScopeTargetI>();

        for (const target of targets) {
            targetsByKey.set(toTargetKey(target.workspaceId, target.environmentId), target);
        }

        return [...targetsByKey.values()];
    }, [targets]);

    const scopeQueries = useQueries({
        queries: uniqueTargets.map((target) => {
            const variables: MyWorkspaceScopesQueryVariables = {
                environment: APP_ENVIRONMENT_ENUMS[Number(target.environmentId)],
                workspaceId: target.workspaceId,
            };

            return {
                queryFn: fetcher<MyWorkspaceScopesQuery, MyWorkspaceScopesQueryVariables>(
                    MyWorkspaceScopesDocument,
                    variables
                ),
                queryKey: ['MyWorkspaceScopes', variables],
            };
        }),
    });

    const permittedTargetKeys = useMemo(() => {
        const targetKeys = new Set<string>();

        uniqueTargets.forEach((target, index) => {
            if ((scopeQueries[index]?.data?.myWorkspaceScopes ?? []).includes(scope)) {
                targetKeys.add(toTargetKey(target.workspaceId, target.environmentId));
            }
        });

        return targetKeys;
    }, [scope, scopeQueries, uniqueTargets]);

    return useCallback(
        (workspaceId: string, environmentId: string) =>
            permittedTargetKeys.has(toTargetKey(workspaceId, environmentId)),
        [permittedTargetKeys]
    );
}
