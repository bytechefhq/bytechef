import PrivateRoute from '@/shared/auth/PrivateRoute';
import {UserI} from '@/shared/models/user.model';
import {authenticationStore, useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {act, render, screen} from '@testing-library/react';
import {useEffect} from 'react';
import {MemoryRouter, Route, Routes} from 'react-router-dom';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const ADMIN_ACCOUNT: UserI = {activated: true, authorities: ['ROLE_ADMIN'], login: 'admin'};

const createDeferred = <T,>() => {
    let resolve!: (value: T) => void;

    const promise = new Promise<T>((promiseResolve) => {
        resolve = promiseResolve;
    });

    return {promise, resolve};
};

const ResetAuthenticationWhenSignedOut = () => {
    const authenticated = useAuthenticationStore((state) => state.authenticated);
    const reset = useAuthenticationStore((state) => state.reset);

    useEffect(() => {
        if (!authenticated) {
            reset();
        }
    }, [authenticated, reset]);

    return null;
};

const renderPrivateRoute = (hasAnyAuthorities?: string[]) =>
    render(
        <MemoryRouter initialEntries={['/protected']}>
            <ResetAuthenticationWhenSignedOut />

            <Routes>
                <Route
                    element={
                        <PrivateRoute hasAnyAuthorities={hasAnyAuthorities}>
                            <div>protected page</div>
                        </PrivateRoute>
                    }
                    path="/protected"
                />

                <Route element={<div>login page</div>} path="/login" />
            </Routes>
        </MemoryRouter>
    );

describe('PrivateRoute', () => {
    beforeEach(() => {
        authenticationStore.getState().reset();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('renders nothing while the session has not been fetched', () => {
        const {container} = renderPrivateRoute();

        expect(screen.queryByText('protected page')).not.toBeInTheDocument();
        expect(screen.queryByText('login page')).not.toBeInTheDocument();
        expect(container.textContent).toBe('');
    });

    it('renders the page for an authenticated account', () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        renderPrivateRoute();

        expect(screen.getByText('protected page')).toBeInTheDocument();
    });

    it('redirects to the login page once the session is known to be unauthenticated', () => {
        authenticationStore.setState({authenticated: false, sessionHasBeenFetched: true});

        renderPrivateRoute();

        expect(screen.getByText('login page')).toBeInTheDocument();
    });

    it('refuses an authenticated account that lacks the required authority', () => {
        authenticationStore.setState({
            account: {...ADMIN_ACCOUNT, authorities: ['ROLE_USER']},
            authenticated: true,
            sessionHasBeenFetched: true,
        });

        renderPrivateRoute(['ROLE_ADMIN']);

        expect(screen.getByText('You are not authorized to access this page.')).toBeInTheDocument();
        expect(screen.queryByText('protected page')).not.toBeInTheDocument();
    });

    it('redirects to the login page after logging out', async () => {
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

        const refreshDeferred = createDeferred<void>();

        vi.stubGlobal(
            'fetch',
            vi
                .fn()
                .mockResolvedValueOnce(new Response(null, {status: 200}))
                .mockImplementationOnce((_url: string, init: RequestInit) =>
                    refreshDeferred.promise.then(() => {
                        if (init.signal?.aborted) {
                            throw new DOMException('The operation was aborted.', 'AbortError');
                        }

                        return new Response(null, {status: 401});
                    })
                )
        );

        renderPrivateRoute();

        expect(screen.getByText('protected page')).toBeInTheDocument();

        await act(async () => {
            await authenticationStore.getState().logout();
        });

        await act(async () => {
            refreshDeferred.resolve();
        });

        expect(await screen.findByText('login page')).toBeInTheDocument();
    });
});
