import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {create} from 'zustand';

type PostTurnCallbackType = () => void;

interface PostTurnRegistryStateI {
    callbacks: Partial<Record<Source, PostTurnCallbackType[]>>;
    register: (source: Source, callback: PostTurnCallbackType) => () => void;
    runFor: (source: Source) => void;
}

// Many surfaces share one Source: a listing page and its detail page both refresh on DATA_TABLE turns, and each
// needs its own invalidation. Callbacks accumulate per source, and the cleanup returned by register removes only
// its own entry.
const useCopilotPostTurnRegistry = create<PostTurnRegistryStateI>((set, get) => ({
    callbacks: {},
    register: (source, callback) => {
        set((state) => ({callbacks: {...state.callbacks, [source]: [...(state.callbacks[source] ?? []), callback]}}));

        // Each cleanup owns exactly one registration: it removes a single occurrence (the same function may be
        // registered by two surfaces) and does nothing if called again, since React StrictMode invokes effect
        // cleanups twice.
        let unregistered = false;

        return () => {
            if (unregistered) {
                return;
            }

            unregistered = true;

            set((state) => {
                const sourceCallbacks = state.callbacks[source];

                if (!sourceCallbacks) {
                    return state;
                }

                const callbackIndex = sourceCallbacks.indexOf(callback);

                if (callbackIndex === -1) {
                    return state;
                }

                const remainingCallbacks = [
                    ...sourceCallbacks.slice(0, callbackIndex),
                    ...sourceCallbacks.slice(callbackIndex + 1),
                ];

                const nextCallbacks = {...state.callbacks};

                if (remainingCallbacks.length > 0) {
                    nextCallbacks[source] = remainingCallbacks;
                } else {
                    delete nextCallbacks[source];
                }

                return {callbacks: nextCallbacks};
            });
        };
    },
    runFor: (source) => {
        // Copy before iterating: a callback may unregister during the run.
        const sourceCallbacks = [...(get().callbacks[source] ?? [])];

        for (const callback of sourceCallbacks) {
            callback();
        }
    },
}));

export default useCopilotPostTurnRegistry;
