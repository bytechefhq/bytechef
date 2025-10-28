// Module under test
import {loadEnvironments} from '@/routes';
import {QueryClient} from '@tanstack/react-query';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

// Mocks
vi.mock('@/shared/stores/useAuthenticationStore', () => {
    return {
        authenticationStore: {
            getState: vi.fn(() => ({authenticated: false})),
        },
    };
});

vi.mock('@/shared/stores/useEnvironmentStore', () => {
    return {
        environmentStore: {
            getState: vi.fn(() => ({setEnvironments: vi.fn()})),
        },
    };
});

// We don't need to mock EnvironmentApi because we'll stub QueryClient.fetchQuery directly

const authModule = await import('@/shared/stores/useAuthenticationStore');
const envStoreModule = await import('@/shared/stores/useEnvironmentStore');

describe('loadEnvironmentsIfAuthenticated', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        queryClient = new QueryClient();
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('does nothing when not authenticated', async () => {
        // Arrange
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (authModule.authenticationStore.getState as any).mockReturnValue({authenticated: false});

        const fetchSpy = vi.spyOn(queryClient, 'fetchQuery');

        const setEnvironmentsSpy = vi.fn();
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (envStoreModule.environmentStore.getState as any).mockReturnValue({setEnvironments: setEnvironmentsSpy});

        // Act
        await loadEnvironments(queryClient);

        // Assert
        expect(fetchSpy).not.toHaveBeenCalled();
        expect(setEnvironmentsSpy).not.toHaveBeenCalled();
    });

    it('fetches environments and sets them when authenticated', async () => {
        // Arrange
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (authModule.authenticationStore.getState as any).mockReturnValue({authenticated: true});

        const environments = [
            {id: 1, name: 'Dev'},
            {id: 2, name: 'Prod'},
        ];
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const fetchSpy = vi.spyOn(queryClient, 'fetchQuery').mockResolvedValue(environments as any);

        const setEnvironmentsSpy = vi.fn();
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (envStoreModule.environmentStore.getState as any).mockReturnValue({setEnvironments: setEnvironmentsSpy});

        // Act
        await loadEnvironments(queryClient);

        // Assert
        expect(fetchSpy).toHaveBeenCalledTimes(1);
        expect(setEnvironmentsSpy).toHaveBeenCalledWith(environments);
    });
});
