import {useActivateStore} from '@/pages/account/public/stores/useActivateStore';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';

const stubFetch = () => {
    const fetchMock = vi.fn();

    vi.stubGlobal('fetch', fetchMock);

    return fetchMock;
};

describe('useActivateStore', () => {
    beforeEach(() => {
        document.cookie = 'XSRF-TOKEN=xsrf-token';

        useActivateStore.setState({activationFailure: false, activationSuccess: false, loading: false});
    });

    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('marks the activation successful on 200', async () => {
        stubFetch().mockResolvedValue(new Response(null, {status: 200}));

        await useActivateStore.getState().activate('activation-key');

        const state = useActivateStore.getState();

        expect(state.activationSuccess).toBe(true);
        expect(state.activationFailure).toBe(false);
        expect(state.loading).toBe(false);
    });

    it('marks the activation failed on a non-200 response', async () => {
        stubFetch().mockResolvedValue(new Response(null, {status: 500}));

        await useActivateStore.getState().activate('activation-key');

        const state = useActivateStore.getState();

        expect(state.activationSuccess).toBe(false);
        expect(state.activationFailure).toBe(true);
        expect(state.loading).toBe(false);
    });

    it('marks the activation failed and stops loading when the request fails at the network level', async () => {
        stubFetch().mockRejectedValue(new TypeError('Failed to fetch'));

        await expect(useActivateStore.getState().activate('activation-key')).resolves.toBeUndefined();

        const state = useActivateStore.getState();

        expect(state.activationSuccess).toBe(false);
        expect(state.activationFailure).toBe(true);
        expect(state.loading).toBe(false);
    });

    it('allows a new activation attempt after a network failure', async () => {
        const fetchMock = stubFetch();

        fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'));
        fetchMock.mockResolvedValueOnce(new Response(null, {status: 200}));

        await useActivateStore.getState().activate('activation-key');

        await useActivateStore.getState().activate('activation-key');

        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(useActivateStore.getState().activationSuccess).toBe(true);
    });
});
