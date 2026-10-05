import {act, render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import SigningKeyDialog from '../SigningKeyDialog';

const hoisted = vi.hoisted(() => ({
    createMutate: vi.fn(),
    revealPrivateKey: undefined as ((result: {privateKey?: string}) => void) | undefined,
    updateMutate: vi.fn(),
}));

vi.mock('@/ee/shared/mutations/embedded/signingKeys.mutations', () => ({
    // The dialog only reaches its reveal screen through the create mutation's onSuccess, so the mock hands that
    // callback back to the test instead of discarding it.
    useCreateSigningKeyMutation: (options: {onSuccess: (result: {privateKey?: string}) => void}) => {
        hoisted.revealPrivateKey = options.onSuccess;

        return {mutate: hoisted.createMutate, reset: vi.fn()};
    },
    useUpdateSigningKeyMutation: () => ({mutate: hoisted.updateMutate, reset: vi.fn()}),
}));

vi.mock('@/ee/shared/queries/embedded/signingKeys.queries', () => ({
    SigningKeyKeys: {signingKeys: ['signingKeys']},
}));

const onClose = vi.fn();

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
    vi.clearAllMocks();
});

describe('SigningKeyDialog', () => {
    describe('create mode', () => {
        it('should render the create title', () => {
            render(<SigningKeyDialog onClose={onClose} />);

            expect(screen.getByRole('heading', {name: 'Create Signing Key'})).toBeInTheDocument();
        });

        it('should render an empty name field', () => {
            render(<SigningKeyDialog onClose={onClose} />);

            expect(screen.getByLabelText('Name')).toHaveValue('');
        });

        it('should create the signing key on save', async () => {
            const user = userEvent.setup();

            render(<SigningKeyDialog onClose={onClose} />);

            await user.type(screen.getByLabelText('Name'), 'My key');
            await user.click(screen.getByRole('button', {name: 'Create Signing Key'}));

            expect(hoisted.createMutate).toHaveBeenCalledTimes(1);
        });
    });

    describe('edit mode', () => {
        it('should render the edit title and prefill the name', () => {
            render(<SigningKeyDialog onClose={onClose} signingKey={{id: 1, keyId: 'key-1', name: 'Existing'}} />);

            expect(screen.getByRole('heading', {name: 'Edit Signing Key'})).toBeInTheDocument();
            expect(screen.getByLabelText('Name')).toHaveValue('Existing');
            expect(screen.getByRole('button', {name: 'Save'})).toBeInTheDocument();
        });
    });

    // The key is shown exactly once, and this screen was untested: a regression here loses the only copy the user
    // will ever be offered.
    describe('private key reveal', () => {
        const revealPrivateKey = async (privateKey: string) => {
            const onSuccess = hoisted.revealPrivateKey;

            if (!onSuccess) {
                throw new Error('The create mutation never received an onSuccess handler.');
            }

            await act(async () => onSuccess({privateKey}));
        };

        it('should show the private key once the signing key is created', async () => {
            render(<SigningKeyDialog onClose={onClose} />);

            await revealPrivateKey('-----BEGIN PRIVATE KEY-----');

            expect(screen.getByRole('heading', {name: 'Save your private Signing Key'})).toBeInTheDocument();
            expect(screen.getByDisplayValue('-----BEGIN PRIVATE KEY-----')).toBeInTheDocument();
        });

        it('should swap the actions for a single Done control', async () => {
            render(<SigningKeyDialog onClose={onClose} />);

            await revealPrivateKey('-----BEGIN PRIVATE KEY-----');

            expect(screen.getByRole('button', {name: 'Done'})).toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Cancel'})).not.toBeInTheDocument();
            expect(screen.queryByRole('button', {name: 'Create Signing Key'})).not.toBeInTheDocument();
        });
    });

    describe('dialog chrome', () => {
        it('should render the close, cancel and save controls', () => {
            render(<SigningKeyDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Close'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Cancel'})).toBeInTheDocument();
            expect(screen.getByRole('button', {name: 'Create Signing Key'})).toBeInTheDocument();
        });

        it('should keep the cancel button out of the form submission', () => {
            render(<SigningKeyDialog onClose={onClose} />);

            expect(screen.getByRole('button', {name: 'Cancel'})).toHaveAttribute('type', 'button');
        });
    });

    describe('interactions', () => {
        it('should call onClose when cancel is clicked', async () => {
            const user = userEvent.setup();

            render(<SigningKeyDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Cancel'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });

        it('should call onClose when the close button is clicked', async () => {
            const user = userEvent.setup();

            render(<SigningKeyDialog onClose={onClose} />);

            await user.click(screen.getByRole('button', {name: 'Close'}));

            expect(onClose).toHaveBeenCalledTimes(1);
        });
    });
});
