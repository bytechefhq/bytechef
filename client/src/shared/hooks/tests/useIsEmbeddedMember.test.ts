import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {useIsEmbeddedMember} from '@/shared/hooks/useIsEmbeddedMember';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

function setAccount(authorities: string[]): void {
    authenticationStore.setState({
        account: {authorities, login: 'tester'} as never,
        authenticated: true,
    });
}

describe('useIsEmbeddedMember', () => {
    beforeEach(() => {
        permissionStore.setState({embeddedScopeStates: {}, workspaceScopeStates: {}});
        authenticationStore.setState({account: undefined, authenticated: false});
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
    });

    it('testATenantAdminIsAMember', () => {
        setAccount(['ROLE_ADMIN']);

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: false, member: true});
    });

    it('testCommunityIsAMember', () => {
        applicationInfoStore.setState({application: {edition: EditionType.CE}});
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: false, member: true});
    });

    it('testAMemberHoldsAScopeInAnyEnvironment', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {[UNION_ENVIRONMENT_ID]: {scopes: ['EMBEDDED_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: false, member: true});
    });

    it('testANonMemberIsNotAMember', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {[UNION_ENVIRONMENT_ID]: {scopes: [], status: 'loaded'}},
        });

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: false, member: false});
    });

    it('testReportsLoadingWhileTheUnionLoads', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({embeddedScopeStates: {[UNION_ENVIRONMENT_ID]: {status: 'loading'}}});

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: true, member: false});
    });

    it('testAnUnloadedUnionIsLoadingRatherThanNonMember', () => {
        setAccount(['ROLE_USER']);

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: true, member: false});
    });

    it('testAnErroredUnionIsNotAMember', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({embeddedScopeStates: {[UNION_ENVIRONMENT_ID]: {status: 'error'}}});

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: false, member: false});
    });

    it('testAScopeInASingleEnvironmentDoesNotMakeAMemberWithoutTheUnion', () => {
        setAccount(['ROLE_USER']);
        permissionStore.setState({
            embeddedScopeStates: {[DEVELOPMENT_ENVIRONMENT]: {scopes: ['EMBEDDED_VIEW'], status: 'loaded'}},
        });

        const {result} = renderHook(() => useIsEmbeddedMember());

        expect(result.current).toEqual({loading: true, member: false});
    });
});
