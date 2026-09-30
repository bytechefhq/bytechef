import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {
    act,
    render,
    resetAll,
    screen,
    userEvent,
    waitFor,
    windowResizeObserver,
    within,
} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes, useLocation} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

import Register from '../Register';
import {mockApplicationInfoStore} from '../tests/mocks/mockApplicationInfoStore';

screen.debug();

const renderRegisterPage = () => {
    render(
        <MemoryRouter initialEntries={['/register']}>
            <Routes>
                <Route element={<Register />} path="/register" />
            </Routes>
        </MemoryRouter>
    );
};

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn(),
}));

vi.mock('@/shared/stores/useFeatureFlagsStore', () => ({
    useFeatureFlagsStore: vi.fn(),
}));

(useFeatureFlagsStore as unknown as Mock).mockReturnValue(vi.fn());

beforeEach(() => {
    mockApplicationInfoStore();
    windowResizeObserver();
});

afterEach(() => {
    resetAll();

    vi.restoreAllMocks();
});

const triggerShowPasswordInputField = async () => {
    const emailInput = screen.getByLabelText('Email');
    const continueButton = screen.getByRole('button', {name: 'Continue'});

    await userEvent.type(emailInput, 'test@example.com');
    await userEvent.click(continueButton);
};

it('should render the register page', async () => {
    renderRegisterPage();

    await waitFor(() => {
        expect(screen.getByText('Create your account')).toBeInTheDocument();
    });
});

it('should show validation error after clicking "Continue" button if email field is empty', async () => {
    renderRegisterPage();

    await act(async () => {
        userEvent.click(screen.getByRole('button', {name: 'Continue'}));
    });

    await waitFor(() => {
        expect(screen.getByText('Email is required')).toBeInTheDocument();
    });
});

it('should show password input field after clicking "Continue" if email is valid', async () => {
    await act(async () => renderRegisterPage());

    await triggerShowPasswordInputField();

    await waitFor(() => {
        expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });
});

it('should set type as password initially and toggle between types when "show password" icon is clicked', async () => {
    await act(async () => renderRegisterPage());

    await triggerShowPasswordInputField();

    await waitFor(() => {
        expect(screen.getByLabelText('Password')).toBeInTheDocument();
    });

    const passwordInputField = screen.getByLabelText('Password');
    await userEvent.type(passwordInputField, 'password');

    const showPasswordButton = screen.getByRole('button', {name: /Show Password/i});

    expect(passwordInputField).toHaveAttribute('type', 'password');

    await userEvent.click(showPasswordButton);
    await waitFor(() => {
        expect(passwordInputField).toHaveAttribute('type', 'text');
    });

    await userEvent.click(showPasswordButton);
    await waitFor(() => {
        expect(passwordInputField).toHaveAttribute('type', 'password');
    });
});

const LocationEmailPage = ({title}: {title: string}) => {
    const location = useLocation();

    return <p>{`${title}: ${location.state?.email}`}</p>;
};

const renderRegisterPageWithDestinations = () => {
    render(
        <MemoryRouter initialEntries={['/register']}>
            <Routes>
                <Route element={<Register />} path="/register" />

                <Route element={<LocationEmailPage title="Login page" />} path="/login" />

                <Route element={<LocationEmailPage title="Password reset page" />} path="/password-reset/init" />

                <Route element={<p>Account error page</p>} path="/account-error" />
            </Routes>
        </MemoryRouter>
    );
};

const mockRegisterResponse = (errorKey: number, detail: string) => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
        new Response(JSON.stringify({detail, entityClass: 'User', errorKey, status: 400, title: 'Error'}), {
            headers: {'Content-Type': 'application/problem+json'},
            status: 400,
        })
    );
};

const submitRegistration = async () => {
    await triggerShowPasswordInputField();

    await userEvent.type(await screen.findByLabelText('Password'), 'Password1');
    await userEvent.click(screen.getByRole('button', {name: 'Continue with password'}));
};

it('should offer to log in or reset the password when the email already has an account', async () => {
    mockRegisterResponse(101, 'Email is already in use!');

    renderRegisterPageWithDestinations();

    await submitRegistration();

    const alert = await screen.findByRole('alert');

    expect(alert).toHaveTextContent('An account with this email already exists.');
    expect(screen.queryByText('Account error page')).not.toBeInTheDocument();

    await userEvent.click(within(alert).getByRole('link', {name: 'Log in'}));

    expect(await screen.findByText('Login page: test@example.com')).toBeInTheDocument();
});

it('should pass the email to the password reset page when the email already has an account', async () => {
    mockRegisterResponse(101, 'Email is already in use!');

    renderRegisterPageWithDestinations();

    await submitRegistration();

    await userEvent.click(within(await screen.findByRole('alert')).getByRole('link', {name: 'Reset password'}));

    expect(await screen.findByText('Password reset page: test@example.com')).toBeInTheDocument();
});

it('should navigate to the account error page for other registration errors', async () => {
    mockRegisterResponse(102, 'Login name already used!');

    renderRegisterPageWithDestinations();

    await submitRegistration();

    expect(await screen.findByText('Account error page')).toBeInTheDocument();
});

it('should show socials login buttons with correct feature flag', async () => {
    (useFeatureFlagsStore as unknown as Mock).mockReturnValue((featureFlag: string) => {
        return featureFlag === 'ff-1874';
    });

    renderRegisterPage();

    expect(screen.queryByText('Continue with Google')).toBeInTheDocument();
    expect(screen.queryByText('Continue with Github')).toBeInTheDocument();
});
