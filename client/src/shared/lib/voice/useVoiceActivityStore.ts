import {create} from 'zustand';
import {devtools} from 'zustand/middleware';

export type VoiceActivityType = 'idle' | 'reconnecting' | {tool: string};

export interface VoiceActivityStateI {
    activity: VoiceActivityType;
    beginSession: () => number;
    endReason: string | null;
    generation: number;
    reset: () => void;
    setActivity: (activity: VoiceActivityType, generation?: number) => void;
    setEndReason: (endReason: string | null, generation?: number) => void;
}

export const voiceActivityStore = create<VoiceActivityStateI>()(
    devtools(
        (set, get) => ({
            activity: 'idle',
            beginSession: () => {
                const generation = get().generation + 1;

                set({activity: 'idle', endReason: null, generation});

                return generation;
            },
            endReason: null,
            generation: 0,
            reset: () => set({activity: 'idle', endReason: null}),
            setActivity: (activity, generation) => {
                if (generation !== undefined && generation !== get().generation) {
                    return;
                }

                set({activity});
            },
            setEndReason: (endReason, generation) => {
                if (generation !== undefined && generation !== get().generation) {
                    return;
                }

                set({endReason});
            },
        }),
        {name: 'VoiceActivityStore'}
    )
);

export const useVoiceActivityStore = voiceActivityStore;
