import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {act} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('permissionStore', () => {
    beforeEach(() => {
        permissionStore.setState({
            workspaceScopeStates: {},
        });
    });

    describe('setWorkspaceScopePermissions', () => {
        it('stores scopes for a workspace id and environment with status loaded', () => {
            act(() => {
                permissionStore
                    .getState()
                    .setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['WORKFLOW_VIEW', 'WORKFLOW_EDIT']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42][DEVELOPMENT_ENVIRONMENT]).toEqual({
                scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'],
                status: 'loaded',
            });
        });

        it('preserves scopes for other workspaces', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['WORKFLOW_VIEW']);
                permissionStore
                    .getState()
                    .setWorkspaceScopePermissions(99, DEVELOPMENT_ENVIRONMENT, ['CONNECTION_VIEW']);
            });

            expect(permissionStore.getState().workspaceScopeStates[42][DEVELOPMENT_ENVIRONMENT]).toEqual({
                scopes: ['WORKFLOW_VIEW'],
                status: 'loaded',
            });
            expect(permissionStore.getState().workspaceScopeStates[99][DEVELOPMENT_ENVIRONMENT]).toEqual({
                scopes: ['CONNECTION_VIEW'],
                status: 'loaded',
            });
        });

        it('keeps each environment of a workspace separate', () => {
            act(() => {
                permissionStore
                    .getState()
                    .setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['DEPLOYMENT_CREATE']);
                permissionStore.getState().setWorkspaceScopePermissions(42, PRODUCTION_ENVIRONMENT, []);
            });

            expect(permissionStore.getState().workspaceScopeStates[42]).toEqual({
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['DEPLOYMENT_CREATE'], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
            });
        });
    });

    describe('setWorkspaceScopeLoading / setWorkspaceScopeError', () => {
        it('drops cached scopes when transitioning to loading', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopeLoading(42, DEVELOPMENT_ENVIRONMENT);
            });

            expect(permissionStore.getState().workspaceScopeStates[42][DEVELOPMENT_ENVIRONMENT]).toEqual({
                status: 'loading',
            });
        });

        it('drops cached scopes when transitioning to error', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopeError(42, DEVELOPMENT_ENVIRONMENT);
            });

            expect(permissionStore.getState().workspaceScopeStates[42][DEVELOPMENT_ENVIRONMENT]).toEqual({
                status: 'error',
            });
        });

        it('leaves the other environments of the workspace untouched', () => {
            act(() => {
                permissionStore.getState().setWorkspaceScopePermissions(42, DEVELOPMENT_ENVIRONMENT, ['WORKFLOW_VIEW']);
                permissionStore.getState().setWorkspaceScopeLoading(42, PRODUCTION_ENVIRONMENT);
            });

            expect(permissionStore.getState().workspaceScopeStates[42][DEVELOPMENT_ENVIRONMENT]).toEqual({
                scopes: ['WORKFLOW_VIEW'],
                status: 'loaded',
            });
            expect(permissionStore.getState().workspaceScopeStates[42][PRODUCTION_ENVIRONMENT]).toEqual({
                status: 'loading',
            });
        });
    });

    describe('clearPermissions', () => {
        it('removes all stored permissions', () => {
            permissionStore.setState({
                workspaceScopeStates: {42: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}}},
            });

            act(() => {
                permissionStore.getState().clearPermissions();
            });

            expect(permissionStore.getState().workspaceScopeStates).toEqual({});
        });
    });
});
