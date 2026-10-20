import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID, useEmbeddedScopeState, useHasEmbeddedScope} from '@/shared/hooks/useHasEmbeddedScope';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {act, renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

function setAccount(authorities: string[]): void {
    authenticationStore.setState({
        account: {authorities, login: 'tester'} as never,
        authenticated: true,
    });
}

describe('useHasEmbeddedScope', () => {
    beforeEach(() => {
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        authenticationStore.setState({account: undefined, authenticated: false});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    it('testTenantAdminHoldsEveryScope', () => {
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_DELETE'));

        expect(result.current).toBe(true);
    });

    it('testReturnsTrueWhenTheUserHasTheScope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW', 'WORKFLOW_EDIT'], status: 'loaded'},
            },
        });

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_EDIT'));

        expect(result.current).toBe(true);
    });

    it('testReturnsFalseWhenTheUserLacksTheScope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });

    it('testReturnsFalseWhileLoading', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}});

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('testReturnsFalseOnError', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'error'}}});

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('testReturnsFalseWhenNothingIsLoaded', () => {
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('testReturnsFalseWithoutAnAccountAndNothingLoaded', () => {
        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_VIEW'));

        expect(result.current).toBe(false);
    });

    it('testAdminAuthorityDoesNotApplyWhenNotAuthenticated', () => {
        authenticationStore.setState({
            account: {authorities: ['ROLE_ADMIN'], login: 'tester'} as never,
            authenticated: false,
        });

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_DELETE'));

        expect(result.current).toBe(false);
    });

    it('testAnswersForTheCurrentEnvironment', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['MCP_CREATE'], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
            },
        });

        const {result} = renderHook(() => useHasEmbeddedScope('MCP_CREATE'));

        expect(result.current).toBe(false);
    });

    it('testChangesItsAnswerWhenTheEnvironmentSwitches', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['MCP_CREATE'], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: ['MCP_VIEW'], status: 'loaded'},
            },
        });

        const {result} = renderHook(() => useHasEmbeddedScope('MCP_CREATE'));

        expect(result.current).toBe(true);

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        });

        expect(result.current).toBe(false);
    });

    it('testAnswersForAFixedEnvironment', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_EDIT'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_EDIT', DEVELOPMENT_ENVIRONMENT));

        expect(result.current).toBe(true);
    });

    it('testFailsClosedInAnEnvironmentThatHasNotLoaded', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['MCP_CREATE'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useHasEmbeddedScope('MCP_CREATE'));

        expect(result.current).toBe(false);
    });

    it('testReturnsTrueOnCommunityWithNothingLoaded', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
            authenticated: true,
        });

        const {result} = renderHook(() => useHasEmbeddedScope('INTEGRATION_DELETE'));

        expect(result.current).toBe(true);
    });

    it('testDoesNotGrantOnAnUnresolvedEdition', () => {
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useHasEmbeddedScope('INTEGRATION_DELETE'));

        expect(result.current).toBe(false);
    });

    it('testTheUnionAnswersAScopeHeldInAnyEnvironment', () => {
        setAccount(['ROLE_USER']);
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({
            embeddedScopeStates: {
                [PRODUCTION_ENVIRONMENT]: {scopes: [], status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: ['INTEGRATION_CREATE'], status: 'loaded'},
            },
        });

        const {result: unionResult} = renderHook(() => useHasEmbeddedScope('INTEGRATION_CREATE', UNION_ENVIRONMENT_ID));
        const {result: currentResult} = renderHook(() => useHasEmbeddedScope('INTEGRATION_CREATE'));

        expect(unionResult.current).toBe(true);
        expect(currentResult.current).toBe(false);
    });

    it('testAWorkspaceScopeIsNeverReadForAnEmbeddedScope', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {},
            workspaceScopeStates: {
                42: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_EDIT'], status: 'loaded'}},
            },
        });

        const {result} = renderHook(() => useHasEmbeddedScope('WORKFLOW_EDIT'));

        expect(result.current).toBe(false);
    });
});

describe('useEmbeddedScopeState', () => {
    beforeEach(() => {
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        authenticationStore.setState({account: undefined, authenticated: false});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    it('testReportsAnUnresolvedEditionAsItsOwnState', () => {
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useEmbeddedScopeState('WORKFLOW_VIEW'));

        expect(result.current).toEqual({editionUnknown: true, error: false, granted: false, loading: false});
    });

    it('testReportsGrantedForATenantAdmin', () => {
        applicationInfoStore.setState({application: null});
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useEmbeddedScopeState('WORKFLOW_VIEW'));

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: true, loading: false});
    });

    it('testReportsAPlainDenial', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useEmbeddedScopeState('WORKFLOW_DELETE'));

        expect(result.current).toEqual({editionUnknown: false, error: false, granted: false, loading: false});
    });

    it('testReportsLoadingAndErrorSeparately', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'loading'}}});

        const {result: loadingResult} = renderHook(() => useEmbeddedScopeState('WORKFLOW_VIEW'));

        expect(loadingResult.current).toEqual({editionUnknown: false, error: false, granted: false, loading: true});

        permissionStore.setState({embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {status: 'error'}}});

        const {result: errorResult} = renderHook(() => useEmbeddedScopeState('WORKFLOW_VIEW'));

        expect(errorResult.current).toEqual({editionUnknown: false, error: true, granted: false, loading: false});
    });
});
