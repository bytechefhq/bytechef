import {render, resetAll, screen} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServers from './McpServers';

const hoisted = vi.hoisted(() => ({
    canCreate: true,
    mcpServersResult: {
        allComponentNames: [] as string[],
        componentDefinitions: [],
        componentDefinitionsIsLoading: false,
        filterData: {},
        filteredMcpServers: [] as unknown[],
        mcpProjectsIsLoading: false,
        mcpServersError: null,
        mcpServersIsLoading: false,
        tags: [],
        tagsError: null,
        tagsIsLoading: false,
        uniqueProjects: [],
        validMcpServers: [] as unknown[],
    },
}));

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: () => hoisted.canCreate,
}));

vi.mock('./hooks/useMcpServers', () => ({
    default: () => hoisted.mcpServersResult,
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
    default: ({right}: {right?: ReactNode}) => <div>{right}</div>,
}));

vi.mock('./components/McpServerDialog', () => ({
    default: ({triggerNode}: {triggerNode: ReactNode}) => <>{triggerNode}</>,
}));

vi.mock('./components/McpServersFilterTitle', () => ({
    default: () => null,
}));

vi.mock('./components/McpServersLeftSidebarNav', () => ({
    default: () => null,
}));

vi.mock('./components/mcp-server-list/McpServerList', () => ({
    default: () => <div>Server list</div>,
}));

beforeEach(() => {
    hoisted.canCreate = true;
    hoisted.mcpServersResult.filteredMcpServers = [];
    hoisted.mcpServersResult.validMcpServers = [];
});

afterEach(() => {
    resetAll();
});

describe('McpServers', () => {
    it('offers Create MCP Server on the empty list to a member with MCP_CREATE', () => {
        render(<McpServers />);

        expect(screen.getByRole('button', {name: 'Create MCP Server'})).toBeInTheDocument();
    });

    it('hides Create MCP Server from a member without MCP_CREATE', () => {
        hoisted.canCreate = false;

        render(<McpServers />);

        expect(screen.queryByRole('button', {name: 'Create MCP Server'})).not.toBeInTheDocument();
    });

    it('hides New MCP Server from a member without MCP_CREATE', () => {
        hoisted.canCreate = false;
        hoisted.mcpServersResult.filteredMcpServers = [{id: '1'}];
        hoisted.mcpServersResult.validMcpServers = [{id: '1'}];

        render(<McpServers />);

        expect(screen.getByText('Server list')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'New MCP Server'})).not.toBeInTheDocument();
    });

    it('offers New MCP Server to a member with MCP_CREATE', () => {
        hoisted.mcpServersResult.filteredMcpServers = [{id: '1'}];
        hoisted.mcpServersResult.validMcpServers = [{id: '1'}];

        render(<McpServers />);

        expect(screen.getByRole('button', {name: 'New MCP Server'})).toBeInTheDocument();
    });
});
