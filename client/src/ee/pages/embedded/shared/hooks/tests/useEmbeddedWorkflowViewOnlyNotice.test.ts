import {useEmbeddedWorkflowViewOnlyNotice} from '@/ee/pages/embedded/shared/hooks/useEmbeddedWorkflowViewOnlyNotice';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useEmbeddedWorkflowViewOnlyNotice', () => {
    beforeEach(() => {
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'member'} as never,
            authenticated: true,
        });
    });

    it('stays hidden while the scopes are loading', () => {
        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}});

        const {result} = renderHook(() => useEmbeddedWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('stays hidden while the edition is unknown', () => {
        applicationInfoStore.setState({application: undefined as never});
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useEmbeddedWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });

    it('shows when the scopes failed to load', () => {
        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'error'}}});

        const {result} = renderHook(() => useEmbeddedWorkflowViewOnlyNotice());

        expect(result.current).toBe(true);
    });

    it('shows when loaded without Development workflow edit', () => {
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useEmbeddedWorkflowViewOnlyNotice());

        expect(result.current).toBe(true);
    });

    it('stays hidden when Development workflow edit is held', () => {
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_EDIT', 'WORKFLOW_VIEW'], status: 'loaded'},
            },
        });

        const {result} = renderHook(() => useEmbeddedWorkflowViewOnlyNotice());

        expect(result.current).toBe(false);
    });
});
