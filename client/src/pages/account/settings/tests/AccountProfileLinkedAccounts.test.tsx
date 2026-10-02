import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import AccountProfileLinkedAccounts from '../AccountProfileLinkedAccounts';

const hoisted = vi.hoisted(() => ({
    toastErrorMock: vi.fn(),
    toastMock: vi.fn(),
}));

vi.mock('sonner', () => ({
    toast: Object.assign(hoisted.toastMock, {error: hoisted.toastErrorMock}),
}));

const linkedAccount = {authProvider: 'GOOGLE', hasPassword: true, providerId: 'google-123'};

let resolveUnlink: (response: {ok: boolean}) => void;
let rejectUnlink: (error: Error) => void;

const fetchMock = vi.fn((url: string, init?: RequestInit) => {
    if (init?.method === 'DELETE') {
        return new Promise((resolve, reject) => {
            resolveUnlink = resolve;
            rejectUnlink = reject;
        });
    }

    return Promise.resolve({json: () => Promise.resolve(linkedAccount), ok: true});
});

const confirmUnlink = async () => {
    await userEvent.click(await screen.findByRole('button', {name: 'Unlink'}));

    await userEvent.click(screen.getByRole('button', {name: 'Unlink'}));
};

const unlinkRequests = () => fetchMock.mock.calls.filter(([, init]) => init?.method === 'DELETE');

beforeEach(() => {
    windowResizeObserver();

    vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
    resetAll();
    vi.unstubAllGlobals();
});

describe('AccountProfileLinkedAccounts unlink dialog', () => {
    it('closes without unlinking when cancelled', async () => {
        render(<AccountProfileLinkedAccounts />);

        await userEvent.click(await screen.findByRole('button', {name: 'Unlink'}));

        await userEvent.click(screen.getByRole('button', {name: 'Cancel'}));

        expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument();
        expect(unlinkRequests()).toHaveLength(0);
    });

    it('keeps the dialog open and disabled while the unlink request is in flight', async () => {
        render(<AccountProfileLinkedAccounts />);

        await confirmUnlink();

        expect(screen.getByRole('alertdialog')).toBeInTheDocument();
        expect(screen.getByRole('button', {name: 'Unlink'})).toBeDisabled();
        expect(screen.getByRole('button', {name: 'Cancel'})).toBeDisabled();
        expect(unlinkRequests()).toEqual([['/api/account/linked-accounts/GOOGLE', {method: 'DELETE'}]]);

        resolveUnlink({ok: true});

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

        expect(hoisted.toastMock).toHaveBeenCalledWith('Provider has been unlinked.');
    });

    it('reports a failed unlink request and closes the dialog', async () => {
        render(<AccountProfileLinkedAccounts />);

        await confirmUnlink();

        rejectUnlink(new Error('Network error'));

        await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());

        expect(hoisted.toastErrorMock).toHaveBeenCalledWith('Failed to unlink provider.');
        expect(hoisted.toastMock).not.toHaveBeenCalled();
    });

    it('reports a rejected unlink response', async () => {
        render(<AccountProfileLinkedAccounts />);

        await confirmUnlink();

        resolveUnlink({ok: false});

        await waitFor(() => expect(hoisted.toastErrorMock).toHaveBeenCalledWith('Failed to unlink provider.'));
    });
});
