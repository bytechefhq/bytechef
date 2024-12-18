import {render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import AccountErrorPage from '../AccountErrorPage';
import Login from '../Login';
import PasswordResetEmailSent from '../PasswordResetEmailSent';
import PasswordResetInit from '../PasswordResetInit';
import {mockPasswordResetStore} from '../tests/mocks/mockPasswordResetStore';
import {resetAll, windowResizeObserver} from './utils/testUtils';

screen.debug();

const renderPasswordResetInitPage = () => {
    render(
        <MemoryRouter initialEntries={['/password-reset/init']}>
            <Routes>
                <Route element={<PasswordResetInit />} path="/password-reset/init" />
            </Routes>
        </MemoryRouter>
    );
};

vi.mock('@/pages/account/public/stores/usePasswordResetStore', () => ({
    usePasswordResetStore: vi.fn(),
}));

beforeEach(() => {
    mockPasswordResetStore();
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('should render the password reset init page', () => {
    renderPasswordResetInitPage();

    expect(screen.getByText('Forgot your password?')).toBeInTheDocument();
    expect(screen.getByText('Send link to email')).toBeInTheDocument();
});

it('should render "AccountError" page when "resetPasswordFailure" is true', async () => {
    mockPasswordResetStore({
        resetPasswordFailure: true,
    });

    render(
        <MemoryRouter initialEntries={['/password-reset/init']}>
            <Routes>
                <Route element={<PasswordResetInit />} path="/password-reset/init" />

                <Route element={<AccountErrorPage />} path="/account-error" />
            </Routes>
        </MemoryRouter>
    );

    await waitFor(() => {
        expect(screen.getByText('Something went wrong. Try again.')).toBeInTheDocument();
    });
});

it('should render "PasswordResetEmailSent" page when "resetPasswordSuccess" is true', async () => {
    mockPasswordResetStore({
        resetPasswordSuccess: true,
    });

    render(
        <MemoryRouter initialEntries={['/password-reset/init']}>
            <Routes>
                <Route element={<PasswordResetInit />} path="/password-reset/init" />

                <Route element={<PasswordResetEmailSent />} path="/password-reset/email" />
            </Routes>
        </MemoryRouter>
    );

    await waitFor(() => {
        expect(screen.getByText('Please check your email')).toBeInTheDocument();
    });
});

it('should show validation errors after clicking "Send link to email" button, if input field is empty', async () => {
    renderPasswordResetInitPage();

    const sendLinkButton = screen.getByText('Send link to email');
    await userEvent.click(sendLinkButton);

    await waitFor(() => {
        expect(screen.getByText('Email is required')).toBeInTheDocument();
    });
});

it('should render "Login" page when "Log in" link is clicked', async () => {
    render(
        <MemoryRouter initialEntries={['/password-reset/init']}>
            <Routes>
                <Route element={<PasswordResetInit />} path="/password-reset/init" />

                <Route element={<Login />} path="/login" />
            </Routes>
        </MemoryRouter>
    );

    const loginLink = screen.getByText('Log in');
    await userEvent.click(loginLink);

    await waitFor(() => {
        expect(screen.getByText('Welcome back')).toBeInTheDocument();
    });
});
