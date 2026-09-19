import {useEffect, useRef} from 'react';
import {toast} from 'sonner';

/**
 * Surface query errors that would otherwise be silently swallowed. Raw-fetch api helpers (AI Hub, auto-memories)
 * don't go through the global GraphQL interceptor, so a failed list/get request would show "0 items" with no
 * toast and no inline error. We toast on the *first* time a given error message is observed (per hook instance) to
 * avoid toast-spam when React Query retries automatically.
 */
export function useReportQueryError(action: string, error: Error | null) {
    const lastReportedMessageRef = useRef<string | null>(null);

    useEffect(() => {
        if (!error) {
            lastReportedMessageRef.current = null;

            return;
        }

        const message = error.message || `${action} failed`;

        if (lastReportedMessageRef.current === message) {
            return;
        }

        lastReportedMessageRef.current = message;

        console.error(`${action} failed:`, error);

        toast.error(message);
    }, [action, error]);
}
