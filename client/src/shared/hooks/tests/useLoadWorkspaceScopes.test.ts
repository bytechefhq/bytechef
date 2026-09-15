import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT, STAGING_ENVIRONMENT} from '@/shared/constants';
import {useLoadWorkspaceScopes} from '@/shared/hooks/useLoadWorkspaceScopes';
import {useMyWorkspaceScopesQuery} from '@/shared/middleware/graphql';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkspaceScopePermissionStateType, permissionStore} from '@/shared/stores/usePermissionStore';
import {act, renderHook} from '@testing-library/react';
import {toast} from 'sonner';
import {beforeEach, describe, expect, it, vi} from 'vitest';

// Hoisted handles so each test can configure the mocked query result before the component renders. Without hoisting
// the mock module would close over the initial values and ignore later mutations.
const hoisted = vi.hoisted(() => ({
    queryResult: {
        data: undefined as undefined | {myWorkspaceScopes: string[] | null},
        isError: false,
        isLoading: false,
    },
}));

vi.mock('@/shared/middleware/graphql', () => ({
    EnvironmentEnum: {Development: 'DEVELOPMENT', Production: 'PRODUCTION', Staging: 'STAGING'},
    useMyWorkspaceScopesQuery: vi.fn(() => hoisted.queryResult),
}));

vi.mock('sonner', () => ({
    toast: {
        error: vi.fn(),
    },
}));

const WORKSPACE_ID = 42;

function setQueryResult(result: typeof hoisted.queryResult): void {
    hoisted.queryResult = result;
}

function setEdition(edition: EditionType | null): void {
    applicationInfoStore.setState({application: edition === null ? null : {edition}});
}

function storedState(environmentId: number = DEVELOPMENT_ENVIRONMENT): WorkspaceScopePermissionStateType | undefined {
    return permissionStore.getState().workspaceScopeStates[WORKSPACE_ID]?.[environmentId];
}

// TanStack Query types `enabled` as boolean-or-resolver, so this is deliberately `unknown`: the assertions below
// compare it to a literal, which is exactly what these hooks pass.
function queryEnabled(): unknown {
    const [, options] = vi.mocked(useMyWorkspaceScopesQuery).mock.calls[0];

    return options?.enabled;
}

describe('useLoadWorkspaceScopes', () => {
    beforeEach(() => {
        permissionStore.setState({
            workspaceScopeStates: {},
        });
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        hoisted.queryResult = {data: undefined, isError: false, isLoading: false};
        vi.clearAllMocks();

        // `myWorkspaceScopes` is EE-only, so every assertion about fetching and storing scopes presupposes an
        // established Enterprise edition. The CE and unresolved-edition cases are asserted explicitly below.
        setEdition(EditionType.EE);
    });

    it('marks the workspace as loading while the query is in flight', () => {
        // The discriminated union forces "loading" to drop any previously cached scopes. Without this, a user
        // demoted mid-session would keep stale ALLOW decisions while the re-fetch is in flight.
        setQueryResult({data: undefined, isError: false, isLoading: true});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({status: 'loading'});
    });

    it('stores fetched scopes when the query succeeds', () => {
        setQueryResult({
            data: {myWorkspaceScopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']},
            isError: false,
            isLoading: false,
        });

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({
            scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'],
            status: 'loaded',
        });
    });

    it('stores empty scopes when the server reports no membership', () => {
        // A defined-but-empty scope list means the server authoritatively says the user has no access. Storing this
        // as 'loaded' (with empty scopes) lets useHasWorkspaceScope distinguish "still loading" from "loaded but
        // denied".
        setQueryResult({data: {myWorkspaceScopes: []}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({
            scopes: [],
            status: 'loaded',
        });
    });

    it('treats a null scopes payload as loaded with empty scopes', () => {
        // The GraphQL schema defines myWorkspaceScopes as `[String!]!` (non-nullable list of non-nullable strings),
        // but the type generator emits `string[] | null`. A null payload should be treated as "no membership".
        setQueryResult({data: {myWorkspaceScopes: null}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({
            scopes: [],
            status: 'loaded',
        });
    });

    it('marks the workspace as errored, fires a toast, and clears stale scopes when no scopes ever loaded', () => {
        // With no answer from the server at all there is nothing to trust. The discriminated union forces 'error' to
        // drop scopes, and the toast informs the user why gating tightened.
        permissionStore.setState({
            workspaceScopeStates: {
                [WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
            },
        });

        setQueryResult({data: undefined, isError: true, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({status: 'error'});
        expect(toast.error).toHaveBeenCalledOnce();
    });

    it('keeps the loaded scopes when a background refetch fails', () => {
        // React Query keeps the last good data next to the error of a failed refetch. Dropping it would make an editor
        // read-only over a network blip, while the server still enforces every write.
        setQueryResult({data: {myWorkspaceScopes: ['WORKFLOW_EDIT']}, isError: true, isLoading: false});

        const {result} = renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState()).toEqual({scopes: ['WORKFLOW_EDIT'], status: 'loaded'});
        expect(result.current).toEqual({error: false, loading: false});
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('does not touch the store when workspaceId is undefined', () => {
        // Caller may not yet know the workspaceId (route params still loading). The hook must no-op rather than
        // pollute the store with state for an unknown id.
        renderHook(() => useLoadWorkspaceScopes(undefined));

        expect(permissionStore.getState().workspaceScopeStates).toEqual({});
    });

    it('returns the underlying loading and error flags for the caller', () => {
        // The hook exposes loading/error so callers that do their own UI gating do not have to subscribe to the
        // store. This is the convenience contract on top of the store-side state machine.
        setQueryResult({data: undefined, isError: false, isLoading: true});

        const {result} = renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(result.current).toEqual({error: false, loading: true});
    });

    it('asks for the scopes of the environment currently selected', () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        const [variables] = vi.mocked(useMyWorkspaceScopesQuery).mock.calls[0];

        expect(variables).toEqual({environment: 'PRODUCTION', workspaceId: String(WORKSPACE_ID)});
    });

    it('stores scopes under the current environment and Development, and no other', () => {
        // Development is loaded whichever environment is selected, because workflow definition writes are checked
        // there.
        environmentStore.setState({currentEnvironmentId: STAGING_ENVIRONMENT});
        setQueryResult({data: {myWorkspaceScopes: ['WORKFLOW_VIEW']}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(storedState(STAGING_ENVIRONMENT)).toEqual({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
        expect(storedState(DEVELOPMENT_ENVIRONMENT)).toEqual({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
        expect(storedState(PRODUCTION_ENVIRONMENT)).toBeUndefined();
    });

    it('also asks for the Development scopes when another environment is selected', () => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        const requestedEnvironments = vi
            .mocked(useMyWorkspaceScopesQuery)
            .mock.calls.map(([variables, options]) => [variables.environment, options?.enabled]);

        expect(requestedEnvironments).toContainEqual(['DEVELOPMENT', true]);
    });

    it('asks again and stores the new answer when the environment switches', () => {
        setQueryResult({data: {myWorkspaceScopes: ['DEPLOYMENT_CREATE']}, isError: false, isLoading: false});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        setQueryResult({data: {myWorkspaceScopes: ['WORKFLOW_VIEW']}, isError: false, isLoading: false});

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        });

        const requestedEnvironments = vi
            .mocked(useMyWorkspaceScopesQuery)
            .mock.calls.map(([variables]) => variables.environment);

        expect(requestedEnvironments).toContain('PRODUCTION');
        expect(storedState(PRODUCTION_ENVIRONMENT)).toEqual({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});
    });

    it('enables the query once the edition is known to be Enterprise', () => {
        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(queryEnabled()).toBe(true);
    });

    it('never fires the query on Community and leaves the store untouched', () => {
        // The EE-only field does not exist in the CE schema, so the request would come back with an `errors` array
        // that graphqlFetcher throws on — producing this hook's toast plus a second one from the global fetch
        // interceptor on every project open. Nothing is stored either: useHasWorkspaceScope short-circuits to
        // granted on CE, so there is no state to keep.
        setEdition(EditionType.CE);

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(queryEnabled()).toBe(false);
        expect(permissionStore.getState().workspaceScopeStates).toEqual({});
        expect(toast.error).not.toHaveBeenCalled();
    });

    it('waits rather than guessing while the edition is still unresolved', () => {
        // `application` stays null for the whole session when `/actuator/info` answers anything but 200:
        // getApplicationInfo assigns it only on 200, leaves `loading` true otherwise, and early-returns while
        // loading, so it never retries. Gating on "not CE" would read that state as Enterprise and fire the
        // EE-only query on a Community install, which is the whole bug. Requiring positively-established EE
        // makes the query wait instead.
        setEdition(null);

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(queryEnabled()).toBe(false);
        expect(permissionStore.getState().workspaceScopeStates).toEqual({});
    });

    it('does not write a loading entry on Community even while the disabled query reports pending', () => {
        // Guard against a regression where the effect writes state from the query flags without re-checking the
        // edition: a CE store entry would make useHasWorkspaceScope observe 'loading' and hide gated controls.
        setEdition(EditionType.CE);
        setQueryResult({data: undefined, isError: false, isLoading: true});

        renderHook(() => useLoadWorkspaceScopes(WORKSPACE_ID));

        expect(permissionStore.getState().workspaceScopeStates).toEqual({});
    });
});
