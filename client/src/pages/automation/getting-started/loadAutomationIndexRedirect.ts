import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AUTHORITIES, DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {ENVIRONMENT_ENUMS} from '@/shared/hooks/useLoadWorkspaceScopes';
import {WorkspaceApi} from '@/shared/middleware/automation/configuration';
import {
    AutomationGettingStartedCompletedDocument,
    AutomationGettingStartedCompletedQuery,
    AutomationGettingStartedCompletedQueryVariables,
    MyWorkspaceScopesDocument,
    MyWorkspaceScopesQuery,
    MyWorkspaceScopesQueryVariables,
} from '@/shared/middleware/graphql';
import {fetcher} from '@/shared/middleware/graphqlFetcher';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {QueryClient} from '@tanstack/react-query';

import resolveAutomationIndexRedirect, {
    AUTOMATION_DEFAULT_ROUTE_PATH,
    AUTOMATION_DEPLOYMENTS_ROUTE_PATH,
} from './resolveAutomationIndexRedirect';

const ensureApplicationInfoLoaded = async () => {
    const {application, getApplicationInfo} = applicationInfoStore.getState();

    if (!application) {
        await getApplicationInfo();
    }
};

const ensureAccountLoaded = async () => {
    const {getAccount, sessionHasBeenFetched} = authenticationStore.getState();

    if (!sessionHasBeenFetched) {
        await getAccount();
    }
};

const fetchWorkspaceScopes = async (
    queryClient: QueryClient,
    workspaceId: number,
    environmentId: number
): Promise<readonly string[]> => {
    const variables = {environment: ENVIRONMENT_ENUMS[environmentId], workspaceId: String(workspaceId)};

    const data = await queryClient.query({
        queryFn: fetcher<MyWorkspaceScopesQuery, MyWorkspaceScopesQueryVariables>(MyWorkspaceScopesDocument, variables),
        queryKey: ['MyWorkspaceScopes', variables],
    });

    permissionStore.getState().setWorkspaceScopePermissions(workspaceId, environmentId, data.myWorkspaceScopes ?? []);

    return data.myWorkspaceScopes ?? [];
};

const resolveWorkspaceId = async (queryClient: QueryClient): Promise<number | undefined> => {
    const {account} = authenticationStore.getState();
    const {currentWorkspaceId, setCurrentWorkspaceId} = useWorkspaceStore.getState();

    if (account?.id === undefined) {
        return currentWorkspaceId;
    }

    const userId = account.id;

    try {
        const workspaces = await queryClient.query({
            queryFn: () => new WorkspaceApi().getUserWorkspaces({id: userId}),
            queryKey: WorkspaceKeys.userWorkspaces(userId),
        });

        if (workspaces.some((workspace) => workspace.id === currentWorkspaceId)) {
            return currentWorkspaceId;
        }

        const firstWorkspaceId = workspaces[0]?.id;

        if (firstWorkspaceId !== undefined) {
            setCurrentWorkspaceId(firstWorkspaceId);
        }

        return firstWorkspaceId;
    } catch {
        return currentWorkspaceId;
    }
};

const resolveCanEdit = async (queryClient: QueryClient, currentWorkspaceId: number): Promise<boolean> => {
    const {application} = applicationInfoStore.getState();
    const {account, authenticated} = authenticationStore.getState();

    if (!authenticated) {
        return false;
    }

    if (application?.edition === EditionType.CE || account?.authorities?.includes(AUTHORITIES.ADMIN)) {
        return true;
    }

    const {currentEnvironmentId} = environmentStore.getState();

    const workspaceScopeState =
        permissionStore.getState().workspaceScopeStates[currentWorkspaceId]?.[currentEnvironmentId];

    if (workspaceScopeState?.status === 'loaded') {
        return workspaceScopeState.scopes.includes('PROJECT_CREATE');
    }

    if (ENVIRONMENT_ENUMS[currentEnvironmentId] === undefined) {
        return false;
    }

    try {
        const scopes = await fetchWorkspaceScopes(queryClient, currentWorkspaceId, currentEnvironmentId);

        return scopes.includes('PROJECT_CREATE');
    } catch {
        return false;
    }
};

export default async function loadAutomationIndexRedirect(queryClient: QueryClient): Promise<string> {
    if (environmentStore.getState().currentEnvironmentId !== DEVELOPMENT_ENVIRONMENT) {
        return AUTOMATION_DEPLOYMENTS_ROUTE_PATH;
    }

    try {
        await Promise.all([ensureApplicationInfoLoaded(), ensureAccountLoaded()]);
    } catch {
        return AUTOMATION_DEFAULT_ROUTE_PATH;
    }

    const currentWorkspaceId = await resolveWorkspaceId(queryClient);

    if (currentWorkspaceId === undefined) {
        return AUTOMATION_DEFAULT_ROUTE_PATH;
    }

    const canEdit = await resolveCanEdit(queryClient, currentWorkspaceId);

    if (!canEdit) {
        return resolveAutomationIndexRedirect({canEdit: false});
    }

    try {
        const data = await queryClient.query({
            queryFn: fetcher<AutomationGettingStartedCompletedQuery, AutomationGettingStartedCompletedQueryVariables>(
                AutomationGettingStartedCompletedDocument,
                {workspaceId: currentWorkspaceId}
            ),
            queryKey: ['automationGettingStartedCompleted', {workspaceId: currentWorkspaceId}],
        });

        return resolveAutomationIndexRedirect({canEdit, completed: data.automationGettingStartedCompleted});
    } catch {
        return AUTOMATION_DEFAULT_ROUTE_PATH;
    }
}
