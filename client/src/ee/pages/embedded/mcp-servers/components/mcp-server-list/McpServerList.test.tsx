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
    useMcpIntegrationInstanceConfigurationsByServerIdQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: () => <div>Server configuration</div>,
}));

vi.mock('./McpServerListItem', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <div>{mcpServer.name}</div>,
}));

vi.mock('./McpServerToolsAddButton', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <button>Add tools to {mcpServer.name}</button>,
}));

vi.mock('./McpServerToolsContent', () => ({
    default: ({mcpServer}: {mcpServer: McpServer}) => <div>Tools of {mcpServer.name}</div>,
}));

vi.mock('./hooks/useMcpServerList', () => ({
    default: (mcpServers: McpServer[]) => ({createHandleRefresh: () => vi.fn(), sortedMcpServers: mcpServers}),
}));

const mcpServers = [{id: '1', name: 'mcpserver1'} as McpServer, {id: '2', name: 'mcpserver2'} as McpServer];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('McpServerList', () => {
    it('shows the Add button next to the tabs while the Tools tab is active', () => {
        render(<McpServerList mcpServers={mcpServers} />);

        expect(screen.getByRole('button', {name: 'Add tools to mcpserver1'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add tools to mcpserver2'})).toBeInTheDocument();
    });

    it('hides the Add button only for the server whose Connect tab is selected', async () => {
        render(<McpServerList mcpServers={mcpServers} />);

        const [firstConnectTab] = screen.getAllByRole('tab', {name: 'Connect'});

        await userEvent.click(firstConnectTab);

        expect(screen.queryByRole('button', {name: 'Add tools to mcpserver1'})).not.toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Add tools to mcpserver2'})).toBeInTheDocument();
    });

    it('shows the Add button again when switching back to the Tools tab', async () => {
        render(<McpServerList mcpServers={mcpServers} />);

        const [firstConnectTab] = screen.getAllByRole('tab', {name: 'Connect'});
        const [firstToolsTab] = screen.getAllByRole('tab', {name: 'Tools'});

        await userEvent.click(firstConnectTab);
        await userEvent.click(firstToolsTab);

        expect(screen.getByRole('button', {name: 'Add tools to mcpserver1'})).toBeInTheDocument();
    });
});
