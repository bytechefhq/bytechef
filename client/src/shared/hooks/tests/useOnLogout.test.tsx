import {authenticationStore} from '@/shared/stores/useAuthenticationStore';
import {act, renderHook} from '@testing-library/react';
import {afterEach, describe, expect, it, vi} from 'vitest';

import {useOnLogout} from '../useOnLogout';

describe('useOnLogout', () => {
    afterEach(() => {
        authenticationStore.setState({authenticated: false});
    });

    it('does not invoke callback when mounted for a logged-out visitor', () => {
        authenticationStore.setState({authenticated: false});

        const callback = vi.fn();

        renderHook(() => useOnLogout(callback));

        expect(callback).not.toHaveBeenCalled();
    });

    it('does not invoke callback when the visitor logs in', () => {
        authenticationStore.setState({authenticated: false});

        const callback = vi.fn();

        renderHook(() => useOnLogout(callback));

        act(() => {
            authenticationStore.setState({authenticated: true});
        });

        expect(callback).not.toHaveBeenCalled();
    });

    it('invokes callback when an authenticated user logs out', () => {
        authenticationStore.setState({authenticated: true});

        const callback = vi.fn();

        renderHook(() => useOnLogout(callback));

        act(() => {
            authenticationStore.setState({authenticated: false});
        });

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('invokes callback when a user logs in and then logs out', () => {
        authenticationStore.setState({authenticated: false});

        const callback = vi.fn();

        renderHook(() => useOnLogout(callback));

        act(() => {
            authenticationStore.setState({authenticated: true});
        });
        act(() => {
            authenticationStore.setState({authenticated: false});
        });

        expect(callback).toHaveBeenCalledTimes(1);
    });

    it('uses the latest callback when the consumer re-renders with a new one', () => {
        authenticationStore.setState({authenticated: true});

        const initialCallback = vi.fn();
        const laterCallback = vi.fn();

        const {rerender} = renderHook(({callback}) => useOnLogout(callback), {
            initialProps: {callback: initialCallback},
        });

        rerender({callback: laterCallback});

        act(() => {
            authenticationStore.setState({authenticated: false});
        });

        expect(initialCallback).not.toHaveBeenCalled();
        expect(laterCallback).toHaveBeenCalledTimes(1);
    });
});
