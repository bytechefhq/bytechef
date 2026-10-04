import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import McpServerList from './McpServerList';

vi.mock('@/components/ui/collapsible', () => ({
    Collapsible: ({children}: {children: ReactNode}) => <div>{children}</div>,
    CollapsibleContent: ({children}: {children: ReactNode}) => <div>{children}</div>,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpProjectsByServerIdQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/components/mcp-server/McpServerConfiguration', () => ({
    default: () => <div>Server configuration</div>,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialog', () => ({
    default: ({mcpServerId}: {mcpServerId: string}) => <div role="dialog">Component dialog for {mcpServerId}</div>,
}));

vi.mock('@/pages/automation/mcp-servers/components/McpProjectWorkflowDialog', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <div role="dialog">Workflow dialog for {mcpServer.name}</div>,
}));

vi.mock('./McpServerListItem', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <div>{mcpServer.name}</div>,
}));

vi.mock('./McpServerToolsContent', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <div>Tools of {mcpServer.name}</div>,
}));

vi.mock('./hooks/useMcpServerList', () => ({
    default: (mcpServers: McpServer[]) => ({createHandleRefresh: () => vi.fn(), sortedMcpServers: mcpServers}),
}));

const mcpServers = [{id: '1', name: 'mcpserver1'} as McpServer];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerList', () => {
    it('opens the project component dialog from the Tools tab Add Component button', async () => {
        render(<McpServerList mcpServers={mcpServers} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

        expect(screen.getByText('Component dialog for 1')).toBeInTheDocument();
    });

    it('opens the project workflow dialog from the Tools tab Add Workflows menu item', async () => {
        render(<McpServerList mcpServers={mcpServers} />);

        await userEvent.click(screen.getByRole('button', {name: 'Add Tools'}));
        await userEvent.click(screen.getByRole('menuitem', {name: 'Add Workflows'}));

        expect(screen.getByText('Workflow dialog for mcpserver1')).toBeInTheDocument();
    });

    it('shows the server configuration on the Connect tab', async () => {
        render(<McpServerList mcpServers={mcpServers} />);

        await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));

        expect(screen.getByText('Server configuration')).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();
    });
});
