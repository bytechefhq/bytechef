import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserFilterNav from '../ConnectedUserFilterNav';

const hoisted = vi.hoisted(() => ({
    connectedUser: undefined as {externalId: string; id: number; name?: string} | undefined,
    connectedUsers: [] as {externalId: string; id: number; name?: string}[],
    useGetConnectedUsersQuery: vi.fn(),
}));

vi.mock('@/ee/shared/queries/embedded/connectedUsers.queries', () => ({
    useGetConnectedUserQuery: () => ({data: hoisted.connectedUser}),
    useGetConnectedUsersQuery: (request: unknown) => {
        hoisted.useGetConnectedUsersQuery(request);

        return {data: {content: hoisted.connectedUsers}, isLoading: false};
    },
}));

describe('ConnectedUserFilterNav', () => {
    beforeEach(() => {
        windowResizeObserver();

        hoisted.connectedUser = undefined;
        hoisted.connectedUsers = [
            {externalId: 'acme-1', id: 42, name: 'Acme Corp'},
            {externalId: 'user-without-name', id: 43},
        ];
    });

    afterEach(() => {
        resetAll();
    });

    it('searches connected users with the typed text', async () => {
        render(<ConnectedUserFilterNav environmentId={1} onSelect={vi.fn()} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));
        await userEvent.type(screen.getByPlaceholderText('Search...'), 'acme');

        await waitFor(
            () => {
                expect(hoisted.useGetConnectedUsersQuery).toHaveBeenCalledWith(
                    expect.objectContaining({environmentId: 1, search: 'acme'})
                );
            },
            {interval: 5, timeout: 5000}
        );
    });

    it('selects a connected user', async () => {
        const onSelect = vi.fn();

        render(<ConnectedUserFilterNav environmentId={1} onSelect={onSelect} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));
        await userEvent.click(screen.getByText('Acme Corp'));

        expect(onSelect).toHaveBeenCalledWith(42);
    });

    it('lists the connected users under a Connected Users group, below Admin connections', async () => {
        hoisted.connectedUser = {externalId: 'acme-1', id: 42, name: 'Acme Corp'};

        render(<ConnectedUserFilterNav environmentId={1} onSelect={vi.fn()} selectedConnectedUserId={42} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));

        const connectedUsersGroup = screen.getByRole('group', {name: 'Connected Users'});

        expect(connectedUsersGroup).toHaveTextContent('Acme Corp');
        expect(connectedUsersGroup).toHaveTextContent('user-without-name');
        expect(connectedUsersGroup).not.toHaveTextContent('Admin connections');
    });

    it('lists a connected user without a name by external id', async () => {
        render(<ConnectedUserFilterNav environmentId={1} onSelect={vi.fn()} />);

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));

        expect(screen.getByText('user-without-name')).toBeInTheDocument();
    });

    it('shows the selected connected user and clears it with the Admin connections entry', async () => {
        const onSelect = vi.fn();

        hoisted.connectedUser = {externalId: 'acme-1', id: 42, name: 'Acme Corp'};
        hoisted.connectedUsers = [];

        render(<ConnectedUserFilterNav environmentId={1} onSelect={onSelect} selectedConnectedUserId={42} />);

        expect(screen.getByRole('combobox', {name: 'Connected user'})).toHaveTextContent('Acme Corp');

        expect(screen.queryByRole('button', {name: 'Clear connected user'})).not.toBeInTheDocument();

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));
        await userEvent.click(screen.getByRole('option', {name: 'Admin connections'}));

        expect(onSelect).toHaveBeenCalledWith(undefined);
    });

    it('shows that admin connections are listed when no connected user is selected', async () => {
        render(<ConnectedUserFilterNav environmentId={1} onSelect={vi.fn()} />);

        expect(screen.getByRole('combobox', {name: 'Connected user'})).toHaveTextContent('Admin connections');

        await userEvent.click(screen.getByRole('combobox', {name: 'Connected user'}));

        expect(screen.queryByRole('option', {name: 'Admin connections'})).not.toBeInTheDocument();
    });
});
