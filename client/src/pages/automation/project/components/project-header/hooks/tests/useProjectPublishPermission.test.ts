import useProjectPublishPermission from '@/pages/automation/project/components/project-header/hooks/useProjectPublishPermission';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

const WORKSPACE_ID = 1049;

function setEnterpriseMemberScopeState(scopeState: {scopes?: string[]; status: 'loaded' | 'loading'}): void {
    applicationInfoStore.setState({application: {edition: EditionType.EE}});
    authenticationStore.setState({
        account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
        authenticated: true,
    });
    permissionStore.setState({
        workspaceScopeStates: {[WORKSPACE_ID]: {[DEVELOPMENT_ENVIRONMENT]: scopeState}},
    } as never);
}

describe('useProjectPublishPermission', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: WORKSPACE_ID});
    });

    it('allows publishing for a member holding both PROJECT_PUBLISH and WORKFLOW_EDIT', () => {
        setEnterpriseMemberScopeState({scopes: ['PROJECT_PUBLISH', 'WORKFLOW_EDIT'], status: 'loaded'});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(true);
        expect(result.current.disabledTooltip).toBe('No changes to publish');
    });

    it('denies publishing for a member holding neither publish scope', () => {
        setEnterpriseMemberScopeState({scopes: ['WORKFLOW_VIEW'], status: 'loaded'});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(false);
        expect(result.current.disabledTooltip).toBe('You do not have permission to publish this project');
    });

    it('denies publishing for a member holding PROJECT_PUBLISH but not WORKFLOW_EDIT', () => {
        setEnterpriseMemberScopeState({scopes: ['PROJECT_PUBLISH'], status: 'loaded'});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(false);
    });

    it('denies publishing for a member holding WORKFLOW_EDIT but not PROJECT_PUBLISH', () => {
        setEnterpriseMemberScopeState({scopes: ['WORKFLOW_EDIT'], status: 'loaded'});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(false);
    });

    it('does not call an entitled member unauthorized while the scopes are still loading', () => {
        setEnterpriseMemberScopeState({status: 'loading'});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(false);
        expect(result.current.disabledTooltip).toBe('Checking your permissions');
    });

    it('keeps publishing available on Community, where members have no distinct permissions', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        permissionStore.setState({workspaceScopeStates: {}});

        const {result} = renderHook(() => useProjectPublishPermission());

        expect(result.current.canPublishProject).toBe(true);
    });
});
