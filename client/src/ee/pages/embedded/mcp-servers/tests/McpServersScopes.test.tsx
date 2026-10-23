import {TooltipProvider} from '@/components/ui/tooltip';
import McpIntegrationInstanceConfigurationListItemDropdownMenu from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationListItemDropdownMenu';
import McpIntegrationInstanceConfigurationWorkflowListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/McpIntegrationInstanceConfigurationWorkflowListItem';
import {
    McpIntegrationInstanceConfigurationItemType,
    McpIntegrationInstanceConfigurationWorkflowItemType,
} from '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationList';
import McpServerList from '@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerList';
import McpServerListItem from '@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerListItem';
import McpComponentListItemDropdownMenu from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentListItemDropdownMenu';
import McpComponentToolListItem from '@/pages/automation/mcp-servers/components/mcp-component-list/McpComponentToolListItem';
import {useWorkspaceStore} from '@/pages/automation/stores/useWorkspaceStore';
import {McpActivePopoverProvider} from '@/shared/contexts/McpActivePopoverContext';
import {McpComponent, McpServer, McpTool} from '@/shared/middleware/graphql';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {ReactNode} from 'react';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
    renderChildren: ({children}: {children: ReactNode}) => <div>{children}</div>,
    useRealScopeHook: false,
    workspaceGrants: [] as string[],
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            hoisted.useRealScopeHook
                ? actual.useHasEmbeddedScope(scope as never, environmentId)
                : (
                      hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []
                  ).includes(scope),
    };
});

vi.mock('@/shared/hooks/useHasWorkspaceScope', () => ({
    useHasWorkspaceScope: (workspaceId: number | undefined, scope: string) =>
        workspaceId !== undefined && hoisted.workspaceGrants.includes(scope),
}));

vi.mock('@/components/ui/collapsible', () => ({
    Collapsible: hoisted.renderChildren,
    CollapsibleContent: hoisted.renderChildren,
    CollapsibleTrigger: hoisted.renderChildren,
}));

vi.mock('@/pages/platform/mcp-servers/components/McpComponentToolPropertiesPopover', () => ({
    default: () => null,
}));

vi.mock(
    '@/pages/automation/mcp-servers/components/mcp-component-list/hooks/useMcpProjectComponentToolDropdownMenu',
    () => ({
        default: () => ({
            handleConfirmDelete: vi.fn(),
            isDeletePending: false,
            setShowDeleteDialog: vi.fn(),
            showDeleteDialog: false,
        }),
    })
);

vi.mock(
    '@/ee/pages/embedded/mcp-servers/components/mcp-integration-instance-configuration-list/hooks/useMcpIntegrationInstanceConfigurationWorkflowListItem',
    () => ({
        default: () => ({
            handleCloseEditDialog: vi.fn(),
            handleConfirmDelete: vi.fn(),
            integrationInstanceConfigurationWorkflow: undefined,
            isDeletePending: false,
            setShowDeleteDialog: vi.fn(),
            setShowEditWorkflowDialog: vi.fn(),
            showDeleteDialog: false,
            showEditWorkflowDialog: false,
            workflow: undefined,
        }),
    })
);

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/hooks/useMcpServerList', () => ({
    default: (mcpServers: unknown[]) => ({createHandleRefresh: () => () => {}, sortedMcpServers: mcpServers}),
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/hooks/useMcpServerListItem', () => ({
    default: () => ({
        handleDeleteClick: vi.fn(),
        handleMcpServerListItemClick: vi.fn(),
        handleOnCheckedChange: vi.fn(),
        isEnablePending: false,
        isPending: false,
        mcpServerTagIds: [],
        setShowDeleteDialog: vi.fn(),
        setShowEditDialog: vi.fn(),
        showDeleteDialog: false,
        showEditDialog: false,
        toolsCollapsibleTriggerRef: {current: null},
        updateEmbeddedMcpServerTagsMutation: {mutate: vi.fn()},
    }),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/middleware/graphql')>();

    return {
        ...actual,
        useMcpIntegrationInstanceConfigurationsByServerIdQuery: () => ({data: undefined}),
    };
});

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-server-list/McpServerToolsContent', () => ({
    default: () => <div>Tools content</div>,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/mcp-component-dialog/McpComponentDialog', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/McpIntegrationInstanceConfigurationWorkflowDialog', () => ({
    default: () => null,
}));

vi.mock('@/ee/pages/embedded/mcp-servers/components/McpServerDialog', () => ({
    default: () => null,
}));

const DEVELOPMENT = 0;
const PRODUCTION = 2;
const SERVER_URL = 'http://127.0.0.1:5173/api/embedded/server-secret-key/mcp';
const URL_HINT = 'Only members who can edit this server can see its URL.';

const mcpServer = {
    enabled: true,
    environmentId: `${PRODUCTION}`,
    id: '1',
    name: 'Support server',
    tags: [],
    url: SERVER_URL,
} as unknown as McpServer;

const mcpTool = {enabled: true, id: '42', name: 'createTicket', title: 'Create Ticket'} as McpTool;

const grantInCurrentEnvironment = (scopes: string[]) => {
    hoisted.grants = {[PRODUCTION]: scopes};
};

const renderTool = () =>
    render(
        <TooltipProvider>
            <McpActivePopoverProvider>
                <McpComponentToolListItem
                    componentName="zendesk"
                    componentVersion={1}
                    connectionId="7"
                    embedded
                    mcpTool={mcpTool}
                />

                <McpComponentListItemDropdownMenu
                    embedded
                    mcpComponent={{id: '3'} as McpComponent}
                    onEditClick={vi.fn()}
                />
            </McpActivePopoverProvider>
        </TooltipProvider>
    );

const renderServerList = () =>
    render(
        <TooltipProvider>
            <McpServerList mcpServers={[mcpServer]} />
        </TooltipProvider>
    );

const openConnectTab = async () => {
    await userEvent.click(screen.getByRole('tab', {name: 'Connect'}));
};

describe('embedded MCP scopes', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.grants = {};
        hoisted.useRealScopeHook = false;
        hoisted.workspaceGrants = [];

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
            useWorkspaceStore.setState({currentWorkspaceId: 1049});
        });
    });

    afterEach(() => {
        act(() => {
            authenticationStore.setState({account: undefined, authenticated: false});
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });

        resetAll();
    });

    it('testEmbeddedMcpNoLongerBypassesTheScopeCheck', () => {
        hoisted.workspaceGrants = ['MCP_EDIT', 'MCP_VIEW'];
        hoisted.grants = {[DEVELOPMENT]: ['MCP_EDIT', 'MCP_VIEW'], [PRODUCTION]: ['MCP_VIEW']};

        const viewerRender = renderTool();

        expect(screen.queryByRole('button', {name: 'Configure Create Ticket'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Create Ticket actions'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'MCP Component Actions'})).not.toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable Create Ticket'})).toBeDisabled();

        viewerRender.unmount();

        hoisted.workspaceGrants = [];
        grantInCurrentEnvironment(['MCP_EDIT', 'MCP_VIEW']);

        renderTool();

        expect(screen.getByRole('button', {name: 'Configure Create Ticket'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Create Ticket actions'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'MCP Component Actions'})).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable Create Ticket'})).toBeEnabled();
    });

    it('testAViewerSeesTheMcpUrlAsHidden', async () => {
        grantInCurrentEnvironment(['MCP_VIEW']);

        renderServerList();

        expect(screen.queryByRole('button', {name: 'Add Component'})).not.toBeInTheDocument();

        await openConnectTab();

        expect(screen.getByText('Hidden')).toBeInTheDocument();
        expect(screen.getByText(URL_HINT)).toBeInTheDocument();
        expect(screen.queryByText(SERVER_URL)).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Copy server URL'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Regenerate server URL'})).not.toBeInTheDocument();
    });

    it('testOnlyATenantAdminRegeneratesTheMcpUrl', async () => {
        grantInCurrentEnvironment(['MCP_EDIT', 'MCP_VIEW']);

        const editorRender = renderServerList();

        await openConnectTab();

        expect(screen.getByText(SERVER_URL)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Copy server URL'})).toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Regenerate server URL'})).not.toBeInTheDocument();

        editorRender.unmount();

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        renderServerList();

        await openConnectTab();

        expect(screen.getByRole('button', {name: 'Regenerate server URL'})).toBeInTheDocument();
    });

    it('testAddingWorkflowToolsAlsoNeedsInstanceConfigurationCreate', async () => {
        grantInCurrentEnvironment(['MCP_EDIT', 'MCP_VIEW']);

        const editorRender = renderServerList();

        expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow Tools'}));

        expect(screen.queryByRole('button', {name: 'Add Workflows'})).not.toBeInTheDocument();

        editorRender.unmount();

        grantInCurrentEnvironment(['INTEGRATION_INSTANCE_CONFIGURATION_CREATE', 'MCP_EDIT', 'MCP_VIEW']);

        renderServerList();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow Tools'}));

        expect(screen.getByRole('button', {name: 'Add Workflows'})).toBeInTheDocument();
    });

    it('testMcpIntegrationConfigurationsFollowTheInstanceConfigurationScopes', async () => {
        const renderConfiguration = () =>
            render(
                <McpActivePopoverProvider>
                    <McpIntegrationInstanceConfigurationListItemDropdownMenu
                        mcpIntegrationInstanceConfiguration={{id: '11'} as McpIntegrationInstanceConfigurationItemType}
                        onEditWorkflowsClick={vi.fn()}
                        onUpdateIntegrationVersionClick={vi.fn()}
                    />

                    <McpIntegrationInstanceConfigurationWorkflowListItem
                        componentName="hubspot"
                        mcpIntegrationInstanceConfigurationWorkflow={
                            {
                                id: '9',
                                workflow: {label: 'Sync contacts'},
                            } as McpIntegrationInstanceConfigurationWorkflowItemType
                        }
                    />
                </McpActivePopoverProvider>
            );

        grantInCurrentEnvironment(['INTEGRATION_INSTANCE_CONFIGURATION_VIEW', 'MCP_EDIT', 'MCP_VIEW']);

        const viewerRender = renderConfiguration();

        expect(screen.queryByRole('button', {name: 'MCP Integration Actions'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Configure Sync contacts'})).not.toBeInTheDocument();
        expect(screen.queryByRole('button', {name: 'Sync contacts actions'})).not.toBeInTheDocument();

        viewerRender.unmount();

        grantInCurrentEnvironment([
            'INTEGRATION_INSTANCE_CONFIGURATION_EDIT',
            'INTEGRATION_INSTANCE_CONFIGURATION_VIEW',
        ]);

        renderConfiguration();

        expect(screen.getByRole('button', {name: 'Configure Sync contacts'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'MCP Integration Actions'}));

        expect(await screen.findByText('Edit Workflows')).toBeInTheDocument();
        expect(screen.getByText('Update Integration Version')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testMcpServerControlsFollowTheMcpScopes', async () => {
        grantInCurrentEnvironment(['MCP_VIEW']);

        const viewerRender = render(
            <TooltipProvider>
                <McpServerListItem mcpServer={mcpServer} />
            </TooltipProvider>
        );

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'MCP Server Actions'})).not.toBeInTheDocument();

        viewerRender.unmount();

        grantInCurrentEnvironment(['MCP_EDIT', 'MCP_VIEW']);

        render(
            <TooltipProvider>
                <McpServerListItem mcpServer={mcpServer} />
            </TooltipProvider>
        );

        expect(screen.getByRole('switch')).toBeEnabled();

        await userEvent.click(screen.getByRole('button', {name: 'MCP Server Actions'}));

        expect(await screen.findByText('Edit')).toBeInTheDocument();
        expect(screen.queryByText('Delete')).not.toBeInTheDocument();
    });

    it('testATenantAdminSeesEveryControl', async () => {
        hoisted.useRealScopeHook = true;

        act(() => {
            authenticationStore.setState({
                account: {authorities: ['ROLE_ADMIN'], login: 'admin'} as never,
                authenticated: true,
            });
        });

        const toolRender = renderTool();

        expect(screen.getByRole('button', {name: 'Configure Create Ticket'})).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'MCP Component Actions'})).toBeInTheDocument();
        expect(screen.getByRole('switch', {name: 'Enable Create Ticket'})).toBeEnabled();

        toolRender.unmount();

        renderServerList();

        expect(screen.getByRole('button', {name: 'Add Component'})).toBeInTheDocument();

        await userEvent.click(screen.getByRole('tab', {name: 'Workflow Tools'}));

        expect(screen.getByRole('button', {name: 'Add Workflows'})).toBeInTheDocument();

        await openConnectTab();

        expect(screen.getByText(SERVER_URL)).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Regenerate server URL'})).toBeInTheDocument();
    });
});
