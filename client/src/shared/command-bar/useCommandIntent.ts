import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {useEffect, useRef} from 'react';

export function useCommandIntent(key: string, handler: (payload?: unknown) => void, enabled = true): void {
    const handlerRef = useRef(handler);

    handlerRef.current = handler;

    const claim = useCommandIntentStore((state) => state.claim);
    const intent = useCommandIntentStore((state) => state.intent);

    useEffect(() => {
        if (!enabled || intent?.key !== key) {
            return;
        }

        const claimed = claim(key);

        if (claimed) {
            handlerRef.current(claimed.payload);
        }
    }, [claim, enabled, intent, key]);
}
