import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

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
