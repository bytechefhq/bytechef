import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {render, resetAll, screen, waitFor, windowResizeObserver} from '@/shared/util/test-utils';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {Mock, afterEach, beforeEach, expect, it, vi} from 'vitest';

import OAuth2Redirect from '../OAuth2Redirect';
import {mockApplicationInfoStore} from './mocks/mockApplicationInfoStore';
import {mockAuthenticationStore} from './mocks/mockAuthenticationStore';

vi.mock('@/shared/stores/useAuthenticationStore', () => ({
    useAuthenticationStore: vi.fn(),
}));

vi.mock('@/shared/stores/useApplicationInfoStore', () => ({
    useApplicationInfoStore: vi.fn(),
}));

const LoginPage = () => <div>Login Page</div>;
const HomePage = () => <div>Home Page</div>;

const renderOAuth2Redirect = () => {
    render(
        <MemoryRouter initialEntries={['/oauth2/redirect']}>
            <Routes>
                <Route element={<OAuth2Redirect />} path="/oauth2/redirect" />

                <Route element={<HomePage />} path="/" />

                <Route element={<LoginPage />} path="/login" />
            </Routes>
        </MemoryRouter>
    );
};

beforeEach(() => {
    mockApplicationInfoStore();
    mockAuthenticationStore({
        getAccount: vi.fn().mockResolvedValue(null),
    });
    windowResizeObserver();
});

afterEach(() => {
    resetAll();
});

it('should redirect to home when account is returned successfully', async () => {
    mockAuthenticationStore({
        getAccount: vi.fn().mockResolvedValue({email: 'test@example.com', firstName: 'Test', lastName: 'User'}),
    });

    renderOAuth2Redirect();

    await waitFor(() => {
        expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
});

it('should redirect to login with error when no account is returned', async () => {
    mockAuthenticationStore({
        getAccount: vi.fn().mockResolvedValue(null),
    });

    renderOAuth2Redirect();

    await waitFor(() => {
        expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
});

it('should redirect to login with error when getAccount rejects', async () => {
    mockAuthenticationStore({
        getAccount: vi.fn().mockRejectedValue(new Error('Network error')),
    });

    renderOAuth2Redirect();

    await waitFor(() => {
        expect(screen.getByText('Login Page')).toBeInTheDocument();
    });
});

it('should redirect to home immediately when already authenticated', async () => {
    (useAuthenticationStore as unknown as Mock).mockReturnValue({
        authenticated: true,
        getAccount: vi.fn().mockResolvedValue(null),
    });

    renderOAuth2Redirect();

    await waitFor(() => {
        expect(screen.getByText('Home Page')).toBeInTheDocument();
    });
});
