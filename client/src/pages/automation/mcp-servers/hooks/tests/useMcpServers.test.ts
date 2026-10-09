import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {renderHook} from '@testing-library/react';
import {ReactNode, createElement} from 'react';
import {MemoryRouter} from 'react-router-dom';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import useMcpServers from '../useMcpServers';

const hoisted = vi.hoisted(() => ({
    useMcpProjectsQuery: vi.fn(),
    useWorkspaceMcpServerTagsQuery: vi.fn(),
    useWorkspaceMcpServersQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useMcpProjectsQuery: hoisted.useMcpProjectsQuery,
    useWorkspaceMcpServerTagsQuery: hoisted.useWorkspaceMcpServerTagsQuery,
    useWorkspaceMcpServersQuery: hoisted.useWorkspaceMcpServersQuery,
}));

vi.mock('@/shared/queries/automation/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [], isLoading: false}),
}));

const wrapper = ({children}: {children: ReactNode}) => createElement(MemoryRouter, null, children);

describe('useMcpServers', () => {
    beforeEach(() => {
        useWorkspaceStore.setState({currentWorkspaceId: 42});
        environmentStore.setState({currentEnvironmentId: 1});

        hoisted.useWorkspaceMcpServersQuery.mockReturnValue({
            data: {
                workspaceMcpServers: [
                    {environmentId: '1', id: 'server-1', mcpComponents: [], tags: []},
                    {environmentId: '1', id: 'server-2', mcpComponents: [], tags: []},
                ],
            },
            error: null,
            isLoading: false,
        });
        hoisted.useWorkspaceMcpServerTagsQuery.mockReturnValue({
            data: {workspaceMcpServerTags: [{id: 'tag-1', name: 'sales'}]},
            error: null,
            isLoading: false,
        });
        hoisted.useMcpProjectsQuery.mockReturnValue({
            data: {
                mcpProjects: [
                    {mcpServerId: 'server-1', project: {id: 'project-1', name: 'Sales'}},
                    {mcpServerId: 'server-other', project: {id: 'project-2', name: 'Other workspace'}},
                ],
            },
            isLoading: false,
        });
    });

    it('should scope the tag and project queries to the current workspace', () => {
        renderHook(() => useMcpServers(), {wrapper});

        expect(hoisted.useWorkspaceMcpServersQuery).toHaveBeenCalledWith({workspaceId: '42'});
        expect(hoisted.useWorkspaceMcpServerTagsQuery).toHaveBeenCalledWith({workspaceId: '42'});
        expect(hoisted.useMcpProjectsQuery).toHaveBeenCalledWith({workspaceId: '42'});
    });

    it('should expose the workspace MCP server tags', () => {
        const {result} = renderHook(() => useMcpServers(), {wrapper});

        expect(result.current.tags).toEqual([{id: 'tag-1', name: 'sales'}]);
    });

    it('should expose no tags while the workspace tags are not loaded', () => {
        hoisted.useWorkspaceMcpServerTagsQuery.mockReturnValue({data: undefined, error: null, isLoading: true});

        const {result} = renderHook(() => useMcpServers(), {wrapper});

        expect(result.current.tags).toBeUndefined();
        expect(result.current.tagsIsLoading).toBe(true);
    });

    it('should only list projects attached to the workspace MCP servers', () => {
        const {result} = renderHook(() => useMcpServers(), {wrapper});

        expect(result.current.uniqueProjects).toEqual([{id: 'project-1', name: 'Sales'}]);
    });
});
