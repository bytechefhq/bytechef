import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import AcceptInvitation from '../AcceptInvitation';

const renderAcceptInvitationPage = () => {
    render(
        <MemoryRouter initialEntries={['/accept-invitation?key=invitation-key']}>
            <Routes>
                <Route element={<AcceptInvitation />} path="/accept-invitation" />
            </Routes>
        </MemoryRouter>
    );
};

const submitPassword = async (password: string) => {
    await userEvent.type(screen.getByLabelText('Password'), password);

    await userEvent.click(screen.getByRole('button', {name: 'Activate account'}));
};

beforeEach(() => {
    windowResizeObserver();
});

afterEach(() => {
    resetAll();

    vi.restoreAllMocks();
});

it('should render the set password form', () => {
    renderAcceptInvitationPage();

    expect(screen.getByText('Set your password')).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Activate account'})).toBeInTheDocument();
});

it('should send the invitation key with the new password and confirm the activation', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, {status: 200}));

    renderAcceptInvitationPage();

    await submitPassword('Password1');

    await waitFor(() => expect(screen.getByText('Your account is ready')).toBeInTheDocument());

    expect(fetchSpy).toHaveBeenCalledWith(
        '/api/account/accept-invitation',
        expect.objectContaining({
            body: JSON.stringify({key: 'invitation-key', newPassword: 'Password1'}),
            method: 'POST',
        })
    );
});

it('should explain when the invitation link is invalid or expired', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, {status: 500}));

    renderAcceptInvitationPage();

    await submitPassword('Password1');

    await waitFor(() => expect(screen.getByRole('alert')).toHaveTextContent('invalid or has expired'));
});

it('should not submit a password that breaks the password rules', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch');

    renderAcceptInvitationPage();

    await submitPassword('short');

    expect(fetchSpy).not.toHaveBeenCalled();
});
