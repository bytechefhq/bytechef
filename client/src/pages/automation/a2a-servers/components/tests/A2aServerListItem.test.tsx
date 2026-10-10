import {TooltipProvider} from '@/components/ui/tooltip';
import {A2aServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerListItem from '../A2aServerListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useA2aProjectsByServerIdQuery: () => ({
        data: {
            a2aProjectsByServerId: [
                {id: '1', projectDeploymentId: '4', projectId: '3', projectVersion: 1, workflowIds: ['a', 'b']},
            ],
        },
        isError: false,
        isLoading: false,
    }),
    useDeleteA2aServerMutation: () => ({mutate: hoisted.deleteMutate}),
    useUpdateA2aServerMutation: () => ({isPending: false, mutate: hoisted.updateMutate}),
    useUpdateA2aServerTagsMutation: () => ({isPending: false, mutate: vi.fn()}),
}));

vi.mock('../A2aServerDialog', () => ({default: () => null}));

vi.mock('../A2aServerWorkflowDialog', () => ({default: () => null}));

vi.mock('../a2a-project-list/A2aProjectList', () => ({default: () => <div>Project list</div>}));

const a2aServer = {
    authenticationRequired: true,
    enabled: true,
    environmentId: '2',
    id: '7',
    name: 'Sales agent',
    secretKey: 'server-secret',
    tags: [{id: '3', name: 'sales'}],
} as A2aServer;

const expandListItem = (user: ReturnType<typeof userEvent.setup>) =>
    user.click(screen.getByRole('button', {name: /2 workflow skills/}));

const renderListItem = (listedA2aServer: A2aServer = a2aServer) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <TooltipProvider>
                <A2aServerListItem a2aServer={listedA2aServer} />
            </TooltipProvider>
        </QueryClientProvider>
    );

describe('A2aServerListItem', () => {
    beforeEach(() => {
        hoisted.deleteMutate.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('disables the server from the enabled toggle', async () => {
        const user = userEvent.setup();

        renderListItem();

        await user.click(screen.getByRole('switch', {name: 'Enabled'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.updateMutate.mock.calls[0][0]).toEqual({id: '7', input: {enabled: false}});
    });

    it('asks for confirmation before deleting the server', async () => {
        const user = userEvent.setup();

        renderListItem();

        await user.click(screen.getByRole('button', {name: 'Server actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete'}));

        expect(await screen.findByText('Are you absolutely sure?')).toBeInTheDocument();
        expect(hoisted.deleteMutate).not.toHaveBeenCalled();

        await user.click(screen.getByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(hoisted.deleteMutate).toHaveBeenCalledTimes(1));

        expect(hoisted.deleteMutate.mock.calls[0][0]).toEqual({id: '7'});
    });

    it('does not delete the server when the confirmation is cancelled', async () => {
        const user = userEvent.setup();

        renderListItem();

        await user.click(screen.getByRole('button', {name: 'Server actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete'}));
        await user.click(await screen.findByRole('button', {name: 'Cancel'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });

    it('offers to add workflows and lists the projects of the server', async () => {
        const user = userEvent.setup();

        renderListItem();

        expect(screen.queryByText('Project list')).not.toBeInTheDocument();

        await expandListItem(user);

        expect(screen.getByText('Project list')).toBeInTheDocument();

        await user.click(screen.getByRole('button', {name: 'Server actions'}));

        expect(screen.queryByRole('menuitem', {name: 'Add Workflows'})).not.toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Manage Skills'})).not.toBeInTheDocument();

        await user.keyboard('{Escape}');

        expect(screen.getByRole('button', {name: 'Add Workflows'})).toBeInTheDocument();
    });

    it('shows the agent card and endpoint URLs in the Connect tab', async () => {
        const user = userEvent.setup();

        renderListItem();

        expect(screen.queryByText(/agent-card\.json/)).not.toBeInTheDocument();

        await expandListItem(user);
        await user.click(screen.getByRole('tab', {name: 'Connect'}));

        const endpointUrl = `${window.location.origin}/api/automation/a2a/server-secret`;

        expect(await screen.findByText(`${endpointUrl}/.well-known/agent-card.json`)).toBeInTheDocument();
        expect(screen.getByText(endpointUrl)).toBeInTheDocument();
    });

    it('explains the server has no URL when it has no secret key', async () => {
        const user = userEvent.setup();

        renderListItem({...a2aServer, secretKey: null});

        await expandListItem(user);
        await user.click(screen.getByRole('tab', {name: 'Connect'}));

        expect(await screen.findByText('This server has no URL yet.')).toBeInTheDocument();
        expect(screen.queryByText(/agent-card\.json/)).not.toBeInTheDocument();
    });

    it('shows the tags of the server', () => {
        renderListItem();

        expect(screen.getByText('sales')).toBeInTheDocument();
    });
});
