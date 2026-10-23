import {ConnectedUserKeys} from '@/ee/shared/queries/embedded/connectedUsers.queries';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserEditDialog from '../ConnectedUserEditDialog';

const hoisted = vi.hoisted(() => ({
    getConnectedUserMock: vi.fn(),
    updateConnectedUserMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/connected-user', () => ({
    ConnectedUserApi: class {
        getConnectedUser = hoisted.getConnectedUserMock;
        updateConnectedUser = hoisted.updateConnectedUserMock;
    },
    ResponseError: class ResponseError extends Error {
        response: {status: number};

        constructor(response: {status: number}) {
            super('response error');

            this.response = response;
        }
    },
}));

vi.mock('@/ee/shared/middleware/embedded/connected-user/apis/ConnectedUserApi', () => ({
    ConnectedUserApi: class {
        getConnectedUser = hoisted.getConnectedUserMock;
    },
}));

const connectedUser = {
    email: 'jane@example.com',
    externalId: 'user-1',
    id: 5,
    metadata: {plan: 'pro', region: 'eu'},
    name: 'Jane',
    version: 3,
};

beforeEach(() => {
    windowResizeObserver();

    hoisted.updateConnectedUserMock.mockResolvedValue({...connectedUser, version: 4});
});

afterEach(() => {
    resetAll();
});

describe('ConnectedUserEditDialog', () => {
    it('pre-fills the values, shows the external ID read-only and sends replaced metadata with the version', async () => {
        const onClose = vi.fn();

        render(<ConnectedUserEditDialog connectedUser={connectedUser} onClose={onClose} />);

        expect(screen.getByLabelText('Name')).toHaveValue('Jane');
        expect(screen.getByLabelText('External ID')).toHaveValue('user-1');
        expect(screen.getByLabelText('External ID')).toHaveAttribute('readonly');

        await userEvent.click(screen.getByRole('button', {name: 'Remove metadata 2'}));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateConnectedUserMock).toHaveBeenCalledWith({
                id: 5,
                updateConnectedUserRequest: {
                    email: 'jane@example.com',
                    metadata: {plan: 'pro'},
                    name: 'Jane',
                    version: 3,
                },
            })
        );

        await waitFor(() => expect(onClose).toHaveBeenCalled());
    });

    it('blocks submit when an edited metadata key duplicates another or is cleared while its value stays', async () => {
        render(<ConnectedUserEditDialog connectedUser={connectedUser} onClose={vi.fn()} />);

        await userEvent.clear(screen.getByLabelText('Metadata key 2'));
        await userEvent.type(screen.getByLabelText('Metadata key 2'), 'plan');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Duplicate key')).toBeInTheDocument();

        await userEvent.clear(screen.getByLabelText('Metadata key 2'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(await screen.findByText('Key is required')).toBeInTheDocument();
        expect(hoisted.updateConnectedUserMock).not.toHaveBeenCalled();
    });

    it('sends an empty name when it is cleared', async () => {
        render(<ConnectedUserEditDialog connectedUser={connectedUser} onClose={vi.fn()} />);

        await userEvent.clear(screen.getByLabelText('Name'));
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateConnectedUserMock).toHaveBeenCalledWith(
                expect.objectContaining({
                    updateConnectedUserRequest: expect.objectContaining({name: ''}),
                })
            )
        );
    });

    it('shows the conflict message on a 409 and resets the form from refetched data on Reload', async () => {
        const {ResponseError} = await import('@/ee/shared/middleware/embedded/connected-user');

        hoisted.updateConnectedUserMock.mockRejectedValue(new ResponseError({status: 409} as Response));
        hoisted.getConnectedUserMock.mockResolvedValue({...connectedUser, name: 'Janet', version: 4});

        render(<ConnectedUserEditDialog connectedUser={connectedUser} onClose={vi.fn()} />);

        await userEvent.type(screen.getByLabelText('Name'), 'XYZ');
        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        expect(
            await screen.findByText('This connected user changed since you opened it. Reload to see the latest values.')
        ).toBeInTheDocument();

        await userEvent.click(screen.getByRole('button', {name: 'Reload'}));

        await waitFor(() => expect(screen.getByLabelText('Name')).toHaveValue('Janet'));

        expect(
            screen.queryByText('This connected user changed since you opened it. Reload to see the latest values.')
        ).not.toBeInTheDocument();

        hoisted.updateConnectedUserMock.mockResolvedValue({});

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateConnectedUserMock).toHaveBeenLastCalledWith(
                expect.objectContaining({
                    updateConnectedUserRequest: expect.objectContaining({name: 'Janet', version: 4}),
                })
            )
        );
    });

    it('submits the version of the values shown, not a version left in the query cache', async () => {
        const queryClient = new QueryClient();

        queryClient.setQueryData(ConnectedUserKeys.connectedUser(5), {...connectedUser, version: 9});

        render(
            <QueryClientProvider client={queryClient}>
                <ConnectedUserEditDialog connectedUser={connectedUser} onClose={vi.fn()} />
            </QueryClientProvider>
        );

        await userEvent.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() =>
            expect(hoisted.updateConnectedUserMock).toHaveBeenCalledWith(
                expect.objectContaining({
                    updateConnectedUserRequest: expect.objectContaining({version: 3}),
                })
            )
        );
    });
});
