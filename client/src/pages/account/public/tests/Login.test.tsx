import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

import AccountErrorPage from '../AccountErrorPage';
import Login from '../Login';
import PasswordResetInit from '../PasswordResetInit';
import Register from '../Register';
import {mockApplicationInfoStore} from '../tests/mocks/mockApplicationInfoStore';
import {mockAuthenticationStore} from '../tests/mocks/mockAuthenticationStore';

screen.debug();

const renderLoginPage = () => {
    render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route element={<Login />} path="/login" />
            </Routes>
        </MemoryRouter>
    );
};

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn(),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: vi.fn(),
}));

(useFeatureFlagsStore as unknown as Mock).mockReturnValue(vi.fn());

beforeEach(() => {
    mockApplicationInfoStore();
    mockAuthenticationStore();
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('should render the login page on "/login" path', () => {
    renderLoginPage();

    expect(screen.getByText('Welcome back')).toBeInTheDocument();
});

it('should set type as password initially and toggle between types when "show password" icon is clicked', async () => {
    renderLoginPage();

    const passwordInputField = screen.getByLabelText('Password');
    await userEvent.type(passwordInputField, 'password');

    expect(passwordInputField).toHaveAttribute('type', 'password');

    const showPasswordButton = screen.getByRole('button', {name: /show password/i});
    await userEvent.click(showPasswordButton);
    expect(passwordInputField).toHaveAttribute('type', 'text');

    await userEvent.click(showPasswordButton);
    expect(passwordInputField).toHaveAttribute('type', 'password');
});

it('should not submit form when only "show password" button is clicked', async () => {
    renderLoginPage();

    const emailInputField = screen.getByLabelText('Email');
    const passwordInputField = screen.getByLabelText('Password');
    const stayLoggedInCheckbox = screen.getByRole('checkbox', {name: /stay logged in/i});

    await userEvent.type(emailInputField, 'test@example.com');
    await userEvent.type(passwordInputField, 'password');
    await userEvent.click(stayLoggedInCheckbox);

    const showPasswordButton = screen.getByRole('button', {name: /show password/i});

    await userEvent.click(showPasswordButton);

    await waitFor(() => {
        expect(useAuthenticationStore((state) => state).login).not.toHaveBeenCalled();
    });
});

it('should render "PasswordResetInit" page if "Forgot your password" is clicked', async () => {
    render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route element={<Login />} path="/login" />

                <Route element={<PasswordResetInit />} path="/password-reset/init" />
            </Routes>
        </MemoryRouter>
    );

    expect(screen.getByText('Welcome back')).toBeInTheDocument();

    const forgotPasswordButton = screen.getByText('Forgot your password?');
    await userEvent.click(forgotPasswordButton);

    await waitFor(() => {
        expect(screen.getByText('Send link to email')).toBeInTheDocument();
    });
});

it('should render "Register" page if "Create account" is clicked', async () => {
    render(
        <MemoryRouter initialEntries={['/login']}>
            <Routes>
                <Route element={<Login />} path="/login" />

                <Route element={<Register />} path="/register" />
            </Routes>
        </MemoryRouter>
    );

    expect(screen.getByText('Welcome back')).toBeInTheDocument();

    const createAccountButton = screen.getByText('Create account');
    await userEvent.click(createAccountButton);

    await waitFor(() => {
        expect(screen.getByText('Create your account')).toBeInTheDocument();
    });
});

it('should submit form with correct login details when "Log in" is clicked', async () => {
    renderLoginPage();

    const emailInputField = screen.getByLabelText('Email');
    const passwordInputField = screen.getByLabelText('Password');
    const stayLoggedInCheckbox = screen.getByRole('checkbox', {name: /stay logged in/i});

    const logInButton = screen.getByRole('button', {name: /log in/i});

    await userEvent.type(emailInputField, 'test@example.com');
    await userEvent.type(passwordInputField, 'password');
    await userEvent.click(stayLoggedInCheckbox);

    await userEvent.click(logInButton);

    await waitFor(() => {
        expect(useAuthenticationStore((state) => state).login).toHaveBeenCalledWith(
            'test@example.com',
            'password',
            true
        );
    });
});

it('should show validation errors after clicking "Log in" button, if input fields are empty strings', async () => {
    renderLoginPage();

    const emailInputField = screen.getByLabelText('Email');
    const passwordInputField = screen.getByLabelText('Password');

    const logInButton = screen.getByRole('button', {name: /log in/i});

    await userEvent.type(emailInputField, ' ');
    await userEvent.type(passwordInputField, ' ');

    await userEvent.click(logInButton);

    expect(screen.getByText('Email is required')).toBeInTheDocument();
    expect(screen.getByText('Password is required')).toBeInTheDocument();
});

it('should toggle "Stay logged in" checkbox on click', async () => {
    renderLoginPage();

    const checkbox = screen.getByLabelText('Stay logged in');

    expect(checkbox).toBeInTheDocument();

    expect(checkbox).not.toBeChecked();

    await userEvent.click(checkbox);
    expect(checkbox).toBeChecked();

    await userEvent.click(checkbox);
    expect(checkbox).not.toBeChecked();
});

it('should disable submit button and show loading icon while log in credentials are being authenticated', async () => {
    const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

    mockAuthenticationStore({
        login: async () => {
            await delay(1000);
        },
    });

    renderLoginPage();

    const logInButton = screen.getByLabelText('log in button');

    expect(logInButton).not.toBeDisabled();
    expect(screen.queryByLabelText('loading icon')).not.toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Email'), 'test@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'password');
    await userEvent.click(screen.getByRole('button', {name: /log in/i}));

    expect(logInButton).toBeDisabled();
    expect(screen.getByLabelText('loading icon')).toBeInTheDocument();
});

it('should redirect to account-error page when ?error=oauth2 is present', async () => {
    render(
        <MemoryRouter initialEntries={['/login?error=oauth2']}>
            <Routes>
                <Route element={<Login />} path="/login" />

                <Route element={<AccountErrorPage />} path="/account-error" />
            </Routes>
        </MemoryRouter>
    );

    await waitFor(() => {
        expect(screen.getByText('Error')).toBeInTheDocument();
        expect(
            screen.getByText('Failed to sign in with social provider. Please try again or use email/password.')
        ).toBeInTheDocument();
    });
});

it('should show socials login buttons with correct feature flag', async () => {
    (useFeatureFlagsStore as unknown as Mock).mockReturnValue((featureFlag: string) => {
        return featureFlag === 'ff-1874';
    });

    renderLoginPage();

    expect(screen.queryByText('Continue with Google')).toBeInTheDocument();
    expect(screen.queryByText('Continue with Github')).toBeInTheDocument();
});
