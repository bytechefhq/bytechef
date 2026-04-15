import {permissionStore} from '@/shared/stores/usePermissionStore';
import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('permissionStore', () => {
    beforeEach(() => {
        permissionStore.setState({
            workspaceScopeStates: {},
            workspaceStates: {},
        });
    });

    describe('setWorkspaceScopePermissions', () => {
        it('stores scopes for a workspace id with status loaded', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({
                scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'],
                status: 'loaded',
            });
        });

        it('overwrites scopes when called again for the same workspace', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopePermissions(42, ['CONNECTION_VIEW']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({
                scopes: ['CONNECTION_VIEW'],
                status: 'loaded',
            });
        });

        it('preserves scopes for other workspaces', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopePermissions(99, ['CONNECTION_VIEW']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({
                scopes: ['WORKFLOW_VIEW'],
                status: 'loaded',
            });
            expect(permissionStore.getState().workspaceScopeStates[99]).toEqual({
                scopes: ['CONNECTION_VIEW'],
                status: 'loaded',
            });
        });

        it('replaces a prior loading state with loaded', () => {
            // Discriminated-union invariant: a workspace cannot be simultaneously loading and loaded. Setting the
            // permissions transitions the entry to a single 'loaded' record, which is exactly what the load hook
            // relies on to clear stale ALLOW decisions made during a re-fetch.
            act(() => {
                permissionStore.getState().setWorkspaceScopeLoading(42);
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({
                scopes: ['WORKFLOW_VIEW'],
                status: 'loaded',
            });
        });
    });

    describe('setWorkspaceScopeLoading / setWorkspaceScopeError', () => {
        it('drops cached scopes when transitioning to loading', () => {
            // Without this, a user demoted mid-session would keep their previously cached scopes while the
            // re-fetch is in flight, allowing UI affordances the backend has already revoked. The discriminated
            // union forces the loading entry to drop scopes.
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopeLoading(42);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({status: 'loading'});
        });

        it('drops cached scopes when transitioning to error', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopeError(42);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({status: 'error'});
        });
    });

    describe('setWorkspaceRole', () => {
        it('stores role for a workspace id with status loaded', () => {
            act(() => {
                permissionStore.getState().setWorkspaceRole(7, 'ADMIN');
            });

            expect(permissionStore.getState().workspaceStates[7]).toEqual({
                role: 'ADMIN',
                status: 'loaded',
            });
        });

        it('preserves roles for other workspaces', () => {
            act(() => {
                permissionStore.getState().setWorkspaceRole(7, 'ADMIN');
                permissionStore.getState().setWorkspaceRole(8, 'EDITOR');
            });

            expect(permissionStore.getState().workspaceStates[7]).toEqual({
                role: 'ADMIN',
                status: 'loaded',
            });
            expect(permissionStore.getState().workspaceStates[8]).toEqual({
                role: 'EDITOR',
                status: 'loaded',
            });
        });
    });

    describe('setWorkspaceLoading / setWorkspaceError', () => {
        it('drops cached role when transitioning to loading', () => {
            act(() => {
                permissionStore.getState().setWorkspaceRole(7, 'ADMIN');
                permissionStore.getState().setWorkspaceLoading(7);
            });

            expect(permissionStore.getState().workspaceStates[7]).toEqual({status: 'loading'});
        });

        it('drops cached role when transitioning to error', () => {
            act(() => {
                permissionStore.getState().setWorkspaceRole(7, 'ADMIN');
                permissionStore.getState().setWorkspaceError(7);
            });

            expect(permissionStore.getState().workspaceStates[7]).toEqual({status: 'error'});
        });
    });

    describe('clearPermissions', () => {
        it('removes all stored permissions and roles', () => {
            permissionStore.setState({
                workspaceScopeStates: {42: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
                workspaceStates: {7: {role: 'ADMIN', status: 'loaded'}},
            });

            act(() => {
                permissionStore.getState().clearPermissions();
            });

            expect(permissionStore.getState().workspaceScopeStates).toEqual({});
            expect(permissionStore.getState().workspaceStates).toEqual({});
        });
    });

    describe('clearWorkspaceScopePermissions', () => {
        it('removes the entry for a single workspace and leaves others intact', () => {
            permissionStore.setState({
                workspaceScopeStates: {
                    42: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
                    99: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
                },
                workspaceStates: {},
            });

            act(() => {
                permissionStore.getState().clearWorkspaceScopePermissions(42);
            });

            expect(permissionStore.getState().workspaceScopeStates).toEqual({
                99: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
            });
        });
    });

    describe('clearWorkspaceRole', () => {
        it('removes the entry for a single workspace and leaves others intact', () => {
            permissionStore.setState({
                workspaceScopeStates: {},
                workspaceStates: {
                    7: {role: 'ADMIN', status: 'loaded'},
                    8: {role: 'EDITOR', status: 'loaded'},
                },
            });

            act(() => {
                permissionStore.getState().clearWorkspaceRole(7);
            });

            expect(permissionStore.getState().workspaceStates).toEqual({
                8: {role: 'EDITOR', status: 'loaded'},
            });
        });
    });
});
