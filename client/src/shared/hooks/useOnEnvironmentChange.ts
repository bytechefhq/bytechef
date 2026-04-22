import {useEnvironmentStore} from '@/shared/stores/useEnvironmentStore';
import {useEffect, useRef} from 'react';

export function useOnEnvironmentChange(callback: () => void): void {
    const currentEnvironmentId = useEnvironmentStore((state) => state.currentEnvironmentId);

    const previousEnvironmentIdRef = useRef(currentEnvironmentId);
    const callbackRef = useRef(callback);

    callbackRef.current = callback;

    useEffect(() => {
        if (previousEnvironmentIdRef.current === currentEnvironmentId) {
            return;
        }

        previousEnvironmentIdRef.current = currentEnvironmentId;

        callbackRef.current();
    }, [currentEnvironmentId]);
}
