import {A2aServer} from '@/shared/middleware/graphql';
import {QueryClient, QueryClientProvider} from '@tanstack/react-query';
import {render, screen, waitFor} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {beforeEach, describe, expect, it, vi} from 'vitest';

import A2aServerDialog from '../A2aServerDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/middleware/graphql', async (importOriginal) => ({
    ...(await importOriginal<typeof import('@/shared/middleware/graphql')>()),
    useCreateA2aServerMutation: () => ({mutate: hoisted.createMutate}),
    useUpdateA2aServerMutation: () => ({mutate: hoisted.updateMutate}),
}));

vi.mock('@/shared/stores/useEnvironmentStore', () => {
    const state = {currentEnvironmentId: 2};

    return {useEnvironmentStore: (selector: (currentState: typeof state) => unknown) => selector(state)};
});

const renderDialog = (a2aServer?: A2aServer) =>
    render(
        <QueryClientProvider client={new QueryClient()}>
            <A2aServerDialog
                a2aServer={a2aServer}
                onOpenChange={vi.fn()}
                open
                triggerNode={<button type="button">open</button>}
            />
        </QueryClientProvider>
    );

const getNameInput = () => screen.getByPlaceholderText('Enter server name');

const existingA2aServer = {
    authenticationRequired: true,
    description: 'Answers sales questions',
    enabled: true,
    id: '7',
    name: 'Sales agent',
} as A2aServer;

describe('A2aServerDialog', () => {
    beforeEach(() => {
        hoisted.createMutate.mockReset();
        hoisted.updateMutate.mockReset();
    });

    it('keeps the edited values while the update is pending', async () => {
        const user = userEvent.setup();

        renderDialog(existingA2aServer);

        await user.clear(getNameInput());
        await user.type(getNameInput(), 'Support agent');
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(getNameInput()).toHaveValue('Support agent');

        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(2));

        expect(hoisted.updateMutate.mock.calls[1][0]).toEqual({
            id: '7',
            input: {
                authenticationRequired: true,
                description: 'Answers sales questions',
                enabled: true,
                name: 'Support agent',
            },
        });
    });

    it('keeps the saved values after a successful update', async () => {
        const user = userEvent.setup();

        hoisted.updateMutate.mockImplementation((_variables, options) => options.onSuccess());

        renderDialog(existingA2aServer);

        await user.clear(getNameInput());
        await user.type(getNameInput(), 'Support agent');
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.updateMutate).toHaveBeenCalledTimes(1));

        expect(getNameInput()).toHaveValue('Support agent');
    });

    it('can create a second server after the first one is saved', async () => {
        const user = userEvent.setup();

        hoisted.createMutate.mockImplementation((_variables, options) => options.onSuccess());

        renderDialog();

        await user.type(getNameInput(), 'First agent');
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(1));

        expect(getNameInput()).toHaveValue('');

        await user.type(getNameInput(), 'Second agent');
        await user.click(screen.getByRole('button', {name: 'Save'}));

        await waitFor(() => expect(hoisted.createMutate).toHaveBeenCalledTimes(2));

        expect(hoisted.createMutate.mock.calls[1][0]).toEqual({
            input: {
                authenticationRequired: true,
                description: '',
                environmentId: '2',
                name: 'Second agent',
            },
        });
    });
});
