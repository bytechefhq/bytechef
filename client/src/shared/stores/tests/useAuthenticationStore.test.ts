import {UserI} from '@/shared/models/user.model';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const ACCOUNT: UserI = {activated: true, authorities: ['ROLE_ADMIN'], login: 'admin'};

const accountResponse = (account: UserI = ACCOUNT) => new Response(JSON.stringify(account), {status: 200});

const emptyResponse = (status: number) => new Response(null, {status});

const LOGIN_EMAIL = 'admin+1@example.com';

const LOGIN_CREDENTIAL = 'a&b';

const createDeferred = <T>() => {
    let resolve!: (value: T) => void;
    let reject!: (reason: unknown) => void;

    const promise = new Promise<T>((promiseResolve, promiseReject) => {
        resolve = promiseResolve;
        reject = promiseReject;
    });

    return {promise, reject, resolve};
};

const stubFetch = () => {
    const fetchMock = vi.fn();

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
};

const accountRequestSignal = (fetchMock: ReturnType<typeof vi.fn>, callIndex = 0): AbortSignal =>
    fetchMock.mock.calls[callIndex][1].signal;

const requestedUrls = (fetchMock: ReturnType<typeof vi.fn>) => fetchMock.mock.calls.map((call) => call[0]);

describe('authenticationStore', () => {
    beforeEach(() => {
        authenticationStore.getState().reset();
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    describe('getAccount', () => {
        it('stores the account and marks the session authenticated on 200', async () => {
            stubFetch().mockResolvedValue(accountResponse());

            const account = await authenticationStore.getState().getAccount();

            const state = authenticationStore.getState();

            expect(account).toEqual(ACCOUNT);
            expect(state.account).toEqual(ACCOUNT);
            expect(state.authenticated).toBe(true);
            expect(state.loading).toBe(false);
            expect(state.sessionHasBeenFetched).toBe(true);
        });

        it('does not mark a non-activated account authenticated', async () => {
            stubFetch().mockResolvedValue(accountResponse({...ACCOUNT, activated: false}));

            await authenticationStore.getState().getAccount();

            expect(authenticationStore.getState().authenticated).toBe(false);
        });

        it('clears authenticated when the session is no longer valid', async () => {
            authenticationStore.setState({authenticated: true});

            stubFetch().mockResolvedValue(emptyResponse(401));

            const account = await authenticationStore.getState().getAccount();

            const state = authenticationStore.getState();

            expect(account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.loading).toBe(false);
            expect(state.sessionHasBeenFetched).toBe(true);
            expect(state.showLogin).toBe(true);
            expect(state).not.toHaveProperty('isAuthenticated');
        });

        it('sets loading while the request is in flight', async () => {
            const deferred = createDeferred<Response>();

            stubFetch().mockReturnValue(deferred.promise);

            const request = authenticationStore.getState().getAccount();

            expect(authenticationStore.getState().loading).toBe(true);

            deferred.resolve(accountResponse());

            await request;

            expect(authenticationStore.getState().loading).toBe(false);
        });

        it('shares the in-flight request between concurrent callers', async () => {
            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch().mockReturnValue(deferred.promise);

            const firstRequest = authenticationStore.getState().getAccount();
            const secondRequest = authenticationStore.getState().getAccount();

            expect(secondRequest).toBe(firstRequest);
            expect(fetchMock).toHaveBeenCalledTimes(1);

            deferred.resolve(accountResponse());

            await expect(firstRequest).resolves.toEqual(ACCOUNT);
            await expect(secondRequest).resolves.toEqual(ACCOUNT);
        });

        it('shares the unauthenticated result between concurrent callers', async () => {
            const deferred = createDeferred<Response>();

            stubFetch().mockReturnValue(deferred.promise);

            const firstRequest = authenticationStore.getState().getAccount();
            const secondRequest = authenticationStore.getState().getAccount();

            deferred.resolve(emptyResponse(401));

            await expect(firstRequest).resolves.toBeUndefined();
            await expect(secondRequest).resolves.toBeUndefined();
        });

        it('starts a new request once the previous one has settled', async () => {
            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(401))
                .mockResolvedValueOnce(accountResponse());

            await expect(authenticationStore.getState().getAccount()).resolves.toBeUndefined();
            await expect(authenticationStore.getState().getAccount()).resolves.toEqual(ACCOUNT);

            expect(fetchMock).toHaveBeenCalledTimes(2);
            expect(authenticationStore.getState().authenticated).toBe(true);
        });

        it('rejects every caller when the request fails and allows a retry', async () => {
            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(deferred.promise)
                .mockResolvedValueOnce(accountResponse());

            const firstRequest = authenticationStore.getState().getAccount();
            const secondRequest = authenticationStore.getState().getAccount();

            deferred.reject(new TypeError('Failed to fetch'));

            await expect(firstRequest).rejects.toThrow('Failed to fetch');
            await expect(secondRequest).rejects.toThrow('Failed to fetch');

            expect(authenticationStore.getState().loading).toBe(false);

            await expect(authenticationStore.getState().getAccount()).resolves.toEqual(ACCOUNT);

            expect(fetchMock).toHaveBeenCalledTimes(2);
        });

        it('aborts a superseded request so its response cannot reach the fetch interceptor', async () => {
            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(deferred.promise)
                .mockResolvedValueOnce(emptyResponse(401));

            const staleRequest = authenticationStore.getState().getAccount();

            expect(accountRequestSignal(fetchMock).aborted).toBe(false);

            authenticationStore.getState().clearAuthentication();

            expect(accountRequestSignal(fetchMock).aborted).toBe(true);

            deferred.resolve(accountResponse());

            await expect(staleRequest).resolves.toEqual(ACCOUNT);

            expect(authenticationStore.getState().authenticated).toBe(false);
        });

        it('lets a request started after the session was cleared update the store', async () => {
            const staleDeferred = createDeferred<Response>();
            const freshDeferred = createDeferred<Response>();

            stubFetch().mockReturnValueOnce(staleDeferred.promise).mockReturnValueOnce(freshDeferred.promise);

            const staleRequest = authenticationStore.getState().getAccount();

            authenticationStore.getState().clearAuthentication();

            const freshRequest = authenticationStore.getState().getAccount();

            staleDeferred.resolve(emptyResponse(401));

            await staleRequest;

            freshDeferred.resolve(accountResponse());

            await expect(freshRequest).resolves.toEqual(ACCOUNT);

            const state = authenticationStore.getState();

            expect(state.account).toEqual(ACCOUNT);
            expect(state.authenticated).toBe(true);
        });

        it('resolves an aborted request instead of rejecting it', async () => {
            const deferred = createDeferred<Response>();

            stubFetch().mockReturnValueOnce(deferred.promise);

            const staleRequest = authenticationStore.getState().getAccount();

            authenticationStore.getState().reset();

            deferred.reject(new DOMException('The operation was aborted.', 'AbortError'));

            await expect(staleRequest).resolves.toBeUndefined();
        });

        it('sends the XSRF token from the cookie', async () => {
            document.cookie = 'XSRF-TOKEN=token-value';

            const fetchMock = stubFetch().mockResolvedValue(accountResponse());

            await authenticationStore.getState().getAccount();

            expect(fetchMock).toHaveBeenCalledWith('/api/account', {
                headers: {'X-XSRF-TOKEN': 'token-value'},
                method: 'GET',
                signal: expect.any(AbortSignal),
            });

            document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        });
    });

    describe('login', () => {
        it('posts the encoded credentials', async () => {
            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(accountResponse());

            await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, true);

            const body = new URLSearchParams(fetchMock.mock.calls[0][1].body);

            expect(fetchMock.mock.calls[0][0]).toBe('/api/authentication');
            expect(body.get('username')).toBe(LOGIN_EMAIL);
            expect(body.get('password')).toBe(LOGIN_CREDENTIAL);
            expect(body.get('remember-me')).toBe('true');
            expect(body.get('submit')).toBe('Login');
        });

        it('fetches the account on 200 without adding fields outside the store shape', async () => {
            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(accountResponse());

            const account = await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, false);

            const state = authenticationStore.getState();

            expect(account).toEqual(ACCOUNT);
            expect(requestedUrls(fetchMock)).toEqual(['/api/authentication', '/api/account']);
            expect(state.authenticated).toBe(true);
            expect(state.loginError).toBe(false);
            expect(state.showLogin).toBe(false);
            expect(state).not.toHaveProperty('loginSuccess');
        });

        it('does not let a request started before a failed login restore the session', async () => {
            const deferred = createDeferred<Response>();

            stubFetch().mockReturnValueOnce(deferred.promise).mockResolvedValueOnce(emptyResponse(401));

            const staleRequest = authenticationStore.getState().getAccount();

            await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, false);

            deferred.resolve(accountResponse());

            await expect(staleRequest).resolves.toEqual(ACCOUNT);

            const state = authenticationStore.getState();

            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.loginError).toBe(true);
        });

        it('does not reuse an account request started before the login succeeded', async () => {
            const preLoginDeferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(preLoginDeferred.promise)
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(accountResponse());

            const preLoginRequest = authenticationStore.getState().getAccount();

            const account = await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, false);

            expect(requestedUrls(fetchMock)).toEqual(['/api/account', '/api/authentication', '/api/account']);
            expect(accountRequestSignal(fetchMock).aborted).toBe(true);
            expect(account).toEqual(ACCOUNT);
            expect(authenticationStore.getState().authenticated).toBe(true);

            preLoginDeferred.resolve(emptyResponse(401));

            await preLoginRequest;

            expect(authenticationStore.getState().authenticated).toBe(true);
        });

        it('requires MFA on 202 without fetching the account', async () => {
            const fetchMock = stubFetch().mockResolvedValueOnce(emptyResponse(202));

            const account = await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, false);

            const state = authenticationStore.getState();

            expect(account).toBeUndefined();
            expect(fetchMock).toHaveBeenCalledTimes(1);
            expect(state.loginError).toBe(false);
            expect(state.mfaRequired).toBe(true);
            expect(state.showLogin).toBe(false);
        });

        it('resets the session and flags a login error on failure', async () => {
            authenticationStore.setState({account: ACCOUNT, authenticated: true, mfaRequired: true});

            stubFetch().mockResolvedValueOnce(emptyResponse(401));

            const account = await authenticationStore.getState().login(LOGIN_EMAIL, LOGIN_CREDENTIAL, false);

            const state = authenticationStore.getState();

            expect(account).toBeUndefined();
            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.loginError).toBe(true);
            expect(state.mfaRequired).toBe(false);
            expect(state.showLogin).toBe(true);
        });
    });

    describe('verifyMfa', () => {
        it('fetches the account and clears mfaRequired on 200', async () => {
            authenticationStore.setState({mfaRequired: true});

            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(accountResponse());

            const account = await authenticationStore.getState().verifyMfa('123456');

            expect(account).toEqual(ACCOUNT);
            expect(requestedUrls(fetchMock)).toEqual(['/api/mfa/verify', '/api/account']);
            expect(fetchMock.mock.calls[0][1].body).toBe(JSON.stringify({code: '123456'}));
            expect(authenticationStore.getState().mfaRequired).toBe(false);
            expect(authenticationStore.getState().authenticated).toBe(true);
        });

        it('does not reuse an account request started before the code was verified', async () => {
            const preVerifyDeferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(preVerifyDeferred.promise)
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(accountResponse());

            const preVerifyRequest = authenticationStore.getState().getAccount();

            const account = await authenticationStore.getState().verifyMfa('123456');

            expect(requestedUrls(fetchMock)).toEqual(['/api/account', '/api/mfa/verify', '/api/account']);
            expect(accountRequestSignal(fetchMock).aborted).toBe(true);
            expect(account).toEqual(ACCOUNT);

            preVerifyDeferred.resolve(emptyResponse(401));

            await preVerifyRequest;

            expect(authenticationStore.getState().authenticated).toBe(true);
        });

        it('flags a login error when the code is rejected', async () => {
            authenticationStore.setState({mfaRequired: true});

            stubFetch().mockResolvedValueOnce(emptyResponse(401));

            const account = await authenticationStore.getState().verifyMfa('000000');

            expect(account).toBeUndefined();
            expect(authenticationStore.getState().loginError).toBe(true);
            expect(authenticationStore.getState().mfaRequired).toBe(true);
        });

        it('flags a login error when the request fails', async () => {
            stubFetch().mockRejectedValueOnce(new TypeError('Failed to fetch'));

            const account = await authenticationStore.getState().verifyMfa('123456');

            expect(account).toBeUndefined();
            expect(authenticationStore.getState().loginError).toBe(true);
        });
    });

    describe('logout', () => {
        it('resets the session and fetches a fresh account on 200', async () => {
            authenticationStore.setState({account: ACCOUNT, authenticated: true, sessionHasBeenFetched: true});

            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(200))
                .mockResolvedValueOnce(emptyResponse(401));

            await authenticationStore.getState().logout();

            const state = authenticationStore.getState();

            expect(requestedUrls(fetchMock)).toEqual(['/api/logout', '/api/account']);
            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.showLogin).toBe(true);
        });

        it('keeps the session state but still fetches the account when logout fails', async () => {
            authenticationStore.setState({account: ACCOUNT, authenticated: true});

            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockResolvedValueOnce(emptyResponse(500))
                .mockReturnValueOnce(deferred.promise);

            await authenticationStore.getState().logout();

            expect(requestedUrls(fetchMock)).toEqual(['/api/logout', '/api/account']);
            expect(authenticationStore.getState().account).toEqual(ACCOUNT);
            expect(authenticationStore.getState().authenticated).toBe(true);

            deferred.resolve(accountResponse());
        });

        it('abandons the in-flight request even when the logout request fails', async () => {
            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(deferred.promise)
                .mockRejectedValueOnce(new TypeError('Failed to fetch'));

            authenticationStore.setState({account: ACCOUNT, authenticated: true});

            const staleRequest = authenticationStore.getState().getAccount();

            await expect(authenticationStore.getState().logout()).rejects.toThrow('Failed to fetch');

            expect(accountRequestSignal(fetchMock).aborted).toBe(true);
            expect(authenticationStore.getState().loading).toBe(false);

            deferred.resolve(accountResponse());

            await staleRequest;

            expect(authenticationStore.getState().account).toEqual(ACCOUNT);

            authenticationStore.getState().clearAuthentication();

            expect(authenticationStore.getState().authenticated).toBe(false);
        });

        it('does not let a request started before logout restore the old session', async () => {
            const staleDeferred = createDeferred<Response>();
            const freshDeferred = createDeferred<Response>();

            const fetchMock = stubFetch()
                .mockReturnValueOnce(staleDeferred.promise)
                .mockResolvedValueOnce(emptyResponse(200))
                .mockReturnValueOnce(freshDeferred.promise);

            const staleRequest = authenticationStore.getState().getAccount();

            await authenticationStore.getState().logout();

            expect(requestedUrls(fetchMock)).toEqual(['/api/account', '/api/logout', '/api/account']);

            freshDeferred.resolve(emptyResponse(401));

            await vi.waitFor(() => expect(authenticationStore.getState().sessionHasBeenFetched).toBe(true));

            staleDeferred.resolve(accountResponse());

            await expect(staleRequest).resolves.toEqual(ACCOUNT);

            const state = authenticationStore.getState();

            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.loading).toBe(false);
            expect(state.showLogin).toBe(true);
        });
    });

    describe('clearAuthentication', () => {
        it('does not let a request started before the session was cleared restore it', async () => {
            const deferred = createDeferred<Response>();

            stubFetch().mockReturnValueOnce(deferred.promise);

            const staleRequest = authenticationStore.getState().getAccount();

            authenticationStore.getState().clearAuthentication();

            deferred.resolve(accountResponse());

            await expect(staleRequest).resolves.toEqual(ACCOUNT);

            const state = authenticationStore.getState();

            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.showLogin).toBe(true);
        });

        it('marks the session unauthenticated and shows the login', () => {
            authenticationStore.setState({authenticated: true, loading: true, mfaRequired: true, showLogin: false});

            authenticationStore.getState().clearAuthentication();

            const state = authenticationStore.getState();

            expect(state.authenticated).toBe(false);
            expect(state.loading).toBe(false);
            expect(state.mfaRequired).toBe(false);
            expect(state.showLogin).toBe(true);
        });
    });

    describe('reset', () => {
        it('restores the initial state', () => {
            authenticationStore.setState({
                account: ACCOUNT,
                authenticated: true,
                loading: true,
                loginError: true,
                mfaRequired: true,
                sessionHasBeenFetched: true,
                showLogin: true,
            });

            authenticationStore.getState().reset();

            const state = authenticationStore.getState();

            expect(state.account).toBeUndefined();
            expect(state.authenticated).toBe(false);
            expect(state.loading).toBe(false);
            expect(state.loginError).toBe(false);
            expect(state.mfaRequired).toBe(false);
            expect(state.sessionHasBeenFetched).toBe(false);
            expect(state.showLogin).toBe(false);
        });

        it('drops the in-flight request so the next call fetches again', async () => {
            const deferred = createDeferred<Response>();
            const fetchMock = stubFetch()
                .mockReturnValueOnce(deferred.promise)
                .mockResolvedValueOnce(accountResponse());

            const staleRequest = authenticationStore.getState().getAccount();

            authenticationStore.getState().reset();

            const freshRequest = authenticationStore.getState().getAccount();

            expect(freshRequest).not.toBe(staleRequest);
            expect(fetchMock).toHaveBeenCalledTimes(2);

            await freshRequest;

            deferred.resolve(emptyResponse(401));

            await staleRequest;

            expect(authenticationStore.getState().authenticated).toBe(true);
        });
    });
});
