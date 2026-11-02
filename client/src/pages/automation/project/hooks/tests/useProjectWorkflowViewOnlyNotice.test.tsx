import {useProjectWorkflowViewOnlyNotice} from '@/pages/automation/project/hooks/useProjectWorkflowViewOnlyNotice';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {WorkspaceScopePermissionStateType, permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const WORKSPACE_ID = 1049;

const setWorkspaceScopeState = (workspaceScopeState: WorkspaceScopePermissionStateType) =>
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: workspaceScopeState}},
    });

describe('useProjectWorkflowViewOnlyNotice', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'viewer'} as never,
            authenticated: true,
        });
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        permissionStore.setState({workspaceScopeStates: {}});
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    it('stays hidden before the scopes start loading', () => {
        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('stays hidden while the scopes load', () => {
        setWorkspaceScopeState({status: 'loading'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('shows once the scopes loaded without WORKFLOW_EDIT', () => {
        setWorkspaceScopeState({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(true);
    });

    it('shows when the scopes failed to load', () => {
        setWorkspaceScopeState({status: 'error'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(true);
    });

    it('stays hidden once the scopes loaded with WORKFLOW_EDIT', () => {
        setWorkspaceScopeState({scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'], status: 'loaded'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('stays hidden while the edition is unresolved', () => {
        applicationInfoStore.setState({application: null});
        setWorkspaceScopeState({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('never shows on Community', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        setWorkspaceScopeState({status: 'error'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('never shows for a tenant admin', () => {
        authenticationStore.setState({
            account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
            authenticated: true,
        });
        setWorkspaceScopeState({scopes: [], status: 'loaded'});

        const {result} = renderHook(() => useProjectWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });
});
