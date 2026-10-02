import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AccountProfileMfa from '../AccountProfileMfa';

const hoisted = vi.hoisted(() => ({
    toastErrorMock: vi.fn(),
    toastMock: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: Object.assign(hoisted.toastMock, {error: hoisted.toastErrorMock}),
}));

const fetchMock = vi.fn((url: string) => {
    if (url === '/api/account/mfa/status') {
        return Promise.resolve({json: () => Promise.resolve({totpEnabled: true}), ok: true});
    }

    return Promise.resolve({ok: true});
});

const openDisableDialog = async () => {
    await userEvent.click(await screen.findByRole('button', {name: 'Disable 2FA'}));

    return screen.getByRole('dialog', {name: 'Disable Two-Factor Authentication'});
};

beforeEach(() => {
    windowResizeObserver();

    vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
    resetAll();
    vi.unstubAllGlobals();
});

describe('AccountProfileMfa disable dialog', () => {
    it('clears the entered credentials when the dialog is dismissed', async () => {
        render(<AccountProfileMfa />);

        await openDisableDialog();

        await userEvent.type(screen.getByLabelText('Password'), 'secret');

        await userEvent.keyboard('{Escape}');

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

        await openDisableDialog();

        expect(screen.getByLabelText('Password')).toHaveValue('');
    });

    it('disables two-factor authentication and closes the dialog', async () => {
        render(<AccountProfileMfa />);

        await openDisableDialog();

        await userEvent.type(screen.getByLabelText('Password'), 'secret');
        await userEvent.type(screen.getByLabelText('Authentication Code'), '123456');

        await userEvent.click(screen.getByRole('button', {name: 'Disable 2FA'}));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

        expect(fetchMock).toHaveBeenCalledWith(
            '/api/account/mfa/disable',
            expect.objectContaining({body: JSON.stringify({code: '123456', password: 'secret'}), method: 'POST'})
        );
        expect(hoisted.toastMock).toHaveBeenCalledWith('Two-factor authentication has been disabled.');
        expect(screen.getByRole('button', {name: 'Set up 2FA'})).toBeInTheDocument();
    });
});
