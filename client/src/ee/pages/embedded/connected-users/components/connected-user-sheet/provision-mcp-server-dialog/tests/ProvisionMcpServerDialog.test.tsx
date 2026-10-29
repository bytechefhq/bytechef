import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import ProvisionMcpServerDialog from '../ProvisionMcpServerDialog';

const hoisted = vi.hoisted(() => ({
    mutate: vi.fn(),
    mutationOptions: {} as {onSuccess?: () => void},
    provisionableQuery: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useProvisionConnectedUserMcpServerMutation: (options: {onSuccess?: () => void}) => {
        hoisted.mutationOptions = options;

        return {isPending: false, mutate: hoisted.mutate};
    },
    useProvisionableConnectedUserMcpServersQuery: hoisted.provisionableQuery,
}));

const MCP_SERVERS = [
    {
        coveredComponentNames: ['Slack', 'Jira'],
        id: '3',
        missingComponentNames: [],
        name: 'Full server',
        provisioned: false,
    },
    {
        coveredComponentNames: ['Slack'],
        id: '4',
        missingComponentNames: ['Jira', 'Asana'],
        name: 'Partial server',
        provisioned: false,
    },
    {
        coveredComponentNames: [],
        id: '5',
        missingComponentNames: ['Jira', 'Asana'],
        name: 'Uncovered server',
        provisioned: false,
    },
    {
        coveredComponentNames: ['Slack'],
        id: '6',
        missingComponentNames: [],
        name: 'Existing server',
        provisioned: true,
    },
    {
        coveredComponentNames: ['Jira'],
        id: '8',
        missingComponentNames: [],
        name: 'Last server',
        provisioned: false,
    },
];

const renderDialog = (onClose = vi.fn(), queryClient = new QueryClient()) => {
    render(
        <QueryClientProvider client={queryClient}>
            <ProvisionMcpServerDialog connectedUserId={7} onClose={onClose} />
        </QueryClientProvider>
    );

    return {onClose, queryClient};
};

describe('ProvisionMcpServerDialog', () => {
    beforeEach(() => {
        hoisted.mutate.mockReset();
        hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserMcpServers: MCP_SERVERS}});
    });

    it('shows the empty state with only a Close button when no MCP server is defined', () => {
        hoisted.provisionableQuery.mockReturnValue({data: {provisionableConnectedUserMcpServers: []}});

        renderDialog();

        expect(screen.getByText('No MCP servers are defined.')).toBeInTheDocument();
        expect(screen.getAllByRole('button', {name: 'Close'}).some((button) => button.textContent === 'Close')).toBe(
            true
        );
        expect(screen.queryByRole('button', {name: 'Save'})).not.toBeInTheDocument();
        expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    });

    it('describes covered, partially covered and uncovered servers', () => {
        renderDialog();

        expect(
            screen.getByText('Expose an MCP server through the integrations this connected user already has.')
        ).toBeInTheDocument();
        expect(screen.getByText('Uses Slack, Jira')).toBeInTheDocument();
        expect(
            screen.getByText('Uses Slack. Missing Jira, Asana: add those integrations first to expose them.')
        ).toBeInTheDocument();
        expect(screen.getByText('Add Jira, Asana on the Integrations tab first.')).toBeInTheDocument();
        expect(screen.getByRole('radio', {name: /Full server/})).toBeEnabled();
        expect(screen.getByRole('radio', {name: /Partial server/})).toBeEnabled();
        expect(screen.getByRole('radio', {name: /Uncovered server/})).toBeDisabled();
    });

    it('lists provisioned servers last with a badge and keeps them selectable', () => {
        renderDialog();

        expect(screen.getByText('Provisioned')).toBeInTheDocument();
        expect(screen.getByRole('radio', {name: /Existing server/})).toBeEnabled();
        expect(screen.getAllByRole('radio').map((radio) => radio.getAttribute('value'))).toEqual([
            '3',
            '4',
            '5',
            '8',
            '6',
        ]);
    });

    it('keeps Save disabled until a selectable server is chosen', async () => {
        renderDialog();

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.click(screen.getByRole('radio', {name: /Uncovered server/}));

        expect(screen.getByRole('button', {name: 'Save'})).toBeDisabled();

        await userEvent.click(screen.getByRole('radio', {name: /Full server/}));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());
    });

    it('submits the chosen server, invalidates the caches and closes', async () => {
        const {onClose, queryClient} = renderDialog();

        const invalidateQueries = vi.spyOn(queryClient, 'invalidateQueries');

        await userEvent.click(screen.getByRole('radio', {name: /Full server/}));

        await waitFor(() => expect(screen.getByRole('button', {name: 'Save'})).toBeEnabled());

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(hoisted.mutate).toHaveBeenCalledWith({connectedUserId: '7', mcpServerId: '3'});

        hoisted.mutationOptions.onSuccess?.();

        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUserMcpServers']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['provisionableConnectedUserMcpServers']});
        expect(invalidateQueries).toHaveBeenCalledWith({queryKey: ['connectedUsers', 7]});
        expect(onClose).toHaveBeenCalled();
    });
});
