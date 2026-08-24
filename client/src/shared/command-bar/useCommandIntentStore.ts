import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

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

            set(() => ({intent: undefined}));

            return intent;
        },
        intent: undefined,
        publish: (key: string, payload?: unknown) => {
            cancelUnclaimedIntentTimeout();

            set(() => ({intent: {key, payload}}));

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
