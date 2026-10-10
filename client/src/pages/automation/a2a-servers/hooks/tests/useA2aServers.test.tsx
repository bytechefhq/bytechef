import {renderHook} from '@testing-library/react';
import {ReactNode} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {describe, expect, it, vi} from 'vitest';

import useA2aServers, {A2aServersFilterType} from '../useA2aServers';

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useA2aProjectsQuery: () => ({
        data: {
            a2aProjects: [
                {a2aServerId: '1', id: '11', projectId: '5'},
                {a2aServerId: '2', id: '12', projectId: '6'},
                {a2aServerId: '9', id: '13', projectId: '7'},
            ],
        },
        isLoading: false,
    }),
    useA2aServerTagsQuery: () => ({
        data: {a2aServerTags: [{id: '3', name: 'sales'}]},
        error: null,
        isLoading: false,
    }),
    useA2aServersQuery: () => ({
        data: {
            a2aServers: [
                {environmentId: '2', id: '1', name: 'Sales agent', tags: [{id: '3', name: 'sales'}]},
                {environmentId: '2', id: '2', name: 'Support agent', tags: []},
                {environmentId: '1', id: '9', name: 'Other environment', tags: []},
            ],
        },
        error: null,
        isLoading: false,
    }),
}));

vi.mock('@/shared/queries/automation/projects.queries', () => ({
    useGetWorkspaceProjectsQuery: () => ({
        data: [
            {id: 5, name: 'Sales project'},
            {id: 6, name: 'Support project'},
            {id: 7, name: 'Other project'},
        ],
    }),
}));

vi.mock('@/pages/automation/stores/useWorkspaceStore', () => {
    const state = {currentWorkspaceId: 1};

    return {useWorkspaceStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

vi.mock('@/shared/stores/useEnvironmentStore', () => {
    const state = {currentEnvironmentId: 2};

    return {useEnvironmentStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

const renderUseA2aServers = (search = '') =>
    renderHook(() => useA2aServers(), {
        wrapper: ({children}: {children: ReactNode}) => (
            <MemoryRouter initialEntries={[`/automation/a2a-servers${search}`]}>{children}</MemoryRouter>
        ),
    });

describe('useA2aServers', () => {
    it('lists the servers and projects of the current environment', () => {
        const {result} = renderUseA2aServers();

        expect(result.current.filteredA2aServers.map((a2aServer) => a2aServer.id)).toEqual(['1', '2']);
        expect(result.current.uniqueProjects).toEqual([
            {id: '5', name: 'Sales project'},
            {id: '6', name: 'Support project'},
        ]);
        expect(result.current.filterData).toEqual({id: undefined, type: A2aServersFilterType.Project});
    });

    it('filters the servers by project', () => {
        const {result} = renderUseA2aServers('?projectId=6');

        expect(result.current.filteredA2aServers.map((a2aServer) => a2aServer.id)).toEqual(['2']);
        expect(result.current.filterData).toEqual({id: '6', type: A2aServersFilterType.Project});
    });

    it('filters the servers by tag', () => {
        const {result} = renderUseA2aServers('?tagId=3');

        expect(result.current.filteredA2aServers.map((a2aServer) => a2aServer.id)).toEqual(['1']);
        expect(result.current.filterData).toEqual({id: '3', type: A2aServersFilterType.Tag});
    });
});
