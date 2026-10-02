import {render, resetAll, screen, userEvent, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import DeleteIdentityProviderAlertDialog from '../DeleteIdentityProviderAlertDialog';

const hoisted = vi.hoisted(() => ({
    handleCloseMock: vi.fn(),
    handleDeleteMock: vi.fn(),
    isPending: false,
    open: true,
}));

vi.mock('../hooks/useDeleteIdentityProviderAlertDialog', () => ({
    default: () => ({
        handleClose: hoisted.handleCloseMock,
        handleDelete: hoisted.handleDeleteMock,
        handleOpen: vi.fn(),
        isPending: hoisted.isPending,
        open: hoisted.open,
    }),
}));

beforeEach(() => {
    windowResizeObserver();

    hoisted.isPending = false;
    hoisted.open = true;
});

afterEach(() => {
    resetAll();
});

describe('DeleteIdentityProviderAlertDialog', () => {
    it('renders nothing while no identity provider is selected for deletion', () => {
        hoisted.open = false;

        render(<DeleteIdentityProviderAlertDialog />);

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
    });

    it('deletes the identity provider when confirmed', async () => {
        render(<DeleteIdentityProviderAlertDialog />);

        await userEvent.click(screen.getByRole('button', {name: 'Delete'}));

        expect(hoisted.handleDeleteMock).toHaveBeenCalledTimes(1);
        expect(hoisted.handleCloseMock).not.toHaveBeenCalled();
    });

    it('closes without deleting when cancelled', async () => {
        render(<DeleteIdentityProviderAlertDialog />);

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(hoisted.handleCloseMock).toHaveBeenCalledTimes(1);
        expect(hoisted.handleDeleteMock).not.toHaveBeenCalled();
    });

    it('disables both actions while the deletion is in flight', () => {
        hoisted.isPending = true;

        render(<DeleteIdentityProviderAlertDialog />);

        expect(screen.getByRole('button', {name: 'Delete'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();
    });
});
