import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

// Long enough for a cold route chunk plus its data query -- every claimant sits behind a data gate (e.g.
// Connections waits on the full component-definition list), so 5s was too tight on a cold load -- but short enough
// that a stale intent cannot survive the user wandering away and back.
export const UNCLAIMED_INTENT_LIFETIME = 15000;

export interface CommandIntentI {
    key: string;
    payload?: unknown;
}

interface CommandIntentStateI {
    claim: (key: string) => CommandIntentI | undefined;
    intent: CommandIntentI | undefined;
    publish: (key: string, payload?: unknown) => void;
    reset: () => void;
}

// Held outside the store because a timeout handle is not state anyone should read or persist.
let unclaimedIntentTimeoutId: ReturnType<typeof setTimeout> | undefined;

function cancelUnclaimedIntentTimeout() {
    if (unclaimedIntentTimeoutId !== undefined) {
        clearTimeout(unclaimedIntentTimeoutId);

        unclaimedIntentTimeoutId = undefined;
    }
}

export const useCommandIntentStore = create<CommandIntentStateI>()(
    devtools((set, get) => ({
        claim: (key: string) => {
            const {intent} = get();

            if (intent?.key !== key) {
                return undefined;
            }

            cancelUnclaimedIntentTimeout();

            // Cleared synchronously so a page rendering the same dialog twice opens only one of them.
            set(() => ({intent: undefined}));

            return intent;
        },
        intent: undefined,
        publish: (key: string, payload?: unknown) => {
            cancelUnclaimedIntentTimeout();

            set(() => ({intent: {key, payload}}));

            // An intent that outlives its navigation must not fire on an unrelated screen later -- without this
            // expiry, a stale "dataTable.create" published just before the user navigates away from a
            // still-loading list page would sit in the store forever and pop open the next unrelated
            // CreateDataTableDialog instance the user happens to mount (e.g. a single data table's own page).
            unclaimedIntentTimeoutId = setTimeout(() => {
                if (get().intent?.key === key) {
                    if (import.meta.env.DEV) {
                        console.warn(`Command intent "${key}" expired without being claimed.`);
                    }

                    set(() => ({intent: undefined}));
                }
            }, UNCLAIMED_INTENT_LIFETIME);
        },
        reset: () => {
            cancelUnclaimedIntentTimeout();

            set(() => ({intent: undefined}));
        },
    }))
);
