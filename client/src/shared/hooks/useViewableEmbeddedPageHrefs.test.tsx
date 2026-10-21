import {DEVELOPMENT_ENVIRONMENT, PRODUCTION_ENVIRONMENT} from '@/shared/constants';
import {UNION_ENVIRONMENT_ID} from '@/shared/hooks/useHasEmbeddedScope';
import {useViewableEmbeddedPageHrefs} from '@/shared/hooks/useViewableEmbeddedPageHrefs';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {permissionStore} from '@/shared/stores/usePermissionStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it} from 'vitest';

describe('useViewableEmbeddedPageHrefs', () => {
    beforeEach(() => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            account: {authorities: ['ROLE_USER'], login: 'tester'} as never,
            authenticated: true,
        });
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        permissionStore.setState({embeddedScopeStates: {}});
    });

    it('shows Automations but hides Integrations when WORKFLOW_VIEW is held only in Development and Production is current', () => {
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW', 'WORKFLOW_VIEW'], status: 'loaded'},
            },
        } as never);

        const {result} = renderHook(() => useViewableEmbeddedPageHrefs());

        expect(result.current).toContain('/embedded/automation-workflows');
        expect(result.current).not.toContain('/embedded/integrations');
        expect(result.current).toContain('/embedded/connections');
    });

    it('shows Automations from the union but hides Integrations when WORKFLOW_VIEW is held only in Production', () => {
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: [], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
            },
        } as never);

        const {result} = renderHook(() => useViewableEmbeddedPageHrefs());

        expect(result.current).toContain('/embedded/automation-workflows');
        expect(result.current).not.toContain('/embedded/integrations');
    });

    it('hides Automations when the union does not hold WORKFLOW_VIEW', () => {
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: [], status: 'loaded'},
                [PRODUCTION_ENVIRONMENT]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: ['CONNECTION_VIEW'], status: 'loaded'},
            },
        } as never);

        const {result} = renderHook(() => useViewableEmbeddedPageHrefs());

        expect(result.current).not.toContain('/embedded/automation-workflows');
        expect(result.current).not.toContain('/embedded/integrations');
        expect(result.current).toContain('/embedded/connections');
    });

    it('shows Integrations in Development when WORKFLOW_VIEW is held there', () => {
        environmentStore.setState({currentEnvironmentId: DEVELOPMENT_ENVIRONMENT});
        permissionStore.setState({
            embeddedScopeStates: {
                [DEVELOPMENT_ENVIRONMENT]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
                [UNION_ENVIRONMENT_ID]: {scopes: ['WORKFLOW_VIEW'], status: 'loaded'},
            },
        } as never);

        const {result} = renderHook(() => useViewableEmbeddedPageHrefs());

        expect(result.current).toEqual(['/embedded/integrations', '/embedded/automation-workflows']);
    });
});
