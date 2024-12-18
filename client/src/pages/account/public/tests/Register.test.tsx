import {act, render, screen, userEvent, waitFor} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, expect, it, vi} from 'vitest';

import Register from '../Register';
import {mockApplicationInfoStore} from '../tests/mocks/mockApplicationInfoStore';
import {resetAll, windowResizeObserver} from './utils/testUtils';

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
