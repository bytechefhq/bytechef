import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

// Discriminated union encodes "loading", "error", and "loaded" as mutually exclusive states. Previously the store
// held three parallel maps (projectLoading / projectError / projectPermissions) all keyed by the same projectId, with
// an implicit invariant that they agreed on which key was in which state. A stray setProjectLoadState(id, true, false)
// could leave projectPermissions[id] populated from a prior session while projectLoading[id] was true. Collapsing into
// one keyed record with a tagged union makes the invariant impossible to violate \u2014 the only way to set "loaded"
// is to provide scopes, the only way to set "loading" is to drop scopes.
export type ProjectPermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly scopes: readonly string[]; readonly status: 'loaded'};

export type WorkspacePermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly role: string; readonly status: 'loaded'};

interface PermissionStoreI {
    clearPermissions: () => void;
    clearProjectPermissions: (projectId: number) => void;
    clearWorkspaceRole: (workspaceId: number) => void;
    projectStates: {[projectId: number]: ProjectPermissionStateType};
    setProjectError: (projectId: number) => void;
    setProjectLoading: (projectId: number) => void;
    setProjectPermissions: (projectId: number, scopes: string[]) => void;
    setWorkspaceError: (workspaceId: number) => void;
    setWorkspaceLoading: (workspaceId: number) => void;
    setWorkspaceRole: (workspaceId: number, role: string) => void;
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
                    projectStates: {},
                    workspaceStates: {},
                }),
            // Targeted clears for "the server now says this user has no membership / no scopes" — used by the
            // permission loaders when a fetch returns a defined-but-null/empty payload. Without these, a user
            // demoted mid-session keeps their previously cached role/scope set and passes gating checks that
            // should now deny.
            clearProjectPermissions: (projectId) =>
                set((state) => {
                    const nextStates = {...state.projectStates};

                    delete nextStates[projectId];

                    return {projectStates: nextStates};
                }),
            clearWorkspaceRole: (workspaceId) =>
                set((state) => {
                    const nextStates = {...state.workspaceStates};

                    delete nextStates[workspaceId];

                    return {workspaceStates: nextStates};
                }),
            projectStates: {},
            setProjectError: (projectId) =>
                set((state) => ({
                    projectStates: {...state.projectStates, [projectId]: {status: 'error'}},
                })),
            setProjectLoading: (projectId) =>
                set((state) => ({
                    projectStates: {...state.projectStates, [projectId]: {status: 'loading'}},
                })),
            setProjectPermissions: (projectId, scopes) =>
                set((state) => ({
                    projectStates: {...state.projectStates, [projectId]: {scopes, status: 'loaded'}},
                })),
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
            workspaceStates: {},
        }),
        {name: 'PermissionStore'}
    )
);

export const usePermissionStore = permissionStore;
