import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type WorkspaceScopePermissionStateType =
    | {readonly status: 'loading'}
    | {readonly status: 'error'}
    | {readonly scopes: readonly string[]; readonly status: 'loaded'};

export type WorkspaceScopeStatesType = {
    [workspaceId: number]: {[environmentId: number]: WorkspaceScopePermissionStateType};
};

export type EmbeddedScopeStatesType = {[environmentId: number]: WorkspaceScopePermissionStateType};

interface PermissionStoreI {
    clearPermissions: () => void;
    embeddedScopeStates: EmbeddedScopeStatesType;
    setEmbeddedScopeError: (environmentId: number) => void;
    setEmbeddedScopeLoading: (environmentId: number) => void;
    setEmbeddedScopePermissions: (environmentId: number, scopes: string[]) => void;
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
                    embeddedScopeStates: {},
                    workspaceScopeStates: {},
                }),
            embeddedScopeStates: {},
            setEmbeddedScopeError: (environmentId) =>
                set((state) => ({
                    embeddedScopeStates: {...state.embeddedScopeStates, [environmentId]: {status: 'error'}},
                })),
            setEmbeddedScopeLoading: (environmentId) =>
                set((state) => ({
                    embeddedScopeStates: {...state.embeddedScopeStates, [environmentId]: {status: 'loading'}},
                })),
            setEmbeddedScopePermissions: (environmentId, scopes) =>
                set((state) => ({
                    embeddedScopeStates: {...state.embeddedScopeStates, [environmentId]: {scopes, status: 'loaded'}},
                })),
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
