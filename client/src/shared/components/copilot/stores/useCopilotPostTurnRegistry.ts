import {Source} from '@/shared/components/copilot/stores/useCopilotStore';
import {create} from 'zustand';

type PostTurnCallbackType = () => void;

interface PostTurnRegistryStateI {
    callbacks: Partial<Record<Source, PostTurnCallbackType>>;
    register: (source: Source, callback: PostTurnCallbackType) => () => void;
    runFor: (source: Source) => void;
}

const useCopilotPostTurnRegistry = create<PostTurnRegistryStateI>((set, get) => ({
    callbacks: {},
    register: (source, callback) => {
        set((state) => ({callbacks: {...state.callbacks, [source]: callback}}));

        return () => {
            set((state) => {
                if (state.callbacks[source] !== callback) {
                    return state;
                }

                const nextCallbacks = {...state.callbacks};

                delete nextCallbacks[source];

                return {callbacks: nextCallbacks};
            });
        };
    },
    runFor: (source) => {
        get().callbacks[source]?.();
    },
}));

export default useCopilotPostTurnRegistry;
