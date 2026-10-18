import {useApplicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {act, render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

import AccountErrorPage from '../AccountErrorPage';
import Register from '../Register';
import {createMockApplicationInfoStore, mockApplicationInfoStore} from '../tests/mocks/mockApplicationInfoStore';

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

beforeEach(() => {
    mockApplicationInfoStore();
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
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

it('should show only the social login buttons of configured providers', async () => {
    const applicationInfoStore = createMockApplicationInfoStore({socialLogin: {enabled: true, providers: ['google']}});

    (useApplicationInfoStore as unknown as Mock).mockImplementation(
        (selector: (state: typeof applicationInfoStore) => unknown) => selector(applicationInfoStore)
    );

    renderRegisterPage();

    expect(screen.queryByText('Continue with Google')).toBeInTheDocument();
    expect(screen.queryByText('Continue with Github')).not.toBeInTheDocument();
});

it('should show both social login buttons when both providers are configured', async () => {
    const applicationInfoStore = createMockApplicationInfoStore({
        socialLogin: {enabled: true, providers: ['github', 'google']},
    });

    (useApplicationInfoStore as unknown as Mock).mockImplementation(
        (selector: (state: typeof applicationInfoStore) => unknown) => selector(applicationInfoStore)
    );

    renderRegisterPage();

    expect(screen.queryByText('Continue with Google')).toBeInTheDocument();
    expect(screen.queryByText('Continue with Github')).toBeInTheDocument();
});

it('should hide social login buttons when social login is disabled', async () => {
    const applicationInfoStore = createMockApplicationInfoStore({
        socialLogin: {enabled: false, providers: ['github', 'google']},
    });

    (useApplicationInfoStore as unknown as Mock).mockImplementation(
        (selector: (state: typeof applicationInfoStore) => unknown) => selector(applicationInfoStore)
    );

    renderRegisterPage();

    expect(screen.queryByText('Continue with Google')).not.toBeInTheDocument();
    expect(screen.queryByText('Continue with Github')).not.toBeInTheDocument();
});

it('should navigate to the account error page with a message when registration fails without an error body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, {status: 403}));

    render(
        <MemoryRouter initialEntries={['/register']}>
            <Routes>
                <Route element={<Register />} path="/register" />

                <Route element={<AccountErrorPage />} path="/account-error" />
            </Routes>
        </MemoryRouter>
    );

    await triggerShowPasswordInputField();

    await userEvent.type(await screen.findByLabelText('Password'), 'Password1');
    await userEvent.click(screen.getByRole('button', {name: 'Continue with password'}));

    expect(await screen.findByText('Registration failed. Please try again.')).toBeInTheDocument();

    vi.restoreAllMocks();
});

it('should navigate to the verify email page after a successful registration', async () => {
    const applicationInfoStore = createMockApplicationInfoStore({signUp: {activationRequired: true, enabled: true}});

    (useApplicationInfoStore as unknown as Mock).mockImplementation(
        (selector: (state: typeof applicationInfoStore) => unknown) => selector(applicationInfoStore)
    );

    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, {status: 201}));

    render(
        <MemoryRouter initialEntries={['/register']}>
            <Routes>
                <Route element={<Register />} path="/register" />

                <Route element={<p>Verify email page</p>} path="/verify-email" />
            </Routes>
        </MemoryRouter>
    );

    await triggerShowPasswordInputField();

    await userEvent.type(await screen.findByLabelText('Password'), 'Password1');
    await userEvent.click(screen.getByRole('button', {name: 'Continue with password'}));

    expect(await screen.findByText('Verify email page')).toBeInTheDocument();

    vi.restoreAllMocks();
});
