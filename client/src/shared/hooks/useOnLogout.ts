import {useAuthenticationStore} from '@/shared/stores/useAuthenticationStore';
import {useEffect, useRef} from 'react';

export function useOnLogout(callback: () => void): void {
    const authenticated = useAuthenticationStore((state) => state.authenticated);

    const previousAuthenticatedRef = useRef(authenticated);
    const callbackRef = useRef(callback);

    callbackRef.current = callback;

    useEffect(() => {
        const wasAuthenticated = previousAuthenticatedRef.current;

        previousAuthenticatedRef.current = authenticated;

        if (wasAuthenticated && !authenticated) {
            callbackRef.current();
        }
    }, [authenticated]);
}
