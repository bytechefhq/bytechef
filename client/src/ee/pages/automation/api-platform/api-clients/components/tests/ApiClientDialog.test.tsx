import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import ApiClientDialog from '../ApiClientDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    revealSecretKey: undefined as ((result: {secretKey?: string}) => void) | undefined,
    updateMutate: vi.fn(),
}));

vi.mock('@/shared/mutations/platform/apiClients.mutations', () => ({
    // The dialog only reaches its reveal screen through the create mutation's onSuccess, so the mock hands that
    // callback back to the test instead of discarding it.
    useCreateApiClientMutation: (options: {onSuccess: (result: {secretKey?: string}) => void}) => {
        hoisted.revealSecretKey = options.onSuccess;

        return {mutate: hoisted.createMutate, reset: vi.fn()};
    },
    useUpdateApiClientMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/shared/queries/platform/apiClients.queries', () => ({
    ApiClientKeys: {apiClients: ['apiClients']},
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('ApiClientDialog', () => {
    describe('create mode', () => {
        it('should render the create title', () => {
            render(<ApiClientDialog onClose={onClose} />);

            expect(screen.getByRole('heading', {name: 'Create API Client'})).toBeInTheDocument();
        });

        it('should render an empty name field', () => {
            render(<ApiClientDialog onClose={onClose} />);

            expect(screen.getByLabelText('Name')).toHaveValue('');
        });

        it('should create the client on save', async () => {
            const user = userEvent.setup();

            render(<ApiClientDialog onClose={onClose} />);

            await user.type(screen.getByLabelText('Name'), 'My client');
            await user.click(screen.getByRole('button', {name: 'Create API Client'}));

            expect(hoisted.createMutate).toHaveBeenCalledTimes(1);
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and prefill the name', () => {
            render(<ApiClientDialog apiClient={{id: 1, name: 'Existing', secretKey: 'sk-123'}} onClose={onClose} />);

            expect(screen.getByRole('heading', {name: 'Edit API Client'})).toBeInTheDocument();
            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });
    });

    // The secret is shown exactly once, and this screen was untested: a regression here loses the only copy the user
    // will ever be offered.
    describe('secret key reveal', () => {
        const revealSecretKey = async (secretKey: string) => {
            const onSuccess = hoisted.revealSecretKey;

            if (!onSuccess) {
                throw new Error('The create mutation never received an onSuccess handler.');
            }

            await act(async () => onSuccess({secretKey}));
        };

        it('should show the secret key once the client is created', async () => {
            render(<ApiClientDialog onClose={onClose} />);

            await revealSecretKey('sk-created');

            expect(screen.getByRole('heading', {name: 'Save your secret API key'})).toBeInTheDocument();
            expect(screen.getByDisplayValue('sk-created')).toBeInTheDocument();
        });

        it('should swap the actions for a single Done control', async () => {
            render(<ApiClientDialog onClose={onClose} />);

            await revealSecretKey('sk-created');

            expect(screen.getByRole('button', {name: 'Done'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Create API Client'})).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<ApiClientDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Create API Client'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<ApiClientDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiClientDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<ApiClientDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
