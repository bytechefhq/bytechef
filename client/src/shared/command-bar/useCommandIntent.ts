import {useCommandIntentStore} from '@/shared/command-bar/useCommandIntentStore';
import {useEffect, useRef} from 'react';

/**
 * Claims a one-shot command intent. Call it from the component that owns the state the intent should change -- for
 * the creation dialogs that is the dialog itself, since none of them expose a controlled `open` prop.
 *
 * `enabled` defaults to `true` so a component with a single call site can ignore it, but a component reused across
 * many call sites (most creation dialogs) must pass it through explicitly and default it to `false` at the call
 * site -- otherwise every mount of that component anywhere in the app becomes an eligible claimant for the intent,
 * including instances that are not the command's actual destination. The hook itself is always called
 * unconditionally; only the claim inside the effect is gated, since hooks cannot be called conditionally.
 *
 * The effect reacts to the published `intent` itself, not just to mount. A command runs `[navigate(page), intent
 * (key)]`, but navigating to the route the user is already on does not remount the route element -- an
 * already-mounted claimant would never see a mount-only effect re-run, so a create command issued while already
 * standing on its destination page would silently do nothing. Reading `intent` from the store keeps this working
 * for that same-route case while staying one-shot: `claim` clears the store synchronously, so once claimed the
 * effect's own dependency (`intent`) reverts to `undefined` and it does not fire again for the same publication. It
 * still cannot fire on a later remount or a page refresh, since the store is not persisted -- that is intentional,
 * matching the intent's one-shot contract.
 */
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
