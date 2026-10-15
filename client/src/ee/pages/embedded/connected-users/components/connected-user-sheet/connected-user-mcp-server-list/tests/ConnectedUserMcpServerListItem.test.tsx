import {TooltipProvider} from '@/components/ui/tooltip';
import {ConnectedUserIntegrationInstance} from '@/ee/shared/middleware/embedded/connected-user';
import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {ConnectedUserMcpServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, within} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserMcpServerListItem from '../ConnectedUserMcpServerListItem';

interface MutationOptionsI {
    onSuccess?: () => void;
}

const hoisted = vi.hoisted(() => ({
    deleteServerMutate: vi.fn(),
    deleteServerOptions: {} as {onSuccess?: () => void},
    enableServerMutate: vi.fn(),
    enableServerOptions: {} as {onSuccess?: () => void},
    enableToolMutate: vi.fn(),
    enableToolOptions: {} as {onSuccess?: () => void},
    enableWorkflowMutate: vi.fn(),
    enableWorkflowOptions: {} as {onSuccess?: () => void},
    isTenantAdmin: true,
}));

vi.mock('@/shared/hooks/useIsTenantAdmin', () => ({
    useIsTenantAdmin: () => hoisted.isTenantAdmin,
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<object>()),
    useDeleteConnectedUserMcpServerMutation: (options: MutationOptionsI) => {
        hoisted.deleteServerOptions = options;

        return {isPending: false, mutate: hoisted.deleteServerMutate};
    },
    useEnableConnectedUserMcpServerMutation: (options: MutationOptionsI) => {
        hoisted.enableServerOptions = options;

        return {isPending: false, mutate: hoisted.enableServerMutate};
    },
    useEnableConnectedUserMcpToolMutation: (options: MutationOptionsI) => {
        hoisted.enableToolOptions = options;

        return {isPending: false, mutate: hoisted.enableToolMutate};
    },
}));

vi.mock('@/ee/shared/mutations/embedded/integrationInstanceWorkflows.mutations', () => ({
    useEnableIntegrationInstanceWorkflowMutation: (options: MutationOptionsI) => {
        hoisted.enableWorkflowOptions = options;

        return {isPending: false, mutate: hoisted.enableWorkflowMutate};
    },
}));

vi.mock('@/ee/shared/queries/embedded/componentDefinitions.queries', () => ({
    useGetComponentDefinitionsQuery: () => ({data: [{name: 'gmail', title: 'Gmail'}]}),
}));

vi.mock('@/shared/queries/platform/componentDefinitions.queries', () => ({
    useGetComponentDefinitionQuery: () => ({
        data: {
            clusterElements: [
                {
                    description: "Lists the email messages in the user's mailbox.",
                    name: 'searchEmail',
                    title: 'Search Email',
                    type: 'TOOLS',
                },
            ],
        },
    }),
}));

const connectedUserIntegrationInstances = [{credentialStatus: 'VALID', id: 10}] as ConnectedUserIntegrationInstance[];

const toolsOnlyMcpServer = {
    enabled: true,
    environmentId: '0',
    id: '1',
    lastModifiedDate: null,
    name: 'Google',
    tools: [
        {
            componentName: 'gmail',
            componentVersion: 1,
            enabled: true,
            id: '100',
            integrationInstanceId: '10',
            name: 'searchEmail',
        },
        {
            componentName: 'gmail',
            componentVersion: 1,
            enabled: false,
            id: '101',
            integrationInstanceId: '10',
            name: 'getEmail',
        },
    ],
    workflows: [],
} as ConnectedUserMcpServer;

const mixedMcpServer = {
    ...toolsOnlyMcpServer,
    workflows: [
        {
            componentName: 'gmail',
            description: 'Sends an email',
            enabled: true,
            integrationInstanceId: '10',
            integrationVersion: 6,
            lastExecutionDate: '2026-10-05T10:00:00Z',
            name: 'Send Email',
            workflowId: 'workflow-1',
        },
        {
            componentName: 'gmail',
            description: null,
            enabled: false,
            integrationInstanceId: '10',
            integrationVersion: 6,
            lastExecutionDate: null,
            name: 'Archive Email',
            workflowId: 'workflow-2',
        },
    ],
} as ConnectedUserMcpServer;

const renderListItem = (mcpServer: ConnectedUserMcpServer, queryClient = new QueryClient()) =>
    render(
        <QueryClientProvider client={queryClient}>
            <TooltipProvider>
                <ConnectedUserMcpServerListItem
                    connectedUserId={5}
                    connectedUserIntegrationInstances={connectedUserIntegrationInstances}
                    mcpServer={mcpServer}
                />
            </TooltipProvider>
        </QueryClientProvider>
    );

const expandCard = async () => userEvent.click(screen.getByText('Google'));

const expandGroups = async () => {
    for (const showToolsButton of screen.getAllByRole('button', {name: 'Show tools'})) {
        await userEvent.click(showToolsButton);
    }
};

describe('ConnectedUserMcpServerListItem', () => {
    beforeEach(() => {
        vi.clearAllMocks();

        hoisted.isTenantAdmin = true;
    });

    it('counts component and workflow tools in the card header', () => {
        renderListItem(mixedMcpServer);

        expect(screen.getByText('2 component tools')).toBeInTheDocument();
        expect(screen.getByText('2 workflow tools')).toBeInTheDocument();
    });

    it('groups tools by component, collapsed, with version badges and credential status', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();

        expect(screen.getByText('Component Tools')).toBeInTheDocument();
        expect(screen.getByText('Workflow Tools')).toBeInTheDocument();
        expect(screen.getByText('v1')).toBeInTheDocument();
        expect(screen.getByText('v6')).toBeInTheDocument();
        expect(screen.getAllByText('Account Connected')).toHaveLength(2);
        expect(screen.queryByText('Search Email')).not.toBeInTheDocument();
    });

    it('shows tool titles and descriptions and toggles a tool for the user', async () => {
        renderListItem(toolsOnlyMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByText('Search Email')).toBeInTheDocument();
        expect(screen.getByText("Lists the email messages in the user's mailbox.")).toBeInTheDocument();
        expect(screen.getByText('getEmail')).toBeInTheDocument();

        const getEmailRow = screen.getByText('getEmail').closest('li')!;

        await userEvent.click(within(getEmailRow).getByRole('switch'));

        expect(hoisted.enableToolMutate).toHaveBeenCalledWith({enable: true, id: '101'});
    });

    it('shows workflow descriptions and last executions and toggles a workflow through the integration instance', async () => {
        renderListItem(mixedMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByText('Sends an email')).toBeInTheDocument();
        expect(screen.getByText(/^Executed at /)).toBeInTheDocument();
        expect(screen.getByText('No executions')).toBeInTheDocument();

        const archiveEmailRow = screen.getByText('Archive Email').closest('li')!;

        await userEvent.click(within(archiveEmailRow).getByRole('switch'));

        expect(hoisted.enableWorkflowMutate).toHaveBeenCalledWith({enable: true, id: 10, workflowId: 'workflow-2'});
    });

    it('offers Delete only for servers without workflow tools', async () => {
        const {unmount} = renderListItem(mixedMcpServer);

        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();

        unmount();

        renderListItem(toolsOnlyMcpServer);

        expect(screen.getByRole('button', {name: 'More actions'})).toBeInTheDocument();
    });

    it('toggles the whole server and refreshes the Integrations tab data', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(mixedMcpServer, queryClient);

        await userEvent.click(screen.getAllByRole('switch')[0]);

        expect(hoisted.enableServerMutate).toHaveBeenCalledWith({
            connectedUserId: '5',
            enable: false,
            mcpServerId: '1',
        });

        hoisted.enableServerOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ConnectedUserKeys.connectedUser(5)});
    });

    it('deletes the server for the user after confirmation and refreshes the list', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(toolsOnlyMcpServer, queryClient);

        await userEvent.click(screen.getByRole('button', {name: 'More actions'}));
        await userEvent.click(await screen.findByRole('menuitem', {name: 'Delete'}));
        await userEvent.click(await screen.findByRole('button', {name: 'Delete'}));

        expect(hoisted.deleteServerMutate).toHaveBeenCalledWith({connectedUserId: '5', mcpServerId: '1'});

        hoisted.deleteServerOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
    });

    it('refreshes the MCP servers and Integrations data after a tool or workflow toggle', async () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(mixedMcpServer, queryClient);

        await expandCard();
        await expandGroups();

        hoisted.enableToolOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});

        invalidateQueriesSpy.mockClear();

        hoisted.enableWorkflowOptions.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ConnectedUserKeys.connectedUser(5)});
    });

    it('shows the server and its component tools read-only to a user who is not a tenant admin', async () => {
        hoisted.isTenantAdmin = false;

        renderListItem(toolsOnlyMcpServer);

        await expandCard();
        await expandGroups();

        expect(screen.getByText('getEmail')).toBeInTheDocument();
        expect(screen.getAllByRole('switch')).toHaveLength(3);
        screen.getAllByRole('switch').forEach((switchElement) => expect(switchElement).toBeDisabled());
        expect(screen.queryByRole('button', {name: 'More actions'})).not.toBeInTheDocument();
    });
});
