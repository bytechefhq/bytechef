import {useFeatureFlagsStore} from '@/shared/stores/useFeatureFlagsStore';
import {render, resetAll, screen, userEvent, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

import AccountErrorPage from '../AccountErrorPage';
import Login from '../Login';
import RegisterSuccess from '../RegisterSuccess';
import {mockActivateStore} from '../tests/mocks/mockActivateStore';
import {mockApplicationInfoStore} from '../tests/mocks/mockApplicationInfoStore';
import {mockAuthenticationStore} from '../tests/mocks/mockAuthenticationStore';

const renderRegisterSuccessPage = (initialPath = '/activate?key=test-activation-key') => {
    render(
        <MemoryRouter initialEntries={[initialPath]}>
            <Routes>
                <Route element={<RegisterSuccess />} path="/activate" />

                <Route element={<AccountErrorPage />} path="/account-error" />

                <Route element={<Login />} path="/login" />
            </Routes>
        </MemoryRouter>
    );
};

vi.mock('@/pages/account/public/stores/useActivateStore', () => ({
    useActivateStore: vi.fn(),
}));

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
    mockActivateStore();
    mockApplicationInfoStore();
    mockAuthenticationStore();
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('should render the register success page', () => {
    renderRegisterSuccessPage();

    expect(screen.getByText('Account created successfully')).toBeInTheDocument();
    expect(screen.getByText("You're ready to start using ByteChef.")).toBeInTheDocument();
    expect(screen.getByRole('button', {name: 'Start'})).toBeInTheDocument();
});

it('should call activate with the key from URL params', () => {
    const mockStore = mockActivateStore();

    renderRegisterSuccessPage('/activate?key=my-activation-key');

    expect(mockStore.activate).toHaveBeenCalledWith('my-activation-key');
});

it('should call activate only once even if component re-renders', () => {
    const mockStore = mockActivateStore();

    const {rerender} = render(
        <MemoryRouter initialEntries={['/activate?key=test-key']}>
            <Routes>
                <Route element={<RegisterSuccess />} path="/activate" />
            </Routes>
        </MemoryRouter>
    );

    expect(mockStore.activate).toHaveBeenCalledTimes(1);

    rerender(
        <MemoryRouter initialEntries={['/activate?key=test-key']}>
            <Routes>
                <Route element={<RegisterSuccess />} path="/activate" />
            </Routes>
        </MemoryRouter>
    );

    expect(mockStore.activate).toHaveBeenCalledTimes(1);
});

it('should not call activate when key is not present in URL', () => {
    const mockStore = mockActivateStore();

    renderRegisterSuccessPage('/activate');

    expect(mockStore.activate).not.toHaveBeenCalled();
});

it('should navigate to error page when activationFailure is true', async () => {
    mockActivateStore({
        activationFailure: true,
    });

    renderRegisterSuccessPage();

    await waitFor(() => {
        expect(screen.getByText('Something went wrong. Try again.')).toBeInTheDocument();
    });
});

it('should show loading state when loading is true', () => {
    mockActivateStore({
        loading: true,
    });

    renderRegisterSuccessPage();

    const startButton = screen.getByRole('button', {name: 'Start'});

    expect(startButton).toBeDisabled();
});

it('should navigate to login page when "Start" button is clicked', async () => {
    mockActivateStore({
        activationSuccess: true,
        loading: false,
    });

    renderRegisterSuccessPage();

    const startButton = screen.getByRole('button', {name: 'Start'});
    await userEvent.click(startButton);

    await waitFor(() => {
        expect(screen.getByText('Welcome back')).toBeInTheDocument();
    });
});

it('should not call activate if activationSuccess is already true', () => {
    const mockStore = mockActivateStore({
        activationSuccess: true,
    });

    renderRegisterSuccessPage('/activate?key=test-key');

    expect(mockStore.activate).not.toHaveBeenCalled();
});
