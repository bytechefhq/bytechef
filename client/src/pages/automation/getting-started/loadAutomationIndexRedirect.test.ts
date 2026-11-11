import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {AUTHORITIES} from '@/shared/constants';
import {WorkspaceKeys} from '@/shared/queries/automation/workspaces.queries';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import loadAutomationIndexRedirect from './loadAutomationIndexRedirect';

const WORKSPACE_ID = 7;
const ENVIRONMENT_ID = 0;
const STAGING_ENVIRONMENT_ID = 1;

const USER_ACCOUNT = {
    activated: true,
    authorities: [AUTHORITIES.USER],
    email: 'user@localhost.com',
    id: 2,
};

const OTHER_WORKSPACE_ID = 9;

const ADMIN_ACCOUNT = {...USER_ACCOUNT, authorities: [AUTHORITIES.ADMIN], email: 'admin@localhost.com', id: 1};

interface QueryResponsesI {
    completed?: boolean | Error;
    scopes?: string[] | Error;
    workspaces?: {id: number}[] | Error;
}

const respond = <T>(value: T | Error): Promise<T> =>
    value instanceof Error ? Promise.reject(value) : Promise.resolve(value);

const mockQueries = (
    queryClient: QueryClient,
    {completed = false, scopes = ['PROJECT_CREATE'], workspaces = [{id: WORKSPACE_ID}]}: QueryResponsesI = {}
) =>
    vi.spyOn(queryClient, 'query').mockImplementation((({queryKey}: {queryKey: readonly unknown[]}) => {
        if (queryKey.flat()[0] === 'workspaces') {
            return respond(workspaces);
        }

        if (queryKey[0] === 'MyWorkspaceScopes') {
            return respond(scopes).then((myWorkspaceScopes) => ({myWorkspaceScopes}));
        }

        return respond(completed).then((automationGettingStartedCompleted) => ({automationGettingStartedCompleted}));
    }) as unknown as QueryClient['query']);

const queryKeysOf = (querySpy: ReturnType<typeof mockQueries>) =>
    querySpy.mock.calls.map(([options]) => (options as {queryKey: readonly unknown[]}).queryKey.flat()[0]);

describe('loadAutomationIndexRedirect', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();

        applicationInfoStore.setState(applicationInfoStore.getInitialState(), true);
        authenticationStore.setState(authenticationStore.getInitialState(), true);
        permissionStore.setState({workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: ENVIRONMENT_ID});
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});

        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('lands on getting started until setup is done', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const querySpy = mockQueries(queryClient);

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(querySpy).toHaveBeenCalledWith(
            expect.objectContaining({queryKey: WorkspaceKeys.userWorkspaces(ADMIN_ACCOUNT.id)})
        );
        expect(querySpy).toHaveBeenCalledWith(
            expect.objectContaining({
                queryKey: ['automationGettingStartedCompleted', {workspaceId: WORKSPACE_ID}],
            })
        );
    });

    it('lands on getting started for an editor whose scopes are fetched from an empty store', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const querySpy = mockQueries(queryClient);

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(querySpy).toHaveBeenCalledWith(
            expect.objectContaining({
                queryKey: ['MyWorkspaceScopes', {environment: 'DEVELOPMENT', workspaceId: String(WORKSPACE_ID)}],
            })
        );
        expect(permissionStore.getState().workspaceScopeStates[WORKSPACE_ID]?.[ENVIRONMENT_ID]).toEqual({
            scopes: ['PROJECT_CREATE'],
            status: 'loaded',
        });
    });

    it('lands on getting started for an editor with loaded scopes', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[ENVIRONMENT_ID]: {scopes: ['PROJECT_CREATE'], status: 'loaded'}}},
        });

        const querySpy = mockQueries(queryClient);

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(queryKeysOf(querySpy)).toEqual(['workspaces', 'automationGettingStartedCompleted']);
    });

    it('selects the first workspace and lands on getting started when the workspace id is cleared', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        const querySpy = mockQueries(queryClient, {workspaces: [{id: OTHER_WORKSPACE_ID}, {id: WORKSPACE_ID}]});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(useWorkspaceStore.getState().currentWorkspaceId).toBe(OTHER_WORKSPACE_ID);
        expect(querySpy).toHaveBeenCalledWith(
            expect.objectContaining({
                queryKey: ['automationGettingStartedCompleted', {workspaceId: OTHER_WORKSPACE_ID}],
            })
        );
    });

    it('replaces a stale workspace id and checks the scopes of the selected workspace', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[ENVIRONMENT_ID]: {scopes: ['API_KEY_VIEW'], status: 'loaded'}}},
        });

        const querySpy = mockQueries(queryClient, {workspaces: [{id: OTHER_WORKSPACE_ID}]});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(useWorkspaceStore.getState().currentWorkspaceId).toBe(OTHER_WORKSPACE_ID);
        expect(querySpy).toHaveBeenCalledWith(
            expect.objectContaining({
                queryKey: ['MyWorkspaceScopes', {environment: 'DEVELOPMENT', workspaceId: String(OTHER_WORKSPACE_ID)}],
            })
        );
    });

    it('keeps the stored workspace when the workspaces request fails', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        mockQueries(queryClient, {workspaces: new Error('Request failed')});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('getting-started');
        expect(useWorkspaceStore.getState().currentWorkspaceId).toBe(WORKSPACE_ID);
    });

    it('lands on projects when the user has no workspace', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        useWorkspaceStore.setState({currentWorkspaceId: undefined});

        const querySpy = mockQueries(queryClient, {workspaces: []});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
        expect(queryKeysOf(querySpy)).toEqual(['workspaces']);
    });

    it('lands on projects without the completed query when fetched scopes lack PROJECT_CREATE', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const querySpy = mockQueries(queryClient, {scopes: ['API_KEY_VIEW']});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
        expect(queryKeysOf(querySpy)).toEqual(['workspaces', 'MyWorkspaceScopes']);
    });

    it('lands on projects when the scopes fetch fails', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const querySpy = mockQueries(queryClient, {scopes: new Error('Request failed')});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
        expect(queryKeysOf(querySpy)).toEqual(['workspaces', 'MyWorkspaceScopes']);
    });

    it('lands on projects once setup is done', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        mockQueries(queryClient, {completed: true});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
    });

    it('lands on projects for a viewer without firing the completed query', async () => {
        authenticationStore.setState({account: USER_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        permissionStore.setState({
            workspaceScopeStates: {[WORKSPACE_ID]: {[ENVIRONMENT_ID]: {scopes: ['API_KEY_VIEW'], status: 'loaded'}}},
        });

        const querySpy = mockQueries(queryClient);

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
        expect(queryKeysOf(querySpy)).toEqual(['workspaces']);
    });

    it('lands on projects when the completed query fails', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        mockQueries(queryClient, {completed: new Error('Request failed')});

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
    });

    it('lands on deployments without any request outside Development', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT_ID});

        const querySpy = mockQueries(queryClient);

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('deployments');
        expect(querySpy).not.toHaveBeenCalled();
    });

    it('lands on projects when the application info request fails', async () => {
        applicationInfoStore.setState({
            application: undefined,
            getApplicationInfo: vi.fn(() => Promise.reject(new Error('Request failed'))),
        });
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const querySpy = vi.spyOn(queryClient, 'query');

        await expect(loadAutomationIndexRedirect(queryClient)).resolves.toBe('projects');
        expect(querySpy).not.toHaveBeenCalled();
    });
});
