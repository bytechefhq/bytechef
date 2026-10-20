import {TooltipProvider} from '@/components/ui/tooltip';
import ConnectedUserSheetPanelIntegrationListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationListItem';
import ConnectedUserSheetPanelIntegrationWorkflowListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowListItem';
import ConnectedUserMcpServerListItem from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItem';
import ConnectedUserMcpServerListItemToolRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemToolRow';
import ConnectedUserMcpServerListItemWorkflowRow from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-mcp-server-list/ConnectedUserMcpServerListItemWorkflowRow';
import ConnectedUserProjectWorkflowListItem, {
    ConnectedUserProjectWorkflowType,
} from '@/ee/pages/embedded/connected-users/components/connected-user-sheet/connected-user-project-workflow-list/ConnectedUserProjectWorkflowListItem';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {act, render, screen, userEvent} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const hoisted = vi.hoisted(() => ({
    grants: {} as Record<number, string[]>,
}));

vi.mock('@/shared/hooks/useHasEmbeddedScope', async (importOriginal) => {
    const actual = await importOriginal<typeof import('@/shared/hooks/useHasEmbeddedScope')>();
    const {environmentStore: mockedEnvironmentStore} = await import('@/shared/stores/useEnvironmentStore');

    return {
        ...actual,
        useHasEmbeddedScope: (scope: string, environmentId?: number) =>
            (hoisted.grants[environmentId ?? mockedEnvironmentStore.getState().currentEnvironmentId] ?? []).includes(
                scope
            ),
    };
});

vi.mock('@/ee/shared/queries/embedded/integrationWorkflows.queries', () => ({
    useGetIntegrationVersionWorkflowsQuery: () => ({data: []}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstanceConfigurations.queries', () => ({
    useGetIntegrationInstanceConfigurationQuery: () => ({data: undefined}),
}));

vi.mock('@/ee/shared/queries/embedded/integrationInstances.queries', () => ({
    IntegrationInstanceKeys: {integrationInstance: (id: number) => ['integrationInstance', id]},
    useGetIntegrationInstanceQuery: () => ({
        data: {integrationInstanceConfiguration: {enabled: true, integrationVersion: 1, name: 'Config'}},
    }),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstances.mutations', () => ({
    useDeleteIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableIntegrationInstanceMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations', () => ({
    useEnableIntegrationInstanceWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    ConnectedUserKeys: {connectedUser: (id: number) => ['connectedUser', id], connectedUsers: ['connectedUsers']},
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({data: undefined}),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useDeleteConnectedUserMcpServerMutation: () => ({isPending: false, mutate: vi.fn()}),
    useDeleteConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableConnectedUserMcpServerMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableConnectedUserMcpToolMutation: () => ({isPending: false, mutate: vi.fn()}),
    useEnableConnectedUserProjectWorkflowMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('react-inlinesvg', () => ({default: () => <span />}));

vi.mock('@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserCredentialStatus', () => ({
    default: () => <span />,
}));

vi.mock(
    '@/ee/pages/embedded/connected-users/components/connected-user-sheet/ConnectedUserSheetPanelIntegrationWorkflowList',
    () => ({default: () => null})
);

const DEVELOPMENT = 0;
const PRODUCTION = 2;

const grant = (scopes: string[]) => {
    hoisted.grants = {[DEVELOPMENT]: scopes, [PRODUCTION]: scopes};
};

const renderInProduction = (ui: React.ReactElement) =>
    render(
        <TooltipProvider>
            <ul>{ui}</ul>
        </TooltipProvider>
    );

const integrationInstance = {
    connectedUserId: 5,
    credentialStatus: 'VALID',
    enabled: true,
    id: 9,
    integrationId: 1,
    integrationInstanceConfigurationId: 3,
    integrationVersion: 1,
} as never;

const renderIntegrationListItem = () =>
    renderInProduction(
        <ConnectedUserSheetPanelIntegrationListItem
            componentDefinition={{icon: 'x', name: 'gmail', title: 'Gmail'} as never}
            componentDefinitions={[]}
            connectedUserId={5}
            connectedUserIntegrationInstance={integrationInstance}
        />
    );

const projectWorkflow = {
    connectedUserId: '5',
    enabled: true,
    id: '12',
    lastExecutionDate: null,
    projectId: '0',
    staticWebhookUrl: null,
    workflow: {id: 'wf', label: 'Agent', triggers: []},
    workflowUuid: 'wf',
    workflowVersion: 1,
} as unknown as ConnectedUserProjectWorkflowType;

const mcpServer = {
    enabled: true,
    id: '4',
    lastModifiedDate: null,
    name: 'Sales MCP',
    tools: [],
    workflows: [],
} as never;

describe('connected user sheet scopes', () => {
    beforeEach(() => {
        hoisted.grants = {};

        act(() => {
            environmentStore.setState({currentEnvironmentId: PRODUCTION});
        });
    });

    afterEach(() => {
        act(() => {
            environmentStore.setState({currentEnvironmentId: DEVELOPMENT});
        });
    });

    it('lets only CONNECTED_USER_MANAGE toggle an integration instance and only CONNECTED_USER_DELETE delete it', async () => {
        grant(['CONNECTED_USER_VIEW']);

        const deniedRender = renderIntegrationListItem();

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();

        deniedRender.unmount();

        grant(['CONNECTED_USER_DELETE', 'CONNECTED_USER_MANAGE']);

        renderIntegrationListItem();

        expect(screen.getByRole('switch')).toBeEnabled();

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(await screen.findByText('Delete')).toBeInTheDocument();
    });

    it('lets only CONNECTED_USER_MANAGE toggle an instance workflow', () => {
        const renderRow = () =>
            renderInProduction(
                <ConnectedUserSheetPanelIntegrationWorkflowListItem
                    componentDefinitions={[]}
                    integrationInstance={{connectedUserId: 5, id: 9} as never}
                    integrationInstanceConfigurationWorkflow={{enabled: true} as never}
                    integrationInstanceWorkflow={{enabled: true} as never}
                    workflow={{id: 'wf', label: 'Flow'} as never}
                />
            );

        grant(['CONNECTED_USER_VIEW']);

        const deniedRender = renderRow();

        expect(screen.getByRole('switch')).toBeDisabled();

        deniedRender.unmount();

        grant(['CONNECTED_USER_MANAGE']);

        renderRow();

        expect(screen.getByRole('switch')).toBeEnabled();
    });

    it('lets only CONNECTED_USER_MANAGE toggle and CONNECTED_USER_DELETE delete a project workflow', async () => {
        grant(['CONNECTED_USER_VIEW']);

        const deniedRender = renderInProduction(
            <ConnectedUserProjectWorkflowListItem connectedUserProjectWorkflow={projectWorkflow} />
        );

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();

        deniedRender.unmount();

        grant(['CONNECTED_USER_DELETE', 'CONNECTED_USER_MANAGE']);

        renderInProduction(<ConnectedUserProjectWorkflowListItem connectedUserProjectWorkflow={projectWorkflow} />);

        expect(screen.getByRole('switch')).toBeEnabled();

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(await screen.findByText('Delete')).toBeInTheDocument();
    });

    it('lets only MCP_EDIT toggle and MCP_DELETE delete a connected user MCP server', async () => {
        const renderServer = () =>
            renderInProduction(
                <ConnectedUserMcpServerListItem
                    connectedUserId={5}
                    connectedUserIntegrationInstances={[]}
                    mcpServer={mcpServer}
                />
            );

        grant(['MCP_VIEW']);

        const deniedRender = renderServer();

        expect(screen.getByRole('switch')).toBeDisabled();
        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();

        deniedRender.unmount();

        grant(['MCP_DELETE', 'MCP_EDIT']);

        renderServer();

        expect(screen.getByRole('switch')).toBeEnabled();

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));

        expect(await screen.findByText('Delete')).toBeInTheDocument();
    });

    it('lets only MCP_EDIT toggle a connected user MCP tool', () => {
        const renderTool = () =>
            renderInProduction(
                <ConnectedUserMcpServerListItemToolRow
                    tool={{componentName: 'gmail', componentVersion: 1, enabled: true, id: '8', name: 'send'} as never}
                />
            );

        grant(['MCP_VIEW']);

        const deniedRender = renderTool();

        expect(screen.getByRole('switch')).toBeDisabled();

        deniedRender.unmount();

        grant(['MCP_EDIT']);

        renderTool();

        expect(screen.getByRole('switch')).toBeEnabled();
    });

    it('lets only CONNECTED_USER_MANAGE toggle a connected user MCP workflow', () => {
        const renderWorkflow = () =>
            renderInProduction(
                <ConnectedUserMcpServerListItemWorkflowRow
                    connectedUserId={5}
                    workflow={
                        {
                            enabled: true,
                            integrationInstanceId: '9',
                            lastExecutionDate: null,
                            name: 'Flow',
                            workflowId: 'wf',
                        } as never
                    }
                />
            );

        grant(['CONNECTED_USER_VIEW']);

        const deniedRender = renderWorkflow();

        expect(screen.getByRole('switch')).toBeDisabled();

        deniedRender.unmount();

        grant(['CONNECTED_USER_MANAGE']);

        renderWorkflow();

        expect(screen.getByRole('switch')).toBeEnabled();
    });
});
