import {A2aServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerListItem from '../A2aServerListItem';

const hoisted = vi.hoisted(() => ({
    deleteMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useDeleteA2aServerMutation: () => ({mutate: hoisted.deleteMutate}),
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
} as A2aServer;

const renderListItem = (listedA2aServer: A2aServer = a2aServer) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <A2aServerListItem a2aServer={listedA2aServer} />
        </QueryClientProvider>
    );

describe('A2aServerListItem', () => {
    beforeEach(() => {
        hoisted.deleteMutate.mockReset();
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

        await user.click(screen.getByRole('button'));
        await user.click(await screen.findByRole('menuitem', {name: 'Delete'}));
        await user.click(await screen.findByRole('button', {name: 'Cancel'}));

        expect(hoisted.deleteMutate).not.toHaveBeenCalled();
    });

    it('offers to add a project and lists the projects of the server', async () => {
        const user = userEvent.setup();

        renderListItem();

        expect(screen.getByText('Project list')).toBeInTheDocument();

        await user.click(screen.getByRole('button'));

        expect(await screen.findByRole('menuitem', {name: 'Add Project'})).toBeInTheDocument();
        expect(screen.queryByRole('menuitem', {name: 'Manage Skills'})).not.toBeInTheDocument();
    });

    it('shows the agent card URL when the server has a secret key', () => {
        renderListItem();

        expect(
            screen.getByText(`${window.location.origin}/api/automation/a2a/server-secret/.well-known/agent-card.json`)
        ).toBeInTheDocument();
    });

    it('hides the agent card URL when the server has no secret key', () => {
        renderListItem({...a2aServer, secretKey: null});

        expect(screen.queryByText(/agent-card\.json/)).not.toBeInTheDocument();
    });
});
