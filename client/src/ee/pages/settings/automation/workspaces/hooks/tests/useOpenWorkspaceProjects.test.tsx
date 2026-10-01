import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {DEVELOPMENT_ENVIRONMENT} from '@/shared/constants';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter, useLocation} from 'react-router-dom';
import {beforeEach, describe, expect, it} from 'vitest';

import useOpenWorkspaceProjects from '../useOpenWorkspaceProjects';

const PRODUCTION_ENVIRONMENT = 2;

const wrapper = ({children}: {children: ReactNode}) => (
    <MemoryRouter initialEntries={['/automation/settings/workspaces']}>{children}</MemoryRouter>
);

describe('useOpenWorkspaceProjects', () => {
    beforeEach(() => {
        environmentStore.setState({currentEnvironmentId: PRODUCTION_ENVIRONMENT});
        useWorkspaceStore.setState({currentWorkspaceId: 1});
    });

    it('switches to the workspace, selects the Development environment and opens the projects page', () => {
        const {result} = renderHook(
            () => ({location: useLocation(), openWorkspaceProjects: useOpenWorkspaceProjects()}),
            {wrapper}
        );

        act(() => {
            result.current.openWorkspaceProjects(7);
        });

        expect(useWorkspaceStore.getState().currentWorkspaceId).toBe(7);
        expect(environmentStore.getState().currentEnvironmentId).toBe(DEVELOPMENT_ENVIRONMENT);
        expect(result.current.location.pathname).toBe('/automation/projects');
    });
});
