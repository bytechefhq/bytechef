import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {renderHook} from '@testing-library/react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import {useEnvironmentScopeNote} from '../useEnvironmentScopeNote';

const hoisted = vi.hoisted(() => ({
    edition: 'EE' as string | undefined,
    environments: [] as Array<{id: string; name: string}>,
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: (selector: (state: {application: {edition?: string}}) => unknown) =>
        selector({application: {edition: hoisted.edition}}),
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useEnvironmentsQuery: () => ({data: {environments: hoisted.environments}}),
}));

const allEnvironments = [
    {id: '0', name: 'DEVELOPMENT'},
    {id: '1', name: 'STAGING'},
    {id: '2', name: 'PRODUCTION'},
];

beforeEach(() => {
    hoisted.edition = 'EE';
    hoisted.environments = allEnvironments;

    environmentStore.setState({currentEnvironmentId: 1});
});

describe('useEnvironmentScopeNote', () => {
    it('names the environment being viewed when there is more than one', () => {
        const {result} = renderHook(() => useEnvironmentScopeNote('Deployments'));

        expect(result.current).toBe("Deployments are separate for each environment. You're viewing Staging.");
    });

    it('says nothing when the workspace has a single environment', () => {
        hoisted.environments = [{id: '1', name: 'STAGING'}];

        const {result} = renderHook(() => useEnvironmentScopeNote('Deployments'));

        expect(result.current).toBeUndefined();
    });

    it('says nothing outside the enterprise edition, where environments are not selectable', () => {
        hoisted.edition = 'CE';

        const {result} = renderHook(() => useEnvironmentScopeNote('Deployments'));

        expect(result.current).toBeUndefined();
    });
});
