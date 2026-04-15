import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

// Discriminated union encodes "loading", "error", and "loaded" as mutually exclusive states. Previously the store
// held three parallel maps (workspaceScopeLoading / workspaceScopeError / workspaceScopePermissions) all keyed by the
// same workspaceId, with an implicit invariant that they agreed on which key was in which state. A stray
// setWorkspaceScopeLoadState(id, true, false) could leave workspaceScopePermissions[id] populated from a prior session
// while workspaceScopeLoading[id] was true. Collapsing into one keyed record with a tagged union makes the invariant
// impossible to violate — the only way to set "loaded" is to provide scopes, the only way to set "loading" is to
// drop scopes.
export type WorkspaceScopePermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly scopes: readonly string[]; readonly status: 'loaded'};

export type WorkspacePermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly role: string; readonly status: 'loaded'};

interface PermissionStoreI {
    clearPermissions: () => void;
    clearWorkspaceRole: (workspaceId: number) => void;
    clearWorkspaceScopePermissions: (workspaceId: number) => void;
    setWorkspaceError: (workspaceId: number) => void;
    setWorkspaceLoading: (workspaceId: number) => void;
    setWorkspaceRole: (workspaceId: number, role: string) => void;
    setWorkspaceScopeError: (workspaceId: number) => void;
    setWorkspaceScopeLoading: (workspaceId: number) => void;
    setWorkspaceScopePermissions: (workspaceId: number, scopes: string[]) => void;
    workspaceScopeStates: {[workspaceId: number]: WorkspaceScopePermissionStateType};
    workspaceStates: {[workspaceId: number]: WorkspacePermissionStateType};
}

// Exported so tests can call `permissionStore.setState({...initial...})` in `beforeEach` per the project's
// Zustand testing convention (see CLAUDE.md "Zustand Store Testing"). Hook callers continue to use
// `usePermissionStore` exactly as before.
export const permissionStore = create<PermissionStoreI>()(
    devtools(
        (set) => ({
            clearPermissions: () =>
                set({
                    workspaceScopeStates: {},
                    workspaceStates: {},
                }),
            clearWorkspaceRole: (workspaceId) =>
                set((state) => {
                    const nextStates = {...state.workspaceStates};

                    delete nextStates[workspaceId];

                    return {workspaceStates: nextStates};
                }),
            // Targeted clears for "the server now says this user has no membership / no scopes" — used by the
            // permission loaders when a fetch returns a defined-but-null/empty payload. Without these, a user
            // demoted mid-session keeps their previously cached role/scope set and passes gating checks that
            // should now deny.
            clearWorkspaceScopePermissions: (workspaceId) =>
                set((state) => {
                    const nextStates = {...state.workspaceScopeStates};

                    delete nextStates[workspaceId];

                    return {workspaceScopeStates: nextStates};
                }),
            setWorkspaceError: (workspaceId) =>
                set((state) => ({
                    workspaceStates: {...state.workspaceStates, [workspaceId]: {status: 'error'}},
                })),
            setWorkspaceLoading: (workspaceId) =>
                set((state) => ({
                    workspaceStates: {...state.workspaceStates, [workspaceId]: {status: 'loading'}},
                })),
            setWorkspaceRole: (workspaceId, role) =>
                set((state) => ({
                    workspaceStates: {...state.workspaceStates, [workspaceId]: {role, status: 'loaded'}},
                })),
            setWorkspaceScopeError: (workspaceId) =>
                set((state) => ({
                    workspaceScopeStates: {...state.workspaceScopeStates, [workspaceId]: {status: 'error'}},
                })),
            setWorkspaceScopeLoading: (workspaceId) =>
                set((state) => ({
                    workspaceScopeStates: {...state.workspaceScopeStates, [workspaceId]: {status: 'loading'}},
                })),
            setWorkspaceScopePermissions: (workspaceId, scopes) =>
                set((state) => ({
                    workspaceScopeStates: {...state.workspaceScopeStates, [workspaceId]: {scopes, status: 'loaded'}},
                })),
            workspaceScopeStates: {},
            workspaceStates: {},
        }),
        {name: 'PermissionStore'}
    )
);

export const usePermissionStore = permissionStore;
