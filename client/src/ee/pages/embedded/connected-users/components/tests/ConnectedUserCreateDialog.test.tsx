import useConnectedUserSheetStore from '@/ee/pages/embedded/connected-users/stores/useConnectedUserSheetStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ConnectedUserCreateDialog from '../ConnectedUserCreateDialog';

const hoisted = vi.hoisted(() => ({
    createConnectedUserMock: vi.fn(),
}));

vi.mock('@/ee/shared/middleware/embedded/connected-user', () => ({
    ConnectedUserApi: class {
        createConnectedUser = hoisted.createConnectedUserMock;
    },
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => ({
    useEnvironmentStore: (selector: (state: {currentEnvironmentId: number}) => unknown) =>
        selector({currentEnvironmentId: 2}),
}));

beforeEach(() => {
    windowResizeObserver();

    hoisted.createConnectedUserMock.mockResolvedValue({id: 77});

    useConnectedUserSheetStore.setState({connectedUserId: 0, connectedUserSheetOpen: false});
});

afterEach(() => {
    resetAll();
});

describe('ConnectedUserCreateDialog', () => {
    it('submits the entered values, drops blank metadata rows and opens the new user sheet', async () => {
        const onClose = vi.fn();

        render(<ConnectedUserCreateDialog onClose={onClose} />);

        await userEvent.type(screen.getByLabelText(/External ID/), 'user-1');
        await userEvent.type(screen.getByLabelText('Name'), 'Jane');
        await userEvent.type(screen.getByLabelText('Email'), 'jane@example.com');

        await userEvent.click(screen.getByRole('button', {name: 'Add metadata'}));
        await userEvent.type(screen.getByLabelText('Metadata key 1'), 'plan');
        await userEvent.type(screen.getByLabelText('Metadata value 1'), 'pro');

        await userEvent.click(screen.getByRole('button', {name: 'Add metadata'}));

        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        await waitFor(() =>
            expect(hoisted.createConnectedUserMock).toHaveBeenCalledWith({
                createConnectedUserRequest: {
                    email: 'jane@example.com',
                    environmentId: 2,
                    externalId: 'user-1',
                    metadata: {plan: 'pro'},
                    name: 'Jane',
                },
            })
        );

        await waitFor(() => expect(onClose).toHaveBeenCalled());

        expect(useConnectedUserSheetStore.getState().connectedUserId).toBe(77);
        expect(useConnectedUserSheetStore.getState().connectedUserSheetOpen).toBe(true);
    });

    it('blocks submit when a metadata row has a value but no key', async () => {
        render(<ConnectedUserCreateDialog onClose={vi.fn()} />);

        await userEvent.type(screen.getByLabelText(/External ID/), 'user-1');

        await userEvent.click(screen.getByRole('button', {name: 'Add metadata'}));
        await userEvent.type(screen.getByLabelText('Metadata value 1'), 'pro');

        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(await screen.findByText('Key is required')).toBeInTheDocument();
        expect(hoisted.createConnectedUserMock).not.toHaveBeenCalled();

        await userEvent.type(screen.getByLabelText('Metadata key 1'), 'plan');

        await waitFor(() => expect(screen.queryByText('Key is required')).not.toBeInTheDocument());
    });

    it('blocks submit on a duplicate metadata key and keeps keys verbatim', async () => {
        render(<ConnectedUserCreateDialog onClose={vi.fn()} />);

        await userEvent.type(screen.getByLabelText(/External ID/), 'user-1');

        await userEvent.click(screen.getByRole('button', {name: 'Add metadata'}));
        await userEvent.type(screen.getByLabelText('Metadata key 1'), 'plan');
        await userEvent.type(screen.getByLabelText('Metadata value 1'), 'pro');

        await userEvent.click(screen.getByRole('button', {name: 'Add metadata'}));
        await userEvent.type(screen.getByLabelText('Metadata key 2'), 'plan');
        await userEvent.type(screen.getByLabelText('Metadata value 2'), 'free');

        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(await screen.findByText('Duplicate key')).toBeInTheDocument();
        expect(hoisted.createConnectedUserMock).not.toHaveBeenCalled();

        await userEvent.type(screen.getByLabelText('Metadata key 2'), ' ');

        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        await waitFor(() =>
            expect(hoisted.createConnectedUserMock).toHaveBeenCalledWith({
                createConnectedUserRequest: expect.objectContaining({metadata: {plan: 'pro', 'plan ': 'free'}}),
            })
        );
    });

    it('does not submit without an external ID', async () => {
        render(<ConnectedUserCreateDialog onClose={vi.fn()} />);

        await userEvent.click(screen.getByRole('button', {name: 'Create'}));

        expect(hoisted.createConnectedUserMock).not.toHaveBeenCalled();
    });
});
