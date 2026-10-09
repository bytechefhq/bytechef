import useCopilotPostTurnRegistry from '@/shared/components/copilot/stores/useCopilotPostTurnRegistry';
import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render} from '@testing-library/react';
import {ReactNode} from 'react';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import McpServers from '../McpServers';

vi.mock('../hooks/useMcpServers', () => ({
    default: () => ({
        allComponentNames: [],
        componentDefinitions: [],
        componentDefinitionsIsLoading: false,
        filterData: {id: undefined, type: 0},
        filteredMcpServers: [],
        mcpProjectsIsLoading: false,
        mcpServersError: null,
        mcpServersIsLoading: false,
        tags: [],
        tagsError: null,
        tagsIsLoading: false,
        uniqueProjects: [],
        validMcpServers: [],
    }),
}));

vi.mock('@/shared/layout/LayoutContainer', () => ({
    default: ({children, header}: {children: ReactNode; header: ReactNode}) => (
        <div>
            {header}

            {children}
        </div>
    ),
}));

vi.mock('@/shared/layout/Header', () => ({
    default: ({right}: {right?: ReactNode}) => <header>{right}</header>,
}));

vi.mock('@/shared/components/copilot/CopilotButton', () => ({
    default: () => null,
}));

vi.mock('../components/McpServerDialog', () => ({
    default: () => null,
}));

vi.mock('../components/McpServersLeftSidebarNav', () => ({
    default: () => null,
}));

const renderMcpServers = () => {
    const queryClient = new QueryClient();
    const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries').mockResolvedValue();

    const result = render(
        <QueryClientProvider client={queryClient}>
            <McpServers />
        </QueryClientProvider>
    );

    return {...result, invalidateQueriesSpy};
};

describe('McpServers', () => {
    beforeEach(() => {
        useCopilotPostTurnRegistry.setState({callbacks: {}});
    });

    it('should refresh the MCP server queries after a copilot turn for the MCP server source', () => {
        const {invalidateQueriesSpy} = renderMcpServers();

        useCopilotPostTurnRegistry.getState().runFor(Source.MCP_SERVER);

        expect(invalidateQueriesSpy).toHaveBeenCalledTimes(3);
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['workspaceMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpProjects']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['mcpProjectsByServerId']});
    });

    it('should not refresh the MCP server queries after a copilot turn for another source', () => {
        const {invalidateQueriesSpy} = renderMcpServers();

        useCopilotPostTurnRegistry.getState().runFor(Source.WORKFLOW_EDITOR);

        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
    });

    it('should unregister the post-turn callback when unmounted', () => {
        const {invalidateQueriesSpy, unmount} = renderMcpServers();

        unmount();

        useCopilotPostTurnRegistry.getState().runFor(Source.MCP_SERVER);

        expect(useCopilotPostTurnRegistry.getState().callbacks[Source.MCP_SERVER]).toBeUndefined();
        expect(invalidateQueriesSpy).not.toHaveBeenCalled();
    });
});
