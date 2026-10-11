import {TooltipProvider} from '@/components/ui/tooltip';
import {A2aServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerListItem from '../A2aServerListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
    tagsMutationOptions: undefined as {onSuccess?: () => void} | undefined,
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
    useUpdateA2aServerTagsMutation: (options?: {onSuccess?: () => void}) => {
        hoisted.tagsMutationOptions = options;

        return {isPending: false, mutate: vi.fn()};
    },
}));

vi.mock('../A2aServerDialog', () => ({
    default: ({open}: {open?: boolean}) => (open ? <div>Editing the A2A server</div> : null),
}));

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

const renderListItem = (listedA2aServer: A2aServer = a2aServer, queryClient: QueryClient = new QueryClient()) =>
    render(
        <QueryClientProvider client={queryClient}>
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

    it('refreshes the servers after the enabled toggle is saved', async () => {
        const user = userEvent.setup();
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.updateMutate.mockImplementation((_variables, options) => options.onSuccess());

        renderListItem(a2aServer, queryClient);

        await user.click(screen.getByRole('switch', {name: 'Enabled'}));

        await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['a2aServers']}));
    });

    it('refreshes the servers and closes the confirmation after the server is deleted', async () => {
        const user = userEvent.setup();
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        hoisted.deleteMutate.mockImplementation((_variables, options) => options.onSuccess());

        renderListItem(a2aServer, queryClient);

        await user.click(screen.getByRole('button', {name: 'Server actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete'}));
        await user.click(await screen.findByRole('button', {name: 'Delete'}));

        await waitFor(() => expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['a2aServers']}));

        await waitFor(() => expect(screen.queryByText('Are you absolutely sure?')).not.toBeInTheDocument());
    });

    it('refreshes the servers and their tags after the tags are saved', () => {
        const queryClient = new QueryClient();
        const invalidateQueriesSpy = vi.spyOn(queryClient, 'invalidateQueries');

        renderListItem(a2aServer, queryClient);

        hoisted.tagsMutationOptions?.onSuccess?.();

        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['a2aServers']});
        expect(invalidateQueriesSpy).toHaveBeenCalledWith({queryKey: ['a2aServerTags']});
    });

    it('opens the edit dialog from the server actions', async () => {
        const user = userEvent.setup();

        renderListItem();

        await user.click(screen.getByRole('button', {name: 'Server actions'}));
        await user.click(await screen.findByRole('menuitem', {name: 'Edit'}));

        expect(await screen.findByText('Editing the A2A server')).toBeInTheDocument();
    });
});
