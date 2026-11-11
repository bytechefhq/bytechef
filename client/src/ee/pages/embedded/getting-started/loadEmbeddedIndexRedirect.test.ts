import {AUTHORITIES} from '@/shared/constants';
import {EditionType, applicationInfoStore} from '@/shared/stores/useApplicationInfoStore';
import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {environmentStore} from '@/shared/stores/useEnvironmentStore';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

import loadEmbeddedIndexRedirect from './loadEmbeddedIndexRedirect';

const createDeferred = () => {
    let resolveDeferred: () => void = () => undefined;
    let rejectDeferred: (error: Error) => void = () => undefined;

    const promise = new Promise<void>((resolve, reject) => {
        resolveDeferred = resolve;
        rejectDeferred = reject;
    });

    return {promise, reject: rejectDeferred, resolve: resolveDeferred};
};

const ADMIN_ACCOUNT = {
    activated: true,
    authorities: [AUTHORITIES.ADMIN],
    email: 'admin@localhost.com',
    id: 1,
};

describe('loadEmbeddedIndexRedirect', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();

        applicationInfoStore.setState(applicationInfoStore.getInitialState(), true);
        authenticationStore.setState(authenticationStore.getInitialState(), true);
        environmentStore.setState({currentEnvironmentId: 0});
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('redirects to getting started when edition and account arrive after the loader starts', async () => {
        const applicationInfoDeferred = createDeferred();
        const accountDeferred = createDeferred();

        applicationInfoStore.setState({
            getApplicationInfo: vi.fn(async () => {
                await applicationInfoDeferred.promise;

                applicationInfoStore.setState({application: {edition: EditionType.EE}});
            }),
        });
        authenticationStore.setState({
            getAccount: vi.fn(async () => {
                await accountDeferred.promise;

                authenticationStore.setState({
                    account: ADMIN_ACCOUNT,
                    authenticated: true,
                    sessionHasBeenFetched: true,
                });

                return ADMIN_ACCOUNT;
            }),
        });

        const querySpy = vi.spyOn(queryClient, 'query').mockResolvedValue({embeddedGettingStartedCompleted: false});

        const redirectPathPromise = loadEmbeddedIndexRedirect(queryClient);

        applicationInfoDeferred.resolve();
        accountDeferred.resolve();

        await expect(redirectPathPromise).resolves.toBe('getting-started');
        expect(querySpy).toHaveBeenCalledWith(expect.objectContaining({queryKey: ['embeddedGettingStartedCompleted']}));
    });

    it('does not refetch information that is already loaded', async () => {
        const getApplicationInfo = vi.fn(() => Promise.resolve());
        const getAccount = vi.fn(() => Promise.resolve(ADMIN_ACCOUNT));

        applicationInfoStore.setState({application: {edition: EditionType.EE}, getApplicationInfo});
        authenticationStore.setState({
            account: ADMIN_ACCOUNT,
            authenticated: true,
            getAccount,
            sessionHasBeenFetched: true,
        });

        vi.spyOn(queryClient, 'query').mockResolvedValue({embeddedGettingStartedCompleted: true});

        await expect(loadEmbeddedIndexRedirect(queryClient)).resolves.toBe('integrations');
        expect(getApplicationInfo).not.toHaveBeenCalled();
        expect(getAccount).not.toHaveBeenCalled();
    });

    it('redirects unauthenticated users to integrations without querying completion', async () => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({
            getAccount: vi.fn(async () => {
                authenticationStore.setState({authenticated: false, sessionHasBeenFetched: true});

                return undefined;
            }),
        });

        const querySpy = vi.spyOn(queryClient, 'query');

        await expect(loadEmbeddedIndexRedirect(queryClient)).resolves.toBe('integrations');
        expect(querySpy).not.toHaveBeenCalled();
    });

    it('redirects to integrations outside Development without querying completion', async () => {
        applicationInfoStore.setState({application: {edition: EditionType.EE}});
        authenticationStore.setState({account: ADMIN_ACCOUNT, authenticated: true, sessionHasBeenFetched: true});
        environmentStore.setState({currentEnvironmentId: 1});

        const querySpy = vi.spyOn(queryClient, 'query');

        await expect(loadEmbeddedIndexRedirect(queryClient)).resolves.toBe('integrations');
        expect(querySpy).not.toHaveBeenCalled();
    });

    it('falls back to integrations when the application info request fails', async () => {
        const applicationInfoDeferred = createDeferred();

        applicationInfoStore.setState({getApplicationInfo: vi.fn(() => applicationInfoDeferred.promise)});
        authenticationStore.setState({
            account: ADMIN_ACCOUNT,
            authenticated: true,
            sessionHasBeenFetched: true,
        });

        const querySpy = vi.spyOn(queryClient, 'query');

        const redirectPathPromise = loadEmbeddedIndexRedirect(queryClient);

        applicationInfoDeferred.reject(new Error('Request failed'));

        await expect(redirectPathPromise).resolves.toBe('integrations');
        expect(querySpy).not.toHaveBeenCalled();
    });
});
