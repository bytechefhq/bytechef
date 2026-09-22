import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {create} from 'zustand';

type PostTurnCallbackType = () => void;

interface PostTurnRegistryStateI {
    callbacks: Partial<Record<Source, PostTurnCallbackType[]>>;
    register: (source: Source, callback: PostTurnCallbackType) => () => void;
    runFor: (source: Source) => void;
}

const useCopilotPostTurnRegistry = create<PostTurnRegistryStateI>((set, get) => ({
    callbacks: {},
    register: (source, callback) => {
        set((state) => ({callbacks: {...state.callbacks, [source]: [...(state.callbacks[source] ?? []), callback]}}));

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
        const sourceCallbacks = [...(get().callbacks[source] ?? [])];

        for (const callback of sourceCallbacks) {
            try {
                callback();
            } catch (error) {
                console.error('Copilot post-turn callback failed', {error, source});
            }
        }
    },
}));

export default useCopilotPostTurnRegistry;
