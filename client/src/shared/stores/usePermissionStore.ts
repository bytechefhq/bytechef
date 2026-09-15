import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

// Discriminated union encodes "loading", "error", and "loaded" as mutually exclusive states. The alternative is three
// parallel maps (workspaceScopeLoading / workspaceScopeError / workspaceScopePermissions) all keyed by the same
// workspaceId, with an implicit invariant that they agree on which key is in which state — a stray
// setWorkspaceScopeLoadState(id, true, false) could then leave workspaceScopePermissions[id] populated from a prior
// session while workspaceScopeLoading[id] was true. One keyed record with a tagged union makes that impossible: the
// only way to set "loaded" is to provide scopes, the only way to set "loading" is to drop them.
export type WorkspaceScopePermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly scopes: readonly string[]; readonly status: 'loaded'};

export type WorkspaceScopeStatesType = {
    [workspaceId: number]: {[environmentId: number]: WorkspaceScopePermissionStateType};
};

interface PermissionStoreI {
    clearPermissions: () => void;
    setWorkspaceScopeError: (workspaceId: number, environmentId: number) => void;
    setWorkspaceScopeLoading: (workspaceId: number, environmentId: number) => void;
    setWorkspaceScopePermissions: (workspaceId: number, environmentId: number, scopes: string[]) => void;
    workspaceScopeStates: WorkspaceScopeStatesType;
}

const withWorkspaceScopeState = (
    workspaceScopeStates: WorkspaceScopeStatesType,
    workspaceId: number,
    environmentId: number,
    workspaceScopeState: WorkspaceScopePermissionStateType
): WorkspaceScopeStatesType => ({
    ...workspaceScopeStates,
    [workspaceId]: {...workspaceScopeStates[workspaceId], [environmentId]: workspaceScopeState},
});

// Exported so tests can call `permissionStore.setState({...initial...})` in `beforeEach` per the project's
// Zustand testing convention (see CLAUDE.md "Zustand Store Testing"). Hook callers continue to use
// `usePermissionStore` exactly as before.
export const permissionStore = create<PermissionStoreI>()(
    devtools(
        (set) => ({
            clearPermissions: () =>
                set({
                    workspaceScopeStates: {},
                }),
            setWorkspaceScopeError: (workspaceId, environmentId) =>
                set((state) => ({
                    workspaceScopeStates: withWorkspaceScopeState(
                        state.workspaceScopeStates,
                        workspaceId,
                        environmentId,
                        {status: 'error'}
                    ),
                })),
            setWorkspaceScopeLoading: (workspaceId, environmentId) =>
                set((state) => ({
                    workspaceScopeStates: withWorkspaceScopeState(
                        state.workspaceScopeStates,
                        workspaceId,
                        environmentId,
                        {status: 'loading'}
                    ),
                })),
            setWorkspaceScopePermissions: (workspaceId, environmentId, scopes) =>
                set((state) => ({
                    workspaceScopeStates: withWorkspaceScopeState(
                        state.workspaceScopeStates,
                        workspaceId,
                        environmentId,
                        {scopes, status: 'loaded'}
                    ),
                })),
            workspaceScopeStates: {},
        }),
        {name: 'PermissionStore'}
    )
);

export const usePermissionStore = permissionStore;
