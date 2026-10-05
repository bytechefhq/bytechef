import EmbeddedMcpServerList from '@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerList';
import AutomationMcpServerList from '@/pages/automation/mcp-servers/components/mcp-server-list/McpServerList';
import {McpServer} from '@/shared/middleware/graphql';
import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ComponentType, ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

interface McpServerListPropsI {
    mcpServers: McpServer[];
}

const hoisted = vi.hoisted(() => ({
    createDialogMock:
        (label: string) =>
        ({mcpServer, mcpServerId}: {mcpServer?: {name: string}; mcpServerId?: string}) => (
            <div role="dialog">{`${label} for ${mcpServerId ?? mcpServer?.name}`}</div>
        ),
    renderChildren: ({children}: {children: ReactNode}) => <div>{children}</div>,
    renderServerName: ({mcpServer}: {mcpServer: {name: string}}) => <div>{mcpServer.name}</div>,
    useMcpServerList: (mcpServers: unknown[]) => ({createHandleRefresh: () => () => {}, sortedMcpServers: mcpServers}),
}));

vi.mock('@/components/ui/collapsible', () => ({
    Collapsible: hoisted.renderChildren,
    CollapsibleContent: hoisted.renderChildren,
}));

vi.mock('@/shared/middleware/graphql', () => ({
    useMcpIntegrationInstanceConfigurationsByServerIdQuery: () => ({data: undefined}),
    useMcpProjectsByServerIdQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/components/mcp-server/McpServerConfiguration', () => ({
    default: () => <div>Server configuration</div>,
}));

vi.mock('@/shared/components/mcp-server/McpServerConfigurationCode', () => ({
    default: () => <div>Server configuration</div>,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-component-dialog/McpComponentDialog', () => ({
    default: hoisted.createDialogMock('Project component dialog'),
}));

vi.mock('@/pages/automation/mcp-servers/components/McpProjectWorkflowDialog', () => ({
    default: hoisted.createDialogMock('Project workflow dialog'),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog', () => ({
    default: hoisted.createDialogMock('Integration component dialog'),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog', () => ({
    default: hoisted.createDialogMock('Integration workflow dialog'),
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-server-list/McpServerListItem', () => ({
    default: hoisted.renderServerName,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerListItem', () => ({
    default: hoisted.renderServerName,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-server-list/McpServerToolsContent', () => ({
    default: () => <div>Tools content</div>,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerToolsContent', () => ({
    default: () => <div>Tools content</div>,
}));

vi.mock('@/pages/automation/mcp-servers/components/mcp-server-list/hooks/useMcpServerList', () => ({
    default: hoisted.useMcpServerList,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/hooks/useMcpServerList', () => ({
    default: hoisted.useMcpServerList,
}));

const mcpServers = [{id: '1', name: 'mcpserver1'} as McpServer];

const editions: {
    componentDialogLabel: string;
    edition: string;
    mcpServerList: ComponentType<McpServerListPropsI>;
    workflowDialogLabel: string;
}[] = [
    {
        componentDialogLabel: 'Project component dialog for 1',
        edition: 'automation',
        mcpServerList: AutomationMcpServerList,
        workflowDialogLabel: 'Project workflow dialog for mcpserver1',
    },
    {
        componentDialogLabel: 'Integration component dialog for 1',
        edition: 'embedded',
        mcpServerList: EmbeddedMcpServerList,
        workflowDialogLabel: 'Integration workflow dialog for mcpserver1',
    },
];

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe.each(editions)(
    '$edition McpServerList',
    ({componentDialogLabel, mcpServerList: McpServerList, workflowDialogLabel}) => {
        it('opens its own component dialog from the Tools tab Add Component button', async () => {
            render(<McpServerList mcpServers={mcpServers} />);

            await userEvent.click(screen.getByRole('button', {name: 'Add Component'}));

            expect(screen.getByText(componentDialogLabel)).toBeInTheDocument();
        });

        it('opens its own workflow dialog from the Workflows tab Add Workflows button', async () => {
            render(<McpServerList mcpServers={mcpServers} />);

            await userEvent.click(screen.getByRole('tab', {name: 'Workflows'}));
            await userEvent.click(screen.getByRole('button', {name: 'Add Workflows'}));

            expect(screen.getByText(workflowDialogLabel)).toBeInTheDocument();
        });

        it('shows the server configuration on the Connect tab', async () => {
            render(<McpServerList mcpServers={mcpServers} />);

            await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));

            expect(screen.getByText('Server configuration')).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();
        });
    }
);
